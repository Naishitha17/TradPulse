package com.tradepulse.ui.views;

import com.tradepulse.model.Order;
import com.tradepulse.model.Transaction;
import com.tradepulse.model.User;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.PortfolioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrdersView extends VBox {
    private final AuthService authService;
    private final PortfolioService portfolioService;

    private boolean showOrders = true;
    private final TableView<Order> ordersTable = new TableView<>();
    private final TableView<Transaction> transactionsTable = new TableView<>();
    private final ObservableList<Order> orderData = FXCollections.observableArrayList();
    private final ObservableList<Transaction> txData = FXCollections.observableArrayList();

    private final Button ordersTabBtn = new Button("Order Executions (Trades)");
    private final Button txTabBtn = new Button("Transaction Cash Ledger");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OrdersView(AuthService authService, PortfolioService portfolioService) {
        this.authService = authService;
        this.portfolioService = portfolioService;

        setSpacing(16);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");

        buildUI();
        refresh();
    }

    private void buildUI() {
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        ordersTabBtn.getStyleClass().add("btn-primary");
        txTabBtn.getStyleClass().add("btn-secondary");

        ordersTabBtn.setOnAction(e -> {
            showOrders = true;
            ordersTabBtn.getStyleClass().setAll("btn-primary");
            txTabBtn.getStyleClass().setAll("btn-secondary");
            ordersTable.setVisible(true);
            ordersTable.setManaged(true);
            transactionsTable.setVisible(false);
            transactionsTable.setManaged(false);
        });

        txTabBtn.setOnAction(e -> {
            showOrders = false;
            txTabBtn.getStyleClass().setAll("btn-primary");
            ordersTabBtn.getStyleClass().setAll("btn-secondary");
            ordersTable.setVisible(false);
            ordersTable.setManaged(false);
            transactionsTable.setVisible(true);
            transactionsTable.setManaged(true);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("↻ Refresh Ledger");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());

        topBar.getChildren().addAll(ordersTabBtn, txTabBtn, spacer, refreshBtn);

        // 1. Setup Orders Table
        ordersTable.setItems(orderData);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(ordersTable, Priority.ALWAYS);

        TableColumn<Order, String> idCol = new TableColumn<>("#ID");
        idCol.setPrefWidth(60);
        idCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));

        TableColumn<Order, String> typeCol = new TableColumn<>("SIDE");
        typeCol.setPrefWidth(80);
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(item.equalsIgnoreCase("BUY") ? "badge-cyan" : "badge-red");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Order, String> tickerCol = new TableColumn<>("TICKER");
        tickerCol.setPrefWidth(90);
        tickerCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTicker()));
        tickerCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            }
        });

        TableColumn<Order, String> qtyCol = new TableColumn<>("QUANTITY");
        qtyCol.setPrefWidth(90);
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,d", d.getValue().getQuantity())));

        TableColumn<Order, String> priceCol = new TableColumn<>("EXEC PRICE");
        priceCol.setPrefWidth(100);
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getPrice())));

        TableColumn<Order, String> feeCol = new TableColumn<>("FEE");
        feeCol.setPrefWidth(80);
        feeCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getFee())));

        TableColumn<Order, String> totalCol = new TableColumn<>("NET AMOUNT");
        totalCol.setPrefWidth(120);
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getTotalAmount())));

        TableColumn<Order, String> pnlCol = new TableColumn<>("REALIZED P&L");
        pnlCol.setPrefWidth(120);
        pnlCol.setCellValueFactory(d -> {
            BigDecimal p = d.getValue().getRealizedPnl();
            if (p.compareTo(BigDecimal.ZERO) == 0 && d.getValue().getType().name().equals("BUY")) {
                return new SimpleStringProperty("—");
            }
            return new SimpleStringProperty(String.format("%s₹%,.2f", p.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", p));
        });
        pnlCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if (item.startsWith("+")) setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    else if (item.startsWith("-")) setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #94a3b8;");
                }
            }
        });

        TableColumn<Order, String> dateCol = new TableColumn<>("DATE & TIME");
        dateCol.setPrefWidth(160);
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FMT) : "—"));

        ordersTable.getColumns().addAll(List.of(idCol, typeCol, tickerCol, qtyCol, priceCol, feeCol, totalCol, pnlCol, dateCol));

        // 2. Setup Transactions Table
        transactionsTable.setItems(txData);
        transactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        transactionsTable.setVisible(false);
        transactionsTable.setManaged(false);
        VBox.setVgrow(transactionsTable, Priority.ALWAYS);

        TableColumn<Transaction, String> txIdCol = new TableColumn<>("#TX");
        txIdCol.setPrefWidth(60);
        txIdCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));

        TableColumn<Transaction, String> txTypeCol = new TableColumn<>("TYPE");
        txTypeCol.setPrefWidth(100);
        txTypeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));

        TableColumn<Transaction, String> txAmtCol = new TableColumn<>("CASH IMPACT");
        txAmtCol.setPrefWidth(120);
        txAmtCol.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%s₹%,.2f", d.getValue().getAmount().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", d.getValue().getAmount())));
        txAmtCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle(item.startsWith("+") ? "-fx-text-fill: #10b981; -fx-font-weight: bold;" : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Transaction, String> txBalCol = new TableColumn<>("BALANCE AFTER");
        txBalCol.setPrefWidth(130);
        txBalCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getBalanceAfter())));

        TableColumn<Transaction, String> txDescCol = new TableColumn<>("DESCRIPTION");
        txDescCol.setPrefWidth(260);
        txDescCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        TableColumn<Transaction, String> txDateCol = new TableColumn<>("TIMESTAMP");
        txDateCol.setPrefWidth(160);
        txDateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FMT) : "—"));

        transactionsTable.getColumns().addAll(List.of(txIdCol, txTypeCol, txAmtCol, txBalCol, txDescCol, txDateCol));

        getChildren().addAll(topBar, ordersTable, transactionsTable);
    }

    public void refresh() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        List<Order> orders = portfolioService.getOrders(u.getId());
        orderData.setAll(orders);

        List<Transaction> txs = portfolioService.getTransactions(u.getId());
        txData.setAll(txs);
    }
}
