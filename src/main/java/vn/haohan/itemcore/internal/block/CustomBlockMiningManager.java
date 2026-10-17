package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import org.bukkit.Bukkit;
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
import vn.haohan.itemcore.internal.block.mining.NativeMiningStrategy;

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

    public static final int CONSECUTIVE_MINING_DELAY_TICKS = 5;

    private final CustomBlockService customBlockService;
    private final VirtualEntityIdManager idManager;
    private final BlockBreakOverlayRenderer overlayRenderer;
    private final Plugin plugin;
    private final NativeMiningStrategy nativeStrategy;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, BlockMiningSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> consecutiveCooldowns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastToolWarningTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> activeMiningLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> playerHeldSlots = new ConcurrentHashMap<>();
    private AnimationObserver animationObserver;

    public CustomBlockMiningManager(
            CustomBlockService customBlockService,
            VirtualEntityIdManager idManager,
            BlockBreakOverlayRenderer overlayRenderer,
            NativeMiningStrategy nativeStrategy,
            Plugin plugin) {
        this.customBlockService = Objects.requireNonNull(customBlockService, "customBlockService cannot be null");
        this.idManager = idManager;
        this.overlayRenderer = overlayRenderer;
        this.nativeStrategy = nativeStrategy != null ? nativeStrategy : new NativeMiningStrategy(plugin);
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger(CustomBlockMiningManager.class.getName());
    }

    public CustomBlockMiningManager(
            CustomBlockService customBlockService,
            VirtualEntityIdManager idManager,
            BlockBreakOverlayRenderer overlayRenderer,
            Plugin plugin) {
        this(customBlockService, idManager, overlayRenderer, new NativeMiningStrategy(plugin), plugin);
    }

    public CustomBlockMiningManager(CustomBlockService customBlockService, Plugin plugin) {
        this(customBlockService, null, null, new NativeMiningStrategy(plugin), plugin);
    }

    public NativeMiningStrategy getNativeStrategy() {
        return nativeStrategy;
    }

    public void setAnimationObserver(AnimationObserver observer) {
        this.animationObserver = observer;
    }

    public boolean hasActiveSession(UUID playerUuid) {
        BlockMiningSession session = sessions.get(playerUuid);
        return session != null && session.getDefinition().isEffectiveTool(session.getPlayer().getInventory().getItemInMainHand());
    }

    public void cancelSession(UUID playerUuid) {
        consecutiveCooldowns.remove(playerUuid);
        AdjacentNoteBlockUpdateHelper.clearPlayer(playerUuid);
        BlockMiningSession session = sessions.remove(playerUuid);
        Player player = null;
        if (session != null) {
            player = session.getPlayer();
            sendBreakAnimation(player, session.getBlockLocation(), -1);
            if (overlayRenderer != null && session.getCrackEntityId() != -1) {
                destroyCrackOverlay(player, session.getBlockLocation(), session.getCrackEntityId());
            }
        }
        if (player == null && playerUuid != null) {
            try {
                player = Bukkit.getPlayer(playerUuid);
            } catch (Throwable ignored) {}
        }
        if (player != null && nativeStrategy != null) {
            nativeStrategy.removeModifier(player);
        }
        if (playerUuid != null) {
            lastToolWarningTimes.remove(playerUuid);
            activeMiningLocations.remove(playerUuid);
            playerHeldSlots.remove(playerUuid);
        }
    }

    public void clearAllSessions() {
        consecutiveCooldowns.clear();
        lastToolWarningTimes.clear();
        activeMiningLocations.clear();
        playerHeldSlots.clear();
        for (BlockMiningSession session : sessions.values()) {
            sendBreakAnimation(session.getPlayer(), session.getBlockLocation(), -1);
            if (overlayRenderer != null && session.getCrackEntityId() != -1) {
                destroyCrackOverlay(session.getPlayer(), session.getBlockLocation(), session.getCrackEntityId());
            }
        }
        sessions.clear();
        if (nativeStrategy != null) {
            try {
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    nativeStrategy.removeModifier(player);
                }
            } catch (Throwable ignored) {}
        }
    }

    public ItemStack getPlayerTool(Player player) {
        if (player == null) return null;
        Integer currentSlot = playerHeldSlots.get(player.getUniqueId());
        if (currentSlot != null && currentSlot >= 0 && currentSlot <= 8) {
            try {
                ItemStack item = player.getInventory().getItem(currentSlot);
                if (item != null) return item;
            } catch (Throwable ignored) {}
        }
        return player.getInventory().getItemInMainHand();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            Object rawPlayer = event.getPlayer();
            if (!(rawPlayer instanceof Player player)) {
                return;
            }

            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getSequence() >= 0) {
                AdjacentNoteBlockUpdateHelper.recordPendingAction(player.getUniqueId(), packet.getSequence());
            }
            Vector3i blockPos = packet.getBlockPosition();
            if (blockPos == null) return;

            Location loc = new Location(player.getWorld(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            Block block = loc.getBlock();

            // Reject at packet layer if digging custom block with ineffective tool or unbreakable block
            if (player.getGameMode() != GameMode.CREATIVE && customBlockService.isCustomBlock(block)) {
                Optional<CustomBlockDefinition> optDef = customBlockService.getCustomBlock(block);
                if (optDef.isPresent()) {
                    CustomBlockDefinition def = optDef.get();
                    ItemStack tool = getPlayerTool(player);
                    boolean ineffective = def.isUnbreakable() || !def.isEffectiveTool(tool);

                    if (ineffective && (packet.getAction() == DiggingAction.START_DIGGING || packet.getAction() == DiggingAction.FINISHED_DIGGING)) {
                        event.setCancelled(true);

                        if (packet.getSequence() >= 0) {
                            NoteBlockEventListener.acknowledgeBlockChange(player, packet.getSequence());
                        }

                        try {
                            player.sendBlockChange(loc, block.getBlockData());
                        } catch (Throwable ignored) {}

                        long now = System.currentTimeMillis();
                        Long lastWarn = lastToolWarningTimes.get(player.getUniqueId());
                        if (lastWarn == null || now - lastWarn >= 1500L) {
                            lastToolWarningTimes.put(player.getUniqueId(), now);
                            sendToolRequirementWarning(player, def);
                        }

                        if (def.isNative() || def.isVanillaOverwrite()) {
                            if (nativeStrategy != null) {
                                nativeStrategy.onStartDigging(player, block, def, tool);
                            }
                        }
                        return;
                    }
                }
            }

            handleDiggingAction(player, packet.getAction(), blockPos, loc);
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            Object rawPlayer = event.getPlayer();
            if (!(rawPlayer instanceof Player player)) {
                return;
            }

            try {
                com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement packet =
                        new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement(event);
                AdjacentNoteBlockUpdateHelper.recordPendingPlacement(player.getUniqueId(), packet.getSequence());
            } catch (Throwable ignored) {}
        } else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            Object rawPlayer = event.getPlayer();
            if (!(rawPlayer instanceof Player player)) {
                return;
            }

            com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange packet =
                    new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange(event);
            int newSlot = packet.getSlot();
            if (newSlot >= 0 && newSlot <= 8) {
                playerHeldSlots.put(player.getUniqueId(), newSlot);
                ItemStack newTool = player.getInventory().getItem(newSlot);
                handleItemSwap(player, newTool);
            }
        }
    }

    /**
     * Handles tool swapping while mining a custom block.
     * If the player swaps to an effective tool, upgrades the session to progressive mining.
     * If the player swaps away to an ineffective tool, pauses progress and destroys overlay.
     */
    public void handleItemSwap(Player player, ItemStack newTool) {
        if (player == null) return;

        Location activeLoc = activeMiningLocations.get(player.getUniqueId());
        if (activeLoc != null && activeLoc.getWorld() != null && activeLoc.getWorld().equals(player.getWorld())) {
            Block block = activeLoc.getBlock();
            Optional<CustomBlockDefinition> optDef = customBlockService.getCustomBlock(block);
            if (optDef.isPresent()) {
                CustomBlockDefinition def = optDef.get();
                if (def.isNative() || def.isVanillaOverwrite()) {
                    try {
                        player.sendBlockChange(activeLoc, block.getBlockData());
                    } catch (Throwable ignored) {}

                    if (nativeStrategy != null) {
                        nativeStrategy.onStartDigging(player, block, def, newTool);
                    }
                }
            }
        }

        BlockMiningSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        CustomBlockDefinition def = session.getDefinition();
        boolean nowEffective = def.isEffectiveTool(newTool);

        if (nowEffective) {
            session.resetInvalidToolState();
            if (session.getCrackEntityId() == -1) {
                int crackEntityId = -1;
                if (!def.isVanillaOverwrite() && idManager != null) {
                    crackEntityId = idManager.allocateRawId();
                }
                session.setCrackEntityId(crackEntityId);
                if (!def.isVanillaOverwrite() && overlayRenderer != null && crackEntityId != -1) {
                    int stage = Math.min(9, (int) (session.getProgress() * 9.0f));
                    spawnCrackOverlay(player, session.getBlockLocation(), crackEntityId, stage);
                }
            }
        } else {
            if (overlayRenderer != null && session.getCrackEntityId() != -1) {
                destroyCrackOverlay(player, session.getBlockLocation(), session.getCrackEntityId());
                session.setCrackEntityId(-1);
            }
        }
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

            ItemStack tool = getPlayerTool(player);
            activeMiningLocations.put(player.getUniqueId(), loc);

            if (def.isNative() || def.isVanillaOverwrite()) {
                if (!def.isEffectiveTool(tool)) {
                    long now = System.currentTimeMillis();
                    Long lastWarn = lastToolWarningTimes.get(player.getUniqueId());
                    if (lastWarn == null || now - lastWarn >= 1500L) {
                        lastToolWarningTimes.put(player.getUniqueId(), now);
                        sendToolRequirementWarning(player, def);
                    }
                }
                if (nativeStrategy != null) {
                    nativeStrategy.onStartDigging(player, block, def, tool);
                }
                return;
            }

            if (def.isUnbreakable()) {
                return; // Unbreakable, cannot be mined
            }

            boolean effectiveTool = def.isEffectiveTool(tool);

            // Calculate consecutive delay if player previously broke a block in continuous mining
            int delayTicks = 0;
            Long cooldownUntil = consecutiveCooldowns.remove(player.getUniqueId());
            if (cooldownUntil != null) {
                long now = System.currentTimeMillis();
                long remainingMillis = cooldownUntil - now;
                if (remainingMillis > 0) {
                    delayTicks = (int) Math.min(CONSECUTIVE_MINING_DELAY_TICKS, Math.ceil(remainingMillis / 50.0));
                }
            }

            if (!effectiveTool) {
                // If tool is not effective, track session to give warning after 0.75s (15 ticks)
                BlockMiningSession session = new BlockMiningSession(player, loc, def, -1);
                session.setDelayTicksRemaining(delayTicks);
                sessions.put(player.getUniqueId(), session);
                return;
            }

            if (def.isInstantBreak() && delayTicks == 0) {
                breakCustomBlock(player, loc, def);
                return;
            }

            // Start progressive mining session
            int crackEntityId = -1;
            // Only spawn crack overlay for barrier custom blocks, not vanilla overwrite blocks
            if (!def.isVanillaOverwrite() && idManager != null) {
                crackEntityId = idManager.allocateRawId();
            }
            BlockMiningSession session = new BlockMiningSession(player, loc, def, crackEntityId);
            session.setDelayTicksRemaining(delayTicks);
            sessions.put(player.getUniqueId(), session);

            if (!def.isVanillaOverwrite() && overlayRenderer != null && crackEntityId != -1) {
                spawnCrackOverlay(player, loc, crackEntityId, 0);
            }

        } else if (action == DiggingAction.CANCELLED_DIGGING) {
            activeMiningLocations.remove(player.getUniqueId());
            Block block = loc.getBlock();
            Optional<CustomBlockDefinition> optDef = customBlockService.getCustomBlock(block);
            if (optDef.isPresent()) {
                CustomBlockDefinition def = optDef.get();
                if (def.isNative() || def.isVanillaOverwrite()) {
                    if (nativeStrategy != null) {
                        nativeStrategy.onStopDigging(player, block, def);
                    }
                    return;
                }
            }
            cancelSession(player.getUniqueId());
        } else if (action == DiggingAction.FINISHED_DIGGING) {
            activeMiningLocations.remove(player.getUniqueId());
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
                if (overlayRenderer != null && session.getCrackEntityId() != -1) {
                    destroyCrackOverlay(player, loc, session.getCrackEntityId());
                }
                continue;
            }

            ItemStack tool = player.getInventory().getItemInMainHand();
            boolean effectiveTool = def.isEffectiveTool(tool);

            // Handle spontaneous tool switch if packet arrived or item changed in hand
            if (effectiveTool && session.getCrackEntityId() == -1 && !def.isVanillaOverwrite() && idManager != null) {
                int crackEntityId = idManager.allocateRawId();
                session.setCrackEntityId(crackEntityId);
                session.resetInvalidToolState();
                if (overlayRenderer != null) {
                    spawnCrackOverlay(player, loc, crackEntityId, session.getLastStage() >= 0 ? session.getLastStage() : 0);
                }
            }

            if (!effectiveTool) {
                int invalidTicks = session.incrementAndGetInvalidToolTicks();
                if (invalidTicks >= 15 && !session.isWarnedInvalidTool()) {
                    session.setWarnedInvalidTool(true);
                    sendToolRequirementWarning(player, def);
                }
                // Do not progress mining for invalid tool
                continue;
            }

            // Decrement consecutive mining delay if any
            if (session.decrementDelayTicks()) {
                continue;
            }

            float delta = MiningSpeedCalculator.calculateProgressPerTick(player, tool, def);
            session.addProgress(delta);

            // Play mining hit sound every 4 ticks (~0.2s like vanilla mining) for barrier custom blocks
            if (!def.isVanillaOverwrite() && session.incrementAndGetMiningTicks() % 4 == 0) {
                if (def.getSoundGroup() != null && def.getSoundGroup().getHitSound() != null) {
                    def.getSoundGroup().getHitSound().play(loc.clone().add(0.5, 0.5, 0.5));
                }
            }

            int stage = Math.min(9, (int) (session.getProgress() * 9.0f));
            if (stage != session.getLastStage()) {
                session.setLastStage(stage);
                sendBreakAnimation(player, loc, stage);
                if (overlayRenderer != null && session.getCrackEntityId() != -1) {
                    updateCrackOverlay(player, loc, session.getCrackEntityId(), stage);
                }
            }

            if (session.getProgress() >= 1.0f) {
                it.remove();
                sendBreakAnimation(player, loc, -1);
                if (overlayRenderer != null && session.getCrackEntityId() != -1) {
                    destroyCrackOverlay(player, loc, session.getCrackEntityId());
                }
                // Record consecutive mining delay for continuous mining next blocks (5 ticks = 250ms)
                consecutiveCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (CONSECUTIVE_MINING_DELAY_TICKS * 50L));
                breakCustomBlock(player, loc, def);
            }
        }
    }

    private void breakCustomBlock(Player player, Location loc, CustomBlockDefinition def) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        MiningContext context = new MiningContext(player, loc.getBlock(), def, tool);

        boolean canHarvest = def.getMiningCondition().canHarvest(context);
        if (canHarvest) {
            boolean useVanillaDrops = def.isVanillaOverwrite() && def.getDropItemId() == null;
            if (useVanillaDrops) {
                Block block = loc.getBlock();
                if (loc.getWorld() != null && block != null) {
                    java.util.Collection<ItemStack> vanillaDrops = block.getDrops(tool, player);
                    if (vanillaDrops != null) {
                        for (ItemStack drop : vanillaDrops) {
                            if (drop != null) {
                                loc.getWorld().dropItemNaturally(loc, drop);
                            }
                        }
                    }
                }
            } else {
                List<ItemStack> drops = def.getDropProvider().getDrops(context);
                if (drops != null && loc.getWorld() != null) {
                    for (ItemStack drop : drops) {
                        if (drop != null) {
                            loc.getWorld().dropItemNaturally(loc, drop);
                        }
                    }
                }
            }
        }

        // Sounds and particles
        if (loc.getWorld() != null) {
            try {
                if (def.getSoundGroup() != null && def.getSoundGroup().getBreakSound() != null) {
                    def.getSoundGroup().getBreakSound().play(loc.clone().add(0.5, 0.5, 0.5));
                } else {
                    loc.getWorld().playSound(loc.clone().add(0.5, 0.5, 0.5), Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);
                }
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

    private void spawnCrackOverlay(Player player, Location loc, int crackEntityId, int stage) {
        overlayRenderer.spawnCrack(player, crackEntityId, loc, stage);
        if (loc.getWorld() != null) {
            for (Player other : loc.getWorld().getPlayers()) {
                if (other.equals(player)) continue;
                if (other.getLocation().distanceSquared(loc) <= 1024) {
                    overlayRenderer.spawnCrack(other, crackEntityId, loc, stage);
                }
            }
        }
    }

    private void updateCrackOverlay(Player player, Location loc, int crackEntityId, int stage) {
        overlayRenderer.updateCrack(player, crackEntityId, stage);
        if (loc.getWorld() != null) {
            for (Player other : loc.getWorld().getPlayers()) {
                if (other.equals(player)) continue;
                if (other.getLocation().distanceSquared(loc) <= 1024) {
                    overlayRenderer.updateCrack(other, crackEntityId, stage);
                }
            }
        }
    }

    private void destroyCrackOverlay(Player player, Location loc, int crackEntityId) {
        overlayRenderer.destroyCrack(player, crackEntityId);
        if (loc.getWorld() != null) {
            for (Player other : loc.getWorld().getPlayers()) {
                if (other.equals(player)) continue;
                if (other.getLocation().distanceSquared(loc) <= 1024) {
                    overlayRenderer.destroyCrack(other, crackEntityId);
                }
            }
        }
    }

    public void sendToolRequirementWarning(Player player, CustomBlockDefinition def) {
        if (player == null || def == null) return;
        StringBuilder sb = new StringBuilder("§c[!] Bạn cần ");
        if (def.getRequiredTier() != null) {
            sb.append("công cụ cấp độ §e").append(def.getRequiredTier().name().toUpperCase()).append("§c ");
        }
        if (!def.getHarvestToolTypes().isEmpty()) {
            sb.append("thuộc loại §e").append(def.getHarvestToolTypes()).append("§c ");
        }
        sb.append("để khai thác khối này!");

        player.sendMessage(sb.toString());
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        } catch (Throwable ignored) {}
    }
}
