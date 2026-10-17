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
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolType;

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
     * Estimates the vanilla Minecraft client-side break time (in seconds) for a NoteBlock
     * when mined with the given tool (without potion effects or enchants).
     *
     * <p>Vanilla NoteBlock properties:
     * - Hardness: 0.8
     * - Material: Wood (preferred tool is Axe)
     * - Break time with effective tool (Axe): hardness * 1.5 / toolMultiplier = 1.2 / toolMultiplier
     * - Break time without preferred tool (Hand, Pickaxe, Shovel, Sword, Hoe): hardness * 5.0 = 4.0 seconds</p>
     *
     * @param tool the tool held by the player
     * @return client-side NoteBlock break time in seconds
     */
    public static float getClientNoteBlockBreakTime(ItemStack tool) {
        if (tool == null || tool.getType() == null || tool.getType().isAir()) {
            return 4.0f; // Hand: 0.8 * 5.0 = 4.0s
        }

        ToolType toolType = ToolType.fromMaterial(tool.getType());
        if (toolType == ToolType.AXE) {
            ToolTier tier = ToolTier.fromMaterial(tool.getType());
            float multiplier = (tier != null) ? tier.baseMultiplier() : 1.0f;
            return 1.2f / Math.max(1.0f, multiplier);
        }

        // Sword deals 1.5x damage on web/bamboo, but on NoteBlock (wood) swords have multiplier 1.5 -> hardness * 5.0 / 1.5 = 2.67s
        if (toolType == ToolType.SWORD) {
            return 4.0f / 1.5f;
        }

        // Non-axe tools (Pickaxe, Shovel, Hoe): Not preferred for NoteBlock -> hardness * 5.0 = 4.0s
        return 4.0f;
    }

    /**
     * Calculates attribute multiplier factor from client NoteBlock break time vs target hardness (in seconds).
     *
     * <p>Formula: {@code factor = clientNoteBlockBreakTime / targetHardness}.
     * When applied as {@code BLOCK_BREAK_SPEED} modifier, the actual break time becomes:
     * {@code actualBreakTime = clientNoteBlockBreakTime / factor = targetHardness}.</p>
     *
     * @param clientNoteBlockBreakTime client base break time for NoteBlock with current tool (in seconds)
     * @param targetHardness custom block target hardness in seconds
     * @return speed multiplier factor
     */
    public static double calculateAttributeFactor(float clientNoteBlockBreakTime, float targetHardness) {
        return calculateAttributeFactor(clientNoteBlockBreakTime, targetHardness, 1.0f, false);
    }

    /**
     * Calculates attribute multiplier factor incorporating the target block's speed multiplier
     * (tool tier, custom mining speed, efficiency enchantment, potion effects, etc.).
     *
     * @param clientNoteBlockBreakTime client base break time for NoteBlock with current tool (in seconds)
     * @param targetHardness custom block target hardness in seconds
     * @param targetSpeedMultiplier effective speed multiplier for the target block
     * @param hasPreferredTools whether the target custom block requires preferred tools
     * @return speed multiplier factor
     */
    public static double calculateAttributeFactor(float clientNoteBlockBreakTime, float targetHardness, float targetSpeedMultiplier, boolean hasPreferredTools) {
        if (targetHardness < 0.0f) {
            return 0.01; // unbreakable block, heavy reduction factor
        }
        if (targetHardness == 0.0f) {
            return 100.0; // instant break
        }
        double baseFactor = (double) clientNoteBlockBreakTime / (double) targetHardness;
        if (!hasPreferredTools) {
            return baseFactor;
        }
        float speed = Math.max(1.0f, targetSpeedMultiplier);
        // Preferred tools in vanilla have a 1.5x multiplier on hardness instead of 5.0x
        return baseFactor * (double) (speed / 1.5f);
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
            // Penalize invalid tool or unbreakable block by 100% (speed factor 0.0)
            attr.addTransientModifier(new AttributeModifier(modifierKey, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            return;
        }

        float clientBreakTime = getClientNoteBlockBreakTime(tool);
        float targetHardness = def.getHardness();
        float speedMultiplier = 1.0f;
        boolean hasPreferred = def.hasPreferredTools();
        if (hasPreferred) {
            speedMultiplier = vn.haohan.itemcore.internal.block.MiningSpeedCalculator.calculateSpeedMultiplier(player, tool, def);
        }
        double factor = calculateAttributeFactor(clientBreakTime, targetHardness, speedMultiplier, hasPreferred);

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
