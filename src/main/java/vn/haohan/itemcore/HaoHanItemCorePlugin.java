package vn.haohan.itemcore;

import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.block.CustomBlockDefinition;
import vn.haohan.itemcore.api.block.CustomBlockModel;
import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.internal.block.BlockDisplayRenderer;
import vn.haohan.itemcore.internal.block.CustomBlockEventListener;
import vn.haohan.itemcore.internal.block.CustomBlockStorage;
import vn.haohan.itemcore.internal.block.CustomBlockVisibilityManager;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockRegistry;
import vn.haohan.itemcore.internal.block.DefaultCustomBlockService;
import vn.haohan.itemcore.internal.block.VirtualEntityIdManager;
import vn.haohan.itemcore.internal.command.ItemCoreCommand;
import vn.haohan.itemcore.internal.config.ItemConfigLoader;
import vn.haohan.itemcore.internal.config.RecipeConfigLoader;
import vn.haohan.itemcore.internal.event.ItemEventRouter;
import vn.haohan.itemcore.internal.gui.ItemBrowserGUI;
import vn.haohan.itemcore.internal.gui.RecipeViewerGUI;
import vn.haohan.itemcore.internal.item.DefaultItemFactory;
import vn.haohan.itemcore.internal.item.DefaultItemRegistry;
import vn.haohan.itemcore.internal.item.DefaultItemService;
import vn.haohan.itemcore.internal.recipe.BukkitRecipeAdapter;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeRegistry;
import vn.haohan.itemcore.internal.recipe.DefaultRecipeService;
import vn.haohan.itemcore.internal.texture.DefaultIconTextureRegistry;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * HaoHanItemCore Plugin — Plugin nền tảng quản lý Custom Item & Recipe.
 * 
 * <p>
 * Lifecycle:
 * <ol>
 * <li>onLoad() → Initialize registries</li>
 * <li>onEnable() → Load configs, register events, register commands</li>
 * <li>onDisable() → Cleanup</li>
 * </ol>
 */
public final class HaoHanItemCorePlugin extends JavaPlugin {

    private DefaultItemRegistry itemRegistry;
    private DefaultItemFactory itemFactory;
    private DefaultItemService itemService;
    private DefaultRecipeRegistry recipeRegistry;
    private DefaultRecipeService recipeService;
    private BukkitRecipeAdapter recipeAdapter;
    private ItemConfigLoader itemConfigLoader;
    private RecipeConfigLoader recipeConfigLoader;
    private DefaultIconTextureRegistry iconTextureRegistry;

    // CustomBlock subsystem
    private DefaultCustomBlockRegistry customBlockRegistry;
    private CustomBlockStorage customBlockStorage;
    private VirtualEntityIdManager virtualEntityIdManager;
    private BlockDisplayRenderer blockDisplayRenderer;
    private CustomBlockVisibilityManager customBlockVisibilityManager;
    private DefaultCustomBlockService customBlockService;
    private CustomBlockEventListener customBlockEventListener;
    private vn.haohan.itemcore.internal.block.CustomBlockMiningManager customBlockMiningManager;
    private vn.haohan.itemcore.api.block.ToolTierRegistry toolTierRegistry;
    private vn.haohan.itemcore.internal.config.ToolTierConfigLoader toolTierConfigLoader;
    private org.bukkit.scheduler.BukkitTask miningTickerTask;
    private com.github.retrooper.packetevents.event.PacketListenerCommon miningPacketListenerHandle;
    private com.github.retrooper.packetevents.event.PacketListenerCommon chunkPacketListenerHandle;

