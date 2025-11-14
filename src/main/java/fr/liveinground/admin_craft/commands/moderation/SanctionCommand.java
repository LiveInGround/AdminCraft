package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.moderation.*;
import fr.liveinground.admin_craft.storage.types.reports.PlayerReportsData;
import fr.liveinground.admin_craft.storage.types.reports.ReportData;
import fr.liveinground.admin_craft.storage.types.tools.PlayerHistoryData;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class SanctionCommand {

    private static final String DUPLICATE_SANCTION = "Action denied: the player is already muted.\nThe process was stopped to prevent unintended stacking of sanctions.\nTo apply another type of sanction, please use the relevant command.";

    private static final SuggestionProvider<CommandSourceStack> REASON_SUGGESTIONS =
            (context, builder) -> {
                List<String> reasons = Config.availableReasons;
                if (reasons.isEmpty()) reasons = Collections.emptyList();
                return SharedSuggestionProvider.suggest(reasons, builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sanction")
                        .requires(commandSource -> commandSource.hasPermission(Config.sanction_level))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("reason", StringArgumentType.word()).suggests(REASON_SUGGESTIONS).executes(ctx -> {
                                            ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                                            String reason = StringArgumentType.getString(ctx, "reason");
                                            if (!Config.availableReasons.contains(reason)) {
                                                ctx.getSource().sendFailure(Component.literal("This reason is not configured yet."));
                                                return 1;
                                            }
                                            Map<Integer, SanctionTemplate> sanctionMap = Config.sanctions.get(reason);
                                            PlayerHistoryData history = AdminCraft.playerDataManager.getHistoryFromUUID(sanctionedPlayer.getStringUUID());
                                            int counter = 1;
                                            if (!(history==null || history.sanctionList.isEmpty())) {
                                                for (SanctionData data: history.sanctionList) {
                                                    if (data.reason.equals(sanctionMap.get(1).sanctionMessage())) {
                                                        counter ++;
                                                    }
                                                }
                                            }

                                            if (sanctionMap.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("This reason is not configured yet."));
                                                return 1;
                                            }
                                            if (counter > sanctionMap.size()) counter = sanctionMap.size();
                                            boolean ok = false;
                                            SanctionTemplate template=null;
                                            while (!ok) {
                                                if (sanctionMap.containsKey(counter)) {
                                                    template = sanctionMap.get(counter);
                                                    ok = true;
                                                } else {
                                                    if (counter <= 0) {
                                                        ctx.getSource().sendFailure(Component.literal("No sanction configured. Please take a look at the sanction config."));
                                                        return 1;
                                                    } else {
                                                        counter --;
                                                    }
                                                }
                                            }
                                            reason = template.sanctionMessage();
                                            switch (template.type()) {
                                                case BAN:
                                                    CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().toString(), sanctionedPlayer, reason, null);
                                                    break;
                                                case TEMPBAN:
                                                    Date banExpiresOn = SanctionConfig.getDurationAsDate(template.duration());
                                                    CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().toString(), sanctionedPlayer, reason, banExpiresOn);
                                                    break;
                                                case KICK:
                                                    CustomSanctionSystem.kickPlayer(sanctionedPlayer, reason);
                                                    break;
                                                case MUTE:
                                                    if (AdminCraft.mutedPlayersUUID.contains(sanctionedPlayer.getStringUUID())) {
                                                        ctx.getSource().sendFailure(Component.literal(DUPLICATE_SANCTION));
                                                        return 1;
                                                    }
                                                    CustomSanctionSystem.mutePlayer(sanctionedPlayer, reason, null);
                                                    break;
                                                case TEMPMUTE:
                                                    if (AdminCraft.mutedPlayersUUID.contains(sanctionedPlayer.getStringUUID())) {
                                                        ctx.getSource().sendFailure(Component.literal(DUPLICATE_SANCTION));
                                                        return 1;
                                                    }
                                                    Date muteExpiresOn = SanctionConfig.getDurationAsDate(template.duration());
                                                    CustomSanctionSystem.mutePlayer(sanctionedPlayer, reason, muteExpiresOn);
                                                    break;
                                                case WARN:
                                                    CustomSanctionSystem.warnPlayer(sanctionedPlayer, reason, ctx.getSource().toString());
                                                    break;
                                            }

                                            final String finalReason = reason;
                                            final SanctionTemplate finalTemplate = template;
                                            ctx.getSource().sendSuccess(() -> Component.literal(PlaceHolderSystem.replacePlaceholders("%player% was sanctionned (%type%): %reason%.",
                                                    Map.of("player", sanctionedPlayer.getDisplayName().getString(),
                                                            "type", finalTemplate.type().toString(),
                                                            "reason", finalReason))), true);

                                            return 1;
                                        }))
                                )
        );

        dispatcher.register(Commands.literal("history")
                .requires(commandSource -> commandSource.hasPermission(Config.sanction_level))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(ctx -> {
                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                    if (profiles.isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("No player was found"));
                        return 1;
                    }
                    GameProfile targetProfile = profiles.iterator().next();
                    ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(targetProfile.getId());

                    assert player != null;

                    MutableComponent output = Component.literal("");
                    output.append(Component.literal(player.getName().getString() + "'s history:\n").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD, ChatFormatting.UNDERLINE));
                    PlayerHistoryData playerHistory = AdminCraft.playerDataManager.getHistoryFromUUID(player.getStringUUID());
                    PlayerReportsData reportsData = AdminCraft.playerDataManager.getReportDatasByUUID(player.getStringUUID());
                    if (!((playerHistory == null || playerHistory.sanctionList.isEmpty()) && (reportsData == null || reportsData.reports().isEmpty()))) {
                        if (playerHistory!=null && !playerHistory.sanctionList.isEmpty()) {
                            for (SanctionData data : playerHistory.sanctionList) {
                                if (data.expiresOn != null) {
                                    if (data.expiresOn.before(new Date())) {
                                        output.append(Component.literal("- " + data.sanctionType.name() + ": ")
                                                        .withStyle(ChatFormatting.DARK_RED))
                                                .append(Component.literal(data.reason)
                                                        .withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" (" + data.date.toString() + "), ")
                                                        .withStyle(ChatFormatting.RED))
                                                .append(Component.literal("expired on " + data.expiresOn.toString())
                                                        .withStyle(ChatFormatting.AQUA));
                                    } else {
                                        output.append(Component.literal("- " + data.sanctionType.name() + ": ")
                                                        .withStyle(ChatFormatting.DARK_RED))
                                                .append(Component.literal(data.reason)
                                                        .withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" (" + data.date.toString() + "), ")
                                                        .withStyle(ChatFormatting.RED))
                                                .append(Component.literal(SanctionConfig.getDurationAsStringFromDate(data.expiresOn))
                                                        .withStyle(ChatFormatting.AQUA));
                                    }
                                } else {

                                    output.append(Component.literal("- " + data.sanctionType.name() + ": ")
                                                    .withStyle(ChatFormatting.DARK_RED))
                                            .append(Component.literal(data.reason)
                                                    .withStyle(ChatFormatting.YELLOW))
                                            .append(Component.literal(" (" + data.date.toString() + ")")
                                                    .withStyle(ChatFormatting.RED));
                                }
                                output.append("\n");
                            }
                        } else {
                            output.append(Component.literal("This player was never sanctioned.\n").withStyle(ChatFormatting.GREEN));
                        }
                        output.append(Component.literal("Reports:\n").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD, ChatFormatting.UNDERLINE));
                        if (reportsData != null && !reportsData.reports().isEmpty()) {
                            for (ReportData data: reportsData.reports()) {
                                output.append(Component.literal("- REPORT: ")
                                                .withStyle(ChatFormatting.DARK_RED))
                                        .append(Component.literal(data.reason())
                                                .withStyle(ChatFormatting.YELLOW))
                                        .append(Component.literal(" (reported by " + data.sourceUUID() + ", ")
                                                .withStyle(ChatFormatting.AQUA))
                                        .append(Component.literal(data.date().toString())
                                                .withStyle(ChatFormatting.RED))
                                        .append(Component.literal(")\n").withStyle(ChatFormatting.AQUA));
                            }
                        } else {
                            output.append(Component.literal("This player was never reported.\n").withStyle(ChatFormatting.GREEN));
                        }
                    } else {
                        output.append(Component.literal("This player has no history").withStyle(ChatFormatting.GREEN));

                    }

                    ctx.getSource().sendSuccess(() -> output, false);

                    return 1;
                }))
        );
    }
}