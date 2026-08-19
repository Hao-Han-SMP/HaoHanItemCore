package vn.haohan.itemcore.api.texture;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Một icon texture đã được nạp vào bộ nhớ.
 *
 * <p>Image là bản sao độc lập của vùng ảnh nguồn, vì vậy cắt một atlas không
 * làm thay đổi ảnh atlas gốc.</p>
 */
public final class IconTexture {
    private final String id;
    private final BufferedImage image;

    public IconTexture(String id, BufferedImage image) {
        this.id = requireId(id);
        this.image = copy(Objects.requireNonNull(image, "image cannot be null"));
    }

    public String getId() { return id; }

    /** Trả về bản sao để caller không thể làm hỏng texture đã đăng ký. */
    public BufferedImage getImage() { return copy(image); }

    public int getWidth() { return image.getWidth(); }

    public int getHeight() { return image.getHeight(); }

    static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Texture ID cannot be blank");
        }
        return id;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = result.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return result;
    }
}
