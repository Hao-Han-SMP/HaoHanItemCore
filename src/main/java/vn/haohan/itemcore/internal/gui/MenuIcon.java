package vn.haohan.itemcore.internal.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Shared menu icons backed by minecraft:paper custom model data. */
final class MenuIcon {
    static final int PREVIOUS_ACTIVE = 900001;
    static final int PREVIOUS_DISABLED = 900002;
    static final int NEXT_ACTIVE = 900003;
    static final int NEXT_DISABLED = 900004;
    static final int RECIPE_ARROW = 900005;
    static final int CLOSE = 900006;
    static final int INFO = 900007;
    static final int BACK = 900010;
    static final int SEARCH = 900012;
    static final int FILTER = 900013;
    static final int RESET = 900014;
    static final int PLUS = 900015;
    static final int HAMMER = 900016;

    private MenuIcon() {}

    @SuppressWarnings("deprecation")
    static ItemStack create(int customModelData, Component displayName) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            if (displayName != null) {
                if (!displayName.hasDecoration(TextDecoration.ITALIC)) {
                    displayName = displayName.decoration(TextDecoration.ITALIC, false);
                }
                meta.displayName(displayName);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
