package vn.haohan.itemcore.api.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockSoundGroupTest {

    @Test
    void testDefaultValues() {
        CustomBlockSoundGroup group = CustomBlockSoundGroup.builder().build();
        assertNotNull(group.getBreakSound());
        assertNotNull(group.getStepSound());
        assertNotNull(group.getPlaceSound());
        assertNotNull(group.getHitSound());
        assertNotNull(group.getFallSound());

        assertEquals("minecraft:block.stone.break", group.getBreakSound().soundKey());
        assertEquals(1.0f, group.getBreakSound().volume());
        assertEquals(0.8f, group.getBreakSound().pitch());
    }

    @Test
    void testCustomSoundValues() {
        CustomBlockSoundGroup group = CustomBlockSoundGroup.builder()
                .placeSound("haohan:block.crystal.place", 1.2f, 0.9f)
                .breakSound("haohan:block.crystal.break", 1.0f, 1.0f)
                .hitSound("minecraft:block.amethyst_block.hit", 0.5f, 1.1f)
                .stepSound("minecraft:block.amethyst_block.step")
                .fallSound("minecraft:block.amethyst_block.fall")
                .build();

        assertEquals("haohan:block.crystal.place", group.getPlaceSound().soundKey());
        assertEquals(1.2f, group.getPlaceSound().volume());
        assertEquals(0.9f, group.getPlaceSound().pitch());

        assertEquals("haohan:block.crystal.break", group.getBreakSound().soundKey());
        assertEquals("minecraft:block.amethyst_block.hit", group.getHitSound().soundKey());
        assertEquals("minecraft:block.amethyst_block.step", group.getStepSound().soundKey());
        assertEquals("minecraft:block.amethyst_block.fall", group.getFallSound().soundKey());
    }
}
