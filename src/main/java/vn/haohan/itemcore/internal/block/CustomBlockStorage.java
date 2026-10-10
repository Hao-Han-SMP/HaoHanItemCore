package vn.haohan.itemcore.internal.block;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compact spatial storage and chunk-level persistence for custom blocks.
 *
 * <p>Hierarchy: {@code World UUID -> Chunk Key -> Local Block Key -> Custom Block ID}.</p>
 * <p>Persistence: Compact binary chunk serialization in Chunk PersistentDataContainer.</p>
 * <p>Provides O(1) thread-safe lookups without ticking or global world scanning.</p>
 */
public final class CustomBlockStorage {

    private final Plugin plugin;
    private final Logger logger;
    private final NamespacedKey chunkStorageKey;

    // World UUID -> ChunkKey (packed long) -> LocalKey (packed int) -> CustomBlock ID
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, ConcurrentHashMap<Integer, String>>> worldIndex = new ConcurrentHashMap<>();

    // Tracks chunks modified since last persistence write
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();

    public CustomBlockStorage(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.logger = plugin.getLogger();
        NamespacedKey key = null;
        try {
            key = new NamespacedKey(plugin, "custom_blocks_storage");
        } catch (Throwable t) {
            key = NamespacedKey.fromString("haohanitemcore:custom_blocks_storage");
        }
        this.chunkStorageKey = key != null ? key : NamespacedKey.fromString("haohanitemcore:custom_blocks_storage");
    }

    /**
     * Packs chunk coordinates into a 64-bit integer.
     */
    public static long packChunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Gets chunk X from a packed chunk key.
     */
    public static int unpackChunkX(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    /**
     * Gets chunk Z from a packed chunk key.
     */
    public static int unpackChunkZ(long chunkKey) {
        return (int) chunkKey;
    }

    /**
     * Packs local chunk block coordinates (-64 <= y <= 320) into a single 32-bit integer.
     *
     * <p>Bit layout: [14 bits: rx (0..15)] [10 bits: localY (0..511)] [4 bits: rz (0..15)]</p>
     */
    public static int packLocalKey(int x, int y, int z) {
        int rx = x & 15;
        int rz = z & 15;
        int localY = (y + 64) & 0x3FF; // supports Y from -64 up to 447
        return (rx << 14) | (localY << 4) | rz;
    }

    public static int unpackLocalX(int localKey) {
        return (localKey >> 14) & 15;
    }

    public static int unpackLocalY(int localKey) {
        return ((localKey >> 4) & 0x3FF) - 64;
    }

    public static int unpackLocalZ(int localKey) {
        return localKey & 15;
    }

    /**
     * Gets the custom block ID at the specified world coordinate.
     *
     * <p>Thread requirement: Thread-safe, safe for async execution.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param world the world
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return custom block ID, or null if no custom block is present
     */
    public String get(World world, int x, int y, int z) {
        if (world == null) return null;
        return get(world.getUID(), x, y, z);
    }

    /**
     * Gets the custom block ID by world UUID and coordinates.
     */
    public String get(UUID worldUid, int x, int y, int z) {
        ConcurrentHashMap<Long, ConcurrentHashMap<Integer, String>> chunks = worldIndex.get(worldUid);
        if (chunks == null) return null;

        long chunkKey = packChunkKey(x >> 4, z >> 4);
        ConcurrentHashMap<Integer, String> blockMap = chunks.get(chunkKey);
        if (blockMap == null) return null;

        int localKey = packLocalKey(x, y, z);
        return blockMap.get(localKey);
    }

    /**
     * Sets a custom block entry at the given location.
     *
     * <p>Thread requirement: Safe to call concurrently.</p>
     *
     * @param world the world
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @param customBlockId ID of custom block; must not be null
     */
    public void set(World world, int x, int y, int z, String customBlockId) {
        Objects.requireNonNull(world, "world cannot be null");
        set(world.getUID(), x, y, z, customBlockId);
    }

    public void set(UUID worldUid, int x, int y, int z, String customBlockId) {
        Objects.requireNonNull(worldUid, "worldUid cannot be null");
        Objects.requireNonNull(customBlockId, "customBlockId cannot be null");

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long chunkKey = packChunkKey(chunkX, chunkZ);
        int localKey = packLocalKey(x, y, z);

        worldIndex.computeIfAbsent(worldUid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>())
                .put(localKey, customBlockId);

        dirtyChunks.add(chunkKey);
    }

    /**
     * Removes a custom block entry from storage.
     *
     * @param world the world
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return previous custom block ID, or null if none was stored
     */
    public String remove(World world, int x, int y, int z) {
        if (world == null) return null;
        return remove(world.getUID(), x, y, z);
    }

    public String remove(UUID worldUid, int x, int y, int z) {
        if (worldUid == null) return null;

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long chunkKey = packChunkKey(chunkX, chunkZ);
        int localKey = packLocalKey(x, y, z);

        ConcurrentHashMap<Long, ConcurrentHashMap<Integer, String>> chunks = worldIndex.get(worldUid);
        if (chunks == null) return null;

        ConcurrentHashMap<Integer, String> blockMap = chunks.get(chunkKey);
        if (blockMap == null) return null;

        String removed = blockMap.remove(localKey);
        if (removed != null) {
            dirtyChunks.add(chunkKey);
            if (blockMap.isEmpty()) {
                chunks.remove(chunkKey);
            }
        }
        return removed;
    }

    /**
     * Returns an unmodifiable snapshot of custom blocks located in a chunk.
     *
     * @param world the world
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return map of localKey -> customBlockId
     */
    public Map<Integer, String> getInChunk(World world, int chunkX, int chunkZ) {
        if (world == null) return Collections.emptyMap();
        return getInChunk(world.getUID(), chunkX, chunkZ);
    }

    public Map<Integer, String> getInChunk(UUID worldUid, int chunkX, int chunkZ) {
        if (worldUid == null) return Collections.emptyMap();
        ConcurrentHashMap<Long, ConcurrentHashMap<Integer, String>> chunks = worldIndex.get(worldUid);
        if (chunks == null) return Collections.emptyMap();

        long chunkKey = packChunkKey(chunkX, chunkZ);
        ConcurrentHashMap<Integer, String> blockMap = chunks.get(chunkKey);
        if (blockMap == null || blockMap.isEmpty()) return Collections.emptyMap();

        return new HashMap<>(blockMap);
    }

    /**
     * Loads custom block entries for a chunk from its PersistentDataContainer into memory.
     *
     * <p>Must be invoked during ChunkLoadEvent or when reading chunk on main thread.</p>
     *
     * @param chunk the loaded chunk
     */
    public void loadChunk(Chunk chunk) {
        if (chunk == null) return;
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        long chunkKey = packChunkKey(chunkX, chunkZ);

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        byte[] bytes = pdc.get(chunkStorageKey, PersistentDataType.BYTE_ARRAY);
        if (bytes == null || bytes.length == 0) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int count = in.readInt();
            if (count <= 0) return;

            ConcurrentHashMap<Integer, String> blockMap = worldIndex
                    .computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>());

            for (int i = 0; i < count; i++) {
                int localKey = in.readInt();
                String id = in.readUTF();
                blockMap.put(localKey, id);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to deserialize custom blocks for chunk " + chunkX + "," + chunkZ, e);
        }
    }

