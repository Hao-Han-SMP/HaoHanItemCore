package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import vn.haohan.itemcore.api.block.CustomBlockInteractContext;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.api.block.NativeCustomBlock;

import java.util.Objects;
import java.util.Optional;

/**
 * Event listener intercepting NoteBlock interactions, physics updates, piston movements,
 * and sound emissions for native custom blocks.
 *
 * <p>Protects native NoteBlock custom blockstate representations against vanilla tuning,
 * redstone activation sounds, piston shifts, and vertical neighbor physics desynchronization.</p>
 */
public final class NoteBlockEventListener implements Listener {

    private final CustomBlockRegistry registry;
    private final Plugin plugin;

    public NoteBlockEventListener(CustomBlockRegistry registry, Plugin plugin) {
        this.registry = Objects.requireNonNull(registry, "CustomBlockRegistry cannot be null");
        this.plugin = plugin;
    }

    /**
     * Retrieves the {@link NativeCustomBlock} definition for the given block if it represents one.
     *
     * @param block the block to inspect
     * @return an {@link Optional} containing the native custom block definition if found
     */
    public Optional<NativeCustomBlock> getNativeCustomBlock(Block block) {
        if (block == null || block.getType() != Material.NOTE_BLOCK) {
            return Optional.empty();
        }
        if (!(block.getBlockData() instanceof NoteBlock noteBlock)) {
            return Optional.empty();
        }
        int variation = NoteBlockVariationTable.resolveVariation(noteBlock);
        if (variation < 0) {
            return Optional.empty();
        }
        return registry.getByVariation(variation);
    }

    /**
     * Checks whether the specified block is registered as a native NoteBlock custom block.
     *
     * @param block the block to inspect
     * @return true if the block is a registered native custom block, false otherwise
     */
    public boolean isNativeBlock(Block block) {
        return getNativeCustomBlock(block).isPresent();
    }

    /**
     * Checks if right-click note pitch tuning should be denied on the given block.
     *
     * @param block the block being interacted with
     * @return true if block is a native custom block
     */
    public boolean shouldDenyNoteTune(Block block) {
        return isNativeBlock(block);
    }

    /**
     * Intercepts right-clicks on NoteBlock native custom blocks to prevent vanilla pitch tuning,
     * while delegating to explicit custom block interact handlers if configured.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTuneNoteBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(block);
        if (optDef.isPresent()) {
            NativeCustomBlock def = optDef.get();
            if (def.getInteractHandler() != null) {
                try {
                    def.getInteractHandler().onInteract(new CustomBlockInteractContext(
                            event.getPlayer(),
                            block,
                            event.getItem(),
                            event.getHand() != null ? event.getHand() : EquipmentSlot.HAND,
                            event.getAction()
                    ));
                } catch (Throwable t) {
                    if (plugin != null) {
                        plugin.getLogger().warning("Error executing custom block interact action for "
                                + def.getId() + ": " + t.getMessage());
                    }
                }
            }
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    /**
     * Cancels redstone or player-triggered note playback on native custom blocks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNotePlay(NotePlayEvent event) {
        if (isNativeBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * Suppresses generic Minecraft game events (such as Sculk sensor triggers) for NoteBlock sounds.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGenericGameEvent(GenericGameEvent event) {
        if (event.getEvent() == GameEvent.NOTE_BLOCK_PLAY
                || (event.getEvent() != null && "note_block_play".equals(event.getEvent().getKey().getKey()))) {
            Location loc = event.getLocation();
            if (loc != null) {
                try {
                    Block block = loc.getBlock();
                    if (isNativeBlock(block)) {
                        event.setCancelled(true);
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    /**
     * Prevents pistons from extending into or pushing native custom blocks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (isNativeBlock(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents pistons from pulling native custom blocks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (isNativeBlock(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Suppresses block physics updates on native custom blocks and their vertical neighbors
     * to eliminate visual popping, instrument resetting, and desynchronization.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        // Short-circuit self first before triggering chunk relative lookups
        if (isNativeBlock(block)
                || isNativeBlock(block.getRelative(BlockFace.DOWN))
                || isNativeBlock(block.getRelative(BlockFace.UP))) {
            event.setCancelled(true);
        }
    }

    /**
     * Sends sequence acknowledgement via PacketEvents to synchronize client-side prediction
     * without direct NMS access.
     *
     * @param player the player to acknowledge
     * @param sequence the sequence ID received from client digging or placement packets
     */
    public static void acknowledgeBlockChange(Player player, int sequence) {
        if (player == null || sequence < 0) return;
        try {
            if (PacketEvents.getAPI() != null && PacketEvents.getAPI().getPlayerManager() != null) {
                WrapperPlayServerAcknowledgeBlockChanges packet =
                        new WrapperPlayServerAcknowledgeBlockChanges(sequence);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        } catch (Throwable ignored) {
            // Graceful fallback when PacketEvents is uninitialized (unit tests or mock environment)
        }
    }

    /**
     * Resends block state packets for vertical neighbors of a given location to prevent
     * client-side rendering artifacts after block breaking or placement.
     *
     * @param player the target player
     * @param loc the central block location
     */
    public static void resendNeighborBlockStates(Player player, Location loc) {
        if (player == null || loc == null || loc.getWorld() == null) return;
        try {
            Block block = loc.getBlock();
            Block above = block.getRelative(BlockFace.UP);
            Block below = block.getRelative(BlockFace.DOWN);
            player.sendBlockChange(above.getLocation(), above.getBlockData());
            player.sendBlockChange(below.getLocation(), below.getBlockData());
        } catch (Throwable ignored) {
        }
    }
}
