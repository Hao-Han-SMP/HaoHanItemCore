package vn.haohan.itemmanager.api;

import vn.haohan.itemmanager.api.item.ItemFactory;
import vn.haohan.itemmanager.api.item.ItemRegistry;
import vn.haohan.itemmanager.api.item.ItemService;
import vn.haohan.itemmanager.api.recipe.RecipeRegistry;
import vn.haohan.itemmanager.api.recipe.RecipeService;

/**
 * API chính của HaoHanItemManager.
 * Cung cấp access tới tất cả hệ thống con.
 * 
 * <p>Ví dụ sử dụng:
 * <pre>
 * // Tạo item
 * ItemStack crystal = HaoHanItemManager.get().getItemService().create("magic:fire_crystal", 4);
 * 
 * // Kiểm tra item
 * boolean isCrystal = HaoHanItemManager.get().getItemService().isItem(stack, "magic:fire_crystal");
 * 
 * // Tìm recipe
 * List&lt;RecipeDefinition&gt; recipes = HaoHanItemManager.get().getRecipeService().findByResult("magic:fire_crystal");
 * </pre>
 */
public final class HaoHanItemManager {

    private static HaoHanItemManager instance;

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    private final ItemService itemService;
    private final RecipeRegistry recipeRegistry;
    private final RecipeService recipeService;

    public HaoHanItemManager(ItemRegistry itemRegistry, ItemFactory itemFactory, ItemService itemService,
                      RecipeRegistry recipeRegistry, RecipeService recipeService) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.itemService = itemService;
        this.recipeRegistry = recipeRegistry;
        this.recipeService = recipeService;
    }

    /**
     * Lấy instance singleton của HaoHanItemManager.
     * @throws IllegalStateException nếu plugin chưa được khởi tạo
     */
    public static HaoHanItemManager get() {
        if (instance == null) {
            throw new IllegalStateException("HaoHanItemManager has not been initialized yet!");
        }
        return instance;
    }

    /**
     * Internal: Set instance. Chỉ được gọi từ HaoHanItemManagerPlugin.
     */
    public static void setInstance(HaoHanItemManager engine) {
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
}
