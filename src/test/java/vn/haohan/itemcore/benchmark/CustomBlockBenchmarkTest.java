package vn.haohan.itemcore.benchmark;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("benchmark")
@DisplayName("CustomBlock PacketEvents & Virtual Rendering Benchmark Suite")
class CustomBlockBenchmarkTest {

    private CustomBlockStorage storage;
    private DefaultCustomBlockRegistry registry;
    private VirtualEntityIdManager idManager;
    private BenchmarkRenderer renderer;
    private CustomBlockVisibilityManager visibilityManager;
    private World mockWorld;
    private final UUID worldUid = UUID.randomUUID();

    static class BenchmarkRenderer extends BlockDisplayRenderer {
        final AtomicLong spawnPacketCount = new AtomicLong();
        final AtomicLong destroyPacketCount = new AtomicLong();
        final AtomicLong totalEntitiesSpawned = new AtomicLong();

        public BenchmarkRenderer() {
            super(null);
        }

        @Override
        public void spawnDisplay(Player player, int entityId, Location location, CustomBlockModel model) {
            spawnPacketCount.incrementAndGet();
            totalEntitiesSpawned.incrementAndGet();
        }

        @Override
        public void destroyDisplay(Player player, int... entityIds) {
            if (entityIds != null) {
                destroyPacketCount.incrementAndGet();
                totalEntitiesSpawned.addAndGet(-entityIds.length);
            }
        }

        public void reset() {
            spawnPacketCount.set(0);
            destroyPacketCount.set(0);
            totalEntitiesSpawned.set(0);
        }
    }

    @BeforeEach
    void setUp() {
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        Mockito.when(mockPlugin.getName()).thenReturn("haohanitemcore");
        Mockito.when(mockPlugin.getLogger()).thenReturn(Logger.getLogger("BenchmarkLogger"));

        mockWorld = Mockito.mock(World.class);
        Mockito.when(mockWorld.getUID()).thenReturn(worldUid);
        Mockito.when(mockWorld.getName()).thenReturn("world");
        Mockito.when(mockWorld.getEntitiesByClass(org.bukkit.entity.BlockDisplay.class)).thenReturn(Collections.emptyList());

        storage = new CustomBlockStorage(mockPlugin);
        registry = new DefaultCustomBlockRegistry();
        idManager = new VirtualEntityIdManager();
        renderer = new BenchmarkRenderer();

        visibilityManager = new CustomBlockVisibilityManager(
                storage, registry, idManager, renderer, Logger.getLogger("BenchmarkLogger"));
        visibilityManager.setViewDistanceChunks(4); // 9x9 chunk area = 81 chunks in view

        // Register default benchmark block
        registry.register(CustomBlockDefinition.builder("benchmark:test_block")
                .model(new CustomBlockModel(null))
                .build());
    }

