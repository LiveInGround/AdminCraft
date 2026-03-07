package fr.liveinground.admin_craft.commands.moderation;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PermissionValue;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;


public class TempBanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tempban")
                        .requires(commandSource -> commandSource.permissions().hasPermission(PermissionValue.fromOld(Config.tempban_level).permission()))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.argument("duration", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                                                    String reason = "Banned by an operator";
                                                    Date duration = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "duration"));
                                                    if (duration == null) {
                                                        ctx.getSource().sendFailure(Component.literal("Invalid duration, expecting format 1d1h1m1s"));
                                                        return 1;
                                                    }
                                                    tempban(ctx, profiles, duration, reason);
                                                    return 1;
                                                })
                                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                                                            String reason = StringArgumentType.getString(ctx, "reason");
                                                            Date duration = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "duration"));
                                                            if (duration == null) {
                                                                ctx.getSource().sendFailure(Component.literal("Invalid duration, expecting format 1d1h1m1s"));
                                                                return 1;
                                                            }
                                                            tempban(ctx, profiles, duration, reason);
                                                            return 1;
                                                        })))
                                )
        );
    }

    private static void tempban(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Date duration, String reason) {
        CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().getTextName(), player, reason, duration);
        ctx.getSource().sendSuccess(() -> Component.literal("Temporarily banned " + player.getDisplayName().getString() + ": " + reason), true);
    }

    private static void tempban(CommandContext<CommandSourceStack> ctx, Collection<NameAndId> player, Date duration, String reason) {
        CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().getTextName(), player, reason, duration);
        Optional<NameAndId> p = player.stream().findFirst();
        if (p.isPresent()) {
            String p2 = p.get().name();
            ctx.getSource().sendSuccess(() -> Component.literal("Temporarily banned profile " + p2 + ": " + reason), true);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("Temporarily banned an unknown profile " + ": " + reason), true);
        }

    }
}
