package com.tradepulse.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private int id;
    private int userId;
    private String ticker;
    private OrderType type;
    private OrderStatus status;
    private int quantity;
    private BigDecimal price;
    private BigDecimal fee;
    private BigDecimal totalAmount;
    private BigDecimal realizedPnl;
    private LocalDateTime createdAt;

    public Order() {
        this.createdAt = LocalDateTime.now();
        this.fee = BigDecimal.ZERO;
        this.realizedPnl = BigDecimal.ZERO;
    }

    public Order(int id, int userId, String ticker, OrderType type, OrderStatus status, int quantity,
                 BigDecimal price, BigDecimal fee, BigDecimal totalAmount, BigDecimal realizedPnl, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.ticker = ticker;
        this.type = type;
        this.status = status;
        this.quantity = quantity;
        this.price = price;
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        this.totalAmount = totalAmount;
        this.realizedPnl = realizedPnl != null ? realizedPnl : BigDecimal.ZERO;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
