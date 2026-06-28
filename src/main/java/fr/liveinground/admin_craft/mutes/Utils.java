package fr.liveinground.admin_craft.mutes;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.ServerHolder;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
    public static List<ServerPlayer> getOnlineOperators() {
        PlayerList pl = ServerHolder.getServer().getPlayerList();
        List<ServerPlayer> onlinePlayers = pl.getPlayers();

        ServerOpList opList = pl.getOps();

        return onlinePlayers.stream()
                .filter(player -> {
                    ServerOpListEntry entry = opList.get(player.nameAndId());
                    return entry != null && entry.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.MODERATORS));
                })
                .collect(Collectors.toList());
    }

    public static void logCancelledMessage(ServerPlayer player, String message) {
        if (Config.log_cancelled_events) {
            String logMessage = LangManager.tr(TrKeys.MUTE_MESSAGE_CANCELLED_LOG, Map.of("player", player.getDisplayName().getString(), "message", message));
            AdminCraft.LOGGER.info(logMessage);
            for (ServerPlayer p: getOnlineOperators()) {
                p.sendSystemMessage(Component.literal(logMessage));
            }
        }
    }
}
