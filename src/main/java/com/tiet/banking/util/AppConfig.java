package com.tiet.banking.util;

import java.nio.file.Path;

public record AppConfig(
        Path databasePath,
        int pbkdf2Iterations,
        int sqliteBusyTimeoutMs) {

    public static final String DEFAULT_DB_PATH = "data/banking.db";
    public static final int DEFAULT_PBKDF2_ITERATIONS = 65_536;
    public static final int DEFAULT_SQLITE_BUSY_TIMEOUT_MS = 5_000;

    public static AppConfig loadDefault() {
        String configuredPath = System.getProperty("banking.db.path", DEFAULT_DB_PATH);
        int iterations = Integer.getInteger("banking.pbkdf2.iterations", DEFAULT_PBKDF2_ITERATIONS);
        int timeoutMs = Integer.getInteger("banking.sqlite.busyTimeoutMs", DEFAULT_SQLITE_BUSY_TIMEOUT_MS);
        return new AppConfig(Path.of(configuredPath), iterations, timeoutMs);
    }
}
