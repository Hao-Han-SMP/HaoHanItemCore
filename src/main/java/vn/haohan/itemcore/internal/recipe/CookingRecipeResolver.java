package vn.haohan.itemcore.internal.recipe;

import vn.haohan.itemcore.api.item.ItemFactory;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeService;
import vn.haohan.itemcore.api.recipe.RecipeType;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Bộ phân giải và xử lý công thức Nung / Lò nung (Furnace / Smelting / Cooking Resolver).
 * 
 * <p>Nhiệm vụ:
 * <ul>
 *   <li>Xác minh nguyên liệu nung cho Lò Nung (Furnace), Lò Cao (Blast Furnace), Lò Hun Khói (Smoker), Lửa Trại (Campfire).</li>
 *   <li>Chống Carrier Leak: Ngăn chặn tuyệt đối quặng / vật phẩm custom bị nung thành phôi vanilla ngoài ý muốn.</li>
 *   <li>Xử lý chuyển đổi chính xác từ Custom Ore / Item sang Custom Result.</li>
 * </ul>
 */
public final class CookingRecipeResolver {

    private final RecipeIngredientMatcher matcher;
    private final RecipeService recipeService;
    private final ItemFactory itemFactory;
    private final ItemService itemService;

    public CookingRecipeResolver(RecipeIngredientMatcher matcher, RecipeService recipeService,
                                 ItemFactory itemFactory, ItemService itemService) {
        this.matcher = matcher;
        this.recipeService = recipeService;
        this.itemFactory = itemFactory;
        this.itemService = itemService;
    }

    /**
     * Xử lý khi lò nung hoàn tất nung 1 vật phẩm (FurnaceSmeltEvent).
     */
    public void handleFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        if (RecipeIngredientMatcher.isAir(source)) {
            return;
        }

        Block block = event.getBlock();
        RecipeDefinition matched = findCookingRecipe(source, block.getType());

        if (matcher.isCustomItem(source)) {
            if (matched != null && itemFactory != null) {
                // Khớp custom cooking recipe
                ItemStack result = itemFactory.create(
                        matched.getResult().item(),
                        matched.getResult().amount()
                );
                if (itemService != null) {
                    itemService.validateAndUpdate(result);
                }
                event.setResult(result);
            } else {
                // Custom item nhưng không có công thức nung hợp lệ -> Chặn Carrier Leak
                event.setCancelled(true);
                event.setResult(new ItemStack(Material.AIR));
            }
        } else {
            // Source là Vanilla Item
            ItemStack currentResult = event.getResult();
            if (currentResult != null && matcher.isCustomItem(currentResult)) {
                if (matched == null) {
                    // Vanilla item nhưng lại cho ra Custom Item không hợp lệ -> Huỷ
                    event.setCancelled(true);
                    event.setResult(new ItemStack(Material.AIR));
                }
            }
        }
    }

    /**
     * Xử lý khi lò nung bắt đầu nung 1 vật phẩm mới (FurnaceStartSmeltEvent).
     */
    public void handleFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        ItemStack source = event.getSource();
        if (RecipeIngredientMatcher.isAir(source)) {
            return;
        }

        if (matcher.isCustomItem(source)) {
            RecipeDefinition matched = findCookingRecipe(source, event.getBlock().getType());
            if (matched == null) {
                // Dừng tiến trình nung nếu không có công thức custom hợp lệ
                event.setTotalCookTime(Short.MAX_VALUE);
            } else if (matched.getCookingTime() > 0) {
                event.setTotalCookTime(matched.getCookingTime());
            }
        }
    }

    /**
     * Xử lý cho lửa trại (Campfire / Soul Campfire qua BlockCookEvent).
     */
    public void handleBlockCook(BlockCookEvent event) {
        ItemStack source = event.getSource();
        if (RecipeIngredientMatcher.isAir(source)) {
            return;
        }

        RecipeDefinition matched = findCookingRecipe(source, event.getBlock().getType());

        if (matcher.isCustomItem(source)) {
            if (matched != null && itemFactory != null) {
                ItemStack result = itemFactory.create(
                        matched.getResult().item(),
                        matched.getResult().amount()
                );
                if (itemService != null) {
                    itemService.validateAndUpdate(result);
                }
                event.setResult(result);
            } else {
                event.setCancelled(true);
                event.setResult(new ItemStack(Material.AIR));
            }
        }
    }

    /**
     * Tìm công thức nung phù hợp dựa trên ItemStack nguồn và loại khối lò nung.
     */
    public RecipeDefinition findCookingRecipe(ItemStack source, Material blockType) {
        if (recipeService == null || RecipeIngredientMatcher.isAir(source)) {
            return null;
        }

        RecipeType expectedType = switch (blockType) {
            case BLAST_FURNACE -> RecipeType.BLASTING;
            case SMOKER -> RecipeType.SMOKING;
            case CAMPFIRE, SOUL_CAMPFIRE -> RecipeType.CAMPFIRE;
            default -> RecipeType.SMELTING;
        };

        for (RecipeDefinition recipe : recipeService.all()) {
            if (recipe.getType() != expectedType && recipe.getType() != RecipeType.SMELTING) {
                continue;
            }

            List<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.isEmpty()) {
                continue;
            }

            if (matcher.matches(ingredients.getFirst(), source)) {
                return recipe;
            }
        }

        return null;
    }
}
