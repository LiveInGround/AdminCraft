package fr.liveinground.admin_craft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ChatCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chat")
                .requires(source -> Config.enableChatChannels && source.hasPermission(Config.chatChannelLevel))
                .then(Commands.argument("channel", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            //todo: enhance later
                            builder.suggest("global");
                            builder.suggest("team");
                            builder.suggest("local");
                            if (ctx.getSource().hasPermission(1)) {
                                builder.suggest("staff");
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            //todo
                            return 1;
                        })
                )
                .executes(ctx -> {
                    //todo
                    return 1;
                })
        );
    }
}