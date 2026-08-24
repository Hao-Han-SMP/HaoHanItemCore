package vn.haohan.itemcore.internal.recipe;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemFactory;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeService;
import vn.haohan.itemcore.api.recipe.RecipeType;
import vn.haohan.itemcore.api.recipe.ShapedRecipeDefinition;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Bộ phân giải và xử lý công thức chế tạo thông minh (Auto-Craft Smart Resolver).
 * 
 * <p>Giải quyết triệt để vấn đề:
 * Khi người chơi bấm vào công thức trong Recipe Book của Minecraft, Paper sẽ kích hoạt
 * sự kiện PlayerRecipeBookClickEvent. Resolver sẽ can thiệp huỷ bỏ cơ chế bốc đồ mặc
 * định của Minecraft (tránh bốc nhầm sắt vanilla vào bàn chế tạo) và tự động bốc đúng
 * Custom Items từ kho đồ của người chơi đặt vào bàn chế tạo.
 */
public final class CraftingRecipeResolver {

    private final ItemRegistry itemRegistry;
    private final ItemService itemService;
    private final RecipeService recipeService;
    private final ItemFactory itemFactory;

    public CraftingRecipeResolver(ItemRegistry itemRegistry, ItemService itemService,
                                  RecipeService recipeService, ItemFactory itemFactory) {
        this.itemRegistry = itemRegistry;
        this.itemService = itemService;
        this.recipeService = recipeService;
        this.itemFactory = itemFactory;
    }

    /**
     * Xử lý sự kiện khi người chơi bấm vào Recipe Book (Paper Event).
     * Trả về true nếu sự kiện đã được nhận diện và xử lý.
     */
    public boolean handleRecipeBookClick(com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent event) {
        NamespacedKey recipeKey = event.getRecipe();
        if (recipeKey == null) {
            return false;
        }

        RecipeDefinition customRecipe = findRecipeByKey(recipeKey);
        if (customRecipe == null) {
            return false;
        }

        // Huỷ bỏ sự kiện mặc định để vanilla server không bốc nhầm sắt thường
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!(player.getOpenInventory().getTopInventory() instanceof CraftingInventory craftingInv)) {
            return true;
        }

        if (customRecipe instanceof ShapedRecipeDefinition shaped) {
            fillShapedRecipe(craftingInv, player, shaped);
        } else if (customRecipe.getType() == RecipeType.SHAPELESS) {
            fillShapelessRecipe(craftingInv, player, customRecipe);
        }

