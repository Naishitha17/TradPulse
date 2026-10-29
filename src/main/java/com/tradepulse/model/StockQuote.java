package com.tradepulse.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StockQuote {
    private String ticker;
    private String name;
    private String sector;
    private String description;
    private String marketCap;
    private double peRatio;
    private double high52w;
    private double low52w;
    private double currentPrice;
    private double previousClose;
    private double change;
    private double changePercent;
    private long volume;
    private double dayHigh;
    private double dayLow;
    private double openPrice;
    private List<OHLC> history;

    public StockQuote() {
        this.history = new ArrayList<>();
    }

    public StockQuote(String ticker, String name, String sector, String description, String marketCap,
                      double peRatio, double high52w, double low52w, double basePrice) {
        this.ticker = ticker;
        this.name = name;
        this.sector = sector;
        this.description = description;
        this.marketCap = marketCap;
        this.peRatio = peRatio;
        this.high52w = high52w;
        this.low52w = low52w;
        this.currentPrice = basePrice;
        this.previousClose = basePrice;
        this.openPrice = basePrice;
        this.dayHigh = basePrice;
        this.dayLow = basePrice;
        this.change = 0.0;
        this.changePercent = 0.0;
        this.volume = 120_000;
        this.history = new ArrayList<>();
    }

    public synchronized void updatePrice(double newPrice, long addedVolume) {
        this.currentPrice = BigDecimal.valueOf(newPrice).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.change = BigDecimal.valueOf(currentPrice - previousClose).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.changePercent = previousClose > 0 
                ? BigDecimal.valueOf((change / previousClose) * 100).setScale(2, RoundingMode.HALF_UP).doubleValue() 
                : 0.0;
        this.volume += addedVolume;
        if (currentPrice > dayHigh) dayHigh = currentPrice;
        if (currentPrice < dayLow || dayLow == 0) dayLow = currentPrice;
        if (currentPrice > high52w) high52w = currentPrice;
        if (currentPrice < low52w) low52w = currentPrice;
    }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMarketCap() { return marketCap; }
    public void setMarketCap(String marketCap) { this.marketCap = marketCap; }

    public double getPeRatio() { return peRatio; }
    public void setPeRatio(double peRatio) { this.peRatio = peRatio; }

    public double getHigh52w() { return high52w; }
    public void setHigh52w(double high52w) { this.high52w = high52w; }

    public double getLow52w() { return low52w; }
    public void setLow52w(double low52w) { this.low52w = low52w; }

    public synchronized double getCurrentPrice() { return currentPrice; }
    public synchronized void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getPreviousClose() { return previousClose; }
    public void setPreviousClose(double previousClose) { this.previousClose = previousClose; }

    public synchronized double getChange() { return change; }
    public synchronized double getChangePercent() { return changePercent; }

    public synchronized long getVolume() { return volume; }
    public synchronized void setVolume(long volume) { this.volume = volume; }

    public synchronized double getDayHigh() { return dayHigh; }
    public synchronized double getDayLow() { return dayLow; }

    public double getOpenPrice() { return openPrice; }
    public void setOpenPrice(double openPrice) { this.openPrice = openPrice; }

    public synchronized List<OHLC> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized void setHistory(List<OHLC> history) {
        this.history = new ArrayList<>(history);
    }

    public synchronized void addOHLC(OHLC ohlc) {
        this.history.add(ohlc);
        if (this.history.size() > 500) {
            this.history.remove(0);
        }
    }
}
