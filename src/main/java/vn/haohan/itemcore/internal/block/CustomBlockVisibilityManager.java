package vn.haohan.itemcore.internal.block;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.internal.config.CustomBlockCullingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages player-specific visibility and spatial culling for custom blocks.
 *
 * <p>Enforces zero-overhead intra-chunk movement: when a player moves within the same chunk
 * without camera angle change, no packets or spatial loops are executed. When crossing chunk
 * borders or rotating past threshold, calculates culling diffs and dispatches delta packets.</p>
 */
public final class CustomBlockVisibilityManager {

    private final CustomBlockStorage storage;
    private final CustomBlockRegistry registry;
    private final VirtualEntityIdManager idManager;
    private final BlockDisplayRenderer renderer;
    private final CustomBlockCullingService cullingService;
    private volatile CustomBlockCullingConfig cullingConfig;
    private final Logger logger;

    // Default radial chunk view distance for custom block rendering
    private int viewDistanceChunks = 5;

    // Player UUID -> Set of visible chunk keys
    private final ConcurrentHashMap<UUID, Set<Long>> playerVisibleChunks = new ConcurrentHashMap<>();

    // Player UUID -> ChunkKey -> Set of active virtual entity IDs currently spawned on client
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Set<Integer>>> playerChunkEntities = new ConcurrentHashMap<>();

    // Player UUID -> [lastYaw, lastPitch] for camera rotation debounce
    private final ConcurrentHashMap<UUID, float[]> playerLastLook = new ConcurrentHashMap<>();

