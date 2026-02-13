package fr.liveinground.admin_craft.moderation;

import com.mojang.authlib.GameProfile;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.storage.types.PlayerMuteData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CustomSanctionSystem {
    public static void banPlayer(MinecraftServer server, String source, GameProfile player, String reason, @Nullable Date expiresOn) {
        PlayerList playerList = server.getPlayerList();
        UserBanList banList = playerList.getBans();
        if (banList.isBanned(player)) {
            return;
        }
        UserBanListEntry banEntry = new UserBanListEntry(player, null, source, expiresOn, reason);
        banList.add(banEntry);

        ServerPlayer serverPlayer = AdminCraft.getOnlinePlayer(server, player);
        if (serverPlayer != null) {
            MutableComponent banMessage = Component.literal("");
            banMessage.append(Component.literal("You are now banned on this server.\nReason » ").withStyle(ChatFormatting.RED));
            banMessage.append(Component.literal(reason + "\n").withStyle(ChatFormatting.YELLOW));
            if (expiresOn == null) {
                banMessage.append(Component.literal("This is a permanent ban.\n")).withStyle(ChatFormatting.RED);
            } else {
                banMessage.append(Component.literal("This sanction will end on ").withStyle(ChatFormatting.RED));
                banMessage.append(Component.literal(expiresOn + "\n").withStyle(ChatFormatting.YELLOW));
            }

            serverPlayer.connection.disconnect(banMessage);

        }

        AdminCraft.playerDataManager.addSanction(String.valueOf(player.getId()), Sanction.BAN, reason, expiresOn);
    }

    public static void kickPlayer(ServerPlayer player, String reason) {
        player.connection.disconnect(Component.literal(reason).withStyle(ChatFormatting.RED));
        AdminCraft.playerDataManager.addSanction(player.getStringUUID(), Sanction.KICK, reason, null);
    }

    public static void mutePlayer(MinecraftServer server, GameProfile player, String reason, @Nullable Date expiresOn) {
        if (!AdminCraft.mutedPlayersUUID.contains(String.valueOf(player.getId()))) {
            AdminCraft.playerDataManager.addMuteEntry(
                    new PlayerMuteData(player.getName(), String.valueOf(player.getId()), reason, expiresOn)
            );

            ServerPlayer serverPlayer = AdminCraft.getOnlinePlayer(server, player);
            if (serverPlayer != null) {
                String msg = PlaceHolderSystem.replacePlaceholders(Config.mute_message, Map.of("reason", reason));
                serverPlayer.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));


                if (expiresOn != null) {
                    long diff = expiresOn.getTime() - System.currentTimeMillis();
                    long days = TimeUnit.MILLISECONDS.toDays(diff);
                    diff -= TimeUnit.DAYS.toMillis(days);

                    long hours = TimeUnit.MILLISECONDS.toHours(diff);
                    diff -= TimeUnit.HOURS.toMillis(hours);

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);

                    String daysStr = String.valueOf(days);
                    String hoursStr = String.valueOf(hours);
                    String minutesStr = String.valueOf(minutes);

                    String timeMsg = PlaceHolderSystem.replacePlaceholders(
                            Config.time_remaining,
                            Map.of(
                                    "days", daysStr,
                                    "hours", hoursStr,
                                    "minutes", minutesStr
                            )
                    );
                    serverPlayer.sendSystemMessage(Component.literal(timeMsg).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }

    public static void unMutePlayer(ServerPlayer player) {
        if (AdminCraft.mutedPlayersUUID.contains(player.getStringUUID())) {
            AdminCraft.playerDataManager.removeMuteEntry(AdminCraft.playerDataManager.getPlayerMuteDataByUUID(player.getStringUUID()));
            Component messageComponent = Component.literal(Config.unmute_message).withStyle(ChatFormatting.GREEN);
            player.sendSystemMessage(messageComponent);
        }
    }

    public static void warnPlayer(ServerPlayer player, @Nullable String reason, @Nullable String operator) {
        if (operator == null) {
            operator = "The Great Server (TGS)";
        }
        if (reason == null) {
            reason = "Warned by an operator";
        }
        Component title = Component.literal(Config.warn_title).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component message = Component.literal(PlaceHolderSystem.replacePlaceholders(Config.warn_message,
                Map.of("operator", operator,
                        "reason", reason))).withStyle(ChatFormatting.YELLOW);

        player.sendSystemMessage(Component.literal("---------------------------------------------").withStyle(ChatFormatting.DARK_RED));
        player.sendSystemMessage(title);
        player.sendSystemMessage(message);
        player.sendSystemMessage(Component.literal("---------------------------------------------").withStyle(ChatFormatting.DARK_RED));


        AdminCraft.playerDataManager.addSanction(player.getStringUUID(), Sanction.WARN, reason, null);
    }
}
