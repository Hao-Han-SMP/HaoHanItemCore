package vn.haohan.itemcore.internal.recipe;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemFactory;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;
import vn.haohan.itemcore.api.recipe.Ingredient;
import vn.haohan.itemcore.api.recipe.RecipeDefinition;
import vn.haohan.itemcore.api.recipe.RecipeService;
import vn.haohan.itemcore.api.recipe.RecipeType;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.List;

/**
 * Bộ phân giải và xử lý công thức Bàn Thợ Rèn (Smithing Table Resolver).
 * 
 * <p>Nhiệm vụ:
 * <ul>
 *   <li>Xác minh nghiêm ngặt 3 ô nguyên liệu: Template (Khuôn đúc), Base (Trang bị gốc), Addition (Nguyên liệu phụ).</li>
 *   <li>Chống Carrier Leak: Ngăn tuyệt đối Custom Item tham gia vào công thức nâng cấp Netherite hoặc công thức vanilla khác.</li>
 *   <li>Bảo toàn & Phục hồi công thức Vanilla: Đảm bảo 100% công thức Netherite Upgrade và Armor Trim vanilla hoạt động trơn tru.</li>
 *   <li>Giải quyết chính xác Custom Smithing Recipe và hỗ trợ dispatch sự kiện onCraft.</li>
 * </ul>
 */
public final class SmithingRecipeResolver {

    private final RecipeIngredientMatcher matcher;
    private final RecipeService recipeService;
    private final ItemFactory itemFactory;
    private final ItemService itemService;
    private final ItemRegistry itemRegistry;

    public SmithingRecipeResolver(RecipeIngredientMatcher matcher, RecipeService recipeService,
                                  ItemFactory itemFactory, ItemService itemService, ItemRegistry itemRegistry) {
        this.matcher = matcher;
        this.recipeService = recipeService;
        this.itemFactory = itemFactory;
        this.itemService = itemService;
        this.itemRegistry = itemRegistry;
    }

    /**
     * Xử lý sự kiện PrepareSmithingEvent khi các slot trong bàn thợ rèn thay đổi.
     */
    public void resolve(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack template = inventory.getItem(0); // Template slot
        ItemStack base = inventory.getItem(1);     // Base slot
        ItemStack addition = inventory.getItem(2); // Addition slot

        // Nếu tất cả các slot đều trống thì không làm gì
        if (isEmpty(template) && isEmpty(base) && isEmpty(addition)) {
            return;
        }

        RecipeDefinition customRecipe = findSmithingRecipe(template, base, addition);

        if (customRecipe != null) {
            // Khớp công thức custom -> Tạo kết quả Custom Item
            if (itemFactory != null) {
                ItemStack result = itemFactory.create(
                        customRecipe.getResult().item(),
                        customRecipe.getResult().amount()
                );
                if (itemService != null) {
                    itemService.validateAndUpdate(result);
                }
                event.setResult(result);
            }
            return;
        }

        // Không khớp công thức custom nào:
        // 1. Chống Carrier Leak: Nếu bất kỳ slot nào chứa Custom Item mà không khớp công thức custom -> Chặn ngay lập tức
        if (matcher.containsCustomItem(template, base, addition)) {
            event.setResult(null);
            return;
        }

        // 2. Bảo toàn & Phục hồi công thức Vanilla:
        ItemStack currentResult = event.getResult();
        if (currentResult != null && !matcher.isCustomItem(currentResult) && !RecipeIngredientMatcher.isAir(currentResult)) {
            // Kết quả vanilla hợp lệ do Minecraft server tạo -> Giữ nguyên
            return;
        }

        // Nếu Minecraft server trả về null (do bị ghi đè bởi registry custom) hoặc trả về custom rác:
        // Tự động phân giải công thức vanilla (Netherite Upgrade / Armor Trim)
        ItemStack fallbackVanillaResult = resolveVanillaSmithing(template, base, addition);
        if (fallbackVanillaResult != null) {
            event.setResult(fallbackVanillaResult);
        } else if (currentResult != null && matcher.isCustomItem(currentResult)) {
            event.setResult(null);
        }
    }

    /**
     * Tự động phân giải các công thức Smithing chuẩn của Vanilla (Netherite Upgrade & Armor Trim).
     */
    public ItemStack resolveVanillaSmithing(ItemStack template, ItemStack base, ItemStack addition) {
        if (matcher.containsCustomItem(template, base, addition)) {
            return null;
        }

        if (isEmpty(template) || isEmpty(base) || isEmpty(addition)) {
            return null;
        }

        Material templateMat = template.getType();
        Material baseMat = base.getType();
        Material additionMat = addition.getType();

        // 1. Nâng cấp Netherite (Kim cương -> Netherite)
        if (templateMat == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE && additionMat == Material.NETHERITE_INGOT) {
            Material targetMat = getNetheriteUpgradeTarget(baseMat);
            if (targetMat != null) {
                ItemStack result = base.clone();
                result.setType(targetMat);
                result.setAmount(1);
                return result;
            }
        }

        // 2. Điêu khắc giáp (Armor Trim)
        if (isTrimTemplate(templateMat) && isTrimMaterial(additionMat) && isTrimmableArmor(baseMat)) {
            ItemStack result = base.clone();
            result.setAmount(1);
            applyArmorTrim(result, templateMat, additionMat);
            return result;
        }

        return null;
    }

