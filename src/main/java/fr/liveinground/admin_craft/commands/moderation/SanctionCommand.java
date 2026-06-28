package fr.liveinground.admin_craft.commands.moderation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionTemplate;
import fr.liveinground.admin_craft.storage.types.tools.PlayerHistoryData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SanctionCommand {

    private static final String DUPLICATE_SANCTION = "Action denied: the player is already muted.\nThe process was stopped to prevent unintended stacking of sanctions.\nTo apply another type of sanction, please use the relevant command.";

    private static final SuggestionProvider<CommandSourceStack> REASON_SUGGESTIONS =
            (context, builder) -> {
                List<String> reasons = Config.availableReasons;
                if (reasons.isEmpty()) reasons = Collections.emptyList();
                return SharedSuggestionProvider.suggest(reasons, builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sanction")
                        .requires(commandSource -> commandSource.permissions().hasPermission(Config.sanction_level))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("reason", StringArgumentType.word()).suggests(REASON_SUGGESTIONS).executes(ctx -> {
                                            ServerPlayer sanctionedPlayer = EntityArgument.getPlayer(ctx, "player");
                                            String reason = StringArgumentType.getString(ctx, "reason");
                                            if (!Config.availableReasons.contains(reason)) {
                                                ctx.getSource().sendFailure(Component.literal("This reason is not configured yet."));
                                                return 1;
                                            }
                                            Map<Integer, SanctionTemplate> sanctionMap = Config.sanctions.get(reason);
                                            PlayerHistoryData history = AdminCraft.playerDataManager.getHistoryFromUUID(sanctionedPlayer.getStringUUID());
                                            int counter = 1;
                                            if (!(history==null || history.sanctionList.isEmpty())) {
                                                for (SanctionData data: history.sanctionList) {
                                                    if (data.reason.equals(sanctionMap.get(1).sanctionMessage())) {
                                                        counter ++;
                                                    }
                                                }
                                            }

                                            if (sanctionMap.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("This reason is not configured yet."));
                                                return 1;
                                            }
                                            if (counter > sanctionMap.size()) counter = sanctionMap.size();
                                            boolean ok = false;
                                            SanctionTemplate template=null;
                                            while (!ok) {
                                                if (sanctionMap.containsKey(counter)) {
                                                    template = sanctionMap.get(counter);
                                                    ok = true;
                                                } else {
                                                    if (counter <= 0) {
                                                        ctx.getSource().sendFailure(Component.literal("No sanction configured. Please take a look at the sanction config."));
                                                        return 1;
                                                    } else {
                                                        counter --;
                                                    }
                                                }
                                            }
                                            reason = template.sanctionMessage();
                                            switch (template.type()) {
                                                case BAN:
                                                    CustomSanctionSystem.banPlayer(ctx.getSource().getServer(), ctx.getSource().toString(), sanctionedPlayer.nameAndId(), reason, SanctionConfig.getDurationAsDate(template.duration()), true, null);
                                                    break;
                                                case KICK:
                                                    CustomSanctionSystem.kickPlayer(sanctionedPlayer, reason);
                                                    break;
                                                case MUTE:
                                                    if (AdminCraft.mutedPlayersUUID.contains(sanctionedPlayer.getStringUUID())) {
                                                        ctx.getSource().sendFailure(Component.literal(DUPLICATE_SANCTION));
                                                        return 1;
                                                    }
                                                    CustomSanctionSystem.mutePlayer(ctx.getSource().getServer(), sanctionedPlayer.nameAndId(), reason, SanctionConfig.getDurationAsDate(template.duration()), true, null);
                                                    break;
                                                case WARN:
                                                    CustomSanctionSystem.warnPlayer(sanctionedPlayer, reason, ctx.getSource().getDisplayName().getString());
                                                    break;
                                            }

                                            final String finalReason = reason;
                                            final SanctionTemplate finalTemplate = template;
                                            ctx.getSource().sendSuccess(() -> Component.literal(PlaceHolderSystem.replacePlaceholders("%player% was sanctionned (%type%): %reason%.",
                                                    Map.of("player", sanctionedPlayer.getDisplayName().getString(),
                                                            "type", finalTemplate.type().toString(),
                                                            "reason", finalReason))), true);

                                            return 1;
                                        }))
                                )
        );
    }
}