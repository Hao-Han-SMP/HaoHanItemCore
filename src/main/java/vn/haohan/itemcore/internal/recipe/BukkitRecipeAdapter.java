package vn.haohan.itemcore.internal.recipe;

import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.recipe.*;
import vn.haohan.itemcore.internal.item.DefaultItemFactory;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.recipe.*;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Chuyển đổi RecipeDefinition → Bukkit Recipe và đăng ký với server.
 * Xử lý tất cả các loại recipe: Shaped, Shapeless, Smelting, Blasting, Smoking, Campfire, Stonecutting.
 */
public final class BukkitRecipeAdapter {

    private final Plugin plugin;
    private final ItemRegistry itemRegistry;
    private final DefaultItemFactory itemFactory;
    private final Logger logger;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public BukkitRecipeAdapter(Plugin plugin, ItemRegistry itemRegistry, DefaultItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.logger = plugin.getLogger();
    }

    /**
     * Đăng ký RecipeDefinition với Bukkit server.
     * Trả về true nếu đăng ký thành công.
     */
    public boolean register(RecipeDefinition recipe) {
        try {
            org.bukkit.inventory.Recipe bukkitRecipe = toBukkitRecipe(recipe);
            if (bukkitRecipe == null) {
                // MACHINE type không register với Bukkit
                return true;
            }
            plugin.getServer().addRecipe(bukkitRecipe);
            NamespacedKey key = createKey(recipe);
            registeredKeys.add(key);
            return true;
        } catch (Exception e) {
            logger.warning("[BukkitRecipeAdapter] Failed to register recipe: " + recipe.getId());
            logger.warning("  Reason: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa tất cả recipe đã đăng ký.
     */
    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            plugin.getServer().removeRecipe(key);
        }
        registeredKeys.clear();
    }

    /**
     * Chuyển RecipeDefinition → Bukkit Recipe.
     */
    private org.bukkit.inventory.Recipe toBukkitRecipe(RecipeDefinition recipe) {
        return switch (recipe.getType()) {
            case SHAPED -> toShapedRecipe((ShapedRecipeDefinition) recipe);
            case SHAPELESS -> toShapelessRecipe(recipe);
            case SMELTING -> toSmeltingRecipe(recipe);
            case BLASTING -> toBlastingRecipe(recipe);
            case SMOKING -> toSmokingRecipe(recipe);
            case CAMPFIRE -> toCampfireRecipe(recipe);
            case STONECUTTING -> toStonecuttingRecipe(recipe);
            case SMITHING, MACHINE -> null; // Plugin tự xử lý
        };
    }

    private org.bukkit.inventory.ShapedRecipe toShapedRecipe(ShapedRecipeDefinition recipe) {
        NamespacedKey key = createKey(recipe);
        ItemStack result = createResultItem(recipe.getResult());
        org.bukkit.inventory.ShapedRecipe shaped = new org.bukkit.inventory.ShapedRecipe(key, result);

        // Set pattern
        shaped.shape(recipe.getPattern().toArray(new String[0]));

        // Set ingredients
        for (Map.Entry<Character, Ingredient> entry : recipe.getIngredientMap().entrySet()) {
            RecipeChoice choice = toRecipeChoice(entry.getValue());
            shaped.setIngredient(entry.getKey(), choice);
        }

        return shaped;
    }

    private org.bukkit.inventory.ShapelessRecipe toShapelessRecipe(RecipeDefinition recipe) {
        NamespacedKey key = createKey(recipe);
        ItemStack result = createResultItem(recipe.getResult());
        org.bukkit.inventory.ShapelessRecipe shapeless = new org.bukkit.inventory.ShapelessRecipe(key, result);

        for (Ingredient ingredient : recipe.getIngredients()) {
            RecipeChoice choice = toRecipeChoice(ingredient);
            for (int i = 0; i < ingredient.amount(); i++) {
                shapeless.addIngredient(choice);
            }
        }

        return shapeless;
    }

    private org.bukkit.inventory.FurnaceRecipe toSmeltingRecipe(RecipeDefinition recipe) {
        NamespacedKey key = createKey(recipe);
        ItemStack result = createResultItem(recipe.getResult());
        RecipeChoice input = toRecipeChoice(recipe.getIngredients().getFirst());

        return new org.bukkit.inventory.FurnaceRecipe(
                key, result, input, recipe.getExperience(), recipe.getCookingTime()
        );
    }

    private org.bukkit.inventory.BlastingRecipe toBlastingRecipe(RecipeDefinition recipe) {
        NamespacedKey key = createKey(recipe);
        ItemStack result = createResultItem(recipe.getResult());
        RecipeChoice input = toRecipeChoice(recipe.getIngredients().getFirst());

        return new org.bukkit.inventory.BlastingRecipe(
                key, result, input, recipe.getExperience(), recipe.getCookingTime()
        );
    }

    private org.bukkit.inventory.SmokingRecipe toSmokingRecipe(RecipeDefinition recipe) {
        NamespacedKey key = createKey(recipe);
        ItemStack result = createResultItem(recipe.getResult());
        RecipeChoice input = toRecipeChoice(recipe.getIngredients().getFirst());

        return new org.bukkit.inventory.SmokingRecipe(
                key, result, input, recipe.getExperience(), recipe.getCookingTime()
        );
    }

    private org.bukkit.inventory.CampfireRecipe toCampfireRecipe(RecipeDefinition recipe) {
        NamespacedKey key = createKey(recipe);
        ItemStack result = createResultItem(recipe.getResult());
        RecipeChoice input = toRecipeChoice(recipe.getIngredients().getFirst());

        return new org.bukkit.inventory.CampfireRecipe(
                key, result, input, recipe.getExperience(), recipe.getCookingTime()
        );
    }

    private org.bukkit.inventory.StonecuttingRecipe toStonecuttingRecipe(RecipeDefinition recipe) {
        NamespacedKey key = createKey(recipe);
        ItemStack result = createResultItem(recipe.getResult());
        RecipeChoice input = toRecipeChoice(recipe.getIngredients().getFirst());

        return new org.bukkit.inventory.StonecuttingRecipe(key, result, input);
    }

    /**
     * Tạo ItemStack kết quả từ ItemResult.
     */
    private ItemStack createResultItem(ItemResult result) {
        String itemId = result.item();

        // Custom item
        if (itemRegistry.exists(itemId)) {
            return itemFactory.create(itemId, result.amount());
        }

        // Vanilla item (minecraft:iron_ingot → IRON_INGOT)
        if (itemId.startsWith("minecraft:")) {
            String materialName = itemId.substring("minecraft:".length()).toUpperCase();
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                return new ItemStack(material, result.amount());
            }
        }

        throw new IllegalArgumentException("Unknown result item: " + itemId);
    }

