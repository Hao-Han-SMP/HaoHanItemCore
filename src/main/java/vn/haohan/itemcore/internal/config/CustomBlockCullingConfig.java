package vn.haohan.itemcore.internal.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Immutable configuration for Custom Block client-side culling.
 */
public final class CustomBlockCullingConfig {

    public static final double DEFAULT_SPHERICAL_RADIUS = 32.0;
    public static final double DEFAULT_HYSTERESIS_MARGIN = 4.0;
    public static final boolean DEFAULT_SURFACE_CULLING = true;
    public static final boolean DEFAULT_FRUSTUM_CULLING = true;
    public static final double DEFAULT_FRUSTUM_MARGIN_DEGREES = 20.0;
    public static final double DEFAULT_FRUSTUM_ANGLE_THRESHOLD = 15.0;
    public static final double DEFAULT_FOV_DEGREES = 70.0;

    private final double sphericalRadius;
    private final double sphericalRadiusSq;
    private final double hysteresisRadiusSq;
    private final boolean surfaceCullingEnabled;
    private final boolean frustumCullingEnabled;
    private final double frustumMarginDegrees;
    private final double frustumAngleThreshold;
    private final double frustumCosThreshold;

    public CustomBlockCullingConfig(
            double sphericalRadius,
            double hysteresisMargin,
            boolean surfaceCullingEnabled,
            boolean frustumCullingEnabled,
            double frustumMarginDegrees,
            double frustumAngleThreshold) {
        this.sphericalRadius = Math.max(8.0, sphericalRadius);
        this.sphericalRadiusSq = this.sphericalRadius * this.sphericalRadius;
        double hystRadius = this.sphericalRadius + Math.max(0.0, hysteresisMargin);
        this.hysteresisRadiusSq = hystRadius * hystRadius;
        this.surfaceCullingEnabled = surfaceCullingEnabled;
        this.frustumCullingEnabled = frustumCullingEnabled;
        this.frustumMarginDegrees = Math.max(0.0, frustumMarginDegrees);
        this.frustumAngleThreshold = Math.max(1.0, frustumAngleThreshold);

        double halfFovRad = Math.toRadians((DEFAULT_FOV_DEGREES / 2.0) + this.frustumMarginDegrees);
        this.frustumCosThreshold = Math.cos(halfFovRad);
    }

    public static CustomBlockCullingConfig defaultConfig() {
        return new CustomBlockCullingConfig(
                DEFAULT_SPHERICAL_RADIUS,
                DEFAULT_HYSTERESIS_MARGIN,
                DEFAULT_SURFACE_CULLING,
                DEFAULT_FRUSTUM_CULLING,
                DEFAULT_FRUSTUM_MARGIN_DEGREES,
                DEFAULT_FRUSTUM_ANGLE_THRESHOLD
        );
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
        boolean surface = section.getBoolean("surface-culling", DEFAULT_SURFACE_CULLING);
        boolean frustum = section.getBoolean("frustum-culling", DEFAULT_FRUSTUM_CULLING);
        double fovMargin = section.getDouble("frustum-margin-degrees", DEFAULT_FRUSTUM_MARGIN_DEGREES);
        double angleThreshold = section.getDouble("frustum-angle-threshold", DEFAULT_FRUSTUM_ANGLE_THRESHOLD);

        return new CustomBlockCullingConfig(radius, margin, surface, frustum, fovMargin, angleThreshold);
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

    public boolean isSurfaceCullingEnabled() {
        return surfaceCullingEnabled;
    }

    public boolean isFrustumCullingEnabled() {
        return frustumCullingEnabled;
    }

    public double getFrustumMarginDegrees() {
        return frustumMarginDegrees;
    }

    public double getFrustumAngleThreshold() {
        return frustumAngleThreshold;
    }

    public double getFrustumCosThreshold() {
        return frustumCosThreshold;
    }
}
