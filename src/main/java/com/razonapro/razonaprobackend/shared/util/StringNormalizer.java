package com.razonapro.razonaprobackend.shared.util;

import org.springframework.util.StringUtils;

public final class StringNormalizer {

    private StringNormalizer() {}

    /** trim + UPPERCASE; null-safe; vacío → null */
    public static String upper(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    /** trim + lowercase; null-safe; vacío → null. Para emails (case-insensitive). */
    public static String lower(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    /** trim sin cambiar case; null-safe; vacío → null */
    public static String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}