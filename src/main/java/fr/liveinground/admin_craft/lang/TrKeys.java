package fr.liveinground.admin_craft.lang;

public class TrKeys {
    private TrKeys() {
    }

    // =========================
    // Spawn protection
    // =========================
    public static final String SPAWN_ENTER = "spawn.enter";
    public static final String SPAWN_LEAVE = "spawn.leave";

    // =========================
    // Time system
    // =========================
    public static final String TIME_REMAINING = "misc.time_remaining";
    public static final String TIME_REMAINING_SHORT = "misc.time_remaining.short";

    // =========================
    // Mute system
    // =========================
    public static final String MUTE_MESSAGE_CANCELLED = "mute.message_cancelled";
    public static final String MUTE_MESSAGE_CANCELLED_LOG = "mute.message_cancelled_log";

    // =========================
    // Commands - mute/unmute
    // =========================
    public static final String COMMAND_MUTE_SUCCESS = "command.mute.success";
    public static final String COMMAND_MUTE_FAIL_ALREADY_MUTED = "command.mute.fail.already_muted";

    public static final String COMMAND_UNMUTE_SUCCESS = "command.unmute.success";
    public static final String COMMAND_UNMUTE_FAILED_NOT_MUTED = "command.unmute.failed.not_muted";

    // =========================
    // Report system
    // =========================
    public static final String REPORT_SUCCESS = "command.report.success";
    public static final String REPORT_FAILED_WEBHOOK = "command.report.failed.webhook";
    public static final String REPORT_FAILED_SELF = "command.report.failed.self";
    public static final String REPORT_FAILED_DATABASE = "command.report.failed.database";

    // =========================
    // Freeze system
    // =========================
    public static final String FREEZE_BEGINS = "freeze.begins";
    public static final String FREEZE_ENDS = "freeze.ends";

    // =========================
    // Discord integration
    // =========================
    public static final String DISCORD_COMMAND_STATUS_APPEAL_ENABLED = "discord.command.status.appeal.enabled";
    public static final String DISCORD_COMMAND_STATUS_APPEAL_DISABLED = "discord.command.status.appeal.disabled";
    public static final String DISCORD_COMMAND_STATUS_SERVER_ONLINE = "discord.command.status.server.online";
    public static final String DISCORD_COMMAND_STATUS_FAILURE_ARGUMENT = "discord.command.status.failure.argument";
    public static final String DISCORD_STATUS = "discord.status";

    public static final String DISCORD_COMMAND_POST_EMBED_FAILURE_PERMISSION = "discord.command.post_embed.failure.permission";

    // =========================
    // Discord modals
    // =========================
    public static final String DISCORD_MODAL_INFO_TITLE = "discord.modal.info.title";
    public static final String DISCORD_MODAL_INFO_ID = "discord.modal.info.id";
    public static final String DISCORD_MODAL_INFO_IGN = "discord.modal.info.ign";
    public static final String DISCORD_MODAL_INFO_ID_PLACEHOLDER = "discord.modal.info.id.placeholder";
    public static final String DISCORD_MODAL_INFO_IGN_PLACEHOLDER = "discord.modal.info.ign.placeholder";

    public static final String DISCORD_MODAL_APPEAL_TITLE = "discord.modal.appeal.title";
    public static final String DISCORD_MODAL_APPEAL_REASON = "discord.modal.appeal.reason";
    public static final String DISCORD_MODAL_APPEAL_REASON_PLACEHOLDER = "discord.modal.appeal.reason.placeholder";

    public static final String DISCORD_MODAL_INFO_FAILURE_NOT_FOUND = "discord.modal.info.failure.not_found";

    // =========================
    // Discord staff system
    // =========================
    public static final String DISCORD_STAFF_BUTTON_ERROR_MEMBER_NOT_FOUND = "discord.staff.button.error.member_not_found";
    public static final String DISCORD_STAFF_BUTTON_ERROR_SANCTION_NOT_FOUND = "discord.staff.button.error.sanction_not_found";

    public static final String DISCORD_STAFF_MODAL_REFUSE_TITLE = "discord.staff.modal.refuse.title";
    public static final String DISCORD_STAFF_MODAL_REFUSE_REASON = "discord.staff.modal.refuse.reason";
    public static final String DISCORD_STAFF_MODAL_REFUSE_REASON_PLACEHOLDER = "discord.staff.modal.refuse.reason.placeholder";

    public static final String DISCORD_STAFF_MODAL_DURATION_TITLE = "discord.staff.modal.duration.title";
    public static final String DISCORD_STAFF_MODAL_DURATION_DURATION = "discord.staff.modal.duration.duration";

    public static final String DISCORD_STAFF_BUTTON_ACCEPT_SUCCESS = "discord.staff.button.accept.success";
    public static final String DISCORD_STAFF_BUTTON_ACCEPT_FAILURE = "discord.staff.button.accept.failure";

    public static final String DISCORD_STAFF_BUTTON_DELETE = "discord.staff.button.delete";
    public static final String DISCORD_STAFF_BUTTON_DELETE_SUCCESS = "discord.staff.button.delete.success";

    public static final String DISCORD_STAFF_BUTTON_DENY_FAILURE_INVALID_DURATION = "discord.staff.button.deny.failure.invalid_duration";
    public static final String DISCORD_STAFF_BUTTON_DENY_FAILURE_STATUS_UPDATE = "discord.staff.button.deny.failure.status_update";
    public static final String DISCORD_STAFF_BUTTON_DENY_SUCCESS = "discord.staff.button.deny.success";

