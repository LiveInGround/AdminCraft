package fr.liveinground.admin_craft.tests;

import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionTemplate;

import java.util.*;

public class TestSanc {
    public static void main(String[] args) {
        List<? extends String> st = List.of("Foreign_language@Violation of rule 5: Using foreign language in public chat@1->warn", "Respect@Violation of rule 1: Disrespect@1->warn@2->tempban:1h@3->tempban:1d@4->tempban:14d", "Racism@Violation of rule 2: Racism@1->warn@2->tempban:1h@3->tempban:1d@4->tempban:14d", "Advertisement@Violation of rule 3: Advertisment@1->warn@2->tempban:1h@3->tempban:1d@4->tempban:14d", "Spam@Violation of rule 4: Spamming@1->warn@2->tempban:1h@3->tempban:1d@4->tempban:14d", "Listen_to_staff@Violation of rule 13: Listen to staff@1->warn@2->tempban:1h@3->tempban:1d@4->tempban:14d", "Killing@Violation of rule 10: Killing@1->tempban:1d@2->tempban:14d", "Griefing@Violation of rule 8: Griefing@1->tempban:14d@2->ban", "Ban_evasion@Violation of rule 7: Ban evasion@1->ban", "DDoS_DoS_Threats@Threatening of DDoS/DoS@1->ban", "Seed_Cracking@Violation of rule 12: Seed cracking@1->ban");
        List<String> availableReasons = new ArrayList<>();
        Map<String, Map<Integer, SanctionTemplate>> sanctions = new HashMap<>();

        for (String entry: st) {
            System.out.println("Analysing sanction entry " + entry);
            try {
                String[] parts = entry.split("@");
                if (parts.length < 3) {
                    System.out.println("Invalid template (length < 3): " + entry);
                    continue;
                }

                String displayName = parts[0].trim();
                String reason = parts[1].trim();

                availableReasons.add(displayName);
                Map<Integer, SanctionTemplate> levels = new HashMap<>();

                for (int i = 2; i < parts.length; i++) {
                    String segment = parts[i].trim();
                    String[] levelSplit = segment.split("->");
                    if (levelSplit.length != 2) continue;

                    int level = Integer.parseInt(levelSplit[0].trim());
                    String action = levelSplit[1].trim();

                    String duration = null;
                    Sanction type;

                    if (action.contains(":")) {
                        String[] actSplit = action.split(":");
                        type = Sanction.valueOf(actSplit[0].trim().toUpperCase());
                        duration = actSplit[1].trim();
                    } else {
                        type = Sanction.valueOf(action.trim().toUpperCase());
                    }

                    SanctionTemplate template = new SanctionTemplate(displayName, reason, type, duration);
                    levels.put(level, template);
                }

                sanctions.put(displayName, levels);
            } catch (Exception e) {
                System.out.println("Error parsing sanction template: {}");
                System.out.println(entry);
                System.out.println("Exception details:");
                System.out.println(e);
            }
        }

        System.out.println(sanctions);
        System.out.println(availableReasons);
    }
}
