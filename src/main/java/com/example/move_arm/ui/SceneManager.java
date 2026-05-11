package com.example.move_arm.ui;

import java.util.HashMap;
import java.util.Map;

import com.example.move_arm.comtroller.BaseSettingsController;
import com.example.move_arm.comtroller.GameController;
import com.example.move_arm.comtroller.MenuController;
import com.example.move_arm.comtroller.MoreResultsController;
import com.example.move_arm.comtroller.ResultsController;
import com.example.move_arm.comtroller.SelectionController;
import com.example.move_arm.comtroller.StartWindowController;
import com.example.move_arm.comtroller.StatisticsController;
import com.example.move_arm.ui.presenter.HoldGamePresenter;
import com.example.move_arm.ui.presenter.HoverGamePresenter;
import com.example.move_arm.ui.view.HoldGameView;
import com.example.move_arm.ui.view.HoverGameView;
import com.example.move_arm.util.AppLogger;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static SceneManager instance;
    private final Stage primaryStage;
    private MainLayoutController mainController;

    // Кэш загруженных представлений
    private final Map<String, Parent> viewCache = new HashMap<>();

    // === КОНСТАНТЫ (оставлены как были) ===
    public static final String START = "start";
    public static final String GAME = "game";
    public static final String RESULTS = "results";
    public static final String MORERESULTS = "moreresults";
    public static final String HOVER_SETTINGS = "hover_settings";
    public static final String HOLD_SETTINGS = "hold_settings";
    public static final String SELECTION = "selection";
    public static final String MENU = "menu";
    public static final String STATISTICS = "statistics";
    public static final String NEURAL_GAME = "neural_game";

    private SceneManager(Stage stage) {
        this.primaryStage = stage;
    }

    public static void init(Stage stage) {
        if (instance == null) {
            instance = new SceneManager(stage);
            AppLogger.info("SceneManager: Инициализирован (режим регионов)");
        }
    }

    public static SceneManager get() {
        if (instance == null) {
            throw new IllegalStateException("SceneManager не инициализирован. Вызовите init() в start().");
        }
        return instance;
    }

    // Загружаем главный layout один раз
    public void showMainLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/move_arm/fxml/main-layout.fxml"));
            Parent root = loader.load();
            mainController = loader.getController();

            // Создаём Scene, но НЕ вызываем show()
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            AppLogger.info("SceneManager: Главный layout (с регионом) успешно загружен");

        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить main-layout.fxml", e);
        }
    }

    /**
     * Основной метод загрузки FXML в регион
     */
    private void loadIntoRegion(String key, String fxmlPath, ConsumerAfterLoad afterLoad) {
        try {
            Parent view = viewCache.get(key);

            if (view == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                view = loader.load();
                Object controller = loader.getController();

                // Передаём SceneManager всем контроллерам, у которых есть метод setSceneManager
                setSceneManagerToController(controller);

                viewCache.put(key, view);

                // Выполняем дополнительные действия после загрузки (например startGame())
                if (afterLoad != null) {
                    afterLoad.accept(controller);
                }
            }

            // Заменяем содержимое центрального региона
            if (mainController != null) {
                mainController.switchContent(view);
            } else {
                AppLogger.error("MainLayoutController не инициализирован");
            }

        } catch (Exception e) {
            AppLogger.error("Ошибка загрузки экрана: " + key, e);
        }
    }

    // Устанавливаем SceneManager контроллеру (если поддерживает)
    private void setSceneManagerToController(Object controller) {
        if (controller == null) return;

        if (controller instanceof GameController gc) {
            gc.setSceneManager(this);
        } else if (controller instanceof MenuController mc) {
            mc.setSceneManager(this);
        } else if (controller instanceof ResultsController rc) {
            rc.setSceneManager(this);
        } else if (controller instanceof MoreResultsController mrc) {
            mrc.setSceneManager(this);
        } else if (controller instanceof StatisticsController sc) {
            sc.setSceneManager(this);
        } else if (controller instanceof SelectionController sel) {
            sel.setSceneManager(this);
        } else if (controller instanceof BaseSettingsController set) {
            set.setSceneManager(this);
        } else if (controller instanceof StartWindowController swc) {
            swc.setSceneManager(this);
        }
        // Добавляй сюда новые контроллеры по мере появления
    }

    // ===================== Публичные методы навигации =====================

    public void showStart() {
        loadIntoRegion(START, "/com/example/move_arm/fxml/start-window.fxml", null);
    }

    public void showMenu() {
        loadIntoRegion(MENU, "/com/example/move_arm/fxml/menu-view.fxml", null);
    }

    public void showSelection() {
        loadIntoRegion(SELECTION, "/com/example/move_arm/fxml/selection-view.fxml", null);
    }

    public void showStatistics() {
        loadIntoRegion(STATISTICS, "/com/example/move_arm/fxml/hover-statistics-view.fxml", null);
    }

    public void showHoverSettings() {
        removeFromCache(HOVER_SETTINGS);
        loadIntoRegion(HOVER_SETTINGS, "/com/example/move_arm/fxml/hover-settings.fxml", null);
    }

    public void showHoldSettings() {
        removeFromCache(HOLD_SETTINGS);
        loadIntoRegion(HOLD_SETTINGS, "/com/example/move_arm/fxml/hold-settings.fxml", null);
    }

    public void showResults() {
        removeFromCache(RESULTS);
        loadIntoRegion(RESULTS, "/com/example/move_arm/fxml/results-view.fxml", null);
    }

    public void showMoreResults() {
        removeFromCache(MORERESULTS);
        loadIntoRegion(MORERESULTS, "/com/example/move_arm/fxml/more-results-view.fxml", null);
    }

    public void startNewGame() {
        AppLogger.info("SceneManager: startNewGame() — запускаем Hover игру");

        removeFromCache(GAME);

        loadIntoRegion(GAME, "/com/example/move_arm/fxml/game-view.fxml", ctrl -> {
            AppLogger.info("SceneManager: Загружен контроллер: " 
                        + (ctrl != null ? ctrl.getClass().getName() : "null"));

            if (ctrl instanceof HoverGameView view) {
                AppLogger.info("SceneManager: Успешно создан HoverGameView → создаём Presenter");
                HoverGamePresenter presenter = new HoverGamePresenter(view, this);
                presenter.startNewGame();

            } else if (ctrl instanceof GameController oldCtrl) {
                AppLogger.warn("SceneManager: Загружен СТАРЫЙ GameController. Пока используем его.");
                Platform.runLater(() -> Platform.runLater(oldCtrl::startGame));
            } else {
                AppLogger.error("SceneManager: Неизвестный тип контроллера!");
            }
        });
    }

    public void showHoldGame() {
        removeFromCache("holdGame");
        loadIntoRegion("holdGame", "/com/example/move_arm/fxml/hold-game-view.fxml",
            ctrl -> {
                if (ctrl instanceof HoldGameView view) {
                    // Двойной runLater — надёжно даёт время на layout
                    Platform.runLater(() -> {
                        Platform.runLater(() -> {
                            HoldGamePresenter presenter = new HoldGamePresenter(view, this);
                            presenter.startNewGame();
                        });
                    });
                }
            });
    }

    // Дополнительные методы
    public void clearCache() {
        viewCache.clear();
        AppLogger.info("SceneManager: Кэш представлений очищен");
    }

    // Вспомогательный функциональный интерфейс
    @FunctionalInterface
    private interface ConsumerAfterLoad {
        void accept(Object controller);
    }
    public void removeFromCache(String key) {
        if (viewCache.remove(key) != null) {
            AppLogger.info("SceneManager: Представление '" + key + "' удалено из кэша (для рестарта)");
        }
    }
    public void startNeuralGame() {
        AppLogger.info("SceneManager: startNeuralGame() — запускаем Neural режим");

        removeFromCache(NEURAL_GAME);

        loadIntoRegion(NEURAL_GAME, "/com/example/move_arm/fxml/neural-game-view.fxml", ctrl -> {
            AppLogger.info("SceneManager: Загружен контроллер Neural: " 
                        + (ctrl != null ? ctrl.getClass().getName() : "null"));

            if (ctrl instanceof com.example.move_arm.ui.view.NeuralGameViewImpl view) {
                AppLogger.info("SceneManager: Создаём NeuralGamePresenter");
                com.example.move_arm.ui.presenter.NeuralGamePresenter presenter = 
                    new com.example.move_arm.ui.presenter.NeuralGamePresenter(view, this);
                presenter.startNewGame();

            } else {
                AppLogger.error("SceneManager: Неизвестный контроллер для Neural: " + ctrl);
            }
        });
    }
    /**
 * Показывает экран результатов специально для Neural-режима
 */
    public void showNeuralResults() {
        AppLogger.info("SceneManager: Показываем результаты Neural игры");
        removeFromCache(RESULTS);
        
        loadIntoRegion(RESULTS, "/com/example/move_arm/fxml/results-view.fxml", ctrl -> {
            if (ctrl instanceof ResultsController rc) {
                rc.setIsNeuralMode(true);           // ← Важно!
                // Можно также явно установить тип
                // rc.setGameType("neural");
            }
        });
    }
}
