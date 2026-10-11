package vn.haohan.itemcore.item;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import vn.haohan.itemcore.api.item.AttributeComponent;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AttributeComponentTest {

    private Attribute createFakeAttribute(String keyName) {
        NamespacedKey key = NamespacedKey.minecraft(keyName);
        return (Attribute) Proxy.newProxyInstance(
                Attribute.class.getClassLoader(),
                new Class<?>[]{Attribute.class},
                (proxy, method, args) -> {
                    if ("getKey".equals(method.getName())) {
                        return key;
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return key.hashCode();
                    }
                    return null;
                }
        );
    }

    @Test
    void testAttributeComponentIsIdempotent() {
        Attribute fakeAttr = createFakeAttribute("generic.attack_damage");

        AttributeComponent component = new AttributeComponent(
                fakeAttr,
                3.0,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );

        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(item.getItemMeta()).thenReturn(meta);

        NamespacedKey expectedKey = new NamespacedKey("haohanitemcore", "attribute_generic_attack_damage");

        // First apply: no existing modifiers
        Mockito.when(meta.getAttributeModifiers(fakeAttr)).thenReturn(null);

        component.apply(item, null);

        ArgumentCaptor<AttributeModifier> modifierCaptor = ArgumentCaptor.forClass(AttributeModifier.class);
        Mockito.verify(meta, Mockito.times(1)).addAttributeModifier(Mockito.eq(fakeAttr), modifierCaptor.capture());
        assertEquals(expectedKey, modifierCaptor.getValue().getKey());
        assertEquals(3.0, modifierCaptor.getValue().getAmount());

        // Second apply: modifier already exists with exact same values
        AttributeModifier existingMod = modifierCaptor.getValue();
        Mockito.when(meta.getAttributeModifiers(fakeAttr)).thenReturn(List.of(existingMod));

        component.apply(item, null);

        // Verify addAttributeModifier was NOT called a second time
        Mockito.verify(meta, Mockito.times(1)).addAttributeModifier(Mockito.eq(fakeAttr), Mockito.any());
    }

    @Test
    void testAttributeComponentUpdatesWhenAmountChanges() {
        Attribute fakeAttr = createFakeAttribute("generic.attack_damage");

        AttributeComponent component = new AttributeComponent(
                fakeAttr,
                5.0,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );

        ItemStack item = Mockito.mock(ItemStack.class);
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(item.getItemMeta()).thenReturn(meta);

        NamespacedKey expectedKey = new NamespacedKey("haohanitemcore", "attribute_generic_attack_damage");
        AttributeModifier oldMod = new AttributeModifier(expectedKey, 3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);

        Mockito.when(meta.getAttributeModifiers(fakeAttr)).thenReturn(List.of(oldMod));

        component.apply(item, null);

        // Should remove old modifier and add new modifier with updated amount
        Mockito.verify(meta).removeAttributeModifier(fakeAttr, oldMod);
        ArgumentCaptor<AttributeModifier> modifierCaptor = ArgumentCaptor.forClass(AttributeModifier.class);
        Mockito.verify(meta).addAttributeModifier(Mockito.eq(fakeAttr), modifierCaptor.capture());
        assertEquals(5.0, modifierCaptor.getValue().getAmount());
    }
}
