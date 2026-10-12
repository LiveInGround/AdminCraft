package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.commands.tools.HistoryCommand;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.reports.ReportData;
import fr.liveinground.admin_craft.storage.types.reports.ReportStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ReportsCommand {
    private static final int reportsPerPage = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reports")
                .requires(commandSource -> commandSource.hasPermission(Config.reports_level))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        list(ctx, GameProfileArgument.getGameProfiles(ctx, "player"), IntegerArgumentType.getInteger(ctx, "page"));
                                        return 1;
                                    })
                                )
                                .executes(ctx -> {
                                    list(ctx, GameProfileArgument.getGameProfiles(ctx, "player"), 1);
                                    return 1;
                                })
                        )
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                list(ctx, null, IntegerArgumentType.getInteger(ctx, "page"));
                                return 1;
                            })
                        )
                        .executes(ctx -> {
                            list(ctx, null, 1);
                            return 1;
                        })
                )
                .then(Commands.literal("from")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("claim")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("unclaim")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("complete")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
        );
    }

    private static void list(CommandContext<CommandSourceStack> ctx, @Nullable Collection<NameAndId> profiles, int page) {
        assert page >= 1;
        List<ReportData> data;
        Optional<NameAndId> oprofile = null;
        MutableComponent output;

        if (profiles == null) {
            data = Database.listReports();
            output = Component.literal("").append(Component.literal("Reports overview:\n").withStyle(ChatFormatting.GOLD));
        } else {
            oprofile = profiles.stream().findFirst();
            NameAndId profile;
            if (oprofile.isPresent()) {
                profile = oprofile.get();
                data = Database.listReportsOf(profile.id());
            } else {
                ctx.getSource().sendFailure(Component.literal("No profile found.").withStyle(ChatFormatting.RED));
                return;
            }
            if (data.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal(profile.name() + " hasn't been reported yet."), false);
                return;
            }
            output = Component.literal("").append(Component.literal(profile.name() + "'s reports:\n").withStyle(ChatFormatting.GOLD));
        }
        if (data.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No report was issued yet."), false);
            return;
        }

        int totalPages = Math.max(1, (data.size() + reportsPerPage - 1) / reportsPerPage);

        page = Math.clamp(page, 1, totalPages);

        int index_min = (page - 1) * reportsPerPage;
        int index_max = Math.min(index_min + reportsPerPage, data.size());

        for (int i = index_min; i < index_max; i++) {
            ReportData report = data.get(i);

            if (profiles == null && !report.status().equals(ReportStatus.UNRESOLVED)) continue;
            output.append(Component.literal("* ").withStyle(ChatFormatting.GRAY));

            output.append(Component.literal("#" + report.id() + "\n").withStyle(ChatFormatting.DARK_RED));

            output.append(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("Status: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(report.status().toString() + "\n")
                            .withStyle(getStatusColor(report.status())));
            HistoryCommand.addField(output, "Date", report.date().toString());
            HistoryCommand.addField(output, "Reason", report.reason());
            HistoryCommand.addField(output, "Reported by", report.sourceUUID().toString());
            HistoryCommand.addField(output, "Target", report.targetUUID().toString());

            if (report.claimedUUID() != null) {
                HistoryCommand.addField(output, "Claimed by", report.claimedUUID().toString());
            } else {
                HistoryCommand.addField(output, "Claimed by", "Nobody");
            }
        }
        output.append(Component.literal("\nPage " + page + " on " + totalPages));

        ctx.getSource().sendSuccess(() -> output, false);
    }

    private static ChatFormatting getStatusColor(ReportStatus status) {
        return switch (status) {
            case UNRESOLVED -> ChatFormatting.YELLOW;
            case CLAIMED -> ChatFormatting.AQUA;
            case SANCTION_ISSUED -> ChatFormatting.GREEN;
            default -> ChatFormatting.GRAY;
        };
    }

}