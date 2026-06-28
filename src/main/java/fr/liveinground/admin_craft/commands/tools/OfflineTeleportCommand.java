package fr.liveinground.admin_craft.commands.tools;

import com.mojang.brigadier.CommandDispatcher;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.storage.nbt.PlayerDataLoader;
import fr.liveinground.admin_craft.storage.nbt.PlayerDataSaver;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static net.minecraft.commands.CommandSourceStack.ERROR_NOT_PLAYER;

public class OfflineTeleportCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("otp")
                .requires(source -> source.permissions().hasPermission(Config.otp_level))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            // /otp <target> (has to be run as a player)
                            if (!ctx.getSource().isPlayer()) throw ERROR_NOT_PLAYER.create();
                            teleportToPlayer(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "target"));
                            return 1;
                        })
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> {
                                    // /otp <target> <player> (can be run by the console)
                                    teleportPlayerToPlayer(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "target"), GameProfileArgument.getGameProfiles(ctx, "player"));
                                    return 1;
                                })
                        )
                        .then(Commands.argument("destination", Vec3Argument.vec3())
                                .executes(ctx -> {
                                    // /otp <target> <destination> (can be run by the console)
                                    teleportPlayerToCoordinates(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "target"), Vec3Argument.getVec3(ctx, "destination"));
                                    return 1;
                                }))
                )
        );
    }

    private static void teleportPlayerToPlayer(CommandSourceStack source, Collection<NameAndId> profiles, Collection<NameAndId> destPlayerProfiles) {
        NameAndId destPlayerProfile = AdminCraft.getOneProfile(destPlayerProfiles);
        if (destPlayerProfile == null) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return;
        }
        ServerPlayer destPlayer = AdminCraft.getOnlinePlayer(source.getServer(), destPlayerProfile);
        Vec3 dest;
        if (destPlayer != null) {
            dest = new Vec3(destPlayer.getX(), destPlayer.getY(), destPlayer.getZ());
        } else {
            try {
                dest = PlayerDataLoader.getOfflineLocation(source.getLevel(), destPlayerProfile.id());
            } catch (IOException e) {
                AdminCraft.LOGGER.error("Failed to load destination player's location: ", e);
                source.sendFailure(Component.literal("An issue occurred.").withStyle(ChatFormatting.RED));
                return;
            }
        }
        teleportPlayerToCoordinates(source, profiles, dest);
    }

    private static void teleportPlayerToCoordinates(CommandSourceStack source, Collection<NameAndId> profiles, Vec3 destination) {
        NameAndId profile = AdminCraft.getOneProfile(profiles);
        if (profile == null) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return;
        }
        ServerPlayer onlinePlayer = source.getServer().getPlayerList().getPlayer(profile.id());
        if (onlinePlayer != null) {
            Set<Relative> _ignored = Set.of();
            onlinePlayer.teleportTo(source.getLevel(), destination.x, destination.y, destination.z, _ignored, onlinePlayer.getYRot(), onlinePlayer.getXRot(), true);
        } else {
            try {
                PlayerDataSaver.setOfflineLocation(source.getLevel(), profile.id(), destination);
            } catch (IOException e) {
                AdminCraft.LOGGER.error("Failed to save offline teleport location for {}", profile.name(), e);
                source.sendFailure(Component.literal("Failure: something went wrong (IOException)").withStyle(ChatFormatting.RED));
                return;
            }
        }
        source.sendSuccess(() -> Component.literal(PlaceHolderSystem.replacePlaceholders("Teleported %player% to %x%, %y%, %z%",
                        Map.of("player", profile.name(),
                                "x", String.valueOf(destination.x),
                                "y", String.valueOf(destination.y),
                                "z", String.valueOf(destination.z)))),
                true);
    }

    private static void teleportToPlayer(CommandSourceStack source, Collection<NameAndId> profiles) {
        ServerPlayer sourcePlayer = source.getPlayer();

        NameAndId profile = AdminCraft.getOneProfile(profiles);
        if (profile == null) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return;
        }

        ServerPlayer player = AdminCraft.getOnlinePlayer(source.getServer(), profile);
        if (player != null) {
            Set<Relative> _ignored = Set.of();
            Objects.requireNonNull(sourcePlayer).teleportTo(source.getLevel(), player.getX(), player.getY(), player.getZ(), _ignored, player.getYRot(), player.getXRot(), true);
        } else {
            try {
                Vec3 destination = PlayerDataLoader.getOfflineLocation(source.getLevel(), profile.id());
                if (destination == null) {
                    source.sendFailure(Component.literal("Failure: found no data").withStyle(ChatFormatting.RED));
                    return;
                }
                Objects.requireNonNull(sourcePlayer).teleportTo(destination.x(), destination.y(), destination.z());
            } catch (IOException e) {
                AdminCraft.LOGGER.error("Failed to save offline teleport location for {}", profile.name(), e);
                source.sendFailure(Component.literal("Failure: something went wrong (IOException)").withStyle(ChatFormatting.RED));
            }
        }
        source.sendSuccess(() -> Component.literal("Teleported to " + profile.name()), true);
    }
}