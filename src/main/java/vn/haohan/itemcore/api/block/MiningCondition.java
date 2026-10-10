package vn.haohan.itemcore.api.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import vn.haohan.itemcore.api.HaoHanItemCore;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Condition determining whether a tool can harvest items from a custom block.
 * If false, the block still breaks but drops no items.
 */
@FunctionalInterface
public interface MiningCondition {

    /**
     * Evaluates if the tool in context meets harvest criteria.
     *
     * @param context mining context
     * @return true if harvestable, false if block drops nothing
     */
    boolean canHarvest(MiningContext context);

    static MiningCondition always() {
        return context -> true;
    }

    static MiningCondition requiresPickaxe() {
        return context -> {
            ItemStack tool = context != null ? context.tool() : null;
            if (tool == null || tool.getType() == null || tool.getType() == Material.AIR) return false;
            return ToolType.fromMaterial(tool.getType()) == ToolType.PICKAXE;
        };
    }

    static MiningCondition minVanillaTier(ToolTier minTier) {
        Objects.requireNonNull(minTier, "minTier cannot be null");
        return context -> {
            ItemStack tool = context != null ? context.tool() : null;
            if (tool == null || tool.getType() == null || tool.getType() == Material.AIR) return false;
            ToolTier tier = resolveTier(tool);
            return tier != null && tier.level() >= minTier.level();
        };
    }

    static MiningCondition requiresTool(ToolType type, ToolTier minTier) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(minTier, "minTier cannot be null");
        return context -> {
            ItemStack tool = context != null ? context.tool() : null;
            if (tool == null || tool.getType() == null || tool.getType() == Material.AIR) return false;
            ToolType t = ToolType.fromMaterial(tool.getType());
            ToolTier tier = resolveTier(tool);
            if (type != ToolType.ANY && t != type) return false;
            return tier != null && tier.level() >= minTier.level();
        };
    }

    static MiningCondition of(Predicate<ItemStack> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return context -> predicate.test(context != null ? context.tool() : null);
    }

    private static ToolTier resolveTier(ItemStack tool) {
        if (tool == null || tool.getType() == null || tool.getType() == Material.AIR) return null;
        try {
            var core = HaoHanItemCore.get();
            if (core != null && core.getToolTierRegistry() != null) {
                ToolTier t = core.getToolTierRegistry().findTier(tool);
                if (t != null) return t;
            }
        } catch (Throwable ignored) {}
        return ToolTier.fromMaterial(tool.getType());
    }
}
