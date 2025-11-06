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
            at = @At("RETURN")
    )
    private void onRespawn(ServerPlayer player, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerLevel overworld = player.getServer().overworld();
        BlockPos current = player.getRespawnPosition();
        BlockPos defaultSpawn = overworld.getSharedSpawnPos();

        if ((current == null || current.equals(defaultSpawn)) && Config.spawn_override) {
            player.setRespawnPosition(
                    Level.OVERWORLD,
                    new BlockPos(Config.spawn_x, Config.spawn_y, Config.spawn_z),
                    0f,
                    true,
                    false
            );
        }
        cir.setReturnValue(player);
    }
}
