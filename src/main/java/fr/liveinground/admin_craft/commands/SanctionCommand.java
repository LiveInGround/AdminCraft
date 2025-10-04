package fr.liveinground.admin_craft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.moderation.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.codehaus.plexus.util.cli.Commandline;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SanctionCommand {

    private static final List<String> reasons = SanctionConfig.availableReasons;

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(reasons, builder);


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("sanction")
                .requires(commandSource -> commandSource.hasPermission(Config.sanction_level))
                .then(Commands.argument("player", EntityArgument.player()))
                .then(Commands.argument("reason", StringArgumentType.word()).suggests(MODE_SUGGESTIONS)).executes(ctx -> {
                    ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                    String reason = StringArgumentType.getString(ctx, "reason");
                    if (!reasons.contains(reason)) {
                        ctx.getSource().sendFailure(Component.literal("This reason is not configured yet."));
                        return 1;
                    }
                    Map<Integer, SanctionTemplate> sanctionMap = SanctionConfig.sanctions.get(reason);
                    PlayerHistoryData history = AdminCraft.playerDataManager.getHistoryFromUUID(sanctionedPlayer.getStringUUID());
                    int counter = 0;
                    for (SanctionData data: history.sanctionList) {
                        if (data.reason.equals(reason)) {
                            counter ++;
                        }
                    }

                    SanctionTemplate template = sanctionMap.get(counter);
                    switch (template.type()) {
                        case BAN:
                            CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().toString(), sanctionedPlayer, reason, null);
                            break;
                        case TEMPBAN:
                            Date banExpiresOn = SanctionConfig.getDurationAsDate(template.duration());
                            CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().toString(), sanctionedPlayer, reason, banExpiresOn);
                            break;
                        case KICK:
                            CustomSanctionSystem.kickPlayer(sanctionedPlayer, reason);
                            break;
                        case MUTE:
                            CustomSanctionSystem.mutePlayer(sanctionedPlayer, reason, null);
                            break;
                        case TEMPMUTE:
                            Date muteExpiresOn = SanctionConfig.getDurationAsDate(template.duration());
                            CustomSanctionSystem.mutePlayer(sanctionedPlayer, reason, muteExpiresOn);
                            break;
                        case WARN:
                            // todo
                            break;
                    }

                    ctx.getSource().sendSuccess(() -> Component.literal(PlaceHolderSystem.replacePlaceholders("%player% was sanctionned (%type%): %reason%.",
                            Map.of("player", sanctionedPlayer.getDisplayName().getString(),
                                    "type", template.type().toString(),
                                    "reason", reason))), true);

                    return 1;
                })
            );
    }
}