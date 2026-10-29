package com.tradepulse.ui.views;

import com.tradepulse.model.User;
import com.tradepulse.service.AuthService;
import com.tradepulse.ui.components.ToastNotification;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class AuthView extends StackPane {
    private final AuthService authService;
    private final Consumer<User> onAuthSuccess;

    private boolean isLoginMode = true;
    private final VBox formCard = new VBox(14);
    private final Label titleLabel = new Label("Sign In to TradePulse");
    private final Label subtitleLabel = new Label("Enter your credentials to access your trading terminal");
    private final Label errorLabel = new Label();

    // Fields
    private final TextField identifierField = new TextField(); // username or email
    private final TextField emailField = new TextField();
    private final TextField displayNameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final CheckBox termsCheck = new CheckBox("I agree to the Terms of Service & Virtual Trading Rules");
    private final Button submitBtn = new Button("Sign In");
    private final Button switchModeBtn = new Button("Don't have an account? Register here");

    public AuthView(AuthService authService, Consumer<User> onAuthSuccess) {
        this.authService = authService;
        this.onAuthSuccess = onAuthSuccess;

        setStyle("-fx-background-color: #0b0f19;");
        setAlignment(Pos.CENTER);
        setPadding(new Insets(24));

        buildUI();
    }

    private void buildUI() {
        formCard.setMaxWidth(420);
        formCard.setStyle("-fx-background-color: #111827; -fx-padding: 32; -fx-background-radius: 14px; " +
                "-fx-border-color: #1e293b; -fx-border-radius: 14px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 18, 0, 0, 6);");

        // Brand Banner
        HBox brand = new HBox(8);
        brand.setAlignment(Pos.CENTER);
        Label logoIcon = new Label("⚡");
        logoIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #06b6d4;");
        Label brandTitle = new Label("TradePulse Nexus");
        brandTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        brand.getChildren().addAll(logoIcon, brandTitle);

        titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        // Inputs styling
        identifierField.setPromptText("Username or Email");
        emailField.setPromptText("Email Address");
        displayNameField.setPromptText("Full Display Name");
        passwordField.setPromptText("Password");
        confirmPasswordField.setPromptText("Confirm Password");

        termsCheck.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        termsCheck.setSelected(true);

        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleSubmit());

        // Enter key in password triggers submission
        passwordField.setOnAction(e -> handleSubmit());
        confirmPasswordField.setOnAction(e -> handleSubmit());

        switchModeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #06b6d4; -fx-font-size: 12px; -fx-cursor: hand;");
        switchModeBtn.setOnAction(e -> toggleMode());

        renderFormFields();

        getChildren().add(formCard);
    }

    private void renderFormFields() {
        formCard.getChildren().clear();

        HBox brand = new HBox(8);
        brand.setAlignment(Pos.CENTER);
        Label logoIcon = new Label("⚡");
        logoIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #06b6d4;");
        Label brandTitle = new Label("TradePulse Nexus");
        brandTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        brand.getChildren().addAll(logoIcon, brandTitle);

        formCard.getChildren().addAll(brand, titleLabel, subtitleLabel, errorLabel);

        if (isLoginMode) {
            titleLabel.setText("Sign In to TradePulse");
            subtitleLabel.setText("Enter your credentials to access your trading terminal");
            submitBtn.setText("Sign In");
            switchModeBtn.setText("Don't have an account? Register here");

            identifierField.setPromptText("Username or Email (e.g. admin or alex_trader)");
            formCard.getChildren().addAll(
                    new Label("Username or Email") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;"); }},
                    identifierField,
                    new Label("Password") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;"); }},
                    passwordField,
                    submitBtn,
                    switchModeBtn
            );
        } else {
            titleLabel.setText("Create New Account");
            subtitleLabel.setText("Get ₹1,00,000 in virtual funds for simulated paper trading");
            submitBtn.setText("Register & Open Portfolio");
            switchModeBtn.setText("Already registered? Sign in here");

            identifierField.setPromptText("Unique Username");
            formCard.getChildren().addAll(
                    new Label("Username") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;"); }},
                    identifierField,
                    new Label("Email Address") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;"); }},
                    emailField,
                    new Label("Display Name") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;"); }},
                    displayNameField,
                    new Label("Password (min 6 characters)") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;"); }},
                    passwordField,
                    new Label("Confirm Password") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;"); }},
                    confirmPasswordField,
                    termsCheck,
                    submitBtn,
                    switchModeBtn
            );
        }
    }

    private void toggleMode() {
        this.isLoginMode = !isLoginMode;
        errorLabel.setVisible(false);
        renderFormFields();
    }

    private void handleSubmit() {
        errorLabel.setVisible(false);
        try {
            if (isLoginMode) {
                String id = identifierField.getText();
                String pass = passwordField.getText();
                User user = authService.login(id, pass);
                if (onAuthSuccess != null) onAuthSuccess.accept(user);
            } else {
                String username = identifierField.getText();
                String email = emailField.getText();
                String displayName = displayNameField.getText();
                String pass = passwordField.getText();
                String confirm = confirmPasswordField.getText();

                if (!pass.equals(confirm)) {
                    showError("Passwords do not match.");
                    return;
                }
                if (!termsCheck.isSelected()) {
                    showError("You must agree to the Terms of Service.");
                    return;
                }

                User user = authService.register(username, email, pass, displayName);
                if (onAuthSuccess != null) onAuthSuccess.accept(user);
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
