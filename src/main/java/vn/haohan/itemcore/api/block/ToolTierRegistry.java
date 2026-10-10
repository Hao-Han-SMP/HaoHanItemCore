package vn.haohan.itemcore.api.block;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.TierComponent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry storing all standard and custom tool tiers.
 */
public class ToolTierRegistry {

    private static final NamespacedKey TIER_LEVEL_KEY = new NamespacedKey("haohanitemcore", "tool_tier_level");
    private static final NamespacedKey TIER_NAME_KEY = new NamespacedKey("haohanitemcore", "tool_tier_name");

    private final Map<String, ToolTier> tiers = new ConcurrentHashMap<>();

    public ToolTierRegistry() {
        register(ToolTier.WOOD);
        register(ToolTier.GOLD);
        register(ToolTier.STONE);
        register(ToolTier.IRON);
        register(ToolTier.DIAMOND);
        register(ToolTier.NETHERITE);
    }

    public void register(ToolTier tier) {
        Objects.requireNonNull(tier, "ToolTier cannot be null");
        tiers.put(tier.name().toLowerCase(), tier);
    }

    public ToolTier register(String name, int level, float baseMultiplier) {
        ToolTier tier = ToolTier.of(name, level, baseMultiplier);
        register(tier);
        return tier;
    }

    public Optional<ToolTier> get(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(tiers.get(name.toLowerCase()));
    }

    public ToolTier getOrDefault(String name, ToolTier fallback) {
        if (name == null) return fallback;
        return tiers.getOrDefault(name.toLowerCase(), fallback);
    }

    /**
     * Resolves the ToolTier from an ItemStack.
     * Hierarchy:
     * 1. PDC tag on ItemStack (tool_tier_name / tool_tier_level)
     * 2. Custom ItemDefinition TierComponent or 'tier' property
     * 3. Fallback to vanilla material mapping
     */
    public ToolTier findTier(ItemStack item) {
        if (item == null || item.getType() == null || item.getType() == Material.AIR) return null;

        // 1. PDC check
        if (item.hasItemMeta()) {
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(TIER_NAME_KEY, PersistentDataType.STRING)) {
                String tierName = pdc.get(TIER_NAME_KEY, PersistentDataType.STRING);
                if (tierName != null && tiers.containsKey(tierName.toLowerCase())) {
                    return tiers.get(tierName.toLowerCase());
                }
            }
            if (pdc.has(TIER_LEVEL_KEY, PersistentDataType.INTEGER)) {
                Integer level = pdc.get(TIER_LEVEL_KEY, PersistentDataType.INTEGER);
                if (level != null) {
                    // Find by level or create dynamic tier
                    for (ToolTier t : tiers.values()) {
                        if (t.level() == level) return t;
                    }
                    return ToolTier.of("tier_" + level, level, 2.0f * level);
                }
            }
        }

        // 2. Custom ItemDefinition check
        try {
            var core = HaoHanItemCore.get();
            if (core != null && core.getItemService() != null) {
                String customId = core.getItemService().getId(item);
                if (customId != null) {
                    ItemDefinition def = core.getItemService().getDefinition(customId);
                    if (def != null) {
                        for (var comp : def.getComponents()) {
                            if (comp instanceof TierComponent tc) {
                                if (tc.tierName() != null && tiers.containsKey(tc.tierName().toLowerCase())) {
                                    return tiers.get(tc.tierName().toLowerCase());
                                }
                                for (ToolTier t : tiers.values()) {
                                    if (t.level() == tc.tier()) return t;
                                }
                                return ToolTier.of("tier_" + tc.tier(), tc.tier(), 2.0f * tc.tier());
                            }
                        }
                        Object tierProp = def.getProperties().get("tier");
                        if (tierProp instanceof String tn && tiers.containsKey(tn.toLowerCase())) {
                            return tiers.get(tn.toLowerCase());
                        } else if (tierProp instanceof Number num) {
                            int lvl = num.intValue();
                            for (ToolTier t : tiers.values()) {
                                if (t.level() == lvl) return t;
                            }
                            return ToolTier.of("tier_" + lvl, lvl, 2.0f * lvl);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. Vanilla fallback
        return ToolTier.fromMaterial(item.getType());
    }
}