    @Override
    public void onLoad() {
        Logger log = getLogger();

        // Initialize registries
        itemRegistry = new DefaultItemRegistry(log);
        itemFactory = new DefaultItemFactory(itemRegistry, this);
        itemService = new DefaultItemService(itemRegistry, itemFactory, this);
        recipeRegistry = new DefaultRecipeRegistry(log);
        recipeService = new DefaultRecipeService(recipeRegistry);
        iconTextureRegistry = new DefaultIconTextureRegistry();
        recipeAdapter = new BukkitRecipeAdapter(this, itemRegistry, itemFactory);
        recipeRegistry.setRecipeAdapter(recipeAdapter);

        // Initialize custom block subsystem
        customBlockRegistry = new DefaultCustomBlockRegistry(log);
        customBlockStorage = new CustomBlockStorage(this);
        virtualEntityIdManager = new VirtualEntityIdManager();
        blockDisplayRenderer = new BlockDisplayRenderer();
        customBlockVisibilityManager = new CustomBlockVisibilityManager(
                customBlockStorage, customBlockRegistry, virtualEntityIdManager, blockDisplayRenderer, log);
        customBlockService = new DefaultCustomBlockService(
                customBlockRegistry, customBlockStorage, customBlockVisibilityManager, this);
        toolTierRegistry = new vn.haohan.itemcore.api.block.ToolTierRegistry();

        // Set singleton
        HaoHanItemCore.setInstance(new HaoHanItemCore(
                itemRegistry, itemFactory, itemService,
                recipeRegistry, recipeService, iconTextureRegistry,
                customBlockService, toolTierRegistry));

        log.info("HaoHanItemCore loaded. Registries initialized.");
    }

