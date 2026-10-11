package vn.haohan.itemcore.api.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import vn.haohan.itemcore.api.HaoHanItemCore;

import java.util.*;

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
    private final ToolType toolType;
    private final Set<ToolType> harvestToolTypes;
    private final ToolTier requiredTier;
    private final CustomBlockSoundGroup soundGroup;
    private final CustomBlockInteractHandler interactHandler;
    private final CustomBlockStepHandler stepHandler;

    private CustomBlockDefinition(Builder builder) {
        this.id = builder.id;
        this.baseBlock = resolveBaseBlock(builder.id, builder.baseBlock);
        this.model = builder.model;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        this.dropItemId = builder.dropItemId;
        this.hardness = builder.hardness;
        this.harvestToolTypes = Collections.unmodifiableSet(new HashSet<>(builder.harvestToolTypes));
        this.requiredTier = builder.requiredTier;
        this.soundGroup = builder.soundGroup != null ? builder.soundGroup : CustomBlockSoundGroup.STONE;
        this.interactHandler = builder.interactHandler;
        this.stepHandler = builder.stepHandler;

        // ToolType fallback for backwards compatibility
        if (builder.toolType != null) {
            this.toolType = builder.toolType;
        } else if (!this.harvestToolTypes.isEmpty()) {
            this.toolType = this.harvestToolTypes.iterator().next();
        } else {
            this.toolType = null;
        }

        // Auto-generate unified mining condition if none explicitly given
        if (builder.miningCondition != null) {
            this.miningCondition = builder.miningCondition;
        } else if (hasPreferredTools()) {
            this.miningCondition = context -> isEffectiveTool(context != null ? context.tool() : null);
        } else {
            this.miningCondition = MiningCondition.always();
        }

        this.dropProvider = builder.dropProvider != null
                ? builder.dropProvider
                : BlockDropProvider.builder()
                        .defaultDrop(builder.dropItemId)
                        .silkTouchDropsSelf(builder.silkTouchDropsSelf)
                        .build();
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public Material getBaseBlock() {
        return baseBlock;
    }

    /**
     * Returns true if this definition overwrites an existing vanilla Minecraft block
     * instead of using a physical Barrier + BlockDisplay.
     */
    public boolean isVanillaOverwrite() {
        return baseBlock != null && baseBlock != Material.BARRIER && safeIsBlock(baseBlock);
    }

    private static boolean safeIsBlock(Material material) {
        if (material == null) return false;
        try {
            return !material.isAir() && material.isBlock();
        } catch (Throwable ignored) {
            // Paper test environment without RegistryAccess initialized
            String name = material.name();
            return !name.equals("AIR") && !name.equals("CAVE_AIR") && !name.equals("VOID_AIR")
                    && !name.endsWith("_ITEM") && !name.endsWith("_INGOT");
        }
    }

    private static Material resolveBaseBlock(String id, Material explicitBaseBlock) {
        if (explicitBaseBlock != null) {
            return explicitBaseBlock;
        }
        if (id != null) {
            String lower = id.toLowerCase(Locale.ROOT);
            if (lower.startsWith("minecraft:")) {
                String matName = lower.substring("minecraft:".length());
                Material mat = Material.matchMaterial(matName);
                if (mat != null && safeIsBlock(mat)) {
                    return mat;
                }
            } else if (!lower.contains(":")) {
                Material mat = Material.matchMaterial(lower);
                if (mat != null && safeIsBlock(mat)) {
                    return mat;
                }
            }
        }
        return Material.BARRIER;
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

    public ToolType getToolType() {
        return toolType;
    }

    public Set<ToolType> getHarvestToolTypes() {
        return harvestToolTypes;
    }

    public ToolTier getRequiredTier() {
        return requiredTier;
    }

    public CustomBlockSoundGroup getSoundGroup() {
        return soundGroup;
    }

    public CustomBlockInteractHandler getInteractHandler() {
        return interactHandler;
    }

    public CustomBlockStepHandler getStepHandler() {
        return stepHandler;
    }

    /**
     * Checks whether this block has preferred harvest tools or tier requirements configured.
     * If false, all tools and bare hand mine this block at the exact configured base break time without tool multipliers.
     */
    public boolean hasPreferredTools() {
        return toolType != null || !harvestToolTypes.isEmpty() || requiredTier != null;
    }

    /**
     * Checks if the given tool satisfies the tool type and minimum tier required to effectively mine this block.
     */
    public boolean isEffectiveTool(ItemStack tool) {
        if (!hasPreferredTools()) {
            return true;
        }

        if (tool == null || tool.getType() == null || tool.getType() == Material.AIR) {
            if (harvestToolTypes.isEmpty() && requiredTier == null) {
                return true;
            }
            return (harvestToolTypes.contains(ToolType.HAND) || harvestToolTypes.contains(ToolType.ANY)) && requiredTier == null;
        }

        // 1. Tool type check
        if (!harvestToolTypes.isEmpty() && !harvestToolTypes.contains(ToolType.ANY)) {
            ToolType currentType = ToolType.fromMaterial(tool.getType());
            // Try resolving custom tool type from item definition
            try {
                var core = HaoHanItemCore.get();
                if (core != null && core.getItemService() != null) {
                    String customId = core.getItemService().getId(tool);
                    if (customId != null) {
                        var def = core.getItemService().getDefinition(customId);
                        if (def != null) {
                            Object ttProp = def.getProperties().get("tool_type");
                            if (ttProp instanceof String s) {
                                currentType = ToolType.valueOf(s.toUpperCase(Locale.ROOT));
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            if (currentType == null || !harvestToolTypes.contains(currentType)) {
                return false;
            }
        }

        // 2. Tool tier check
        if (requiredTier != null) {
            ToolTier toolTier = null;
            try {
                var core = HaoHanItemCore.get();
                if (core != null && core.getToolTierRegistry() != null) {
                    toolTier = core.getToolTierRegistry().findTier(tool);
                }
            } catch (Throwable ignored) {}
            if (toolTier == null) {
                toolTier = ToolTier.fromMaterial(tool.getType());
            }

            if (toolTier == null || toolTier.level() < requiredTier.level()) {
                return false;
            }
        }

        return true;
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
        return "CustomBlockDefinition{id='" + id + "', hardness=" + hardness + ", harvestToolTypes=" + harvestToolTypes + ", requiredTier=" + requiredTier + "}";
    }

    /**
     * Builder for {@link CustomBlockDefinition}.
     */
    public static final class Builder {
        private final String id;
        private Material baseBlock;
        private CustomBlockModel model;
        private final Map<String, Object> properties = new HashMap<>();
        private String dropItemId;
        private float hardness = 0.0f;
        private MiningCondition miningCondition;
        private BlockDropProvider dropProvider;
        private ToolType toolType;
        private final Set<ToolType> harvestToolTypes = new HashSet<>();
        private ToolTier requiredTier;
        private boolean silkTouchDropsSelf = true;
        private CustomBlockSoundGroup soundGroup;
        private CustomBlockInteractHandler interactHandler;
        private CustomBlockStepHandler stepHandler;

        private Builder(String id) {
            Objects.requireNonNull(id, "Custom block ID cannot be null");
            if (id.isBlank()) {
                throw new IllegalArgumentException("Custom block ID cannot be blank");
            }
            this.id = id;
        }

        public Builder baseBlock(Material baseBlock) {
            this.baseBlock = baseBlock;
            return this;
        }

        public Builder soundGroup(CustomBlockSoundGroup soundGroup) {
            this.soundGroup = soundGroup;
            return this;
        }

        public Builder onInteract(CustomBlockInteractHandler handler) {
            this.interactHandler = handler;
            return this;
        }

        public Builder onStep(CustomBlockStepHandler handler) {
            this.stepHandler = handler;
            return this;
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

        public Builder toolType(ToolType toolType) {
            this.toolType = toolType;
            if (toolType != null) {
                this.harvestToolTypes.add(toolType);
            }
            return this;
        }

        public Builder mineableWith(ToolType toolType, ToolTier minTier) {
            if (toolType != null) {
                this.harvestToolTypes.add(toolType);
            }
            this.requiredTier = minTier;
            return this;
        }

        public Builder mineableWith(Collection<ToolType> toolTypes, ToolTier minTier) {
            if (toolTypes != null) {
                this.harvestToolTypes.addAll(toolTypes);
            }
            this.requiredTier = minTier;
            return this;
        }

        public Builder mineableWith(ToolType toolType) {
            return mineableWith(toolType, ToolTier.WOOD);
        }

        public Builder requiredTier(ToolTier minTier) {
            this.requiredTier = minTier;
            return this;
        }

        public Builder silkTouchDropsSelf(boolean dropsSelf) {
            this.silkTouchDropsSelf = dropsSelf;
            return this;
        }

        public CustomBlockDefinition build() {
            if (this.model == null) {
                Material resolved = resolveBaseBlock(this.id, this.baseBlock);
                if (resolved != null && resolved != Material.BARRIER && safeIsBlock(resolved)) {
                    try {
                        this.model = new CustomBlockModel(org.bukkit.Bukkit.createBlockData(resolved));
                    } catch (Throwable ignored) {
                        this.model = new CustomBlockModel(null);
                    }
                } else {
                    this.model = new CustomBlockModel(null);
                }
            }
            return new CustomBlockDefinition(this);
        }
    }
}
