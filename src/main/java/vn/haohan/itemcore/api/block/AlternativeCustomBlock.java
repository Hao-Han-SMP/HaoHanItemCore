package vn.haohan.itemcore.api.block;

import org.bukkit.Material;

public final class AlternativeCustomBlock extends CustomBlockDefinition {

    private AlternativeCustomBlock(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return "AlternativeCustomBlock{id='" + getId() + "', baseBlock=" + getBaseBlock()
                + ", hardness=" + getHardness() + "}";
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder extends CustomBlockDefinition.AbstractBuilder<Builder, AlternativeCustomBlock> {
        private Builder(String id) {
            super(id, Material.BARRIER);
        }

        @Override
        public AlternativeCustomBlock build() {
            return new AlternativeCustomBlock(this);
        }
    }
}
