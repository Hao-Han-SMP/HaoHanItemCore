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

        float hardness = definition.getHardness();
        if (hardness <= 0.0f) {
            return 1.0f;
        }

        boolean isEffective = definition.isEffectiveTool(tool);
        if (!isEffective) {
            return 0.0f;
        }

        // Vanilla Minecraft damage per tick:
        // damage = speedMultiplier / (hardness * 30.0f) khi có thể harvest block.
        // Nếu block không yêu cầu preferred tool, thời gian đào = hardness * 20.0f (breakTimeSeconds)
        float baseTicks = definition.hasPreferredTools() ? (hardness * 30.0f) : (definition.getBreakTimeSeconds() * 20.0f);
        if (baseTicks <= 0.0f) {
            return 1.0f;
        }

        float speedMultiplier = 1.0f;

        // Tool multipliers (tier base speed, custom mining_speed, efficiency enchantments/attributes)
        // only apply if the block has preferred tools configured and tool is valid.
        if (definition.hasPreferredTools() && tool != null && tool.getType() != null && tool.getType() != Material.AIR) {
            // 1. Base Tool Speed: Custom mining_speed overrides or sets base multiplier; otherwise ToolTier
            Float customSpeed = null;
            try {
                var core = HaoHanItemCore.get();
                if (core != null && core.getItemService() != null) {
                    String customId = core.getItemService().getId(tool);
                    if (customId != null) {
                        ItemDefinition itemDef = core.getItemService().getDefinition(customId);
                        if (itemDef != null) {
                            Object speedProp = itemDef.getProperties().get("mining_speed");
                            if (speedProp instanceof Number num) {
                                customSpeed = num.floatValue();
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            if (customSpeed != null && customSpeed > 0.0f) {
                speedMultiplier = customSpeed;
            } else {
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
            }

            // 2. Efficiency enchantment bonus: level * level + 1 (Vanilla Minecraft)
            int efficiencyLevel = getEfficiencyLevel(tool);
            if (efficiencyLevel > 0) {
                speedMultiplier += (efficiencyLevel * efficiencyLevel + 1);
            }

            // 3. Mining Efficiency Attribute bonus (nếu có modifier bổ sung trên tool hoặc player)
            try {
                Attribute miningEffAttr = Attribute.valueOf("PLAYER_MINING_EFFICIENCY");
                if (miningEffAttr != null) {
                    if (player != null && player.getAttribute(miningEffAttr) != null) {
                        double val = player.getAttribute(miningEffAttr).getValue();
                        if (val > 0.0) {
                            speedMultiplier += (float) val;
                        }
                    } else if (tool.hasItemMeta()) {
                        Collection<AttributeModifier> modifiers = tool.getItemMeta().getAttributeModifiers(miningEffAttr);
                        if (modifiers != null) {
                            for (AttributeModifier mod : modifiers) {
                                speedMultiplier += (float) mod.getAmount();
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Environmental & Player Attribute modifiers
        if (player != null) {
            // Player block break speed attribute multiplier (Paper 1.21: player.block_break_speed, default 1.0)
            try {
                Attribute breakSpeedAttr = Attribute.valueOf("PLAYER_BLOCK_BREAK_SPEED");
                if (breakSpeedAttr != null && player.getAttribute(breakSpeedAttr) != null) {
                    double breakSpeed = player.getAttribute(breakSpeedAttr).getValue();
                    if (breakSpeed > 0.0) {
                        speedMultiplier *= (float) breakSpeed;
                    }
                }
            } catch (Throwable ignored) {}

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
