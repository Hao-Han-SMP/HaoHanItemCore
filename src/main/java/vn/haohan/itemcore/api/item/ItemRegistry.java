package vn.haohan.itemcore.api.item;

import java.util.Collection;
import java.util.List;

/**
 * Registry chịu trách nhiệm lưu trữ và tra cứu ItemDefinition.
 * Là source of truth duy nhất cho tất cả custom items.
 */
public interface ItemRegistry {

    /**
     * Đăng ký một ItemDefinition mới.
     * @throws IllegalArgumentException nếu ID đã tồn tại hoặc không hợp lệ
     */
    void register(ItemDefinition definition);

    /**
     * Lấy ItemDefinition theo ID, trả về null nếu không tìm thấy.
     */
    ItemDefinition get(String id);

    /**
     * Lấy ItemDefinition theo ID, throw exception nếu không tìm thấy.
     * @throws IllegalArgumentException nếu item không tồn tại
     */
    ItemDefinition require(String id);

    /**
     * Kiểm tra item có tồn tại trong registry không.
     */
    boolean exists(String id);

    /**
     * Xóa item khỏi registry.
     */
    void unregister(String id);

    /**
     * Lấy tất cả ItemDefinition đã đăng ký.
     */
    Collection<ItemDefinition> all();

    /**
     * Lấy tất cả ItemDefinition thuộc một namespace.
     */
    List<ItemDefinition> getByNamespace(String namespace);

    /**
     * Tìm kiếm item theo keyword (trong ID hoặc display name).
     */
    List<ItemDefinition> search(String keyword);

    /**
     * Lấy tổng số item đã đăng ký.
     */
    int size();

    /**
     * Xóa tất cả items.
     */
    void clear();
}
