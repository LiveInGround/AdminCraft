package fr.liveinground.admin_craft.lang;

import fr.liveinground.admin_craft.PlaceHolderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class LangManager {

    private static final TranslationProvider PROVIDER = new TranslationProvider();

    private static String language = "en_US";

    private static final Map<String, String> ALIASES = Map.of(
            "b", "bold",
            "u", "underlined",
            "i", "italic",
            "s", "strikethrough",
            "o", "obfuscated"
    );

    private LangManager() {}

    public static void setLanguage(String lang) {
        language = lang;
    }

    public static String getLanguage() {
        return language;
    }

    public static void load(Path langDir) {
        PROVIDER.clear();

        Path file = langDir.resolve(language + ".json");

        PROVIDER.loadFromFile(file);
    }

    public static String tr(String key) {
        return PROVIDER.getRaw(key);
    }

    public static String tr(String key, Map<String, String> placeholders) {
        String value = tr(key);
        return PlaceHolderSystem.replacePlaceholders(value, placeholders);
    }

    public static void reload(Path langDir) {
        load(langDir);
    }

    private static String unescapeSubString(String s) {
        return s.replace("\\\\", "\\")
                .replace("\\<", "<")
                .replace("\\>", ">");
    }


    private static boolean isEscaped(String input, int index) {
        int backslashes = 0;
        for (int i = index - 1; i >= 0 && input.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes%2 != 0;
    }

    public static Component format(String input) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY;
        int cursor = 0;

        while (cursor < input.length()) {
            int start = input.indexOf("<", cursor);
            if (start == -1) {
                result.append(Component.literal(unescapeSubString(input.substring(cursor))).withStyle(style));
                break;
            }

            if (isEscaped(input, start)) {
                result.append(Component.literal(unescapeSubString(input.substring(cursor, start + 1))).withStyle(style));
                cursor = start + 1;
                continue;
            }

            if (start > cursor) {
                result.append(Component.literal(unescapeSubString(input.substring(cursor, start))).withStyle(style));
            }

            int end = input.indexOf('>', start + 1);

            if (end == -1) {
                result.append(Component.literal(unescapeSubString(input.substring(start))).withStyle(style));
                break;
            }

            if (isEscaped(input, end)) {
                result.append(Component.literal(unescapeSubString(input.substring(start, end + 1))).withStyle(style));
                cursor = end + 1;
                continue;
            }

            String tag = input.substring(start + 1, end).toLowerCase(Locale.ROOT);

            tag = ALIASES.getOrDefault(tag, tag);

            if (tag.equals("reset")) {
                style = Style.EMPTY;
            } else {
                ChatFormatting formatting = ChatFormatting.getByName(tag);

                if (formatting != null) {
                    if (formatting.isColor()) {
                        style = style.withColor(formatting);
                    } else {
                        switch (formatting) {
                            case BOLD -> style = style.withBold(true);
                            case ITALIC -> style = style.withItalic(true);
                            case UNDERLINE -> style = style.withUnderlined(true);
                            case STRIKETHROUGH -> style = style.withStrikethrough(true);
                            case OBFUSCATED -> style = style.withObfuscated(true);
                        }
                    }
                } else {
                    result.append(Component.literal(unescapeSubString(input.substring(start, end + 1))).withStyle(style));
                }
            }
            cursor = end + 1;
        }
        return result;
    }

    public static Component replace(Component component, String placeholder, Component replacement) {
        MutableComponent result = Component.empty().withStyle(component.getStyle());

        if (component.getContents() instanceof PlainTextContents.LiteralContents(String text)) {
            int cursor = 0;

            while (true) {
                int index = text.indexOf(placeholder, cursor);

                if (index == -1) {
                    if (cursor < text.length()) {
                        result.append(Component.literal(text.substring(cursor)));
                    }
                    break;
                }

                if (index > cursor) {
                    result.append(Component.literal(text.substring(cursor, index)));
                }

                result.append(replacement.copy());
                cursor = index + placeholder.length();
            }
        } else {
            result.append(component.copy());
            return result;
        }

        for (Component sibling : component.getSiblings()) {
            result.append(replace(sibling, placeholder, replacement));
        }

        return result;
    }

    public static Component parse(String key, Map<String, String> placeholders) {
        String translated = tr(key);
        String replaced = PlaceHolderSystem.safeReplacePlaceholders(translated, placeholders);

        return format(replaced);
    }

    public static Component parse(String key) {
        return parse(key, Map.of());
    }
}
