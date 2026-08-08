package vn.haohan.itemmanager.internal.item;

import vn.haohan.itemmanager.api.item.ItemDefinition;
import vn.haohan.itemmanager.api.item.ItemRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Default implementation của ItemRegistry.
 * Thread-safe, sử dụng ConcurrentHashMap.
 */
public final class DefaultItemRegistry implements ItemRegistry {

    private final Map<String, ItemDefinition> items = new ConcurrentHashMap<>();
    private final Logger logger;

    public DefaultItemRegistry(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void register(ItemDefinition definition) {
        Objects.requireNonNull(definition, "ItemDefinition cannot be null");

        String id = definition.getId();

        if (!ItemDefinition.isValidId(id)) {
            throw new IllegalArgumentException(
                "Invalid item ID: '" + id + "'. Must be in format 'namespace:key'."
            );
        }

        if (items.containsKey(id)) {
            throw new IllegalArgumentException(
                "Item already registered: '" + id + "'"
            );
        }

        items.put(id, definition);
        logger.info("[ItemRegistry] Registered: " + id +
                " (Material: " + definition.getMaterial() + ", Type: " + definition.getType() + ")");
    }

    @Override
    public ItemDefinition get(String id) {
        return items.get(id);
    }

    @Override
    public ItemDefinition require(String id) {
        ItemDefinition def = items.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Item not found: '" + id + "'");
        }
        return def;
    }

    @Override
    public boolean exists(String id) {
        return items.containsKey(id);
    }

    @Override
    public void unregister(String id) {
        ItemDefinition removed = items.remove(id);
        if (removed != null) {
            logger.info("[ItemRegistry] Unregistered: " + id);
        }
    }

    @Override
    public Collection<ItemDefinition> all() {
        return Collections.unmodifiableCollection(items.values());
    }

    @Override
    public List<ItemDefinition> getByNamespace(String namespace) {
        return items.values().stream()
                .filter(def -> def.getNamespace().equals(namespace))
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDefinition> search(String keyword) {
        String lower = keyword.toLowerCase();
        return items.values().stream()
                .filter(def ->
                    def.getId().toLowerCase().contains(lower) ||
                    def.getDisplayName().toLowerCase().contains(lower)
                )
                .collect(Collectors.toList());
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public void clear() {
        items.clear();
        logger.info("[ItemRegistry] Cleared all items.");
    }
}
