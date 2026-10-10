package vn.haohan.itemcore.block;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.internal.block.VirtualEntityIdManager;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VirtualEntityIdManager Tests")
class VirtualEntityIdManagerTest {

    private VirtualEntityIdManager idManager;
    private final UUID worldUid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        idManager = new VirtualEntityIdManager();
    }

    @Test
    @DisplayName("Should generate collision-free virtual IDs starting above 1,500,000,000")
    void testVirtualIdRange() {
        int id1 = idManager.getOrCreateId(worldUid, 10, 64, 20);
        int id2 = idManager.getOrCreateId(worldUid, 11, 64, 20);

        assertTrue(id1 >= 1_500_000_000, "Virtual ID must start at or above 1.5B");
        assertTrue(id2 >= 1_500_000_000, "Virtual ID must start at or above 1.5B");
        assertNotEquals(id1, id2, "Different coordinates must have different IDs");
    }

    @Test
    @DisplayName("Should return identical virtual ID for the same block position")
    void testSameCoordinateSameId() {
        int firstCall = idManager.getOrCreateId(worldUid, 100, 70, -50);
        int secondCall = idManager.getOrCreateId(worldUid, 100, 70, -50);

        assertEquals(firstCall, secondCall, "Identical position must yield identical virtual entity ID");
        assertEquals(firstCall, idManager.getId(worldUid, 100, 70, -50));
    }

    @Test
    @DisplayName("Should release entity ID correctly")
    void testReleaseEntityId() {
        int id = idManager.getOrCreateId(worldUid, 5, 60, 5);
        assertEquals(id, idManager.getId(worldUid, 5, 60, 5));

        int released = idManager.removeId(worldUid, 5, 60, 5);
        assertEquals(id, released);
        assertEquals(-1, idManager.getId(worldUid, 5, 60, 5));
    }

    @Test
    @DisplayName("Should maintain consistency and uniqueness under high concurrent access")
    void testConcurrentIdAllocation() throws InterruptedException {
        int threadCount = 10;
        int blocksPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<Integer> uniqueIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (int t = 0; t < threadCount; t++) {
            final int threadOffset = t * 1000;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < blocksPerThread; i++) {
                        int x = threadOffset + i;
                        int id = idManager.getOrCreateId(worldUid, x, 64, 0);
                        uniqueIds.add(id);

                        // Second call should yield same ID
                        int cachedId = idManager.getOrCreateId(worldUid, x, 64, 0);
                        assertEquals(id, cachedId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Id allocation timed out");
        assertEquals(threadCount * blocksPerThread, uniqueIds.size(), "All distinct coordinates must have unique entity IDs");
    }
}
