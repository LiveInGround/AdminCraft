package fr.liveinground.admin_craft;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class FreezeEventListener {
    private static final Vec3 ZERO = new Vec3(0, 0, 0);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (AdminCraft.frozenPlayersUUID.contains(player.getStringUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && AdminCraft.frozenPlayersUUID.contains(player.getStringUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemTeleport(EntityTeleportEvent.ItemConsumption event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && AdminCraft.frozenPlayersUUID.contains(player.getStringUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommandTeleport(EntityTeleportEvent.TeleportCommand event) {
        Entity entity = event.getEntity();
        if (Config.freeze_allow_ext_tp && entity instanceof Player player && AdminCraft.frozenPlayersUUID.contains(player.getStringUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpreadTeleport(EntityTeleportEvent.SpreadPlayersCommand event) {
        Entity entity = event.getEntity();
        if (Config.freeze_allow_ext_tp && entity instanceof Player player && AdminCraft.frozenPlayersUUID.contains(player.getStringUUID())) {
            event.setCanceled(true);
        }
    }


}
