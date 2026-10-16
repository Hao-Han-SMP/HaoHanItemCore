package vn.haohan.itemcore.block;

import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.api.block.NativeCustomBlock;
import vn.haohan.itemcore.internal.block.AdjacentNoteBlockUpdateHelper;
import vn.haohan.itemcore.internal.block.NoteBlockEventListener;
import vn.haohan.itemcore.internal.block.NoteBlockVariationTable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteBlockEventListenerTest {

    private Block createMockNativeNoteBlock(int variation, NativeCustomBlock customBlock, CustomBlockRegistry registry) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.NOTE_BLOCK);
        NoteBlock noteBlock = mock(NoteBlock.class);
        when(noteBlock.getInstrument()).thenReturn(NoteBlockVariationTable.getInstrument(variation));
        when(noteBlock.getNote()).thenReturn(new Note(NoteBlockVariationTable.getNote(variation)));
        when(noteBlock.isPowered()).thenReturn(NoteBlockVariationTable.isPowered(variation));
        when(block.getBlockData()).thenReturn(noteBlock);
        when(registry.getByVariation(variation)).thenReturn(Optional.ofNullable(customBlock));
        return block;
    }

    @Test
    @DisplayName("Right-clicking a native custom block must deny interaction to prevent note tune")
    void testDenyRightClickTune() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(10).build();

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);
        Block block = createMockNativeNoteBlock(10, customBlock, registry);

        assertTrue(listener.isNativeBlock(block));

        Player player = mock(Player.class);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerTuneNoteBlock(event);
        assertEquals(Event.Result.DENY, event.useInteractedBlock());
    }

    @Test
    @DisplayName("Right-clicking a vanilla note block must not be denied")
    void testVanillaNoteBlockAllowed() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        Block vanillaBlock = mock(Block.class);
        when(vanillaBlock.getType()).thenReturn(Material.NOTE_BLOCK);
        NoteBlock vanillaNoteData = mock(NoteBlock.class);
        when(vanillaNoteData.getInstrument()).thenReturn(Instrument.PIANO);
        when(vanillaNoteData.getNote()).thenReturn(new Note(0));
        when(vanillaNoteData.isPowered()).thenReturn(false);
        when(vanillaBlock.getBlockData()).thenReturn(vanillaNoteData);

        assertFalse(listener.isNativeBlock(vanillaBlock));

        Player player = mock(Player.class);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, vanillaBlock, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerTuneNoteBlock(event);
        assertNotEquals(Event.Result.DENY, event.useInteractedBlock());
    }

    @Test
    @DisplayName("Right-clicking a native custom block with explicit interactHandler executes handler and denies vanilla note tune")
    void testExplicitInteractHandlerExecuted() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        AtomicBoolean handlerInvoked = new AtomicBoolean(false);

        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:interactive_block")
                .customVariation(15)
                .onInteract(ctx -> handlerInvoked.set(true))
                .build();

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);
        Block block = createMockNativeNoteBlock(15, customBlock, registry);

        Player player = mock(Player.class);
        when(player.isSneaking()).thenReturn(false);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerTuneNoteBlock(event);

        assertTrue(handlerInvoked.get());
        assertEquals(Event.Result.DENY, event.useInteractedBlock());
    }

    @Test
    @DisplayName("Right-clicking top face of native custom block with flint and steel denies item use")
    void testFlintAndSteelDeniedOnTopFace() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(18).build();
        Block block = createMockNativeNoteBlock(18, customBlock, registry);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);
        Player player = mock(Player.class);
        ItemStack flint = mock(ItemStack.class);
        when(flint.getType()).thenReturn(Material.FLINT_AND_STEEL);

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, flint, block, BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerTuneNoteBlock(event);

        assertEquals(Event.Result.DENY, event.useInteractedBlock());
        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    @DisplayName("NotePlayEvent on native custom block is cancelled")
    void testNotePlayEventCancelled() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(20).build();
        Block block = createMockNativeNoteBlock(20, customBlock, registry);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        NotePlayEvent event = new NotePlayEvent(block, Instrument.BASS_DRUM, new Note(5));
        listener.onNotePlay(event);
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("Placing piston or shulker box directly above native note block is cancelled")
    void testUnsupportedBlockAboveNoteBlockCancelled() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(50).build();
        Block nativeBlockBelow = createMockNativeNoteBlock(50, customBlock, registry);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.PISTON);
        when(placedBlock.getRelative(BlockFace.DOWN)).thenReturn(nativeBlockBelow);

        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(placedBlock);

        listener.onPlacingUnsupportedBlockAboveNoteBlock(event);
        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Native custom block is protected against catching fire")
    void testFireIgnitionCancelled() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(55).build();
        Block nativeBlock = createMockNativeNoteBlock(55, customBlock, registry);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        BlockIgniteEvent igniteEvent = mock(BlockIgniteEvent.class);
        when(igniteEvent.getBlock()).thenReturn(nativeBlock);

        listener.onCatchFire(igniteEvent);
        verify(igniteEvent).setCancelled(true);
    }

    @Test
    @DisplayName("acknowledgeBlockChange helper handles null or negative sequence gracefully")
    void testAcknowledgeBlockChangeSafeFallback() {
        Player player = mock(Player.class);
        NoteBlockEventListener.acknowledgeBlockChange(null, 1);
        NoteBlockEventListener.acknowledgeBlockChange(player, -1);
        NoteBlockEventListener.acknowledgeBlockChange(player, 100);
    }

    @Test
    @DisplayName("finishAdjacentBlockChange sends block change, resends vertical native neighbors, and clears pending sequence")
    void testFinishAdjacentBlockChangeHorizontal() {
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(playerId);

        Block changed = mock(Block.class);
        Location loc = new Location(null, 10, 64, 10);
        when(changed.getLocation()).thenReturn(loc);
        when(changed.getBlockData()).thenReturn(mock(BlockData.class));

        Block air = mock(Block.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(changed.getRelative(BlockFace.UP)).thenReturn(air);
        when(changed.getRelative(BlockFace.DOWN)).thenReturn(air);

        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);

        AdjacentNoteBlockUpdateHelper.recordPendingPlacement(playerId, 42);
        AdjacentNoteBlockUpdateHelper.finishAdjacentBlockChange(player, changed, registry);

        verify(player).sendBlockChange(eq(loc), any(BlockData.class));
        assertNull(AdjacentNoteBlockUpdateHelper.pollPendingPlacement(playerId));
    }

    @Test
    @DisplayName("checkPaperNoteBlockUpdatesConfig logs warning when disable-noteblock-updates is false or missing")
    void testPaperNoteBlockUpdatesWarning(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.util.logging.Logger logger = mock(java.util.logging.Logger.class);
        java.io.File missingFile = tempDir.resolve("missing.yml").toFile();

        vn.haohan.itemcore.HaoHanItemCorePlugin.checkPaperNoteBlockUpdatesConfig(missingFile, logger);
        verify(logger, atLeastOnce()).warning(contains("disable-noteblock-updates"));

        reset(logger);
        java.io.File enabledFile = tempDir.resolve("paper-global.yml").toFile();
        java.nio.file.Files.writeString(enabledFile.toPath(), "block-updates:\n  disable-noteblock-updates: true\n");

        vn.haohan.itemcore.HaoHanItemCorePlugin.checkPaperNoteBlockUpdatesConfig(enabledFile, logger);
        verify(logger, never()).warning(anyString());
    }

    @Test
    @DisplayName("onVerticalNeighborPlace acknowledges sequence and resends both placed block and vertical native NoteBlock")
    void testVerticalNeighborPlaceSyncsPredictionAndNeighbor() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(30).build();
        Block nativeBlockBelow = createMockNativeNoteBlock(30, customBlock, registry);
        Location belowLoc = new Location(null, 10, 63, 10);
        when(nativeBlockBelow.getLocation()).thenReturn(belowLoc);

        Block airAbove = mock(Block.class);
        when(airAbove.getType()).thenReturn(Material.AIR);

        Block placedBlock = mock(Block.class);
        Location placedLoc = new Location(null, 10, 64, 10);
        BlockData placedData = mock(BlockData.class);
        when(placedBlock.getLocation()).thenReturn(placedLoc);
        when(placedBlock.getBlockData()).thenReturn(placedData);
        when(placedBlock.getRelative(BlockFace.DOWN)).thenReturn(nativeBlockBelow);
        when(placedBlock.getRelative(BlockFace.UP)).thenReturn(airAbove);

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(playerId);

        AdjacentNoteBlockUpdateHelper.recordPendingPlacement(playerId, 77);

        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(placedBlock);
        when(event.getPlayer()).thenReturn(player);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);
        listener.onVerticalNeighborPlace(event);

        assertNull(AdjacentNoteBlockUpdateHelper.pollPendingPlacement(playerId),
                "Pending sequence must be acknowledged and consumed on vertical neighbor place");
        verify(player).sendBlockChange(placedLoc, placedData);
        verify(player).sendBlockChange(belowLoc, nativeBlockBelow.getBlockData());
    }

    @Test
    @DisplayName("onVerticalNeighborBreak acknowledges digging sequence, sends AIR for broken block, and resends vertical native NoteBlock")
    void testVerticalNeighborBreakSyncsAirAndNeighbor() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(35).build();
        Block nativeBlockAbove = createMockNativeNoteBlock(35, customBlock, registry);
        Location aboveLoc = new Location(null, 10, 65, 10);
        when(nativeBlockAbove.getLocation()).thenReturn(aboveLoc);

        Block airBelow = mock(Block.class);
        when(airBelow.getType()).thenReturn(Material.AIR);

        Block brokenBlock = mock(Block.class);
        Location brokenLoc = new Location(null, 10, 64, 10);
        when(brokenBlock.getLocation()).thenReturn(brokenLoc);
        when(brokenBlock.getRelative(BlockFace.UP)).thenReturn(nativeBlockAbove);
        when(brokenBlock.getRelative(BlockFace.DOWN)).thenReturn(airBelow);

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(playerId);

        AdjacentNoteBlockUpdateHelper.recordPendingPlacement(playerId, 88);

        org.bukkit.event.block.BlockBreakEvent event = mock(org.bukkit.event.block.BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(brokenBlock);
        when(event.getPlayer()).thenReturn(player);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);
        listener.onVerticalNeighborBreak(event);

        assertNull(AdjacentNoteBlockUpdateHelper.pollPendingPlacement(playerId),
                "Pending digging sequence must be acknowledged and consumed on vertical neighbor break");
        verify(player).sendBlockChange( aboveLoc, nativeBlockAbove.getBlockData());
    }
}

