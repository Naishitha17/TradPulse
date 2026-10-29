package com.tradepulse.ui.components;

import com.tradepulse.model.StockQuote;
import com.tradepulse.model.User;
import com.tradepulse.model.UserProfile;
import com.tradepulse.service.MarketDataService;
import com.tradepulse.service.NotificationService;
import com.tradepulse.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class HeaderBar extends HBox {
    private final Label titleLabel = new Label("Dashboard");
    private final Label subtitleLabel = new Label("Overview & Analytics");
    private final Label cashBalanceLabel = new Label("₹1,00,000.00");
    private final Label unreadBadge = new Label("0");
    private final Button notifBtn = new Button("🔔");
    private final Button themeBtn = new Button("☀️");
    private final TextField searchField = new TextField();

    private final MarketDataService marketDataService;
    private final NotificationService notificationService;
    private final Consumer<String> onSelectTicker;
    private final Runnable onOpenNotifications;
    private final Runnable onOpenProfile;

    public HeaderBar(MarketDataService marketDataService, NotificationService notificationService,
                     Consumer<String> onSelectTicker, Runnable onOpenNotifications, Runnable onOpenProfile) {
        this.marketDataService = marketDataService;
        this.notificationService = notificationService;
        this.onSelectTicker = onSelectTicker;
        this.onOpenNotifications = onOpenNotifications;
        this.onOpenProfile = onOpenProfile;

        getStyleClass().add("header-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setPadding(new Insets(12, 24, 12, 24));

        buildUI();
    }

    private void buildUI() {
        // Page title & subtitle
        VBox titleBox = new VBox(2);
        titleLabel.getStyleClass().add("header-title");
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Global search field
        searchField.setPromptText("Search stocks (e.g. AAPL, RELIANCE, NVDA)...");
        searchField.setPrefWidth(300);
        searchField.setOnAction(e -> handleSearch());

        // Market status pill
        HBox statusPill = new HBox(6);
        statusPill.setAlignment(Pos.CENTER);
        statusPill.getStyleClass().add("status-pill-active");
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: #10b981; -fx-font-size: 10px;");
        Label statusText = new Label("SIMULATION ACTIVE");
        statusText.getStyleClass().add("status-pill-text");
        statusPill.getChildren().addAll(dot, statusText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quick cash balance
        HBox cashPill = new HBox(6);
        cashPill.setAlignment(Pos.CENTER);
        cashPill.setStyle("-fx-background-color: #151d2d; -fx-border-color: #2e3b4e; -fx-border-radius: 20px; " +
                "-fx-background-radius: 20px; -fx-padding: 4 14 4 14;");
        Label cashTag = new Label("CASH:");
        cashTag.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        cashBalanceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #06b6d4; -fx-font-weight: bold;");
        cashPill.getChildren().addAll(cashTag, cashBalanceLabel);

        // Notification Bell with unread count badge
        StackPane notifStack = new StackPane();
        notifBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        notifBtn.setOnAction(e -> {
            if (onOpenNotifications != null) onOpenNotifications.run();
        });

        unreadBadge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; " +
                "-fx-padding: 1 5 1 5; -fx-background-radius: 10px;");
        unreadBadge.setVisible(false);
        StackPane.setAlignment(unreadBadge, Pos.TOP_RIGHT);
        notifStack.getChildren().addAll(notifBtn, unreadBadge);

        // Theme switch button
        themeBtn.setStyle("-fx-background-color: #151d2d; -fx-border-color: #2e3b4e; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 6 10 6 10;");
        themeBtn.setOnAction(e -> {
            Scene scene = getScene();
            ThemeManager.toggleTheme(scene);
            themeBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        });

        getChildren().addAll(titleBox, searchField, statusPill, spacer, cashPill, notifStack, themeBtn);
    }

    private void handleSearch() {
        String text = searchField.getText();
        if (text == null || text.isBlank()) return;

        List<StockQuote> found = marketDataService.searchStocks(text, "ALL", "ALL");
        if (!found.isEmpty() && onSelectTicker != null) {
            onSelectTicker.accept(found.get(0).getTicker());
            searchField.clear();
        }
    }

    public void setPageTitle(String title, String subtitle) {
        this.titleLabel.setText(title);
        this.subtitleLabel.setText(subtitle);
    }

    public void updateCashBalance(String formattedBalance) {
        this.cashBalanceLabel.setText(formattedBalance);
    }

    public void updateUnreadCount(int count) {
        if (count > 0) {
            unreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            unreadBadge.setVisible(true);
        } else {
            unreadBadge.setVisible(false);
        }
    }
}
