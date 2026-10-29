package com.tradepulse.ui.views;

import com.tradepulse.model.StockQuote;
import com.tradepulse.model.User;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.MarketDataService;
import com.tradepulse.service.PortfolioService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MarketWatchView extends VBox {
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;
    private final AuthService authService;
    private final Consumer<String> onSelectTicker;

    private final TextField searchField = new TextField();
    private final ComboBox<String> sectorFilter = new ComboBox<>();
    private final ComboBox<String> movementFilter = new ComboBox<>();
    private final TableView<StockQuote> table = new TableView<>();
    private final ObservableList<StockQuote> tableData = FXCollections.observableArrayList();

    public MarketWatchView(MarketDataService marketDataService, PortfolioService portfolioService,
                           AuthService authService, Consumer<String> onSelectTicker) {
        this.marketDataService = marketDataService;
        this.portfolioService = portfolioService;
        this.authService = authService;
        this.onSelectTicker = onSelectTicker;

        setSpacing(16);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");

        buildUI();
        refresh();

        // Listen for live quote ticks to refresh prices smoothly
        marketDataService.addQuoteListener(q -> Platform.runLater(table::refresh));
    }

    private void buildUI() {
        // Header and Search/Filter Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search by ticker or company name...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTable());

        sectorFilter.getItems().add("All Sectors");
        sectorFilter.getItems().addAll(marketDataService.getSectors());
        sectorFilter.setValue("All Sectors");
        sectorFilter.setOnAction(e -> filterTable());

        movementFilter.getItems().addAll("All Movements", "Gainers (+)", "Losers (-)");
        movementFilter.setValue("All Movements");
        movementFilter.setOnAction(e -> filterTable());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label("60+ Global & Bluechip Companies Listed");
        countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        topBar.getChildren().addAll(searchField, sectorFilter, movementFilter, spacer, countLabel);

        // Table Configuration
        table.setItems(tableData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        // 1. Watchlist Star Column
        TableColumn<StockQuote, String> starCol = new TableColumn<>("★");
        starCol.setPrefWidth(45);
        starCol.setMaxWidth(50);
        starCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTicker()));
        starCol.setCellFactory(col -> new TableCell<>() {
            private final Button starBtn = new Button();
            {
                starBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 13px;");
                starBtn.setOnAction(e -> {
                    StockQuote q = getTableView().getItems().get(getIndex());
                    User u = authService.getCurrentUser();
                    if (u != null) {
                        if (portfolioService.isInWatchlist(u.getId(), q.getTicker())) {
                            portfolioService.removeFromWatchlist(u.getId(), q.getTicker());
                            starBtn.setText("☆");
                            starBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #64748b;");
                        } else {
                            portfolioService.addToWatchlist(u.getId(), q.getTicker());
                            starBtn.setText("★");
                            starBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #f59e0b;");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String ticker, boolean empty) {
                super.updateItem(ticker, empty);
                if (empty || ticker == null) {
                    setGraphic(null);
                } else {
                    User u = authService.getCurrentUser();
                    boolean inWatchlist = u != null && portfolioService.isInWatchlist(u.getId(), ticker);
                    starBtn.setText(inWatchlist ? "★" : "☆");
                    starBtn.setStyle(inWatchlist ? "-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #f59e0b;"
                            : "-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #64748b;");
                    setGraphic(starBtn);
                }
            }
        });

        // 2. Ticker Column
        TableColumn<StockQuote, String> tickerCol = new TableColumn<>("TICKER");
        tickerCol.setPrefWidth(85);
        tickerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTicker()));
        tickerCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #06b6d4;");
                }
            }
        });

        // 3. Name Column
        TableColumn<StockQuote, String> nameCol = new TableColumn<>("COMPANY");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        // 4. Sector Column
        TableColumn<StockQuote, String> sectorCol = new TableColumn<>("SECTOR");
        sectorCol.setPrefWidth(130);
        sectorCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSector()));

        // 5. Price Column
        TableColumn<StockQuote, String> priceCol = new TableColumn<>("PRICE");
        priceCol.setPrefWidth(100);
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("₹%,.2f", data.getValue().getCurrentPrice())));
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            }
        });

        // 6. Change Column
        TableColumn<StockQuote, String> changeCol = new TableColumn<>("CHANGE (%)");
        changeCol.setPrefWidth(110);
        changeCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%s%.2f%%",
                data.getValue().getChange() >= 0 ? "+" : "", data.getValue().getChangePercent())));
        changeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    boolean isGainer = item.startsWith("+");
                    badge.getStyleClass().add(isGainer ? "badge-green" : "badge-red");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // 7. Day High / Low
        TableColumn<StockQuote, String> rangeCol = new TableColumn<>("DAY RANGE");
        rangeCol.setPrefWidth(130);
        rangeCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("₹%,.2f - ₹%,.2f",
                data.getValue().getDayLow(), data.getValue().getDayHigh())));

        // 8. Volume
        TableColumn<StockQuote, String> volCol = new TableColumn<>("VOLUME");
        volCol.setPrefWidth(90);
        volCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%,d", data.getValue().getVolume())));

        // 9. Action Button
        TableColumn<StockQuote, String> actionCol = new TableColumn<>("ACTION");
        actionCol.setPrefWidth(90);
        actionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTicker()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button tradeBtn = new Button("Trade");
            {
                tradeBtn.getStyleClass().add("btn-primary");
                tradeBtn.setStyle("-fx-padding: 4 12 4 12; -fx-font-size: 11px;");
                tradeBtn.setOnAction(e -> {
                    StockQuote q = getTableView().getItems().get(getIndex());
                    if (onSelectTicker != null) onSelectTicker.accept(q.getTicker());
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : tradeBtn);
            }
        });

        table.getColumns().addAll(List.of(starCol, tickerCol, nameCol, sectorCol, priceCol, changeCol, rangeCol, volCol, actionCol));

        // Click row to navigate
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                StockQuote selected = table.getSelectionModel().getSelectedItem();
                if (selected != null && onSelectTicker != null) {
                    onSelectTicker.accept(selected.getTicker());
                }
            }
        });

        getChildren().addAll(topBar, table);
    }

    public void refresh() {
        filterTable();
    }

    private void filterTable() {
        String search = searchField.getText();
        String sector = sectorFilter.getValue();
        if ("All Sectors".equals(sector)) sector = "ALL";

        String move = movementFilter.getValue();
        String moveParam = "ALL";
        if ("Gainers (+)".equals(move)) moveParam = "GAINERS";
        if ("Losers (-)".equals(move)) moveParam = "LOSERS";

        List<StockQuote> filtered = marketDataService.searchStocks(search, sector, moveParam);
        tableData.setAll(filtered);
    }
}
