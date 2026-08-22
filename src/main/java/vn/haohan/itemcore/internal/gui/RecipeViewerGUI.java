package vn.haohan.itemcore.internal.gui;

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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Recipe Viewer GUI — hiển thị chi tiết công thức chế tạo theo đúng layout
 * container của từng RecipeForm:
 * - Smithing Table (Blacksmith form: Template + Base + Addition ➔ Result)
 * - Furnace / Blasting / Smoker (Smelting form: Input ➔ Flame (time/xp) ➔
 * Result ➔ Fuel)
 * - Stonecutter (Stonecutter form: Input ➔ Saw ➔ Result)
 * - Campfire (Campfire form: Input ➔ Fire ➔ Result)
 * - Crafting Table (3x3 Shaped & Shapeless grid ➔ Arrow ➔ Result)
 * - Custom Machine (Machine component layout)
 */
public final class RecipeViewerGUI implements Listener {

    private static final int GUI_SIZE = 54; // 6 rows

    // Header & Navigation Slots
    private static final int HEADER_STATION_SLOT = 4;
    private static final int BACK_SLOT = 45;
    private static final int PREV_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    // Crafting 3x3 Slots
    private static final int[] CRAFTING_SLOTS = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
    private static final int CRAFTING_ARROW_SLOT = 23;
    private static final int CRAFTING_RESULT_SLOT = 25;

    // Smithing Slots
    private static final int SMITHING_TEMPLATE_SLOT = 19;
    private static final int SMITHING_PLUS1_SLOT = 20;
    private static final int SMITHING_BASE_SLOT = 21;
    private static final int SMITHING_PLUS2_SLOT = 22;
    private static final int SMITHING_ADDITION_SLOT = 23;
    private static final int SMITHING_ARROW_SLOT = 24;
    private static final int SMITHING_RESULT_SLOT = 25;

    // Furnace / Smelting Slots
    private static final int FURNACE_INPUT_SLOT = 11;
    private static final int FURNACE_FLAME_SLOT = 20;
    private static final int FURNACE_ARROW_SLOT = 21;
    private static final int FURNACE_RESULT_SLOT = 24;
    private static final int FURNACE_FUEL_SLOT = 29;

    // Stonecutter Slots
    private static final int STONECUTTER_INPUT_SLOT = 19;
    private static final int STONECUTTER_SAW_SLOT = 21;
    private static final int STONECUTTER_ARROW_SLOT = 22;
    private static final int STONECUTTER_RESULT_SLOT = 25;

    // Campfire Slots
    private static final int CAMPFIRE_INPUT_SLOT = 11;
    private static final int CAMPFIRE_FIRE_SLOT = 20;
    private static final int CAMPFIRE_ARROW_SLOT = 22;
    private static final int CAMPFIRE_RESULT_SLOT = 24;

    private final ItemService itemService;
    private final RecipeService recipeService;
    private final ItemRegistry itemRegistry;
    private ItemBrowserGUI itemBrowser;

    public RecipeViewerGUI(ItemService itemService, RecipeService recipeService,
            ItemRegistry itemRegistry) {
        this.itemService = itemService;
        this.recipeService = recipeService;
        this.itemRegistry = itemRegistry;
    }

    public RecipeViewerGUI(Plugin plugin, ItemService itemService, RecipeService recipeService,
            ItemRegistry itemRegistry) {
        this(itemService, recipeService, itemRegistry);
    }

    public void setItemBrowser(ItemBrowserGUI itemBrowser) {
        this.itemBrowser = itemBrowser;
    }

    public void open(Player player, String itemId) {
        open(player, itemId, 0, ItemBrowserFilter.empty());
    }

    public void open(Player player, String itemId, int returnPage, String returnSearchQuery) {
        open(player, itemId, returnPage, new ItemBrowserFilter(returnSearchQuery, null, null));
    }

    public void open(Player player, String itemId, int returnPage, ItemBrowserFilter returnFilter) {
        List<RecipeDefinition> recipes = recipeService.findByResult(itemId);

        if (recipes.isEmpty()) {
            player.sendMessage(Component.text("Không tìm thấy công thức chế tạo cho: " + itemId, NamedTextColor.RED));
            return;
        }

        open(player, itemId, recipes, 0, returnPage, returnFilter);
    }