    public CustomBlockVisibilityManager(
            CustomBlockStorage storage,
            CustomBlockRegistry registry,
            VirtualEntityIdManager idManager,
            BlockDisplayRenderer renderer,
            CustomBlockCullingService cullingService,
            CustomBlockCullingConfig cullingConfig,
            Logger logger) {
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.idManager = Objects.requireNonNull(idManager, "idManager cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.cullingService = cullingService != null ? cullingService : new CustomBlockCullingService();
        this.cullingConfig = cullingConfig != null ? cullingConfig : CustomBlockCullingConfig.defaultConfig();
        this.logger = logger != null ? logger : Logger.getLogger(CustomBlockVisibilityManager.class.getName());
    }

    public CustomBlockVisibilityManager(
            CustomBlockStorage storage,
            CustomBlockRegistry registry,
            VirtualEntityIdManager idManager,
            BlockDisplayRenderer renderer,
            Logger logger) {
        this(storage, registry, idManager, renderer, new CustomBlockCullingService(), CustomBlockCullingConfig.defaultConfig(), logger);
    }

    public int getViewDistanceChunks() {
        return viewDistanceChunks;
    }

    public void setViewDistanceChunks(int viewDistanceChunks) {
        this.viewDistanceChunks = Math.max(1, Math.min(32, viewDistanceChunks));
    }

    public CustomBlockCullingConfig getCullingConfig() {
        return cullingConfig;
    }

    public void setCullingConfig(CustomBlockCullingConfig cullingConfig) {
        this.cullingConfig = Objects.requireNonNull(cullingConfig, "cullingConfig cannot be null");
    }

    public CustomBlockCullingService getCullingService() {
        return cullingService;
    }

    /**
     * Handles player joining the server. Computes initial visible chunks and spawns custom blocks.
     */
    public void handlePlayerJoin(Player player) {
        if (player == null || !player.isOnline()) return;
        Location loc = player.getLocation();
        playerLastLook.put(player.getUniqueId(), new float[]{loc.getYaw(), loc.getPitch()});
        syncPlayerView(player);
    }

    /**
     * Handles player movement using chunk coordinates.
     */
    public void handlePlayerMove(Player player, int fromChunkX, int fromChunkZ, int toChunkX, int toChunkZ) {
        if (fromChunkX == toChunkX && fromChunkZ == toChunkZ) {
            return;
        }
        updatePlayerChunks(player, toChunkX, toChunkZ);
    }

    /**
     * Handles player move with Location objects, detecting chunk crossings and camera rotation deltas.
     *
     * @return true if visibility state was evaluated, false if intra-chunk movement without rotation
     */
    public boolean handlePlayerMove(Player player, Location from, Location to) {
        if (from == null || to == null || player == null) return false;
        int fromChunkX = from.getBlockX() >> 4;
        int fromChunkZ = from.getBlockZ() >> 4;
        int toChunkX = to.getBlockX() >> 4;
        int toChunkZ = to.getBlockZ() >> 4;

        if (fromChunkX != toChunkX || fromChunkZ != toChunkZ) {
            updatePlayerChunks(player, toChunkX, toChunkZ);
            playerLastLook.put(player.getUniqueId(), new float[]{to.getYaw(), to.getPitch()});
            return true;
        }

        // Intra-chunk movement: check camera rotation delta
        if (cullingConfig.isFrustumCullingEnabled()) {
            float[] last = playerLastLook.get(player.getUniqueId());
            float curYaw = to.getYaw();
            float curPitch = to.getPitch();

            if (last == null) {
                playerLastLook.put(player.getUniqueId(), new float[]{curYaw, curPitch});
                return false;
            }

            float dYaw = Math.abs(curYaw - last[0]);
            if (dYaw > 180f) dYaw = 360f - dYaw;
            float dPitch = Math.abs(curPitch - last[1]);

            if (dYaw >= cullingConfig.getFrustumAngleThreshold() || dPitch >= cullingConfig.getFrustumAngleThreshold()) {
                playerLastLook.put(player.getUniqueId(), new float[]{curYaw, curPitch});
                updatePlayerCullingInLoadedChunks(player, to);
                return true;
            }
        }

        return false;
    }

    /**
     * Initializes player custom block visibility at a specific location.
     */
    public void initPlayer(Player player, Location location) {
        if (player == null || location == null) return;
        playerLastLook.put(player.getUniqueId(), new float[]{location.getYaw(), location.getPitch()});

        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;

        Set<Long> newChunks = calculateChunksInRadius(centerChunkX, centerChunkZ, viewDistanceChunks);
        playerVisibleChunks.put(player.getUniqueId(), newChunks);

        World world = location.getWorld();
        for (long chunkKey : newChunks) {
            evaluateChunkCustomBlocks(player, world, chunkKey, location);
        }
    }

    /**
     * Handles player teleportation or respawn. Re-syncs visual state completely.
     */
    public void handlePlayerTeleport(Player player) {
        if (player == null || !player.isOnline()) return;
        clearPlayer(player);
        syncPlayerView(player);
    }

    /**
     * Handles player changing worlds. Purges old world entities and synchronizes new world.
     */
    public void handlePlayerWorldChange(Player player) {
        if (player == null || !player.isOnline()) return;
        clearPlayer(player);
        syncPlayerView(player);
    }

    /**
     * Handles player disconnecting. Cleans up all tracked memory to prevent memory leaks.
     */
    public void handlePlayerQuit(Player player) {
        if (player == null) return;
        clearPlayer(player);
    }

    /**
     * Handles a custom block placed in the world. Spawns visual BlockDisplay for viewing players.
     */
    public void handleBlockPlaced(Location location, CustomBlockDefinition definition) {
        if (location == null || definition == null || location.getWorld() == null) return;

        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long chunkKey = CustomBlockStorage.packChunkKey(chunkX, chunkZ);

        int entityId = idManager.getOrCreateId(world.getUID(), x, y, z);

        for (Player player : world.getPlayers()) {
            Set<Long> visible = playerVisibleChunks.get(player.getUniqueId());
            if (visible != null && visible.contains(chunkKey)) {
                Location pLoc = player.getLocation();
                boolean canSee = true;

                if (cullingConfig.isSurfaceCullingEnabled()) {
                    canSee = cullingService.isSurfaceExposed(world, x, y, z);
                }
                if (canSee) {
                    double bx = x + 0.5, by = y + 0.5, bz = z + 0.5;
                    canSee = cullingService.isInSphere(pLoc.getX(), pLoc.getY(), pLoc.getZ(), bx, by, bz, cullingConfig.getSphericalRadiusSq());
                }
                if (canSee && cullingConfig.isFrustumCullingEnabled()) {
                    double[] dir = cullingService.getDirection(pLoc.getYaw(), pLoc.getPitch());
                    canSee = cullingService.isInFrustum(pLoc.getX(), pLoc.getY() + 1.62, pLoc.getZ(),
                            dir[0], dir[1], dir[2], x + 0.5, y + 0.5, z + 0.5,
                            cullingConfig.getFrustumCosThreshold(), CustomBlockCullingService.DEFAULT_NEAR_DISTANCE_SQ);
                }

                if (canSee) {
                    renderer.spawnDisplay(player, entityId, location, definition.getModel());
                    playerChunkEntities.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet())
                            .add(entityId);
                }
            }
        }
    }

    /**
     * Handles a custom block broken or exploded. Despawns visual BlockDisplay for viewing players.
     */
    public void handleBlockBroken(Location location) {
        if (location == null || location.getWorld() == null) return;

        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long chunkKey = CustomBlockStorage.packChunkKey(chunkX, chunkZ);

        int entityId = idManager.removeId(world.getUID(), x, y, z);
        if (entityId == -1) return;

        for (Player player : world.getPlayers()) {
            Set<Long> visible = playerVisibleChunks.get(player.getUniqueId());
            if (visible != null && visible.contains(chunkKey)) {
                renderer.destroyDisplay(player, entityId);
                var chunkMap = playerChunkEntities.get(player.getUniqueId());
                if (chunkMap != null) {
                    var entitySet = chunkMap.get(chunkKey);
                    if (entitySet != null) {
                        entitySet.remove(entityId);
                    }
                }
            }
        }
    }

