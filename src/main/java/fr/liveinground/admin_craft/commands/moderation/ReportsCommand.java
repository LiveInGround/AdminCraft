package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.commands.tools.HistoryCommand;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.reports.ReportData;
import fr.liveinground.admin_craft.storage.types.reports.ReportStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ReportsCommand {
    private static final int reportsPerPage = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reports")
                .requires(commandSource -> commandSource.hasPermission(Config.reports_level))
                .then(Commands.literal("list")
                        .then(Commands.literal("player")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.literal("page")
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> {
                                                            list(ctx, IntegerArgumentType.getInteger(ctx, "page"), GameProfileArgument.getGameProfiles(ctx, "player"));
                                                            return 1;
                                                        })
                                                )
                                        )
                                        .executes(ctx -> {
                                            list(ctx, 1, GameProfileArgument.getGameProfiles(ctx, "player"));
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("page")
                            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    list(ctx, IntegerArgumentType.getInteger(ctx, "page"));
                                    return 1;
                                })
                            )
                        )
                        .executes(ctx -> {
                            list(ctx, 1);
                            return 1;
                        })
                )
                .then(Commands.literal("from")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .then(Commands.literal("page")
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    listFrom(ctx, IntegerArgumentType.getInteger(ctx, "page"), GameProfileArgument.getGameProfiles(ctx, "player"));
                                                    return 1;
                                                })
                                        )
                                )
                                .executes(ctx -> {
                                    listFrom(ctx, 1, GameProfileArgument.getGameProfiles(ctx, "player"));
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("claim")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                        int id = IntegerArgumentType.getInteger(ctx, "id");
                                        ReportData data = Database.getReport(id);
                                        if (data == null) {
                                            ctx.getSource().sendFailure(Component.literal("No such report.").withStyle(ChatFormatting.RED));
                                            return 1;
                                        }
                                        if (data.status() != ReportStatus.UNRESOLVED) {
                                            ctx.getSource().sendFailure(Component.literal("This report can't be claimed (" + data.status() + ").").withStyle(ChatFormatting.RED));
                                            return 1;
                                        }
                                        String uuid;
                                        if (ctx.getSource().isPlayer()) {
                                            uuid = Objects.requireNonNull(ctx.getSource().getPlayer()).getStringUUID();
                                        } else {
                                            throw new IllegalStateException();
                                        }
                                        if (Database.updateReportStatus(id, ReportStatus.CLAIMED, UUID.fromString(uuid))) {
                                            ctx.getSource().sendSuccess(() -> Component.literal("Claimed report #" + id), true);
                                        } else {
                                            ctx.getSource().sendFailure(Component.literal("Something went wrong with the database, try again.").withStyle(ChatFormatting.RED));
                                        }

                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("unclaim")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    int id = IntegerArgumentType.getInteger(ctx, "id");
                                    ReportData reportData = Database.getReport(id);
                                    if (reportData == null) {
                                        ctx.getSource().sendFailure(LangManager.parse(TrKeys.REPORTS_NOT_FOUND, Map.of("id", String.valueOf(id))));
                                        return 1;
                                    }
                                    if (!(reportData.status() == ReportStatus.CLAIMED)) {
                                        ctx.getSource().sendFailure(LangManager.parse(TrKeys.REPORTS_UNCLAIM_NOT_CLAIMED));
                                        return 1;
                                    }
                                    Database.updateReportStatus(id, ReportStatus.UNRESOLVED, null);
                                    ctx.getSource().sendSuccess(() -> LangManager.parse(TrKeys.REPORTS_UNCLAIM_SUCCESS, Map.of("id", String.valueOf(id))), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    int id = IntegerArgumentType.getInteger(ctx, "id");
                                    ReportData data = Database.getReport(id);
                                    if (data == null) {
                                        ctx.getSource().sendFailure(LangManager.parse(TrKeys.REPORTS_NOT_FOUND, Map.of("id", String.valueOf(id))));
                                        return 1;
                                    }
                                    ctx.getSource().sendSuccess(() -> reportInfo(data), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("complete")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .then(Commands.literal("NO_ACTION_TAKEN")
                                    .executes(ctx -> {
                                        complete(ctx, ReportStatus.NO_ACTION_TAKEN);
                                        return 1;
                                    })
                                )
                                .then(Commands.literal("SANCTION_ISSUED")
                                        .executes(ctx -> {
                                            complete(ctx, ReportStatus.SANCTION_ISSUED);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }

    private static void listFrom(CommandContext<CommandSourceStack> ctx, int page, Collection<NameAndId> idCollection) {
        NameAndId profile = idCollection.stream().findFirst().orElse(null);
        if (profile == null) {
            ctx.getSource().sendFailure(LangManager.parse(TrKeys.PLAYER_NOT_FOUND));
            return;
        }
        List<ReportData> data = Database.listReportsOf(profile.id());
        MutableComponent output = Component.empty();
        output.append(LangManager.format("<gold>Reports by <aqua>" + profile.name() + "<gold>: \n"));
        output.append(listReports(data, page, "reports from " + profile.name()));
        ctx.getSource().sendSuccess(() -> output, false);
    }

    private static void list(CommandContext<CommandSourceStack> ctx, int page) {
        List<ReportData> data = Database.listReports().stream().filter(report -> report.status() == ReportStatus.UNRESOLVED).toList();
        MutableComponent output = Component.empty();
        output.append(LangManager.format("<gold>Reports: \n"));
        output.append(listReports(data, page, "reports list"));
        ctx.getSource().sendSuccess(() -> output, false);
    }

    private static void list(CommandContext<CommandSourceStack> ctx, int page, Collection<NameAndId> idCollection) {
        MutableComponent output = Component.empty();
        NameAndId profile = idCollection.stream().findFirst().orElse(null);
        if (profile == null) {
            ctx.getSource().sendFailure(LangManager.parse(TrKeys.PLAYER_NOT_FOUND));
            return;
        }
        List<ReportData> data = Database.listReportsOf(profile.id());
        output.append(LangManager.format("<gold>Reports for %aqua%" + profile.name() + "<gold>: \n"));
        output.append(listReports(data, page, "reports list player " + profile.name()));
        ctx.getSource().sendSuccess(() -> output, false);
    }

    private static void complete(CommandContext<CommandSourceStack> ctx, ReportStatus status) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        ReportData reportData = Database.getReport(id);
        if (reportData == null) {
            ctx.getSource().sendFailure(LangManager.parse(TrKeys.REPORTS_NOT_FOUND, Map.of("id", String.valueOf(id))));
            return;
        }
        if ((reportData.status() == ReportStatus.SANCTION_ISSUED || reportData.status() == ReportStatus.NO_ACTION_TAKEN)) {
            ctx.getSource().sendFailure(LangManager.parse(TrKeys.REPORTS_COMPLETED_FAILED));
            return;
        }
        Database.updateReportStatus(id, status, null);
        ctx.getSource().sendSuccess(() -> LangManager.parse(TrKeys.REPORTS_COMPLETED_SUCCESS, Map.of("id", String.valueOf(id), "status", status.name())), true);
    }

    private static Component listReports(List<ReportData> reports, int page, String cmd) {
        assert page >= 1;
        int totalPages = Math.max(1, (reports.size() + reportsPerPage - 1) / reportsPerPage);

        page = Math.clamp(page, 1, totalPages);

        int index_min = (page - 1) * reportsPerPage;
        int index_max = Math.min(index_min + reportsPerPage, reports.size());

        MutableComponent output = Component.empty();
        for (int i = index_min; i < index_max; i++) {
            ReportData report = reports.get(i);
            Style style = Style.EMPTY;
            style.withClickEvent(new ClickEvent.RunCommand("reports info " + report.id()));
            MutableComponent rComponent = Component.empty().withStyle();
            rComponent.append(LangManager.format(" <dark_gray>* ["));
            rComponent.append(Component.literal(report.status().name()).withStyle(getStatusColor(report.status())));
            rComponent.append(LangManager.format("<dark_gray>] <gold>" + report.sourceUUID() + "<dark_red> reported <gold>" + report.targetUUID() + "<dark_red>: <yellow>" + report.reason() + "\n"));
            output.append(rComponent);
        }
        Style style1 = Style.EMPTY;
        style1.withClickEvent(new ClickEvent.RunCommand(cmd + " page " + (page - 1)));
        output.append(Component.literal(" < ").withStyle(ChatFormatting.GOLD).withStyle(style1));
        output.append(Component.literal(page + "/" + totalPages).withStyle(ChatFormatting.AQUA));
        Style style2 = Style.EMPTY;
        style2.withClickEvent(new ClickEvent.RunCommand(cmd + " page " + (page + 1)));
        output.append(Component.literal(" > ").withStyle(ChatFormatting.GOLD).withStyle(style2));
        return output;
    }

    /*
    private static void list(CommandContext<CommandSourceStack> ctx, @Nullable Collection<NameAndId> profiles, int page) {
        assert page >= 1;
        List<ReportData> data;
        Optional<NameAndId> oprofile;
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
            output.append(reportInfo(report));
        }
        output.append(Component.literal("\nPage " + page + " on " + totalPages));

        ctx.getSource().sendSuccess(() -> output, false);
    }*/

    private static Component reportInfo(ReportData report) {
        MutableComponent output = Component.empty();
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

        return output;
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