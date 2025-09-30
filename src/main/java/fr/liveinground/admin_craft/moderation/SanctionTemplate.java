package fr.liveinground.admin_craft.moderation;

public record SanctionTemplate(String name, String sanctionMessage, Sanction type, String duration) {
}
