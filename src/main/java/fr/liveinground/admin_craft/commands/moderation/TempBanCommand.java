package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;


public class TempBanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tempban")
                        .requires(commandSource -> commandSource.hasPermission(Config.tempban_level))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.argument("duration", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    GameProfile profile = AdminCraft.getOneProfile(GameProfileArgument.getGameProfiles(ctx, "player"));
                                                    if (profile == null) {
                                                        ctx.getSource().sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
                                                        return 1;
                                                    }
                                                    String reason = "Banned by an operator";
                                                    Date duration = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "duration"));

                                                    if (duration == null) {
                                                        ctx.getSource().sendFailure(Component.literal("Invalid duration, expecting format 1d1h1m1s"));
                                                        return 1;
                                                    }
                                                    tempban(ctx, profile, duration, reason);
                                                    return 1;

                                                })
                                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            GameProfile profile = AdminCraft.getOneProfile(GameProfileArgument.getGameProfiles(ctx, "player"));
                                                            if (profile == null) {
                                                                ctx.getSource().sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
                                                                return 1;
                                                            }

                                                            String reason = StringArgumentType.getString(ctx, "reason");
                                                            Date duration = SanctionConfig.getDurationAsDate(StringArgumentType.getString(ctx, "duration"));

                                                            if (duration == null) {
                                                                ctx.getSource().sendFailure(Component.literal("Invalid duration, expecting format 1d1h1m1s"));
                                                                return 1;
                                                            }
                                                            tempban(ctx, profile, duration, reason);
                                                            return 1;
                                                        })))
                                )
        );
    }

    private static void tempban(CommandContext<CommandSourceStack> ctx, GameProfile player, Date duration, String reason) {
        CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().getTextName(), player, reason, duration);
        ctx.getSource().sendSuccess(() -> Component.literal("Banned " + player.getName() + " " + duration + ": " + reason), true);
    }
}
