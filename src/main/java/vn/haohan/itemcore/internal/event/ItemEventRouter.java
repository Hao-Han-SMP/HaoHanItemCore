package vn.haohan.itemcore.internal.event;

import vn.haohan.itemcore.api.item.*;
import vn.haohan.itemcore.internal.item.DefaultItemFactory;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Event Router: nhận Minecraft event, xác định custom item, và dispatch tới ItemBehavior.
 * 
 * <p>Flow:
 * <pre>
 * PlayerInteractEvent → HaoHanItemCore → Item ID → ItemDefinition → ItemBehavior.onUse()
 * </pre>
 */
public final class ItemEventRouter implements Listener {

    private final ItemRegistry registry;
    private final NamespacedKey itemIdKey;
    private final Logger logger;
    private final Plugin plugin;

    public ItemEventRouter(ItemRegistry registry, Plugin plugin) {
        this.registry = registry;
        this.itemIdKey = new NamespacedKey(plugin, DefaultItemFactory.ITEM_ID_KEY_NAME);
        this.logger = plugin.getLogger();
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        ItemDefinition definition = getDefinition(item);
        if (definition == null || !definition.hasBehavior()) return;

        ItemContext context = new ItemContext(event.getPlayer(), item, definition, event);

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> definition.getBehavior().onUse(context);
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> definition.getBehavior().onInteract(context);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        ItemDefinition definition = getDefinition(item);
        if (definition == null || !definition.hasBehavior()) return;

        ItemContext context = new ItemContext(event.getPlayer(), item, definition, event);
        definition.getBehavior().onBreak(context);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();

        ItemDefinition definition = getDefinition(result);
        if (definition == null || !definition.hasBehavior()) return;

        if (event.getWhoClicked() instanceof Player player) {
            ItemContext context = new ItemContext(player, result, definition, event);
            definition.getBehavior().onCraft(context);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item != null) {
            vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().validateAndUpdate(item);
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null) {
            vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().validateAndUpdate(cursor);
        }

        if (item == null) return;

        ItemDefinition definition = getDefinition(item);
        if (definition == null || !definition.hasBehavior()) return;

        if (event.getWhoClicked() instanceof Player player) {
            ItemContext context = new ItemContext(player, item, definition, event);
            definition.getBehavior().onInventoryClick(context);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        ItemDefinition definition = getDefinition(item);
        if (definition == null || !definition.hasBehavior()) return;

        ItemContext context = new ItemContext(event.getPlayer(), item, definition, event);
        definition.getBehavior().onDrop(context);
    }

    /**
     * Xác định ItemDefinition từ ItemStack thông qua PersistentDataContainer.
     */
    private ItemDefinition getDefinition(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(itemIdKey, PersistentDataType.STRING)) return null;

        String id = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (id == null) return null;

        return registry.get(id);
    }

    // --- Helper functions for Custom Block persistent metadata container ---

    private void saveBlockPDC(Block block, PersistentDataContainer itemPDC) {
        PersistentDataContainer chunkPDC = block.getChunk().getPersistentDataContainer();
        int rx = block.getX() & 15;
        int ry = block.getY();
        int rz = block.getZ() & 15;
        NamespacedKey blockKey = new NamespacedKey(plugin, "b_" + rx + "_" + ry + "_" + rz);

        PersistentDataContainer blockPDC = chunkPDC.getAdapterContext().newPersistentDataContainer();
        itemPDC.copyTo(blockPDC, true);
        chunkPDC.set(blockKey, PersistentDataType.TAG_CONTAINER, blockPDC);
    }

    private PersistentDataContainer getBlockPDC(Block block) {
        PersistentDataContainer chunkPDC = block.getChunk().getPersistentDataContainer();
        int rx = block.getX() & 15;
        int ry = block.getY();
        int rz = block.getZ() & 15;
        NamespacedKey blockKey = new NamespacedKey(plugin, "b_" + rx + "_" + ry + "_" + rz);

        if (chunkPDC.has(blockKey, PersistentDataType.TAG_CONTAINER)) {
            return chunkPDC.get(blockKey, PersistentDataType.TAG_CONTAINER);
        }
        return null;
    }

    private void removeBlockPDC(Block block) {
        PersistentDataContainer chunkPDC = block.getChunk().getPersistentDataContainer();
        int rx = block.getX() & 15;
        int ry = block.getY();
        int rz = block.getZ() & 15;
        NamespacedKey blockKey = new NamespacedKey(plugin, "b_" + rx + "_" + ry + "_" + rz);
        chunkPDC.remove(blockKey);
    }

    // --- Block Place Event ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(itemIdKey, PersistentDataType.STRING)) return;

        String id = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (id == null) return;

        // Save block metadata
        saveBlockPDC(event.getBlock(), pdc);

        // Apply custom block data if configured or present on the item
        ItemDefinition definition = registry.get(id);
        Block block = event.getBlockPlaced();
        boolean blockDataApplied = false;

