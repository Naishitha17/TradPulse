package com.tradepulse.ui.components;

import com.tradepulse.model.Notification;
import com.tradepulse.model.User;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.NotificationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationModal extends StackPane {
    private final NotificationService notificationService;
    private final AuthService authService;
    private final Runnable onClose;
    private final VBox listContainer = new VBox(8);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd • HH:mm");

    public NotificationModal(NotificationService notificationService, AuthService authService, Runnable onClose) {
        this.notificationService = notificationService;
        this.authService = authService;
        this.onClose = onClose;

        setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        setAlignment(Pos.CENTER);

        buildUI();
        refresh();
    }

    private void buildUI() {
        VBox card = new VBox(14);
        card.setMaxWidth(460);
        card.setMaxHeight(500);
        card.setStyle("-fx-background-color: #111827; -fx-padding: 24; -fx-background-radius: 12px; " +
                "-fx-border-color: #1e293b; -fx-border-radius: 12px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.5), 20, 0, 0, 6);");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Notification Center");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button markAllBtn = new Button("Mark all read");
        markAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #06b6d4; -fx-font-size: 11px; -fx-cursor: hand;");
        markAllBtn.setOnAction(e -> {
            User u = authService.getCurrentUser();
            if (u != null) {
                notificationService.markAllAsRead(u.getId());
                refresh();
            }
        });

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        header.getChildren().addAll(title, sp, markAllBtn, closeBtn);

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(header, scroll);
        getChildren().add(card);

        // Click outside closes modal
        setOnMouseClicked(e -> {
            if (e.getTarget() == this && onClose != null) {
                onClose.run();
            }
        });
    }

    public void refresh() {
        listContainer.getChildren().clear();
        User u = authService.getCurrentUser();
        if (u == null) return;

        List<Notification> list = notificationService.getUserNotifications(u.getId());
        if (list.isEmpty()) {
            Label empty = new Label("You have no notifications yet.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            listContainer.getChildren().add(empty);
            return;
        }

        for (Notification n : list) {
            HBox item = new HBox(10);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(10));
            item.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 6; -fx-border-color: %s; -fx-border-radius: 6;",
                    n.isRead() ? "#151d2d" : "#1a2438",
                    n.isRead() ? "#1e293b" : "#06b6d4"));

            Label icon = new Label(getIconForType(n.getType().name()));
            icon.setStyle("-fx-font-size: 16px;");

            VBox text = new VBox(2);
            Label tLbl = new Label(n.getTitle());
            tLbl.setStyle(String.format("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: %s;",
                    n.isRead() ? "#e2e8f0" : "#38bdf8"));

            Label mLbl = new Label(n.getMessage());
            mLbl.setWrapText(true);
            mLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            String timeStr = n.getCreatedAt() != null ? n.getCreatedAt().format(DATE_FMT) : "";
            Label time = new Label(timeStr);
            time.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b;");

            text.getChildren().addAll(tLbl, mLbl, time);
            HBox.setHgrow(text, Priority.ALWAYS);

            item.getChildren().addAll(icon, text);
            item.setOnMouseClicked(e -> {
                notificationService.markAsRead(n.getId());
                refresh();
            });

            listContainer.getChildren().add(item);
        }
    }

    private String getIconForType(String type) {
        return switch (type) {
            case "ORDER" -> "💳";
            case "ALERT" -> "🔔";
            case "SOCIAL" -> "💬";
            default -> "ℹ️";
        };
    }
}
