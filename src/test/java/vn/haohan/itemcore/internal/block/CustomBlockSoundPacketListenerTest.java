package vn.haohan.itemcore.internal.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockSoundPacketListenerTest {

    @Test
    void testRecentPlacementTracking() {
        CustomBlockSoundPacketListener listener = new CustomBlockSoundPacketListener();
        assertFalse(listener.isRecentPlacement(10, 64, -20));

        listener.markRecentPlacement(10, 64, -20);
        assertTrue(listener.isRecentPlacement(10, 64, -20));

        listener.clearRecentPlacement(10, 64, -20);
        assertFalse(listener.isRecentPlacement(10, 64, -20));
    }
}
