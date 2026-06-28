package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class MuteCommand {

    private MuteCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.permissions().hasPermission(Config.mute_level))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            mute(ctx, "Muted by an operator", null);
                            return 1;
                        })
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining();
                                    builder.suggest("<reason>");
                                    if (!remaining.contains("--noappeal")) {
                                        builder.suggest("--noappeal");
                                    }
                                    if (!remaining.contains("--delayedappeal:")) {
                                        builder.suggest("--delayedappeal:");
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    mute(ctx, StringArgumentType.getString(ctx, "args"), null);
                                    return 1;
                                }))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> source.permissions().hasPermission(Config.mute_level))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                            if (!profiles.isEmpty()) {

                                NameAndId targetProfile = profiles.iterator().next();

                                if (!AdminCraft.mutedPlayersUUID.contains(targetProfile.id().toString())) {
                                    String msg = LangManager.tr(TrKeys.COMMAND_UNMUTE_FAILED_NOT_MUTED, Map.of("player", targetProfile.name()));
                                    Component messageToOperator = Component.literal(msg);
                                    ctx.getSource().sendFailure(messageToOperator);
                                    return 1;
                                }

                                CustomSanctionSystem.unMutePlayer(targetProfile);

                                String msg = LangManager.tr(TrKeys.COMMAND_UNMUTE_SUCCESS, Map.of("player", targetProfile.name()));
                                Component messageToOperator = Component.literal(msg);
                                ctx.getSource().sendSuccess(() -> messageToOperator, true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("No player with this username was found."));
                            }
                            return 1;
                        })
                ));

        dispatcher.register(Commands.literal("tempmute")
                .requires(commandSource -> commandSource.permissions().hasPermission(Config.mute_level))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String duration = StringArgumentType.getString(ctx, "duration");
                                            mute(ctx, "Muted by an operator", duration);
                                            return 1;
                                        })
                                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> {
                                                    String remaining = builder.getRemaining();
                                                    builder.suggest("<reason>");
                                                    if (!remaining.contains("--noappeal")) {
                                                        builder.suggest("--noappeal");
                                                    }
                                                    if (!remaining.contains("--delayedappeal:")) {
                                                        builder.suggest("--delayedappeal:");
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String duration = StringArgumentType.getString(ctx, "duration");
                                                    mute(ctx, StringArgumentType.getString(ctx, "args"), duration);
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }

    private static void mute(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull String args, @Nullable String duration) throws CommandSyntaxException {
        NameAndId profile = AdminCraft.getOneProfile(GameProfileArgument.getGameProfiles(ctx, "player"));
        if (profile == null) {
            ctx.getSource().sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return;
        }
        if (AdminCraft.mutedPlayersUUID.contains(String.valueOf(profile.id()))) {
            ctx.getSource().sendFailure(Component.literal(LangManager.tr(TrKeys.COMMAND_MUTE_FAIL_ALREADY_MUTED, Map.of("player", profile.name()))));
            return;
        }
        Date sanctionDuration = SanctionConfig.getDurationAsDate(duration);
        if (sanctionDuration == null && duration != null) {
            ctx.getSource().sendFailure(Component.literal("Invalid format for the sanction duration.").withStyle(ChatFormatting.RED));
            return;
        }

        String reason;
        boolean appealable = Config.default_can_appeal;
        Date appealDelay = SanctionConfig.getDurationAsDate(Config.default_appeal_delay);
        String[] splitted = args.split(" ");
        StringBuilder builder = new StringBuilder();

        for (String i : splitted) {
            if (i.equals("--noappeal")) appealable = false;
            else if (i.contains("--delayedappeal:")) {
                String[] delaysplit = i.split(":");
                if (delaysplit.length == 2) {
                    appealDelay = SanctionConfig.getDurationAsDate(delaysplit[1]);
                    if (appealDelay == null) {
                        ctx.getSource().sendFailure(Component.literal("Invalid duration format for the appeal delay.").withStyle(ChatFormatting.RED));
                        return;
                    }
                }
            } else builder.append(" ").append(i);
        }
        if (!builder.isEmpty()) {
            builder.delete(0, 0);
            reason = builder.toString();
        } else {
            reason = "Muted by an operator";
        }
        if (CustomSanctionSystem.mutePlayer(ctx.getSource().getServer(), profile, reason, sanctionDuration, appealable, appealDelay) == null) {
            ctx.getSource().sendFailure(Component.literal("Something went wrong.").withStyle(ChatFormatting.RED));
            return;
        }

        String msgToOperator = LangManager.tr(TrKeys.COMMAND_MUTE_SUCCESS, Map.of("player", profile.name(), "reason", reason));
        ctx.getSource().sendSuccess(() -> Component.literal(msgToOperator), true);
    }
}