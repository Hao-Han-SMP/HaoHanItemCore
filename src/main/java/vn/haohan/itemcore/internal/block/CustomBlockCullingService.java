package vn.haohan.itemcore.internal.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * High-performance geometric and mathematical culling service for Custom Block rendering.
 *
 * <p>Enforces O(1) checks per block:
 * <ul>
 *   <li>Spherical radius test using distance squared (no {@link Math#sqrt}).</li>
 *   <li>Conical frustum check with pre-calculated cosine thresholds and near-distance safety sphere.</li>
 *   <li>1-face surface exposure check querying 6 cardinal block faces.</li>
 * </ul>
 * </p>
 */
public class CustomBlockCullingService {

    public static final double DEFAULT_NEAR_DISTANCE_SQ = 3.5 * 3.5; // 12.25

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

    /**
     * Calculates the cosine threshold for a conical frustum with a safety margin.
     *
     * @param fovDegrees horizontal/diagonal FOV in degrees (e.g. 70.0)
     * @param marginDegrees safety margin degrees to prevent edge pop-in (e.g. 20.0)
     * @return cosine threshold
     */
    public double calculateCosThreshold(double fovDegrees, double marginDegrees) {
        double halfFovRad = Math.toRadians((fovDegrees / 2.0) + marginDegrees);
        return Math.cos(halfFovRad);
    }

    /**
     * Checks if a block position is within the player's view cone (frustum).
     *
     * @param eyeX player eye X
     * @param eyeY player eye Y
     * @param eyeZ player eye Z
     * @param dirX normalized player look direction X
     * @param dirY normalized player look direction Y
     * @param dirZ normalized player look direction Z
     * @param bx block center X
     * @param by block center Y
     * @param bz block center Z
     * @param cosThreshold precalculated cosine threshold
     * @param nearDistSq squared near-distance threshold (always visible within this radius)
     * @return true if visible in view cone
     */
    public boolean isInFrustum(double eyeX, double eyeY, double eyeZ,
                               double dirX, double dirY, double dirZ,
                               double bx, double by, double bz,
                               double cosThreshold, double nearDistSq) {
        double vx = bx - eyeX;
        double vy = by - eyeY;
        double vz = bz - eyeZ;
        double distSq = vx * vx + vy * vy + vz * vz;

        // If inside near safety radius, always visible (prevents pop-in when turning around)
        if (distSq <= nearDistSq) {
            return true;
        }

        double invLen = 1.0 / Math.sqrt(distSq);
        double dot = (vx * dirX + vy * dirY + vz * dirZ) * invLen;
        return dot >= cosThreshold;
    }

    /**
     * Calculates direction vector from Minecraft yaw and pitch.
     *
     * @param yaw player yaw in degrees
     * @param pitch player pitch in degrees
     * @return 3-element double array [dirX, dirY, dirZ]
     */
    public double[] getDirection(float yaw, float pitch) {
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);
        double cosPitch = Math.cos(radPitch);

        double dirX = -Math.sin(radYaw) * cosPitch;
        double dirY = -Math.sin(radPitch);
        double dirZ = Math.cos(radYaw) * cosPitch;

        return new double[]{dirX, dirY, dirZ};
    }

    /**
     * Checks if at least 1 of the 6 cardinal faces of the block touches air or a non-occluding block.
     *
     * @param world world
     * @param x block X
     * @param y block Y
     * @param z block Z
     * @return true if at least 1 face is exposed
     */
    public boolean isSurfaceExposed(World world, int x, int y, int z) {
        if (world == null) return true;

        return isExposedFace(world, x + 1, y, z)
                || isExposedFace(world, x - 1, y, z)
                || isExposedFace(world, x, y + 1, z)
                || isExposedFace(world, x, y - 1, z)
                || isExposedFace(world, x, y, z + 1)
                || isExposedFace(world, x, y, z - 1);
    }

    private boolean isExposedFace(World world, int x, int y, int z) {
        try {
            Block block = world.getBlockAt(x, y, z);
            if (block == null) return true;
            Material mat = block.getType();
            if (mat == null || isAirMaterial(mat)) return true;

            try {
                if (org.bukkit.Bukkit.getServer() == null) {
                    return isNonOccludingFallback(mat);
                }
                return !mat.isOccluding();
            } catch (Throwable ignored) {
                return isNonOccludingFallback(mat);
            }
        } catch (Throwable t) {
            return true;
        }
    }

    public boolean isAirMaterial(Material mat) {
        return mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR;
    }

    private boolean isNonOccludingFallback(Material mat) {
        if (isAirMaterial(mat)) return true;
        String name = mat.name();
        return name.contains("AIR")
                || name.contains("GLASS")
                || name.contains("SLAB")
                || name.contains("STAIRS")
                || name.contains("DOOR")
                || name.contains("FENCE")
                || name.contains("WALL")
                || name.contains("LEAVES")
                || name.contains("WATER")
                || name.contains("LAVA")
                || name.contains("TORCH")
                || name.contains("LANTERN")
                || name.contains("CARPET")
                || name.contains("SIGN");
    }
}
