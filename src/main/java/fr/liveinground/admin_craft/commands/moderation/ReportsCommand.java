package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.liveinground.admin_craft.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;

public class ReportsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reports")
                .requires(commandSource -> commandSource.hasPermission(Config.reports_level))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                        .executes(ctx -> {
                            //todo
                            return 1;
                        })
                )
                .then(Commands.literal("of")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("claim")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("unclaim")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    //todo
                                    return 1;
                                })
                        )
                )
        );
    }
}