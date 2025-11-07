package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;


public class TempBanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tempban")
                        .requires(commandSource -> commandSource.hasPermission(Config.tempban_level))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("duration", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                                                    String reason = "Banned by an operator";
                                                    Date duration = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "duration"));
                                                    if (duration == null) {
                                                        ctx.getSource().sendFailure(Component.literal("Invalid duration, expecting format 1d1h1m1s"));
                                                        return 1;
                                                    }
                                                    tempban(ctx, sanctionedPlayer, duration, reason);
                                                    return 1;
                                                })
                                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                                                            String reason = StringArgumentType.getString(ctx, "reason");
                                                            Date duration = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "duration"));
                                                            if (duration == null) {
                                                                ctx.getSource().sendFailure(Component.literal("Invalid duration, expecting format 1d1h1m1s"));
                                                                return 1;
                                                            }
                                                            tempban(ctx, sanctionedPlayer, duration, reason);
                                                            return 1;
                                                        })))
                                )
        );
    }

    private static void tempban(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Date duration, String reason) {
        CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().getTextName(), player, reason, duration);
        ctx.getSource().sendSuccess(() -> Component.literal("Banned " + player.getDisplayName().getString() + " " + duration + ": " + reason), true);
    }
}
