package com.tiet.banking.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {
    private static volatile boolean initialized;

    private AppLogger() {
    }

    public static synchronized void configure() {
        if (initialized) {
            return;
        }
        Logger rootBankingLogger = Logger.getLogger("com.tiet.banking");
        rootBankingLogger.setLevel(Level.INFO);
        try {
            Path logDir = Path.of("logs");
            Files.createDirectories(logDir);
            FileHandler fileHandler = new FileHandler(logDir.resolve("banking.log").toString(), 1_048_576, 3, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);
            rootBankingLogger.addHandler(fileHandler);
        } catch (IOException ex) {
            rootBankingLogger.log(Level.WARNING, "File logging unavailable; falling back to console logging.", ex);
        }
        initialized = true;
    }

    public static Logger getLogger(Class<?> type) {
        configure();
        return Logger.getLogger(type.getName());
    }
}
