package vn.haohan.itemcore.recipe;

import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.ItemResult;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeType;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRecipeRegistryTest {

    private DefaultRecipeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultRecipeRegistry(Logger.getLogger("TestLogger"));
    }

    @Test
    void testRegisterAndRetrieve() {
        RecipeDefinition recipe = new RecipeDefinition(
                "test:recipe_1",
                RecipeType.SHAPELESS,
                List.of(new Ingredient.ItemIngredient("minecraft:iron_ingot", 1)),
                new ItemResult("test:iron_rod", 2)
        );

        registry.register(recipe);

        assertTrue(registry.exists("test:recipe_1"));
        assertEquals(1, registry.size());
        assertEquals(recipe, registry.get("test:recipe_1"));
        assertEquals(recipe, registry.require("test:recipe_1"));
    }

    @Test
    void testRegisterReplacesOldRecipe() {
        RecipeDefinition recipe1 = new RecipeDefinition(
                "test:recipe_duplicate",
                RecipeType.SHAPELESS,
                List.of(new Ingredient.ItemIngredient("minecraft:iron_ingot", 1)),
                new ItemResult("test:iron_rod", 2)
        );

        RecipeDefinition recipe2 = new RecipeDefinition(
                "test:recipe_duplicate",
                RecipeType.SHAPELESS,
                List.of(new Ingredient.ItemIngredient("minecraft:gold_ingot", 1)),
                new ItemResult("test:gold_rod", 2)
        );

        registry.register(recipe1);
        assertEquals("test:iron_rod", registry.get("test:recipe_duplicate").getResult().item());

        // Registering same ID should replace instead of throwing error
        assertDoesNotThrow(() -> registry.register(recipe2));
        assertEquals(1, registry.size());
        assertEquals("test:gold_rod", registry.get("test:recipe_duplicate").getResult().item());
    }

    @Test
    void testUnregister() {
        RecipeDefinition recipe = new RecipeDefinition(
                "test:recipe_to_remove",
                RecipeType.SHAPELESS,
                List.of(new Ingredient.ItemIngredient("minecraft:iron_ingot", 1)),
                new ItemResult("test:iron_rod", 2)
        );

        registry.register(recipe);
        assertTrue(registry.exists("test:recipe_to_remove"));

        registry.unregister("test:recipe_to_remove");
        assertFalse(registry.exists("test:recipe_to_remove"));
        assertEquals(0, registry.size());
        assertNull(registry.get("test:recipe_to_remove"));
    }

    @Test
    void testClear() {
        registry.register(new RecipeDefinition(
                "test:r1",
                RecipeType.SHAPELESS,
                List.of(new Ingredient.ItemIngredient("minecraft:iron_ingot", 1)),
                new ItemResult("test:iron_rod", 2)
        ));
        registry.register(new RecipeDefinition(
                "test:r2",
                RecipeType.SHAPELESS,
                List.of(new Ingredient.ItemIngredient("minecraft:gold_ingot", 1)),
                new ItemResult("test:gold_rod", 2)
        ));

        assertEquals(2, registry.size());
        registry.clear();
        assertEquals(0, registry.size());
    }

    @Test
    void testInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(new RecipeDefinition(
                    "",
                    RecipeType.SHAPELESS,
                    List.of(new Ingredient.ItemIngredient("minecraft:iron_ingot", 1)),
                    new ItemResult("test:iron_rod", 2)
            ));
        });
    }
}