    /**
     * Chuyển Ingredient → RecipeChoice.
     */
    private RecipeChoice toRecipeChoice(Ingredient ingredient) {
        if (ingredient instanceof Ingredient.ItemIngredient item) {
            // Custom item → ExactChoice
            if (itemRegistry.exists(item.id())) {
                ItemStack example = itemFactory.create(item.id());
                return new RecipeChoice.ExactChoice(example);
            }

            // Vanilla item → MaterialChoice
            if (item.id().startsWith("minecraft:")) {
                String materialName = item.id().substring("minecraft:".length()).toUpperCase();
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    return new RecipeChoice.MaterialChoice(material);
                }
            }

            throw new IllegalArgumentException("Unknown ingredient item: " + item.id());
        }

        if (ingredient instanceof Ingredient.MaterialIngredient mat) {
            return new RecipeChoice.MaterialChoice(mat.material());
        }

        if (ingredient instanceof Ingredient.TagIngredient tag) {
            // Tag ingredient: fallback to material for now
            logger.warning("[BukkitRecipeAdapter] Tag ingredients are not fully supported yet: " + tag.tag());
            return new RecipeChoice.MaterialChoice(Material.STONE);
        }

        throw new IllegalArgumentException("Unknown ingredient type: " + ingredient.getClass().getName());
    }

    private NamespacedKey createKey(RecipeDefinition recipe) {
        // Chuyển "magic:mana_crystal" → NamespacedKey("baseengine", "magic_mana_crystal")
        String keyStr = recipe.getId().replace(':', '_');
        return new NamespacedKey(plugin, keyStr);
    }
}
