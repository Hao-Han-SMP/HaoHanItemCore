package vn.haohan.itemcore.api.block;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Represents a single sound action with identifier, volume and pitch.
 */
public record CustomBlockSound(String soundKey, float volume, float pitch) {

    public CustomBlockSound {
        if (soundKey == null || soundKey.isBlank()) {
            soundKey = "minecraft:block.stone.break";
        }
    }

    public static CustomBlockSound of(String soundKey) {
        return new CustomBlockSound(soundKey, 1.0f, 1.0f);
    }

    public static CustomBlockSound of(String soundKey, float volume, float pitch) {
        return new CustomBlockSound(soundKey, volume, pitch);
    }

    public static CustomBlockSound of(Sound sound, float volume, float pitch) {
        return new CustomBlockSound(sound != null ? sound.getKey().toString() : "minecraft:block.stone.break", volume, pitch);
    }

    /**
     * Plays this sound at the given location for all nearby players.
     */
    public void play(Location loc) {
        if (loc == null || loc.getWorld() == null || soundKey.isBlank() || volume <= 0.0f) {
            return;
        }
        try {
            // First check if it matches a Bukkit Sound enum directly
            String rawEnumName = soundKey.toUpperCase(Locale.ROOT).replace("MINECRAFT:", "").replace(".", "_");
            try {
                if (org.bukkit.Bukkit.getServer() != null) {
                    Sound sound = Sound.valueOf(rawEnumName);
                    loc.getWorld().playSound(loc, sound, org.bukkit.SoundCategory.BLOCKS, volume, pitch);
                    return;
                }
            } catch (Throwable ignored) {}

            // Otherwise, play via adventure key / custom string sound
            loc.getWorld().playSound(loc, soundKey, org.bukkit.SoundCategory.BLOCKS, volume, pitch);
        } catch (Throwable ignored) {}
    }

    /**
     * Plays this sound specifically for one player.
     */
    public void play(Player player, Location loc) {
        if (player == null || !player.isOnline() || loc == null || soundKey.isBlank() || volume <= 0.0f) {
            return;
        }
        try {
            String rawEnumName = soundKey.toUpperCase(Locale.ROOT).replace("MINECRAFT:", "").replace(".", "_");
            try {
                if (org.bukkit.Bukkit.getServer() != null) {
                    Sound sound = Sound.valueOf(rawEnumName);
                    player.playSound(loc, sound, volume, pitch);
                    return;
                }
            } catch (Throwable ignored) {}

            player.playSound(loc, soundKey, org.bukkit.SoundCategory.BLOCKS, volume, pitch);
        } catch (Throwable ignored) {}
    }
}
