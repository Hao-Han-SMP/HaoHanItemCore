package vn.haohan.itemmanager.api.recipe;

/**
 * Các loại recipe được hỗ trợ.
 */
public enum RecipeType {
    /** Crafting table - có pattern cố định */
    SHAPED,
    /** Crafting table - không quan tâm vị trí */
    SHAPELESS,
    /** Furnace */
    SMELTING,
    /** Blast furnace */
    BLASTING,
    /** Smoker */
    SMOKING,
    /** Campfire */
    CAMPFIRE,
    /** Stonecutter */
    STONECUTTING,
    /** Smithing table */
    SMITHING,
    /** Custom machine - plugin tự xử lý */
    MACHINE
}
