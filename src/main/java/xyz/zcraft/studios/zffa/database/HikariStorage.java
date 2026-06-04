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
import java.sql.Statement;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class HikariStorage implements StorageEngine {
    private final ZFfaPlugin plugin;
    private final ExecutorService executor;
    private HikariDataSource dataSource;
    private String databaseType;

    public HikariStorage(ZFfaPlugin plugin, ExecutorService executor) {
        this.plugin = plugin;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            HikariConfig config = new HikariConfig();
            databaseType = plugin.getConfig().getString("settings.database-type", "SQLITE").toUpperCase(Locale.ROOT);
            if ("MYSQL".equals(databaseType)) {
                String host = plugin.getConfig().getString("settings.mysql.host", "127.0.0.1");
                int port = plugin.getConfig().getInt("settings.mysql.port", 3306);
                String database = plugin.getConfig().getString("settings.mysql.database", "zffa");
                String parameters = plugin.getConfig().getString("settings.mysql.parameters",
                        "useSSL=false&characterEncoding=utf8&useUnicode=true&rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048");
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?" + parameters);
                config.setUsername(plugin.getConfig().getString("settings.mysql.username", "root"));
                config.setPassword(plugin.getConfig().getString("settings.mysql.password", ""));
                config.setMaximumPoolSize(Math.max(2, plugin.getConfig().getInt("settings.mysql.pool-size", 6)));
                config.setMinimumIdle(Math.max(1, plugin.getConfig().getInt("settings.mysql.minimum-idle", 1)));
                config.setConnectionTimeout(Math.max(1000L, plugin.getConfig().getLong("settings.mysql.connection-timeout-ms", 10000L)));
                config.setMaxLifetime(Math.max(30000L, plugin.getConfig().getLong("settings.mysql.max-lifetime-ms", 1800000L)));
            } else {
                File dbFile = new File(plugin.getDataFolder(), "zffa.db");
                config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                config.setMaximumPoolSize(1);
                config.setConnectionInitSql("PRAGMA busy_timeout=" + Math.max(1000, plugin.getConfig().getInt("settings.sqlite.busy-timeout-ms", 5000)));
            }
            config.setPoolName("ZFFA-Hikari");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            this.dataSource = new HikariDataSource(config);
            applyDatabaseTuning();
            createTables();
        }, executor);
    }

    private void applyDatabaseTuning() {
        if (!"SQLITE".equals(databaseType)) return;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (plugin.getConfig().getBoolean("settings.sqlite.wal", true)) {
                statement.execute("PRAGMA journal_mode=WAL");
            }
            statement.execute("PRAGMA synchronous=" + plugin.getConfig().getString("settings.sqlite.synchronous", "NORMAL"));
            statement.execute("PRAGMA foreign_keys=ON");
        } catch (SQLException exception) {
            plugin.getLogger().warning("Unable to apply SQLite tuning: " + exception.getMessage());
        }
    }

    private void createTables() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     CREATE TABLE IF NOT EXISTS zffa_profiles (
                         uuid VARCHAR(36) PRIMARY KEY,
                         name VARCHAR(16) NOT NULL,
                         elo INT NOT NULL,
                         wins INT NOT NULL,
                         losses INT NOT NULL,
                         kills INT NOT NULL DEFAULT 0,
                         deaths INT NOT NULL DEFAULT 0,
                         streak INT NOT NULL DEFAULT 0
                     )
                     """)) {
            statement.executeUpdate();
            addColumnIfMissing(connection, "kills");
            addColumnIfMissing(connection, "deaths");
            addColumnIfMissing(connection, "streak");
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
                 PreparedStatement statement = connection.prepareStatement("SELECT name, elo, wins, losses, kills, deaths, streak FROM zffa_profiles WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerProfile(
                                uuid,
                                rs.getString("name"),
                                rs.getInt("elo"),
                                rs.getInt("wins"),
                                rs.getInt("losses"),
                                rs.getInt("kills"),
                                rs.getInt("deaths"),
                                rs.getInt("streak")
                        );
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
        return saveProfiles(java.util.List.of(profile));
    }

    @Override
    public CompletableFuture<Void> saveProfiles(Collection<PlayerProfile> profiles) {
        return CompletableFuture.runAsync(() -> {
            if (profiles == null || profiles.isEmpty()) return;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(upsertSql())) {
                for (PlayerProfile profile : profiles) {
                    bindProfile(statement, profile);
                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException exception) {
                plugin.getLogger().warning("Failed to save " + profiles.size() + " profile(s): " + exception.getMessage());
            }
        }, executor);
    }

    private String upsertSql() {
        if ("MYSQL".equals(databaseType)) {
            return """
                    INSERT INTO zffa_profiles (uuid, name, elo, wins, losses, kills, deaths, streak)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        elo = VALUES(elo),
                        wins = VALUES(wins),
                        losses = VALUES(losses),
                        kills = VALUES(kills),
                        deaths = VALUES(deaths),
                        streak = VALUES(streak)
                    """;
        }
        return """
                INSERT INTO zffa_profiles (uuid, name, elo, wins, losses, kills, deaths, streak)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    name = excluded.name,
                    elo = excluded.elo,
                    wins = excluded.wins,
                    losses = excluded.losses,
                    kills = excluded.kills,
                    deaths = excluded.deaths,
                    streak = excluded.streak
                """;
    }

    private void bindProfile(PreparedStatement statement, PlayerProfile profile) throws SQLException {
        statement.setString(1, profile.uuid().toString());
        statement.setString(2, profile.name());
        statement.setInt(3, profile.elo());
        statement.setInt(4, profile.wins());
        statement.setInt(5, profile.losses());
        statement.setInt(6, profile.kills());
        statement.setInt(7, profile.deaths());
        statement.setInt(8, profile.streak());
    }

    @Override
    public void close() {
        if (dataSource != null) dataSource.close();
    }
}
