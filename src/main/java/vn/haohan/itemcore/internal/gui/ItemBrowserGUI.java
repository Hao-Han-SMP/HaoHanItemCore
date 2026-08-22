package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Item Browser GUI — hiển thị tất cả custom items (phân trang).
 * Click vào item để xem recipes.
 */
public final class ItemBrowserGUI implements Listener {

    private static final int GUI_SIZE = 54; // 6 rows
    private static final int ITEMS_PER_PAGE = 45; // 5 rows of items
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;
    private static final int INFO_SLOT = 49;
    private static final int CLOSE_SLOT = 45;

    private final ItemRegistry itemRegistry;
    private final ItemService itemService;
    private final RecipeViewerGUI recipeViewer;

    public ItemBrowserGUI(ItemRegistry itemRegistry, ItemService itemService, RecipeViewerGUI recipeViewer) {
        this.itemRegistry = itemRegistry;
        this.itemService = itemService;
        this.recipeViewer = recipeViewer;
    }

    /**
     * Mở Item Browser cho player.
     */
    public void open(Player player) {
        open(player, 0);
    }

    /**
     * Mở Item Browser tại trang chỉ định.
     */
    public void open(Player player, int page) {
        List<ItemDefinition> allItems = new ArrayList<>(itemRegistry.all());
        allItems.sort(Comparator.comparing(ItemDefinition::getId));

        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        ItemBrowserHolder holder = new ItemBrowserHolder(allItems, page, totalPages);
        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE,
                Component.text("Item Browser", NamedTextColor.DARK_PURPLE));
        holder.setInventory(gui);

        populateItems(gui, holder);
        populateNavigation(gui, holder);

        player.openInventory(gui);
    }

    private void populateItems(Inventory gui, ItemBrowserHolder holder) {
        int startIndex = holder.getPage() * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < holder.getItems().size()) {
                ItemDefinition def = holder.getItems().get(itemIndex);
                ItemStack display = itemService.create(def.getId());
                gui.setItem(i, display);
            }
        }
    }

    private void populateNavigation(Inventory gui, ItemBrowserHolder holder) {
        // Navigation bar (bottom row)
        ItemStack border = createBorderItem();
        for (int i = ITEMS_PER_PAGE; i < GUI_SIZE; i++) {
            gui.setItem(i, border);
        }

        // Previous page
        gui.setItem(PREV_SLOT, createNavItem("§a◀ Previous Page", holder.getPage() > 0));

        // Next page
        gui.setItem(NEXT_SLOT, createNavItem("§a▶ Next Page", holder.getPage() < holder.getTotalPages() - 1));

        // Info
        ItemStack info = MenuIcon.create(MenuIcon.INFO,
                Component.text("Page " + (holder.getPage() + 1) + " / " + holder.getTotalPages(),
                        NamedTextColor.GOLD));
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.lore(List.of(
                    Component.text("Total items: " + holder.getItems().size(), NamedTextColor.GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                    Component.text("Left click: Take item", NamedTextColor.YELLOW).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                    Component.text("Right click: View recipe", NamedTextColor.YELLOW).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                    Component.text("Shift + left click: Take a stack", NamedTextColor.YELLOW).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
            info.setItemMeta(infoMeta);
        }
        gui.setItem(INFO_SLOT, info);

        // Close
        gui.setItem(CLOSE_SLOT, createCloseItem());
    }

    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            meta.setHideTooltip(true);
            border.setItemMeta(meta);
        }
        return border;
    }

    private ItemStack createNavItem(String name, boolean active) {
        boolean previous = name.contains("Previous");
        return MenuIcon.create(previous
                ? (active ? MenuIcon.PREVIOUS_ACTIVE : MenuIcon.PREVIOUS_DISABLED)
                : (active ? MenuIcon.NEXT_ACTIVE : MenuIcon.NEXT_DISABLED),
                Component.text(name));
    }

    private ItemStack createCloseItem() {
        return MenuIcon.create(MenuIcon.CLOSE, Component.text("Close", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        if (!(event.getView().getTopInventory().getHolder() instanceof ItemBrowserHolder holder))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        // Clicks outside top inventory are ignored after cancellation
        if (slot < 0 || slot >= topSize) {
            return;
        }

        if (handleNavigationClick(slot, player, holder)) {
            return;
        }

        if (slot < ITEMS_PER_PAGE) {
            handleItemClick(slot, player, holder, event.getCurrentItem(), event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        if (!(event.getView().getTopInventory().getHolder() instanceof ItemBrowserHolder))
            return;

        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    private boolean handleNavigationClick(int slot, Player player, ItemBrowserHolder holder) {
        if (slot == PREV_SLOT && holder.getPage() > 0) {
            open(player, holder.getPage() - 1);
            return true;
        }
        if (slot == NEXT_SLOT && holder.getPage() < holder.getTotalPages() - 1) {
            open(player, holder.getPage() + 1);
            return true;
        }
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return true;
        }
        return false;
    }

    private void handleItemClick(int slot, Player player, ItemBrowserHolder holder,
            ItemStack clicked, ClickType click) {
        if (clicked != null && clicked.getType() != Material.AIR) {
            String itemId = itemService.getId(clicked);
            if (itemId != null) {
                if (click.isRightClick()) {
                    if (!recipeViewer.hasRecipes(itemId))
                        return;
                    recipeViewer.open(player, itemId);
                } else if (click.isLeftClick()) {
                    int amount = click.isShiftClick()
                            ? Math.max(1, Math.min(64, itemRegistry.get(itemId).getMaxStackSize()))
                            : 1;
                    player.getInventory().addItem(itemService.create(itemId, amount));
                }
            }
        }
    }
}
