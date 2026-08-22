package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.item.ItemCategory;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Item Browser GUI — hiển thị custom items với bộ lọc tập trung vào 1 item duy nhất:
 * - Điều khiển đa năng qua thao tác chuột (Left, Shift+Left, Middle, Shift+Right, Right click)
 * - Mở các Dialog danh sách dài (MultiAction list) cho Danh mục và Plugin/Namespace nguồn
 * - Tìm kiếm bằng từ khóa qua Text Dialog
 * - Xem công thức chế tạo (Recipe Viewer) và lấy item
 */
public final class ItemBrowserGUI implements Listener {

    private static final int GUI_SIZE = 54; // 6 rows
    private static final int ITEMS_PER_PAGE = 45; // 5 rows of items

    private static final int CLOSE_SLOT = 45;
    private static final int FILTER_SLOT = 46;
    private static final int PREV_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private final Plugin plugin;
    private final ItemRegistry itemRegistry;
    private final ItemService itemService;
    private final RecipeViewerGUI recipeViewer;

    // Lưu trữ trạng thái filter và trang duyệt của từng người chơi
    private final Map<UUID, ItemBrowserFilter> playerFilters = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();

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
     * Mở Item Browser cho player, tự động giữ nguyên bộ lọc và trang đã chọn trước đó.
     */
    public void open(Player player) {
        ItemBrowserFilter lastFilter = playerFilters.getOrDefault(player.getUniqueId(), ItemBrowserFilter.empty());
        int lastPage = playerPages.getOrDefault(player.getUniqueId(), 0);
        open(player, lastPage, lastFilter);
    }

    /**
     * Mở Item Browser tại trang chỉ định, giữ nguyên filter đã lưu.
     */
    public void open(Player player, int page) {
        ItemBrowserFilter lastFilter = playerFilters.getOrDefault(player.getUniqueId(), ItemBrowserFilter.empty());
        open(player, page, lastFilter);
    }

    /**
     * Mở Item Browser với từ khóa tìm kiếm mới (cập nhật vào filter).
     */
    public void open(Player player, int page, String searchQuery) {
        ItemBrowserFilter current = playerFilters.getOrDefault(player.getUniqueId(), ItemBrowserFilter.empty());
        open(player, page, current.withQuery(searchQuery));
    }

    /**
     * Mở Item Browser với đầy đủ bộ lọc và lưu lại phiên lọc cho player.
     */
    public void open(Player player, int page, ItemBrowserFilter filter) {
        if (filter == null) {
            filter = ItemBrowserFilter.empty();
        }

        // Lưu phiên lọc của player
        playerFilters.put(player.getUniqueId(), filter);
        playerPages.put(player.getUniqueId(), page);

        final ItemBrowserFilter finalFilter = filter;
        List<ItemDefinition> allItems = new ArrayList<>(itemRegistry.all());
        allItems.sort((a, b) -> a.getId().compareTo(b.getId()));

        List<ItemDefinition> displayItems = allItems.stream()
                .filter(finalFilter::matches)
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil((double) displayItems.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        ItemBrowserHolder holder = new ItemBrowserHolder(displayItems, page, totalPages, finalFilter);
        Component title = createBrowserTitle(finalFilter, displayItems.size(), allItems.size());

        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);
        holder.setInventory(gui);

        populateItems(gui, holder);
        populateNavigation(gui, holder, displayItems.size(), allItems.size());

        player.openInventory(gui);
    }

