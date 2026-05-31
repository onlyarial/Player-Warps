package com.oncearial.playerwarps.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern AMP_HEX = Pattern.compile("(?i)&x(&[0-9a-f]){6}");
    private static final Pattern SECTION_HEX = Pattern.compile("(?i)§x(§[0-9a-f]){6}");
    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)[&§][0-9a-fk-or]");
    private Text() {}

    public static String color(String input) {
        if (input == null) return "";
        Matcher matcher = HEX.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String stripFormatting(String input) {
        if (input == null) return "";
        String stripped = HEX.matcher(input).replaceAll("");
        stripped = AMP_HEX.matcher(stripped).replaceAll("");
        stripped = SECTION_HEX.matcher(stripped).replaceAll("");
        stripped = LEGACY_COLOR.matcher(stripped).replaceAll("");
        return stripped;
    }

    public static void send(CommandSender sender, String prefix, String message) {
        sender.sendMessage(color(prefix + message));
    }
}

