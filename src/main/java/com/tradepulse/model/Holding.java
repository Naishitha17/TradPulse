package com.tradepulse.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Holding {
    private int id;
    private int userId;
    private String ticker;
    private int shares;
    private BigDecimal averageBuyPrice;
    private LocalDateTime updatedAt;

    public Holding() {}

    public Holding(int id, int userId, String ticker, int shares, BigDecimal averageBuyPrice, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.ticker = ticker;
        this.shares = shares;
        this.averageBuyPrice = averageBuyPrice;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public int getShares() { return shares; }
    public void setShares(int shares) { this.shares = shares; }

    public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
    public void setAverageBuyPrice(BigDecimal averageBuyPrice) { this.averageBuyPrice = averageBuyPrice; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getTotalInvestedCost() {
        return averageBuyPrice.multiply(BigDecimal.valueOf(shares));
    }
}
