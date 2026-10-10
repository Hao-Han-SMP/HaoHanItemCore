package vn.haohan.itemcore.api.block;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry for custom block action handlers, enabling decoupling between
 * YAML configurations and compiled Java logic.
 */
public final class CustomBlockActionRegistry {

    private static final Map<String, CustomBlockInteractHandler> INTERACT_ACTIONS = new ConcurrentHashMap<>();
    private static final Map<String, CustomBlockStepHandler> STEP_ACTIONS = new ConcurrentHashMap<>();

    private CustomBlockActionRegistry() {}

    public static void registerInteractAction(String id, CustomBlockInteractHandler handler) {
        if (id != null && handler != null) {
            INTERACT_ACTIONS.put(id, handler);
        }
    }

    public static Optional<CustomBlockInteractHandler> getInteractAction(String id) {
        return id != null ? Optional.ofNullable(INTERACT_ACTIONS.get(id)) : Optional.empty();
    }

    public static void registerStepAction(String id, CustomBlockStepHandler handler) {
        if (id != null && handler != null) {
            STEP_ACTIONS.put(id, handler);
        }
    }

    public static Optional<CustomBlockStepHandler> getStepAction(String id) {
        return id != null ? Optional.ofNullable(STEP_ACTIONS.get(id)) : Optional.empty();
    }

    public static void clear() {
        INTERACT_ACTIONS.clear();
        STEP_ACTIONS.clear();
    }
}
