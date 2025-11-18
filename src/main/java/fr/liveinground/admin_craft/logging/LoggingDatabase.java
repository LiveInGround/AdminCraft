package fr.liveinground.admin_craft.logging;

import fr.liveinground.admin_craft.AdminCraft;
import fr.liveinground.admin_craft.storage.PlayerDataManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoggingDatabase {
    private static final String URL = "jdbc:sqlite:" + PlayerDataManager.ROOT + "world_logger.db";
    private static final ExecutorService DATABASE_EXECUTOR =
            Executors.newSingleThreadExecutor();

    public static void start() {
        try (Connection connection = DriverManager.getConnection(URL)) {
            if (connection != null) {
                AdminCraft.LOGGER.info("Connected to logger's database");
            }
        } catch (SQLException e) {
            AdminCraft.LOGGER.warn("An issue occurred while trying to start the world logger: ", e);
        }
    }

    private static void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS world_changes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type INTEGER," +
                "x INTEGER," +
                "y INTEGER" +
                "z INTEGER" +
                "material INTEGER" +
                "timestamp INTEGER)";

        asyncPostCommandToDB(sql);
    }

    private static void asyncPostCommandToDB(@NotNull String command, @Nullable String[] args) {
        getDatabaseExecutor().submit(() -> {
            try (Connection conn = DriverManager.getConnection(URL);
                 PreparedStatement stmt = conn.prepareStatement(command)) {

                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        stmt.setString(i + 1, args[i]);
                    }
                }

                stmt.executeUpdate();

            } catch (SQLException e) {
                AdminCraft.LOGGER.error("Failed to run database command: ", e);
            }
        });
    }

    private static void asyncPostCommandToDB(@NotNull String command) {
        getDatabaseExecutor().submit(() -> {
            try (Connection conn = DriverManager.getConnection(URL);
                 PreparedStatement stmt = conn.prepareStatement(command)) {

                stmt.executeUpdate();

            } catch (SQLException e) {
                AdminCraft.LOGGER.error("Failed to run database command: ", e);
            }
        });
    }

    private static ExecutorService getDatabaseExecutor() {
        return DATABASE_EXECUTOR;
    }

    public static void shutdown() {
        DATABASE_EXECUTOR.shutdown();
    }
}