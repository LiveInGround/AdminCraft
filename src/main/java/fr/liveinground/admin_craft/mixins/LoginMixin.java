package fr.liveinground.admin_craft.mixins;

import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.discord.DiscordBot;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.sanction.AppealStatus;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

@Mixin(PlayerList.class)
public class LoginMixin {

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void canPlayerLogin(SocketAddress address, NameAndId player, CallbackInfoReturnable<Component> cir) {
        List<DatabaseSanctionData> punishments = Database.getCurrentSanctions(player.id().toString());
        if (!punishments.isEmpty()) {
            DatabaseSanctionData sanction = punishments.stream()
                    .filter(d ->
                        d.type().equals(Sanction.BAN)
                    )
                    .findFirst()
                    .orElse(null);

            if (sanction != null) {
                MutableComponent message = Component.literal("")
                        .append(Component.literal(LangManager.tr(TrKeys.DISCONNECT_BANNED_TITLE) + "\n").withStyle(ChatFormatting.RED))
                        .append(Component.literal(LangManager.tr(TrKeys.DISCONNECT_BANNED_ID)).withStyle(ChatFormatting.RED))
                        .append(Component.literal(sanction.id() + "\n").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(LangManager.tr(TrKeys.DISCONNECT_BANNED_REASON)).withStyle(ChatFormatting.RED))
                        .append(Component.literal(sanction.reason()).withStyle(ChatFormatting.YELLOW));
                if (sanction.expiresOn() == null) {
                    message.append(Component.literal("\n" + LangManager.tr(TrKeys.DISCONNECT_BANNED_DURATION_PERMANENT)).withStyle(ChatFormatting.RED));
                } else {
                    message.append(Component.literal("\n" + LangManager.tr(TrKeys.DISCONNECT_BANNED_DURATION_EXPIRES_IN, Map.of("duration", SanctionConfig.getOnlyDurationAsStringFromDate(sanction.expiresOn())))).withStyle(ChatFormatting.RED));
                }
                if (sanction.status().equals(AppealStatus.NOT_ALLOWED) || !DiscordBot.enabled) {
                    message.append(Component.literal("\n" + LangManager.tr(TrKeys.DISCONNECT_BANNED_APPEAL_NOT_ALLOWED)).withStyle(ChatFormatting.YELLOW));
                } else {
                    message.append(Component.literal("\n" + LangManager.tr(TrKeys.DISCONNECT_BANNED_APPEAL_LINK, Map.of("link", Config.invite_link))).withStyle(ChatFormatting.YELLOW));
                }

                cir.setReturnValue(message);
                cir.cancel();
            }
        }
    }
}