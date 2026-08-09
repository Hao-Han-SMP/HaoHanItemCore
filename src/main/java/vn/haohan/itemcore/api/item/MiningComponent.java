package vn.haohan.itemcore.api.item;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Adds hidden mining efficiency to a newly-created stack. */
public record MiningComponent(int efficiency) implements ItemComponent {
    public MiningComponent {
        if (efficiency < 1) throw new IllegalArgumentException("Mining efficiency must be positive");
    }

    @Override
    public void apply(ItemStack item, ItemDefinition definition) {
        item.addUnsafeEnchantment(Enchantment.EFFICIENCY, efficiency);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
    }

    @Override
    public void appendLore(List<String> lore) { lore.add("&a+" + efficiency + " &bMining Efficiency"); }
}