    /**
     * Persists custom blocks to chunk PDC and purges chunk memory index to prevent memory leaks.
     *
     * <p>Must be invoked during ChunkUnloadEvent.</p>
     *
     * @param chunk the unloading chunk
     */
    public void unloadChunk(Chunk chunk) {
        if (chunk == null) return;
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        long chunkKey = packChunkKey(chunkX, chunkZ);

        saveChunk(chunk);

        // Evict from memory
        ConcurrentHashMap<Long, ConcurrentHashMap<Integer, String>> chunks = worldIndex.get(world.getUID());
        if (chunks != null) {
            chunks.remove(chunkKey);
        }
        dirtyChunks.remove(chunkKey);
    }

    /**
     * Saves chunk data into PDC if modified.
     *
     * @param chunk the chunk to save
     */
    public void saveChunk(Chunk chunk) {
        if (chunk == null) return;
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        long chunkKey = packChunkKey(chunkX, chunkZ);

        ConcurrentHashMap<Long, ConcurrentHashMap<Integer, String>> chunks = worldIndex.get(world.getUID());
        ConcurrentHashMap<Integer, String> blockMap = (chunks != null) ? chunks.get(chunkKey) : null;

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        if (blockMap == null || blockMap.isEmpty()) {
            if (pdc.has(chunkStorageKey, PersistentDataType.BYTE_ARRAY)) {
                pdc.remove(chunkStorageKey);
            }
            dirtyChunks.remove(chunkKey);
            return;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(blockMap.size());
            for (Map.Entry<Integer, String> entry : blockMap.entrySet()) {
                out.writeInt(entry.getKey());
                out.writeUTF(entry.getValue());
            }
            out.flush();
            pdc.set(chunkStorageKey, PersistentDataType.BYTE_ARRAY, baos.toByteArray());
            dirtyChunks.remove(chunkKey);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to serialize custom blocks for chunk " + chunkX + "," + chunkZ, e);
        }
    }

    /**
     * Loads custom block entries for all currently loaded chunks across all worlds.
     */
    public void loadAll(org.bukkit.Server server) {
        if (server == null) return;
        for (World world : server.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                loadChunk(chunk);
            }
        }
    }

    /**
     * Saves all modified chunks across all loaded worlds to their PersistentDataContainers.
     */
    public void saveAll(org.bukkit.Server server) {
        if (server == null) return;
        for (World world : server.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                long chunkKey = packChunkKey(chunk.getX(), chunk.getZ());
                if (dirtyChunks.contains(chunkKey)) {
                    saveChunk(chunk);
                }
            }
        }
    }

    public boolean isDirty(long chunkKey) {
        return dirtyChunks.contains(chunkKey);
    }

    public void markClean(long chunkKey) {
        dirtyChunks.remove(chunkKey);
    }

    /**
     * Clears all in-memory indices across all worlds.
     */
    public void clear() {
        worldIndex.clear();
        dirtyChunks.clear();
    }

    public void clearCache() {
        clear();
    }

    /**
     * Returns total count of loaded custom blocks across all worlds.
     */
    public int size() {
        int total = 0;
        for (var worldMap : worldIndex.values()) {
            for (var chunkMap : worldMap.values()) {
                total += chunkMap.size();
            }
        }
        return total;
    }
}
