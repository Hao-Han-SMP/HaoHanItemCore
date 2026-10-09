package vn.haohan.itemcore.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.MiningComponent;
import vn.haohan.itemcore.internal.block.MiningSpeedCalculator;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.recipe.TestItemService;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class MiningSpeedCalculatorTest {

    private DefaultItemRegistry itemRegistry;
    private TestItemService itemService;

    @BeforeEach
    public void setUp() {
        itemRegistry = new DefaultItemRegistry(Logger.getLogger("SpeedTest"));
        itemService = new TestItemService(itemRegistry);
        HaoHanItemCore.setInstance(new HaoHanItemCore(
                itemRegistry, null, itemService,
                null, null, null, null));
    }

    @Test
    public void testUnbreakableAndInstantBreak() {
        CustomBlockDefinition unbreakable = CustomBlockDefinition.builder("custom:bedrock")
                .unbreakable()
                .build();
        assertEquals(0.0f, MiningSpeedCalculator.calculateProgressPerTick(null, null, unbreakable), 0.0001f);

        CustomBlockDefinition instant = CustomBlockDefinition.builder("custom:slime")
                .instantBreak()
                .build();
        assertEquals(1.0f, MiningSpeedCalculator.calculateProgressPerTick(null, null, instant), 0.0001f);
    }

    @Test
    public void testBaseSpeedWithHandAndTools() {
        // 2 seconds base time = 40 ticks
        CustomBlockDefinition block = CustomBlockDefinition.builder("custom:stone")
                .hardness(2.0f)
                .build();

        // Hand speed = 1.0 -> 1.0 / 40 = 0.025 per tick
        float handProgress = MiningSpeedCalculator.calculateProgressPerTick(null, null, block);
        assertEquals(0.025f, handProgress, 0.0001f);

        // Diamond pickaxe speed = 8.0 -> 8.0 / 40 = 0.2 per tick
        ItemStack diamondPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        float diamondProgress = MiningSpeedCalculator.calculateProgressPerTick(null, diamondPick, block);
        assertEquals(0.2f, diamondProgress, 0.0001f);

        // Netherite pickaxe speed = 9.0 -> 9.0 / 40 = 0.225 per tick
        ItemStack netheritePick = TestItemService.mockItem(Material.NETHERITE_PICKAXE);
        float netheriteProgress = MiningSpeedCalculator.calculateProgressPerTick(null, netheritePick, block);
        assertEquals(0.225f, netheriteProgress, 0.0001f);
    }

    @Test
    public void testCustomItemMiningBonus() {
        CustomBlockDefinition block = CustomBlockDefinition.builder("custom:ore")
                .hardness(2.0f)
                .build();

        // Custom drill with extra mining_speed property = 5.0
        itemRegistry.register(ItemDefinition.builder("custom:drill")
                .material(Material.DIAMOND_PICKAXE)
                .displayName("Drill")
                .property("mining_speed", 5.0)
                .build());

        ItemStack drill = itemService.create("custom:drill");
        // Base diamond pick = 8.0 + custom 5.0 = 13.0 -> 13.0 / 40 = 0.325 per tick
        float drillProgress = MiningSpeedCalculator.calculateProgressPerTick(null, drill, block);
        assertEquals(0.325f, drillProgress, 0.0001f);
    }

    @Test
    public void testSwordAndWrongToolsDoNotMineFasterThanPickaxe() {
        // 2 seconds base time = 40 ticks, requires PICKAXE
        CustomBlockDefinition block = CustomBlockDefinition.builder("custom:ruby_ore")
                .hardness(2.0f)
                .toolType(vn.haohan.itemcore.api.block.ToolType.PICKAXE)
                .build();

        // Diamond Sword is NOT a pickaxe -> speed multiplier must be 1.0f (hand speed = 0.025f)
        ItemStack diamondSword = TestItemService.mockItem(Material.DIAMOND_SWORD);
        float swordProgress = MiningSpeedCalculator.calculateProgressPerTick(null, diamondSword, block);
        assertEquals(0.025f, swordProgress, 0.0001f, "Diamond sword must not receive tool tier speed on a pickaxe block");

        // Wooden Pickaxe speed = 2.0 -> 2.0 / 40 = 0.05 per tick (faster than diamond sword!)
        ItemStack woodPick = TestItemService.mockItem(Material.WOODEN_PICKAXE);
        float woodPickProgress = MiningSpeedCalculator.calculateProgressPerTick(null, woodPick, block);
        assertEquals(0.05f, woodPickProgress, 0.0001f);
        assertTrue(woodPickProgress > swordProgress, "Wooden pickaxe must mine faster than a diamond sword on a pickaxe block");

        // Diamond Pickaxe speed = 8.0 -> 8.0 / 40 = 0.2 per tick
        ItemStack diamondPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        float diamondPickProgress = MiningSpeedCalculator.calculateProgressPerTick(null, diamondPick, block);
        assertEquals(0.2f, diamondPickProgress, 0.0001f);
    }
}

