package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.CustomBlockInteractContext;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.api.block.CustomBlockSound;
import vn.haohan.itemcore.api.block.CustomBlockSoundGroup;
import vn.haohan.itemcore.api.block.NativeCustomBlock;

import java.util.*;

/**
 * Event listener intercepting NoteBlock interactions, physics updates, piston movements,
 * creative middle-click pick block, explosions, fire ignition, and sound emissions for native custom blocks.
 *
 * <p>Full feature set ported from Oraxen's NoteBlockMechanicListener & NoteBlockSoundListener,
 * adapted to Paper 1.21 API and PacketEvents without direct NMS dependencies.</p>
 */
public final class NoteBlockEventListener implements Listener {

    private final CustomBlockRegistry registry;
    private final Plugin plugin;

    private static final Map<Instrument, List<String>> INSTRUMENT_MAP = createInstrumentMap();

    public NoteBlockEventListener(CustomBlockRegistry registry, Plugin plugin) {
        this.registry = Objects.requireNonNull(registry, "CustomBlockRegistry cannot be null");
        this.plugin = plugin;
    }

    /**
     * Retrieves the {@link NativeCustomBlock} definition for the given block if it represents one.
     */
    public Optional<NativeCustomBlock> getNativeCustomBlock(Block block) {
        if (block == null || block.getType() != Material.NOTE_BLOCK) {
            return Optional.empty();
        }
        if (!(block.getBlockData() instanceof NoteBlock noteBlock)) {
            return Optional.empty();
        }
        int variation = NoteBlockVariationTable.resolveVariation(noteBlock);
        if (variation < 0) {
            return Optional.empty();
        }
        return registry.getByVariation(variation);
    }

    /**
     * Checks whether the specified block is registered as a native NoteBlock custom block.
     */
    public boolean isNativeBlock(Block block) {
        return getNativeCustomBlock(block).isPresent();
    }

    /**
     * Checks if right-click note pitch tuning should be denied on the given block.
     */
    public boolean shouldDenyNoteTune(Block block) {
        return isNativeBlock(block);
    }

