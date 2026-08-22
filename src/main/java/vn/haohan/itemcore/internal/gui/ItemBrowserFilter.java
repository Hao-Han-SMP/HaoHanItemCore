package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.item.ItemCategory;
import vn.haohan.itemcore.api.item.ItemDefinition;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Đối tượng đóng gói trạng thái lọc cho Item Browser GUI.
 * Hỗ trợ lọc đồng thời theo:
 * 1. Từ khóa tìm kiếm (search query)
 * 2. Plugin / Namespace nguồn
 * 3. Phân loại danh mục (ItemCategory: Tools, Weapons, Armor, Custom Block...)
 */
public final class ItemBrowserFilter {

    public static final String ALL_NAMESPACES = "ALL";
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)[§&][0-9a-fk-or]");

    private final String query;
    private final String namespace;
    private final ItemCategory category;

    public ItemBrowserFilter(String query, String namespace, ItemCategory category) {
        this.query = (query != null && !query.isBlank()) ? query.trim() : null;
        this.namespace = (namespace != null && !namespace.equalsIgnoreCase(ALL_NAMESPACES) && !namespace.isBlank())
                ? namespace.trim().toLowerCase(Locale.ROOT)
                : null;
        this.category = (category != null) ? category : ItemCategory.ALL;
    }

    public static ItemBrowserFilter empty() {
        return new ItemBrowserFilter(null, null, ItemCategory.ALL);
    }

    public String getQuery() {
        return query;
    }

    public String getNamespace() {
        return namespace;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public boolean isDefault() {
        return query == null && namespace == null && category == ItemCategory.ALL;
    }

    public ItemBrowserFilter withQuery(String newQuery) {
        return new ItemBrowserFilter(newQuery, this.namespace, this.category);
    }

    public ItemBrowserFilter withNamespace(String newNamespace) {
        return new ItemBrowserFilter(this.query, newNamespace, this.category);
    }

    public ItemBrowserFilter withCategory(ItemCategory newCategory) {
        return new ItemBrowserFilter(this.query, this.namespace, newCategory);
    }

    /**
     * Kiểm tra ItemDefinition có thỏa mãn toàn bộ tiêu chí lọc hiện tại không.
     */
    public boolean matches(ItemDefinition def) {
        if (def == null) return false;

        // 1. Kiểm tra Namespace / Plugin
        if (namespace != null && !namespace.equalsIgnoreCase(ALL_NAMESPACES)) {
            String itemNamespace = def.getNamespace().toLowerCase(Locale.ROOT);
            if (!itemNamespace.equals(namespace)) {
                return false;
            }
        }

        // 2. Kiểm tra Category
        if (category != null && category != ItemCategory.ALL) {
            if (!category.matches(def)) {
                return false;
            }
        }

        // 3. Kiểm tra Search Query
        if (query != null && !query.isBlank()) {
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            return matchesKeyword(def, lowerQuery);
        }

        return true;
    }

    private boolean matchesKeyword(ItemDefinition def, String lowerQuery) {
        if (def.getId().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            return true;
        }
        if (def.getDisplayName() != null) {
            String plainName = stripColor(def.getDisplayName()).toLowerCase(Locale.ROOT);
            if (plainName.contains(lowerQuery)) {
                return true;
            }
        }
        if (def.getLore() != null) {
            for (String line : def.getLore()) {
                String plainLore = stripColor(line).toLowerCase(Locale.ROOT);
                if (plainLore.contains(lowerQuery)) {
                    return true;
                }
            }
        }
        if (def.getMaterial() != null && def.getMaterial().name().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            return true;
        }
        if (def.getProperties() != null) {
            for (Map.Entry<String, Object> entry : def.getProperties().entrySet()) {
                if (entry.getKey().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    return true;
                }
                if (entry.getValue() != null && String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String stripColor(String input) {
        return input == null ? "" : STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemBrowserFilter that)) return false;
        return Objects.equals(query, that.query) &&
               Objects.equals(namespace, that.namespace) &&
               category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, namespace, category);
    }
}
