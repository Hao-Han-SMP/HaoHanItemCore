package vn.haohan.itemcore.internal.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronizes client-side block prediction and vertical NoteBlock neighbors via Paper API & PacketEvents.
 */
public final class AdjacentNoteBlockUpdateHelper {

    // ponytail: single latest sequence per player; client processes block placement packets sequentially
    private static final Map<UUID, Integer> pendingPlaceSequences = new ConcurrentHashMap<>();

    private AdjacentNoteBlockUpdateHelper() {}

    public static void recordPendingPlacement(UUID playerId, int sequence) {
        if (playerId != null && sequence >= 0) {
            pendingPlaceSequences.put(playerId, sequence);
        }
    }

    public static Integer pollPendingPlacement(UUID playerId) {
        return playerId != null ? pendingPlaceSequences.remove(playerId) : null;
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            pendingPlaceSequences.remove(playerId);
        }
    }

    public static void acknowledgeBlockChanges(Player player) {
        if (player == null) return;
        Integer seq = pollPendingPlacement(player.getUniqueId());
        if (seq != null) {
            NoteBlockEventListener.acknowledgeBlockChange(player, seq);
        }
    }

    public static boolean hasCustomVerticalNeighbor(Block block, CustomBlockRegistry registry) {
        if (block == null || registry == null) return false;
        return isNativeBlock(block.getRelative(BlockFace.DOWN), registry)
                || isNativeBlock(block.getRelative(BlockFace.UP), registry);
    }

    public static void resendCustomVerticalNeighbors(Block changedBlock, Player actor, CustomBlockRegistry registry) {
        if (changedBlock == null || registry == null) return;
        for (BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.UP}) {
            Block neighbor = changedBlock.getRelative(face);
            if (!isNativeBlock(neighbor, registry)) continue;

            Location loc = neighbor.getLocation();
            BlockData data = neighbor.getBlockData();
            if (actor != null && actor.isOnline()) {
                acknowledgeBlockChanges(actor);
                actor.sendBlockChange(loc, data);
            }
            World world = changedBlock.getWorld();
            if (world == null) continue;
            int trackingDistance = (Bukkit.getViewDistance() + 1) * 16;
            for (Player viewer : world.getPlayers()) {
                if (actor != null && viewer.getUniqueId().equals(actor.getUniqueId())) continue;
                Location vLoc = viewer.getLocation();
                if (Math.abs(vLoc.getX() - loc.getX()) <= trackingDistance
                        && Math.abs(vLoc.getZ() - loc.getZ()) <= trackingDistance) {
                    viewer.sendBlockChange(loc, data);
                }
            }
        }
    }

    public static void finishAdjacentBlockChange(Player player, Block changedBlock, CustomBlockRegistry registry) {
        if (changedBlock == null || player == null || !player.isOnline()) return;
        acknowledgeBlockChanges(player);
        player.sendBlockChange(changedBlock.getLocation(), changedBlock.getBlockData());
        resendCustomVerticalNeighbors(changedBlock, player, registry);
    }

    public static void finishVerticalNeighborBreak(Player player, Block brokenBlock, CustomBlockRegistry registry) {
        if (brokenBlock == null || registry == null || !hasCustomVerticalNeighbor(brokenBlock, registry)) return;
        if (player != null && player.isOnline()) {
            acknowledgeBlockChanges(player);
            try {
                player.sendBlockChange(brokenBlock.getLocation(), Bukkit.createBlockData(Material.AIR));
            } catch (Throwable ignored) {}
        }
        resendCustomVerticalNeighbors(brokenBlock, player, registry);
    }

    private static boolean isNativeBlock(Block block, CustomBlockRegistry registry) {
        if (block == null || block.getType() != Material.NOTE_BLOCK) return false;
        if (!(block.getBlockData() instanceof NoteBlock noteBlock)) return false;
        int variation = NoteBlockVariationTable.resolveVariation(noteBlock);
        return variation >= 0 && registry.getByVariation(variation).isPresent();
    }
}
