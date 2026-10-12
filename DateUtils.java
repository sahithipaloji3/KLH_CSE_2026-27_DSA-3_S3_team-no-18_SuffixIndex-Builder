package com.suffixindex.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Date and time formatting utilities.
 */
public class DateUtils {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatEpoch(long epochMillis) {
        if (epochMillis <= 0) {
            return "N/A";
        }
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        return dt.format(DISPLAY_FORMATTER);
    }

    public static String currentDateString() {
        return LocalDateTime.now().format(DATE_ONLY_FORMATTER);
    }

    public static String currentDateTimeString() {
        return LocalDateTime.now().format(DISPLAY_FORMATTER);
    }

    public static String nowFormatted() {
        return currentDateTimeString();
    }
}
