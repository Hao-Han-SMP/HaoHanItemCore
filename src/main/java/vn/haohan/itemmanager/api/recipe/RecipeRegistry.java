package vn.haohan.itemmanager.api.recipe;

import java.util.Collection;
import java.util.List;

/**
 * Registry quản lý tất cả RecipeDefinition.
 */
public interface RecipeRegistry {

    /**
     * Đăng ký một recipe mới.
     * @throws IllegalArgumentException nếu ID đã tồn tại hoặc không hợp lệ
     */
    void register(RecipeDefinition recipe);

    /**
     * Lấy recipe theo ID, trả về null nếu không tìm thấy.
     */
    RecipeDefinition get(String id);

    /**
     * Lấy recipe theo ID, throw exception nếu không tìm thấy.
     */
    RecipeDefinition require(String id);

    /**
     * Kiểm tra recipe có tồn tại không.
     */
    boolean exists(String id);

    /**
     * Xóa recipe khỏi registry.
     */
    void unregister(String id);

    /**
     * Lấy tất cả recipe.
     */
    Collection<RecipeDefinition> all();

    /**
     * Lấy tổng số recipe.
     */
    int size();

    /**
     * Xóa tất cả recipes.
     */
    void clear();
}
