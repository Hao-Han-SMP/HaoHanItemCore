package vn.haohan.itemcore.internal.block;

import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.api.block.NativeCustomBlock;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * High-performance concurrent in-memory implementation of {@link CustomBlockRegistry}.
 */
public final class DefaultCustomBlockRegistry implements CustomBlockRegistry {

    private final Map<String, CustomBlockDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<org.bukkit.Material, CustomBlockDefinition> vanillaOverwrites = new ConcurrentHashMap<>();
    private final Map<Integer, NativeCustomBlock> nativeByVariation = new ConcurrentHashMap<>();
    private final Logger logger;

    public DefaultCustomBlockRegistry() {
        this(null);
    }

    public DefaultCustomBlockRegistry(Logger logger) {
        this.logger = logger != null ? logger : Logger.getLogger(DefaultCustomBlockRegistry.class.getName());
    }

    @Override
    public void register(CustomBlockDefinition definition) {
        Objects.requireNonNull(definition, "CustomBlockDefinition cannot be null");
        String id = definition.getId();
        CustomBlockDefinition existing = definitions.putIfAbsent(id, definition);
        if (existing != null) {
            throw new IllegalArgumentException("Custom block with ID '" + id + "' is already registered!");
        }
        if (definition instanceof NativeCustomBlock nativeBlock) {
            NativeCustomBlock existingVar = nativeByVariation.putIfAbsent(nativeBlock.getCustomVariation(), nativeBlock);
            if (existingVar != null) {
                definitions.remove(id);
                throw new IllegalArgumentException("Custom block variation " + nativeBlock.getCustomVariation()
                        + " is already registered by '" + existingVar.getId() + "'!");
            }
        }
        if (definition.isVanillaOverwrite()) {
            vanillaOverwrites.put(definition.getBaseBlock(), definition);
        }
    }

    @Override
    public Optional<CustomBlockDefinition> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(definitions.get(id));
    }

    @Override
    public Optional<CustomBlockDefinition> getByMaterial(org.bukkit.Material material) {
        if (material == null) return Optional.empty();
        return Optional.ofNullable(vanillaOverwrites.get(material));
    }

    @Override
    public Optional<NativeCustomBlock> getByVariation(int variation) {
        return Optional.ofNullable(nativeByVariation.get(variation));
    }

    @Override
    public Optional<NativeCustomBlock> getByBlockData(BlockData blockData) {
        if (blockData instanceof NoteBlock noteBlock) {
            int variation = NoteBlockVariationTable.resolveVariation(noteBlock);
            if (variation != -1) {
                return getByVariation(variation);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean has(String id) {
        return id != null && definitions.containsKey(id);
    }

    @Override
    public Collection<CustomBlockDefinition> all() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    @Override
    public int size() {
        return definitions.size();
    }

    @Override
    public void clear() {
        definitions.clear();
        vanillaOverwrites.clear();
        nativeByVariation.clear();
    }
}
