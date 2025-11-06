package fr.liveinground.admin_craft.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.storage.types.*;
import fr.liveinground.admin_craft.storage.types.reports.PlayerReportsData;
import fr.liveinground.admin_craft.storage.types.reports.ReportData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionData;
import fr.liveinground.admin_craft.storage.types.tools.PlayerHistoryData;
import fr.liveinground.admin_craft.storage.types.tools.PlayerIPSData;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String ROOT = "AdminCraft_Storage";

    private static final String MUTE_FILE_NAME = "mutes.json";
    private static final String IPS_FILE_NAME = "ips.json";
    // private static final String STAFF_MODE_DATA = "staff_mode.json";
    private static final String SANCTION_HISTORY = "sanction_history.json";
    // private static final String WORLD_CHANGES_DATABASE_FILE = "world_changes.db";
    private static final String REPORTS = "reports.json";

    private final Path mute_data_file;
    private final Path ips_data_file;
    private final Path sanction_history_file;
    // private final Path staff_mode_data_file;
    private final Path reports_data_file;

    private final List<PlayerMuteData> muteEntries = new ArrayList<>();
    private final List<PlayerIPSData> ipsEntries = new ArrayList<>();
    private final List<PlayerHistoryData> historyEntries = new ArrayList<>();
    private final List<PlayerReportsData> reportsEntries = new ArrayList<>();

    public PlayerDataManager(Path worldPath) {
        Path rootDir = worldPath.resolve(ROOT);
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate root dir : " + rootDir, e);
        }

        this.mute_data_file = worldPath.resolve(ROOT).resolve(MUTE_FILE_NAME);
        this.ips_data_file = worldPath.resolve(ROOT).resolve(IPS_FILE_NAME);
        // this.staff_mode_data_file = worldPath.resolve(ROOT).resolve(STAFF_MODE_DATA);
        this.sanction_history_file = worldPath.resolve(ROOT).resolve(SANCTION_HISTORY);
        this.reports_data_file = worldPath.resolve(ROOT).resolve(REPORTS);

        createIfAbsent(mute_data_file);
        createIfAbsent(ips_data_file);
        createIfAbsent(sanction_history_file);
        createIfAbsent(reports_data_file);

        load();
    }

    private void createIfAbsent(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.writeString(path, "[]", StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate file: " + path, e);
        }
    }

    public List<PlayerHistoryData> getHistoryEntries() {
        return historyEntries;
    }

    public PlayerHistoryData getHistoryFromUUID(String playerUUID) {
        for (PlayerHistoryData data: historyEntries) {
            if (data.uuid.equals(playerUUID)) return data;
        }
        return null;
    }

    public void addHistoryEntry (String uuid, List<SanctionData> sanctionList) {
        PlayerHistoryData playerData = new PlayerHistoryData(uuid, sanctionList);
        boolean exists = false;
        for (PlayerHistoryData d: historyEntries) {
            if (d.uuid.equals(uuid)) {
                playerData.sanctionList.addAll(d.sanctionList);
            }
        }
        if (!exists) {
            historyEntries.add(playerData);
        }
    }

    public void removeHistoryEntry (String uuid) {
        historyEntries.remove(getHistoryFromUUID(uuid));
    }

    public List<PlayerMuteData> getMuteEntries() {
        return muteEntries;
    }

    public void addMuteEntry(PlayerMuteData entry) {
        Sanction s;
        if (entry.expiresOn == null) {
            s = Sanction.MUTE;
        } else {
            s = Sanction.TEMPMUTE;
        }
        addSanction(entry.uuid, s, entry.reason, entry.expiresOn);
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

    public void load() {
        // Mute system
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

        // ips system
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

        // sanctions system
        try (Reader reader = Files.newBufferedReader(sanction_history_file)) {
            Type type = new TypeToken<List<PlayerHistoryData>>(){}.getType();
            List<PlayerHistoryData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                historyEntries.clear();
                historyEntries.addAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Failed to load history datas: " + e.getMessage());
        }

        // report system
        try (Reader reader = Files.newBufferedReader(reports_data_file)) {
            Type type = new TypeToken<List<PlayerReportsData>>(){}.getType();
            List<PlayerReportsData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                reportsEntries.clear();
                reportsEntries.addAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Failed to load report datas: " + e.getMessage());
        }

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

    @Nullable
    public PlayerMuteData getPlayerMuteDataByUUID (String playerUUID) {
        for (PlayerMuteData data: muteEntries) {
            if (data.uuid.equals(playerUUID)) {
                return data;
            }
        }
        return null;
    }

    public void addSanction(String uuid, Sanction type, String reason, @Nullable Date expiresOn) {
        SanctionData d = new SanctionData(type, reason, new Date(), expiresOn);
        PlayerHistoryData history = getHistoryFromUUID(uuid);
        List<SanctionData> sanctionDataList;
        if (history == null) {
            sanctionDataList = new ArrayList<>();
            sanctionDataList.add(d);
        } else {
            sanctionDataList = history.sanctionList;
            sanctionDataList.add(d);
            removeHistoryEntry(uuid);
        }
        addHistoryEntry(uuid, sanctionDataList);
    }

    public void addIPSData(String name, String uuid, String ips) {
        ipsEntries.add(new PlayerIPSData(name, uuid, ips));
    }

    @Nullable
    public PlayerReportsData getReportDatasByUUID(String uuid) {
        for (PlayerReportsData d: reportsEntries) {
            if (d.playerUUID().equals(uuid)) {
                return d;
            }
        }
        return null;
    }

    public void addReport(String targetUUID, String sourceUUID, String reason) {
        ReportData data = new ReportData(targetUUID, sourceUUID, reason, new Date());
        PlayerReportsData d = getReportDatasByUUID(targetUUID);
        if (d == null) {
            List<ReportData> l = new ArrayList<>();
            l.add(data);
            d = new PlayerReportsData(targetUUID, l);
            reportsEntries.add(d);
        } else {
            d.reports().add(data);
        }
    }
    
    public void save() {
        try {
            try (Writer writer = Files.newBufferedWriter(mute_data_file)) {
                GSON.toJson(muteEntries, writer);
            }
            try (Writer writer = Files.newBufferedWriter(ips_data_file)) {
                GSON.toJson(ipsEntries, writer);
            }
            try (Writer writer = Files.newBufferedWriter(sanction_history_file)) {
                GSON.toJson(historyEntries, writer);
            }
            try (Writer writer = Files.newBufferedWriter(reports_data_file)) {
                GSON.toJson(reports_entries, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save datas: " + e.getMessage());
        }
    }

    // todo: ips storage (alt accounts easy detection)


    // todo: staff mode data storage

    // todo: create the db system for world logging
}
