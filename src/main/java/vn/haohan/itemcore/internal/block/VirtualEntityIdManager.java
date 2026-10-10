package vn.haohan.itemcore.internal.block;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages unique, collision-free virtual entity IDs for packet-level BlockDisplay rendering.
 *
 * <p>Virtual entity IDs are allocated starting at 1,500,000,000 to ensure they never collide
 * with Bukkit/NMS server-side entity IDs. IDs are shared across all viewing players for a given
 * block position to maximize packet reuse and minimize memory overhead.</p>
 */
public final class VirtualEntityIdManager {

    private static final int INITIAL_VIRTUAL_ID = 1_500_000_000;
    private final AtomicInteger counter = new AtomicInteger(INITIAL_VIRTUAL_ID);

    // World UUID -> ChunkKey -> LocalKey -> Virtual Entity ID
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Integer>>> blockEntityIds = new ConcurrentHashMap<>();

    /**
     * Allocates a new raw virtual entity ID.
     *
     * @return a unique virtual entity ID
     */
    public int allocateRawId() {
        int id = counter.incrementAndGet();
        if (id < 0) { // integer overflow guard
            counter.set(INITIAL_VIRTUAL_ID);
            id = counter.incrementAndGet();
        }
        return id;
    }

    /**
     * Gets or creates a virtual entity ID for the given block position.
     *
     * @param worldUid the world UUID
     * @param x world X
     * @param y world Y
     * @param z world Z
     * @return the virtual entity ID
     */
    public int getOrCreateId(UUID worldUid, int x, int y, int z) {
        Objects.requireNonNull(worldUid, "worldUid cannot be null");
        long chunkKey = CustomBlockStorage.packChunkKey(x >> 4, z >> 4);
        int localKey = CustomBlockStorage.packLocalKey(x, y, z);

        return blockEntityIds.computeIfAbsent(worldUid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(localKey, k -> allocateRawId());
    }

    /**
     * Gets the virtual entity ID for a block if it exists.
     *
     * @param worldUid the world UUID
     * @param x world X
     * @param y world Y
     * @param z world Z
     * @return the virtual entity ID, or -1 if none assigned
     */
    public int getId(UUID worldUid, int x, int y, int z) {
        if (worldUid == null) return -1;
        long chunkKey = CustomBlockStorage.packChunkKey(x >> 4, z >> 4);
        int localKey = CustomBlockStorage.packLocalKey(x, y, z);

        var chunks = blockEntityIds.get(worldUid);
        if (chunks == null) return -1;
        var blocks = chunks.get(chunkKey);
        if (blocks == null) return -1;
        Integer id = blocks.get(localKey);
        return id != null ? id : -1;
    }

    /**
     * Removes and frees the virtual entity ID for a block position.
     *
     * @param worldUid the world UUID
     * @param x world X
     * @param y world Y
     * @param z world Z
     * @return the removed virtual entity ID, or -1 if none was assigned
     */
    public int removeId(UUID worldUid, int x, int y, int z) {
        if (worldUid == null) return -1;
        long chunkKey = CustomBlockStorage.packChunkKey(x >> 4, z >> 4);
        int localKey = CustomBlockStorage.packLocalKey(x, y, z);

        var chunks = blockEntityIds.get(worldUid);
        if (chunks == null) return -1;
        var blocks = chunks.get(chunkKey);
        if (blocks == null) return -1;

        Integer removed = blocks.remove(localKey);
        if (removed != null && blocks.isEmpty()) {
            chunks.remove(chunkKey);
        }
        return removed != null ? removed : -1;
    }

    /**
     * Clears all virtual entity IDs for an unloaded chunk.
     *
     * @param worldUid the world UUID
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    public void clearChunk(UUID worldUid, int chunkX, int chunkZ) {
        if (worldUid == null) return;
        var chunks = blockEntityIds.get(worldUid);
        if (chunks == null) return;
        long chunkKey = CustomBlockStorage.packChunkKey(chunkX, chunkZ);
        chunks.remove(chunkKey);
    }

    /**
     * Clears all allocated virtual entity IDs across all worlds.
     */
    public void clear() {
        blockEntityIds.clear();
        counter.set(INITIAL_VIRTUAL_ID);
    }
}
