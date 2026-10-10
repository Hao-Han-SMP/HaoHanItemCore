package vn.haohan.itemcore.block;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolTierRegistry;
import vn.haohan.itemcore.internal.config.ToolTierConfigLoader;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ToolTierConfigLoaderTest {

    private ToolTierRegistry registry;
    private ToolTierConfigLoader loader;

    @BeforeEach
    void setUp() {
        registry = new ToolTierRegistry();
        loader = new ToolTierConfigLoader(Logger.getLogger("TestLogger"));
    }

    @Test
    void testLoadTiersFromYaml(@TempDir File tempDir) throws IOException {
        File configFile = new File(tempDir, "tiers.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("tiers.copper.level", 2);
        yaml.set("tiers.copper.multiplier", 3.5);
        yaml.set("tiers.mithril.level", 6);
        yaml.set("tiers.mithril.multiplier", 10.0);
        yaml.save(configFile);

        int loaded = loader.loadFile(configFile, registry);
        assertEquals(2, loaded);

        ToolTier copper = registry.get("copper").orElse(null);
        assertNotNull(copper);
        assertEquals(2, copper.level());
        assertEquals(3.5f, copper.baseMultiplier());

        ToolTier mithril = registry.get("mithril").orElse(null);
        assertNotNull(mithril);
        assertEquals(6, mithril.level());
        assertEquals(10.0f, mithril.baseMultiplier());
    }
}
