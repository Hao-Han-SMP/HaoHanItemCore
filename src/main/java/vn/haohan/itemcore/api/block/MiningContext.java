package vn.haohan.itemcore.api.block;

import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.item.ItemDefinition;

import java.util.Optional;

/**
 * Contextual data when a player mines a custom block.
 */
public record MiningContext(
        Player player,
        Block block,
        CustomBlockDefinition definition,
        ItemStack tool
) {
    public boolean hasEnchantment(Enchantment enchantment) {
        if (tool == null || enchantment == null) return false;
        return tool.getEnchantmentLevel(enchantment) > 0;
    }

    public int getEnchantmentLevel(Enchantment enchantment) {
        if (tool == null || enchantment == null) return 0;
        return tool.getEnchantmentLevel(enchantment);
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
