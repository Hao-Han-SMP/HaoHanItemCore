package vn.haohan.itemcore.recipe;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.ItemResult;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeType;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.internal.recipe.CookingRecipeResolver;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeRegistry;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeService;
import vn.haohan.itemcore.internal.recipe.RecipeIngredientMatcher;
import vn.haohan.itemcore.internal.recipe.StonecuttingRecipeResolver;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CookingAndStonecuttingResolverTest {

    private DefaultItemRegistry itemRegistry;
    private ItemService itemService;
    private DefaultRecipeService recipeService;
    private RecipeIngredientMatcher matcher;
    private CookingRecipeResolver cookingResolver;
    private StonecuttingRecipeResolver stonecuttingResolver;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("CookingTest");
        itemRegistry = new DefaultItemRegistry(logger);
        itemService = new TestItemService(itemRegistry);
        DefaultRecipeRegistry recipeRegistry = new DefaultRecipeRegistry(logger);
        recipeService = new DefaultRecipeService(recipeRegistry);
        matcher = new RecipeIngredientMatcher(itemService);
        cookingResolver = new CookingRecipeResolver(matcher, recipeService, null, itemService, itemRegistry);
        stonecuttingResolver = new StonecuttingRecipeResolver(matcher, recipeService, null, itemService, itemRegistry);

        // Register custom items
        itemRegistry.register(ItemDefinition.builder("example:raw_mithril")
                .material(Material.RAW_IRON)
                .displayName("Raw Mithril")
                .type(ItemType.MATERIAL)
                .build());

        itemRegistry.register(ItemDefinition.builder("example:mithril_ingot")
                .material(Material.IRON_INGOT)
                .displayName("Mithril Ingot")
                .type(ItemType.MATERIAL)
                .build());

        itemRegistry.register(ItemDefinition.builder("example:unsmeltable_ore")
                .material(Material.RAW_IRON)
                .displayName("Unsmeltable Ore")
                .type(ItemType.MATERIAL)
                .build());

        itemRegistry.register(ItemDefinition.builder("example:custom_stone")
                .material(Material.STONE)
                .displayName("Custom Stone")
                .type(ItemType.MATERIAL)
                .build());

        itemRegistry.register(ItemDefinition.builder("example:custom_brick")
                .material(Material.STONE_BRICKS)
                .displayName("Custom Brick")
                .type(ItemType.MATERIAL)
                .build());

        // Register smelting recipe: example:raw_mithril -> example:mithril_ingot
        recipeRegistry.register(new RecipeDefinition(
                "example:mithril_smelting",
                RecipeType.SMELTING,
                List.of(new Ingredient.ItemIngredient("example:raw_mithril")),
                new ItemResult("example:mithril_ingot", 1),
                0.7f,
                200
        ));

        // Register stonecutting recipe: example:custom_stone -> example:custom_brick
        recipeRegistry.register(new RecipeDefinition(
                "example:custom_stone_cutting",
                RecipeType.STONECUTTING,
                List.of(new Ingredient.ItemIngredient("example:custom_stone")),
                new ItemResult("example:custom_brick", 1)
        ));
    }

    @Test
    void testCookingRecipeMatchedForCustomRawOre() {
        ItemStack rawMithril = itemService.create("example:raw_mithril");
        RecipeDefinition recipe = cookingResolver.findCookingRecipe(rawMithril, Material.FURNACE);

        assertNotNull(recipe);
        assertEquals("example:mithril_smelting", recipe.getId());
    }

    @Test
    void testCookingRecipeNotMatchedForVanillaRawIron() {
        ItemStack vanillaRawIron = TestItemService.mockItem(Material.RAW_IRON);
        RecipeDefinition recipe = cookingResolver.findCookingRecipe(vanillaRawIron, Material.FURNACE);

        assertNull(recipe, "Vanilla raw iron must NOT trigger custom smelting recipe!");
    }

    @Test
    void testUnsmeltableCustomItemDetectedForCarrierLeakProtection() {
        ItemStack unsmeltable = itemService.create("example:unsmeltable_ore");
        RecipeDefinition recipe = cookingResolver.findCookingRecipe(unsmeltable, Material.FURNACE);

        assertNull(recipe);
        assertTrue(matcher.isCustomItem(unsmeltable), "Custom item must be recognized to cancel vanilla furnace smelting");
    }

    @Test
    void testStonecuttingRecipeMatchedForCustomStone() {
        ItemStack customStone = itemService.create("example:custom_stone");
        RecipeDefinition recipe = stonecuttingResolver.findStonecuttingRecipe(customStone);

        assertNotNull(recipe);
        assertEquals("example:custom_stone_cutting", recipe.getId());
    }

    @Test
    void testStonecuttingRecipeNotMatchedForVanillaStone() {
        ItemStack vanillaStone = TestItemService.mockItem(Material.STONE);
        RecipeDefinition recipe = stonecuttingResolver.findStonecuttingRecipe(vanillaStone);

        assertNull(recipe, "Vanilla stone must NOT match custom stonecutting recipe!");
    }
}
