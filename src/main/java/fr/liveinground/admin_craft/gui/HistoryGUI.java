package fr.liveinground.admin_craft.gui;

import fr.liveinground.admin_craft.PlaceHolderSystem;
import fr.liveinground.admin_craft.moderation.PlayerHistoryData;
import fr.liveinground.admin_craft.moderation.SanctionConfig;
import fr.liveinground.admin_craft.moderation.SanctionData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

public class HistoryGUI {
    public static void open(ServerPlayer player, PlayerHistoryData data, String name) {
        SimpleContainer inventory = new SimpleContainer(54);

        for (SanctionData d: data.sanctionList) {
            Item item;
            String sanction = d.sanctionType.name();
            String expires = SanctionConfig.getDurationAsStringFromDate(d.expiresOn);
            String reason = d.reason;
            String date = SanctionConfig.getDurationAsStringFromDate(d.date);
            switch (d.sanctionType) {
                case WARN:
                    item = Items.FEATHER;
                    break;
                case TEMPMUTE:
                    item = Items.ACACIA_SIGN;
                    break;
                case MUTE:
                    item = Items.MANGROVE_SIGN;
                    break;
                case TEMPBAN:
                    item = Items.DIAMOND_SWORD;
                    break;
                case BAN:
                    item = Items.NETHERITE_SWORD;
                    break;
                case KICK:
                    item = Items.IRON_SWORD;
                    break;
                default:
                    item = Items.BARRIER;
                    sanction = "INTERNAL ERROR";
                    expires = "N/A";
                    date = "N/A";
                    reason = "N/A";
            }
            ItemStack stack = new ItemStack(item);
            stack.setHoverName(Component.literal(sanction).withStyle(ChatFormatting.YELLOW));
            ListTag lore = new ListTag();
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Reason: " + reason).withStyle(ChatFormatting.RED))));
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Date: " + date).withStyle(ChatFormatting.RED))));
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Expires: " + expires).withStyle(ChatFormatting.RED))));
            CompoundTag display = item.getOrCreateTagElement("display");
            display.put("Lore", lore);
        }


        player.openMenu(new SimpleMenuProvider((id, inv, ply) ->
                new ChestMenu(MenuType.GENERIC_9x6, id, inv, inventory, 6),
                Component.literal(PlaceHolderSystem.replacePlaceholders("%player%'s history", Map.of("player", name))))
        );

    }
}
