package com.gpl.common.util;

import lombok.experimental.UtilityClass;

/**
 * Utility class for sanitizing strings.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@UtilityClass
public class StringSanitizer {

    /*
     * Truncates a string to maximum length.
     */
    public String truncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }

    /*
     * Normalizes whitespace by trimming and collapsing internal spaces.
     */
    public String normalizeWhitespace(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /*
     * Strips HTML tags from text.
     */
    public String stripHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "");
    }

    /*
     * Converts to upper case and trims.
     */
    public String toUpperTrimmed(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().toUpperCase();
    }
}
