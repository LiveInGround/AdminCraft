package fr.liveinground.admin_craft.moderation;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.ServerHolder;
import fr.liveinground.admin_craft.discord.DiscordBot;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.PlayerMuteData;
import fr.liveinground.admin_craft.storage.types.sanction.AppealStatus;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CustomSanctionSystem {
    public static String banPlayer(MinecraftServer server, String source, NameAndId player, String reason, @Nullable Date expiresOn, boolean appealable, @Nullable Date appealDelay) {
        PlayerList playerList = server.getPlayerList();
        UserBanList banList = playerList.getBans();
        if (banList.isBanned(player)) {
            return null;
        }
        UserBanListEntry banEntry = new UserBanListEntry(player, null, source, expiresOn, reason);
        banList.add(banEntry);

        ServerPlayer serverPlayer = AdminCraft.getOnlinePlayer(server, player);
        String id = Database.registerSanction(player.id().toString(), player.name(), new SanctionData(Sanction.BAN, reason, new Date(), expiresOn), appealable, appealDelay);
        if (id == null) {
            id = LangManager.tr(TrKeys.BAN_ID_ERROR);
        }
        if (serverPlayer != null) {
            String trKey_header;
            if (expiresOn == null) {
                trKey_header = TrKeys.SANCTION_BANNED_PERM;
            } else {
                trKey_header = TrKeys.SANCTION_BANNED_TEMP;
            }
            MutableComponent message = Component.literal("")
                    .append(LangManager.parse(trKey_header, Map.of(
                        "id", id,
                            "reason", reason,
                            "duration", SanctionConfig.getOnlyDurationAsStringFromDate(expiresOn)
                    )));


            if (!appealable || !DiscordBot.enabled) {
                message.append(LangManager.parse(TrKeys.SANCTION_BANNED_NOAPPEAL));
            } else {
                message.append(LangManager.parse(TrKeys.SANCTION_BANNED_APPEALABLE, Map.of(
                        "link", Config.invite_link
                )));
            }

            serverPlayer.connection.disconnect(message);
        }
        return id;
        //AdminCraft.playerDataManager.addSanction(String.valueOf(player.id()), Sanction.BAN, reason, expiresOn);
    }

    public static void kickPlayer(ServerPlayer player, String reason) {
        player.connection.disconnect(Component.literal(reason).withStyle(ChatFormatting.RED));
        Database.registerSanction(player.getStringUUID(), player.getPlainTextName(), new SanctionData(Sanction.KICK, reason, new Date(), null), false, null);
        //AdminCraft.playerDataManager.addSanction(player.getStringUUID(), Sanction.KICK, reason, null);
    }

    public static String mutePlayer(MinecraftServer server, NameAndId player, String reason, @Nullable Date expiresOn, boolean appealable, @Nullable Date appealDelay) {
        if (!AdminCraft.mutedPlayersUUID.contains(String.valueOf(player))) {
            //AdminCraft.playerDataManager.addMuteEntry(new PlayerMuteData(player.name(), player.id().toString(), reason, expiresOn));
            String id = Database.registerSanction(player.id().toString(), player.name(), new SanctionData(Sanction.MUTE, reason, new Date(), expiresOn), appealable, appealDelay);
            if (id == null) return null;
            ServerPlayer serverPlayer = AdminCraft.getOnlinePlayer(server, player);
            if (serverPlayer != null) {
                String msg;
                if (DiscordBot.enabled) {
                    msg = LangManager.tr(TrKeys.MUTE_BEGINS_REASON, Map.of("reason", reason, "id", id, "link", Config.invite_link));
                } else {
                    msg = LangManager.tr(TrKeys.MUTE_BEGINS_REASON_NOAPPEAL, Map.of("reason", reason));
                }
                serverPlayer.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));

                if (expiresOn != null) {
                    String timeMsg = SanctionConfig.getDurationAsStringFromDate(expiresOn);
                    serverPlayer.sendSystemMessage(Component.literal(timeMsg).withStyle(ChatFormatting.YELLOW));
                }
            }
            AdminCraft.playerDataManager.addMuteEntry(new PlayerMuteData(player.name(), player.id().toString(), reason, expiresOn));
            return id;
        }
        return null;
    }

    public static void unMutePlayer(NameAndId player) {
        if (AdminCraft.mutedPlayersUUID.contains(player.id().toString())) {
            AdminCraft.playerDataManager.removeMuteEntry(AdminCraft.playerDataManager.getPlayerMuteDataByUUID(player.id().toString()));
            Date now = new Date();
            Optional<DatabaseSanctionData> muteData = Database.getCurrentSanctions(player.id().toString()).stream().filter(
                    data -> data.type().equals(Sanction.MUTE)
                            && !data.removed()
                            && (data.expiresOn() == null || data.expiresOn().after(now)
                    )
            ).findFirst();
            if (muteData.isEmpty()) {
                AdminCraft.LOGGER.warn("Couldn't find any suitable sanction in the database corresponding to {}'s mute, skipping database change while unmuting.", player.name());
            } else {
                Database.removeSanction(muteData.get().id());
            }
            try {
                ServerPlayer serverPlayer = ServerHolder.getServer().getPlayerList().getPlayer(player.id());

                if (serverPlayer != null) {
                    Component messageComponent = Component.literal(LangManager.tr(TrKeys.MUTE_ENDS)).withStyle(ChatFormatting.GREEN);
                    serverPlayer.sendSystemMessage(messageComponent);
                }
            } catch (IllegalStateException e) {
                AdminCraft.LOGGER.error("Failed to display a message to the unmuted player {}", player.name(), e);
            }
        }
    }

    public static void warnPlayer(ServerPlayer player, @Nullable String reason, @Nullable String operator) {
        if (operator == null) {
            operator = "Server";
        }
        if (reason == null) {
            reason = "Warned by an operator";
        }

        player.sendSystemMessage(LangManager.parse(TrKeys.SANCTION_WARNED, Map.of(
                "operator", operator,
                "reason", reason
        )));

        Database.registerSanction(player.getStringUUID(), player.getPlainTextName(), new SanctionData(Sanction.WARN, reason, new Date(), null), false, null);
        //AdminCraft.playerDataManager.addSanction(player.getStringUUID(), Sanction.WARN, reason, null);
    }

    public static boolean applyAppealToSanction(DatabaseSanctionData data, AppealStatus status) {
        if (Database.changeAppealStatus(data.id(), status)) {
            NameAndId nameAndId = new NameAndId(UUID.fromString(data.uuid()), data.ign());
            switch (data.type()) {
                case MUTE -> unMutePlayer(nameAndId);
                case BAN -> ServerHolder.getServer().getPlayerList().getBans().remove(nameAndId);
                default -> AdminCraft.LOGGER.warn("An appeal was made for a non-appealable sanction type (sanction {}). Skipping appeal success procedure...", data.id());
            }
            return true;
        } else {
            AdminCraft.LOGGER.error("Failed to update appeal status for sanction id {} to {} in the database", data.id(), status.toString());
            return false;
        }
    }

    public static boolean changeDuration(@NotNull DatabaseSanctionData data, @Nullable Date newExpires) {
        NameAndId nameAndId = new NameAndId(UUID.fromString(data.uuid()), data.ign());
        if (Database.changeAppealStatus(data.id(), AppealStatus.REDUCED)) {
            switch (data.type()) {
                case MUTE -> changeMuteDuration(nameAndId, newExpires);
                case BAN -> changeBanDuration(nameAndId, newExpires);
                default -> AdminCraft.LOGGER.warn("An appeal was made for a non-appealable sanction type (sanction {}). Skipping sanction reducing procedure...", data.id());
            }
            if (!Database.editDuration(data.id(), newExpires)) AdminCraft.LOGGER.warn("Failed to edit the duration in the database. This wont impact the actual sanction.");
            return true;
        }
        AdminCraft.LOGGER.error("Failed to update appeal status for sanction id {} to REDUCED in the database", data.id());
        return false;
    }

    public static void changeMuteDuration(@NotNull NameAndId player, @Nullable Date newExpires) {
        if (AdminCraft.mutedPlayersUUID.contains(player.id().toString())) {
            PlayerMuteData oldData = AdminCraft.playerDataManager.getPlayerMuteDataByUUID(player.id().toString());
            AdminCraft.playerDataManager.removeMuteEntry(oldData);
            AdminCraft.playerDataManager.addMuteEntry(new PlayerMuteData(player.name(), player.id().toString(), Objects.requireNonNull(oldData).reason, newExpires));
            ServerPlayer serverPlayer = ServerHolder.getServer().getPlayerList().getPlayer(player.id());
            if (serverPlayer != null) {
                serverPlayer.sendSystemMessage(Component.literal("Your mute duration has been changed. It will now expire in " + SanctionConfig.getDurationAsStringFromDate(newExpires)).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    public static void changeBanDuration(NameAndId player, @Nullable Date newExpires) {
        PlayerList list = ServerHolder.getServer().getPlayerList();
        if (list.getBans().isBanned(player)) {
            UserBanListEntry old = Objects.requireNonNull(list.getBans().get(player));
            list.getBans().remove(player);
            list.getBans().add(new UserBanListEntry(player, old.getCreated(), old.getSource(), newExpires, old.getReason()));
        }
    }
}
