package vn.haohan.itemcore.internal.block;

/**
 * Geometric spherical culling service for Custom Block rendering.
 */
public class CustomBlockCullingService {

    /**
     * Checks if a target block coordinate is within a spherical radius from the player.
     *
     * @param px player X
     * @param py player Y
     * @param pz player Z
     * @param bx block X center/corner
     * @param by block Y center/corner
     * @param bz block Z center/corner
     * @param radiusSq precomputed squared radius (R^2)
     * @return true if inside spherical radius
     */
    public boolean isInSphere(double px, double py, double pz, double bx, double by, double bz, double radiusSq) {
        double dx = bx - px;
        double dy = by - py;
        double dz = bz - pz;
        return (dx * dx + dy * dy + dz * dz) <= radiusSq;
    }
}
