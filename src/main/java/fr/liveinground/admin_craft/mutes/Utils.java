package fr.liveinground.admin_craft.mutes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PermissionValue;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class Utils {
    public static List<ServerPlayer> getOnlineOperators() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();

        ServerOpList opList = server.getPlayerList().getOps();

        return onlinePlayers.stream()
                .filter(player -> {
                    ServerOpListEntry entry = opList.get(player.nameAndId());
                    return entry != null && entry.permissions().hasPermission(PermissionValue.MODERATORS.permission()); // niveau OP ≥ 1
                })
                .collect(Collectors.toList());
    }

    public static void logCancelledMessage(ServerPlayer player, String message) {
        if (Config.log_cancelled_events) {
            final String logMessage = PlaceHolderSystem.replacePlaceholders(Config.cancel_log_format, Map.of("player", player.getDisplayName().getString(), "message", message));
            AdminCraft.LOGGER.info(logMessage);
            for (ServerPlayer p: getOnlineOperators()) {
                p.sendSystemMessage(Component.literal(logMessage));
            }
        }
    }
}
