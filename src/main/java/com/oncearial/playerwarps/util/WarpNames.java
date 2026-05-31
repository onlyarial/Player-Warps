package com.oncearial.playerwarps.util;

import java.text.Normalizer;
import java.util.Locale;

public final class WarpNames {
    private WarpNames() {}

    public static String join(String[] args, int start) {
        return join(args, start, args.length);
    }

    public static String join(String[] args, int start, int endExclusive) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < endExclusive; i++) {
            if (i > start) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }

    public static String normalize(String input) {
        if (input == null) return "";
        String stripped = Text.stripFormatting(input);
        String normalized = Normalizer.normalize(stripped, Normalizer.Form.NFKC);
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String input, int maxVisibleLength) {
        if (input == null) return false;
        String visible = Text.stripFormatting(input).trim();
        if (visible.isEmpty() || visible.length() > maxVisibleLength) return false;

        for (int i = 0; i < visible.length(); i++) {
            if (Character.isISOControl(visible.charAt(i))) return false;
        }
        return true;
    }
}
