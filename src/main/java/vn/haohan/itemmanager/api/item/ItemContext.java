package vn.haohan.itemmanager.api.item;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Context được truyền vào ItemBehavior khi event xảy ra.
 * Chứa đầy đủ thông tin cần thiết để behavior xử lý.
 */
public record ItemContext(
        Player player,
        ItemStack item,
        ItemDefinition definition,
        Event event
) {
}
