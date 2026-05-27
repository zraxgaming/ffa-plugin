package xyz.zcraft.studios.zffa.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import xyz.zcraft.studios.zffa.ZFfaPlugin;
import xyz.zcraft.studios.zffa.profile.PlayerProfile;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class HikariStorage implements StorageEngine {
    private final ZFfaPlugin plugin;
    private final ExecutorService executor;
    private HikariDataSource dataSource;

    public HikariStorage(ZFfaPlugin plugin, ExecutorService executor) {
        this.plugin = plugin;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            HikariConfig config = new HikariConfig();
            String type = plugin.getConfig().getString("settings.database-type", "SQLITE").toUpperCase(Locale.ROOT);
            if ("MYSQL".equals(type)) {
                String host = plugin.getConfig().getString("settings.mysql.host", "127.0.0.1");
                int port = plugin.getConfig().getInt("settings.mysql.port", 3306);
                String database = plugin.getConfig().getString("settings.mysql.database", "zffa");
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=utf8");
                config.setUsername(plugin.getConfig().getString("settings.mysql.username", "root"));
                config.setPassword(plugin.getConfig().getString("settings.mysql.password", ""));
                config.setMaximumPoolSize(plugin.getConfig().getInt("settings.mysql.pool-size", 10));
            } else {
                File dbFile = new File(plugin.getDataFolder(), "zffa.db");
                config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                config.setMaximumPoolSize(1);
            }
            config.setPoolName("ZFFA-Hikari");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            this.dataSource = new HikariDataSource(config);
            createTables();
        }, executor);
    }

    private void createTables() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     CREATE TABLE IF NOT EXISTS zffa_profiles (
                         uuid VARCHAR(36) PRIMARY KEY,
                         name VARCHAR(16) NOT NULL,
                         elo INT NOT NULL,
                         wins INT NOT NULL,
                         losses INT NOT NULL
                     )
                     """)) {
            statement.executeUpdate();
            addColumnIfMissing(connection, "kills");
            addColumnIfMissing(connection, "deaths");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create profile table", exception);
        }
    }

    private void addColumnIfMissing(Connection connection, String column) {
        try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE zffa_profiles ADD COLUMN " + column + " INT NOT NULL DEFAULT 0")) {
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // Column already exists.
        }
    }

    @Override
    public CompletableFuture<PlayerProfile> loadProfile(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT name, elo, wins, losses, kills, deaths FROM zffa_profiles WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerProfile(uuid, rs.getString("name"), rs.getInt("elo"), rs.getInt("wins"), rs.getInt("losses"), rs.getInt("kills"), rs.getInt("deaths"));
                    }
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("Failed to load profile " + uuid + ": " + exception.getMessage());
            }
            return PlayerProfile.fresh(uuid, name);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         REPLACE INTO zffa_profiles (uuid, name, elo, wins, losses, kills, deaths)
                         VALUES (?, ?, ?, ?, ?, ?, ?)
                         """)) {
                statement.setString(1, profile.uuid().toString());
                statement.setString(2, profile.name());
                statement.setInt(3, profile.elo());
                statement.setInt(4, profile.wins());
                statement.setInt(5, profile.losses());
                statement.setInt(6, profile.kills());
                statement.setInt(7, profile.deaths());
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().warning("Failed to save profile " + profile.uuid() + ": " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public void close() {
        if (dataSource != null) dataSource.close();
    }
}
