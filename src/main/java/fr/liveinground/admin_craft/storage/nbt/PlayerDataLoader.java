package fr.liveinground.admin_craft.storage.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDataLoader {

    @Nullable
    private static CompoundTag loadOfflinePlayerData(ServerLevel level, UUID uuid) throws IOException {
        File playerDataDir = level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File file = new File(playerDataDir, uuid.toString() + ".dat");

        if (!file.exists()) return null;

        return NbtIo.readCompressed(file);
    }

    private static SimpleContainer loadInventoryFromNBT(CompoundTag tag) {
        SimpleContainer inv = new SimpleContainer(36);

        ListTag list = tag.getList("Inventory", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag item = (CompoundTag) t;
            int slot = item.getByte("Slot") & 255;
            if (slot < inv.getContainerSize()) {
                inv.setItem(slot, ItemStack.of(item));
            }
        }
        return inv;
    }

    @Nullable
    public static SimpleContainer loadInventoryFromUUID(ServerLevel level, UUID uuid) throws IOException {
        CompoundTag tag = loadOfflinePlayerData(level, uuid);
        if (tag == null) return null;
        else return loadInventoryFromNBT(tag);
    }

    private static SimpleContainer loadEnderChestFromNBT(CompoundTag tag) {
        SimpleContainer ec = new SimpleContainer(27);

        ListTag list = tag.getList("EnderItems", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag item = (CompoundTag) t;
            int slot = item.getByte("Slot") & 255;

            if (slot < ec.getContainerSize()) {
                ec.setItem(slot, ItemStack.of(item));
            }
        }
        return ec;
    }

    @Nullable
    public static SimpleContainer loadEnderChestFromUUID(ServerLevel level, UUID uuid) throws IOException {
        CompoundTag tag = loadOfflinePlayerData(level, uuid);
        if (tag == null) return null;
        else return loadEnderChestFromNBT(tag);
    }

    @Nullable
    public static List<String> listOfflineTags(ServerLevel level, UUID uuid) throws IOException {
        File playerDataDir = level.getServer()
                .getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .toFile();

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (!file.exists()) return null;

        CompoundTag root = NbtIo.readCompressed(file);
        ListTag list;

        if (root.contains("Tags", Tag.TAG_LIST)) {
            list = root.getList("Tags", Tag.TAG_STRING);
        } else {
            list = new ListTag();
        }

        List<String> tagStringArray = new ArrayList<>();
        for (Tag t: list) {
            tagStringArray.add(t.getAsString());
        }
        return tagStringArray;
    }

    @Nullable
    public static Vec3 getOfflineLocation(ServerLevel level, UUID uuid) throws  IOException {
        File playerDataDir = level.getServer()
                .getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .toFile();

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (!file.exists()) return null;

        CompoundTag root = NbtIo.readCompressed(file);

        ListTag list;
        if (root.contains("Pos", Tag.TAG_DOUBLE)) {
            list = root.getList("Pos", Tag.TAG_DOUBLE);
        } else return null;

        return new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
    }
}
