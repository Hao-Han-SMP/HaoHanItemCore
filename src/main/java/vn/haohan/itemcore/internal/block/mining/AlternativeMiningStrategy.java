package vn.haohan.itemcore.internal.block.mining;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.internal.block.MiningSpeedCalculator;

/**
 * Strategy for Alternative custom blocks (backed by physical Barrier + BlockDisplay).
 *
 * <p>Uses server-side tick-based mining progression managed by
 * {@link vn.haohan.itemcore.internal.block.CustomBlockMiningManager} and PacketEvents
 * break animations.</p>
 */
public final class AlternativeMiningStrategy implements MiningSpeedStrategy {

    @Override
    public void onStartDigging(Player player, Block block, CustomBlockDefinition def, ItemStack tool) {
        // Alternative mining uses CustomBlockMiningManager's tick-based sessions
    }

    @Override
    public void onStopDigging(Player player, Block block, CustomBlockDefinition def) {
        // Handled in CustomBlockMiningManager.cancelSession
    }

    @Override
    public float getBreakTimeSeconds(Player player, ItemStack tool, CustomBlockDefinition def) {
        float progressPerTick = MiningSpeedCalculator.calculateProgressPerTick(player, tool, def);
        return progressPerTick > 0.0f ? (1.0f / progressPerTick) / 20.0f : -1.0f;
    }
}
