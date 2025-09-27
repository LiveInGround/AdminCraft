package fr.liveinground.admin_craft.mutes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraftforge.server.ServerLifecycleHooks;

import fr.liveinground.admin_craft.AdminCraft;

import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static List<ServerPlayer> getOnlineOperators() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();

        ServerOpList opList = server.getPlayerList().getOps();

        return onlinePlayers.stream()
                .filter(player -> {
                    ServerOpListEntry entry = opList.get(player.getGameProfile());
                    return entry != null && entry.getLevel() >= 1; // niveau OP ≥ 1
                })
                .collect(Collectors.toList());
    }

    public static void logCancelledMessage(ServerPlayer player, String message) {
        // todo: custom message with placeholders

        final String logMessage = "[CANCELLED] <" + player.getDisplayName().getString() + "> " + message;
        AdminCraft.LOGGER.info(logMessage);

        for (ServerPlayer p: getOnlineOperators()) {
            p.sendSystemMessage(Component.literal(logMessage));
        }
    }
}
