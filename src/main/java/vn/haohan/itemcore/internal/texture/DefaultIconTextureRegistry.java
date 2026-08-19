package vn.haohan.itemcore.internal.texture;

import vn.haohan.itemcore.api.texture.IconTexture;
import vn.haohan.itemcore.api.texture.IconTextureLoader;
import vn.haohan.itemcore.api.texture.IconTextureRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultIconTextureRegistry implements IconTextureRegistry {
    private final Map<String, IconTexture> textures = new LinkedHashMap<>();
    private final Map<String, List<IconTexture>> atlases = new LinkedHashMap<>();

    @Override
    public synchronized IconTexture register(String id, IconTexture texture) {
        Objects.requireNonNull(texture, "texture cannot be null");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Texture ID cannot be blank");
        IconTexture stored = new IconTexture(id, texture.getImage());
        textures.put(id, stored);
        return stored;
    }

    @Override
    public synchronized IconTexture register(String id, Path file) throws IOException {
        return register(id, IconTextureLoader.load(id, file));
    }

    @Override
    public synchronized List<IconTexture> registerAtlas(String atlasId, Path file,
                                                          int iconWidth, int iconHeight) throws IOException {
        List<IconTexture> loaded = IconTextureLoader.loadAtlas(atlasId, file, iconWidth, iconHeight);
        List<IconTexture> stored = new ArrayList<>();
        for (IconTexture texture : loaded) stored.add(register(texture.getId(), texture));
        List<IconTexture> immutable = List.copyOf(stored);
        atlases.put(atlasId, immutable);
        return immutable;
    }

    @Override
    public synchronized Optional<IconTexture> get(String id) {
        return Optional.ofNullable(textures.get(id));
    }

    @Override
    public synchronized List<IconTexture> getAtlas(String atlasId) {
        return atlases.getOrDefault(atlasId, List.of());
    }

    @Override
    public synchronized void unregister(String id) {
        textures.remove(id);
        atlases.values().removeIf(list -> list.stream().anyMatch(texture -> texture.getId().equals(id)));
    }

    @Override
    public synchronized void clear() {
        textures.clear();
        atlases.clear();
    }
}
