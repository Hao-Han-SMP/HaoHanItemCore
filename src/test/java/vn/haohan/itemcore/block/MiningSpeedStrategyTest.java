package vn.haohan.itemcore.block;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.NativeCustomBlock;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.internal.block.mining.AlternativeMiningStrategy;
import vn.haohan.itemcore.internal.block.mining.NativeMiningStrategy;
import vn.haohan.itemcore.recipe.TestItemService;

import static org.junit.jupiter.api.Assertions.*;

class MiningSpeedStrategyTest {

    @Test
    @DisplayName("NativeMiningStrategy calculates client NoteBlock break time correctly per tool")
    void testClientNoteBlockBreakTime() {
        // Hand / null: 4.0s
        assertEquals(4.0f, NativeMiningStrategy.getClientNoteBlockBreakTime(null), 0.001f);

        // Pickaxe / Shovel: not preferred for wood NoteBlock -> 4.0s
        org.bukkit.inventory.ItemStack diamondPick = TestItemService.mockItem(org.bukkit.Material.DIAMOND_PICKAXE);
        assertEquals(4.0f, NativeMiningStrategy.getClientNoteBlockBreakTime(diamondPick), 0.001f);

        // Diamond Axe: base multiplier 8.0 -> 1.2 / 8.0 = 0.15s
        org.bukkit.inventory.ItemStack diamondAxe = TestItemService.mockItem(org.bukkit.Material.DIAMOND_AXE);
        assertEquals(0.15f, NativeMiningStrategy.getClientNoteBlockBreakTime(diamondAxe), 0.001f);

        // Wooden Axe: base multiplier 2.0 -> 1.2 / 2.0 = 0.6s
        org.bukkit.inventory.ItemStack woodenAxe = TestItemService.mockItem(org.bukkit.Material.WOODEN_AXE);
        assertEquals(0.6f, NativeMiningStrategy.getClientNoteBlockBreakTime(woodenAxe), 0.001f);
    }

    @Test
    @DisplayName("NativeMiningStrategy calculates correct attribute multiplier from client break time and target hardness")
    void testAttributeFactorCalculation() {
        // When client break time = 4.0s (e.g. hand or pickaxe on NoteBlock)
        // If target hardness = 2.0s, attribute factor should be 4.0 / 2.0 = 2.0
        float targetHardness = 2.0f;
        double factor = NativeMiningStrategy.calculateAttributeFactor(4.0f, targetHardness);
        assertEquals(2.0, factor, 0.001);

        // If target hardness = 8.0s, attribute factor should be 4.0 / 8.0 = 0.5
        factor = NativeMiningStrategy.calculateAttributeFactor(4.0f, 8.0f);
        assertEquals(0.5, factor, 0.001);

        // Instant break (hardness == 0.0) -> high factor (100.0)
        double instantFactor = NativeMiningStrategy.calculateAttributeFactor(4.0f, 0.0f);
        assertEquals(100.0, instantFactor, 0.001);

        // Unbreakable block (hardness < 0.0) -> heavy reduction factor (0.01)
        double unbreakableFactor = NativeMiningStrategy.calculateAttributeFactor(4.0f, -1.0f);
        assertEquals(0.01, unbreakableFactor, 0.001);
    }

