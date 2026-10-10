package vn.haohan.itemcore.api.block;

import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.item.ItemDefinition;

import java.util.Map;
import java.util.Optional;

/**
 * Contextual data when a player mines a custom block.
 */
public record MiningContext(
        Player player,
        Block block,
        CustomBlockDefinition definition,
        ItemStack tool,
        Boolean silkTouchOverride,
        Integer fortuneOverride,
        Integer efficiencyOverride
) {
    public MiningContext(Player player, Block block, CustomBlockDefinition definition, ItemStack tool) {
        this(player, block, definition, tool, null, null, null);
    }

    public boolean hasEnchantment(Enchantment enchantment) {
        if (tool == null || enchantment == null) return false;
        try {
            return tool.getEnchantmentLevel(enchantment) > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public int getEnchantmentLevel(Enchantment enchantment) {
        if (tool == null || enchantment == null) return 0;
        try {
            return tool.getEnchantmentLevel(enchantment);
        } catch (Throwable t) {
            return 0;
        }
    }

    public boolean hasSilkTouch() {
        if (silkTouchOverride != null) {
            return silkTouchOverride;
        }
        if (tool == null) return false;
        try {
            for (Map.Entry<Enchantment, Integer> entry : tool.getEnchantments().entrySet()) {
                if (entry.getKey() != null && "silk_touch".equals(entry.getKey().getKey().getKey())) {
                    return entry.getValue() > 0;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public int getFortuneLevel() {
        if (fortuneOverride != null) {
            return fortuneOverride;
        }
        if (tool == null) return 0;
        try {
            for (Map.Entry<Enchantment, Integer> entry : tool.getEnchantments().entrySet()) {
                if (entry.getKey() != null && "fortune".equals(entry.getKey().getKey().getKey())) {
                    return entry.getValue();
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public int getEfficiencyLevel() {
        if (efficiencyOverride != null) {
            return efficiencyOverride;
        }
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

    public boolean hasCustomItem() {
        return getCustomItemDefinition().isPresent();
    }

    public Optional<ItemDefinition> getCustomItemDefinition() {
        if (tool == null || !tool.hasItemMeta()) return Optional.empty();
        var core = HaoHanItemCore.get();
        if (core == null || core.getItemService() == null) return Optional.empty();
        String id = core.getItemService().getId(tool);
        if (id == null) return Optional.empty();
        return Optional.ofNullable(core.getItemService().getDefinition(id));
    }
}
