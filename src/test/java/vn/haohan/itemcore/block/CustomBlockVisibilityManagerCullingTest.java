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
        visibilityManager.setViewDistanceChunks(2); // 5x5 chunk area

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
    @DisplayName("Frustum Culling: block ahead spawns, block behind outside near zone does not spawn")
    void testFrustumCullingDirectional() {
        // Looking South (+Z): yaw=0, pitch=0
        Location playerLoc = new Location(mockWorld, 0.5, 64, 0.5, 0f, 0f);
        Player player = createMockPlayer(UUID.randomUUID(), "PlayerFrustum", playerLoc);

        // Block in front: (0, 64, 15) -> angle 0 deg
        storage.set(worldUid, 0, 64, 15, "test:block");
        // Block behind: (0, 64, -15) -> angle 180 deg (behind)
        storage.set(worldUid, 0, 64, -15, "test:block");

        visibilityManager.initPlayer(player, playerLoc);

        assertEquals(1, renderer.spawns.get(), "Only block within view frustum must spawn");
    }

    @Test
    @DisplayName("Camera rotation: rotating towards previously culled block causes it to spawn")
    void testRotationTriggersCullingUpdate() {
        // Looking South (+Z, yaw=0)
        Location startLoc = new Location(mockWorld, 0.5, 64, 0.5, 0f, 0f);
        Player player = createMockPlayer(UUID.randomUUID(), "PlayerRotator", startLoc);

        // Block behind player (+Z is front, -Z is behind)
        storage.set(worldUid, 0, 64, -15, "test:block");

        visibilityManager.initPlayer(player, startLoc);
        assertEquals(0, renderer.spawns.get(), "Block behind should not spawn initially");

        // Player turns around 180 degrees (looking North: yaw=180)
        Location rotatedLoc = new Location(mockWorld, 0.5, 64, 0.5, 180f, 0f);
        Mockito.when(player.getLocation()).thenReturn(rotatedLoc);

        visibilityManager.handlePlayerMove(player, startLoc, rotatedLoc);

        assertEquals(1, renderer.spawns.get(), "Block now in front must spawn after rotation");
    }
}
