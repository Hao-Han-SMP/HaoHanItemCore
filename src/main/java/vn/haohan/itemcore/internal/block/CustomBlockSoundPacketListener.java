package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import org.bukkit.Location;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PacketEvents listener that intercepts outgoing SoundEffect packets to prevent double sounds
 * when Barrier blocks are placed as custom block anchors.
 */
public class CustomBlockSoundPacketListener implements PacketListener {

    // Store recent custom block placement block coordinates for double-sound suppression (block X, Y, Z packed)
    private final Set<Long> recentPlacementLocations = ConcurrentHashMap.newKeySet();

    public static long packBlockPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) y & 0xFFF) << 26) | ((long) z & 0x3FFFFFF);
    }

    public void markRecentPlacement(Location loc) {
        if (loc == null) return;
        markRecentPlacement(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void markRecentPlacement(int x, int y, int z) {
        recentPlacementLocations.add(packBlockPos(x, y, z));
    }

    public void clearRecentPlacement(int x, int y, int z) {
        recentPlacementLocations.remove(packBlockPos(x, y, z));
    }

    public boolean isRecentPlacement(int x, int y, int z) {
        return recentPlacementLocations.contains(packBlockPos(x, y, z));
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event == null || event.getPacketType() != PacketType.Play.Server.SOUND_EFFECT) {
            return;
        }

        try {
            WrapperPlayServerSoundEffect packet = new WrapperPlayServerSoundEffect(event);
            com.github.retrooper.packetevents.util.Vector3d pos = packet.getPosition();
            if (pos == null) return;

            int blockX = (int) Math.floor(pos.getX());
            int blockY = (int) Math.floor(pos.getY());
            int blockZ = (int) Math.floor(pos.getZ());

            if (isRecentPlacement(blockX, blockY, blockZ)) {
                Sound sound = packet.getSound();
                if (sound != null && sound.getName() != null) {
                    String soundName = sound.getName().toString().toLowerCase();
                    // If server sends barrier or stone place sound at this coordinate, cancel packet to avoid double sound
                    if (soundName.contains("barrier") || (soundName.contains("stone") && soundName.contains("place"))) {
                        event.setCancelled(true);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
