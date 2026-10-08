package vn.haohan.itemcore.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.BlockDropProvider;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.MiningContext;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.recipe.TestItemService;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class BlockDropProviderTest {

    private DefaultItemRegistry itemRegistry;
    private TestItemService itemService;
    private CustomBlockDefinition testBlockDef;

    @BeforeEach
    public void setUp() {
        itemRegistry = new DefaultItemRegistry(Logger.getLogger("DropTest"));
        itemService = new TestItemService(itemRegistry);
        HaoHanItemCore.setInstance(new HaoHanItemCore(
                itemRegistry, null, itemService,
                null, null, null, null));

        itemRegistry.register(ItemDefinition.builder("custom:ruby")
                .material(Material.REDSTONE)
                .displayName("Ruby")
                .build());

        itemRegistry.register(ItemDefinition.builder("custom:ruby_ore")
                .material(Material.BARRIER)
                .displayName("Ruby Ore")
                .build());

        testBlockDef = CustomBlockDefinition.builder("custom:ruby_ore").build();
    }

    private MiningContext createContext(ItemStack tool) {
        return new MiningContext(null, null, testBlockDef, tool);
    }

    private MiningContext createContextWithSilkTouch(ItemStack tool) {
        return new MiningContext(null, null, testBlockDef, tool, true, null, null);
    }

    @Test
    public void testDefaultDropWithoutSilkTouch() {
        BlockDropProvider provider = BlockDropProvider.builder()
                .defaultDrop("custom:ruby", 1)
                .silkTouchDrop("custom:ruby_ore")
                .build();

        ItemStack normalPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        List<ItemStack> drops = provider.getDrops(createContext(normalPick));

        assertEquals(1, drops.size());
        assertEquals("custom:ruby", itemService.getId(drops.get(0)));
    }

    @Test
    public void testSilkTouchOverride() {
        BlockDropProvider provider = BlockDropProvider.builder()
                .defaultDrop("custom:ruby", 1)
                .silkTouchDropsSelf()
                .build();

        ItemStack silkPick = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        List<ItemStack> drops = provider.getDrops(createContextWithSilkTouch(silkPick));

        assertEquals(1, drops.size());
        assertEquals("custom:ruby_ore", itemService.getId(drops.get(0)));
    }

    @Test
    public void testCustomRulePrecedence() {
        BlockDropProvider provider = BlockDropProvider.builder()
                .defaultDrop("custom:ruby", 1)
                .rule(ctx -> ctx.tool() != null && ctx.tool().getType() == Material.GOLDEN_PICKAXE,
                        ctx -> List.of(itemService.create("custom:ruby", 5)))
                .build();

        ItemStack goldPick = TestItemService.mockItem(Material.GOLDEN_PICKAXE);
        List<ItemStack> drops = provider.getDrops(createContext(goldPick));

        assertEquals(1, drops.size());
        assertEquals(5, drops.get(0).getAmount());
    }
}
