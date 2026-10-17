package vn.haohan.itemcore.api.item.food;

import vn.haohan.itemcore.api.item.ItemContext;

/**
 * Functional interface for custom developer actions triggered when an item is consumed.
 */
@FunctionalInterface
public interface CustomFoodAction {
    void execute(ItemContext context);
}
