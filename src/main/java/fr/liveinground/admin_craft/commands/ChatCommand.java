package fr.liveinground.admin_craft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.chat.ChatChannel;
import fr.liveinground.admin_craft.storage.types.chat.ChatPlayerPreference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Map;
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
                            String channelStr = StringArgumentType.getString(ctx, "channel").toUpperCase();
                            try {
                                channel = ChatChannel.valueOf(channelStr);
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendFailure(LangManager.parse(TrKeys.CHAT_INVALID_CHANNEL, Map.of("channel", channelStr)));
                                return 1;
                            }
                            if ((channel.equals(ChatChannel.STAFF) && !(ctx.getSource().hasPermission(Config.staffChatLevel) && Config.enableStaffChat))
                                    || channel.equals(ChatChannel.LOCAL) && !Config.enableLocalChat) {
                                //deny access if the correct perm is missing or if the chat is disabled
                                ctx.getSource().sendFailure(LangManager.parse(TrKeys.CHAT_NO_PERM));
                                return 1;
                            }
                            Database.updateChatPreference(uuid, channel);
                            ctx.getSource().sendSuccess(() -> LangManager.parse(TrKeys.CHAT_SWITCHED_CHANNEL, Map.of("channel", channelStr)), false);
                            return 1;
                        })
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String channelStr = StringArgumentType.getString(ctx, "channel");
                                    ChatChannel channel;
                                    try {
                                        channel = ChatChannel.valueOf(channelStr);
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendFailure(LangManager.parse(TrKeys.CHAT_INVALID_CHANNEL, Map.of("channel", channelStr)));
                                        return 1;
                                    }
                                    if ((channel.equals(ChatChannel.STAFF) && !(ctx.getSource().hasPermission(Config.staffChatLevel) && Config.enableStaffChat))
                                    || channel.equals(ChatChannel.LOCAL) && !Config.enableLocalChat) {
                                        //deny access if the correct perm is missing or if the chat is disabled
                                        ctx.getSource().sendFailure(LangManager.parse(TrKeys.CHAT_NO_PERM));
                                        return 1;
                                    }

                                    CommandSourceStack source = ctx.getSource();
                                    Vec3 pos;
                                    PlayerTeam team;
                                    if (ctx.getSource().isPlayer()) {
                                        pos = Objects.requireNonNull(source.getPlayer()).position();
                                        team = source.getPlayer().getTeam();
                                    } else {
                                        pos = new Vec3(0, 0, 0);
                                        team = null;
                                    }
                                    AdminCraft.sendMessageInChannel(ctx.getSource().getTextName(), StringArgumentType.getString(ctx, "message"), channel, pos, team);
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
                    ctx.getSource().sendSuccess(() -> LangManager.parse(TrKeys.CHAT_CURRENT, Map.of("channel", pref.channel().name())), false);
                    return 1;
                })
        );
    }
}