        return true;
    }

    /**
     * Xử lý sự kiện PrepareItemCraftEvent (khi matrix thay đổi hoặc xếp tay).
     */
    public boolean resolve(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        if (matrix == null || matrix.length == 0 || isMatrixEmpty(matrix)) {
            return false;
        }

        Player player = event.getView().getPlayer() instanceof Player p ? p : null;

        for (RecipeDefinition recipe : recipeService.all()) {
            if (recipe.getType() == RecipeType.SHAPED && recipe instanceof ShapedRecipeDefinition shaped) {
                if (tryResolveShaped(inventory, matrix, player, shaped)) {
                    return true;
                }
            } else if (recipe.getType() == RecipeType.SHAPELESS) {
                if (tryResolveShapeless(inventory, matrix, player, recipe)) {
                    return true;
                }
            }
        }

        return false;
    }

    public RecipeDefinition findRecipeByKey(NamespacedKey key) {
        String fullKeyStr = key.toString();
        String keyPath = key.getKey();

        for (RecipeDefinition r : recipeService.all()) {
            if (r.getId().equalsIgnoreCase(fullKeyStr)) {
                return r;
            }
            String rIdNormalized = r.getId().replace(':', '_');
            if (rIdNormalized.equalsIgnoreCase(keyPath)) {
                return r;
            }
            if (keyPath.endsWith(rIdNormalized) || rIdNormalized.endsWith(keyPath)) {
                return r;
            }
            String rKeyOnly = r.getId().substring(r.getId().indexOf(':') + 1);
            if (keyPath.equalsIgnoreCase(rKeyOnly) || keyPath.endsWith("_" + rKeyOnly)) {
                return r;
            }
        }
        return null;
    }

    private void fillShapedRecipe(CraftingInventory craftingInv, Player player, ShapedRecipeDefinition shaped) {
        PlayerInventory playerInv = player.getInventory();
        List<String> pattern = shaped.getPattern();
        int H = pattern.size();
        int W = 0;
        for (String row : pattern) {
            W = Math.max(W, row.length());
        }

        int gridRows = craftingInv.getMatrix().length == 4 ? 2 : 3;
        int gridCols = craftingInv.getMatrix().length == 4 ? 2 : 3;

        if (H > gridRows || W > gridCols) {
            return;
        }

        Map<String, Integer> neededCustomItems = new HashMap<>();
        Map<Material, Integer> neededMaterials = new HashMap<>();

        for (int r = 0; r < H; r++) {
            String row = pattern.get(r);
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (ch != ' ') {
                    Ingredient ing = shaped.getIngredientMap().get(ch);
                    if (ing instanceof Ingredient.ItemIngredient itemIng) {
                        if (itemIng.isVanilla()) {
                            Material mat = parseVanillaMaterial(itemIng.id());
                            if (mat != null) {
                                neededMaterials.put(mat, neededMaterials.getOrDefault(mat, 0) + 1);
                            }
                        } else {
                            neededCustomItems.put(itemIng.id(), neededCustomItems.getOrDefault(itemIng.id(), 0) + 1);
                        }
                    } else if (ing instanceof Ingredient.MaterialIngredient matIng) {
                        neededMaterials.put(matIng.material(), neededMaterials.getOrDefault(matIng.material(), 0) + 1);
                    }
                }
            }
        }

        // Kiểm tra nguyên liệu trong túi đồ
        for (var entry : neededCustomItems.entrySet()) {
            if (countCustomItems(playerInv, entry.getKey()) < entry.getValue()) {
                player.sendActionBar(Component.text("Thiếu nguyên liệu: " + getDisplayName(entry.getKey()), NamedTextColor.RED));
                return;
            }
        }
        for (var entry : neededMaterials.entrySet()) {
            if (countMaterials(playerInv, entry.getKey()) < entry.getValue()) {
                player.sendActionBar(Component.text("Thiếu nguyên liệu: " + entry.getKey().name(), NamedTextColor.RED));
                return;
            }
        }

        // Trả lại các item hiện có trên matrix về túi đồ người chơi
        returnMatrixToPlayer(craftingInv, player);

        // Đặt đúng các nguyên liệu custom và vanilla vào matrix
        ItemStack[] newMatrix = new ItemStack[gridRows * gridCols];
        for (int r = 0; r < H; r++) {
            String row = pattern.get(r);
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                int slotIndex = r * gridCols + c;
                if (ch != ' ') {
                    Ingredient ing = shaped.getIngredientMap().get(ch);
                    if (ing instanceof Ingredient.ItemIngredient itemIng) {
                        if (itemIng.isVanilla()) {
                            Material mat = parseVanillaMaterial(itemIng.id());
                            newMatrix[slotIndex] = takeMaterial(playerInv, mat, 1);
                        } else {
                            newMatrix[slotIndex] = takeCustomItem(playerInv, itemIng.id(), 1);
                        }
                    } else if (ing instanceof Ingredient.MaterialIngredient matIng) {
                        newMatrix[slotIndex] = takeMaterial(playerInv, matIng.material(), 1);
                    }
                }
            }
        }

        craftingInv.setMatrix(newMatrix);
        ItemStack result = itemFactory.create(shaped.getResult().item(), shaped.getResult().amount());
        itemService.validateAndUpdate(result);
        craftingInv.setResult(result);
        player.updateInventory();
    }

    private void fillShapelessRecipe(CraftingInventory craftingInv, Player player, RecipeDefinition recipe) {
        PlayerInventory playerInv = player.getInventory();
        List<Ingredient> ingredients = recipe.getIngredients();
        int totalNeeded = 0;
        for (Ingredient ing : ingredients) {
            totalNeeded += ing.amount();
        }
        int gridSize = craftingInv.getMatrix().length;

        if (totalNeeded > gridSize) {
            return;
        }

        Map<String, Integer> neededCustomItems = new HashMap<>();
        Map<Material, Integer> neededMaterials = new HashMap<>();

        for (Ingredient ing : ingredients) {
            if (ing instanceof Ingredient.ItemIngredient itemIng) {
                if (itemIng.isVanilla()) {
                    Material mat = parseVanillaMaterial(itemIng.id());
                    if (mat != null) {
                        neededMaterials.put(mat, neededMaterials.getOrDefault(mat, 0) + ing.amount());
                    }
                } else {
                    neededCustomItems.put(itemIng.id(), neededCustomItems.getOrDefault(itemIng.id(), 0) + ing.amount());
                }
            } else if (ing instanceof Ingredient.MaterialIngredient matIng) {
                neededMaterials.put(matIng.material(), neededMaterials.getOrDefault(matIng.material(), 0) + ing.amount());
            }
        }

        for (var entry : neededCustomItems.entrySet()) {
            if (countCustomItems(playerInv, entry.getKey()) < entry.getValue()) {
                player.sendActionBar(Component.text("Thiếu nguyên liệu: " + getDisplayName(entry.getKey()), NamedTextColor.RED));
                return;
            }
        }
        for (var entry : neededMaterials.entrySet()) {
            if (countMaterials(playerInv, entry.getKey()) < entry.getValue()) {
                player.sendActionBar(Component.text("Thiếu nguyên liệu: " + entry.getKey().name(), NamedTextColor.RED));
                return;
            }
        }

        returnMatrixToPlayer(craftingInv, player);

        ItemStack[] newMatrix = new ItemStack[gridSize];
        int slotIndex = 0;
        for (Ingredient ing : ingredients) {
            for (int i = 0; i < ing.amount(); i++) {
                if (slotIndex >= gridSize) break;
                if (ing instanceof Ingredient.ItemIngredient itemIng) {
                    if (itemIng.isVanilla()) {
                        Material mat = parseVanillaMaterial(itemIng.id());
                        newMatrix[slotIndex++] = takeMaterial(playerInv, mat, 1);
                    } else {
                        newMatrix[slotIndex++] = takeCustomItem(playerInv, itemIng.id(), 1);
                    }
                } else if (ing instanceof Ingredient.MaterialIngredient matIng) {
                    newMatrix[slotIndex++] = takeMaterial(playerInv, matIng.material(), 1);
                }
            }
        }

        craftingInv.setMatrix(newMatrix);
        ItemStack result = itemFactory.create(recipe.getResult().item(), recipe.getResult().amount());
        itemService.validateAndUpdate(result);
        craftingInv.setResult(result);
        player.updateInventory();
    }

    private void returnMatrixToPlayer(CraftingInventory craftingInv, Player player) {
        PlayerInventory playerInv = player.getInventory();
        ItemStack[] currentMatrix = craftingInv.getMatrix();
        boolean modified = false;
        for (int i = 0; i < currentMatrix.length; i++) {
            ItemStack item = currentMatrix[i];
            if (item != null && !item.getType().isAir()) {
                HashMap<Integer, ItemStack> leftover = playerInv.addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                currentMatrix[i] = null;
                modified = true;
            }
        }
        if (modified) {
            craftingInv.setMatrix(currentMatrix);
        }
    }

    private boolean isMatrixEmpty(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    private boolean tryResolveShaped(CraftingInventory inventory, ItemStack[] matrix,
                                     Player player, ShapedRecipeDefinition shaped) {
        int gridRows = matrix.length == 4 ? 2 : 3;
        int gridCols = matrix.length == 4 ? 2 : 3;

        List<String> pattern = shaped.getPattern();
        int patternHeight = pattern.size();
        int patternWidth = 0;
        for (String row : pattern) {
            patternWidth = Math.max(patternWidth, row.length());
        }

        if (patternHeight > gridRows || patternWidth > gridCols) {
            return false;
        }

        for (int dr = 0; dr <= gridRows - patternHeight; dr++) {
            for (int dc = 0; dc <= gridCols - patternWidth; dc++) {
                boolean matched = true;

                for (int r = 0; r < gridRows; r++) {
                    for (int c = 0; c < gridCols; c++) {
                        int matrixIndex = r * gridCols + c;
                        ItemStack slotItem = matrix[matrixIndex];

                        boolean inPattern = (r >= dr && r < dr + patternHeight
                                && c >= dc && c < dc + pattern.get(r - dr).length());

                        if (inPattern) {
                            char ch = pattern.get(r - dr).charAt(c - dc);
                            if (ch == ' ') {
                                if (slotItem != null && !slotItem.getType().isAir()) {
                                    matched = false;
                                    break;
                                }
                            } else {
                                Ingredient ing = shaped.getIngredientMap().get(ch);
                                if (ing == null) {
                                    matched = false;
                                    break;
                                }

                                if (ing instanceof Ingredient.ItemIngredient itemIng) {
                                    if (itemIng.isVanilla()) {
                                        if (!matchVanillaItem(slotItem, itemIng.id())) {
                                            matched = false;
                                            break;
                                        }
                                    } else {
                                        if (!matchesCustomItem(slotItem, itemIng.id())) {
                                            matched = false;
                                            break;
                                        }
                                    }
                                } else if (ing instanceof Ingredient.MaterialIngredient matIng) {
                                    if (!matchMaterial(slotItem, matIng.material())) {
                                        matched = false;
                                        break;
                                    }
                                } else {
                                    matched = false;
                                    break;
                                }
                            }
                        } else {
                            if (slotItem != null && !slotItem.getType().isAir()) {
                                matched = false;
                                break;
                            }
                        }
                    }
                    if (!matched) break;
                }

                if (matched) {
                    ItemStack result = itemFactory.create(shaped.getResult().item(), shaped.getResult().amount());
                    itemService.validateAndUpdate(result);
                    inventory.setResult(result);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean tryResolveShapeless(CraftingInventory inventory, ItemStack[] matrix,
                                        Player player, RecipeDefinition recipe) {
        List<Ingredient> required = recipe.getIngredients();
        List<ItemStack> nonNullSlots = new ArrayList<>();

        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                nonNullSlots.add(item);
            }
        }

        int totalNeeded = 0;
        for (Ingredient ing : required) {
            totalNeeded += ing.amount();
        }
        if (nonNullSlots.size() != totalNeeded) {
            return false;
        }

        List<Ingredient> expandedReq = new ArrayList<>();
        for (Ingredient ing : required) {
            for (int k = 0; k < ing.amount(); k++) {
                expandedReq.add(ing);
            }
        }

        boolean[] usedSlot = new boolean[nonNullSlots.size()];

        for (Ingredient ing : expandedReq) {
            boolean matched = false;
            for (int j = 0; j < nonNullSlots.size(); j++) {
                if (usedSlot[j]) continue;
                ItemStack slotItem = nonNullSlots.get(j);
                if (ing instanceof Ingredient.ItemIngredient itemIng) {
                    if (itemIng.isVanilla() && matchVanillaItem(slotItem, itemIng.id())) {
                        usedSlot[j] = true;
                        matched = true;
                        break;
                    } else if (!itemIng.isVanilla() && matchesCustomItem(slotItem, itemIng.id())) {
                        usedSlot[j] = true;
                        matched = true;
                        break;
                    }
                } else if (ing instanceof Ingredient.MaterialIngredient matIng) {
                    if (matchMaterial(slotItem, matIng.material())) {
                        usedSlot[j] = true;
                        matched = true;
                        break;
                    }
                }
            }

            if (!matched) {
                return false;
            }
        }

        ItemStack result = itemFactory.create(recipe.getResult().item(), recipe.getResult().amount());
        itemService.validateAndUpdate(result);
        inventory.setResult(result);
        return true;
    }

    public boolean matchesCustomItem(ItemStack item, String targetId) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        if (itemService.isItem(item, targetId)) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey k : pdc.getKeys()) {
            if (k.getKey().equals("custom_item_id") || k.getKey().equals("item_id")) {
                String val = pdc.get(k, PersistentDataType.STRING);
                if (val != null) {
                    if (val.equals(targetId)) return true;
                    if (targetId.endsWith(":" + val)) return true;
                    if (val.endsWith(":" + targetId)) return true;
                }
            }
        }
        return false;
    }

    private boolean matchVanillaItem(ItemStack item, String vanillaId) {
        if (item == null || item.getType().isAir() || itemService.isCustomItem(item)) {
            return false;
        }
        String matName = vanillaId.substring("minecraft:".length()).toUpperCase(Locale.ROOT);
        return item.getType().name().equals(matName);
    }

    private boolean matchMaterial(ItemStack item, Material material) {
        return item != null && !item.getType().isAir()
                && item.getType() == material
                && !itemService.isCustomItem(item);
    }

    private int countCustomItems(PlayerInventory inv, String customId) {
        int count = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && !stack.getType().isAir() && matchesCustomItem(stack, customId)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private int countMaterials(PlayerInventory inv, Material material) {
        int count = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && !stack.getType().isAir()
                    && stack.getType() == material
                    && !itemService.isCustomItem(stack)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private ItemStack takeCustomItem(PlayerInventory inv, String customId, int amount) {
        int remaining = amount;
        ItemStack sample = null;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.getType().isAir() && matchesCustomItem(stack, customId)) {
                if (sample == null) {
                    sample = stack.clone();
                    sample.setAmount(amount);
                }
                int stackAmount = stack.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    inv.setItem(i, null);
                } else {
                    stack.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
        return remaining == 0 ? sample : null;
    }

    private ItemStack takeMaterial(PlayerInventory inv, Material material, int amount) {
        int remaining = amount;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.getType().isAir()
                    && stack.getType() == material
                    && !itemService.isCustomItem(stack)) {
                int stackAmount = stack.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    inv.setItem(i, null);
                } else {
                    stack.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
        return remaining == 0 ? new ItemStack(material, amount) : null;
    }

    private Material parseVanillaMaterial(String vanillaId) {
        if (vanillaId.startsWith("minecraft:")) {
            return Material.matchMaterial(vanillaId.substring("minecraft:".length()).toUpperCase(Locale.ROOT));
        }
        return Material.matchMaterial(vanillaId.toUpperCase(Locale.ROOT));
    }

    private String getDisplayName(String customId) {
        ItemDefinition def = itemRegistry.get(customId);
        if (def != null && def.getDisplayName() != null) {
            return def.getDisplayName();
        }
        return customId;
    }
}
