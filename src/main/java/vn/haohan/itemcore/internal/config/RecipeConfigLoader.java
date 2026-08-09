package vn.haohan.itemcore.internal.config;

import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.recipe.*;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Load recipe definitions từ YAML files trong thư mục recipes/.
 * Hỗ trợ cả file đơn lẻ và thư mục con.
 * 
 * <p>Format cho shaped recipe:
 * <pre>
 * id: magic:mana_crystal
 * type: SHAPED
 * pattern:
 *   - " R "
 *   - "RCR"
 *   - " R "
 * ingredients:
 *   R:
 *     item: "magic:mana_shard"
 *     amount: 1
 *   C:
 *     item: "minecraft:blaze_rod"
 *     amount: 1
 * result:
 *   item: "magic:mana_crystal"
 *   amount: 1
 * </pre>
 * 
 * <p>Format cho smelting recipe:
 * <pre>
 * id: machine:refined_steel
 * type: SMELTING
 * input:
 *   item: "machine:steel_core"
 * result:
 *   item: "machine:refined_steel"
 *   amount: 1
 * experience: 1.0
 * cooking-time: 200
 * </pre>
 */
public final class RecipeConfigLoader {

    private final Logger logger;
    private final ItemRegistry itemRegistry;

    public RecipeConfigLoader(Logger logger, ItemRegistry itemRegistry) {
        this.logger = logger;
        this.itemRegistry = itemRegistry;
    }

    /**
     * Load tất cả recipe files từ thư mục recipes/ (bao gồm thư mục con).
     * @return Danh sách RecipeDefinition đã load.
     */
    public List<RecipeDefinition> loadAll(File recipesDir) {
        List<RecipeDefinition> recipes = new ArrayList<>();

        if (!recipesDir.exists() || !recipesDir.isDirectory()) {
            logger.info("[RecipeConfigLoader] No recipes directory found. Skipping.");
            return recipes;
        }

        loadDirectory(recipesDir, recipes);

        logger.info("[RecipeConfigLoader] Loaded " + recipes.size() + " recipes total.");
        return recipes;
    }

