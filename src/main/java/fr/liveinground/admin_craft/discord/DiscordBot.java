package fr.liveinground.admin_craft.discord;

import com.hypherionmc.sdlink.core.discord.BotController;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.JDA;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.Permission;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.entities.Guild;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.entities.Role;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.interactions.commands.OptionType;
import com.hypherionmc.sdlink.shaded.dv8tion.jda.api.interactions.commands.build.Commands;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordBot {
    public static JDA jda;
    public static boolean enabled;
    public static Guild guild;
    public static Role staff;

    public static final String INFO_BUTTON_ID = "info_button";
    public static final String APPEAL_BUTTON_ID = "appeal_button";
    public static final String INFO_MODAL_ID = "info_modal";
    public static final String APPEAL_MODAL_ID = "appeal_modal";

    public static final String REFUSE_APPEAL_BUTTON_ID = "_refuse_button_";
    public static final String OPEN_DISCUSSION_APPEAL_BUTTON_ID = "_discuss_button_";
    public static final String ACCEPT_LIGHT_APPEAL_BUTTON_ID = "_accept-light_button_";
    public static final String ACCEPT_TOTAL_APPEAL_BUTTON_ID = "_accept-total_button_";
    public static final String DELETE_TICKET_BUTTON_ID = "delete";

    public static final String STAFF_REASON_MODAL_ID = "_reason_";
    public static final String STAFF_DURATION_MODAL_ID = "_duration_";

    public static final Map<String, List<String>> playerCache = new HashMap<>();

    public static void start() {
        AdminCraft.LOGGER.info("Starting appeal bot...");
        if (Config.enable_appeals && !Config.bot_token.equals("configthisplease")) {
            jda = BotController.INSTANCE.getJDA();
            guild = jda.getGuildById(Config.guild_id);
            if (guild == null) {
                enabled = false;
                AdminCraft.LOGGER.error("Could not retrieve guild from ID {}: is the bot in the guild?", Config.guild_id);
                return;
            }
            staff = guild.getRoleById(Config.staff_role_id);
            if (staff == null) {
                enabled = false;
                AdminCraft.LOGGER.error("Could not retrieve staff role from ID {}: is the role in the guild?", Config.guild_id);
                return;
            }
            register_commands();
            jda.addEventListener(new BotListener());
            try {
                jda.awaitReady();
                if (!guild.getSelfMember().hasPermission(Permission.ADMINISTRATOR) && !guild.getSelfMember().hasPermission(Permission.MANAGE_CHANNEL, Permission.MESSAGE_SEND)) {
                    enabled = false;
                    AdminCraft.LOGGER.error("Could not enable the bot, because he doesn't have the MANAGE_CHANNEL and MESSAGE_SEND permission. Please add these permission to his role and restart the server.");
                    return;
                }
                AdminCraft.LOGGER.info("Appeal system enabled!");
                enabled = true;
            } catch (InterruptedException e) {
                AdminCraft.LOGGER.error("Error while starting appeal bot: {}", String.valueOf(e));
                enabled = false;
            }

        } else {
            if (!Config.enable_appeals) {
                AdminCraft.LOGGER.info("Appeal system disabled, skipping...");
            } else {
                AdminCraft.LOGGER.warn("Bot token was not configured, skipping...");
            }
            enabled = false;
        }
    }

    private static void register_commands() {
        guild.updateCommands().addCommands(Commands.slash("status", "Get bot or server status")
                .addOption(OptionType.STRING, "target", "What do you want to get the status of?", true)).queue();
        guild.updateCommands().addCommands(Commands.slash("post_embed", "Makes the bot send the appeal embed")
                .addOption(OptionType.STRING, "title", "The embed title", true)
                .addOption(OptionType.STRING, "description", "The embed description", true)
                .addOption(OptionType.STRING, "emoji", "The button emoji", true)
                .addOption(OptionType.STRING, "label", "The button label", true)).queue();
    }
}
