package vn.haohan.itemmanager.internal.recipe;

import vn.haohan.itemmanager.api.recipe.RecipeDefinition;
import vn.haohan.itemmanager.api.recipe.RecipeRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Default implementation của RecipeRegistry.
 * Thread-safe, sử dụng ConcurrentHashMap.
 */
public final class DefaultRecipeRegistry implements RecipeRegistry {

    private final Map<String, RecipeDefinition> recipes = new ConcurrentHashMap<>();
    private final Logger logger;

    public DefaultRecipeRegistry(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void register(RecipeDefinition recipe) {
        Objects.requireNonNull(recipe, "RecipeDefinition cannot be null");

        String id = recipe.getId();

        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Recipe ID cannot be null or empty");
        }

        if (recipes.containsKey(id)) {
            throw new IllegalArgumentException("Recipe already registered: '" + id + "'");
        }

        recipes.put(id, recipe);
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
        logger.info("[RecipeRegistry] Cleared all recipes.");
    }
}