    public static final String DISCORD_STAFF_BUTTON_DISCUSS_LABEL = "discord.staff.button.discuss.label";

    public static final String DISCORD_STAFF_BUTTON_ACCEPT_LABEL = "discord.staff.button.accept.label";

    public static final String DISCORD_STAFF_BUTTON_REDUCE_LABEL = "discord.staff.button.reduce.label";

    public static final String DISCORD_STAFF_BUTTON_REDUCE_SUCCESS = "discord.staff.button.reduce.success";

    public static final String DISCORD_STAFF_BUTTON_REFUSE_LABEL = "discord.staff.button.refuse.label";

    public static final String DISCORD_STAFF_BUTTON_FAILURE_NOT_STAFF = "discord.staff.button.failure.not_staff";

    // =========================
    // Discord embeds
    // =========================
    public static final String DISCORD_EMBED_INFO_TITLE = "discord.embed.info.title";
    public static final String DISCORD_EMBED_INFO_DESCRIPTION = "discord.embed.info.description";
    public static final String DISCORD_EMBED_INFO_IGN = "discord.embed.info.ign";
    public static final String DISCORD_EMBED_INFO_UUID = "discord.embed.info.uuid";
    public static final String DISCORD_EMBED_INFO_ID = "discord.embed.info.id";
    public static final String DISCORD_EMBED_INFO_TYPE = "discord.embed.info.type";
    public static final String DISCORD_EMBED_INFO_REASON = "discord.embed.info.reason";
    public static final String DISCORD_EMBED_INFO_DATE = "discord.embed.info.date";
    public static final String DISCORD_EMBED_INFO_EXPIRES = "discord.embed.info.expires";
    public static final String DISCORD_EMBED_INFO_EXPIRES_EXPIRED = "discord.embed.info.expires.expired";
    public static final String DISCORD_EMBED_INFO_APPEAL_DELAY = "discord.embed.info.appeal.delay";
    public static final String DISCORD_EMBED_INFO_APPEAL_DELAY_CONTENT = "discord.embed.info.appeal.delay.content";
    public static final String DISCORD_EMBED_INFO_APPEAL_STATUS = "discord.embed.info.appeal.status";

    public static final String DISCORD_EMBED_APPEAL_TITLE = "discord.embed.appeal.title";
    public static final String DISCORD_EMBED_APPEAL_DESCRIPTION = "discord.embed.appeal.description";
    public static final String DISCORD_EMBED_APPEAL_DATE = "discord.embed.appeal.date";
    public static final String DISCORD_EMBED_APPEAL_DISCORD_USER = "discord.embed.appeal.discord_user";
    public static final String DISCORD_EMBED_APPEAL_EXPIRES = "discord.embed.appeal.expires";
    public static final String DISCORD_EMBED_APPEAL_ID = "discord.embed.appeal.id";
    public static final String DISCORD_EMBED_APPEAL_IGN = "discord.embed.appeal.ign";
    public static final String DISCORD_EMBED_APPEAL_PLAYER_MESSAGE = "discord.embed.appeal.player_message";
    public static final String DISCORD_EMBED_APPEAL_REASON = "discord.embed.appeal.reason";
    public static final String DISCORD_EMBED_APPEAL_TYPE = "discord.embed.appeal.type";
    public static final String DISCORD_EMBED_APPEAL_UUID = "discord.embed.appeal.uuid";

    // =========================
    // Discord direct messages
    // =========================
    public static final String DISCORD_DM_APPEAL_ACCEPTED = "discord.dm.appeal.accepted";
    public static final String DISCORD_DM_APPEAL_DENIED = "discord.dm.appeal.denied";
    public static final String DISCORD_DM_APPEAL_REDUCED_EXPIRED = "discord.dm.appeal.reduced.expired";
    public static final String DISCORD_DM_APPEAL_REDUCED_NORMAL = "discord.dm.appeal.reduced.normal";

    // =========================
    // Discord buttons
    // =========================
    public static final String DISCORD_BUTTON_APPEAL_LABEL = "discord.button.appeal.label";

    // =========================
    // Generic discord errors
    // =========================
    public static final String DISCORD_FAILURE = "discord.failure";
    public static final String DISCORD_FAILURE_CACHE = "discord.failure.cache";
    public static final String DISCORD_FAILURE_GETMEMBER_IS_NULL = "discord.failure.getmember_is_null";
    public static final String DISCORD_FAILURE_NODATA = "discord.failure.nodata";

    // =========================
    // Discord misc
    // =========================
    public static final String DISCORD_REASON_NULL = "discord.reason.null";
    public static final String DISCORD_APPEAL_SUCCESS = "discord.appeal.success";
    public static final String DISCORD_DISCUSSION_STARTS = "discord.discussion.starts";

    public static final String BAN_ID_ERROR = "ban.id.error";

    // =========================
    // Sanctions
    // =========================
    public static final String SANCTION_WARNED = "sanction.warned";

    public static final String SANCTION_BANNED_PERM = "sanction.banned.perm";
    public static final String SANCTION_BANNED_TEMP = "sanction.banned.temp";

    public static final String SANCTION_MUTED_PERM = "sanction.muted.perm";
    public static final String SANCTION_MUTED_TEMP = "sanction.muted.temp";
    public static final String SANCTION_MUTED_DURATION_CHANGE = "sanction.muted.duration_change";
    public static final String SANCTION_UNMUTED = "sanction.unmuted";


    public static final String SANCTION_NOAPPEAL = "sanction.noappeal";
    public static final String SANCTION_APPEALABLE = "sanction.appealable";
}