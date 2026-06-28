package fr.liveinground.admin_craft.commands.tools;

import com.mojang.brigadier.CommandDispatcher;
import fr.liveinground.admin_craft.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.util.Date;

public class BansCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bans")
                .requires(source -> source.permissions().hasPermission(Config.bans_level))
                .executes(ctx -> {
                    UserBanList bans = ctx.getSource().getServer().getPlayerList().getBans();
                    MutableComponent message = Component.literal("");
                    message.append(Component.literal("Banned players:\n").withStyle(ChatFormatting.GOLD));

                    for (UserBanListEntry entry: bans.getEntries()) {
                        message.append(Component.literal("* ").withStyle(ChatFormatting.GRAY));
                        message.append(Component.literal(entry.getUser().name() + "\n").withStyle(ChatFormatting.DARK_RED));
                        addField(message, "Moderator", entry.getSource());
                        String reason = entry.getReason();
                        if (reason == null) {
                            reason = "Banned by an operator";
                        }
                        addField(message, "Reason", reason);
                        addField(message, "Date", entry.getCreated().toString());
                        Date expires = entry.getExpires();
                        if (expires == null) {
                            addField(message, "Expires", "forever");
                        } else {
                            addField(message, "Expires", expires.toString());
                        }
                    }
                    if (bans.getEntries().isEmpty()) {
                        message.append(Component.literal("No player is banned.").withStyle(ChatFormatting.GREEN));
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
