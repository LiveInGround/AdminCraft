package fr.liveinground.admin_craft.mutes;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.storage.PlayerDataManager;
import fr.liveinground.admin_craft.moderation.CustomSanctionSystem;
import fr.liveinground.admin_craft.storage.types.PlayerMuteData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

import static fr.liveinground.admin_craft.AdminCraft.playerDataManager;

public class MuteEventsHandler {
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        if (AdminCraft.mutedPlayersUUID.contains(player.getStringUUID())) {
            Component messageComponent = Component.literal(Config.mute_message_cancelled).withStyle(ChatFormatting.RED);
            player.sendSystemMessage(messageComponent);
            Utils.logCancelledMessage(player, event.getRawText());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCommandEvent(CommandEvent event) {

        String fullCmd = event.getParseResults().getReader().getString();
        String[] args = fullCmd.split(" ");

        if (args.length > 0) {
            String cmd = args[0].replace("/", "");
            boolean _forbidden_cmd = false;
            for (String fcmd: Config.mute_forbidden_cmd) {
                if (cmd.equalsIgnoreCase(fcmd)) {
                    _forbidden_cmd = true;
                    break;
                }
            }
            if (_forbidden_cmd) {
                try {
                    ServerPlayer sender = event.getParseResults().getContext().getSource().getPlayerOrException();
                    if (AdminCraft.mutedPlayersUUID.contains(sender.getStringUUID())) {
                        for (ServerPlayer op: Utils.getOnlineOperators()) {
                            if (args.length >= 3 && op.getName().getString().equalsIgnoreCase(args[1]) && Config.allow_to_ops_msg) {
                                op.sendSystemMessage(Component.literal("Note: The following message was sent by a muted player. Since you have operator permission, the message wasn't cancelled."));
                                event.setCanceled(false);
                                return;
                            }
                        }
                        Utils.logCancelledMessage(sender, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                        event.setCanceled(true);
                        Component messageComponent = Component.literal(Config.mute_message_cancelled).withStyle(ChatFormatting.RED);
                        sender.sendSystemMessage(messageComponent);

                    }
                } catch (CommandSyntaxException ignored) {}
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        Path serverPath = event.getServer().getServerDirectory().toPath();
        playerDataManager = new PlayerDataManager(serverPath);
        for (PlayerMuteData playerData: playerDataManager.getMuteEntries()) {
            AdminCraft.mutedPlayersUUID.add(playerData.uuid);
        }

    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (playerDataManager != null) {
            playerDataManager.save();
        }
    }

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel && ((ServerLevel) event.getLevel()).dimension() == Level.OVERWORLD) {
            if (playerDataManager != null) {
                playerDataManager.save();
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {

            if ((event.getPlacedBlock().getBlock() instanceof SignBlock) || (event.getPlacedBlock().getBlock() instanceof WallSignBlock))
                if (AdminCraft.mutedPlayersUUID.contains(p.getStringUUID()) && Config.prevent_signs) {
                    event.setCanceled(true);
                    // event.getLevel().playSound(p, event.getPos(), SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
                    Utils.logCancelledMessage(p, "[BLOCK] The placement of a sign by this muted player was canceled.");
                }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player.level().getGameTime() %20 == 0) {
            if (AdminCraft.mutedPlayersUUID.contains(e.player.getStringUUID())) {
                Date now = new Date();
                PlayerMuteData data = playerDataManager.getPlayerMuteDataByUUID(e.player.getStringUUID());
                if (data.expiresOn != null && data.expiresOn.before(now)) {
                    CustomSanctionSystem.unMutePlayer((ServerPlayer) e.player);
                }
            }
        }
    }
}
