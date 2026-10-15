package vn.haohan.itemcore.api.block;

import org.bukkit.Material;

public final class NativeCustomBlock extends CustomBlockDefinition {

    public static final int MIN_VARIATION = 0;
    public static final int MAX_VARIATION = 774;

    private final int customVariation;
    private final boolean directional;

    private NativeCustomBlock(Builder builder) {
        super(builder);
        this.customVariation = builder.customVariation;
        this.directional = builder.directional;
    }

    public int getCustomVariation() {
        return customVariation;
    }

    public boolean isDirectional() {
        return directional;
    }

    @Override
    public String toString() {
        return "NativeCustomBlock{id='" + getId() + "', baseBlock=" + getBaseBlock()
                + ", customVariation=" + customVariation + ", directional=" + directional
                + ", hardness=" + getHardness() + "}";
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder extends CustomBlockDefinition.AbstractBuilder<Builder, NativeCustomBlock> {
        private int customVariation = 0;
        private boolean directional = false;

        private Builder(String id) {
            super(id, Material.NOTE_BLOCK);
        }

        public Builder customVariation(int variation) {
            if (variation < MIN_VARIATION || variation > MAX_VARIATION) {
                throw new IllegalArgumentException("customVariation must be between " + MIN_VARIATION + " and " + MAX_VARIATION + ", got: " + variation);
            }
            this.customVariation = variation;
            return this;
        }

        public Builder directional(boolean directional) {
            this.directional = directional;
            return this;
        }

        @Override
        public NativeCustomBlock build() {
            return new NativeCustomBlock(this);
        }
    }
}