    /**
     * Resolves NativeCustomBlock definition from an ItemStack if it defines a custom variation.
     */
    public Optional<NativeCustomBlock> getNativeCustomBlockFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return Optional.empty();
        try {
            var core = HaoHanItemCore.get();
            if (core != null && core.getItemService() != null) {
                String id = core.getItemService().getId(item);
                if (id != null) {
                    var optDef = registry.get(id);
                    if (optDef.isPresent() && optDef.get() instanceof NativeCustomBlock nativeDef) {
                        return Optional.of(nativeDef);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    /**
     * Intercepts right-clicks on NoteBlock native custom blocks at LOWEST priority to deny vanilla pitch tuning,
     * while delegating to explicit custom block interact handlers if configured.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTuneNoteBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(block);
        if (optDef.isPresent()) {
            NativeCustomBlock def = optDef.get();
            if (def.getInteractHandler() != null) {
                try {
                    def.getInteractHandler().onInteract(new CustomBlockInteractContext(
                            event.getPlayer(),
                            block,
                            event.getItem(),
                            event.getHand() != null ? event.getHand() : EquipmentSlot.HAND,
                            event.getAction()
                    ));
                } catch (Throwable t) {
                    if (plugin != null) {
                        plugin.getLogger().warning("Error executing custom block interact action for "
                                + def.getId() + ": " + t.getMessage());
                    }
                }
            }
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    /**
     * Allows placing blocks against NoteBlock native custom blocks without note tuning glitches (ported from Oraxen).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceAgainstNoteBlock(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand();
        BlockFace blockFace = event.getBlockFace();

        if (hand == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.NOTE_BLOCK) return;

        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(block);
        if (optDef.isEmpty()) return;
        NativeCustomBlock def = optDef.get();

        if (!player.isSneaking() && def.getInteractHandler() != null) return;
        event.setUseInteractedBlock(Event.Result.DENY);

        if (item == null || item.getType() == Material.AIR) return;

        BlockData newData = null;
        Optional<NativeCustomBlock> itemNativeOpt = getNativeCustomBlockFromItem(item);
        if (itemNativeOpt.isPresent()) {
            NativeCustomBlock itemDef = itemNativeOpt.get();
            newData = NoteBlockVariationTable.createBlockData(itemDef.getCustomVariation());
        } else if (item.getType().isBlock()) {
            try {
                newData = item.getType().createBlockData();
            } catch (Throwable ignored) {}
        }

        if (newData != null) {
            makePlayerPlaceBlock(player, hand, item, block, blockFace, newData);
            Block target = isReplaceable(block.getType()) ? block : block.getRelative(blockFace);
            AdjacentNoteBlockUpdateHelper.finishAdjacentBlockChange(player, target, block.getLocation(), registry);
        }
    }

    /**
     * Prevents client prediction glitched states when placing a block next to a NoteBlock vertical neighbor (ported from Oraxen).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceVanillaBlockNextToCustomNoteBlock(PlayerInteractEvent event) {
        Block placedAgainst = event.getClickedBlock();
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || placedAgainst == null || item == null || hand == null) return;
        if (isNativeBlock(placedAgainst) || getNativeCustomBlockFromItem(item).isPresent()) return;
        if (!item.getType().isBlock()) return;

        Block target = isReplaceable(placedAgainst.getType())
                ? placedAgainst
                : placedAgainst.getRelative(event.getBlockFace());
        if (!isReplaceable(target.getType())) return;
        if (!AdjacentNoteBlockUpdateHelper.hasCustomVerticalNeighbor(target, registry)) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        makePlayerPlaceBlock(event.getPlayer(), hand, item, placedAgainst, event.getBlockFace(), item.getType().createBlockData());
        AdjacentNoteBlockUpdateHelper.finishAdjacentBlockChange(event.getPlayer(), target, placedAgainst.getLocation(), registry);
    }

    /**
     * Prevents placing pistons and shulker boxes directly above NoteBlock native custom blocks (ported from Oraxen).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlacingUnsupportedBlockAboveNoteBlock(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (!isUnsupportedBlockAboveNoteBlock(placed.getType())) return;
        if (isNativeBlock(placed.getRelative(BlockFace.DOWN))) {
            event.setCancelled(true);
        }
    }

    /**
     * Pick Block in Creative mode: Gives the player the correct custom item stack (ported from Oraxen).
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        if (!(event.getInventory().getHolder() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() != Material.NOTE_BLOCK) return;

        RayTraceResult rayTrace = player.rayTraceBlocks(6.0);
        if (rayTrace == null || rayTrace.getHitBlock() == null) return;

        Block block = rayTrace.getHitBlock();
        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(block);
        if (optDef.isEmpty()) return;
        NativeCustomBlock def = optDef.get();

        ItemStack customItem = null;
        try {
            var core = HaoHanItemCore.get();
            if (core != null && core.getItemFactory() != null) {
                customItem = core.getItemFactory().create(def.getId());
            }
        } catch (Throwable ignored) {}

        if (customItem == null) return;

        for (int i = 0; i <= 8; i++) {
            ItemStack slotItem = player.getInventory().getItem(i);
            if (slotItem == null) continue;
            try {
                var core = HaoHanItemCore.get();
                if (core != null && core.getItemService() != null && def.getId().equals(core.getItemService().getId(slotItem))) {
                    player.getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    return;
                }
            } catch (Throwable ignored) {}
        }
        event.getView().setCursor(customItem);
    }

    /**
     * Cancels note play on native custom blocks, while simulating accurate instruments for vanilla note blocks (ported from Oraxen).
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlay(NotePlayEvent event) {
        if (isNativeBlock(event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        if (event.getInstrument() != Instrument.PIANO) {
            return;
        }
        String blockType = event.getBlock().getRelative(BlockFace.DOWN).getType().toString().toLowerCase(Locale.ROOT);
        Instrument fakeInstrument = INSTRUMENT_MAP.entrySet().stream()
                .filter(e -> e.getValue().contains(blockType))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(Instrument.PIANO);
        event.setInstrument(fakeInstrument);
    }

    /**
     * Suppresses generic note block game events and restores snapshot if powered by redstone (ported from Oraxen).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGenericGameEvent(GenericGameEvent event) {
        if (event.getEvent() == GameEvent.NOTE_BLOCK_PLAY
                || (event.getEvent() != null && "note_block_play".equals(event.getEvent().getKey().getKey()))) {
            Location loc = event.getLocation();
            if (loc != null) {
                try {
                    if (loc.getWorld() != null && !loc.isChunkLoaded()) {
                        return;
                    }
                    Block block = loc.getBlock();
                    if (isNativeBlock(block)) {
                        event.setCancelled(true);
                        if (block.getBlockData() instanceof NoteBlock noteBlock) {
                            NoteBlock data = (NoteBlock) noteBlock.clone();
                            if (plugin != null && plugin.isEnabled()) {
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    if (block.getType() == Material.NOTE_BLOCK && !block.getBlockData().equals(data)) {
                                        block.setBlockData(data, false);
                                    }
                                }, 1L);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    /**
     * Prevents pistons from pushing native custom blocks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (isNativeBlock(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents pistons from pulling native custom blocks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (isNativeBlock(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Suppresses block physics updates on native custom blocks and vertical neighbors,
     * updating the entire vertical stack recursively (ported from Oraxen).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        Block below = block.getRelative(BlockFace.DOWN);

        if (isNativeBlock(below)) {
            event.setCancelled(true);
            updateAndCheck(below);
        } else if (isNativeBlock(above)) {
            event.setCancelled(true);
            updateAndCheck(above);
        }
        if (isNativeBlock(block)) {
            event.setCancelled(true);
            updateAndCheck(block);
        }
    }

    /**
     * Prevents igniting fire on top of native custom blocks with flint and steel or fire charges.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSetFire(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (block == null || block.getType() != Material.NOTE_BLOCK) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getBlockFace() != BlockFace.UP) return;
        if (item == null) return;

        if (isNativeBlock(block)) {
            if (item.getType() == Material.FLINT_AND_STEEL || item.getType() == Material.FIRE_CHARGE) {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

    /**
     * Prevents native custom blocks from catching fire.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatchFire(BlockIgniteEvent event) {
        Block block = event.getBlock();
        if (isNativeBlock(block)) {
            event.setCancelled(true);
        }
    }

    /**
     * Cleans up native custom blocks when destroyed by entity explosions.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    /**
     * Cleans up native custom blocks when destroyed by block explosions.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) return;
        List<Block> toRemove = new ArrayList<>();
        for (Block block : blocks) {
            if (isNativeBlock(block)) {
                toRemove.add(block);
                try {
                    var core = HaoHanItemCore.get();
                    if (core != null && core.getCustomBlockService() != null) {
                        core.getCustomBlockService().removeCustomBlock(block.getLocation());
                    } else {
                        block.setType(Material.AIR);
                    }
                } catch (Throwable ignored) {
                    block.setType(Material.AIR);
                }
            }
        }
        blocks.removeAll(toRemove);
    }

    /**
     * Plays authentic custom block step and fall sounds instead of vanilla wood sounds (ported from Oraxen NoteBlockSoundListener).
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onStepFall(GenericGameEvent event) {
        GameEvent gameEvent = event.getEvent();
        if (gameEvent != GameEvent.STEP && gameEvent != GameEvent.HIT_GROUND) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return;

        Location eventLoc = event.getLocation();
        if (eventLoc == null || eventLoc.getWorld() == null) return;

        Block block = eventLoc.getBlock();
        Block standingBlock = block.getType() == Material.NOTE_BLOCK ? block : block.getRelative(BlockFace.DOWN);
        if (standingBlock.getType() != Material.NOTE_BLOCK) return;

        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(standingBlock);
        if (optDef.isEmpty()) return;
        NativeCustomBlock def = optDef.get();

        CustomBlockSoundGroup soundGroup = def.getSoundGroup();
        if (soundGroup == null) return;

        CustomBlockSound sound = (gameEvent == GameEvent.STEP) ? soundGroup.getStepSound() : soundGroup.getFallSound();
        if (sound == null) return;

        sound.play(entity.getLocation());
    }

    /**
     * Plays custom hit sound when hitting a native NoteBlock (ported from Oraxen NoteBlockSoundListener).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitWood(BlockDamageEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NOTE_BLOCK) return;

        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(block);
        if (optDef.isEmpty()) return;
        NativeCustomBlock def = optDef.get();

        CustomBlockSoundGroup soundGroup = def.getSoundGroup();
        if (soundGroup == null || soundGroup.getHitSound() == null) return;

        CustomBlockSound sound = soundGroup.getHitSound();
        sound.play(block.getLocation().toCenterLocation());
    }

    public void updateAndCheck(Block block) {
        if (block == null) return;
        final Block blockAbove = block.getRelative(BlockFace.UP);
        if (blockAbove.getType() == Material.NOTE_BLOCK) {
            blockAbove.getState().update(true, true);
            Block nextBlock = blockAbove.getRelative(BlockFace.UP);
            if (nextBlock.getType() == Material.NOTE_BLOCK) {
                updateAndCheck(blockAbove);
            }
        }
    }

    public void makePlayerPlaceBlock(Player player, EquipmentSlot hand, ItemStack item,
                                     Block placedAgainst, BlockFace face, BlockData newData) {
        if (player == null || placedAgainst == null || newData == null) return;
        Block target = isReplaceable(placedAgainst.getType()) ? placedAgainst : placedAgainst.getRelative(face);
        if (!isReplaceable(target.getType())) return;
        if (isStandingInside(player, target)) return;

        World world = target.getWorld();
        if (target.getY() < world.getMinHeight() || target.getY() >= world.getMaxHeight()) return;

        BlockData oldData = target.getBlockData();
        target.setBlockData(newData);

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst, item, player, true, hand);
        if (!placeEvent.callEvent() || !placeEvent.canBuild()) {
            target.setBlockData(oldData);
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE && item != null && item.getAmount() > 0) {
            item.setAmount(item.getAmount() - 1);
        }

        if (hand == EquipmentSlot.OFF_HAND) {
            player.swingOffHand();
        } else {
            player.swingMainHand();
        }
        target.getWorld().sendGameEvent(player, GameEvent.BLOCK_PLACE, target.getLocation().toVector());
    }

    public static boolean isReplaceable(Material material) {
        if (material == null) return false;
        if (material.isAir()) return true;
        try {
            return Tag.REPLACEABLE.isTagged(material);
        } catch (Throwable ignored) {
            return material == Material.WATER || material == Material.LAVA || material == Material.SHORT_GRASS
                    || material == Material.TALL_GRASS || material == Material.SEAGRASS || material == Material.SNOW;
        }
    }

    public static boolean isStandingInside(Player player, Block block) {
        if (player == null || block == null || block.getWorld() == null) return false;
        try {
            BoundingBox blockBox = BoundingBox.of(block.getLocation().toCenterLocation(), 0.5, 0.5, 0.5);
            return !block.getWorld().getNearbyEntities(blockBox).stream()
                    .filter(e -> e instanceof LivingEntity && (!(e instanceof Player p) || p.getGameMode() != GameMode.SPECTATOR))
                    .toList().isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isUnsupportedBlockAboveNoteBlock(Material material) {
        if (material == null) return false;
        return material == Material.PISTON || material == Material.STICKY_PISTON
                || material == Material.SHULKER_BOX || material.name().endsWith("_SHULKER_BOX");
    }

    /**
     * Sends sequence acknowledgement via PacketEvents to synchronize client-side prediction
     * without direct NMS access.
     */
    public static void acknowledgeBlockChange(Player player, int sequence) {
        if (player == null || sequence < 0) return;
        try {
            if (PacketEvents.getAPI() != null && PacketEvents.getAPI().getPlayerManager() != null) {
                WrapperPlayServerAcknowledgeBlockChanges packet =
                        new WrapperPlayServerAcknowledgeBlockChanges(sequence);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        } catch (Throwable ignored) {
            // Graceful fallback when PacketEvents is uninitialized (unit tests or mock environment)
        }
    }

    /**
     * Resends block state packets for vertical neighbors of a given location to prevent
     * client-side rendering artifacts after block breaking or placement.
     */
    public static void resendNeighborBlockStates(Player player, Location loc) {
        if (player == null || loc == null || loc.getWorld() == null) return;
        try {
            Block block = loc.getBlock();
            Block above = block.getRelative(BlockFace.UP);
            Block below = block.getRelative(BlockFace.DOWN);
            player.sendBlockChange(above.getLocation(), above.getBlockData());
            player.sendBlockChange(below.getLocation(), below.getBlockData());
        } catch (Throwable ignored) {
        }
    }

    private static Map<Instrument, List<String>> createInstrumentMap() {
        Map<Instrument, List<String>> map = new EnumMap<>(Instrument.class);
        map.put(Instrument.BELL, List.of("gold_block"));
        map.put(Instrument.BASS_DRUM, List.of("stone", "netherrack", "bedrock", "observer", "coral", "obsidian", "anchor", "quartz", "deepslate"));
        map.put(Instrument.FLUTE, List.of("clay"));
        map.put(Instrument.CHIME, List.of("packed_ice", "blue_ice"));
        map.put(Instrument.GUITAR, List.of("wool"));
        map.put(Instrument.XYLOPHONE, List.of("bone_block"));
        map.put(Instrument.IRON_XYLOPHONE, List.of("iron_block"));
        map.put(Instrument.COW_BELL, List.of("soul_sand"));
        map.put(Instrument.DIDGERIDOO, List.of("pumpkin"));
        map.put(Instrument.BIT, List.of("emerald_block"));
        map.put(Instrument.BANJO, List.of("hay_bale"));
        map.put(Instrument.PLING, List.of("glowstone"));
        map.put(Instrument.BASS_GUITAR, List.of("wood", "oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log"));
        map.put(Instrument.SNARE_DRUM, List.of("sand", "gravel", "concrete_powder", "soul_soil"));
        map.put(Instrument.STICKS, List.of("glass", "sea_lantern", "beacon"));
        return Collections.unmodifiableMap(map);
    }
}
