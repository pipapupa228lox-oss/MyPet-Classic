/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.api.util.message.MessageRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Legacy colour helper.
 * <p>
 * Older configurations, skilltree files and translation strings use Bukkit's legacy notation — {@code <gold>},
 * {@code <dark_red>}, {@code <a>} or {@code &6}. Everything player facing is rendered as an Adventure
 * {@link net.kyori.adventure.text.Component} by {@link MessageRenderer}, so this class now only normalises those
 * legacy tags to their MiniMessage spelling ({@code <dark_red>}, {@code <obfuscated>}, …) and keeps the old
 * helper methods working. It no longer depends on {@code org.bukkit.ChatColor}.
 */
public final class Colorizer {

    private static final Map<String, String> CODE_TAGS = new HashMap<>();
    private static final Map<String, String> NAME_TAGS = new HashMap<>();
    private static final Map<Character, String> ANSI_CODES = new HashMap<>();
    private static final Pattern TAG = Pattern.compile("(?i)<(/?)([a-z0-9_]+)>");

    private Colorizer() {
    }

    /**
     * Normalises legacy colour tags to MiniMessage tags and leaves everything else untouched. Ampersand and section
     * codes are handled by {@link MessageRenderer} when the text is rendered.
     */
    public static String setColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = TAG.matcher(text);
        StringBuilder out = new StringBuilder(text.length() + 16);
        while (matcher.find()) {
            String replacement = tag(matcher.group(2));
            if (replacement != null) {
                String name = replacement.substring(1, replacement.length() - 1);
                replacement = "<" + matcher.group(1) + name + ">";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement != null ? replacement : matcher.group()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * @return the plain text without colours, formatting or tags
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return MessageRenderer.strip(setColors(text));
    }

    /**
     * Converts section/ampersand colour codes to ANSI escapes for console output.
     */
    public static String toAnsi(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        boolean converted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '\u00A7' || c == '&') && i + 1 < text.length()) {
                String ansi = ANSI_CODES.get(Character.toLowerCase(text.charAt(i + 1)));
                if (ansi != null) {
                    sb.append(ansi);
                    converted = true;
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        if (converted) {
            sb.append("\u001b[m");
        }
        return sb.toString();
    }

    private static String tag(String raw) {
        if (raw.length() == 1) {
            String single = CODE_TAGS.get(raw.toLowerCase());
            if (single != null) {
                return single;
            }
        }
        return NAME_TAGS.get(raw.toLowerCase().replace("_", ""));
    }

    static {
        CODE_TAGS.put("0", "<black>");
        CODE_TAGS.put("1", "<dark_blue>");
        CODE_TAGS.put("2", "<dark_green>");
        CODE_TAGS.put("3", "<dark_aqua>");
        CODE_TAGS.put("4", "<dark_red>");
        CODE_TAGS.put("5", "<dark_purple>");
        CODE_TAGS.put("6", "<gold>");
        CODE_TAGS.put("7", "<gray>");
        CODE_TAGS.put("8", "<dark_gray>");
        CODE_TAGS.put("9", "<blue>");
        CODE_TAGS.put("a", "<green>");
        CODE_TAGS.put("b", "<aqua>");
        CODE_TAGS.put("c", "<red>");
        CODE_TAGS.put("d", "<light_purple>");
        CODE_TAGS.put("e", "<yellow>");
        CODE_TAGS.put("f", "<white>");
        CODE_TAGS.put("k", "<obfuscated>");
        CODE_TAGS.put("l", "<bold>");
        CODE_TAGS.put("m", "<strikethrough>");
        CODE_TAGS.put("n", "<underlined>");
        CODE_TAGS.put("o", "<italic>");
        CODE_TAGS.put("r", "<reset>");

        for (Map.Entry<String, String> entry : CODE_TAGS.entrySet()) {
            NAME_TAGS.put(entry.getKey(), entry.getValue());
        }
        NAME_TAGS.put("black", "<black>");
        NAME_TAGS.put("darkblue", "<dark_blue>");
        NAME_TAGS.put("darkgreen", "<dark_green>");
        NAME_TAGS.put("darkaqua", "<dark_aqua>");
        NAME_TAGS.put("darkred", "<dark_red>");
        NAME_TAGS.put("darkpurple", "<dark_purple>");
        NAME_TAGS.put("gold", "<gold>");
        NAME_TAGS.put("gray", "<gray>");
        NAME_TAGS.put("grey", "<gray>");
        NAME_TAGS.put("darkgray", "<dark_gray>");
        NAME_TAGS.put("darkgrey", "<dark_gray>");
        NAME_TAGS.put("blue", "<blue>");
        NAME_TAGS.put("green", "<green>");
        NAME_TAGS.put("aqua", "<aqua>");
        NAME_TAGS.put("red", "<red>");
        NAME_TAGS.put("lightpurple", "<light_purple>");
        NAME_TAGS.put("yellow", "<yellow>");
        NAME_TAGS.put("white", "<white>");
        NAME_TAGS.put("magic", "<obfuscated>");
        NAME_TAGS.put("obfuscated", "<obfuscated>");
        NAME_TAGS.put("bold", "<bold>");
        NAME_TAGS.put("strikethrough", "<strikethrough>");
        NAME_TAGS.put("strike", "<strikethrough>");
        NAME_TAGS.put("underline", "<underlined>");
        NAME_TAGS.put("underlined", "<underlined>");
        NAME_TAGS.put("italic", "<italic>");
        NAME_TAGS.put("reset", "<reset>");

        // Colors
        ANSI_CODES.put('0', "\u001b[30m");   // BLACK
        ANSI_CODES.put('1', "\u001b[34m");   // DARK_BLUE
        ANSI_CODES.put('2', "\u001b[32m");   // DARK_GREEN
        ANSI_CODES.put('3', "\u001b[36m");   // DARK_AQUA
        ANSI_CODES.put('4', "\u001b[31m");   // DARK_RED
        ANSI_CODES.put('5', "\u001b[35m");   // DARK_PURPLE
        ANSI_CODES.put('6', "\u001b[33m");   // GOLD
        ANSI_CODES.put('7', "\u001b[37m");   // GRAY
        ANSI_CODES.put('8', "\u001b[90m");   // DARK_GRAY
        ANSI_CODES.put('9', "\u001b[94m");   // BLUE
        ANSI_CODES.put('a', "\u001b[92m");   // GREEN
        ANSI_CODES.put('b', "\u001b[96m");   // AQUA
        ANSI_CODES.put('c', "\u001b[91m");   // RED
        ANSI_CODES.put('d', "\u001b[95m");   // LIGHT_PURPLE
        ANSI_CODES.put('e', "\u001b[93m");   // YELLOW
        ANSI_CODES.put('f', "\u001b[97m");   // WHITE
        // Formatting
        ANSI_CODES.put('k', "\u001b[8m");    // OBFUSCATED
        ANSI_CODES.put('l', "\u001b[1m");    // BOLD
        ANSI_CODES.put('m', "\u001b[9m");    // STRIKETHROUGH
        ANSI_CODES.put('n', "\u001b[4m");    // UNDERLINE
        ANSI_CODES.put('o', "\u001b[3m");    // ITALIC
        ANSI_CODES.put('r', "\u001b[m");     // RESET
    }
}
