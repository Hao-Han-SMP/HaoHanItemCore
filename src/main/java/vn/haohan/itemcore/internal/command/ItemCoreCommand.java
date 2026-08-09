package vn.haohan.itemcore.internal.command;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeRegistry;
import vn.haohan.itemcore.api.recipe.RecipeService;
import vn.haohan.itemcore.internal.gui.ItemBrowserGUI;
import vn.haohan.itemcore.internal.gui.RecipeViewerGUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Command handler cho /im (Item Core).
 * 
 * <p>Sub-commands:
 * <ul>
 *   <li>/im items - Liệt kê tất cả items</li>
 *   <li>/im item [id] - Xem chi tiết item</li>
 *   <li>/im give [player] [id] [amount] - Cho item</li>
 *   <li>/im recipes - Liệt kê recipes</li>
 *   <li>/im recipe [id] - Xem recipe (GUI)</li>
 *   <li>/im reload - Reload config</li>
 *   <li>/im search [keyword] - Tìm kiếm</li>
 *   <li>/im browse - Mở Item Browser GUI</li>
 * </ul>
 */
public final class ItemCoreCommand implements BasicCommand {

    private final ItemRegistry itemRegistry;
    private final ItemService itemService;
    private final RecipeRegistry recipeRegistry;
    private final RecipeService recipeService;
    private final RecipeViewerGUI recipeViewer;
    private final ItemBrowserGUI itemBrowser;
    private final Runnable reloadCallback;

