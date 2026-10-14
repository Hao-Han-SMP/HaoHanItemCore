package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import vn.haohan.itemcore.api.block.CustomBlockModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles client-side Display rendering (BlockDisplay or ItemDisplay) via PacketEvents packets.
 *
 * <p>Produces pure protocol-level packets:
 * <ul>
 *   <li>{@link WrapperPlayServerSpawnEntity}: Spawns client-side virtual Display entity.</li>
 *   <li>{@link WrapperPlayServerEntityMetadata}: Sets scale, translation, item/block state and display type.</li>
 *   <li>{@link WrapperPlayServerDestroyEntities}: Despawns Display entities when leaving view.</li>
 * </ul>
 * Zero Bukkit/NMS Display entities are created or ticked on the server.</p>
 */
public class BlockDisplayRenderer {

    // Index mappings for Minecraft 1.20+ Display metadata
    public static final int METADATA_TRANSLATION_INDEX = 11;
    public static final int METADATA_SCALE_INDEX = 12;
    public static final int METADATA_BLOCK_STATE_INDEX = 23;
    public static final int METADATA_ITEM_INDEX = 23;
    public static final int METADATA_ITEM_DISPLAY_TYPE_INDEX = 24;

    private final Logger logger;
    private BiConsumer<Player, PacketWrapper<?>> packetInterceptor; // used for benchmarks and test assertions

    public BlockDisplayRenderer() {
        this(null);
    }

    public BlockDisplayRenderer(Logger logger) {
        this.logger = logger != null ? logger : Logger.getLogger(BlockDisplayRenderer.class.getName());
    }

    /**
     * Sets an optional packet interceptor (useful in tests and benchmark suites).
     *
     * @param interceptor consumer receiving player and dispatched packet
     */
    public void setPacketInterceptor(BiConsumer<Player, PacketWrapper<?>> interceptor) {
        this.packetInterceptor = interceptor;
    }

    /**
     * Dispatches packets to spawn a client-side Display entity for a specific player.
     *
     * <p>Thread requirement: Thread-safe.</p>
     * <p>World/Packet impact: Emits 1 spawn packet and 1 metadata packet to the player's connection.</p>
     *
     * @param player player to receive the packets; must be online
     * @param entityId virtual entity ID
     * @param location world location of the block
     * @param model model definition specifying item/block state and scale
     */
    public void spawnDisplay(Player player, int entityId, Location location, CustomBlockModel model) {
        if (player == null || !player.isOnline() || model == null || location == null) {
            return;
        }

        try {
            boolean isItem = model.isItemDisplay();
            Vector3d pos = isItem
                    ? new Vector3d(location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5)
                    : new Vector3d(location.getBlockX(), location.getBlockY(), location.getBlockZ());

            WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                    entityId,
                    Optional.of(UUID.randomUUID()),
                    isItem ? EntityTypes.ITEM_DISPLAY : EntityTypes.BLOCK_DISPLAY,
                    pos,
                    0f,
                    0f,
                    0f,
                    0,
                    Optional.empty()
            );

            List<EntityData<?>> metadataList = model.getCachedMetadata();
            if (metadataList == null || metadataList.isEmpty()) {
                metadataList = createDisplayMetadata(model);
            }
            WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadataList);

            send(player, spawnPacket);
            send(player, metadataPacket);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to spawn Display virtual entity " + entityId + " for player " + player.getName(), e);
        }
    }

    /**
     * Dispatches packets to destroy one or more virtual Display entities on a player's client.
     *
     * <p>Thread requirement: Thread-safe.</p>
     * <p>World/Packet impact: Emits 1 destroy packet to the player.</p>
     *
     * @param player player to receive packet; must be online
     * @param entityIds array of virtual entity IDs to remove
     */
    public void destroyDisplay(Player player, int... entityIds) {
        if (player == null || !player.isOnline() || entityIds == null || entityIds.length == 0) {
            return;
        }

        try {
            WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityIds);
            send(player, destroyPacket);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to destroy Display entities for player " + player.getName(), e);
        }
    }

    /**
     * Dispatches a metadata update packet for an already spawned Display entity.
     *
     * @param player player to update
     * @param entityId virtual entity ID
     * @param newModel updated model
     */
    public void updateDisplayModel(Player player, int entityId, CustomBlockModel newModel) {
        if (player == null || !player.isOnline() || newModel == null) {
            return;
        }

        try {
            List<EntityData<?>> metadataList = newModel.getCachedMetadata();
            if (metadataList == null || metadataList.isEmpty()) {
                metadataList = createDisplayMetadata(newModel);
            }
            WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadataList);
            send(player, metadataPacket);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to update Display metadata for player " + player.getName(), e);
        }
    }

    /**
     * Builds the metadata entries for a Display entity using the specified model.
     */
    public List<EntityData<?>> createDisplayMetadata(CustomBlockModel model) {
        List<EntityData<?>> list = new ArrayList<>(4);

        // Scale
        org.joml.Vector3f scale = model.getScale();
        list.add(new EntityData<>(
                METADATA_SCALE_INDEX,
                EntityDataTypes.VECTOR3F,
                new com.github.retrooper.packetevents.util.Vector3f(scale.x, scale.y, scale.z)
        ));

        // Translation offset
        org.joml.Vector3f trans = model.getTranslation();
        list.add(new EntityData<>(
                METADATA_TRANSLATION_INDEX,
                EntityDataTypes.VECTOR3F,
                new com.github.retrooper.packetevents.util.Vector3f(trans.x, trans.y, trans.z)
        ));

        if (model.isItemDisplay()) {
            com.github.retrooper.packetevents.protocol.item.ItemStack peItem = null;
            if (model.getItemStack() != null) {
                try {
                    peItem = SpigotConversionUtil.fromBukkitItemStack(model.getItemStack());
                } catch (Throwable ignored) {
                }
            }
            if (peItem == null) {
                peItem = com.github.retrooper.packetevents.protocol.item.ItemStack.builder()
                        .type(ItemTypes.BARRIER)
                        .amount(1)
                        .build();
            }
            list.add(new EntityData<>(
                    METADATA_ITEM_INDEX,
                    EntityDataTypes.ITEMSTACK,
                    peItem
            ));
            list.add(new EntityData<>(
                    METADATA_ITEM_DISPLAY_TYPE_INDEX,
                    EntityDataTypes.BYTE,
                    (byte) 8 // FIXED
            ));
        } else {
            // Displayed Block State
            int blockStateId = 0;
            if (model.getBlockData() != null) {
                try {
                    blockStateId = SpigotConversionUtil.fromBukkitBlockData(model.getBlockData()).getGlobalId();
                } catch (Throwable ignored) {
                    // fallback if called in isolated headless test environment
                }
            }
            list.add(new EntityData<>(
                    METADATA_BLOCK_STATE_INDEX,
                    EntityDataTypes.BLOCK_STATE,
                    blockStateId
            ));
        }

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
