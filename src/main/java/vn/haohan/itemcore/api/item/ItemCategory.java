package vn.haohan.itemcore.api.item;

import org.bukkit.Material;

import java.util.Locale;

/**
 * Danh mục phân loại item trong hệ thống Item Browser & Filter.
 */
public enum ItemCategory {
    ALL("Tất cả", Material.NETHER_STAR, "Tất cả các vật phẩm"),
    TOOLS("Công cụ", Material.DIAMOND_PICKAXE, "Cuốc, rìu, xẻng, kéo, cần câu, công cụ khai khoáng"),
    WEAPONS("Vũ khí", Material.DIAMOND_SWORD, "Kiếm, cung, nỏ, đinh ba, mace, vũ khí chiến đấu"),
    ARMOR("Giáp & Trang bị", Material.NETHERITE_CHESTPLATE, "Mũ, áo giáp, quần, giày, giáp sói, đồ mặc"),
    CUSTOM_BLOCKS("Custom Block", Material.NOTE_BLOCK, "Các khối block tùy chỉnh, NoteBlock, machine blocks"),
    MATERIALS("Nguyên liệu", Material.EMERALD, "Quặng, thỏi kim loại, đá quý, mảnh vỡ, tài nguyên"),
    FOOD("Thức ăn", Material.GOLDEN_APPLE, "Thực phẩm, nước uống, đồ tiêu hao hồi phục"),
    MACHINES("Máy móc & Linh kiện", Material.REDSTONE, "Linh kiện máy móc, vi mạch, công nghệ"),
    CURRENCY("Tiền tệ", Material.GOLD_INGOT, "Đồng xu, tiền giấy, token kinh tế"),
    SPECIAL("Đặc biệt", Material.BEACON, "Chìa khóa, đĩa nhạc, vật phẩm nhiệm vụ, sự kiện");

    private final String displayName;
    private final Material icon;
    private final String description;

    ItemCategory(String displayName, Material icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Xác định xem một ItemDefinition có thuộc Category này hay không.
     */
    public boolean matches(ItemDefinition def) {
        if (this == ALL) {
            return true;
        }

        ItemType type = def.getType();
        Material mat = def.getMaterial();
        String matName = mat != null ? mat.name().toUpperCase(Locale.ROOT) : "";

        return switch (this) {
            case ALL -> true;
            case CUSTOM_BLOCKS -> isCustomBlock(def, mat, matName);
            case TOOLS -> isTool(def, type, matName);
            case WEAPONS -> isWeapon(def, type, matName);
            case ARMOR -> isArmor(def, type, matName);
            case MATERIALS -> type == ItemType.MATERIAL || isMaterial(matName);
            case FOOD -> type == ItemType.FOOD || isFood(matName);
            case MACHINES -> type == ItemType.MACHINE_COMPONENT;
            case CURRENCY -> type == ItemType.CURRENCY;
            case SPECIAL -> type == ItemType.SPECIAL || (!isTool(def, type, matName) && !isWeapon(def, type, matName) && !isArmor(def, type, matName) && !isCustomBlock(def, mat, matName));
        };
    }

    private static boolean isCustomBlock(ItemDefinition def, Material mat, String matName) {
        if (def.getProperties().containsKey("custom_block_data") ||
            def.getProperties().containsKey("is_block") ||
            def.getProperties().containsKey("block_type") ||
            def.getProperties().containsKey("custom_block_drop")) {
            return true;
        }
        if (mat == Material.NOTE_BLOCK || mat == Material.PLAYER_HEAD || mat == Material.PLAYER_WALL_HEAD) {
            if (def.getProperties().containsKey("custom_block_data")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTool(ItemDefinition def, ItemType type, String matName) {
        if (type == ItemType.TOOL) return true;
        if (matName.endsWith("_PICKAXE") || matName.endsWith("_AXE") || matName.endsWith("_SHOVEL") ||
            matName.endsWith("_HOE") || matName.equals("SHEARS") || matName.equals("FISHING_ROD") ||
            matName.equals("FLINT_AND_STEEL") || matName.equals("BRUSH") || matName.equals("COMPASS") ||
            matName.equals("CLOCK") || matName.equals("SPYGLASS") || matName.equals("LEAD")) {
            return true;
        }
        for (var comp : def.getComponents()) {
            if (comp instanceof MiningComponent || comp instanceof DurabilityComponent) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWeapon(ItemDefinition def, ItemType type, String matName) {
        if (type == ItemType.WEAPON) return true;
        return matName.endsWith("_SWORD") || matName.equals("BOW") || matName.equals("CROSSBOW") ||
               matName.equals("TRIDENT") || matName.equals("MACE") || matName.equals("SHIELD") ||
               matName.equals("WIND_CHARGE");
    }

    private static boolean isArmor(ItemDefinition def, ItemType type, String matName) {
        if (type == ItemType.ARMOR) return true;
        if (def.getProperties().containsKey("equippable_asset_id")) return true;
        return matName.endsWith("_HELMET") || matName.endsWith("_CHESTPLATE") ||
               matName.endsWith("_LEGGINGS") || matName.endsWith("_BOOTS") ||
               matName.equals("ELYTRA") || matName.equals("TURTLE_HELMET") ||
               matName.endsWith("_WOLF_ARMOR");
    }

    private static boolean isFood(String matName) {
        return matName.equals("APPLE") || matName.equals("BREAD") || matName.equals("COOKED_BEEF") ||
               matName.equals("COOKED_PORKCHOP") || matName.equals("COOKED_CHICKEN") || matName.equals("COOKED_MUTTON") ||
               matName.equals("COOKED_COD") || matName.equals("COOKED_SALMON") || matName.equals("GOLDEN_APPLE") ||
               matName.equals("ENCHANTED_GOLDEN_APPLE") || matName.equals("GOLDEN_CARROT") || matName.equals("CARROT") ||
               matName.equals("POTATO") || matName.equals("BAKED_POTATO") || matName.equals("BEETROOT") ||
               matName.equals("BEETROOT_SOUP") || matName.equals("MUSHROOM_STEW") || matName.equals("RABBIT_STEW") ||
               matName.equals("SUSPICIOUS_STEW") || matName.equals("COOKIE") || matName.equals("MELON_SLICE") ||
               matName.equals("DRIED_KELP") || matName.equals("SWEET_BERRIES") || matName.equals("GLOW_BERRIES") ||
               matName.equals("HONEY_BOTTLE") || matName.equals("POTION");
    }

    private static boolean isMaterial(String matName) {
        return matName.endsWith("_INGOT") || matName.endsWith("_NUGGET") || matName.endsWith("_RAW") ||
               matName.startsWith("RAW_") || matName.endsWith("_GEM") || matName.endsWith("_DUST") ||
               matName.equals("DIAMOND") || matName.equals("EMERALD") || matName.equals("AMETHYST_SHARD") ||
               matName.equals("QUARTZ") || matName.equals("LAPIS_LAZULI") || matName.equals("REDSTONE") ||
               matName.equals("COAL") || matName.equals("CHARCOAL") || matName.equals("PRISMARINE_SHARD") ||
               matName.equals("PRISMARINE_CRYSTALS") || matName.equals("NETHER_BRICK") || matName.equals("BRICK");
    }
}
