package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.recipe.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Recipe Viewer GUI — hiển thị recipe trong inventory GUI.
 * Hỗ trợ:
 * - Shaped recipe với crafting grid
 * - Navigation Previous/Next
 * - Click vào ingredient để xem recipe của nó
 */
public final class RecipeViewerGUI implements Listener {

    private static final int GUI_SIZE = 54; // 6 rows
    private static final int[] CRAFTING_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30}; // 3x3 grid
    private static final int RESULT_SLOT = 24;
    private static final int ARROW_SLOT = 22;
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;
    private static final int INFO_SLOT = 49;
    private static final int BACK_SLOT = 45;

    private final Plugin plugin;
    private final ItemService itemService;
    private final RecipeService recipeService;
    private final ItemRegistry itemRegistry;

    // Track active GUIs
    private final Map<UUID, ViewerSession> activeSessions = new HashMap<>();

    public RecipeViewerGUI(Plugin plugin, ItemService itemService, RecipeService recipeService,
                           ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.recipeService = recipeService;
        this.itemRegistry = itemRegistry;
    }

    /**
     * Mở Recipe Viewer cho một item ID.
     */
    public void open(Player player, String itemId) {
        List<RecipeDefinition> recipes = recipeService.findByResult(itemId);

        if (recipes.isEmpty()) {
            player.sendMessage(Component.text("Không tìm thấy recipe cho: " + itemId, NamedTextColor.RED));
            return;
        }

        ViewerSession session = new ViewerSession(itemId, recipes, 0);
        activeSessions.put(player.getUniqueId(), session);

        Inventory gui = createGUI(session);
        player.openInventory(gui);
    }

    public boolean hasRecipes(String itemId) {
        return !recipeService.findByResult(itemId).isEmpty();
    }

    /**
     * Mở Recipe Viewer cho một RecipeDefinition cụ thể.
     */
    public void open(Player player, RecipeDefinition recipe) {
        List<RecipeDefinition> recipes = List.of(recipe);
        ViewerSession session = new ViewerSession(recipe.getResult().item(), recipes, 0);
        activeSessions.put(player.getUniqueId(), session);

        Inventory gui = createGUI(session);
        player.openInventory(gui);
    }

    private Inventory createGUI(ViewerSession session) {
        RecipeDefinition recipe = session.currentRecipe();
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                Component.text("Recipe: " + recipe.getResult().item()));

        // Fill border
        ItemStack border = createBorderItem();
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, border);
        }

        populateRecipeGrid(gui, recipe);
        populateNavigation(gui, session);

        return gui;
    }

    private void populateRecipeGrid(Inventory gui, RecipeDefinition recipe) {
        // Clear crafting area and result area
        for (int slot : CRAFTING_SLOTS) {
            gui.setItem(slot, null);
        }
        gui.setItem(RESULT_SLOT, null);
        gui.setItem(ARROW_SLOT, null);

        // Place recipe content
        if (recipe instanceof ShapedRecipeDefinition shaped) {
            placeShapedRecipe(gui, shaped);
        } else {
            placeNonShapedRecipe(gui, recipe);
        }

        // Arrow
        gui.setItem(ARROW_SLOT, createArrowItem());

        // Result
        gui.setItem(RESULT_SLOT, createDisplayItem(recipe.getResult().item(), recipe.getResult().amount()));
    }

    private void populateNavigation(Inventory gui, ViewerSession session) {
        // Navigation
        if (session.recipes.size() > 1) {
            gui.setItem(PREV_SLOT, createNavItem("§a◀ Previous", session.index > 0));
            gui.setItem(NEXT_SLOT, createNavItem("§a▶ Next", session.index < session.recipes.size() - 1));
            gui.setItem(INFO_SLOT, createInfoItem(session));
        }

        // Back button
        gui.setItem(BACK_SLOT, createBackItem());
    }

    private void placeShapedRecipe(Inventory gui, ShapedRecipeDefinition recipe) {
        List<String> pattern = recipe.getPattern();
        Map<Character, Ingredient> ingredientMap = recipe.getIngredientMap();

        for (int row = 0; row < pattern.size(); row++) {
            String line = pattern.get(row);
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                int slotIndex = row * 3 + col;
                if (slotIndex < CRAFTING_SLOTS.length) {
                    if (c != ' ' && ingredientMap.containsKey(c)) {
                        Ingredient ingredient = ingredientMap.get(c);
                        gui.setItem(CRAFTING_SLOTS[slotIndex], createIngredientItem(ingredient));
                    } else {
                        gui.setItem(CRAFTING_SLOTS[slotIndex], null);
                    }
                }
            }
        }
    }

    private void placeNonShapedRecipe(Inventory gui, RecipeDefinition recipe) {
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size() && i < CRAFTING_SLOTS.length; i++) {
            gui.setItem(CRAFTING_SLOTS[i], createIngredientItem(ingredients.get(i)));
        }
    }

    private ItemStack createIngredientItem(Ingredient ingredient) {
        if (ingredient instanceof Ingredient.ItemIngredient item) {
            return createDisplayItem(item.id(), item.amount());
        }
        if (ingredient instanceof Ingredient.MaterialIngredient mat) {
            ItemStack stack = new ItemStack(mat.material(), mat.amount());
            return stack;
        }
        if (ingredient instanceof Ingredient.TagIngredient tag) {
            ItemStack stack = new ItemStack(Material.NAME_TAG, tag.amount());
            ItemMeta meta = stack.getItemMeta();
            meta.displayName(Component.text("Tag: " + tag.tag(), NamedTextColor.YELLOW));
            stack.setItemMeta(meta);
            return stack;
        }
        return new ItemStack(Material.BARRIER);
    }

    private ItemStack createDisplayItem(String itemId, int amount) {
        // Custom item
        if (itemRegistry.exists(itemId)) {
            ItemStack display = itemService.create(itemId, Math.max(1, amount));
            ItemDefinition definition = itemRegistry.get(itemId);
            if (definition != null && definition.getItemModel() == null
                    && definition.getCustomModelData() != null) {
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    meta.setItemModel(null);
                    display.setItemMeta(meta);
                }
            }
            return display;
        }

        // Vanilla item
        if (itemId.startsWith("minecraft:")) {
            String materialName = itemId.substring("minecraft:".length()).toUpperCase();
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                return new ItemStack(material, Math.max(1, amount));
            }
        }

        // Unknown
        ItemStack unknown = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = unknown.getItemMeta();
        meta.displayName(Component.text("Unknown: " + itemId, NamedTextColor.RED));
        unknown.setItemMeta(meta);
        return unknown;
    }

    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" "));
        border.setItemMeta(meta);
        return border;
    }

    private ItemStack createArrowItem() {
        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = arrow.getItemMeta();
        meta.displayName(Component.text("→", NamedTextColor.WHITE, TextDecoration.BOLD));
        arrow.setItemMeta(meta);
        return arrow;
    }

    private ItemStack createNavItem(String name, boolean active) {
        ItemStack item = new ItemStack(active ? Material.LIME_DYE : Material.GRAY_DYE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(ViewerSession session) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Recipe " + (session.index + 1) + " / " + session.recipes.size(),
                NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Type: " + session.currentRecipe().getType(), NamedTextColor.GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c✖ Close"));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ViewerSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (handleNavigationClick(slot, player, session)) {
            return;
        }

        handleIngredientClick(event.getCurrentItem(), player, session);
    }

    private boolean handleNavigationClick(int slot, Player player, ViewerSession session) {
        if (slot == PREV_SLOT && session.index > 0) {
            session.index--;
            player.openInventory(createGUI(session));
            return true;
        }
        if (slot == NEXT_SLOT && session.index < session.recipes.size() - 1) {
            session.index++;
            player.openInventory(createGUI(session));
            return true;
        }
        if (slot == BACK_SLOT) {
            player.closeInventory();
            return true;
        }
        return false;
    }

    private void handleIngredientClick(ItemStack clicked, Player player, ViewerSession session) {
        if (clicked != null && clicked.getType() != Material.AIR) {
            String clickedId = itemService.getId(clicked);
            if (clickedId != null && !clickedId.equals(session.itemId)) {
                List<RecipeDefinition> recipes = recipeService.findByResult(clickedId);
                if (!recipes.isEmpty()) {
                    open(player, clickedId);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            activeSessions.remove(player.getUniqueId());
        }
    }

    /**
     * Session tracking cho mỗi player.
     */
    private static final class ViewerSession {
        final String itemId;
        final List<RecipeDefinition> recipes;
        int index;

        ViewerSession(String itemId, List<RecipeDefinition> recipes, int index) {
            this.itemId = itemId;
            this.recipes = recipes;
            this.index = index;
        }

        RecipeDefinition currentRecipe() {
            return recipes.get(index);
        }
    }
}
