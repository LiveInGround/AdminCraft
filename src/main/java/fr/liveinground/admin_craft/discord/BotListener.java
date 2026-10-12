package fr.liveinground.admin_craft.discord;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.sanction.AppealStatus;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static fr.liveinground.admin_craft.discord.DiscordBot.guild;
import static fr.liveinground.admin_craft.discord.DiscordBot.staff;

public class BotListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (DiscordBot.enabled && event.getMessage().getContentRaw().equals("!AC ping") && event.isFromGuild() && event.getGuild().equals(guild)) {
            event.getMessage().reply(LangManager.tr(TrKeys.DISCORD_STATUS)).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!DiscordBot.enabled || !event.isFromGuild() || !Objects.requireNonNull(event.getGuild()).equals(guild)) {
            return;
        }
        if (event.getName().equals("status")) {

            String target = Objects.requireNonNull(event.getOption("target")).getAsString();
            if (target.equals("appeals")) {
                if (DiscordBot.enabled) {
                    event.reply(LangManager.tr(TrKeys.DISCORD_COMMAND_STATUS_APPEAL_ENABLED)).queue();
                } else {
                    event.reply(LangManager.tr(TrKeys.DISCORD_COMMAND_STATUS_APPEAL_DISABLED)).queue();
                }
            } else if (target.equals("server")) {
                event.reply(LangManager.tr(TrKeys.DISCORD_COMMAND_STATUS_SERVER_ONLINE)).queue();
            } else {
                event.reply(LangManager.tr(TrKeys.DISCORD_COMMAND_STATUS_FAILURE_ARGUMENT)).setEphemeral(true).queue();
            }
        }
        else if (event.getName().equals("post_embed")) {
            if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR)) {
                event.reply(LangManager.tr(TrKeys.DISCORD_COMMAND_POST_EMBED_FAILURE_PERMISSION)).setEphemeral(true).queue();
                return;
            }

            String title = Objects.requireNonNull(event.getOption("title")).getAsString();
            String description = Objects.requireNonNull(event.getOption("description")).getAsString();
            String emojiName = Objects.requireNonNull(event.getOption("emoji")).getAsString();

            Emoji emoji = Emoji.fromFormatted(emojiName);

            String label = Objects.requireNonNull(event.getOption("label")).getAsString();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(title);
            builder.setDescription(description);
            builder.setColor(Color.CYAN);

            event.getChannel().sendMessageEmbeds(builder.build()).setActionRow(Button.secondary(DiscordBot.INFO_BUTTON_ID, label)
                    .withEmoji(emoji)).queue();
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!DiscordBot.enabled || !event.isFromGuild() || !Objects.requireNonNull(event.getGuild()).equals(guild)) {
            return;
        }
        if (Objects.equals(event.getButton().getId(), DiscordBot.INFO_BUTTON_ID)) {
            Modal modal = Modal.create(DiscordBot.INFO_MODAL_ID, LangManager.tr(TrKeys.DISCORD_MODAL_INFO_TITLE))
                    .addActionRow(TextInput.create("id", LangManager.tr(TrKeys.DISCORD_MODAL_INFO_ID), TextInputStyle.SHORT)
                            .setPlaceholder(LangManager.tr(TrKeys.DISCORD_MODAL_INFO_ID_PLACEHOLDER))
                            .setRequired(true)
                            .setMinLength(9)
                            .setMaxLength(9)
                            .build())
                    .addActionRow(TextInput.create("ign", LangManager.tr(TrKeys.DISCORD_MODAL_INFO_IGN), TextInputStyle.SHORT)
                            .setPlaceholder(LangManager.tr(TrKeys.DISCORD_MODAL_INFO_IGN_PLACEHOLDER))
                            .setRequired(true)
                            .setMinLength(3)
                            .setMaxLength(16)
                            .build())
                    .build();
            event.replyModal(modal).queue();
        } else if (Objects.equals(event.getButton().getId(), DiscordBot.APPEAL_BUTTON_ID)) {
            Modal modal = Modal.create(DiscordBot.APPEAL_MODAL_ID, LangManager.tr(TrKeys.DISCORD_MODAL_APPEAL_TITLE))
                    .addActionRow(TextInput.create("reason", LangManager.tr(TrKeys.DISCORD_MODAL_APPEAL_REASON), TextInputStyle.PARAGRAPH)
                            .setPlaceholder(LangManager.tr(TrKeys.DISCORD_MODAL_APPEAL_REASON_PLACEHOLDER))
                            .setRequired(true)
                            .build())
                    .build();
            event.getHook().editOriginalComponents().queue();  // Remove buttons on the message to prevent multiple appeals
            event.replyModal(modal).queue();
        } else if (Objects.equals(event.getButton().getId(), DiscordBot.DELETE_TICKET_BUTTON_ID)) {
            if (!Objects.requireNonNull(event.getMember()).getRoles().contains(staff)) {
                event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_FAILURE_NOT_STAFF, Map.of("staff_role", DiscordBot.staff.getAsMention())))
                        .setEphemeral(true)
                        .queue();
                return;
            }
            event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DELETE_SUCCESS))
                    .queue(success ->
                            event.getChannel().delete().queueAfter(5, TimeUnit.SECONDS)
                    );
            return;
        }
        String[] splitted = Objects.requireNonNull(event.getButton().getId()).split("_");

        if (splitted.length == 3) {
            String memberID = splitted[0];
            String buttonID = "_" + splitted[1] + "_";
            String id = splitted[2];

            Member member = Objects.requireNonNull(guild).getMemberById(memberID);

            if (member == null) {
                event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ERROR_MEMBER_NOT_FOUND)).queue();
                return;
            }
            if (buttonID.equals(DiscordBot.REFUSE_APPEAL_BUTTON_ID)) {
                if (!Objects.requireNonNull(event.getMember()).getRoles().contains(staff)) {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_FAILURE_NOT_STAFF, Map.of("staff_role", DiscordBot.staff.getAsMention())))
                            .setEphemeral(true)
                            .queue();
                    return;
                }
                event.replyModal(Modal.create(memberID + DiscordBot.STAFF_REASON_MODAL_ID + id, LangManager.tr(TrKeys.DISCORD_STAFF_MODAL_REFUSE_TITLE))
                                .addActionRow(TextInput.create("reason", LangManager.tr(TrKeys.DISCORD_STAFF_MODAL_REFUSE_REASON), TextInputStyle.PARAGRAPH)
                                        .setPlaceholder(LangManager.tr(TrKeys.DISCORD_STAFF_MODAL_REFUSE_REASON_PLACEHOLDER))
                                        .setRequired(false)
                                        .build())
                                .build())
                        .queue();
                return;
            }
            if (buttonID.equals(DiscordBot.ACCEPT_TOTAL_APPEAL_BUTTON_ID)) {
                if (!Objects.requireNonNull(event.getMember()).getRoles().contains(staff)) {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_FAILURE_NOT_STAFF, Map.of("staff_role", DiscordBot.staff.getAsMention())))
                            .setEphemeral(true)
                            .queue();
                    return;
                }
                DatabaseSanctionData dataset = Database.getSanctionData(id);
                if (dataset == null) {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ERROR_SANCTION_NOT_FOUND, Map.of("id", id))).setEphemeral(true).queue();
                    return;
                }
                if (CustomSanctionSystem.applyAppealToSanction(dataset, AppealStatus.ACCEPTED)) {
                    event.getChannel().asTextChannel().upsertPermissionOverride(member).setDenied(EnumSet.of(Permission.VIEW_CHANNEL)).queue();
                    member.getUser().openPrivateChannel().queue(channel -> channel.sendMessage(
                            LangManager.tr(TrKeys.DISCORD_DM_APPEAL_ACCEPTED, Map.of(
                                    "mention", member.getAsMention(),
                                    "id", id,
                                    "guild", guild.getName()
                            ))
                    ).queue());
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ACCEPT_SUCCESS))
                            .addActionRow(Button.danger(DiscordBot.DELETE_TICKET_BUTTON_ID, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DELETE)))
                            .setEphemeral(false)
                            .queue();
                    event.getMessage().editMessageComponents().queue();
                    AdminCraft.LOGGER.info("Appeal for sanction {} has been approved.", id);
                } else {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ACCEPT_FAILURE)).setEphemeral(true).queue();
                    return;
                }
            }
            if (buttonID.equals(DiscordBot.ACCEPT_LIGHT_APPEAL_BUTTON_ID)) {
                if (!Objects.requireNonNull(event.getMember()).getRoles().contains(staff)) {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_FAILURE_NOT_STAFF, Map.of("staff_role", DiscordBot.staff.getAsMention())))
                            .setEphemeral(true)
                            .queue();
                    return;
                }
                event.replyModal(Modal.create(memberID + DiscordBot.STAFF_DURATION_MODAL_ID + id, LangManager.tr(TrKeys.DISCORD_STAFF_MODAL_DURATION_TITLE))
                        .addActionRow(TextInput.create("duration", LangManager.tr(TrKeys.DISCORD_STAFF_MODAL_DURATION_DURATION), TextInputStyle.SHORT).setRequired(true).build()).build()).queue();
                return;
            }
            if (buttonID.equals(DiscordBot.OPEN_DISCUSSION_APPEAL_BUTTON_ID)) {
                if (!Objects.requireNonNull(event.getMember()).getRoles().contains(staff)) {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_FAILURE_NOT_STAFF, Map.of("staff_role", DiscordBot.staff.getAsMention())))
                            .setEphemeral(true)
                            .queue();
                    return;
                }
                event.getChannel().asTextChannel().upsertPermissionOverride(member).setAllowed(EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND)).queue();
                event.getChannel().sendMessage(LangManager.tr(TrKeys.DISCORD_DISCUSSION_STARTS, Map.of("mention", member.getAsMention(), "id", id))).queue();

                Message message = event.getMessage();
                List<ActionRow> newRows = new ArrayList<>();

                for (ActionRow row : message.getActionRows()) {
                    List<Button> newButtons = new ArrayList<>();

                    for (Button button : row.getButtons()) {
                        if (button.getId() != null && button.getId().equals(event.getButton().getId())) {
                            newButtons.add(button.asDisabled());
                        } else {
                            newButtons.add(button);
                        }
                    }
                    newRows.add(ActionRow.of(newButtons));
                }
                message.editMessageComponents(newRows).queue();
                event.reply("Done").setEphemeral(true).queue();
                return;
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!DiscordBot.enabled || !event.isFromGuild() || !Objects.requireNonNull(event.getGuild()).equals(guild)) {
            AdminCraft.LOGGER.info("Ignored interaction because bot is disabled or is in another guild");
            return;
        }
        event.deferReply(true).queue();
        if (event.getModalId().equals(DiscordBot.INFO_MODAL_ID)) {
            String id = Objects.requireNonNull(event.getValue("id")).getAsString();
            String ign = Objects.requireNonNull(event.getValue("ign")).getAsString();
            if (Database.sanctionDoesntExists(id, ign)) {
                event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_MODAL_INFO_FAILURE_NOT_FOUND)).setEphemeral(true).queue();
                return;
            }
            DatabaseSanctionData data = Database.getSanctionData(id, ign);
            if (data == null) {
                event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_MODAL_INFO_FAILURE_NOT_FOUND)).setEphemeral(true).queue();
                return;
            }
            UUID uuid = UUID.fromString(data.uuid());

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_TITLE));
            embed.setDescription(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_DESCRIPTION));
            embed.setColor(Color.RED);

            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_IGN), ign, false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_UUID), uuid.toString(), false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_ID), id, false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_TYPE), data.type().toString(), false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_REASON), data.reason(), false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_DATE), data.date().toString(), false);
            boolean expired = false;
            if (data.expiresOn() != null) {
                if (data.expiresOn().after(new Date())) {
                    embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_EXPIRES), data.expiresOn().toString(), false);
                } else {
                    embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_EXPIRES), data.expiresOn() + LangManager.tr(TrKeys.DISCORD_EMBED_INFO_EXPIRES_EXPIRED), false);
                    expired = true;
                }
            }
            AppealStatus status = Database.getAppealStatus(id);
            if (status == null) {
                AdminCraft.LOGGER.error("Couldn't get appeal status for sanction id {} getAppealStatus returned null: ", id);
                event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_FAILURE)).setEphemeral(true).queue();
                return;
            }
            Date delay = null;
            if (status.equals(AppealStatus.DELAYED)) {
                delay = Database.getAppealDelay(id);
                if (delay == null) {
                    AdminCraft.LOGGER.error("Could not get appeal delay for sanction id {}", id);
                    event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_FAILURE)).setEphemeral(true).queue();
                    return;
                }
                if (delay.before(new Date())) {
                    status = AppealStatus.NOT_REQUESTED;
                    Database.changeAppealStatus(id, status);
                }
            }
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_APPEAL_STATUS), status.status(), false);
            if (delay != null && status.equals(AppealStatus.DELAYED)) {
                embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_APPEAL_DELAY), LangManager.tr(TrKeys.DISCORD_EMBED_INFO_APPEAL_DELAY_CONTENT, Map.of("delay", delay.toString())), false);
            }
            event.getHook().sendMessageEmbeds(embed.build()).addActionRow(Button.success(DiscordBot.APPEAL_BUTTON_ID, LangManager.tr(TrKeys.DISCORD_BUTTON_APPEAL_LABEL)).withDisabled(!status.equals(AppealStatus.NOT_REQUESTED) || expired)).setEphemeral(true).queue();
            List<String> cache = new ArrayList<>();
            cache.add(id);
            cache.add(uuid.toString());
            cache.add(ign);
            DiscordBot.playerCache.put(Objects.requireNonNull(event.getMember()).getId(), cache);

        } else if (event.getModalId().equals(DiscordBot.APPEAL_MODAL_ID)) {
            Member member = event.getMember();
            if (member == null) {
                AdminCraft.LOGGER.error("An interaction failed, because event.getMember() is null.");
                event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_FAILURE_GETMEMBER_IS_NULL)).setEphemeral(true).queue();
                return;
            }
            String id = DiscordBot.playerCache.get(member.getId()).getFirst();
            String uuidStr = DiscordBot.playerCache.get(member.getId()).get(1);
            String ign = DiscordBot.playerCache.get(member.getId()).get(2);
            if (id == null) {
                event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_FAILURE_CACHE)).setEphemeral(true).queue();
                return;
            }
            String appealReason = Objects.requireNonNull(event.getInteraction().getValue("reason")).getAsString();

            DatabaseSanctionData data = Database.getSanctionData(id, ign);
            if (data == null) {
                event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_FAILURE_NODATA)).setEphemeral(true).queue();
                return;
            }
            UUID uuid=UUID.fromString(uuidStr);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_TITLE));
            embed.setDescription(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_DESCRIPTION));
            embed.setColor(Color.RED);

            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_IGN), ign, false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_UUID), uuid.toString(), true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_ID), id, false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_TYPE), data.type().toString(), false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_REASON), data.reason(), false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_DATE), data.date().toString(), false);
            if (data.expiresOn() != null) {
                embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_EXPIRES), data.expiresOn().toString(), false);
            }
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_PLAYER_MESSAGE), appealReason, false);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_DISCORD_USER), member.getAsMention() + " (" + member.getEffectiveName() + ")", false);

            TextChannel appealChannel = guild.createTextChannel("appeal-" + id, event.getChannel().asTextChannel().getParentCategory())
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(staff, EnumSet.of(
                            Permission.VIEW_CHANNEL,
                            Permission.MESSAGE_HISTORY,
                            Permission.MESSAGE_SEND), null)
                    .complete();
            appealChannel.sendMessage(staff.getAsMention()).queue();
            appealChannel.sendMessageEmbeds(embed.build()).setActionRow(
                    Button.secondary(member.getId() + DiscordBot.OPEN_DISCUSSION_APPEAL_BUTTON_ID + id, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DISCUSS_LABEL)),
                    Button.success(member.getId() + DiscordBot.ACCEPT_TOTAL_APPEAL_BUTTON_ID + id, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ACCEPT_LABEL)),
                    Button.success(member.getId() + DiscordBot.ACCEPT_LIGHT_APPEAL_BUTTON_ID + id, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_REDUCE_LABEL)),
                    Button.danger(member.getId() + DiscordBot.REFUSE_APPEAL_BUTTON_ID + id, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_REFUSE_LABEL))
            ).queue(message -> message.pin().queue());
            Database.changeAppealStatus(id, AppealStatus.IN_PROGRESS);
            event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_APPEAL_SUCCESS)).setEphemeral(true).queue();
        }
        String[] splitted = event.getModalId().split("_");
        if (splitted.length == 3) {
            String memberID = splitted[0];
            String modalID = "_" + splitted[1] + "_";
            String id = splitted[2];

            Member member = guild.getMemberById(memberID);
            if (member == null) {
                event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ERROR_MEMBER_NOT_FOUND)).queue();
                return;
            }

            if (modalID.equals(DiscordBot.STAFF_REASON_MODAL_ID)) {
                ModalMapping reasonMapping = event.getValue("reason");
                String reason;
                if (reasonMapping == null) {
                    reason = LangManager.tr(TrKeys.DISCORD_REASON_NULL);
                } else {
                    reason = reasonMapping.getAsString();
                }
                if (Database.changeAppealStatus(id, AppealStatus.REFUSED)) {
                    member.getUser().openPrivateChannel().queue(channel -> channel.sendMessage(
                            LangManager.tr(TrKeys.DISCORD_DM_APPEAL_DENIED, Map.of(
                                    "mention", member.getAsMention(),
                                    "id", id,
                                    "guid", guild.getName(),
                                    "reason", reason
                            ))
                    ).queue());
                    event.getChannel().asTextChannel().upsertPermissionOverride(member).setDenied(EnumSet.of(Permission.VIEW_CHANNEL)).queue();
                    event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DENY_SUCCESS))
                            .addActionRow(Button.danger(DiscordBot.DELETE_TICKET_BUTTON_ID, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DELETE)))
                            .setEphemeral(false)
                            .queue();
                    event.getChannel().getHistory().retrievePast(1).queue(messages -> {
                        Message firstMessage = messages.getFirst();

                        firstMessage.editMessageComponents()
                                .queue();
                    });
                    AdminCraft.LOGGER.info("Appeal for sanction {} has been denied.", id);
                } else {
                    event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DENY_FAILURE_STATUS_UPDATE)).setEphemeral(true).queue();
                }
            }
            if (modalID.equals(DiscordBot.STAFF_DURATION_MODAL_ID)) {
                DatabaseSanctionData data = Database.getSanctionData(id);
                if (data == null) {
                    event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ERROR_SANCTION_NOT_FOUND, Map.of("id", id))).setEphemeral(true).queue();
                    return;
                }
                String duration = Objects.requireNonNull(event.getValue("duration")).getAsString();
                Date dateFromNow = SanctionConfig.getDurationAsDateSince(duration, data.date());
                if (dateFromNow == null) {
                    event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DENY_FAILURE_INVALID_DURATION)).setEphemeral(true).queue();
                    return;
                }
                if (dateFromNow.before(new Date())) {

                    if (CustomSanctionSystem.applyAppealToSanction(data, AppealStatus.REDUCED)) {
                        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessage(
                                LangManager.tr(TrKeys.DISCORD_DM_APPEAL_REDUCED_EXPIRED, Map.of(
                                        "mention", member.getAsMention(),
                                        "id", id,
                                        "guild", guild.getName(),
                                        "expires", dateFromNow.toString()
                                ))
                        ).queue());
                    } else {
                        event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ACCEPT_FAILURE)).setEphemeral(true).queue();
                        return;
                    }
                } else {
                    if (CustomSanctionSystem.changeDuration(data, dateFromNow)) {
                        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessage(
                                LangManager.tr(TrKeys.DISCORD_DM_APPEAL_REDUCED_NORMAL, Map.of(
                                        "mention", member.getAsMention(),
                                        "id", id,
                                        "guid", guild.getName(),
                                        "expires", dateFromNow.toString()
                                ))
                        ).queue());
                    } else {
                        event.getHook().sendMessage(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ACCEPT_FAILURE)).setEphemeral(true).queue();
                        return;
                    }
                }
                event.getChannel().asTextChannel().upsertPermissionOverride(member).setDenied(EnumSet.of(Permission.VIEW_CHANNEL)).queue();
                event.getChannel().sendMessage(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_REDUCE_SUCCESS))
                        .addActionRow(Button.danger(DiscordBot.DELETE_TICKET_BUTTON_ID, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DELETE)))
                        .queue();
                event.reply("Done").setEphemeral(true).queue();
                event.getChannel().getHistory().retrievePast(1).queue(messages -> {
                    Message firstMessage = messages.getFirst();

                    firstMessage.editMessageComponents()
                            .queue();
                });
                AdminCraft.LOGGER.info("Appeal for sanction {} successfully approved and sanction is reduced.", id);
            }
        }
    }
}
