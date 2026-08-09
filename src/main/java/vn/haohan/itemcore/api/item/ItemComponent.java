package vn.haohan.itemcore.api.item;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Optional programmatic extension applied while an item stack is created. */
public interface ItemComponent {
    default void apply(ItemStack item, ItemDefinition definition) { }

    default void appendLore(List<String> lore) { }
}
