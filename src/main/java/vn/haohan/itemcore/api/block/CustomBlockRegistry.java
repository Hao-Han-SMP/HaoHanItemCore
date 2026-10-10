package vn.haohan.itemcore.api.block;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for managing {@link CustomBlockDefinition}s in memory.
 *
 * <p>Implementations must be thread-safe to permit asynchronous lookups by background tasks.</p>
 */
public interface CustomBlockRegistry {

    /**
     * Registers a new custom block definition.
     *
     * <p>When to use: Call during plugin startup or reload when registering available blocks.</p>
     * <p>Thread requirement: Thread-safe.</p>
     * <p>World/Packet impact: None. Only registers logical definition in memory.</p>
     *
     * @param definition the definition to register; must not be null
     * @throws NullPointerException if definition is null
     * @throws IllegalArgumentException if a definition with the same ID is already registered
     */
    void register(CustomBlockDefinition definition);

    /**
     * Retrieves a custom block definition by its identifier.
     *
     * <p>When to use: Look up definitions when creating models, parsing configs, or handling drops.</p>
     * <p>Thread requirement: Thread-safe, safe to call asynchronously.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param id the unique custom block ID; must not be null
     * @return an {@link Optional} containing the definition if found, or empty if unknown
     */
    Optional<CustomBlockDefinition> get(String id);

    /**
     * Checks if a custom block definition with the given ID exists.
     *
     * <p>When to use: Validation checks before placing or referencing blocks.</p>
     * <p>Thread requirement: Thread-safe.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @param id the identifier to test
     * @return true if registered, false otherwise
     */
    boolean has(String id);

    /**
     * Returns an unmodifiable collection of all registered custom block definitions.
     *
     * <p>When to use: Iterating through available blocks (e.g., commands, tabs, GUIs).</p>
     * <p>Thread requirement: Thread-safe.</p>
     * <p>World/Packet impact: None.</p>
     *
     * @return collection of definitions
     */
    Collection<CustomBlockDefinition> all();

    /**
     * Returns the total number of registered custom block definitions.
     *
     * @return count of registered definitions
     */
    int size();

    /**
     * Clears all registered definitions from the registry.
     *
     * <p>When to use: Call during reload or disable cycles.</p>
     * <p>Thread requirement: Thread-safe.</p>
     */
    void clear();
}
