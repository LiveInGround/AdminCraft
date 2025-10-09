package fr.liveinground.admin_craft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = AdminCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --------------------------
    // -- Commands permissions --
    // --------------------------

    private static final ForgeConfigSpec.IntValue MUTE_LEVEL;
    public static int mute_level;

    private static final ForgeConfigSpec.IntValue ALT_LEVEL;
    public static int alt_level;

    private static final ForgeConfigSpec.IntValue SANCTION_LEVEL;
    public static int sanction_level;

    private static final ForgeConfigSpec.IntValue FREEZE_LEVEL;
    public static int freeze_level;

    private static final ForgeConfigSpec.IntValue WARN_LEVEL;
    public static int warn_level;

    // ----------------------
    // -- Spawn protection --
    // ----------------------

    private static final ForgeConfigSpec.BooleanValue ENABLE_SPAWN_PROTECTION;
    public static boolean sp_enabled;

    private static final ForgeConfigSpec.IntValue SP_OP_LEVEL;
    public static int sp_op_level;

    private static final ForgeConfigSpec.IntValue SPAWN_PROTECTION_CENTER_X;
    public static int sp_center_x;

    private static final ForgeConfigSpec.IntValue SPAWN_PROTECTION_CENTER_Z;
    public static int sp_center_z;

    private static final ForgeConfigSpec.IntValue SPAWN_PROTECTION_RADIUS;
    public static int sp_radius;

    private static final ForgeConfigSpec.BooleanValue ALLOW_PVP;
    public static boolean sp_pvp_enabled;

    private static final ForgeConfigSpec.BooleanValue ALLOW_EXPLOSION;
    public static boolean sp_explosion_enabled;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_BLOCKS;
    public static Set<Block> allowedBlocks;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SP_EFFECTS;
    public static Set<MobEffect> sp_effects;

    // --------------------
    // -- Spawn override --
    // --------------------

    private static final ForgeConfigSpec.BooleanValue ENABLE_SPAWN_OVERRIDE;
    public static boolean spawn_override;

    private static final ForgeConfigSpec.IntValue SPAWN_X;
    public static int spawn_x;

    private static final ForgeConfigSpec.IntValue SPAWN_Y;
    public static int spawn_y;

    private static final ForgeConfigSpec.IntValue SPAWN_Z;
    public static int spawn_z;

    // -----------------
    // -- Mute system --
    // -----------------

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MUTE_FORBIDDEN_CMD;
    public static Set<String> mute_forbidden_cmd;

    private static final ForgeConfigSpec.BooleanValue MUTE_PREVENT_SIGN_PLACING;
    public static boolean prevent_signs;

    private static final ForgeConfigSpec.BooleanValue LOG_CANCELLED_EVENTS;
    public static boolean log_cancelled_events;

    private static final ForgeConfigSpec.BooleanValue ALLOW_MESSAGES_TO_OPS;
    public static boolean allow_to_ops_msg;

    // --------------
    // -- Messages --
    // --------------

    private static final ForgeConfigSpec.ConfigValue<String> SPAWN_PROTECTION_ENTER;
    public static String sp_enter_msg;

    private static final ForgeConfigSpec.ConfigValue<String> SPAWN_PROTECTION_LEAVE;
    public static String sp_leave_msg;

    private static final ForgeConfigSpec.ConfigValue<String> TIME_REMAINING;
    public static String time_remaining;

    private static final ForgeConfigSpec.ConfigValue<String> TIME_REMAINING_SHORT;
    public static String time_remaining_short;

    private static final ForgeConfigSpec.ConfigValue<String> MUTE_MESSAGE;
    public static String mute_message;

    private static final ForgeConfigSpec.ConfigValue<String> MUTE_MESSAGE_NO_REASON;
    public static String mute_message_no_reason;

    private static final ForgeConfigSpec.ConfigValue<String> MUTE_SUCCESS;
    public static String mute_success;

    private static final ForgeConfigSpec.ConfigValue<String> MUTE_FAILED_ALREADY_MUTED;
    public static String mute_failed_already_muted;

    private static final ForgeConfigSpec.ConfigValue<String> MUTE_MESSAGE_CANCELLED;
    public static String mute_message_cancelled;

    private static final ForgeConfigSpec.ConfigValue<String> CANCEL_LOG_FORMAT;
    public static String cancel_log_format;

    private static final ForgeConfigSpec.ConfigValue<String> UNMUTE_MESSAGE;
    public static String unmute_message;

    private static final ForgeConfigSpec.ConfigValue<String> UNMUTE_SUCCESS;
    public static String unmute_success;

    private static final ForgeConfigSpec.ConfigValue<String> UNMUTE_FAILED_NOT_MUTED;
    public static String unmute_failed_not_muted;

    private static final ForgeConfigSpec.ConfigValue<String> WARN_TITLE;
    public static String warn_title;

    private static final ForgeConfigSpec.ConfigValue<String> WARN_MESSAGE;
    public static String warn_message;


    static {
        BUILDER.push("commandsPermissions");

        MUTE_LEVEL = BUILDER.comment("The OP level required to run the /mute and /unmute commands").defineInRange("mute", 3, 0, 4);
        ALT_LEVEL = BUILDER.comment("The OP level required to run the /alts command").defineInRange("alts", 3, 0, 4);
        SANCTION_LEVEL = BUILDER.comment("The OP level required to run the /sanction and /history commands").defineInRange("sanction", 3, 0, 4);
        FREEZE_LEVEL = BUILDER.comment("The OP level required to run the /freeze command").defineInRange("freeze", 3, 0, 4);
        WARN_LEVEL = BUILDER.comment("The op level required to run the /warn command").defineInRange("warnOPLevel", 3, 0, 4);

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
                        Config::validateEffectName);
        ALLOWED_BLOCKS = BUILDER.comment("Blocks players are allowed to interact with in the spawn protection")
                .defineListAllowEmpty(
                        "allowedBlocks",
                        List.of("minecraft:stone_button"),
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
        
        MUTE_FORBIDDEN_CMD = BUILDER.comment("The list of commands the players can't use while muted").defineListAllowEmpty("muteForbiddenCommands", List.of("msg", "tell", "teammsg", "w"), Config::validateString);
        MUTE_PREVENT_SIGN_PLACING = BUILDER.comment("Should the mod prevent muted players using signs ?").define("preventSigns", true);
        LOG_CANCELLED_EVENTS = BUILDER.comment("Should the mod log cancelled events to ops and console ?").define("logCancelledEvent", true);
        ALLOW_MESSAGES_TO_OPS = BUILDER.comment("Should the mod allow muted players to use commands to send messages to ops ?").define("allowMessagesToOps", true);

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


        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean validateBlockName(final Object obj) {
        if (!(obj instanceof String blockName)) return false;

        ResourceLocation rl = ResourceLocation.tryParse(blockName);
        if (rl == null) return false;

        return ForgeRegistries.BLOCKS.containsKey(rl);
    }

    private static boolean validateEffectName(final Object obj) {
        if (!(obj instanceof String effectName)) return false;

        ResourceLocation rl = ResourceLocation.tryParse(effectName);
        if (rl == null) return false;

        return ForgeRegistries.MOB_EFFECTS.containsKey(rl);
    }

    private static boolean validateString(final Object obj) {
        return true;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {

        // --------------------------
        // -- Commands permissions --
        // --------------------------

        mute_level = MUTE_LEVEL.get();
        alt_level = ALT_LEVEL.get();
        sanction_level = SANCTION_LEVEL.get();
        freeze_level = FREEZE_LEVEL.get();
        warn_level = WARN_LEVEL.get();

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
                .map(blockName -> ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(blockName)))
                .collect(Collectors.toSet());
        sp_effects = SP_EFFECTS.get().stream()
                .map(effectName -> ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(effectName)))
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
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        sp_effects.clear();
        allowedBlocks.clear();
        mute_forbidden_cmd.clear();
        onLoad(null);
    }
}