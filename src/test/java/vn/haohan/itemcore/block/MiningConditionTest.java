package vn.haohan.itemcore.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.MiningCondition;
import vn.haohan.itemcore.api.block.MiningContext;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.recipe.TestItemService;

import static org.junit.jupiter.api.Assertions.*;

public class MiningConditionTest {

    private MiningContext createContext(ItemStack tool) {
        return new MiningContext(null, null, null, tool);
    }

    @Test
    public void testAlways() {
        MiningCondition condition = MiningCondition.always();
        assertTrue(condition.canHarvest(createContext(null)));
        assertTrue(condition.canHarvest(createContext(TestItemService.mockItem(Material.STICK))));
    }

    @Test
    public void testRequiresPickaxe() {
        MiningCondition condition = MiningCondition.requiresPickaxe();
        assertFalse(condition.canHarvest(createContext(null)));
        assertFalse(condition.canHarvest(createContext(TestItemService.mockItem(Material.DIAMOND_SWORD))));
        assertFalse(condition.canHarvest(createContext(TestItemService.mockItem(Material.AIR))));
        assertTrue(condition.canHarvest(createContext(TestItemService.mockItem(Material.WOODEN_PICKAXE))));
        assertTrue(condition.canHarvest(createContext(TestItemService.mockItem(Material.DIAMOND_PICKAXE))));
    }

    @Test
    public void testMinVanillaTier() {
        MiningCondition ironTier = MiningCondition.minVanillaTier(ToolTier.IRON);
        assertFalse(ironTier.canHarvest(createContext(null)));
        assertFalse(ironTier.canHarvest(createContext(TestItemService.mockItem(Material.WOODEN_PICKAXE))));
        assertFalse(ironTier.canHarvest(createContext(TestItemService.mockItem(Material.STONE_PICKAXE))));
        assertTrue(ironTier.canHarvest(createContext(TestItemService.mockItem(Material.IRON_PICKAXE))));
        assertTrue(ironTier.canHarvest(createContext(TestItemService.mockItem(Material.DIAMOND_PICKAXE))));
        assertTrue(ironTier.canHarvest(createContext(TestItemService.mockItem(Material.NETHERITE_PICKAXE))));
    }

    @Test
    public void testRequiresToolWithTypeAndTier() {
        MiningCondition diamondAxe = MiningCondition.requiresTool(ToolType.AXE, ToolTier.DIAMOND);
        assertFalse(diamondAxe.canHarvest(createContext(TestItemService.mockItem(Material.IRON_AXE))));
        assertFalse(diamondAxe.canHarvest(createContext(TestItemService.mockItem(Material.DIAMOND_PICKAXE))));
        assertTrue(diamondAxe.canHarvest(createContext(TestItemService.mockItem(Material.DIAMOND_AXE))));
        assertTrue(diamondAxe.canHarvest(createContext(TestItemService.mockItem(Material.NETHERITE_AXE))));
    }

    @Test
    public void testCustomPredicate() {
        MiningCondition custom = MiningCondition.of(item -> item != null && item.getType() == Material.SHEARS);
        assertFalse(custom.canHarvest(createContext(TestItemService.mockItem(Material.DIAMOND_PICKAXE))));
        assertTrue(custom.canHarvest(createContext(TestItemService.mockItem(Material.SHEARS))));
    }
}
