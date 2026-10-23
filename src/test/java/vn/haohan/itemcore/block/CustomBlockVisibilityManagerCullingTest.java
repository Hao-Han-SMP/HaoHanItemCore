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
import vn.haohan.itemcore.internal.block.*;
import vn.haohan.itemcore.internal.config.CustomBlockCullingConfig;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockVisibilityManagerCullingTest {

    private CustomBlockStorage storage;
    private DefaultCustomBlockRegistry registry;
    private VirtualEntityIdManager idManager;
    private TestRenderer renderer;
    private CustomBlockCullingService cullingService;
    private CustomBlockCullingConfig cullingConfig;
    private CustomBlockVisibilityManager visibilityManager;
    private World mockWorld;
    private final UUID worldUid = UUID.randomUUID();

    static class TestRenderer extends BlockDisplayRenderer {
        final AtomicInteger spawns = new AtomicInteger();
        final AtomicInteger destroys = new AtomicInteger();

        public TestRenderer() {
            super(null);
        }

        @Override
        public void spawnDisplay(Player player, int entityId, Location location, CustomBlockModel model) {
            spawns.incrementAndGet();
        }

        @Override
        public void destroyDisplay(Player player, int... entityIds) {
            if (entityIds != null) {
                destroys.addAndGet(entityIds.length);
            }
        }
    }

    @BeforeEach
    void setUp() {
        Plugin plugin = Mockito.mock(Plugin.class);
        Mockito.when(plugin.getName()).thenReturn("haohanitemcore");
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));

        mockWorld = Mockito.mock(World.class);
        Mockito.when(mockWorld.getUID()).thenReturn(worldUid);
        Mockito.when(mockWorld.getName()).thenReturn("world");

        storage = new CustomBlockStorage(plugin);
        registry = new DefaultCustomBlockRegistry();
        idManager = new VirtualEntityIdManager();
        renderer = new TestRenderer();
        cullingService = new CustomBlockCullingService();
        cullingConfig = CustomBlockCullingConfig.defaultConfig();

        visibilityManager = new CustomBlockVisibilityManager(
                storage, registry, idManager, renderer, cullingService, cullingConfig, Logger.getLogger("TestLogger"));
        visibilityManager.setViewDistanceChunks(3); // 7x7 chunk area

        registry.register(CustomBlockDefinition.builder("test:block")
                .model(new CustomBlockModel(null))
                .build());
    }

    private Player createMockPlayer(UUID uuid, String name, Location loc) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(uuid);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.isOnline()).thenReturn(true);
        Mockito.when(player.getWorld()).thenReturn(mockWorld);
        Mockito.when(player.getLocation()).thenReturn(loc);
        return player;
    }

    @Test
    @DisplayName("Spherical Radius Culling: blocks outside 32m radius are not spawned")
    void testSphericalRadiusCulling() {
        Location playerLoc = new Location(mockWorld, 0.5, 64, 0.5, 0f, 0f);
        Player player = createMockPlayer(UUID.randomUUID(), "PlayerSphere", playerLoc);

        // Block at 10m away -> within 32m
        storage.set(worldUid, 0, 64, 10, "test:block");
        // Block at 40m away -> outside 32m
        storage.set(worldUid, 0, 64, 40, "test:block");

        visibilityManager.initPlayer(player, playerLoc);

        assertEquals(1, renderer.spawns.get(), "Only block within 32m spherical radius must spawn");
    }

    @Test
    @DisplayName("Omnidirectional Visibility: blocks behind the player also spawn (no frustum culling)")
    void testOmnidirectionalVisibilityWithinRadius() {
        // Player looking South (+Z): yaw=0, pitch=0
        Location playerLoc = new Location(mockWorld, 0.5, 64, 0.5, 0f, 0f);
        Player player = createMockPlayer(UUID.randomUUID(), "PlayerOmni", playerLoc);

        // Block in front: (0, 64, 15)
        storage.set(worldUid, 0, 64, 15, "test:block");
        // Block directly behind: (0, 64, -15)
        storage.set(worldUid, 0, 64, -15, "test:block");

        visibilityManager.initPlayer(player, playerLoc);

        assertEquals(2, renderer.spawns.get(), "Both blocks in front and behind within spherical radius must spawn");
    }

    @Test
    @DisplayName("Intra-chunk rotation: rotation does not trigger culling re-evaluation or spawn packets")
    void testIntraChunkRotationDoesNotTriggerUpdate() {
        Location startLoc = new Location(mockWorld, 0.5, 64, 0.5, 0f, 0f);
        Player player = createMockPlayer(UUID.randomUUID(), "PlayerRotator", startLoc);

        storage.set(worldUid, 0, 64, 15, "test:block");
        visibilityManager.initPlayer(player, startLoc);
        assertEquals(1, renderer.spawns.get());

        // Player rotates 180 degrees in place (same chunk)
        Location rotatedLoc = new Location(mockWorld, 0.5, 64, 0.5, 180f, 0f);
        Mockito.when(player.getLocation()).thenReturn(rotatedLoc);

        boolean evaluated = visibilityManager.handlePlayerMove(player, startLoc, rotatedLoc);

        assertFalse(evaluated, "Intra-chunk rotation must immediately return false");
        assertEquals(1, renderer.spawns.get(), "No new spawn packets should be dispatched");
        assertEquals(0, renderer.destroys.get(), "No destroy packets should be dispatched");
    }

    @Test
    @DisplayName("Hysteresis Retention: block remains visible within hysteresis margin when re-evaluated")
    void testHysteresisRetention() {
        // Chunk 0, 0
        Location loc1 = new Location(mockWorld, 0.5, 64, 0.5);
        Player player = createMockPlayer(UUID.randomUUID(), "PlayerHysteresis", loc1);

        // Block at (0, 64, 30) -> distance 29.5m (within 32m radius)
        storage.set(worldUid, 0, 64, 30, "test:block");

        visibilityManager.initPlayer(player, loc1);
        assertEquals(1, renderer.spawns.get());

        // Player moves across chunk boundary to chunk (1, 0): X=16.5
        // Distance to block (0, 64, 30): sqrt(16^2 + 29.5^2) = sqrt(256 + 870.25) = sqrt(1126.25) ~ 33.56m
        // 33.56m > 32m (spherical radius), but <= 36m (hysteresis margin)
        Location loc2 = new Location(mockWorld, 16.5, 64, 0.5);
        Mockito.when(player.getLocation()).thenReturn(loc2);

        boolean evaluated = visibilityManager.handlePlayerMove(player, loc1, loc2);
        assertTrue(evaluated);

        assertEquals(0, renderer.destroys.get(), "Block within hysteresis threshold (36m) must NOT be destroyed");
    }
}
