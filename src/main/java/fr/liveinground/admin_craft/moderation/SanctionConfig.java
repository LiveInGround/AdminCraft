package fr.liveinground.admin_craft.moderation;

import com.electronwill.nightconfig.core.file.FileConfig;
import fr.liveinground.admin_craft.AdminCraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SanctionConfig {
    private static FileConfig sanctionConfig;
    public static Map<String, Map<Integer, String>> sanctions = new HashMap<>();    // Sanction config
    public static Map<String, Map<Integer, String>> escalates = new HashMap<>();  // Escalate config

    public static void load(Path configDir) {
        Path file = configDir.resolve("admin_craft_sanctions.toml");

        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
                // Default
                Files.writeString(file, """
                [reasons.cheat]
                1 = "tempban:1d"
                2 = "tempban:3m"

                [reasons.spam]
                1 = "tempmute:1h"
                
                [reasons._mute]
                3 = "tempban:1d"
                """);
            } catch (IOException e) {
                AdminCraft.LOGGER.error(e.getMessage());
            }
        }

        sanctionConfig = FileConfig.builder(file).autoreload().autosave().build();
        sanctionConfig.load();

        sanctions.clear();
        escalates.clear();

        // Exemple : sanctions.reasons.cheat.1 = "tempban:1d"
        if (sanctionConfig.contains("reasons")) {
            var reasons = sanctionConfig.get("reasons");
            if (reasons instanceof Map<?,?> map) {
                for (var entry : map.entrySet()) {
                    String reason = entry.getKey().toString();
                    Map<Integer, String> levels = new HashMap<>();
                    if (entry.getValue() instanceof Map<?,?> inner) {
                        for (var innerEntry : inner.entrySet()) {
                            try {
                                int lvl = Integer.parseInt(innerEntry.getKey().toString());
                                levels.put(lvl, innerEntry.getValue().toString());
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (reason.startsWith("_")) {
                        reason = reason.substring(1);
                        String[] parts = levels[1].split(":", 2);

                        String type = parts[0];
                        String value = parts.length > 1 ? parts[1] : "";

                        escalates.put(reason.substring(1), levels);
                    } else {
                        sanctions.put(reason, levels);
                    }
                }
            }
        }
    }
}
