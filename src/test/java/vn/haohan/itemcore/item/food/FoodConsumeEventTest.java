package vn.haohan.itemcore.item.food;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.item.ItemBehavior;
import vn.haohan.itemcore.api.item.ItemContext;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.food.FoodComponent;
import vn.haohan.itemcore.internal.event.ItemEventRouter;
import vn.haohan.itemcore.internal.item.DefaultItemFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FoodConsumeEventTest {

    private ItemRegistry registry;
    private ItemEventRouter router;

    @BeforeEach
    void setUp() {
        HaoHanItemCore core = Mockito.mock(HaoHanItemCore.class);
        Mockito.when(core.getItemService()).thenReturn(Mockito.mock(vn.haohan.itemcore.api.item.ItemService.class));
        Mockito.when(core.getRecipeService()).thenReturn(Mockito.mock(vn.haohan.itemcore.api.recipe.RecipeService.class));
        Mockito.when(core.getItemFactory()).thenReturn(Mockito.mock(vn.haohan.itemcore.api.item.ItemFactory.class));
        HaoHanItemCore.setInstance(core);

        registry = Mockito.mock(ItemRegistry.class);
        router = new ItemEventRouter(registry, null);
    }

    @Test
    void testOnPlayerItemConsumeTriggersBehaviorAndActions() {
        AtomicBoolean behaviorCalled = new AtomicBoolean(false);
        AtomicBoolean actionCalled = new AtomicBoolean(false);

        ItemBehavior behavior = new ItemBehavior() {
            @Override
            public void onConsume(ItemContext context) {
                behaviorCalled.set(true);
            }
        };

        ItemDefinition def = ItemDefinition.builder("custom:pizza")
                .material(Material.BREAD)
                .behavior(behavior)
                .food(food -> food
                        .nutrition(8)
                        .onConsume(ctx -> actionCalled.set(true))
                )
                .build();

        Mockito.when(registry.get("custom:pizza")).thenReturn(def);

        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        PersistentDataContainer pdc = Mockito.mock(PersistentDataContainer.class);
        NamespacedKey itemIdKey = new NamespacedKey("haohanitemcore", DefaultItemFactory.ITEM_ID_KEY_NAME);

        Mockito.when(item.hasItemMeta()).thenReturn(true);
        Mockito.when(item.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.getPersistentDataContainer()).thenReturn(pdc);
        Mockito.when(pdc.has(Mockito.any(NamespacedKey.class), Mockito.eq(PersistentDataType.STRING))).thenReturn(true);
        Mockito.when(pdc.get(Mockito.any(NamespacedKey.class), Mockito.eq(PersistentDataType.STRING))).thenReturn("custom:pizza");

        Player player = Mockito.mock(Player.class);
        PlayerItemConsumeEvent event = Mockito.mock(PlayerItemConsumeEvent.class);
        Mockito.when(event.getPlayer()).thenReturn(player);
        Mockito.when(event.getItem()).thenReturn(item);

        router.onPlayerItemConsume(event);

        assertTrue(behaviorCalled.get());
        assertTrue(actionCalled.get());
    }
}
