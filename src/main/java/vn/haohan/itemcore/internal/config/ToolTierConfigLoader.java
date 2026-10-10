package vn.haohan.itemcore.internal.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import vn.haohan.itemcore.api.block.ToolTier;
import vn.haohan.itemcore.api.block.ToolTierRegistry;

import java.io.File;
import java.util.logging.Logger;

/**
 * Loads custom ToolTier definitions from tiers.yml into ToolTierRegistry.
 */
public final class ToolTierConfigLoader {

    private final Logger logger;

    public ToolTierConfigLoader(Logger logger) {
        this.logger = logger != null ? logger : Logger.getLogger(ToolTierConfigLoader.class.getName());
    }

    public int loadFile(File file, ToolTierRegistry registry) {
        if (file == null || !file.exists() || !file.isFile()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection tiersSection = config.getConfigurationSection("tiers");
        if (tiersSection == null) {
            tiersSection = config;
        }

        int count = 0;
        for (String key : tiersSection.getKeys(false)) {
            ConfigurationSection sec = tiersSection.getConfigurationSection(key);
            if (sec == null) continue;

            try {
                int level = sec.getInt("level", 1);
                double mult = sec.getDouble("multiplier", (double) level * 2.0);
                ToolTier tier = ToolTier.of(key, level, (float) mult);
                registry.register(tier);
                count++;
            } catch (Exception e) {
                logger.warning("[ToolTierConfigLoader] Failed to parse tier: " + key + ": " + e.getMessage());
            }
        }

        logger.info("[ToolTierConfigLoader] Loaded " + count + " tool tiers from " + file.getName());
        return count;
    }
}
