package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Item Browser GUI — hiển thị tất cả custom items (phân trang & tìm kiếm qua Dialog).
 * Click vào item để xem recipes.
 */
public final class ItemBrowserGUI implements Listener {

    private static final int GUI_SIZE = 54; // 6 rows
    private static final int ITEMS_PER_PAGE = 45; // 5 rows of items
    private static final int CLOSE_SLOT = 45;
    private static final int SEARCH_SLOT = 46;
    private static final int PREV_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)[§&][0-9a-fk-or]");

    private final Plugin plugin;
    private final ItemRegistry itemRegistry;
    private final ItemService itemService;
    private final RecipeViewerGUI recipeViewer;

    public ItemBrowserGUI(Plugin plugin, ItemRegistry itemRegistry, ItemService itemService, RecipeViewerGUI recipeViewer) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.itemService = itemService;
        this.recipeViewer = recipeViewer;
    }

    public ItemBrowserGUI(ItemRegistry itemRegistry, ItemService itemService, RecipeViewerGUI recipeViewer) {
        this(null, itemRegistry, itemService, recipeViewer);
    }

    /**
     * Mở Item Browser cho player.
     */
    public void open(Player player) {
        open(player, 0, null);
    }

    /**
     * Mở Item Browser tại trang chỉ định.
     */
    public void open(Player player, int page) {
        open(player, page, null);
    }

    /**
     * Mở Item Browser với trang và từ khóa tìm kiếm.
     */
    public void open(Player player, int page, String searchQuery) {
        List<ItemDefinition> allItems = new ArrayList<>(itemRegistry.all());
        allItems.sort((a, b) -> a.getId().compareTo(b.getId()));

        String query = (searchQuery != null && !searchQuery.isBlank()) ? searchQuery.trim() : null;
        List<ItemDefinition> displayItems;
        if (query != null) {
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            displayItems = allItems.stream()
                    .filter(def -> matchesSearch(def, lowerQuery))
                    .toList();
        } else {
            displayItems = allItems;
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) displayItems.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        ItemBrowserHolder holder = new ItemBrowserHolder(displayItems, page, totalPages, query);
        Component title = (query != null)
                ? Component.text("Search: " + query, NamedTextColor.DARK_PURPLE)
                : Component.text("Item Browser", NamedTextColor.DARK_PURPLE);

        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);
        holder.setInventory(gui);

        populateItems(gui, holder);
        populateNavigation(gui, holder);

        player.openInventory(gui);
    }

    private boolean matchesSearch(ItemDefinition def, String query) {
        if (def.getId().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        if (def.getDisplayName() != null) {
            String plainName = stripColor(def.getDisplayName()).toLowerCase(Locale.ROOT);
            if (plainName.contains(query)) {
                return true;
            }
        }
        if (def.getLore() != null) {
            for (String line : def.getLore()) {
                String plainLore = stripColor(line).toLowerCase(Locale.ROOT);
                if (plainLore.contains(query)) {
                    return true;
                }
            }
        }
        if (def.getMaterial() != null && def.getMaterial().name().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        if (def.getProperties() != null) {
            for (Map.Entry<String, Object> entry : def.getProperties().entrySet()) {
                if (entry.getKey().toLowerCase(Locale.ROOT).contains(query)) {
                    return true;
                }
                if (entry.getValue() != null && String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT).contains(query)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String stripColor(String input) {
        return input == null ? "" : STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
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

        // Close
        gui.setItem(CLOSE_SLOT, createCloseItem());

        // Search
        gui.setItem(SEARCH_SLOT, createSearchItem(holder.getSearchQuery()));

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
            List<Component> loreList = new ArrayList<>();
            loreList.add(Component.text("Total items: " + holder.getItems().size(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (holder.getSearchQuery() != null) {
                loreList.add(Component.text("Filter: \"" + holder.getSearchQuery() + "\"", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            loreList.add(Component.text("Left click: Take item", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            loreList.add(Component.text("Right click: View recipe", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            loreList.add(Component.text("Shift + left click: Take a stack", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            infoMeta.lore(loreList);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(INFO_SLOT, info);
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

    private ItemStack createSearchItem(String currentQuery) {
        ItemStack searchItem = MenuIcon.create(MenuIcon.SEARCH, Component.text("Search", NamedTextColor.AQUA));
        ItemMeta meta = searchItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            if (currentQuery != null) {
                lore.add(Component.text("Current: " + currentQuery, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Left click: Change keyword", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Right click: Clear filter", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("Click to search items", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            searchItem.setItemMeta(meta);
        }
        return searchItem;
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

    public void openSearchDialog(Player player, String currentQuery) {
        Dialog searchDialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Search Items", NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false))
                        .inputs(List.of(
                                DialogInput.text("query", Component.text("Keyword", NamedTextColor.YELLOW)
                                                .decoration(TextDecoration.ITALIC, false))
                                        .initial(currentQuery != null ? currentQuery : "")
                                        .maxLength(50)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Search", NamedTextColor.GREEN)
                                        .decoration(TextDecoration.ITALIC, false))
                                .action(DialogAction.customClick((responseView, audience) -> {
                                    String query = responseView.getText("query");
                                    if (audience instanceof Player p) {
                                        if (plugin != null) {
                                            Bukkit.getScheduler().runTask(plugin, () -> open(p, 0, query));
                                        } else {
                                            open(p, 0, query);
                                        }
                                    }
                                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build()))
                                .build(),
                        ActionButton.builder(Component.text("Cancel", NamedTextColor.RED)
                                        .decoration(TextDecoration.ITALIC, false))
                                .action(DialogAction.customClick((responseView, audience) -> {
                                    if (audience instanceof Player p) {
                                        if (plugin != null) {
                                            Bukkit.getScheduler().runTask(plugin, () -> open(p, 0, currentQuery));
                                        } else {
                                            open(p, 0, currentQuery);
                                        }
                                    }
                                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build()))
                                .build()
                )));

        player.showDialog(searchDialog);
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

        if (handleNavigationClick(slot, player, holder, event.getClick())) {
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

    private boolean handleNavigationClick(int slot, Player player, ItemBrowserHolder holder, ClickType click) {
        if (slot == PREV_SLOT && holder.getPage() > 0) {
            open(player, holder.getPage() - 1, holder.getSearchQuery());
            return true;
        }
        if (slot == NEXT_SLOT && holder.getPage() < holder.getTotalPages() - 1) {
            open(player, holder.getPage() + 1, holder.getSearchQuery());
            return true;
        }
        if (slot == SEARCH_SLOT) {
            if (click.isRightClick() && holder.getSearchQuery() != null) {
                open(player, 0, null); // Clear search
            } else {
                openSearchDialog(player, holder.getSearchQuery());
            }
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
                    recipeViewer.open(player, itemId, holder.getPage(), holder.getSearchQuery());
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
