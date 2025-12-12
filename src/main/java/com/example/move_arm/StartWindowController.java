package com.example.move_arm;

import com.example.move_arm.database.UserDao;
import com.example.move_arm.model.User;
import com.example.move_arm.service.GameService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class StartWindowController {

    @FXML private TextField usernameField;
    @FXML private Button actionButton;
    @FXML private Hyperlink switchModeLink;
    @FXML private Label messageLabel;
    @FXML private Button playGuestButton;
    @FXML private ListView<String> usersListView;
    @FXML private Button deleteUserButton;
    @FXML private Button exitButton;
    @FXML private Label formTitle;

    private boolean registerMode = false;

    private SceneManager sceneManager;
    private final UserDao userDao = new UserDao();
    private final GameService gameService = GameService.getInstance();
    private List<User> userList;

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    @FXML
    public void initialize() {
        messageLabel.setText("");
        refreshUsersList();

        configureFormMode(false);

        actionButton.setOnAction(e -> handleAction());
        switchModeLink.setOnAction(e -> toggleMode());
        playGuestButton.setOnAction(e -> handlePlayAsGuest());
        deleteUserButton.setOnAction(e -> handleDeleteSelectedUser());
        exitButton.setOnAction(e -> Platform.exit());
    }


    private void configureFormMode(boolean isRegister) {
        this.registerMode = isRegister;

        if (!isRegister) {
            formTitle.setText("Вход");
            actionButton.setText("Войти");
            switchModeLink.setText("Нет аккаунта? Регистрация");
        } else {
            formTitle.setText("Регистрация");
            actionButton.setText("Зарегистрироваться");
            switchModeLink.setText("Есть аккаунт? Вход");
        }
    }

    private void toggleMode() {
        configureFormMode(!registerMode);
        messageLabel.setText("");
        usernameField.clear();
    }

    private void handleAction() {
        if (registerMode) handleRegister();
        else handleLogin();
    }

    private void refreshUsersList() {
        usersListView.getItems().clear();
        try {
            userList = userDao.listAll();
            for (User u : userList) usersListView.getItems().add(u.getUsername());
        } catch (Exception ignored) {}
    }

    private void handleLogin() {
        String username = sanitize(usernameField.getText());
        if (username.isEmpty()) {
            setMessage("Введите имя пользователя", true);
            return;
        }

        Optional<User> u = userDao.findByUsername(username);
        if (u.isEmpty()) {
            setMessage("Пользователь не найден", true);
            return;
        }

        gameService.setCurrentUser(u.get());
        sceneManager.clearCache();
        sceneManager.showSelection();
    }

    private void handleRegister() {
        String username = sanitize(usernameField.getText());
        if (username.isEmpty()) {
            setMessage("Введите имя для регистрации", true);
            return;
        }

        if (userDao.findByUsername(username).isPresent()) {
            setMessage("Пользователь уже существует", true);
            return;
        }

        User created = userDao.createUser(username);
        refreshUsersList();
        setMessage("Пользователь создан", false);
        configureFormMode(false);
    }

    private void handlePlayAsGuest() {
        User guest = userDao.findByUsername("guest")
                .orElseGet(() -> userDao.createUser("guest"));
        gameService.setCurrentUser(guest);
        sceneManager.clearCache();
        sceneManager.showSelection();
    }

    private void handleDeleteSelectedUser() {
        String sel = usersListView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            setMessage("Выберите пользователя", true);
            return;
        }
        if (sel.equals("guest")) {
            setMessage("Нельзя удалить guest", true);
            return;
        }
        userDao.deleteByUsername(sel);
        refreshUsersList();
    }

    private String sanitize(String s) {
        return s == null ? "" : s.trim();
    }

    private void setMessage(String text, boolean error) {
        messageLabel.setText(text);
        messageLabel.setStyle(error ? "-fx-text-fill:#ff7777" : "-fx-text-fill:#88ff88");
    }
}
