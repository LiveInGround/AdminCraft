package fr.liveinground.admin_craft.storage;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.storage.types.reports.ReportData;
import fr.liveinground.admin_craft.storage.types.reports.ReportStatus;
import fr.liveinground.admin_craft.storage.types.sanction.AppealStatus;
import fr.liveinground.admin_craft.storage.types.sanction.DatabaseSanctionData;
import fr.liveinground.admin_craft.storage.types.sanction.Sanction;
import fr.liveinground.admin_craft.storage.types.sanction.SanctionData;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Database {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Path DATABASE_PATH = FMLPaths.GAMEDIR.get()
            .resolve("AdminCraft_Storage")
            .resolve("storage.db");

    private static final String URL = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();

    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");

            Files.createDirectories(DATABASE_PATH.getParent());
            if (Files.notExists(DATABASE_PATH)) {
                Files.createFile(DATABASE_PATH);
            }

        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
        return DriverManager.getConnection(URL);
    }

    private static String generateID() {
        StringBuilder builder = new StringBuilder(9);
        builder.append("#");
        for (int i = 0; i < 8; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            builder.append(CHARACTERS.charAt(index));
        }
        String id = builder.toString();
        String finalId = id;
        if (!querySanction("SELECT * FROM sanctions WHERE id = ?;", stmt -> stmt.setString(1, finalId)).isEmpty()) {
            id = generateID();
        }
        return id;
    }

    @FunctionalInterface
    private interface StatementPreparer {
        void prepare(PreparedStatement stmt) throws SQLException;
    }

    private static List<DatabaseSanctionData> querySanction(String sql, StatementPreparer preparer) {
        List<DatabaseSanctionData> results = new ArrayList<>();
        try (
                Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            preparer.prepare(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long expires = rs.getLong("expires");
                    Date expiresDate;
                    if (rs.wasNull()) {
                        expiresDate = null;
                    } else {
                        expiresDate = new Date(expires);
                    }
                    long appeal = rs.getLong("appealDate");
                    Date appealDate;
                    if (rs.wasNull()) {
                        appealDate = null;
                    } else {
                        appealDate = new Date(appeal);
                    }

                    DatabaseSanctionData data = new DatabaseSanctionData(
                            rs.getString("id"),
                            rs.getString("uuid"),
                            rs.getString("ign"),
                            Sanction.valueOf(rs.getString("type")),
                            rs.getString("reason"),
                            new Date(rs.getLong("date")),
                            expiresDate,
                            AppealStatus.valueOf(rs.getString("appealStatus")),
                            appealDate,
                            rs.getBoolean("removed")
                    );
                    results.add(data);
                }
            }
        } catch (SQLException e) {
            AdminCraft.LOGGER.error("Issue while posting a query to the sanction database: ", e);
        }

        return results;
    }

    private static List<ReportData> queryReports(String sql, StatementPreparer preparer) {
        List<ReportData> results = new ArrayList<>();
        try (
                Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            preparer.prepare(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String claimedUUIDStr = rs.getString("claimeduuid");
                    UUID claimedUUID;
                    if (rs.wasNull()) {
                        claimedUUID = null;
                    } else {
                        claimedUUID = UUID.fromString(claimedUUIDStr);
                    }
                    ReportData data = new ReportData(
                            rs.getInt("id"),
                            ReportStatus.valueOf(rs.getString("status")),
                            UUID.fromString(rs.getString("reporteruuid")),
                            UUID.fromString(rs.getString("reporteduuid")),
                            rs.getString("reason"),
                            new Date(rs.getLong("date")),
                            claimedUUID
                    );

                    results.add(data);
                }
            }
        } catch (SQLException e) {
            AdminCraft.LOGGER.error("Issue while posting a query to the report table: ", e);
        }

        return results;
    }

    private static boolean update(String sql, StatementPreparer preparer) {

        try (
                Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            preparer.prepare(stmt);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            AdminCraft.LOGGER.error("An issue occurred while trying to update the database: ", e);
            return false;
        }
    }

    @Nullable
    private static Exception post(String sql) {
        try (
                Connection connection = Database.connect();
                Statement statement = connection.createStatement()
        ) {
            statement.execute(sql);
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    public static void start() {
        String init = """
                CREATE TABLE IF NOT EXISTS sanctions (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    uuid TEXT NOT NULL,
                    ign TEXT,
                    reason TEXT NOT NULL,
                    date BIGINT NOT NULL,
                    expires BIGINT,
                    appealStatus TEXT NOT NULL,
                    appealDate BIGINT,
                    removed INTEGER NOT NULL DEFAULT 0
                );
                
                CREATE TABLE IF NOT EXISTS reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    status TEXT DEFAULT UNRESOLVED,
                    reporteruuid TEXT NOT NULL,
                    reporteduuid TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    date BIGINT NOT NULL,
                    claimeduuid TEXT
                );""";
        Exception ans = post(init);
        if (ans == null) {
            AdminCraft.LOGGER.info("Successfully connected to the database");
        } else {
            AdminCraft.LOGGER.error("Error while initialising the database: ", ans);
        }
    }

    public static boolean registerReport(UUID reporter, UUID reported, String reason) {
        return update("INSERT INTO reports(reporteruuid, reporteduuid, reason, date) VALUES (?, ?, ?, ?);", stmt -> {
            stmt.setString(1, reporter.toString());
            stmt.setString(2, reported.toString());
            stmt.setString(3, reason);
            stmt.setLong(4, new Date().getTime());
        });
    }

    public static List<ReportData> listReportsOf(UUID player) {
        return queryReports("SELECT * FROM reports WHERE reporteduuid = ?", stmt ->{
            stmt.setString(1, player.toString());
        });
    }

    public static List<ReportData> listReportsFrom(UUID player) {
        return queryReports("SELECT * FROM reports WHERE reporteruuid = ?", stmt ->{
            stmt.setString(1, player.toString());
        });
    }

    public static boolean updateReportStatus(int id, ReportStatus status, @Nullable UUID claimer) {
        if (status.equals(ReportStatus.CLAIMED)) {
            if (claimer == null) {
                AdminCraft.LOGGER.error("Trying to claim report " + id + " without a provided staff!");
                return false;
            }
            return update("UPDATE reports SET status = ?, claimeduuid = ? WHERE id = ?;", stmt -> {
                stmt.setString(1, status.name());
                stmt.setString(2, claimer.toString());
                stmt.setInt(3, id);
            });
        } else {
            return update("UPDATE reports SET status = ? WHERE id = ?;", stmt -> {
                stmt.setString(1, status.name());
                stmt.setInt(3, id);
            });
        }
    }

    public static boolean deleteReport(int id) {
        return update("DELETE FROM reports WHERE ID = ?;", stmt -> {
            stmt.setInt(1, id);
        });
    }

    @Nullable
    public static String registerSanction(String uuid, String ign, SanctionData data, boolean appealable, @Nullable Date appealDelay) {
        final String ID = generateID();
        boolean result = update("INSERT INTO sanctions(id, type, uuid, ign, reason, date, expires, appealStatus, appealDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);", stmt -> {
            stmt.setString(1, ID);
            stmt.setString(2, data.sanctionType.toString());
            stmt.setString(3, uuid);
            stmt.setString(4, ign);
            stmt.setString(5, data.reason);
            stmt.setLong(6, data.date.getTime());
            if (data.expiresOn != null) {
                stmt.setLong(7, data.expiresOn.getTime());
            } else {
                stmt.setNull(7, Types.BIGINT);
            }
            if (appealable) {
                if (appealDelay != null) {
                    stmt.setString(8, AppealStatus.DELAYED.toString());
                    stmt.setLong(9, appealDelay.getTime());
                } else {
                    stmt.setString(8, AppealStatus.NOT_REQUESTED.toString());
                    stmt.setNull(9, java.sql.Types.BIGINT);
                }
            } else {
                stmt.setString(8, AppealStatus.NOT_ALLOWED.toString());
                stmt.setNull(9, java.sql.Types.BIGINT);
            }

        });
        if (result) {
            return ID;
        } else {
            return null;
        }
    }

    public static boolean sanctionDoesntExists(String id, String ign) {
        return querySanction(
                "SELECT * FROM sanctions WHERE id = ? AND ign = ?;",
                stmt -> {
                    stmt.setString(1, id);
                    stmt.setString(2, ign);
                }).isEmpty();
    }

    public static @Nullable AppealStatus getAppealStatus(String id) {
        DatabaseSanctionData data = querySanction(
                "SELECT * FROM sanctions WHERE id = ?;",
                stmt -> stmt.setString(1, id)
        ).stream().findFirst().orElse(null);
        if (data == null) return null;
        else return data.status();
    }

    @Nullable
    public static Date getAppealDelay(String id) {
        AppealStatus status = getAppealStatus(id);
        if (status == null || !status.equals(AppealStatus.DELAYED)) return null;
        DatabaseSanctionData data = querySanction("SELECT * FROM sanctions WHERE id = ?;",
                stmt -> stmt.setString(1, id)).stream().findFirst().orElse(null);
        if (data == null) return null;
        return data.appealDelay();
    }

    public static boolean changeAppealStatus(String id, AppealStatus status) {
        return update("UPDATE sanctions SET appealStatus = ? WHERE id = ?;",
                stmt -> {
            stmt.setString(1, status.toString());
            stmt.setString(2, id);
                });
    }

    @Nullable
    public static DatabaseSanctionData getSanctionData(String id, String ign) {
        return querySanction("SELECT * FROM sanctions WHERE id = ? AND ign = ?;",
                stmt -> {
                    stmt.setString(1, id);
                    stmt.setString(2, ign);
                }).stream().findFirst().orElse(null);
    }

    @Nullable
    public static DatabaseSanctionData getSanctionData(String id) {
        return querySanction("SELECT * FROM sanctions WHERE id = ?;",
                stmt -> stmt.setString(1, id)).stream().findFirst().orElse(null);
    }

    public static List<DatabaseSanctionData> getCurrentSanctions(String uuid) {
        Date now = new Date();
        return querySanction(
                "SELECT * FROM sanctions WHERE uuid = ? AND removed = 0;",
                stmt -> stmt.setString(1, uuid)
        ).stream()
                .filter(databaseSanctionData -> !databaseSanctionData.hasExpired(now))
                .toList();
    }

    public static List<DatabaseSanctionData> getHistory(String uuid) {
        return querySanction(
                "SELECT * FROM sanctions WHERE uuid = ?;",
                stmt -> stmt.setString(1, uuid)
        );
    }

    public static boolean editDuration(String id, @Nullable Date newDuration) {
        return update("UPDATE sanctions SET expires = ? WHERE id = ?;", stmt -> {
            if (newDuration == null) {
                stmt.setNull(1, Types.BIGINT);
            } else {
                stmt.setLong(1, newDuration.getTime());
            }
            stmt.setString(2, id);
        });
    }

    public static boolean editAppealDelay(String id, @NotNull Date newDelay) {
        return update("UPDATE sanctions SET appealDate = ? WHERE id = ?;", stmt -> {
            stmt.setLong(1, newDelay.getTime());
            stmt.setString(2, id);
        });
    }

    public static List<String> listIDs() {
        List<String> ids = new ArrayList<>();
        List<DatabaseSanctionData> sd = querySanction("SELECT * FROM sanctions;", stmt -> {});
        for (DatabaseSanctionData d: sd) {
            ids.add(d.id());
        }
        return ids;
    }

    public static boolean removeSanction(String id) {
        return update("UPDATE sanctions SET removed = 1 WHERE id = ?;", stmt -> {
            stmt.setString(1, id);
        });
    }
}
