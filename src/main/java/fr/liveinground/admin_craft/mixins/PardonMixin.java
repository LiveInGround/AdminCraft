package fr.liveinground.admin_craft.mixins;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.storage.Database;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Date;
import java.util.Optional;

@Mixin(UserBanList.class)
public class PardonMixin {

    @Inject(method = "remove(Lnet/minecraft/server/players/NameAndId;)Z", at = @At("HEAD"))
    private void onPardon(NameAndId p_443373_, CallbackInfoReturnable<Boolean> cir) {
        UserBanList self = (UserBanList)(Object)this;
        UserBanListEntry entry = self.get(p_443373_);
        Date now = new Date();
        Optional<DatabaseSanctionData> banData;

        if (entry != null) {
            AdminCraft.LOGGER.debug("not null");
            String reason = entry.getReason();
            Date created = entry.getCreated();
            AdminCraft.LOGGER.debug("Reason: {}; created: {}", reason, created);
            banData = Database.getCurrentSanctions(p_443373_.id().toString()).stream().filter(
                    data -> data.type().equals(Sanction.BAN)
                            && data.reason().equals(reason)
                            && Math.abs(data.date().getTime() - created.getTime()) < 5000   // avoid offset caused by a system lag
            ).findFirst();
        } else {
            banData = Database.getCurrentSanctions(p_443373_.id().toString()).stream().filter(
                    data -> data.type().equals(Sanction.BAN)
            ).findFirst();
        }
        if (banData.isEmpty()) {
            AdminCraft.LOGGER.warn("Couldn't find any suitable sanction in the database corresponding to {}'s ban, skipping database change while unbanning.", p_443373_.name());
        } else {
            Database.removeSanction(banData.get().id());
        }
    }
}