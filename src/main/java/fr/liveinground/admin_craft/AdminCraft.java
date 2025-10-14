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
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(AdminCraft.MODID)
public class AdminCraft {

    public static final String MODID = "my_server_utilities";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String SP_TAG = "inSpawnProtection";

    public static List<String> mutedPlayersUUID = new ArrayList<>();
    public static List<String> frozenPlayersUUID = new ArrayList<>();
    public static PlayerDataManager playerDataManager;

    public AdminCraft(FMLJavaModLoadingContext ctx) {

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(MuteEventsHandler.class);

        ctx.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        SanctionConfig.load(event.getServer().getServerDirectory().toPath().resolve("world").resolve("serverconfig"));
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        MuteCommand.register(event.getDispatcher());
        WarnCommand.register(event.getDispatcher());
        AltCommand.register(event.getDispatcher());
        SanctionCommand.register(event.getDispatcher());
        FreezeCommand.register(event.getDispatcher());
        if (Config.enable_reports) {
            ReportCommand.register(event.getDispatcher());
        }
        TempBanCommand.register(event.getDispatcher());
        // StaffModeCommand.register(event.getDispatcher());
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
            if (frozenPlayersUUID.contains(player.getStringUUID())) {
                // e.setCanceled(true);
                // this event is not cancellable
                return;
            }
            ServerPlayer serverPlayer = (ServerPlayer) player;
            if (isInSP(player.level(), player.getOnPos())) {
                for (MobEffect effect : Config.sp_effects) {
                    player.addEffect(new MobEffectInstance(effect, Integer.MAX_VALUE, 255, false, false));
                }
                if (player.getTags().contains(SP_TAG)) return;
                else {
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
        playerDataManager.addIPSData(player.getName().toString(), player.getStringUUID(), player.getIpAddress());
    }

    /*
    @SubscribeEvent
    public void onSpawnFinalise(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        BlockPos currentRespawn = serverPlayer.getRespawnPosition();
        BlockPos worldSpawn = serverPlayer.getServer().overworld().getSharedSpawnPos();

        if (currentRespawn == null || currentRespawn.equals(worldSpawn)) {
            serverPlayer.setRespawnPosition(
                    Level.OVERWORLD,
                    new BlockPos(Config.spawn_x, Config.spawn_y, Config.spawn_z),
                    0f,
                    true,
                    false
            );
        }
    }*/
}
