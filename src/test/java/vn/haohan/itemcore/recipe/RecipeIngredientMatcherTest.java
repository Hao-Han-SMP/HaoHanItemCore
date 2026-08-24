package vn.haohan.itemcore.recipe;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.internal.recipe.RecipeIngredientMatcher;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class RecipeIngredientMatcherTest {

    private DefaultItemRegistry itemRegistry;
    private ItemService itemService;
    private RecipeIngredientMatcher matcher;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("MatcherTest");
        itemRegistry = new DefaultItemRegistry(logger);
        itemService = new TestItemService(itemRegistry);
        matcher = new RecipeIngredientMatcher(itemService);

        // Register custom item Aero Compound (carrier: Material.PAPER)
        itemRegistry.register(ItemDefinition.builder("example:aero_compound")
                .material(Material.PAPER)
                .displayName("Aero Compound")
                .type(ItemType.MATERIAL)
                .build());

        // Register custom item Custom Helmet (carrier: Material.NETHERITE_HELMET)
        itemRegistry.register(ItemDefinition.builder("example:custom_helmet")
                .material(Material.NETHERITE_HELMET)
                .displayName("Custom Helmet")
                .type(ItemType.ARMOR)
                .build());
    }

    @Test
    void testVanillaItemMatchesVanillaIngredient() {
        ItemStack vanillaPaper = TestItemService.mockItem(Material.PAPER);
        Ingredient vanillaPaperIng = new Ingredient.ItemIngredient("minecraft:paper");

        assertTrue(matcher.matches(vanillaPaperIng, vanillaPaper));
        assertTrue(matcher.isVanillaItem(vanillaPaper));
        assertFalse(matcher.isCustomItem(vanillaPaper));
    }

    @Test
    void testCustomItemDoesNotMatchVanillaIngredient() {
        ItemStack aeroCompound = itemService.create("example:aero_compound");
        Ingredient vanillaPaperIng = new Ingredient.ItemIngredient("minecraft:paper");

        // Custom item có carrier là PAPER không bao giờ được khớp với ingredient yêu cầu minecraft:paper
        assertFalse(matcher.matches(vanillaPaperIng, aeroCompound), "Carrier leak: Custom item matched vanilla ingredient!");
        assertTrue(matcher.isCustomItem(aeroCompound));
        assertFalse(matcher.isVanillaItem(aeroCompound));
    }

    @Test
    void testCustomItemMatchesCustomIngredient() {
        ItemStack aeroCompound = itemService.create("example:aero_compound");
        Ingredient customIng = new Ingredient.ItemIngredient("example:aero_compound");

        assertTrue(matcher.matches(customIng, aeroCompound));
    }

    @Test
    void testVanillaItemDoesNotMatchCustomIngredient() {
        ItemStack vanillaPaper = TestItemService.mockItem(Material.PAPER);
        Ingredient customIng = new Ingredient.ItemIngredient("example:aero_compound");

        // Giấy thường không thể thỏa mãn ingredient yêu cầu custom item Aero Compound
        assertFalse(matcher.matches(customIng, vanillaPaper), "Vanilla item matched custom ingredient!");
    }

    @Test
    void testMaterialIngredientRequiresVanillaItem() {
        ItemStack vanillaPaper = TestItemService.mockItem(Material.PAPER);
        ItemStack aeroCompound = itemService.create("example:aero_compound");
        Ingredient matIng = new Ingredient.MaterialIngredient(Material.PAPER);

        assertTrue(matcher.matches(matIng, vanillaPaper));
        assertFalse(matcher.matches(matIng, aeroCompound), "Custom item matched pure Material ingredient!");
    }

    @Test
    void testContainsCustomItem() {
        ItemStack vanillaPaper = TestItemService.mockItem(Material.PAPER);
        ItemStack vanillaHelmet = TestItemService.mockItem(Material.NETHERITE_HELMET);
        ItemStack aeroCompound = itemService.create("example:aero_compound");

        assertFalse(matcher.containsCustomItem(vanillaPaper, vanillaHelmet));
        assertTrue(matcher.containsCustomItem(vanillaPaper, aeroCompound, vanillaHelmet));
    }
}
