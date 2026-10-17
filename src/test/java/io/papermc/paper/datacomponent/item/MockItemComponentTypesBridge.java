package io.papermc.paper.datacomponent.item;

import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MockItemComponentTypesBridge implements ItemComponentTypesBridge {

    @Override
    public FoodProperties.Builder food() {
        return new FoodProperties.Builder() {
            private int nutrition = 0;
            private float saturation = 0f;
            private boolean canAlwaysEat = false;

            @Override
            public FoodProperties.Builder nutrition(int nutrition) {
                this.nutrition = nutrition;
                return this;
            }

            @Override
            public FoodProperties.Builder saturation(float saturation) {
                this.saturation = saturation;
                return this;
            }

            @Override
            public FoodProperties.Builder canAlwaysEat(boolean canAlwaysEat) {
                this.canAlwaysEat = canAlwaysEat;
                return this;
            }

            @Override
            public FoodProperties build() {
                int n = this.nutrition;
                float s = this.saturation;
                boolean c = this.canAlwaysEat;
                return new FoodProperties() {
                    @Override public int nutrition() { return n; }
                    @Override public float saturation() { return s; }
                    @Override public boolean canAlwaysEat() { return c; }
                    @Override public FoodProperties.Builder toBuilder() {
                        return food().nutrition(n).saturation(s).canAlwaysEat(c);
                    }
                };
            }
        };
    }

    @Override
    public Consumable.Builder consumable() {
        return new Consumable.Builder() {
            private float consumeSeconds = 1.6f;
            private ItemUseAnimation animation = ItemUseAnimation.EAT;
            private boolean hasParticles = true;
            private final List<ConsumeEffect> effects = new ArrayList<>();

            @Override
            public Consumable.Builder consumeSeconds(float seconds) {
                this.consumeSeconds = seconds;
                return this;
            }

            @Override
            public Consumable.Builder animation(ItemUseAnimation animation) {
                this.animation = animation;
                return this;
            }

            @Override
            public Consumable.Builder sound(Key sound) {
                return this;
            }

            @Override
            public Consumable.Builder hasConsumeParticles(boolean hasParticles) {
                this.hasParticles = hasParticles;
                return this;
            }

            @Override
            public Consumable.Builder effects(List<ConsumeEffect> effects) {
                this.effects.clear();
                if (effects != null) this.effects.addAll(effects);
                return this;
            }

            @Override
            public Consumable.Builder addEffect(ConsumeEffect effect) {
                if (effect != null) this.effects.add(effect);
                return this;
            }

            @Override
            public Consumable.Builder addEffects(List<ConsumeEffect> effects) {
                if (effects != null) this.effects.addAll(effects);
                return this;
            }

            @Override
            public Consumable build() {
                float cs = this.consumeSeconds;
                ItemUseAnimation anim = this.animation;
                boolean hp = this.hasParticles;
                List<ConsumeEffect> effs = List.copyOf(this.effects);
                return new Consumable() {
                    @Override public float consumeSeconds() { return cs; }
                    @Override public ItemUseAnimation animation() { return anim; }
                    @Override public Key sound() { return null; }
                    @Override public boolean hasConsumeParticles() { return hp; }
                    @Override public List<ConsumeEffect> consumeEffects() { return effs; }
                    @Override public Consumable.Builder toBuilder() {
                        Consumable.Builder b = consumable().consumeSeconds(cs).animation(anim).hasConsumeParticles(hp);
                        b.effects(effs);
                        return b;
                    }
                };
            }
        };
    }

    // Default implementations returning null for unused component builders
    @Override public ChargedProjectiles.Builder chargedProjectiles() { return null; }
    @Override public PotDecorations.Builder potDecorations() { return null; }
    @Override public ItemLore.Builder lore() { return null; }
    @Override public ItemEnchantments.Builder enchantments() { return null; }
    @Override public ItemAttributeModifiers.Builder modifiers() { return null; }
    @Override public DyedItemColor.Builder dyedItemColor() { return null; }
    @Override public PotionContents.Builder potionContents() { return null; }
    @Override public BundleContents.Builder bundleContents() { return null; }
    @Override public SuspiciousStewEffects.Builder suspiciousStewEffects() { return null; }
    @Override public MapItemColor.Builder mapItemColor() { return null; }
    @Override public MapDecorations.Builder mapDecorations() { return null; }
    @Override public MapDecorations.DecorationEntry decorationEntry(org.bukkit.map.MapCursor.Type type, double x, double y, float rot) { return null; }
    @Override public SeededContainerLoot.Builder seededContainerLoot(Key lootTable) { return null; }
    @Override public WrittenBookContent.Builder writtenBookContent(io.papermc.paper.text.Filtered<String> title, String author) { return null; }
    @Override public WritableBookContent.Builder writeableBookContent() { return null; }
    @Override public ItemArmorTrim.Builder itemArmorTrim(org.bukkit.inventory.meta.trim.ArmorTrim trim) { return null; }
    @Override public LodestoneTracker.Builder lodestoneTracker() { return null; }
    @Override public Fireworks.Builder fireworks() { return null; }
    @Override public ResolvableProfile.Builder resolvableProfile() { return null; }
    @Override public ResolvableProfile.SkinPatchBuilder skinPatch() { return null; }
    @Override public ResolvableProfile.SkinPatch emptySkinPatch() { return null; }
    @Override public ResolvableProfile resolvableProfile(com.destroystokyo.paper.profile.PlayerProfile profile) { return null; }
    @Override public BannerPatternLayers.Builder bannerPatternLayers() { return null; }
    @Override public BlockItemDataProperties.Builder blockItemStateProperties() { return null; }
    @Override public ItemContainerContents.Builder itemContainerContents() { return null; }
    @Override public JukeboxPlayable.Builder jukeboxPlayable(org.bukkit.JukeboxSong song) { return null; }
    @Override public Tool.Builder tool() { return null; }
    @Override public Tool.Rule rule(io.papermc.paper.registry.set.RegistryKeySet<org.bukkit.block.BlockType> blocks, Float speed, net.kyori.adventure.util.TriState correctForDrops) { return null; }
    @Override public ItemAdventurePredicate.Builder itemAdventurePredicate() { return null; }
    @Override public CustomModelData.Builder customModelData() { return null; }
    @Override public MapId mapId(int id) { return null; }
    @Override public UseRemainder useRemainder(ItemStack remainder) { return null; }
    @Override public UseCooldown.Builder useCooldown(float seconds) { return null; }
    @Override public DamageResistant damageResistant(io.papermc.paper.registry.tag.TagKey<org.bukkit.damage.DamageType> damageType) { return null; }
    @Override public Enchantable enchantable(int value) { return null; }
    @Override public Repairable repairable(io.papermc.paper.registry.set.RegistryKeySet<org.bukkit.inventory.ItemType> items) { return null; }
    @Override public Equippable.Builder equippable(org.bukkit.inventory.EquipmentSlot slot) { return null; }
    @Override public DeathProtection.Builder deathProtection() { return null; }
    @Override public OminousBottleAmplifier ominousBottleAmplifier(int amplifier) { return null; }
    @Override public BlocksAttacks.Builder blocksAttacks() { return null; }
    @Override public TooltipDisplay.Builder tooltipDisplay() { return null; }
    @Override public Weapon.Builder weapon() { return null; }
    @Override public KineticWeapon.Builder kineticWeapon() { return null; }
    @Override public UseEffects.Builder useEffects() { return null; }
    @Override public PiercingWeapon.Builder piercingWeapon() { return null; }
    @Override public AttackRange.Builder attackRange() { return null; }
    @Override public SwingAnimation.Builder swingAnimation() { return null; }
    @Override public KineticWeapon.Condition kineticWeaponCondition(int maxDurationTicks, float damageMultiplier, float forwardMovement) { return null; }
}
