package vn.haohan.itemcore.internal.recipe;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeService;
import vn.haohan.itemcore.api.recipe.RecipeType;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.StonecutterInventory;

import java.util.List;

/**
 * Bộ phân giải và xử lý công thức Bàn Cắt Đá (Stonecutter Resolver).
 * 
 * <p>Nhiệm vụ:
 * <ul>
 *   <li>Xác minh nguyên liệu cắt đá cho Stonecutter.</li>
 *   <li>Chống Carrier Leak: Ngăn chặn Custom Item bị cắt đá thành các khối vanilla ngoài ý muốn.</li>
 *   <li>Xử lý công thức Stonecutting custom và dispatch onCraft.</li>
 * </ul>
 */
public final class StonecuttingRecipeResolver {

    private final RecipeIngredientMatcher matcher;
    private final RecipeService recipeService;
    private final ItemRegistry itemRegistry;

    public StonecuttingRecipeResolver(RecipeIngredientMatcher matcher, RecipeService recipeService,
                                      ItemRegistry itemRegistry) {
        this.matcher = matcher;
        this.recipeService = recipeService;
        this.itemRegistry = itemRegistry;
    }

    /**
     * Xử lý khi người chơi thao tác trong giao diện Bàn Cắt Đá.
     */
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof StonecutterInventory inventory)) {
            return;
        }

        // Slot 1 là ô kết quả của StonecutterInventory
        if (event.getRawSlot() != 1) {
            return;
        }

        ItemStack input = inventory.getItem(0);
        if (RecipeIngredientMatcher.isAir(input)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (RecipeIngredientMatcher.isAir(result)) {
            return;
        }

        RecipeDefinition customRecipe = findStonecuttingRecipe(input);

        if (matcher.isCustomItem(input)) {
            if (customRecipe == null) {
                // Input là Custom Item nhưng không có công thức cắt đá custom -> Chặn Carrier Leak
                event.setCancelled(true);
                inventory.setItem(1, null);
                return;
            }

            if (event.getWhoClicked() instanceof Player player) {
                String resultId = customRecipe.getResult().item();
                ItemDefinition definition = itemRegistry != null ? itemRegistry.get(resultId) : null;
                if (definition != null && definition.hasBehavior()) {
                    var context = new vn.haohan.itemcore.api.item.ItemContext(player, result, definition, event);
                    definition.getBehavior().onCraft(context);
                }
            }
        } else {
            // Input là Vanilla Item
            if (matcher.isCustomItem(result) && customRecipe == null) {
                event.setCancelled(true);
                inventory.setItem(1, null);
            }
        }
    }

    /**
     * Tìm công thức cắt đá custom phù hợp với ItemStack đầu vào.
     */
    public RecipeDefinition findStonecuttingRecipe(ItemStack input) {
        if (recipeService == null || RecipeIngredientMatcher.isAir(input)) {
            return null;
        }

        for (RecipeDefinition recipe : recipeService.all()) {
            if (recipe.getType() != RecipeType.STONECUTTING) {
                continue;
            }

            List<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.isEmpty()) {
                continue;
            }

            if (matcher.matches(ingredients.getFirst(), input)) {
                return recipe;
            }
        }

        return null;
    }
}
