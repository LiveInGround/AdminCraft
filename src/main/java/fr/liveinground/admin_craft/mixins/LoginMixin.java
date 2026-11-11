package fr.liveinground.admin_craft.mixins;

import fr.liveinground.admin_craft.Config;
import fr.liveinground.admin_craft.discord.DiscordBot;
import fr.liveinground.admin_craft.lang.LangManager;
import fr.liveinground.admin_craft.lang.TrKeys;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
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
                String trKey_header;
                if (sanction.expiresOn() == null) {
                    trKey_header = TrKeys.SANCTION_BANNED_PERM;
                } else {
                    trKey_header = TrKeys.SANCTION_BANNED_TEMP;
                }
                MutableComponent message = Component.literal("")
                        .append(LangManager.parse(trKey_header, Map.of(
                                "id", sanction.id(),
                                "reason", sanction.reason(),
                                "duration", SanctionConfig.getOnlyDurationAsStringFromDate(sanction.expiresOn())
                        )));
                message.append(Component.literal("\n"));

                if (!sanction.canBeAppealed() || !DiscordBot.enabled) {
                    message.append(LangManager.parse(TrKeys.SANCTION_NOAPPEAL));
                } else {
                    message.append(LangManager.parse(TrKeys.SANCTION_APPEALABLE, Map.of(
                            "link", Config.invite_link
                    )));
                }

                cir.setReturnValue(message);
                cir.cancel();
            }
        }
    }
}