    public boolean hasRecipes(String itemId) {
        return !recipeService.findByResult(itemId).isEmpty();
    }

    public void open(Player player, RecipeDefinition recipe) {
        open(player, recipe, 0, ItemBrowserFilter.empty());
    }

    public void open(Player player, RecipeDefinition recipe, int returnPage, String returnSearchQuery) {
        open(player, recipe, returnPage, new ItemBrowserFilter(returnSearchQuery, null, null));
    }

    public void open(Player player, RecipeDefinition recipe, int returnPage, ItemBrowserFilter returnFilter) {
        List<RecipeDefinition> recipes = List.of(recipe);
        open(player, recipe.getResult().item(), recipes, 0, returnPage, returnFilter);
    }

    public void open(Player player, String itemId, List<RecipeDefinition> recipes, int index) {
        open(player, itemId, recipes, index, 0, ItemBrowserFilter.empty());
    }

    public void open(Player player, String itemId, List<RecipeDefinition> recipes, int index, int returnPage,
            ItemBrowserFilter returnFilter) {
        if (recipes.isEmpty())
            return;
        index = Math.max(0, Math.min(index, recipes.size() - 1));

        RecipeViewerHolder holder = new RecipeViewerHolder(itemId, recipes, index, returnPage, returnFilter);
        RecipeDefinition recipe = recipes.get(index);

        Component title = createRecipeTitle(recipe, index, recipes.size());
        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);
        holder.setInventory(gui);

