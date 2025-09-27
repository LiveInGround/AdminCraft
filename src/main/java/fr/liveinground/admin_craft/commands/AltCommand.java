package fr.liveinground.admin_craft.commands;

import com.mojang.brigadier.CommandDispatcher;
import fr.liveinground.admin_craft.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public class AltCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.hasPermission(Config.alt_level))
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    return 1;
                }
                ))
        );
    }
}