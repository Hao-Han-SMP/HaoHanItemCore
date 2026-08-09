package vn.haohan.itemcore.api.item;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

/** Adds a Paper attribute modifier to newly-created stacks. */
public record AttributeComponent(Attribute attribute, double amount, AttributeModifier.Operation operation,
                                 EquipmentSlotGroup slotGroup) implements ItemComponent {
    public AttributeComponent {
        if (attribute == null || operation == null || slotGroup == null) {
            throw new IllegalArgumentException("Attribute component fields cannot be null");
        }
    }

    public AttributeComponent(Attribute attribute, double amount) {
        this(attribute, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
    }

    @Override
    public void apply(ItemStack item, ItemDefinition definition) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        var key = new NamespacedKey("haohanitemcore", "attribute_" + attribute.name().toLowerCase());
        meta.addAttributeModifier(attribute, new AttributeModifier(key, amount, operation, slotGroup));
        item.setItemMeta(meta);
    }
}
