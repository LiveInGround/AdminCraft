package fr.liveinground.admin_craft.storage.types.chat;

import java.util.UUID;

public record ChatPlayerPreference(UUID playerUUID, ChatChannel channel) {
}
