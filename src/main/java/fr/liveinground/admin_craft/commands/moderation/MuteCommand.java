package fr.liveinground.admin_craft.commands.moderation;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PermissionValue;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class MuteCommand {

    private MuteCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.permissions().hasPermission(PermissionValue.fromOld(Config.mute_level).permission()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                           mute(ctx, null, null);
                           return 1;
                        })
                    .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                            String reason = StringArgumentType.getString(ctx, "reason");
                            mute(ctx, reason, null);
                            return 1;
                        }
                        ))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> source.permissions().hasPermission(PermissionValue.fromOld(Config.mute_level).permission()))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                            if (!profiles.isEmpty()) {

                                NameAndId targetProfile = profiles.iterator().next();
                                ServerPlayer playerToUnmute = ctx.getSource().getServer().getPlayerList().getPlayer(targetProfile.id());

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
                            } else {
                                ctx.getSource().sendFailure(Component.literal("No player with this username was found."));
                            }
                            return 1;
                        })
                ));

        dispatcher.register(Commands.literal("tempmute")
                .requires(commandSource -> commandSource.permissions().hasPermission(PermissionValue.fromOld(Config.mute_level).permission()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .executes(ctx -> {
                                            Date duration = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "duration"));
                                            if (duration == null) {
                                                ctx.getSource().sendFailure(Component.literal("Invalid duration, expecting format 1d1h1m1s"));
                                                return 1;
                                            }
                                            mute(ctx, null, duration);
                                            return 1;
                                        })
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    Date duration = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "duration"));
                                                    if (duration == null) {
                                                        ctx.getSource().sendFailure(Component.literal("Invalid duration, expecting format 1d1h1m1s"));
                                                        return 1;
                                                    }
                                                    mute(ctx, StringArgumentType.getString(ctx, "reason"), duration);
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }

    private static void mute(@NotNull CommandContext<CommandSourceStack> ctx, @Nullable String reason, @Nullable Date duration) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        if (reason == null) {
            reason = "Muted by an operator.";
        }
        if (AdminCraft.mutedPlayersUUID.contains(player.getStringUUID())) {
            ctx.getSource().sendFailure(Component.literal(PlaceHolderSystem.replacePlaceholders(Config.mute_failed_already_muted, Map.of("player", player.getName().getString()))));
            return;
        }
        CustomSanctionSystem.mutePlayer(player, reason, duration);

        String msgToOperator = PlaceHolderSystem.replacePlaceholders(Config.mute_success, Map.of("player", player.getName().getString(), "reason", reason));
        ctx.getSource().sendSuccess(() -> Component.literal(msgToOperator), true);
    }
}