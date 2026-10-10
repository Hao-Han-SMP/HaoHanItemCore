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

        BlockData blockData = null;
        Object modelObj = itemDef.getProperties().get("custom_block_model");
        if (modelObj instanceof String modelStr) {
            try {
                blockData = Bukkit.createBlockData(modelStr);
            } catch (Exception ignored) {}
        }
        if (blockData == null) {
            try {
                blockData = Bukkit.createBlockData(Material.STONE);
            } catch (Throwable ignored) {}
        }

        String dropId = itemDef.getId();
        Object dropObj = itemDef.getProperties().get("custom_block_drop");
        if (dropObj instanceof String d) {
            dropId = d;
        }

        CustomBlockDefinition.Builder builder = CustomBlockDefinition.builder(customBlockId)
                .model(new CustomBlockModel(blockData))
                .dropItemId(dropId);

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
