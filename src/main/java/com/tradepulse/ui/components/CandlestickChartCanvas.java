package com.tradepulse.ui.components;

import com.tradepulse.model.OHLC;
import com.tradepulse.util.ThemeManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CandlestickChartCanvas extends Pane {
    private final Canvas canvas;
    private List<OHLC> data = new ArrayList<>();
    private final StringProperty hudText = new SimpleStringProperty("Hover over candles to view OHLC data");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd");

    private double mouseX = -1;
    private double mouseY = -1;

    public CandlestickChartCanvas() {
        this.canvas = new Canvas();
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, oldVal, newVal) -> draw());
        heightProperty().addListener((obs, oldVal, newVal) -> draw());

        canvas.setOnMouseMoved(e -> {
            this.mouseX = e.getX();
            this.mouseY = e.getY();
            updateHoverHUD();
            draw();
        });

        canvas.setOnMouseExited(e -> {
            this.mouseX = -1;
            this.mouseY = -1;
            hudText.set("Hover over candles to view OHLC data");
            draw();
        });

        setPrefHeight(380);
        setMinHeight(250);
    }

    public StringProperty hudTextProperty() {
        return hudText;
    }

    public void setData(List<OHLC> ohlcList) {
        this.data = ohlcList != null ? new ArrayList<>(ohlcList) : new ArrayList<>();
        draw();
    }

    private void updateHoverHUD() {
        if (data == null || data.isEmpty() || mouseX < 0) return;

        double w = getWidth();
        double padLeft = 40;
        double padRight = 65;
        double chartWidth = w - padLeft - padRight;
        int count = data.size();

        if (chartWidth <= 0 || count == 0) return;

        double candleSpacing = chartWidth / count;
        int index = (int) ((mouseX - padLeft) / candleSpacing);

        if (index >= 0 && index < count) {
            OHLC o = data.get(index);
            String timeStr = o.getTimestamp().format(TIME_FMT);
            hudText.set(String.format("Time: %s  |  O: ₹%.2f  |  H: ₹%.2f  |  L: ₹%.2f  |  C: ₹%.2f  |  Vol: %,d",
                    timeStr, o.getOpen(), o.getHigh(), o.getLow(), o.getClose(), o.getVolume()));
        }
    }

    public void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        boolean isDark = ThemeManager.isDark();
        Color bg = isDark ? Color.web("#111827") : Color.web("#ffffff");
        Color gridColor = isDark ? Color.web("#1e293b", 0.6) : Color.web("#e2e8f0", 0.8);
        Color textColor = isDark ? Color.web("#94a3b8") : Color.web("#64748b");
        Color green = isDark ? Color.web("#10b981") : Color.web("#059669");
        Color red = isDark ? Color.web("#ef4444") : Color.web("#dc2626");
        Color maColor = isDark ? Color.web("#06b6d4") : Color.web("#0284c7");

        // Background
        gc.setFill(bg);
        gc.fillRect(0, 0, w, h);

        if (data == null || data.isEmpty()) {
            gc.setFill(textColor);
            gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
            gc.fillText("Awaiting market data...", w / 2 - 60, h / 2);
            return;
        }

        double padTop = 20;
        double padBottom = 35;
        double padLeft = 20;
        double padRight = 65;

        double chartW = w - padLeft - padRight;
        double chartH = h - padTop - padBottom;

        // Calculate min/max price and max volume
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        long maxVol = 1;

        for (OHLC o : data) {
            if (o.getLow() < minPrice) minPrice = o.getLow();
            if (o.getHigh() > maxPrice) maxPrice = o.getHigh();
            if (o.getVolume() > maxVol) maxVol = o.getVolume();
        }

        double priceMargin = (maxPrice - minPrice) * 0.08;
        if (priceMargin == 0) priceMargin = 1.0;
        double plotMin = minPrice - priceMargin;
        double plotMax = maxPrice + priceMargin;
        double priceRange = plotMax - plotMin;

        // Draw horizontal grid lines & price axis
        int gridLines = 5;
        gc.setStroke(gridColor);
        gc.setLineWidth(1.0);
        gc.setFont(Font.font("Segoe UI", 10));
        gc.setFill(textColor);

        for (int i = 0; i <= gridLines; i++) {
            double y = padTop + (chartH / gridLines) * i;
            gc.strokeLine(padLeft, y, w - padRight, y);

            double p = plotMax - (priceRange / gridLines) * i;
            gc.fillText(String.format("₹%.2f", p), w - padRight + 8, y + 4);
        }

        int count = data.size();
        double candleSpacing = chartW / count;
        double candleW = Math.max(2.0, candleSpacing * 0.7);

        // Draw Volume Histogram (bottom 25% of chart)
        double volMaxHeight = chartH * 0.22;
        for (int i = 0; i < count; i++) {
            OHLC o = data.get(i);
            double x = padLeft + i * candleSpacing + (candleSpacing - candleW) / 2;
            double volH = ((double) o.getVolume() / maxVol) * volMaxHeight;
            double volY = (h - padBottom) - volH;

            Color volColor = o.isBullish() ? Color.web(green.toString(), 0.25) : Color.web(red.toString(), 0.25);
            gc.setFill(volColor);
            gc.fillRect(x, volY, candleW, volH);
        }

        // Draw Candlesticks (Wick + Body)
        List<Double> maPointsX = new ArrayList<>();
        List<Double> maPointsY = new ArrayList<>();
        int maPeriod = 15;

        for (int i = 0; i < count; i++) {
            OHLC o = data.get(i);
            double cx = padLeft + i * candleSpacing + candleSpacing / 2;
            double x = cx - candleW / 2;

            double openY = padTop + chartH * (1.0 - (o.getOpen() - plotMin) / priceRange);
            double closeY = padTop + chartH * (1.0 - (o.getClose() - plotMin) / priceRange);
            double highY = padTop + chartH * (1.0 - (o.getHigh() - plotMin) / priceRange);
            double lowY = padTop + chartH * (1.0 - (o.getLow() - plotMin) / priceRange);

            Color color = o.isBullish() ? green : red;

            // Wick
            gc.setStroke(color);
            gc.setLineWidth(1.2);
            gc.strokeLine(cx, highY, cx, lowY);

            // Body
            double bodyTop = Math.min(openY, closeY);
            double bodyH = Math.max(2.0, Math.abs(closeY - openY));

            gc.setFill(color);
            gc.fillRect(x, bodyTop, candleW, bodyH);

            // Compute SMA points
            if (i >= maPeriod - 1) {
                double sum = 0;
                for (int m = i - maPeriod + 1; m <= i; m++) {
                    sum += data.get(m).getClose();
                }
                double avg = sum / maPeriod;
                double maY = padTop + chartH * (1.0 - (avg - plotMin) / priceRange);
                maPointsX.add(cx);
                maPointsY.add(maY);
            }

            // Timestamp labels on x-axis periodically
            if (i % (Math.max(1, count / 6)) == 0) {
                String label = o.getTimestamp().format(TIME_FMT);
                gc.setFill(textColor);
                gc.fillText(label, cx - 12, h - padBottom + 16);
                gc.setStroke(gridColor);
                gc.strokeLine(cx, padTop, cx, h - padBottom);
            }
        }

        // Draw Moving Average Line (SMA 15)
        if (maPointsX.size() > 1) {
            gc.setStroke(maColor);
            gc.setLineWidth(1.8);
            for (int k = 0; k < maPointsX.size() - 1; k++) {
                gc.strokeLine(maPointsX.get(k), maPointsY.get(k), maPointsX.get(k + 1), maPointsY.get(k + 1));
            }
        }

        // Draw Interactive Crosshair
        if (mouseX >= padLeft && mouseX <= w - padRight && mouseY >= padTop && mouseY <= h - padBottom) {
            gc.setStroke(Color.web("#94a3b8", 0.7));
            gc.setLineWidth(1.0);
            gc.setLineDashes(4, 4);

            // Vertical crosshair
            gc.strokeLine(mouseX, padTop, mouseX, h - padBottom);

            // Horizontal crosshair
            gc.strokeLine(padLeft, mouseY, w - padRight, mouseY);

            // Crosshair Price Tag on right axis
            double hoverPrice = plotMax - (priceRange * (mouseY - padTop) / chartH);
            gc.setFill(isDark ? Color.web("#1e293b") : Color.web("#0284c7"));
            gc.fillRect(w - padRight, mouseY - 9, padRight - 5, 18);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.fillText(String.format("₹%.2f", hoverPrice), w - padRight + 6, mouseY + 4);

            gc.setLineDashes(null); // Reset line dashes
        }
    }
}
