package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WarnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("warn")
                .requires(commandSource -> commandSource.hasPermission(Config.warn_level))
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                    CustomSanctionSystem.warnPlayer(sanctionedPlayer, null, ctx.getSource().getDisplayName().getString());

                    ctx.getSource().sendSuccess(() -> Component.literal(sanctionedPlayer.getDisplayName().getString() + " was warned"), true);
                    return 1;
                }).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                            String reason = StringArgumentType.getString(ctx, "reason");
                            ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                            CustomSanctionSystem.warnPlayer(sanctionedPlayer, reason, ctx.getSource().getDisplayName().getString());

                            ctx.getSource().sendSuccess(() -> Component.literal(sanctionedPlayer.getDisplayName().getString() +  " was warned: " + reason), true);
                            return 1;
                        }
                ))));
    }
}
