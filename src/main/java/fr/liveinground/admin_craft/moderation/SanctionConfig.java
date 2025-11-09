package fr.liveinground.admin_craft.moderation;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionTemplate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;


public class SanctionConfig {
    private final Path file;
    private final List<String> availableReasons = new ArrayList<>();
    private final Map<String, Map<Integer, SanctionTemplate>> sanctions = new HashMap<>();

    public SanctionConfig(Path configDir) {
        this.file = configDir.resolve("admin_craft_sanctions.yml");
    }

    public void load() {
        sanctions.clear();
        availableReasons.clear();

        if (!Files.exists(file)) {
            try {
                createDefault();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create default config", e);
            }
        }

        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

        try (InputStream in = Files.newInputStream(file)) {
            Object root = yaml.load(in);
            if (!(root instanceof Map<?, ?> map)) {
                AdminCraft.LOGGER.error("Invalid YAML format in " + file);
                return;
            }

            Object reasonsObj = map.get("reasons");
            if (!(reasonsObj instanceof Map<?, ?> reasons)) {
                System.err.println("'reasons' section is missing");
                return;
            }

            for (Map.Entry<?, ?> entry : reasons.entrySet()) {
                String key = entry.getKey().toString();
                Object sectionObj = entry.getValue();
                if (!(sectionObj instanceof Map<?, ?> section)) continue;

                Object displayNameObj = section.get("displayName");
                String displayName = (displayNameObj != null) ? displayNameObj.toString() : key;
                Object messageObj = section.get("message");
                String message = (messageObj != null) ? messageObj.toString() : "No message provided";

                Map<Integer, SanctionTemplate> levelMap = new HashMap<>();

                Object levelsObj = section.get("levels");
                if (levelsObj instanceof Map<?, ?> levels) {
                    for (Map.Entry<?, ?> levelEntry : levels.entrySet()) {
                        try {
                            int level = Integer.parseInt(levelEntry.getKey().toString());
                            String raw = levelEntry.getValue().toString();
                            SanctionTemplate template = parseTemplate(displayName, message, raw);
                            levelMap.put(level, template);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid level in " + key + ": " + levelEntry.getKey());
                        }
                    }
                }

                sanctions.put(displayName, levelMap);
                availableReasons.add(displayName);
            }

            System.out.println("Sanctions loaded: " + availableReasons.size() + " reasons.");
        } catch (IOException e) {
            throw new RuntimeException("Issue when reading " + file, e);
        }
    }

    private SanctionTemplate parseTemplate(String name, String message, String raw) {
        if (raw == null) return new SanctionTemplate(name, message, Sanction.WARN, null);

        String[] parts = raw.split(":", 2);
        String typeStr = parts[0].toLowerCase();
        String duration = parts.length > 1 ? parts[1] : null;

        Sanction type = switch (typeStr) {
            case "warn" -> Sanction.WARN;
            case "kick" -> Sanction.KICK;
            case "ban" -> Sanction.BAN;
            case "tempban" -> Sanction.TEMPBAN;
            case "tempmute" -> Sanction.TEMPMUTE;
            default -> throw new IllegalStateException("Unexpected value: " + typeStr);
        };

        return new SanctionTemplate(name, message, type, duration);
    }

    private void createDefault() throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
            String defaultYaml = """
                reasons:
                  cheat:
                    displayName: "Cheating"
                    message: "Cheating / Unfair advantage"
                    levels:
                      1: "tempban:1d"
                      2: "tempban:3m"
                      3: "ban"

                  spam:
                    displayName: "Spam"
                    message: "Spamming is not allowed!"
                    levels:
                      1: "warn"
                      2: "kick"
                      3: "tempmute:1h"
                      5: "ban"
                """;
            out.write(defaultYaml.getBytes());
        }
    }

    public List<String> getAvailableReasons() {
        return Collections.unmodifiableList(availableReasons);
    }

    public Map<String, Map<Integer, SanctionTemplate>> getSanctions() {
        return Collections.unmodifiableMap(sanctions);
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
