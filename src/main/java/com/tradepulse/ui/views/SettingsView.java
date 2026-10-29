package com.tradepulse.ui.views;

import com.tradepulse.model.User;
import com.tradepulse.model.UserProfile;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.PortfolioService;
import com.tradepulse.ui.components.ToastNotification;
import com.tradepulse.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

public class SettingsView extends ScrollPane {
    private final AuthService authService;
    private final PortfolioService portfolioService;

    // Profile Settings
    private final TextField displayNameField = new TextField();
    private final TextArea bioArea = new TextArea();
    private final ComboBox<String> avatarBox = new ComboBox<>();
    private final Button saveProfileBtn = new Button("Save Profile Changes");

    // Appearance & Currency
    private final ComboBox<String> themeBox = new ComboBox<>();
    private final ComboBox<String> currencyBox = new ComboBox<>();

    // Password Security
    private final PasswordField currentPassField = new PasswordField();
    private final PasswordField newPassField = new PasswordField();
    private final PasswordField confirmPassField = new PasswordField();
    private final Button updatePassBtn = new Button("Change Password");

    // Notification Toggles
    private final CheckBox notifyTradesCheck = new CheckBox("Notify on order executions (Buys & Sells)");
    private final CheckBox notifyAlertsCheck = new CheckBox("Notify when stock target prices trigger");
    private final CheckBox notifyMessagesCheck = new CheckBox("Notify on incoming private community messages");

    // Privacy Toggles
    private final CheckBox publicProfileCheck = new CheckBox("Make profile publicly visible to other traders");
    private final CheckBox showLeaderboardCheck = new CheckBox("Show my trading return % on community leaderboard");

    public SettingsView(AuthService authService, PortfolioService portfolioService) {
        this.authService = authService;
        this.portfolioService = portfolioService;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        buildUI();
        refresh();
    }

