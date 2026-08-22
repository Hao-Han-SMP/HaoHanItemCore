package vn.haohan.itemcore;

import vn.haohan.itemcore.api.item.ItemCategory;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.ItemResult;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeType;
import vn.haohan.itemcore.internal.gui.ItemBrowserFilter;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemFilterTest {

    @Test
    void testItemCategoryMatching() {
        // 1. Tool item
        ItemDefinition tool = ItemDefinition.builder("weapon:ruby_pickaxe")
                .material(Material.DIAMOND_PICKAXE)
                .type(ItemType.TOOL)
                .build();
        assertTrue(ItemCategory.TOOLS.matches(tool));
        assertFalse(ItemCategory.WEAPONS.matches(tool));
        assertFalse(ItemCategory.CUSTOM_BLOCKS.matches(tool));

        // 2. Weapon item
        ItemDefinition weapon = ItemDefinition.builder("weapon:fire_sword")
                .material(Material.NETHERITE_SWORD)
                .type(ItemType.WEAPON)
                .build();
        assertTrue(ItemCategory.WEAPONS.matches(weapon));
        assertFalse(ItemCategory.TOOLS.matches(weapon));

        // 3. Custom Block item
        ItemDefinition block = ItemDefinition.builder("haohan:anorthosite_ore")
                .material(Material.NOTE_BLOCK)
                .property("custom_block_data", "minecraft:note_block[note=24,instrument=pling,powered=true]")
                .build();
        assertTrue(ItemCategory.CUSTOM_BLOCKS.matches(block));
        assertFalse(ItemCategory.FOOD.matches(block));

        // 4. Armor item
        ItemDefinition armor = ItemDefinition.builder("haohan:spacesuit_helmet")
                .material(Material.NETHERITE_HELMET)
                .type(ItemType.ARMOR)
                .property("equippable_asset_id", "haohan:spacesuit")
                .build();
        assertTrue(ItemCategory.ARMOR.matches(armor));

        // 5. Material item
        ItemDefinition mat = ItemDefinition.builder("magic:mana_crystal")
                .material(Material.DIAMOND)
                .type(ItemType.MATERIAL)
                .build();
        assertTrue(ItemCategory.MATERIALS.matches(mat));

        // 6. Food item
        ItemDefinition food = ItemDefinition.builder("food:energy_drink")
                .material(Material.POTION)
                .type(ItemType.FOOD)
                .build();
        assertTrue(ItemCategory.FOOD.matches(food));
    }

    @Test
    void testItemBrowserFilterCriteria() {
        ItemDefinition item1 = ItemDefinition.builder("magic:fire_crystal")
                .displayName("§cFire Crystal")
                .material(Material.EMERALD)
                .type(ItemType.MATERIAL)
                .build();

        ItemDefinition item2 = ItemDefinition.builder("weapon:fire_sword")
                .displayName("§6Fire Sword")
                .material(Material.DIAMOND_SWORD)
                .type(ItemType.WEAPON)
                .build();

        ItemDefinition item3 = ItemDefinition.builder("haohan:quantum_core")
                .displayName("§bQuantum Core")
                .material(Material.NETHER_STAR)
                .type(ItemType.MACHINE_COMPONENT)
                .build();

        // Filter: default / all
        ItemBrowserFilter defaultFilter = ItemBrowserFilter.empty();
        assertTrue(defaultFilter.matches(item1));
        assertTrue(defaultFilter.matches(item2));
        assertTrue(defaultFilter.matches(item3));

        // Filter: by namespace "magic"
        ItemBrowserFilter magicFilter = new ItemBrowserFilter(null, "magic", ItemCategory.ALL);
        assertTrue(magicFilter.matches(item1));
        assertFalse(magicFilter.matches(item2));
        assertFalse(magicFilter.matches(item3));

        // Filter: by keyword "fire"
        ItemBrowserFilter fireFilter = new ItemBrowserFilter("fire", null, ItemCategory.ALL);
        assertTrue(fireFilter.matches(item1));
        assertTrue(fireFilter.matches(item2));
        assertFalse(fireFilter.matches(item3));

        // Filter: keyword "fire" + category WEAPONS
        ItemBrowserFilter fireWeaponFilter = new ItemBrowserFilter("fire", null, ItemCategory.WEAPONS);
        assertFalse(fireWeaponFilter.matches(item1)); // is Material, not Weapon
        assertTrue(fireWeaponFilter.matches(item2));  // is Weapon and has "fire"

        // Filter: category MACHINES
        ItemBrowserFilter machineFilter = new ItemBrowserFilter(null, null, ItemCategory.MACHINES);
        assertFalse(machineFilter.matches(item1));
        assertFalse(machineFilter.matches(item2));
        assertTrue(machineFilter.matches(item3));
    }

    @Test
    void testRecipeDefinitionTypes() {
        RecipeDefinition smithing = new RecipeDefinition(
                "smithing:upgrade_sword",
                RecipeType.SMITHING,
                List.of(
                        new Ingredient.ItemIngredient("minecraft:netherite_upgrade_smithing_template", 1),
                        new Ingredient.ItemIngredient("minecraft:diamond_sword", 1),
                        new Ingredient.ItemIngredient("minecraft:netherite_ingot", 1)
                ),
                new ItemResult("weapon:netherite_fire_sword", 1)
        );

        assertEquals(RecipeType.SMITHING, smithing.getType());
        assertEquals(3, smithing.getIngredients().size());
        assertEquals("weapon:netherite_fire_sword", smithing.getResult().item());

        // Cooking / Smelting recipe
        RecipeDefinition smelting = new RecipeDefinition(
                "smelting:iron_plate",
                RecipeType.SMELTING,
                List.of(new Ingredient.ItemIngredient("minecraft:iron_ingot", 1)),
                new ItemResult("machine:iron_plate", 1),
                0.7f,
                160
        );
        assertEquals(RecipeType.SMELTING, smelting.getType());
        assertEquals(160, smelting.getCookingTime());
        assertEquals(0.7f, smelting.getExperience());
    }

    @Test
    void testFilterWithMethods() {
        ItemBrowserFilter filter = ItemBrowserFilter.empty();
        assertTrue(filter.isDefault());

        ItemBrowserFilter f1 = filter.withQuery("ruby");
        assertEquals("ruby", f1.getQuery());
        assertEquals(ItemCategory.ALL, f1.getCategory());
        assertNull(f1.getNamespace());

        ItemBrowserFilter f2 = f1.withCategory(ItemCategory.TOOLS);
        assertEquals("ruby", f2.getQuery());
        assertEquals(ItemCategory.TOOLS, f2.getCategory());

        ItemBrowserFilter f3 = f2.withNamespace("weapon");
        assertEquals("weapon", f3.getNamespace());
        assertFalse(f3.isDefault());
    }

    @Test
    void testCurrencyAndSpecialCategoryMatching() {
        ItemDefinition coin = ItemDefinition.builder("eco:gold_coin")
                .material(Material.GOLD_INGOT)
                .type(ItemType.CURRENCY)
                .build();
        assertTrue(ItemCategory.CURRENCY.matches(coin));

        ItemDefinition questKey = ItemDefinition.builder("quest:boss_key")
                .material(Material.TRIPWIRE_HOOK)
                .type(ItemType.SPECIAL)
                .build();
        assertTrue(ItemCategory.SPECIAL.matches(questKey));
    }
}
