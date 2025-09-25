package fr.liveinground.admin_craft;

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
}
