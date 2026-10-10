package vn.haohan.itemcore.internal.block;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.item.ItemDefinition;

import java.util.Collection;
import java.util.Map;

/**
 * Calculates per-tick mining progression for custom blocks.
 */
public final class MiningSpeedCalculator {

    private MiningSpeedCalculator() {}

    /**
     * Calculates the mining progress delta completed in a single server tick (0.0 to 1.0).
     */
    public static float calculateProgressPerTick(Player player, ItemStack tool, CustomBlockDefinition definition) {
        if (definition == null || definition.isUnbreakable()) {
            return 0.0f;
        }
        if (definition.isInstantBreak()) {
            return 1.0f;
        }

        float baseTicks = definition.getBreakTimeSeconds() * 20.0f;
        if (baseTicks <= 0.0f) {
            return 1.0f;
        }

        boolean isEffective = definition.isEffectiveTool(tool);
        if (!isEffective) {
            return 0.0f;
        }

        float speedMultiplier = 1.0f;

        // Tool multipliers (tier, custom mining_speed, paper attribute modifiers, efficiency enchantments)
        // only apply if the block has preferred tools configured.
        // If no preferred tools are defined, all tools and bare hand mine at the exact configured base speed (speedMultiplier = 1.0f).
        if (definition.hasPreferredTools() && tool != null && tool.getType() != null && tool.getType() != Material.AIR) {
            // 1. ToolTier base multiplier
            ToolTier tier = null;
            try {
                var core = HaoHanItemCore.get();
                if (core != null && core.getToolTierRegistry() != null) {
                    tier = core.getToolTierRegistry().findTier(tool);
                }
            } catch (Throwable ignored) {}
            if (tier == null) {
                tier = ToolTier.fromMaterial(tool.getType());
            }
            if (tier != null) {
                speedMultiplier = tier.baseMultiplier();
            }

            // 2. Custom item YAML properties mining_speed
            try {
                var core = HaoHanItemCore.get();
                if (core != null && core.getItemService() != null) {
                    String customId = core.getItemService().getId(tool);
                    if (customId != null) {
                        ItemDefinition itemDef = core.getItemService().getDefinition(customId);
                        if (itemDef != null) {
                            Object speedProp = itemDef.getProperties().get("mining_speed");
                            if (speedProp instanceof Number num) {
                                speedMultiplier += num.floatValue();
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // 3. Paper AttributeModifier for Attribute.PLAYER_MINING_EFFICIENCY or Attribute.SUBMERGED_MINING_SPEED
            if (tool.hasItemMeta()) {
                try {
                    Attribute miningEffAttr = Attribute.valueOf("PLAYER_MINING_EFFICIENCY");
                    if (miningEffAttr != null) {
                        Collection<AttributeModifier> modifiers = tool.getItemMeta().getAttributeModifiers(miningEffAttr);
                        if (modifiers != null) {
                            for (AttributeModifier mod : modifiers) {
                                speedMultiplier += (float) mod.getAmount();
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 4. Efficiency enchantment bonus
            int efficiencyLevel = getEfficiencyLevel(tool);
            if (efficiencyLevel > 0) {
                speedMultiplier += (efficiencyLevel * efficiencyLevel + 1);
            }
        }

        // Environmental modifiers apply to both effective and non-effective tools (bù thêm thời gian đào)
        if (player != null) {
            try {
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    if (effect != null && effect.getType() != null) {
                        String name = effect.getType().getName();
                        if ("HASTE".equalsIgnoreCase(name) || "FAST_DIGGING".equalsIgnoreCase(name)) {
                            speedMultiplier *= (1.0f + 0.2f * (effect.getAmplifier() + 1));
                        } else if ("MINING_FATIGUE".equalsIgnoreCase(name) || "SLOW_DIGGING".equalsIgnoreCase(name)) {
                            speedMultiplier *= (float) Math.pow(0.3, Math.min(effect.getAmplifier() + 1, 4));
                        }
                    }
                }
            } catch (Throwable ignored) {}

            try {
                if (player.isInWater()) {
                    speedMultiplier /= 5.0f;
                }
            } catch (Throwable ignored) {}

            try {
                if (!player.isOnGround()) {
                    speedMultiplier /= 5.0f;
                }
            } catch (Throwable ignored) {}
        }

        return speedMultiplier / baseTicks;
    }

    private static int getEfficiencyLevel(ItemStack tool) {
        if (tool == null) return 0;
        try {
            for (Map.Entry<Enchantment, Integer> entry : tool.getEnchantments().entrySet()) {
                if (entry.getKey() != null && "efficiency".equals(entry.getKey().getKey().getKey())) {
                    return entry.getValue();
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }
}
