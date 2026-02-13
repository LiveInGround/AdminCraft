package fr.liveinground.admin_craft.commands.tools;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.storage.nbt.PlayerDataLoader;
import fr.liveinground.admin_craft.storage.nbt.PlayerDataSaver;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

public class OfflineTeleportCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("otp")
                .requires(source -> source.hasPermission(Config.otp_level))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    teleportPlayerToPlayer(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "target"), EntityArgument.getPlayer(ctx, "player"));
                                    return 1;
                                })
                        )
                        .then(Commands.argument("destination", Vec3Argument.vec3())
                                .executes(ctx -> {
                                    teleportPlayerToCoordinates(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "target"), Vec3Argument.getVec3(ctx, "destination"));
                                    return 1;
                                })
                        )
                )
                .then(Commands.argument("destination", GameProfileArgument.gameProfile())
                        .requires(CommandSourceStack::isPlayer)
                        .executes(ctx -> {
                            teleportToPlayer(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "destination"));
                            return 1;
                        }))
        );
    }

    private static void teleportPlayerToPlayer(CommandSourceStack source, Collection<GameProfile> profiles, ServerPlayer destPlayer) {
        teleportPlayerToCoordinates(source, profiles, new Vec3(destPlayer.getX(), destPlayer.getY(), destPlayer.getZ()));
    }

    private static void teleportPlayerToCoordinates(CommandSourceStack source, Collection<GameProfile> profiles, Vec3 destination) {
        GameProfile profile = AdminCraft.getOneProfile(profiles);
        if (profile == null) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return;
        }
        ServerPlayer onlinePlayer = source.getServer().getPlayerList().getPlayer(profile.getId());
        if (onlinePlayer != null) {
            onlinePlayer.teleportTo(source.getLevel(), destination.x, destination.y, destination.z, 0, 0);
            source.sendSuccess(() ->
                            Component.literal(String.format(
                                    "Teleported %s to %.2f, %.2f, %.2f",
                                    onlinePlayer.getDisplayName().getString(),
                                    destination.x, destination.y, destination.z
                            )),
                    true);
        } else {
            try {
                PlayerDataSaver.setOfflineLocation(source.getLevel(), profile.getId(), destination);
            } catch (IOException e) {
                AdminCraft.LOGGER.error("Failed to save offline teleport location for {}", profile.getName(), e);
                source.sendFailure(Component.literal("Failure: something went wrong (IOException)").withStyle(ChatFormatting.RED));
            }
        }
    }

    private static void teleportToPlayer(CommandSourceStack source, Collection<GameProfile> profiles) {
        ServerPlayer sourcePlayer = source.getPlayer();

        GameProfile profile = AdminCraft.getOneProfile(profiles);
        if (profile == null) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return;
        }

        ServerPlayer player = source.getServer().getPlayerList().getPlayer(profile.getId());
        if (player != null) {
            sourcePlayer.teleportTo(Objects.requireNonNull(source.getServer().getLevel(player.level().dimension())), player.getX(), player.getY(), player.getZ(), 0, 0);
            source.sendSuccess(() -> Component.literal("Teleported to " + player.getDisplayName().getString()), true);
        } else {
            try {
                Vec3 destination = PlayerDataLoader.getOfflineLocation(source.getLevel(), profile.getId());
                if (destination == null) {
                    source.sendFailure(Component.literal("Failure: found no data").withStyle(ChatFormatting.RED));
                    return;
                }
                sourcePlayer.teleportTo(destination.x(), destination.y(), destination.z());
            } catch (IOException e) {
                AdminCraft.LOGGER.error("Failed to save offline teleport location for {}", profile.getName(), e);
                source.sendFailure(Component.literal("Failure: something went wrong (IOException)").withStyle(ChatFormatting.RED));
            }
        }

    }
}