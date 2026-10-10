package vn.haohan.itemcore.item;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.internal.event.ItemEventRouter;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;

import static org.junit.jupiter.api.Assertions.*;

public class ItemEventRouterVanillaTest {

    private ItemService mockItemService;
    private ItemEventRouter router;

    @BeforeEach
    void setUp() {
        mockItemService = Mockito.mock(ItemService.class);
        HaoHanItemCore core = Mockito.mock(HaoHanItemCore.class);
        Mockito.when(core.getItemService()).thenReturn(mockItemService);
        Mockito.when(core.getRecipeService()).thenReturn(Mockito.mock(vn.haohan.itemcore.api.recipe.RecipeService.class));
        Mockito.when(core.getItemFactory()).thenReturn(Mockito.mock(vn.haohan.itemcore.api.item.ItemFactory.class));
        HaoHanItemCore.setInstance(core);

        DefaultItemRegistry registry = new DefaultItemRegistry(null);
        org.bukkit.plugin.Plugin plugin = Mockito.mock(org.bukkit.plugin.Plugin.class);
        Mockito.when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("RouterTest"));

        router = new ItemEventRouter(registry, plugin);
    }

    @Test
    void testPrepareItemCraftValidatesResult() {
        PrepareItemCraftEvent event = Mockito.mock(PrepareItemCraftEvent.class);
        CraftingInventory inv = Mockito.mock(CraftingInventory.class);
        ItemStack result = Mockito.mock(ItemStack.class);
        Mockito.when(result.getType()).thenReturn(Material.DIAMOND_PICKAXE);
        Mockito.when(inv.getResult()).thenReturn(result);
        Mockito.when(event.getInventory()).thenReturn(inv);

        ItemStack converted = Mockito.mock(ItemStack.class);
        Mockito.when(mockItemService.validateAndUpdate(result)).thenReturn(converted);

        router.onPrepareItemCraft(event);

        Mockito.verify(mockItemService).validateAndUpdate(result);
        Mockito.verify(inv).setResult(converted);
    }

    @Test
    void testEntityPickupItemValidatesItem() {
        EntityPickupItemEvent event = Mockito.mock(EntityPickupItemEvent.class);
        Player player = Mockito.mock(Player.class);
        Item itemEntity = Mockito.mock(Item.class);
        ItemStack itemStack = Mockito.mock(ItemStack.class);

        Mockito.when(event.getEntity()).thenReturn(player);
        Mockito.when(event.getItem()).thenReturn(itemEntity);
        Mockito.when(itemEntity.getItemStack()).thenReturn(itemStack);

        router.onEntityPickupItem(event);

        Mockito.verify(mockItemService).validateAndUpdate(itemStack);
    }

    @Test
    void testPlayerItemHeldValidatesItem() {
        PlayerItemHeldEvent event = Mockito.mock(PlayerItemHeldEvent.class);
        Player player = Mockito.mock(Player.class);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        ItemStack itemStack = Mockito.mock(ItemStack.class);

        Mockito.when(event.getPlayer()).thenReturn(player);
        Mockito.when(event.getNewSlot()).thenReturn(3);
        Mockito.when(player.getInventory()).thenReturn(inv);
        Mockito.when(inv.getItem(3)).thenReturn(itemStack);

        router.onPlayerItemHeld(event);

        Mockito.verify(mockItemService).validateAndUpdate(itemStack);
    }
}
