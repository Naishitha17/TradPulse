package com.tradepulse.model;

import java.math.BigDecimal;

public class HoldingViewItem {
    private String ticker;
    private String name;
    private String sector;
    private int shares;
    private BigDecimal averageBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal totalCost;
    private BigDecimal currentMarketValue;
    private BigDecimal unrealizedPnl;
    private double unrealizedPnlPercent;
    private double allocationPercent;

    public HoldingViewItem(String ticker, String name, String sector, int shares, BigDecimal averageBuyPrice,
                           BigDecimal currentPrice, BigDecimal totalCost, BigDecimal currentMarketValue,
                           BigDecimal unrealizedPnl, double unrealizedPnlPercent, double allocationPercent) {
        this.ticker = ticker;
        this.name = name;
        this.sector = sector;
        this.shares = shares;
        this.averageBuyPrice = averageBuyPrice;
        this.currentPrice = currentPrice;
        this.totalCost = totalCost;
        this.currentMarketValue = currentMarketValue;
        this.unrealizedPnl = unrealizedPnl;
        this.unrealizedPnlPercent = unrealizedPnlPercent;
        this.allocationPercent = allocationPercent;
    }

    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public String getSector() { return sector; }
    public int getShares() { return shares; }
    public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getCurrentMarketValue() { return currentMarketValue; }
    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public double getUnrealizedPnlPercent() { return unrealizedPnlPercent; }
    public double getAllocationPercent() { return allocationPercent; }
}
