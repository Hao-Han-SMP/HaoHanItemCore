package vn.haohan.itemcore.internal.recipe;

import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.recipe.Ingredient;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Locale;

/**
 * Bộ kiểm tra và xác minh tính tương thích của nguyên liệu (Recipe Ingredient Matcher).
 * 
 * <p>Đảm bảo phân định minh bạch tuyệt đối giữa Custom Item và Vanilla Item:
 * <ul>
 *   <li>Nguyên liệu Custom Item chỉ chấp nhận đúng ItemStack chứa dữ liệu định danh của Custom Item đó.</li>
 *   <li>Nguyên liệu Vanilla Item chỉ chấp nhận ItemStack nguyên bản của Minecraft, từ chối mọi Custom Item (chống Carrier Leak).</li>
 * </ul>
 */
public final class RecipeIngredientMatcher {

    private final ItemService itemService;

    public RecipeIngredientMatcher(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Kiểm tra một ItemStack có phải là null hoặc khối không khí (Air) hay không.
     */
    public static boolean isAir(ItemStack item) {
        if (item == null) {
            return true;
        }
        Material mat = item.getType();
        return mat == null || mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR;
    }

    /**
     * Kiểm tra một ItemStack có khớp chính xác với Ingredient của công thức hay không.
     *
     * @param ingredient Ingredient yêu cầu từ công thức
     * @param item       ItemStack thực tế đặt vào slot
     * @return true nếu khớp chính xác
     */
    public boolean matches(Ingredient ingredient, ItemStack item) {
        if (ingredient == null || isAir(item)) {
            return false;
        }

        if (ingredient instanceof Ingredient.ItemIngredient itemIng) {
            if (itemIng.isVanilla() || itemIng.id().startsWith("minecraft:")) {
                return matchVanillaItem(item, itemIng.id());
            } else {
                return matchesCustomItem(item, itemIng.id());
            }
        }

        if (ingredient instanceof Ingredient.MaterialIngredient matIng) {
            return matchMaterial(item, matIng.material());
        }

        if (ingredient instanceof Ingredient.TagIngredient tagIng) {
            return matchTag(item, tagIng.tag());
        }

        return false;
    }

    /**
     * Kiểm tra item có phải là Vanilla Item của một ID cụ thể (ví dụ: "minecraft:paper" hoặc "paper").
     */
    public boolean matchVanillaItem(ItemStack item, String vanillaId) {
        if (isAir(item) || isCustomItem(item)) {
            return false;
        }

        String matName = vanillaId;
        if (matName.startsWith("minecraft:")) {
            matName = matName.substring("minecraft:".length());
        }
        matName = matName.toUpperCase(Locale.ROOT);

        return item.getType().name().equals(matName);
    }

    /**
     * Kiểm tra item có phải là Vanilla Item của một Material cụ thể.
     */
    public boolean matchMaterial(ItemStack item, Material material) {
        if (isAir(item) || isCustomItem(item)) {
            return false;
        }
        return item.getType() == material;
    }

    /**
     * Kiểm tra item có phải là Custom Item có ID cụ thể hay không.
     */
    public boolean matchesCustomItem(ItemStack item, String targetId) {
        if (item == null) {
            return false;
        }

        if (itemService != null && itemService.isItem(item, targetId)) {
            return true;
        }

        // Fallback kiểm tra trực tiếp qua PDC (có try-catch an toàn khi chạy headless test)
        try {
            if (!item.hasItemMeta()) {
                return false;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (NamespacedKey k : pdc.getKeys()) {
                if (k.getKey().equals("custom_item_id") || k.getKey().equals("item_id")) {
                    String val = pdc.get(k, PersistentDataType.STRING);
                    if (val != null) {
                        if (val.equalsIgnoreCase(targetId)) {
                            return true;
                        }
                        if (targetId.endsWith(":" + val) || val.endsWith(":" + targetId)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Kiểm tra xem một ItemStack có phải là bất kỳ Custom Item nào không.
     */
    public boolean isCustomItem(ItemStack item) {
        if (isAir(item)) {
            return false;
        }

        if (itemService != null && itemService.isCustomItem(item)) {
            return true;
        }

        try {
            if (!item.hasItemMeta()) {
                return false;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (NamespacedKey k : pdc.getKeys()) {
                if (k.getKey().equals("custom_item_id") || k.getKey().equals("item_id")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Kiểm tra xem một ItemStack có phải là Vanilla Item hợp lệ không.
     */
    public boolean isVanillaItem(ItemStack item) {
        return !isAir(item) && !isCustomItem(item);
    }

    /**
     * Kiểm tra trong danh sách / mảng ItemStacks có chứa bất kỳ Custom Item nào không.
     */
    public boolean containsCustomItem(ItemStack... items) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (isCustomItem(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra trong Collection ItemStacks có chứa bất kỳ Custom Item nào không.
     */
    public boolean containsCustomItem(Collection<ItemStack> items) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (isCustomItem(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchTag(ItemStack item, String tag) {
        if (isCustomItem(item) || isAir(item)) {
            return false;
        }
        // Basic tag matching support
        String tagName = tag.toLowerCase(Locale.ROOT);
        if (tagName.startsWith("#minecraft:")) {
            tagName = tagName.substring("#minecraft:".length());
        } else if (tagName.startsWith("#")) {
            tagName = tagName.substring(1);
        }

        String itemMat = item.getType().name().toLowerCase(Locale.ROOT);
        return itemMat.contains(tagName);
    }
}
