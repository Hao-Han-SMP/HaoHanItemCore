package vn.haohan.itemcore.api.item;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.List;

/** Declares custom durability; the mutable value can be stored with ItemInstanceData. */
public record DurabilityComponent(int maxDurability) implements ItemComponent {
    public DurabilityComponent {
        if (maxDurability < 1) throw new IllegalArgumentException("Durability must be positive");
    }

    @Override
    public void apply(ItemStack item, ItemDefinition definition) {
        var meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
            damageable.setUnbreakable(true);
            damageable.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(damageable);
        }
    }

    @Override
    public void appendLore(List<String> lore) { lore.add("&7Durability: &f" + maxDurability); }
}
