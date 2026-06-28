package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import java.util.Date;


public class TempBanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tempban")
                .requires(commandSource -> commandSource.permissions().hasPermission(Config.tempban_level))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .executes(ctx -> {
                                            tempban(
                                                    ctx,
                                                    AdminCraft.getOneProfile(GameProfileArgument.getGameProfiles(ctx, "player")),
                                                    StringArgumentType.getString(ctx, "duration"),
                                                    "The ban hammer has spoken!"
                                            );
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
                                                    tempban(
                                                            ctx,
                                                            AdminCraft.getOneProfile(GameProfileArgument.getGameProfiles(ctx, "player")),
                                                            StringArgumentType.getString(ctx, "duration"),
                                                            StringArgumentType.getString(ctx, "args")
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }

    private static void tempban(CommandContext<CommandSourceStack> ctx, NameAndId player, String duration, String args) {
        Date sanctionDuration = SanctionConfig.getDurationAsDate(duration);
        if (sanctionDuration == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid format for the sanction duration.").withStyle(ChatFormatting.RED));
            return;
        }

        String reason;
        boolean appealable = Config.default_can_appeal;
        Date appealDelay = SanctionConfig.getDurationAsDate(Config.default_appeal_delay);
        String[] splitted = args.split(" ");
        StringBuilder builder = new StringBuilder();

        for (String i: splitted) {
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
        }
        else {
            reason = "The Ban Hammer has spoken!";
        }
        CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().getTextName(), player, reason, sanctionDuration, appealable, appealDelay);
        ctx.getSource().sendSuccess(() -> Component.literal("Temporarily banned " + player.name() + ": " + reason), true);
    }
}
