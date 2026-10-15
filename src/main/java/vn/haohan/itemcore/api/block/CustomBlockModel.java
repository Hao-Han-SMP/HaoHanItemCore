package vn.haohan.itemcore.api.block;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

/**
 * Visual and rendering definition for a client-side display entity (BlockDisplay or ItemDisplay).
 *
 * <p>Supports both BlockData (for vanilla block overwrites via BlockDisplay) and
 * ItemStack (for custom models via ItemDisplay). This object is immutable and thread-safe.</p>
 */
public final class CustomBlockModel {

    /**
     * The standard scale for custom block models (1.0f standard block size).
     */
    public static final float DEFAULT_SCALE = 1.0f;

    /**
     * Standard translation offset (0.0f).
     */
    public static final float DEFAULT_TRANSLATION_OFFSET = 0.0f;

    private final BlockData blockData;
    private final ItemStack itemStack;
    private final boolean itemDisplay;
    private final Vector3f scale;
    private final Vector3f translation;
    private final List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> cachedMetadata;

    /**
     * Constructs a default empty block model.
     */
    public CustomBlockModel() {
        this((BlockData) null);
    }

    /**
     * Constructs a custom block model with the specified block data.
     *
     * @param blockData the Bukkit BlockData to render in the BlockDisplay
     */
    public CustomBlockModel(BlockData blockData) {
        this(blockData, null, false, new Vector3f(DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE),
                new Vector3f(DEFAULT_TRANSLATION_OFFSET, DEFAULT_TRANSLATION_OFFSET, DEFAULT_TRANSLATION_OFFSET));
    }

    /**
     * Full constructor with explicit block data, item stack, scale, and translation offset.
     */
    public CustomBlockModel(BlockData blockData, ItemStack itemStack, Vector3f scale, Vector3f translation) {
        this(blockData, itemStack, itemStack != null, scale, translation);
    }

    /**
     * Full constructor with explicit display mode flag.
     */
    public CustomBlockModel(BlockData blockData, ItemStack itemStack, boolean itemDisplay, Vector3f scale, Vector3f translation) {
        this.blockData = blockData;
        ItemStack safeItem = null;
        if (itemStack != null) {
            try {
                safeItem = itemStack.clone();
            } catch (Throwable t) {
                safeItem = itemStack;
            }
        }
        this.itemStack = safeItem;
        this.itemDisplay = itemDisplay;
        this.scale = scale != null ? new Vector3f(scale) : new Vector3f(DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE);
        this.translation = translation != null ? new Vector3f(translation) : new Vector3f(DEFAULT_TRANSLATION_OFFSET, DEFAULT_TRANSLATION_OFFSET, DEFAULT_TRANSLATION_OFFSET);
        this.cachedMetadata = compileMetadata();
    }

    private List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> compileMetadata() {
        try {
            return doCompileMetadata();
        } catch (Throwable ignored) {
            return java.util.Collections.emptyList();
        }
    }

    private List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> doCompileMetadata() {
        if (com.github.retrooper.packetevents.PacketEvents.getAPI() == null) {
            return java.util.Collections.emptyList();
        }
        try {
            List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> list = new java.util.ArrayList<>(4);

            // Scale
            list.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<>(
                    12, // METADATA_SCALE_INDEX
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.VECTOR3F,
                    new com.github.retrooper.packetevents.util.Vector3f(scale.x, scale.y, scale.z)
            ));

            // Translation
            list.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<>(
                    11, // METADATA_TRANSLATION_INDEX
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.VECTOR3F,
                    new com.github.retrooper.packetevents.util.Vector3f(translation.x, translation.y, translation.z)
            ));

