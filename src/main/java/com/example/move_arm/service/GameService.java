package com.example.move_arm.service;

import com.example.move_arm.database.ClickDao;
import com.example.move_arm.database.GameResultDao;
import com.example.move_arm.database.GameTypeDao;
import com.example.move_arm.database.UserDao;
import com.example.move_arm.database.DatabaseManager;
import com.example.move_arm.model.ClickData;
import com.example.move_arm.model.GameResult;
import com.example.move_arm.model.User;
import com.example.move_arm.model.Statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * GameService — централизованный сервис для:
 *  - сохранения результатов игры и кликов в базу данных (через DAO);
 *  - хранения в памяти кликов последней игры (lastGameClicks) для совместимости с UI;
 *  - управления текущим пользователем (currentUser), запоминания last_user_id в app_meta.
 *
 * Предполагается, что DatabaseManager, GameResultDao, ClickDao, UserDao, GameTypeDao
 * реализованы и доступны в проекте (как обсуждали ранее).
 */
public class GameService {

    private static final GameService INSTANCE = new GameService();

    private final UserDao userDao = new UserDao();
    private final GameTypeDao gameTypeDao = new GameTypeDao();
    private final GameResultDao gameResultDao = new GameResultDao();
    private final ClickDao clickDao = new ClickDao();
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    private User currentUser;
    private List<ClickData> lastGameClicks = new ArrayList<>();

    private GameService() {
        // Попробуем восстановить последнего пользователя из app_meta (если есть)
        try {
            String lastUserId = dbManager.getAppProperty("last_user_id");
            if (lastUserId != null) {
                try {
                    int id = Integer.parseInt(lastUserId);
                    Optional<User> u = userDao.findById(id);
                    if (u.isPresent()) currentUser = u.get();
                } catch (NumberFormatException ignored) { }
            }
        } catch (Exception ignored) { }

        // Если currentUser не задан — гарантируем наличие guest
        if (currentUser == null) {
            Optional<User> guest = userDao.findByUsername("guest");
            currentUser = guest.orElseGet(() -> userDao.createUser("guest"));
            // сохранение last_user_id
            try {
                dbManager.setAppProperty("last_user_id", String.valueOf(currentUser.getId()));
            } catch (Exception ignored) { }
        }
    }

    public static GameService getInstance() {
        return INSTANCE;
    }

    // --- User management -------------------------------------------------

    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Устанавливает текущего пользователя и запоминает его id в app_meta (last_user_id).
     */
    public void setCurrentUser(User user) {
        if (user == null) return;
        this.currentUser = user;
        try {
            dbManager.setAppProperty("last_user_id", String.valueOf(user.getId()));
        } catch (Exception e) {
            // логирование не обязателен здесь, но можно оставить
            System.err.println("GameService: Не удалось сохранить last_user_id: " + e.getMessage());
        }
    }

    // --- Game results / clicks ------------------------------------------

    /**
     * Сохранить список кликов игры в БД, собрать статистику и вернуть id записи game_results.
     * Также сохраняет копию кликов в lastGameClicks (для совместимости).
     *
     * Метод ожидает, что ClickData.getClickTimeNs() уже содержит относительные времена (ns от старта игры).
     */
    public int addGameClicks(List<ClickData> clicks) {
        if (clicks == null) clicks = Collections.emptyList();

        // Копируем в память
        lastGameClicks = new ArrayList<>(clicks);

        // Формируем GameResult
        GameResult result = new GameResult();
        result.setUserId(currentUser != null ? currentUser.getId() : 0);

        // Гарантируем наличие game_type (например "hover")
        int gameTypeId = gameTypeDao.getIdOrCreate("hover", "Move Arm hover game");
        result.setGameTypeId(gameTypeId);

        result.setScore(clicks.size());

        long durationMs = 0L;
        if (clicks.size() >= 2) {
            long first = clicks.get(0).getClickTimeNs();
            long last = clicks.get(clicks.size() - 1).getClickTimeNs();
            durationMs = Math.max(0L, (last - first) / 1_000_000L);
        }
        result.setDurationMs(durationMs);

        // Статистика
        result.setHitRate(Statistics.getHitRatePercent(clicks));
        result.setAvgIntervalMs(Statistics.getAverageClickIntervalMs(clicks));
        result.setAvgDistancePx(Statistics.getAverageCursorDistance(clicks));
        result.setAvgSpeed(Statistics.getAverageSpeedPxPerMs(clicks));

        // Сохраняем результат и клики в БД
        int resultId = gameResultDao.insert(result);
        if (!clicks.isEmpty()) {
            clickDao.insertClicks(resultId, clicks);
        }
        return resultId;
    }

    /**
     * Возвращает клики последней игры (в памяти). Если требуется клики из БД для конкретного resultId,
     * используйте ClickDao.readClicksForResult(resultId) (реализован в ClickDao).
     */
    public List<ClickData> getLastGameClicks() {
        return Collections.unmodifiableList(lastGameClicks);
    }

    /**
     * Очищает временные/кэшированные данные (не трогает записи в БД).
     */
    public void clear() {
        lastGameClicks.clear();
    }

    /**
     * Удаляет все результаты текущего пользователя из БД и очищает кэш.
     */
    public void clearAllForCurrentUser() {
        gameResultDao.deleteByUserId(currentUser.getId());
        clear();
    }

    /**
     * Для совместимости: печатает краткую сводку по последней игре в stdout/log.
     */
    public void printLastGameSummary() {
        List<ClickData> last = getLastGameClicks();
        System.out.println("СТАТИСТИКА ПОСЛЕДНЕЙ ИГРЫ:");
        System.out.println(Statistics.getSummary(last));
    }

    // --- После этой точки — методы, которые нужны UI контроллерам ---------

    /**
     * Возвращает список GameResult'ов для текущего пользователя (из БД).
     */
    public List<GameResult> getResultsForCurrentUser() {
        if (currentUser == null) return Collections.emptyList();
        return gameResultDao.findByUserId(currentUser.getId());
    }

    /**
     * Возвращает все результаты в БД (возможен use-case администрирования).
     */
    public List<GameResult> getAllResults() {
        return gameResultDao.findAll(); // предполагается реализация findAll() в GameResultDao или добавить её
    }
}