    private void buildUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        VBox titleBox = new VBox(2);
        Label title = new Label("Terminal Settings & Personal Preferences");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subtitle = new Label("Manage personal profile, display theme, security credentials, and notifications");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, subtitle);

        // 1. Profile Settings Card
        VBox profCard = new VBox(12);
        profCard.getStyleClass().add("panel-card");
        Label profTitle = new Label("Personal Profile");
        profTitle.getStyleClass().add("panel-header-title");

        GridPane profGrid = new GridPane();
        profGrid.setHgap(16);
        profGrid.setVgap(12);

        Label dLbl = new Label("Display Name:");
        dLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        displayNameField.setPrefWidth(260);

        Label aLbl = new Label("Avatar Style:");
        aLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        avatarBox.getItems().addAll("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_admin");
        avatarBox.setValue("avatar_1");

        Label bLbl = new Label("Bio / Trading Strategy:");
        bLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        bioArea.setPrefRowCount(3);
        bioArea.setWrapText(true);

        profGrid.add(dLbl, 0, 0);
        profGrid.add(displayNameField, 1, 0);
        profGrid.add(aLbl, 0, 1);
        profGrid.add(avatarBox, 1, 1);
        profGrid.add(bLbl, 0, 2);
        profGrid.add(bioArea, 1, 2);

        saveProfileBtn.getStyleClass().add("btn-primary");
        saveProfileBtn.setOnAction(e -> handleSaveProfile());

        profCard.getChildren().addAll(profTitle, profGrid, saveProfileBtn);

        // 2. Appearance & Preferences Card
        VBox appCard = new VBox(12);
        appCard.getStyleClass().add("panel-card");
        Label appTitle = new Label("Theme & Display Preferences");
        appTitle.getStyleClass().add("panel-header-title");

        HBox appRow = new HBox(20);
        appRow.setAlignment(Pos.CENTER_LEFT);

        VBox themeCol = new VBox(4);
        Label tLbl = new Label("Theme Mode:");
        tLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        themeBox.getItems().addAll("DARK (Terminal)", "LIGHT (Clean)");
        themeBox.setValue("DARK (Terminal)");
        themeBox.setOnAction(e -> {
            boolean dark = themeBox.getValue().contains("DARK");
            ThemeManager.setTheme(dark ? "DARK" : "LIGHT", getScene());
        });
        themeCol.getChildren().addAll(tLbl, themeBox);

        VBox curCol = new VBox(4);
        Label cLbl = new Label("Display Currency:");
        cLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        currencyBox.getItems().addAll("INR (₹)", "USD ($)", "EUR (€)");
        currencyBox.setValue("INR (₹)");
        curCol.getChildren().addAll(cLbl, currencyBox);

        appRow.getChildren().addAll(themeCol, curCol);
        appCard.getChildren().addAll(appTitle, appRow);

        // 3. Security & Password Card
        VBox secCard = new VBox(12);
        secCard.getStyleClass().add("panel-card");
        Label secTitle = new Label("Security & Password");
        secTitle.getStyleClass().add("panel-header-title");

        GridPane secGrid = new GridPane();
        secGrid.setHgap(16);
        secGrid.setVgap(12);

        currentPassField.setPromptText("Current Password");
        newPassField.setPromptText("New Password (min 6 chars)");
        confirmPassField.setPromptText("Confirm New Password");

        secGrid.add(new Label("Current Password:") {{ setStyle("-fx-text-fill: #94a3b8;"); }}, 0, 0);
        secGrid.add(currentPassField, 1, 0);
        secGrid.add(new Label("New Password:") {{ setStyle("-fx-text-fill: #94a3b8;"); }}, 0, 1);
        secGrid.add(newPassField, 1, 1);
        secGrid.add(new Label("Confirm Password:") {{ setStyle("-fx-text-fill: #94a3b8;"); }}, 0, 2);
        secGrid.add(confirmPassField, 1, 2);

        updatePassBtn.getStyleClass().add("btn-secondary");
        updatePassBtn.setOnAction(e -> handleChangePassword());

        secCard.getChildren().addAll(secTitle, secGrid, updatePassBtn);

        // 4. Notifications & Privacy Card
        VBox notifCard = new VBox(12);
        notifCard.getStyleClass().add("panel-card");
        Label notifTitle = new Label("Notifications & Privacy Controls");
        notifTitle.getStyleClass().add("panel-header-title");

        notifyTradesCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12px;");
        notifyAlertsCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12px;");
        notifyMessagesCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12px;");

        publicProfileCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12px;");
        showLeaderboardCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12px;");

        Button savePrefsBtn = new Button("Save Preferences");
        savePrefsBtn.getStyleClass().add("btn-secondary");
        savePrefsBtn.setOnAction(e -> handleSavePreferences());

        notifCard.getChildren().addAll(
                notifTitle,
                notifyTradesCheck, notifyAlertsCheck, notifyMessagesCheck,
                new Separator(),
                publicProfileCheck, showLeaderboardCheck,
                savePrefsBtn
        );

        root.getChildren().addAll(titleBox, profCard, appCard, secCard, notifCard);
        setContent(root);
    }

    public void refresh() {
        User u = authService.getCurrentUser();
        UserProfile prof = authService.getCurrentProfile();
        if (u == null || prof == null) return;

        displayNameField.setText(u.getDisplayName());
        bioArea.setText(prof.getBio());
        avatarBox.setValue(prof.getAvatarSeed());

        themeBox.setValue(ThemeManager.isDark() ? "DARK (Terminal)" : "LIGHT (Clean)");
        currencyBox.setValue(prof.getCurrency().equals("INR") ? "INR (₹)" : prof.getCurrency());

        notifyTradesCheck.setSelected(prof.isNotifyTrades());
        notifyAlertsCheck.setSelected(prof.isNotifyPriceAlerts());
        notifyMessagesCheck.setSelected(prof.isNotifyMessages());

        publicProfileCheck.setSelected(prof.isPublic());
        showLeaderboardCheck.setSelected(prof.isShowTradesOnLeaderboard());
    }

    private void handleSaveProfile() {
        User u = authService.getCurrentUser();
        UserProfile prof = authService.getCurrentProfile();
        if (u == null || prof == null) return;

        String newName = displayNameField.getText().trim();
        if (!newName.isEmpty()) {
            authService.updateDisplayName(u.getId(), newName);
        }

        prof.setBio(bioArea.getText().trim());
        prof.setAvatarSeed(avatarBox.getValue());
        authService.updateProfile(prof);

        ToastNotification.show(this, "Profile Saved", "Your profile details have been updated.", ToastNotification.Type.SUCCESS);
    }

    private void handleSavePreferences() {
        UserProfile prof = authService.getCurrentProfile();
        if (prof == null) return;

        prof.setNotifyTrades(notifyTradesCheck.isSelected());
        prof.setNotifyPriceAlerts(notifyAlertsCheck.isSelected());
        prof.setNotifyMessages(notifyMessagesCheck.isSelected());

        prof.setPublic(publicProfileCheck.isSelected());
        prof.setShowTradesOnLeaderboard(showLeaderboardCheck.isSelected());

        authService.updateProfile(prof);
        ToastNotification.show(this, "Preferences Saved", "Notification and privacy settings updated.", ToastNotification.Type.SUCCESS);
    }

    private void handleChangePassword() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        String oldPass = currentPassField.getText();
        String newPass = newPassField.getText();
        String confirm = confirmPassField.getText();

        if (newPass.length() < 6) {
            ToastNotification.show(this, "Password Too Short", "New password must be at least 6 characters.", ToastNotification.Type.ERROR);
            return;
        }
        if (!newPass.equals(confirm)) {
            ToastNotification.show(this, "Mismatch", "New password confirmation does not match.", ToastNotification.Type.ERROR);
            return;
        }

        boolean success = authService.changePassword(u.getId(), oldPass, newPass);
        if (success) {
            currentPassField.clear();
            newPassField.clear();
            confirmPassField.clear();
            ToastNotification.show(this, "Password Changed", "Your password has been successfully updated.", ToastNotification.Type.SUCCESS);
        } else {
            ToastNotification.show(this, "Authentication Failed", "Current password is incorrect.", ToastNotification.Type.ERROR);
        }
    }
}
