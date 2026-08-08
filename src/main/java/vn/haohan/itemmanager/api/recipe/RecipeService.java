package vn.haohan.itemmanager.api.recipe;

import java.util.Collection;
import java.util.List;

/**
 * Service cung cấp lookup hữu ích cho recipe.
 * 
 * <p>Ví dụ:
 * <pre>
 * // Tìm tất cả recipe tạo ra steel_plate
 * List&lt;RecipeDefinition&gt; recipes = recipeService.findByResult("machine:steel_plate");
 * 
 * // Tìm tất cả recipe sử dụng steel_core làm nguyên liệu
 * List&lt;RecipeDefinition&gt; usages = recipeService.findByIngredient("machine:steel_core");
 * </pre>
 */
public interface RecipeService {

    /**
     * Tìm tất cả recipe có result là item ID chỉ định.
     */
    List<RecipeDefinition> findByResult(String itemId);

    /**
     * Tìm tất cả recipe sử dụng item ID chỉ định làm ingredient.
     */
    List<RecipeDefinition> findByIngredient(String itemId);

    /**
     * Tìm tất cả recipe theo type.
     */
    List<RecipeDefinition> findByType(RecipeType type);

    /**
     * Lấy tất cả recipe.
     */
    Collection<RecipeDefinition> all();

    /**
     * Tìm kiếm recipe theo keyword.
     */
    List<RecipeDefinition> search(String keyword);
}
