package com.example.move_arm;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppLogger {

    private static final Path LOG_FILE =
            Path.of(System.getenv("APPDATA"), "MoveArm", "log.txt");

    public static void log(String message) {
        try {
            Files.createDirectories(LOG_FILE.getParent());
            try (FileWriter writer = new FileWriter(LOG_FILE.toFile(), true)) {
                writer.write(java.time.LocalDateTime.now() + " - " + message + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
