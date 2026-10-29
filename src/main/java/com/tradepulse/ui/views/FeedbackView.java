package com.tradepulse.ui.views;

import com.tradepulse.model.FeedbackCategory;
import com.tradepulse.model.User;
import com.tradepulse.service.AdminService;
import com.tradepulse.service.AuthService;
import com.tradepulse.ui.components.ToastNotification;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class FeedbackView extends VBox {
    private final AuthService authService;
    private final AdminService adminService;

    private int selectedRating = 5;
    private final HBox starsBox = new HBox(8);
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private final TextArea commentArea = new TextArea();
    private final Button submitBtn = new Button("Submit Feedback");

    public FeedbackView(AuthService authService, AdminService adminService) {
        this.authService = authService;
        this.adminService = adminService;

        setSpacing(20);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");

        buildUI();
    }

    private void buildUI() {
        VBox titleBox = new VBox(2);
        Label title = new Label("Platform Feedback & User Suggestions");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subtitle = new Label("Help us improve TradePulse Nexus by reporting issues, feature requests, or platform ideas");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, subtitle);

        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("panel-card");
        formCard.setMaxWidth(600);

        Label formTitle = new Label("Submit Your Feedback");
        formTitle.getStyleClass().add("panel-header-title");

        // Star rating picker
        VBox ratingSection = new VBox(6);
        Label rateLabel = new Label("Your Rating:");
        rateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        renderStars();
        ratingSection.getChildren().addAll(rateLabel, starsBox);

        // Category
        VBox catSection = new VBox(6);
        Label catLabel = new Label("Category:");
        catLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        categoryBox.getItems().addAll("GENERAL", "FEATURE", "SUGGESTION", "BUG");
        categoryBox.setValue("GENERAL");
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        catSection.getChildren().addAll(catLabel, categoryBox);

        // Comment
        VBox commentSection = new VBox(6);
        Label comLabel = new Label("Comments / Details:");
        comLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        commentArea.setPromptText("Describe your experience, report a bug, or suggest a new tool for the platform...");
        commentArea.setPrefRowCount(5);
        commentArea.setWrapText(true);
        commentSection.getChildren().addAll(comLabel, commentArea);

        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleSubmit());

        formCard.getChildren().addAll(formTitle, ratingSection, catSection, commentSection, submitBtn);

        getChildren().addAll(titleBox, formCard);
    }

    private void renderStars() {
        starsBox.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            final int starIndex = i;
            Button star = new Button(i <= selectedRating ? "★" : "☆");
            star.setStyle(String.format("-fx-background-color: transparent; -fx-font-size: 24px; -fx-cursor: hand; -fx-text-fill: %s; -fx-padding: 0;",
                    i <= selectedRating ? "#f59e0b" : "#475569"));
            star.setOnAction(e -> {
                selectedRating = starIndex;
                renderStars();
            });
            starsBox.getChildren().add(star);
        }
    }

    private void handleSubmit() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        String comment = commentArea.getText();
        if (comment == null || comment.trim().isEmpty()) {
            ToastNotification.show(this, "Empty Comment", "Please provide a brief comment or description.", ToastNotification.Type.ERROR);
            return;
        }

        try {
            FeedbackCategory cat = FeedbackCategory.valueOf(categoryBox.getValue());
            adminService.submitFeedback(u.getId(), selectedRating, cat, comment);
            commentArea.clear();
            selectedRating = 5;
            renderStars();
            ToastNotification.show(this, "Feedback Submitted", "Thank you for your feedback! Our moderators review all submissions.", ToastNotification.Type.SUCCESS);
        } catch (Exception e) {
            ToastNotification.show(this, "Submission Error", e.getMessage(), ToastNotification.Type.ERROR);
        }
    }
}
