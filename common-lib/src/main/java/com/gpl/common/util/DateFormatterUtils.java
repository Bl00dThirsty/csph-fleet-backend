package com.gpl.common.util;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for formatting and parsing dates.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@UtilityClass
public class DateFormatterUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter FRENCH_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    /*
     * Formats an instant to ISO-8601 string.
     */
    public String formatIso(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_FORMATTER.format(instant);
    }

    /*
     * Formats an instant to French string format.
     */
    public String formatFrench(Instant instant) {
        if (instant == null) {
            return null;
        }
        return FRENCH_FORMATTER.format(instant);
    }

    /*
     * Parses an ISO-8601 string to instant.
     */
    public Instant parseIso(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) {
            return null;
        }
        return Instant.parse(isoString);
    }
}
