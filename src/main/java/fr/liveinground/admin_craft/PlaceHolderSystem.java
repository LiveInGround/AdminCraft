package fr.liveinground.admin_craft;

import java.util.HashMap;
import java.util.Map;

public class PlaceHolderSystem {
    public static String replacePlaceholders(String text, Map<String, String> values) {
        if (text == null || values == null || values.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    public static String safeReplacePlaceholders(String text, Map<String, String> values) {
        Map<String, String> values2 = new HashMap<>();
        final String toEscape = "<>\\";

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = values.get(key);
            StringBuilder escapedValue = new StringBuilder(value.length());

            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);

                if (toEscape.indexOf(c) >= 0) {
                    escapedValue.append('\\');
                }
                escapedValue.append(c);
            }
            values2.put(key, escapedValue.toString());
        }
        return replacePlaceholders(text, values2);
    }
}