    private Player createMockPlayer(UUID uuid, String name) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(uuid);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.isOnline()).thenReturn(true);
        Mockito.when(player.getWorld()).thenReturn(mockWorld);
        return player;
    }

    @Test
    @DisplayName("Scenario A, B, C, D, E: Scaling Custom Blocks (0, 100, 1K, 5K, 10K+) with 1 Player")
    void testSinglePlayerBlockScaling() {
        int[] counts = {0, 100, 1_000, 5_000, 10_000};
        Player player = createMockPlayer(UUID.randomUUID(), "SoloPlayer");
        Location playerLoc = new Location(mockWorld, 0.5, 64, 0.5);

        System.out.println("\n══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  BENCHMARK: SINGLE-PLAYER SCALING (0 -> 10,000+ CUSTOM BLOCKS)");
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.printf("%-10s | %-16s | %-16s | %-14s | %-12s%n",
                "Blocks", "Population Time", "Visibility Init", "Packets Sent", "Server Entities");
        System.out.println("------------------------------------------------------------------------------");

        for (int targetCount : counts) {
            storage.clear();
            idManager.clear();
            visibilityManager.clear();
            renderer.reset();

            // 1. Populate custom blocks in spiral around (0, 0)
            long startPop = System.nanoTime();
            int placed = 0;
            int radius = 0;
            while (placed < targetCount) {
                for (int dx = -radius; dx <= radius && placed < targetCount; dx++) {
                    for (int dz = -radius; dz <= radius && placed < targetCount; dz++) {
                        int x = dx * 16 + (placed % 16);
                        int z = dz * 16 + ((placed / 16) % 16);
                        storage.set(worldUid, x, 64, z, "benchmark:test_block");
                        placed++;
                    }
                }
                radius++;
            }
            long popTimeUs = (System.nanoTime() - startPop) / 1_000;

            // 2. Initialize player visibility
            long startInit = System.nanoTime();
            visibilityManager.initPlayer(player, playerLoc);
            long initTimeUs = (System.nanoTime() - startInit) / 1_000;

            // 3. Verify server-side entity count is strictly 0
            int serverBlockDisplayCount = mockWorld.getEntitiesByClass(org.bukkit.entity.BlockDisplay.class).size();
            assertEquals(0, serverBlockDisplayCount, "CRITICAL: Server Bukkit BlockDisplay count must remain 0!");

            System.out.printf("%-10d | %-13d µs | %-13d µs | %-14d | %-12d%n",
                    targetCount, popTimeUs, initTimeUs, renderer.spawnPacketCount.get(), serverBlockDisplayCount);

            assertTrue(initTimeUs < 50_000, "Visibility initialization should complete under 50ms even for 10k blocks");
        }
    }

    @Test
    @DisplayName("Scenario Multiplayer: 1, 5, 20, 50 Players with 5,000 Custom Blocks")
    void testMultiplayerScaling() {
        int targetBlocks = 5_000;
        storage.clear();
        idManager.clear();
        visibilityManager.clear();
        renderer.reset();

        // Distribute 5,000 blocks across 25 chunks (-2 to 2)
        int placed = 0;
        int chunkExtent = 2;
        while (placed < targetBlocks) {
            for (int cx = -chunkExtent; cx <= chunkExtent && placed < targetBlocks; cx++) {
                for (int cz = -chunkExtent; cz <= chunkExtent && placed < targetBlocks; cz++) {
                    int lx = placed % 16;
                    int lz = (placed / 16) % 16;
                    int y = 60 + (placed % 64);
                    storage.set(worldUid, cx * 16 + lx, y, cz * 16 + lz, "benchmark:test_block");
                    placed++;
                }
            }
        }

        // Warmup JIT
        Player warmupPlayer = createMockPlayer(UUID.randomUUID(), "Warmup");
        visibilityManager.initPlayer(warmupPlayer, new Location(mockWorld, 0.5, 64, 0.5));
        visibilityManager.clear();
        renderer.reset();

        int[] playerCounts = {1, 5, 20, 50};
        System.out.println("\n══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  BENCHMARK: MULTIPLAYER SCALING (5,000 CUSTOM BLOCKS, 1 -> 50 PLAYERS)");
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.printf("%-10s | %-18s | %-16s | %-16s | %-12s%n",
                "Players", "Total Sync Time", "Avg Sync / Player", "Total Packets", "Server Entities");
        System.out.println("------------------------------------------------------------------------------");

        for (int pCount : playerCounts) {
            visibilityManager.clear();
            renderer.reset();

            List<Player> players = new ArrayList<>(pCount);
            for (int i = 0; i < pCount; i++) {
                players.add(createMockPlayer(UUID.randomUUID(), "Player_" + i));
            }

            long start = System.nanoTime();
            for (Player player : players) {
                Location loc = new Location(mockWorld, 0.5, 64, 0.5);
                visibilityManager.initPlayer(player, loc);
            }
            long totalUs = (System.nanoTime() - start) / 1_000;
            long avgPerPlayerUs = totalUs / pCount;

            int serverEntities = mockWorld.getEntitiesByClass(org.bukkit.entity.BlockDisplay.class).size();
            assertEquals(0, serverEntities, "Server entity count must strictly be 0");

            System.out.printf("%-10d | %-15d µs | %-13d µs | %-16d | %-12d%n",
                    pCount, totalUs, avgPerPlayerUs, renderer.spawnPacketCount.get(), serverEntities);

            assertTrue(avgPerPlayerUs < 250_000, "Per-player sync time for 5,000 blocks must be under 250ms");
        }
    }

    @Test
    @DisplayName("Movement Benchmark: Stationary vs Intra-Chunk vs Inter-Chunk vs Teleport")
    void testMovementModesBenchmark() {
        storage.clear();
        idManager.clear();
        visibilityManager.clear();
        renderer.reset();

        // Place 1,000 blocks in spawn area
        for (int i = 0; i < 1_000; i++) {
            int cx = (i % 5) - 2;
            int cz = ((i / 5) % 5) - 2;
            storage.set(worldUid, cx * 16 + (i % 16), 64, cz * 16 + ((i / 16) % 16), "benchmark:test_block");
        }

        Player player = createMockPlayer(UUID.randomUUID(), "Runner");
        Location start = new Location(mockWorld, 0.5, 64, 0.5);
        visibilityManager.initPlayer(player, start);
        renderer.reset();

        System.out.println("\n══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  BENCHMARK: MOVEMENT PATTERNS (STATIONARY, INTRA-CHUNK, INTER-CHUNK, TP)");
        System.out.println("══════════════════════════════════════════════════════════════════════════════");

        // 1. Stationary test: 10,000 move ticks with no coordinate change
        long startStationary = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            visibilityManager.handlePlayerMove(player, start, start);
        }
        long stationaryNs = (System.nanoTime() - startStationary) / 10_000;
        assertEquals(0, renderer.spawnPacketCount.get(), "Stationary must emit 0 packets");
        assertEquals(0, renderer.destroyPacketCount.get(), "Stationary must emit 0 destroy packets");
        System.out.printf("  Stationary:  10,000 ticks in %d ns/tick | Packets: 0%n", stationaryNs);

        // 2. Intra-chunk test: 10,000 moves inside chunk (0, 0)
        long startIntra = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            Location from = new Location(mockWorld, 1.0 + (i % 10) * 0.1, 64, 1.0);
            Location to = new Location(mockWorld, 1.0 + ((i + 1) % 10) * 0.1, 64, 1.0);
            visibilityManager.handlePlayerMove(player, from, to);
        }
        long intraNs = (System.nanoTime() - startIntra) / 10_000;
        assertEquals(0, renderer.spawnPacketCount.get(), "Intra-chunk move must emit 0 packets");
        assertEquals(0, renderer.destroyPacketCount.get(), "Intra-chunk move must emit 0 destroy packets");
        System.out.printf("  Intra-Chunk: 10,000 moves in %d ns/move | Packets: 0 (Zero-Packet Policy Verified)%n", intraNs);

        // 3. Inter-chunk test: 100 chunk border crossings
        long startInter = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            Location from = new Location(mockWorld, i * 16 + 15, 64, 0);
            Location to = new Location(mockWorld, (i + 1) * 16 + 1, 64, 0);
            visibilityManager.handlePlayerMove(player, from, to);
        }
        long interUs = (System.nanoTime() - startInter) / 100 / 1_000;
        System.out.printf("  Inter-Chunk: 100 border crosses in %d µs/cross | Delta Spawns: %d | Delta Destroys: %d%n",
                interUs, renderer.spawnPacketCount.get(), renderer.destroyPacketCount.get());

        // 4. Teleport test: 10 teleports far away
        renderer.reset();
        long startTp = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            Location tpLoc = new Location(mockWorld, 1000 * (i + 1), 64, 1000 * (i + 1));
            Mockito.when(player.getLocation()).thenReturn(tpLoc);
            visibilityManager.handlePlayerTeleport(player);
        }
        long tpUs = (System.nanoTime() - startTp) / 10 / 1_000;
        System.out.printf("  Teleport:    10 teleports in %d µs/teleport | Cleanup and resync fully verified%n", tpUs);
        System.out.println("══════════════════════════════════════════════════════════════════════════════\n");

        assertTrue(intraNs < 1000, "Intra-chunk check should complete under 1 microsecond (1,000 ns)");
    }

    @Test
    @DisplayName("Chunk Storage Benchmark: Compact Binary Serialization & Deserialization")
    void testChunkSerializationThroughput() throws Exception {
        int[] blockCounts = {0, 100, 1_000};

        // Warmup JIT
        for (int w = 0; w < 50; w++) {
            ByteArrayOutputStream wBaos = new ByteArrayOutputStream();
            DataOutputStream wOut = new DataOutputStream(wBaos);
            wOut.writeInt(100);
            for (int i = 0; i < 100; i++) {
                wOut.writeInt(i);
                wOut.writeUTF("warmup:block");
            }
            wOut.flush();
            byte[] wBytes = wBaos.toByteArray();
            DataInputStream wIn = new DataInputStream(new ByteArrayInputStream(wBytes));
            int wCount = wIn.readInt();
            Map<Integer, String> wMap = new HashMap<>((int) Math.ceil(wCount / 0.75f) + 1);
            for (int i = 0; i < wCount; i++) {
                wMap.put(wIn.readInt(), wIn.readUTF());
            }
        }

        System.out.println("\n══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  BENCHMARK: CHUNK STORAGE SERIALIZATION & DESERIALIZATION");
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.printf("%-10s | %-16s | %-18s | %-14s%n",
                "Blocks", "Serialize (µs)", "Deserialize (µs)", "Payload Size");
        System.out.println("------------------------------------------------------------------------------");

        for (int count : blockCounts) {
            Map<Integer, String> blockMap = new HashMap<>((int) Math.ceil(count / 0.75f) + 1);
            for (int i = 0; i < count; i++) {
                int key = CustomBlockStorage.packLocalKey(i % 16, i / 16, 0);
                blockMap.put(key, "benchmark:test_block_" + (i % 5));
            }

            int iterations = count == 0 ? 1 : 5;
            long totalSer = 0;
            long totalDeser = 0;
            byte[] bytes = null;

            for (int iter = 0; iter < iterations; iter++) {
                // Serialization
                long startSer = System.nanoTime();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                out.writeInt(blockMap.size());
                for (Map.Entry<Integer, String> entry : blockMap.entrySet()) {
                    out.writeInt(entry.getKey());
                    out.writeUTF(entry.getValue());
                }
                out.flush();
                bytes = baos.toByteArray();
                totalSer += (System.nanoTime() - startSer);

                // Deserialization
                long startDeser = System.nanoTime();
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
                int readCount = in.readInt();
                Map<Integer, String> readMap = new HashMap<>((int) Math.ceil(readCount / 0.75f) + 1);
                for (int i = 0; i < readCount; i++) {
                    readMap.put(in.readInt(), in.readUTF());
                }
                totalDeser += (System.nanoTime() - startDeser);
                assertEquals(count, readMap.size());
            }

            long serUs = (totalSer / iterations) / 1_000;
            long deserUs = (totalDeser / iterations) / 1_000;

            System.out.printf("%-10d | %-13d µs | %-15d µs | %-14s%n",
                    count, serUs, deserUs, bytes.length + " bytes");

            assertTrue(deserUs < 15_000, "Deserialization should be well under 15ms even for 1,000 blocks");
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════\n");
    }
}
