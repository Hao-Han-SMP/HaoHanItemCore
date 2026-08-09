package vn.haohan.itemcore.api.recipe;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mô tả shaped recipe (crafting table có pattern cố định).
 * 
 * <p>Ví dụ:
 * <pre>
 * new ShapedRecipeDefinition(
 *     "machine:steel_plate",
 *     List.of("III", "ICI", "III"),
 *     Map.of(
 *         'I', new Ingredient.ItemIngredient("minecraft:iron_ingot"),
 *         'C', new Ingredient.ItemIngredient("machine:steel_core")
 *     ),
 *     new ItemResult("machine:steel_plate", 1)
 * );
 * </pre>
 */
public final class ShapedRecipeDefinition extends RecipeDefinition {

    private final List<String> pattern;
    private final Map<Character, Ingredient> ingredientMap;

    public ShapedRecipeDefinition(String id, List<String> pattern,
                                   Map<Character, Ingredient> ingredientMap,
                                   ItemResult result) {
        super(id, RecipeType.SHAPED, List.of(), result);
        this.pattern = List.copyOf(Objects.requireNonNull(pattern, "Pattern cannot be null"));
        this.ingredientMap = Map.copyOf(Objects.requireNonNull(ingredientMap, "Ingredient map cannot be null"));
        validate();
    }

    private void validate() {
        if (pattern.isEmpty() || pattern.size() > 3) {
            throw new IllegalArgumentException("Pattern must have 1-3 rows");
        }
        for (String row : pattern) {
            if (row.length() > 3) {
                throw new IllegalArgumentException("Each pattern row must have at most 3 characters");
            }
        }
        // Kiểm tra tất cả ký tự trong pattern (ngoại trừ space) phải có ingredient tương ứng
        for (String row : pattern) {
            for (char c : row.toCharArray()) {
                if (c != ' ' && !ingredientMap.containsKey(c)) {
                    throw new IllegalArgumentException(
                        "Pattern character '" + c + "' has no matching ingredient"
                    );
                }
            }
        }
    }

    public List<String> getPattern() { return pattern; }

    public Map<Character, Ingredient> getIngredientMap() { return ingredientMap; }

    @Override
    public String toString() {
        return "ShapedRecipeDefinition{id='" + getId() + "', pattern=" + pattern + ", result=" + getResult() + "}";
    }
}
