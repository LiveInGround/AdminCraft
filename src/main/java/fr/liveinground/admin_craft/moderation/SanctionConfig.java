package fr.liveinground.admin_craft.moderation;

import com.electronwill.nightconfig.core.file.FileConfig;
import fr.liveinground.admin_craft.AdminCraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SanctionConfig {
    private static FileConfig sanctionConfig;
    public static Map<String, Map<Integer, SanctionTemplate>> sanctions = new HashMap<>();    // Sanction config
    public static Map<String, Map<Integer, SanctionTemplate>> escalates = new HashMap<>();  // Escalate config
    public static List<String> availableReasons;

    public static void load(Path configDir) {
        Path file = configDir.resolve("admin_craft_sanctions.toml");

        if (!Files.exists(file)) {
            try {
                Files.createFile(file);

                // Default config file
                Files.writeString(file, """
                # NOTE: The server has to restart to reload this file.
                # WARNING: Tempmute and Tempban are not implemented yet. Please avoid using them (nothing will occur if this sanction is set). Stay tuned!
                [reasons]
                    [reasons.cheat]
                        displayName = "Cheating"
                        message = "Cheating / Unfair advantage"
                        1 = "tempban:1d"
                        2 = "tempban:3m"
                        3 = "ban"

                    [reasons.spam]
                        displayName = "Spam"
                        message = "Spamming is not allowed!"
                        1 = "warn"
                        2 = "tempmute:1h"

                    [reasons._warn]
                        message = "You received %n% warns!"
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

        if (sanctionConfig.contains("reasons")) {
            var reasons = sanctionConfig.get("reasons");
            if (reasons instanceof Map<?, ?> map) {
                for (var entry : map.entrySet()) {
                    String reason = entry.getKey().toString();
                    Map<Integer, SanctionTemplate> sanctionsMap = new HashMap<>();

                    // Fallbacks
                    String displayName = reason;
                    String message = "";

                    if (entry.getValue() instanceof Map<?, ?> inner) {
                        for (var innerEntry : inner.entrySet()) {
                            String key = innerEntry.getKey().toString();

                            if (key.equalsIgnoreCase("displayName")) {
                                displayName = innerEntry.getValue().toString();
                                availableReasons.add(displayName);
                                continue;
                            }
                            if (key.equalsIgnoreCase("message")) {
                                message = innerEntry.getValue().toString();
                                continue;
                            }

                            try {
                                int lvl = Integer.parseInt(key);
                                String sanctionStr = innerEntry.getValue().toString();

                                String[] parts = sanctionStr.split(":", 2);
                                Sanction type = Sanction.valueOf(parts[0].toUpperCase());
                                String duration = parts.length > 1 ? parts[1] : "";

                                SanctionTemplate s = new SanctionTemplate(displayName, message, type, duration);
                                sanctionsMap.put(lvl, s);
                            } catch (NumberFormatException ex) {
                                AdminCraft.LOGGER.warn("Invalid key in " + reason + ": " + key);
                            }
                        }
                    }

                    if (reason.startsWith("_")) {
                        escalates.put(reason.substring(1), sanctionsMap);
                    } else {
                        sanctions.put(reason, sanctionsMap);
                    }
                }
            }
        }
    }

    public static boolean checkDuration(String input) {
        return (getDuration(input) != null);
    }

    public static List<Integer> getDuration(String input) {
        // Pattern pour capturer les nombres avant d, h, m, s
        Pattern pattern = Pattern.compile("(\\d+)d(\\d+)h(\\d+)m(\\d+)s");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            int days = Integer.parseInt(matcher.group(1));
            int hours = Integer.parseInt(matcher.group(2));
            int minutes = Integer.parseInt(matcher.group(3));
            int seconds = Integer.parseInt(matcher.group(4));
            List<Integer> output = new ArrayList<>();
            output.add(days);
            output.add(hours);
            output.add(minutes);
            output.add(seconds);
            return output;
        } else {
            return null;
        }
    }
}
