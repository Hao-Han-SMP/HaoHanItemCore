package vn.haohan.itemcore.internal.event;

import vn.haohan.itemcore.api.item.*;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.internal.item.DefaultItemFactory;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Event Router: nhận Minecraft event, xác định custom item, và dispatch tới
 * ItemBehavior.
 * 
 * <p>
 * Flow:
 * 
 * <pre>
 * PlayerInteractEvent → HaoHanItemCore → Item ID → ItemDefinition → ItemBehavior.onUse()
 * </pre>
 */
public final class ItemEventRouter implements Listener {

    private final ItemRegistry registry;
    private final NamespacedKey itemIdKey;
    private final Logger logger;
    private final Plugin plugin;
    private final vn.haohan.itemcore.internal.recipe.BukkitRecipeAdapter recipeAdapter;
    private final vn.haohan.itemcore.internal.recipe.RecipeIngredientMatcher matcher;
    private final vn.haohan.itemcore.internal.recipe.CraftingRecipeResolver craftingRecipeResolver;
    private final vn.haohan.itemcore.internal.recipe.SmithingRecipeResolver smithingRecipeResolver;
    private final vn.haohan.itemcore.internal.recipe.CookingRecipeResolver cookingRecipeResolver;
    private final vn.haohan.itemcore.internal.recipe.StonecuttingRecipeResolver stonecuttingRecipeResolver;

    public ItemEventRouter(ItemRegistry registry, Plugin plugin) {
        this(registry, null, plugin);
    }

    public ItemEventRouter(ItemRegistry registry, vn.haohan.itemcore.internal.recipe.BukkitRecipeAdapter recipeAdapter,
            Plugin plugin) {
        this.registry = registry;
        this.recipeAdapter = recipeAdapter;
        this.itemIdKey = new NamespacedKey(plugin, DefaultItemFactory.ITEM_ID_KEY_NAME);
        this.logger = plugin.getLogger();
        this.plugin = plugin;

        var itemService = HaoHanItemCore.get().getItemService();
        var recipeService = HaoHanItemCore.get().getRecipeService();
        var itemFactory = HaoHanItemCore.get().getItemFactory();

        this.matcher = new vn.haohan.itemcore.internal.recipe.RecipeIngredientMatcher(itemService);
        this.craftingRecipeResolver = new vn.haohan.itemcore.internal.recipe.CraftingRecipeResolver(
                registry, itemService, recipeService, itemFactory, matcher);
        this.smithingRecipeResolver = new vn.haohan.itemcore.internal.recipe.SmithingRecipeResolver(
                matcher, recipeService, itemFactory, itemService, registry);
        this.cookingRecipeResolver = new vn.haohan.itemcore.internal.recipe.CookingRecipeResolver(
                matcher, recipeService, itemFactory, itemService);
        this.stonecuttingRecipeResolver = new vn.haohan.itemcore.internal.recipe.StonecuttingRecipeResolver(
                matcher, recipeService, registry);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null)
            return;

        ItemDefinition definition = getDefinition(item);
        if (definition == null)
            return;

