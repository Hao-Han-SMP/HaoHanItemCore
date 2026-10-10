package vn.haohan.itemcore.api.block;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Optional;

/**
 * Primary public service for managing, querying, and mutating custom blocks.
 *
 * <p>Custom blocks use {@link org.bukkit.Material#BARRIER} as their physical world block
 * and render visual models via client-side BlockDisplay packets. The server does not create
 * or tick any Bukkit BlockDisplay entities.</p>
 */
public interface CustomBlockService {

    /**
     * Checks whether a physical block in the world is a registered custom block.
     *
     * <p>When to use: Call before processing block events (break, interact, physics).</p>
     * <p>Thread requirement: Thread-safe, safe for async lookup.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param block the Bukkit block to check; may be null
     * @return true if the block is a registered custom block, false otherwise
     */
    boolean isCustomBlock(Block block);

    /**
     * Retrieves the custom block definition for a world block, if it is a custom block.
     *
     * <p>When to use: Call to access custom block metadata, drop item, or model information.</p>
     * <p>Thread requirement: Thread-safe, safe for async lookup.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param block the world block to resolve; may be null
     * @return an {@link Optional} containing the definition if present and registered
     */
    Optional<CustomBlockDefinition> getCustomBlock(Block block);

    /**
     * Retrieves the custom block ID string for a world block.
     *
     * <p>When to use: Fast lookup when only the ID is needed.</p>
     * <p>Thread requirement: Thread-safe, safe for async lookup.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param block the world block; may be null
     * @return custom block ID string, or null if not a custom block
     */
    String getCustomBlockId(Block block);

    /**
     * Places a custom block at the specified location.
     *
     * <p>This method mutates the world by placing a {@link org.bukkit.Material#BARRIER} block,
     * registers the location in spatial storage, and broadcasts client-side BlockDisplay
     * spawn packets to viewing players.</p>
     *
     * <p>When to use: Programmatically placing a custom block (commands, mechanics, structures).</p>
     * <p>Thread requirement: Must be invoked on the server main thread (or region thread in Folia).</p>
     * <p>World/Packet impact: Changes block to BARRIER; sends packets to nearby players.</p>
     *
     * @param location the target world location; must not be null
     * @param customBlockId registered custom block ID; must not be null
     * @return true if the block was successfully placed, false if definition is unknown
     * @throws NullPointerException if location or customBlockId is null
     * @throws IllegalStateException if called asynchronously
     */
    boolean placeCustomBlock(Location location, String customBlockId);

    /**
     * Removes a custom block at the specified location.
     *
     * <p>This method sets the world block to {@link org.bukkit.Material#AIR}, removes the entry
     * from spatial storage, and broadcasts client-side BlockDisplay destroy packets.</p>
     *
     * <p>When to use: Programmatically removing or destroying a custom block.</p>
     * <p>Thread requirement: Must be invoked on the server main thread (or region thread in Folia).</p>
     * <p>World/Packet impact: Changes block to AIR; sends destroy packets to nearby players.</p>
     *
     * @param location the target world location; must not be null
     * @return true if a custom block was present and removed, false otherwise
     * @throws NullPointerException if location is null
     * @throws IllegalStateException if called asynchronously
     */
    boolean removeCustomBlock(Location location);

    /**
     * Registers a custom block definition into the underlying registry.
     *
     * <p>When to use: Plugin registration hook.</p>
     * <p>Thread requirement: Thread-safe.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param definition custom block definition
     * @throws NullPointerException if definition is null
     */
    void register(CustomBlockDefinition definition);

    /**
     * Gets the custom block registry.
     *
     * @return the registry instance
     */
    CustomBlockRegistry getRegistry();

    /**
     * Estimates the mining duration in seconds for a player holding a tool targeting this definition.
     */
    float calculateMiningTimeSeconds(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack tool, CustomBlockDefinition definition);
}
