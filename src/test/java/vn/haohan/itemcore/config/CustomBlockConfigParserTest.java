package vn.haohan.itemcore.config;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.AlternativeCustomBlock;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.NativeCustomBlock;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.internal.config.CustomBlockConfigParser;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockConfigParserTest {

    @Test
    @DisplayName("Item with custom_variation parses to NativeCustomBlock")
    void testParseNativeCustomBlock() {
        ItemDefinition item = ItemDefinition.builder("haohan:copper_ore")
                .property("custom_block_id", "haohan:copper_ore")
                .property("custom_variation", 55)
                .property("custom_block_hardness", 3.0f)
                .build();

        CustomBlockDefinition blockDef = CustomBlockConfigParser.parseCustomBlock(item);
        assertInstanceOf(NativeCustomBlock.class, blockDef);
        NativeCustomBlock nativeBlock = (NativeCustomBlock) blockDef;
        assertEquals(55, nativeBlock.getCustomVariation());
        assertEquals(3.0f, nativeBlock.getHardness());
        assertTrue(nativeBlock.isNative());
        assertFalse(nativeBlock.isAlternative());
        assertEquals(Material.NOTE_BLOCK, nativeBlock.getBaseBlock());
    }

    @Test
    @DisplayName("Item with custom_block_variation fallback parses to NativeCustomBlock with directional flag")
    void testParseNativeCustomBlockFallbackAndDirectional() {
        ItemDefinition item = ItemDefinition.builder("haohan:directional_machine")
                .property("custom_block_id", "haohan:directional_machine")
                .property("custom_block_variation", 120)
                .property("custom_directional", true)
                .property("custom_block_hardness", 5.0f)
                .build();

        CustomBlockDefinition blockDef = CustomBlockConfigParser.parseCustomBlock(item);
        assertInstanceOf(NativeCustomBlock.class, blockDef);
        NativeCustomBlock nativeBlock = (NativeCustomBlock) blockDef;
        assertEquals(120, nativeBlock.getCustomVariation());
        assertTrue(nativeBlock.isDirectional());
        assertEquals(5.0f, nativeBlock.getHardness());
    }

    @Test
    @DisplayName("Item without custom_variation parses to AlternativeCustomBlock")
    void testParseAlternativeCustomBlock() {
        ItemDefinition item = ItemDefinition.builder("haohan:charging_station")
                .property("custom_block_id", "haohan:charging_station")
                .property("custom_block_hardness", 2.0f)
                .property("custom_block_tool_type", "PICKAXE")
                .build();

        CustomBlockDefinition blockDef = CustomBlockConfigParser.parseCustomBlock(item);
        assertInstanceOf(AlternativeCustomBlock.class, blockDef);
        assertTrue(blockDef.isAlternative());
        assertFalse(blockDef.isNative());
        assertEquals(Material.BARRIER, blockDef.getBaseBlock());
        assertEquals(2.0f, blockDef.getHardness());
        assertTrue(blockDef.getHarvestToolTypes().contains(ToolType.PICKAXE));
    }

    @Test
    @DisplayName("Vanilla block overwrite parses to CustomBlockDefinition with vanilla base material")
    void testParseVanillaBlockOverwrite() {
        ItemDefinition item = ItemDefinition.builder("minecraft:ancient_debris")
                .material(Material.ANCIENT_DEBRIS)
                .property("custom_block_id", "minecraft:ancient_debris")
                .property("custom_block_hardness", 12.0f)
                .build();

        CustomBlockDefinition blockDef = CustomBlockConfigParser.parseCustomBlock(item);
        assertNotNull(blockDef);
        assertTrue(blockDef.isVanillaOverwrite());
        assertFalse(blockDef.isNative());
        assertFalse(blockDef.isAlternative());
        assertEquals(Material.ANCIENT_DEBRIS, blockDef.getBaseBlock());
        assertEquals(12.0f, blockDef.getHardness());
    }

    @Test
    @DisplayName("Item without custom_block_id returns null")
    void testParseItemWithoutCustomBlockId() {
        ItemDefinition item = ItemDefinition.builder("haohan:regular_item")
                .property("some_property", "value")
                .build();

        CustomBlockDefinition blockDef = CustomBlockConfigParser.parseCustomBlock(item);
        assertNull(blockDef);
    }
}
