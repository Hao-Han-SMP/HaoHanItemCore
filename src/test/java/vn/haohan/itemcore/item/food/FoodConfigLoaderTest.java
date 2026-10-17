package vn.haohan.itemcore.item.food;

import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.api.item.food.CustomFoodActionRegistry;
import vn.haohan.itemcore.api.item.food.FoodComponent;
import vn.haohan.itemcore.internal.config.ItemConfigLoader;
import vn.haohan.itemcore.internal.item.food.DefaultCustomFoodActionRegistry;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class FoodConfigLoaderTest {

    private CustomFoodActionRegistry actionRegistry;
    private ItemConfigLoader loader;
    private ItemRegistry registry;

    @BeforeEach
    void setUp() {
        actionRegistry = new DefaultCustomFoodActionRegistry();
        loader = new ItemConfigLoader(Logger.getLogger("TestLoader"), actionRegistry);
        registry = Mockito.mock(ItemRegistry.class);
    }

    @Test
    void testParseYamlWithFoodSection() throws Exception {
        AtomicBoolean actionTriggered = new AtomicBoolean(false);
        actionRegistry.register("mana_restore", ctx -> actionTriggered.set(true));

        String yaml = """
            rpg:
              mana_potion:
                material: POTION
                display-name: "§bMana Potion"
                type: FOOD
                food:
                  nutrition: 3
                  saturation: 4.5
                  can-always-eat: true
                  consume-seconds: 1.0
                  custom-effects:
                    - "mana_restore"
                  potion-effects:
                    - type: REGENERATION
                      duration: 100
                      amplifier: 1
                      probability: 1.0
            """;

        File tempFile = File.createTempFile("items_food", ".yml");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), yaml);

        int count = loader.loadFile(tempFile, registry);
        assertEquals(1, count);

        org.mockito.ArgumentCaptor<ItemDefinition> captor = org.mockito.ArgumentCaptor.forClass(ItemDefinition.class);
        Mockito.verify(registry).register(captor.capture());

        ItemDefinition def = captor.getValue();
        assertEquals("rpg:mana_potion", def.getId());
        assertEquals(ItemType.FOOD, def.getType());

        FoodComponent food = def.getComponents().stream()
                .filter(c -> c instanceof FoodComponent)
                .map(c -> (FoodComponent) c)
                .findFirst()
                .orElse(null);

        assertNotNull(food);
        assertEquals(3, food.nutrition());
        assertEquals(4.5f, food.saturation(), 0.001f);
        assertTrue(food.canAlwaysEat());
        assertEquals(1.0f, food.consumeSeconds(), 0.001f);
        assertEquals(1, food.potionEffects().size());
        assertEquals(PotionEffectType.REGENERATION, food.potionEffects().get(0).effect().getType());
        assertEquals(1, food.customActions().size());

        food.customActions().get(0).execute(null);
        assertTrue(actionTriggered.get());
    }
}
