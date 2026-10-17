package vn.haohan.itemcore.internal.item.food;

import vn.haohan.itemcore.api.item.food.CustomFoodAction;
import vn.haohan.itemcore.api.item.food.CustomFoodActionRegistry;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultCustomFoodActionRegistry implements CustomFoodActionRegistry {

    private final Map<String, CustomFoodAction> actions = new ConcurrentHashMap<>();

    @Override
    public void register(String id, CustomFoodAction action) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Action ID cannot be null or blank");
        }
        Objects.requireNonNull(action, "CustomFoodAction cannot be null");
        actions.put(id, action);
    }

    @Override
    public CustomFoodAction get(String id) {
        if (id == null) return null;
        return actions.get(id);
    }

    @Override
    public boolean has(String id) {
        if (id == null) return false;
        return actions.containsKey(id);
    }

    @Override
    public void unregister(String id) {
        if (id != null) {
            actions.remove(id);
        }
    }
}
