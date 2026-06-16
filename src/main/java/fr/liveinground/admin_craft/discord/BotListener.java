package fr.liveinground.admin_craft.discord;

import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.EmbedBuilder;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.Permission;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.entities.Member;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.entities.emoji.Emoji;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.events.message.MessageReceivedEvent;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.hooks.ListenerAdapter;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.interactions.components.buttons.Button;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.interactions.components.text.TextInput;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.interactions.modals.Modal;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.interactions.modals.ModalMapping;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.storage.SanctionDatabase;
import fr.liveinground.admin_craft.storage.types.sanction.AppealStatus;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
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
                DatabaseSanctionData dataset = SanctionDatabase.getSanctionData(id);
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
                            .queue();
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
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!DiscordBot.enabled || !event.isFromGuild() || !Objects.requireNonNull(event.getGuild()).equals(guild)) {
            return;
        }
        if (event.getId().equals(DiscordBot.INFO_MODAL_ID)) {
            String id = Objects.requireNonNull(event.getValue("id")).getAsString();
            String ign = Objects.requireNonNull(event.getValue("ign")).getAsString();
            if (SanctionDatabase.sanctionDoesntExists(id, ign)) {
                event.reply(LangManager.tr(TrKeys.DISCORD_MODAL_INFO_FAILURE_NOT_FOUND)).setEphemeral(true).queue();
                return;
            }
            DatabaseSanctionData data = SanctionDatabase.getSanctionData(id, ign);
            if (data == null) {
                event.reply(LangManager.tr(TrKeys.DISCORD_MODAL_INFO_FAILURE_NOT_FOUND)).setEphemeral(true).queue();
                return;
            }
            UUID uuid = UUID.fromString(data.uuid());

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_TITLE));
            embed.setDescription(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_DESCRIPTION));
            embed.setColor(Color.RED);

            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_IGN), ign, true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_UUID), uuid.toString(), true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_ID), id, true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_TYPE), data.type().toString(), true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_REASON), data.reason(), true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_DATE), data.date().toString(), true);
            boolean expired = false;
            if (data.expiresOn() != null) {
                if (data.expiresOn().before(new Date())) {
                    embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_EXPIRES), data.expiresOn().toString(), true);
                } else {
                    embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_EXPIRES), data.expiresOn() + LangManager.tr(TrKeys.DISCORD_EMBED_INFO_EXPIRES_EXPIRED), true);
                    expired = true;
                }
            }
            AppealStatus status = SanctionDatabase.getAppealStatus(id);
            if (status == null) {
                AdminCraft.LOGGER.error("Couldn't get appeal status for sanction id {} getAppealStatus returned null: ", id);
                event.reply(LangManager.tr(TrKeys.DISCORD_FAILURE)).setEphemeral(true).queue();
                return;
            }
            Date delay = null;
            if (status.equals(AppealStatus.DELAYED)) {
                delay = SanctionDatabase.getAppealDelay(id);
                if (delay == null) {
                    AdminCraft.LOGGER.error("Could not get appeal delay for sanction id {}", id);
                    event.reply(LangManager.tr(TrKeys.DISCORD_FAILURE)).setEphemeral(true).queue();
                    return;
                }
                if (delay.before(new Date())) {
                    status = AppealStatus.NOT_REQUESTED;
                    SanctionDatabase.changeAppealStatus(id, status);
                }
            }
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_APPEAL_STATUS), status.status(), true);
            if (delay != null && status.equals(AppealStatus.DELAYED)) {
                embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_INFO_APPEAL_DELAY), LangManager.tr(TrKeys.DISCORD_EMBED_INFO_APPEAL_DELAY_CONTENT, Map.of("delay", delay.toString())), true);
            }
            event.replyEmbeds(embed.build()).addActionRow(Button.success(DiscordBot.APPEAL_BUTTON_ID, LangManager.tr(TrKeys.DISCORD_BUTTON_APPEAL_LABEL)).withDisabled(!status.equals(AppealStatus.NOT_REQUESTED) || expired)).setEphemeral(true).queue();
            List<String> cache = new ArrayList<>();
            cache.add(id);
            cache.add(uuid.toString());
            cache.add(ign);
            DiscordBot.playerCache.put(Objects.requireNonNull(event.getMember()).getId(), cache);

        } else if (event.getId().equals(DiscordBot.APPEAL_MODAL_ID)) {
            Member member = event.getMember();
            if (member == null) {
                AdminCraft.LOGGER.error("An interaction failed, because event.getMember() is null.");
                event.reply(LangManager.tr(TrKeys.DISCORD_FAILURE_GETMEMBER_IS_NULL)).setEphemeral(true).queue();
                return;
            }
            String id = DiscordBot.playerCache.get(member.getId()).getFirst();
            String uuidStr = DiscordBot.playerCache.get(member.getId()).get(1);
            String ign = DiscordBot.playerCache.get(member.getId()).get(2);
            if (id == null) {
                event.reply(LangManager.tr(TrKeys.DISCORD_FAILURE_CACHE)).setEphemeral(true).queue();
                return;
            }
            String appealReason = Objects.requireNonNull(event.getInteraction().getValue("reason")).getAsString();

            DatabaseSanctionData data = SanctionDatabase.getSanctionData(id, ign);
            if (data == null) {
                event.reply(LangManager.tr(TrKeys.DISCORD_FAILURE_NODATA)).setEphemeral(true).queue();
                return;
            }
            UUID uuid=UUID.fromString(uuidStr);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_TITLE));
            embed.setDescription(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_DESCRIPTION));
            embed.setColor(Color.RED);

            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_IGN), ign, true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_UUID), uuid.toString(), true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_ID), id, true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_TYPE), data.type().toString(), true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_REASON), data.reason(), true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_DATE), data.date().toString(), true);
            if (data.expiresOn() != null) {
                embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_EXPIRES), data.expiresOn().toString(), true);
            }
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_PLAYER_MESSAGE), appealReason, true);
            embed.addField(LangManager.tr(TrKeys.DISCORD_EMBED_APPEAL_DISCORD_USER), member.getAsMention() + " (" + member.getEffectiveName() + ")", true);

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
            ).queue();
            SanctionDatabase.changeAppealStatus(id, AppealStatus.IN_PROGRESS);
            event.reply(LangManager.tr(TrKeys.DISCORD_APPEAL_SUCCESS)).setEphemeral(true).queue();
        }
        String[] splitted = event.getModalId().split("_");
        if (splitted.length == 3) {
            String memberID = splitted[0];
            String modalID = "_" + splitted[1] + "_";
            String id = splitted[2];

            Member member = guild.getMemberById(memberID);
            if (member == null) {
                event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ERROR_MEMBER_NOT_FOUND)).queue();
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
                if (SanctionDatabase.changeAppealStatus(id, AppealStatus.REFUSED)) {
                    member.getUser().openPrivateChannel().queue(channel -> channel.sendMessage(
                            LangManager.tr(TrKeys.DISCORD_DM_APPEAL_DENIED, Map.of(
                                    "mention", member.getAsMention(),
                                    "id", id,
                                    "guid", guild.getName(),
                                    "reason", reason
                            ))
                    ).queue());
                    event.getChannel().asTextChannel().upsertPermissionOverride(member).setDenied(EnumSet.of(Permission.VIEW_CHANNEL)).queue();
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DENY_SUCCESS))
                            .addActionRow(Button.danger(DiscordBot.DELETE_TICKET_BUTTON_ID, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DELETE)))
                            .queue();
                    AdminCraft.LOGGER.info("Appeal for sanction {} has been denied.", id);
                } else {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DENY_FAILURE_STATUS_UPDATE)).setEphemeral(true).queue();
                }
            }
            if (modalID.equals(DiscordBot.STAFF_DURATION_MODAL_ID)) {
                DatabaseSanctionData data = SanctionDatabase.getSanctionData(id);
                if (data == null) {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ERROR_SANCTION_NOT_FOUND, Map.of("id", id))).setEphemeral(true).queue();
                    return;
                }
                String duration = Objects.requireNonNull(event.getValue("duration")).getAsString();
                Date dateFromNow = SanctionConfig.getDurationAsDateSince(duration, data.date());
                if (dateFromNow == null) {
                    event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DENY_FAILURE_INVALID_DURATION)).setEphemeral(true).queue();
                    return;
                }
                if (dateFromNow.before(new Date())) {

                    if (CustomSanctionSystem.applyAppealToSanction(data, AppealStatus.REDUCED)) {
                        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessage(
                                LangManager.tr(TrKeys.DISCORD_DM_APPEAL_REDUCED_EXPIRED, Map.of(
                                        "mention", member.getAsMention(),
                                        "id", id,
                                        "guid", guild.getName(),
                                        "expires", dateFromNow.toString()
                                ))
                        ).queue());
                    } else {
                        event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ACCEPT_FAILURE)).setEphemeral(true).queue();
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
                        event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_ACCEPT_FAILURE)).setEphemeral(true).queue();
                        return;
                    }
                }
                event.getChannel().asTextChannel().upsertPermissionOverride(member).setDenied(EnumSet.of(Permission.VIEW_CHANNEL)).queue();
                event.reply(LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_REDUCE_SUCCESS))
                        .addActionRow(Button.danger(DiscordBot.DELETE_TICKET_BUTTON_ID, LangManager.tr(TrKeys.DISCORD_STAFF_BUTTON_DELETE)))
                        .queue();
                AdminCraft.LOGGER.info("Appeal for sanction {} successfully approved and sanction is reduced.", id);
            }
        }
    }
}