        // 1. Fill background with sleek border panes
        ItemStack border = createBorderItem();
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, border);
        }

        // 2. Render station-specific container layout
        renderRecipeContainer(gui, recipe);

        // 3. Render navigation controls
        populateNavigation(gui, holder);

        player.openInventory(gui);
    }

    private Component createRecipeTitle(RecipeDefinition recipe, int index, int total) {
        String stationName = switch (recipe.getType()) {
            case SMITHING -> "Smithing Table";
            case SMELTING -> "Furnace";
            case BLASTING -> "Blast Furnace";
            case SMOKING -> "Smoker";
            case CAMPFIRE -> "Campfire";
            case STONECUTTING -> "Stonecutter";
            case MACHINE -> "Machine";
            case SHAPED, SHAPELESS -> "Crafting";
        };
        return Component.text(stationName + ": " + recipe.getResult().item() + " (" + (index + 1) + "/" + total + ")",
                NamedTextColor.DARK_PURPLE);
    }

    private void renderRecipeContainer(Inventory gui, RecipeDefinition recipe) {
        switch (recipe.getType()) {
            case SMITHING -> renderSmithingContainer(gui, recipe);
            case SMELTING, BLASTING, SMOKING -> renderCookingContainer(gui, recipe);
            case CAMPFIRE -> renderCampfireContainer(gui, recipe);
            case STONECUTTING -> renderStonecutterContainer(gui, recipe);
            case SHAPED -> renderShapedCraftingContainer(gui, (ShapedRecipeDefinition) recipe);
            case SHAPELESS, MACHINE -> renderShapelessOrMachineContainer(gui, recipe);
        }
    }

    // --- Container Form 1: Smithing Table (Blacksmith) ---
    private void renderSmithingContainer(Inventory gui, RecipeDefinition recipe) {
        // Station Header
        gui.setItem(HEADER_STATION_SLOT, createStationHeader(
                Material.SMITHING_TABLE,
                "§6Smithing Table (Bàn Thợ Rèn)",
                "§7Nâng cấp hoặc rèn trang bị/công cụ",
                "§eLoại công thức: §fSmithing Transform"));

        // Clear active slots
        gui.setItem(SMITHING_TEMPLATE_SLOT, null);
        gui.setItem(SMITHING_PLUS1_SLOT, null);
        gui.setItem(SMITHING_BASE_SLOT, null);
        gui.setItem(SMITHING_PLUS2_SLOT, null);
        gui.setItem(SMITHING_ADDITION_SLOT, null);
        gui.setItem(SMITHING_ARROW_SLOT, null);
        gui.setItem(SMITHING_RESULT_SLOT, null);

        List<Ingredient> ingredients = recipe.getIngredients();

        // 1. Template slot (ô 19)
        if (!ingredients.isEmpty()) {
            gui.setItem(SMITHING_TEMPLATE_SLOT,
                    createAnnotatedIngredientItem(ingredients.get(0), "§e[Khuôn Đúc / Template]"));
        } else {
            gui.setItem(SMITHING_TEMPLATE_SLOT, createSlotGuide(Material.STRUCTURE_VOID, "§7Khuôn đúc rèn (Template)"));
        }

        // Plus 1 (ô 20)
        gui.setItem(SMITHING_PLUS1_SLOT, createSymbolItem("§7+"));

        // 2. Base item slot (ô 21)
        if (ingredients.size() > 1) {
            gui.setItem(SMITHING_BASE_SLOT,
                    createAnnotatedIngredientItem(ingredients.get(1), "§e[Trang Bị Gốc / Base Item]"));
        } else {
            gui.setItem(SMITHING_BASE_SLOT, createSlotGuide(Material.IRON_SWORD, "§7Trang bị gốc (Base Item)"));
        }

        // Plus 2 (ô 22)
        gui.setItem(SMITHING_PLUS2_SLOT, createSymbolItem("§7+"));

        // 3. Addition ingot slot (ô 23)
        if (ingredients.size() > 2) {
            gui.setItem(SMITHING_ADDITION_SLOT,
                    createAnnotatedIngredientItem(ingredients.get(2), "§e[Vật Liệu Rèn / Addition]"));
        } else {
            gui.setItem(SMITHING_ADDITION_SLOT,
                    createSlotGuide(Material.NETHERITE_INGOT, "§7Nguyên liệu rèn (Addition)"));
        }

        // Arrow / Hammer icon (ô 24)
        gui.setItem(SMITHING_ARROW_SLOT, createHammerItem("§6Rèn nâng cấp (Transform)"));

        // Result (ô 25)
        gui.setItem(SMITHING_RESULT_SLOT, createDisplayItem(recipe.getResult().item(), recipe.getResult().amount()));
    }

    // --- Container Form 2: Furnace / Blasting / Smoker ---
    private void renderCookingContainer(Inventory gui, RecipeDefinition recipe) {
        Material stationMat = switch (recipe.getType()) {
            case BLASTING -> Material.BLAST_FURNACE;
            case SMOKING -> Material.SMOKER;
            default -> Material.FURNACE;
        };
        String stationName = switch (recipe.getType()) {
            case BLASTING -> "§6Blast Furnace (Lò Cao)";
            case SMOKING -> "§6Smoker (Lò Hun Khói)";
            default -> "§6Furnace (Lò Nung)";
        };

        gui.setItem(HEADER_STATION_SLOT, createStationHeader(
                stationMat,
                stationName,
                "§7Nung luyện quặng, kim loại hoặc thức ăn",
                "§eThời gian nung: §f" + (recipe.getCookingTime() / 20.0) + "s (" + recipe.getCookingTime() + " ticks)",
                "§aKinh nghiệm: §f+" + recipe.getExperience() + " XP"));

        // Clear active slots
        gui.setItem(FURNACE_INPUT_SLOT, null);
        gui.setItem(FURNACE_FLAME_SLOT, null);
        gui.setItem(FURNACE_ARROW_SLOT, null);
        gui.setItem(FURNACE_RESULT_SLOT, null);
        gui.setItem(FURNACE_FUEL_SLOT, null);

        // Input ingredient (ô 11)
        List<Ingredient> ingredients = recipe.getIngredients();
        if (!ingredients.isEmpty()) {
            gui.setItem(FURNACE_INPUT_SLOT,
                    createAnnotatedIngredientItem(ingredients.getFirst(), "§e[Nguyên Liệu Nung / Input]"));
        }

        // Flame indicator (ô 20)
        gui.setItem(FURNACE_FLAME_SLOT, createFlameItem(recipe.getCookingTime(), recipe.getExperience()));

        // Arrow (ô 21)
        gui.setItem(FURNACE_ARROW_SLOT, createArrowItem());

        // Fuel suggestion (ô 29)
        gui.setItem(FURNACE_FUEL_SLOT, createFuelGuideItem());

        // Result (ô 24)
        gui.setItem(FURNACE_RESULT_SLOT, createDisplayItem(recipe.getResult().item(), recipe.getResult().amount()));
    }

    // --- Container Form 3: Campfire ---
    private void renderCampfireContainer(Inventory gui, RecipeDefinition recipe) {
        gui.setItem(HEADER_STATION_SLOT, createStationHeader(
                Material.CAMPFIRE,
                "§6Campfire (Lửa Trại)",
                "§7Nướng thực phẩm chậm không cần nhiên liệu",
                "§eThời gian nướng: §f" + (recipe.getCookingTime() / 20.0) + "s",
                "§aKinh nghiệm: §f+" + recipe.getExperience() + " XP"));

        gui.setItem(CAMPFIRE_INPUT_SLOT, null);
        gui.setItem(CAMPFIRE_FIRE_SLOT, null);
        gui.setItem(CAMPFIRE_ARROW_SLOT, null);
        gui.setItem(CAMPFIRE_RESULT_SLOT, null);

        if (!recipe.getIngredients().isEmpty()) {
            gui.setItem(CAMPFIRE_INPUT_SLOT,
                    createAnnotatedIngredientItem(recipe.getIngredients().getFirst(), "§e[Thực Phẩm Nướng]"));
        }

        gui.setItem(CAMPFIRE_FIRE_SLOT, createCampfireFlameItem(recipe.getCookingTime(), recipe.getExperience()));
        gui.setItem(CAMPFIRE_ARROW_SLOT, createArrowItem());
        gui.setItem(CAMPFIRE_RESULT_SLOT, createDisplayItem(recipe.getResult().item(), recipe.getResult().amount()));
    }

    // --- Container Form 4: Stonecutter ---
    private void renderStonecutterContainer(Inventory gui, RecipeDefinition recipe) {
        gui.setItem(HEADER_STATION_SLOT, createStationHeader(
                Material.STONECUTTER,
                "§6Stonecutter (Máy Cắt Đá)",
                "§7Cắt khối đá trực tiếp 1-1 không hao hụt",
                "§eLoại công thức: §fStonecutting"));

        gui.setItem(STONECUTTER_INPUT_SLOT, null);
        gui.setItem(STONECUTTER_SAW_SLOT, null);
        gui.setItem(STONECUTTER_ARROW_SLOT, null);
        gui.setItem(STONECUTTER_RESULT_SLOT, null);

        if (!recipe.getIngredients().isEmpty()) {
            gui.setItem(STONECUTTER_INPUT_SLOT,
                    createAnnotatedIngredientItem(recipe.getIngredients().getFirst(), "§e[Khối Đá Đầu Vào / Input]"));
        }

        gui.setItem(STONECUTTER_SAW_SLOT, createSawBladeItem());
        gui.setItem(STONECUTTER_ARROW_SLOT, createArrowItem());
        gui.setItem(STONECUTTER_RESULT_SLOT, createDisplayItem(recipe.getResult().item(), recipe.getResult().amount()));
    }

    // --- Container Form 5: Shaped Crafting Table (3x3 Grid) ---
    private void renderShapedCraftingContainer(Inventory gui, ShapedRecipeDefinition shaped) {
        gui.setItem(HEADER_STATION_SLOT, createStationHeader(
                Material.CRAFTING_TABLE,
                "§6Crafting Table (Bàn Chế Tạo)",
                "§7Công thức có hình dạng cố định (Shaped Recipe)",
                "§eLưới chế tạo 3x3 chuẩn"));

        for (int slot : CRAFTING_SLOTS) {
            gui.setItem(slot, null);
        }
        gui.setItem(CRAFTING_ARROW_SLOT, null);
        gui.setItem(CRAFTING_RESULT_SLOT, null);

        List<String> pattern = shaped.getPattern();
        Map<Character, Ingredient> ingredientMap = shaped.getIngredientMap();

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

        gui.setItem(CRAFTING_ARROW_SLOT, createArrowItem());
        gui.setItem(CRAFTING_RESULT_SLOT, createDisplayItem(shaped.getResult().item(), shaped.getResult().amount()));
    }

    // --- Container Form 6: Shapeless & Custom Machine ---
    private void renderShapelessOrMachineContainer(Inventory gui, RecipeDefinition recipe) {
        boolean isMachine = recipe.getType() == RecipeType.MACHINE;
        Material stationMat = isMachine ? Material.CRAFTER : Material.CRAFTING_TABLE;
        String title = isMachine ? "§6Custom Machine (Trạm Chế Tạo)" : "§6Crafting Table (Shapeless)";
        String desc = isMachine ? "§7Chế tạo thông qua máy móc công nghệ"
                : "§7Công thức tự do không quan tâm vị trí đặt";

        gui.setItem(HEADER_STATION_SLOT, createStationHeader(stationMat, title, desc));

        for (int slot : CRAFTING_SLOTS) {
            gui.setItem(slot, null);
        }
        gui.setItem(CRAFTING_ARROW_SLOT, null);
        gui.setItem(CRAFTING_RESULT_SLOT, null);

        List<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size() && i < CRAFTING_SLOTS.length; i++) {
            gui.setItem(CRAFTING_SLOTS[i], createIngredientItem(ingredients.get(i)));
        }

        gui.setItem(CRAFTING_ARROW_SLOT, createArrowItem());
        gui.setItem(CRAFTING_RESULT_SLOT, createDisplayItem(recipe.getResult().item(), recipe.getResult().amount()));
    }

    // --- UI Component Helpers ---

    private void populateNavigation(Inventory gui, RecipeViewerHolder holder) {
        if (holder.getRecipes().size() > 1) {
            gui.setItem(PREV_SLOT, createNavItem("§a◀ Previous Recipe", holder.getIndex() > 0));
            gui.setItem(NEXT_SLOT,
                    createNavItem("§a▶ Next Recipe", holder.getIndex() < holder.getRecipes().size() - 1));
            gui.setItem(INFO_SLOT, createInfoItem(holder));
        }

        // Back button
        gui.setItem(BACK_SLOT, createBackItem());
    }

    private ItemStack createStationHeader(Material mat, String title, String... loreLines) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(title).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAnnotatedIngredientItem(Ingredient ingredient, String annotation) {
        ItemStack item = createIngredientItem(ingredient);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> currentLore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            currentLore.add(0,
                    Component.text(annotation, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(currentLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSlotGuide(Material iconMat, String label) {
        ItemStack item = new ItemStack(iconMat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSymbolItem(String symbol) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(symbol, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHammerItem(String label) {
        ItemStack item = new ItemStack(Material.ANVIL, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFlameItem(int cookingTime, float exp) {
        ItemStack item = new ItemStack(Material.FIRE_CHARGE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6Ngọn Lửa Nung").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component
                            .text("Thời gian: " + (cookingTime / 20.0) + "s (" + cookingTime + " ticks)",
                                    NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Kinh nghiệm: +" + exp + " XP", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCampfireFlameItem(int cookingTime, float exp) {
        ItemStack item = new ItemStack(Material.CAMPFIRE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6Lửa Trại Nướng").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Thời gian: " + (cookingTime / 20.0) + "s", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Kinh nghiệm: +" + exp + " XP", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSawBladeItem() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(
                    Component.text("§6Lưỡi Cưa Cắt Đá", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFuelGuideItem() {
        ItemStack item = new ItemStack(Material.COAL, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(
                    Component.text("§7Nhiên Liệu Nung", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Than đá, Khối than, Than củi,", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Xô dung nham, Gỗ...", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC,
                            false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createIngredientItem(Ingredient ingredient) {
        if (ingredient instanceof Ingredient.ItemIngredient item) {
            return createDisplayItem(item.id(), item.amount());
        }
        if (ingredient instanceof Ingredient.MaterialIngredient mat) {
            return new ItemStack(mat.material(), mat.amount());
        }
        if (ingredient instanceof Ingredient.TagIngredient tag) {
            ItemStack stack = new ItemStack(Material.NAME_TAG, tag.amount());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Tag: " + tag.tag(), NamedTextColor.YELLOW));
                stack.setItemMeta(meta);
            }
            return stack;
        }
        return new ItemStack(Material.BARRIER);
    }

    private ItemStack createDisplayItem(String itemId, int amount) {
        if (itemRegistry.exists(itemId)) {
            return itemService.create(itemId, Math.max(1, amount));
        }

        if (itemId.startsWith("minecraft:")) {
            String materialName = itemId.substring("minecraft:".length()).toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                return new ItemStack(material, Math.max(1, amount));
            }
        }

        ItemStack unknown = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = unknown.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Unknown: " + itemId, NamedTextColor.RED));
            unknown.setItemMeta(meta);
        }
        return unknown;
    }

    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            meta.setHideTooltip(true);
            border.setItemMeta(meta);
        }
        return border;
    }

    private ItemStack createArrowItem() {
        return MenuIcon.create(MenuIcon.RECIPE_ARROW, Component.empty());
    }

    private ItemStack createNavItem(String name, boolean active) {
        boolean previous = name.contains("Previous");
        return MenuIcon.create(previous
                ? (active ? MenuIcon.PREVIOUS_ACTIVE : MenuIcon.PREVIOUS_DISABLED)
                : (active ? MenuIcon.NEXT_ACTIVE : MenuIcon.NEXT_DISABLED),
                Component.text(name));
    }

    private ItemStack createInfoItem(RecipeViewerHolder holder) {
        ItemStack item = MenuIcon.create(MenuIcon.INFO,
                Component.text("Recipe " + (holder.getIndex() + 1) + " / " + holder.getRecipes().size(),
                        NamedTextColor.GOLD));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(List.of(
                    Component.text("Type: " + holder.currentRecipe().getType(), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackItem() {
        return MenuIcon.create(MenuIcon.BACK, Component.text("Back", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        if (!(event.getView().getTopInventory().getHolder() instanceof RecipeViewerHolder holder))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        if (slot < 0 || slot >= topSize) {
            return;
        }

        if (handleNavigationClick(slot, player, holder)) {
            return;
        }

        handleIngredientClick(event.getCurrentItem(), player, holder);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        if (!(event.getView().getTopInventory().getHolder() instanceof RecipeViewerHolder))
            return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    private boolean handleNavigationClick(int slot, Player player, RecipeViewerHolder holder) {
        if (slot == PREV_SLOT && holder.getIndex() > 0) {
            open(player, holder.getItemId(), holder.getRecipes(), holder.getIndex() - 1, holder.getReturnPage(),
                    holder.getReturnFilter());
            return true;
        }
        if (slot == NEXT_SLOT && holder.getIndex() < holder.getRecipes().size() - 1) {
            open(player, holder.getItemId(), holder.getRecipes(), holder.getIndex() + 1, holder.getReturnPage(),
                    holder.getReturnFilter());
            return true;
        }
        if (slot == BACK_SLOT) {
            if (itemBrowser != null) {
                itemBrowser.open(player, holder.getReturnPage(), holder.getReturnFilter());
            } else {
                player.closeInventory();
            }
            return true;
        }
        return false;
    }

    private void handleIngredientClick(ItemStack clicked, Player player, RecipeViewerHolder holder) {
        if (clicked != null && clicked.getType() != Material.AIR) {
            String clickedId = itemService.getId(clicked);
            if (clickedId != null && !clickedId.equals(holder.getItemId())) {
                List<RecipeDefinition> recipes = recipeService.findByResult(clickedId);
                if (!recipes.isEmpty()) {
                    open(player, clickedId, holder.getReturnPage(), holder.getReturnFilter());
                }
            }
        }
    }
}
