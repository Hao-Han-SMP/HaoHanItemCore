package vn.haohan.itemcore.internal.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import vn.haohan.itemcore.api.block.BlockDropProvider;
import vn.haohan.itemcore.api.block.CustomBlockActionRegistry;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockModel;
import vn.haohan.itemcore.api.block.CustomBlockSound;
import vn.haohan.itemcore.api.block.CustomBlockSoundGroup;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.api.item.ItemDefinition;

import java.util.Map;

/**
 * Parses {@link CustomBlockDefinition} from {@link ItemDefinition} properties.
 */
public final class CustomBlockConfigParser {

    private CustomBlockConfigParser() {}

    @SuppressWarnings("unchecked")
    public static CustomBlockDefinition parseCustomBlock(ItemDefinition itemDef) {
        if (itemDef == null || itemDef.getProperties() == null) {
            return null;
        }

        Object cbIdObj = itemDef.getProperties().get("custom_block_id");
        if (!(cbIdObj instanceof String customBlockId) || customBlockId.isBlank()) {
            return null;
        }

        CustomBlockModel customModel;
        Object modelObj = itemDef.getProperties().get("custom_block_model");

        if (itemDef.isVanillaOverwrite()) {
            BlockData blockData = null;
            if (modelObj instanceof String modelStr) {
                try {
                    blockData = Bukkit.createBlockData(modelStr);
                } catch (Exception ignored) {}
            }
            if (blockData == null && itemDef.getMaterial() != null) {
                try {
                    if (itemDef.getMaterial().isBlock()) {
                        blockData = itemDef.getMaterial().createBlockData();
                    }
                } catch (Throwable ignored) {}
            }
            if (blockData == null) {
                try {
                    blockData = Bukkit.createBlockData(Material.STONE);
                } catch (Throwable ignored) {}
            }
            customModel = new CustomBlockModel(blockData);
        } else {
            org.bukkit.inventory.ItemStack itemStack = null;
            try {
                var core = vn.haohan.itemcore.api.HaoHanItemCore.get();
                if (core != null && core.getItemFactory() != null) {
                    itemStack = core.getItemFactory().create(itemDef);
                }
            } catch (Throwable ignored) {}

            if (itemStack == null) {
                try {
                    Material mat = itemDef.getMaterial() != null ? itemDef.getMaterial() : Material.BARRIER;
                    itemStack = new org.bukkit.inventory.ItemStack(mat);
                    org.bukkit.inventory.meta.ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        if (itemDef.getCustomModelData() != null) {
                            meta.setCustomModelData(itemDef.getCustomModelData());
                        }
                        String itemModel = itemDef.getItemModel();
                        if (itemModel == null && itemDef.getId() != null && itemDef.getId().contains(":")) {
                            itemModel = itemDef.getId();
                        }
                        if (itemModel != null) {
                            try {
                                meta.setItemModel(org.bukkit.NamespacedKey.fromString(itemModel));
                            } catch (Throwable ignored) {}
                        }
                        itemStack.setItemMeta(meta);
                    }
                } catch (Throwable ignored) {
                    itemStack = null;
                }
            }
            customModel = CustomBlockModel.of(itemStack);
        }

        String dropId = null;
        Object dropObj = itemDef.getProperties().get("custom_block_drop");
        if (dropObj instanceof String d) {
            dropId = d;
        } else if (!itemDef.isVanillaOverwrite()) {
            dropId = itemDef.getId();
        }

        CustomBlockDefinition.Builder builder = CustomBlockDefinition.builder(customBlockId)
                .model(customModel);
        if (dropId != null) {
            builder.dropItemId(dropId);
        }

        Object hardnessObj = itemDef.getProperties().get("custom_block_hardness");
        if (hardnessObj instanceof Number num) {
            builder.hardness(num.floatValue());
        } else if (hardnessObj instanceof Boolean bool) {
            builder.hardness(bool);
        }

        if (Boolean.TRUE.equals(itemDef.getProperties().get("custom_block_unbreakable"))) {
            builder.unbreakable();
        }
        if (Boolean.TRUE.equals(itemDef.getProperties().get("custom_block_instant_break"))) {
            builder.instantBreak();
        }

        Object silkTouchObj = itemDef.getProperties().get("custom_block_silk_touch_drop");
        if (silkTouchObj instanceof String silkDropId) {
            builder.dropProvider(BlockDropProvider.builder()
                    .defaultDrop(dropId)
                    .silkTouchDrop(silkDropId)
                    .build());
        } else if (Boolean.TRUE.equals(itemDef.getProperties().get("custom_block_silk_touch_drops_self"))) {
            builder.dropProvider(BlockDropProvider.builder()
                    .defaultDrop(dropId)
                    .silkTouchDropsSelf()
                    .build());
        }

        Object toolTypeObj = itemDef.getProperties().get("custom_block_tool_type");
        if (toolTypeObj == null) {
            toolTypeObj = itemDef.getProperties().get("tool_type");
        }
        if (toolTypeObj instanceof String ttStr) {
            try {
                builder.toolType(ToolType.valueOf(ttStr.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        Object tierObj = itemDef.getProperties().get("custom_block_required_tier");
        if (tierObj == null) {
            tierObj = itemDef.getProperties().get("required_tier");
        }
        if (tierObj instanceof String tierName) {
            vn.haohan.itemcore.api.block.ToolTier tier = null;
            try {
                var core = vn.haohan.itemcore.api.HaoHanItemCore.get();
                if (core != null && core.getToolTierRegistry() != null) {
                    tier = core.getToolTierRegistry().get(tierName).orElse(null);
                }
            } catch (Throwable ignored) {}
            if (tier != null) {
                builder.requiredTier(tier);
            }
        }

        // --- Sound Group Parsing ---
        Object soundsObj = itemDef.getProperties().get("custom_block_sounds");
        if (soundsObj instanceof Map<?, ?> soundMap) {
            CustomBlockSoundGroup.Builder sgBuilder = CustomBlockSoundGroup.builder();
            if (soundMap.get("place") instanceof String s) sgBuilder.placeSound(s);
            if (soundMap.get("break") instanceof String s) sgBuilder.breakSound(s);
            if (soundMap.get("hit") instanceof String s) sgBuilder.hitSound(s);
            if (soundMap.get("step") instanceof String s) sgBuilder.stepSound(s);
            if (soundMap.get("fall") instanceof String s) sgBuilder.fallSound(s);
            builder.soundGroup(sgBuilder.build());
        }

        // --- Interaction Action Hooks ---
        Object onInteractObj = itemDef.getProperties().get("custom_block_on_interact");
        if (onInteractObj instanceof String interactActionId) {
            CustomBlockActionRegistry.getInteractAction(interactActionId).ifPresent(builder::onInteract);
        }

        Object onStepObj = itemDef.getProperties().get("custom_block_on_step");
        if (onStepObj instanceof String stepActionId) {
            CustomBlockActionRegistry.getStepAction(stepActionId).ifPresent(builder::onStep);
        }

        return builder.build();
    }
}
