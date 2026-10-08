package vn.haohan.itemcore.api.block;

import org.bukkit.Material;

/**
 * Standard tool tiers ordered by mining power.
 */
public enum ToolTier {
    WOOD(1),
    GOLD(1),
    STONE(2),
    IRON(3),
    DIAMOND(4),
    NETHERITE(5);

    private final int level;

    ToolTier(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Resolves the ToolTier from a vanilla material, or null if not a recognized tool.
     */
    public static ToolTier fromMaterial(Material material) {
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
}
