package vn.haohan.itemcore.api.recipe;

import java.util.List;
import java.util.Objects;

/**
 * Mô tả một recipe (công thức chế tạo).
 * Dùng cho SHAPELESS, SMELTING, BLASTING, SMOKING, CAMPFIRE, STONECUTTING, MACHINE.
 * 
 * <p>Với SHAPED recipe, sử dụng {@link ShapedRecipeDefinition}.
 */
public class RecipeDefinition {

    private final String id;
    private final RecipeType type;
    private final List<Ingredient> ingredients;
    private final ItemResult result;
    private final float experience;
    private final int cookingTime;

    public RecipeDefinition(String id, RecipeType type, List<Ingredient> ingredients, ItemResult result) {
        this(id, type, ingredients, result, 0f, 200);
    }

    public RecipeDefinition(String id, RecipeType type, List<Ingredient> ingredients, ItemResult result,
                            float experience, int cookingTime) {
        this.id = Objects.requireNonNull(id, "Recipe ID cannot be null");
        this.type = Objects.requireNonNull(type, "Recipe type cannot be null");
        this.ingredients = List.copyOf(Objects.requireNonNull(ingredients, "Ingredients cannot be null"));
        this.result = Objects.requireNonNull(result, "Result cannot be null");
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    public String getId() { return id; }

    /**
     * Lấy namespace từ Recipe ID.
     */
    public String getNamespace() {
        int colonIndex = id.indexOf(':');
        return colonIndex > 0 ? id.substring(0, colonIndex) : "";
    }

    /**
     * Lấy key từ Recipe ID.
     */
    public String getKey() {
        int colonIndex = id.indexOf(':');
        return colonIndex > 0 ? id.substring(colonIndex + 1) : id;
    }

    public RecipeType getType() { return type; }

    public List<Ingredient> getIngredients() { return ingredients; }

    public ItemResult getResult() { return result; }

    public float getExperience() { return experience; }

    public int getCookingTime() { return cookingTime; }

    @Override
    public String toString() {
        return "RecipeDefinition{id='" + id + "', type=" + type + ", result=" + result + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipeDefinition that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
