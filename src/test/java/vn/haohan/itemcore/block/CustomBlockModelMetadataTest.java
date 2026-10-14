package vn.haohan.itemcore.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.haohan.itemcore.api.block.CustomBlockModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockModelMetadataTest {

    @Test
    @DisplayName("CustomBlockModel pre-compiles and caches PacketEvents metadata")
    void testCachedMetadataCreation() {
        ItemStack item = vn.haohan.itemcore.recipe.TestItemService.mockItem(Material.DIAMOND_SWORD);
        CustomBlockModel model = CustomBlockModel.ofItem(item);

        List<EntityData<?>> metadata = model.getCachedMetadata();
        assertNotNull(metadata, "Cached metadata must not be null");

        // Identity check: subsequent calls return the exact same instance (zero allocation)
        assertSame(metadata, model.getCachedMetadata(), "Must return identical cached instance");

        if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded()) {
            assertFalse(metadata.isEmpty(), "Cached metadata must contain display attributes when PacketEvents is loaded");
        }
    }
}
