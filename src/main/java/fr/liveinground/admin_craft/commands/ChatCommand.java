package fr.liveinground.admin_craft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.chat.ChatChannel;
import fr.liveinground.admin_craft.storage.types.chat.ChatPlayerPreference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.UUID;

public class ChatCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chat")
                .requires(source -> Config.enableChatChannels && source.hasPermission(Config.chatChannelLevel))
                .then(Commands.argument("channel", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("global");
                            builder.suggest("team");
                            if (Config.enableLocalChat) {
                                builder.suggest("local");
                            }
                            if (Config.enableStaffChat && ctx.getSource().hasPermission(Config.staffChatLevel)) {
                                builder.suggest("staff");
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            UUID uuid;
                            if (source.isPlayer()) {
                                uuid = Objects.requireNonNull(source.getPlayer()).getUUID();
                            } else {
                                // console/function/command blocks uuid
                                uuid = new UUID(0L, 0L);
                            }
                            ChatChannel channel;
                            String channelStr = StringArgumentType.getString(ctx, "channel");
                            try {
                                channel = ChatChannel.valueOf(channelStr);
                            } catch (IllegalArgumentException e) {
                                //todo: create translation string to tell the user that his command failed miserably >:)
                                ctx.getSource().sendFailure(Component.empty());
                                return 1;
                            }
                            Database.updateChatPreference(uuid, channel);
                            //todo: send success message
                            return 1;
                        })
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String channelStr = StringArgumentType.getString(ctx, "channel");
                                    ChatChannel channel;
                                    try {
                                        channel = ChatChannel.valueOf(channelStr);
                                    } catch (IllegalArgumentException e) {
                                        //todo: create translation string to tell the user that his command failed miserably >:)
                                        ctx.getSource().sendFailure(Component.empty());
                                        return 1;
                                    }

                                    AdminCraft.sendMessageInChannel(ctx.getSource().getTextName(), StringArgumentType.getString(ctx, "message"), channel);
                                    return 1;
                                })
                        )
                )
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    UUID uuid;
                    if (source.isPlayer()) {
                        uuid = Objects.requireNonNull(source.getPlayer()).getUUID();
                    } else {
                        // console/function/command blocks uuid
                        uuid = new UUID(0L, 0L);
                    }
                    ChatPlayerPreference pref = Database.getPlayerCurrentChannel(uuid);

                    //todo: display selected channel
                    //todo: create translation string for the message
                    return 1;
                })
        );
    }
}