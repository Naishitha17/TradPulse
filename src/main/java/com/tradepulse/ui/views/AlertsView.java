package com.tradepulse.ui.views;

import com.tradepulse.model.AlertCondition;
import com.tradepulse.model.PriceAlert;
import com.tradepulse.model.StockQuote;
import com.tradepulse.model.User;
import com.tradepulse.service.AlertService;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.MarketDataService;
import com.tradepulse.ui.components.ToastNotification;
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

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AlertsView extends VBox {
    private final AuthService authService;
    private final AlertService alertService;
    private final MarketDataService marketDataService;

    private final TextField tickerField = new TextField();
    private final ComboBox<String> conditionBox = new ComboBox<>();
    private final TextField targetPriceField = new TextField();
    private final Button createBtn = new Button("Set Price Alert");

    private final TableView<PriceAlert> alertTable = new TableView<>();
    private final ObservableList<PriceAlert> alertData = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AlertsView(AuthService authService, AlertService alertService, MarketDataService marketDataService) {
        this.authService = authService;
        this.alertService = alertService;
        this.marketDataService = marketDataService;

        setSpacing(20);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");

        buildUI();
        refresh();
    }

    private void buildUI() {
        VBox titleBox = new VBox(2);
        Label title = new Label("Price Target Alerts & Notifications");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subtitle = new Label("Receive instant notifications when market prices hit your specified threshold");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, subtitle);

        // 1. Create Alert Card
        VBox createCard = new VBox(10);
        createCard.getStyleClass().add("panel-card");

        Label formTitle = new Label("Create New Target Price Alert");
        formTitle.getStyleClass().add("panel-header-title");

        HBox formRow = new HBox(12);
        formRow.setAlignment(Pos.CENTER_LEFT);

        tickerField.setPromptText("Ticker (e.g. AAPL, NVDA)");
        tickerField.setPrefWidth(180);

        conditionBox.getItems().addAll("Price rises ABOVE (≥)", "Price drops BELOW (≤)");
        conditionBox.setValue("Price rises ABOVE (≥)");
        conditionBox.setPrefWidth(200);

        targetPriceField.setPromptText("Target Price (₹)");
        targetPriceField.setPrefWidth(160);

        createBtn.getStyleClass().add("btn-primary");
        createBtn.setOnAction(e -> handleCreateAlert());

        formRow.getChildren().addAll(tickerField, conditionBox, targetPriceField, createBtn);
        createCard.getChildren().addAll(formTitle, formRow);

        // 2. Active & Triggered Alerts Table
        VBox tableCard = new VBox(10);
        tableCard.getStyleClass().add("panel-card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        Label tableTitle = new Label("Your Configured Alerts");
        tableTitle.getStyleClass().add("panel-header-title");

        alertTable.setItems(alertData);
        alertTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(alertTable, Priority.ALWAYS);

        TableColumn<PriceAlert, String> tickerCol = new TableColumn<>("STOCK");
        tickerCol.setPrefWidth(100);
        tickerCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTicker()));
        tickerCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #06b6d4;");
            }
        });

        TableColumn<PriceAlert, String> condCol = new TableColumn<>("CONDITION");
        condCol.setPrefWidth(120);
        condCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCondition() == AlertCondition.ABOVE ? "Rises Above (≥)" : "Drops Below (≤)"));

        TableColumn<PriceAlert, String> targetCol = new TableColumn<>("TARGET PRICE");
        targetCol.setPrefWidth(120);
        targetCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%,.2f", d.getValue().getTargetPrice())));
        targetCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            }
        });

        TableColumn<PriceAlert, String> curCol = new TableColumn<>("CURRENT PRICE");
        curCol.setPrefWidth(120);
        curCol.setCellValueFactory(d -> {
            StockQuote q = marketDataService.getStock(d.getValue().getTicker());
            return new SimpleStringProperty(q != null ? String.format("₹%,.2f", q.getCurrentPrice()) : "—");
        });

        TableColumn<PriceAlert, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setPrefWidth(110);
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isTriggered() ? "TRIGGERED" : "ACTIVE"));
        statusCol.setCellFactory(col -> new TableCell<>() {
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

        TableColumn<PriceAlert, String> dateCol = new TableColumn<>("CREATED");
        dateCol.setPrefWidth(140);
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FMT) : "—"));

        TableColumn<PriceAlert, String> actionCol = new TableColumn<>("ACTION");
        actionCol.setPrefWidth(90);
        actionCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("Delete");
            {
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setStyle("-fx-padding: 3 8 3 8; -fx-font-size: 11px;");
                delBtn.setOnAction(e -> {
                    PriceAlert alert = getTableView().getItems().get(getIndex());
                    User u = authService.getCurrentUser();
                    if (u != null) {
                        alertService.deleteAlert(alert.getId(), u.getId());
                        refresh();
                        ToastNotification.show(AlertsView.this, "Alert Deleted", "Alert removed successfully.", ToastNotification.Type.INFO);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        alertTable.getColumns().addAll(List.of(tickerCol, condCol, targetCol, curCol, statusCol, dateCol, actionCol));
        tableCard.getChildren().addAll(tableTitle, alertTable);

        getChildren().addAll(titleBox, createCard, tableCard);
    }

    public void refresh() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        List<PriceAlert> list = alertService.getUserAlerts(u.getId());
        alertData.setAll(list);
    }

    private void handleCreateAlert() {
        User u = authService.getCurrentUser();
        if (u == null) return;

        String ticker = tickerField.getText().trim().toUpperCase();
        StockQuote quote = marketDataService.getStock(ticker);
        if (quote == null) {
            ToastNotification.show(this, "Stock Not Found", "Please enter a valid stock ticker symbol.", ToastNotification.Type.ERROR);
            return;
        }

        try {
            double price = Double.parseDouble(targetPriceField.getText().trim());
            if (price <= 0) throw new NumberFormatException();

            AlertCondition cond = conditionBox.getValue().contains("ABOVE") ? AlertCondition.ABOVE : AlertCondition.BELOW;
            alertService.createAlert(u.getId(), ticker, cond, BigDecimal.valueOf(price));

            tickerField.clear();
            targetPriceField.clear();
            refresh();

            ToastNotification.show(this, "Alert Created", String.format("Alert set for %s at ₹%,.2f", ticker, price), ToastNotification.Type.SUCCESS);
        } catch (Exception e) {
            ToastNotification.show(this, "Invalid Price", "Please enter a valid positive target price.", ToastNotification.Type.ERROR);
        }
    }
}
