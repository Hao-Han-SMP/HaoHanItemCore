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
        when(block.getType()).thenReturn(Material.NOTE_BLOCK);
        when(service.isCustomBlock(block)).thenReturn(true);
        when(service.getCustomBlock(block)).thenReturn(Optional.of(def));

        CustomBlockEventListener listener = new CustomBlockEventListener(service, storage, vm, null, plugin);
        Player player = mock(Player.class);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.DIAMOND_PICKAXE));
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        verify(service, never()).removeCustomBlock(any());
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
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.STICK));
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        verify(service).removeCustomBlock(loc);
    }
}
