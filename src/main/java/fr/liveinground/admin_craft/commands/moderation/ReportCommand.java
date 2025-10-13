package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.mutes.Utils;
import fr.liveinground.admin_craft.storage.types.reports.PlayerReportsData;
import fr.liveinground.admin_craft.storage.types.reports.ReportData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ReportCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("report")
                .requires(CommandSourceStack::isPlayer)
                .requires(commandSource -> commandSource.hasPermission(0))
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("reason", StringArgumentType.greedyString())).executes(ctx -> {
                            if (ctx.getSource().getPlayer() != null) {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                ServerPlayer reportedPlayer = EntityArgument.getPlayer(ctx, "player");
                                String reason = StringArgumentType.getString(ctx, "reason");

                                if (player.equals(reportedPlayer)) {
                                    ctx.getSource().sendFailure(Component.literal("You can't report yourself!"));
                                    return 1;
                                }

                                AdminCraft.playerDataManager.addReport(reportedPlayer.getStringUUID(), player.getStringUUID(), reason);

                                for (ServerPlayer operator: Utils.getOnlineOperators()) {
                                    operator.sendSystemMessage(Component.literal(PlaceHolderSystem.replacePlaceholders("%player% was reported by %source%: %reason%.",
                                            Map.of("player", reportedPlayer.getDisplayName().getString(),
                                                    "source", player.getDisplayName().getString(),
                                                    "reason", reason))).withStyle(ChatFormatting.YELLOW));
                                }

                                if ((Config.report_webhook != null)) {
                                    CompletableFuture.runAsync(() -> {
                                        try {
                                            sendWebhookMessage(reportedPlayer, player, reason);
                                        } catch (Exception e) {
                                            AdminCraft.LOGGER.error("An error occurred while posting a report into Discord Webhooks:", e);
                                            player.sendSystemMessage(Component.literal("An issue may have occurred during your report. Don't hesitate to contact the staff if no operator is online.").withStyle(ChatFormatting.YELLOW));
                                        }
                                    });
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("Report successfully submitted. Thank you for your vigilance.").withStyle(ChatFormatting.GREEN), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("This command can only be run by players."));
                            }

                    return 1;
                })));

        dispatcher.register(Commands.literal("reports")
                .requires(commandSource -> commandSource.hasPermission(Config.reports_level))
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> {
                            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                            if (!profiles.isEmpty()) {

                                GameProfile targetProfile = profiles.iterator().next();
                                ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(targetProfile.getId());
                                if (player == null) {
                                    ctx.getSource().sendFailure(Component.literal("No player was found"));
                                    return 1;
                                }

                                PlayerReportsData data = AdminCraft.playerDataManager.getReportDatasByUUID(player.getStringUUID());
                                if (data == null) {
                                    ctx.getSource().sendSuccess(() -> Component.literal(player.getDisplayName().getString() + " wasn't reported."), false);
                                    return 1;
                                }
                                StringBuilder list = new StringBuilder(player.getDisplayName().getString() + "'s reports");
                                for (ReportData d: data.reports()) {
                                    list.append(PlaceHolderSystem.replacePlaceholders("  - REPORT: %reason% (reported by %source%, %date%)",
                                            Map.of("reason", d.reason(),
                                                    "source", d.sourceUUID(),
                                                    "date", d.date().toString())));
                                }

                                ctx.getSource().sendSuccess(() -> Component.literal(list.toString()), false);

                            } else {
                                ctx.getSource().sendFailure(Component.literal("No player was found"));
                            }
                            return 1;
                })));
    }

    private static String calculateDist(ServerPlayer reportedPlayer, ServerPlayer player) {
        String dist;
        if (reportedPlayer.level().dimension().equals(player.level().dimension())) {
            double x1 = player.getOnPos().getX();
            double y1 = player.getOnPos().getY();
            double z1 = player.getOnPos().getZ();

            double x2 = reportedPlayer.getOnPos().getX();
            double y2 = reportedPlayer.getOnPos().getY();
            double z2 = reportedPlayer.getOnPos().getZ();


            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            dist = String.valueOf(Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)));
        } else {
            dist = "Players are not in the same dimension";
        }
        return dist;
    }

    private static void sendWebhookMessage(ServerPlayer reportedPlayer, ServerPlayer player, String reason) throws Exception {
        URL url = new URL(Config.report_webhook);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String json = """
            {
              "username": "Report system",
              "avatar_url": "https://images-ext-1.discordapp.net/external/UinHcMDrxmO4hwH3wSq1EHAxDA2wZYrsdDQPmKqUHuE/https/cdn.discordapp.com/icons/1420500702811390014/38b64cae66eb4cdb642ff18432e8876b.png?format=webp&quality=lossless",
              "embeds": [
                {
                  "title": "A new report was issued by player",
                  "description": "%sourceName% (%sourceUUID%) reported %targetName% (%targetUUID%): '%reason%'",
                  "color": 16711680,
                  "fields": [
                    { "name": "%sourceName% health", "value": "%sourceHealth% H.P.", "inline": false },
                    { "name": "%targetName% health", "value": "%targetHealth% H.P.", "inline": false },

                    { "name": "%sourceName% location", "value": "%sourceLevel%, %sourceX%, %sourceY%, %sourceZ%", "inline": false },
                    { "name": "%targetName% location", "value": "%targetLevel%, %targetX%, %targetY%, %targetZ%", "inline": false },
                    { "name": "Distance", "value": "%distance%", "inline": false }

                  ],
                  "footer": { "text": "AdminCraft - Report system" },
                  "timestamp": "%date%"
                }
              ]
            }
            """;

        String dist = calculateDist(reportedPlayer, player);

        json = PlaceHolderSystem.replacePlaceholders(json,
                Map.ofEntries(
                        Map.entry("reason", reason),
                        Map.entry("targetName", reportedPlayer.getDisplayName().getString()),
                        Map.entry("targetHealth", reportedPlayer.getHealth() + "/" + reportedPlayer.getMaxHealth()),
                        Map.entry("targetUUID", reportedPlayer.getStringUUID()),
                        Map.entry("targetLevel", reportedPlayer.level().toString()),
                        Map.entry("targetX", String.valueOf(reportedPlayer.getOnPos().getX())),
                        Map.entry("targetY", String.valueOf(reportedPlayer.getOnPos().getY())),
                        Map.entry("targetZ", String.valueOf(reportedPlayer.getOnPos().getZ())),

                        Map.entry("sourceName", player.getDisplayName().getString()),
                        Map.entry("sourceHealth", player.getHealth() + "/" + player.getMaxHealth()),
                        Map.entry("sourceUUID", player.getStringUUID()),
                        Map.entry("sourceLevel", player.level().toString()),
                        Map.entry("sourceX", String.valueOf(player.getOnPos().getX())),
                        Map.entry("sourceY", String.valueOf(player.getOnPos().getY())),
                        Map.entry("sourceZ", String.valueOf(player.getOnPos().getZ())),

                        Map.entry("distance", dist),

                        Map.entry("date", Instant.now().toString())
                ));

        try (OutputStream os = connection.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }
}
