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

package de.Keyle.MyPet.api.util.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Central text renderer of MyPetClassic.
 * <p>
 * Every string that reaches a player is parsed here and sent as an Adventure {@link Component}. The parser accepts
 * three notations in the same string:
 * <ul>
 *     <li>MiniMessage tags — {@code <gold>}, {@code <gradient:#FFB6C1:#E75480>}, {@code <bold>}, …</li>
 *     <li>legacy ampersand codes — {@code &6}, {@code &l}, {@code &#FFB6C1}</li>
 *     <li>legacy section codes — {@code §6}, {@code §l} (what the translation files use after colourising)</li>
 * </ul>
 * Unknown tags are left untouched, so a pet name like {@code "<3"} is not treated as markup.
 */
public final class MessageRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().strict(false).build();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static volatile BiFunction<Player, String, String> placeholderResolver = (player, text) -> text;

    private MessageRenderer() {
    }

    /**
     * Lets the PlaceholderAPI hook resolve placeholders in rendered text without the API module depending on it.
     */
    public static void setPlaceholderResolver(@Nullable BiFunction<Player, String, String> resolver) {
        placeholderResolver = resolver != null ? resolver : (player, text) -> text;
    }

    public static Component parse(@Nullable String input) {
        return parse(input, null);
    }

    public static Component parse(@Nullable String input, @Nullable Player viewer) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String text = input;
        if (viewer != null) {
            text = placeholderResolver.apply(viewer, text);
        }
        text = legacyToTags(text);
        try {
            return MINI_MESSAGE.deserialize(text);
        } catch (RuntimeException exception) {
            // a broken tag must never swallow a message, fall back to the raw legacy text
            return LEGACY.deserialize(text);
        }
    }

    public static void send(@Nullable CommandSender sender, @Nullable String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        Player viewer = sender instanceof Player ? (Player) sender : null;
        sender.sendMessage(parse(message, viewer));
    }

    public static void actionBar(@Nullable Player player, @Nullable String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendActionBar(parse(message, player));
    }

    /**
     * Cuts a formatted string to a number of <em>visible</em> characters without splitting MiniMessage tags or
     * {@code §}/{@code &} colour codes. Used for nametags, which have a hard length limit.
     */
    public static String cut(@Nullable String input, int maxVisible) {
        if (input == null || maxVisible <= 0) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length());
        int visible = 0;
        int i = 0;
        while (i < input.length() && visible < maxVisible) {
            char c = input.charAt(i);
            if (c == '<') {
                int end = input.indexOf('>', i);
                if (end > i && input.substring(i + 1, end).matches("[a-zA-Z0-9_:#!/.,-]+")) {
                    out.append(input, i, end + 1);
                    i = end + 1;
                    continue;
                }
            }
            if (c == '§' || c == '&') {
                if (i + 1 < input.length()) {
                    out.append(c).append(input.charAt(i + 1));
                    i += 2;
                    continue;
                }
            }
            out.append(c);
            visible++;
            i++;
        }
        return out.toString();
    }

    /**
     * @return the plain text of the rendered component, without colours or MiniMessage tags
     */
    public static String strip(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }
        return PLAIN.serialize(parse(input));
    }

    /**
     * @return a section-code string for consumers that still only accept legacy text
     */
    public static String toLegacy(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }
        return LEGACY.serialize(parse(input));
    }

    /**
     * @return a section-code string for consumers that still only accept legacy text
     */
    public static String toLegacy(@Nullable Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY.serialize(component);
    }

    /**
     * Converts {@code §x}/{@code &x} colour and format codes (including {@code &#RRGGBB} and {@code §x§R§R§G§G§B§B})
     * to their MiniMessage equivalents so both notations can live in the same string.
     */
    static String legacyToTags(String input) {
        if (input.indexOf('§') == -1 && input.indexOf('&') == -1) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c != '§' && c != '&') || i + 1 >= input.length()) {
                out.append(c);
                continue;
            }
            char next = Character.toLowerCase(input.charAt(i + 1));

            // &#RRGGBB / §#RRGGBB
            if (next == '#' && i + 7 < input.length()) {
                String hex = input.substring(i + 2, i + 8);
                if (hex.matches("[0-9a-fA-F]{6}")) {
                    out.append("<#").append(hex).append('>');
                    i += 7;
                    continue;
                }
            }

            // §x§R§R§G§G§B§B / &x&R&R&G&G&B&B
            if (next == 'x' && i + 13 < input.length()) {
                StringBuilder hex = new StringBuilder(6);
                int j = i + 2;
                boolean valid = true;
                for (int k = 0; k < 6; k++) {
                    if (j + 1 >= input.length() || (input.charAt(j) != '§' && input.charAt(j) != '&')) {
                        valid = false;
                        break;
                    }
                    hex.append(input.charAt(j + 1));
                    j += 2;
                }
                if (valid && hex.toString().matches("[0-9a-fA-F]{6}")) {
                    out.append("<#").append(hex).append('>');
                    i = j - 1;
                    continue;
                }
            }

            String tag = switch (next) {
                case '0' -> "<black>";
                case '1' -> "<dark_blue>";
                case '2' -> "<dark_green>";
                case '3' -> "<dark_aqua>";
                case '4' -> "<dark_red>";
                case '5' -> "<dark_purple>";
                case '6' -> "<gold>";
                case '7' -> "<gray>";
                case '8' -> "<dark_gray>";
                case '9' -> "<blue>";
                case 'a' -> "<green>";
                case 'b' -> "<aqua>";
                case 'c' -> "<red>";
                case 'd' -> "<light_purple>";
                case 'e' -> "<yellow>";
                case 'f' -> "<white>";
                case 'k' -> "<obfuscated>";
                case 'l' -> "<bold>";
                case 'm' -> "<strikethrough>";
                case 'n' -> "<underlined>";
                case 'o' -> "<italic>";
                case 'r' -> "<reset>";
                default -> null;
            };
            if (tag != null) {
                out.append(tag);
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
