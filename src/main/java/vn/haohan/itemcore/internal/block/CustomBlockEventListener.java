package vn.haohan.itemcore.internal.block;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockService;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Event listener managing physical Barrier block interactions, lifecycle events,
 * and spatial visibility triggers.
 */
public final class CustomBlockEventListener implements Listener {

    private final CustomBlockService customBlockService;
    private final CustomBlockStorage storage;
    private final CustomBlockVisibilityManager visibilityManager;
    private final ItemRegistry itemRegistry;
    private final Plugin plugin;
    private final NamespacedKey itemIdKey;

    public CustomBlockEventListener(
            CustomBlockService customBlockService,
            CustomBlockStorage storage,
            CustomBlockVisibilityManager visibilityManager,
            ItemRegistry itemRegistry,
            Plugin plugin) {
        this.customBlockService = Objects.requireNonNull(customBlockService, "customBlockService cannot be null");
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
        this.visibilityManager = Objects.requireNonNull(visibilityManager, "visibilityManager cannot be null");
        this.itemRegistry = itemRegistry;
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    // --- Block Placement ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(itemIdKey, PersistentDataType.STRING)) return;

        String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null) return;

        String customBlockId = resolveCustomBlockId(itemId);
        if (customBlockId == null) return;

        Block block = event.getBlockPlaced();
        Location loc = block.getLocation();

        // Mutate block to Barrier and register
        block.setType(Material.BARRIER, false);
        customBlockService.placeCustomBlock(loc, customBlockId);
    }

    private String resolveCustomBlockId(String itemId) {
        if (customBlockService.getRegistry().has(itemId)) {
            return itemId;
        }
        if (itemRegistry != null) {
            ItemDefinition itemDef = itemRegistry.get(itemId);
            if (itemDef != null) {
                Object customBlockIdObj = itemDef.getProperties().get("custom_block_id");
                if (customBlockIdObj instanceof String cbId && customBlockService.getRegistry().has(cbId)) {
                    return cbId;
                }
            }
        }
        return null;
    }

    // --- Block Breaking ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!customBlockService.isCustomBlock(block)) return;

        Optional<CustomBlockDefinition> optDef = customBlockService.getCustomBlock(block);
        if (optDef.isEmpty()) return;
        CustomBlockDefinition def = optDef.get();

        // Cancel default Barrier particles and drops
        event.setDropItems(false);

        // Remove from custom block system
        Location loc = block.getLocation();
        customBlockService.removeCustomBlock(loc);

        // Creative players should not drop items
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        dropCustomBlockItem(loc, def, event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand());
    }

    private void dropCustomBlockItem(Location loc, CustomBlockDefinition def, Player player, ItemStack tool) {
        if (loc == null || loc.getWorld() == null || def == null) return;
        vn.haohan.itemcore.api.block.MiningContext context = new vn.haohan.itemcore.api.block.MiningContext(player, loc.getBlock(), def, tool);
        if (def.getMiningCondition().canHarvest(context)) {
            List<ItemStack> drops = def.getDropProvider().getDrops(context);
            if (drops != null) {
                for (ItemStack drop : drops) {
                    if (drop != null) {
                        loc.getWorld().dropItemNaturally(loc, drop);
                    }
                }
            }
        }
    }

    // --- Explosions ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (customBlockService.isCustomBlock(block)) {
                Optional<CustomBlockDefinition> optDef = customBlockService.getCustomBlock(block);
                Location loc = block.getLocation();
                customBlockService.removeCustomBlock(loc);
                optDef.ifPresent(def -> dropCustomBlockItem(loc, def, null, null));
                it.remove(); // Prevent vanilla barrier handling
            }
        }
    }

    // --- Pistons ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (customBlockService.isCustomBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (customBlockService.isCustomBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // --- Player Interactions ---

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // Prevent interacting into existing custom blocks when not sneaking
        if (customBlockService.isCustomBlock(clicked) && !event.getPlayer().isSneaking()) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }

        // Support survival/adventure placement for BARRIER-based custom block items
        handleBarrierPlacement(event, clicked);
    }

    private void handleBarrierPlacement(PlayerInteractEvent event, Block clicked) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BARRIER || !item.hasItemMeta()) return;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(itemIdKey, PersistentDataType.STRING)) return;
        String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null) return;

        String customBlockId = resolveCustomBlockId(itemId);
        if (customBlockId == null) return;

        Player player = event.getPlayer();
        // Creative mode can rely on vanilla BlockPlaceEvent if allowed or this fallback
        BlockFace face = event.getBlockFace();
        Block target = clicked.isReplaceable() ? clicked : clicked.getRelative(face);

        if (!target.isReplaceable() && !target.isEmpty()) return;
        if (target.getY() < target.getWorld().getMinHeight() || target.getY() >= target.getWorld().getMaxHeight()) return;

        // Check if player bounding box collides with target
        org.bukkit.util.BoundingBox targetBox = org.bukkit.util.BoundingBox.of(target);
        if (player.getBoundingBox().overlaps(targetBox)) {
            return;
        }

        // Trigger BlockPlaceEvent for permission plugins / protections
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                target, target.getState(), clicked, item, player, true,
                event.getHand() != null ? event.getHand() : EquipmentSlot.HAND);
        org.bukkit.Bukkit.getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled() || !placeEvent.canBuild()) {
            return;
        }

        // Mutate block to Barrier and register
        target.setType(Material.BARRIER, false);
        customBlockService.placeCustomBlock(target.getLocation(), customBlockId);

        // Deduct item if in survival/adventure
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.subtract(1);
        }

        if (event.getHand() != null) {
            player.swingHand(event.getHand());
        }
        target.getWorld().playSound(target.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
        event.setCancelled(true);
    }

    // --- Player Movement & Spatial Culling ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        int fromChunkX = from.getBlockX() >> 4;
        int fromChunkZ = from.getBlockZ() >> 4;
        int toChunkX = to.getBlockX() >> 4;
        int toChunkZ = to.getBlockZ() >> 4;

        if (fromChunkX != toChunkX || fromChunkZ != toChunkZ) {
            visibilityManager.handlePlayerMove(event.getPlayer(), fromChunkX, fromChunkZ, toChunkX, toChunkZ);
        }
    }

    // --- Lifecycle Events ---

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        visibilityManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        visibilityManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        visibilityManager.handlePlayerTeleport(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        visibilityManager.handlePlayerWorldChange(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                visibilityManager.handlePlayerTeleport(event.getPlayer());
            }
        });
    }

    // --- Chunk Lifecycle ---

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        storage.loadChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        visibilityManager.handleChunkUnload(event.getChunk());
        storage.unloadChunk(event.getChunk());
    }
}
