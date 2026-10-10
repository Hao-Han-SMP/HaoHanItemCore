package vn.haohan.itemcore.api.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockInteractionTest {

    @BeforeEach
    void setUp() {
        CustomBlockActionRegistry.clear();
    }

    @Test
    void testActionRegistryAndHandlerExecution() {
        AtomicBoolean interactCalled = new AtomicBoolean(false);
        AtomicBoolean stepCalled = new AtomicBoolean(false);

        CustomBlockActionRegistry.registerInteractAction("test_interact", ctx -> {
            interactCalled.set(true);
        });

        CustomBlockActionRegistry.registerStepAction("test_step", ctx -> {
            stepCalled.set(true);
        });

        Player player = Mockito.mock(Player.class);
        Block block = Mockito.mock(Block.class);

        CustomBlockInteractContext interactCtx = new CustomBlockInteractContext(
                player, block, Mockito.mock(ItemStack.class), EquipmentSlot.HAND, Action.RIGHT_CLICK_BLOCK);
        CustomBlockActionRegistry.getInteractAction("test_interact").orElseThrow().onInteract(interactCtx);
        assertTrue(interactCalled.get());

        CustomBlockStepContext stepCtx = new CustomBlockStepContext(
                player, block, null, null);
        CustomBlockActionRegistry.getStepAction("test_step").orElseThrow().onStep(stepCtx);
        assertTrue(stepCalled.get());
    }

    @Test
    void testCustomBlockDefinitionCarriesHandlersAndSoundGroup() {
        AtomicBoolean clicked = new AtomicBoolean(false);

        CustomBlockDefinition def = CustomBlockDefinition.builder("example:custom_bench")
                .soundGroup(CustomBlockSoundGroup.STONE)
                .onInteract(ctx -> clicked.set(true))
                .build();

        assertNotNull(def.getSoundGroup());
        assertEquals("minecraft:block.stone.place", def.getSoundGroup().getPlaceSound().soundKey());

        assertNotNull(def.getInteractHandler());
        def.getInteractHandler().onInteract(null);
        assertTrue(clicked.get());
    }
}
