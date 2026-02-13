package fr.liveinground.admin_craft.commands.tools;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.storage.nbt.PlayerDataLoader;
import fr.liveinground.admin_craft.storage.nbt.PlayerDataSaver;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class OfflineTagCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("otag")
                .requires(source -> source.hasPermission(Config.otag_level))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .then(Commands.literal("add")
                                .then(Commands.argument("tag", StringArgumentType.word())
                                        .executes(ctx -> addTagToProfile(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "target"), StringArgumentType.getString(ctx, "tag")))
                                )
                        )
                        .then(Commands.literal("list")
                                        .executes(ctx -> listProfileTags(
                                                ctx.getSource(),
                                                GameProfileArgument.getGameProfiles(ctx, "target"))
                                        )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("tag", StringArgumentType.word())
                                        .executes(ctx -> removeTagToProfile(
                                                ctx.getSource(),
                                                GameProfileArgument.getGameProfiles(ctx, "target"),
                                                StringArgumentType.getString(ctx, "tag")
                                        ))
                                )
                        )
                )
        );
    }

    private static int addTagToProfile(CommandSourceStack source, Collection<GameProfile> profiles, String tag) {
        if (profiles.isEmpty()) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return 1;
        }
        GameProfile profile = null;
        for (GameProfile p: profiles) {
            if (p != null) {
                profile = p;
                break;
            }
        }
        if (profile == null) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return 1;
        }

        ServerPlayer player = source.getServer().getPlayerList().getPlayer(profile.getId());
        if (player != null) {
            player.addTag(tag);
            source.sendSuccess(() -> Component.literal("Added tag '" + tag + "' to " + player.getDisplayName().getString()), true);
            return 1;
        } else {
            try {
                int success = PlayerDataSaver.addOfflineTag(source.getLevel(), profile.getId(), tag);
                if (success == 200) {
                    final GameProfile gp = profile;
                    source.sendSuccess(() -> Component.literal("Added tag '" + tag + "' to offline player " + gp.getName()), true);
                } else if (success == 404) {
                    source.sendFailure(Component.literal("Failure: targeted playerdata file not found").withStyle(ChatFormatting.RED));
                } else if (success == 409) {
                    source.sendFailure(Component.literal("Failure: the target already has the tag").withStyle(ChatFormatting.RED));
                } else {
                    source.sendFailure(Component.literal("Failure: something went wrong").withStyle(ChatFormatting.RED));
                }
            } catch (IOException e) {
                source.sendFailure(Component.literal("Failure: something went wrong (IOException)").withStyle(ChatFormatting.RED));
            }
            return 1;
        }
    }

    private static int removeTagToProfile(CommandSourceStack source, Collection<GameProfile> profiles, String tag) {
        if (profiles.isEmpty()) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return 1;
        }
        GameProfile profile = null;
        for (GameProfile p: profiles) {
            if (p != null) {
                profile = p;
                break;
            }
        }
        if (profile == null) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return 1;
        }

        ServerPlayer player = source.getServer().getPlayerList().getPlayer(profile.getId());
        if (player != null) {
            player.addTag(tag);
            source.sendSuccess(() -> Component.literal("Removed tag '" + tag + "' to " + player.getDisplayName().getString()), true);
            return 1;
        } else {
            try {
                int success = PlayerDataSaver.removeOfflineTag(source.getLevel(), profile.getId(), tag);
                if (success == 200) {
                    final GameProfile gp = profile;
                    source.sendSuccess(() -> Component.literal("Removed tag '" + tag + "' to offline player " + gp.getName()), true);
                } else if (success == 404) {
                    source.sendFailure(Component.literal("Failure: targeted playerdata file not found").withStyle(ChatFormatting.RED));
                } else if (success == 409) {
                    source.sendFailure(Component.literal("Failure: the target doesn't have the tag").withStyle(ChatFormatting.RED));
                } else {
                    source.sendFailure(Component.literal("Failure: something went wrong").withStyle(ChatFormatting.RED));
                }
            } catch (IOException e) {
                source.sendFailure(Component.literal("Failure: something went wrong (IOException)").withStyle(ChatFormatting.RED));
            }
            return 1;
        }
    }

    private static int listProfileTags(CommandSourceStack source, Collection<GameProfile> profiles) {
        if (profiles.isEmpty()) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return 1;
        }
        GameProfile profile = null;
        for (GameProfile p: profiles) {
            if (p != null) {
                profile = p;
                break;
            }
        }
        if (profile == null) {
            source.sendFailure(Component.literal("No player was found").withStyle(ChatFormatting.RED));
            return 1;
        }

        ServerPlayer player = source.getServer().getPlayerList().getPlayer(profile.getId());
        if (player != null) {
            Set<String> tags = player.getTags();
            if (tags.isEmpty()) {
                source.sendSuccess(() -> Component.literal(player.getDisplayName().getString() + " has no tags"), false);
                return 1;
            }
            String builder = "Player " + player.getDisplayName().getString() +
                    " has " + tags.size() +
                    " tags: " +
                    String.join(", ", tags);
            source.sendSuccess(() -> Component.literal(builder), false);

        } else {
            try {
                List<String> tags = PlayerDataLoader.listOfflineTags(source.getLevel(), profile.getId());
                final GameProfile gm = profile;
                if (tags == null) {
                    source.sendSuccess(() -> Component.literal(gm.getName() + " has no tags"), false);
                    return 1;
                }
                String builder = "Player " + gm.getName() +
                        " has " + tags.size() +
                        " tags: " +
                        String.join(", ", tags);
                source.sendSuccess(() -> Component.literal(builder), false);
            } catch (IOException e) {
                source.sendFailure(Component.literal("Failure: something went wrong (IOException)").withStyle(ChatFormatting.RED));
            }
        }

        return 1;
    }
}