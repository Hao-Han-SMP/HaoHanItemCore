package vn.haohan.itemcore.recipe;

import vn.haohan.itemcore.api.item.ItemDefinition;
import vn.haohan.itemcore.api.item.ItemInstanceData;
import vn.haohan.itemcore.api.item.ItemRegistry;
import vn.haohan.itemcore.api.item.ItemService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Lightweight ItemService stub for unit testing without Bukkit Server dependency.
 */
public class TestItemService implements ItemService {

    private final ItemRegistry registry;
    private final Map<ItemStack, String> customItems = new IdentityHashMap<>();

    public TestItemService(ItemRegistry registry) {
        this.registry = registry;
    }

    public static ItemStack mockItem(Material mat, int amount) {
        ItemStack stack = Mockito.mock(ItemStack.class);
        final Material[] currentMat = new Material[]{mat};
        final int[] currentAmount = new int[]{amount};

        Mockito.when(stack.getType()).thenAnswer(i -> currentMat[0]);
        Mockito.when(stack.getAmount()).thenAnswer(i -> currentAmount[0]);
        Mockito.doAnswer(i -> {
            currentMat[0] = i.getArgument(0);
            return null;
        }).when(stack).setType(ArgumentMatchers.any(Material.class));
        Mockito.doAnswer(i -> {
            currentAmount[0] = i.getArgument(0);
            return null;
        }).when(stack).setAmount(ArgumentMatchers.anyInt());

        Mockito.when(stack.clone()).thenAnswer(invocation -> mockItem(currentMat[0], currentAmount[0]));

        return stack;
    }

    public static ItemStack mockItem(Material mat) {
        return mockItem(mat, 1);
    }

    @Override
    public ItemStack create(String id) {
        return create(id, 1);
    }

    @Override
    public ItemStack create(String id, int amount) {
        ItemDefinition def = registry.get(id);
        Material mat = def != null ? def.getMaterial() : Material.PAPER;
        ItemStack stack = mockItem(mat, amount);
        customItems.put(stack, id);
        return stack;
    }

    @Override
    public boolean isItem(ItemStack item, String id) {
        if (item == null) return false;
        String registeredId = customItems.get(item);
        return id.equals(registeredId);
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        return item != null && customItems.containsKey(item);
    }

    @Override
    public String getId(ItemStack item) {
        return item != null ? customItems.get(item) : null;
    }

    @Override
    public ItemDefinition getDefinition(String id) {
        return registry.get(id);
    }

    @Override
    public boolean exists(String id) {
        return registry.exists(id);
    }

    @Override
    public Map<String, Object> getProperties(ItemStack item) {
        String id = getId(item);
        if (id == null) return Map.of();
        ItemDefinition def = registry.get(id);
        return def != null ? def.getProperties() : Map.of();
    }

    @Override
    public ItemInstanceData getInstanceData() {
        return null;
    }

    @Override
    public ItemStack validateAndUpdate(ItemStack item) {
        return item;
    }
}