    /**
     * Handles chunk unloading. Destroys displays of that chunk for players tracking it.
     */
    public void handleChunkUnload(Chunk chunk) {
        if (chunk == null) return;
        handleChunkUnload(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    public void handleChunkUnload(World world, int chunkX, int chunkZ) {
        if (world == null) return;
        long chunkKey = CustomBlockStorage.packChunkKey(chunkX, chunkZ);

        for (Player player : world.getPlayers()) {
            var chunkMap = playerChunkEntities.get(player.getUniqueId());
            if (chunkMap != null) {
                Set<Integer> entities = chunkMap.remove(chunkKey);
                if (entities != null && !entities.isEmpty()) {
                    int[] ids = entities.stream().mapToInt(Integer::intValue).toArray();
                    renderer.destroyDisplay(player, ids);
                }
            }
            Set<Long> visible = playerVisibleChunks.get(player.getUniqueId());
            if (visible != null) {
                visible.remove(chunkKey);
            }
        }

        idManager.clearChunk(world.getUID(), chunkX, chunkZ);
    }

    /**
     * Synchronizes all custom blocks in view distance for a player.
     */
    private void syncPlayerView(Player player) {
        Location loc = player.getLocation();
        int centerChunkX = loc.getBlockX() >> 4;
        int centerChunkZ = loc.getBlockZ() >> 4;

        Set<Long> newChunks = calculateChunksInRadius(centerChunkX, centerChunkZ, viewDistanceChunks);
        playerVisibleChunks.put(player.getUniqueId(), newChunks);

        World world = player.getWorld();
        for (long chunkKey : newChunks) {
            evaluateChunkCustomBlocks(player, world, chunkKey, loc);
        }
    }

    /**
     * Updates player visibility when crossing into a new chunk.
     */
    private void updatePlayerChunks(Player player, int newChunkX, int newChunkZ) {
        Set<Long> previousChunks = playerVisibleChunks.getOrDefault(player.getUniqueId(), Collections.emptySet());
        Set<Long> currentChunks = calculateChunksInRadius(newChunkX, newChunkZ, viewDistanceChunks);

        playerVisibleChunks.put(player.getUniqueId(), currentChunks);

        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Entered chunks -> evaluate & spawn
        for (long chunkKey : currentChunks) {
            if (!previousChunks.contains(chunkKey)) {
                evaluateChunkCustomBlocks(player, world, chunkKey, playerLoc);
            }
        }

        // Left chunks -> destroy
        for (long chunkKey : previousChunks) {
            if (!currentChunks.contains(chunkKey)) {
                destroyChunkCustomBlocks(player, chunkKey);
            }
        }
    }

    /**
     * Re-evaluates culling state for all currently visible chunks of a player.
     */
    public void updatePlayerCullingInLoadedChunks(Player player, Location loc) {
        if (player == null || loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;

        Set<Long> visibleChunks = playerVisibleChunks.get(player.getUniqueId());
        if (visibleChunks == null || visibleChunks.isEmpty()) return;

        for (long chunkKey : visibleChunks) {
            evaluateChunkCustomBlocks(player, world, chunkKey, loc);
        }
    }

    /**
     * Handles chunk data packet delivery to client. Evaluates custom blocks contained in the chunk.
     */
    public void handleChunkData(Player player, int chunkX, int chunkZ) {
        if (player == null || !player.isOnline()) return;
        World world = player.getWorld();
        if (world == null) return;
        long chunkKey = CustomBlockStorage.packChunkKey(chunkX, chunkZ);
        playerVisibleChunks.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(chunkKey);
        evaluateChunkCustomBlocks(player, world, chunkKey, player.getLocation());
    }

    /**
     * Handles chunk unload packet delivery to client. Despawns virtual BlockDisplay entities.
     */
    public void handleChunkUnloadPacket(Player player, int chunkX, int chunkZ) {
        if (player == null) return;
        long chunkKey = CustomBlockStorage.packChunkKey(chunkX, chunkZ);
        var visibleSet = playerVisibleChunks.get(player.getUniqueId());
        if (visibleSet != null) {
            visibleSet.remove(chunkKey);
        }
        destroyChunkCustomBlocks(player, chunkKey);
    }

    private void evaluateChunkCustomBlocks(Player player, World world, long chunkKey, Location playerLoc) {
        int chunkX = CustomBlockStorage.unpackChunkX(chunkKey);
        int chunkZ = CustomBlockStorage.unpackChunkZ(chunkKey);

        Map<Integer, String> blocksInChunk = storage.getInChunk(world, chunkX, chunkZ);
        if (blocksInChunk.isEmpty()) return;

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        var playerEntities = playerChunkEntities.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet());

        double px = playerLoc.getX();
        double py = playerLoc.getY();
        double pz = playerLoc.getZ();
        double eyeX = px;
        double eyeY = py + 1.62;
        double eyeZ = pz;

        float yaw = playerLoc.getYaw();
        float pitch = playerLoc.getPitch();
        double[] dir = cullingService.getDirection(yaw, pitch);
        double fovCosThreshold = cullingConfig.getFrustumCosThreshold();

        List<Integer> toDestroy = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : blocksInChunk.entrySet()) {
            int localKey = entry.getKey();
            String defId = entry.getValue();

            var optDef = registry.get(defId);
            if (optDef.isEmpty()) continue;
            CustomBlockDefinition def = optDef.get();

            int worldX = baseX + CustomBlockStorage.unpackLocalX(localKey);
            int worldY = CustomBlockStorage.unpackLocalY(localKey);
            int worldZ = baseZ + CustomBlockStorage.unpackLocalZ(localKey);

            int entityId = idManager.getOrCreateId(world.getUID(), worldX, worldY, worldZ);
            boolean isSpawned = playerEntities.contains(entityId);

            // Tier 1: 1-Face Surface Culling
            if (cullingConfig.isSurfaceCullingEnabled() && !cullingService.isSurfaceExposed(world, worldX, worldY, worldZ)) {
                if (isSpawned) {
                    toDestroy.add(entityId);
                    playerEntities.remove(entityId);
                }
                continue;
            }

            // Tier 2: Spherical Radius with Hysteresis
            double bx = worldX + 0.5;
            double by = worldY + 0.5;
            double bz = worldZ + 0.5;
            double radiusSq = isSpawned ? cullingConfig.getHysteresisRadiusSq() : cullingConfig.getSphericalRadiusSq();

            if (!cullingService.isInSphere(px, py, pz, bx, by, bz, radiusSq)) {
                if (isSpawned) {
                    toDestroy.add(entityId);
                    playerEntities.remove(entityId);
                }
                continue;
            }

            // Tier 3: Conical Frustum Culling
            if (cullingConfig.isFrustumCullingEnabled()) {
                boolean inFrustum = cullingService.isInFrustum(eyeX, eyeY, eyeZ, dir[0], dir[1], dir[2],
                        bx, by, bz, fovCosThreshold, CustomBlockCullingService.DEFAULT_NEAR_DISTANCE_SQ);
                if (!inFrustum) {
                    if (isSpawned) {
                        toDestroy.add(entityId);
                        playerEntities.remove(entityId);
                    }
                    continue;
                }
            }

            // Passes all culling filters
            if (!isSpawned) {
                Location loc = new Location(world, worldX, worldY, worldZ);
                renderer.spawnDisplay(player, entityId, loc, def.getModel());
                playerEntities.add(entityId);
            }
        }

        if (!toDestroy.isEmpty()) {
            int[] array = toDestroy.stream().mapToInt(Integer::intValue).toArray();
            renderer.destroyDisplay(player, array);
        }
    }

    private void destroyChunkCustomBlocks(Player player, long chunkKey) {
        var chunkMap = playerChunkEntities.get(player.getUniqueId());
        if (chunkMap == null) return;

        Set<Integer> entities = chunkMap.remove(chunkKey);
        if (entities != null && !entities.isEmpty()) {
            int[] ids = entities.stream().mapToInt(Integer::intValue).toArray();
            renderer.destroyDisplay(player, ids);
        }
    }

    private void clearPlayer(Player player) {
        UUID uid = player.getUniqueId();
        playerVisibleChunks.remove(uid);
        playerLastLook.remove(uid);

        var chunkMap = playerChunkEntities.remove(uid);
        if (chunkMap != null && !chunkMap.isEmpty()) {
            List<Integer> allIds = new ArrayList<>();
            for (Set<Integer> ids : chunkMap.values()) {
                allIds.addAll(ids);
            }
            if (!allIds.isEmpty() && player.isOnline()) {
                int[] array = allIds.stream().mapToInt(Integer::intValue).toArray();
                renderer.destroyDisplay(player, array);
            }
        }
    }

    private Set<Long> calculateChunksInRadius(int centerX, int centerZ, int radius) {
        Set<Long> set = new HashSet<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                set.add(CustomBlockStorage.packChunkKey(centerX + dx, centerZ + dz));
            }
        }
        return set;
    }

    public void clear() {
        playerVisibleChunks.clear();
        playerChunkEntities.clear();
        playerLastLook.clear();
    }

    public void shutdown() {
        clear();
    }
}
