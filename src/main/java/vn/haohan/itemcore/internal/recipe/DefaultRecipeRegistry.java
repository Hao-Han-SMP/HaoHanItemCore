package vn.haohan.itemcore.internal.recipe;

import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Default implementation của RecipeRegistry.
 * Thread-safe, tự động đồng bộ đăng ký với BukkitRecipeAdapter.
 */
public final class DefaultRecipeRegistry implements RecipeRegistry {

    private final Map<String, RecipeDefinition> recipes = new ConcurrentHashMap<>();
    private final Logger logger;
    private BukkitRecipeAdapter recipeAdapter;

    public DefaultRecipeRegistry(Logger logger) {
        this.logger = logger;
    }

    public void setRecipeAdapter(BukkitRecipeAdapter recipeAdapter) {
        this.recipeAdapter = recipeAdapter;
    }

    @Override
    public void register(RecipeDefinition recipe) {
        Objects.requireNonNull(recipe, "RecipeDefinition cannot be null");

        String id = recipe.getId();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Recipe ID cannot be null or empty");
        }

        if (recipes.containsKey(id)) {
            // Thay thế recipe cũ nếu đã tồn tại để hỗ trợ plugin reload
            unregister(id);
        }

        recipes.put(id, recipe);

        if (recipeAdapter != null) {
            recipeAdapter.register(recipe);
        }

        logger.info("[RecipeRegistry] Registered: " + id +
                " (Type: " + recipe.getType() + ", Result: " + recipe.getResult().item() + ")");
    }

    @Override
    public RecipeDefinition get(String id) {
        return recipes.get(id);
    }

    @Override
    public RecipeDefinition require(String id) {
        RecipeDefinition recipe = recipes.get(id);
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe not found: '" + id + "'");
        }
        return recipe;
    }

    @Override
    public boolean exists(String id) {
        return recipes.containsKey(id);
    }

    @Override
    public void unregister(String id) {
        RecipeDefinition removed = recipes.remove(id);
        if (removed != null) {
            if (recipeAdapter != null) {
                recipeAdapter.unregister(id);
            }
            logger.info("[RecipeRegistry] Unregistered: " + id);
        }
    }

    @Override
    public Collection<RecipeDefinition> all() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    @Override
    public int size() {
        return recipes.size();
    }

    @Override
    public void clear() {
        recipes.clear();
        if (recipeAdapter != null) {
            recipeAdapter.unregisterAll();
        }
        logger.info("[RecipeRegistry] Cleared all recipes.");
    }
}

