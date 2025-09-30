package fr.liveinground.admin_craft.moderation;

import com.mojang.authlib.GameProfile;
import fr.liveinground.admin_craft.AdminCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.BanListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import javax.annotation.Nullable;
import java.util.Date;

public class CustomSanctionSystem {
    public static void banPlayer(MinecraftServer server, String source, ServerPlayer player, String reason, @Nullable Date expiresOn) {
        PlayerList playerList = server.getPlayerList();
        UserBanList banList = playerList.getBans();
        if (banList.isBanned(player.getGameProfile())) {
            return;
        }
        UserBanListEntry banEntry = new UserBanListEntry(player.getGameProfile(), null, source, expiresOn, reason);
        banList.add(banEntry);

        player.connection.disconnect(Component.literal("You are banned on this server:\n" + reason).withStyle(ChatFormatting.RED));

        AdminCraft.playerDataManager.addSanction(player.getStringUUID(), Sanction.BAN, reason, expiresOn);
    }

    public static void kickPlayer(ServerPlayer player, String reason) {
        player.connection.disconnect(Component.literal(reason).withStyle(ChatFormatting.RED));
        AdminCraft.playerDataManager.addSanction(player.getStringUUID(), Sanction.KICK, reason, null);
    }

    public static void mutePlayer(ServerPlayer player, String reason) {
        // todo
    }
}
