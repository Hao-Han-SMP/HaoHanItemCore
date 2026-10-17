package vn.haohan.itemcore.api;

import vn.haohan.itemcore.api.item.ItemFactory;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.recipe.RecipeRegistry;
import vn.haohan.itemcore.api.recipe.RecipeService;
import vn.haohan.itemcore.api.texture.IconTextureRegistry;

/**
 * API chính của HaoHanItemCore.
 * Cung cấp access tới tất cả hệ thống con.
 * 
 * <p>Ví dụ sử dụng:
 * <pre>
 * // Tạo item
 * ItemStack crystal = HaoHanItemCore.get().getItemService().create("magic:fire_crystal", 4);
 * 
 * // Kiểm tra item
 * boolean isCrystal = HaoHanItemCore.get().getItemService().isItem(stack, "magic:fire_crystal");
 * 
 * // Tìm recipe
 * List&lt;RecipeDefinition&gt; recipes = HaoHanItemCore.get().getRecipeService().findByResult("magic:fire_crystal");
 * </pre>
 */
public final class HaoHanItemCore {

    private static HaoHanItemCore instance;

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    private final ItemService itemService;
    private final RecipeRegistry recipeRegistry;
    private final RecipeService recipeService;
    private final IconTextureRegistry iconTextureRegistry;
    private final vn.haohan.itemcore.api.block.CustomBlockService customBlockService;
    private final vn.haohan.itemcore.api.block.ToolTierRegistry toolTierRegistry;
    private final vn.haohan.itemcore.api.item.food.CustomFoodActionRegistry foodActionRegistry;

    public HaoHanItemCore(ItemRegistry itemRegistry, ItemFactory itemFactory, ItemService itemService,
                          RecipeRegistry recipeRegistry, RecipeService recipeService,
                          IconTextureRegistry iconTextureRegistry,
                          vn.haohan.itemcore.api.block.CustomBlockService customBlockService,
                          vn.haohan.itemcore.api.block.ToolTierRegistry toolTierRegistry,
                          vn.haohan.itemcore.api.item.food.CustomFoodActionRegistry foodActionRegistry) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.itemService = itemService;
        this.recipeRegistry = recipeRegistry;
        this.recipeService = recipeService;
        this.iconTextureRegistry = iconTextureRegistry;
        this.customBlockService = customBlockService;
        this.toolTierRegistry = toolTierRegistry != null ? toolTierRegistry : new vn.haohan.itemcore.api.block.ToolTierRegistry();
        this.foodActionRegistry = foodActionRegistry != null ? foodActionRegistry : new vn.haohan.itemcore.internal.item.food.DefaultCustomFoodActionRegistry();
    }

    public HaoHanItemCore(ItemRegistry itemRegistry, ItemFactory itemFactory, ItemService itemService,
                          RecipeRegistry recipeRegistry, RecipeService recipeService,
                          IconTextureRegistry iconTextureRegistry,
                          vn.haohan.itemcore.api.block.CustomBlockService customBlockService,
                          vn.haohan.itemcore.api.block.ToolTierRegistry toolTierRegistry) {
        this(itemRegistry, itemFactory, itemService, recipeRegistry, recipeService, iconTextureRegistry, customBlockService, toolTierRegistry, null);
    }

    public HaoHanItemCore(ItemRegistry itemRegistry, ItemFactory itemFactory, ItemService itemService,
                          RecipeRegistry recipeRegistry, RecipeService recipeService,
                          IconTextureRegistry iconTextureRegistry,
                          vn.haohan.itemcore.api.block.CustomBlockService customBlockService) {
        this(itemRegistry, itemFactory, itemService, recipeRegistry, recipeService, iconTextureRegistry, customBlockService, null);
    }

    public HaoHanItemCore(ItemRegistry itemRegistry, ItemFactory itemFactory, ItemService itemService,
                          RecipeRegistry recipeRegistry, RecipeService recipeService,
                          IconTextureRegistry iconTextureRegistry) {
        this(itemRegistry, itemFactory, itemService, recipeRegistry, recipeService, iconTextureRegistry, null, null);
    }

    /**
     * Lấy instance singleton của HaoHanItemCore.
     * @throws IllegalStateException nếu plugin chưa được khởi tạo
     */
    public static HaoHanItemCore get() {
        if (instance == null) {
            throw new IllegalStateException("HaoHanItemCore has not been initialized yet!");
        }
        return instance;
    }

    /**
     * Internal: Set instance. Chỉ được gọi từ HaoHanItemCorePlugin.
     */
    public static void setInstance(HaoHanItemCore engine) {
        instance = engine;
    }

    /**
     * Lấy ItemRegistry để đăng ký/tra cứu ItemDefinition.
     */
    public ItemRegistry getItemRegistry() { return itemRegistry; }

    /**
     * Lấy ItemFactory để tạo ItemStack từ ItemDefinition.
     */
    public ItemFactory getItemFactory() { return itemFactory; }

    /**
     * Lấy ItemService - facade đơn giản cho plugin khác.
     */
    public ItemService getItemService() { return itemService; }

    /**
     * Lấy RecipeRegistry để đăng ký/tra cứu RecipeDefinition.
     */
    public RecipeRegistry getRecipeRegistry() { return recipeRegistry; }

    /**
     * Lấy RecipeService - lookup recipes.
     */
    public RecipeService getRecipeService() { return recipeService; }

    /** Registry nạp icon riêng lẻ hoặc cắt từ texture atlas. */
    public IconTextureRegistry getIconTextureRegistry() { return iconTextureRegistry; }

    /** Service quản lý custom blocks (Barrier + BlockDisplay rendering). */
    public vn.haohan.itemcore.api.block.CustomBlockService getCustomBlockService() { return customBlockService; }

    /** Registry quản lý ToolTier chuẩn và custom. */
    public vn.haohan.itemcore.api.block.ToolTierRegistry getToolTierRegistry() { return toolTierRegistry; }

    /** Registry quản lý custom food actions của plugin dev. */
    public vn.haohan.itemcore.api.item.food.CustomFoodActionRegistry getFoodActionRegistry() { return foodActionRegistry; }
}
