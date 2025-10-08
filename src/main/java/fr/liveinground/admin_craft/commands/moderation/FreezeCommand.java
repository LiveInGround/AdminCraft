package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FreezeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("freeze")
                .requires(commandSource -> commandSource.hasPermission(Config.freezeLevel))
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                    if (AdminCraft.frozenPlayersUUID.contains(player.getStringUUID())) {
                        return unfreeze(ctx, player);
                    } else {
                        return freeze(ctx, player);
                    }
                })));
    }

    private static int freeze(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        AdminCraft.frozenPlayersUUID.add(player.getStringUUID());
        // todo: send messages

        ctx.getSource().sendSuccess(() -> Component.literal(player.getDisplayName().getString() + " was frozen"), true);
        return 1;
    }

    private static int unfreeze(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        AdminCraft.frozenPlayersUUID.remove(player.getStringUUID());

        // todo: send messages

        ctx.getSource().sendSuccess(() -> Component.literal(player.getDisplayName().getString() + " was unfrozen"), true);

        return 1;
    }
}
