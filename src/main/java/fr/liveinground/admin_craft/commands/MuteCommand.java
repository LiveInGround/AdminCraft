package fr.liveinground.admin_craft.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.mutes.PlayerMuteData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.Collection;
import java.util.Map;

public class MuteCommand {

    private MuteCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.hasPermission(permissionLevel))
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                            if (AdminCraft.mutedPlayersUUID.contains(playerToMute.getStringUUID())) {
                                ctx.getSource().sendFailure(Component.literal(Config.mute_failed_already_muted));
                                return 1;
                            }
                            AdminCraft.playerDataManager.addMuteEntry(new PlayerMuteData(playerToMute.getName().toString(), playerToMute.getStringUUID(), "Muted by an operator"));
                            // ...
                            String msg = Config.mute_message;
                            playerToMute.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));

                            String msgToOperator = PlaceHolderSystem.replacePlaceholders(Config.mute_success, Map.of("player", playerToMute.getName().getString(), "reason", reason));
                            ctx.getSource().sendSuccess(() -> Component.literal(msgToOperator), true);

                            return 1;
                        }).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                            if (AdminCraft.mutedPlayersUUID.contains(playerToMute.getStringUUID())) {
                                ctx.getSource().sendFailure(Component.literal(Config.mute_failed_already_muted));
                                return 1;
                            }
                            String reason = StringArgumentType.getString(ctx, "reason");
                            AdminCraft.playerDataManager.addMuteEntry(new PlayerMuteData(playerToMute.getName().toString(), playerToMute.getStringUUID(), reason));

                            String msg = PlaceHolderSystem.replacePlaceholders(Config.mute_message, Map.of("reason", reason));
                            playerToMute.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));

                            String msgToOperator = PlaceHolderSystem.replacePlaceholders(Config.mute_success, Map.of("player", playerToMute.getName().getString(), "reason", reason));
                            ctx.getSource().sendSuccess(() -> Component.literal(msgToOperator), true);
                            return 1;
                        }
                        ))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> source.hasPermission(permissionLevel))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {

                            // todo: get config messages

                            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                            if (!profiles.isEmpty()) {

                                GameProfile targetProfile = profiles.iterator().next();
                                ServerPlayer playerToUnmute = ctx.getSource().getServer().getPlayerList().getPlayer(targetProfile.getId());


                                if (!AdminCraft.mutedPlayersUUID.contains(playerToUnmute.getStringUUID())) {
                                    Component messageToOperator = AdminCraft.parseComponent(ctx.getSource().getPlayerOrException().level(), muteMessages.unmuteFailedNotMuted().get());
                                    ctx.getSource().sendFailure(messageToOperator);
                                    return 1;
                                }
                                AdminCraft.playerDataManager.removeEntry(AdminCraft.playerDataManager.getPlayerDataByUUID(playerToUnmute.getStringUUID()));


                                Component messageComponent = AdminCraft.parseComponent(playerToUnmute.level(), muteMessages.muteEnds().get());
                                playerToUnmute.sendSystemMessage(messageComponent);

                                Component messageToOperator = AdminCraft.parseComponent(ctx.getSource().getLevel(), String.format(muteMessages.unmuteSuccess().get(), playerToUnmute.getName().getString()));
                                ctx.getSource().sendSuccess(() -> messageToOperator, true);
                                return 1;
                            } else {
                                ctx.getSource().sendFailure(Component.literal("No player with this username was found."));
                                return 1;
                            }
                        })
                ));
    }
}