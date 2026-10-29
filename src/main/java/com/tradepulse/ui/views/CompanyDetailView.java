package com.tradepulse.ui.views;

import com.tradepulse.model.AlertCondition;
import com.tradepulse.model.Holding;
import com.tradepulse.model.NewsArticle;
import com.tradepulse.model.Order;
import com.tradepulse.model.StockQuote;
import com.tradepulse.model.User;
import com.tradepulse.model.UserProfile;
import com.tradepulse.service.AlertService;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.MarketDataService;
import com.tradepulse.service.NewsService;
import com.tradepulse.service.PortfolioService;
import com.tradepulse.service.TradingEngine;
import com.tradepulse.ui.components.CandlestickChartCanvas;
import com.tradepulse.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public class CompanyDetailView extends ScrollPane {
    private final String ticker;
    private final MarketDataService marketDataService;
    private final TradingEngine tradingEngine;
    private final PortfolioService portfolioService;
    private final AuthService authService;
    private final AlertService alertService;
    private final NewsService newsService;
    private final Runnable onBack;

    private final CandlestickChartCanvas chartCanvas = new CandlestickChartCanvas();
    private final Label priceLabel = new Label();
    private final Label changeLabel = new Label();
    private final Label hudLabel = new Label();
    private final Button starBtn = new Button();

    // Trading Panel Controls
    private boolean isBuyTab = true;
    private final TextField quantityField = new TextField("10");
    private final Label estTotalLabel = new Label("₹0.00");
    private final Label estFeeLabel = new Label("₹0.00");
    private final Label availableBalanceLabel = new Label("Available: ₹0.00");
    private final Button tradeActionBtn = new Button("Execute Buy Order");

    // Holdings Overview Card
    private final Label ownedSharesLabel = new Label("0");
    private final Label avgCostLabel = new Label("₹0.00");
    private final Label holdingValueLabel = new Label("₹0.00");
    private final Label holdingPnlLabel = new Label("₹0.00 (0.00%)");

    // Alert Panel Controls
    private final ComboBox<String> alertConditionBox = new ComboBox<>();
    private final TextField alertPriceField = new TextField();

    // News Container
    private final VBox newsContainer = new VBox(8);

    public CompanyDetailView(String ticker, MarketDataService marketDataService, TradingEngine tradingEngine,
                             PortfolioService portfolioService, AuthService authService,
                             AlertService alertService, NewsService newsService, Runnable onBack) {
        this.ticker = ticker.toUpperCase();
        this.marketDataService = marketDataService;
        this.tradingEngine = tradingEngine;
        this.portfolioService = portfolioService;
        this.authService = authService;
        this.alertService = alertService;
        this.newsService = newsService;
        this.onBack = onBack;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        buildUI();
        refresh();

        // Listen for market price ticks
        marketDataService.addQuoteListener(q -> {
            if (q.getTicker().equalsIgnoreCase(this.ticker)) {
                Platform.runLater(this::refreshQuoteOnly);
            }
        });
    }

    private void buildUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        StockQuote quote = marketDataService.getStock(ticker);
        if (quote == null) {
            root.getChildren().add(new Label("Stock ticker " + ticker + " not found."));
            setContent(root);
            return;
        }

        // 1. Back button and Company Header
        Button backBtn = new Button("← Back to Market Watch");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        HBox tickerRow = new HBox(8);
        tickerRow.setAlignment(Pos.CENTER_LEFT);

        Label tickerLbl = new Label(quote.getTicker());
        tickerLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #06b6d4;");

        Label sectorBadge = new Label(quote.getSector());
        sectorBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 4;");

        starBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 16px;");
        starBtn.setOnAction(e -> toggleWatchlist());

        tickerRow.getChildren().addAll(tickerLbl, sectorBadge, starBtn);

        Label nameLbl = new Label(quote.getName());
        nameLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");

        titleBox.getChildren().addAll(tickerRow, nameLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        priceLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        priceBox.getChildren().addAll(priceLabel, changeLabel);

        headerRow.getChildren().addAll(backBtn, titleBox, spacer, priceBox);

        // 2. Company Key Statistics Cards
        HBox statsRow = new HBox(12);
        statsRow.getChildren().addAll(
                createStatBox("52W HIGH", String.format("₹%,.2f", quote.getHigh52w())),
                createStatBox("52W LOW", String.format("₹%,.2f", quote.getLow52w())),
                createStatBox("DAY RANGE", String.format("₹%,.2f - ₹%,.2f", quote.getDayLow(), quote.getDayHigh())),
                createStatBox("MARKET CAP", quote.getMarketCap()),
                createStatBox("P/E RATIO", String.format("%.1f", quote.getPeRatio())),
                createStatBox("VOLUME", String.format("%,d", quote.getVolume()))
        );

        // 3. Interactive Candlestick Chart Card
        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("panel-card");

        HBox chartToolbar = new HBox(10);
        chartToolbar.setAlignment(Pos.CENTER_LEFT);

        Label chartTitle = new Label("Candlestick Terminal & Volume");
        chartTitle.getStyleClass().add("panel-header-title");

        hudLabel.textProperty().bind(chartCanvas.hudTextProperty());
        hudLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #06b6d4; -fx-font-family: monospace;");

        Region chartSpacer = new Region();
        HBox.setHgrow(chartSpacer, Priority.ALWAYS);

        HBox intervalBox = new HBox(4);
        String[] intervals = {"1m", "5m", "1h", "1D"};
        for (String iv : intervals) {
            Button ivBtn = new Button(iv);
            ivBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
            ivBtn.setOnAction(e -> {
                chartCanvas.setData(quote.getHistory());
            });
            intervalBox.getChildren().add(ivBtn);
        }

        chartToolbar.getChildren().addAll(chartTitle, hudLabel, chartSpacer, intervalBox);
        chartCanvas.setData(quote.getHistory());
        chartCard.getChildren().addAll(chartToolbar, chartCanvas);

        // 4. Split Grid: Left = User Holdings & Target Alerts & News; Right = Virtual Trading Card
        HBox bottomSplit = new HBox(20);

        // LEFT COLUMN
        VBox leftColumn = new VBox(16);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        // A. User Holdings in this stock
        VBox holdingsCard = new VBox(10);
        holdingsCard.getStyleClass().add("panel-card");
        Label holdTitle = new Label("Your Position in " + quote.getTicker());
        holdTitle.getStyleClass().add("panel-header-title");

        HBox holdMetrics = new HBox(16);
        holdMetrics.getChildren().addAll(
                createStatBox("SHARES OWNED", ownedSharesLabel),
                createStatBox("AVERAGE COST", avgCostLabel),
                createStatBox("CURRENT VALUE", holdingValueLabel),
                createStatBox("UNREALIZED P&L", holdingPnlLabel)
        );
        holdingsCard.getChildren().addAll(holdTitle, holdMetrics);

        // B. Target Price Alert Card
        VBox alertCard = new VBox(10);
        alertCard.getStyleClass().add("panel-card");
        Label alertTitle = new Label("Set Price Alert");
        alertTitle.getStyleClass().add("panel-header-title");

        HBox alertForm = new HBox(10);
        alertForm.setAlignment(Pos.CENTER_LEFT);

        alertConditionBox.getItems().addAll("Price rises ABOVE (≥)", "Price drops BELOW (≤)");
        alertConditionBox.setValue("Price rises ABOVE (≥)");

        alertPriceField.setPromptText("Target Price (₹)");
        alertPriceField.setPrefWidth(140);
        alertPriceField.setText(String.format("%.2f", quote.getCurrentPrice() * 1.05));

        Button setAlertBtn = new Button("Create Alert");
        setAlertBtn.getStyleClass().add("btn-primary");
        setAlertBtn.setOnAction(e -> handleCreateAlert());

        alertForm.getChildren().addAll(alertConditionBox, alertPriceField, setAlertBtn);
        alertCard.getChildren().addAll(alertTitle, alertForm);

        // C. Company Financial News
        VBox newsCard = new VBox(10);
        newsCard.getStyleClass().add("panel-card");
        Label newsTitle = new Label("Recent Headlines for " + quote.getName());
        newsTitle.getStyleClass().add("panel-header-title");
        newsCard.getChildren().addAll(newsTitle, newsContainer);

        leftColumn.getChildren().addAll(holdingsCard, alertCard, newsCard);

        // RIGHT COLUMN: Trading Execution Card
        VBox tradingCard = new VBox(14);
        tradingCard.getStyleClass().add("panel-card");
        tradingCard.setPrefWidth(340);
        tradingCard.setMinWidth(320);

        Label tradeTitle = new Label("Virtual Trade Execution");
        tradeTitle.getStyleClass().add("panel-header-title");

        // Buy / Sell Tabs
        HBox tabBox = new HBox(8);
        Button buyTab = new Button("BUY");
        Button sellTab = new Button("SELL");

        buyTab.setMaxWidth(Double.MAX_VALUE);
        sellTab.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buyTab, Priority.ALWAYS);
        HBox.setHgrow(sellTab, Priority.ALWAYS);

        buyTab.getStyleClass().add("btn-success");
        sellTab.getStyleClass().add("btn-secondary");

        buyTab.setOnAction(e -> {
            isBuyTab = true;
            buyTab.getStyleClass().setAll("btn-success");
            sellTab.getStyleClass().setAll("btn-secondary");
            tradeActionBtn.setText("Execute Buy Order");
            tradeActionBtn.getStyleClass().setAll("btn-success");
            updateTradingEstimates();
        });

        sellTab.setOnAction(e -> {
            isBuyTab = false;
            sellTab.getStyleClass().setAll("btn-danger");
            buyTab.getStyleClass().setAll("btn-secondary");
            tradeActionBtn.setText("Execute Sell Order");
            tradeActionBtn.getStyleClass().setAll("btn-danger");
            updateTradingEstimates();
        });

        tabBox.getChildren().addAll(buyTab, sellTab);

        quantityField.setPromptText("Quantity of shares");
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> updateTradingEstimates());

        availableBalanceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        VBox estimatesBox = new VBox(6);
        estimatesBox.setStyle("-fx-background-color: #151d2d; -fx-padding: 12; -fx-background-radius: 6;");

        HBox r1 = createEstimateRow("Order Type:", new Label("Market Order (Instant)"));
        HBox r2 = createEstimateRow("Estimated Value:", estTotalLabel);
        HBox r3 = createEstimateRow("Brokerage Fee (0.05%):", estFeeLabel);

        estimatesBox.getChildren().addAll(r1, r2, r3);

        tradeActionBtn.setMaxWidth(Double.MAX_VALUE);
        tradeActionBtn.getStyleClass().add("btn-success");
        tradeActionBtn.setOnAction(e -> handleOrderExecution());

        Label disclaim = new Label("⚠️ Virtual funds only. Zero real-money risk.");
        disclaim.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        tradingCard.getChildren().addAll(tradeTitle, tabBox, new Label("Shares Quantity:"), quantityField, availableBalanceLabel, estimatesBox, tradeActionBtn, disclaim);

        bottomSplit.getChildren().addAll(leftColumn, tradingCard);

        root.getChildren().addAll(headerRow, statsRow, chartCard, bottomSplit);
        setContent(root);
    }

    private VBox createStatBox(String label, String value) {
        VBox box = new VBox(2);
        box.setStyle("-fx-background-color: #111827; -fx-padding: 10 14 10 14; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");
        HBox.setHgrow(box, Priority.ALWAYS);

        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        box.getChildren().addAll(l, v);
        return box;
    }

    private VBox createStatBox(String label, Label valueLabel) {
        VBox box = new VBox(2);
        box.setStyle("-fx-background-color: #151d2d; -fx-padding: 10 14 10 14; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");
        HBox.setHgrow(box, Priority.ALWAYS);

        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        valueLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        box.getChildren().addAll(l, valueLabel);
        return box;
    }

    private HBox createEstimateRow(String label, Label valueLabel) {
        HBox row = new HBox();
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        valueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(l, sp, valueLabel);
        return row;
    }

    public void refresh() {
        refreshQuoteOnly();
        refreshUserHoldings();
        refreshWatchlistStar();
        refreshNews();
        updateTradingEstimates();
    }

    private void refreshQuoteOnly() {
        StockQuote q = marketDataService.getStock(ticker);
        if (q == null) return;

        priceLabel.setText(String.format("₹%,.2f", q.getCurrentPrice()));

        boolean isUp = q.getChange() >= 0;
        changeLabel.setText(String.format("%s₹%.2f (%s%.2f%%)", isUp ? "+" : "", q.getChange(), isUp ? "+" : "", q.getChangePercent()));
        changeLabel.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 3 8 3 8; -fx-background-radius: 4;",
                isUp ? "#10b981" : "#ef4444",
                isUp ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)"));

        chartCanvas.setData(q.getHistory());
        updateTradingEstimates();
    }

    private void refreshUserHoldings() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        List<Holding> holdings = portfolioService.getHoldings(u.getId());
        Holding matching = holdings.stream().filter(h -> h.getTicker().equalsIgnoreCase(ticker)).findFirst().orElse(null);

        StockQuote q = marketDataService.getStock(ticker);
        double curPrice = q != null ? q.getCurrentPrice() : 0.0;

        if (matching != null) {
            ownedSharesLabel.setText(String.valueOf(matching.getShares()));
            avgCostLabel.setText(String.format("₹%,.2f", matching.getAverageBuyPrice()));

            BigDecimal marketVal = BigDecimal.valueOf(curPrice * matching.getShares()).setScale(2, RoundingMode.HALF_UP);
            holdingValueLabel.setText(String.format("₹%,.2f", marketVal));

            BigDecimal invested = matching.getTotalInvestedCost();
            BigDecimal pnl = marketVal.subtract(invested).setScale(2, RoundingMode.HALF_UP);
            double pnlPct = invested.compareTo(BigDecimal.ZERO) > 0
                    ? pnl.divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            boolean isPos = pnl.compareTo(BigDecimal.ZERO) >= 0;
            holdingPnlLabel.setText(String.format("%s₹%,.2f (%s%.2f%%)", isPos ? "+" : "", pnl, isPos ? "+" : "", pnlPct));
            holdingPnlLabel.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: %s;",
                    isPos ? "#10b981" : "#ef4444"));
        } else {
            ownedSharesLabel.setText("0 shares");
            avgCostLabel.setText("₹0.00");
            holdingValueLabel.setText("₹0.00");
            holdingPnlLabel.setText("₹0.00 (0.00%)");
            holdingPnlLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        }
    }

    private void refreshWatchlistStar() {
        User u = authService.getCurrentUser();
        if (u != null) {
            boolean inWatch = portfolioService.isInWatchlist(u.getId(), ticker);
            starBtn.setText(inWatch ? "★ In Watchlist" : "☆ Add to Watchlist");
            starBtn.setStyle(inWatch ? "-fx-background-color: transparent; -fx-text-fill: #f59e0b; -fx-font-weight: bold;"
                    : "-fx-background-color: transparent; -fx-text-fill: #94a3b8;");
        }
    }

    private void toggleWatchlist() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        if (portfolioService.isInWatchlist(u.getId(), ticker)) {
            portfolioService.removeFromWatchlist(u.getId(), ticker);
        } else {
            portfolioService.addToWatchlist(u.getId(), ticker);
        }
        refreshWatchlistStar();
    }

    private void refreshNews() {
        newsContainer.getChildren().clear();
        List<NewsArticle> articles = newsService.getNewsForTicker(ticker);
        for (NewsArticle a : articles) {
            VBox box = new VBox(4);
            box.setStyle("-fx-background-color: #151d2d; -fx-padding: 10; -fx-background-radius: 6;");

            Label title = new Label(a.getTitle());
            title.setWrapText(true);
            title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

            Label desc = new Label(a.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            HBox meta = new HBox(8);
            Label src = new Label("Source: " + a.getSource());
            src.setStyle("-fx-font-size: 10px; -fx-text-fill: #06b6d4;");
            meta.getChildren().add(src);

            box.getChildren().addAll(title, desc, meta);
            newsContainer.getChildren().add(box);
        }
    }

    private void updateTradingEstimates() {
        User u = authService.getCurrentUser();
        UserProfile prof = authService.getCurrentProfile();
        StockQuote q = marketDataService.getStock(ticker);
        if (q == null || u == null || prof == null) return;

        int qty = 1;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) qty = 1;
        } catch (Exception ignored) {}

        double gross = qty * q.getCurrentPrice();
        double fee = Math.max(1.0, Math.min(20.0, gross * 0.0005));

        estTotalLabel.setText(String.format("₹%,.2f", gross));
        estFeeLabel.setText(String.format("₹%,.2f", fee));

        if (isBuyTab) {
            availableBalanceLabel.setText(String.format("Available Cash: ₹%,.2f", prof.getVirtualBalance()));
        } else {
            List<Holding> holdings = portfolioService.getHoldings(u.getId());
            Holding h = holdings.stream().filter(item -> item.getTicker().equalsIgnoreCase(ticker)).findFirst().orElse(null);
            int owned = h != null ? h.getShares() : 0;
            availableBalanceLabel.setText(String.format("Owned Shares: %d shares", owned));
        }
    }

    private void handleOrderExecution() {
        User u = authService.getCurrentUser();
        StockQuote q = marketDataService.getStock(ticker);
        if (u == null || q == null) return;

        int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            ToastNotification.show(this, "Invalid Quantity", "Please enter a positive whole number of shares.", ToastNotification.Type.ERROR);
            return;
        }

        // Show Confirmation Dialog
        String action = isBuyTab ? "BUY" : "SELL";
        double price = q.getCurrentPrice();
        double total = qty * price;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Virtual Trade");
        confirm.setHeaderText(String.format("Confirm %s Market Order: %s", action, ticker));
        confirm.setContentText(String.format("Are you sure you want to %s %d shares of %s at ₹%,.2f?\nTotal Value: ₹%,.2f (Virtual Funds)",
                action, qty, ticker, price, total));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (isBuyTab) {
                    tradingEngine.executeBuy(u.getId(), ticker, qty, price);
                    ToastNotification.show(this, "Order Executed", String.format("Successfully bought %d shares of %s!", qty, ticker), ToastNotification.Type.SUCCESS);
                } else {
                    tradingEngine.executeSell(u.getId(), ticker, qty, price);
                    ToastNotification.show(this, "Order Executed", String.format("Successfully sold %d shares of %s!", qty, ticker), ToastNotification.Type.SUCCESS);
                }
                authService.refreshCurrentProfile();
                refresh();
            } catch (Exception ex) {
                ToastNotification.show(this, "Trade Failed", ex.getMessage(), ToastNotification.Type.ERROR);
            }
        }
    }

    private void handleCreateAlert() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        try {
            double target = Double.parseDouble(alertPriceField.getText().trim());
            if (target <= 0) throw new NumberFormatException();

            AlertCondition cond = alertConditionBox.getValue().contains("ABOVE") ? AlertCondition.ABOVE : AlertCondition.BELOW;
            alertService.createAlert(u.getId(), ticker, cond, BigDecimal.valueOf(target));
            ToastNotification.show(this, "Price Alert Set", String.format("We will notify you when %s reaches ₹%,.2f.", ticker, target), ToastNotification.Type.SUCCESS);
        } catch (Exception e) {
            ToastNotification.show(this, "Invalid Target Price", "Please enter a valid target price.", ToastNotification.Type.ERROR);
        }
    }
}
