package com.example.move_arm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Утилита для логирования в файл.
 */
public class AppLogger {
    private static Logger logger;

    public static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("MoveArmLogger");

            try {
                Path logDir = Path.of(System.getProperty("user.home"), ".move_arm_logs");
                if (!Files.exists(logDir)) {
                    Files.createDirectories(logDir);
                }

                Path logFile = logDir.resolve("move_arm.log");
                FileHandler handler = new FileHandler(logFile.toString(), true);
                handler.setFormatter(new SimpleFormatter());
                logger.addHandler(handler);

                logger.setLevel(Level.ALL);
                logger.info("=== Приложение запущено ===");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return logger;
    }
}
