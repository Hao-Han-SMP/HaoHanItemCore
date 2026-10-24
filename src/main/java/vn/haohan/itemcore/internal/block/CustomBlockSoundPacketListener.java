package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import org.bukkit.Location;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PacketEvents listener that intercepts outgoing sound and effect packets:
 * 1. Suppresses vanilla barrier / stone / wood / noteblock place sounds upon placement.
 * 2. Suppresses vanilla Effect 2001 (PARTICLES_DESTROY_BLOCK) and wood/noteblock break sounds upon custom block breaking.
 */
public class CustomBlockSoundPacketListener implements PacketListener {

    // Store recent custom block placement block coordinates for double-sound suppression (block X, Y, Z packed)
    private final Set<Long> recentPlacementLocations = ConcurrentHashMap.newKeySet();
    // Store recent custom block break block coordinates for vanilla break sound & effect suppression
    private final Set<Long> recentBreakLocations = ConcurrentHashMap.newKeySet();
    // Store allowed custom break sound identifiers per break position (e.g. if custom block uses a wood break sound)
    private final ConcurrentHashMap<Long, String> allowedBreakSounds = new ConcurrentHashMap<>();

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

    public void markRecentBreak(Location loc) {
        markRecentBreak(loc, null);
    }

    public void markRecentBreak(Location loc, String allowedSoundKey) {
        if (loc == null) return;
        markRecentBreak(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), allowedSoundKey);
    }

    public void markRecentBreak(int x, int y, int z) {
        markRecentBreak(x, y, z, null);
    }

    public void markRecentBreak(int x, int y, int z, String allowedSoundKey) {
        long packed = packBlockPos(x, y, z);
        recentBreakLocations.add(packed);
        if (allowedSoundKey != null && !allowedSoundKey.isBlank()) {
            allowedBreakSounds.put(packed, allowedSoundKey.toLowerCase());
        }
    }

    public void clearRecentBreak(int x, int y, int z) {
        long packed = packBlockPos(x, y, z);
        recentBreakLocations.remove(packed);
        allowedBreakSounds.remove(packed);
    }

    public boolean isRecentBreak(int x, int y, int z) {
        return recentBreakLocations.contains(packBlockPos(x, y, z));
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event == null) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.EFFECT) {
            try {
                WrapperPlayServerEffect effect = new WrapperPlayServerEffect(event);
                // Effect 2001 is PARTICLES_DESTROY_BLOCK (plays block break sound + particles)
                if (effect.getType() == 2001) {
                    Vector3i pos = effect.getPosition();
                    if (pos != null && isRecentBreakNearby(pos.getX(), pos.getY(), pos.getZ())) {
                        event.setCancelled(true);
                    }
                }
            } catch (Throwable ignored) {}
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SOUND_EFFECT) {
            try {
                WrapperPlayServerSoundEffect packet = new WrapperPlayServerSoundEffect(event);
                handleSoundEffectPacket(event, packet);
            } catch (Throwable ignored) {}
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND_EFFECT) {
            try {
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect packet =
                        new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect(event);
                handleEntitySoundEffectPacket(event, packet);
            } catch (Throwable ignored) {}
        }
    }

    public void handleEntitySoundEffectPacket(PacketSendEvent event, com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect packet) {
        if (event == null || packet == null) return;
        Sound sound = packet.getSound();
        String soundName = extractSoundName(sound);
        if (soundName != null) {
            soundName = soundName.toLowerCase();
            if (soundName.contains("wood") || soundName.contains("note_block")) {
                if (!recentBreakLocations.isEmpty()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public void handleSoundEffectPacket(PacketSendEvent event, WrapperPlayServerSoundEffect packet) {
        if (event == null || packet == null) return;
        com.github.retrooper.packetevents.util.Vector3d pos = packet.getPosition();
        if (pos == null) return;

        int blockX = (int) Math.floor(pos.getX());
        int blockY = (int) Math.floor(pos.getY());
        int blockZ = (int) Math.floor(pos.getZ());

        Sound sound = packet.getSound();
        String soundName = extractSoundName(sound);
        if (soundName != null) {
            soundName = soundName.toLowerCase();

            // Suppress vanilla place sounds at recent placement locations
            if (isRecentPlacement(blockX, blockY, blockZ)) {
                if (soundName.contains("barrier")
                        || (soundName.contains("stone") && soundName.contains("place"))
                        || (soundName.contains("wood") && soundName.contains("place"))
                        || soundName.contains("note_block")) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Suppress vanilla NoteBlock / wood sounds at recent break locations (tolerance of 1 block)
            if (isRecentBreakNearby(blockX, blockY, blockZ)) {
                if (isAllowedBreakSound(blockX, blockY, blockZ, soundName)) {
                    return; // Explicitly allowed custom sound for this broken block
                }
                if (soundName.contains("wood") || soundName.contains("note_block")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean isAllowedBreakSound(int x, int y, int z, String soundName) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    String allowed = allowedBreakSounds.get(packBlockPos(x + dx, y + dy, z + dz));
                    if (allowed != null && soundName.contains(allowed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String extractSoundName(Sound sound) {
        if (sound == null) return null;
        if (sound.getName() != null) {
            return sound.getName().toString();
        }
        if (sound.getSoundId() != null) {
            return sound.getSoundId().toString();
        }
        return sound.toString();
    }

    public boolean isRecentBreakNearby(int x, int y, int z) {
        if (isRecentBreak(x, y, z)) return true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (isRecentBreak(x + dx, y + dy, z + dz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
