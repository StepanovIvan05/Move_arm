package com.example.move_arm;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        try {
            // Можно установить уровень логирования через аргументы командной строки
            if (args.length > 0 && args[0].equals("--debug")) {
                AppLogger.setLevel(AppLogger.Level.DEBUG);
            }

            AppLogger.info("Launcher: Запуск приложения через Launcher");
            AppLogger.debug("Launcher: Аргументы командной строки: " + String.join(" ", args));

            Application.launch(HelloApplication.class, args);

            AppLogger.info("Launcher: Приложение завершило работу");
        } catch (Exception e) {
            AppLogger.error("Launcher: Критическая ошибка при запуске приложения", e);
            System.exit(1);
        }
    }
}