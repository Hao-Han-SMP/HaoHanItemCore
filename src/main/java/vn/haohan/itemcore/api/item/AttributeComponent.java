package vn.haohan.itemcore.api.item;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Adds a Paper attribute modifier to newly-created stacks. */
public record AttributeComponent(Attribute attribute, NamespacedKey attributeKey, double amount,
                                 AttributeModifier.Operation operation, EquipmentSlotGroup slotGroup) implements ItemComponent {
    public AttributeComponent {
        if ((attribute == null && attributeKey == null) || operation == null || slotGroup == null) {
            throw new IllegalArgumentException("Attribute component fields cannot be null");
        }
    }

    public AttributeComponent(Attribute attribute, double amount, AttributeModifier.Operation operation,
                              EquipmentSlotGroup slotGroup) {
        this(attribute, attribute != null ? attribute.getKey() : null, amount, operation, slotGroup);
    }

    public AttributeComponent(NamespacedKey attributeKey, double amount, AttributeModifier.Operation operation,
                              EquipmentSlotGroup slotGroup) {
        this(null, attributeKey, amount, operation, slotGroup);
    }

    public AttributeComponent(Attribute attribute, double amount) {
        this(attribute, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
    }

    public AttributeComponent(NamespacedKey attributeKey, double amount) {
        this(attributeKey, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
    }

    @Override
    public void apply(ItemStack item, ItemDefinition definition) {
        var meta = item.getItemMeta();
        if (meta == null) return;

        Attribute attr = this.attribute;
        NamespacedKey targetKey = this.attributeKey;
        if (attr == null && targetKey != null) {
            try {
                attr = org.bukkit.Registry.ATTRIBUTE.get(targetKey);
            } catch (Throwable t) {
                return;
            }
        }
        if (attr == null) return;
        if (targetKey == null) {
            targetKey = attr.getKey();
        }

        var key = new NamespacedKey("haohanitemcore", "attribute_" + targetKey.getKey().replace('.', '_').toLowerCase(Locale.ROOT));

        var existingModifiers = meta.getAttributeModifiers(attr);
        if (existingModifiers != null) {
            for (var existing : existingModifiers) {
                if (existing.getKey().equals(key)) {
                    if (existing.getAmount() == amount && existing.getOperation() == operation && existing.getSlotGroup() == slotGroup) {
                        return;
                    }
                    meta.removeAttributeModifier(attr, existing);
                    break;
                }
            }
        }

        meta.addAttributeModifier(attr, new AttributeModifier(key, amount, operation, slotGroup));
        item.setItemMeta(meta);
    }
}
