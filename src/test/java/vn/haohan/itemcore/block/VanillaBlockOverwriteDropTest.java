package vn.haohan.itemcore.block;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.internal.block.CustomBlockEventListener;
import vn.haohan.itemcore.internal.block.CustomBlockStorage;
import vn.haohan.itemcore.internal.block.CustomBlockVisibilityManager;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockRegistry;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VanillaBlockOverwriteDropTest {

    private CustomBlockRegistry registry;
    private CustomBlockStorage storage;
    private CustomBlockVisibilityManager visibilityManager;
    private DefaultCustomBlockService service;
    private Plugin plugin;

    @BeforeEach
    public void setup() {
        registry = new DefaultCustomBlockRegistry();
        storage = Mockito.mock(CustomBlockStorage.class);
        visibilityManager = Mockito.mock(CustomBlockVisibilityManager.class);
        plugin = Mockito.mock(Plugin.class);
        Mockito.when(plugin.getName()).thenReturn("test");
        service = new DefaultCustomBlockService(registry, storage, visibilityManager, plugin);
    }

    @Test
    public void testVanillaOverwriteWithoutCustomDropLeavesEventDropItemsTrue() {
        // Vanilla overwrite block with NO custom drop set
        CustomBlockDefinition def = CustomBlockDefinition.builder("minecraft:iron_ore")
                .mineableWith(ToolType.PICKAXE, ToolTier.STONE)
                .build();
        service.register(def);

        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        ItemStack ironPick = Mockito.mock(ItemStack.class);
        Mockito.when(ironPick.getType()).thenReturn(Material.IRON_PICKAXE);
        Mockito.when(inventory.getItemInMainHand()).thenReturn(ironPick);
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        World world = Mockito.mock(World.class);
        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getType()).thenReturn(Material.IRON_ORE);
        Location loc = Mockito.mock(Location.class);
        Mockito.when(loc.getWorld()).thenReturn(world);
        Mockito.when(loc.getBlock()).thenReturn(block);
        Mockito.when(block.getLocation()).thenReturn(loc);
        Mockito.when(world.getBlockAt(0, 0, 0)).thenReturn(block);

        CustomBlockEventListener listener = new CustomBlockEventListener(
                service, storage, visibilityManager, null, plugin
        );

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        assertTrue(event.isDropItems());

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
        // For vanilla overwrite with no custom drop, vanilla drop items must NOT be cancelled
        assertTrue(event.isDropItems(), "Vanilla drops must remain enabled (isDropItems true) when no custom drop is configured");
    }

    @Test
    public void testVanillaOverwriteWithCustomDropCancelsEventDropItems() {
        // Vanilla overwrite block WITH an explicit custom drop
        CustomBlockDefinition def = CustomBlockDefinition.builder("minecraft:iron_ore")
                .dropItemId("custom:special_raw_iron")
                .mineableWith(ToolType.PICKAXE, ToolTier.STONE)
                .build();
        service.register(def);

        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        ItemStack ironPick = Mockito.mock(ItemStack.class);
        Mockito.when(ironPick.getType()).thenReturn(Material.IRON_PICKAXE);
        Mockito.when(inventory.getItemInMainHand()).thenReturn(ironPick);
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        World world = Mockito.mock(World.class);
        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getType()).thenReturn(Material.IRON_ORE);
        Location loc = Mockito.mock(Location.class);
        Mockito.when(loc.getWorld()).thenReturn(world);
        Mockito.when(loc.getBlock()).thenReturn(block);
        Mockito.when(block.getLocation()).thenReturn(loc);
        Mockito.when(world.getBlockAt(0, 0, 0)).thenReturn(block);

        CustomBlockEventListener listener = new CustomBlockEventListener(
                service, storage, visibilityManager, null, plugin
        );

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        assertTrue(event.isDropItems());

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
        // When explicit custom drop is configured, default vanilla drops should be cancelled
        assertFalse(event.isDropItems(), "Vanilla drops must be cancelled (isDropItems false) when explicit custom drop is defined");
    }
}
