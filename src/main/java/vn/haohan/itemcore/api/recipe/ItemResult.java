package vn.haohan.itemcore.api.recipe;

/**
 * Kết quả output của recipe.
 * Tham chiếu tới Item System bằng item ID.
 * 
 * @param item Item ID (ví dụ: "machine:steel_plate" hoặc "minecraft:iron_ingot")
 * @param amount Số lượng output
 */
public record ItemResult(
        String item,
        int amount
) {
    public ItemResult {
        if (item == null || item.isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be at least 1");
        }
    }

    public ItemResult(String item) {
        this(item, 1);
    }
}
