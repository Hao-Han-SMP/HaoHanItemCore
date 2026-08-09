package vn.haohan.itemcore.api.item;

import java.util.List;

/** A titled block of lore lines attached to an item definition. */
public record ItemInfoSection(String title, List<String> lines) {
    public ItemInfoSection {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Info section title cannot be blank");
        if (lines == null) throw new IllegalArgumentException("Info section lines cannot be null");
        lines = List.copyOf(lines);
    }
}
