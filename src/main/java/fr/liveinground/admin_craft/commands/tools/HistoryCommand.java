package fr.liveinground.admin_craft.commands.tools;

import com.mojang.brigadier.CommandDispatcher;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.storage.SanctionDatabase;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public class HistoryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("history")
                .requires(commandSource -> commandSource.permissions().hasPermission(Config.history_level))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                            if (profiles.isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("No player was found"));
                                return 1;
                            }
                            NameAndId targetProfile = profiles.iterator().next();

                            List<DatabaseSanctionData> history = SanctionDatabase.getHistory(targetProfile.id().toString());
                            if (history.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("This player was never sanctioned."), false);
                                return 1;
                            }
                            MutableComponent message = Component.literal("");
                            message.append(Component.literal(targetProfile.name() + "'s history:\n").withStyle(ChatFormatting.GOLD));
                            for (DatabaseSanctionData sanction: history) {
                                Date expiresOn = sanction.expiresOn();
                                message.append(Component.literal("* ").withStyle(ChatFormatting.GRAY));
                                if (sanction.removed()) {
                                    message.append(Component.literal(sanction.id() + "\n").withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH));
                                } else {
                                    message.append(Component.literal(sanction.id() + "\n").withStyle(ChatFormatting.DARK_RED));
                                }
                                addField(message, "Date", sanction.date().toString());
                                addField(message, "Type", sanction.type().toString());
                                addField(message, "Reason", sanction.reason());
                                if (expiresOn != null) {
                                    if (expiresOn.after(new Date())) {
                                        addField(message, "Expires on", expiresOn.toString());
                                    } else {
                                        addField(message, "Expires on", expiresOn + " (EXPIRED)");
                                    }
                                } else {
                                    addField(message, "Expires on", "never");
                                }
                                addField(message, "Appeal status", sanction.status().status());
                            }
                            ctx.getSource().sendSuccess(() -> message, false);

                            return 1;
                        }))
        );
    }

    private static void addField(MutableComponent component, String name, String value) {
        component.append(Component.literal("  - ").withStyle(ChatFormatting.GOLD));
        component.append(Component.literal(name + ": ").withStyle(ChatFormatting.YELLOW));
        component.append(Component.literal(value));
        component.append(Component.literal("\n"));
    }
}