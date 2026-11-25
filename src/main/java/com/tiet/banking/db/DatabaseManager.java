package com.tiet.banking.db;

import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.util.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.sqlite.SQLiteConfig;

public class DatabaseManager {
    private final Path databasePath;
    private final int busyTimeoutMs;

    public DatabaseManager(AppConfig config) {
        this(config.databasePath(), config.sqliteBusyTimeoutMs());
    }

    public DatabaseManager(Path databasePath, int busyTimeoutMs) {
        this.databasePath = databasePath;
        this.busyTimeoutMs = busyTimeoutMs;
    }

    public Connection openWriteConnection() throws SQLException {
        return openConnection(SQLiteConfig.TransactionMode.IMMEDIATE);
    }

    public Connection openReadConnection() throws SQLException {
        return openConnection(SQLiteConfig.TransactionMode.DEFERRED);
    }

    private Connection openConnection(SQLiteConfig.TransactionMode txMode) throws SQLException {
        return openConnection(txMode, false);
    }

    private Connection openConnection(SQLiteConfig.TransactionMode txMode, boolean configureWal) throws SQLException {
        ensureParentDirectory();
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setBusyTimeout(busyTimeoutMs);
        config.setTransactionMode(txMode);
        if (configureWal) {
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        }
        String url = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        return DriverManager.getConnection(url, config.toProperties());
    }

    public void initializeSchema() {
        String sql = loadSchemaSql();
        try (Connection connection = openConnection(SQLiteConfig.TransactionMode.IMMEDIATE, true);
             Statement statement = connection.createStatement()) {
            for (String rawPart : sql.split(";")) {
                String trimmed = rawPart.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to initialize SQLite schema.", ex);
        }
    }

    private void ensureParentDirectory() {
        try {
            Path parent = databasePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ex) {
            throw new PersistenceException("Failed to create database directory.", ex);
        }
    }

    private String loadSchemaSql() {
        try (InputStream stream = DatabaseManager.class.getResourceAsStream("/db/schema.sql")) {
            if (stream == null) {
                throw new PersistenceException("Schema resource /db/schema.sql not found.");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new PersistenceException("Failed to read /db/schema.sql.", ex);
        }
    }
}
