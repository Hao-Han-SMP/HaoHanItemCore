package vn.haohan.itemcore.api.item;

import org.bukkit.inventory.ItemStack;

/**
 * Factory chuyển ItemDefinition thành ItemStack.
 * Chịu trách nhiệm tạo ItemStack với đầy đủ metadata.
 */
public interface ItemFactory {

    /**
     * Tạo ItemStack từ item ID với amount = 1.
     * @throws IllegalArgumentException nếu item không tồn tại
     */
    ItemStack create(String id);

    /**
     * Tạo ItemStack từ item ID với amount chỉ định.
     * @throws IllegalArgumentException nếu item không tồn tại
     */
    ItemStack create(String id, int amount);

    /**
     * Tạo ItemStack từ ItemDefinition với amount = 1.
     */
    ItemStack create(ItemDefinition definition);

    /**
     * Tạo ItemStack từ ItemDefinition với amount chỉ định.
     */
    ItemStack create(ItemDefinition definition, int amount);
}
