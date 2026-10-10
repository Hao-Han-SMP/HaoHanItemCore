package vn.haohan.itemcore.block;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockModel;
import vn.haohan.itemcore.internal.block.BlockDisplayRenderer;
import vn.haohan.itemcore.internal.block.CustomBlockStorage;
import vn.haohan.itemcore.internal.block.CustomBlockVisibilityManager;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockRegistry;
import vn.haohan.itemcore.internal.block.VirtualEntityIdManager;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomBlockVisibilityManager Culling & Packet Tests")
class CustomBlockVisibilityManagerTest {

    private CustomBlockStorage storage;
    private DefaultCustomBlockRegistry registry;
    private VirtualEntityIdManager idManager;
    private TestBlockDisplayRenderer renderer;
    private CustomBlockVisibilityManager visibilityManager;

    private World mockWorld;
    private final UUID worldUid = UUID.randomUUID();

    static class TestBlockDisplayRenderer extends BlockDisplayRenderer {
        final AtomicInteger spawnCount = new AtomicInteger();
        final AtomicInteger destroyCount = new AtomicInteger();

        public TestBlockDisplayRenderer() {
            super(null);
        }

        @Override
        public void spawnDisplay(Player player, int entityId, Location location, CustomBlockModel model) {
            spawnCount.incrementAndGet();
        }

        @Override
        public void destroyDisplay(Player player, int... entityIds) {
            if (entityIds != null) {
                destroyCount.addAndGet(entityIds.length);
            }
        }
    }

    @BeforeEach
    void setUp() {
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        Mockito.when(mockPlugin.getName()).thenReturn("haohanitemcore");
        Mockito.when(mockPlugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));

        mockWorld = Mockito.mock(World.class);
        Mockito.when(mockWorld.getUID()).thenReturn(worldUid);
        Mockito.when(mockWorld.getName()).thenReturn("world");

        storage = new CustomBlockStorage(mockPlugin);
        registry = new DefaultCustomBlockRegistry();
        idManager = new VirtualEntityIdManager();
        renderer = new TestBlockDisplayRenderer();

        visibilityManager = new CustomBlockVisibilityManager(
                storage, registry, idManager, renderer, Logger.getLogger("VisibilityTest"));
        visibilityManager.setViewDistanceChunks(2); // 5x5 chunk area = 25 chunks

