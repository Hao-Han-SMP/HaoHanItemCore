package vn.haohan.itemcore.api.texture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Registry thống nhất cho icon riêng lẻ và icon được cắt từ atlas. */
public interface IconTextureRegistry {
    IconTexture register(String id, IconTexture texture);

    IconTexture register(String id, Path file) throws IOException;

    default IconTexture register(String id, File file) throws IOException {
        return register(id, file.toPath());
    }

    List<IconTexture> registerAtlas(String atlasId, Path file, int iconWidth, int iconHeight)
            throws IOException;

    default List<IconTexture> registerAtlas(String atlasId, File file, int iconWidth, int iconHeight)
            throws IOException {
        return registerAtlas(atlasId, file.toPath(), iconWidth, iconHeight);
    }

    Optional<IconTexture> get(String id);

    List<IconTexture> getAtlas(String atlasId);

    void unregister(String id);

    void clear();
}
