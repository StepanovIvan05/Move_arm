package com.example.move_arm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLogger {

    private static final Path LOG_FILE = initLogPath();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Уровни логирования
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    private static Level currentLevel = Level.INFO;

    private static Path initLogPath() {
        try {
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isBlank()) {
                appData = System.getProperty("user.home");
            }
            Path logPath = Path.of(appData, "MoveArm", "log.txt");
            Files.createDirectories(logPath.getParent());
            return logPath;
        } catch (IOException e) {
            System.err.println("Не удалось создать директорию для логов: " + e.getMessage());
            return Path.of("move_arm_log.txt"); // fallback
        }
    }

    public static void setLevel(Level level) {
        currentLevel = level;
    }

    public static void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    public static void info(String message) {
        log(Level.INFO, message, null);
    }

    public static void warn(String message) {
        log(Level.WARN, message, null);
    }

    public static void error(String message) {
        log(Level.ERROR, message, null);
    }

    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    private static void log(Level level, String message, Throwable throwable) {
        if (level.ordinal() < currentLevel.ordinal()) {
            return;
        }

        try (FileWriter fw = new FileWriter(LOG_FILE.toFile(), true);
             PrintWriter writer = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String threadName = Thread.currentThread().getName();
            String className = getCallingClassName();

            writer.printf("%s [%s] [%s] [%s] - %s%n",
                    timestamp, level, threadName, className, message);

            if (throwable != null) {
                throwable.printStackTrace(writer);
            }

            // Также выводим в консоль для отладки
            System.out.printf("%s [%s] [%s] - %s%n", timestamp, level, className, message);
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }

        } catch (IOException e) {
            System.err.println("Ошибка записи в лог: " + e.getMessage());
        }
    }

    private static String getCallingClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Ищем первый элемент, который не относится к самому логгеру
        for (int i = 1; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!className.equals(AppLogger.class.getName())) {
                return stackTrace[i].getClassName() + "." + stackTrace[i].getMethodName() + ":" + stackTrace[i].getLineNumber();
            }
        }
        return "Unknown";
    }
}