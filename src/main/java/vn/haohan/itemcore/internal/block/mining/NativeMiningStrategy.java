package vn.haohan.itemcore.internal.block.mining;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;

/**
 * Strategy for Native custom blocks (backed by physical NoteBlock states).
 *
 * <p>Uses Paper 1.21's {@code Attribute.BLOCK_BREAK_SPEED} attribute modifier
 * to adjust client and server mining rate to match custom hardness relative
 * to vanilla NoteBlock base hardness (0.8s).</p>
 */
public final class NativeMiningStrategy implements MiningSpeedStrategy {

    private static final Attribute BLOCK_BREAK_SPEED_ATTR = resolveBlockBreakSpeedAttribute();

    private final Plugin plugin;
    private final NamespacedKey modifierKey;

    public NativeMiningStrategy(Plugin plugin) {
        this.plugin = plugin;
        this.modifierKey = createModifierKey(plugin);
    }

    private static NamespacedKey createModifierKey(Plugin plugin) {
        if (plugin != null) {
            try {
                return new NamespacedKey(plugin, "custom_block_speed");
            } catch (Throwable t) {
                String ns = "haohanitemcore";
                try {
                    String name = plugin.getName();
                    if (name != null && !name.isBlank()) {
                        ns = name.toLowerCase(java.util.Locale.ROOT);
                    }
                } catch (Throwable ignored) {}
                return new NamespacedKey(ns, "custom_block_speed");
            }
        }
        return new NamespacedKey("haohanitemcore", "custom_block_speed");
    }

    public NativeMiningStrategy() {
        this(null);
    }

    public NamespacedKey getModifierKey() {
        return modifierKey;
    }

    /**
     * Calculates attribute multiplier factor from target hardness (in seconds)
     * vs vanilla NoteBlock hardness (0.8 seconds).
     *
     * @param vanillaHardness base vanilla block hardness (default 0.8)
     * @param targetHardness custom block hardness in seconds
     * @return speed multiplier factor
     */
    public static double calculateAttributeFactor(float vanillaHardness, float targetHardness) {
        if (targetHardness < 0.0f) {
            return 0.01; // unbreakable block, heavy reduction factor
        }
        if (targetHardness == 0.0f) {
            return 100.0; // instant break
        }
        return (double) vanillaHardness / (double) targetHardness;
    }

    @Override
    public void onStartDigging(Player player, Block block, CustomBlockDefinition def, ItemStack tool) {
        if (player == null || def == null || BLOCK_BREAK_SPEED_ATTR == null) {
            return;
        }
        AttributeInstance attr = player.getAttribute(BLOCK_BREAK_SPEED_ATTR);
        if (attr == null) {
            return;
        }

        // Clean previous modifier if any
        attr.removeModifier(modifierKey);

        if (def.isUnbreakable() || !def.isEffectiveTool(tool)) {
            // Penalize invalid tool or unbreakable block to match vanilla slow-break or unbreakable
            attr.addTransientModifier(new AttributeModifier(modifierKey, -0.99, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            return;
        }

        float vanillaHardness = 0.8f; // NoteBlock default hardness
        float targetHardness = def.getHardness();
        double factor = calculateAttributeFactor(vanillaHardness, targetHardness);

        // AttributeModifier with MULTIPLY_SCALAR_1: value 1.0 = normal speed, factor - 1.0 adjusts accordingly
        attr.addTransientModifier(new AttributeModifier(modifierKey, factor - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    @Override
    public void onStopDigging(Player player, Block block, CustomBlockDefinition def) {
        if (player == null || BLOCK_BREAK_SPEED_ATTR == null) {
            return;
        }
        AttributeInstance attr = player.getAttribute(BLOCK_BREAK_SPEED_ATTR);
        if (attr != null) {
            attr.removeModifier(modifierKey);
        }
    }

    /**
     * Public cleanup hook to remove the custom mining speed modifier from the player.
     *
     * @param player the player to clean up
     */
    public void removeModifier(Player player) {
        onStopDigging(player, null, null);
    }

    @Override
    public float getBreakTimeSeconds(Player player, ItemStack tool, CustomBlockDefinition def) {
        if (def == null) {
            return 0.8f;
        }
        if (def.isUnbreakable() || def.getHardness() < 0.0f) {
            return -1.0f;
        }
        return def.getBreakTimeSeconds();
    }

    @SuppressWarnings("removal")
    private static Attribute resolveBlockBreakSpeedAttribute() {
        try {
            return Attribute.BLOCK_BREAK_SPEED;
        } catch (Throwable t1) {
            try {
                return Attribute.valueOf("PLAYER_BLOCK_BREAK_SPEED");
            } catch (Throwable t2) {
                try {
                    return org.bukkit.Registry.ATTRIBUTE.get(NamespacedKey.minecraft("player.block_break_speed"));
                } catch (Throwable t3) {
                    return null;
                }
            }
        }
    }
}
