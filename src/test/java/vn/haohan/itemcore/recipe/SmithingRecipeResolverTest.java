package vn.haohan.itemcore.recipe;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.ItemResult;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeType;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeRegistry;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeService;
import vn.haohan.itemcore.internal.recipe.RecipeIngredientMatcher;
import vn.haohan.itemcore.internal.recipe.SmithingRecipeResolver;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SmithingRecipeResolverTest {

    private DefaultItemRegistry itemRegistry;
    private ItemService itemService;
    private DefaultRecipeService recipeService;
    private RecipeIngredientMatcher matcher;
    private SmithingRecipeResolver resolver;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("SmithingTest");
        itemRegistry = new DefaultItemRegistry(logger);
        itemService = new TestItemService(itemRegistry);
        DefaultRecipeRegistry recipeRegistry = new DefaultRecipeRegistry(logger);
        recipeService = new DefaultRecipeService(recipeRegistry);
        matcher = new RecipeIngredientMatcher(itemService);
        resolver = new SmithingRecipeResolver(matcher, recipeService, null, itemService, itemRegistry);

        // Register custom items
        itemRegistry.register(ItemDefinition.builder("example:aero_compound")
                .material(Material.PAPER)
                .displayName("Aero Compound")
                .type(ItemType.MATERIAL)
                .build());

        itemRegistry.register(ItemDefinition.builder("example:custom_helmet")
                .material(Material.NETHERITE_HELMET)
                .displayName("Custom Helmet")
                .type(ItemType.ARMOR)
                .build());

        itemRegistry.register(ItemDefinition.builder("example:custom_diamond_helmet")
                .material(Material.DIAMOND_HELMET)
                .displayName("Custom Diamond Helmet")
                .type(ItemType.ARMOR)
                .build());

        // Register custom smithing recipe:
        // Template: NETHERITE_UPGRADE_SMITHING_TEMPLATE
        // Base: minecraft:netherite_helmet
        // Addition: example:aero_compound
        // Result: example:custom_helmet
        recipeRegistry.register(new RecipeDefinition(
                "example:custom_helmet_smithing",
                RecipeType.SMITHING,
                List.of(
                        new Ingredient.ItemIngredient("minecraft:netherite_upgrade_smithing_template"),
                        new Ingredient.ItemIngredient("minecraft:netherite_helmet"),
                        new Ingredient.ItemIngredient("example:aero_compound")
                ),
                new ItemResult("example:custom_helmet", 1)
        ));
    }

    @Test
    void testCustomSmithingRecipeMatchedWithExactCustomItem() {
        ItemStack template = TestItemService.mockItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack base = TestItemService.mockItem(Material.NETHERITE_HELMET);
        ItemStack addition = itemService.create("example:aero_compound");

        RecipeDefinition recipe = resolver.findSmithingRecipe(template, base, addition);
        assertNotNull(recipe, "Custom smithing recipe should be found when exact custom item is provided");
        assertEquals("example:custom_helmet_smithing", recipe.getId());
    }

    @Test
    void testVanillaPaperRejectedForCustomSmithingRecipe() {
        ItemStack template = TestItemService.mockItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack base = TestItemService.mockItem(Material.NETHERITE_HELMET);
        ItemStack vanillaPaper = TestItemService.mockItem(Material.PAPER); // Giấy thường (Carrier của Aero Compound)

        RecipeDefinition recipe = resolver.findSmithingRecipe(template, base, vanillaPaper);
        assertNull(recipe, "Vanilla paper must NOT match custom recipe expecting Aero Compound!");
    }

    @Test
    void testVanillaNetheriteUpgradeNotMatchedAsCustomRecipe() {
        ItemStack template = TestItemService.mockItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack base = TestItemService.mockItem(Material.DIAMOND_HELMET);
        ItemStack addition = TestItemService.mockItem(Material.NETHERITE_INGOT);

        RecipeDefinition recipe = resolver.findSmithingRecipe(template, base, addition);
        assertNull(recipe, "Vanilla netherite upgrade should not match custom recipes");
        assertFalse(matcher.containsCustomItem(template, base, addition), "Vanilla items should not be marked as custom");
    }

    @Test
    void testCustomItemInVanillaSmithingDetectedAsCarrierLeak() {
        ItemStack template = TestItemService.mockItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack customDiamondHelmet = itemService.create("example:custom_diamond_helmet");
        ItemStack netheriteIngot = TestItemService.mockItem(Material.NETHERITE_INGOT);

        // Custom diamond helmet không khớp custom recipe nào
        RecipeDefinition recipe = resolver.findSmithingRecipe(template, customDiamondHelmet, netheriteIngot);
        assertNull(recipe);
        // Nhưng matcher phát hiện có custom item -> Chặn Carrier Leak
        assertTrue(matcher.containsCustomItem(template, customDiamondHelmet, netheriteIngot));
    }

    @Test
    void testResolveVanillaNetheriteHelmet() {
        ItemStack template = TestItemService.mockItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack baseHelmet = TestItemService.mockItem(Material.DIAMOND_HELMET);
        ItemStack netheriteIngot = TestItemService.mockItem(Material.NETHERITE_INGOT);

        ItemStack result = resolver.resolveVanillaSmithing(template, baseHelmet, netheriteIngot);
        assertNotNull(result, "Vanilla Netherite Helmet upgrade should resolve successfully");
        assertEquals(Material.NETHERITE_HELMET, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    void testResolveVanillaNetheriteSword() {
        ItemStack template = TestItemService.mockItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack baseSword = TestItemService.mockItem(Material.DIAMOND_SWORD);
        ItemStack netheriteIngot = TestItemService.mockItem(Material.NETHERITE_INGOT);

        ItemStack result = resolver.resolveVanillaSmithing(template, baseSword, netheriteIngot);
        assertNotNull(result, "Vanilla Netherite Sword upgrade should resolve successfully");
        assertEquals(Material.NETHERITE_SWORD, result.getType());
        assertEquals(1, result.getAmount());
    }

    @Test
    void testRejectCarrierLeakInVanillaSmithingFallback() {
        ItemStack template = TestItemService.mockItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack customDiamondHelmet = itemService.create("example:custom_diamond_helmet");
        ItemStack netheriteIngot = TestItemService.mockItem(Material.NETHERITE_INGOT);

        ItemStack result = resolver.resolveVanillaSmithing(template, customDiamondHelmet, netheriteIngot);
        assertNull(result, "Custom item in vanilla smithing fallback must be rejected!");
    }
}
