package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renders client-side breaking animation overlays on custom blocks via virtual ItemDisplay entities.
 */
public class BlockBreakOverlayRenderer {

    // Index mappings for ItemDisplay
    public static final int METADATA_TRANSLATION_INDEX = 11;
    public static final int METADATA_SCALE_INDEX = 12;
    public static final int METADATA_ITEM_INDEX = 23;
    public static final int METADATA_ITEM_DISPLAY_TYPE_INDEX = 24;

    public static final float OVERLAY_SCALE = 1.001f;
    public static final float OVERLAY_TRANSLATION = -0.000025f;
    public static final int BASE_CUSTOM_MODEL_DATA = 90000;

    private final Logger logger;
    private BiConsumer<Player, PacketWrapper<?>> packetInterceptor;

    public BlockBreakOverlayRenderer(Logger logger) {
        this.logger = logger != null ? logger : Logger.getLogger(BlockBreakOverlayRenderer.class.getName());
    }

    public void setPacketInterceptor(BiConsumer<Player, PacketWrapper<?>> interceptor) {
        this.packetInterceptor = interceptor;
    }

    /**
     * Spawns a virtual ItemDisplay overlay entity for crack animation.
     */
    public void spawnCrack(Player player, int entityId, Location location, int stage) {
        if (player == null || !player.isOnline() || location == null) {
            return;
        }

        try {
            Vector3d pos = new Vector3d(location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
            WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                    entityId,
                    Optional.of(UUID.randomUUID()),
                    EntityTypes.ITEM_DISPLAY,
                    pos,
                    0f,
                    0f,
                    0f,
                    0,
                    Optional.empty()
            );

            List<EntityData<?>> metadataList = createMetadata(stage);
            WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadataList);

            send(player, spawnPacket);
            send(player, metadataPacket);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to spawn break overlay entity " + entityId, e);
        }
    }

    /**
     * Updates the crack stage of an existing virtual overlay entity.
     */
    public void updateCrack(Player player, int entityId, int stage) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            List<EntityData<?>> metadataList = createMetadata(stage);
            WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadataList);
            send(player, metadataPacket);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to update break overlay metadata " + entityId, e);
        }
    }

    /**
     * Destroys virtual overlay entity.
     */
    public void destroyCrack(Player player, int... entityIds) {
        if (player == null || !player.isOnline() || entityIds == null || entityIds.length == 0) {
            return;
        }

        try {
            WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityIds);
            send(player, destroyPacket);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to destroy break overlay entity", e);
        }
    }

    private List<EntityData<?>> createMetadata(int stage) {
        List<EntityData<?>> list = new ArrayList<>(4);

        // Scale: 1.00005f
        list.add(new EntityData<>(
                METADATA_SCALE_INDEX,
                EntityDataTypes.VECTOR3F,
                new Vector3f(OVERLAY_SCALE, OVERLAY_SCALE, OVERLAY_SCALE)
        ));

        // Translation offset: -0.000025f
        list.add(new EntityData<>(
                METADATA_TRANSLATION_INDEX,
                EntityDataTypes.VECTOR3F,
                new Vector3f(OVERLAY_TRANSLATION, OVERLAY_TRANSLATION, OVERLAY_TRANSLATION)
        ));

        // Displayed Item with stage CustomModelData
        int clampedStage = Math.max(0, Math.min(9, stage));
        int customModelData = BASE_CUSTOM_MODEL_DATA + clampedStage;
        
        com.github.retrooper.packetevents.protocol.item.ItemStack peItem = null;
        try {
            org.bukkit.inventory.ItemStack bukkitItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER);
            org.bukkit.inventory.meta.ItemMeta meta = bukkitItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                bukkitItem.setItemMeta(meta);
            }
            peItem = io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
        } catch (Throwable ignored) {
            // fallback in headless unit tests
            peItem = com.github.retrooper.packetevents.protocol.item.ItemStack.builder()
                    .type(ItemTypes.PAPER)
                    .amount(1)
                    .build();
        }

        list.add(new EntityData<>(
                METADATA_ITEM_INDEX,
                EntityDataTypes.ITEMSTACK,
                peItem
        ));

        // Item display transform: FIXED (5)
        list.add(new EntityData<>(
                METADATA_ITEM_DISPLAY_TYPE_INDEX,
                EntityDataTypes.BYTE,
                (byte) 5
        ));

        return list;
    }

    private void send(Player player, PacketWrapper<?> packet) {
        if (packetInterceptor != null) {
            packetInterceptor.accept(player, packet);
        }

        if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded() && player.isOnline()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }
}
