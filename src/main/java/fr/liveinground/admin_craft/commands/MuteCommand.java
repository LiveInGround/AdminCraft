package fr.liveinground.admin_craft.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.hasPermission(Config.mute_level))
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                            if (AdminCraft.mutedPlayersUUID.contains(playerToMute.getStringUUID())) {
                                ctx.getSource().sendFailure(Component.literal(Config.mute_failed_already_muted));
                                return 1;
                            }

                            CustomSanctionSystem.mutePlayer(playerToMute, "Muted by an operator.", null);

                            String msgToOperator = PlaceHolderSystem.replacePlaceholders(Config.mute_success, Map.of("player", playerToMute.getName().getString()));
                            ctx.getSource().sendSuccess(() -> Component.literal(msgToOperator), true);

                            return 1;
                        }).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer playerToMute = EntityArgument.getPlayer(ctx, "player");
                            if (AdminCraft.mutedPlayersUUID.contains(playerToMute.getStringUUID())) {
                                ctx.getSource().sendFailure(Component.literal(Config.mute_failed_already_muted));
                                return 1;
                            }
                            String reason = StringArgumentType.getString(ctx, "reason");

                            CustomSanctionSystem.mutePlayer(playerToMute, reason, null);

                            String msgToOperator = PlaceHolderSystem.replacePlaceholders(Config.mute_success, Map.of("player", playerToMute.getName().getString(), "reason", reason));
                            ctx.getSource().sendSuccess(() -> Component.literal(msgToOperator), true);
                            return 1;
                        }
                        ))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> source.hasPermission(Config.mute_level))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                            if (!profiles.isEmpty()) {

                                GameProfile targetProfile = profiles.iterator().next();
                                ServerPlayer playerToUnmute = ctx.getSource().getServer().getPlayerList().getPlayer(targetProfile.getId());

                                if (playerToUnmute == null) {
                                    ctx.getSource().sendFailure(Component.literal("No player with this username was found."));
                                    return 1;
                                }

                                if (!AdminCraft.mutedPlayersUUID.contains(playerToUnmute.getStringUUID())) {
                                    String msg = PlaceHolderSystem.replacePlaceholders(Config.unmute_failed_not_muted, Map.of("player", playerToUnmute.getName().getString()));
                                    Component messageToOperator = Component.literal(msg);
                                    ctx.getSource().sendFailure(messageToOperator);
                                    return 1;
                                }

                                CustomSanctionSystem.unMutePlayer(playerToUnmute);

                                String msg = PlaceHolderSystem.replacePlaceholders(Config.unmute_success, Map.of("player", playerToUnmute.getName().getString()));
                                Component messageToOperator = Component.literal(msg);
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