    @Test
    @DisplayName("NativeMiningStrategy handles unbreakable blocks with penalty and returns -1.0s break time")
    void testNativeMiningStrategyUnbreakableBlockHandling() {
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        Mockito.when(mockPlugin.getName()).thenReturn("haohanitemcore");
        Mockito.when(mockPlugin.namespace()).thenReturn("haohanitemcore");

        NativeMiningStrategy strategy = new NativeMiningStrategy(mockPlugin);

        Player mockPlayer = Mockito.mock(Player.class);
        Block mockBlock = Mockito.mock(Block.class);
        AttributeInstance mockAttr = Mockito.mock(AttributeInstance.class);
        Mockito.when(mockPlayer.getAttribute(Mockito.any(Attribute.class))).thenReturn(mockAttr);

        NativeCustomBlock unbreakableDef = NativeCustomBlock.builder("custom:bedrock_ore")
                .unbreakable()
                .build();

        // 1. Break time must be -1.0f
        assertEquals(-1.0f, strategy.getBreakTimeSeconds(mockPlayer, null, unbreakableDef), 0.001f);

        // 2. Digging unbreakable block must apply -1.0 penalty (100% speed reduction)
        strategy.onStartDigging(mockPlayer, mockBlock, unbreakableDef, null);

        ArgumentCaptor<AttributeModifier> modifierCaptor = ArgumentCaptor.forClass(AttributeModifier.class);
        Mockito.verify(mockAttr).addTransientModifier(modifierCaptor.capture());
        AttributeModifier captured = modifierCaptor.getValue();
        assertEquals(-1.0, captured.getAmount(), 0.001);
        assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1, captured.getOperation());
    }

    @Test
    @DisplayName("NativeMiningStrategy removeModifier public cleanup hook delegates to onStopDigging")
    void testRemoveModifierHook() {
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        Mockito.when(mockPlugin.getName()).thenReturn("haohanitemcore");
        Mockito.when(mockPlugin.namespace()).thenReturn("haohanitemcore");

        NativeMiningStrategy strategy = new NativeMiningStrategy(mockPlugin);

        Player mockPlayer = Mockito.mock(Player.class);
        AttributeInstance mockAttr = Mockito.mock(AttributeInstance.class);
        Mockito.when(mockPlayer.getAttribute(Mockito.any(Attribute.class))).thenReturn(mockAttr);

        strategy.removeModifier(mockPlayer);
        Mockito.verify(mockAttr).removeModifier(Mockito.any(NamespacedKey.class));
    }

    @Test
    @DisplayName("NativeMiningStrategy applies transient attribute modifier on digging and removes on stop")
    void testNativeMiningStrategyDiggingLifecycle() {
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        Mockito.when(mockPlugin.getName()).thenReturn("haohanitemcore");
        Mockito.when(mockPlugin.namespace()).thenReturn("haohanitemcore");

        NativeMiningStrategy strategy = new NativeMiningStrategy(mockPlugin);

        Player mockPlayer = Mockito.mock(Player.class);
        Block mockBlock = Mockito.mock(Block.class);
        AttributeInstance mockAttr = Mockito.mock(AttributeInstance.class);

        Mockito.when(mockPlayer.getAttribute(Mockito.any(Attribute.class))).thenReturn(mockAttr);

        NativeCustomBlock def = NativeCustomBlock.builder("custom:ruby_ore")
                .hardness(1.6f)
                .build();

        // 1. onStartDigging with effective tool (hand mining a block with no tool requirement, hardness 1.6s)
        // Client NoteBlock break time with hand = 4.0s. Target hardness = 1.6s.
        // Factor = 4.0 / 1.6 = 2.5 -> modifier amount = 2.5 - 1.0 = 1.5
        strategy.onStartDigging(mockPlayer, mockBlock, def, null);

        // Should first clean previous modifier
        Mockito.verify(mockAttr, Mockito.atLeastOnce()).removeModifier(Mockito.any(NamespacedKey.class));

        // Should add transient modifier with factor - 1.0 (2.5 - 1.0 = 1.5)
        ArgumentCaptor<AttributeModifier> modifierCaptor = ArgumentCaptor.forClass(AttributeModifier.class);
        Mockito.verify(mockAttr).addTransientModifier(modifierCaptor.capture());
        AttributeModifier captured = modifierCaptor.getValue();
        assertEquals(1.5, captured.getAmount(), 0.001);
        assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1, captured.getOperation());

        // 2. onStopDigging
        strategy.onStopDigging(mockPlayer, mockBlock, def);
        Mockito.verify(mockAttr, Mockito.times(2)).removeModifier(Mockito.any(NamespacedKey.class));
    }

    @Test
    @DisplayName("NativeMiningStrategy heavily penalizes ineffective tools")
    void testNativeMiningStrategyIneffectiveToolPenalty() {
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        Mockito.when(mockPlugin.getName()).thenReturn("haohanitemcore");
        Mockito.when(mockPlugin.namespace()).thenReturn("haohanitemcore");

        NativeMiningStrategy strategy = new NativeMiningStrategy(mockPlugin);

        Player mockPlayer = Mockito.mock(Player.class);
        Block mockBlock = Mockito.mock(Block.class);
        AttributeInstance mockAttr = Mockito.mock(AttributeInstance.class);
        Mockito.when(mockPlayer.getAttribute(Mockito.any(Attribute.class))).thenReturn(mockAttr);

        // Block requires Pickaxe
        NativeCustomBlock def = NativeCustomBlock.builder("custom:hard_ore")
                .hardness(2.0f)
                .mineableWith(ToolType.PICKAXE, ToolTier.IRON)
                .build();

        // Tool is null / hand -> ineffective!
        strategy.onStartDigging(mockPlayer, mockBlock, def, null);

        ArgumentCaptor<AttributeModifier> modifierCaptor = ArgumentCaptor.forClass(AttributeModifier.class);
        Mockito.verify(mockAttr).addTransientModifier(modifierCaptor.capture());
        AttributeModifier captured = modifierCaptor.getValue();
        // Should completely penalize speed (-1.0 -> 0.0 speed)
        assertEquals(-1.0, captured.getAmount(), 0.001);
        assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1, captured.getOperation());
    }

    @Test
    @DisplayName("NativeMiningStrategy getBreakTimeSeconds returns def break time or 0.8s fallback")
    void testNativeBreakTime() {
        NativeMiningStrategy strategy = new NativeMiningStrategy();

        NativeCustomBlock def = NativeCustomBlock.builder("custom:wood")
                .hardness(3.5f)
                .build();

        assertEquals(3.5f, strategy.getBreakTimeSeconds(null, null, def), 0.001f);
        assertEquals(0.8f, strategy.getBreakTimeSeconds(null, null, null), 0.001f);
    }

    @Test
    @DisplayName("AlternativeMiningStrategy getBreakTimeSeconds delegates to MiningSpeedCalculator")
    void testAlternativeMiningStrategy() {
        AlternativeMiningStrategy strategy = new AlternativeMiningStrategy();

        // 2.0s hardness without preferred tool = 40 ticks, 0.025 progress per tick -> 2.0 seconds
        CustomBlockDefinition def = CustomBlockDefinition.builder("custom:alt_block")
                .hardness(2.0f)
                .build();

        float breakTime = strategy.getBreakTimeSeconds(null, null, def);
        assertEquals(2.0f, breakTime, 0.001f);

        // Unbreakable block -> -1.0s
        CustomBlockDefinition unbreakable = CustomBlockDefinition.builder("custom:alt_unbreakable")
                .unbreakable()
                .build();
        assertEquals(-1.0f, strategy.getBreakTimeSeconds(null, null, unbreakable), 0.001f);
    }

    @Test
    @DisplayName("MiningSpeedCalculator calculateSpeedMultiplier calculates base tool tier and efficiency enchantments")
    void testCalculateSpeedMultiplierWithToolAndEfficiency() {
        CustomBlockDefinition oreDef = CustomBlockDefinition.builder("custom:tin_ore")
                .hardness(3.0f)
                .mineableWith(ToolType.PICKAXE, ToolTier.STONE)
                .build();

        // 1. Diamond Pickaxe without enchantment -> 8.0 base multiplier
        org.bukkit.inventory.ItemStack diamondPick = TestItemService.mockItem(org.bukkit.Material.DIAMOND_PICKAXE);
        float diamondSpeed = vn.haohan.itemcore.internal.block.MiningSpeedCalculator.calculateSpeedMultiplier(null, diamondPick, oreDef);
        assertEquals(8.0f, diamondSpeed, 0.001f);

        // 2. Netherite Pickaxe without enchantment -> 9.0 base multiplier
        org.bukkit.inventory.ItemStack netheritePick = TestItemService.mockItem(org.bukkit.Material.NETHERITE_PICKAXE);
        float netheriteSpeed = vn.haohan.itemcore.internal.block.MiningSpeedCalculator.calculateSpeedMultiplier(null, netheritePick, oreDef);
        assertEquals(9.0f, netheriteSpeed, 0.001f);
    }

    @Test
    @DisplayName("NativeMiningStrategy calculates higher attribute factor for higher tool speeds and tiers")
    void testNativeMiningStrategyScalesWithToolTiersAndSpeed() {
        // Target hardness = 3.0s, NoteBlock client break time for pickaxe = 4.0s
        float hardness = 3.0f;
        float clientBreakTime = 4.0f;

        // Diamond tool speed = 8.0 -> factor = (4.0 / 3.0) * (8.0 / 1.5) = 1.3333 * 5.3333 = 7.1111
        double diamondFactor = NativeMiningStrategy.calculateAttributeFactor(clientBreakTime, hardness, 8.0f, true);
        assertEquals((4.0 / 3.0) * (8.0 / 1.5), diamondFactor, 0.001);

        // Netherite Eff 5 speed = 35.0 -> factor = (4.0 / 3.0) * (35.0 / 1.5) = 31.1111
        double netheriteEff5Factor = NativeMiningStrategy.calculateAttributeFactor(clientBreakTime, hardness, 35.0f, true);
        assertEquals((4.0 / 3.0) * (35.0 / 1.5), netheriteEff5Factor, 0.001);

        // Ratio of factors must equal ratio of speeds (35.0 / 8.0 = 4.375x faster)
        assertEquals(35.0 / 8.0, netheriteEff5Factor / diamondFactor, 0.001);
    }
}

