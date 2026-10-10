package vn.haohan.itemcore.api.block;

import org.bukkit.Sound;

import java.util.Objects;

/**
 * Group of sounds related to custom block lifecycle actions.
 */
public final class CustomBlockSoundGroup {

    public static final CustomBlockSoundGroup STONE = builder()
            .breakSound("minecraft:block.stone.break", 1.0f, 0.8f)
            .stepSound("minecraft:block.stone.step", 1.0f, 1.0f)
            .placeSound("minecraft:block.stone.place", 1.0f, 0.8f)
            .hitSound("minecraft:block.stone.hit", 1.0f, 0.5f)
            .fallSound("minecraft:block.stone.fall", 1.0f, 1.0f)
            .build();

    private final CustomBlockSound breakSound;
    private final CustomBlockSound stepSound;
    private final CustomBlockSound placeSound;
    private final CustomBlockSound hitSound;
    private final CustomBlockSound fallSound;

    private CustomBlockSoundGroup(Builder builder) {
        this.breakSound = builder.breakSound;
        this.stepSound = builder.stepSound;
        this.placeSound = builder.placeSound;
        this.hitSound = builder.hitSound;
        this.fallSound = builder.fallSound;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CustomBlockSound getBreakSound() {
        return breakSound;
    }

    public CustomBlockSound getStepSound() {
        return stepSound;
    }

    public CustomBlockSound getPlaceSound() {
        return placeSound;
    }

    public CustomBlockSound getHitSound() {
        return hitSound;
    }

    public CustomBlockSound getFallSound() {
        return fallSound;
    }

    public static final class Builder {
        private CustomBlockSound breakSound = CustomBlockSound.of("minecraft:block.stone.break", 1.0f, 0.8f);
        private CustomBlockSound stepSound = CustomBlockSound.of("minecraft:block.stone.step", 1.0f, 1.0f);
        private CustomBlockSound placeSound = CustomBlockSound.of("minecraft:block.stone.place", 1.0f, 0.8f);
        private CustomBlockSound hitSound = CustomBlockSound.of("minecraft:block.stone.hit", 1.0f, 0.5f);
        private CustomBlockSound fallSound = CustomBlockSound.of("minecraft:block.stone.fall", 1.0f, 1.0f);

        private Builder() {}

        public Builder breakSound(CustomBlockSound sound) {
            if (sound != null) this.breakSound = sound;
            return this;
        }

        public Builder breakSound(String key, float volume, float pitch) {
            return breakSound(CustomBlockSound.of(key, volume, pitch));
        }

        public Builder breakSound(String key) {
            return breakSound(CustomBlockSound.of(key));
        }

        public Builder breakSound(Sound sound, float volume, float pitch) {
            return breakSound(CustomBlockSound.of(sound, volume, pitch));
        }

        public Builder stepSound(CustomBlockSound sound) {
            if (sound != null) this.stepSound = sound;
            return this;
        }

        public Builder stepSound(String key, float volume, float pitch) {
            return stepSound(CustomBlockSound.of(key, volume, pitch));
        }

        public Builder stepSound(String key) {
            return stepSound(CustomBlockSound.of(key));
        }

        public Builder stepSound(Sound sound, float volume, float pitch) {
            return stepSound(CustomBlockSound.of(sound, volume, pitch));
        }

        public Builder placeSound(CustomBlockSound sound) {
            if (sound != null) this.placeSound = sound;
            return this;
        }

        public Builder placeSound(String key, float volume, float pitch) {
            return placeSound(CustomBlockSound.of(key, volume, pitch));
        }

        public Builder placeSound(String key) {
            return placeSound(CustomBlockSound.of(key));
        }

        public Builder placeSound(Sound sound, float volume, float pitch) {
            return placeSound(CustomBlockSound.of(sound, volume, pitch));
        }

        public Builder hitSound(CustomBlockSound sound) {
            if (sound != null) this.hitSound = sound;
            return this;
        }

        public Builder hitSound(String key, float volume, float pitch) {
            return hitSound(CustomBlockSound.of(key, volume, pitch));
        }

        public Builder hitSound(String key) {
            return hitSound(CustomBlockSound.of(key));
        }

        public Builder hitSound(Sound sound, float volume, float pitch) {
            return hitSound(CustomBlockSound.of(sound, volume, pitch));
        }

        public Builder fallSound(CustomBlockSound sound) {
            if (sound != null) this.fallSound = sound;
            return this;
        }

        public Builder fallSound(String key, float volume, float pitch) {
            return fallSound(CustomBlockSound.of(key, volume, pitch));
        }

        public Builder fallSound(String key) {
            return fallSound(CustomBlockSound.of(key));
        }

        public Builder fallSound(Sound sound, float volume, float pitch) {
            return fallSound(CustomBlockSound.of(sound, volume, pitch));
        }

        public CustomBlockSoundGroup build() {
            return new CustomBlockSoundGroup(this);
        }
    }
}
