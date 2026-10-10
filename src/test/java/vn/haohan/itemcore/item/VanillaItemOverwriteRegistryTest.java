package vn.haohan.itemcore.item;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;

import static org.junit.jupiter.api.Assertions.*;

public class VanillaItemOverwriteRegistryTest {

    @Test
    void testVanillaOverwriteDetectionAndLookup() {
        DefaultItemRegistry registry = new DefaultItemRegistry(null);

        ItemDefinition def = ItemDefinition.builder("minecraft:diamond_pickaxe")
                .material(Material.DIAMOND_PICKAXE)
                .displayName("§bDiamond Pickaxe Overwrite")
                .tier("diamond", 6)
                .build();

        assertTrue(def.isVanillaOverwrite(), "Should identify minecraft: prefix as vanilla overwrite");
        registry.register(def);

        assertEquals(def, registry.getVanillaOverwrite(Material.DIAMOND_PICKAXE));
        assertNull(registry.getVanillaOverwrite(Material.IRON_PICKAXE));
    }

    @Test
    void testExplicitOverwriteFlag() {
        DefaultItemRegistry registry = new DefaultItemRegistry(null);

        ItemDefinition def = ItemDefinition.builder("custom:iron_pickaxe")
                .material(Material.IRON_PICKAXE)
                .overwriteVanilla(true)
                .build();

        assertTrue(def.isVanillaOverwrite());
        registry.register(def);

        assertEquals(def, registry.getVanillaOverwrite(Material.IRON_PICKAXE));
    }
}
