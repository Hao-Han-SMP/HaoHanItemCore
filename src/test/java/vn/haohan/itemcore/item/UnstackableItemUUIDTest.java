package vn.haohan.itemcore.item;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemType;
import vn.haohan.itemcore.internal.item.DefaultItemFactory;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UnstackableItemUUIDTest {

    private DefaultItemRegistry registry;
    private NamespacedKey uuidKey;

    @BeforeEach
    void setUp() {
        registry = new DefaultItemRegistry(Logger.getLogger("UnstackableItemUUIDTest"));
        uuidKey = new NamespacedKey("haohanitemcore", "uuid");
    }

    @Test
    void testUnstackableItemWithoutUniqueFlagDoesNotGetUUID() {
        ItemDefinition def = ItemDefinition.builder("haohan:telescope_broken")
                .material(Material.PAPER)
                .displayName("Kính viễn vọng")
                .type(ItemType.MATERIAL)
                .maxStackSize(1)
                .build();
        registry.register(def);

        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);

        Mockito.when(item.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);
        Mockito.when(pdc.has(uuidKey, PersistentDataType.STRING)).thenReturn(false);

        DefaultItemFactory.applyComponents(item, def, null);

        // Verify UUID was NOT added because unique_uuid flag was not set
        Mockito.verify(pdc, Mockito.never()).set(Mockito.eq(uuidKey), Mockito.eq(PersistentDataType.STRING), Mockito.anyString());
    }

    @Test
    void testStaleUUIDIsRemovedFromNonUniqueUnstackableItem() {
        ItemDefinition def = ItemDefinition.builder("haohan:telescope_broken")
                .material(Material.PAPER)
                .displayName("Kính viễn vọng")
                .type(ItemType.MATERIAL)
                .maxStackSize(1)
                .build();
        registry.register(def);

        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);

        Mockito.when(item.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);
        Mockito.when(pdc.has(uuidKey, PersistentDataType.STRING)).thenReturn(true);

        DefaultItemFactory.applyComponents(item, def, null);

        // Verify stale UUID was removed
        Mockito.verify(pdc).remove(uuidKey);
    }

    @Test
    void testUnstackableItemWithUniqueFlagGetsUUID() {
        ItemDefinition def = ItemDefinition.builder("haohan:unique_sword")
                .material(Material.DIAMOND_SWORD)
                .displayName("Unique Sword")
                .type(ItemType.WEAPON)
                .maxStackSize(1)
                .properties(Map.of("unique_uuid", true))
                .build();
        registry.register(def);

        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);

        Mockito.when(item.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);
        Mockito.when(pdc.has(uuidKey, PersistentDataType.STRING)).thenReturn(false);

        DefaultItemFactory.applyComponents(item, def, null);

        // Verify UUID was added because unique_uuid flag was true
        Mockito.verify(pdc).set(Mockito.eq(uuidKey), Mockito.eq(PersistentDataType.STRING), Mockito.anyString());
    }
}
