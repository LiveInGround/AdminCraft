package fr.liveinground.admin_craft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import net.minecraft.network.chat.Component;

import java.util.List;

public class DurationArgument implements ArgumentType<String> {
    private static final SimpleCommandExceptionType INVALID_DURATION =
            new SimpleCommandExceptionType(Component.literal("Invalid duration. Expected format: 1d2h3m4s"));

    public static DurationArgument duration() {
        return new DurationArgument();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();

        if (!SanctionConfig.checkDuration(input)) {
            throw INVALID_DURATION.create();
        }

        return input;
    }

    public static List<Integer> getDuration(CommandContext<?> context, String name) {
        String input = context.getArgument(name, String.class);
        return SanctionConfig.getDuration(input);
    }

    public static java.util.Date getDurationAsDate(CommandContext<?> context, String name) {
        String input = context.getArgument(name, String.class);
        return SanctionConfig.getDurationAsDate(input);
    }
}
