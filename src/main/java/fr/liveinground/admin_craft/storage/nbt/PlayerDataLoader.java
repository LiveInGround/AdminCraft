package fr.liveinground.admin_craft.storage.nbt;

import fr.liveinground.admin_craft.AdminCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerDataLoader {

    @Nullable
    private static CompoundTag loadOfflinePlayerData(ServerLevel level, UUID uuid) throws IOException {
        File playerDataDir = level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File file = new File(playerDataDir, uuid.toString() + ".dat");

        if (!file.exists()) return null;

        return NbtIo.readCompressed(
                file.toPath(),
                NbtAccounter.unlimitedHeap()
        );
    }

    private static SimpleContainer loadInventoryFromNBT(CompoundTag tag, Level level) {
        RegistryOps<Tag> registryOps = level.registryAccess()
                .createSerializationContext(NbtOps.INSTANCE);

        SimpleContainer inv = new SimpleContainer(36);

        Optional<ListTag> olt = tag.getList("Inventory");
        ListTag list = olt.orElse(new ListTag());
        for (Tag t : list) {
            CompoundTag item = (CompoundTag) t;
            int slot = item.getByte("Slot").map(b -> b & 255).orElseThrow(() -> new IllegalStateException("Missing slot in nbt"));
            if (slot < inv.getContainerSize()) {
                ItemStack stack = ItemStack.CODEC.parse(registryOps, item).result().orElse(ItemStack.EMPTY);
                inv.setItem(slot, stack);            }
        }
        return inv;
    }

    @Nullable
    public static SimpleContainer loadInventoryFromUUID(ServerLevel level, UUID uuid) throws IOException {
        CompoundTag tag = loadOfflinePlayerData(level, uuid);
        if (tag == null) return null;
        else return loadInventoryFromNBT(tag, level);
    }

    private static SimpleContainer loadEnderChestFromNBT(CompoundTag tag, Level level) {
        RegistryOps<Tag> registryOps = level.registryAccess()
                .createSerializationContext(NbtOps.INSTANCE);

        SimpleContainer ec = new SimpleContainer(27);
        Optional<ListTag> olt = tag.getList("EnderItems");
        ListTag list = olt.orElse(new ListTag());
        for (Tag t : list) {
            CompoundTag item = (CompoundTag) t;
            int slot = item.getByte("Slot").map(b -> b & 255).orElseThrow(() -> new IllegalStateException("Missing slot in nbt"));

            if (slot < ec.getContainerSize()) {
                ItemStack stack = ItemStack.CODEC.parse(registryOps, item).result().orElse(ItemStack.EMPTY);
                ec.setItem(slot, stack);            }
        }
        return ec;
    }

    @Nullable
    public static SimpleContainer loadEnderChestFromUUID(ServerLevel level, UUID uuid) throws IOException {
        CompoundTag tag = loadOfflinePlayerData(level, uuid);
        if (tag == null) return null;
        else return loadEnderChestFromNBT(tag, level);
    }

    @Nullable
    public static List<String> listOfflineTags(ServerLevel level, UUID uuid) throws IOException {
        File playerDataDir = level.getServer()
                .getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .toFile();

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (!file.exists()) return null;

        CompoundTag root = NbtIo.readCompressed(
                file.toPath(),
                NbtAccounter.unlimitedHeap()
        );
        ListTag list;
        if (root.contains("Tags")) {
            Optional<ListTag> olt = root.getList("Tags");
            list = olt.orElse(new ListTag());
        } else {
            list = new ListTag();
        }

        List<String> tagStringArray = new ArrayList<>();
        for (Tag t: list) {
            tagStringArray.add(t.asString().orElse(""));
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

        CompoundTag root = NbtIo.readCompressed(
                file.toPath(),
                NbtAccounter.unlimitedHeap()
        );

        ListTag list;
        if (root.contains("Pos")) {
            list = root.getList("Pos").orElse(null);
        } else return null;
        if (list == null || list.isEmpty()) {
            AdminCraft.LOGGER.warn("Offline player location unavailable (UUID: {}): 'Pos' tag missing or empty. Defaulting to (0,0,0).", uuid);
            return new Vec3(0, 0, 0);

        } else {
            Optional<Double> x = list.getDouble(0);
            Optional<Double> y = list.getDouble(1);
            Optional<Double> z = list.getDouble(2);
            if (x.isPresent() && y.isPresent() && z.isPresent())
                return new Vec3(x.get(), y.get(), z.get());
            else {
                AdminCraft.LOGGER.warn("Offline player location invalid (UUID: {}): 'Pos' tag does not contain 3 valid doubles. Defaulting to (0,0,0).", uuid);
                return new Vec3(0, 0, 0);
            }
        }
    }
}
