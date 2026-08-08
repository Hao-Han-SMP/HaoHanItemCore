package vn.haohan.itemmanager;

import vn.haohan.itemmanager.api.HaoHanItemManager;
import vn.haohan.itemmanager.api.item.ItemRegistry;
import vn.haohan.itemmanager.api.recipe.RecipeDefinition;
import vn.haohan.itemmanager.api.recipe.RecipeRegistry;
import vn.haohan.itemmanager.api.recipe.RecipeService;
import vn.haohan.itemmanager.internal.command.ItemManagerCommand;
import vn.haohan.itemmanager.internal.config.ItemConfigLoader;
import vn.haohan.itemmanager.internal.config.RecipeConfigLoader;
import vn.haohan.itemmanager.internal.event.ItemEventRouter;
import vn.haohan.itemmanager.internal.gui.ItemBrowserGUI;
import vn.haohan.itemmanager.internal.gui.RecipeViewerGUI;
import vn.haohan.itemmanager.internal.item.DefaultItemFactory;
import vn.haohan.itemmanager.internal.item.DefaultItemRegistry;
import vn.haohan.itemmanager.internal.item.DefaultItemService;
import vn.haohan.itemmanager.internal.recipe.BukkitRecipeAdapter;
import vn.haohan.itemmanager.internal.recipe.DefaultRecipeRegistry;
import vn.haohan.itemmanager.internal.recipe.DefaultRecipeService;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * HaoHanItemManager Plugin — Plugin nền tảng quản lý Custom Item & Recipe.
 * 
 * <p>Lifecycle:
 * <ol>
 *   <li>onLoad() → Initialize registries</li>
 *   <li>onEnable() → Load configs, register events, register commands</li>
 *   <li>onDisable() → Cleanup</li>
 * </ol>
 */
public final class HaoHanItemManagerPlugin extends JavaPlugin {

    private DefaultItemRegistry itemRegistry;
    private DefaultItemFactory itemFactory;
    private DefaultItemService itemService;
    private DefaultRecipeRegistry recipeRegistry;
    private DefaultRecipeService recipeService;
    private BukkitRecipeAdapter recipeAdapter;
    private ItemConfigLoader itemConfigLoader;
    private RecipeConfigLoader recipeConfigLoader;

    @Override
    public void onLoad() {
        Logger log = getLogger();

        // Initialize registries
        itemRegistry = new DefaultItemRegistry(log);
        itemFactory = new DefaultItemFactory(itemRegistry, this);
        itemService = new DefaultItemService(itemRegistry, itemFactory, this);
        recipeRegistry = new DefaultRecipeRegistry(log);
        recipeService = new DefaultRecipeService(recipeRegistry);
        recipeAdapter = new BukkitRecipeAdapter(this, itemRegistry, itemFactory);

        // Set singleton
        HaoHanItemManager.setInstance(new HaoHanItemManager(
                itemRegistry, itemFactory, itemService,
                recipeRegistry, recipeService
        ));

        log.info("HaoHanItemManager loaded. Registries initialized.");
    }

    @Override
    public void onEnable() {
        Logger log = getLogger();

        // Save default configs
        saveDefaultConfigs();

        // Load items and recipes from YAML
        loadAllConfigs();

        // Register event listeners
        ItemEventRouter eventRouter = new ItemEventRouter(itemRegistry, this);
        getServer().getPluginManager().registerEvents(eventRouter, this);

        // Create GUI handlers
        RecipeViewerGUI recipeViewer = new RecipeViewerGUI(this, itemService, recipeService, itemRegistry);
        ItemBrowserGUI itemBrowser = new ItemBrowserGUI(itemRegistry, itemService, recipeViewer);

        getServer().getPluginManager().registerEvents(recipeViewer, this);
        getServer().getPluginManager().registerEvents(itemBrowser, this);

        // Register commands
        ItemManagerCommand commandHandler = new ItemManagerCommand(
                itemRegistry, itemService, recipeRegistry, recipeService,
                recipeViewer, itemBrowser, this::reload
        );

        registerCommand(
                "im",
                "HaoHanItemManager command",
                List.of("itemmanager", "haohanitemmanage"),
                commandHandler
        );

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  HaoHanItemManager v" + getPluginMeta().getVersion() + " enabled!");
        log.info("  Items: " + itemRegistry.size());
        log.info("  Recipes: " + recipeRegistry.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public void onDisable() {
        recipeAdapter.unregisterAll();
        HaoHanItemManager.setInstance(null);
        getLogger().info("HaoHanItemManager disabled.");
    }

    /**
     * Reload tất cả configs.
     */
    private void reload() {
        getLogger().info("Reloading HaoHanItemManager...");

        // Unregister Bukkit recipes
        recipeAdapter.unregisterAll();

        // Clear registries
        itemRegistry.clear();
        recipeRegistry.clear();

        // Reload
        loadAllConfigs();

        getLogger().info("HaoHanItemManager reloaded! Items: " + itemRegistry.size() +
                ", Recipes: " + recipeRegistry.size());
    }

    /**
     * Load tất cả items và recipes từ YAML configs.
     */
    private void loadAllConfigs() {
        Logger log = getLogger();

        // Load items first (recipes depend on items)
        itemConfigLoader = new ItemConfigLoader(log);
        File itemsDir = new File(getDataFolder(), "items");
        int itemCount = itemConfigLoader.loadAll(itemsDir, itemRegistry);
        log.info("Loaded " + itemCount + " items from config.");

        // Load recipes
        recipeConfigLoader = new RecipeConfigLoader(log, itemRegistry);
        File recipesDir = new File(getDataFolder(), "recipes");
        List<RecipeDefinition> recipes = recipeConfigLoader.loadAll(recipesDir);

        // Register with HaoHanItemManager and Bukkit
        for (RecipeDefinition recipe : recipes) {
            try {
                recipeRegistry.register(recipe);
                recipeAdapter.register(recipe);
            } catch (Exception e) {
                log.warning("Failed to register recipe: " + recipe.getId() + " — " + e.getMessage());
            }
        }
    }

    /**
     * Save default config files nếu chưa tồn tại.
     */
    private void saveDefaultConfigs() {
        saveResource("config.yml", false);

        // Tạo thư mục items/ và recipes/ nếu chưa tồn tại
        File itemsDir = new File(getDataFolder(), "items");
        File recipesDir = new File(getDataFolder(), "recipes");

        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
            saveResource("items/example.yml", false);
        }

        if (!recipesDir.exists()) {
            recipesDir.mkdirs();
            saveResource("recipes/example.yml", false);
        }
    }
}
