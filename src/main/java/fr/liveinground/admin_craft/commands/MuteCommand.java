package fr.liveinground.admin_craft.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.mutes.PlayerMuteData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;

import java.awt.*;
import java.util.Collection;

public class MuteCommand {

    private MuteCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, int permissionLevel) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.hasPermission(permissionLevel))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {

                                    // todo: get Config message

                                    ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                                    if (AdminCraft.mutedPlayersUUID.contains(playerToMute.getStringUUID())) {
                                        Component messageToOperator = AdminCraft.parseComponent(ctx.getSource().getPlayerOrException().level(), muteMessages.muteFailedAlreadyMuted().get());
                                        ctx.getSource().sendFailure(messageToOperator);
                                        return 1;
                                    }
                                    String reason = StringArgumentType.getString(ctx, "reason");
                                    AdminCraft.playerDataManager.addEntry(new PlayerMuteData(playerToMute.getName().toString(), playerToMute.getStringUUID(), reason));

                                    Component messageComponent = AdminCraft.parseComponent(playerToMute.level(), String.format(muteMessages.muteStarts().get(), reason));
                                    playerToMute.sendSystemMessage(messageComponent);

                                    Component messageToOperator = AdminCraft.parseComponent(ctx.getSource().getLevel(), String.format(muteMessages.muteSuccess().get(), playerToMute.getName().getString()));
                                    ctx.getSource().sendSuccess(() -> messageToOperator, true);
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