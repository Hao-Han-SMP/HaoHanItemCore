package vn.haohan.itemcore.api.texture;

import java.awt.image.BufferedImage;
import java.awt.Color;
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

    /** Tạo một icon mới bằng cách lật ảnh theo chiều ngang. */
    public IconTexture flipHorizontal(String newId) {
        BufferedImage result = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                result.setRGB(getWidth() - 1 - x, y, image.getRGB(x, y));
            }
        }
        return new IconTexture(newId, result);
    }

    /** Tạo một icon mới bằng cách lật ảnh theo chiều dọc. */
    public IconTexture flipVertical(String newId) {
        BufferedImage result = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                result.setRGB(x, getHeight() - 1 - y, image.getRGB(x, y));
            }
        }
        return new IconTexture(newId, result);
    }

    /** Tạo một icon mới bằng cách xoay theo chiều kim đồng hồ. */
    public IconTexture rotate90(String newId) {
        BufferedImage result = new BufferedImage(getHeight(), getWidth(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                result.setRGB(getHeight() - 1 - y, x, image.getRGB(x, y));
            }
        }
        return new IconTexture(newId, result);
    }

    /** Tạo một icon mới bằng cách xoay 180 độ. */
    public IconTexture rotate180(String newId) {
        BufferedImage result = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                result.setRGB(getWidth() - 1 - x, getHeight() - 1 - y, image.getRGB(x, y));
            }
        }
        return new IconTexture(newId, result);
    }

    /** Tạo một icon mới bằng cách xoay ngược chiều kim đồng hồ. */
    public IconTexture rotate270(String newId) {
        BufferedImage result = new BufferedImage(getHeight(), getWidth(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                result.setRGB(y, getWidth() - 1 - x, image.getRGB(x, y));
            }
        }
        return new IconTexture(newId, result);
    }

    /**
     * Tạo một icon mới bằng cách dịch hue theo số độ chỉ định.
     * Alpha, saturation và brightness của từng pixel được giữ nguyên.
     * Ví dụ: {@code hueShift("ui:arrow:blue", 120)}.
     */
    public IconTexture hueShift(String newId, float degrees) {
        float shift = degrees / 360.0f;
        BufferedImage result = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] hsb = new float[3];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    result.setRGB(x, y, argb);
                    continue;
                }

                Color.RGBtoHSB((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF,
                        argb & 0xFF, hsb);
                hsb[0] = (hsb[0] + shift) % 1.0f;
                if (hsb[0] < 0) hsb[0] += 1.0f;
                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                result.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
            }
        }
        return new IconTexture(newId, result);
    }

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
