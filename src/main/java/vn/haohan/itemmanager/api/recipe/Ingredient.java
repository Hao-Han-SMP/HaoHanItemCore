package vn.haohan.itemmanager.api.recipe;

import org.bukkit.Material;

/**
 * Ingredient là nguyên liệu đầu vào của recipe.
 * Hỗ trợ 3 loại: Custom Item, Vanilla Material, và Tag.
 * 
 * <p>Sealed interface chỉ cho phép các implementation bên trong.
 */
public sealed interface Ingredient permits Ingredient.ItemIngredient, Ingredient.MaterialIngredient, Ingredient.TagIngredient {

    /**
     * Số lượng nguyên liệu cần thiết.
     */
    int amount();

    /**
     * Ingredient sử dụng custom item hoặc vanilla item bằng namespaced ID.
     * 
     * <p>Ví dụ:
     * <pre>
     * new Ingredient.ItemIngredient("magic:fire_crystal", 2)
     * new Ingredient.ItemIngredient("minecraft:iron_ingot", 8)
     * </pre>
     * 
     * @param id Namespaced item ID
     * @param amount Số lượng cần thiết
     */
    record ItemIngredient(String id, int amount) implements Ingredient {
        public ItemIngredient {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("Item ID cannot be null or empty");
            }
            if (amount < 1) {
                throw new IllegalArgumentException("Amount must be at least 1");
            }
        }

        public ItemIngredient(String id) {
            this(id, 1);
        }

        /**
         * Kiểm tra ingredient này có phải vanilla item không.
         */
        public boolean isVanilla() {
            return id.startsWith("minecraft:");
        }
    }

    /**
     * Ingredient sử dụng Bukkit Material trực tiếp.
     * 
     * @param material Bukkit Material
     * @param amount Số lượng cần thiết
     */
    record MaterialIngredient(Material material, int amount) implements Ingredient {
        public MaterialIngredient {
            if (material == null) {
                throw new IllegalArgumentException("Material cannot be null");
            }
            if (amount < 1) {
                throw new IllegalArgumentException("Amount must be at least 1");
            }
        }

        public MaterialIngredient(Material material) {
            this(material, 1);
        }
    }

    /**
     * Ingredient sử dụng tag, chấp nhận bất kỳ item nào thuộc tag.
     * 
     * @param tag Tag ID
     * @param amount Số lượng cần thiết
     */
    record TagIngredient(String tag, int amount) implements Ingredient {
        public TagIngredient {
            if (tag == null || tag.isEmpty()) {
                throw new IllegalArgumentException("Tag cannot be null or empty");
            }
            if (amount < 1) {
                throw new IllegalArgumentException("Amount must be at least 1");
            }
        }

        public TagIngredient(String tag) {
            this(tag, 1);
        }
    }
}
