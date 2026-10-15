package vn.haohan.itemcore.block;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Server;
import org.bukkit.block.data.type.NoteBlock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.NativeCustomBlock;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockRegistry;
import vn.haohan.itemcore.internal.block.NoteBlockVariationTable;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteBlockVariationTableTest {

    private static void setBukkitServer(Server server) {
        try {
            Field field = Bukkit.class.getDeclaredField("server");
            field.setAccessible(true);
            field.set(null, server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        setBukkitServer(null);
    }

    @Test
    @DisplayName("Variation 0 must map to instrument bass drum, note 0, powered false (ID 25)")
    void testVariationZero() {
        Instrument instrument = NoteBlockVariationTable.getInstrument(0);
        int note = NoteBlockVariationTable.getNote(0);
        boolean powered = NoteBlockVariationTable.isPowered(0);

        assertEquals(Instrument.BASS_DRUM, instrument);
        assertEquals(0, note);
        assertFalse(powered);
    }

    @Test
    @DisplayName("Variation boundary points: 0, 374, 375, 774")
    void testVariationBoundaries() {
        // v = 0 -> state 25: BASS_DRUM, note 0, unpowered
        assertEquals(Instrument.BASS_DRUM, NoteBlockVariationTable.getInstrument(0));
        assertEquals(0, NoteBlockVariationTable.getNote(0));
        assertFalse(NoteBlockVariationTable.isPowered(0));

        // v = 374 -> state 399: PLING (inst 15), note 24, unpowered
        assertEquals(Instrument.PLING, NoteBlockVariationTable.getInstrument(374));
        assertEquals(24, NoteBlockVariationTable.getNote(374));
        assertFalse(NoteBlockVariationTable.isPowered(374));

        // v = 375 -> state 400: PIANO (inst 0), note 0, powered
        assertEquals(Instrument.PIANO, NoteBlockVariationTable.getInstrument(375));
        assertEquals(0, NoteBlockVariationTable.getNote(375));
        assertTrue(NoteBlockVariationTable.isPowered(375));

        // v = 774 -> state 799: PLING (inst 15), note 24, powered
        assertEquals(Instrument.PLING, NoteBlockVariationTable.getInstrument(774));
        assertEquals(24, NoteBlockVariationTable.getNote(774));
        assertTrue(NoteBlockVariationTable.isPowered(774));
    }

    @Test
    @DisplayName("Variation mapping must be reversible for all 775 indices")
    void testBidirectionalMapping() {
        for (int v = 0; v < 775; v++) {
            Instrument inst = NoteBlockVariationTable.getInstrument(v);
            int note = NoteBlockVariationTable.getNote(v);
            boolean powered = NoteBlockVariationTable.isPowered(v);

            int resolved = NoteBlockVariationTable.resolveVariation(inst, note, powered);
            assertEquals(v, resolved, "Reversible variation for " + v);
        }
    }

    @Test
    @DisplayName("Vanilla unpowered PIANO notes 0..24 must resolve to -1")
    void testVanillaReservation() {
        for (int note = 0; note <= 24; note++) {
            int resolved = NoteBlockVariationTable.resolveVariation(Instrument.PIANO, note, false);
            assertEquals(-1, resolved, "Vanilla harp note " + note + " must resolve to -1");
        }
    }

    @Test
    @DisplayName("Mob head instruments must resolve to -1 (not custom variations)")
    void testMobHeadInstrumentsResolution() {
        Instrument[] mobHeads = {
                Instrument.ZOMBIE, Instrument.SKELETON, Instrument.CREEPER,
                Instrument.DRAGON, Instrument.WITHER_SKELETON, Instrument.PIGLIN,
                Instrument.CUSTOM_HEAD
        };
        for (Instrument mobHead : mobHeads) {
            assertEquals(-1, NoteBlockVariationTable.resolveVariation(mobHead, 0, false));
            assertEquals(-1, NoteBlockVariationTable.resolveVariation(mobHead, 0, true));
        }
    }

    @Test
    @DisplayName("Out-of-bounds variations must throw IllegalArgumentException")
    void testOutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> NoteBlockVariationTable.getInstrument(-1));
        assertThrows(IllegalArgumentException.class, () -> NoteBlockVariationTable.getInstrument(775));
        assertThrows(IllegalArgumentException.class, () -> NoteBlockVariationTable.getNote(-1));
        assertThrows(IllegalArgumentException.class, () -> NoteBlockVariationTable.getNote(775));
        assertThrows(IllegalArgumentException.class, () -> NoteBlockVariationTable.isPowered(-1));
        assertThrows(IllegalArgumentException.class, () -> NoteBlockVariationTable.isPowered(775));

        assertEquals(-1, NoteBlockVariationTable.resolveVariation(null, 0, false));
        assertEquals(-1, NoteBlockVariationTable.resolveVariation(Instrument.PIANO, -1, false));
        assertEquals(-1, NoteBlockVariationTable.resolveVariation(Instrument.PIANO, 25, false));
    }

    @Test
    @DisplayName("resolveVariation with NoteBlock block data")
    void testResolveVariationNoteBlock() {
        NoteBlock mockNoteBlock = mock(NoteBlock.class);
        when(mockNoteBlock.getInstrument()).thenReturn(Instrument.BASS_DRUM);
        when(mockNoteBlock.getNote()).thenReturn(new Note(0));
        when(mockNoteBlock.isPowered()).thenReturn(false);

        assertEquals(0, NoteBlockVariationTable.resolveVariation(mockNoteBlock));
        assertEquals(-1, NoteBlockVariationTable.resolveVariation((NoteBlock) null));
    }

    @Test
    @DisplayName("createBlockData must configure NoteBlock block data matching variation")
    void testCreateBlockData() {
        Server server = mock(Server.class);
        NoteBlock noteBlock = mock(NoteBlock.class);
        when(server.createBlockData(Material.NOTE_BLOCK)).thenReturn(noteBlock);
        setBukkitServer(server);

        NoteBlock created = NoteBlockVariationTable.createBlockData(10);
        assertNotNull(created);
        verify(noteBlock).setInstrument(NoteBlockVariationTable.getInstrument(10));
        verify(noteBlock).setNote(new Note(NoteBlockVariationTable.getNote(10)));
        verify(noteBlock).setPowered(NoteBlockVariationTable.isPowered(10));
    }

    @Test
    @DisplayName("Registry should index NativeCustomBlock by variation and BlockData")
    void testRegistryVariationIndex() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:test_ore")
                .customVariation(10)
                .build();

        registry.register(customBlock);

        Optional<NativeCustomBlock> byVar = registry.getByVariation(10);
        assertTrue(byVar.isPresent());
        assertEquals("haohan:test_ore", byVar.get().getId());

        assertTrue(registry.getByVariation(99).isEmpty());

        NoteBlock noteBlock = mock(NoteBlock.class);
        Instrument inst = NoteBlockVariationTable.getInstrument(10);
        int note = NoteBlockVariationTable.getNote(10);
        boolean powered = NoteBlockVariationTable.isPowered(10);
        when(noteBlock.getInstrument()).thenReturn(inst);
        when(noteBlock.getNote()).thenReturn(new Note(note));
        when(noteBlock.isPowered()).thenReturn(powered);

        Optional<NativeCustomBlock> byData = registry.getByBlockData(noteBlock);
        assertTrue(byData.isPresent());
        assertEquals("haohan:test_ore", byData.get().getId());

        NativeCustomBlock duplicateVar = NativeCustomBlock.builder("haohan:duplicate_ore")
                .customVariation(10)
                .build();
        assertThrows(IllegalArgumentException.class, () -> registry.register(duplicateVar));
        // Ensure first registration wasn't corrupted
        assertTrue(registry.getByVariation(10).isPresent());
        assertEquals("haohan:test_ore", registry.getByVariation(10).get().getId());

        registry.clear();
        assertTrue(registry.getByVariation(10).isEmpty());
        assertTrue(registry.getByBlockData(noteBlock).isEmpty());
    }

    @Test
    @DisplayName("Registry handles concurrent access for variation and block data lookups")
    void testConcurrentRegistryVariationLookups() throws InterruptedException {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        int count = 50;
        for (int i = 0; i < count; i++) {
            NativeCustomBlock block = NativeCustomBlock.builder("haohan:concurrent_block_" + i)
                    .customVariation(i)
                    .build();
            registry.register(block);
        }

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCounter = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    for (int iter = 0; iter < 100; iter++) {
                        for (int i = 0; i < count; i++) {
                            Optional<NativeCustomBlock> opt = registry.getByVariation(i);
                            if (opt.isPresent() && opt.get().getCustomVariation() == i) {
                                successCounter.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(done);
        assertEquals(threads * 100 * count, successCounter.get());
    }
}
