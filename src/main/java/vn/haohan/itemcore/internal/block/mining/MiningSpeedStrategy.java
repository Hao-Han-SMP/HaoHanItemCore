package vn.haohan.itemcore.internal.block.mining;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;

/**
 * Strategy interface defining how mining progression and digging lifecycle
 * are handled for different custom block execution models (Native NoteBlock vs Alternative Barrier/Display).
 */
public interface MiningSpeedStrategy {

    /**
     * Invoked when a player begins digging a custom block.
     *
     * @param player the player digging
     * @param block the target block
     * @param def the custom block definition
     * @param tool the tool currently in main hand
     */
    void onStartDigging(Player player, Block block, CustomBlockDefinition def, ItemStack tool);

    /**
     * Invoked when a player stops or finishes digging a custom block.
     *
     * @param player the player digging
     * @param block the target block
     * @param def the custom block definition
     */
    void onStopDigging(Player player, Block block, CustomBlockDefinition def);

    /**
     * Computes the total break time in seconds required to break the block under current conditions.
     *
     * @param player the player digging, or null
     * @param tool the tool being used, or null
     * @param def the custom block definition
     * @return break time in seconds, or -1.0 if unbreakable
     */
    float getBreakTimeSeconds(Player player, ItemStack tool, CustomBlockDefinition def);
}
