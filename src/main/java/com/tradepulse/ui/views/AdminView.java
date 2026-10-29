package com.tradepulse.ui.views;

import com.tradepulse.model.FeedbackCategory;
import com.tradepulse.model.FeedbackItem;
import com.tradepulse.model.FeedbackStatus;
import com.tradepulse.model.Role;
import com.tradepulse.model.User;
import com.tradepulse.service.AdminService;
import com.tradepulse.service.AuthService;
import com.tradepulse.ui.components.ToastNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class AdminView extends ScrollPane {
    private final AuthService authService;
    private final AdminService adminService;

    private final Label totalCountLabel = new Label("0");
    private final Label avgRatingLabel = new Label("0.0 / 5.0");
    private final TableView<FeedbackItem> feedbackTable = new TableView<>();
    private final TableView<User> userTable = new TableView<>();
    private final ObservableList<FeedbackItem> feedbackData = FXCollections.observableArrayList();
    private final ObservableList<User> userData = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AdminView(AuthService authService, AdminService adminService) {
        this.authService = authService;
        this.adminService = adminService;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        buildUI();
        refresh();
    }

    private void buildUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        User currentUser = authService.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            VBox deniedBox = new VBox(10);
            deniedBox.setAlignment(Pos.CENTER);
            deniedBox.setPadding(new Insets(60));
            Label lockIcon = new Label("🔒");
            lockIcon.setStyle("-fx-font-size: 36px;");
            Label deniedTitle = new Label("Access Denied");
            deniedTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
            Label deniedDesc = new Label("This console requires System Administrator privileges.");
            deniedDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            deniedBox.getChildren().addAll(lockIcon, deniedTitle, deniedDesc);
            root.getChildren().add(deniedBox);
            setContent(root);
            return;
        }

        VBox titleBox = new VBox(2);
        Label title = new Label("Administrative Moderation & User Management Console");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subtitle = new Label("Platform oversight, user feedback review, and role enforcement");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, subtitle);

        // 1. Metric Cards
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(16);
        VBox c1 = createMetricCard("TOTAL SUBMISSIONS", totalCountLabel);
        VBox c2 = createMetricCard("AVERAGE PLATFORM RATING", avgRatingLabel);
        GridPane.setHgrow(c1, Priority.ALWAYS);
        GridPane.setHgrow(c2, Priority.ALWAYS);
        metricsGrid.add(c1, 0, 0);
        metricsGrid.add(c2, 1, 0);

        // 2. Feedback Moderation Table
        VBox fbCard = new VBox(10);
        fbCard.getStyleClass().add("panel-card");
        Label fbTitle = new Label("User Feedback & Reports Queue");
        fbTitle.getStyleClass().add("panel-header-title");

        feedbackTable.setItems(feedbackData);
        feedbackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        feedbackTable.setPrefHeight(260);

        TableColumn<FeedbackItem, String> fbIdCol = new TableColumn<>("#");
        fbIdCol.setPrefWidth(50);
        fbIdCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));

        TableColumn<FeedbackItem, String> fbUserCol = new TableColumn<>("USER");
        fbUserCol.setPrefWidth(120);
        fbUserCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));

        TableColumn<FeedbackItem, String> fbRatingCol = new TableColumn<>("RATING");
        fbRatingCol.setPrefWidth(90);
        fbRatingCol.setCellValueFactory(d -> new SimpleStringProperty("★".repeat(d.getValue().getRating())));
        fbRatingCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px;");
            }
        });

        TableColumn<FeedbackItem, String> fbCatCol = new TableColumn<>("CATEGORY");
        fbCatCol.setPrefWidth(100);
        fbCatCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory().name()));

        TableColumn<FeedbackItem, String> fbCommentCol = new TableColumn<>("COMMENT");
        fbCommentCol.setPrefWidth(260);
        fbCommentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getComment()));

        TableColumn<FeedbackItem, String> fbStatusCol = new TableColumn<>("STATUS");
        fbStatusCol.setPrefWidth(100);
        fbStatusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        fbStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(item.equals("REVIEWED") ? "badge-green" : "badge-cyan");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<FeedbackItem, String> fbActionCol = new TableColumn<>("ACTION");
        fbActionCol.setPrefWidth(120);
        fbActionCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        fbActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button reviewBtn = new Button("Mark Reviewed");
            {
                reviewBtn.getStyleClass().add("btn-secondary");
                reviewBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8;");
                reviewBtn.setOnAction(e -> {
                    FeedbackItem item = getTableView().getItems().get(getIndex());
                    adminService.updateFeedbackStatus(item.getId(), FeedbackStatus.REVIEWED, Role.ADMIN);
                    refresh();
                    ToastNotification.show(AdminView.this, "Status Updated", "Feedback marked as reviewed.", ToastNotification.Type.SUCCESS);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : reviewBtn);
            }
        });

        feedbackTable.getColumns().addAll(List.of(fbIdCol, fbUserCol, fbRatingCol, fbCatCol, fbCommentCol, fbStatusCol, fbActionCol));
        fbCard.getChildren().addAll(fbTitle, feedbackTable);

        // 3. User Accounts Management Table
        VBox userCard = new VBox(10);
        userCard.getStyleClass().add("panel-card");
        Label userTitle = new Label("Registered User Accounts");
        userTitle.getStyleClass().add("panel-header-title");

        userTable.setItems(userData);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        userTable.setPrefHeight(260);

        TableColumn<User, String> uIdCol = new TableColumn<>("#ID");
        uIdCol.setPrefWidth(50);
        uIdCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));

        TableColumn<User, String> uNameCol = new TableColumn<>("USERNAME");
        uNameCol.setPrefWidth(120);
        uNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));

        TableColumn<User, String> uDisplayCol = new TableColumn<>("DISPLAY NAME");
        uDisplayCol.setPrefWidth(140);
        uDisplayCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayName()));

        TableColumn<User, String> uEmailCol = new TableColumn<>("EMAIL");
        uEmailCol.setPrefWidth(180);
        uEmailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));

        TableColumn<User, String> uRoleCol = new TableColumn<>("ROLE");
        uRoleCol.setPrefWidth(90);
        uRoleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().name()));

        TableColumn<User, String> uStatusCol = new TableColumn<>("STATUS");
        uStatusCol.setPrefWidth(90);
        uStatusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "ACTIVE" : "DISABLED"));
        uStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(item.equals("ACTIVE") ? "badge-green" : "badge-red");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<User, String> uActionCol = new TableColumn<>("MODERATION");
        uActionCol.setPrefWidth(120);
        uActionCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        uActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button toggleBtn = new Button("Toggle Active");
            {
                toggleBtn.getStyleClass().add("btn-secondary");
                toggleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8;");
                toggleBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.isAdmin()) {
                        ToastNotification.show(AdminView.this, "Cannot Deactivate Admin", "Admin accounts cannot be deactivated.", ToastNotification.Type.ERROR);
                        return;
                    }
                    adminService.toggleUserActive(u.getId(), !u.isActive(), Role.ADMIN);
                    refresh();
                    ToastNotification.show(AdminView.this, "User Updated", "User active status toggled.", ToastNotification.Type.SUCCESS);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : toggleBtn);
            }
        });

        userTable.getColumns().addAll(List.of(uIdCol, uNameCol, uDisplayCol, uEmailCol, uRoleCol, uStatusCol, uActionCol));
        userCard.getChildren().addAll(userTitle, userTable);

        root.getChildren().addAll(titleBox, metricsGrid, fbCard, userCard);
        setContent(root);
    }

    private VBox createMetricCard(String label, Label valueLabel) {
        VBox card = new VBox(4);
        card.getStyleClass().add("metric-card");
        Label l = new Label(label);
        l.getStyleClass().add("metric-label");
        valueLabel.getStyleClass().add("metric-value");
        card.getChildren().addAll(l, valueLabel);
        return card;
    }

    public void refresh() {
        User u = authService.getCurrentUser();
        if (u == null || !u.isAdmin()) return;

        Map<String, Object> metrics = adminService.getFeedbackMetrics(Role.ADMIN);
        totalCountLabel.setText(String.valueOf(metrics.getOrDefault("totalCount", 0)));
        avgRatingLabel.setText(String.format("%.1f / 5.0", metrics.getOrDefault("avgRating", 0.0)));

        List<FeedbackItem> fbList = adminService.getAllFeedback(Role.ADMIN);
        feedbackData.setAll(fbList);

        List<User> users = adminService.getAllUsers(Role.ADMIN);
        userData.setAll(users);
    }
}
