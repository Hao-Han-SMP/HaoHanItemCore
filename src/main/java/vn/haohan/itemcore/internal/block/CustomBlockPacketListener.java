package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for outgoing server chunk packets to stream client-side BlockDisplay entities
 * immediately as chunks load on the client.
 */
public class CustomBlockPacketListener implements PacketListener {

    private final CustomBlockVisibilityManager visibilityManager;
    private final Logger logger;

    public CustomBlockPacketListener(CustomBlockVisibilityManager visibilityManager, Logger logger) {
        this.visibilityManager = Objects.requireNonNull(visibilityManager, "visibilityManager cannot be null");
        this.logger = logger != null ? logger : Logger.getLogger(CustomBlockPacketListener.class.getName());
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event == null || event.getPacketType() == null) return;

        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            Object rawPlayer = event.getPlayer();
            if (!(rawPlayer instanceof Player player)) return;

            try {
                WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
                if (packet.getColumn() != null) {
                    visibilityManager.handleChunkData(player, packet.getColumn().getX(), packet.getColumn().getZ());
                }
            } catch (Throwable t) {
                logger.log(Level.FINE, "Failed to process CHUNK_DATA for custom blocks", t);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            Object rawPlayer = event.getPlayer();
            if (!(rawPlayer instanceof Player player)) return;

            try {
                WrapperPlayServerUnloadChunk packet = new WrapperPlayServerUnloadChunk(event);
                visibilityManager.handleChunkUnloadPacket(player, packet.getChunkX(), packet.getChunkZ());
            } catch (Throwable t) {
                logger.log(Level.FINE, "Failed to process UNLOAD_CHUNK for custom blocks", t);
            }
        }
    }
}
