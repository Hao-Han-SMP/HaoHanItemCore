package vn.haohan.itemcore.internal.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Immutable configuration for Custom Block client-side culling.
 */
public final class CustomBlockCullingConfig {

    public static final double DEFAULT_SPHERICAL_RADIUS = 32.0;
    public static final double DEFAULT_HYSTERESIS_MARGIN = 4.0;

    private final double sphericalRadius;
    private final double sphericalRadiusSq;
    private final double hysteresisRadiusSq;

    public CustomBlockCullingConfig(double sphericalRadius, double hysteresisMargin) {
        this.sphericalRadius = Math.max(8.0, sphericalRadius);
        this.sphericalRadiusSq = this.sphericalRadius * this.sphericalRadius;
        double hystRadius = this.sphericalRadius + Math.max(0.0, hysteresisMargin);
        this.hysteresisRadiusSq = hystRadius * hystRadius;
    }

    public static CustomBlockCullingConfig defaultConfig() {
        return new CustomBlockCullingConfig(DEFAULT_SPHERICAL_RADIUS, DEFAULT_HYSTERESIS_MARGIN);
    }

    public static CustomBlockCullingConfig fromConfig(ConfigurationSection root) {
        if (root == null) {
            return defaultConfig();
        }

        ConfigurationSection section = root.getConfigurationSection("custom-blocks.culling");
        if (section == null) {
            return defaultConfig();
        }

        double radius = section.getDouble("spherical-radius", DEFAULT_SPHERICAL_RADIUS);
        double margin = section.getDouble("hysteresis-margin", DEFAULT_HYSTERESIS_MARGIN);

        return new CustomBlockCullingConfig(radius, margin);
    }

    public double getSphericalRadius() {
        return sphericalRadius;
    }

    public double getSphericalRadiusSq() {
        return sphericalRadiusSq;
    }

    public double getHysteresisRadiusSq() {
        return hysteresisRadiusSq;
    }
}
