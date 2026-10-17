package vn.haohan.itemcore.api.item.food;

import org.bukkit.potion.PotionEffect;
import java.util.Objects;

public record FoodEffectEntry(PotionEffect effect, float probability) {
    public FoodEffectEntry {
        Objects.requireNonNull(effect, "PotionEffect cannot be null");
        if (probability < 0.0f || probability > 1.0f) {
            throw new IllegalArgumentException("Probability must be between 0.0 and 1.0");
        }
    }
}
