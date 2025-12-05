package com.example.move_arm;

import com.example.move_arm.database.UserDao;
import com.example.move_arm.model.User;
import com.example.move_arm.service.GameService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер стартового окна — простой логин/регистрация пользователя.
 */
public class StartWindowController {

    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;
    @FXML private Button playGuestButton;
    @FXML private ListView<String> usersListView;
    @FXML private List<User> userListView;
    @FXML private Button deleteUserButton;

    private SceneManager sceneManager;
    private final UserDao userDao = new UserDao();
    private final GameService gameService = GameService.getInstance();

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    @FXML
    public void initialize() {
        messageLabel.setText("");
        refreshUsersList();

        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> handleRegister());
        playGuestButton.setOnAction(e -> handlePlayAsGuest());
        deleteUserButton.setOnAction(e -> handleDeleteSelectedUser());
    }

    private void refreshUsersList() {
        usersListView.getItems().clear();
        try {
            userListView = userDao.listAll();
            for (User user : userListView) {
                usersListView.getItems().add(user.getUsername());
            }
            Optional<User> guest = userDao.findByUsername("guest");
            if (usersListView.getItems().isEmpty() && guest.isPresent()) usersListView.getItems().add(guest.get().getUsername());
        } catch (Exception ignored) { /* безопасно */ }
    }

    @FXML
    private void handleLogin() {
        String username = sanitize(usernameField.getText());
        if (username.isEmpty()) {
            setMessage("Введите имя пользователя", true);
            return;
        }

        Optional<User> u = userDao.findByUsername(username);
        if (u.isPresent()) {
            gameService.setCurrentUser(u.get());
            setMessage("Вход выполнен: " + username, false);
            AppLogger.info("StartWindowController: Пользователь вошёл: " + username);
            // Переходим в игру (или меню). Здесь — старт игры:
            sceneManager.clearCache();
            sceneManager.showSelection();
        } else {
            setMessage("Пользователь не найден. Зарегистрируйтесь или используйте 'Регистрация'.", true);
        }
    }

    @FXML
    private void handleRegister() {
        String username = sanitize(usernameField.getText());
        if (username.isEmpty()) {
            setMessage("Введите имя для регистрации", true);
            return;
        }

        Optional<User> existing = userDao.findByUsername(username);
        if (existing.isPresent()) {
            setMessage("Пользователь с таким именем уже существует", true);
            return;
        }

        try {
            User created = userDao.createUser(username);
            gameService.setCurrentUser(created);
            setMessage("Пользователь зарегистрирован и вошёл: " + username, false);
            AppLogger.info("StartWindowController: Пользователь зарегистрирован: " + username);
            refreshUsersList();
            sceneManager.clearCache();
            sceneManager.showSelection();
        } catch (Exception ex) {
            AppLogger.error("StartWindowController: Ошибка регистрации", ex);
            setMessage("Ошибка регистрации: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void handlePlayAsGuest() {
        Optional<User> g = userDao.findByUsername("guest");
        User guest = g.orElseGet(() -> userDao.createUser("guest"));
        gameService.setCurrentUser(guest);
        setMessage("Игрок: guest", false);
        sceneManager.clearCache();
        sceneManager.showSelection();
    }

    @FXML
    private void handleDeleteSelectedUser() {
        String sel = usersListView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.isBlank()) {
            setMessage("Выберите пользователя в списке", true);
        }
        else if (sel.equals("guest")) {
            setMessage("Нельзя удалять guest", true);
        }
        else {
            userDao.deleteByUsername(sel);
            refreshUsersList();
        }
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.trim();
    }

    private void setMessage(String text, boolean isError) {
        messageLabel.setText(text);
        if (isError) {
            messageLabel.setStyle("-fx-text-fill: #ff6666;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #aaffaa;");
        }
    }
}
