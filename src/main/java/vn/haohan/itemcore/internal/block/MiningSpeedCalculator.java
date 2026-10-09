package vn.haohan.itemcore.internal.block;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.api.item.ItemDefinition;

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

        float speedMultiplier = 1.0f;

        if (tool != null && tool.getType() != Material.AIR) {
            ToolType requiredType = (definition != null) ? definition.getToolType() : null;
            ToolType currentToolType = ToolType.fromMaterial(tool.getType());
            boolean isEffectiveTool = (requiredType == null || requiredType == ToolType.ANY || requiredType == currentToolType);

            if (isEffectiveTool) {
                ToolTier tier = ToolTier.fromMaterial(tool.getType());
                if (tier != null) {
                    speedMultiplier = switch (tier) {
                        case WOOD -> 2.0f;
                        case STONE -> 4.0f;
                        case IRON -> 6.0f;
                        case DIAMOND -> 8.0f;
                        case NETHERITE -> 9.0f;
                        case GOLD -> 12.0f;
                    };
                }

                // Custom item speed property support
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
                } catch (IllegalStateException ignored) {}

                // Efficiency enchantment
                int efficiencyLevel = getEfficiencyLevel(tool);
                if (efficiencyLevel > 0) {
                    speedMultiplier += (efficiencyLevel * efficiencyLevel + 1);
                }
            }
        }

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
