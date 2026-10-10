package vn.haohan.itemcore.api.block;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Visual and rendering definition for a client-side BlockDisplay entity.
 *
 * <p>Defines the block data or material to display, fixed scale (strictly 1.00005f),
 * and alignment translation offset. This object is immutable and thread-safe.</p>
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
    private final Vector3f scale;
    private final Vector3f translation;

    /**
     * Constructs a custom block model with the specified block data.
     *
     * <p>When to use: Call when creating a visual definition for a custom block.</p>
     * <p>Thread requirement: Thread-safe.</p>
     * <p>World/Packet impact: None (pure data model).</p>
     *
     * @param blockData the Bukkit BlockData to render in the BlockDisplay; must not be null
     * @throws NullPointerException if blockData is null
     */
    public CustomBlockModel(BlockData blockData) {
        this.blockData = blockData;
        this.scale = new Vector3f(DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE);
        this.translation = new Vector3f(DEFAULT_TRANSLATION_OFFSET, DEFAULT_TRANSLATION_OFFSET, DEFAULT_TRANSLATION_OFFSET);
    }

    /**
     * Constructs a custom block model from a simple Bukkit Material.
     *
     * <p>When to use: Convenience factory method for standard materials without extra states.</p>
     * <p>Thread requirement: Safe to call on any thread if material creates default BlockData.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param material the material to render; must be a block material
     * @return a new CustomBlockModel instance
     * @throws IllegalArgumentException if material is not a block
     * @throws NullPointerException if material is null
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
     *
     * <p>When to use: When custom properties or states (e.g. facing, lit) are defined.</p>
     * <p>Thread requirement: Thread-safe.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param blockData the custom block data; must not be null
     * @return a new CustomBlockModel instance
     * @throws NullPointerException if blockData is null
     */
    public static CustomBlockModel of(BlockData blockData) {
        return new CustomBlockModel(blockData);
    }

    /**
     * Gets the displayed Bukkit BlockData.
     *
     * <p>Thread requirement: Thread-safe.</p>
     *
     * @return the BlockData instance
     */
    public BlockData getBlockData() {
        return blockData;
    }

    /**
     * Gets the rendering scale vector.
     *
     * <p>Returns a new copy of the Vector3f with (1.00005f, 1.00005f, 1.00005f).</p>
     * <p>Thread requirement: Thread-safe.</p>
     *
     * @return copy of scale vector
     */
    public Vector3f getScale() {
        return new Vector3f(scale);
    }

    /**
     * Gets the rendering translation offset vector.
     *
     * <p>Thread requirement: Thread-safe.</p>
     *
     * @return copy of translation vector
     */
    public Vector3f getTranslation() {
        return new Vector3f(translation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomBlockModel that)) return false;
        return Objects.equals(blockData, that.blockData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockData);
    }

    @Override
    public String toString() {
        return "CustomBlockModel{blockData=" + blockData.getAsString() + ", scale=" + scale + "}";
    }
}
