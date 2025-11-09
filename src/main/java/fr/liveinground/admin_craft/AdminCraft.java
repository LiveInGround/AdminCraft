package fr.liveinground.admin_craft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import fr.liveinground.admin_craft.commands.AltCommand;
import fr.liveinground.admin_craft.commands.moderation.*;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.mutes.MuteEventsHandler;
import fr.liveinground.admin_craft.storage.PlayerDataManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(AdminCraft.MODID)
public class AdminCraft {

    public static final String MODID = "admin_craft";
    public static final String _VERSION = "1.0.0";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String SP_TAG = "inSpawnProtection";
    public static SanctionConfig sanctionConfig;

    public static List<String> mutedPlayersUUID = new ArrayList<>();
    public static List<String> frozenPlayersUUID = new ArrayList<>();
    public static PlayerDataManager playerDataManager;

    public AdminCraft(FMLJavaModLoadingContext ctx) {
        ctx.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(MuteEventsHandler.class);
        MinecraftForge.EVENT_BUS.register(FreezeEventListener.class);
    }

    @SubscribeEvent
    public void onServerStarted(ServerAboutToStartEvent event) {
        sanctionConfig = new SanctionConfig(event.getServer().getServerDirectory().toPath().resolve("world").resolve("serverconfig"));
        sanctionConfig.load();
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        MuteCommand.register(dispatcher);
        WarnCommand.register(dispatcher);
        AltCommand.register(dispatcher);
        SanctionCommand.register(dispatcher);
        FreezeCommand.register(dispatcher);
        if (Config.ENABLE_REPORTS.get()) {
            LOGGER.debug("Enabling reports");
            ReportCommand.register(dispatcher);
        }
        TempBanCommand.register(dispatcher);
        // StaffModeCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void onExplode(ExplosionEvent event) {
        if (Config.sp_explosion_enabled) return;
        for (BlockPos pos: event.getExplosion().getToBlow()) {
            if (isInSP(event.getLevel(), pos)) {
                event.getExplosion().clearToBlow();
                LOGGER.info("An explosion was cancelled in spawn protection.");
            }
        }
    }

