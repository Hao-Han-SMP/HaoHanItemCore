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

import static org.junit.jupiter.api.Assertions.*;

class MiningSpeedStrategyTest {

    @Test
    @DisplayName("NativeMiningStrategy calculates correct attribute multiplier from seconds hardness")
    void testAttributeFactorCalculation() {
        // Vanilla NoteBlock base break time = 0.8 seconds with base tool
        // If target hardness = 1.6s, attribute factor should be 0.8 / 1.6 = 0.5
        float hardness = 1.6f;
        double factor = NativeMiningStrategy.calculateAttributeFactor(0.8f, hardness);
        assertEquals(0.5, factor, 0.001);

        // If target hardness = 0.4s (faster break), attribute factor should be 0.8 / 0.4 = 2.0
        factor = NativeMiningStrategy.calculateAttributeFactor(0.8f, 0.4f);
        assertEquals(2.0, factor, 0.001);

        // Instant break (hardness <= 0.0) -> high factor (100.0)
        double instantFactor = NativeMiningStrategy.calculateAttributeFactor(0.8f, 0.0f);
        assertEquals(100.0, instantFactor, 0.001);
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

        // 1. onStartDigging with effective tool
        strategy.onStartDigging(mockPlayer, mockBlock, def, null);

        // Should first clean previous modifier
        Mockito.verify(mockAttr, Mockito.atLeastOnce()).removeModifier(Mockito.any(NamespacedKey.class));

        // Should add transient modifier with factor - 1.0 (0.5 - 1.0 = -0.5)
        ArgumentCaptor<AttributeModifier> modifierCaptor = ArgumentCaptor.forClass(AttributeModifier.class);
        Mockito.verify(mockAttr).addTransientModifier(modifierCaptor.capture());
        AttributeModifier captured = modifierCaptor.getValue();
        assertEquals(-0.5, captured.getAmount(), 0.001);
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
        // Should heavily penalize speed (-0.99)
        assertEquals(-0.99, captured.getAmount(), 0.001);
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
}
