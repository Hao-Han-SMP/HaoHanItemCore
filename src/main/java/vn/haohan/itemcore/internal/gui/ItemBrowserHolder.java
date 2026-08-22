package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.item.ItemDefinition;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * InventoryHolder cho ItemBrowserGUI.
 * Gắn trực tiếp dữ liệu phiên duyệt item và trạng thái filter vào container Inventory.
 */
public final class ItemBrowserHolder implements InventoryHolder {

    private final List<ItemDefinition> items;
    private final int page;
    private final int totalPages;
    private final ItemBrowserFilter filter;
    private Inventory inventory;

    public ItemBrowserHolder(List<ItemDefinition> items, int page, int totalPages) {
        this(items, page, totalPages, ItemBrowserFilter.empty());
    }

    public ItemBrowserHolder(List<ItemDefinition> items, int page, int totalPages, String searchQuery) {
        this(items, page, totalPages, new ItemBrowserFilter(searchQuery, null, null));
    }

    public ItemBrowserHolder(List<ItemDefinition> items, int page, int totalPages, ItemBrowserFilter filter) {
        this.items = items;
        this.page = page;
        this.totalPages = totalPages;
        this.filter = (filter != null) ? filter : ItemBrowserFilter.empty();
    }

    public List<ItemDefinition> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public ItemBrowserFilter getFilter() {
        return filter;
    }

    public String getSearchQuery() {
        return filter.getQuery();
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
