package com.tradepulse.ui.views;

import com.tradepulse.model.HoldingViewItem;
import com.tradepulse.model.PortfolioSummary;
import com.tradepulse.model.User;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.MarketDataService;
import com.tradepulse.service.PortfolioService;
import com.tradepulse.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class PortfolioView extends ScrollPane {
    private final AuthService authService;
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;
    private final Consumer<String> onSelectTicker;

    private final Label portfolioValLabel = new Label("₹0.00");
    private final Label cashValLabel = new Label("₹0.00");
    private final Label investedValLabel = new Label("₹0.00");
    private final Label unrealizedPnlLabel = new Label("₹0.00");
    private final Label realizedPnlLabel = new Label("₹0.00");
    private final Label totalReturnLabel = new Label("0.00%");

    private final PieChart allocationChart = new PieChart();
    private final TableView<HoldingViewItem> holdingsTable = new TableView<>();
    private final ObservableList<HoldingViewItem> tableData = FXCollections.observableArrayList();

    public PortfolioView(AuthService authService, MarketDataService marketDataService,
                         PortfolioService portfolioService, Consumer<String> onSelectTicker) {
        this.authService = authService;
        this.marketDataService = marketDataService;
        this.portfolioService = portfolioService;
        this.onSelectTicker = onSelectTicker;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        buildUI();
        refresh();

        marketDataService.addQuoteListener(q -> Platform.runLater(this::refreshMetricsOnly));
    }

    private void buildUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        // 1. Header with Reset Portfolio Action
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Portfolio Valuation & Asset Holdings");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subtitle = new Label("Track real-time capital allocation, profit/loss, and open positions");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button resetBtn = new Button("🔄 Reset Account (₹1,00,000)");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> handleResetPortfolio());

        header.getChildren().addAll(titleBox, spacer, resetBtn);

        // 2. Metrics Grid (6 metrics)
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(16);
        metricsGrid.setVgap(16);

        VBox c1 = createMetricCard("TOTAL PORTFOLIO VALUE", portfolioValLabel);
        VBox c2 = createMetricCard("AVAILABLE CASH", cashValLabel);
        VBox c3 = createMetricCard("INVESTED CAPITAL", investedValLabel);
        VBox c4 = createMetricCard("UNREALIZED P&L", unrealizedPnlLabel);
        VBox c5 = createMetricCard("REALIZED P&L", realizedPnlLabel);
        VBox c6 = createMetricCard("TOTAL RETURN", totalReturnLabel);

        GridPane.setHgrow(c1, Priority.ALWAYS);
        GridPane.setHgrow(c2, Priority.ALWAYS);
        GridPane.setHgrow(c3, Priority.ALWAYS);
        GridPane.setHgrow(c4, Priority.ALWAYS);
        GridPane.setHgrow(c5, Priority.ALWAYS);
        GridPane.setHgrow(c6, Priority.ALWAYS);

        metricsGrid.add(c1, 0, 0);
        metricsGrid.add(c2, 1, 0);
        metricsGrid.add(c3, 2, 0);
        metricsGrid.add(c4, 0, 1);
        metricsGrid.add(c5, 1, 1);
        metricsGrid.add(c6, 2, 1);

        // 3. Middle Section: Sector Allocation Chart
        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("panel-card");
        Label chartTitle = new Label("Sector Allocation Breakdown");
        chartTitle.getStyleClass().add("panel-header-title");

        allocationChart.setLabelsVisible(true);
        allocationChart.setLegendVisible(true);
        allocationChart.setPrefHeight(260);
        allocationChart.setStyle("-fx-background-color: transparent;");

        chartCard.getChildren().addAll(chartTitle, allocationChart);

        // 4. Holdings Table
        VBox tableCard = new VBox(10);
        tableCard.getStyleClass().add("panel-card");
        Label tableTitle = new Label("Current Open Holdings");
        tableTitle.getStyleClass().add("panel-header-title");

        holdingsTable.setItems(tableData);
        holdingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        holdingsTable.setPrefHeight(340);

        TableColumn<HoldingViewItem, String> tickerCol = new TableColumn<>("TICKER");
        tickerCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTicker()));
        tickerCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #06b6d4;");
            }
        });

        TableColumn<HoldingViewItem, String> nameCol = new TableColumn<>("COMPANY");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<HoldingViewItem, String> sectorCol = new TableColumn<>("SECTOR");
        sectorCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSector()));

        TableColumn<HoldingViewItem, String> sharesCol = new TableColumn<>("SHARES");
        sharesCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getShares())));

        TableColumn<HoldingViewItem, String> avgCostCol = new TableColumn<>("AVG COST");
        avgCostCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getAverageBuyPrice())));

        TableColumn<HoldingViewItem, String> curPriceCol = new TableColumn<>("MARKET PRICE");
        curPriceCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getCurrentPrice())));

        TableColumn<HoldingViewItem, String> costCol = new TableColumn<>("TOTAL COST");
        costCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getTotalCost())));

        TableColumn<HoldingViewItem, String> valCol = new TableColumn<>("MARKET VALUE");
        valCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getCurrentMarketValue())));

        TableColumn<HoldingViewItem, String> pnlCol = new TableColumn<>("UNREALIZED P&L");
        pnlCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%s₹%,.2f (%s%.2f%%)",
                d.getValue().getUnrealizedPnl().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
                d.getValue().getUnrealizedPnl(),
                d.getValue().getUnrealizedPnlPercent() >= 0 ? "+" : "",
                d.getValue().getUnrealizedPnlPercent())));
        pnlCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(item.startsWith("+") ? "badge-green" : "badge-red");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<HoldingViewItem, String> allocCol = new TableColumn<>("ALLOCATION");
        allocCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f%%", d.getValue().getAllocationPercent())));

        TableColumn<HoldingViewItem, String> actionCol = new TableColumn<>("ACTION");
        actionCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTicker()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button tradeBtn = new Button("Trade");
            {
                tradeBtn.getStyleClass().add("btn-primary");
                tradeBtn.setStyle("-fx-padding: 3 10 3 10; -fx-font-size: 11px;");
                tradeBtn.setOnAction(e -> {
                    HoldingViewItem item = getTableView().getItems().get(getIndex());
                    if (onSelectTicker != null) onSelectTicker.accept(item.getTicker());
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : tradeBtn);
            }
        });

        holdingsTable.getColumns().addAll(List.of(tickerCol, nameCol, sectorCol, sharesCol, avgCostCol, curPriceCol, costCol, valCol, pnlCol, allocCol, actionCol));
        tableCard.getChildren().addAll(tableTitle, holdingsTable);

        root.getChildren().addAll(header, metricsGrid, chartCard, tableCard);
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
        if (u == null) return;

        refreshMetricsOnly();

        // Refresh Holdings Table Data
        List<HoldingViewItem> items = portfolioService.getHoldingViewItems(u.getId(), marketDataService);
        tableData.setAll(items);

        // Refresh Sector Allocation Pie Chart
        Map<String, Double> sectorMap = new HashMap<>();
        for (HoldingViewItem item : items) {
            sectorMap.merge(item.getSector(), item.getCurrentMarketValue().doubleValue(), Double::sum);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : sectorMap.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey() + String.format(" (₹%,.0f)", entry.getValue()), entry.getValue()));
        }

        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("100% Cash Balance", 100));
        }
        allocationChart.setData(pieData);
    }

    private void refreshMetricsOnly() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        PortfolioSummary s = portfolioService.getPortfolioSummary(u.getId(), marketDataService);
        portfolioValLabel.setText(String.format("₹%,.2f", s.getTotalPortfolioValue()));
        cashValLabel.setText(String.format("₹%,.2f", s.getCashBalance()));
        investedValLabel.setText(String.format("₹%,.2f", s.getInvestedValue()));

        double unPnl = s.getTotalUnrealizedPnl().doubleValue();
        unrealizedPnlLabel.setText(String.format("%s₹%,.2f", unPnl >= 0 ? "+" : "", unPnl));
        unrealizedPnlLabel.setStyle(String.format("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s;",
                unPnl >= 0 ? "#10b981" : "#ef4444"));

        double rPnl = s.getTotalRealizedPnl().doubleValue();
        realizedPnlLabel.setText(String.format("%s₹%,.2f", rPnl >= 0 ? "+" : "", rPnl));
        realizedPnlLabel.setStyle(String.format("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s;",
                rPnl >= 0 ? "#10b981" : "#ef4444"));

        double ret = s.getTotalReturnPercent();
        totalReturnLabel.setText(String.format("%s%.2f%%", ret >= 0 ? "+" : "", ret));
        totalReturnLabel.setStyle(String.format("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s;",
                ret >= 0 ? "#10b981" : "#ef4444"));
    }

    private void handleResetPortfolio() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Virtual Account");
        confirm.setHeaderText("Confirm Virtual Portfolio Reset");
        confirm.setContentText("Are you sure you want to reset your account? All active virtual holdings will be cleared, and your virtual balance will be restored to ₹1,00,000.00.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            portfolioService.resetAccount(u.getId(), new BigDecimal("100000.00"));
            authService.refreshCurrentProfile();
            refresh();
            ToastNotification.show(this, "Account Reset", "Portfolio restored to starting balance of ₹1,00,000.00.", ToastNotification.Type.SUCCESS);
        }
    }
}
