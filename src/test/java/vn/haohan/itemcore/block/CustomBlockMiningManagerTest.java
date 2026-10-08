package vn.haohan.itemcore.block;

import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockService;
import vn.haohan.itemcore.internal.block.CustomBlockMiningManager;
import vn.haohan.itemcore.recipe.TestItemService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomBlockMiningManagerTest {

    private CustomBlockService blockService;
    private CustomBlockMiningManager miningManager;
    private Player mockPlayer;
    private World mockWorld;
    private Block mockBlock;
    private List<Integer> sentStages;

    @BeforeEach
    public void setUp() {
        blockService = Mockito.mock(CustomBlockService.class);
        miningManager = new CustomBlockMiningManager(blockService, null);
        sentStages = new ArrayList<>();
        miningManager.setAnimationObserver((player, loc, stage) -> sentStages.add(stage));

        mockWorld = Mockito.mock(World.class);
        mockBlock = Mockito.mock(Block.class);
        Mockito.when(mockBlock.getWorld()).thenReturn(mockWorld);
        Mockito.when(mockWorld.getBlockAt(Mockito.any(Location.class))).thenReturn(mockBlock);
        Mockito.when(mockWorld.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(mockBlock);

        ItemStack airItem = TestItemService.mockItem(Material.AIR);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        Mockito.when(inv.getItemInMainHand()).thenReturn(airItem);

        mockPlayer = Mockito.mock(Player.class);
        UUID uuid = UUID.randomUUID();
        Mockito.when(mockPlayer.getUniqueId()).thenReturn(uuid);
        Mockito.when(mockPlayer.getEntityId()).thenReturn(1001);
        Mockito.when(mockPlayer.isOnline()).thenReturn(true);
        Mockito.when(mockPlayer.isOnGround()).thenReturn(true);
        Mockito.when(mockPlayer.getWorld()).thenReturn(mockWorld);
        Mockito.when(mockPlayer.getLocation()).thenReturn(new Location(mockWorld, 0, 64, 0));
        Mockito.when(mockPlayer.getInventory()).thenReturn(inv);
    }

    @Test
    public void testStartDiggingUnbreakableBlockIgnored() {
        Location loc = new Location(mockWorld, 1, 64, 1);
        CustomBlockDefinition bedrock = CustomBlockDefinition.builder("custom:bedrock")
                .unbreakable()
                .build();
        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(bedrock));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);

        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(1, 64, 1), loc);

        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()));
        assertTrue(sentStages.isEmpty());
    }

    @Test
    public void testStartAndCancelDigging() {
        Location loc = new Location(mockWorld, 1, 64, 1);
        CustomBlockDefinition stone = CustomBlockDefinition.builder("custom:stone")
                .hardness(2.0f)
                .build();
        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(stone));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);

        // Start digging
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(1, 64, 1), loc);
        assertTrue(miningManager.hasActiveSession(mockPlayer.getUniqueId()));

        // Cancel digging
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.CANCELLED_DIGGING, new Vector3i(1, 64, 1), loc);
        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()));

        // Should have sent stage -1 reset packet
        assertEquals(1, sentStages.size());
        assertEquals(-1, sentStages.get(0));
    }

    @Test
    public void testMiningProgressAndCompletion() {
        Location loc = new Location(mockWorld, 1, 64, 1);
        // 0.1s hardness = 2 ticks with 1.0 speed hand (0.5 progress per tick)
        CustomBlockDefinition soft = CustomBlockDefinition.builder("custom:soft")
                .hardness(0.1f)
                .build();
        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(soft));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);

        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(1, 64, 1), loc);
        assertTrue(miningManager.hasActiveSession(mockPlayer.getUniqueId()));

        // Tick 1
        miningManager.tick();
        assertTrue(miningManager.hasActiveSession(mockPlayer.getUniqueId()));
        assertFalse(sentStages.isEmpty());

        // Tick 2: reaches >= 1.0f -> breaks
        miningManager.tick();
        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()));
        Mockito.verify(blockService).removeCustomBlock(loc);
    }
}
