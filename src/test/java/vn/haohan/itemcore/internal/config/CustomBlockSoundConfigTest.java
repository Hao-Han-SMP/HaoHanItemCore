package vn.haohan.itemcore.internal.config;

import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.CustomBlockActionRegistry;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.item.ItemDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockSoundConfigTest {

    @Test
    void testParseSoundGroupAndActionFromItemProperties() {
        AtomicBoolean interactFired = new AtomicBoolean(false);
        CustomBlockActionRegistry.registerInteractAction("test_forge_interact", ctx -> interactFired.set(true));

        Map<String, Object> soundMap = new HashMap<>();
        soundMap.put("place", "haohan:block.forge.place");
        soundMap.put("break", "haohan:block.forge.break");
        soundMap.put("hit", "minecraft:block.iron.hit");
        soundMap.put("step", "minecraft:block.metal.step");

        Map<String, Object> properties = new HashMap<>();
        properties.put("custom_block_id", "example:forge");
        properties.put("custom_block_sounds", soundMap);
        properties.put("custom_block_on_interact", "test_forge_interact");

        ItemDefinition itemDef = ItemDefinition.builder("example:forge")
                .material(org.bukkit.Material.IRON_BLOCK)
                .properties(properties)
                .build();

        CustomBlockDefinition blockDef = vn.haohan.itemcore.internal.config.CustomBlockConfigParser.parseCustomBlock(itemDef);
        assertNotNull(blockDef);
        assertNotNull(blockDef.getSoundGroup());
        assertEquals("haohan:block.forge.place", blockDef.getSoundGroup().getPlaceSound().soundKey());
        assertEquals("haohan:block.forge.break", blockDef.getSoundGroup().getBreakSound().soundKey());
        assertEquals("minecraft:block.iron.hit", blockDef.getSoundGroup().getHitSound().soundKey());
        assertEquals("minecraft:block.metal.step", blockDef.getSoundGroup().getStepSound().soundKey());

        assertNotNull(blockDef.getInteractHandler());
        blockDef.getInteractHandler().onInteract(null);
        assertTrue(interactFired.get());
    }
}
