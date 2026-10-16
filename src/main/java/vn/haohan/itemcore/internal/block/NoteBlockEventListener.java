package vn.haohan.itemcore.internal.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockInteractContext;
import vn.haohan.itemcore.api.block.CustomBlockRegistry;
import vn.haohan.itemcore.api.block.CustomBlockSound;
import vn.haohan.itemcore.api.block.CustomBlockSoundGroup;
import vn.haohan.itemcore.api.block.NativeCustomBlock;

import java.util.Objects;
import java.util.Optional;

/**
 * Minimal event listener for NoteBlock-backed {@link NativeCustomBlock} instances.
 * Relies on Paper's {@code block-updates.disable-noteblock-updates: true} for server-side state stability,
 * and handles only client-side interaction, block placement against NoteBlocks, sound overrides, and pick-block.
 */
public final class NoteBlockEventListener implements Listener {

    private final CustomBlockRegistry registry;
    private final Plugin plugin;

    public NoteBlockEventListener(CustomBlockRegistry registry, Plugin plugin) {
        this.registry = Objects.requireNonNull(registry, "CustomBlockRegistry cannot be null");
        this.plugin = plugin;
    }

    public Optional<NativeCustomBlock> getNativeCustomBlock(Block block) {
        if (block == null || block.getType() != Material.NOTE_BLOCK) return Optional.empty();
        if (!(block.getBlockData() instanceof NoteBlock noteBlock)) return Optional.empty();
        int variation = NoteBlockVariationTable.resolveVariation(noteBlock);
        return variation >= 0 ? registry.getByVariation(variation) : Optional.empty();
    }

    public boolean isNativeBlock(Block block) {
        return getNativeCustomBlock(block).isPresent();
    }

