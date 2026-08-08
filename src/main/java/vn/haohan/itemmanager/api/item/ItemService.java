package vn.haohan.itemmanager.api.item;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Facade đơn giản cho plugin khác sử dụng.
 * Gộp chức năng từ ItemRegistry và ItemFactory.
 * 
 * <p>Ví dụ:
 * <pre>
 * // Tạo item
 * ItemStack item = HaoHanItemManager.get().getItemService().create("magic:fire_crystal", 4);
 * 
 * // Kiểm tra item
 * boolean isCrystal = HaoHanItemManager.get().getItemService().isItem(item, "magic:fire_crystal");
 * 
 * // Lấy ID
 * String id = HaoHanItemManager.get().getItemService().getId(item);
 * </pre>
 */
public interface ItemService {

    /**
     * Tạo ItemStack từ item ID.
     */
    ItemStack create(String id);

    /**
     * Tạo ItemStack từ item ID với amount.
     */
    ItemStack create(String id, int amount);

    /**
     * Kiểm tra ItemStack có phải là custom item với ID chỉ định không.
     */
    boolean isItem(ItemStack item, String id);

    /**
     * Kiểm tra ItemStack có phải là bất kỳ custom item nào không.
     */
    boolean isCustomItem(ItemStack item);

    /**
     * Lấy ID của custom item từ ItemStack. Trả về null nếu không phải custom item.
     */
    String getId(ItemStack item);

    /**
     * Lấy ItemDefinition từ ID.
     */
    ItemDefinition getDefinition(String id);

    /**
     * Kiểm tra item có tồn tại không.
     */
    boolean exists(String id);

    /**
     * Lấy properties của custom item từ ItemStack.
     */
    Map<String, Object> getProperties(ItemStack item);

    /**
     * Kiểm tra và cập nhật các custom component của custom item (nếu bị thiếu hoặc sai lệch).
     * Trả về ItemStack đã được cập nhật (hoặc chính nó nếu không thay đổi).
     */
    ItemStack validateAndUpdate(ItemStack item);
}
