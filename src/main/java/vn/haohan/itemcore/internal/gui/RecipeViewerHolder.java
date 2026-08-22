package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.recipe.RecipeDefinition;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * InventoryHolder cho RecipeViewerGUI.
 * Gắn trực tiếp dữ liệu công thức, chỉ số recipe và trạng thái filter để quay về Item Browser.
 */
public final class RecipeViewerHolder implements InventoryHolder {

    private final String itemId;
    private final List<RecipeDefinition> recipes;
    private final int index;
    private final int returnPage;
    private final ItemBrowserFilter returnFilter;
    private Inventory inventory;

    public RecipeViewerHolder(String itemId, List<RecipeDefinition> recipes, int index) {
        this(itemId, recipes, index, 0, ItemBrowserFilter.empty());
    }

    public RecipeViewerHolder(String itemId, List<RecipeDefinition> recipes, int index, int returnPage, String returnSearchQuery) {
        this(itemId, recipes, index, returnPage, new ItemBrowserFilter(returnSearchQuery, null, null));
    }

    public RecipeViewerHolder(String itemId, List<RecipeDefinition> recipes, int index, int returnPage, ItemBrowserFilter returnFilter) {
        this.itemId = itemId;
        this.recipes = recipes;
        this.index = index;
        this.returnPage = returnPage;
        this.returnFilter = (returnFilter != null) ? returnFilter : ItemBrowserFilter.empty();
    }

    public String getItemId() {
        return itemId;
    }

    public List<RecipeDefinition> getRecipes() {
        return recipes;
    }

    public int getIndex() {
        return index;
    }

    public int getReturnPage() {
        return returnPage;
    }

    public String getReturnSearchQuery() {
        return returnFilter.getQuery();
    }

    public ItemBrowserFilter getReturnFilter() {
        return returnFilter;
    }

    public RecipeDefinition currentRecipe() {
        return recipes.get(index);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
