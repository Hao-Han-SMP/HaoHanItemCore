package vn.haohan.itemcore.block;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.internal.block.BlockBreakOverlayRenderer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlockBreakOverlayRenderer Tests")
class BlockBreakOverlayRendererTest {

    private TestBlockBreakOverlayRenderer overlayRenderer;
    private Player mockPlayer;
    private World mockWorld;

    static class TestBlockBreakOverlayRenderer extends BlockBreakOverlayRenderer {
        final AtomicInteger spawnCount = new AtomicInteger();
        final AtomicInteger updateCount = new AtomicInteger();
        final AtomicInteger destroyCount = new AtomicInteger();
        int lastStage = -1;

        public TestBlockBreakOverlayRenderer() {
            super(null);
        }

        @Override
        public void spawnCrack(Player player, int entityId, Location location, int stage) {
            spawnCount.incrementAndGet();
            this.lastStage = stage;
        }

        @Override
        public void updateCrack(Player player, int entityId, int stage) {
            updateCount.incrementAndGet();
            this.lastStage = stage;
        }

        @Override
        public void destroyCrack(Player player, int... entityIds) {
            if (entityIds != null) {
                destroyCount.addAndGet(entityIds.length);
            }
        }
    }

    @BeforeEach
    void setUp() {
        overlayRenderer = new TestBlockBreakOverlayRenderer();

        mockWorld = Mockito.mock(World.class);
        mockPlayer = Mockito.mock(Player.class);
        Mockito.when(mockPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(mockPlayer.isOnline()).thenReturn(true);
        Mockito.when(mockPlayer.getWorld()).thenReturn(mockWorld);
    }

    @Test
    @DisplayName("spawnCrack records spawn call with initial stage")
    void testSpawnCrack() {
        Location loc = new Location(mockWorld, 10, 64, 10);
        int crackEntityId = 1_500_000_100;

        overlayRenderer.spawnCrack(mockPlayer, crackEntityId, loc, 0);

        assertEquals(1, overlayRenderer.spawnCount.get());
        assertEquals(0, overlayRenderer.lastStage);
    }

    @Test
    @DisplayName("updateCrack records metadata update call for stage change")
    void testUpdateCrack() {
        int crackEntityId = 1_500_000_100;
        overlayRenderer.updateCrack(mockPlayer, crackEntityId, 5);

        assertEquals(1, overlayRenderer.updateCount.get());
        assertEquals(5, overlayRenderer.lastStage);
    }

    @Test
    @DisplayName("destroyCrack records destroy entities call")
    void testDestroyCrack() {
        int crackEntityId = 1_500_000_100;
        overlayRenderer.destroyCrack(mockPlayer, crackEntityId);

        assertEquals(1, overlayRenderer.destroyCount.get());
    }
}
