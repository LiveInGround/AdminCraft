package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

import java.util.Collection;
import java.util.Date;

public class BanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("admincraft:ban")
                .requires(source -> source.permissions().hasPermission(Config.ban_level))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            ban(ctx, "Banned by an operator");
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
                                    ban(ctx, StringArgumentType.getString(ctx, "args"));
                                    return 1;
                                })
                        )
                )
        );
    }

    private static void ban(CommandContext<CommandSourceStack> ctx, String args) throws CommandSyntaxException {
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
            reason = "Banned by an operator";
        }

        Collection<NameAndId> collection = GameProfileArgument.getGameProfiles(ctx, "player");
        NameAndId target = AdminCraft.getOneProfile(collection);
        if (target == null) {
            ctx.getSource().sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return;
        }
        if (ctx.getSource().getServer().getPlayerList().getBans().isBanned(target)) {
            ctx.getSource().sendFailure(Component.literal("This player is already banned.").withStyle(ChatFormatting.RED));
            return;
        }
        CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().getTextName(), target, reason, null, appealable, appealDelay);
        ctx.getSource().sendSuccess(() -> Component.literal("Banned " + target.name() + ": " + reason), true);
    }
}
