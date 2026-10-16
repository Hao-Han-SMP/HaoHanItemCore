package vn.haohan.itemcore.block;

import org.bukkit.GameEvent;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.api.block.NativeCustomBlock;
import vn.haohan.itemcore.internal.block.AdjacentNoteBlockUpdateHelper;
import vn.haohan.itemcore.internal.block.NoteBlockEventListener;
import vn.haohan.itemcore.internal.block.NoteBlockVariationTable;

import java.util.List;
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

        boolean shouldDeny = listener.shouldDenyNoteTune(block);
        assertTrue(shouldDeny);

        Player player = mock(Player.class);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP);
        listener.onPlayerTuneNoteBlock(event);
        assertEquals(Event.Result.DENY, event.useInteractedBlock());
    }

    @Test
    @DisplayName("Right-clicking a vanilla note block or other block must not be denied")
    void testVanillaNoteBlockAllowed() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        Block vanillaBlock = mock(Block.class);
        when(vanillaBlock.getType()).thenReturn(Material.NOTE_BLOCK);
        NoteBlock vanillaNoteData = mock(NoteBlock.class);
        // Piano, note 0, unpowered -> reserved vanilla (variation -1)
        when(vanillaNoteData.getInstrument()).thenReturn(Instrument.PIANO);
        when(vanillaNoteData.getNote()).thenReturn(new Note(0));
        when(vanillaNoteData.isPowered()).thenReturn(false);
        when(vanillaBlock.getBlockData()).thenReturn(vanillaNoteData);

        assertFalse(listener.isNativeBlock(vanillaBlock));
        assertFalse(listener.shouldDenyNoteTune(vanillaBlock));

        Player player = mock(Player.class);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, vanillaBlock, BlockFace.UP);
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
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP);
        listener.onPlayerTuneNoteBlock(event);

        assertTrue(handlerInvoked.get());
        assertEquals(Event.Result.DENY, event.useInteractedBlock());
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
    @DisplayName("GenericGameEvent NOTE_BLOCK_PLAY on native custom block is cancelled")
    void testGenericGameEventCancelled() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(25).build();
        Block block = createMockNativeNoteBlock(25, customBlock, registry);
        Location loc = mock(Location.class);
        when(loc.clone()).thenReturn(loc);
        when(loc.getBlock()).thenReturn(block);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        GenericGameEvent playEvent = new GenericGameEvent(GameEvent.NOTE_BLOCK_PLAY, loc, null, 16, true);
        listener.onGenericGameEvent(playEvent);
        assertTrue(playEvent.isCancelled());

        GenericGameEvent otherEvent = new GenericGameEvent(GameEvent.STEP, loc, null, 16, true);
        listener.onGenericGameEvent(otherEvent);
        assertFalse(otherEvent.isCancelled());
    }

    @Test
    @DisplayName("BlockPistonExtendEvent affecting native custom block is cancelled")
    void testPistonExtendCancelled() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(30).build();
        Block nativeBlock = createMockNativeNoteBlock(30, customBlock, registry);
        Block normalBlock = mock(Block.class);
        when(normalBlock.getType()).thenReturn(Material.STONE);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        Block piston = mock(Block.class);
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(piston, List.of(normalBlock, nativeBlock), BlockFace.NORTH);
        listener.onPistonExtend(event);
        assertTrue(event.isCancelled());

        BlockPistonExtendEvent normalExtend = new BlockPistonExtendEvent(piston, List.of(normalBlock), BlockFace.NORTH);
        listener.onPistonExtend(normalExtend);
        assertFalse(normalExtend.isCancelled());
    }

    @Test
    @DisplayName("BlockPistonRetractEvent affecting native custom block is cancelled")
    void testPistonRetractCancelled() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(35).build();
        Block nativeBlock = createMockNativeNoteBlock(35, customBlock, registry);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        Block piston = mock(Block.class);
        BlockPistonRetractEvent event = new BlockPistonRetractEvent(piston, List.of(nativeBlock), BlockFace.SOUTH);
        listener.onPistonRetract(event);
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("BlockPhysicsEvent for native custom block or vertical neighbors is cancelled")
    void testBlockPhysicsCancelledForVerticalNeighbors() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(40).build();
        Block nativeBlock = createMockNativeNoteBlock(40, customBlock, registry);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        // Case 1: Physics on native block itself
        Block normalAbove = mock(Block.class);
        when(normalAbove.getType()).thenReturn(Material.AIR);
        Block normalBelow = mock(Block.class);
        when(normalBelow.getType()).thenReturn(Material.DIRT);

        when(nativeBlock.getRelative(BlockFace.UP)).thenReturn(normalAbove);
        when(nativeBlock.getRelative(BlockFace.DOWN)).thenReturn(normalBelow);

        org.bukkit.block.data.BlockData mockBlockData = mock(org.bukkit.block.data.BlockData.class);

        BlockPhysicsEvent eventOnNative = new BlockPhysicsEvent(nativeBlock, mockBlockData);
        listener.onBlockPhysics(eventOnNative);
        assertTrue(eventOnNative.isCancelled());

        // Case 2: Physics on block directly above native block
        Block blockAbove = mock(Block.class);
        when(blockAbove.getType()).thenReturn(Material.STONE);
        when(blockAbove.getRelative(BlockFace.UP)).thenReturn(normalAbove);
        when(blockAbove.getRelative(BlockFace.DOWN)).thenReturn(nativeBlock);

        BlockPhysicsEvent eventAbove = new BlockPhysicsEvent(blockAbove, mockBlockData);
        listener.onBlockPhysics(eventAbove);
        assertTrue(eventAbove.isCancelled());

        // Case 3: Physics on block directly below native block
        Block blockBelow = mock(Block.class);
        when(blockBelow.getType()).thenReturn(Material.STONE);
        when(blockBelow.getRelative(BlockFace.UP)).thenReturn(nativeBlock);
        when(blockBelow.getRelative(BlockFace.DOWN)).thenReturn(normalBelow);

        BlockPhysicsEvent eventBelow = new BlockPhysicsEvent(blockBelow, mockBlockData);
        listener.onBlockPhysics(eventBelow);
        assertTrue(eventBelow.isCancelled());

        // Case 4: Physics on unrelated block
        Block isolatedBlock = mock(Block.class);
        when(isolatedBlock.getType()).thenReturn(Material.COBBLESTONE);
        when(isolatedBlock.getRelative(BlockFace.UP)).thenReturn(normalAbove);
        when(isolatedBlock.getRelative(BlockFace.DOWN)).thenReturn(normalBelow);

        BlockPhysicsEvent normalEvent = new BlockPhysicsEvent(isolatedBlock, mockBlockData);
        listener.onBlockPhysics(normalEvent);
        assertFalse(normalEvent.isCancelled());
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

        org.bukkit.event.block.BlockPlaceEvent event = mock(org.bukkit.event.block.BlockPlaceEvent.class);
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

        org.bukkit.event.block.BlockIgniteEvent igniteEvent = mock(org.bukkit.event.block.BlockIgniteEvent.class);
        when(igniteEvent.getBlock()).thenReturn(nativeBlock);

        listener.onCatchFire(igniteEvent);
        verify(igniteEvent).setCancelled(true);
    }

    @Test
    @DisplayName("Explosion removes native custom blocks from block list")
    void testExplosionRemovesNativeBlocks() {
        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);
        NativeCustomBlock customBlock = NativeCustomBlock.builder("haohan:ore").customVariation(60).build();
        Block nativeBlock = createMockNativeNoteBlock(60, customBlock, registry);
        when(nativeBlock.getLocation()).thenReturn(mock(Location.class));

        Block normalBlock = mock(Block.class);
        when(normalBlock.getType()).thenReturn(Material.DIRT);

        NoteBlockEventListener listener = new NoteBlockEventListener(registry, null);

        java.util.List<Block> blocks = new java.util.ArrayList<>();
        blocks.add(nativeBlock);
        blocks.add(normalBlock);

        org.bukkit.event.block.BlockExplodeEvent explodeEvent = mock(org.bukkit.event.block.BlockExplodeEvent.class);
        when(explodeEvent.blockList()).thenReturn(blocks);

        listener.onBlockExplosion(explodeEvent);

        assertEquals(1, blocks.size());
        assertSame(normalBlock, blocks.get(0));
    }

    @Test
    @DisplayName("acknowledgeBlockChange helper handles null or negative sequence gracefully")
    void testAcknowledgeBlockChangeSafeFallback() {
        Player player = mock(Player.class);
        // Calling helper should not throw exceptions even when PacketEvents uninitialized in test
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
        when(changed.getBlockData()).thenReturn(mock(org.bukkit.block.data.BlockData.class));

        // Horizontal placement: UP and DOWN are AIR
        Block air = mock(Block.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(changed.getRelative(BlockFace.UP)).thenReturn(air);
        when(changed.getRelative(BlockFace.DOWN)).thenReturn(air);

        CustomBlockRegistry registry = mock(CustomBlockRegistry.class);

        AdjacentNoteBlockUpdateHelper.recordPendingPlacement(playerId, 42);
        AdjacentNoteBlockUpdateHelper.finishAdjacentBlockChange(player, changed, registry);

        // Verify block change was sent to player
        verify(player).sendBlockChange(eq(loc), any(org.bukkit.block.data.BlockData.class));
        assertNull(AdjacentNoteBlockUpdateHelper.pollPendingPlacement(playerId));
    }
}
