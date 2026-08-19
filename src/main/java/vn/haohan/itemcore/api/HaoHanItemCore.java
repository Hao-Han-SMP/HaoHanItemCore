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

    public HaoHanItemCore(ItemRegistry itemRegistry, ItemFactory itemFactory, ItemService itemService,
                      RecipeRegistry recipeRegistry, RecipeService recipeService,
                      IconTextureRegistry iconTextureRegistry) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.itemService = itemService;
        this.recipeRegistry = recipeRegistry;
        this.recipeService = recipeService;
        this.iconTextureRegistry = iconTextureRegistry;
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
}
