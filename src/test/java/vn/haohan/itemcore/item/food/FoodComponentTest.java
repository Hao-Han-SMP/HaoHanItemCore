package vn.haohan.itemcore.item.food;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.item.food.FoodComponent;
import vn.haohan.itemcore.api.item.food.FoodEffectEntry;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class FoodComponentTest {

    @Test
    void testFoodComponentBuilderDefaults() {
        FoodComponent food = FoodComponent.builder().build();

        assertEquals(4, food.nutrition());
        assertEquals(2.5f, food.saturation(), 0.001f);
        assertFalse(food.canAlwaysEat());
        assertEquals(1.6f, food.consumeSeconds(), 0.001f);
        assertTrue(food.potionEffects().isEmpty());
        assertTrue(food.customActions().isEmpty());
    }

    @Test
    void testFoodComponentCustomValues() {
        AtomicBoolean actionTriggered = new AtomicBoolean(false);
        PotionEffect effect = new PotionEffect(PotionEffectType.REGENERATION, 100, 1);

        FoodComponent food = FoodComponent.builder()
                .nutrition(8)
                .saturation(12.8f)
                .canAlwaysEat(true)
                .consumeSeconds(2.0f)
                .addPotionEffect(effect, 0.8f)
                .onConsume(ctx -> actionTriggered.set(true))
                .build();

        assertEquals(8, food.nutrition());
        assertEquals(12.8f, food.saturation(), 0.001f);
        assertTrue(food.canAlwaysEat());
        assertEquals(2.0f, food.consumeSeconds(), 0.001f);
        assertEquals(1, food.potionEffects().size());
        assertEquals(effect, food.potionEffects().get(0).effect());
        assertEquals(0.8f, food.potionEffects().get(0).probability(), 0.001f);
        assertEquals(1, food.customActions().size());

        food.customActions().get(0).execute(null);
        assertTrue(actionTriggered.get());
    }

    @Test
    void testInvalidConsumeSecondsThrows() {
        assertThrows(IllegalArgumentException.class, () -> FoodComponent.builder().consumeSeconds(0).build());
        assertThrows(IllegalArgumentException.class, () -> FoodComponent.builder().consumeSeconds(-1.5f).build());
    }

    @Test
    void testApplyToItemStack() {
        ItemStack item = org.mockito.Mockito.mock(ItemStack.class);
        FoodComponent food = FoodComponent.builder()
                .nutrition(6)
                .saturation(7.5f)
                .canAlwaysEat(true)
                .consumeSeconds(1.2f)
                .build();

        food.apply(item, null);

        org.mockito.Mockito.verify(item).setData(
                org.mockito.Mockito.eq(DataComponentTypes.FOOD),
                org.mockito.Mockito.argThat((FoodProperties props) ->
                        props.nutrition() == 6 &&
                        Math.abs(props.saturation() - 7.5f) < 0.001f &&
                        props.canAlwaysEat()
                )
        );

        org.mockito.Mockito.verify(item).setData(
                org.mockito.Mockito.eq(DataComponentTypes.CONSUMABLE),
                org.mockito.Mockito.argThat((Consumable cons) ->
                        Math.abs(cons.consumeSeconds() - 1.2f) < 0.001f
                )
        );
    }
}
