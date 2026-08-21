package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.item.ItemDefinition;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * InventoryHolder cho ItemBrowserGUI.
 * Gắn trực tiếp dữ liệu phiên duyệt item vào container Inventory.
 */
public final class ItemBrowserHolder implements InventoryHolder {

    private final List<ItemDefinition> items;
    private final int page;
    private final int totalPages;
    private Inventory inventory;

    public ItemBrowserHolder(List<ItemDefinition> items, int page, int totalPages) {
        this.items = items;
        this.page = page;
        this.totalPages = totalPages;
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

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
