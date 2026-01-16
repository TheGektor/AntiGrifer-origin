package ru.antigrief.core.database;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Менеджер базы данных (SQLite + HikariCP).
 * Поддерживает миграции.
 *
 * @author Antag0nis1
 */
public class DatabaseManager {

    private final Plugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setPoolName("AntiGriefPool");
        config.setConnectionTestQuery("SELECT 1");

        try {
            this.dataSource = new HikariDataSource(config);
            initTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database", e);
        }
    }

    private void initTables() {
        try (Connection conn = getConnection(); Statement statement = conn.createStatement()) {
            
            // Таблица миграций
            statement.execute("CREATE TABLE IF NOT EXISTS database_migrations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "version VARCHAR(10) NOT NULL, " +
                    "migration_name VARCHAR(255) NOT NULL, " +
                    "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Таблица настроек (для хранения метаданных, например текущей версии БД)
            statement.execute("CREATE TABLE IF NOT EXISTS plugin_settings (" +
                    "key VARCHAR(50) PRIMARY KEY, " +
                    "value TEXT, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Основная таблица игроков (из старой версии, обновленная структура может быть добавлена через миграции)
            statement.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "playtime LONG DEFAULT 0, " +
                    "trusted BOOLEAN DEFAULT 0)");
            
            // Таблица истории фидбека (из предыдущих задач)
             statement.execute("CREATE TABLE IF NOT EXISTS feedback_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "username VARCHAR(16) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "timestamp LONG NOT NULL, " +
                    "status VARCHAR(20) DEFAULT 'OPEN')");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating tables", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource is not initialized.");
        return dataSource.getConnection();
    }
}
