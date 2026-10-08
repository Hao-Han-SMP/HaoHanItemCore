package vn.haohan.itemcore.api.block;

import org.bukkit.Material;

/**
 * Standard tool types.
 */
public enum ToolType {
    PICKAXE,
    AXE,
    SHOVEL,
    HOE,
    SWORD,
    ANY;

    public static ToolType fromMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();
        if (name.endsWith("_PICKAXE")) return PICKAXE;
        if (name.endsWith("_AXE") && !name.endsWith("_PICKAXE")) return AXE;
        if (name.endsWith("_SHOVEL")) return SHOVEL;
        if (name.endsWith("_HOE")) return HOE;
        if (name.endsWith("_SWORD")) return SWORD;
        return null;
    }
}
