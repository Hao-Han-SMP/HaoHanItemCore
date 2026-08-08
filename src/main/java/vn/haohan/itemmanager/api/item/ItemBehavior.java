package vn.haohan.itemmanager.api.item;

/**
 * Interface cho custom item behavior.
 * Mỗi item có thể đăng ký behavior riêng để xử lý event.
 * 
 * <p>Ví dụ:
 * <pre>
 * public class PlasmaSwordBehavior implements ItemBehavior {
 *     @Override
 *     public void onUse(ItemContext context) {
 *         context.player().sendMessage("§bPlasma activated!");
 *     }
 * }
 * </pre>
 */
public interface ItemBehavior {

    /**
     * Được gọi khi player sử dụng item (right-click).
     */
    default void onUse(ItemContext context) {}

    /**
     * Được gọi khi player interact với item.
     */
    default void onInteract(ItemContext context) {}

    /**
     * Được gọi khi player phá block bằng item.
     */
    default void onBreak(ItemContext context) {}

    /**
     * Được gọi khi item được craft.
     */
    default void onCraft(ItemContext context) {}

    /**
     * Được gọi khi player click item trong inventory.
     */
    default void onInventoryClick(ItemContext context) {}

    /**
     * Được gọi khi player drop item.
     */
    default void onDrop(ItemContext context) {}

    /**
     * Được gọi khi player nhặt item.
     */
    default void onPickup(ItemContext context) {}
}
