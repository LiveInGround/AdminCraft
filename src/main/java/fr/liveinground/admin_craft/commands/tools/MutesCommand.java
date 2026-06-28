package fr.liveinground.admin_craft.commands.tools;

import com.mojang.brigadier.CommandDispatcher;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.storage.types.PlayerMuteData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Date;

public class MutesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mutes")
                .requires(source -> source.permissions().hasPermission(Config.mutes_level))
                .executes(ctx -> {
                    MutableComponent message = Component.literal("");
                    message.append(Component.literal("Banned players:\n").withStyle(ChatFormatting.GOLD));

                    if (AdminCraft.mutedPlayersUUID.isEmpty()) {
                        message.append(Component.literal("There is no muted player.").withStyle(ChatFormatting.GREEN));
                    } else {
                        for (PlayerMuteData data: AdminCraft.playerDataManager.getMuteEntries()) {
                            message.append(Component.literal("* ").withStyle(ChatFormatting.GRAY));
                            message.append(Component.literal(data.name + "\n").withStyle(ChatFormatting.DARK_RED));
                            String reason = data.reason;
                            if (reason == null) {
                                reason = "Muted by an operator";
                            }
                            addField(message, "Reason", reason);
                            Date expires = data.expiresOn;
                            if (expires == null) {
                                addField(message, "Expires", "forever");
                            } else {
                                addField(message, "Expires", expires.toString());
                            }
                        }
                    }

                    ctx.getSource().sendSuccess(() -> message, false);
                    return 1;
                })
        );
    }

    private static void addField(MutableComponent component, String name, String value) {
        component.append(Component.literal("  - ").withStyle(ChatFormatting.GOLD));
        component.append(Component.literal(name + ": ").withStyle(ChatFormatting.YELLOW));
        component.append(Component.literal(value));
        component.append(Component.literal("\n"));
    }
}
