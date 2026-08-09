package vn.haohan.itemcore.api.item;

import java.util.List;

/** Adds an ability label to the item's lore. */
public record AbilityComponent(String abilityId, String displayName) implements ItemComponent {
    public AbilityComponent {
        if (abilityId == null || abilityId.isBlank()) throw new IllegalArgumentException("Ability id cannot be blank");
        displayName = displayName == null ? abilityId : displayName;
    }

    public AbilityComponent(String abilityId) { this(abilityId, abilityId); }

    @Override
    public void appendLore(List<String> lore) { lore.add("&bAbility: &f" + displayName); }
}
