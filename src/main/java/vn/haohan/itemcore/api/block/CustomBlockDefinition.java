package vn.haohan.itemcore.api.block;

import org.bukkit.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition of a custom block.
 *
 * <p>Contains logical metadata, physical block representation (strictly {@link Material#BARRIER}),
 * client visual model definition, hardness/break time, mining harvest conditions,
 * drop providers, and arbitrary attributes.
 * This class is immutable and thread-safe.</p>
 */
public final class CustomBlockDefinition {

    private final String id;
    private final Material baseBlock;
    private final CustomBlockModel model;
    private final Map<String, Object> properties;
    private final String dropItemId;
    private final float hardness;
    private final MiningCondition miningCondition;
    private final BlockDropProvider dropProvider;

    private CustomBlockDefinition(Builder builder) {
        this.id = builder.id;
        this.baseBlock = Material.BARRIER;
        this.model = builder.model;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        this.dropItemId = builder.dropItemId;
        this.hardness = builder.hardness;
        this.miningCondition = builder.miningCondition != null ? builder.miningCondition : MiningCondition.always();
        this.dropProvider = builder.dropProvider != null
                ? builder.dropProvider
                : BlockDropProvider.builder().defaultDrop(builder.dropItemId).build();
    }

    /**
     * Creates a new builder for a CustomBlockDefinition.
     *
     * @param id unique identifier of the custom block; must not be null or blank
     * @return a new Builder instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public Material getBaseBlock() {
        return baseBlock;
    }

    public CustomBlockModel getModel() {
        return model;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getDropItemId() {
        return dropItemId;
    }

    public float getHardness() {
        return hardness;
    }

    public float getBreakTimeSeconds() {
        return Math.max(0.0f, hardness);
    }

    public boolean isUnbreakable() {
        return hardness < 0;
    }

    public boolean isInstantBreak() {
        return hardness == 0.0f;
    }

    public MiningCondition getMiningCondition() {
        return miningCondition;
    }

    public BlockDropProvider getDropProvider() {
        return dropProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomBlockDefinition that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CustomBlockDefinition{id='" + id + "', baseBlock=" + baseBlock + ", model=" + model + ", hardness=" + hardness + "}";
    }

    /**
     * Builder for {@link CustomBlockDefinition}.
     */
    public static final class Builder {
        private final String id;
        private CustomBlockModel model;
        private final Map<String, Object> properties = new HashMap<>();
        private String dropItemId;
        private float hardness = 0.0f;
        private MiningCondition miningCondition;
        private BlockDropProvider dropProvider;

        private Builder(String id) {
            Objects.requireNonNull(id, "Custom block ID cannot be null");
            if (id.isBlank()) {
                throw new IllegalArgumentException("Custom block ID cannot be blank");
            }
            this.id = id;
        }

        public Builder model(CustomBlockModel model) {
            this.model = Objects.requireNonNull(model, "model cannot be null");
            return this;
        }

        public Builder property(String key, Object value) {
            if (key != null && value != null) {
                this.properties.put(key, value);
            }
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            if (properties != null) {
                this.properties.putAll(properties);
            }
            return this;
        }

        public Builder dropItemId(String dropItemId) {
            this.dropItemId = dropItemId;
            return this;
        }

        public Builder hardness(float seconds) {
            this.hardness = seconds;
            return this;
        }

        public Builder unbreakable() {
            this.hardness = -1.0f;
            return this;
        }

        public Builder instantBreak() {
            this.hardness = 0.0f;
            return this;
        }

        public Builder hardness(boolean unbreakable) {
            this.hardness = unbreakable ? -1.0f : 0.0f;
            return this;
        }

        public Builder miningCondition(MiningCondition miningCondition) {
            this.miningCondition = miningCondition;
            return this;
        }

        public Builder dropProvider(BlockDropProvider dropProvider) {
            this.dropProvider = dropProvider;
            return this;
        }

        public CustomBlockDefinition build() {
            if (this.model == null) {
                this.model = new CustomBlockModel(null);
            }
            return new CustomBlockDefinition(this);
        }
    }
}
