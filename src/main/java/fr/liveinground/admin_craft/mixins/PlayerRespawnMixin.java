package fr.liveinground.admin_craft.mixins;

import fr.liveinground.admin_craft.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public class PlayerRespawnMixin {
    @Inject(
            method = "respawn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRespawn(ServerPlayer player, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
        if (!Config.spawn_override) return;
        ServerLevel overworld = player.getServer().overworld();

        BlockPos configuredSpawn = new BlockPos(Config.spawn_x, Config.spawn_y, Config.spawn_z);

        BlockPos respawnPosition = player.getRespawnPosition();
        boolean hasResp = (respawnPosition != null && !respawnPosition.equals(overworld.getSharedSpawnPos()));

        if (!hasResp) {
            if (!overworld.getSharedSpawnPos().equals(configuredSpawn)) {
                overworld.setDefaultSpawnPos(configuredSpawn, 0);
            }

            player.setRespawnPosition(Level.OVERWORLD, configuredSpawn, 0f, true, false);

            cir.setReturnValue(player);
        }
    }
}
