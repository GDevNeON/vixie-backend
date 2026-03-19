package com.neong.vixie.helpers.api;

public final class HtmlSanitizer {

    private HtmlSanitizer() {
        // Utility class — no instantiation
    }

    /**
     * Sanitize user input by stripping HTML tags, rejecting null bytes,
     * and trimming leading/trailing whitespace.
     *
     * @param input the raw user input
     * @return sanitized string, or null if input is null
     * @throws IllegalArgumentException if input contains null bytes
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        if (input.contains("\0")) {
            throw new IllegalArgumentException("Input contains null bytes");
        }
        // Strip HTML tags
        String stripped = input.replaceAll("<[^>]*>", "");
        // Trim whitespace
        return stripped.trim();
    }
}
