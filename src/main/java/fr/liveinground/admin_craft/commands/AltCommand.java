package fr.liveinground.admin_craft.commands;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PermissionValue;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.storage.types.tools.PlayerIPSData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class AltCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("alts")
                .requires(commandSource -> commandSource.permissions().hasPermission(PermissionValue.fromOld(Config.alt_level).permission()))
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> {
                    // Get the target player
                    Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                    if (!profiles.isEmpty()) {
                        NameAndId targetProfile = profiles.iterator().next();
                        ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(targetProfile.id());
                        if (player == null) {
                            ctx.getSource().sendFailure(Component.literal("No player with this username was found."));
                            return 1;
                        }

                        // Get the IPS datas
                        PlayerIPSData data = AdminCraft.playerDataManager.getPlayerIPSDataByUUID(player.getStringUUID());
                        if (data == null) {
                            ctx.getSource().sendFailure(Component.literal(PlaceHolderSystem.replacePlaceholders("No IP data found for %player%.", Map.of("player", player.getName().getString()))));
                            return 1;
                        }

                        List<PlayerIPSData> alts = AdminCraft.playerDataManager.getPlayerIPSDataByIP(data.ip);
                        StringBuilder ans = new StringBuilder(PlaceHolderSystem.replacePlaceholders("The following players logged in from the I.P. address %ip%:\n", Map.of("ip", data.ip)));
                        for (PlayerIPSData n: alts) {
                            ans.append(" - ");
                            ans.append(n.name).append("\n");
                        }
                        Component answer = Component.literal(ans.toString()).withStyle(ChatFormatting.GREEN);
                        ctx.getSource().sendSuccess(() -> answer, false);
                    } else {
                        ctx.getSource().sendFailure(Component.literal("No player with this username was found."));
                    }
                    return 1;
                }
                ))
        );
    }
}