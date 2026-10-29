package com.tradepulse.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceAlert {
    private int id;
    private int userId;
    private String ticker;
    private AlertCondition condition;
    private BigDecimal targetPrice;
    private boolean triggered;
    private LocalDateTime createdAt;

    public PriceAlert() {
        this.triggered = false;
        this.createdAt = LocalDateTime.now();
    }

    public PriceAlert(int id, int userId, String ticker, AlertCondition condition, BigDecimal targetPrice, boolean triggered, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.ticker = ticker;
        this.condition = condition;
        this.targetPrice = targetPrice;
        this.triggered = triggered;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public AlertCondition getCondition() { return condition; }
    public void setCondition(AlertCondition condition) { this.condition = condition; }

    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }

    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