    private Component createBrowserTitle(ItemBrowserFilter filter, int filteredCount, int totalCount) {
        if (filter.isDefault()) {
            return Component.text("Item Browser (" + totalCount + ")", NamedTextColor.DARK_PURPLE);
        }
        return Component.text("Filter (" + filteredCount + "/" + totalCount + ")", NamedTextColor.DARK_PURPLE);
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

    private void populateNavigation(Inventory gui, ItemBrowserHolder holder, int filteredCount, int totalCount) {
        ItemStack border = createBorderItem();
        for (int i = ITEMS_PER_PAGE; i < GUI_SIZE; i++) {
            gui.setItem(i, border);
        }

        ItemBrowserFilter filter = holder.getFilter();

        // 45: Close
        gui.setItem(CLOSE_SLOT, createCloseItem());

        // 46: Single Unified Filter Item (kích hoạt các tính năng bằng chuột)
        gui.setItem(FILTER_SLOT, createUnifiedFilterItem(filter, filteredCount, totalCount));

        // 48: Previous Page
        gui.setItem(PREV_SLOT, createNavItem("§a◀ Previous Page", holder.getPage() > 0));

        // 49: Info
        gui.setItem(INFO_SLOT, createInfoItem(holder));

        // 50: Next Page
        gui.setItem(NEXT_SLOT, createNavItem("§a▶ Next Page", holder.getPage() < holder.getTotalPages() - 1));
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

    private ItemStack createUnifiedFilterItem(ItemBrowserFilter filter, int filteredCount, int totalCount) {
        ItemStack item = MenuIcon.create(MenuIcon.SEARCH, Component.text("Search & Filter", NamedTextColor.AQUA));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Trạng thái bộ lọc hiện tại:", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.text(" • Từ khóa: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(filter.getQuery() != null ? "\"" + filter.getQuery() + "\"" : "(Tất cả)",
                            filter.getQuery() != null ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.text(" • Danh mục: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(filter.getCategory().getDisplayName(), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.text(" • Plugin: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(filter.getNamespace() != null ? filter.getNamespace() : "(Tất cả)", NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.text(" • Kết quả: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(filteredCount + " / " + totalCount + " items", NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(Component.text("Thao tác chuột để lọc:", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.text("▶ Chuột trái: ", NamedTextColor.YELLOW)
                    .append(Component.text("Tìm kiếm từ khóa (Search Dialog)", NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.text("▶ Chuột phải: ", NamedTextColor.AQUA)
                    .append(Component.text("Mở Menu Bộ Lọc & Danh mục", NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.text("▶ Chuột giữa: ", NamedTextColor.RED)
                    .append(Component.text("Xóa toàn bộ bộ lọc về mặc định", NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(ItemBrowserHolder holder) {
        ItemStack info = MenuIcon.create(MenuIcon.INFO,
                Component.text("Page " + (holder.getPage() + 1) + " / " + holder.getTotalPages(),
                        NamedTextColor.GOLD));
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            List<Component> loreList = new ArrayList<>();
            loreList.add(Component.text("Items hiển thị: " + holder.getItems().size(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            ItemBrowserFilter f = holder.getFilter();
            if (!f.isDefault()) {
                if (f.getQuery() != null) {
                    loreList.add(Component.text("• Từ khóa: \"" + f.getQuery() + "\"", NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (f.getCategory() != ItemCategory.ALL) {
                    loreList.add(Component.text("• Danh mục: " + f.getCategory().getDisplayName(), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (f.getNamespace() != null) {
                    loreList.add(Component.text("• Plugin: " + f.getNamespace(), NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }

            loreList.add(Component.empty());
            loreList.add(Component.text("Left click: Lấy 1 item", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            loreList.add(Component.text("Shift + Left click: Lấy 1 stack", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            loreList.add(Component.text("Right click: Xem công thức chế tạo", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            infoMeta.lore(loreList);
            info.setItemMeta(infoMeta);
        }
        return info;
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

    /**
     * Mở Dialog Danh Sách Dài (MultiAction List) để chọn Danh Mục Phân Loại (Category).
     */
    public void openCategoryDialog(Player player, ItemBrowserFilter currentFilter) {
        if (currentFilter == null) currentFilter = ItemBrowserFilter.empty();
        final ItemBrowserFilter filterState = currentFilter;

        List<ActionButton> actionButtons = new ArrayList<>();
        for (ItemCategory cat : ItemCategory.values()) {
            int catCount = (int) itemRegistry.all().stream().filter(cat::matches).count();
            boolean isSelected = cat == filterState.getCategory();

            Component label = Component.text((isSelected ? "✔ " : "• ") + cat.getDisplayName() + " (" + catCount + ")",
                    isSelected ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false);

            Component tooltip = Component.text(cat.getDescription(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false);

            ActionButton btn = ActionButton.builder(label)
                    .width(300)
                    .tooltip(tooltip)
                    .action(DialogAction.customClick((responseView, audience) -> {
                        if (audience instanceof Player p) {
                            runSync(() -> open(p, 0, filterState.withCategory(cat)));
                        }
                    }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                    .build();

            actionButtons.add(btn);
        }

        ActionButton exitAction = ActionButton.builder(Component.text("Quay lại / Đóng", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false))
                .width(300)
                .action(DialogAction.customClick((responseView, audience) -> {
                    if (audience instanceof Player p) {
                        runSync(() -> open(p, 0, filterState));
                    }
                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                .build();

        Dialog categoryDialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Chọn Danh Mục Phân Loại", NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(actionButtons, exitAction, 1)));

        player.showDialog(categoryDialog);
    }

    /**
     * Mở Dialog Danh Sách Dài (MultiAction List) để chọn Plugin / Namespace nguồn.
     */
    public void openPluginDialog(Player player, ItemBrowserFilter currentFilter) {
        if (currentFilter == null) currentFilter = ItemBrowserFilter.empty();
        final ItemBrowserFilter filterState = currentFilter;

        Set<String> namespaces = getDiscoveredNamespaces();
        List<ActionButton> actionButtons = new ArrayList<>();

        // Option: Tất cả
        boolean isAllSelected = filterState.getNamespace() == null;
        actionButtons.add(ActionButton.builder(Component.text(
                (isAllSelected ? "✔ " : "• ") + "Tất cả Plugins (" + itemRegistry.size() + " items)",
                isAllSelected ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false))
                .width(300)
                .tooltip(Component.text("Hiển thị vật phẩm từ tất cả các plugin", NamedTextColor.AQUA))
                .action(DialogAction.customClick((responseView, audience) -> {
                    if (audience instanceof Player p) {
                        runSync(() -> open(p, 0, filterState.withNamespace(null)));
                    }
                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                .build());

        // Option: từng plugin/namespace
        for (String ns : namespaces) {
            int nsCount = (int) itemRegistry.all().stream().filter(d -> d.getNamespace().equalsIgnoreCase(ns)).count();
            boolean isSelected = ns.equalsIgnoreCase(filterState.getNamespace());

            Component label = Component.text((isSelected ? "✔ " : "• ") + "Plugin: " + ns + " (" + nsCount + " items)",
                    isSelected ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false);

            Component tooltip = Component.text("Lọc các vật phẩm thuộc plugin/namespace: " + ns, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false);

            ActionButton btn = ActionButton.builder(label)
                    .width(300)
                    .tooltip(tooltip)
                    .action(DialogAction.customClick((responseView, audience) -> {
                        if (audience instanceof Player p) {
                            runSync(() -> open(p, 0, filterState.withNamespace(ns)));
                        }
                    }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                    .build();

            actionButtons.add(btn);
        }

        ActionButton exitAction = ActionButton.builder(Component.text("Quay lại / Đóng", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false))
                .width(300)
                .action(DialogAction.customClick((responseView, audience) -> {
                    if (audience instanceof Player p) {
                        runSync(() -> open(p, 0, filterState));
                    }
                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                .build();

        Dialog pluginDialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Chọn Plugin / Namespace Nguồn", NamedTextColor.AQUA)
                                .decoration(TextDecoration.ITALIC, false))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(actionButtons, exitAction, 1)));

        player.showDialog(pluginDialog);
    }

    /**
     * Mở Text Dialog để nhập từ khóa tìm kiếm.
     */
    public void openSearchDialog(Player player, ItemBrowserFilter currentFilter) {
        if (currentFilter == null) currentFilter = ItemBrowserFilter.empty();
        final ItemBrowserFilter filterState = currentFilter;

        Dialog searchDialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Tìm Kiếm Vật Phẩm", NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false))
                        .inputs(List.of(
                                DialogInput.text("query", Component.text("Từ khóa tìm kiếm / Keyword", NamedTextColor.YELLOW)
                                                .decoration(TextDecoration.ITALIC, false))
                                        .initial(filterState.getQuery() != null ? filterState.getQuery() : "")
                                        .maxLength(50)
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Tìm Kiếm", NamedTextColor.GREEN)
                                        .decoration(TextDecoration.ITALIC, false))
                                .action(DialogAction.customClick((responseView, audience) -> {
                                    String query = responseView.getText("query");
                                    ItemBrowserFilter newFilter = filterState.withQuery(query);
                                    if (audience instanceof Player p) {
                                        runSync(() -> open(p, 0, newFilter));
                                    }
                                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                                .build(),
                        ActionButton.builder(Component.text("Hủy", NamedTextColor.RED)
                                        .decoration(TextDecoration.ITALIC, false))
                                .action(DialogAction.customClick((responseView, audience) -> {
                                    if (audience instanceof Player p) {
                                        runSync(() -> open(p, 0, filterState));
                                    }
                                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                                .build()
                )));

        player.showDialog(searchDialog);
    }

    /**
     * Mở Menu Bộ Lọc Tổng Hợp (MultiAction List).
     */
    public void openFilterMenuDialog(Player player, ItemBrowserFilter currentFilter) {
        if (currentFilter == null) currentFilter = ItemBrowserFilter.empty();
        final ItemBrowserFilter filterState = currentFilter;

        List<ActionButton> actions = new ArrayList<>();

        // 1. Nút tìm kiếm từ khóa
        actions.add(ActionButton.builder(Component.text("🔍 Nhập Từ Khóa Tìm Kiếm", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false))
                .width(300)
                .tooltip(Component.text("Hiện tại: " + (filterState.getQuery() != null ? "\"" + filterState.getQuery() + "\"" : "(Không có)"), NamedTextColor.WHITE))
                .action(DialogAction.customClick((view, aud) -> {
                    if (aud instanceof Player p) runSync(() -> openSearchDialog(p, filterState));
                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                .build());

        // 2. Nút chọn danh mục
        actions.add(ActionButton.builder(Component.text("📁 Chọn Danh Mục Phân Loại", NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false))
                .width(300)
                .tooltip(Component.text("Hiện tại: " + filterState.getCategory().getDisplayName(), NamedTextColor.WHITE))
                .action(DialogAction.customClick((view, aud) -> {
                    if (aud instanceof Player p) runSync(() -> openCategoryDialog(p, filterState));
                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                .build());

        // 3. Nút chọn plugin
        actions.add(ActionButton.builder(Component.text("📦 Chọn Plugin / Namespace Nguồn", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false))
                .width(300)
                .tooltip(Component.text("Hiện tại: " + (filterState.getNamespace() != null ? filterState.getNamespace() : "Tất cả"), NamedTextColor.WHITE))
                .action(DialogAction.customClick((view, aud) -> {
                    if (aud instanceof Player p) runSync(() -> openPluginDialog(p, filterState));
                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                .build());

        // 4. Nút xóa bộ lọc
        if (!filterState.isDefault()) {
            actions.add(ActionButton.builder(Component.text("✖ Xóa Toàn Bộ Bộ Lọc", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false))
                    .width(300)
                    .tooltip(Component.text("Đặt lại từ khóa, danh mục và plugin về mặc định", NamedTextColor.GRAY))
                    .action(DialogAction.customClick((view, aud) -> {
                        if (aud instanceof Player p) runSync(() -> open(p, 0, ItemBrowserFilter.empty()));
                    }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                    .build());
        }

        ActionButton exitAction = ActionButton.builder(Component.text("Quay lại", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                .width(300)
                .action(DialogAction.customClick((view, aud) -> {
                    if (aud instanceof Player p) runSync(() -> open(p, 0, filterState));
                }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(3)).build()))
                .build();

        Dialog menuDialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Menu Bộ Lọc & Tìm Kiếm", NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(actions, exitAction, 1)));

        player.showDialog(menuDialog);
    }

    private Set<String> getDiscoveredNamespaces() {
        return itemRegistry.all().stream()
                .map(def -> def.getNamespace())
                .filter(ns -> ns != null && !ns.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void runSync(Runnable task) {
        if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, task);
        } else {
            task.run();
        }
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
        ItemBrowserFilter filter = holder.getFilter();

        if (slot == PREV_SLOT && holder.getPage() > 0) {
            open(player, holder.getPage() - 1, filter);
            return true;
        }
        if (slot == NEXT_SLOT && holder.getPage() < holder.getTotalPages() - 1) {
            open(player, holder.getPage() + 1, filter);
            return true;
        }
        if (slot == FILTER_SLOT) {
            if (click.isLeftClick()) {
                openSearchDialog(player, filter);
            } else if (click.isRightClick()) {
                openFilterMenuDialog(player, filter);
            } else if (click == ClickType.MIDDLE) {
                if (!filter.isDefault()) {
                    open(player, 0, ItemBrowserFilter.empty());
                } else {
                    openFilterMenuDialog(player, filter);
                }
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
                    recipeViewer.open(player, itemId, holder.getPage(), holder.getFilter());
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerFilters.remove(event.getPlayer().getUniqueId());
        playerPages.remove(event.getPlayer().getUniqueId());
    }
}
