package vn.haohan.itemcore.api.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Adds a tier label to item lore and stores tier in PDC. */
public record TierComponent(String tierName, int tier) implements ItemComponent {

    private static final NamespacedKey TIER_LEVEL_KEY = new NamespacedKey("haohanitemcore", "tool_tier_level");
    private static final NamespacedKey TIER_NAME_KEY = new NamespacedKey("haohanitemcore", "tool_tier_name");

    public TierComponent(int tier) {
        this(null, tier);
    }

    public TierComponent {
        if (tier < 1) throw new IllegalArgumentException("Tier must be positive");
    }

    @Override
    public void apply(ItemStack item, ItemDefinition definition) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(TIER_LEVEL_KEY, PersistentDataType.INTEGER, tier);
        if (tierName != null && !tierName.isBlank()) {
            pdc.set(TIER_NAME_KEY, PersistentDataType.STRING, tierName.toLowerCase());
        }
        item.setItemMeta(meta);
    }

    @Override
    public void appendLore(List<String> lore) {
        String label = (tierName != null && !tierName.isBlank()) ? tierName : String.valueOf(tier);
        lore.add("&7Tier: &f" + label);
    }
}
