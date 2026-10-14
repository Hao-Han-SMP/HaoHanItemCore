package vn.haohan.itemcore.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

    @Test
    @DisplayName("Frustum check: block directly in front is visible")
    void testIsInFrustumFront() {
        // Player at (0, 64, 0), looking toward +Z (yaw=0, pitch=0 -> dir=(0, 0, 1))
        double eyeX = 0, eyeY = 64, eyeZ = 0;
        double dirX = 0, dirY = 0, dirZ = 1;
        double cosThreshold = service.calculateCosThreshold(70, 20); // half-fov=35 + 20 = 55 deg -> cos ~0.5735
        double nearDistSq = 3.5 * 3.5;

        // Block at (0, 64, 10) directly ahead -> angle 0 deg -> cos = 1.0 >= 0.5735
        assertTrue(service.isInFrustum(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, 0, 64, 10, cosThreshold, nearDistSq));

        // Block at (0, 64, -10) behind player -> angle 180 deg -> cos = -1.0 < 0.5735
        assertFalse(service.isInFrustum(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, 0, 64, -10, cosThreshold, nearDistSq));

        // Block at (0, 64, -2) behind player but within near distance 3.5m -> MUST BE TRUE
        assertTrue(service.isInFrustum(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, 0, 64, -2, cosThreshold, nearDistSq));
    }

    @Test
    @DisplayName("1-Face surface check: completely enclosed block is culled")
    void testIsSurfaceExposed() {
        World world = Mockito.mock(World.class);
        Block center = Mockito.mock(Block.class);
        Mockito.when(world.getBlockAt(10, 64, 10)).thenReturn(center);

        // All 6 neighbors are STONE (occluding)
        int[][] deltas = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] d : deltas) {
            Block n = Mockito.mock(Block.class);
            Mockito.when(n.getType()).thenReturn(Material.STONE);
            Mockito.when(world.getBlockAt(10 + d[0], 64 + d[1], 10 + d[2])).thenReturn(n);
        }

        assertFalse(service.isSurfaceExposed(world, 10, 64, 10), "Enclosed block must NOT be surface exposed");

        // Make top block AIR
        Block top = Mockito.mock(Block.class);
        Mockito.when(top.getType()).thenReturn(Material.AIR);
        Mockito.when(world.getBlockAt(10, 65, 10)).thenReturn(top);

        assertTrue(service.isSurfaceExposed(world, 10, 64, 10), "Block with 1 AIR neighbor must be surface exposed");
    }
}
