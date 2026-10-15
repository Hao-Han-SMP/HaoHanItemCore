package vn.haohan.itemcore.api.block;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockSubtypesTest {

    @Test
    @DisplayName("NativeCustomBlock should validate variation range 0..774 and expose isNative()")
    void testNativeCustomBlockCreation() {
        NativeCustomBlock block = NativeCustomBlock.builder("haohan:borax_ore")
                .customVariation(42)
                .hardness(2.5f)
                .mineableWith(ToolType.PICKAXE, ToolTier.IRON)
                .build();

        assertEquals("haohan:borax_ore", block.getId());
        assertEquals(Material.NOTE_BLOCK, block.getBaseBlock());
        assertEquals(42, block.getCustomVariation());
        assertTrue(block.isNative());
        assertFalse(block.isAlternative());
        assertEquals(2.5f, block.getHardness());
        assertFalse(block.isDirectional());

        assertThrows(IllegalArgumentException.class, () ->
                NativeCustomBlock.builder("invalid:neg").customVariation(-1).build()
        );
        assertThrows(IllegalArgumentException.class, () ->
                NativeCustomBlock.builder("invalid:over").customVariation(775).build()
        );
    }

    @Test
    @DisplayName("NativeCustomBlock directional and boundary variations")
    void testNativeCustomBlockDirectionalAndBoundaries() {
        NativeCustomBlock minBlock = NativeCustomBlock.builder("haohan:min_var")
                .customVariation(0)
                .directional(true)
                .build();
        assertEquals(0, minBlock.getCustomVariation());
        assertTrue(minBlock.isDirectional());

        NativeCustomBlock maxBlock = NativeCustomBlock.builder("haohan:max_var")
                .customVariation(774)
                .directional(false)
                .build();
        assertEquals(774, maxBlock.getCustomVariation());
        assertFalse(maxBlock.isDirectional());
    }

    @Test
    @DisplayName("AlternativeCustomBlock should default to Material.BARRIER and expose isAlternative()")
    void testAlternativeCustomBlockCreation() {
        AlternativeCustomBlock block = AlternativeCustomBlock.builder("haohan:charging_station")
                .hardness(5.0f)
                .build();

        assertEquals("haohan:charging_station", block.getId());
        assertEquals(Material.BARRIER, block.getBaseBlock());
        assertFalse(block.isNative());
        assertTrue(block.isAlternative());
        assertEquals(5.0f, block.getHardness());
    }

    @Test
    @DisplayName("CustomBlockDefinition default instance should have isNative() and isAlternative() false unless it's a subtype")
    void testCustomBlockDefinitionDefaults() {
        CustomBlockDefinition legacy = CustomBlockDefinition.builder("haohan:legacy_block")
                .hardness(1.0f)
                .build();

        assertFalse(legacy.isNative());
        assertFalse(legacy.isAlternative());
    }

    @Test
    @DisplayName("toString should contain customVariation and directional for NativeCustomBlock")
    void testNativeCustomBlockToString() {
        NativeCustomBlock block = NativeCustomBlock.builder("haohan:ore")
                .customVariation(10)
                .directional(true)
                .hardness(2.0f)
                .build();
        String str = block.toString();
        assertTrue(str.contains("customVariation=10"));
        assertTrue(str.contains("directional=true"));
        assertTrue(str.contains("id='haohan:ore'"));
    }
}
