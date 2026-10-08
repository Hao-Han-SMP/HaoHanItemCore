package vn.haohan.itemcore.block;

import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.BlockDropProvider;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.MiningCondition;

import static org.junit.jupiter.api.Assertions.*;

public class CustomBlockDefinitionHardnessTest {

    @Test
    public void testDefaultHardness() {
        CustomBlockDefinition def = CustomBlockDefinition.builder("custom:test").build();
        assertEquals(0.0f, def.getHardness(), 0.001f);
        assertTrue(def.isInstantBreak());
        assertFalse(def.isUnbreakable());
        assertNotNull(def.getMiningCondition());
        assertNotNull(def.getDropProvider());
    }

    @Test
    public void testFloatHardness() {
        CustomBlockDefinition def = CustomBlockDefinition.builder("custom:test")
                .hardness(3.5f)
                .build();
        assertEquals(3.5f, def.getHardness(), 0.001f);
        assertEquals(3.5f, def.getBreakTimeSeconds(), 0.001f);
        assertFalse(def.isInstantBreak());
        assertFalse(def.isUnbreakable());
    }

    @Test
    public void testUnbreakable() {
        CustomBlockDefinition def1 = CustomBlockDefinition.builder("custom:test1")
                .unbreakable()
                .build();
        assertTrue(def1.isUnbreakable());
        assertFalse(def1.isInstantBreak());
        assertTrue(def1.getHardness() < 0);

        CustomBlockDefinition def2 = CustomBlockDefinition.builder("custom:test2")
                .hardness(true)
                .build();
        assertTrue(def2.isUnbreakable());
    }

    @Test
    public void testInstantBreak() {
        CustomBlockDefinition def1 = CustomBlockDefinition.builder("custom:test1")
                .hardness(3.0f)
                .instantBreak()
                .build();
        assertTrue(def1.isInstantBreak());
        assertFalse(def1.isUnbreakable());
        assertEquals(0.0f, def1.getHardness(), 0.001f);

        CustomBlockDefinition def2 = CustomBlockDefinition.builder("custom:test2")
                .hardness(false)
                .build();
        assertTrue(def2.isInstantBreak());
    }

    @Test
    public void testMiningConditionAndDropProvider() {
        MiningCondition condition = ctx -> true;
        BlockDropProvider dropProvider = ctx -> java.util.Collections.emptyList();

        CustomBlockDefinition def = CustomBlockDefinition.builder("custom:test")
                .miningCondition(condition)
                .dropProvider(dropProvider)
                .build();

        assertSame(condition, def.getMiningCondition());
        assertSame(dropProvider, def.getDropProvider());
    }
}
