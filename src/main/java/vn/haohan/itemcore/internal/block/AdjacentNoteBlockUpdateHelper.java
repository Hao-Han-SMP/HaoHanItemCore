package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Ported from Oraxen's AdjacentNoteBlockUpdateHelper, adapted for Paper API & PacketEvents.
 * Manages vertical neighbor updates, block change desync prevention, and client sequence acknowledgement.
 */
public final class AdjacentNoteBlockUpdateHelper {

    public record PendingBlockChange(int sequence, int x, int y, int z, boolean placement) {}

    private static final Map<UUID, Deque<PendingBlockChange>> pendingBlockChanges = new ConcurrentHashMap<>();

    private AdjacentNoteBlockUpdateHelper() {}

    /**
     * Records a pending block change sequence received from a client packet (placement or digging).
     */
    public static void recordPendingChange(UUID playerId, int sequence, int x, int y, int z, boolean placement) {
        if (playerId == null || sequence < 0) return;
        Deque<PendingBlockChange> deque = pendingBlockChanges.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(new PendingBlockChange(sequence, x, y, z, placement));
        while (deque.size() > 32) {
            deque.pollFirst();
        }
    }

    /**
     * Clears pending block changes for a player on disconnect.
     */
    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            pendingBlockChanges.remove(playerId);
        }
    }

    /**
     * Acknowledges block changes for a player at a specific location using PacketEvents.
     */
    public static void acknowledgeBlockChanges(Player player, Location packetBlock, boolean placement) {
        if (player == null || packetBlock == null) return;
        Deque<PendingBlockChange> deque = pendingBlockChanges.get(player.getUniqueId());
        if (deque == null) return;

        PendingBlockChange matched = null;
        int targetX = packetBlock.getBlockX();
        int targetY = packetBlock.getBlockY();
        int targetZ = packetBlock.getBlockZ();

        for (PendingBlockChange change : deque) {
            if (change.placement() == placement && change.x() == targetX && change.y() == targetY && change.z() == targetZ) {
                matched = change;
                break;
            }
        }

        if (matched == null) return;
        final int seq = matched.sequence();
        deque.removeIf(change -> change.sequence() <= seq);
        NoteBlockEventListener.acknowledgeBlockChange(player, seq);
    }

    /**
     * Checks if the block has a native NoteBlock vertical neighbor (directly above or below).
     */
    public static boolean hasCustomVerticalNeighbor(Block block, CustomBlockRegistry registry) {
        if (block == null || registry == null) return false;
        return isNativeBlock(block.getRelative(BlockFace.UP), registry)
                || isNativeBlock(block.getRelative(BlockFace.DOWN), registry);
    }

    /**
     * Resends vertical neighbor block states to the actor and nearby viewers within view distance.
     */
    public static void resendCustomVerticalNeighbors(Block changedBlock, Player actor, CustomBlockRegistry registry) {
        if (changedBlock == null || registry == null) return;
        Map<Location, BlockData> updates = customVerticalNeighborStates(changedBlock, registry);
        if (updates.isEmpty()) return;

        if (actor != null && actor.isOnline()) {
            updates.forEach(actor::sendBlockChange);
        }

        World world = changedBlock.getWorld();
        Location origin = changedBlock.getLocation();
        UUID actorId = actor != null ? actor.getUniqueId() : null;
        int trackingDistance = (Bukkit.getViewDistance() + 1) * 16;

        for (Player viewer : world.getPlayers()) {
            if (actorId != null && viewer.getUniqueId().equals(actorId)) continue;
            Location viewerLoc = viewer.getLocation();
            if (Math.abs(viewerLoc.getX() - origin.getX()) <= trackingDistance
                    && Math.abs(viewerLoc.getZ() - origin.getZ()) <= trackingDistance) {
                updates.forEach(viewer::sendBlockChange);
            }
        }
    }

    /**
     * Finalizes an adjacent block change by sending block changes and acknowledging sequence.
     */
    public static void finishAdjacentBlockChange(Player player, Block changedBlock, Location packetBlock, CustomBlockRegistry registry) {
        if (changedBlock == null) return;

        if (player != null && player.isOnline()) {
            player.sendBlockChange(changedBlock.getLocation(), changedBlock.getBlockData());
            if (registry != null && hasCustomVerticalNeighbor(changedBlock, registry)) {
                resendCustomVerticalNeighbors(changedBlock, player, registry);
            }
            acknowledgeBlockChanges(player, packetBlock, true);
        }
    }

    public static Map<Location, BlockData> customVerticalNeighborStates(Block block, CustomBlockRegistry registry) {
        Map<Location, BlockData> updates = new LinkedHashMap<>(2);
        for (BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.UP}) {
            Block neighbor = block.getRelative(face);
            if (isNativeBlock(neighbor, registry)) {
                updates.put(neighbor.getLocation(), neighbor.getBlockData());
            }
        }
        return updates;
    }

    private static boolean isNativeBlock(Block block, CustomBlockRegistry registry) {
        if (block == null || block.getType() != org.bukkit.Material.NOTE_BLOCK) return false;
        if (!(block.getBlockData() instanceof org.bukkit.block.data.type.NoteBlock noteBlock)) return false;
        int variation = NoteBlockVariationTable.resolveVariation(noteBlock);
        return variation >= 0 && registry.getByVariation(variation).isPresent();
    }
}
