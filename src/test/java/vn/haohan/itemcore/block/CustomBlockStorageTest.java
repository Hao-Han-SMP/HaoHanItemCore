package vn.haohan.itemcore.block;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import vn.haohan.itemcore.internal.block.CustomBlockStorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomBlockStorage Tests")
class CustomBlockStorageTest {

    private CustomBlockStorage storage;
    private final UUID worldUid1 = UUID.randomUUID();
    private final UUID worldUid2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        Mockito.when(mockPlugin.getName()).thenReturn("haohanitemcore");
        Mockito.when(mockPlugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));
        storage = new CustomBlockStorage(mockPlugin);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 64, 0",
            "15, 319, 15",
            "0, -64, 0",
            "7, 0, 7",
            "3, -20, 12",
            "15, -64, 15",
            "0, 319, 0"
    })
    @DisplayName("Should accurately pack and unpack local coordinates")
    void testLocalCoordinatePacking(int x, int y, int z) {
        int packed = CustomBlockStorage.packLocalKey(x, y, z);
        assertEquals(x, CustomBlockStorage.unpackLocalX(packed), "Unpacked X must match original");
        assertEquals(y, CustomBlockStorage.unpackLocalY(packed), "Unpacked Y must match original");
        assertEquals(z, CustomBlockStorage.unpackLocalZ(packed), "Unpacked Z must match original");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "100, -200",
            "-1500, 3000",
            "2147483, -2147483",
            "-1, -1"
    })
    @DisplayName("Should accurately pack and unpack chunk coordinates")
    void testChunkCoordinatePacking(int chunkX, int chunkZ) {
        long packed = CustomBlockStorage.packChunkKey(chunkX, chunkZ);
        assertEquals(chunkX, CustomBlockStorage.unpackChunkX(packed), "Unpacked ChunkX must match original");
        assertEquals(chunkZ, CustomBlockStorage.unpackChunkZ(packed), "Unpacked ChunkZ must match original");
    }

    @Test
    @DisplayName("Should set, get, and remove custom block in memory")
    void testSetGetRemoveBlock() {
        storage.set(worldUid1, 10, 64, 25, "haohan:crystal_block");

        assertEquals("haohan:crystal_block", storage.get(worldUid1, 10, 64, 25));
        assertEquals(1, storage.size());

        // World isolation: check different world returns null
        assertNull(storage.get(worldUid2, 10, 64, 25));

        // Coordinate isolation: adjacent block returns null
        assertNull(storage.get(worldUid1, 10, 65, 25));

        // Remove block
        String removed = storage.remove(worldUid1, 10, 64, 25);
        assertEquals("haohan:crystal_block", removed);
        assertNull(storage.get(worldUid1, 10, 64, 25));
        assertEquals(0, storage.size());
    }

    @Test
    @DisplayName("Should track dirty chunks when blocks are modified")
    void testDirtyChunkTracking() {
        int x = 35; // chunkX = 2, localX = 3
        int z = -10; // chunkZ = -1, localZ = 6
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long chunkKey = CustomBlockStorage.packChunkKey(chunkX, chunkZ);

        assertFalse(storage.isDirty(chunkKey));

        storage.set(worldUid1, x, 70, z, "haohan:ruby_block");
        assertTrue(storage.isDirty(chunkKey));

        storage.markClean(chunkKey);
        assertFalse(storage.isDirty(chunkKey));

        storage.remove(worldUid1, x, 70, z);
        assertTrue(storage.isDirty(chunkKey));
    }

    @Test
    @DisplayName("Should correctly serialize and deserialize compact chunk binary format")
    void testBinarySerializationFormat() throws Exception {
        int chunkX = 5;
        int chunkZ = -8;
        long chunkKey = CustomBlockStorage.packChunkKey(chunkX, chunkZ);

        // Put 3 blocks in this chunk
        storage.set(worldUid1, 5 * 16 + 2, 64, -8 * 16 + 3, "haohan:block_a");
        storage.set(worldUid1, 5 * 16 + 5, 120, -8 * 16 + 7, "haohan:block_b");
        storage.set(worldUid1, 5 * 16 + 14, -10, -8 * 16 + 15, "haohan:block_c");

        Map<Integer, String> blocks = storage.getInChunk(worldUid1, chunkX, chunkZ);
        assertEquals(3, blocks.size());

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(blocks.size());
        for (Map.Entry<Integer, String> entry : blocks.entrySet()) {
            out.writeInt(entry.getKey());
            out.writeUTF(entry.getValue());
        }
        out.flush();
        byte[] bytes = baos.toByteArray();

        // Deserialize in a fresh reader
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        int count = in.readInt();
        assertEquals(3, count);
        for (int i = 0; i < count; i++) {
            int localKey = in.readInt();
            String id = in.readUTF();
            assertTrue(blocks.containsKey(localKey));
            assertEquals(blocks.get(localKey), id);
        }
        assertEquals(0, in.available());
    }

    @Test
    @DisplayName("Should clear all cached blocks")
    void testClearCache() {
        storage.set(worldUid1, 0, 64, 0, "test:block");
        storage.set(worldUid2, 16, 64, 16, "test:block");
        assertEquals(2, storage.size());

        storage.clearCache();
        assertEquals(0, storage.size());
        assertNull(storage.get(worldUid1, 0, 64, 0));
        assertNull(storage.get(worldUid2, 16, 64, 16));
    }

    @Test
    @DisplayName("Should persist into Chunk PDC and reload correctly")
    void testChunkPdcSaveAndLoad() {
        org.bukkit.World mockWorld = Mockito.mock(org.bukkit.World.class);
        Mockito.when(mockWorld.getUID()).thenReturn(worldUid1);

        org.bukkit.Chunk mockChunk = Mockito.mock(org.bukkit.Chunk.class);
        Mockito.when(mockChunk.getWorld()).thenReturn(mockWorld);
        Mockito.when(mockChunk.getX()).thenReturn(3);
        Mockito.when(mockChunk.getZ()).thenReturn(4);

        java.util.Map<org.bukkit.NamespacedKey, Object> pdcMap = new java.util.HashMap<>();
        org.bukkit.persistence.PersistentDataContainer pdc = Mockito.mock(org.bukkit.persistence.PersistentDataContainer.class);
        Mockito.doAnswer(inv -> {
            org.bukkit.NamespacedKey k = inv.getArgument(0);
            byte[] bytes = inv.getArgument(2);
            pdcMap.put(k, bytes);
            return null;
        }).when(pdc).set(Mockito.any(), Mockito.eq(org.bukkit.persistence.PersistentDataType.BYTE_ARRAY), Mockito.any());

        Mockito.doAnswer(inv -> {
            org.bukkit.NamespacedKey k = inv.getArgument(0);
            return (byte[]) pdcMap.get(k);
        }).when(pdc).get(Mockito.any(), Mockito.eq(org.bukkit.persistence.PersistentDataType.BYTE_ARRAY));

        Mockito.doAnswer(inv -> {
            org.bukkit.NamespacedKey k = inv.getArgument(0);
            return pdcMap.containsKey(k);
        }).when(pdc).has(Mockito.any(), Mockito.eq(org.bukkit.persistence.PersistentDataType.BYTE_ARRAY));

        Mockito.when(mockChunk.getPersistentDataContainer()).thenReturn(pdc);

        // Put a block in chunk (3, 4) -> world coords: x = 3*16 + 5 = 53, z = 4*16 + 7 = 71
        storage.set(worldUid1, 53, 64, 71, "haohan:ruby_ore");
        assertEquals(1, storage.size());

        // Save chunk into PDC
        storage.saveChunk(mockChunk);
        assertFalse(pdcMap.isEmpty(), "PDC should contain serialized block data");

        // Clear in-memory cache completely (simulating restart)
        storage.clearCache();
        assertEquals(0, storage.size());
        assertNull(storage.get(worldUid1, 53, 64, 71));

        // Load chunk back from PDC (simulating chunk load on restart)
        storage.loadChunk(mockChunk);
        assertEquals("haohan:ruby_ore", storage.get(worldUid1, 53, 64, 71));
        assertEquals(1, storage.size());
    }
}

