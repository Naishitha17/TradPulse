package com.tradepulse.ui.views;

import com.tradepulse.model.HoldingViewItem;
import com.tradepulse.model.Order;
import com.tradepulse.model.PortfolioSummary;
import com.tradepulse.model.StockQuote;
import com.tradepulse.model.User;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.MarketDataService;
import com.tradepulse.service.PortfolioService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DashboardView extends ScrollPane {
    private final AuthService authService;
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;
    private final Consumer<String> onSelectTicker;
    private final Runnable onNavigateMarket;

    private final Label greetingLabel = new Label();
    private final Label portfolioValueLabel = new Label("₹0.00");
    private final Label todayPnlLabel = new Label("₹0.00");
    private final Label returnPctLabel = new Label("0.00%");
    private final Label cashBalanceLabel = new Label("₹0.00");

    private final HBox indicesContainer = new HBox(12);
    private final VBox watchlistContainer = new VBox(6);
    private final VBox recentOrdersContainer = new VBox(6);
    private final VBox onboardingBox = new VBox(12);

    public DashboardView(AuthService authService, MarketDataService marketDataService,
                         PortfolioService portfolioService, Consumer<String> onSelectTicker,
                         Runnable onNavigateMarket) {
        this.authService = authService;
        this.marketDataService = marketDataService;
        this.portfolioService = portfolioService;
        this.onSelectTicker = onSelectTicker;
        this.onNavigateMarket = onNavigateMarket;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        buildUI();
        refresh();

        // Listen for live market ticks
        marketDataService.addQuoteListener(q -> Platform.runLater(this::refreshIndicesOnly));
    }

    private void buildUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        // 1. Welcome Greeting Header
        User user = authService.getCurrentUser();
        String name = user != null ? user.getDisplayName() : "Trader";
        greetingLabel.setText("Welcome back, " + name + " 👋");
        greetingLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label subGreeting = new Label("Here is your simulated market overview and portfolio performance for today.");
        subGreeting.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        VBox headerBox = new VBox(4);
        headerBox.getChildren().addAll(greetingLabel, subGreeting);

        // 2. Metric Cards (4 Grid)
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(16);
        metricsGrid.setVgap(16);

        VBox card1 = createMetricCard("TOTAL PORTFOLIO VALUE", portfolioValueLabel, "Cash + Invested Market Value");
        VBox card2 = createMetricCard("TODAY'S PROFIT / LOSS", todayPnlLabel, "Real-time value change today");
        VBox card3 = createMetricCard("TOTAL RETURN", returnPctLabel, "Since account opened");
        VBox card4 = createMetricCard("AVAILABLE VIRTUAL CASH", cashBalanceLabel, "Ready for virtual deployment");

        GridPane.setHgrow(card1, Priority.ALWAYS);
        GridPane.setHgrow(card2, Priority.ALWAYS);
        GridPane.setHgrow(card3, Priority.ALWAYS);
        GridPane.setHgrow(card4, Priority.ALWAYS);

        metricsGrid.add(card1, 0, 0);
        metricsGrid.add(card2, 1, 0);
        metricsGrid.add(card3, 2, 0);
        metricsGrid.add(card4, 3, 0);

        // 3. Composite Market Indices Bar
        VBox indicesSection = new VBox(8);
        Label indicesTitle = new Label("MARKET PULSE INDICES");
        indicesTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #06b6d4;");
        indicesContainer.setAlignment(Pos.CENTER_LEFT);
        indicesSection.getChildren().addAll(indicesTitle, indicesContainer);

        // 4. Onboarding state (if fresh user)
        onboardingBox.setStyle("-fx-background-color: #111827; -fx-padding: 24; -fx-background-radius: 12px; -fx-border-color: #1e293b; -fx-border-radius: 12px;");
        Label onbTitle = new Label("🚀 Start Your Paper Trading Journey");
        onbTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label onbDesc = new Label("You currently have ₹1,00,000 in virtual funds available. Explore the Market Watch table to analyze 60+ real-world companies across Tech, Finance, Healthcare, and Energy, inspect candlestick charts, and place your first market order risk-free.");
        onbDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        onbDesc.setWrapText(true);
        Button exploreBtn = new Button("Explore Market Watch (60+ Stocks) →");
        exploreBtn.getStyleClass().add("btn-primary");
        exploreBtn.setOnAction(e -> {
            if (onNavigateMarket != null) onNavigateMarket.run();
        });
        onboardingBox.getChildren().addAll(onbTitle, onbDesc, exploreBtn);

        // 5. 2-Column Split: Watchlist & Recent Trades
        HBox split = new HBox(20);

        VBox leftCol = new VBox(12);
        leftCol.getStyleClass().add("panel-card");
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        Label watchTitle = new Label("Your Watchlist");
        watchTitle.getStyleClass().add("panel-header-title");
        leftCol.getChildren().addAll(watchTitle, watchlistContainer);

        VBox rightCol = new VBox(12);
        rightCol.getStyleClass().add("panel-card");
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        Label ordersTitle = new Label("Recent Completed Orders");
        ordersTitle.getStyleClass().add("panel-header-title");
        rightCol.getChildren().addAll(ordersTitle, recentOrdersContainer);

        split.getChildren().addAll(leftCol, rightCol);

        root.getChildren().addAll(headerBox, metricsGrid, indicesSection, onboardingBox, split);
        setContent(root);
    }

    private VBox createMetricCard(String label, Label valueLabel, String subtitle) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");

        Label title = new Label(label);
        title.getStyleClass().add("metric-label");

        valueLabel.getStyleClass().add("metric-value");

        Label sub = new Label(subtitle);
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(title, valueLabel, sub);
        return card;
    }

    public void refresh() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        greetingLabel.setText("Welcome back, " + user.getDisplayName() + " 👋");

        // Load Portfolio Summary
        PortfolioSummary summary = portfolioService.getPortfolioSummary(user.getId(), marketDataService);
        portfolioValueLabel.setText(String.format("₹%,.2f", summary.getTotalPortfolioValue()));
        cashBalanceLabel.setText(String.format("₹%,.2f", summary.getCashBalance()));

        double todayVal = summary.getTodayPnl().doubleValue();
        todayPnlLabel.setText(String.format("%s₹%,.2f", todayVal >= 0 ? "+" : "", todayVal));
        todayPnlLabel.setStyle(String.format("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s;",
                todayVal >= 0 ? "#10b981" : "#ef4444"));

        double retPct = summary.getTotalReturnPercent();
        returnPctLabel.setText(String.format("%s%.2f%%", retPct >= 0 ? "+" : "", retPct));
        returnPctLabel.setStyle(String.format("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s;",
                retPct >= 0 ? "#10b981" : "#ef4444"));

        // Toggle onboarding visibility
        boolean hasHoldingsOrTrades = summary.getTotalHoldingsCount() > 0;
        onboardingBox.setVisible(!hasHoldingsOrTrades);
        onboardingBox.setManaged(!hasHoldingsOrTrades);

        refreshIndicesOnly();
        refreshWatchlist(user.getId());
        refreshRecentOrders(user.getId());
    }

    private void refreshIndicesOnly() {
        indicesContainer.getChildren().clear();
        Map<String, Double> indices = marketDataService.getMarketIndices();
        for (Map.Entry<String, Double> entry : indices.entrySet()) {
            HBox item = new HBox(8);
            item.setAlignment(Pos.CENTER);
            item.setStyle("-fx-background-color: #111827; -fx-border-color: #1e293b; -fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; -fx-padding: 8 16 8 16;");

            Label name = new Label(entry.getKey());
            name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

            Label val = new Label(String.format("%,.2f", entry.getValue()));
            val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

            item.getChildren().addAll(name, val);
            indicesContainer.getChildren().add(item);
        }
    }

    private void refreshWatchlist(int userId) {
        watchlistContainer.getChildren().clear();
        Set<String> watchlist = portfolioService.getWatchlist(userId);

        if (watchlist.isEmpty()) {
            Label empty = new Label("No stocks in your watchlist yet. Star stocks in the Market Watch to track them here.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            empty.setWrapText(true);
            watchlistContainer.getChildren().add(empty);
            return;
        }

        for (String ticker : watchlist) {
            StockQuote q = marketDataService.getStock(ticker);
            if (q == null) continue;

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));
            row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 6; -fx-cursor: hand;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;"));
            row.setOnMouseClicked(e -> {
                if (onSelectTicker != null) onSelectTicker.accept(ticker);
            });

            VBox info = new VBox(1);
            Label tLbl = new Label(q.getTicker());
            tLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            Label nLbl = new Label(q.getName());
            nLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            info.getChildren().addAll(tLbl, nLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label price = new Label(String.format("₹%,.2f", q.getCurrentPrice()));
            price.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

            Label change = new Label(String.format("%s%.2f%%", q.getChange() >= 0 ? "+" : "", q.getChangePercent()));
            change.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 2 8 2 8; -fx-background-radius: 4;",
                    q.getChange() >= 0 ? "#10b981" : "#ef4444",
                    q.getChange() >= 0 ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)"));

            row.getChildren().addAll(info, spacer, price, change);
            watchlistContainer.getChildren().add(row);
        }
    }

    private void refreshRecentOrders(int userId) {
        recentOrdersContainer.getChildren().clear();
        List<Order> orders = portfolioService.getOrders(userId);

        if (orders.isEmpty()) {
            Label empty = new Label("No trade orders placed yet. Place your first simulated buy or sell in the Market Watch.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            empty.setWrapText(true);
            recentOrdersContainer.getChildren().add(empty);
            return;
        }

        int count = Math.min(5, orders.size());
        for (int i = 0; i < count; i++) {
            Order o = orders.get(i);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));
            row.setStyle("-fx-background-color: #151d2d; -fx-background-radius: 6;");

            Label type = new Label(o.getType().name());
            type.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: %s; -fx-padding: 2 6 2 6; -fx-background-radius: 4;",
                    o.getType().name().equals("BUY") ? "#06b6d4" : "#8b5cf6"));

            Label desc = new Label(String.format("%d shares of %s @ ₹%,.2f", o.getQuantity(), o.getTicker(), o.getPrice()));
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #f8fafc;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label total = new Label(String.format("₹%,.2f", o.getTotalAmount()));
            total.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

            row.getChildren().addAll(type, desc, spacer, total);
            recentOrdersContainer.getChildren().add(row);
        }
    }
}
