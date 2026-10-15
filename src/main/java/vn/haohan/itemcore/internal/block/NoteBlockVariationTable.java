package vn.haohan.itemcore.internal.block;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.data.type.NoteBlock;

/**
 * Bi-directional lookup table mapping between custom variation indices (0..774)
 * and vanilla {@link NoteBlock} blockstate properties (instrument, note pitch, powered).
 *
 * <p>State layout:
 * <ul>
 *   <li>Total states in Minecraft NoteBlock: 16 instruments × 25 notes × 2 powered = 800 states.</li>
 *   <li>Vanilla reservation: First 25 states (PIANO / harp, notes 0..24, unpowered) are reserved for vanilla gameplay.</li>
 *   <li>Custom variations: Indices 0..774 map to states 25..799.</li>
 * </ul>
 * </p>
 */
public final class NoteBlockVariationTable {

    public static final int BASE_OFFSET = 25;
    public static final int TOTAL_VARIATIONS = 775;
    public static final int NOTES_PER_INSTRUMENT = 25;
    public static final int STATES_PER_POWER_STATE = 400; // 16 instruments * 25 notes

    private NoteBlockVariationTable() {}

    /**
     * Resolves the {@link Instrument} corresponding to the specified variation index.
     *
     * @param variation the variation index (0..774)
     * @return the corresponding instrument
     * @throws IllegalArgumentException if variation is not within 0..774
     */
    public static Instrument getInstrument(int variation) {
        if (variation < 0 || variation >= TOTAL_VARIATIONS) {
            throw new IllegalArgumentException("Variation out of bounds: " + variation + " (expected 0.." + (TOTAL_VARIATIONS - 1) + ")");
        }
        int id = variation + BASE_OFFSET;
        byte type = (byte) ((id % STATES_PER_POWER_STATE) / NOTES_PER_INSTRUMENT);
        return Instrument.getByType(type);
    }

    /**
     * Resolves the note pitch ID (0..24) corresponding to the specified variation index.
     *
     * @param variation the variation index (0..774)
     * @return note pitch ID (0..24)
     * @throws IllegalArgumentException if variation is not within 0..774
     */
    public static int getNote(int variation) {
        if (variation < 0 || variation >= TOTAL_VARIATIONS) {
            throw new IllegalArgumentException("Variation out of bounds: " + variation + " (expected 0.." + (TOTAL_VARIATIONS - 1) + ")");
        }
        int id = variation + BASE_OFFSET;
        return id % NOTES_PER_INSTRUMENT;
    }

    /**
     * Resolves the powered state corresponding to the specified variation index.
     *
     * @param variation the variation index (0..774)
     * @return true if powered, false otherwise
     * @throws IllegalArgumentException if variation is not within 0..774
     */
    public static boolean isPowered(int variation) {
        if (variation < 0 || variation >= TOTAL_VARIATIONS) {
            throw new IllegalArgumentException("Variation out of bounds: " + variation + " (expected 0.." + (TOTAL_VARIATIONS - 1) + ")");
        }
        int id = variation + BASE_OFFSET;
        return id >= STATES_PER_POWER_STATE;
    }

    /**
     * Inversely resolves the variation index from NoteBlock state properties.
     *
     * @param instrument NoteBlock instrument
     * @param note note pitch ID (0..24)
     * @param powered powered property
     * @return variation index (0..774), or -1 if the state is reserved for vanilla or invalid
     */
    public static int resolveVariation(Instrument instrument, int note, boolean powered) {
        if (instrument == null || note < 0 || note >= NOTES_PER_INSTRUMENT) {
            return -1;
        }
        int instType = instrument.getType();
        if (instType < 0 || instType > 15) {
            return -1;
        }
        int base = (instType * NOTES_PER_INSTRUMENT) + note;
        if (powered) {
            base += STATES_PER_POWER_STATE;
        }
        int variation = base - BASE_OFFSET;
        return (variation >= 0 && variation < TOTAL_VARIATIONS) ? variation : -1;
    }

    /**
     * Inversely resolves the variation index from a {@link NoteBlock} block data instance.
     *
     * @param noteBlock NoteBlock block data
     * @return variation index (0..774), or -1 if null, vanilla, or out of range
     */
    public static int resolveVariation(NoteBlock noteBlock) {
        if (noteBlock == null) {
            return -1;
        }
        return resolveVariation(noteBlock.getInstrument(), noteBlock.getNote().getId(), noteBlock.isPowered());
    }

    /**
     * Creates a {@link NoteBlock} {@link org.bukkit.block.data.BlockData} configured for the variation index.
     *
     * @param variation variation index (0..774)
     * @return configured NoteBlock block data
     */
    public static NoteBlock createBlockData(int variation) {
        NoteBlock data = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        data.setInstrument(getInstrument(variation));
        data.setNote(new Note(getNote(variation)));
        data.setPowered(isPowered(variation));
        return data;
    }
}
