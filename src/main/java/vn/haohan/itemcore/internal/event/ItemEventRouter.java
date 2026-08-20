package vn.haohan.itemcore.internal.event;

import vn.haohan.itemcore.api.item.*;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeType;
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
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.SmithingInventory;
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
    private final java.util.Map<String, org.bukkit.block.data.BlockData> parsedBlockDataCache = new java.util.concurrent.ConcurrentHashMap<>();

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
        if (definition == null) return;

        if (!definition.isUsable() && (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setCancelled(true);
            org.bukkit.entity.Player player = event.getPlayer();
            player.updateInventory();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            return;
        }

        if (!definition.hasBehavior()) return;

        ItemContext context = new ItemContext(event.getPlayer(), item, definition, event);

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> definition.getBehavior().onUse(context);
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> definition.getBehavior().onInteract(context);
            case PHYSICAL -> {}
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

        boolean blockDataApplied = applyCustomBlockData(block, definition, event.getPlayer());

        // Fallback to BlockStateMeta block state from the item stack
        if (!blockDataApplied) {
            applyBlockStateMeta(block, meta, event.getPlayer(), id);
        }
    }

    // --- Custom Block Mechanics (NoteBlock sound & tuning prevention) ---

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNoteBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        // If the player is sneaking, they are attempting to place a block or use the item on hand.
        // In vanilla Minecraft, sneaking players do not tune/interact with NoteBlocks,
        // so the block state will not change. We must return early to avoid interfering
        // with the block placement logic (which would cause placed blocks to disappear).
        if (event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != org.bukkit.Material.NOTE_BLOCK) return;

        org.bukkit.block.data.BlockData customData = getCustomBlockData(block);
        if (customData != null) {
            // Deny block interaction immediately to prevent the block state from changing (tuning the note block)
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            // Also deny using the item in hand to prevent block placement or item use when not sneaking,
            // matching vanilla interactive block behavior (like chests/furnaces).
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            // Correct client immediately to avoid texture flicking
            event.getPlayer().sendBlockChange(block.getLocation(), customData);

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (block.getType() == org.bukkit.Material.NOTE_BLOCK) {
                    block.setBlockData(customData, false);
                }
            });
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

        org.bukkit.block.data.BlockData customData = getCustomBlockData(block);
        if (customData != null) {
            // Cancel the physics event to prevent any state changes (like instrument updates from blocks underneath/adjacent)
            event.setCancelled(true);

            if (!block.getBlockData().getAsString().equals(customData.getAsString())) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (block.getType() == org.bukkit.Material.NOTE_BLOCK) {
                        block.setBlockData(customData, false);
                    }
                });
            }
        }
    }

    // --- Block Break Event ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String id = getCustomBlockId(block);
        if (id == null) return;

        // Clean up block metadata
        PersistentDataContainer blockPDC = getBlockPDC(block);
        removeBlockPDC(block);

        // Cancel default drops
        event.setDropItems(false);

        // Creative mode players should not drop items
        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        // Recreate and drop the custom item
        dropCustomBlockItem(block, id, blockPDC);
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
            if (processExplodedBlock(block)) {
                iterator.remove();
            }
        }
    }

    // --- Piston Events ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (getCustomBlockId(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (getCustomBlockId(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // --- Block Burn Event ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (getCustomBlockId(block) != null) {
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack template = inventory.getItem(0); // Template slot
        ItemStack base = inventory.getItem(1);     // Base item slot
        ItemStack addition = inventory.getItem(2); // Addition ingredient slot

        RecipeDefinition matchedRecipe = findSmithingRecipe(template, base, addition);

        if (matchedRecipe != null) {
            ItemStack resultStack = HaoHanItemCore.get().getItemFactory().create(
                    matchedRecipe.getResult().item(),
                    matchedRecipe.getResult().amount()
            );
            event.setResult(resultStack);
        } else {
            ItemStack currentResult = event.getResult();
            if (currentResult != null && HaoHanItemCore.get().getItemService().isCustomItem(currentResult)) {
                event.setResult(null);
            }
        }
    }

    private boolean matchIngredient(Ingredient ingredient, ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return false;
        }
        if (ingredient instanceof Ingredient.ItemIngredient itemIng) {
            if (itemIng.id().startsWith("minecraft:")) {
                String matName = itemIng.id().substring("minecraft:".length()).toUpperCase();
                return item.getType().name().equals(matName);
            } else {
                return HaoHanItemCore.get().getItemService().isItem(item, itemIng.id());
            }
        } else if (ingredient instanceof Ingredient.MaterialIngredient matIng) {
            return item.getType() == matIng.material();
        }
        return false;
    }

    private boolean applyCustomBlockData(Block block, ItemDefinition definition, Player player) {
        if (definition != null) {
            Object customBlockData = definition.getProperties().get("custom_block_data");
            if (customBlockData instanceof String blockDataStr) {
                try {
                    block.setBlockData(org.bukkit.Bukkit.createBlockData(blockDataStr), false);
                    // Correct the client immediately to prevent visual texture flashing
                    player.sendBlockChange(block.getLocation(), block.getBlockData());
                    return true;
                } catch (Exception e) {
                    logger.warning("Failed to apply custom block data for item " + definition.getId() + ": " + e.getMessage());
                }
            }
        }
        return false;
    }

    private boolean applyBlockStateMeta(Block block, ItemMeta meta, Player player, String itemId) {
        if (meta instanceof BlockStateMeta bsm && bsm.hasBlockState()) {
            try {
                block.setBlockData(bsm.getBlockState().getBlockData(), false);
                // Correct the client immediately to prevent visual texture flashing
                player.sendBlockChange(block.getLocation(), block.getBlockData());
                return true;
            } catch (Exception e) {
                logger.warning("Failed to apply BlockStateMeta block data for item " + itemId + ": " + e.getMessage());
            }
        }
        return false;
    }

    private org.bukkit.block.data.BlockData getOrParseBlockData(String blockDataStr) {
        org.bukkit.block.data.BlockData cached = parsedBlockDataCache.get(blockDataStr);
        if (cached != null) return cached;

        try {
            org.bukkit.block.data.BlockData parsed = org.bukkit.Bukkit.createBlockData(blockDataStr);
            if (parsed != null) {
                parsedBlockDataCache.put(blockDataStr, parsed);
            }
            return parsed;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCustomBlockId(Block block) {
        PersistentDataContainer blockPDC = getBlockPDC(block);
        if (blockPDC != null) {
            return blockPDC.get(itemIdKey, PersistentDataType.STRING);
        }

        // Fallback for worldgen-generated custom blocks
        org.bukkit.block.data.BlockData currentData = block.getBlockData();
        String currentDataStr = currentData.getAsString();

        for (ItemDefinition def : registry.all()) {
            Object customBlockDataObj = def.getProperties().get("custom_block_data");
            if (customBlockDataObj instanceof String blockDataStr) {
                org.bukkit.block.data.BlockData targetData = getOrParseBlockData(blockDataStr);
                if (targetData != null && currentDataStr.equals(targetData.getAsString())) {
                    // Self-healing: Dynamically save the block PDC to chunk PDC so future lookups are immediate
                    PersistentDataContainer dummyPDC = block.getChunk().getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
                    dummyPDC.set(itemIdKey, PersistentDataType.STRING, def.getId());
                    saveBlockPDC(block, dummyPDC);
                    return def.getId();
                }
            }
        }
        return null;
    }

    private org.bukkit.block.data.BlockData getCustomBlockData(Block block) {
        String id = getCustomBlockId(block);
        if (id == null) return null;

        ItemDefinition definition = registry.get(id);
        if (definition != null) {
            Object customBlockData = definition.getProperties().get("custom_block_data");
            if (customBlockData instanceof String blockDataStr) {
                return getOrParseBlockData(blockDataStr);
            }
        }
        return null;
    }

    private void dropCustomBlockItem(Block block, String itemId, PersistentDataContainer blockPDC) {
        ItemDefinition definition = registry.get(itemId);
        if (definition == null) {
            return;
        }

        Object customDrop = definition.getProperties().get("custom_block_drop");
        if (!(customDrop instanceof String dropId) || dropId.isBlank()) {
            return;
        }

        ItemStack dropItem = HaoHanItemCore.get().getItemService().create(dropId);
        if (dropItem != null) {
            // Only copy block state PDC if we are dropping the block itself
            if (dropId.equals(itemId) && blockPDC != null) {
                ItemMeta meta = dropItem.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer itemPDC = meta.getPersistentDataContainer();
                    blockPDC.copyTo(itemPDC, true);
                    dropItem.setItemMeta(meta);
                }
            }
            block.getWorld().dropItemNaturally(block.getLocation(), dropItem);
        }
    }

    private boolean processExplodedBlock(Block block) {
        String id = getCustomBlockId(block);
        if (id == null) return false;

        PersistentDataContainer blockPDC = getBlockPDC(block);
        // Remove metadata
        removeBlockPDC(block);
        block.setType(org.bukkit.Material.AIR);

        // Recreate and drop custom item if custom_block_drop is configured
        dropCustomBlockItem(block, id, blockPDC);
        return true;
    }

    private RecipeDefinition findSmithingRecipe(ItemStack template, ItemStack base, ItemStack addition) {
        var recipeService = HaoHanItemCore.get().getRecipeService();
        if (recipeService == null) return null;

        for (RecipeDefinition recipe : recipeService.all()) {
            if (recipe.getType() != RecipeType.SMITHING) {
                continue;
            }
            List<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.size() < 3) continue;

            if (matchIngredient(ingredients.get(0), template) &&
                matchIngredient(ingredients.get(1), base) &&
                matchIngredient(ingredients.get(2), addition)) {
                return recipe;
            }
        }
        return null;
    }
}