        if (definition != null) {
            Object customBlockData = definition.getProperties().get("custom_block_data");
            if (customBlockData instanceof String blockDataStr) {
                try {
                    block.setBlockData(org.bukkit.Bukkit.createBlockData(blockDataStr), false);
                    blockDataApplied = true;
                    // Correct the client immediately to prevent visual texture flashing
                    event.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData());
                } catch (Exception e) {
                    logger.warning("Failed to apply custom block data for item " + id + ": " + e.getMessage());
                }
            }
        }

        // Fallback to BlockStateMeta block state from the item stack
        if (!blockDataApplied && meta instanceof BlockStateMeta bsm && bsm.hasBlockState()) {
            try {
                block.setBlockData(bsm.getBlockState().getBlockData(), false);
                // Correct the client immediately to prevent visual texture flashing
                event.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData());
            } catch (Exception e) {
                logger.warning("Failed to apply BlockStateMeta block data for item " + id + ": " + e.getMessage());
            }
        }
    }

    // --- Custom Block Mechanics (NoteBlock sound & tuning prevention) ---

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNoteBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != org.bukkit.Material.NOTE_BLOCK) return;

        PersistentDataContainer blockPDC = getBlockPDC(block);
        if (blockPDC != null && blockPDC.has(itemIdKey, PersistentDataType.STRING)) {
            String id = blockPDC.get(itemIdKey, PersistentDataType.STRING);
            ItemDefinition definition = registry.get(id);
            if (definition != null) {
                Object customBlockData = definition.getProperties().get("custom_block_data");
                if (customBlockData instanceof String blockDataStr) {
                    org.bukkit.block.data.BlockData customData = org.bukkit.Bukkit.createBlockData(blockDataStr);
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (block.getType() == org.bukkit.Material.NOTE_BLOCK) {
                            block.setBlockData(customData, false);
                        }
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNoteBlockPlay(NotePlayEvent event) {
        // Cancel all note play events to disable note block sounds completely
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNoteBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() != org.bukkit.Material.NOTE_BLOCK) return;

        // Instead of cancelling the physics event (which causes neighbor block placement to fail/disappear),
        // we check if this is a custom block and restore its state 0 ticks later if it has changed.
        PersistentDataContainer blockPDC = getBlockPDC(block);
        if (blockPDC != null && blockPDC.has(itemIdKey, PersistentDataType.STRING)) {
            String id = blockPDC.get(itemIdKey, PersistentDataType.STRING);
            ItemDefinition definition = registry.get(id);
            if (definition != null) {
                Object customBlockData = definition.getProperties().get("custom_block_data");
                if (customBlockData instanceof String blockDataStr) {
                    org.bukkit.block.data.BlockData customData = org.bukkit.Bukkit.createBlockData(blockDataStr);
                    if (!block.getBlockData().getAsString().equals(customData.getAsString())) {
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            if (block.getType() == org.bukkit.Material.NOTE_BLOCK) {
                                block.setBlockData(customData, false);
                            }
                        });
                    }
                }
            }
        }
    }

    // --- Block Break Event ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        PersistentDataContainer blockPDC = getBlockPDC(block);
        if (blockPDC == null) return;

        String id = blockPDC.get(itemIdKey, PersistentDataType.STRING);
        if (id == null) return;

        // Clean up block metadata
        removeBlockPDC(block);

        // Cancel default drops
        event.setDropItems(false);

        // Creative mode players should not drop items
        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        // Recreate and drop the custom item with original PDC restored
        ItemStack dropItem = vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().create(id);
        if (dropItem != null) {
            ItemMeta meta = dropItem.getItemMeta();
            if (meta != null) {
                PersistentDataContainer itemPDC = meta.getPersistentDataContainer();
                blockPDC.copyTo(itemPDC, true);
                dropItem.setItemMeta(meta);
            }
            block.getWorld().dropItemNaturally(block.getLocation(), dropItem);
        }
    }

    // --- Explosion Events ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        java.util.Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            PersistentDataContainer blockPDC = getBlockPDC(block);
            if (blockPDC == null) continue;

            String id = blockPDC.get(itemIdKey, PersistentDataType.STRING);
            if (id == null) continue;

            // Remove metadata
            removeBlockPDC(block);

            // Remove block from vanilla explosion blocks list to prevent vanilla drops
            iterator.remove();

            // Recreate and drop custom item
            ItemStack dropItem = vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().create(id);
            if (dropItem != null) {
                ItemMeta meta = dropItem.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer itemPDC = meta.getPersistentDataContainer();
                    blockPDC.copyTo(itemPDC, true);
                    dropItem.setItemMeta(meta);
                }
                block.setType(org.bukkit.Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), dropItem);
            }
        }
    }

    // --- Piston Events ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (getBlockPDC(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (getBlockPDC(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // --- Block Burn Event ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (getBlockPDC(block) != null) {
            removeBlockPDC(block);
        }
    }

    // --- Custom Item Automatic Sanitization Events ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().validateAndUpdate(item);
            }
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().validateAndUpdate(armor);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null) {
                vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().validateAndUpdate(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().validateAndUpdate(item);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareItemCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result != null) {
            vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().validateAndUpdate(result);
        }
    }
}
