package vn.haohan.itemcore.api.texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Các hàm đọc icon riêng lẻ và cắt icon từ atlas. */
public final class IconTextureLoader {
    private IconTextureLoader() {}

    public static IconTexture load(String id, Path file) throws IOException {
        return new IconTexture(id, read(file));
    }

    public static IconTexture load(String id, File file) throws IOException {
        return load(id, file.toPath());
    }

    public static IconTexture load(String id, InputStream input) throws IOException {
        return new IconTexture(id, read(input));
    }

    /**
     * Cắt atlas theo thứ tự row-major. Atlas không cần vuông, nhưng kích thước
     * phải chia hết cho kích thước một ô để không bị cắt mất pixel.
     */
    public static List<IconTexture> loadAtlas(String idPrefix, Path file,
                                               int iconWidth, int iconHeight) throws IOException {
        return loadAtlas(idPrefix, read(file), iconWidth, iconHeight);
    }

    public static List<IconTexture> loadAtlas(String idPrefix, File file,
                                               int iconWidth, int iconHeight) throws IOException {
        return loadAtlas(idPrefix, file.toPath(), iconWidth, iconHeight);
    }

    public static List<IconTexture> loadAtlas(String idPrefix, InputStream input,
                                               int iconWidth, int iconHeight) throws IOException {
        return loadAtlas(idPrefix, read(input), iconWidth, iconHeight);
    }

    public static List<IconTexture> loadAtlas(String idPrefix, BufferedImage atlas,
                                               int iconWidth, int iconHeight) {
        IconTexture.requireId(idPrefix);
        Objects.requireNonNull(atlas, "atlas cannot be null");
        if (iconWidth <= 0 || iconHeight <= 0) {
            throw new IllegalArgumentException("Icon dimensions must be positive");
        }
        if (atlas.getWidth() % iconWidth != 0 || atlas.getHeight() % iconHeight != 0) {
            throw new IllegalArgumentException("Atlas dimensions " + atlas.getWidth() + "x" + atlas.getHeight()
                    + " are not divisible by icon dimensions " + iconWidth + "x" + iconHeight);
        }

        List<IconTexture> result = new ArrayList<>();
        int columns = atlas.getWidth() / iconWidth;
        int rows = atlas.getHeight() / iconHeight;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                String id = idPrefix + ":" + (row * columns + column);
                BufferedImage icon = atlas.getSubimage(column * iconWidth, row * iconHeight,
                        iconWidth, iconHeight);
                result.add(new IconTexture(id, icon));
            }
        }
        return List.copyOf(result);
    }

    private static BufferedImage read(Path file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        return read(ImageIO.read(file.toFile()), file.toString());
    }

    private static BufferedImage read(InputStream input) throws IOException {
        return read(ImageIO.read(Objects.requireNonNull(input, "input cannot be null")), "stream");
    }

    private static BufferedImage read(BufferedImage image, String source) throws IOException {
        if (image == null) {
            throw new IOException("Unsupported or invalid image: " + source);
        }
        return image;
    }
}
