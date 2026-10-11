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
    private int crackEntityId;
    private int delayTicksRemaining = 0;
    private float progress = 0.0f;
    private int lastStage = -1;
    private int miningTicks = 0;
    private int invalidToolTicks = 0;
    private boolean warnedInvalidTool = false;

    public BlockMiningSession(Player player, Location blockLocation, CustomBlockDefinition definition, int crackEntityId) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.blockLocation = Objects.requireNonNull(blockLocation, "blockLocation cannot be null");
        this.definition = Objects.requireNonNull(definition, "definition cannot be null");
        this.crackEntityId = crackEntityId;
    }

    public BlockMiningSession(Player player, Location blockLocation, CustomBlockDefinition definition) {
        this(player, blockLocation, definition, -1);
    }

    public int getCrackEntityId() {
        return crackEntityId;
    }

    public void setCrackEntityId(int crackEntityId) {
        this.crackEntityId = crackEntityId;
    }

    public int getDelayTicksRemaining() {
        return delayTicksRemaining;
    }

    public void setDelayTicksRemaining(int delayTicksRemaining) {
        this.delayTicksRemaining = Math.max(0, delayTicksRemaining);
    }

    public boolean decrementDelayTicks() {
        if (delayTicksRemaining > 0) {
            delayTicksRemaining--;
            return true;
        }
        return false;
    }

    public void resetInvalidToolState() {
        this.invalidToolTicks = 0;
        this.warnedInvalidTool = false;
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

    public int incrementAndGetMiningTicks() {
        return ++this.miningTicks;
    }

    public int getLastStage() {
        return lastStage;
    }

    public void setLastStage(int lastStage) {
        this.lastStage = lastStage;
    }

    public int incrementAndGetInvalidToolTicks() {
        return ++this.invalidToolTicks;
    }

    public boolean isWarnedInvalidTool() {
        return warnedInvalidTool;
    }

    public void setWarnedInvalidTool(boolean warnedInvalidTool) {
        this.warnedInvalidTool = warnedInvalidTool;
    }
}
