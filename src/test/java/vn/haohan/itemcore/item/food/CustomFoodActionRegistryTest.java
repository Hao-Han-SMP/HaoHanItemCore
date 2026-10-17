package vn.haohan.itemcore.item.food;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.item.food.CustomFoodAction;
import vn.haohan.itemcore.api.item.food.CustomFoodActionRegistry;
import vn.haohan.itemcore.internal.item.food.DefaultCustomFoodActionRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class CustomFoodActionRegistryTest {

    private CustomFoodActionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultCustomFoodActionRegistry();
    }

    @Test
    void testRegisterAndRetrieveAction() {
        AtomicBoolean executed = new AtomicBoolean(false);
        CustomFoodAction action = ctx -> executed.set(true);

        registry.register("heal_mana", action);

        assertTrue(registry.has("heal_mana"));
        assertSame(action, registry.get("heal_mana"));

        registry.get("heal_mana").execute(null);
        assertTrue(executed.get());
    }

    @Test
    void testUnregisterAction() {
        registry.register("temp_buff", ctx -> {});
        assertTrue(registry.has("temp_buff"));

        registry.unregister("temp_buff");
        assertFalse(registry.has("temp_buff"));
        assertNull(registry.get("temp_buff"));
    }

    @Test
    void testRegisterInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> registry.register(null, ctx -> {}));
        assertThrows(IllegalArgumentException.class, () -> registry.register("", ctx -> {}));
        assertThrows(NullPointerException.class, () -> registry.register("valid_id", null));
    }
}