    private void loadDirectory(File dir, List<RecipeDefinition> recipes) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadDirectory(file, recipes);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                loadFile(file, recipes);
            }
        }
    }

    /**
     * Load recipe từ một file YAML.
     */
    public void loadFile(File file, List<RecipeDefinition> recipes) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        try {
            // Kiểm tra nếu file chứa trực tiếp một recipe (có field "id" ở top level)
            if (config.contains("id")) {
                loadSingleRecipe(config, recipes);
            } else {
                loadMultipleRecipes(config, file.getName(), recipes);
            }
        } catch (Exception e) {
            logger.warning("[RecipeConfigLoader] Failed to load recipe from " +
                    file.getName() + ": " + e.getMessage());
        }
    }

    private void loadSingleRecipe(ConfigurationSection config, List<RecipeDefinition> recipes) {
        RecipeDefinition recipe = parseRecipe(config);
        if (recipe != null) {
            validateRecipe(recipe);
            recipes.add(recipe);
        }
    }

    private void loadMultipleRecipes(YamlConfiguration config, String fileName, List<RecipeDefinition> recipes) {
        // File chứa nhiều recipes dưới dạng sections
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            try {
                RecipeDefinition recipe = parseRecipeSection(section);
                if (recipe != null) {
                    validateRecipe(recipe);
                    recipes.add(recipe);
                }
            } catch (Exception e) {
                logger.warning("[RecipeConfigLoader] Failed to load recipe '" + key +
                        "' from " + fileName + ": " + e.getMessage());
            }
        }
    }

    private RecipeDefinition parseRecipe(ConfigurationSection config) {
        String id = config.getString("id");
        String typeStr = config.getString("type", "SHAPELESS");
        RecipeType type;

        try {
            type = RecipeType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid recipe type: " + typeStr);
        }

        // Result
        ItemResult result = parseResult(config.getConfigurationSection("result"));

        if (type == RecipeType.SHAPED) {
            return parseShapedRecipe(id, config, result);
        } else {
            return parseNonShapedRecipe(id, type, config, result);
        }
    }

    private RecipeDefinition parseRecipeSection(ConfigurationSection section) {
        return parseRecipe(section);
    }

    private ShapedRecipeDefinition parseShapedRecipe(String id, ConfigurationSection config, ItemResult result) {
        // Pattern
        List<String> pattern = config.getStringList("pattern");
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("Shaped recipe must have a pattern");
        }

        // Ingredients map
        ConfigurationSection ingredientsSection = config.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            throw new IllegalArgumentException("Shaped recipe must have ingredients");
        }

        Map<Character, Ingredient> ingredientMap = new HashMap<>();
        for (String charKey : ingredientsSection.getKeys(false)) {
            if (charKey.length() != 1) {
                throw new IllegalArgumentException("Ingredient key must be a single character: " + charKey);
            }
            char c = charKey.charAt(0);
            ConfigurationSection ingredientSection = ingredientsSection.getConfigurationSection(charKey);
            if (ingredientSection != null) {
                ingredientMap.put(c, parseIngredient(ingredientSection));
            }
        }

        return new ShapedRecipeDefinition(id, pattern, ingredientMap, result);
    }

    private RecipeDefinition parseNonShapedRecipe(String id, RecipeType type, ConfigurationSection config,
                                                   ItemResult result) {
        if (type == RecipeType.SMELTING || type == RecipeType.BLASTING ||
            type == RecipeType.SMOKING || type == RecipeType.CAMPFIRE ||
            type == RecipeType.STONECUTTING) {
            return parseCookingRecipe(id, type, config, result);
        }

        if (type == RecipeType.SMITHING) {
            return parseSmithingRecipe(id, config, result);
        }

        return parseShapelessOrMachineRecipe(id, type, config, result);
    }

    private RecipeDefinition parseCookingRecipe(String id, RecipeType type, ConfigurationSection config, ItemResult result) {
        List<Ingredient> ingredients = new ArrayList<>();
        ConfigurationSection inputSection = config.getConfigurationSection("input");
        if (inputSection == null) {
            // Fallback: check ingredients list
            inputSection = config.getConfigurationSection("ingredients");
        }
        if (inputSection != null) {
            // Kiểm tra nếu input trực tiếp có "item" field
            if (inputSection.contains("item")) {
                ingredients.add(parseIngredient(inputSection));
            } else {
                // Multiple ingredients sections
                for (String key : inputSection.getKeys(false)) {
                    ConfigurationSection sub = inputSection.getConfigurationSection(key);
                    if (sub != null) {
                        ingredients.add(parseIngredient(sub));
                    }
                }
            }
        }

        float experience = (float) config.getDouble("experience", 0);
        int cookingTime = config.getInt("cooking-time", 200);

        return new RecipeDefinition(id, type, ingredients, result, experience, cookingTime);
    }

    private RecipeDefinition parseSmithingRecipe(String id, ConfigurationSection config, ItemResult result) {
        List<Ingredient> ingredients = new ArrayList<>();
        ConfigurationSection templateSec = config.getConfigurationSection("template");
        ConfigurationSection baseSec = config.getConfigurationSection("base");
        ConfigurationSection additionSec = config.getConfigurationSection("addition");
        if (templateSec != null && baseSec != null && additionSec != null) {
            ingredients.add(parseIngredient(templateSec));
            ingredients.add(parseIngredient(baseSec));
            ingredients.add(parseIngredient(additionSec));
        } else {
            ConfigurationSection ingredientsSection = config.getConfigurationSection("ingredients");
            if (ingredientsSection != null) {
                for (String key : ingredientsSection.getKeys(false)) {
                    ConfigurationSection sub = ingredientsSection.getConfigurationSection(key);
                    if (sub != null) {
                        ingredients.add(parseIngredient(sub));
                    }
                }
            }
        }
        return new RecipeDefinition(id, RecipeType.SMITHING, ingredients, result);
    }

    private RecipeDefinition parseShapelessOrMachineRecipe(String id, RecipeType type, ConfigurationSection config, ItemResult result) {
        List<Ingredient> ingredients = new ArrayList<>();
        ConfigurationSection ingredientsSection = config.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (String key : ingredientsSection.getKeys(false)) {
                ConfigurationSection sub = ingredientsSection.getConfigurationSection(key);
                if (sub != null) {
                    ingredients.add(parseIngredient(sub));
                }
            }
        }
        return new RecipeDefinition(id, type, ingredients, result);
    }

    private Ingredient parseIngredient(ConfigurationSection section) {
        String type = section.getString("type", "item");
        int amount = section.getInt("amount", 1);

        return switch (type.toLowerCase()) {
            case "item" -> {
                String itemId = section.getString("item", section.getString("id"));
                if (itemId == null) {
                    throw new IllegalArgumentException("Item ingredient must have 'item' field");
                }
                yield new Ingredient.ItemIngredient(itemId, amount);
            }
            case "material" -> {
                String materialStr = section.getString("material");
                if (materialStr == null) {
                    throw new IllegalArgumentException("Material ingredient must have 'material' field");
                }
                org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialStr);
                if (material == null) {
                    throw new IllegalArgumentException("Invalid material: " + materialStr);
                }
                yield new Ingredient.MaterialIngredient(material, amount);
            }
            case "tag" -> {
                String tag = section.getString("tag");
                if (tag == null) {
                    throw new IllegalArgumentException("Tag ingredient must have 'tag' field");
                }
                yield new Ingredient.TagIngredient(tag, amount);
            }
            default -> throw new IllegalArgumentException("Unknown ingredient type: " + type);
        };
    }

    private ItemResult parseResult(ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Recipe must have a result section");
        }
        String item = section.getString("item");
        if (item == null) {
            throw new IllegalArgumentException("Result must have 'item' field");
        }
        int amount = section.getInt("amount", 1);
        return new ItemResult(item, amount);
    }

    /**
     * Validate recipe: kiểm tra item references có tồn tại không.
     */
    private void validateRecipe(RecipeDefinition recipe) {
        // Validate result
        String resultItem = recipe.getResult().item();
        if (!resultItem.startsWith("minecraft:") && !itemRegistry.exists(resultItem)) {
            logger.warning("[RecipeConfigLoader] Recipe '" + recipe.getId() +
                    "' references unknown result item: " + resultItem);
        }

        // Validate ingredients
        List<Ingredient> allIngredients = new ArrayList<>(recipe.getIngredients());
        if (recipe instanceof ShapedRecipeDefinition shaped) {
            allIngredients.addAll(shaped.getIngredientMap().values());
        }

        for (Ingredient ingredient : allIngredients) {
            if (ingredient instanceof Ingredient.ItemIngredient item) {
                if (!item.id().startsWith("minecraft:") && !itemRegistry.exists(item.id())) {
                    logger.warning("[RecipeConfigLoader] Recipe '" + recipe.getId() +
                            "' references unknown ingredient: " + item.id());
                }
            }
        }
    }
}
