package vn.haohan.itemcore.internal.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.api.block.CustomBlockService;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Standard implementation of {@link CustomBlockService}.
 */
public final class DefaultCustomBlockService implements CustomBlockService {

    private final CustomBlockRegistry registry;
    private final CustomBlockStorage storage;
    private final CustomBlockVisibilityManager visibilityManager;
    private final Plugin plugin;
    private final Logger logger;

    public DefaultCustomBlockService(
            CustomBlockRegistry registry,
            CustomBlockStorage storage,
            CustomBlockVisibilityManager visibilityManager,
            Plugin plugin) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
        this.visibilityManager = Objects.requireNonNull(visibilityManager, "visibilityManager cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.logger = plugin.getLogger();
    }

    @Override
    public boolean isCustomBlock(Block block) {
        if (block == null) return false;
        // Optimization: Physical custom blocks are always BARRIER
        if (block.getType() != Material.BARRIER) {
            return false;
        }
        return getCustomBlockId(block) != null;
    }

    @Override
    public Optional<CustomBlockDefinition> getCustomBlock(Block block) {
        String id = getCustomBlockId(block);
        if (id == null) return Optional.empty();
        return registry.get(id);
    }

    @Override
    public String getCustomBlockId(Block block) {
        if (block == null) return null;
        if (block.getType() != Material.BARRIER) {
            return null;
        }
        return storage.get(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    @Override
    public boolean placeCustomBlock(Location location, String customBlockId) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(customBlockId, "customBlockId cannot be null");

        var optDef = registry.get(customBlockId);
        if (optDef.isEmpty()) {
            logger.warning("Attempted to place unregistered custom block ID: " + customBlockId);
            return false;
        }
        CustomBlockDefinition def = optDef.get();

        World world = location.getWorld();
        if (world == null) return false;

        // Ensure physical block is Barrier
        Block block = location.getBlock();
        block.setType(Material.BARRIER, false);

        // Record in spatial storage
        storage.set(world, location.getBlockX(), location.getBlockY(), location.getBlockZ(), customBlockId);
        try {
            storage.saveChunk(location.getChunk());
        } catch (Throwable ignored) {}

        // Dispatch visual packet to viewers
        visibilityManager.handleBlockPlaced(location, def);

        return true;
    }

    @Override
    public boolean removeCustomBlock(Location location) {
        Objects.requireNonNull(location, "location cannot be null");
        World world = location.getWorld();
        if (world == null) return false;

        String removedId = storage.remove(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (removedId == null) {
            return false;
        }
        try {
            storage.saveChunk(location.getChunk());
        } catch (Throwable ignored) {}

        // Set physical block to AIR
        Block block = location.getBlock();
        if (block.getType() == Material.BARRIER) {
            block.setType(Material.AIR, false);
        }

        // Dispatch destroy packet to viewers
        visibilityManager.handleBlockBroken(location);

        return true;
    }

    @Override
    public void register(CustomBlockDefinition definition) {
        registry.register(definition);
    }

    @Override
    public CustomBlockRegistry getRegistry() {
        return registry;
    }

    private vn.haohan.itemcore.internal.block.CustomBlockMiningManager miningManager;

    public vn.haohan.itemcore.internal.block.CustomBlockMiningManager getMiningManager() {
        return miningManager;
    }

    public void setMiningManager(vn.haohan.itemcore.internal.block.CustomBlockMiningManager miningManager) {
        this.miningManager = miningManager;
    }

    public CustomBlockStorage getStorage() {
        return storage;
    }

    public CustomBlockVisibilityManager getVisibilityManager() {
        return visibilityManager;
    }

    @Override
    public float calculateMiningTimeSeconds(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack tool, CustomBlockDefinition definition) {
        if (definition == null || definition.isUnbreakable()) {
            return -1.0f;
        }
        if (definition.isInstantBreak()) {
            return 0.0f;
        }
        float progressPerTick = MiningSpeedCalculator.calculateProgressPerTick(player, tool, definition);
        if (progressPerTick <= 0.0f) {
            return -1.0f;
        }
        return (1.0f / progressPerTick) / 20.0f;
    }
}
