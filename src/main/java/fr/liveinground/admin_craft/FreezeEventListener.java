package fr.liveinground.admin_craft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class FreezeEventListener {
    private static final Vec3 ZERO = new Vec3(0, 0, 0);

    @SubscribeEvent
    public static void onPlayerTickEvent(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (AdminCraft.frozenPlayersUUID.contains(player.getStringUUID())) {
            if (!player.getDeltaMovement().equals(ZERO)) {
                player.setDeltaMovement(ZERO);
                double centerX = Math.floor(player.getX()) + 0.5;
                double centerY = Math.floor(player.getY());
                double centerZ = Math.floor(player.getZ()) + 0.5;
                player.teleportTo(centerX, centerY, centerZ);
            }
            player.setYHeadRot(player.yHeadRotO);
            player.setYRot(player.yRotO);
            player.setXRot(player.xRotO);
        }
    }
}
