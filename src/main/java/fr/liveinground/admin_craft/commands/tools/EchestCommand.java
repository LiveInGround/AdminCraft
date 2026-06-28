package fr.liveinground.admin_craft.commands.tools;

import com.mojang.brigadier.CommandDispatcher;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.storage.nbt.PlayerDataLoader;
import fr.liveinground.admin_craft.storage.nbt.PlayerDataSaver;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public class EchestCommand {


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echest")
                .requires(src -> src.permissions().hasPermission(Config.invsee_level) && src.isPlayer())
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            ServerPlayer operator = ctx.getSource().getPlayer();
                            assert operator != null;

                            Collection<NameAndId> profiles =
                                    GameProfileArgument.getGameProfiles(ctx, "player");

                            if (profiles.isEmpty()) {
                                ctx.getSource().sendFailure(
                                        Component.literal("No player was found.")
                                                .withStyle(ChatFormatting.RED)
                                );
                                return 1;
                            }

                            NameAndId profile = profiles.iterator().next();
                            ServerPlayer target =
                                    ctx.getSource().getServer()
                                            .getPlayerList()
                                            .getPlayer(profile.id());

                            if (target != null) {
                                SimpleContainer ec = target.getEnderChestInventory();

                                operator.openMenu(
                                        new SimpleMenuProvider(
                                                (id, ownInv, player) ->
                                                        new ChestMenu(
                                                                MenuType.GENERIC_9x3,
                                                                id,
                                                                ownInv,
                                                                ec,
                                                                3
                                                        ) {
                                                            @Override
                                                            public boolean stillValid(@NotNull Player player) {
                                                                return player.isAlive();
                                                            }
                                                        },
                                                Component.literal(
                                                        target.getName().getString() + "'s Ender Chest"
                                                )
                                        )
                                );
                                return 1;
                            }

                            try {

                                SimpleContainer ec = PlayerDataLoader.loadEnderChestFromUUID(ctx.getSource().getLevel(), profile.id());
                                if (ec == null) {
                                    ctx.getSource().sendFailure(Component.literal("No data was found.").withStyle(ChatFormatting.RED));
                                    return 1;
                                }

                                final UUID targetUUID = profile.id();

                                operator.openMenu(
                                        new SimpleMenuProvider(
                                                (id, ownInv, player) ->
                                                        new ChestMenu(
                                                                MenuType.GENERIC_9x3,
                                                                id,
                                                                ownInv,
                                                                ec,
                                                                3
                                                        ) {
                                                            @Override
                                                            public boolean stillValid(@NotNull Player player) {
                                                                return player.isAlive();
                                                            }
                                                            @Override
                                                            public void removed(@NotNull Player viewer) {
                                                                super.removed(viewer);
                                                                ServerPlayer online = ctx.getSource().getServer()
                                                                        .getPlayerList()
                                                                        .getPlayer(targetUUID);

                                                                if (online != null) {
                                                                    PlayerDataSaver.applyEchestToOnlinePlayer((SimpleContainer) this.getContainer(), online);
                                                                } else {
                                                                    PlayerDataSaver.saveEchestToNBT(targetUUID, ctx.getSource().getLevel(), (SimpleContainer) this.getContainer());
                                                                }
                                                            }
                                                        },
                                                Component.literal(
                                                        profile.name() + "'s Ender Chest"
                                                )
                                        )
                                );

                            } catch (IOException e) {
                                ctx.getSource().sendFailure(
                                        Component.literal(
                                                "Failed to load offline Ender Chest: "
                                                        + e.getMessage()
                                        )
                                );
                            }

                            return 1;
                        })
                )
        );
    }
}