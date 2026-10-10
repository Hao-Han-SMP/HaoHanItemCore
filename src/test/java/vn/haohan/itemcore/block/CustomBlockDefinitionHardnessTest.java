package vn.haohan.itemcore.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.recipe.TestItemService;

import java.util.Set;

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
    public void testMineableWithToolTypeAndTier() {
        CustomBlockDefinition def = CustomBlockDefinition.builder("custom:ore")
                .hardness(5.0f)
                .mineableWith(ToolType.PICKAXE, ToolTier.IRON)
                .build();

        assertEquals(Set.of(ToolType.PICKAXE), def.getHarvestToolTypes());
        assertEquals(ToolTier.IRON, def.getRequiredTier());

        ItemStack woodPick = TestItemService.mockItem(Material.WOODEN_PICKAXE);
        ItemStack ironPick = TestItemService.mockItem(Material.IRON_PICKAXE);
        ItemStack diamondPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        ItemStack diamondSword = TestItemService.mockItem(Material.DIAMOND_SWORD);

        assertFalse(def.isEffectiveTool(null));
        assertFalse(def.isEffectiveTool(woodPick)); // Tier too low
        assertFalse(def.isEffectiveTool(diamondSword)); // Wrong tool type
        assertTrue(def.isEffectiveTool(ironPick)); // Valid tier and tool
        assertTrue(def.isEffectiveTool(diamondPick)); // Higher tier and tool
    }

    @Test
    public void testMineableWithMultipleTools() {
        CustomBlockDefinition def = CustomBlockDefinition.builder("custom:clay_stone")
                .hardness(3.0f)
                .mineableWith(Set.of(ToolType.PICKAXE, ToolType.SHOVEL), ToolTier.STONE)
                .build();

        assertEquals(2, def.getHarvestToolTypes().size());
        assertTrue(def.getHarvestToolTypes().contains(ToolType.PICKAXE));
        assertTrue(def.getHarvestToolTypes().contains(ToolType.SHOVEL));

        ItemStack woodShovel = TestItemService.mockItem(Material.WOODEN_SHOVEL);
        ItemStack stoneShovel = TestItemService.mockItem(Material.STONE_SHOVEL);
        ItemStack ironPick = TestItemService.mockItem(Material.IRON_PICKAXE);
        ItemStack diamondAxe = TestItemService.mockItem(Material.DIAMOND_AXE);

        assertFalse(def.isEffectiveTool(woodShovel));
        assertTrue(def.isEffectiveTool(stoneShovel));
        assertTrue(def.isEffectiveTool(ironPick));
        assertFalse(def.isEffectiveTool(diamondAxe));
    }

    @Test
    public void testHandMiningEffectiveness() {
        // Block 1: Free block (no tools specified) -> Bare hand is effective
        CustomBlockDefinition freeBlock = CustomBlockDefinition.builder("custom:free")
                .hardness(2.0f)
                .build();
        assertTrue(freeBlock.isEffectiveTool(null));
        assertTrue(freeBlock.isEffectiveTool(TestItemService.mockItem(Material.AIR)));

        // Block 2: Requires Hand explicitly -> Bare hand is effective
        CustomBlockDefinition handBlock = CustomBlockDefinition.builder("custom:leaves")
                .hardness(1.0f)
                .mineableWith(ToolType.HAND, null)
                .build();
        assertTrue(handBlock.isEffectiveTool(null));
        assertTrue(handBlock.isEffectiveTool(TestItemService.mockItem(Material.AIR)));

        // Block 3: Requires Pickaxe -> Bare hand is NOT effective
        CustomBlockDefinition oreBlock = CustomBlockDefinition.builder("custom:ore")
                .hardness(5.0f)
                .mineableWith(ToolType.PICKAXE, ToolTier.STONE)
                .build();
        assertFalse(oreBlock.isEffectiveTool(null));
        assertFalse(oreBlock.isEffectiveTool(TestItemService.mockItem(Material.AIR)));
    }
}
