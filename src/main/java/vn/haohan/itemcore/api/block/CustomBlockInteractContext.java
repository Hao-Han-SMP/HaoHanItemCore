package vn.haohan.itemcore.api.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Context provided when a player interacts with a custom block.
 */
public record CustomBlockInteractContext(
        Player player,
        Block block,
        ItemStack itemInHand,
        EquipmentSlot hand,
        Action action
) {}
