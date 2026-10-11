package vn.haohan.itemcore.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.ToolTier;
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
    public void testBaseSpeedWithHandAndToolsWhenNoPreferredTool() {
        // 2 seconds base time = 40 ticks, no preferred tool specified
        CustomBlockDefinition block = CustomBlockDefinition.builder("custom:stone")
                .hardness(2.0f)
                .build();

        // Hand speed = 1.0 -> 1.0 / 40 = 0.025 per tick
        float handProgress = MiningSpeedCalculator.calculateProgressPerTick(null, null, block);
        assertEquals(0.025f, handProgress, 0.0001f);

        // When no preferred tool is configured, ALL tools and hand obey exact configured break time (0.025 per tick)
        ItemStack diamondPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        float diamondProgress = MiningSpeedCalculator.calculateProgressPerTick(null, diamondPick, block);
        assertEquals(0.025f, diamondProgress, 0.0001f);

        ItemStack sword = TestItemService.mockItem(Material.DIAMOND_SWORD);
        float swordProgress = MiningSpeedCalculator.calculateProgressPerTick(null, sword, block);
        assertEquals(0.025f, swordProgress, 0.0001f);

        ItemStack netheritePick = TestItemService.mockItem(Material.NETHERITE_PICKAXE);
        float netheriteProgress = MiningSpeedCalculator.calculateProgressPerTick(null, netheritePick, block);
        assertEquals(0.025f, netheriteProgress, 0.0001f);
    }

    @Test
    public void testToolSpeedMultiplierWhenPreferredToolSpecified() {
        // Hardness 1.5 (Stone), requires PICKAXE -> Vanilla base ticks = 1.5 * 30 = 45 ticks
        CustomBlockDefinition block = CustomBlockDefinition.builder("custom:stone")
                .hardness(1.5f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE)
                .build();

        // Diamond pickaxe speed = 8.0 -> 8.0 / 45 = 0.17777778 per tick (takes 6 ticks)
        ItemStack diamondPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        float diamondProgress = MiningSpeedCalculator.calculateProgressPerTick(null, diamondPick, block);
        assertEquals(8.0f / 45.0f, diamondProgress, 0.0001f);

        // Iron pickaxe speed = 6.0 -> 6.0 / 45 = 0.13333333 per tick (takes 8 ticks)
        ItemStack ironPick = TestItemService.mockItem(Material.IRON_PICKAXE);
        float ironProgress = MiningSpeedCalculator.calculateProgressPerTick(null, ironPick, block);
        assertEquals(6.0f / 45.0f, ironProgress, 0.0001f);

        // Netherite pickaxe speed = 9.0 -> 9.0 / 45 = 0.2 per tick (takes 5 ticks)
        ItemStack netheritePick = TestItemService.mockItem(Material.NETHERITE_PICKAXE);
        float netheriteProgress = MiningSpeedCalculator.calculateProgressPerTick(null, netheritePick, block);
        assertEquals(9.0f / 45.0f, netheriteProgress, 0.0001f);
    }

    @Test
    public void testCustomItemMiningSpeedOverridesTierWithoutDoubleStacking() {
        // Hardness 1.5 (Stone), requires PICKAXE
        CustomBlockDefinition block = CustomBlockDefinition.builder("custom:stone")
                .hardness(1.5f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE)
                .build();

        // Custom drill on DIAMOND_PICKAXE with mining_speed property = 8.0 (như Metallurgy)
        itemRegistry.register(ItemDefinition.builder("custom:drill")
                .material(Material.DIAMOND_PICKAXE)
                .displayName("Drill")
                .property("mining_speed", 8.0)
                .build());

        ItemStack drill = itemService.create("custom:drill");
        // Base diamond pick = 8.0, custom mining_speed = 8.0 -> Phải KHÔNG bị cộng dồn thành 16.0!
        // Tốc độ phải đúng là 8.0 -> 8.0 / 45 = 0.17777778 per tick
        float drillProgress = MiningSpeedCalculator.calculateProgressPerTick(null, drill, block);
        assertEquals(8.0f / 45.0f, drillProgress, 0.0001f);
    }

    @Test
    public void testSwordAndWrongToolsDoNotMineFasterThanPickaxe() {
        // Hardness 2.0, requires PICKAXE -> Base ticks = 2.0 * 30 = 60 ticks
        CustomBlockDefinition block = CustomBlockDefinition.builder("custom:ruby_ore")
                .hardness(2.0f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE, ToolTier.WOOD)
                .build();

        // Diamond Sword is NOT a pickaxe -> cannot mine -> progress must be 0.0f
        ItemStack diamondSword = TestItemService.mockItem(Material.DIAMOND_SWORD);
        float swordProgress = MiningSpeedCalculator.calculateProgressPerTick(null, diamondSword, block);
        assertEquals(0.0f, swordProgress, 0.0001f, "Diamond sword cannot mine a block requiring pickaxe");

        // Wooden Pickaxe speed = 2.0 -> 2.0 / 60 = 0.03333333 per tick
        ItemStack woodPick = TestItemService.mockItem(Material.WOODEN_PICKAXE);
        float woodPickProgress = MiningSpeedCalculator.calculateProgressPerTick(null, woodPick, block);
        assertEquals(2.0f / 60.0f, woodPickProgress, 0.0001f);
        assertTrue(woodPickProgress > swordProgress);

        // Diamond Pickaxe speed = 8.0 -> 8.0 / 60 = 0.13333333 per tick
        ItemStack diamondPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        float diamondPickProgress = MiningSpeedCalculator.calculateProgressPerTick(null, diamondPick, block);
        assertEquals(8.0f / 60.0f, diamondPickProgress, 0.0001f);
    }

    @Test
    public void testTierRequirementSpeedEnforcement() {
        // Block requires IRON pickaxe (tier 3) or higher, hardness 5.0 (150 ticks)
        CustomBlockDefinition block = CustomBlockDefinition.builder("custom:deep_ore")
                .hardness(5.0f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE, ToolTier.IRON)
                .build();

        // 1. Hand -> Block requires pickaxe -> Hand cannot mine -> 0.0f
        float hand = MiningSpeedCalculator.calculateProgressPerTick(null, null, block);
        assertEquals(0.0f, hand, 0.0001f);

        // 2. Wood Pickaxe (Tier 1) -> Low tier -> Not effective -> 0.0f
        ItemStack woodPick = TestItemService.mockItem(Material.WOODEN_PICKAXE);
        float woodProgress = MiningSpeedCalculator.calculateProgressPerTick(null, woodPick, block);
        assertEquals(0.0f, woodProgress, 0.0001f);

        // 3. Stone Pickaxe (Tier 2) -> Low tier -> Not effective -> 0.0f
        ItemStack stonePick = TestItemService.mockItem(Material.STONE_PICKAXE);
        float stoneProgress = MiningSpeedCalculator.calculateProgressPerTick(null, stonePick, block);
        assertEquals(0.0f, stoneProgress, 0.0001f);

        // 4. Iron Pickaxe (Tier 3) -> Effective! Base multiplier 6.0x -> 6.0 / 150 = 0.04 per tick
        ItemStack ironPick = TestItemService.mockItem(Material.IRON_PICKAXE);
        float ironProgress = MiningSpeedCalculator.calculateProgressPerTick(null, ironPick, block);
        assertEquals(6.0f / 150.0f, ironProgress, 0.0001f);

        // 5. Diamond Pickaxe (Tier 4) -> Effective! Base multiplier 8.0x -> 8.0 / 150 = 0.05333333 per tick
        ItemStack diamondPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        float diamondProgress = MiningSpeedCalculator.calculateProgressPerTick(null, diamondPick, block);
        assertEquals(8.0f / 150.0f, diamondProgress, 0.0001f);
    }

    @Test
    public void testMetallurgyOresAndPickaxesIntegration() {
        // Mô phỏng Borax Ore (Hardness 3.0), yêu cầu Pickaxe Tier STONE (speed 4.0)
        CustomBlockDefinition boraxOre = CustomBlockDefinition.builder("metallurgy:borax_ore")
                .hardness(3.0f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE, ToolTier.STONE)
                .build();

        // 1. Wooden Pickaxe -> Không đủ tier (cần Stone) -> 0.0f
        ItemStack woodPick = TestItemService.mockItem(Material.WOODEN_PICKAXE);
        assertEquals(0.0f, MiningSpeedCalculator.calculateProgressPerTick(null, woodPick, boraxOre), 0.0001f);

        // 2. Stone Pickaxe (Tier Stone, speed 4.0) -> Base ticks = 3.0 * 30 = 90 -> 4.0 / 90
        ItemStack stonePick = TestItemService.mockItem(Material.STONE_PICKAXE);
        assertEquals(4.0f / 90.0f, MiningSpeedCalculator.calculateProgressPerTick(null, stonePick, boraxOre), 0.0001f);

        // 3. Mithril Ore (Hardness 4.0), yêu cầu Pickaxe Tier DIAMOND (speed 8.0)
        CustomBlockDefinition mithrilOre = CustomBlockDefinition.builder("metallurgy:mithril_ore")
                .hardness(4.0f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE, ToolTier.DIAMOND)
                .build();

        // Iron pickaxe không đào được Mithril Ore
        ItemStack ironPick = TestItemService.mockItem(Material.IRON_PICKAXE);
        assertEquals(0.0f, MiningSpeedCalculator.calculateProgressPerTick(null, ironPick, mithrilOre), 0.0001f);

        // Diamond Pickaxe (speed 8.0) -> Base ticks = 4.0 * 30 = 120 -> 8.0 / 120 = 0.06666667 (15 ticks)
        ItemStack diamondPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        assertEquals(8.0f / 120.0f, MiningSpeedCalculator.calculateProgressPerTick(null, diamondPick, mithrilOre), 0.0001f);

        // Metallurgy Custom Soulsteel Pickaxe với custom mining_speed = 11.0
        itemRegistry.register(ItemDefinition.builder("metallurgy:soulsteel_pickaxe")
                .material(Material.NETHERITE_PICKAXE)
                .displayName("Soulsteel Pickaxe")
                .property("mining_speed", 11.0)
                .build());
        ItemStack soulsteelPick = itemService.create("metallurgy:soulsteel_pickaxe");
        // Tốc độ phải là 11.0 / 120, không bị cộng dồn netherite tier (9.0) + custom (11.0) = 20.0
        assertEquals(11.0f / 120.0f, MiningSpeedCalculator.calculateProgressPerTick(null, soulsteelPick, mithrilOre), 0.0001f);
    }
}
