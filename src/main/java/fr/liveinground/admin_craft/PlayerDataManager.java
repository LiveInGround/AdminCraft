package fr.liveinground.admin_craft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.liveinground.admin_craft.ips.PlayerIPSData;
import fr.liveinground.admin_craft.mutes.PlayerMuteData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MUTE_FILE_NAME = "mutes.json";
    private static final String IPS_FILE_NAME = "ips.json";
    private static final String STAFF_MODE_DATA = "staff_mode.json";
    private static final String WORLD_CHANGES_DATABASE_FILE = "world_changes.db";

    private final Path mute_data_file;
    private final Path ips_data_file;
    private final Path staff_mode_data_file;

    private final List<PlayerMuteData> muteEntries = new ArrayList<>();
    private final List<PlayerIPSData> ipsEntries = new ArrayList<>();


    public PlayerDataManager(Path worldPath) {
        this.mute_data_file = worldPath.resolve(MUTE_FILE_NAME);
        this.ips_data_file = worldPath.resolve(IPS_FILE_NAME);
        this.staff_mode_data_file = worldPath.resolve(STAFF_MODE_DATA);

        load(false);
    }

    public List<PlayerMuteData> getEntries() {
        return muteEntries;
    }

    public void addMuteEntry(PlayerMuteData entry) {
        muteEntries.add(entry);
        AdminCraft.mutedPlayersUUID.add(entry.uuid);
    }

    public void removeMuteEntry(PlayerMuteData entry) {
        muteEntries.remove(entry);
        AdminCraft.mutedPlayersUUID.remove(entry.uuid);
    }

    public void removeIPEntry(PlayerIPSData entry) {
        ipsEntries.remove(entry);
    }

    public void load(boolean fileNotFound) {
        // Mute system
        if (Files.exists(mute_data_file)) {
            try (Reader reader = Files.newBufferedReader(mute_data_file)) {
                Type type = new TypeToken<List<PlayerMuteData>>(){}.getType();
                List<PlayerMuteData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    muteEntries.clear();
                    muteEntries.addAll(loaded);
                }
            } catch (IOException e) {
                System.err.println("Failed to load mutes datas: " + e.getMessage());
            }
        } else {
            if (fileNotFound) {
                System.err.println("Failed to create mutes storage");
                return;
            } else {
                // fileNotFound = true;
                try {
                    Files.writeString(mute_data_file, "[]", StandardOpenOption.CREATE_NEW);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (Files.exists(ips_data_file)) {
            try (Reader reader = Files.newBufferedReader(ips_data_file)) {
                Type type = new TypeToken<List<PlayerIPSData>>(){}.getType();
                List<PlayerIPSData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    ipsEntries.clear();
                    ipsEntries.addAll(loaded);
                }
            } catch (IOException e) {
                System.err.println("Failed to load IPS datas: " + e.getMessage());
            }
        } else {
            // fileNotFound = true;
            try {
                Files.writeString(ips_data_file, "[]", StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // todo: ips storage (alt accounts easy detection)


        // todo: staff mode data storage

        // todo: create the db system for world logging

    }

    public PlayerIPSData getPlayerIPSDataByUUID (String playerUUID) {
        for (PlayerIPSData data: ipsEntries) {
            if (data.uuid.equals(playerUUID)) {
                return data;
            }
        }
        return null;
    }

    public List<PlayerIPSData> getPlayerIPSDataByIP (String ip) {
        List<PlayerIPSData> datas = new ArrayList<>();
        for (PlayerIPSData data: ipsEntries) {
            if (data.ip.equals(ip)) {
                datas.add(data);
            }
        }
        return datas;
    }

    public PlayerMuteData getPlayerMuteDataByName (String playerName) {
        for (PlayerMuteData data: muteEntries) {
            if (data.name.equals(playerName)) {
                return data;
            }
        }
        return null;
    }

    public PlayerMuteData getPlayerMuteDataByUUID (String playerUUID) {
        for (PlayerMuteData data: muteEntries) {
            if (data.uuid.equals(playerUUID)) {
                return data;
            }
        }
        return null;
    }

    public void addIPSData (String name, String uuid, String ips) {
        ipsEntries.add(new PlayerIPSData(name, uuid, ips));
    }

    public void save() {
        try {
            try (Writer writer = Files.newBufferedWriter(mute_data_file)) {
                GSON.toJson(muteEntries, writer);
            }
            try (Writer writer = Files.newBufferedWriter(ips_data_file)) {
                GSON.toJson(ipsEntries, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save datas: " + e.getMessage());
        }
    }

    // todo: ips storage (alt accounts easy detection)


    // todo: staff mode data storage

    // todo: create the db system for world logging
}
