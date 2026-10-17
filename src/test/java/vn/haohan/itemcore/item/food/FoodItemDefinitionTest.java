package vn.haohan.itemcore.item.food;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.item.ItemCategory;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.api.item.food.FoodComponent;

import static org.junit.jupiter.api.Assertions.*;

public class FoodItemDefinitionTest {

    @Test
    void testItemDefinitionFoodBuilder() {
        ItemDefinition def = ItemDefinition.builder("custom:magic_apple")
                .material(Material.GOLDEN_APPLE)
                .food(food -> food
                        .nutrition(10)
                        .saturation(15.0f)
                        .canAlwaysEat(true)
                )
                .build();

        assertEquals(ItemType.FOOD, def.getType());
        FoodComponent foodComp = def.getComponents().stream()
                .filter(c -> c instanceof FoodComponent)
                .map(c -> (FoodComponent) c)
                .findFirst()
                .orElse(null);

        assertNotNull(foodComp);
        assertEquals(10, foodComp.nutrition());
        assertEquals(15.0f, foodComp.saturation(), 0.001f);
        assertTrue(foodComp.canAlwaysEat());
        assertTrue(ItemCategory.FOOD.matches(def));
    }

    @Test
    void testNonFoodMaterialBecomesFoodCategoryWithFoodComponent() {
        ItemDefinition def = ItemDefinition.builder("custom:edible_stick")
                .material(Material.STICK)
                .food(FoodComponent.builder().nutrition(2).build())
                .build();

        assertEquals(ItemType.FOOD, def.getType());
        assertTrue(ItemCategory.FOOD.matches(def));
    }
}
