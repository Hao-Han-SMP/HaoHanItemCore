package vn.haohan.itemcore.block;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.internal.block.CustomBlockCullingService;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockCullingServiceTest {

    private final CustomBlockCullingService service = new CustomBlockCullingService();

    @Test
    @DisplayName("Spherical distance check uses distance squared correctly")
    void testIsInSphere() {
        // Player at (0, 0, 0), Block at (3, 4, 0) -> distance is 5, distSq is 25
        assertTrue(service.isInSphere(0, 0, 0, 3, 4, 0, 25.0));
        assertTrue(service.isInSphere(0, 0, 0, 3, 4, 0, 26.0));
        assertFalse(service.isInSphere(0, 0, 0, 3, 4, 0, 24.0));
    }
}
