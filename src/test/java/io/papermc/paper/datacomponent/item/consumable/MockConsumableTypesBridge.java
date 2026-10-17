package io.papermc.paper.datacomponent.item.consumable;

import net.kyori.adventure.key.Key;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class MockConsumableTypesBridge implements ConsumableTypesBridge {

    @Override
    public ConsumeEffect.ApplyStatusEffects applyStatusEffects(List<PotionEffect> effects, float probability) {
        List<PotionEffect> effs = effects != null ? List.copyOf(effects) : List.of();
        return new ConsumeEffect.ApplyStatusEffects() {
            @Override public List<PotionEffect> effects() { return effs; }
            @Override public float probability() { return probability; }
        };
    }

    @Override
    public ConsumeEffect.RemoveStatusEffects removeStatusEffects(io.papermc.paper.registry.set.RegistryKeySet<PotionEffectType> effects) {
        return null;
    }

    @Override
    public ConsumeEffect.ClearAllStatusEffects clearAllStatusEffects() {
        return null;
    }

    @Override
    public ConsumeEffect.PlaySound playSoundEffect(Key sound) {
        return null;
    }

    @Override
    public ConsumeEffect.TeleportRandomly teleportRandomlyEffect(float diameter) {
        return null;
    }
}
