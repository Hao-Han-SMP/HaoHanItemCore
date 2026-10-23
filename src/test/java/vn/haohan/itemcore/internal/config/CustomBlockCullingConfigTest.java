package vn.haohan.itemcore.internal.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class CustomBlockCullingConfigTest {

    @Test
    @DisplayName("Parses culling configuration with custom values")
    void testConfigParsing() {
        String yaml = """
            custom-blocks:
              culling:
                spherical-radius: 32
                hysteresis-margin: 4
            """;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        CustomBlockCullingConfig cullingConfig = CustomBlockCullingConfig.fromConfig(config);

        assertEquals(32.0, cullingConfig.getSphericalRadius());
        assertEquals(1024.0, cullingConfig.getSphericalRadiusSq());
        assertEquals(36.0 * 36.0, cullingConfig.getHysteresisRadiusSq());
    }

    @Test
    @DisplayName("Loads fallback default configuration when section is missing")
    void testConfigDefaults() {
        YamlConfiguration emptyConfig = new YamlConfiguration();
        CustomBlockCullingConfig cullingConfig = CustomBlockCullingConfig.fromConfig(emptyConfig);

        assertEquals(32.0, cullingConfig.getSphericalRadius());
        assertEquals(1024.0, cullingConfig.getSphericalRadiusSq());
        assertEquals(36.0 * 36.0, cullingConfig.getHysteresisRadiusSq());
    }
}
