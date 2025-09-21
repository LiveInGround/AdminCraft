package fr.liveinground.my_server_utilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = My_server_utilities.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_SPAWN_PROTECTION = BUILDER.comment("Should the spawn protection being enabled ?")
            .define("enableSpawnProtection", true);

    private static final ForgeConfigSpec.IntValue SP_OP_LEVEL = BUILDER.comment("The operator level required to bypass protections")
            .defineInRange("bypassOPLevel", 1, 0, 4);

    private static final ForgeConfigSpec.IntValue SPAWN_PROTECTION_CENTER_X = BUILDER.comment("The X coordinate of the spawn protection")
            .defineInRange("spawnProtectionCenterX", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SPAWN_PROTECTION_CENTER_Z = BUILDER.comment("The Z coordinate of the spawn protection")
            .defineInRange("spawnProtectionCenterZ", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SPAWN_PROTECTION_RADIUS = BUILDER.comment("The spawn protection radius")
            .defineInRange("spawnProtectionRadius", 16, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue ENABLE_SPAWN_OVERRIDE = BUILDER.comment("Should the world spawn point being forced ?")
            .define("overrideSpawn", true);

    private static final ForgeConfigSpec.IntValue SPAWN_X = BUILDER.comment("The spawn X coordinate")
            .defineInRange("spawnX", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SPAWN_Y = BUILDER.comment("The spawn Y coordinate")
            .defineInRange("spawnY", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SPAWN_Z = BUILDER.comment("The spawn Z coordinate")
            .defineInRange("spawnZ", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> SPAWN_PROTECTION_ENTER = BUILDER.comment("The message sent when a player enters the spawn protection")
            .define("spawnEnter", "You are now in the spawn protection");

    public static final ForgeConfigSpec.ConfigValue<String> SPAWN_PROTECTION_LEAVE = BUILDER.comment("The message sent when a player leaves the spawn protection")
            .define("spawnEnter", "You are no more in the spawn protection");

    private static final ForgeConfigSpec.BooleanValue ALLOW_PVP = BUILDER.comment("Should PvP being enabled in the spawn protection ?")
            .define("enablePvP", false);

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_BLOCKS = BUILDER.comment("A list of blocks the players are allowed to interact to.")
            .defineListAllowEmpty("allowedBlocks", List.of("minecraft:stone_button"), Config::validateBlockName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SP_EFFECTS = BUILDER.comment("A list of effects applied in the spawn protection")
            .defineListAllowEmpty("spawnProtectionEffects", List.of("minecraft:resistance", "minecraft:regeneration", "minecraft:saturation"), Config::validateEffectName);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean sp_enabled;
    public static int sp_center_x;
    public static int sp_center_z;
    public static int sp_radius;
    public static boolean spawn_override;
    public static String sp_enter_msg;
    public static String sp_leave_msg;
    public static boolean sp_pvp_enabled;
    public static Set<Block> allowedBlocks;
    public static Set<MobEffect> sp_effects;
    public static int sp_op_level;
    public static int spawn_x;
    public static int spawn_y;
    public static int spawn_z;


    private static boolean validateBlockName(final Object obj) {
        return obj instanceof final String blockName && ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(blockName));
    }

    private static boolean validateEffectName(final Object obj) {
        return obj instanceof final String effectName && ForgeRegistries.MOB_EFFECTS.containsKey(new ResourceLocation(effectName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        sp_enabled = ENABLE_SPAWN_PROTECTION.get();
        sp_op_level = SP_OP_LEVEL.get();
        sp_center_x = SPAWN_PROTECTION_CENTER_X.get();
        sp_center_z = SPAWN_PROTECTION_CENTER_Z.get();
        sp_radius = SPAWN_PROTECTION_RADIUS.get();
        spawn_override = ENABLE_SPAWN_OVERRIDE.get();
        spawn_x = SPAWN_X.get();
        spawn_y = SPAWN_Y.get();
        spawn_z = SPAWN_Z.get();
        sp_enter_msg = SPAWN_PROTECTION_ENTER.get();
        sp_leave_msg = SPAWN_PROTECTION_LEAVE.get();
        sp_pvp_enabled = ALLOW_PVP.get();
        allowedBlocks = ALLOWED_BLOCKS.get().stream()
                .map(blockName -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName)))
                .collect(Collectors.toSet());
        sp_effects = SP_EFFECTS.get().stream()
                .map(effectName -> ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectName)))
                .collect(Collectors.toSet());
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        sp_effects.clear();
        allowedBlocks.clear();
        onLoad(null);
    }
}
