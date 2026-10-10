package vn.haohan.itemcore.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolTierRegistry;
import vn.haohan.itemcore.recipe.TestItemService;

import static org.junit.jupiter.api.Assertions.*;

class ToolTierRegistryTest {

    private ToolTierRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolTierRegistry();
    }

    @Test
    void testVanillaTiersRegisteredByDefault() {
        assertNotNull(registry.get("wood").orElse(null));
        assertNotNull(registry.get("stone").orElse(null));
        assertNotNull(registry.get("iron").orElse(null));
        assertNotNull(registry.get("diamond").orElse(null));
        assertNotNull(registry.get("netherite").orElse(null));
        assertNotNull(registry.get("gold").orElse(null));

        assertEquals(1, ToolTier.WOOD.level());
        assertEquals(2, ToolTier.STONE.level());
        assertEquals(3, ToolTier.IRON.level());
        assertEquals(4, ToolTier.DIAMOND.level());
        assertEquals(5, ToolTier.NETHERITE.level());
        assertEquals(1, ToolTier.GOLD.level());
    }

    @Test
    void testRegisterCustomTier() {
        ToolTier mithril = ToolTier.of("mithril", 6, 10.0f);
        registry.register(mithril);

        assertTrue(registry.get("mithril").isPresent());
        assertEquals(6, registry.get("mithril").get().level());
        assertEquals(10.0f, registry.get("mithril").get().baseMultiplier());
    }

    @Test
    void testFindTierFromVanillaItem() {
        ItemStack ironPick = TestItemService.mockItem(Material.IRON_PICKAXE);
        ToolTier tier = registry.findTier(ironPick);
        assertNotNull(tier);
        assertEquals("iron", tier.name().toLowerCase());
        assertEquals(3, tier.level());

        ItemStack diamondSword = TestItemService.mockItem(Material.DIAMOND_SWORD);
        ToolTier swordTier = registry.findTier(diamondSword);
        assertNotNull(swordTier);
        assertEquals("diamond", swordTier.name().toLowerCase());
        assertEquals(4, swordTier.level());
    }
}
