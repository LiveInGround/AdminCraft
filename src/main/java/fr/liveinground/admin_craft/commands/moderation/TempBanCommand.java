package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


public class TempBanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("tempban")
                .requires(commandSource -> commandSource.hasPermission(Config.tempban_level))
                .then(Commands.argument("player", EntityArgument.player()))
                .then(Commands.argument("duration", StringArgumentType.word()))
                        .executes(ctx -> {
                            ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                            String reason = "Banned by an operator";
                            String duration = StringArgumentType.getString(ctx, "duration");
                            CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().getTextName(), sanctionedPlayer, reason, SanctionConfig.getDurationAsDate(duration));
                            ctx.getSource().sendSuccess(() -> Component.literal("Banned " + sanctionedPlayer.getDisplayName().getString() + " " + duration + ": " + reason), true);
                            return 1;
                        })
                .then(Commands.argument("reason", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                    String reason = StringArgumentType.getString(ctx, "reason");
                    String duration = StringArgumentType.getString(ctx, "duration");
                    CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().getTextName(), sanctionedPlayer, reason, SanctionConfig.getDurationAsDate(duration));
                    ctx.getSource().sendSuccess(() -> Component.literal("Banned " + sanctionedPlayer.getDisplayName().getString() + " " + duration + ": " + reason), true);
                    return 1;
                })));
    }
}
