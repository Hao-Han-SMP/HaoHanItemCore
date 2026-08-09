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

        // Place items
        int startIndex = session.page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < session.items.size()) {
                ItemDefinition def = session.items.get(itemIndex);
                ItemStack display = itemService.create(def.getId());
                gui.setItem(i, display);
            }
        }

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
        ItemStack info = new ItemStack(Material.BOOK, 1);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("Page " + (session.page + 1) + " / " + session.totalPages,
                NamedTextColor.GOLD));
        infoMeta.lore(List.of(
                Component.text("Total items: " + session.items.size(), NamedTextColor.GRAY),
                Component.text("Click an item to view recipes", NamedTextColor.YELLOW)
        ));
        info.setItemMeta(infoMeta);
        gui.setItem(INFO_SLOT, info);

        // Close
        gui.setItem(CLOSE_SLOT, createCloseItem());

        return gui;
    }

    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" "));
        border.setItemMeta(meta);
        return border;
    }

    private ItemStack createNavItem(String name, boolean active) {
        ItemStack item = new ItemStack(active ? Material.LIME_DYE : Material.GRAY_DYE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
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

        // Previous page
        if (slot == PREV_SLOT && session.page > 0) {
            open(player, session.page - 1);
            return;
        }

        // Next page
        if (slot == NEXT_SLOT && session.page < session.totalPages - 1) {
            open(player, session.page + 1);
            return;
        }

        // Close
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        // Click on item → view recipes
        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                String itemId = itemService.getId(clicked);
                if (itemId != null) {
                    recipeViewer.open(player, itemId);
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
