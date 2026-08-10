package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.recipe.RecipeService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;
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

    // Track active sessions
    private final Map<UUID, BrowserSession> activeSessions = new HashMap<>();

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

        BrowserSession session = new BrowserSession(allItems, page, totalPages);
        activeSessions.put(player.getUniqueId(), session);

        Inventory gui = createGUI(session);
        player.openInventory(gui);
    }

    private Inventory createGUI(BrowserSession session) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                Component.text("Item Browser", NamedTextColor.DARK_PURPLE));

        populateItems(gui, session);
        populateNavigation(gui, session);

        return gui;
    }

    private void populateItems(Inventory gui, BrowserSession session) {
        int startIndex = session.page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < session.items.size()) {
                ItemDefinition def = session.items.get(itemIndex);
                ItemStack display = itemService.create(def.getId());
                clearInferredModel(display, def);
                gui.setItem(i, display);
            }
        }
    }

    private void populateNavigation(Inventory gui, BrowserSession session) {
        // Navigation bar (bottom row)
        ItemStack border = createBorderItem();
        for (int i = ITEMS_PER_PAGE; i < GUI_SIZE; i++) {
            gui.setItem(i, border);
        }

        // Previous page
        gui.setItem(PREV_SLOT, createNavItem("§a◀ Previous Page", session.page > 0));

        // Next page
        gui.setItem(NEXT_SLOT, createNavItem("§a▶ Next Page", session.page < session.totalPages - 1));

        // Info
        ItemStack info = MenuIcon.create(MenuIcon.INFO,
                Component.text("Page " + (session.page + 1) + " / " + session.totalPages,
                        NamedTextColor.GOLD));
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.lore(List.of(
                Component.text("Total items: " + session.items.size(), NamedTextColor.GRAY),
                Component.text("Left click: Take item", NamedTextColor.YELLOW),
                Component.text("Right click: View recipe", NamedTextColor.YELLOW),
                Component.text("Shift + left click: Take a stack", NamedTextColor.YELLOW)
        ));
        info.setItemMeta(infoMeta);
        gui.setItem(INFO_SLOT, info);

        // Close
        gui.setItem(CLOSE_SLOT, createCloseItem());
    }

    private static void clearInferredModel(ItemStack item, ItemDefinition definition) {
        if (definition.getItemModel() != null || definition.getCustomModelData() == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setItemModel(null);
        item.setItemMeta(meta);
    }

    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" "));
        border.setItemMeta(meta);
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

    @SuppressWarnings("unused")
    private ItemStack createCloseItemLegacy() {
        ItemStack item = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c✖ Close"));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        BrowserSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (handleNavigationClick(slot, player, session)) {
            return;
        }

        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            handleItemClick(slot, player, session, event.getCurrentItem(), event.getClick());
        }
    }

    private boolean handleNavigationClick(int slot, Player player, BrowserSession session) {
        if (slot == PREV_SLOT && session.page > 0) {
            open(player, session.page - 1);
            return true;
        }
        if (slot == NEXT_SLOT && session.page < session.totalPages - 1) {
            open(player, session.page + 1);
            return true;
        }
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return true;
        }
        return false;
    }

    private void handleItemClick(int slot, Player player, BrowserSession session,
                                 ItemStack clicked, ClickType click) {
        if (clicked != null && clicked.getType() != Material.AIR) {
            String itemId = itemService.getId(clicked);
            if (itemId != null) {
                if (click.isRightClick()) {
                    if (!recipeViewer.hasRecipes(itemId)) return;
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            activeSessions.remove(player.getUniqueId());
        }
    }

    private static final class BrowserSession {
        final List<ItemDefinition> items;
        final int page;
        final int totalPages;

        BrowserSession(List<ItemDefinition> items, int page, int totalPages) {
            this.items = items;
            this.page = page;
            this.totalPages = totalPages;
        }
    }
}
