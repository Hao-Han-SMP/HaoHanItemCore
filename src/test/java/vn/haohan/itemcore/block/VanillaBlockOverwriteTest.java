package vn.haohan.itemcore.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
import vn.haohan.itemcore.internal.config.CustomBlockConfigParser;
import vn.haohan.itemcore.api.item.ItemDefinition;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class VanillaBlockOverwriteTest {

    private CustomBlockRegistry registry;
    private CustomBlockStorage storage;
    private CustomBlockVisibilityManager visibilityManager;
    private DefaultCustomBlockService service;

    @BeforeEach
    public void setup() {
        registry = new DefaultCustomBlockRegistry();
        storage = Mockito.mock(CustomBlockStorage.class);
        visibilityManager = Mockito.mock(CustomBlockVisibilityManager.class);
        service = new DefaultCustomBlockService(registry, storage, visibilityManager, Mockito.mock(org.bukkit.plugin.Plugin.class, Mockito.RETURNS_DEEP_STUBS));
    }

    @Test
    public void testDefinitionCreationAndRegistryLookup() {
        CustomBlockDefinition def = CustomBlockDefinition.builder("minecraft:diamond_ore")
                .hardness(8.0f)
                .mineableWith(ToolType.PICKAXE, ToolTier.DIAMOND)
                .build();

        assertTrue(def.isVanillaOverwrite(), "Should be detected as vanilla overwrite");
        assertEquals(Material.DIAMOND_ORE, def.getBaseBlock());

        registry.register(def);

        Optional<CustomBlockDefinition> byId = registry.get("minecraft:diamond_ore");
        assertTrue(byId.isPresent());
        assertEquals(def, byId.get());

        Optional<CustomBlockDefinition> byMat = registry.getByMaterial(Material.DIAMOND_ORE);
        assertTrue(byMat.isPresent());
        assertEquals(def, byMat.get());
    }

    @Test
    public void testServiceDetectsVanillaWorldBlock() {
        CustomBlockDefinition def = CustomBlockDefinition.builder("minecraft:diamond_ore")
                .hardness(8.0f)
                .mineableWith(ToolType.PICKAXE, ToolTier.DIAMOND)
                .build();
        service.register(def);

        Block mockBlock = Mockito.mock(Block.class);
        Mockito.when(mockBlock.getType()).thenReturn(Material.DIAMOND_ORE);

        assertTrue(service.isCustomBlock(mockBlock), "Service should identify DIAMOND_ORE as custom block");
        assertEquals("minecraft:diamond_ore", service.getCustomBlockId(mockBlock));
        assertEquals(Optional.of(def), service.getCustomBlock(mockBlock));

        Block normalBlock = Mockito.mock(Block.class);
        Mockito.when(normalBlock.getType()).thenReturn(Material.COBBLESTONE);
        assertFalse(service.isCustomBlock(normalBlock));
    }

    @Test
    public void testToolTierEffectiveness() {
        ToolTier customTier = ToolTier.of("custom_tier", 5, 10.0f);
        CustomBlockDefinition def = CustomBlockDefinition.builder("minecraft:diamond_ore")
                .mineableWith(ToolType.PICKAXE, customTier)
                .build();

        ItemStack ironPick = Mockito.mock(ItemStack.class);
        Mockito.when(ironPick.getType()).thenReturn(Material.IRON_PICKAXE);
        assertFalse(def.isEffectiveTool(ironPick), "Iron pickaxe (level 3) should not be effective for level 5 ore");

        ItemStack diaPick = Mockito.mock(ItemStack.class);
        Mockito.when(diaPick.getType()).thenReturn(Material.DIAMOND_PICKAXE);
        assertFalse(def.isEffectiveTool(diaPick), "Vanilla diamond pickaxe (level 4) should not be effective for level 5 ore");
    }

    @Test
    public void testBlockBreakEventCancelledForIneffectiveTool() {
        CustomBlockDefinition def = CustomBlockDefinition.builder("minecraft:diamond_ore")
                .mineableWith(ToolType.PICKAXE, ToolTier.NETHERITE)
                .build();
        service.register(def);

        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        ItemStack woodenPick = Mockito.mock(ItemStack.class);
        Mockito.when(woodenPick.getType()).thenReturn(Material.WOODEN_PICKAXE);
        Mockito.when(inventory.getItemInMainHand()).thenReturn(woodenPick);
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);

        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getType()).thenReturn(Material.DIAMOND_ORE);
        Mockito.when(block.getLocation()).thenReturn(new org.bukkit.Location(Mockito.mock(World.class), 0, 0, 0));

        org.bukkit.plugin.Plugin mockPlugin = Mockito.mock(org.bukkit.plugin.Plugin.class);
        Mockito.when(mockPlugin.getName()).thenReturn("test");

        CustomBlockEventListener listener = new CustomBlockEventListener(
                service, storage, visibilityManager, null, mockPlugin
        );

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "Event must be cancelled when mined with insufficient tool tier");
    }

    @Test
    public void testConfigParserWithVanillaBlockAndTier() {
        ItemDefinition itemDef = ItemDefinition.builder("minecraft:ancient_debris")
                .material(Material.ANCIENT_DEBRIS)
                .properties(Map.of(
                        "custom_block_id", "minecraft:ancient_debris",
                        "custom_block_tool_type", "PICKAXE",
                        "custom_block_hardness", 12.0
                ))
                .build();

        CustomBlockDefinition parsed = CustomBlockConfigParser.parseCustomBlock(itemDef);
        assertNotNull(parsed);
        assertTrue(parsed.isVanillaOverwrite());
        assertEquals(Material.ANCIENT_DEBRIS, parsed.getBaseBlock());
        assertEquals(12.0f, parsed.getHardness());
        assertTrue(parsed.getHarvestToolTypes().contains(ToolType.PICKAXE));
    }

    @Test
    public void testVanillaBlockOverwriteWithoutDropConfigHasNullDropItemId() {
        ItemDefinition itemDef = ItemDefinition.builder("minecraft:iron_ore")
                .material(Material.IRON_ORE)
                .properties(Map.of(
                        "custom_block_id", "minecraft:iron_ore",
                        "custom_block_hardness", 4.0
                ))
                .build();

        CustomBlockDefinition parsed = CustomBlockConfigParser.parseCustomBlock(itemDef);
        assertNotNull(parsed);
        assertTrue(parsed.isVanillaOverwrite());
        assertNull(parsed.getDropItemId(), "Vanilla overwrite without explicit drop must have null dropItemId");
    }
}