            if (this.itemDisplay) {
                com.github.retrooper.packetevents.protocol.item.ItemStack peItem = null;
                if (this.itemStack != null) {
                    try {
                        peItem = io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitItemStack(this.itemStack);
                    } catch (Throwable ignored) {
                    }
                }
                if (peItem == null) {
                    peItem = com.github.retrooper.packetevents.protocol.item.ItemStack.builder()
                            .type(com.github.retrooper.packetevents.protocol.item.type.ItemTypes.BARRIER)
                            .amount(1)
                            .build();
                }
                list.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<>(
                        23, // METADATA_ITEM_INDEX
                        com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.ITEMSTACK,
                        peItem
                ));
                list.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<>(
                        24, // METADATA_ITEM_DISPLAY_TYPE_INDEX
                        com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE,
                        (byte) 8 // FIXED
                ));
            } else {
                int blockStateId = 0;
                if (this.blockData != null) {
                    try {
                        blockStateId = io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitBlockData(this.blockData).getGlobalId();
                    } catch (Throwable ignored) {
                    }
                }
                list.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<>(
                        23, // METADATA_BLOCK_STATE_INDEX
                        com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BLOCK_STATE,
                        blockStateId
                ));
            }

            return java.util.Collections.unmodifiableList(list);
        } catch (Throwable t) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Gets the pre-compiled PacketEvents EntityData metadata list for this model.
     *
     * @return unmodifiable list of pre-compiled EntityData
     */
    public List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> getCachedMetadata() {
        if (cachedMetadata.isEmpty() && com.github.retrooper.packetevents.PacketEvents.getAPI() != null) {
            return compileMetadata();
        }
        return cachedMetadata;
    }

    /**
     * Constructs a custom block model from a simple Bukkit Material.
     */
    public static CustomBlockModel of(Material material) {
        Objects.requireNonNull(material, "material cannot be null");
        if (!material.isBlock()) {
            throw new IllegalArgumentException("Material " + material + " is not a valid block material");
        }
        return new CustomBlockModel(material.createBlockData());
    }

    /**
     * Constructs a custom block model from a Bukkit BlockData instance.
     */
    public static CustomBlockModel of(BlockData blockData) {
        return new CustomBlockModel(blockData);
    }

    /**
     * Constructs a custom block model from a Bukkit ItemStack instance.
     */
    public static CustomBlockModel of(ItemStack itemStack) {
        return new CustomBlockModel(null, itemStack, true,
                new Vector3f(DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE),
                new Vector3f(DEFAULT_TRANSLATION_OFFSET, DEFAULT_TRANSLATION_OFFSET, DEFAULT_TRANSLATION_OFFSET));
    }

    /**
     * Convenience alias for constructing from an ItemStack.
     */
    public static CustomBlockModel ofItem(ItemStack itemStack) {
        return of(itemStack);
    }

    /**
     * Checks if this model renders via ItemDisplay.
     */
    public boolean isItemDisplay() {
        return itemDisplay;
    }

    /**
     * Checks if this model renders via BlockDisplay.
     */
    public boolean isBlockDisplay() {
        return !itemDisplay;
    }

    /**
     * Gets the displayed Bukkit BlockData.
     */
    public BlockData getBlockData() {
        return blockData;
    }

    /**
     * Gets a clone of the displayed Bukkit ItemStack, or null if using BlockData.
     */
    public ItemStack getItemStack() {
        if (itemStack == null) return null;
        try {
            return itemStack.clone();
        } catch (Throwable t) {
            return itemStack;
        }
    }

    /**
     * Gets the rendering scale vector.
     */
    public Vector3f getScale() {
        return new Vector3f(scale);
    }

    /**
     * Gets the rendering translation offset vector.
     */
    public Vector3f getTranslation() {
        return new Vector3f(translation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomBlockModel that)) return false;
        return itemDisplay == that.itemDisplay
                && Objects.equals(blockData, that.blockData)
                && Objects.equals(itemStack, that.itemStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockData, itemStack, itemDisplay);
    }

    @Override
    public String toString() {
        if (itemDisplay) {
            String typeStr = "CUSTOM_ITEM";
            if (itemStack != null) {
                try {
                    typeStr = itemStack.getType().name();
                } catch (Throwable ignored) {}
            }
            return "CustomBlockModel{item=" + typeStr + ", scale=" + scale + "}";
        }
        return "CustomBlockModel{blockData=" + (blockData != null ? blockData.getAsString() : "null") + ", scale=" + scale + "}";
    }
}
