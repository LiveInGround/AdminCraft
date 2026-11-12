package fr.liveinground.admin_craft.mixins;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.ServerHolder;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.chat.ChatChannel;
import fr.liveinground.admin_craft.storage.types.chat.ChatPlayerPreference;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public class ChatMixin {


    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void admin_craft$onSendChatMessage(OutgoingChatMessage message, boolean filtered, ChatType.Bound boundType, CallbackInfo ci) {
        if (message instanceof OutgoingChatMessage.Player playerChatMessage) {
            if (ci.isCancelled() || !Config.enableChatChannels) {
                return;
            }
            UUID uuid = playerChatMessage.message().sender();
            ChatPlayerPreference pref = Database.getPlayerCurrentChannel(uuid);
            String key = switch (pref.channel()) {
                case GLOBAL -> TrKeys.CHAT_CHANNEL_GLOBAL;
                case LOCAL -> TrKeys.CHAT_CHANNEL_LOCAL;
                case TEAM -> TrKeys.CHAT_CHANNEL_TEAM;
                case STAFF -> TrKeys.CHAT_CHANNEL_STAFF;
            };

            ServerPlayer player = ServerHolder.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) {
                AdminCraft.sendMessageInChannel("Server", message.content().getString(), pref.channel(), new Vec3(0, 0, 0), null);
                return;
            }
            PlayerTeam team = player.getTeam();

            if (pref.channel() == ChatChannel.TEAM && team == null) {
                AdminCraft.LOGGER.warn("{} tried to send a chat message in {}, but doesn't belong to a team: {}", player.getName().getString(), pref.channel(), message);
                player.sendSystemMessage(LangManager.parse(TrKeys.CHAT_TEAM_NO_TEAM));
                ci.cancel();
                return;
            }
            if (pref.channel() == ChatChannel.STAFF && !player.hasPermissions(Config.staffChatLevel)) {
                player.sendSystemMessage(LangManager.parse(TrKeys.CHAT_NO_PERM));
                ci.cancel();
                return;
            }

            boolean canceled = false;
            switch (pref.channel()) {
                case GLOBAL -> {
                }
                case LOCAL -> {
                    if (((ServerPlayer) (Object) this).position().distanceTo(player.position()) > Config.localChatRange) {
                        canceled = true;
                    }
                }
                case TEAM -> {
                    if (!Objects.equals(((ServerPlayer) (Object) this).getTeam(), player.getTeam())) {
                        canceled = true;
                    }
                }
                case STAFF -> {
                    if (!((ServerPlayer) (Object) this).hasPermissions(Config.staffChatLevel)) {
                        canceled = true;
                    }
                }
            }
            ci.cancel();
            if (!canceled) {
                AdminCraft.sendMessageInChannel(player.getDisplayName(), message.content().getString(), pref.channel(), player.position(), team);
            }
        }
    }

    /*
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    public OutgoingChatMessage admin_craft$editMessage(OutgoingChatMessage message) {
        if (message instanceof OutgoingChatMessage.Player(PlayerChatMessage original)) {
            if (!Config.enableChatChannels) {
                return message;
            }
            UUID uuid = original.sender();
            ChatPlayerPreference pref = Database.getPlayerCurrentChannel(uuid);
            String key = switch (pref.channel()) {
                case GLOBAL -> TrKeys.CHAT_CHANNEL_GLOBAL;
                case LOCAL -> TrKeys.CHAT_CHANNEL_LOCAL;
                case TEAM -> TrKeys.CHAT_CHANNEL_TEAM;
                case STAFF -> TrKeys.CHAT_CHANNEL_STAFF;
            };

            ServerPlayer player = ServerHolder.getServer().getPlayerList().getPlayer(uuid);
            assert player != null;  // we're handling online player messages, so this shouldn't be null.
            PlayerTeam team = player.getTeam();

            if (pref.channel() == ChatChannel.TEAM && team == null) {
                return message;     // canceled
            }

            if (pref.channel() == ChatChannel.STAFF && !player.hasPermissions(Config.staffChatLevel)) {
                return message;     // canceled
            }

            Component msg = LangManager.parse(key, Map.of(
                    "displayName", player.getDisplayName().getString(),
                    "message", message.content().getString(),
                    "team", team != null ? team.getName() : ""
            ));

            boolean canceled = false;
            switch (pref.channel()) {
                case GLOBAL -> {
                }
                case LOCAL -> {
                    if (((ServerPlayer) (Object) this).position().distanceTo(player.position()) > Config.localChatRange) {
                        canceled = true;
                    }
                }
                case TEAM -> {
                    if (!Objects.equals(((ServerPlayer) (Object) this).getTeam(), player.getTeam())) {
                        canceled = true;
                    }
                }
                case STAFF -> {
                    if (!((ServerPlayer) (Object) this).hasPermissions(Config.staffChatLevel)) {
                        canceled = true;
                    }
                }
            }

            if (!canceled) {
                PlayerChatMessage modified = new PlayerChatMessage(
                        original.link(),
                        original.signature(),
                        original.signedBody(),
                        msg,
                        original.filterMask()
                );
                message = OutgoingChatMessage.create(modified);
            }
        }

        return message;
    }*/
}