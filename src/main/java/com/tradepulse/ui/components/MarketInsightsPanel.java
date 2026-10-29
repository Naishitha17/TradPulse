package com.tradepulse.ui.components;

import com.tradepulse.model.NewsArticle;
import com.tradepulse.model.StockQuote;
import com.tradepulse.model.TraderRanking;
import com.tradepulse.service.LeaderboardService;
import com.tradepulse.service.MarketDataService;
import com.tradepulse.service.NewsService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class MarketInsightsPanel extends VBox {
    private final MarketDataService marketDataService;
    private final NewsService newsService;
    private final LeaderboardService leaderboardService;
    private final Consumer<String> onSelectTicker;
    private final Consumer<TraderRanking> onSelectTrader;

    private final VBox newsContainer = new VBox(8);
    private final VBox gainersContainer = new VBox(6);
    private final VBox losersContainer = new VBox(6);
    private final VBox tradersContainer = new VBox(6);

    private Timeline newsScrollTimeline;
    private ScrollPane newsScrollPane;
    private double scrollPos = 0;
    private boolean isHovered = false;

    public MarketInsightsPanel(MarketDataService marketDataService, NewsService newsService,
                               LeaderboardService leaderboardService, Consumer<String> onSelectTicker,
                               Consumer<TraderRanking> onSelectTrader) {
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.leaderboardService = leaderboardService;
        this.onSelectTicker = onSelectTicker;
        this.onSelectTrader = onSelectTrader;

        setPrefWidth(280);
        setMinWidth(260);
        setMaxWidth(320);
        setStyle("-fx-background-color: #0d1322; -fx-border-color: #1e293b; -fx-border-width: 0 0 0 1;");
        setPadding(new Insets(16, 14, 16, 14));
        setSpacing(16);

        buildUI();
        refreshData();

        // Listen for live quote ticks to refresh gainers/losers
        marketDataService.addQuoteListener(q -> Platform.runLater(this::refreshMovers));
    }

    private void buildUI() {
        Label headerTitle = new Label("Market Insights");
        headerTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        // 1. Live Financial News Scrolling Marquee
        Label newsLabel = new Label("LIVE FINANCIAL PULSE");
        newsLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #06b6d4; -fx-letter-spacing: 1px;");

        newsScrollPane = new ScrollPane(newsContainer);
        newsScrollPane.setFitToWidth(true);
        newsScrollPane.setPrefHeight(130);
        newsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        newsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        newsScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // Pause auto-scroll on mouse hover
        newsScrollPane.setOnMouseEntered(e -> isHovered = true);
        newsScrollPane.setOnMouseExited(e -> isHovered = false);

        newsScrollTimeline = new Timeline(new KeyFrame(Duration.millis(80), e -> {
            if (!isHovered && newsScrollPane != null) {
                scrollPos += 0.005;
                if (scrollPos > 1.0) scrollPos = 0;
                newsScrollPane.setVvalue(scrollPos);
            }
        }));
        newsScrollTimeline.setCycleCount(Animation.INDEFINITE);
        newsScrollTimeline.play();

        // 2. Top Movers (Gainers & Losers)
        Label gainersLabel = new Label("TOP GAINERS");
        gainersLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        Label losersLabel = new Label("TOP LOSERS");
        losersLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");

        // 3. Top Traders Leaderboard
        Label tradersLabel = new Label("TOP TRADERS (BY % RETURN)");
        tradersLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #8b5cf6;");

        ScrollPane rootScroll = new ScrollPane();
        rootScroll.setFitToWidth(true);
        rootScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(rootScroll, Priority.ALWAYS);

        VBox content = new VBox(14);
        content.getChildren().addAll(
                newsLabel, newsScrollPane,
                new Separator(),
                gainersLabel, gainersContainer,
                new Separator(),
                losersLabel, losersContainer,
                new Separator(),
                tradersLabel, tradersContainer
        );
        rootScroll.setContent(content);

        getChildren().addAll(headerTitle, rootScroll);
    }

    public void refreshData() {
        refreshNews();
        refreshMovers();
        refreshTraders();
    }

    private void refreshNews() {
        newsContainer.getChildren().clear();
        List<NewsArticle> latest = newsService.getLatestNews();
        for (NewsArticle n : latest) {
            VBox item = new VBox(2);
            item.setStyle("-fx-background-color: #111827; -fx-padding: 8; -fx-background-radius: 6; -fx-border-color: #1f2937; -fx-border-radius: 6;");

            Label title = new Label(n.getTitle());
            title.setWrapText(true);
            title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

            HBox meta = new HBox(6);
            meta.setAlignment(Pos.CENTER_LEFT);
            Label src = new Label(n.getSource());
            src.setStyle("-fx-font-size: 9px; -fx-text-fill: #06b6d4; -fx-font-weight: bold;");

            meta.getChildren().add(src);
            if (n.getRelatedTicker() != null) {
                Label tick = new Label(n.getRelatedTicker());
                tick.setStyle("-fx-font-size: 9px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
                meta.getChildren().add(tick);
            }

            item.getChildren().addAll(title, meta);
            if (n.getRelatedTicker() != null) {
                item.setCursor(javafx.scene.Cursor.HAND);
                item.setOnMouseClicked(e -> {
                    if (onSelectTicker != null) onSelectTicker.accept(n.getRelatedTicker());
                });
            }
            newsContainer.getChildren().add(item);
        }
    }

    private void refreshMovers() {
        gainersContainer.getChildren().clear();
        List<StockQuote> gainers = marketDataService.getTopGainers(5);
        for (StockQuote q : gainers) {
            gainersContainer.getChildren().add(createMoverRow(q, true));
        }

        losersContainer.getChildren().clear();
        List<StockQuote> losers = marketDataService.getTopLosers(5);
        for (StockQuote q : losers) {
            losersContainer.getChildren().add(createMoverRow(q, false));
        }
    }

    private HBox createMoverRow(StockQuote q, boolean isGainer) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 6, 4, 6));
        row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 6; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;"));
        row.setOnMouseClicked(e -> {
            if (onSelectTicker != null) onSelectTicker.accept(q.getTicker());
        });

        Label ticker = new Label(q.getTicker());
        ticker.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label price = new Label(String.format("₹%.2f", q.getCurrentPrice()));
        price.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

        Label change = new Label(String.format("%s%.2f%%", q.getChange() >= 0 ? "+" : "", q.getChangePercent()));
        change.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 2 6 2 6; -fx-background-radius: 4;",
                isGainer ? "#10b981" : "#ef4444",
                isGainer ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)"));

        row.getChildren().addAll(ticker, spacer, price, change);
        return row;
    }

    private void refreshTraders() {
        tradersContainer.getChildren().clear();
        List<TraderRanking> top = leaderboardService.getTopTraders(5);
        int rank = 1;
        for (TraderRanking t : top) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 6, 4, 6));
            row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 6; -fx-cursor: hand;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;"));
            row.setOnMouseClicked(e -> {
                if (onSelectTrader != null) onSelectTrader.accept(t);
            });

            Label rankLbl = new Label("#" + rank);
            rankLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b5cf6;");

            VBox info = new VBox(1);
            Label name = new Label(t.getDisplayName());
            name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            Label handle = new Label("@" + t.getUsername());
            handle.setStyle("-fx-font-size: 9px; -fx-text-fill: #94a3b8;");
            info.getChildren().addAll(name, handle);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label returnLbl = new Label(String.format("%s%.1f%%", t.getReturnPercent() >= 0 ? "+" : "", t.getReturnPercent()));
            returnLbl.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: %s;",
                    t.getReturnPercent() >= 0 ? "#10b981" : "#ef4444"));

            row.getChildren().addAll(rankLbl, info, spacer, returnLbl);
            tradersContainer.getChildren().add(row);
            rank++;
        }
    }
}