        if (!definition.isUsable() && (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setCancelled(true);
            org.bukkit.entity.Player player = event.getPlayer();
            player.updateInventory();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            return;
        }

        if (!definition.hasBehavior())
            return;

        ItemContext context = new ItemContext(event.getPlayer(), item, definition, event);

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> definition.getBehavior().onUse(context);
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> definition.getBehavior().onInteract(context);
            case PHYSICAL -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        ItemDefinition definition = getDefinition(item);
        if (definition == null || !definition.hasBehavior())
            return;

        ItemContext context = new ItemContext(event.getPlayer(), item, definition, event);
        definition.getBehavior().onBreak(context);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        org.bukkit.inventory.Recipe recipe = event.getRecipe();
        org.bukkit.inventory.ItemStack[] matrix = event.getInventory().getMatrix();

        if (recipe instanceof org.bukkit.Keyed keyed
                && org.bukkit.NamespacedKey.MINECRAFT.equals(keyed.getKey().getNamespace())
                && containsCustomItem(matrix)) {
            event.setCancelled(true);
            event.getInventory().setResult(null);
            return;
        }

        if (recipe != null) {
            ItemStack result = recipe.getResult();
            ItemDefinition definition = getDefinition(result);
            if (definition != null && definition.hasBehavior() && event.getWhoClicked() instanceof Player player) {
                ItemContext context = new ItemContext(player, result, definition, event);
                definition.getBehavior().onCraft(context);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterCraft(org.bukkit.event.block.CrafterCraftEvent event) {
        if (!(event.getBlock().getState() instanceof org.bukkit.block.Crafter crafter)) {
            return;
        }
        org.bukkit.inventory.ItemStack[] contents = crafter.getInventory().getContents();
        if (event.getRecipe() instanceof org.bukkit.Keyed keyed
                && org.bukkit.NamespacedKey.MINECRAFT.equals(keyed.getKey().getNamespace())
                && containsCustomItem(contents)) {
            event.setCancelled(true);
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

        if (smithingRecipeResolver != null) {
            smithingRecipeResolver.handleResultClick(event);
        }
        if (stonecuttingRecipeResolver != null) {
            stonecuttingRecipeResolver.handleInventoryClick(event);
        }

        if (item == null)
            return;

        ItemDefinition definition = getDefinition(item);
        if (definition == null || !definition.hasBehavior())
            return;

        if (event.getWhoClicked() instanceof Player player) {
            ItemContext context = new ItemContext(player, item, definition, event);
            definition.getBehavior().onInventoryClick(context);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        ItemDefinition definition = getDefinition(item);
        if (definition == null || !definition.hasBehavior())
            return;

        ItemContext context = new ItemContext(event.getPlayer(), item, definition, event);
        definition.getBehavior().onDrop(context);
    }

    /**
     * Xác định ItemDefinition từ ItemStack thông qua PersistentDataContainer.
     */
    private ItemDefinition getDefinition(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(itemIdKey, PersistentDataType.STRING))
            return null;

        String id = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (id == null)
            return null;

        return registry.get(id);
    }

    // --- Custom Item Automatic Sanitization Events ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (recipeAdapter != null) {
            List<NamespacedKey> keys = recipeAdapter.getRegisteredKeys();
            if (!keys.isEmpty()) {
                player.discoverRecipes(keys);
            }
        }
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRecipeBookClick(com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent event) {
        if (craftingRecipeResolver != null) {
            craftingRecipeResolver.handleRecipeBookClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        org.bukkit.inventory.Recipe recipe = event.getRecipe();
        org.bukkit.inventory.ItemStack[] matrix = event.getInventory().getMatrix();

        // Chặn Carrier Leak: Nếu là recipe của vanilla Minecraft nhưng có chứa Custom
        // Item trong matrix
        if (recipe instanceof org.bukkit.Keyed keyed
                && org.bukkit.NamespacedKey.MINECRAFT.equals(keyed.getKey().getNamespace())
                && containsCustomItem(matrix)) {
            event.getInventory().setResult(null);
            return;
        }

        // Tự động xử lý Recipe Book auto-craft & Custom Recipe
        if (craftingRecipeResolver != null) {
            boolean resolved = craftingRecipeResolver.resolve(event);
            if (resolved) {
                return;
            }
        }

        ItemStack result = event.getInventory().getResult();
        if (result != null) {
            vn.haohan.itemcore.api.HaoHanItemCore.get().getItemService().validateAndUpdate(result);
        }
    }

    private boolean containsCustomItem(org.bukkit.inventory.ItemStack[] items) {
        return matcher != null ? matcher.containsCustomItem(items) : false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (smithingRecipeResolver != null) {
            smithingRecipeResolver.resolve(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceSmelt(org.bukkit.event.inventory.FurnaceSmeltEvent event) {
        if (cookingRecipeResolver != null) {
            cookingRecipeResolver.handleFurnaceSmelt(event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFurnaceStartSmelt(org.bukkit.event.inventory.FurnaceStartSmeltEvent event) {
        if (cookingRecipeResolver != null) {
            cookingRecipeResolver.handleFurnaceStartSmelt(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockCook(org.bukkit.event.block.BlockCookEvent event) {
        if (cookingRecipeResolver != null) {
            cookingRecipeResolver.handleBlockCook(event);
        }
    }
}
