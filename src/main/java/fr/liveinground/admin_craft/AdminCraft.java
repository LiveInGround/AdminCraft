package fr.liveinground.admin_craft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import fr.liveinground.admin_craft.commands.moderation.BanCommand;
import fr.liveinground.admin_craft.commands.moderation.FreezeCommand;
import fr.liveinground.admin_craft.commands.moderation.MuteCommand;
import fr.liveinground.admin_craft.commands.moderation.ReportCommand;
import fr.liveinground.admin_craft.commands.moderation.SanctionCommand;
import fr.liveinground.admin_craft.commands.moderation.TempBanCommand;
import fr.liveinground.admin_craft.commands.moderation.WarnCommand;
import fr.liveinground.admin_craft.commands.moderation.edit.EditAppealCommand;
import fr.liveinground.admin_craft.commands.moderation.edit.EditDurationCommand;
import fr.liveinground.admin_craft.commands.tools.AltCommand;
import fr.liveinground.admin_craft.commands.tools.BansCommand;
import fr.liveinground.admin_craft.commands.tools.EchestCommand;
import fr.liveinground.admin_craft.commands.tools.HistoryCommand;
import fr.liveinground.admin_craft.commands.tools.InvseeCommand;
import fr.liveinground.admin_craft.commands.tools.MutesCommand;
import fr.liveinground.admin_craft.commands.tools.OfflineTagCommand;
import fr.liveinground.admin_craft.commands.tools.OfflineTeleportCommand;
import fr.liveinground.admin_craft.commands.tools.PlayerInfoCommand;
import fr.liveinground.admin_craft.discord.DiscordBot;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.mutes.MuteEventsHandler;
import fr.liveinground.admin_craft.storage.PlayerDataManager;
import fr.liveinground.admin_craft.storage.SanctionDatabase;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mod(AdminCraft.MODID)
public class AdminCraft {

    public static final String MODID = "admin_craft";
    public static final String _VERSION = "1.1.0";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String SP_TAG = "inSpawnProtection";

    public static final List<String> mutedPlayersUUID = new ArrayList<>();
    public static final List<String> frozenPlayersUUID = new ArrayList<>();
    public static PlayerDataManager playerDataManager;

