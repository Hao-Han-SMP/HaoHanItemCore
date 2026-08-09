package vn.haohan.itemcore.api.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Per-item persistent state that is independent from the registered definition. */
public final class ItemInstanceData {
    private final NamespacedKey durabilityKey;
    private final NamespacedKey upgradeLevelKey;

    public ItemInstanceData(Plugin plugin) {
        this(new NamespacedKey(plugin, "durability"), new NamespacedKey(plugin, "upgrade_level"));
    }

    public ItemInstanceData(NamespacedKey durabilityKey, NamespacedKey upgradeLevelKey) {
        this.durabilityKey = durabilityKey;
        this.upgradeLevelKey = upgradeLevelKey;
    }

    public int durability(ItemStack item, int defaultValue) {
        if (item == null || !item.hasItemMeta()) return defaultValue;
        Integer value = item.getItemMeta().getPersistentDataContainer().get(durabilityKey, PersistentDataType.INTEGER);
        return value == null ? defaultValue : value;
    }

    public void setDurability(ItemStack item, int value) {
        if (item == null) return;
        var meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, Math.max(0, value));
        item.setItemMeta(meta);
    }

    public int upgradeLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer value = item.getItemMeta().getPersistentDataContainer().get(upgradeLevelKey, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    public void setUpgradeLevel(ItemStack item, int level) {
        if (item == null) return;
        var meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(upgradeLevelKey, PersistentDataType.INTEGER, Math.max(0, level));
        item.setItemMeta(meta);
    }
}
