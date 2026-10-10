package vn.haohan.itemcore.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.internal.item.DefaultItemFactory;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.internal.item.DefaultItemService;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class VanillaItemOverwriteIntegrationTest {

    private DefaultItemRegistry registry;
    private DefaultItemService itemService;
    private NamespacedKey itemIdKey;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("IntegrationTest");
        registry = new DefaultItemRegistry(logger);
        DefaultItemFactory factory = new DefaultItemFactory(registry, null);
        itemService = new DefaultItemService(registry, factory, null);
        itemIdKey = new NamespacedKey("haohanitemcore", DefaultItemFactory.ITEM_ID_KEY_NAME);
    }

    @Test
    void testFullVanillaOverwriteLifecycle() {
        // 1. Khai báo item overwrite minecraft:diamond_pickaxe
        ItemDefinition diaPickOverwrite = ItemDefinition.builder("minecraft:diamond_pickaxe")
                .material(Material.DIAMOND_PICKAXE)
                .displayName("§bDiamond Pickaxe")
                .addLore("§8Tier: §c6 (Diamond)")
                .addLore("§7Vật phẩm game gốc đã được thiết lập thông số.")
                .type(ItemType.TOOL)
                .toolType(ToolType.PICKAXE)
                .tier("diamond", 6)
                .miningSpeed(8.0f)
                .property("max_damage", 1561)
                .build();

        assertTrue(diaPickOverwrite.isVanillaOverwrite());
        registry.register(diaPickOverwrite);

        // 2. Tra cứu từ Registry
        assertSame(diaPickOverwrite, registry.getVanillaOverwrite(Material.DIAMOND_PICKAXE));
        assertNull(registry.getVanillaOverwrite(Material.STONE_PICKAXE));

        // 3. Giả lập một ItemStack vanilla do người chơi vừa craft hoặc nhặt
        ItemStack vanillaPick = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);

        Mockito.when(vanillaPick.getType()).thenReturn(Material.DIAMOND_PICKAXE);
        Mockito.when(vanillaPick.getAmount()).thenReturn(1);
        Mockito.when(vanillaPick.hasItemMeta()).thenReturn(true);
        Mockito.when(vanillaPick.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);

        // Ban đầu chưa có tag item_id
        Mockito.when(pdc.has(itemIdKey, PersistentDataType.STRING)).thenReturn(false);

        // 4. Cho qua validateAndUpdate
        ItemStack updated = itemService.validateAndUpdate(vanillaPick);
        assertSame(vanillaPick, updated);

        // Xác nhận đã được gắn ID minecraft:diamond_pickaxe
        Mockito.verify(pdc).set(itemIdKey, PersistentDataType.STRING, "minecraft:diamond_pickaxe");

        // Khi đã có ID, mô phỏng pdc.has và pdc.get
        Mockito.when(pdc.has(itemIdKey, PersistentDataType.STRING)).thenReturn(true);
        Mockito.when(pdc.get(itemIdKey, PersistentDataType.STRING)).thenReturn("minecraft:diamond_pickaxe");

        assertTrue(itemService.isCustomItem(vanillaPick));
        assertTrue(itemService.isItem(vanillaPick, "minecraft:diamond_pickaxe"));
        assertEquals("minecraft:diamond_pickaxe", itemService.getId(vanillaPick));
    }
}
