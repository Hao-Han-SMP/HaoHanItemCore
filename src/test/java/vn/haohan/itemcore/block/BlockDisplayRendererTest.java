package vn.haohan.itemcore.block;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.block.CustomBlockModel;
import vn.haohan.itemcore.internal.block.BlockDisplayRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlockDisplayRenderer Tests")
class BlockDisplayRendererTest {

    private BlockDisplayRenderer renderer;
    private Player mockPlayer;
    private World mockWorld;
    private List<PacketWrapper<?>> capturedPackets;

    @BeforeEach
    void setUp() {
        renderer = new BlockDisplayRenderer();
        capturedPackets = new ArrayList<>();
        renderer.setPacketInterceptor((p, packet) -> capturedPackets.add(packet));

        mockWorld = Mockito.mock(World.class);
        mockPlayer = Mockito.mock(Player.class);
        Mockito.when(mockPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(mockPlayer.isOnline()).thenReturn(true);
        Mockito.when(mockPlayer.getWorld()).thenReturn(mockWorld);
    }

    @Test
    @DisplayName("CustomBlockModel detects item display vs block display correctly")
    void testModelTypeDetection() {
        ItemStack item = Mockito.mock(ItemStack.class);
        Mockito.when(item.getType()).thenReturn(Material.BARRIER);
        Mockito.when(item.clone()).thenReturn(item);

        CustomBlockModel itemModel = CustomBlockModel.of(item);
        assertTrue(itemModel.isItemDisplay());
        assertFalse(itemModel.isBlockDisplay());
        assertNotNull(itemModel.getItemStack());
        assertNull(itemModel.getBlockData());

        BlockData blockData = Mockito.mock(BlockData.class);
        Mockito.when(blockData.getAsString()).thenReturn("minecraft:stone");

        CustomBlockModel blockModel = CustomBlockModel.of(blockData);
        assertFalse(blockModel.isItemDisplay());
        assertTrue(blockModel.isBlockDisplay());
        assertNotNull(blockModel.getBlockData());
        assertNull(blockModel.getItemStack());
    }

    @Test
    @DisplayName("Item model spawns ITEM_DISPLAY entity centered at block coordinates")
    void testSpawnItemDisplay() {
        org.junit.jupiter.api.Assumptions.assumeTrue(com.github.retrooper.packetevents.PacketEvents.getAPI() != null,
                "PacketEvents API must be initialized to instantiate EntityTypes");
        Location loc = new Location(mockWorld, 10, 64, 20);
        ItemStack item = Mockito.mock(ItemStack.class);
        Mockito.when(item.getType()).thenReturn(Material.BARRIER);
        Mockito.when(item.clone()).thenReturn(item);
        CustomBlockModel model = CustomBlockModel.of(item);

        renderer.spawnDisplay(mockPlayer, 42, loc, model);

        assertEquals(2, capturedPackets.size(), "Must dispatch 1 spawn packet and 1 metadata packet");
        assertTrue(capturedPackets.get(0) instanceof WrapperPlayServerSpawnEntity);

        WrapperPlayServerSpawnEntity spawn = (WrapperPlayServerSpawnEntity) capturedPackets.get(0);
        assertEquals(EntityTypes.ITEM_DISPLAY, spawn.getEntityType(), "Must spawn ITEM_DISPLAY for item model");
        assertEquals(10.5, spawn.getPosition().getX(), 0.001, "X must be centered");
        assertEquals(64.5, spawn.getPosition().getY(), 0.001, "Y must be centered");
        assertEquals(20.5, spawn.getPosition().getZ(), 0.001, "Z must be centered");

        List<EntityData<?>> metadata = renderer.createDisplayMetadata(model);
        assertTrue(metadata.stream().anyMatch(d -> d.getIndex() == BlockDisplayRenderer.METADATA_ITEM_INDEX));
        assertTrue(metadata.stream().anyMatch(d -> d.getIndex() == BlockDisplayRenderer.METADATA_ITEM_DISPLAY_TYPE_INDEX
                && Byte.valueOf((byte) 8).equals(d.getValue())));
    }

    @Test
    @DisplayName("Block model spawns BLOCK_DISPLAY entity at origin coordinates")
    void testSpawnBlockDisplay() {
        org.junit.jupiter.api.Assumptions.assumeTrue(com.github.retrooper.packetevents.PacketEvents.getAPI() != null,
                "PacketEvents API must be initialized to instantiate EntityTypes");
        Location loc = new Location(mockWorld, 10, 64, 20);
        BlockData blockData = Mockito.mock(BlockData.class);
        Mockito.when(blockData.getAsString()).thenReturn("minecraft:stone");
        CustomBlockModel model = CustomBlockModel.of(blockData);

        renderer.spawnDisplay(mockPlayer, 43, loc, model);

        assertEquals(2, capturedPackets.size());
        assertTrue(capturedPackets.get(0) instanceof WrapperPlayServerSpawnEntity);

        WrapperPlayServerSpawnEntity spawn = (WrapperPlayServerSpawnEntity) capturedPackets.get(0);
        assertEquals(EntityTypes.BLOCK_DISPLAY, spawn.getEntityType(), "Must spawn BLOCK_DISPLAY for block data model");
        assertEquals(10.0, spawn.getPosition().getX(), 0.001);
        assertEquals(64.0, spawn.getPosition().getY(), 0.001);
        assertEquals(20.0, spawn.getPosition().getZ(), 0.001);
    }
}
