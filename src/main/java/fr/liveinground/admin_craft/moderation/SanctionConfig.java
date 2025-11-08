package fr.liveinground.admin_craft.moderation;

import com.electronwill.nightconfig.core.file.FileConfig;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionTemplate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SanctionConfig {
    static FileConfig sanctionConfig;
    public static Map<String, Map<Integer, SanctionTemplate>> sanctions = new HashMap<>();    // Sanction config
    public static List<String> availableReasons = new ArrayList<>();

    public static void load(Path configDir) {
        Path file = configDir.resolve("admin_craft_sanctions.toml");
        AdminCraft.LOGGER.debug("Loading sanctions configuration (" + file + ")");

        if (!Files.exists(file)) {
            AdminCraft.LOGGER.debug("Files doesn't exist");
            try {
                Files.createFile(file);

                // Default config file
                Files.writeString(file, """
                # NOTE: The server has to restart to reload this file.
                [reasons]
                    [reasons.cheat]
                        # This will be displayed in commands, must contain only ONE word (but you still can use underscores)!
                        displayName = "Cheating"
                
                        # This is used as reason message
                        message = "Cheating / Unfair advantage"
                
                        1 = "tempban:1d"    # the first one is mandatory
                        2 = "tempban:3m"
                        3 = "ban"

                    [reasons.spam]
                        displayName = "Spam"
                        message = "Spamming is not allowed!"
                        1 = "warn"
                        2 = "kick"
                        3 = "tempmute:1h"
                        5 = "ban"
                """);
                AdminCraft.LOGGER.debug("File created and written");
            } catch (IOException e) {
                AdminCraft.LOGGER.error(e.getMessage());
            }
        }

        sanctionConfig = FileConfig.builder(file).autoreload().autosave().build();
        sanctionConfig.load();
        AdminCraft.LOGGER.debug("File loaded");

        sanctions.clear();
        AdminCraft.LOGGER.debug("Previous data cleared");

        if (sanctionConfig.contains("reasons")) {
            AdminCraft.LOGGER.debug("'reasons' key detected");
            var reasons = sanctionConfig.get("reasons");
            if (reasons instanceof Map<?, ?> map) {
                AdminCraft.LOGGER.debug("'reasons' key is instance of Map<?, ?>");
                for (var entry : map.entrySet()) {
                    String reason = entry.getKey().toString();
                    Map<Integer, SanctionTemplate> sanctionsMap = new HashMap<>();

                    // Fallbacks
                    String displayName = reason;
                    if (displayName.contains(" ")) {
                        AdminCraft.LOGGER.warn("A custom sanction reason contains a space, skipping...");
                        continue;
                    }
                    String message = "";

                    if (entry.getValue() instanceof Map<?, ?> inner) {
                        for (var innerEntry : inner.entrySet()) {
                            String key = innerEntry.getKey().toString();

                            if (key.equalsIgnoreCase("displayName")) {
                                displayName = innerEntry.getValue().toString();
                                availableReasons.add(displayName);
                                AdminCraft.LOGGER.debug("New reason displayName added to list: " + displayName);
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
                                if (!checkDuration(duration)) continue;

                                SanctionTemplate s = new SanctionTemplate(displayName, message, type, duration);
                                sanctionsMap.put(lvl, s);
                            } catch (NumberFormatException ex) {
                                AdminCraft.LOGGER.warn("Invalid key in " + reason + ": " + key);
                            }
                        }
                    }
                        sanctions.put(reason, sanctionsMap);
                }
            }
        }
    }

    public static boolean checkDuration(String input) {
        return (getDuration(input) != null);
    }

    public static @Nullable List<Integer> getDuration(String input) {
        Pattern pattern = Pattern.compile(
                "^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$"
        );
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches() && !input.isEmpty()) {
            boolean hasAtLeastOne = false;
            List<Integer> output = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                String group = matcher.group(i);
                if (group != null) {
                    hasAtLeastOne = true;
                    output.add(Integer.parseInt(group));
                } else {
                    output.add(0);
                }
            }
            return hasAtLeastOne ? output : null;
        } else {
            return null;
        }
    }

    public static @Nullable Date getDurationAsDate(String input) {
        List<Integer> duration = SanctionConfig.getDuration(input);
        if (duration == null) return null;
        if (!(duration.size() == 4)) return null;
        Integer days = duration.get(0);
        Integer hours = duration.get(1);
        Integer minutes = duration.get(2);
        Integer seconds = duration.get(3);
        LocalDateTime expiresLocal = LocalDateTime.now()
                .plusHours(days * 24)
                .plusHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds);
        return Date.from(expiresLocal.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static String getDurationAsStringFromDate(Date input) {
        if (input != null) {
            long diff = input.getTime() - System.currentTimeMillis();
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            diff -= TimeUnit.DAYS.toMillis(days);

            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            diff -= TimeUnit.HOURS.toMillis(hours);

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);

            String daysStr = String.valueOf(days);
            String hoursStr = String.valueOf(hours);
            String minutesStr = String.valueOf(minutes);
            return PlaceHolderSystem.replacePlaceholders(Config.time_remaining_short,
                    Map.of("days", daysStr,
                            "hours", hoursStr,
                            "minutes", minutesStr));
        } else {
            return "N/A";
        }
    }
}
