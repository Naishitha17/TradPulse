package com.tradepulse.model;

import java.math.BigDecimal;

public class PortfolioSummary {
    private BigDecimal cashBalance;
    private BigDecimal investedValue;
    private BigDecimal currentMarketValue;
    private BigDecimal totalPortfolioValue;
    private BigDecimal totalUnrealizedPnl;
    private BigDecimal totalRealizedPnl;
    private BigDecimal todayPnl;
    private double totalReturnPercent;
    private int totalHoldingsCount;

    public PortfolioSummary(BigDecimal cashBalance, BigDecimal investedValue, BigDecimal currentMarketValue,
                            BigDecimal totalPortfolioValue, BigDecimal totalUnrealizedPnl, BigDecimal totalRealizedPnl,
                            BigDecimal todayPnl, double totalReturnPercent, int totalHoldingsCount) {
        this.cashBalance = cashBalance;
        this.investedValue = investedValue;
        this.currentMarketValue = currentMarketValue;
        this.totalPortfolioValue = totalPortfolioValue;
        this.totalUnrealizedPnl = totalUnrealizedPnl;
        this.totalRealizedPnl = totalRealizedPnl;
        this.todayPnl = todayPnl;
        this.totalReturnPercent = totalReturnPercent;
        this.totalHoldingsCount = totalHoldingsCount;
    }

    public BigDecimal getCashBalance() { return cashBalance; }
    public BigDecimal getInvestedValue() { return investedValue; }
    public BigDecimal getCurrentMarketValue() { return currentMarketValue; }
    public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
    public BigDecimal getTotalUnrealizedPnl() { return totalUnrealizedPnl; }
    public BigDecimal getTotalRealizedPnl() { return totalRealizedPnl; }
    public BigDecimal getTodayPnl() { return todayPnl; }
    public double getTotalReturnPercent() { return totalReturnPercent; }
    public int getTotalHoldingsCount() { return totalHoldingsCount; }
}
