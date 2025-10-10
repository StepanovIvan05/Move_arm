package com.example.move_arm;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;

public class AppLogger {
    private static PrintWriter writer;

    static {
        try {
            // Папка для логов в AppData
            String appData = System.getenv("APPDATA");
            Path logDir = Paths.get(appData, "MoveArm");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            // Лог-файл
            Path logFile = logDir.resolve("log.txt");
            writer = new PrintWriter(new FileWriter(logFile.toFile(), true), true);
            log("=== Приложение запущено ===");
        } catch (IOException e) {
            System.err.println("Не удалось создать лог-файл: " + e.getMessage());
        }
    }

    public static void log(String message) {
        if (writer != null) {
            writer.println(LocalDateTime.now() + " | " + message);
        }
    }

    public static void logError(String message, Throwable throwable) {
        if (writer != null) {
            writer.println(LocalDateTime.now() + " | ERROR: " + message);
            throwable.printStackTrace(writer);
        }
    }
}
