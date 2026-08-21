package vn.haohan.itemcore.internal.item;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemFactory;
import vn.haohan.itemcore.api.item.ItemRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Map;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation của ItemFactory.
 * Tạo ItemStack từ ItemDefinition, gắn PersistentData để nhận diện.
 */
public final class DefaultItemFactory implements ItemFactory {

    /** NamespacedKey dùng để lưu item ID trong PersistentDataContainer */
    public static final String ITEM_ID_KEY_NAME = "item_id";

    private final ItemRegistry registry;
    private final Plugin plugin;
    private final NamespacedKey itemIdKey;

    public DefaultItemFactory(ItemRegistry registry, Plugin plugin) {
        this.registry = registry;
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, ITEM_ID_KEY_NAME);
    }

    @Override
    public ItemStack create(String id) {
        return create(id, 1);
    }

    @Override
    public ItemStack create(String id, int amount) {
        ItemDefinition definition = registry.require(id);
        return create(definition, amount);
    }

    @Override
    public ItemStack create(ItemDefinition definition) {
        return create(definition, 1);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemStack create(ItemDefinition definition, int amount) {
        Objects.requireNonNull(definition, "ItemDefinition cannot be null");

        ItemStack itemStack = new ItemStack(definition.getMaterial(), amount);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta == null) {
            throw new IllegalStateException("Cannot get ItemMeta for material: " + definition.getMaterial());
        }

        // Display name (legacy color code support)
        Component displayName = LegacyComponentSerializer.legacySection()
                .deserialize(definition.getDisplayName());
        meta.displayName(displayName);

        // Lore, including structured sections and component-provided lines.
        List<String> lore = new java.util.ArrayList<>(definition.getLore());
        definition.getInfoSections().forEach(section -> {
            lore.add(section.title());
            lore.addAll(section.lines());
        });
        definition.getComponents().forEach(component -> component.appendLore(lore));
        if (!lore.isEmpty()) {
            List<Component> loreComponents = lore.stream()
                    .map(line -> LegacyComponentSerializer.legacySection().deserialize(line))
                    .collect(Collectors.toList());
            meta.lore(loreComponents);
        }

        // Custom Model Data
        if (definition.getCustomModelData() != null) {
            meta.setCustomModelData(definition.getCustomModelData());
        }

        // Item Model (Paper 1.21+)
        String model = definition.getItemModel();
        if (model == null && definition.getId() != null && definition.getId().contains(":")) {
            model = definition.getId();
        }
        if (model != null) {
            try {
                NamespacedKey modelKey = NamespacedKey.fromString(model);
                if (modelKey != null) {
                    meta.setItemModel(modelKey);
                }
            } catch (Throwable ignored) {
            }
        }

        // Max Stack Size
        if (definition.getMaxStackSize() != 64) {
            meta.setMaxStackSize(definition.getMaxStackSize());
        }

        // PersistentData - lưu item ID để nhận diện
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemIdKey, PersistentDataType.STRING, definition.getId());

        // Set the meta first so that subsequent component settings are not overwritten
        itemStack.setItemMeta(meta);
        for (var component : definition.getComponents()) {
            component.apply(itemStack, definition);
        }

        // Apply all components via the helper
        itemStack = applyComponents(itemStack, definition, plugin);

        return itemStack;
    }

    /**
     * Đồng bộ/cập nhật các custom component của definition vào ItemMeta và
     * ItemStack.
     * Trả về true nếu có bất kỳ thay đổi nào được thực hiện.
     */
    public static ItemStack applyComponents(ItemStack itemStack, ItemDefinition definition, Plugin plugin) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return itemStack;

        boolean metaModified = false;

        // Apply metadata-level components
        metaModified |= applyDisplayName(meta, definition);
        metaModified |= applyLore(meta, definition);
        metaModified |= applyCustomModelData(meta, definition);
        metaModified |= applyItemModel(meta, definition);
        metaModified |= applyMaxStackSize(meta, definition);
        metaModified |= applyMaxDamage(meta, definition, plugin);
        metaModified |= applyJukeboxPlayable(meta, definition, plugin);
        metaModified |= applyUniqueUUID(meta, definition, plugin);
        metaModified |= applyItemFlags(meta, definition);
        metaModified |= applyHideAdditionalTooltip(meta, definition);

        if (metaModified) {
            itemStack.setItemMeta(meta);
        }

        // Apply ItemStack-level components
        applyEquippableComponent(itemStack, definition, plugin);

        // Ensure NMS BLOCK_STATE component is stripped so the client doesn't show
        // raw block state properties (instrument, note, powered) in the item tooltip.
        itemStack = removeBlockStateComponent(itemStack);

        return itemStack;
    }

    private static boolean applyDisplayName(ItemMeta meta, ItemDefinition definition) {
        if (definition.getDisplayName() != null) {
            net.kyori.adventure.text.Component targetName = LegacyComponentSerializer.legacySection()
                    .deserialize(definition.getDisplayName());
            if (!targetName.equals(meta.displayName())) {
                meta.displayName(targetName);
                return true;
            }
        }
        return false;
    }

    private static boolean applyLore(ItemMeta meta, ItemDefinition definition) {
        java.util.List<net.kyori.adventure.text.Component> targetLore = definition.getLore().stream()
                .map(line -> LegacyComponentSerializer.legacySection().deserialize(line))
                .collect(Collectors.toList());
        java.util.List<net.kyori.adventure.text.Component> currentLore = meta.lore();
        if (currentLore == null)
            currentLore = java.util.List.of();
        if (!targetLore.equals(currentLore)) {
            meta.lore(targetLore.isEmpty() ? null : targetLore);
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static boolean applyCustomModelData(ItemMeta meta, ItemDefinition definition) {
        Integer targetCMD = definition.getCustomModelData();
        if (targetCMD != null) {
            if (!meta.hasCustomModelData() || !java.util.Objects.equals(meta.getCustomModelData(), targetCMD)) {
                meta.setCustomModelData(targetCMD);
                return true;
            }
        } else {
            if (meta.hasCustomModelData()) {
                meta.setCustomModelData(null);
                return true;
            }
        }
        return false;
    }

    private static boolean applyItemModel(ItemMeta meta, ItemDefinition definition) {
        String model = definition.getItemModel();
        if (model == null && definition.getId() != null && definition.getId().contains(":")) {
            model = definition.getId();
        }
        if (model != null) {
            try {
                NamespacedKey modelKey = NamespacedKey.fromString(model);
                if (modelKey != null) {
                    NamespacedKey currentModel = meta.getItemModel();
                    if (currentModel == null || !currentModel.equals(modelKey)) {
                        meta.setItemModel(modelKey);
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static boolean applyMaxStackSize(ItemMeta meta, ItemDefinition definition) {
        int targetMaxStack = definition.getMaxStackSize();
        if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != targetMaxStack) {
            meta.setMaxStackSize(targetMaxStack);
            return true;
        }
        return false;
    }

    private static boolean applyMaxDamage(ItemMeta meta, ItemDefinition definition, Plugin plugin) {
        Object maxDamageVal = definition.getProperties().get("max_damage");
        if (maxDamageVal instanceof Number num) {
            if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
                try {
                    int targetMaxDamage = num.intValue();
                    if (!dmg.hasMaxDamage() || dmg.getMaxDamage() != targetMaxDamage) {
                        dmg.setMaxDamage(targetMaxDamage);
                        return true;
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning(
                            "Failed to apply max_damage component for " + definition.getId() + ": " + t.getMessage());
                }
            }
        }
        return false;
    }

    private static boolean applyItemFlags(ItemMeta meta, ItemDefinition definition) {
        boolean modified = false;
        if (definition.getFlags() != null && !definition.getFlags().isEmpty()) {
            for (org.bukkit.inventory.ItemFlag flag : definition.getFlags()) {
                if (!meta.hasItemFlag(flag)) {
                    meta.addItemFlags(flag);
                    modified = true;
                }
            }
        }
        return modified;
    }

    @SuppressWarnings("deprecation")
    private static boolean applyHideAdditionalTooltip(ItemMeta meta, ItemDefinition definition) {
        Object hideVal = definition.getProperties().get("hide_additional_tooltip");
        boolean shouldHide = false;
        if (Boolean.TRUE.equals(hideVal)) {
            shouldHide = true;
        } else if (hideVal == null) {
            // Automatically hide additional tooltips for custom block definitions
            if (definition.getProperties().containsKey("custom_block_data")) {
                shouldHide = true;
            }
        }
        if (shouldHide) {
            if (!meta.hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP)) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                return true;
            }
        }
        return false;
    }

    private static boolean applyJukeboxPlayable(ItemMeta meta, ItemDefinition definition, Plugin plugin) {
        Object jukeboxPlayableVal = definition.getProperties().get("jukebox_playable");
        if (jukeboxPlayableVal instanceof String songKeyStr) {
            try {
                org.bukkit.inventory.meta.components.JukeboxPlayableComponent jb = meta.getJukeboxPlayable();
                NamespacedKey songKey = NamespacedKey.fromString(songKeyStr);
                if (songKey != null) {
                    if (jb.getSongKey() == null || !jb.getSongKey().equals(songKey)) {
                        jb.setSongKey(songKey);
                        meta.setJukeboxPlayable(jb);
                        return true;
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning(
                        "Failed to apply JUKEBOX_PLAYABLE component for " + definition.getId() + ": " + t.getMessage());
            }
        }
        return false;
    }

    private static boolean applyUniqueUUID(ItemMeta meta, ItemDefinition definition, Plugin plugin) {
        NamespacedKey uuidKey = new NamespacedKey(plugin, "uuid");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (definition.getMaxStackSize() == 1) {
            if (!pdc.has(uuidKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                pdc.set(uuidKey, org.bukkit.persistence.PersistentDataType.STRING,
                        java.util.UUID.randomUUID().toString());
                return true;
            }
        } else {
            if (pdc.has(uuidKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                pdc.remove(uuidKey);
                return true;
            }
        }
        return false;
    }

    private static boolean applyEquippableComponent(ItemStack itemStack, ItemDefinition definition, Plugin plugin) {
        Object equippableAssetId = definition.getProperties().get("equippable_asset_id");
        if (equippableAssetId instanceof String assetId) {
            try {
                boolean shouldUpdate = true;
                if (itemStack.hasData(io.papermc.paper.datacomponent.DataComponentTypes.EQUIPPABLE)) {
                    io.papermc.paper.datacomponent.item.Equippable current = itemStack
                            .getData(io.papermc.paper.datacomponent.DataComponentTypes.EQUIPPABLE);
                    if (current != null && current.assetId() != null) {
                        net.kyori.adventure.key.Key targetKey = net.kyori.adventure.key.Key.key(assetId);
                        if (current.assetId().equals(targetKey)) {
                            shouldUpdate = false;
                        }
                    }
                }

                if (shouldUpdate) {
                    org.bukkit.inventory.EquipmentSlot slot = getEquipmentSlotFromMaterial(itemStack.getType());
                    if (slot != null) {
                        io.papermc.paper.datacomponent.item.Equippable equippable = io.papermc.paper.datacomponent.item.Equippable
                                .equippable(slot)
                                .assetId(net.kyori.adventure.key.Key.key(assetId))
                                .equipSound(net.kyori.adventure.key.Key.key("item.armor.equip_netherite"))
                                .build();
                        itemStack.setData(io.papermc.paper.datacomponent.DataComponentTypes.EQUIPPABLE, equippable);
                        return true;
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning(
                        "Failed to apply EQUIPPABLE component for " + definition.getId() + ": " + t.getMessage());
            }
        }
        return false;
    }

    private static ItemStack removeBlockStateComponent(ItemStack itemStack) {
        try {
            Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            java.lang.reflect.Method asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            java.lang.reflect.Method asBukkitCopy = craftItemStackClass.getMethod("asBukkitCopy",
                    asNMSCopy.getReturnType());

            Object nmsStack = asNMSCopy.invoke(null, itemStack);
            if (nmsStack == null) {
                return itemStack;
            }

            Class<?> dataComponentsClass = Class.forName("net.minecraft.core.component.DataComponents");
            java.lang.reflect.Field blockStateField = dataComponentsClass.getField("BLOCK_STATE");
            Object blockStateComponentType = blockStateField.get(null);

            java.lang.reflect.Method hasMethod = nmsStack.getClass().getMethod("has", blockStateField.getType());
            boolean hasBlockState = (boolean) hasMethod.invoke(nmsStack, blockStateComponentType);

            if (hasBlockState) {
                java.lang.reflect.Method removeMethod = nmsStack.getClass().getMethod("remove",
                        blockStateField.getType());
                removeMethod.invoke(nmsStack, blockStateComponentType);
                return (ItemStack) asBukkitCopy.invoke(null, nmsStack);
            }
        } catch (Throwable ignored) {
        }
        return itemStack;
    }

    public static org.bukkit.inventory.EquipmentSlot getEquipmentSlotFromMaterial(org.bukkit.Material material) {
        String name = material.getKey().getKey().toUpperCase(Locale.ROOT);
        if (name.endsWith("_HELMET"))
            return org.bukkit.inventory.EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE"))
            return org.bukkit.inventory.EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS"))
            return org.bukkit.inventory.EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS"))
            return org.bukkit.inventory.EquipmentSlot.FEET;
        return null;
    }

    /**
     * Lấy NamespacedKey dùng để nhận diện custom item.
     */
    public NamespacedKey getItemIdKey() {
        return itemIdKey;
    }
}
