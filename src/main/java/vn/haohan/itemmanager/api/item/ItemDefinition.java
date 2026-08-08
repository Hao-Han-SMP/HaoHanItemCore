package vn.haohan.itemmanager.api.item;

import org.bukkit.Material;

import java.util.*;

/**
 * Metadata mô tả một custom item. Đây là source of truth cho item, không phải ItemStack.
 * 
 * <p>Sử dụng Builder pattern:
 * <pre>
 * ItemDefinition def = ItemDefinition.builder("magic:fire_crystal")
 *     .material(Material.EMERALD)
 *     .displayName("§cFire Crystal")
 *     .lore(List.of("§7A crystal containing", "§7unstable fire energy."))
 *     .customModelData(1001)
 *     .maxStackSize(16)
 *     .properties(Map.of("type", "crystal", "element", "fire"))
 *     .build();
 * </pre>
 */
public final class ItemDefinition {

    private final String id;
    private final Material material;
    private final String displayName;
    private final int maxStackSize;
    private final List<String> lore;
    private final Integer customModelData;
    private final String itemModel;
    private final Map<String, Object> properties;
    private final ItemType type;
    private final ItemBehavior behavior;

    private ItemDefinition(Builder builder) {
        this.id = builder.id;
        this.material = builder.material;
        this.displayName = builder.displayName;
        this.maxStackSize = builder.maxStackSize;
        this.lore = List.copyOf(builder.lore);
        this.customModelData = builder.customModelData;
        this.itemModel = builder.model;
        this.properties = Map.copyOf(builder.properties);
        this.type = builder.type;
        this.behavior = builder.behavior;
    }

    // --- Getters ---

    public String getId() { return id; }

    /**
     * Lấy namespace từ ID (phần trước dấu ':').
     * Ví dụ: "magic:fire_crystal" → "magic"
     */
    public String getNamespace() {
        int colonIndex = id.indexOf(':');
        return colonIndex > 0 ? id.substring(0, colonIndex) : "";
    }

    /**
     * Lấy key từ ID (phần sau dấu ':').
     * Ví dụ: "magic:fire_crystal" → "fire_crystal"
     */
    public String getKey() {
        int colonIndex = id.indexOf(':');
        return colonIndex > 0 ? id.substring(colonIndex + 1) : id;
    }

    public Material getMaterial() { return material; }

    public String getDisplayName() { return displayName; }

    public int getMaxStackSize() { return maxStackSize; }

    public List<String> getLore() { return lore; }

    public Integer getCustomModelData() { return customModelData; }

    public String getItemModel() { return itemModel; }

    public Map<String, Object> getProperties() { return properties; }

    public ItemType getType() { return type; }

    public ItemBehavior getBehavior() { return behavior; }

    public boolean hasBehavior() { return behavior != null; }

    /**
     * Kiểm tra ID có đúng format namespace:key không.
     */
    public static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) return false;
        int colonIndex = id.indexOf(':');
        if (colonIndex <= 0 || colonIndex >= id.length() - 1) return false;
        String namespace = id.substring(0, colonIndex);
        String key = id.substring(colonIndex + 1);
        return namespace.matches("[a-z0-9_]+") && key.matches("[a-z0-9_]+");
    }

    // --- Builder ---

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private Material material = Material.PAPER;
        private String displayName;
        private int maxStackSize = 64;
        private List<String> lore = new ArrayList<>();
        private Integer customModelData = null;
        private String model = null;
        private Map<String, Object> properties = new HashMap<>();
        private ItemType type = ItemType.MATERIAL;
        private ItemBehavior behavior = null;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "Item ID cannot be null");
            this.displayName = id; // Default display name = id
        }

        public Builder material(Material material) {
            this.material = Objects.requireNonNull(material);
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName);
            return this;
        }

        public Builder maxStackSize(int maxStackSize) {
            if (maxStackSize < 1 || maxStackSize > 99) {
                throw new IllegalArgumentException("Max stack size must be between 1 and 99");
            }
            this.maxStackSize = maxStackSize;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = new ArrayList<>(lore);
            return this;
        }

        public Builder addLore(String line) {
            this.lore.add(line);
            return this;
        }

        public Builder customModelData(Integer customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = new HashMap<>(properties);
            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder type(ItemType type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        public Builder behavior(ItemBehavior behavior) {
            this.behavior = behavior;
            return this;
        }

        public ItemDefinition build() {
            if (!ItemDefinition.isValidId(id)) {
                throw new IllegalArgumentException(
                    "Invalid item ID: '" + id + "'. Must be in format 'namespace:key' " +
                    "where namespace and key contain only lowercase letters, digits, and underscores."
                );
            }
            return new ItemDefinition(this);
        }
    }

    @Override
    public String toString() {
        return "ItemDefinition{" +
                "id='" + id + '\'' +
                ", material=" + material +
                ", displayName='" + displayName + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemDefinition that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
