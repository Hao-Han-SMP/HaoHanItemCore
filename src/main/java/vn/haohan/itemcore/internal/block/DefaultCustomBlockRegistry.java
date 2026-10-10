package vn.haohan.itemcore.internal.block;

import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;

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
    }

    @Override
    public Optional<CustomBlockDefinition> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(definitions.get(id));
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
    }
}
