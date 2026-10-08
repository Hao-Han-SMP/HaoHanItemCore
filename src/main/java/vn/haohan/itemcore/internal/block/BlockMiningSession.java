package vn.haohan.itemcore.internal.block;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;

import java.util.Objects;

/**
 * Encapsulates the state of an active mining session by a player on a custom block.
 */
public final class BlockMiningSession {

    private final Player player;
    private final Location blockLocation;
    private final CustomBlockDefinition definition;
    private float progress = 0.0f;
    private int lastStage = -1;

    public BlockMiningSession(Player player, Location blockLocation, CustomBlockDefinition definition) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.blockLocation = Objects.requireNonNull(blockLocation, "blockLocation cannot be null");
        this.definition = Objects.requireNonNull(definition, "definition cannot be null");
    }

    public Player getPlayer() {
        return player;
    }

    public Location getBlockLocation() {
        return blockLocation;
    }

    public CustomBlockDefinition getDefinition() {
        return definition;
    }

    public float getProgress() {
        return progress;
    }

    public void addProgress(float delta) {
        this.progress += delta;
    }

    public int getLastStage() {
        return lastStage;
    }

    public void setLastStage(int lastStage) {
        this.lastStage = lastStage;
    }
}