    @Override
    public void onEnable() {
        Logger log = getLogger();

        // Save default configs
        saveDefaultConfigs();

        // Load items and recipes from YAML
        loadAllConfigs();

        // Register event listeners
        ItemEventRouter eventRouter = new ItemEventRouter(itemRegistry, recipeAdapter, this);
        getServer().getPluginManager().registerEvents(eventRouter, this);

        // Sound packet suppression listener
        vn.haohan.itemcore.internal.block.CustomBlockSoundPacketListener soundPacketListener =
                new vn.haohan.itemcore.internal.block.CustomBlockSoundPacketListener();

        customBlockEventListener = new CustomBlockEventListener(
                customBlockService, customBlockStorage, customBlockVisibilityManager, itemRegistry, this, soundPacketListener);
        getServer().getPluginManager().registerEvents(customBlockEventListener, this);

        // Preload any existing custom blocks from loaded server chunks
        customBlockStorage.loadAll(getServer());

        // Initialize CustomBlockMiningManager & register PacketEvents listener
        vn.haohan.itemcore.internal.block.BlockBreakOverlayRenderer breakOverlayRenderer = new vn.haohan.itemcore.internal.block.BlockBreakOverlayRenderer(log);
        customBlockMiningManager = new vn.haohan.itemcore.internal.block.CustomBlockMiningManager(
                customBlockService, virtualEntityIdManager, breakOverlayRenderer, this);
        customBlockService.setMiningManager(customBlockMiningManager);
        vn.haohan.itemcore.internal.block.CustomBlockPacketListener chunkPacketListener =
                new vn.haohan.itemcore.internal.block.CustomBlockPacketListener(customBlockVisibilityManager, log);
        if (com.github.retrooper.packetevents.PacketEvents.getAPI() != null) {
            miningPacketListenerHandle = com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(
                    customBlockMiningManager,
                    com.github.retrooper.packetevents.event.PacketListenerPriority.NORMAL);
            chunkPacketListenerHandle = com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(
                    chunkPacketListener,
                    com.github.retrooper.packetevents.event.PacketListenerPriority.MONITOR);
            com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(
                    soundPacketListener,
                    com.github.retrooper.packetevents.event.PacketListenerPriority.HIGH);
        }
        miningTickerTask = getServer().getScheduler().runTaskTimer(this, customBlockMiningManager::tick, 1L, 1L);

        // Create GUI handlers
        RecipeViewerGUI recipeViewer = new RecipeViewerGUI(this, itemService, recipeService, itemRegistry);
        ItemBrowserGUI itemBrowser = new ItemBrowserGUI(this, itemRegistry, itemService, recipeViewer);
        recipeViewer.setItemBrowser(itemBrowser);

        getServer().getPluginManager().registerEvents(recipeViewer, this);
        getServer().getPluginManager().registerEvents(itemBrowser, this);

        // Register commands
        ItemCoreCommand commandHandler = new ItemCoreCommand(
                itemRegistry, itemService, recipeRegistry, recipeService,
                recipeViewer, itemBrowser, this::reload);

        registerCommand(
                "im",
                "HaoHanItemCore command",
                List.of("itemcore", "haohanitemcore"),
                commandHandler);

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  HaoHanItemCore v" + getPluginMeta().getVersion() + " enabled!");
        log.info("  Items: " + itemRegistry.size());
        log.info("  Recipes: " + recipeRegistry.size());
        log.info("  CustomBlocks: " + customBlockRegistry.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public void onDisable() {
        if (miningTickerTask != null) {
            miningTickerTask.cancel();
        }
        if (customBlockMiningManager != null) {
            customBlockMiningManager.clearAllSessions();
            if (miningPacketListenerHandle != null && com.github.retrooper.packetevents.PacketEvents.getAPI() != null) {
                try {
                    com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().unregisterListener(miningPacketListenerHandle);
                } catch (Throwable ignored) {}
            }
            if (chunkPacketListenerHandle != null && com.github.retrooper.packetevents.PacketEvents.getAPI() != null) {
                try {
                    com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().unregisterListener(chunkPacketListenerHandle);
                } catch (Throwable ignored) {}
            }
        }
        recipeAdapter.unregisterAll();
        if (customBlockVisibilityManager != null) {
            customBlockVisibilityManager.shutdown();
        }
        if (customBlockStorage != null) {
            customBlockStorage.saveAll(getServer());
            customBlockStorage.clearCache();
        }
        HaoHanItemCore.setInstance(null);
        getLogger().info("HaoHanItemCore disabled.");
    }

    /**
     * Reload tất cả configs.
     */
    private void reload() {
        getLogger().info("Reloading HaoHanItemCore...");

        // Clear registries (recipeRegistry.clear() tự động gọi recipeAdapter.unregisterAll())
        itemRegistry.clear();
        recipeRegistry.clear();
        iconTextureRegistry.clear();
        customBlockRegistry.clear();

        // Reload
        loadAllConfigs();

        getLogger().info("HaoHanItemCore reloaded! Items: " + itemRegistry.size() +
                ", Recipes: " + recipeRegistry.size() +
                ", CustomBlocks: " + customBlockRegistry.size());
    }

    /**
     * Load tất cả items và recipes từ YAML configs.
     */
    private void loadAllConfigs() {
        Logger log = getLogger();

        // Update culling configuration
        if (customBlockVisibilityManager != null) {
            customBlockVisibilityManager.setCullingConfig(
                    vn.haohan.itemcore.internal.config.CustomBlockCullingConfig.fromConfig(getConfig())
            );
        }

        // Load custom tool tiers
        toolTierConfigLoader = new vn.haohan.itemcore.internal.config.ToolTierConfigLoader(log);
        File tiersFile = new File(getDataFolder(), "tiers.yml");
        if (tiersFile.exists()) {
            toolTierConfigLoader.loadFile(tiersFile, toolTierRegistry);
        }

        // Load items first (recipes depend on items)
        itemConfigLoader = new ItemConfigLoader(log);
        File itemsDir = new File(getDataFolder(), "items");
        int itemCount = itemConfigLoader.loadAll(itemsDir, itemRegistry);
        log.info("Loaded " + itemCount + " items from config.");

        // Load recipes
        recipeConfigLoader = new RecipeConfigLoader(log, itemRegistry);
        File recipesDir = new File(getDataFolder(), "recipes");
        List<RecipeDefinition> recipes = recipeConfigLoader.loadAll(recipesDir);

        // Register with HaoHanItemCore (tự động đồng bộ sang Bukkit qua RecipeRegistry)
        for (RecipeDefinition recipe : recipes) {
            try {
                recipeRegistry.register(recipe);
            } catch (Exception e) {
                log.warning("Failed to register recipe: " + recipe.getId() + " — " + e.getMessage());
            }
        }

        // Auto-register custom blocks from item definitions
        for (ItemDefinition itemDef : itemRegistry.all()) {
            CustomBlockDefinition blockDef = vn.haohan.itemcore.internal.config.CustomBlockConfigParser.parseCustomBlock(itemDef);
            if (blockDef != null && !customBlockRegistry.has(blockDef.getId())) {
                customBlockRegistry.register(blockDef);
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