    /**
     * Unified right-click handler for Native NoteBlocks:
     * 1. Denies vanilla pitch tuning.
     * 2. Executes custom interactHandler when not sneaking.
     * 3. Denies flint & steel / fire charge ignition on top face.
     * 4. Places custom or vanilla blocks against the clicked NoteBlock and syncs client prediction.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTuneNoteBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.NOTE_BLOCK) return;

        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(block);
        if (optDef.isEmpty()) return;
        NativeCustomBlock def = optDef.get();

        event.setUseInteractedBlock(Event.Result.DENY);

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand() != null ? event.getHand() : EquipmentSlot.HAND;

        if (!player.isSneaking() && def.getInteractHandler() != null) {
            try {
                def.getInteractHandler().onInteract(new CustomBlockInteractContext(
                        player, block, item, hand, event.getAction()));
            } catch (Throwable t) {
                if (plugin != null) {
                    plugin.getLogger().warning("Error executing custom block interact action for "
                            + def.getId() + ": " + t.getMessage());
                }
            }
            return;
        }

        if (item == null || item.getType() == Material.AIR) return;

        if (event.getBlockFace() == BlockFace.UP
                && (item.getType() == Material.FLINT_AND_STEEL || item.getType() == Material.FIRE_CHARGE)) {
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }

        if (!item.getType().isBlock() && item.getType().isEdible() && !player.isSneaking()) {
            return;
        }

        String customBlockId = resolveCustomBlockIdFromItem(item);
        BlockData newData = null;
        if (customBlockId != null) {
            Optional<CustomBlockDefinition> optPlacingDef = registry.get(customBlockId);
            if (optPlacingDef.isPresent()) {
                newData = (optPlacingDef.get() instanceof NativeCustomBlock nativeDef)
                        ? NoteBlockVariationTable.createBlockData(nativeDef.getCustomVariation())
                        : Bukkit.createBlockData(Material.BARRIER);
            }
        } else if (item.getType().isBlock()) {
            try {
                newData = item.getType().createBlockData();
            } catch (Throwable ignored) {}
        }

        if (newData != null) {
            event.setUseItemInHand(Event.Result.DENY);
            if (makePlayerPlaceCustomOrVanillaBlock(player, hand, item, block, event.getBlockFace(), newData, customBlockId)) {
                Block target = block.isReplaceable() ? block : block.getRelative(event.getBlockFace());
                AdjacentNoteBlockUpdateHelper.finishAdjacentBlockChange(player, target, registry);
                player.sendBlockChange(block.getLocation(), block.getBlockData());
            }
        }
    }

    private String resolveCustomBlockIdFromItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try {
            var core = HaoHanItemCore.get();
            if (core != null && core.getItemService() != null) {
                String id = core.getItemService().getId(item);
                if (id != null) {
                    if (registry.has(id)) return id;
                    var itemRegistry = core.getItemRegistry();
                    if (itemRegistry != null) {
                        var itemDef = itemRegistry.get(id);
                        if (itemDef != null && itemDef.getProperties().get("custom_block_id") instanceof String cbId
                                && registry.has(cbId)) {
                            return cbId;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVerticalNeighborPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (AdjacentNoteBlockUpdateHelper.hasCustomVerticalNeighbor(placed, registry)) {
            AdjacentNoteBlockUpdateHelper.finishAdjacentBlockChange(event.getPlayer(), placed, registry);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVerticalNeighborBreak(BlockBreakEvent event) {
        AdjacentNoteBlockUpdateHelper.finishVerticalNeighborBreak(event.getPlayer(), event.getBlock(), registry);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlacingUnsupportedBlockAboveNoteBlock(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (isUnsupportedBlockAboveNoteBlock(placed.getType()) && isNativeBlock(placed.getRelative(BlockFace.DOWN))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        if (!(event.getInventory().getHolder() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() != Material.NOTE_BLOCK) return;

        RayTraceResult rayTrace = player.rayTraceBlocks(6.0);
        if (rayTrace == null || rayTrace.getHitBlock() == null) return;

        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(rayTrace.getHitBlock());
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlay(NotePlayEvent event) {
        if (isNativeBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatchFire(BlockIgniteEvent event) {
        if (isNativeBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitWood(BlockDamageEvent event) {
        Optional<NativeCustomBlock> optDef = getNativeCustomBlock(event.getBlock());
        if (optDef.isEmpty()) return;
        CustomBlockSoundGroup soundGroup = optDef.get().getSoundGroup();
        if (soundGroup != null && soundGroup.getHitSound() != null) {
            soundGroup.getHitSound().play(event.getBlock().getLocation().toCenterLocation());
        }
    }

    public boolean makePlayerPlaceCustomOrVanillaBlock(Player player, EquipmentSlot hand, ItemStack item,
                                                        Block placedAgainst, BlockFace face, BlockData newData,
                                                        String customBlockId) {
        if (player == null || placedAgainst == null || newData == null) return false;
        Block target = placedAgainst.isReplaceable() ? placedAgainst : placedAgainst.getRelative(face);
        if (!target.isReplaceable() || isStandingInside(player, target)) return false;

        World world = target.getWorld();
        if (target.getY() < world.getMinHeight() || target.getY() >= world.getMaxHeight()) return false;

        var oldState = target.getState();
        BlockData oldData = target.getBlockData();
        target.setBlockData(newData, false);

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(target, oldState, placedAgainst, item, player, true, hand);
        if (!placeEvent.callEvent() || !placeEvent.canBuild()) {
            target.setBlockData(oldData, false);
            return false;
        }

        if (customBlockId != null) {
            try {
                var core = HaoHanItemCore.get();
                if (core != null && core.getCustomBlockService() != null) {
                    core.getCustomBlockService().placeCustomBlock(target.getLocation(), customBlockId);
                }
            } catch (Throwable ignored) {}
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
        return true;
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

    public static void acknowledgeBlockChange(Player player, int sequence) {
        if (player == null || sequence < 0) return;
        try {
            if (PacketEvents.getAPI() != null && PacketEvents.getAPI().getPlayerManager() != null) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(
                        player, new WrapperPlayServerAcknowledgeBlockChanges(sequence));
            }
        } catch (Throwable ignored) {}
    }
}
