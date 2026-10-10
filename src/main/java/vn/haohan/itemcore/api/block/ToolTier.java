package vn.haohan.itemcore.api.block;

import org.bukkit.Material;

import java.util.Objects;

/**
 * Standard or custom tool tier with level and mining speed multiplier.
 */
public interface ToolTier {

    String name();

    int level();

    float baseMultiplier();

    default int getLevel() {
        return level();
    }

    default float getBaseMultiplier() {
        return baseMultiplier();
    }

    ToolTier WOOD = of("wood", 1, 2.0f);
    ToolTier GOLD = of("gold", 1, 12.0f);
    ToolTier STONE = of("stone", 2, 4.0f);
    ToolTier IRON = of("iron", 3, 6.0f);
    ToolTier DIAMOND = of("diamond", 4, 8.0f);
    ToolTier NETHERITE = of("netherite", 5, 9.0f);

    static ToolTier of(String name, int level, float baseMultiplier) {
        Objects.requireNonNull(name, "Tier name cannot be null");
        return new SimpleToolTier(name.toLowerCase(), level, baseMultiplier);
    }

    static ToolTier fromMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();
        if (name.startsWith("WOODEN_")) return WOOD;
        if (name.startsWith("GOLDEN_")) return GOLD;
        if (name.startsWith("STONE_")) return STONE;
        if (name.startsWith("IRON_")) return IRON;
        if (name.startsWith("DIAMOND_")) return DIAMOND;
        if (name.startsWith("NETHERITE_")) return NETHERITE;
        return null;
    }

    record SimpleToolTier(String name, int level, float baseMultiplier) implements ToolTier {
        public SimpleToolTier {
            Objects.requireNonNull(name, "Tier name cannot be null");
        }

        @Override
        public String toString() {
            return "ToolTier{" + name + ", level=" + level + ", mult=" + baseMultiplier + "}";
        }
    }
}
