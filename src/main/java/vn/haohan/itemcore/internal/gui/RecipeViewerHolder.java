package vn.haohan.itemcore.internal.gui;

import vn.haohan.itemcore.api.recipe.RecipeDefinition;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * InventoryHolder cho RecipeViewerGUI.
 * Gắn trực tiếp dữ liệu công thức và chỉ số recipe vào container Inventory.
 */
public final class RecipeViewerHolder implements InventoryHolder {

    private final String itemId;
    private final List<RecipeDefinition> recipes;
    private final int index;
    private final int returnPage;
    private final String returnSearchQuery;
    private Inventory inventory;

    public RecipeViewerHolder(String itemId, List<RecipeDefinition> recipes, int index) {
        this(itemId, recipes, index, 0, null);
    }

    public RecipeViewerHolder(String itemId, List<RecipeDefinition> recipes, int index, int returnPage, String returnSearchQuery) {
        this.itemId = itemId;
        this.recipes = recipes;
        this.index = index;
        this.returnPage = returnPage;
        this.returnSearchQuery = returnSearchQuery;
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
        return returnSearchQuery;
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
