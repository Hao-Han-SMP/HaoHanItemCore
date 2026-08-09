package vn.haohan.itemcore.internal.recipe;

import vn.haohan.itemcore.api.recipe.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation của RecipeService.
 * Cung cấp lookup: findByResult, findByIngredient, findByType, search.
 */
public final class DefaultRecipeService implements RecipeService {

    private final RecipeRegistry registry;

    public DefaultRecipeService(RecipeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<RecipeDefinition> findByResult(String itemId) {
        return registry.all().stream()
                .filter(recipe -> recipe.getResult().item().equals(itemId))
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipeDefinition> findByIngredient(String itemId) {
        return registry.all().stream()
                .filter(recipe -> containsIngredient(recipe, itemId))
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipeDefinition> findByType(RecipeType type) {
        return registry.all().stream()
                .filter(recipe -> recipe.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<RecipeDefinition> all() {
        return registry.all();
    }

    @Override
    public List<RecipeDefinition> search(String keyword) {
        String lower = keyword.toLowerCase();
        return registry.all().stream()
                .filter(recipe ->
                    recipe.getId().toLowerCase().contains(lower) ||
                    recipe.getResult().item().toLowerCase().contains(lower)
                )
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra recipe có chứa ingredient với item ID chỉ định không.
     */
    private boolean containsIngredient(RecipeDefinition recipe, String itemId) {
        // Kiểm tra ingredients list (cho non-shaped recipes)
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredientMatchesId(ingredient, itemId)) {
                return true;
            }
        }

        // Kiểm tra ingredient map (cho shaped recipes)
        if (recipe instanceof ShapedRecipeDefinition shaped) {
            for (Ingredient ingredient : shaped.getIngredientMap().values()) {
                if (ingredientMatchesId(ingredient, itemId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean ingredientMatchesId(Ingredient ingredient, String itemId) {
        if (ingredient instanceof Ingredient.ItemIngredient item) {
            return item.id().equals(itemId);
        }
        if (ingredient instanceof Ingredient.MaterialIngredient mat) {
            return ("minecraft:" + mat.material().name().toLowerCase()).equals(itemId);
        }
        return false;
    }
}