    public static boolean isAllowed(Entity entity, Level level, BlockPos pos) {
        if (!isInSP(level, pos)) return true;
        return entity instanceof ServerPlayer sp && sp.hasPermissions(Config.sp_op_level);
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock e) {
        Player player = e.getEntity();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        BlockPos interactPos = e.getPos();
        Level interactLevel = e.getLevel();

        if (frozenPlayersUUID.contains(player.getStringUUID())) {
            e.setCanceled(true);
            return;
        }

        if (isAllowed(serverPlayer, interactLevel, interactPos)) return;
        if (!Config.allowedBlocks.contains(interactLevel.getBlockState(interactPos).getBlock())) {
            e.setCanceled(true);
        }
    }
    /*
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        Player player = e.getEntity();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        BlockPos interactPos = e.getPos();
        Level interactLevel = e.getLevel();

        if (!isAllowed(serverPlayer, interactLevel, interactPos)) {
            e.setCanceled(true);
        }
    }*/

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof Player p && frozenPlayersUUID.contains(p.getStringUUID())) {
            e.setCanceled(true);
            return;
        }
        if (!(isAllowed(e.getEntity(), (Level) e.getLevel(), e.getPos()))) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockLeftClick(PlayerInteractEvent.LeftClickBlock e) {
        if (!(isAllowed(e.getEntity(), e.getLevel(), e.getPos())) || frozenPlayersUUID.contains(e.getEntity().getStringUUID())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent e) {
        if (!(isAllowed(e.getPlayer(), (Level) e.getLevel(), e.getPos())) || frozenPlayersUUID.contains(e.getPlayer().getStringUUID())) {
            e.setCanceled(true);
        }
    }

    private static boolean isInSP(Level level, BlockPos pos) {
        if (!Config.sp_enabled) return false;
        if (level.dimension() == Level.OVERWORLD) {
            int minX = Config.sp_center_x - Config.sp_radius;
            int maxX = Config.sp_center_x + Config.sp_radius;
            int minZ = Config.sp_center_z - Config.sp_radius;
            int maxZ = Config.sp_center_z + Config.sp_radius;
            return (pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ);
        }
        return false;
    }

    private static boolean isInSP(Entity entity) {
        Level level = entity.level();
        BlockPos pos = entity.getOnPos();
        if (!Config.sp_enabled) return false;
        if (level.dimension() == Level.OVERWORLD) {
            int minX = Config.sp_center_x - Config.sp_radius;
            int maxX = Config.sp_center_x + Config.sp_radius;
            int minZ = Config.sp_center_z - Config.sp_radius;
            int maxZ = Config.sp_center_z + Config.sp_radius;
            return (pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ);
        }
        return false;
    }

    @SubscribeEvent
    public void onPvP(AttackEntityEvent e) {
        Entity target = e.getTarget();
        Player attacker = e.getEntity();

        if (frozenPlayersUUID.contains(target.getStringUUID()) || frozenPlayersUUID.contains(attacker.getStringUUID())) {
            e.setCanceled(true);
            return;
        }

        if (target instanceof Player) {
            if (attacker.hasPermissions(Config.sp_op_level)) return;

            if (isInSP(attacker) || isInSP(target)) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase.equals(TickEvent.Phase.END)) {
            Player player = e.player;
            ServerPlayer serverPlayer = (ServerPlayer) player;
            if (isInSP(player.level(), player.getOnPos())) {
                for (MobEffect effect : Config.sp_effects) {
                    player.addEffect(new MobEffectInstance(effect, Integer.MAX_VALUE, 255, false, false));
                }
                if (!player.getTags().contains(SP_TAG)) {
                    player.addTag(SP_TAG);
                    serverPlayer.displayClientMessage(Component.literal(Config.sp_enter_msg).withStyle(ChatFormatting.GREEN), true);
                }
            } else {
                if (player.getTags().contains(SP_TAG)) {
                    player.removeTag(SP_TAG);
                    for (MobEffect effect : Config.sp_effects) {
                        e.player.removeEffect(effect);
                    }
                    serverPlayer.displayClientMessage(Component.literal(Config.sp_leave_msg).withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (playerDataManager.getPlayerIPSDataByUUID(player.getStringUUID()) != null) {
            playerDataManager.removeIPEntry(playerDataManager.getPlayerIPSDataByUUID(player.getStringUUID()));
        }
        playerDataManager.addIPSData(player.getName().getString(), player.getStringUUID(), player.getIpAddress());
        if (player.hasPermissions(1) && Config.readme) {
            player.sendSystemMessage(Component.literal("---------------------------------------").withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("Thank you for using AdminCraft!"));
            player.sendSystemMessage(Component.literal("For a better experience, you should take a look to our configuration files."));
            player.sendSystemMessage(Component.literal("Found a bug or need help using the mod ? Join our discord or our issue tracker:"));
            player.sendSystemMessage(Component.literal("https://discord.gg/uKpPsaYmgk").withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE));
            player.sendSystemMessage(Component.literal("https://github.com/LiveInGround/AdminCraft/issues").withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE));
            player.sendSystemMessage(Component.literal("Note: you can disable this message in the configuration."));
            player.sendSystemMessage(Component.literal("---------------------------------------").withStyle(ChatFormatting.AQUA));
        }
        if (player.hasPermissions(1) && !Config._config_version.equals(AdminCraft._VERSION)) {
            player.sendSystemMessage(Component.literal("---------------------------------------").withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("AdminCraft was recently updated to a new version (" + AdminCraft._VERSION + ")."));
            player.sendSystemMessage(Component.literal("It strongly recommended to check the configuration file to check there is no issue with it."));
            player.sendSystemMessage(Component.literal("You can disable this message by changing the 'configVersion' key to " + AdminCraft._VERSION + " in the configuration."));
            player.sendSystemMessage(Component.literal("---------------------------------------").withStyle(ChatFormatting.AQUA));
        }
    }
}
