package vn.haohan.itemcore.recipe;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.ItemResult;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.ShapedRecipeDefinition;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.internal.recipe.CraftingRecipeResolver;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeRegistry;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeService;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CraftingRecipeResolverTest {

    private DefaultItemRegistry itemRegistry;
    private DefaultRecipeRegistry recipeRegistry;
    private DefaultRecipeService recipeService;
    private CraftingRecipeResolver resolver;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("ResolverTest");
        itemRegistry = new DefaultItemRegistry(logger);
        recipeRegistry = new DefaultRecipeRegistry(logger);
        recipeService = new DefaultRecipeService(recipeRegistry);

        // Register custom items
        itemRegistry.register(ItemDefinition.builder("haohanmetallurgy:embersteel_ingot")
                .material(Material.IRON_INGOT)
                .displayName("Embersteel Ingot")
                .type(ItemType.MATERIAL)
                .build());

        itemRegistry.register(ItemDefinition.builder("haohanmetallurgy:embersteel_pickaxe")
                .material(Material.IRON_PICKAXE)
                .displayName("Embersteel Pickaxe")
                .type(ItemType.TOOL)
                .build());

        // Register custom shaped recipe
        recipeRegistry.register(new ShapedRecipeDefinition(
                "haohanmetallurgy:embersteel_pickaxe",
                List.of("III", " T ", " T "),
                Map.of(
                        'I', new Ingredient.ItemIngredient("haohanmetallurgy:embersteel_ingot"),
                        'T', new Ingredient.MaterialIngredient(Material.STICK)
                ),
                new ItemResult("haohanmetallurgy:embersteel_pickaxe", 1)
        ));

        resolver = new CraftingRecipeResolver(itemRegistry, null, recipeService, null);
    }

    @Test
    void testRecipeRegisteredAndResolvable() {
        assertEquals(2, itemRegistry.size());
        assertEquals(1, recipeRegistry.size());

        var recipes = recipeService.findByResult("haohanmetallurgy:embersteel_pickaxe");
        assertEquals(1, recipes.size());
        assertTrue(recipes.get(0) instanceof ShapedRecipeDefinition);

        ShapedRecipeDefinition shaped = (ShapedRecipeDefinition) recipes.get(0);
        assertEquals(3, shaped.getPattern().size());
        assertEquals("III", shaped.getPattern().get(0));
        assertEquals(" T ", shaped.getPattern().get(1));
        assertEquals(" T ", shaped.getPattern().get(2));

        // Verify resolver can find custom recipes by NamespacedKey
        assertNotNull(resolver.findRecipeByKey(org.bukkit.NamespacedKey.fromString("haohanmetallurgy:embersteel_pickaxe")));
        assertNotNull(resolver.findRecipeByKey(new org.bukkit.NamespacedKey("minecraft", "haohanmetallurgy_embersteel_pickaxe")));
    }

    @Test
    void testShapelessRecipeRegistered() {
        recipeRegistry.register(new RecipeDefinition(
                "haohanmetallurgy:embersteel_blend",
                vn.haohan.itemcore.api.recipe.RecipeType.SHAPELESS,
                List.of(
                        new Ingredient.ItemIngredient("haohanmetallurgy:embersteel_ingot", 1),
                        new Ingredient.MaterialIngredient(Material.BLAZE_POWDER, 1)
                ),
                new ItemResult("haohanmetallurgy:embersteel_blend", 2)
        ));

        var recipes = recipeService.findByResult("haohanmetallurgy:embersteel_blend");
        assertEquals(1, recipes.size());
        assertEquals(vn.haohan.itemcore.api.recipe.RecipeType.SHAPELESS, recipes.get(0).getType());
    }

    @Test
    void testHasCustomIngredients() {
        var shapedRecipe = recipeRegistry.get("haohanmetallurgy:embersteel_pickaxe");
        assertTrue(resolver.hasCustomIngredients(shapedRecipe));

        // Vanilla ingredients only recipe
        RecipeDefinition vanillaRecipe = new ShapedRecipeDefinition(
                "minecraft:iron_pickaxe_test",
                List.of("III", " T ", " T "),
                Map.of(
                        'I', new Ingredient.ItemIngredient("minecraft:iron_ingot"),
                        'T', new Ingredient.MaterialIngredient(Material.STICK)
                ),
                new ItemResult("minecraft:iron_pickaxe", 1)
        );
        assertFalse(resolver.hasCustomIngredients(vanillaRecipe));
    }

    @Test
    void testGetNeededCustomItems() {
        var recipe = (ShapedRecipeDefinition) recipeRegistry.get("haohanmetallurgy:embersteel_pickaxe");
        Map<String, Integer> needed = resolver.getNeededCustomItems(recipe);
        assertEquals(1, needed.size());
        assertEquals(3, needed.get("haohanmetallurgy:embersteel_ingot"));
    }

    @Test
    void testSendGhostRecipePreviewSafeHeadless() {
        assertDoesNotThrow(() -> resolver.sendGhostRecipePreview(null, null));
        assertDoesNotThrow(() -> resolver.sendGhostRecipePreview(null, new org.bukkit.NamespacedKey("haohanitemcore", "test")));
    }
}
