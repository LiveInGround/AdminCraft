package fr.liveinground.admin_craft.commands.moderation.edit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.storage.SanctionDatabase;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Date;
import java.util.List;

public class EditDurationCommand {
    private static final List<String> permDuration = List.of("perm", "permanent", "null");
    private static final List<Sanction> compatibleSanctions = List.of(Sanction.BAN, Sanction.MUTE);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("editduration")
                .requires(source -> source.permissions().hasPermission(Config.editduration_level))
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            String remaining = builder.getRemaining();
                            for (String id: SanctionDatabase.listIDs()) {
                                if (!remaining.contains(id)) builder.suggest('"' + id + '"');
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    DatabaseSanctionData data = SanctionDatabase.getSanctionData(id);
                                    if (data == null) {
                                        ctx.getSource().sendFailure(Component.literal("No sanction was found with id " + id).withStyle(ChatFormatting.RED));
                                        return 1;
                                    }
                                    if (!compatibleSanctions.contains(data.type())) {
                                        ctx.getSource().sendFailure(Component.literal("Sanction type " + data.type() + " does not have an implemented duration system. Please try again with another sanction.").withStyle(ChatFormatting.RED));
                                        return 1;
                                    }
                                    String duration = StringArgumentType.getString(ctx, "duration");
                                    Date newDuration;
                                    if (permDuration.contains(duration)) {
                                        newDuration = null;
                                    } else {
                                        newDuration = SanctionConfig.getDurationAsDateSince(duration, data.date());
                                        if (newDuration == null || newDuration.before(new Date())) {
                                            ctx.getSource().sendFailure(Component.literal("The provided duration is either invalid or expired.").withStyle(ChatFormatting.RED));
                                            return 1;
                                        }
                                    }
                                    if (CustomSanctionSystem.changeDuration(data, newDuration)) {
                                        String stringNewDuration;
                                        if (newDuration == null) {
                                            stringNewDuration = "never";
                                        } else {
                                            stringNewDuration = SanctionConfig.getDurationAsStringFromDate(newDuration);
                                        }
                                        ctx.getSource().sendSuccess(() -> Component.literal("Successfully changed the duration of the sanction id " + id + ", it will now expire in: " + stringNewDuration), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("An unexpected error occurred.").withStyle(ChatFormatting.RED));
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }
}
