package fr.liveinground.admin_craft.mixins;

import fr.liveinground.admin_craft.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class SpawnRadiusMixin {
    @Inject(
            method = "getSpawnRadius",
            at = @At("HEAD"),
            cancellable = true)
    private void getSpawnRadius(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        if (Config.spawn_override) {
            cir.setReturnValue(0);
        }

    }
}
