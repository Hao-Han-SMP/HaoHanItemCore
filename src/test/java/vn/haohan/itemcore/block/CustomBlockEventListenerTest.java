package vn.haohan.itemcore.block;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.AlternativeCustomBlock;
import vn.haohan.itemcore.api.block.CustomBlockService;
import vn.haohan.itemcore.api.block.NativeCustomBlock;
import vn.haohan.itemcore.internal.block.CustomBlockEventListener;
import vn.haohan.itemcore.internal.block.CustomBlockStorage;
import vn.haohan.itemcore.internal.block.CustomBlockVisibilityManager;

import java.util.Optional;

import static org.mockito.Mockito.*;

class CustomBlockEventListenerTest {

    @Test
    @DisplayName("Breaking a NativeCustomBlock in BlockBreakEvent does not prematurely remove or set type to AIR before Paper finishes")
    void testNativeCustomBlockBreakPreservesPhysicalBlockDuringEvent() {
        CustomBlockService service = mock(CustomBlockService.class);
        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager vm = mock(CustomBlockVisibilityManager.class);
        Plugin plugin = mock(Plugin.class);

        NativeCustomBlock def = NativeCustomBlock.builder("haohan:ore").customVariation(10).build();
        Block block = mock(Block.class);
        Location loc = new Location(null, 1, 2, 3);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.NOTE_BLOCK);
        when(service.isCustomBlock(block)).thenReturn(true);
        when(service.getCustomBlock(block)).thenReturn(Optional.of(def));

        CustomBlockEventListener listener = new CustomBlockEventListener(service, storage, vm, null, plugin);
        ItemStack pickaxe = vn.haohan.itemcore.recipe.TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(pickaxe);
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        verify(service, never()).removeCustomBlock(any());
    }

    @Test
    @DisplayName("Breaking a NativeCustomBlock plays custom breakSound and stops wood break sound")
    void testNativeCustomBlockBreakPlaysCustomBreakSound() {
        CustomBlockService service = mock(CustomBlockService.class);
        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager vm = mock(CustomBlockVisibilityManager.class);
        Plugin plugin = mock(Plugin.class);

        vn.haohan.itemcore.internal.block.CustomBlockSoundPacketListener soundListener =
                mock(vn.haohan.itemcore.internal.block.CustomBlockSoundPacketListener.class);

        vn.haohan.itemcore.api.block.CustomBlockSoundGroup soundGroup =
                vn.haohan.itemcore.api.block.CustomBlockSoundGroup.builder()
                        .breakSound("minecraft:block.stone.break", 1.0f, 1.0f)
                        .build();

        NativeCustomBlock def = NativeCustomBlock.builder("haohan:ore")
                .customVariation(10)
                .soundGroup(soundGroup)
                .build();

        org.bukkit.World world = mock(org.bukkit.World.class);
        Block block = mock(Block.class);
        Location loc = new Location(world, 10, 64, 20);
        when(block.getLocation()).thenReturn(loc);
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.NOTE_BLOCK);
        when(service.isCustomBlock(block)).thenReturn(true);
        when(service.getCustomBlock(block)).thenReturn(Optional.of(def));

        CustomBlockEventListener listener = new CustomBlockEventListener(service, storage, vm, null, plugin, soundListener);
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack pickaxe = vn.haohan.itemcore.recipe.TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        when(inventory.getItemInMainHand()).thenReturn(pickaxe);
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        verify(soundListener).markRecentBreak(loc, "minecraft:block.stone.break");
        verify(player).stopSound(org.bukkit.Sound.BLOCK_WOOD_BREAK, org.bukkit.SoundCategory.BLOCKS);
        verify(world).playSound(any(Location.class), eq("minecraft:block.stone.break"), eq(org.bukkit.SoundCategory.BLOCKS), eq(1.0f), eq(1.0f));
    }

    @Test
    @DisplayName("Breaking an AlternativeCustomBlock in BlockBreakEvent does call removeCustomBlock to clean up storage and display")
    void testAlternativeCustomBlockBreakCallsRemoveCustomBlock() {
        CustomBlockService service = mock(CustomBlockService.class);
        CustomBlockStorage storage = mock(CustomBlockStorage.class);
        CustomBlockVisibilityManager vm = mock(CustomBlockVisibilityManager.class);
        Plugin plugin = mock(Plugin.class);

        AlternativeCustomBlock def = AlternativeCustomBlock.builder("haohan:chair").build();
        Block block = mock(Block.class);
        Location loc = new Location(null, 1, 2, 3);
        when(block.getLocation()).thenReturn(loc);
        when(block.getType()).thenReturn(Material.BARRIER);
        when(service.isCustomBlock(block)).thenReturn(true);
        when(service.getCustomBlock(block)).thenReturn(Optional.of(def));

        CustomBlockEventListener listener = new CustomBlockEventListener(service, storage, vm, null, plugin);
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.STICK));
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        verify(service).removeCustomBlock(loc);
    }
}