    /**
     * Tìm kiếm Custom Smithing Recipe khớp chính xác với 3 nguyên liệu đầu vào.
     */
    public RecipeDefinition findSmithingRecipe(ItemStack template, ItemStack base, ItemStack addition) {
        if (recipeService == null) {
            return null;
        }

        for (RecipeDefinition recipe : recipeService.all()) {
            if (recipe.getType() != RecipeType.SMITHING) {
                continue;
            }

            List<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.size() < 3) {
                continue;
            }

            if (matcher.matches(ingredients.get(0), template) &&
                matcher.matches(ingredients.get(1), base) &&
                matcher.matches(ingredients.get(2), addition)) {
                return recipe;
            }
        }

        return null;
    }

    /**
     * Xử lý sự kiện click vào ô kết quả của Smithing Table.
     */
    public void handleResultClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof SmithingInventory inventory)) {
            return;
        }

        // Slot 3 là ô kết quả trong SmithingInventory
        if (event.getRawSlot() != 3) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (RecipeIngredientMatcher.isAir(result)) {
            return;
        }

        ItemStack template = inventory.getItem(0);
        ItemStack base = inventory.getItem(1);
        ItemStack addition = inventory.getItem(2);

        // Kiểm tra Carrier Leak khi click lấy kết quả
        RecipeDefinition customRecipe = findSmithingRecipe(template, base, addition);
        if (customRecipe == null && matcher.containsCustomItem(template, base, addition)) {
            event.setCancelled(true);
            inventory.setItem(3, null);
            return;
        }

        if (customRecipe != null && matcher.isCustomItem(result)) {
            if (event.getWhoClicked() instanceof Player player) {
                String resultId = customRecipe.getResult().item();
                ItemDefinition definition = itemRegistry != null ? itemRegistry.get(resultId) : null;
                if (definition != null && definition.hasBehavior()) {
                    var context = new vn.haohan.itemcore.api.item.ItemContext(player, result, definition, event);
                    definition.getBehavior().onCraft(context);
                }
            }
        }
    }

    private boolean isEmpty(ItemStack item) {
        return RecipeIngredientMatcher.isAir(item);
    }

    private Material getNetheriteUpgradeTarget(Material diamondMat) {
        if (diamondMat == null) return null;
        return switch (diamondMat) {
            case DIAMOND_HELMET -> Material.NETHERITE_HELMET;
            case DIAMOND_CHESTPLATE -> Material.NETHERITE_CHESTPLATE;
            case DIAMOND_LEGGINGS -> Material.NETHERITE_LEGGINGS;
            case DIAMOND_BOOTS -> Material.NETHERITE_BOOTS;
            case DIAMOND_SWORD -> Material.NETHERITE_SWORD;
            case DIAMOND_PICKAXE -> Material.NETHERITE_PICKAXE;
            case DIAMOND_AXE -> Material.NETHERITE_AXE;
            case DIAMOND_SHOVEL -> Material.NETHERITE_SHOVEL;
            case DIAMOND_HOE -> Material.NETHERITE_HOE;
            default -> null;
        };
    }

    private boolean isTrimTemplate(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.endsWith("_SMITHING_TEMPLATE") && !name.equals("NETHERITE_UPGRADE_SMITHING_TEMPLATE");
    }

    private boolean isTrimMaterial(Material mat) {
        if (mat == null) return false;
        return switch (mat) {
            case AMETHYST_SHARD, COPPER_INGOT, DIAMOND, EMERALD, GOLD_INGOT,
                 IRON_INGOT, LAPIS_LAZULI, NETHERITE_INGOT, QUARTZ, REDSTONE -> true;
            default -> false;
        };
    }

    private boolean isTrimmableArmor(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private void applyArmorTrim(ItemStack armor, Material templateMat, Material additionMat) {
        try {
            ItemMeta meta = armor.getItemMeta();
            if (meta instanceof ArmorMeta armorMeta) {
                TrimPattern pattern = getTrimPattern(templateMat);
                TrimMaterial trimMaterial = getTrimMaterial(additionMat);
                if (pattern != null && trimMaterial != null) {
                    armorMeta.setTrim(new ArmorTrim(trimMaterial, pattern));
                    armor.setItemMeta(armorMeta);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private TrimPattern getTrimPattern(Material template) {
        if (template == null) return null;
        return switch (template) {
            case SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SENTRY;
            case VEX_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.VEX;
            case WILD_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.WILD;
            case COAST_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.COAST;
            case DUNE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.DUNE;
            case WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.WAYFINDER;
            case RAISER_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.RAISER;
            case SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SHAPER;
            case HOST_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.HOST;
            case WARD_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.WARD;
            case SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SILENCE;
            case TIDE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.TIDE;
            case SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SNOUT;
            case RIB_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.RIB;
            case EYE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.EYE;
            case SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SPIRE;
            case FLOW_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.FLOW;
            case BOLT_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.BOLT;
            default -> null;
        };
    }

    private TrimMaterial getTrimMaterial(Material material) {
        if (material == null) return null;
        return switch (material) {
            case AMETHYST_SHARD -> TrimMaterial.AMETHYST;
            case COPPER_INGOT -> TrimMaterial.COPPER;
            case DIAMOND -> TrimMaterial.DIAMOND;
            case EMERALD -> TrimMaterial.EMERALD;
            case GOLD_INGOT -> TrimMaterial.GOLD;
            case IRON_INGOT -> TrimMaterial.IRON;
            case LAPIS_LAZULI -> TrimMaterial.LAPIS;
            case NETHERITE_INGOT -> TrimMaterial.NETHERITE;
            case QUARTZ -> TrimMaterial.QUARTZ;
            case REDSTONE -> TrimMaterial.REDSTONE;
            default -> null;
        };
    }
}
