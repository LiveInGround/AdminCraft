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

import java.util.Collection;
import java.util.Map;

public class ReportCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("report")
                .requires(commandSource -> commandSource.hasPermission(0))
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("reason", StringArgumentType.greedyString())).executes(ctx -> {
                            if (ctx.getSource().getPlayer() != null) {
                                ServerPlayer reportedPlayer = EntityArgument.getPlayer(ctx, "player");
                                String reason = StringArgumentType.getString(ctx, "reason");

                                AdminCraft.playerDataManager.addReport(reportedPlayer.getStringUUID(), ctx.getSource().getPlayer().getStringUUID(), reason);

                                for (ServerPlayer operator: Utils.getOnlineOperators()) {
                                    operator.sendSystemMessage(Component.literal(PlaceHolderSystem.replacePlaceholders("%player% was reported by %source%: %reason%.",
                                            Map.of("player", reportedPlayer.getDisplayName().getString(),
                                                    "source", ctx.getSource().getPlayer().getDisplayName().getString(),
                                                    "reason", reason))).withStyle(ChatFormatting.YELLOW));
                                }

                                if (!(Config.report_webhook == null)) {
                                    // todo: send embed into the webhook
                                }

                            } else {
                                ctx.getSource().sendFailure(Component.literal("This command can only be runed by players."));
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
}