        // Register custom block definition
        registry.register(CustomBlockDefinition.builder("test:block_1")
                .model(new CustomBlockModel(null))
                .build());
    }

    private Player createMockPlayer(UUID uuid, String name) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(uuid);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.isOnline()).thenReturn(true);
        Mockito.when(player.getWorld()).thenReturn(mockWorld);
        Mockito.when(mockWorld.getPlayers()).thenReturn(java.util.List.of(player));
        return player;
    }

    @Test
    @DisplayName("Zero Packet Policy: Intra-chunk player movement must send 0 packets")
    void testIntraChunkMoveSendsZeroPackets() {
        Player player = createMockPlayer(UUID.randomUUID(), "Player1");

        // Place 5 custom blocks in chunk (0, 0)
        for (int i = 0; i < 5; i++) {
            storage.set(worldUid, i, 64, i, "test:block_1");
        }

        // Initialize visibility at (0, 64, 0) in chunk (0, 0)
        Location initialLoc = new Location(mockWorld, 0.5, 64, 0.5);
        visibilityManager.initPlayer(player, initialLoc);

        assertEquals(5, renderer.spawnCount.get(), "Must have spawned 5 custom blocks in view on initial load");
        assertEquals(0, renderer.destroyCount.get());

        // Reset counters to monitor movement
        renderer.spawnCount.set(0);
        renderer.destroyCount.set(0);

        // Player moves within the SAME chunk (0, 0): from (0.5, 64, 0.5) to (14.2, 64, 13.8)
        Location from = initialLoc;
        Location to = new Location(mockWorld, 14.2, 64, 13.8);

        boolean chunkBorderCrossed = visibilityManager.handlePlayerMove(player, from, to);

        assertFalse(chunkBorderCrossed, "Intra-chunk move must NOT trigger chunk border crossing");
        assertEquals(0, renderer.spawnCount.get(), "Intra-chunk move must send strictly 0 spawn packets");
        assertEquals(0, renderer.destroyCount.get(), "Intra-chunk move must send strictly 0 destroy packets");
    }

    @Test
    @DisplayName("Chunk border crossing recalculates diff and sends only delta packets")
    void testInterChunkMoveDispatchesDelta() {
        Player player = createMockPlayer(UUID.randomUUID(), "Player2");

        // Place a block in chunk (-3, 0) [outside view on east move]
        // and a block in chunk (3, 0) [entering view on east move]
        storage.set(worldUid, -3 * 16 + 5, 64, 5, "test:block_1");
        storage.set(worldUid, 3 * 16 + 5, 64, 5, "test:block_1");

        Location start = new Location(mockWorld, 0.5, 64, 0.5); // chunk (0, 0) -> view: [-2..2]
        visibilityManager.initPlayer(player, start);

        // Blocks in -3 and +3 should not be spawned initially
        assertEquals(0, renderer.spawnCount.get());

        // Move east from chunk (0, 0) to chunk (1, 0) -> new view: [-1..3]
        // Chunk (3, 0) enters view! Chunk (-2, 0) exits view.
        Location enteredChunkLoc = new Location(mockWorld, 16.5, 64, 0.5); // chunk (1, 0)
        boolean crossed = visibilityManager.handlePlayerMove(player, start, enteredChunkLoc);

        assertTrue(crossed, "Must detect chunk border crossing");
        assertEquals(1, renderer.spawnCount.get(), "Should spawn block in newly entered chunk (3,0)");
        assertEquals(0, renderer.destroyCount.get());
    }

    @Test
    @DisplayName("Player quit should destroy all active virtual displays and clean player tracking")
    void testPlayerQuitCleanup() {
        Player player = createMockPlayer(UUID.randomUUID(), "Player3");

        storage.set(worldUid, 5, 64, 5, "test:block_1");
        storage.set(worldUid, 6, 64, 6, "test:block_1");

        Location loc = new Location(mockWorld, 0, 64, 0);
        visibilityManager.initPlayer(player, loc);

        assertEquals(2, renderer.spawnCount.get());

        visibilityManager.handlePlayerQuit(player);

        assertEquals(2, renderer.destroyCount.get(), "Must destroy all spawned entities upon player quit");
    }

    @Test
    @DisplayName("Chunk unload should despawn all displays in that chunk for viewers")
    void testChunkUnloadCleanup() {
        Player player = createMockPlayer(UUID.randomUUID(), "Player4");

        storage.set(worldUid, 5, 64, 5, "test:block_1"); // chunk (0, 0)
        storage.set(worldUid, 20, 64, 20, "test:block_1"); // chunk (1, 1)

        Location loc = new Location(mockWorld, 0, 64, 0);
        visibilityManager.initPlayer(player, loc);

        assertEquals(2, renderer.spawnCount.get());

        // Unload chunk (0, 0)
        visibilityManager.handleChunkUnload(mockWorld, 0, 0);

        assertEquals(1, renderer.destroyCount.get(), "Must destroy the 1 block in chunk (0, 0)");
    }

    @Test
    @DisplayName("PacketEvents CHUNK_DATA delivery should spawn custom blocks immediately")
    void testPacketEventsChunkDataAndUnloadDelivery() {
        Player player = createMockPlayer(UUID.randomUUID(), "PlayerPacket");

        // Place 3 blocks in chunk (4, 5)
        for (int i = 0; i < 3; i++) {
            storage.set(worldUid, 4 * 16 + i, 64, 5 * 16 + i, "test:block_1");
        }

        // Deliver CHUNK_DATA packet for chunk (4, 5)
        visibilityManager.handleChunkData(player, 4, 5);

        assertEquals(3, renderer.spawnCount.get(), "Must spawn all 3 blocks when CHUNK_DATA is received");
        assertEquals(0, renderer.destroyCount.get());

        // Deliver UNLOAD_CHUNK packet for chunk (4, 5)
        visibilityManager.handleChunkUnloadPacket(player, 4, 5);

        assertEquals(3, renderer.destroyCount.get(), "Must destroy all 3 blocks when UNLOAD_CHUNK is received");
    }
}

