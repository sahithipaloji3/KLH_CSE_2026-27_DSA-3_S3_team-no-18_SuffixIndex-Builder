package com.suffixindex.util;

/**
 * Validation utilities for application inputs.
 */
public class ValidationUtils {

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static void requireNotBlank(String str, String fieldName) {
        if (isBlank(str)) {
            throw new IllegalArgumentException(fieldName + " cannot be empty or blank");
        }
    }

    public static void requireNotNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }
}
