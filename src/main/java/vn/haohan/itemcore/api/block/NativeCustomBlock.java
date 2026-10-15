package vn.haohan.itemcore.api.block;

import org.bukkit.Material;

import java.util.Collection;
import java.util.Map;

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

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder extends CustomBlockDefinition.Builder {
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

        @Override public Builder baseBlock(Material baseBlock) { super.baseBlock(baseBlock); return this; }
        @Override public Builder soundGroup(CustomBlockSoundGroup soundGroup) { super.soundGroup(soundGroup); return this; }
        @Override public Builder onInteract(CustomBlockInteractHandler handler) { super.onInteract(handler); return this; }
        @Override public Builder onStep(CustomBlockStepHandler handler) { super.onStep(handler); return this; }
        @Override public Builder model(CustomBlockModel model) { super.model(model); return this; }
        @Override public Builder property(String key, Object value) { super.property(key, value); return this; }
        @Override public Builder properties(Map<String, Object> properties) { super.properties(properties); return this; }
        @Override public Builder dropItemId(String dropItemId) { super.dropItemId(dropItemId); return this; }
        @Override public Builder hardness(float seconds) { super.hardness(seconds); return this; }
        @Override public Builder unbreakable() { super.unbreakable(); return this; }
        @Override public Builder instantBreak() { super.instantBreak(); return this; }
        @Override public Builder hardness(boolean unbreakable) { super.hardness(unbreakable); return this; }
        @Override public Builder miningCondition(MiningCondition miningCondition) { super.miningCondition(miningCondition); return this; }
        @Override public Builder dropProvider(BlockDropProvider dropProvider) { super.dropProvider(dropProvider); return this; }
        @Override public Builder toolType(ToolType toolType) { super.toolType(toolType); return this; }
        @Override public Builder mineableWith(ToolType toolType, ToolTier minTier) { super.mineableWith(toolType, minTier); return this; }
        @Override public Builder mineableWith(Collection<ToolType> toolTypes, ToolTier minTier) { super.mineableWith(toolTypes, minTier); return this; }
        @Override public Builder mineableWith(ToolType toolType) { super.mineableWith(toolType); return this; }
        @Override public Builder requiredTier(ToolTier minTier) { super.requiredTier(minTier); return this; }
        @Override public Builder silkTouchDropsSelf(boolean dropsSelf) { super.silkTouchDropsSelf(dropsSelf); return this; }

        @Override
        public NativeCustomBlock build() {
            return new NativeCustomBlock(this);
        }
    }
}
