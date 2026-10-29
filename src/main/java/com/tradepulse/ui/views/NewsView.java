package com.tradepulse.ui.views;

import com.tradepulse.model.NewsArticle;
import com.tradepulse.service.NewsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class NewsView extends ScrollPane {
    private final NewsService newsService;
    private final Consumer<String> onSelectTicker;

    private final TextField searchField = new TextField();
    private final VBox articlesContainer = new VBox(12);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm");

    public NewsView(NewsService newsService, Consumer<String> onSelectTicker) {
        this.newsService = newsService;
        this.onSelectTicker = onSelectTicker;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        buildUI();
        refresh();
    }

    private void buildUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        // Header and Search
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Global Financial & Market News");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subtitle = new Label("Curated macroeconomic reports, company earnings, and market intelligence");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField.setPromptText("Filter news headlines or tickers...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterNews());

        topBar.getChildren().addAll(titleBox, spacer, searchField);

        // API Status Notice Banner
        HBox noticeBanner = new HBox(10);
        noticeBanner.setAlignment(Pos.CENTER_LEFT);
        noticeBanner.setStyle("-fx-background-color: #111827; -fx-border-color: #1e293b; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 16 10 16;");

        Label noticeIcon = new Label("ℹ");
        noticeIcon.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 14px; -fx-font-weight: bold;");

        String statusMsg = newsService.hasValidApiKey()
                ? "Live news feed connected directly to NewsAPI business endpoint."
                : "Market News Simulator Active. To connect a live NewsAPI subscription, set the NEWS_API_KEY environment variable.";
        Label noticeText = new Label(statusMsg);
        noticeText.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");

        noticeBanner.getChildren().addAll(noticeIcon, noticeText);

        root.getChildren().addAll(topBar, noticeBanner, articlesContainer);
        setContent(root);
    }

    public void refresh() {
        filterNews();
    }

    private void filterNews() {
        articlesContainer.getChildren().clear();
        String q = searchField.getText();
        List<NewsArticle> all = newsService.getLatestNews();

        List<NewsArticle> filtered = all.stream()
                .filter(a -> {
                    if (q == null || q.isBlank()) return true;
                    String qL = q.toLowerCase();
                    return a.getTitle().toLowerCase().contains(qL) ||
                           a.getDescription().toLowerCase().contains(qL) ||
                           (a.getRelatedTicker() != null && a.getRelatedTicker().toLowerCase().contains(qL));
                })
                .toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("No financial news articles matching your search query.");
            empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            articlesContainer.getChildren().add(empty);
            return;
        }

        for (NewsArticle a : filtered) {
            VBox card = new VBox(8);
            card.getStyleClass().add("panel-card");

            HBox cardHeader = new HBox(8);
            cardHeader.setAlignment(Pos.CENTER_LEFT);

            Label srcLbl = new Label(a.getSource());
            srcLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #06b6d4;");

            Label timeLbl = new Label("•  " + a.getPublishedAt().format(DATE_FMT));
            timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            cardHeader.getChildren().addAll(srcLbl, timeLbl, sp);

            if (a.getRelatedTicker() != null) {
                Label tickBadge = new Label(a.getRelatedTicker());
                tickBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 4; -fx-cursor: hand;");
                tickBadge.setOnMouseClicked(e -> {
                    if (onSelectTicker != null) onSelectTicker.accept(a.getRelatedTicker());
                });
                cardHeader.getChildren().add(tickBadge);
            }

            Label title = new Label(a.getTitle());
            title.setWrapText(true);
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

            Label desc = new Label(a.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

            HBox footer = new HBox(12);
            footer.setAlignment(Pos.CENTER_LEFT);

            Button openBtn = new Button("Read Full Coverage ↗");
            openBtn.getStyleClass().add("btn-secondary");
            openBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 12 4 12;");
            openBtn.setOnAction(e -> {
                // In desktop apps, opens system default browser if supported
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(a.getUrl()));
                } catch (Exception ignored) {}
            });

            footer.getChildren().add(openBtn);
            card.getChildren().addAll(cardHeader, title, desc, footer);
            articlesContainer.getChildren().add(card);
        }
    }
}
