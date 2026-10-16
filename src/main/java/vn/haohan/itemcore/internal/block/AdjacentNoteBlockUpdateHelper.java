package vn.haohan.itemcore.internal.block;

import org.bukkit.Location;
import org.bukkit.Material;
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

    // ponytail: single latest prediction sequence per player
    private static final Map<UUID, Integer> pendingActionSequences = new ConcurrentHashMap<>();

    private AdjacentNoteBlockUpdateHelper() {}

    public static void recordPendingAction(UUID playerId, int sequence) {
        if (playerId != null && sequence >= 0) {
            pendingActionSequences.put(playerId, sequence);
        }
    }

    public static void recordPendingPlacement(UUID playerId, int sequence) {
        recordPendingAction(playerId, sequence);
    }

    public static Integer pollPendingAction(UUID playerId) {
        return playerId != null ? pendingActionSequences.remove(playerId) : null;
    }

    public static Integer pollPendingPlacement(UUID playerId) {
        return pollPendingAction(playerId);
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            pendingActionSequences.remove(playerId);
        }
    }

    public static void acknowledgeBlockChanges(Player player) {
        if (player == null) return;
        Integer seq = pollPendingAction(player.getUniqueId());
        if (seq != null) {
            NoteBlockEventListener.acknowledgeBlockChange(player, seq);
        }
    }

    public static void resendCustomVerticalNeighbors(Block changedBlock, Player actor, CustomBlockRegistry registry) {
        if (changedBlock == null || registry == null || actor == null || !actor.isOnline()) return;
        for (BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.UP}) {
            Block neighbor = changedBlock.getRelative(face);
            if (!isNativeBlock(neighbor, registry)) continue;
            actor.sendBlockChange(neighbor.getLocation(), neighbor.getBlockData());
        }
    }

    public static void finishAdjacentBlockChange(Player player, Block changedBlock, CustomBlockRegistry registry) {
        if (changedBlock == null || player == null || !player.isOnline()) return;
        player.sendBlockChange(changedBlock.getLocation(), changedBlock.getBlockData());
        resendCustomVerticalNeighbors(changedBlock, player, registry);
        acknowledgeBlockChanges(player);
    }

    public static boolean isNativeBlock(Block block, CustomBlockRegistry registry) {
        if (block == null || block.getType() != Material.NOTE_BLOCK) return false;
        if (!(block.getBlockData() instanceof NoteBlock noteBlock)) return false;
        int variation = NoteBlockVariationTable.resolveVariation(noteBlock);
        return variation >= 0 && registry.getByVariation(variation).isPresent();
    }
}
