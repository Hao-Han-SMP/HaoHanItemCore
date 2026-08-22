package vn.haohan.itemcore.internal.item;

import vn.haohan.itemcore.api.item.*;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Default implementation của ItemService.
 * Facade gộp ItemRegistry + ItemFactory.
 */
public final class DefaultItemService implements ItemService {

    private final ItemRegistry registry;
    private final ItemFactory factory;
    private final NamespacedKey itemIdKey;
    private final Plugin plugin;
    private final ItemInstanceData instanceData;

    public DefaultItemService(ItemRegistry registry, ItemFactory factory, Plugin plugin) {
        this.registry = registry;
        this.factory = factory;
        this.itemIdKey = new NamespacedKey(plugin, DefaultItemFactory.ITEM_ID_KEY_NAME);
        this.plugin = plugin;
        this.instanceData = new ItemInstanceData(plugin);
    }

    @Override
    public ItemStack create(String id) {
        return factory.create(id);
    }

    @Override
    public ItemStack create(String id, int amount) {
        return factory.create(id, amount);
    }

    @Override
    public boolean isItem(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta())
            return false;
        String itemId = getId(item);
        return id.equals(itemId);
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        return getId(item) != null;
    }

    @Override
    public String getId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(itemIdKey, PersistentDataType.STRING)) {
            return pdc.get(itemIdKey, PersistentDataType.STRING);
        }
        return null;
    }

    @Override
    public ItemDefinition getDefinition(String id) {
        return registry.require(id);
    }

    @Override
    public boolean exists(String id) {
        return registry.exists(id);
    }

    @Override
    public Map<String, Object> getProperties(ItemStack item) {
        String id = getId(item);
        if (id == null)
            return Map.of();

        ItemDefinition def = registry.get(id);
        if (def == null)
            return Map.of();

        return def.getProperties();
    }

    @Override
    public ItemInstanceData getInstanceData() {
        return instanceData;
    }

    @Override
    public ItemStack validateAndUpdate(ItemStack item) {
        if (item == null || item.getType().isAir())
            return item;

        String id = getId(item);
        if (id == null)
            return item;

        ItemDefinition definition = registry.get(id);
        if (definition == null)
            return item;

        item = DefaultItemFactory.applyComponents(item, definition, plugin);
        return item;
    }
}
