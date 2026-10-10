package vn.haohan.itemcore.api.block;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import vn.haohan.itemcore.api.HaoHanItemCore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provider determining item drops when a custom block is harvested.
 */
@FunctionalInterface
public interface BlockDropProvider {

    /**
     * Calculates the list of items dropped from mining the block.
     *
     * @param context mining context
     * @return list of items dropped (never null, may be empty)
     */
    List<ItemStack> getDrops(MiningContext context);

    static Builder builder() {
        return new Builder();
    }

    final class Builder {
        private String defaultItemId;
        private int minAmount = 1;
        private int maxAmount = 1;
        private String silkTouchItemId;
        private boolean silkTouchDropsSelf = true;
        private boolean fortuneEnabled = false;
        private final List<DropRule> customRules = new ArrayList<>();

        public Builder defaultDrop(String itemId) {
            return defaultDrop(itemId, 1);
        }

        public Builder defaultDrop(String itemId, int amount) {
            return defaultDrop(itemId, amount, amount);
        }

        public Builder defaultDrop(String itemId, int min, int max) {
            this.defaultItemId = itemId;
            this.minAmount = Math.max(0, min);
            this.maxAmount = Math.max(this.minAmount, max);
            return this;
        }

        public Builder silkTouchDrop(String itemId) {
            this.silkTouchItemId = itemId;
            return this;
        }

        public Builder silkTouchDropsSelf() {
            return silkTouchDropsSelf(true);
        }

        public Builder silkTouchDropsSelf(boolean dropsSelf) {
            this.silkTouchDropsSelf = dropsSelf;
            return this;
        }

        public Builder fortune(boolean enabled) {
            this.fortuneEnabled = enabled;
            return this;
        }

        public Builder rule(Predicate<MiningContext> condition, Function<MiningContext, List<ItemStack>> provider) {
            Objects.requireNonNull(condition, "condition cannot be null");
            Objects.requireNonNull(provider, "provider cannot be null");
            customRules.add(new DropRule(condition, provider));
            return this;
        }

        public BlockDropProvider build() {
            return context -> {
                // 1. Evaluate custom rules first
                for (DropRule rule : customRules) {
                    if (rule.condition.test(context)) {
                        List<ItemStack> drops = rule.provider.apply(context);
                        return drops != null ? drops : Collections.emptyList();
                    }
                }

                // 2. Silk touch check
                boolean hasSilkTouch = context != null && context.hasSilkTouch();
                if (hasSilkTouch) {
                    if (silkTouchDropsSelf && context.definition() != null) {
                        ItemStack item = createItem(context.definition().getId(), 1);
                        return item != null ? List.of(item) : Collections.emptyList();
                    }
                    if (silkTouchItemId != null) {
                        ItemStack item = createItem(silkTouchItemId, 1);
                        return item != null ? List.of(item) : Collections.emptyList();
                    }
                }

                // 3. Default drop calculation
                String dropId = defaultItemId;
                if (dropId == null && context != null && context.definition() != null) {
                    dropId = context.definition().getDropItemId();
                    if (dropId == null || dropId.isBlank()) {
                        dropId = context.definition().getId();
                    }
                }

                if (dropId == null) {
                    return Collections.emptyList();
                }

                int baseAmount = minAmount == maxAmount
                        ? minAmount
                        : ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);

                // 4. Fortune calculation if applicable
                if (fortuneEnabled && !hasSilkTouch && context != null) {
                    int fortuneLevel = context.getFortuneLevel();
                    if (fortuneLevel > 0) {
                        // Vanilla formula: discrete uniform distribution (1 to fortuneLevel + 2) - 1, min 1
                        int bonus = ThreadLocalRandom.current().nextInt(fortuneLevel + 2) - 1;
                        if (bonus > 0) {
                            baseAmount += bonus;
                        }
                    }
                }

                if (baseAmount <= 0) {
                    return Collections.emptyList();
                }

                ItemStack dropped = createItem(dropId, baseAmount);
                return dropped != null ? List.of(dropped) : Collections.emptyList();
            };
        }

        private static ItemStack createItem(String id, int amount) {
            if (id == null) return null;
            try {
                var core = HaoHanItemCore.get();
                if (core != null && core.getItemService() != null) {
                    return core.getItemService().create(id, amount);
                }
            } catch (IllegalStateException ignored) {}

            Material mat = Material.matchMaterial(id.startsWith("minecraft:") ? id.substring(10) : id);
            if (mat != null) {
                return new ItemStack(mat, amount);
            }
            return null;
        }
    }

    record DropRule(Predicate<MiningContext> condition, Function<MiningContext, List<ItemStack>> provider) {}
}