    public AdminCraft(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            LOGGER.error("AdminCraft mod can only be loaded on a server! " +
                    "Please remove it from your 'mods' folder.");
            return;
        }
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        modEventBus.register(Config.class);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(MuteEventsHandler.class);
        NeoForge.EVENT_BUS.register(FreezeEventListener.class);
        SanctionDatabase.start();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartingEvent event) {
        AdminCraft.LOGGER.info("Server is starting...");
        if (Config.enable_appeals) {
            DiscordBot.start();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        AdminCraft.LOGGER.info("Server is stopping...");
        if (Config.enable_appeals) {
            DiscordBot.jda.shutdown();
            // Allow at most 10 seconds for remaining requests to finish
            try {
                if (!DiscordBot.jda.awaitShutdown(Duration.ofSeconds(10))) {
                    DiscordBot.jda.shutdownNow(); // Cancel all remaining requests
                    DiscordBot.jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
                }
            } catch (InterruptedException e) {
                AdminCraft.LOGGER.error("An issue occurred while attempting to shutdown the discord bot:", e);
            }
        }
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        MuteCommand.register(dispatcher);
        WarnCommand.register(dispatcher);
        AltCommand.register(dispatcher);
        SanctionCommand.register(dispatcher);
        FreezeCommand.register(dispatcher);
        ReportCommand.register(dispatcher);
        TempBanCommand.register(dispatcher);
        InvseeCommand.register(dispatcher);
        EchestCommand.register(dispatcher);
        OfflineTeleportCommand.register(dispatcher);
        OfflineTagCommand.register(dispatcher);
        EditDurationCommand.register(dispatcher);
        EditAppealCommand.register(dispatcher);
        HistoryCommand.register(dispatcher);
        BansCommand.register(dispatcher);
        MutesCommand.register(dispatcher);
        PlayerInfoCommand.register(dispatcher);
        BanCommand.register(dispatcher);

        // StaffModeCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (Config.spawn_override) {
            event.getServer().overworld().getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, event.getServer());
            BlockPos spawnPos = new BlockPos(Config.spawn_x, Config.spawn_y, Config.spawn_z);
            event.getServer().setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, spawnPos, 0, 0));
        }
    }

    @SubscribeEvent
    public void onExplode(ExplosionEvent.Detonate event) {
        if (Config.sp_explosion_enabled) return;
        for (BlockPos pos: event.getAffectedBlocks()) {
            if (isInSP(event.getLevel(), pos)) {
                event.getAffectedBlocks().clear();
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
        return isInSP(level, pos);
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
    public void onPlayerTick(PlayerTickEvent.Post e) {
        Player player = e.getEntity();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (isInSP(player.level(), player.getOnPos())) {
            if (!player.getTags().contains(SP_TAG)) {
                for (Holder<MobEffect> holder : Config.loadEffects(player.level())) {
                    player.addEffect(new MobEffectInstance(holder, Integer.MAX_VALUE, 255, false, false));
                }
                player.addTag(SP_TAG);
                serverPlayer.displayClientMessage(Component.literal(LangManager.tr(TrKeys.SPAWN_ENTER)).withStyle(ChatFormatting.GREEN), true);
            }
        } else {
            if (player.getTags().contains(SP_TAG)) {
                for (Holder<MobEffect> holder : Config.loadEffects(player.level())) {
                    player.removeEffect(holder);
                }
                serverPlayer.displayClientMessage(Component.literal(LangManager.tr(TrKeys.SPAWN_LEAVE)).withStyle(ChatFormatting.RED), true);
                player.removeTag(SP_TAG);
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
            player.sendSystemMessage(Component.literal("Thank you for using AdminCraft!").withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("For a better experience, you should take a look to our configuration files."));
            player.sendSystemMessage(Component.literal("Found a bug or need help using the mod ? Join our discord or our issue tracker:"));
            player.sendSystemMessage(Component.literal("https://discord.gg/uKpPsaYmgk").withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE));
            player.sendSystemMessage(Component.literal("https://github.com/LiveInGround/AdminCraft/issues").withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE));
            player.sendSystemMessage(Component.literal("Note: you can disable this message in the configuration."));
        }
        if (player.hasPermissions(1) && !Config._config_version.equals(AdminCraft._VERSION)) {
            player.sendSystemMessage(Component.literal("AdminCraft was recently updated to a new version (" + AdminCraft._VERSION + ").").withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("It strongly recommended to check the configuration file to ensure there is no issue with it."));
            player.sendSystemMessage(Component.literal("You can disable this message by changing the 'configVersion' key to " + AdminCraft._VERSION + " in the configuration."));
        }
    }

    @Nullable
    public static NameAndId getOneProfile(Collection<NameAndId> profiles) {
        if (profiles.isEmpty()) return null;
        for (NameAndId p: profiles) {
            if (p != null) return p;
        }
        return null;
    }

    public static boolean isOnline(MinecraftServer server, NameAndId profile) {
        return getOnlinePlayer(server, profile) != null;
    }

    @Nullable
    public static ServerPlayer getOnlinePlayer(MinecraftServer server, NameAndId profile) {
        return server.getPlayerList().getPlayer(profile.id());
    }

    public static void ensureLangFilesExist() {
        Path dir = FMLPaths.CONFIGDIR.get()
                .resolve(MODID)
                .resolve("lang");

        try {
            Files.createDirectories(dir);

            copyDefault("en_US.json", dir);
            copyDefault("fr_FR.json", dir);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyDefault(String file, Path dir) throws IOException {
        Path target = dir.resolve(file);

        if (Files.exists(target)) return;

        try (InputStream in = AdminCraft.class.getResourceAsStream(
                "/assets/" + MODID + "/lang/" + file)) {

            if (in != null) {
                Files.copy(in, target);
            }
        }
    }
}
