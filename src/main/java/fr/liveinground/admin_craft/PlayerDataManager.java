package fr.liveinground.admin_craft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.liveinground.admin_craft.mutes.PlayerMuteData;

import java.io.File;
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

    private final List<PlayerMuteData> entries = new ArrayList<>();


    public PlayerDataManager(Path worldPath) {
        this.mute_data_file = worldPath.resolve(MUTE_FILE_NAME);
        this.ips_data_file = worldPath.resolve(IPS_FILE_NAME);
        this.staff_mode_data_file = worldPath.resolve(STAFF_MODE_DATA);

        load(false);
    }

    public List<PlayerMuteData> getEntries() {
        return entries;
    }

    public void addMuteEntry(PlayerMuteData entry) {
        entries.add(entry);
        AdminCraft.mutedPlayersUUID.add(entry.uuid);
    }

    public void removeMuteEntry(PlayerMuteData entry) {
        entries.remove(entry);
        AdminCraft.mutedPlayersUUID.remove(entry.uuid);
    }

    public void load(boolean fileNotFound) {
        // Mute system
        if (Files.exists(mute_data_file)) {
            try (Reader reader = Files.newBufferedReader(mute_data_file)) {
                Type type = new TypeToken<List<PlayerMuteData>>(){}.getType();
                List<PlayerMuteData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    entries.clear();
                    entries.addAll(loaded);
                }
            } catch (IOException e) {
                System.err.println("Failed to load mutes datas: " + e.getMessage());
            }
        } else {
            if (fileNotFound) {
                System.err.println("Failed to create mutes storage");
                return;
            } else {
                fileNotFound = true;
                try {
                    Files.writeString(mute_data_file, "[]", StandardOpenOption.CREATE_NEW);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        // todo: ips storage (alt accounts easy detection)


        // todo: staff mode data storage

        // todo: create the db system for world logging

    }

    public PlayerMuteData getPlayerMuteDataByName (String playerName) {
        for (PlayerMuteData data: entries) {
            if (data.name.equals(playerName)) {
                return data;
            }
        }
        return null;
    }

    public PlayerMuteData getPlayerMuteDataByUUID (String playerUUID) {
        for (PlayerMuteData data: entries) {
            if (data.uuid.equals(playerUUID)) {
                return data;
            }
        }
        return null;
    }

    public void save() {
        try {
            try (Writer writer = Files.newBufferedWriter(mute_data_file)) {
                GSON.toJson(entries, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save mutes datas: " + e.getMessage());
        }
    }

    // todo: ips storage (alt accounts easy detection)


    // todo: staff mode data storage

    // todo: create the db system for world logging
}
