package fr.liveinground.admin_craft.commands.moderation.edit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.storage.SanctionDatabase;
import fr.liveinground.admin_craft.storage.types.sanction.AppealStatus;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class EditAppealCommand {
    private static final Map<String, AppealStatus> statusMap = Map.of(
            "NOT_ALLOWED", AppealStatus.NOT_ALLOWED,
            "DELAYED", AppealStatus.DELAYED,
            "NOT_REQUESTED", AppealStatus.NOT_REQUESTED,
            "IN_PROGRESS", AppealStatus.IN_PROGRESS,
            "REFUSED", AppealStatus.REFUSED,
            "REDUCED", AppealStatus.REDUCED,
            "ACCEPTED", AppealStatus.ACCEPTED
    );

    private static final List<Sanction> appealCompatible = List.of(Sanction.BAN, Sanction.MUTE);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("editappeal")
                .requires(source -> source.permissions().hasPermission(Config.editappeal_level))
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            String remaining = builder.getRemaining();
                            for (String id: SanctionDatabase.listIDs()) {
                                if (!remaining.contains(id)) builder.suggest('"' + id + '"');
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("status")
                                .then(Commands.argument("status", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String remaining = builder.getRemaining();
                                            for (String status: statusMap.keySet()) {
                                                if (!remaining.contains(status)) builder.suggest(status);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            DatabaseSanctionData data = SanctionDatabase.getSanctionData(id);
                                            if (data == null) {
                                                ctx.getSource().sendFailure(Component.literal("No sanction was found with id " + id).withStyle(ChatFormatting.RED));
                                                return 1;
                                            }
                                            if (!appealCompatible.contains(data.type())) {
                                                ctx.getSource().sendFailure(Component.literal("Sanction type " + data.type() + " does not have an implemented appeal system. Please try again with another sanction.").withStyle(ChatFormatting.RED));
                                                return 1;
                                            }

                                            AppealStatus status = statusMap.get(StringArgumentType.getString(ctx, "status"));
                                            if (status == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown status, check your input and try again.").withStyle(ChatFormatting.RED));
                                                return 1;
                                            }

                                            if (SanctionDatabase.changeAppealStatus(id, status)) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("Successfully changed appeal status for sanction id " + id + " to " + status), true);
                                            } else {
                                                ctx.getSource().sendFailure(Component.literal("An unexpected issue occurred while updating the database.").withStyle(ChatFormatting.RED));
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("delay")
                                .then(Commands.argument("delay", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            DatabaseSanctionData data = SanctionDatabase.getSanctionData(id);
                                            if (data == null) {
                                                ctx.getSource().sendFailure(Component.literal("No sanction was found with id " + id).withStyle(ChatFormatting.RED));
                                                return 1;
                                            }
                                            if (!appealCompatible.contains(data.type())) {
                                                ctx.getSource().sendFailure(Component.literal("Sanction type " + data.type() + " does not have an implemented appeal system. Please try again with another sanction.").withStyle(ChatFormatting.RED));
                                                return 1;
                                            }

                                            Date newAppealDelay = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "delay"));
                                            if (newAppealDelay == null) {
                                                ctx.getSource().sendFailure(Component.literal("Argument delay is invalid, please enter a valid duration. To disable appeal for this sanction, please use /editappeal <id> status NOT_ALLOWED.").withStyle(ChatFormatting.RED));
                                                return 1;
                                            }
                                            if (SanctionDatabase.editAppealDelay(id, newAppealDelay)) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("Successfully changed the appeal delay of the sanction id " + id + ", appeal will be available in: " + newAppealDelay), true);
                                            } else {
                                                ctx.getSource().sendFailure(Component.literal("An unexpected error occurred.").withStyle(ChatFormatting.RED));
                                            }
                                            return  1;
                                        })
                                )
                        )
                )
        );
    }
}