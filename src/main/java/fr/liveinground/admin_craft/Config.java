package fr.liveinground.admin_craft;

import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionTemplate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    private static final Pattern WEBHOOK_PATTERN = Pattern.compile(
            "^https://discord\\.com/api/webhooks/\\d+/[A-Za-z0-9\\-]+$"
    );
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue README;
    public static boolean readme;

    private static final ModConfigSpec.ConfigValue<String> _CONFIG_VERSION;
    public static String _config_version;

    // --------------------------
    // -- Commands permissions --
    // --------------------------

    private static final ModConfigSpec.IntValue MUTE_LEVEL;
    public static int mute_level;

    private static final ModConfigSpec.IntValue ALT_LEVEL;
    public static int alt_level;

    private static final ModConfigSpec.IntValue SANCTION_LEVEL;
    public static int sanction_level;

    private static final ModConfigSpec.IntValue FREEZE_LEVEL;
    public static int freeze_level;

    private static final ModConfigSpec.IntValue WARN_LEVEL;
    public static int warn_level;

    private static final ModConfigSpec.IntValue REPORTS_LEVEL;
    public static int reports_level;

    private static final ModConfigSpec.IntValue TEMPBAN_LEVEL;
    public static int tempban_level;

    // ---------------
    // -- Sanctions --
    // ---------------

    private static final ModConfigSpec.ConfigValue<List<? extends String>> SANCTION_TEMPLATES;
    public static final List<String> availableReasons = new ArrayList<>();
    public static final Map<String, Map<Integer, SanctionTemplate>> sanctions = new HashMap<>();

    // ----------------------
    // -- Spawn protection --
    // ----------------------

    private static final ModConfigSpec.BooleanValue ENABLE_SPAWN_PROTECTION;
    public static boolean sp_enabled;

    private static final ModConfigSpec.IntValue SP_OP_LEVEL;
    public static int sp_op_level;

    private static final ModConfigSpec.IntValue SPAWN_PROTECTION_CENTER_X;
    public static int sp_center_x;

    private static final ModConfigSpec.IntValue SPAWN_PROTECTION_CENTER_Z;
    public static int sp_center_z;

    private static final ModConfigSpec.IntValue SPAWN_PROTECTION_RADIUS;
    public static int sp_radius;

    private static final ModConfigSpec.BooleanValue ALLOW_PVP;
    public static boolean sp_pvp_enabled;

    private static final ModConfigSpec.BooleanValue ALLOW_EXPLOSION;
    public static boolean sp_explosion_enabled;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_BLOCKS;
    public static Set<Block> allowedBlocks;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> SP_EFFECTS;
    public static Set<MobEffect> sp_effects;

    // --------------------
    // -- Spawn override --
    // --------------------

    private static final ModConfigSpec.BooleanValue ENABLE_SPAWN_OVERRIDE;
    public static boolean spawn_override;

    private static final ModConfigSpec.IntValue SPAWN_X;
    public static int spawn_x;

    private static final ModConfigSpec.IntValue SPAWN_Y;
    public static int spawn_y;

    private static final ModConfigSpec.IntValue SPAWN_Z;
    public static int spawn_z;

    // -----------------
    // -- Mute system --
    // -----------------

    private static final ModConfigSpec.ConfigValue<List<? extends String>> MUTE_FORBIDDEN_CMD;
    public static Set<String> mute_forbidden_cmd;

    private static final ModConfigSpec.BooleanValue MUTE_PREVENT_SIGN_PLACING;
    public static boolean prevent_signs;

    private static final ModConfigSpec.BooleanValue LOG_CANCELLED_EVENTS;
    public static boolean log_cancelled_events;

    private static final ModConfigSpec.BooleanValue ALLOW_MESSAGES_TO_OPS;
    public static boolean allow_to_ops_msg;

    // -------------
    // -- Reports --
    // -------------

    public static final ModConfigSpec.BooleanValue ENABLE_REPORTS;
    public static boolean enable_reports;

    private static final ModConfigSpec.BooleanValue USE_SANCTIONS_REASONS;
    public static boolean use_sanction_reasons;

    private static final ModConfigSpec.ConfigValue<String> REPORT_WEBHOOK;
    public static String report_webhook;

    // --------------
    // -- Messages --
    // --------------

    private static final ModConfigSpec.ConfigValue<String> SPAWN_PROTECTION_ENTER;
    public static String sp_enter_msg;

    private static final ModConfigSpec.ConfigValue<String> SPAWN_PROTECTION_LEAVE;
    public static String sp_leave_msg;

    private static final ModConfigSpec.ConfigValue<String> TIME_REMAINING;
    public static String time_remaining;

    private static final ModConfigSpec.ConfigValue<String> TIME_REMAINING_SHORT;
    public static String time_remaining_short;

    private static final ModConfigSpec.ConfigValue<String> MUTE_MESSAGE;
    public static String mute_message;

    private static final ModConfigSpec.ConfigValue<String> MUTE_MESSAGE_NO_REASON;
    public static String mute_message_no_reason;

    private static final ModConfigSpec.ConfigValue<String> MUTE_SUCCESS;
    public static String mute_success;

    private static final ModConfigSpec.ConfigValue<String> MUTE_FAILED_ALREADY_MUTED;
    public static String mute_failed_already_muted;

    private static final ModConfigSpec.ConfigValue<String> MUTE_MESSAGE_CANCELLED;
    public static String mute_message_cancelled;

    private static final ModConfigSpec.ConfigValue<String> CANCEL_LOG_FORMAT;
    public static String cancel_log_format;

    private static final ModConfigSpec.ConfigValue<String> UNMUTE_MESSAGE;
    public static String unmute_message;

    private static final ModConfigSpec.ConfigValue<String> UNMUTE_SUCCESS;
    public static String unmute_success;

    private static final ModConfigSpec.ConfigValue<String> UNMUTE_FAILED_NOT_MUTED;
    public static String unmute_failed_not_muted;

    private static final ModConfigSpec.ConfigValue<String> WARN_TITLE;
    public static String warn_title;

    private static final ModConfigSpec.ConfigValue<String> WARN_MESSAGE;
    public static String warn_message;

    private static final ModConfigSpec.ConfigValue<String> REPORT_SUCCESS;
    public static String report_success;

    private static final ModConfigSpec.ConfigValue<String> REPORT_WEBHOOK_ISSUE;
    public static String webhook_issue_message;

    private static final ModConfigSpec.ConfigValue<String> REPORT_FAILED_SELF;
    public static String report_failed_self;

    private static final ModConfigSpec.ConfigValue<String> FREEZE_START;
    public static String freeze_start;

    private static final ModConfigSpec.ConfigValue<String> FREEZE_STOP;
    public static String freeze_stop;

    static {
        BUILDER.push("misc");

        README = BUILDER.comment("Enable the readme message for operators when joining the world").worldRestart().define("readme", true);
        _CONFIG_VERSION = BUILDER
                .comment("This setting corresponds to the mod version, to check if the config is up to date. Change it when you update the mod, in order to disable the join message.")
                .define("configVersion", AdminCraft._VERSION);

        BUILDER.pop();
    }

    static {
        BUILDER.push("commandsPermissions");

        MUTE_LEVEL = BUILDER.comment("The OP level required to run the /mute and /unmute commands").worldRestart().defineInRange("mute", 3, 0, 4);
        ALT_LEVEL = BUILDER.comment("The OP level required to run the /alts command").worldRestart().defineInRange("alts", 3, 0, 4);
        SANCTION_LEVEL = BUILDER.comment("The OP level required to run the /sanction and /history commands").worldRestart().defineInRange("sanction", 3, 0, 4);
        FREEZE_LEVEL = BUILDER.comment("The OP level required to run the /freeze command").worldRestart().defineInRange("freeze", 3, 0, 4);
        WARN_LEVEL = BUILDER.comment("The OP level required to run the /warn command").worldRestart().defineInRange("warn", 3, 0, 4);
        REPORTS_LEVEL = BUILDER.comment("The OP level required to run the /reports command").worldRestart().defineInRange("reports", 3, 0, 4);
        TEMPBAN_LEVEL = BUILDER.comment("The OP level required to run the /tempban command").worldRestart().defineInRange("tempban", 3, 0,4);

        BUILDER.pop();
    }

    static {
        BUILDER.push("presetSanctions");

        SANCTION_TEMPLATES = BUILDER.comment("The sanction presets for the /sanction command. Must follow the format 'displayName@used reason@level->type:duration(if required)@level@...'.")
                .comment("Exemple: 'Cheating@Using cheats to get unfair advantages@1->warn@2->tempban:5d@5:ban'")
                .comment("This config key will be updated in hte future to be more intuitive, stay tuned!")
                .define("sanctions", List.of("Cheating@Unfair advantage@1->tempban:1d@2->tempban:30d@3->ban",
                        "spam@Spamming@1->warn@3->kick@4->tempmute:1d@5->mute"));
    }

    static {
        BUILDER.push("spawnProtection");

        ENABLE_SPAWN_PROTECTION = BUILDER.comment("Should the spawn protection being enabled?").define("enabled", true);
        SP_OP_LEVEL = BUILDER.comment("The OP level required to bypass spawn protection").defineInRange("bypassOPLevel", 1, 0, 4);
        SPAWN_PROTECTION_CENTER_X = BUILDER.comment("Center X coordinate of protection").defineInRange("centerX", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        SPAWN_PROTECTION_CENTER_Z = BUILDER.comment("Center Z coordinate of protection").defineInRange("centerZ", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        SPAWN_PROTECTION_RADIUS = BUILDER.comment("Protection radius").defineInRange("radius", 16, 0, Integer.MAX_VALUE);
        ALLOW_PVP = BUILDER.comment("Allow PvP inside spawn protection").define("enablePvP", false);
        ALLOW_EXPLOSION = BUILDER.comment("If set to false, explosions that might break blocks in the spawn protection won't deal any block damage").define("allowExplosions", false);
        SP_EFFECTS = BUILDER.comment("Effects applied in spawn protection")
                .defineListAllowEmpty(
                        "effects",
                        List.of("minecraft:resistance", "minecraft:regeneration", "minecraft:saturation"),
                        () -> "",
                        Config::validateEffectName);
        ALLOWED_BLOCKS = BUILDER.comment("Blocks players are allowed to interact with in the spawn protection")
                .defineListAllowEmpty(
                        "allowedBlocks",
                        List.of("minecraft:stone_button"),
                        () -> "",
                        Config::validateBlockName);

        BUILDER.pop();
    }

    static {
        BUILDER.push("spawnOverride");

        ENABLE_SPAWN_OVERRIDE = BUILDER.comment("Should the world spawn be overridden?").define("enabled", true);
        SPAWN_X = BUILDER.comment("Spawn X coordinate").defineInRange("x", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        SPAWN_Y = BUILDER.comment("Spawn Y coordinate").defineInRange("y", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        SPAWN_Z = BUILDER.comment("Spawn Z coordinate").defineInRange("z", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

        BUILDER.pop();
    }

    static {
        BUILDER.push("muteSystem");
        
        MUTE_FORBIDDEN_CMD = BUILDER.comment("The list of commands the players can't use while muted").defineListAllowEmpty("muteForbiddenCommands", List.of("msg", "tell", "teammsg", "w", "say"), () -> "", Config::validateString);
        MUTE_PREVENT_SIGN_PLACING = BUILDER.comment("Should the mod prevent muted players using signs ?").define("preventSigns", true);
        LOG_CANCELLED_EVENTS = BUILDER.comment("Should the mod log cancelled events to ops and console ?").define("logCancelledEvent", true);
        ALLOW_MESSAGES_TO_OPS = BUILDER.comment("Should the mod allow muted players to use commands to send messages to ops ?").define("allowMessagesToOps", true);

        BUILDER.pop();
    }

    static {
        BUILDER.push("reports");

        ENABLE_REPORTS = BUILDER.comment("Enable the /report command for every players").worldRestart().define("enable", true);
        USE_SANCTIONS_REASONS = BUILDER.comment("Should the mod suggests the sanctions defined in admin_craft_sanctions.toml as report reasons?").worldRestart().define("sanctionReasons", true);
        REPORT_WEBHOOK = BUILDER.comment("Discord webhook to relay reports. Set to 'null' to disable").define("discordWebhook", "null");

        BUILDER.pop();
    }

    static {
        BUILDER.push("messages");

        SPAWN_PROTECTION_ENTER = BUILDER.comment("Message when entering spawn protection")
                .define("enter", "You are now in the spawn protection");

        SPAWN_PROTECTION_LEAVE = BUILDER.comment("Message when leaving spawn protection")
                .define("leave", "You are no more in the spawn protection");

        TIME_REMAINING = BUILDER.comment("Message for displaying a sanction duration. Available placeholders: %days%, %hours%, and %minutes%")
                .define("timeRemainingMessage", "Time remaining: %days% days, %hours%, and %minutes% minutes");
        TIME_REMAINING_SHORT = BUILDER.comment("Message for displaying shortly a sanction duration. Available placeholders: %days%, %hours%, and %minutes%")
                .define("timeRemainingMessageShort", "Time remaining: %days%d %hours%h %minutes%m");

        MUTE_MESSAGE = BUILDER.comment("Message sent to players when they are muted. Available placeholder: %reason%").define("muteMessage", "You were muted by an operator. Reason: %reason%");
        MUTE_MESSAGE_NO_REASON = BUILDER.comment("Message sent to players when they are muted without a specified reason").define("muteMessageNoReason", "You were muted by an operator.");
        MUTE_SUCCESS = BUILDER.comment("Message sent to the moderator once the player is successfully muted. Available placeholders: %player% and %reason%").define("muteSuccess", "%player% was muted: %reason%");
        MUTE_FAILED_ALREADY_MUTED = BUILDER.comment("Message sent to the moderator if the player is already muted. Available placeholders: %player%").define("alreadyMuted", "%player% is already muted");
        MUTE_MESSAGE_CANCELLED = BUILDER.comment("Message sent to muted players when they attempt sending a message in chat").define("cancelChatMessage", "You can't send messages while muted!");
        CANCEL_LOG_FORMAT = BUILDER.comment("The log message sent to operators and console when a muted player's event is cancelled. Available placeholders: %player%, and %message%").define("logFormat", "[CANCELED] <%player% (muted)> %message%");

        UNMUTE_MESSAGE = BUILDER.comment("Message sent to players when they are unmuted").define("unMuteMessage", "You are now unmuted!");
        UNMUTE_SUCCESS = BUILDER.comment("Message sent to the moderator once the player is unmuted. Available placeholder: %player%").define("unMuteSuccess", "%player% was unmuted");
        UNMUTE_FAILED_NOT_MUTED = BUILDER.comment("Message sent to the moderator if the player is not muted. Available placeholder: %player%").define("notMuted", "%player% is not muted");

        WARN_TITLE = BUILDER.comment("The title of the warn message shown to sanctioned players").define("warnTitle", "YOU'VE BEEN WARNED!");
        WARN_MESSAGE = BUILDER.comment("The text under the title in the warn message. Available placeholders: %operator% and %reason%").define("warnMessage", "You've been warned by %operator%: %reason%. Please check the rules!");

        REPORT_SUCCESS = BUILDER.comment("The message sent to the player once his report is issued").define("reportSuccess", "Report successfully submitted. Thank you for your vigilance.");
        REPORT_WEBHOOK_ISSUE = BUILDER.comment("The message sent to the player if an issue occurred with webhook").define("webhookIssue", "An issue may have occurred during your report. Don't hesitate to contact the staff if no operator is online.");
        REPORT_FAILED_SELF = BUILDER.comment("The message sent to a player trying to report himself").define("selfReport", "You can't report yourself!");

        FREEZE_START = BUILDER.comment("The message sent to the player when he is frozen").define("freezeStartMessage", "You have been frozen by an operator. Please wait for instructions and don't log out.");
        FREEZE_STOP = BUILDER.comment("The message sent to the player when he is unfrozen").define("freezeStopMessage", "You are no more frozen. You can continue playing normally.");

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateBlockName(final Object obj) {
        if (!(obj instanceof String blockName)) return false;

        ResourceLocation rl = ResourceLocation.tryParse(blockName);
        if (rl == null) return false;

        return BuiltInRegistries.BLOCK.containsKey(rl);
    }

    private static boolean validateEffectName(final Object obj) {
        if (!(obj instanceof String effectName)) return false;

        ResourceLocation rl = ResourceLocation.tryParse(effectName);
        if (rl == null) return false;

        return BuiltInRegistries.MOB_EFFECT.containsKey(rl);
    }

    private static boolean validateString(final Object obj) {
        return true;
    }

    private static boolean checkWebhook(String url) {
        if (url == null || url.isEmpty()) return false;
        return WEBHOOK_PATTERN.matcher(url).matches();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {

        readme = README.get();
        _config_version = _CONFIG_VERSION.get();

        // --------------------------
        // -- Commands permissions --
        // --------------------------

        mute_level = MUTE_LEVEL.get();
        alt_level = ALT_LEVEL.get();
        sanction_level = SANCTION_LEVEL.get();
        freeze_level = FREEZE_LEVEL.get();
        warn_level = WARN_LEVEL.get();
        reports_level = REPORTS_LEVEL.get();
        tempban_level = TEMPBAN_LEVEL.get();

        // ---------------
        // -- Sanctions --
        // ---------------

        for (String entry: SANCTION_TEMPLATES.get()) {
            try {
                String[] parts = entry.split("@");
                if (parts.length < 3) {
                    AdminCraft.LOGGER.error("Invalid template: " + entry);
                    continue;
                }

                String displayName = parts[0].trim();
                String reason = parts[1].trim();

                availableReasons.add(displayName);
                Map<Integer, SanctionTemplate> levels = new HashMap<>();

                for (int i = 2; i < parts.length; i++) {
                    String segment = parts[i].trim();
                    String[] levelSplit = segment.split("->");
                    if (levelSplit.length != 2) continue;

                    int level = Integer.parseInt(levelSplit[0].trim());
                    String action = levelSplit[1].trim();

                    String duration = null;
                    Sanction type;

                    if (action.contains(":")) {
                        String[] actSplit = action.split(":");
                        type = Sanction.valueOf(actSplit[0].trim().toUpperCase());
                        duration = actSplit[1].trim();
                    } else {
                        type = Sanction.valueOf(action.trim().toUpperCase());
                    }

                    SanctionTemplate template = new SanctionTemplate(displayName, reason, type, duration);
                    levels.put(level, template);
                }

                sanctions.put(displayName, levels);
            } catch (Exception e) {
                AdminCraft.LOGGER.error("Error parsing sanction template: {}", entry);
                AdminCraft.LOGGER.error("Exception details:", e);
            }
        }

        // ----------------------
        // -- Spawn protection --
        // ----------------------

        sp_enabled = ENABLE_SPAWN_PROTECTION.get();
        sp_op_level = SP_OP_LEVEL.get();

        sp_center_x = SPAWN_PROTECTION_CENTER_X.get();
        sp_center_z = SPAWN_PROTECTION_CENTER_Z.get();

        sp_radius = SPAWN_PROTECTION_RADIUS.get();
        sp_pvp_enabled = ALLOW_PVP.get();
        sp_explosion_enabled = ALLOW_EXPLOSION.get();

        allowedBlocks = ALLOWED_BLOCKS.get().stream()
                .map(blockName -> BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(blockName)))
                .collect(Collectors.toSet());
        sp_effects = SP_EFFECTS.get().stream()
                .map(effectName -> BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.tryParse(effectName)))
                .collect(Collectors.toSet());

        // --------------------
        // -- Spawn override --
        // --------------------

        spawn_override = ENABLE_SPAWN_OVERRIDE.get();
        spawn_x = SPAWN_X.get();
        spawn_y = SPAWN_Y.get();
        spawn_z = SPAWN_Z.get();

        // -----------------
        // -- Mute system --
        // -----------------

        mute_forbidden_cmd = new HashSet<>(MUTE_FORBIDDEN_CMD.get());
        prevent_signs = MUTE_PREVENT_SIGN_PLACING.get();
        log_cancelled_events = LOG_CANCELLED_EVENTS.get();
        allow_to_ops_msg = ALLOW_MESSAGES_TO_OPS.get();

        // -------------
        // -- Reports --
        // -------------

        enable_reports = ENABLE_REPORTS.get();
        use_sanction_reasons = USE_SANCTIONS_REASONS.get();
        if (checkWebhook(REPORT_WEBHOOK.get())) report_webhook = REPORT_WEBHOOK.get();
        else report_webhook = null;

        // --------------
        // -- Messages --
        // --------------

        sp_enter_msg = SPAWN_PROTECTION_ENTER.get();
        sp_leave_msg = SPAWN_PROTECTION_LEAVE.get();

        time_remaining = TIME_REMAINING.get();
        time_remaining_short = TIME_REMAINING_SHORT.get();

        mute_message = MUTE_MESSAGE.get();
        mute_message_no_reason = MUTE_MESSAGE_NO_REASON.get();
        mute_success = MUTE_SUCCESS.get();
        mute_failed_already_muted = MUTE_FAILED_ALREADY_MUTED.get();

        mute_message_cancelled = MUTE_MESSAGE_CANCELLED.get();
        cancel_log_format = CANCEL_LOG_FORMAT.get();

        unmute_message = UNMUTE_MESSAGE.get();
        unmute_success = UNMUTE_SUCCESS.get();
        unmute_failed_not_muted = UNMUTE_FAILED_NOT_MUTED.get();

        warn_title = WARN_TITLE.get();
        warn_message = WARN_MESSAGE.get();

        report_success = REPORT_SUCCESS.get();
        webhook_issue_message = REPORT_WEBHOOK_ISSUE.get();
        report_failed_self = REPORT_FAILED_SELF.get();

        freeze_start = FREEZE_START.get();
        freeze_stop = FREEZE_STOP.get();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        if (sp_effects != null) sp_effects.clear();
        if (allowedBlocks != null) allowedBlocks.clear();
        if (mute_forbidden_cmd != null) mute_forbidden_cmd.clear();
        onLoad(null);
    }

    public static Set<Holder<MobEffect>> loadEffects(Level level) {
        return SP_EFFECTS.get().stream()
                .map(ResourceLocation::tryParse)
                .map(loc -> level.registryAccess()
                        .lookupOrThrow(Registries.MOB_EFFECT)
                        .getOrThrow(ResourceKey.create(Registries.MOB_EFFECT, loc))
                )
                .collect(Collectors.toSet());
    }

    public static Set<Holder<Block>> loadBlocks(Level level) {
        return SP_EFFECTS.get().stream()
                .map(ResourceLocation::tryParse)
                .map(loc -> level.registryAccess()
                        .lookupOrThrow(Registries.BLOCK)
                        .getOrThrow(ResourceKey.create(Registries.BLOCK, loc))
                )
                .collect(Collectors.toSet());
    }

}