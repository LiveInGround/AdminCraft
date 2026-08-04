package fr.liveinground.admin_craft.storage.nbt;

import fr.liveinground.admin_craft.AdminCraft;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class PlayerDataSaver {
    public static void applyToOnlinePlayer(SimpleContainer container, ServerPlayer player) {
        Inventory inv = player.getInventory();
        inv.clearContent();

        for (int i = 0; i < container.getContainerSize(); i++) {
            inv.setItem(i, container.getItem(i));
        }

        player.inventoryMenu.broadcastChanges();
    }

    public static void saveToNBT(UUID targetUuid, ServerLevel level, SimpleContainer container) {
        try {
            PlayerDataSaver.saveInventory(level, targetUuid, container);
        } catch (IOException e) {
            AdminCraft.LOGGER.error("Failed to save offline inventory", e);
        }
    }

    public static void saveInventory(ServerLevel level, UUID uuid, SimpleContainer container) throws IOException {
        RegistryOps<Tag> registryOps = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

        File playerDataDir = level.getServer()
                .getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .toFile();

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (!file.exists()) return;

        CompoundTag root = NbtIo.readCompressed(
                file.toPath(),
                NbtAccounter.unlimitedHeap()
        );
        ListTag list = new ListTag();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                AdminCraft.LOGGER.debug("Skipping empty stack (inventory)");
                continue;
            }
            Tag item = ItemStack.CODEC.encodeStart(registryOps, stack).result().orElseThrow(() -> new IllegalStateException("Failed to encode item " + stack.getItem() + " (slot " + stack.getEquipmentSlot() + ")"));
            AdminCraft.LOGGER.debug("Encored inventory ItemStack into tag: {}", item);
            if (item instanceof CompoundTag compound) {
                compound.putByte("Slot", (byte) i);
                list.add(compound);
            } else {
                AdminCraft.LOGGER.error("Encoding issue (inventory), item encoding for slot {} hasn't returned a CompoundTag.", stack.getEquipmentSlot());
                AdminCraft.LOGGER.error("This item stack (inventory) is lost: {}", stack);
            }
        }

        root.put("Inventory", list);
        NbtIo.writeCompressed(root, file.toPath());
    }

    public static void applyEchestToOnlinePlayer(SimpleContainer container, ServerPlayer player) {
        PlayerEnderChestContainer inv = player.getEnderChestInventory();
        inv.clearContent();

        for (int i = 0; i < container.getContainerSize(); i++) {
            inv.setItem(i, container.getItem(i));
        }

        player.inventoryMenu.broadcastChanges();
    }

    public static void saveEchestToNBT(UUID targetUuid, ServerLevel level, SimpleContainer container) {
        try {
            PlayerDataSaver.saveEnderChestInventory(level, targetUuid, container);
        } catch (IOException e) {
            AdminCraft.LOGGER.error("Failed to save offline inventory", e);
        }
    }
    public static void saveEnderChestInventory(ServerLevel level, UUID uuid, SimpleContainer container) throws IOException {
        RegistryOps<Tag> registryOps = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

        File playerDataDir = level.getServer()
                .getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .toFile();

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (!file.exists()) return;

        CompoundTag root = NbtIo.readCompressed(
                file.toPath(),
                NbtAccounter.unlimitedHeap()
        );
        ListTag list = new ListTag();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                AdminCraft.LOGGER.debug("Skipping empty stack (enderchest)");
                continue;
            }
            Tag item = ItemStack.CODEC.encodeStart(registryOps, stack).result().orElseThrow(() -> new IllegalStateException("Failed to encode item " + stack.getItem() + " (slot " + stack.getEquipmentSlot() + ")"));
            AdminCraft.LOGGER.debug("Encored enderchest ItemStack into tag: {}", item);
            if (item instanceof CompoundTag compound) {
                compound.putByte("Slot", (byte) i);
                list.add(compound);
            } else {
                AdminCraft.LOGGER.error("Encoding issue (enderchest), item encoding for slot {} hasn't returned a CompoundTag.", stack.getEquipmentSlot());
                AdminCraft.LOGGER.error("This item stack (enderchest) is lost: {}", stack);
            }
            root.put("EnderItems", list);
            NbtIo.writeCompressed(root, file.toPath());
        }
    }

    public static int addOfflineTag(ServerLevel level, UUID uuid, String tag) throws IOException {
        File playerDataDir = level.getServer()
                .getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .toFile();

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (!file.exists()) return 404;

        CompoundTag root = NbtIo.readCompressed(
                file.toPath(),
                NbtAccounter.unlimitedHeap()
        );
        ListTag list;
        Optional<ListTag> olt = root.getList("Tags");
        if (root.contains("Tags") && olt.isPresent()) {
            list = olt.get();
        } else {
            list = new ListTag();
        }

        for (int i = 0; i < list.size(); i++) {
            Optional<String> os = list.getString(i);
            if (os.isPresent() && os.get().equalsIgnoreCase(tag)) {
                return 409;
            }
        }

        list.add(StringTag.valueOf(tag));
        root.put("Tags", list);

        NbtIo.writeCompressed(root, file.toPath());

        return 200;
    }

    public static int removeOfflineTag(ServerLevel level, UUID uuid, String tag) throws IOException {
        File playerDataDir = level.getServer()
                .getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .toFile();

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (!file.exists()) return 404;

        CompoundTag root = NbtIo.readCompressed(
                file.toPath(),
                NbtAccounter.unlimitedHeap()
        );

        Optional<ListTag> olt = root.getList("Tags");
        if (!root.contains("Tags") || olt.isEmpty()) {
            return 409;
        } else {
            ListTag list = root.getList("Tags").get();  // (ignore "'Optional.get()' without 'isPresent()' check" warning)

            boolean removed = false;
            for (int i = 0; i < list.size(); i++) {
                Optional<String> os = list.getString(i);
                if (os.isPresent() && os.get().equalsIgnoreCase(tag)) {
                    list.remove(i);
                    removed = true;
                    break;
                }
            }

            if (!removed) {
                return 409;
            }

            root.put("Tags", list);

            NbtIo.writeCompressed(root, file.toPath());

            return 200;}
    }

    public static void setOfflineLocation(ServerLevel level, UUID uuid, Vec3 position) throws IOException {
        File playerDataDir = level.getServer()
                .getWorldPath(LevelResource.PLAYER_DATA_DIR)
                .toFile();

        File file = new File(playerDataDir, uuid.toString() + ".dat");
        if (!file.exists()) return;

        CompoundTag root = NbtIo.readCompressed(
                file.toPath(),
                NbtAccounter.unlimitedHeap()
        );
        ListTag newPos = new ListTag();
        newPos.add(DoubleTag.valueOf(position.x));
        newPos.add(DoubleTag.valueOf(position.y));
        newPos.add(DoubleTag.valueOf(position.z));

        root.put("Pos", newPos);

        NbtIo.writeCompressed(root, file.toPath());
    }

}
