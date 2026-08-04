package fr.liveinground.admin_craft;

import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionTemplate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Config {

    private static final Pattern WEBHOOK_PATTERN = Pattern.compile(
            "^https://discord\\.com/api/webhooks/\\d+/[A-Za-z0-9\\-]+$"
    );
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue README;
    public static boolean readme;

    private static final ModConfigSpec.ConfigValue<String> _CONFIG_VERSION;
    public static String _config_version;

    private static final ModConfigSpec.ConfigValue<String> LOCALE;
    public static String locale;

    private static final ModConfigSpec.ConfigValue<Boolean> LOG_PLAYER_COMMANDS;
    public static boolean log_player_commands;

    private static final ModConfigSpec.ConfigValue<Boolean> LOG_SERVER_COMMANDS;
    public static boolean log_server_commands;

    // --------------------------
    // -- Commands permissions --
    // --------------------------

    private static final ModConfigSpec.IntValue MUTE_LEVEL;
    public static Permission mute_level;

    private static final ModConfigSpec.IntValue ALT_LEVEL;
    public static Permission alt_level;

    private static final ModConfigSpec.IntValue SANCTION_LEVEL;
    public static Permission sanction_level;

    private static final ModConfigSpec.IntValue FREEZE_LEVEL;
    public static Permission freeze_level;

    private static final ModConfigSpec.IntValue WARN_LEVEL;
    public static Permission warn_level;

    private static final ModConfigSpec.IntValue REPORTS_LEVEL;
    public static Permission reports_level;

    private static final ModConfigSpec.IntValue TEMPBAN_LEVEL;
    public static Permission tempban_level;

    private static final ModConfigSpec.IntValue BAN_LEVEL;
    public static Permission ban_level;

    private static final ModConfigSpec.IntValue INVSEE_LEVEL;
    public static Permission invsee_level;

    private static final ModConfigSpec.IntValue OTP_LEVEL;
    public static Permission otp_level;

    private static final ModConfigSpec.IntValue OTAG_LEVEL;
    public static Permission otag_level;

    private static final ModConfigSpec.IntValue HISTORY_LEVEL;
    public static Permission history_level;

    private static final ModConfigSpec.IntValue EDITDURATION_LEVEL;
    public static Permission editduration_level;

    private static final ModConfigSpec.IntValue EDITAPPEAL_LEVEL;
    public static Permission editappeal_level;

    private static final ModConfigSpec.IntValue BANS_LEVEL;
    public static Permission bans_level;

    private static final ModConfigSpec.IntValue MUTES_LEVEL;
    public static Permission mutes_level;

    private static final ModConfigSpec.IntValue PLAYER_INFO_LEVEL;
    public static Permission player_info_level;

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
    public static Permission sp_op_level;

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

    // -------------------------
    // -- Appeals and Discord --
    // -------------------------

    private static final ModConfigSpec.ConfigValue<Boolean> ENABLE_APPEALS;
    public static boolean enable_appeals;

    private static final ModConfigSpec.ConfigValue<String> BOT_TOKEN;
    public static String bot_token;

    private static final ModConfigSpec.ConfigValue<String> GUILD_ID;
    public static String guild_id;

    private static final ModConfigSpec.ConfigValue<String> INVITE_LINK;
    public static String invite_link;

    private static final ModConfigSpec.ConfigValue<String> STAFF_ROLE_ID;
    public static String staff_role_id;

    private static final ModConfigSpec.BooleanValue DEFAULT_CAN_APPEAL;
    public static boolean default_can_appeal;

    private static final ModConfigSpec.ConfigValue<String> DEFAULT_APPEAL_DELAY;
    public static String default_appeal_delay;

    static {
        BUILDER.push("misc");

        README = BUILDER.comment("Enable the readme message for operators when joining the world").worldRestart().define("readme", true);
        _CONFIG_VERSION = BUILDER
                .comment("This setting corresponds to the mod version, to check if the config is up to date. Change it when you update the mod, in order to disable the join message.")
                .define("configVersion", AdminCraft._VERSION);
        LOCALE = BUILDER.comment("The lang file the mod should use. Can be one of the supported languages: en_US, fr_FR, hu_HU.").worldRestart().define("locale", "en_US");
        LOG_SERVER_COMMANDS = BUILDER.comment("Should AdminCraft log server commands (console, command blocks, functions, mods...)?").worldRestart().define("logServerCommands", false);
        LOG_PLAYER_COMMANDS = BUILDER.comment("Should AdminCraft log player's commands").worldRestart().define("logPlayerCommands", false);

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
        BAN_LEVEL = BUILDER.comment("The OP level required to run the /ban command").worldRestart().defineInRange("ban", 3, 0, 4);
        INVSEE_LEVEL = BUILDER.comment("The OP level required to run the /invsee and /echest commands").worldRestart().defineInRange("invsee", 2, 0,4);
        OTP_LEVEL = BUILDER.comment("The OP level required to run the /otp command").worldRestart().defineInRange("otp", 2, 0, 4);
        OTAG_LEVEL = BUILDER.comment("The OP level required to run the /otag command").worldRestart().defineInRange("otp", 2, 0, 4);
        HISTORY_LEVEL = BUILDER.comment("The OP level required to run the /history command").worldRestart().defineInRange("history", 2, 0, 4);
        EDITDURATION_LEVEL = BUILDER.comment("The OP level required to run the /editduration command").worldRestart().defineInRange("editduration", 3, 0, 4);
        EDITAPPEAL_LEVEL = BUILDER.comment("The OP level required to run the /editappeal command").worldRestart().defineInRange("editappeal", 3, 0, 4);
        BANS_LEVEL = BUILDER.comment("The OP level required to run the /bans command").worldRestart().defineInRange("bans", 3, 0, 4);
        MUTES_LEVEL = BUILDER.comment("The OP level required to run the /mutes command").worldRestart().defineInRange("mutes", 3, 0, 4);
        PLAYER_INFO_LEVEL = BUILDER.comment("The OP level required to run the /playerinfo command").worldRestart().defineInRange("playerinfo", 3, 0, 4);

        BUILDER.pop();
    }

    static {
        BUILDER.push("presetSanctions");

        SANCTION_TEMPLATES = BUILDER.comment("The sanction presets for the /sanction command. Must follow the format 'displayName@used reason@level->type:durationOrPerm@level@...'.")
                .comment("Exemple: 'Cheating@Using cheats to get unfair advantages@1->warn@2->ban:5d@5->ban:perm'")
                .comment("This config key will be updated in hte future to be more intuitive, stay tuned!")
                .defineListAllowEmpty("sanctions", Arrays.asList("Cheating@Unfair advantage@1->ban:1d@2->ban:30d@3->ban:perm",
                        "spam@Spamming@1->warn@3->kick@4->mute:1d@5->mute:perm"), () -> "", Config::validateSanction);

        BUILDER.pop();
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
        BUILDER.push("discordAppeals").comment("Warning: If something went wrong with the bot, banned and muted players won't see the appeal link!");

        ENABLE_APPEALS = BUILDER.comment("Enable the discord appeal system").worldRestart().define("enable", false);
        BOT_TOKEN = BUILDER.comment("The discord bot token for the appeal system").worldRestart().define("discordToken", "configthisplease");
        GUILD_ID = BUILDER.comment("The Guild ID where the appeal system should work").worldRestart().define("guildID", "configthisplease");
        INVITE_LINK = BUILDER.comment("The invite link to the appeal server, displayed on sanction messages").worldRestart().define("invite", "https://discord.com/invite/yourinvite");
        STAFF_ROLE_ID = BUILDER.comment("The role id allowing to manage appeal tickers").worldRestart().define("staffRoleID", "configthisplease");
        DEFAULT_CAN_APPEAL = BUILDER.comment("Should players be able to appeal if nothing is provided in the command?").worldRestart().define("defaultCanAppeal", true);
        DEFAULT_APPEAL_DELAY = BUILDER.comment("The default appeal delay if nothing is provided in the command").comment("Can be a duration or 'null'").worldRestart().define("defaultAppealDelay", "null");

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateBlockName(final Object obj) {
        if (!(obj instanceof String blockName)) return false;

        Identifier rl = Identifier.tryParse(blockName);
        if (rl == null) return false;

        return BuiltInRegistries.BLOCK.containsKey(rl);
    }

    private static boolean validateEffectName(final Object obj) {
        if (!(obj instanceof String effectName)) return false;

        Identifier rl = Identifier.tryParse(effectName);
        if (rl == null) return false;

        return BuiltInRegistries.MOB_EFFECT.containsKey(rl);
    }

    private static boolean validateString(final Object obj) {
        return true;
    }

    private static boolean validateSanction(final Object obj) {
        if (!(obj instanceof String sanc)) return false;
        String[] parts = sanc.split("@");
        if (parts.length < 3) {
            AdminCraft.LOGGER.warn("Invalid length for the current sanction ('{}'): Expecting at least 3 subsection but found {}", sanc, parts.length);
            return false;
        }
        for (int i = 2; i < parts.length; i++) {
            String segment = parts[i].trim();
            String[] levelSplit = segment.split("->");
            if (levelSplit.length != 2) {
                AdminCraft.LOGGER.warn("Invalid length for the segment '{}', found {} parts instead of 2", segment, levelSplit.length);
                return false;
            }

            String action = levelSplit[1].trim();

            if (action.contains(":")) {
                String[] actSplit = action.split(":");
                try {
                    AdminCraft.LOGGER.debug("Template level validated (detected: complex): {}", Sanction.valueOf(actSplit[0].trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    AdminCraft.LOGGER.warn("Failed to validate action {} (detected: double): sanction not found", action);
                    return false;
                }
            } else {
                try {
                    AdminCraft.LOGGER.debug("Template level validated (detected: simple): {}", Sanction.valueOf(action.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    AdminCraft.LOGGER.warn("Failed to validate action {} (detected: simple): sanction not found", action);
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkWebhook(String url) {
        if (url == null || url.isEmpty()) return false;
        return WEBHOOK_PATTERN.matcher(url).matches();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {

        if (event.getConfig().getSpec() != SPEC) return;

        if (sp_effects != null) sp_effects.clear();
        if (allowedBlocks != null) allowedBlocks.clear();
        if (mute_forbidden_cmd != null) mute_forbidden_cmd.clear();
        availableReasons.clear();
        sanctions.clear();

        readme = README.get();
        _config_version = _CONFIG_VERSION.get();
        locale = LOCALE.get();

        LangManager.setLanguage(locale);
        Path langDir = FMLPaths.CONFIGDIR.get()
                .resolve(AdminCraft.MODID)
                .resolve("lang");

        AdminCraft.ensureLangFilesExist();

        LangManager.reload(langDir);

        log_server_commands = LOG_SERVER_COMMANDS.get();
        log_player_commands = LOG_PLAYER_COMMANDS.get();

        // --------------------------
        // -- Commands permissions --
        // --------------------------

        mute_level = getPermissionFromInt(MUTE_LEVEL.get());
        alt_level = getPermissionFromInt(ALT_LEVEL.get());
        sanction_level = getPermissionFromInt(SANCTION_LEVEL.get());
        freeze_level = getPermissionFromInt(FREEZE_LEVEL.get());
        warn_level = getPermissionFromInt(WARN_LEVEL.get());
        reports_level = getPermissionFromInt(REPORTS_LEVEL.get());
        tempban_level = getPermissionFromInt(TEMPBAN_LEVEL.get());
        ban_level = getPermissionFromInt(BAN_LEVEL.get());
        invsee_level = getPermissionFromInt(INVSEE_LEVEL.get());
        otag_level = getPermissionFromInt(OTAG_LEVEL.get());
        otp_level = getPermissionFromInt(OTP_LEVEL.get());
        history_level = getPermissionFromInt(HISTORY_LEVEL.get());
        editduration_level = getPermissionFromInt(EDITDURATION_LEVEL.get());
        editappeal_level = getPermissionFromInt(EDITAPPEAL_LEVEL.get());
        bans_level = getPermissionFromInt(BANS_LEVEL.get());
        mutes_level = getPermissionFromInt(MUTES_LEVEL.get());
        player_info_level = getPermissionFromInt(PLAYER_INFO_LEVEL.get());

        // ---------------
        // -- Sanctions --
        // ---------------

        for (String entry: SANCTION_TEMPLATES.get()) {
            AdminCraft.LOGGER.debug("Analysing sanction entry {}", entry);
            try {
                String[] parts = entry.split("@");
                if (parts.length < 3) {
                    AdminCraft.LOGGER.error("Invalid template (length < 3): {}", entry);
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
                        try {
                            type = Sanction.valueOf(actSplit[0].trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                        duration = actSplit[1].trim();
                        if (duration.equals("perm")) duration = null;
                    } else {
                        try {
                            type = Sanction.valueOf(action.trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
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
        sp_op_level = getPermissionFromInt(SP_OP_LEVEL.get());

        sp_center_x = SPAWN_PROTECTION_CENTER_X.get();
        sp_center_z = SPAWN_PROTECTION_CENTER_Z.get();

        sp_radius = SPAWN_PROTECTION_RADIUS.get();
        sp_pvp_enabled = ALLOW_PVP.get();
        sp_explosion_enabled = ALLOW_EXPLOSION.get();

        allowedBlocks = ALLOWED_BLOCKS.get().stream()
                .map(blockName -> BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(blockName)))
                .collect(Collectors.toSet());
        sp_effects = SP_EFFECTS.get().stream()
                .map(effectName -> BuiltInRegistries.MOB_EFFECT.getValue(Identifier.tryParse(effectName)))
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

        // ---------------
        // -- Appeals  --
        // ---------------

        enable_appeals = ENABLE_APPEALS.get();
        bot_token = BOT_TOKEN.get();
        guild_id = GUILD_ID.get();
        invite_link = INVITE_LINK.get();
        staff_role_id = STAFF_ROLE_ID.get();
        default_can_appeal = DEFAULT_CAN_APPEAL.get();
        default_appeal_delay = DEFAULT_APPEAL_DELAY.get();
        if (!default_appeal_delay.equals("null") && SanctionConfig.getDurationAsDate(default_appeal_delay) == null) {
            AdminCraft.LOGGER.warn("The configured default appeal delay is invalid, so no delay will be applied. Please set a correct duration or set 'null' to disable this warning.");
        }
    }

    public static Set<Holder<MobEffect>> loadEffects(Level level) {
        return SP_EFFECTS.get().stream()
                .map(Identifier::tryParse).filter(Objects::nonNull)
                .map(loc -> level.registryAccess()
                        .lookupOrThrow(Registries.MOB_EFFECT)
                        .getOrThrow(ResourceKey.create(Registries.MOB_EFFECT, loc))
                )
                .collect(Collectors.toSet());
    }

    private static Permission getPermissionFromInt(int level) {
        return new Permission.HasCommandLevel(PermissionLevel.byId(level));
    }
}