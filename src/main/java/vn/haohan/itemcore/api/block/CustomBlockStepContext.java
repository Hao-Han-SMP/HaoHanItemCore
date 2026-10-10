package vn.haohan.itemcore.api.block;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Context provided when a player steps onto or walks over a custom block.
 */
public record CustomBlockStepContext(
        Player player,
        Block block,
        Location from,
        Location to
) {}
