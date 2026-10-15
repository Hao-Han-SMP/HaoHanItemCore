package vn.haohan.itemcore.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.AlternativeCustomBlock;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.NativeCustomBlock;
import vn.haohan.itemcore.internal.block.CustomBlockMiningManager;
import vn.haohan.itemcore.internal.block.CustomBlockStorage;
import vn.haohan.itemcore.internal.block.CustomBlockVisibilityManager;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockRegistry;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockService;
import vn.haohan.itemcore.internal.block.mining.NativeMiningStrategy;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomBlockServiceRoutingTest {

    private Server server;

    private static void setBukkitServer(Server server) {
        try {
            Field field = Bukkit.class.getDeclaredField("server");
            field.setAccessible(true);
            field.set(null, server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        server = mock(Server.class);
        setBukkitServer(server);
    }

    @AfterEach
    void tearDown() {
        setBukkitServer(null);
    }

    @Test
    @DisplayName("Placing NativeCustomBlock sets NoteBlock blockdata directly and avoids barrier storage")
    void testPlaceNativeBlock() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        NativeCustomBlock nativeBlock = NativeCustomBlock.builder("haohan:native_ore")
                .customVariation(15)
                .build();
        registry.register(nativeBlock);

        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager visManager = mock(CustomBlockVisibilityManager.class);

        NoteBlock noteBlockData = mock(NoteBlock.class);
        when(server.createBlockData(Material.NOTE_BLOCK)).thenReturn(noteBlockData);

        Plugin plugin = mock(Plugin.class);
        DefaultCustomBlockService service = new DefaultCustomBlockService(registry, storage, visManager, plugin);

        World world = mock(World.class);
        Block block = mock(Block.class);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlock()).thenReturn(block);

        boolean placed = service.placeCustomBlock(loc, "haohan:native_ore");
        assertTrue(placed);

        // Should NOT invoke barrier storage or BlockDisplay visibility manager for native blocks!
        verify(storage, never()).set(any(World.class), anyInt(), anyInt(), anyInt(), anyString());
        verify(visManager, never()).handleBlockPlaced(any(), any());
        verify(block).setBlockData(noteBlockData, false);
    }

    @Test
    @DisplayName("Placing AlternativeCustomBlock uses Barrier and invokes barrier storage & visibility manager")
    void testPlaceAlternativeBlock() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        AlternativeCustomBlock altBlock = AlternativeCustomBlock.builder("haohan:alt_furnace")
                .build();
        registry.register(altBlock);

        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager visManager = mock(CustomBlockVisibilityManager.class);
        Plugin plugin = mock(Plugin.class);

        DefaultCustomBlockService service = new DefaultCustomBlockService(registry, storage, visManager, plugin);

        World world = mock(World.class);
        Block block = mock(Block.class);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlock()).thenReturn(block);
        when(loc.getBlockX()).thenReturn(10);
        when(loc.getBlockY()).thenReturn(64);
        when(loc.getBlockZ()).thenReturn(20);

        boolean placed = service.placeCustomBlock(loc, "haohan:alt_furnace");
        assertTrue(placed);

        verify(block).setType(Material.BARRIER, false);
        verify(storage).set(world, 10, 64, 20, "haohan:alt_furnace");
        verify(visManager).handleBlockPlaced(loc, altBlock);
    }

    @Test
    @DisplayName("isCustomBlock and getCustomBlock recognize NativeCustomBlock via registry.getByBlockData")
    void testRecognizeNativeBlock() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        NativeCustomBlock nativeBlock = NativeCustomBlock.builder("haohan:ruby_block")
                .customVariation(20)
                .build();
        registry.register(nativeBlock);

        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager visManager = mock(CustomBlockVisibilityManager.class);
        DefaultCustomBlockService service = new DefaultCustomBlockService(registry, storage, visManager, mock(Plugin.class));

        Block block = mock(Block.class);
        NoteBlock noteBlockData = mock(NoteBlock.class);
        when(noteBlockData.getInstrument()).thenReturn(org.bukkit.Instrument.BASS_DRUM);
        when(noteBlockData.getNote()).thenReturn(new org.bukkit.Note(20));
        when(noteBlockData.isPowered()).thenReturn(false);

        when(block.getType()).thenReturn(Material.NOTE_BLOCK);
        when(block.getBlockData()).thenReturn(noteBlockData);

        assertTrue(service.isCustomBlock(block));
        Optional<CustomBlockDefinition> found = service.getCustomBlock(block);
        assertTrue(found.isPresent());
        assertEquals("haohan:ruby_block", found.get().getId());
        assertEquals("haohan:ruby_block", service.getCustomBlockId(block));
    }

    @Test
    @DisplayName("removeCustomBlock on registered NOTE_BLOCK sets AIR and returns true, while unregistered returns false")
    void testRemoveNativeBlock() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        NativeCustomBlock nativeBlock = NativeCustomBlock.builder("haohan:native_ore")
                .customVariation(10)
                .build();
        registry.register(nativeBlock);

        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager visManager = mock(CustomBlockVisibilityManager.class);
        DefaultCustomBlockService service = new DefaultCustomBlockService(registry, storage, visManager, mock(Plugin.class));

        World world = mock(World.class);
        Block block = mock(Block.class);
        NoteBlock noteBlockData = mock(NoteBlock.class);
        when(noteBlockData.getInstrument()).thenReturn(org.bukkit.Instrument.BASS_DRUM);
        when(noteBlockData.getNote()).thenReturn(new org.bukkit.Note(10));
        when(noteBlockData.isPowered()).thenReturn(false);

        when(block.getType()).thenReturn(Material.NOTE_BLOCK);
        when(block.getBlockData()).thenReturn(noteBlockData);

        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlock()).thenReturn(block);

        boolean removed = service.removeCustomBlock(loc);
        assertTrue(removed);

        verify(block).setType(Material.AIR, false);
        verify(storage, never()).remove(any(World.class), anyInt(), anyInt(), anyInt());
        verify(visManager, never()).handleBlockBroken(any());

        // Unregistered NoteBlock:
        Block unregBlock = mock(Block.class);
        NoteBlock unregData = mock(NoteBlock.class);
        when(unregData.getInstrument()).thenReturn(org.bukkit.Instrument.PIANO);
        when(unregData.getNote()).thenReturn(new org.bukkit.Note(0));
        when(unregData.isPowered()).thenReturn(false);
        when(unregBlock.getType()).thenReturn(Material.NOTE_BLOCK);
        when(unregBlock.getBlockData()).thenReturn(unregData);

        Location unregLoc = mock(Location.class);
        when(unregLoc.getWorld()).thenReturn(world);
        when(unregLoc.getBlock()).thenReturn(unregBlock);

        boolean unregRemoved = service.removeCustomBlock(unregLoc);
        assertFalse(unregRemoved);
        verify(unregBlock, never()).setType(Material.AIR, false);
    }

    @Test
    @DisplayName("MiningManager routes native blocks to NativeMiningStrategy and alternative to progressive mining")
    void testMiningManagerRouting() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        NativeCustomBlock nativeBlock = NativeCustomBlock.builder("haohan:native_block")
                .customVariation(5)
                .hardness(2.0f)
                .build();
        registry.register(nativeBlock);

        AlternativeCustomBlock altBlock = AlternativeCustomBlock.builder("haohan:alt_block")
                .hardness(2.0f)
                .build();
        registry.register(altBlock);

        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager visManager = mock(CustomBlockVisibilityManager.class);
        Plugin plugin = mock(Plugin.class);
        DefaultCustomBlockService service = new DefaultCustomBlockService(registry, storage, visManager, plugin);

        NativeMiningStrategy nativeStrategy = mock(NativeMiningStrategy.class);
        CustomBlockMiningManager miningManager = new CustomBlockMiningManager(
                service, null, null, nativeStrategy, plugin
        );

        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
        when(server.getPlayer(uuid)).thenReturn(player);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack tool = mock(ItemStack.class);
        when(inv.getItemInMainHand()).thenReturn(tool);
        when(player.getInventory()).thenReturn(inv);

        World world = mock(World.class);
        Block nativeMcBlock = mock(Block.class);
        NoteBlock noteBlockData = mock(NoteBlock.class);
        when(noteBlockData.getInstrument()).thenReturn(org.bukkit.Instrument.BASS_DRUM);
        when(noteBlockData.getNote()).thenReturn(new org.bukkit.Note(5));
        when(noteBlockData.isPowered()).thenReturn(false);
        when(nativeMcBlock.getType()).thenReturn(Material.NOTE_BLOCK);
        when(nativeMcBlock.getBlockData()).thenReturn(noteBlockData);

        Location nativeLoc = mock(Location.class);
        when(nativeLoc.getWorld()).thenReturn(world);
        when(nativeLoc.getBlock()).thenReturn(nativeMcBlock);

        // Native START_DIGGING
        miningManager.handleDiggingAction(player, DiggingAction.START_DIGGING, new Vector3i(0, 64, 0), nativeLoc);
        verify(nativeStrategy).onStartDigging(player, nativeMcBlock, nativeBlock, tool);
        assertFalse(miningManager.hasActiveSession(uuid));

        // Native CANCELLED_DIGGING
        miningManager.handleDiggingAction(player, DiggingAction.CANCELLED_DIGGING, new Vector3i(0, 64, 0), nativeLoc);
        verify(nativeStrategy).onStopDigging(player, nativeMcBlock, nativeBlock);

        // Cancel session explicitly removes modifier
        miningManager.cancelSession(uuid);
        verify(nativeStrategy).removeModifier(player);
    }

    @Test
    @DisplayName("MiningManager routes alternative blocks to progressive mining session")
    void testMiningManagerAlternativeRouting() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        AlternativeCustomBlock altBlock = AlternativeCustomBlock.builder("haohan:alt_stone")
                .hardness(2.0f)
                .build();
        registry.register(altBlock);

        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager visManager = mock(CustomBlockVisibilityManager.class);
        Plugin plugin = mock(Plugin.class);
        DefaultCustomBlockService service = new DefaultCustomBlockService(registry, storage, visManager, plugin);

        NativeMiningStrategy nativeStrategy = mock(NativeMiningStrategy.class);
        CustomBlockMiningManager miningManager = new CustomBlockMiningManager(
                service, null, null, nativeStrategy, plugin
        );

        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
        when(server.getPlayer(uuid)).thenReturn(player);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack tool = mock(ItemStack.class);
        when(inv.getItemInMainHand()).thenReturn(tool);
        when(player.getInventory()).thenReturn(inv);

        World world = mock(World.class);
        Block altMcBlock = mock(Block.class);
        when(altMcBlock.getType()).thenReturn(Material.BARRIER);
        when(altMcBlock.getWorld()).thenReturn(world);
        when(altMcBlock.getX()).thenReturn(5);
        when(altMcBlock.getY()).thenReturn(64);
        when(altMcBlock.getZ()).thenReturn(5);
        when(storage.get(world, 5, 64, 5)).thenReturn("haohan:alt_stone");

        Location altLoc = mock(Location.class);
        when(altLoc.getWorld()).thenReturn(world);
        when(altLoc.getBlock()).thenReturn(altMcBlock);

        // Alternative START_DIGGING -> starts progressive mining session
        miningManager.handleDiggingAction(player, DiggingAction.START_DIGGING, new Vector3i(5, 64, 5), altLoc);
        assertTrue(miningManager.hasActiveSession(uuid));
        verify(nativeStrategy, never()).onStartDigging(any(), any(), any(), any());

        // Alternative CANCELLED_DIGGING -> cancels progressive mining session
        miningManager.handleDiggingAction(player, DiggingAction.CANCELLED_DIGGING, new Vector3i(5, 64, 5), altLoc);
        assertFalse(miningManager.hasActiveSession(uuid));
    }

    @Test
    @DisplayName("clearAllSessions removes native attribute modifiers from all online players")
    void testClearAllSessionsCleansOnlinePlayers() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager visManager = mock(CustomBlockVisibilityManager.class);
        Plugin plugin = mock(Plugin.class);
        DefaultCustomBlockService service = new DefaultCustomBlockService(registry, storage, visManager, plugin);

        NativeMiningStrategy nativeStrategy = mock(NativeMiningStrategy.class);
        CustomBlockMiningManager miningManager = new CustomBlockMiningManager(
                service, null, null, nativeStrategy, plugin
        );

        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        doReturn(java.util.List.of(p1, p2)).when(server).getOnlinePlayers();

        miningManager.clearAllSessions();

        verify(nativeStrategy).removeModifier(p1);
        verify(nativeStrategy).removeModifier(p2);
    }

    @Test
    @DisplayName("onBlockBreakCleanup on MONITOR priority cancels mining session on cancelled break")
    void testOnBlockBreakCleanupCancelsSession() {
        DefaultCustomBlockRegistry registry = new DefaultCustomBlockRegistry();
        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager visManager = mock(CustomBlockVisibilityManager.class);
        Plugin plugin = mock(Plugin.class);
        DefaultCustomBlockService service = new DefaultCustomBlockService(registry, storage, visManager, plugin);

        CustomBlockMiningManager miningManager = mock(CustomBlockMiningManager.class);
        service.setMiningManager(miningManager);

        vn.haohan.itemcore.internal.block.CustomBlockEventListener listener =
                new vn.haohan.itemcore.internal.block.CustomBlockEventListener(service, storage, visManager, null, plugin);

        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        Block block = mock(Block.class);

        org.bukkit.event.block.BlockBreakEvent breakEvent = new org.bukkit.event.block.BlockBreakEvent(block, player);
        breakEvent.setCancelled(true);

        listener.onBlockBreakCleanup(breakEvent);

        verify(miningManager).cancelSession(uuid);
    }
}
