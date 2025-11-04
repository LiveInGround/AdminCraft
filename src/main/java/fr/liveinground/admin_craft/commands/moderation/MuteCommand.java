package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class MuteCommand {

    private MuteCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("mute")
                .requires(commandSource -> commandSource.hasPermission(Config.mute_level))
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                           mute(ctx, null, null);
                           return 1;
                        })).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                            String reason = StringArgumentType.getString(ctx, "reason");
                            mute(ctx, reason, null);
                            return 1;
                        }
                        )));

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

        dispatcher.register(Commands.literal("tempmute")
                .requires(commandSource -> commandSource.hasPermission(Config.mute_level))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("duration", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            mute(ctx, null, StringArgumentType.getString(ctx, "duration"));
                            return 1;
                }).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx -> {
                            String reason = StringArgumentType.getString(ctx, "reason");
                            mute(ctx, reason, StringArgumentType.getString(ctx, "duration"));
                            return 1;
                        }
                )))));
    }

    private static void mute(@NotNull CommandContext<CommandSourceStack> ctx, @Nullable String reason, @Nullable String duration) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        if (reason == null) {
            reason = "Muted by an operator.";
        }
        if (AdminCraft.mutedPlayersUUID.contains(player.getStringUUID())) {
            ctx.getSource().sendFailure(Component.literal(Config.mute_failed_already_muted));
            return;
        }
        Date duration_f;
        if (duration != null)
            duration_f = SanctionConfig.getDurationAsDate(duration);
        else
            duration_f = null;
        CustomSanctionSystem.mutePlayer(player, reason, duration_f);

        String msgToOperator = PlaceHolderSystem.replacePlaceholders(Config.mute_success, Map.of("player", player.getName().getString(), "reason", reason));
        ctx.getSource().sendSuccess(() -> Component.literal(msgToOperator), true);
    }
}