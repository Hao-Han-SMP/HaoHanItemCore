package vn.haohan.itemcore.api.item;

import java.util.List;

/** Adds a tier label to item lore. */
public record TierComponent(int tier) implements ItemComponent {
    public TierComponent {
        if (tier < 1) throw new IllegalArgumentException("Tier must be positive");
    }

    @Override
    public void appendLore(List<String> lore) { lore.add("&7Tier: &f" + tier); }
}
