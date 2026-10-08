package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockService;
import vn.haohan.itemcore.api.block.MiningContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages Survival mode custom block mining sessions and broadcasts
 * PacketEvents client-side break animations.
 */
public class CustomBlockMiningManager implements PacketListener {

    @FunctionalInterface
    public interface AnimationObserver {
        void onAnimation(Player player, Location location, int stage);
    }

    private final CustomBlockService customBlockService;
    private final Plugin plugin;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, BlockMiningSession> sessions = new ConcurrentHashMap<>();
    private AnimationObserver animationObserver;

    public CustomBlockMiningManager(CustomBlockService customBlockService, Plugin plugin) {
        this.customBlockService = Objects.requireNonNull(customBlockService, "customBlockService cannot be null");
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger(CustomBlockMiningManager.class.getName());
    }

    public void setAnimationObserver(AnimationObserver observer) {
        this.animationObserver = observer;
    }

    public boolean hasActiveSession(UUID playerUuid) {
        return sessions.containsKey(playerUuid);
    }

    public void cancelSession(UUID playerUuid) {
        BlockMiningSession session = sessions.remove(playerUuid);
        if (session != null) {
            sendBreakAnimation(session.getPlayer(), session.getBlockLocation(), -1);
        }
    }

    public void clearAllSessions() {
        for (BlockMiningSession session : sessions.values()) {
            sendBreakAnimation(session.getPlayer(), session.getBlockLocation(), -1);
        }
        sessions.clear();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) {
            return;
        }

        Object rawPlayer = event.getPlayer();
        if (!(rawPlayer instanceof Player player)) {
            return;
        }

        WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        Vector3i blockPos = packet.getBlockPosition();
        if (blockPos == null) return;

        Location loc = new Location(player.getWorld(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
        handleDiggingAction(player, packet.getAction(), blockPos, loc);
    }

    public void handleDiggingAction(Player player, DiggingAction action, Vector3i blockPos, Location loc) {
        if (action == DiggingAction.START_DIGGING) {
            Block block = loc.getBlock();
            if (!customBlockService.isCustomBlock(block)) {
                return;
            }

            Optional<CustomBlockDefinition> optDef = customBlockService.getCustomBlock(block);
            if (optDef.isEmpty()) return;
            CustomBlockDefinition def = optDef.get();

            if (player.getGameMode() == GameMode.CREATIVE) {
                return; // Let standard Bukkit BlockBreakEvent handle creative breaking
            }

            if (def.isUnbreakable()) {
                return; // Unbreakable, cannot be mined
            }

            if (def.isInstantBreak()) {
                breakCustomBlock(player, loc, def);
                return;
            }

            // Start progressive mining session
            sessions.put(player.getUniqueId(), new BlockMiningSession(player, loc, def));

        } else if (action == DiggingAction.CANCELLED_DIGGING) {
            cancelSession(player.getUniqueId());
        }
    }

    /**
     * Ticks active mining sessions; should be called once every server tick.
     */
    public void tick() {
        Iterator<Map.Entry<UUID, BlockMiningSession>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BlockMiningSession> entry = it.next();
            BlockMiningSession session = entry.getValue();
            Player player = session.getPlayer();
            Location loc = session.getBlockLocation();
            CustomBlockDefinition def = session.getDefinition();

            // Sanity validation
            if (!player.isOnline()
                    || !player.getWorld().equals(loc.getWorld())
                    || player.getLocation().distanceSquared(loc) > 36.0
                    || !customBlockService.isCustomBlock(loc.getBlock())) {
                it.remove();
                sendBreakAnimation(player, loc, -1);
                continue;
            }

            ItemStack tool = player.getInventory().getItemInMainHand();
            float delta = MiningSpeedCalculator.calculateProgressPerTick(player, tool, def);
            session.addProgress(delta);

            int stage = Math.min(9, (int) (session.getProgress() * 9.0f));
            if (stage != session.getLastStage()) {
                session.setLastStage(stage);
                sendBreakAnimation(player, loc, stage);
            }

            if (session.getProgress() >= 1.0f) {
                it.remove();
                sendBreakAnimation(player, loc, -1);
                breakCustomBlock(player, loc, def);
            }
        }
    }

    private void breakCustomBlock(Player player, Location loc, CustomBlockDefinition def) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        MiningContext context = new MiningContext(player, loc.getBlock(), def, tool);

        boolean canHarvest = def.getMiningCondition().canHarvest(context);
        if (canHarvest) {
            List<ItemStack> drops = def.getDropProvider().getDrops(context);
            if (drops != null && loc.getWorld() != null) {
                for (ItemStack drop : drops) {
                    if (drop != null) {
                        loc.getWorld().dropItemNaturally(loc, drop);
                    }
                }
            }
        }

        // Sounds and particles
        if (loc.getWorld() != null) {
            try {
                loc.getWorld().playSound(loc.clone().add(0.5, 0.5, 0.5), Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);
                if (def.getModel() != null && def.getModel().getBlockData() != null) {
                    loc.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, loc.clone().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, def.getModel().getBlockData());
                }
            } catch (Throwable ignored) {}
        }

        // Tool damage (if tool is damageable and player not in Creative)
        if (tool != null && player.getGameMode() != GameMode.CREATIVE) {
            try {
                if (tool.hasItemMeta() && tool.getItemMeta() instanceof Damageable) {
                    tool.damage(1, player);
                }
            } catch (Throwable ignored) {}
        }

        customBlockService.removeCustomBlock(loc);
    }

    public void sendBreakAnimation(Player player, Location loc, int stage) {
        if (animationObserver != null) {
            animationObserver.onAnimation(player, loc, stage);
        }

        if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded() && player.isOnline()) {
            try {
                Vector3i pos = new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                WrapperPlayServerBlockBreakAnimation packet = new WrapperPlayServerBlockBreakAnimation(
                        player.getEntityId(), pos, (byte) stage
                );
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);

                if (loc.getWorld() != null) {
                    for (Player other : loc.getWorld().getPlayers()) {
                        if (other.equals(player)) continue;
                        if (other.getLocation().distanceSquared(loc) <= 1024) {
                            PacketEvents.getAPI().getPlayerManager().sendPacket(other, packet);
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to send BlockBreakAnimation packet for player " + player.getName(), e);
            }
        }
    }
}
