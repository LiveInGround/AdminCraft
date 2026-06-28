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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public class InvseeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invsee")
                .requires(commandSource -> commandSource.permissions().hasPermission(Config.invsee_level) && commandSource.isPlayer())
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            ServerPlayer operator = ctx.getSource().getPlayer();
                            assert operator != null;

                            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                            if (profiles.isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("No player was found.").withStyle(ChatFormatting.RED));
                                return 1;
                            }
                            NameAndId profile = null;
                            for (NameAndId p: profiles) {
                                if (p != null) {
                                    profile = p;
                                    break;
                                }
                            }

                            if (profile == null) {
                                ctx.getSource().sendFailure(Component.literal("No profile was found.").withStyle(ChatFormatting.RED));
                                return 1;
                            }

                            ServerPlayer onlinePlayer = ctx.getSource().getServer().getPlayerList().getPlayer(profile.id());

                            if (onlinePlayer == null) {
                                try {
                                    SimpleContainer inv = PlayerDataLoader.loadInventoryFromUUID(ctx.getSource().getLevel(), profile.id());

                                    if (inv == null) {
                                        ctx.getSource().sendFailure(Component.literal("No data was found.").withStyle(ChatFormatting.RED));
                                        return 1;
                                    }
                                    final UUID targetUUID = profile.id();

                                    operator.openMenu(new SimpleMenuProvider((id, ownInv, player) -> new ChestMenu(MenuType.GENERIC_9x4, id, ownInv, inv, 4) {
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
                                                PlayerDataSaver.applyToOnlinePlayer((SimpleContainer) this.getContainer(), online);
                                            } else {
                                                PlayerDataSaver.saveToNBT(targetUUID, ctx.getSource().getLevel(), (SimpleContainer) this.getContainer());
                                            }
                                        }
                                    }, Component.literal(profile.name() + "'s inventory")));

                                } catch (IOException e) {
                                    ctx.getSource().sendFailure(Component.literal("An error occurred when attempting to load this offline player data's: " + e.getMessage()));
                                    return 1;
                                }
                            } else {
                                Inventory playerInv = onlinePlayer.getInventory();
                                operator.openMenu(new SimpleMenuProvider((id, ownInv, player) -> new ChestMenu(MenuType.GENERIC_9x4, id, ownInv, playerInv, 4) {
                                    @Override
                                    public boolean stillValid(@NotNull Player player) {
                                        return player.isAlive();
                                    }
                                }, Component.literal(onlinePlayer.getDisplayName().getString() + "'s inventory")));
                            }

                            return 1;
                        })
                )
        );
    }
}