package fr.liveinground.admin_craft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.moderation.Sanction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import org.codehaus.plexus.util.cli.Commandline;

public class SanctionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("alts")
                .requires(commandSource -> commandSource.hasPermission(Config.alt_level))
                .then(Commands.argument("player", EntityArgument.player()))
                .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                    // todo: get reason

                    // todo: get sanction


                    return 1;
                })
            ));
    }
}