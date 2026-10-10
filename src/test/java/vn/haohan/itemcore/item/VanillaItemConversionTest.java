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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.internal.item.DefaultItemFactory;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.internal.item.DefaultItemService;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class VanillaItemConversionTest {

    private DefaultItemRegistry registry;
    private DefaultItemService service;
    private Plugin plugin;
    private NamespacedKey itemIdKey;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("ConversionTest");
        registry = new DefaultItemRegistry(logger);
        DefaultItemFactory factory = new DefaultItemFactory(registry, null);
        service = new DefaultItemService(registry, factory, null);
        itemIdKey = new NamespacedKey("haohanitemcore", DefaultItemFactory.ITEM_ID_KEY_NAME);
    }

    @Test
    void testValidateAndUpdateConvertsVanillaItemWhenOverwriteExists() {
        ItemDefinition def = ItemDefinition.builder("minecraft:diamond_pickaxe")
                .material(Material.DIAMOND_PICKAXE)
                .displayName("§bEnhanced Diamond Pickaxe")
                .tier("diamond", 6)
                .build();
        registry.register(def);

        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);

        Mockito.when(item.getType()).thenReturn(Material.DIAMOND_PICKAXE);
        Mockito.when(item.getAmount()).thenReturn(1);
        Mockito.when(item.hasItemMeta()).thenReturn(true);
        Mockito.when(item.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);
        Mockito.when(pdc.has(itemIdKey, PersistentDataType.STRING)).thenReturn(false);

        ItemStack result = service.validateAndUpdate(item);
        assertSame(item, result);

        // Verify that itemIdKey was set on PDC with the overwrite ID
        Mockito.verify(pdc).set(itemIdKey, PersistentDataType.STRING, "minecraft:diamond_pickaxe");
    }

    @Test
    void testValidateAndUpdateIgnoresVanillaItemWithoutOverwrite() {
        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);

        Mockito.when(item.getType()).thenReturn(Material.IRON_PICKAXE);
        Mockito.when(item.getAmount()).thenReturn(1);
        Mockito.when(item.hasItemMeta()).thenReturn(true);
        Mockito.when(item.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);
        Mockito.when(pdc.has(itemIdKey, PersistentDataType.STRING)).thenReturn(false);

        ItemStack result = service.validateAndUpdate(item);
        assertSame(item, result);

        // Should NOT set itemIdKey
        Mockito.verify(pdc, Mockito.never()).set(Mockito.eq(itemIdKey), Mockito.eq(PersistentDataType.STRING), Mockito.anyString());
    }
}
