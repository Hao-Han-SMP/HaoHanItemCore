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
import vn.haohan.itemcore.internal.block.VirtualEntityIdManager;
import vn.haohan.itemcore.recipe.TestItemService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomBlockMiningManagerTest {

    private CustomBlockService blockService;
    private VirtualEntityIdManager idManager;
    private BlockBreakOverlayRendererTest.TestBlockBreakOverlayRenderer overlayRenderer;
    private CustomBlockMiningManager miningManager;
    private Player mockPlayer;
    private World mockWorld;
    private Block mockBlock;
    private List<Integer> sentStages;

    @BeforeEach
    public void setUp() {
        blockService = Mockito.mock(CustomBlockService.class);
        idManager = new VirtualEntityIdManager();
        overlayRenderer = new BlockBreakOverlayRendererTest.TestBlockBreakOverlayRenderer();
        miningManager = new CustomBlockMiningManager(blockService, idManager, overlayRenderer, null);
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
        assertEquals(1, overlayRenderer.spawnCount.get(), "Overlay crack entity should be spawned upon start digging");

        // Cancel digging
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.CANCELLED_DIGGING, new Vector3i(1, 64, 1), loc);
        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()));
        assertEquals(1, overlayRenderer.destroyCount.get(), "Overlay crack entity should be destroyed upon cancel digging");

        // Should have sent stage -1 reset packet
        assertEquals(1, sentStages.size());
        assertEquals(-1, sentStages.get(0));
    }

    @Test
    public void testNativeBlockMiningTicksPlayHitSoundAndStopWoodHit() {
        Location loc = new Location(mockWorld, 1, 64, 1);
        ItemStack pickaxe = TestItemService.mockItem(Material.DIAMOND_PICKAXE);
        Mockito.when(mockPlayer.getInventory().getItemInMainHand()).thenReturn(pickaxe);

        vn.haohan.itemcore.api.block.CustomBlockSoundGroup soundGroup =
                vn.haohan.itemcore.api.block.CustomBlockSoundGroup.builder()
                        .hitSound("minecraft:block.iron.hit", 1.0f, 1.0f)
                        .build();

        vn.haohan.itemcore.api.block.NativeCustomBlock nativeBlock =
                vn.haohan.itemcore.api.block.NativeCustomBlock.builder("custom:native_ore")
                        .customVariation(5)
                        .soundGroup(soundGroup)
                        .build();

        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(nativeBlock));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);

        // Start digging native block
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(1, 64, 1), loc);

        // Tick 1, 2, 3
        miningManager.tick();
        miningManager.tick();
        miningManager.tick();

        // Tick 4: should trigger hit sound and stop wood hit
        miningManager.tick();

        Mockito.verify(mockPlayer, Mockito.atLeastOnce()).stopSound(org.bukkit.Sound.BLOCK_WOOD_HIT, org.bukkit.SoundCategory.BLOCKS);
        Mockito.verify(mockWorld, Mockito.atLeastOnce()).playSound(Mockito.any(Location.class), Mockito.eq("minecraft:block.iron.hit"), Mockito.eq(org.bukkit.SoundCategory.BLOCKS), Mockito.eq(1.0f), Mockito.eq(1.0f));

        // Stop digging
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.CANCELLED_DIGGING, new Vector3i(1, 64, 1), loc);
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
        assertEquals(1, overlayRenderer.spawnCount.get());

        // Tick 1
        miningManager.tick();
        assertTrue(miningManager.hasActiveSession(mockPlayer.getUniqueId()));
        assertFalse(sentStages.isEmpty());
        assertTrue(overlayRenderer.updateCount.get() > 0, "Overlay crack stage should be updated during mining tick");

        // Tick 2: reaches >= 1.0f -> breaks
        miningManager.tick();
        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()));
        assertEquals(1, overlayRenderer.destroyCount.get(), "Overlay crack entity should be destroyed upon mining completion");
        Mockito.verify(blockService).removeCustomBlock(loc);
    }

    @Test
    public void testStartDiggingDisallowedWithBareHandWhenBlockRequiresPickaxe() {
        Location loc = new Location(mockWorld, 1, 64, 1);
        CustomBlockDefinition ore = CustomBlockDefinition.builder("custom:iron_ore")
                .hardness(5.0f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE, vn.haohan.itemcore.api.block.ToolTier.IRON)
                .build();
        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(ore));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);

        // Player holds AIR (bare hand) -> Should NOT create session
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(1, 64, 1), loc);

        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()), "Session must not start with bare hand on a pickaxe-only block");
        assertEquals(0, overlayRenderer.spawnCount.get(), "Crack overlay must not spawn");
    }

    @Test
    public void testStartDiggingDisallowedWithWrongTool() {
        Location loc = new Location(mockWorld, 1, 64, 1);
        CustomBlockDefinition ore = CustomBlockDefinition.builder("custom:iron_ore")
                .hardness(5.0f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE, vn.haohan.itemcore.api.block.ToolTier.IRON)
                .build();
        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(ore));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);

        // Player holds Diamond Sword -> Should NOT create session
        ItemStack sword = TestItemService.mockItem(Material.DIAMOND_SWORD);
        Mockito.when(mockPlayer.getInventory().getItemInMainHand()).thenReturn(sword);

        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(1, 64, 1), loc);

        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()), "Session must not start with sword on a pickaxe-only block");
        assertEquals(0, overlayRenderer.spawnCount.get(), "Crack overlay must not spawn");
    }

    @Test
    public void testConsecutiveMiningAppliesDelay() {
        Location loc1 = new Location(mockWorld, 1, 64, 1);
        Location loc2 = new Location(mockWorld, 2, 64, 1);
        CustomBlockDefinition soft = CustomBlockDefinition.builder("custom:soft")
                .hardness(0.05f) // 1 tick break (0.05 * 20 = 1 tick)
                .build();
        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(soft));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);

        // Mine block 1
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(1, 64, 1), loc1);
        miningManager.tick(); // Breaks block 1
        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()), "Block 1 should break");

        // Immediately start mining block 2 (consecutive mining without letting go of mouse)
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(2, 64, 1), loc2);
        assertTrue(miningManager.hasActiveSession(mockPlayer.getUniqueId()), "Session 2 should be active");

        // First 5 ticks should be in consecutive mining delay (progress does NOT increase)
        for (int i = 0; i < 5; i++) {
            miningManager.tick();
            assertTrue(miningManager.hasActiveSession(mockPlayer.getUniqueId()), "Session 2 should still be active during delay tick " + i);
        }

        // On tick 6 (after 5-tick delay), progress should apply and break block 2
        miningManager.tick();
        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()), "Block 2 should break on tick after delay");
    }

    @Test
    public void testHotbarSwappingFromInvalidToValidToolStartsMining() {
        Location loc = new Location(mockWorld, 1, 64, 1);
        CustomBlockDefinition ore = CustomBlockDefinition.builder("custom:iron_ore")
                .hardness(0.4f) // 0.4 * 30 = 12 ticks base; Iron Pickaxe speedMultiplier = 6.0 -> 0.5 progress/tick -> 2 ticks
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE, vn.haohan.itemcore.api.block.ToolTier.IRON)
                .build();
        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(ore));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);

        // Player starts aiming at block with bare hand (invalid)
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(1, 64, 1), loc);
        // hasActiveSession returns false because hand is not effective tool
        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()));
        assertEquals(0, overlayRenderer.spawnCount.get());

        // Player swaps hotbar slot to an Iron Pickaxe while still holding left click
        ItemStack ironPick = TestItemService.mockItem(Material.IRON_PICKAXE);
        Mockito.when(mockPlayer.getInventory().getItemInMainHand()).thenReturn(ironPick);
        miningManager.handleItemSwap(mockPlayer, ironPick);

        // Session should now be active and effective, crack overlay spawned
        assertTrue(miningManager.hasActiveSession(mockPlayer.getUniqueId()), "Session should become active after swapping to valid tool");
        assertEquals(1, overlayRenderer.spawnCount.get(), "Crack overlay should spawn when tool becomes valid");

        // Tick 1: reaches 0.5 progress
        miningManager.tick();
        assertTrue(miningManager.hasActiveSession(mockPlayer.getUniqueId()));

        // Tick 2: reaches >= 1.0 progress -> breaks block
        miningManager.tick();
        assertFalse(miningManager.hasActiveSession(mockPlayer.getUniqueId()), "Block should break after mining completes");
        Mockito.verify(blockService).removeCustomBlock(loc);
    }

    @Test
    public void testNativeHotbarSwapSendsBlockChangeAndUpdatesStrategy() {
        Location loc = new Location(mockWorld, 2, 64, 2);
        vn.haohan.itemcore.api.block.NativeCustomBlock nativeOre = vn.haohan.itemcore.api.block.NativeCustomBlock.builder("haohan:anorthosite_ore")
                .customVariation(774)
                .hardness(90.0f)
                .mineableWith(vn.haohan.itemcore.api.block.ToolType.PICKAXE, vn.haohan.itemcore.api.block.ToolTier.NETHERITE)
                .build();
        Mockito.when(blockService.getCustomBlock(Mockito.any())).thenReturn(Optional.of(nativeOre));
        Mockito.when(blockService.isCustomBlock(Mockito.any())).thenReturn(true);
        org.bukkit.block.data.BlockData mockData = Mockito.mock(org.bukkit.block.data.BlockData.class);
        Mockito.when(mockBlock.getBlockData()).thenReturn(mockData);

        // Player starts digging native block with Diamond Axe (invalid tool)
        ItemStack diamondAxe = TestItemService.mockItem(Material.DIAMOND_AXE);
        Mockito.when(mockPlayer.getInventory().getItemInMainHand()).thenReturn(diamondAxe);
        miningManager.handleDiggingAction(mockPlayer, DiggingAction.START_DIGGING, new Vector3i(2, 64, 2), loc);

        // Player swaps to Netherite Pickaxe
        ItemStack netheritePick = TestItemService.mockItem(Material.NETHERITE_PICKAXE);
        miningManager.handleItemSwap(mockPlayer, netheritePick);

        // Must send sendBlockChange to reset client cracking stage
        Mockito.verify(mockPlayer).sendBlockChange(loc, mockData);
    }

    @Test
    public void testGetPlayerToolFallsBackAndUsesHeldSlot() {
        ItemStack handItem = TestItemService.mockItem(Material.DIAMOND_AXE);
        Mockito.when(mockPlayer.getInventory().getItemInMainHand()).thenReturn(handItem);

        // Without slot change, returns item in main hand
        assertEquals(handItem, miningManager.getPlayerTool(mockPlayer));

        // When player slot is updated to slot 3
        ItemStack slot3Item = TestItemService.mockItem(Material.NETHERITE_PICKAXE);
        Mockito.when(mockPlayer.getInventory().getItem(3)).thenReturn(slot3Item);

        // Simulate HELD_ITEM_CHANGE by calling handleItemSwap
        // Slot tracking is updated in manager
        miningManager.handleItemSwap(mockPlayer, slot3Item);
        // Direct tool check
        assertNotNull(miningManager.getPlayerTool(mockPlayer));
    }
}

