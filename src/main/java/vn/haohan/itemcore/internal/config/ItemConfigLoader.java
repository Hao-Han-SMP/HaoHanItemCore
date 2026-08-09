package vn.haohan.itemcore.internal.config;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemType;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Load item definitions từ YAML files trong thư mục items/.
 * 
 * <p>Format:
 * <pre>
 * namespace:
 *   item_key:
 *     material: EMERALD
 *     display-name: "§cFire Crystal"
 *     lore:
 *       - "§7A crystal containing"
 *       - "§7unstable fire energy."
 *     custom-model-data: 1001
 *     max-stack-size: 16
 *     type: MATERIAL
 *     properties:
 *       element: fire
 * </pre>
 */
public final class ItemConfigLoader {

    private final Logger logger;

    public ItemConfigLoader(Logger logger) {
        this.logger = logger;
    }

    /**
     * Load tất cả item files từ thư mục items/.
     * @return Số lượng items đã load thành công.
     */
    public int loadAll(File itemsDir, ItemRegistry registry) {
        if (!itemsDir.exists() || !itemsDir.isDirectory()) {
            logger.info("[ItemConfigLoader] No items directory found. Skipping.");
            return 0;
        }

        File[] files = itemsDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null || files.length == 0) {
            logger.info("[ItemConfigLoader] No YAML files found in items/");
            return 0;
        }

        int count = 0;
        for (File file : files) {
            count += loadFile(file, registry);
        }

        return count;
    }

    /**
     * Load items từ một file YAML.
     */
    public int loadFile(File file, ItemRegistry registry) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = 0;

        // Top-level keys = namespaces
        for (String namespace : config.getKeys(false)) {
            ConfigurationSection namespaceSection = config.getConfigurationSection(namespace);
            if (namespaceSection != null) {
                count += loadNamespace(namespace, namespaceSection, file.getName(), registry);
            }
        }

        logger.info("[ItemConfigLoader] Loaded " + count + " items from " + file.getName());
        return count;
    }

    private int loadNamespace(String namespace, ConfigurationSection namespaceSection, String fileName, ItemRegistry registry) {
        int count = 0;
        // Sub-keys = item keys
        for (String itemKey : namespaceSection.getKeys(false)) {
            ConfigurationSection itemSection = namespaceSection.getConfigurationSection(itemKey);
            if (itemSection == null) continue;

            try {
                ItemDefinition definition = parseItem(namespace, itemKey, itemSection);
                registry.register(definition);
                count++;
            } catch (Exception e) {
                logger.warning("[ItemConfigLoader] Failed to load item: " +
                        namespace + ":" + itemKey + " from " + fileName);
                logger.warning("  Reason: " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * Parse một item section thành ItemDefinition.
     */
    private ItemDefinition parseItem(String namespace, String key, ConfigurationSection section) {
        String id = namespace + ":" + key;

        // Material (required)
        String materialStr = section.getString("material", "PAPER");
        Material material = Material.matchMaterial(materialStr);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material: " + materialStr);
        }

        // Builder
        ItemDefinition.Builder builder = ItemDefinition.builder(id)
                .material(material);

        // Display name
        String displayName = section.getString("display-name");
        if (displayName != null) {
            builder.displayName(displayName);
        }

        // Lore
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            builder.lore(lore);
        }

        // Custom model data
        if (section.contains("custom-model-data")) {
            builder.customModelData(section.getInt("custom-model-data"));
        }

        // Model
        String model = section.getString("model");
        if (model != null) {
            builder.model(model);
        }

        // Max stack size
        if (section.contains("max-stack-size")) {
            builder.maxStackSize(section.getInt("max-stack-size"));
        }

        // Item type
        String typeStr = section.getString("type");
        if (typeStr != null) {
            try {
                builder.type(ItemType.valueOf(typeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warning("[ItemConfigLoader] Invalid item type: " + typeStr + " for " + id);
            }
        }

        // Properties
        ConfigurationSection propsSection = section.getConfigurationSection("properties");
        if (propsSection != null) {
            Map<String, Object> properties = new HashMap<>();
            for (String propKey : propsSection.getKeys(false)) {
                properties.put(propKey, propsSection.get(propKey));
            }
            builder.properties(properties);
        }

        return builder.build();
    }
}
