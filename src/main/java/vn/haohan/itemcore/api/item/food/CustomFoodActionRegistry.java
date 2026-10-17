package vn.haohan.itemcore.api.item.food;

/**
 * Registry managing custom food action handlers.
 */
public interface CustomFoodActionRegistry {
    void register(String id, CustomFoodAction action);
    CustomFoodAction get(String id);
    boolean has(String id);
    void unregister(String id);
}
