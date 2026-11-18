package com.umeed.donations;

import java.util.UUID;

/**
 * Search filters. Every field may be null (no filter). {@code maxKm} needs a location; the controller checks it.
 */
public record DonationSearch(
        String text, UUID categoryId, String city, Double latitude, Double longitude, Double maxKm) {

    /** "No distance limit" for the nearest-first query: far more than any distance on Earth. */
    static final double ANY_DISTANCE_KM = 100_000;

    boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    /**
     * An ILIKE pattern for the given words: they must appear in this order, with anything between them.
     * The characters that ILIKE treats specially are escaped, so a search for "50%" means just that.
     * Empty input matches everything.
     */
    static String likePattern(String input) {
        if (input == null || input.isBlank()) {
            return "%";
        }
        String escaped = input.strip()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + String.join("%", escaped.split("\\s+")) + "%";
    }
}
