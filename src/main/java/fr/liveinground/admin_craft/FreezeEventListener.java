package fr.liveinground.admin_craft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FreezeEventListener {
    private static final Vec3 nullDeltaMovement = new Vec3(0,0,0);

    @SubscribeEvent
    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent e) {
        if (e.phase.equals(TickEvent.Phase.END)) {
            Player player = e.player;
            if (AdminCraft.frozenPlayersUUID.contains(player.getStringUUID())) {
                Vec3 delta = player.getDeltaMovement();
                if (!delta.equals(nullDeltaMovement)) {
                    player.setDeltaMovement(nullDeltaMovement);
                    Vec3 currentPos = player.position();
                    Vec3 lastStablePos = new Vec3(player.getOnPos().getX(), player.getOnPos().getY(), player.getOnPos().getZ());

                    if (!currentPos.equals(lastStablePos)) {
                        player.teleportTo(lastStablePos.x, lastStablePos.y, lastStablePos.z);
                    }
                }
                player.setYHeadRot(player.yHeadRotO);
                player.setYRot(player.yRotO);
                player.setXRot(player.xRotO);
            }
        }
    }
}