    public ItemCoreCommand(ItemRegistry itemRegistry, ItemService itemService,
                               RecipeRegistry recipeRegistry, RecipeService recipeService,
                               RecipeViewerGUI recipeViewer, ItemBrowserGUI itemBrowser,
                               Runnable reloadCallback) {
        this.itemRegistry = itemRegistry;
        this.itemService = itemService;
        this.recipeRegistry = recipeRegistry;
        this.recipeService = recipeService;
        this.recipeViewer = recipeViewer;
        this.itemBrowser = itemBrowser;
        this.reloadCallback = reloadCallback;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "items" -> handleItems(sender);
            case "item" -> handleItem(sender, args);
            case "give" -> handleGive(sender, args);
            case "recipes" -> handleRecipes(sender);
            case "recipe" -> handleRecipe(sender, args);
            case "reload" -> handleReload(sender);
            case "search" -> handleSearch(sender, args);
            case "browse" -> handleBrowse(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown sub-command: " + sub, NamedTextColor.RED));
                sendHelp(sender);
            }
        }
    }

    private boolean handleItems(CommandSender sender) {
        Collection<ItemDefinition> items = itemRegistry.all();

        if (items.isEmpty()) {
            sender.sendMessage(Component.text("No items registered.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("━━━ Custom Items (" + items.size() + ") ━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text(""));

        // Group by namespace
        Map<String, List<ItemDefinition>> byNamespace = items.stream()
                .collect(Collectors.groupingBy(ItemDefinition::getNamespace));

        for (Map.Entry<String, List<ItemDefinition>> entry : byNamespace.entrySet()) {
            sender.sendMessage(Component.text("  [" + entry.getKey() + "]", NamedTextColor.AQUA));
            for (ItemDefinition def : entry.getValue()) {
                Component line = Component.text("    • ", NamedTextColor.GRAY)
                        .append(Component.text(def.getId(), NamedTextColor.WHITE)
                                .hoverEvent(HoverEvent.showText(Component.text(
                                        "Material: " + def.getMaterial() + "\n" +
                                        "Type: " + def.getType() + "\n" +
                                        "Click to view"
                                )))
                                .clickEvent(ClickEvent.runCommand("/im item " + def.getId())))
                        .append(Component.text(" — " + def.getDisplayName()));
                sender.sendMessage(line);
            }
        }
        sender.sendMessage(Component.text(""));
        return true;
    }

    private boolean handleItem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /im item <id>", NamedTextColor.RED));
            return true;
        }

        String id = args[1];
        ItemDefinition def = itemRegistry.get(id);

        if (def == null) {
            sender.sendMessage(Component.text("Item not found: " + id, NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("━━━ Item: " + def.getId() + " ━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  ID: ", NamedTextColor.GRAY).append(Component.text(def.getId(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Name: ", NamedTextColor.GRAY).append(Component.text(def.getDisplayName())));
        sender.sendMessage(Component.text("  Material: ", NamedTextColor.GRAY).append(Component.text(def.getMaterial().name(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Type: ", NamedTextColor.GRAY).append(Component.text(def.getType().name(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Max Stack: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(def.getMaxStackSize()), NamedTextColor.WHITE)));

        if (def.getCustomModelData() != null) {
            sender.sendMessage(Component.text("  Model Data: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(def.getCustomModelData()), NamedTextColor.WHITE)));
        }

        if (!def.getLore().isEmpty()) {
            sender.sendMessage(Component.text("  Lore:", NamedTextColor.GRAY));
            for (String line : def.getLore()) {
                sender.sendMessage(Component.text("    " + line));
            }
        }

        if (!def.getProperties().isEmpty()) {
            sender.sendMessage(Component.text("  Properties:", NamedTextColor.GRAY));
            for (Map.Entry<String, Object> entry : def.getProperties().entrySet()) {
                sender.sendMessage(Component.text("    " + entry.getKey() + ": " + entry.getValue(), NamedTextColor.WHITE));
            }
        }

        // Show related recipes
        List<RecipeDefinition> recipes = recipeService.findByResult(id);
        if (!recipes.isEmpty()) {
            sender.sendMessage(Component.text("  Recipes (" + recipes.size() + "):", NamedTextColor.GREEN));
            for (RecipeDefinition recipe : recipes) {
                Component recipeLine = Component.text("    • ", NamedTextColor.GRAY)
                        .append(Component.text(recipe.getId(), NamedTextColor.YELLOW)
                                .clickEvent(ClickEvent.runCommand("/im recipe " + recipe.getId()))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to view recipe"))));
                sender.sendMessage(recipeLine);
            }
        }

        sender.sendMessage(Component.text(""));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("baseengine.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /im give <player> <id> [amount]", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        String id = args[2];
        int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;

        if (!itemRegistry.exists(id)) {
            sender.sendMessage(Component.text("Item not found: " + id, NamedTextColor.RED));
            return true;
        }

        ItemStack item = itemService.create(id, amount);
        target.getInventory().addItem(item);

        sender.sendMessage(Component.text("Gave " + amount + "x ", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.GOLD))
                .append(Component.text(" to " + target.getName(), NamedTextColor.GREEN)));

        return true;
    }

    private boolean handleRecipes(CommandSender sender) {
        Collection<RecipeDefinition> recipes = recipeRegistry.all();

        if (recipes.isEmpty()) {
            sender.sendMessage(Component.text("No recipes registered.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("━━━ Recipes (" + recipes.size() + ") ━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text(""));

        for (RecipeDefinition recipe : recipes) {
            Component line = Component.text("  • ", NamedTextColor.GRAY)
                    .append(Component.text(recipe.getId(), NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand("/im recipe " + recipe.getId()))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to view"))))
                    .append(Component.text(" [" + recipe.getType() + "]", NamedTextColor.DARK_GRAY))
                    .append(Component.text(" → " + recipe.getResult().item(), NamedTextColor.GREEN));
            sender.sendMessage(line);
        }
        sender.sendMessage(Component.text(""));
        return true;
    }

    private boolean handleRecipe(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /im recipe <id>", NamedTextColor.RED));
            return true;
        }

        String id = args[1];

        // Nếu là player → mở GUI
        if (sender instanceof Player player) {
            RecipeDefinition recipe = recipeRegistry.get(id);
            if (recipe != null) {
                recipeViewer.open(player, recipe);
            } else {
                // Thử tìm theo result item
                recipeViewer.open(player, id);
            }
        } else {
            RecipeDefinition recipe = recipeRegistry.get(id);
            if (recipe == null) {
                sender.sendMessage(Component.text("Recipe not found: " + id, NamedTextColor.RED));
            } else {
                sender.sendMessage(Component.text("Recipe: " + recipe, NamedTextColor.GREEN));
            }
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("baseengine.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Reloading HaoHanItemCore...", NamedTextColor.YELLOW));
        reloadCallback.run();
        sender.sendMessage(Component.text("HaoHanItemCore reloaded! Items: " + itemRegistry.size() +
                ", Recipes: " + recipeRegistry.size(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /im search <keyword>", NamedTextColor.RED));
            return true;
        }

        String keyword = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        List<ItemDefinition> items = itemRegistry.search(keyword);
        List<RecipeDefinition> recipes = recipeService.search(keyword);

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("━━━ Search: \"" + keyword + "\" ━━━", NamedTextColor.GOLD, TextDecoration.BOLD));

        if (!items.isEmpty()) {
            sender.sendMessage(Component.text("  Items (" + items.size() + "):", NamedTextColor.AQUA));
            for (ItemDefinition def : items) {
                sender.sendMessage(Component.text("    • ", NamedTextColor.GRAY)
                        .append(Component.text(def.getId(), NamedTextColor.WHITE)
                                .clickEvent(ClickEvent.runCommand("/im item " + def.getId()))));
            }
        }

        if (!recipes.isEmpty()) {
            sender.sendMessage(Component.text("  Recipes (" + recipes.size() + "):", NamedTextColor.GREEN));
            for (RecipeDefinition recipe : recipes) {
                sender.sendMessage(Component.text("    • ", NamedTextColor.GRAY)
                        .append(Component.text(recipe.getId(), NamedTextColor.WHITE)
                                .clickEvent(ClickEvent.runCommand("/im recipe " + recipe.getId()))));
            }
        }

        if (items.isEmpty() && recipes.isEmpty()) {
            sender.sendMessage(Component.text("  No results found.", NamedTextColor.GRAY));
        }

        sender.sendMessage(Component.text(""));
        return true;
    }

    private boolean handleBrowse(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        itemBrowser.open(player);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("━━━ HaoHanItemCore Commands ━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /im items", NamedTextColor.YELLOW).append(Component.text(" — List all items", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /im item <id>", NamedTextColor.YELLOW).append(Component.text(" — View item details", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /im give <player> <id> [amount]", NamedTextColor.YELLOW).append(Component.text(" — Give item", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /im recipes", NamedTextColor.YELLOW).append(Component.text(" — List all recipes", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /im recipe <id>", NamedTextColor.YELLOW).append(Component.text(" — View recipe (GUI)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /im search <keyword>", NamedTextColor.YELLOW).append(Component.text(" — Search items/recipes", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /im browse", NamedTextColor.YELLOW).append(Component.text(" — Open Item Browser GUI", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /im reload", NamedTextColor.YELLOW).append(Component.text(" — Reload configs", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(""));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(List.of("items", "item", "give", "recipes", "recipe", "search", "browse", "reload"), args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "item", "recipe" -> filterStartsWith(getAllIds(), args[1]);
                case "give" -> filterStartsWith(getOnlinePlayerNames(), args[1]);
                case "search" -> List.of();
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filterStartsWith(getAllItemIds(), args[2]);
        }

        return List.of();
    }

    private List<String> getAllIds() {
        List<String> ids = new ArrayList<>();
        itemRegistry.all().forEach(def -> ids.add(def.getId()));
        recipeRegistry.all().forEach(recipe -> ids.add(recipe.getId()));
        return ids;
    }

    private List<String> getAllItemIds() {
        return itemRegistry.all().stream()
                .map(ItemDefinition::getId)
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
