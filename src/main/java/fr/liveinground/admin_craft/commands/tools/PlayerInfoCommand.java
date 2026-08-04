package fr.liveinground.admin_craft.commands.tools;

import com.mojang.brigadier.CommandDispatcher;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.storage.types.PlayerMuteData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.Objects;

public class PlayerInfoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playerinfo")
                .requires(source -> source.permissions().hasPermission(Config.player_info_level))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                                ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                NameAndId nameAndId = new NameAndId(player.getUUID(), player.getPlainTextName());
                                boolean isBanned = ctx.getSource().getServer().getPlayerList().getBans().isBanned(nameAndId);
                                boolean isMuted = AdminCraft.mutedPlayersUUID.contains(nameAndId.id().toString());
                                boolean isFrozen = AdminCraft.frozenPlayersUUID.contains(nameAndId.id().toString());

                                MutableComponent message = Component.literal("");
                                message.append(Component.literal(nameAndId.name() + "'s informations:\n").withStyle(ChatFormatting.GOLD));

                                Map<Boolean, Component> map = Map.of(
                                        true, Component.literal("Yes").withStyle(ChatFormatting.RED),
                                        false, Component.literal("No").withStyle(ChatFormatting.GREEN));

                                addField(message, "Banned", map.get(isBanned));
                                if (isBanned) {
                                    String reason = Objects.requireNonNull(ctx.getSource().getServer().getPlayerList().getBans().get(nameAndId)).getReason();
                                    if (reason == null) {
                                        reason = "Banned by an operator";
                                    }
                                    addField(message, "   -> Reason", reason);
                                }

                                addField(message, "Muted", map.get(isMuted));
                                if (isMuted) {
                                    for (PlayerMuteData data: AdminCraft.playerDataManager.getMuteEntries()) {
                                        if (data.uuid.equals(nameAndId.id().toString())) {
                                            addField(message, "   -> Reason", data.reason);
                                            break;
                                        }
                                    }
                                }

                                addField(message, "Frozen", map.get(isFrozen));
                                addField(message, "Gamemode", Map.of(GameType.ADVENTURE, "Adventure",
                                        GameType.CREATIVE, "Creative",
                                        GameType.SPECTATOR, "Spectator",
                                        GameType.SURVIVAL, "Survival").get(player.gameMode.getGameModeForPlayer()));
                                BlockPos pos = player.getOnPos();
                                addField(message, "World", player.level().dimension().registry().getPath());
                                addField(message, "Position", PlaceHolderSystem.replacePlaceholders("%x% %y% %z%", Map.of(
                                        "x", String.valueOf(pos.getX()),
                                        "y", String.valueOf(pos.getY() + 1),
                                        "z", String.valueOf(pos.getZ()))
                                        )
                                );

                                ctx.getSource().sendSuccess(() -> message, false);

                                return 1;
                        })
                )
        );
    }

    private static void addField(MutableComponent component, String name, Component value) {
        component.append(Component.literal("  - ").withStyle(ChatFormatting.GOLD));
        component.append(Component.literal(name + ": ").withStyle(ChatFormatting.YELLOW));
        component.append(value);
        component.append(Component.literal("\n"));
    }

    private static void addField(MutableComponent component, String name, String value) {
        component.append(Component.literal("  - ").withStyle(ChatFormatting.GOLD));
        component.append(Component.literal(name + ": ").withStyle(ChatFormatting.YELLOW));
        component.append(Component.literal(value));
        component.append(Component.literal("\n"));
    }
}
