package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.Holding;
import com.tradepulse.model.Order;
import com.tradepulse.model.OrderStatus;
import com.tradepulse.model.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

public class TradingEngine {
    private static final Logger log = LoggerFactory.getLogger(TradingEngine.class);
    private final DatabaseManager dbManager;
    private final ConcurrentHashMap<Integer, Object> userLocks = new ConcurrentHashMap<>();

    public TradingEngine(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private Object getLock(int userId) {
        return userLocks.computeIfAbsent(userId, k -> new Object());
    }

    public Order executeBuy(int userId, String ticker, int quantity, double price) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker symbol is required.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }

        ticker = ticker.toUpperCase().trim();
        BigDecimal bPrice = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossAmount = bPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        
        // Fee: 0.05% with a minimum of 1.00 and max of 20.00
        BigDecimal fee = grossAmount.multiply(new BigDecimal("0.0005")).setScale(2, RoundingMode.HALF_UP);
        if (fee.compareTo(BigDecimal.ONE) < 0) fee = BigDecimal.ONE;
        if (fee.compareTo(new BigDecimal("20.00")) > 0) fee = new BigDecimal("20.00");

        BigDecimal totalCost = grossAmount.add(fee);

        synchronized (getLock(userId)) {
            try (Connection conn = dbManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Check cash balance
                    BigDecimal currentBalance;
                    String balanceSql = "SELECT virtual_balance FROM profiles WHERE user_id = ?";
                    try (PreparedStatement bStmt = conn.prepareStatement(balanceSql)) {
                        bStmt.setInt(1, userId);
                        try (ResultSet rs = bStmt.executeQuery()) {
                            if (rs.next()) {
                                currentBalance = BigDecimal.valueOf(rs.getDouble("virtual_balance"));
                            } else {
                                throw new IllegalStateException("User profile not found.");
                            }
                        }
                    }

                    if (currentBalance.compareTo(totalCost) < 0) {
                        throw new InsufficientFundsException(
                                String.format("Insufficient virtual funds. Required: ₹%s (including ₹%s fee), Available: ₹%s",
                                        totalCost.toPlainString(), fee.toPlainString(), currentBalance.toPlainString()));
                    }

                    BigDecimal newBalance = currentBalance.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);

                    // 1. Deduct cash
                    String deductSql = "UPDATE profiles SET virtual_balance = ? WHERE user_id = ?";
                    try (PreparedStatement dStmt = conn.prepareStatement(deductSql)) {
                        dStmt.setDouble(1, newBalance.doubleValue());
                        dStmt.setInt(2, userId);
                        dStmt.executeUpdate();
                    }

                    // 2. Check existing holding
                    String holdingSelectSql = "SELECT id, shares, average_buy_price FROM holdings WHERE user_id = ? AND ticker = ?";
                    int holdingId = -1;
                    int oldShares = 0;
                    BigDecimal oldAvgPrice = BigDecimal.ZERO;

                    try (PreparedStatement hStmt = conn.prepareStatement(holdingSelectSql)) {
                        hStmt.setInt(1, userId);
                        hStmt.setString(2, ticker);
                        try (ResultSet rs = hStmt.executeQuery()) {
                            if (rs.next()) {
                                holdingId = rs.getInt("id");
                                oldShares = rs.getInt("shares");
                                oldAvgPrice = BigDecimal.valueOf(rs.getDouble("average_buy_price"));
                            }
                        }
                    }

                    if (holdingId > 0) {
                        // Weighted average cost calculation
                        int totalShares = oldShares + quantity;
                        BigDecimal oldCost = oldAvgPrice.multiply(BigDecimal.valueOf(oldShares));
                        BigDecimal newCost = bPrice.multiply(BigDecimal.valueOf(quantity));
                        BigDecimal combinedAvg = oldCost.add(newCost).divide(BigDecimal.valueOf(totalShares), 2, RoundingMode.HALF_UP);

                        String updateHoldingSql = "UPDATE holdings SET shares = ?, average_buy_price = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                        try (PreparedStatement uhStmt = conn.prepareStatement(updateHoldingSql)) {
                            uhStmt.setInt(1, totalShares);
                            uhStmt.setDouble(2, combinedAvg.doubleValue());
                            uhStmt.setInt(3, holdingId);
                            uhStmt.executeUpdate();
                        }
                    } else {
                        String insertHoldingSql = "INSERT INTO holdings (user_id, ticker, shares, average_buy_price) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement ihStmt = conn.prepareStatement(insertHoldingSql)) {
                            ihStmt.setInt(1, userId);
                            ihStmt.setString(2, ticker);
                            ihStmt.setInt(3, quantity);
                            ihStmt.setDouble(4, bPrice.doubleValue());
                            ihStmt.executeUpdate();
                        }
                    }

                    // 3. Record Order
                    int orderId;
                    String orderSql = "INSERT INTO orders (user_id, ticker, type, status, quantity, price, fee, total_amount, realized_pnl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement oStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                        oStmt.setInt(1, userId);
                        oStmt.setString(2, ticker);
                        oStmt.setString(3, OrderType.BUY.name());
                        oStmt.setString(4, OrderStatus.COMPLETED.name());
                        oStmt.setInt(5, quantity);
                        oStmt.setDouble(6, bPrice.doubleValue());
                        oStmt.setDouble(7, fee.doubleValue());
                        oStmt.setDouble(8, totalCost.doubleValue());
                        oStmt.setDouble(9, 0.0);
                        oStmt.executeUpdate();

                        try (ResultSet keys = oStmt.getGeneratedKeys()) {
                            if (keys.next()) {
                                orderId = keys.getInt(1);
                            } else {
                                throw new SQLException("Failed to get order ID.");
                            }
                        }
                    }

                    // 4. Record Transaction
                    String txSql = "INSERT INTO transactions (user_id, order_id, type, amount, balance_after, description) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement tStmt = conn.prepareStatement(txSql)) {
                        tStmt.setInt(1, userId);
                        tStmt.setInt(2, orderId);
                        tStmt.setString(3, "BUY");
                        tStmt.setDouble(4, totalCost.negate().doubleValue());
                        tStmt.setDouble(5, newBalance.doubleValue());
                        tStmt.setString(6, String.format("Bought %d shares of %s at ₹%s (Fee: ₹%s)", quantity, ticker, bPrice.toPlainString(), fee.toPlainString()));
                        tStmt.executeUpdate();
                    }

                    // 5. User Notification
                    String notifSql = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement nStmt = conn.prepareStatement(notifSql)) {
                        nStmt.setInt(1, userId);
                        nStmt.setString(2, "Order Executed: BUY " + ticker);
                        nStmt.setString(3, String.format("Successfully bought %d shares of %s at ₹%s. Net: ₹%s", quantity, ticker, bPrice.toPlainString(), totalCost.toPlainString()));
                        nStmt.setString(4, "ORDER");
                        nStmt.executeUpdate();
                    }

                    conn.commit();
                    log.info("Executed BUY order #{} for user {}: {} x {}", orderId, userId, quantity, ticker);

                    return new Order(orderId, userId, ticker, OrderType.BUY, OrderStatus.COMPLETED, quantity, bPrice, fee, totalCost, BigDecimal.ZERO, null);
                } catch (Exception e) {
                    conn.rollback();
                    log.error("Failed to execute BUY order: {}", e.getMessage(), e);
                    if (e instanceof InsufficientFundsException) throw (InsufficientFundsException) e;
                    throw new RuntimeException("Trade execution failed: " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Database connection failure during buy trade", e);
                throw new RuntimeException("Database error during trade execution", e);
            }
        }
    }

    public Order executeSell(int userId, String ticker, int quantity, double price) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker symbol is required.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }

        ticker = ticker.toUpperCase().trim();
        BigDecimal bPrice = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossProceeds = bPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal fee = grossProceeds.multiply(new BigDecimal("0.0005")).setScale(2, RoundingMode.HALF_UP);
        if (fee.compareTo(BigDecimal.ONE) < 0) fee = BigDecimal.ONE;
        if (fee.compareTo(new BigDecimal("20.00")) > 0) fee = new BigDecimal("20.00");

        BigDecimal netProceeds = grossProceeds.subtract(fee);

        synchronized (getLock(userId)) {
            try (Connection conn = dbManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Check holdings
                    int holdingId;
                    int availableShares;
                    BigDecimal avgBuyPrice;

                    String holdingSelectSql = "SELECT id, shares, average_buy_price FROM holdings WHERE user_id = ? AND ticker = ?";
                    try (PreparedStatement hStmt = conn.prepareStatement(holdingSelectSql)) {
                        hStmt.setInt(1, userId);
                        hStmt.setString(2, ticker);
                        try (ResultSet rs = hStmt.executeQuery()) {
                            if (rs.next()) {
                                holdingId = rs.getInt("id");
                                availableShares = rs.getInt("shares");
                                avgBuyPrice = BigDecimal.valueOf(rs.getDouble("average_buy_price"));
                            } else {
                                throw new InsufficientHoldingsException("You do not currently own any shares of " + ticker + ".");
                            }
                        }
                    }

                    if (availableShares < quantity) {
                        throw new InsufficientHoldingsException(
                                String.format("Insufficient shares of %s. Requested to sell: %d, Currently holding: %d",
                                        ticker, quantity, availableShares));
                    }

                    // Calculate realized P&L: (salePrice - avgBuyPrice) * quantity - fee
                    BigDecimal costBasis = avgBuyPrice.multiply(BigDecimal.valueOf(quantity));
                    BigDecimal realizedPnl = grossProceeds.subtract(costBasis).subtract(fee).setScale(2, RoundingMode.HALF_UP);

                    // Fetch current cash balance
                    BigDecimal currentBalance;
                    String balanceSql = "SELECT virtual_balance FROM profiles WHERE user_id = ?";
                    try (PreparedStatement bStmt = conn.prepareStatement(balanceSql)) {
                        bStmt.setInt(1, userId);
                        try (ResultSet rs = bStmt.executeQuery()) {
                            if (rs.next()) {
                                currentBalance = BigDecimal.valueOf(rs.getDouble("virtual_balance"));
                            } else {
                                throw new IllegalStateException("User profile not found.");
                            }
                        }
                    }

                    BigDecimal newBalance = currentBalance.add(netProceeds).setScale(2, RoundingMode.HALF_UP);

                    // 1. Add cash proceeds
                    String creditSql = "UPDATE profiles SET virtual_balance = ? WHERE user_id = ?";
                    try (PreparedStatement cStmt = conn.prepareStatement(creditSql)) {
                        cStmt.setDouble(1, newBalance.doubleValue());
                        cStmt.setInt(2, userId);
                        cStmt.executeUpdate();
                    }

                    // 2. Update or delete holding
                    if (availableShares == quantity) {
                        String deleteHoldingSql = "DELETE FROM holdings WHERE id = ?";
                        try (PreparedStatement dhStmt = conn.prepareStatement(deleteHoldingSql)) {
                            dhStmt.setInt(1, holdingId);
                            dhStmt.executeUpdate();
                        }
                    } else {
                        String updateHoldingSql = "UPDATE holdings SET shares = shares - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                        try (PreparedStatement uhStmt = conn.prepareStatement(updateHoldingSql)) {
                            uhStmt.setInt(1, quantity);
                            uhStmt.setInt(2, holdingId);
                            uhStmt.executeUpdate();
                        }
                    }

                    // 3. Record Order
                    int orderId;
                    String orderSql = "INSERT INTO orders (user_id, ticker, type, status, quantity, price, fee, total_amount, realized_pnl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement oStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                        oStmt.setInt(1, userId);
                        oStmt.setString(2, ticker);
                        oStmt.setString(3, OrderType.SELL.name());
                        oStmt.setString(4, OrderStatus.COMPLETED.name());
                        oStmt.setInt(5, quantity);
                        oStmt.setDouble(6, bPrice.doubleValue());
                        oStmt.setDouble(7, fee.doubleValue());
                        oStmt.setDouble(8, netProceeds.doubleValue());
                        oStmt.setDouble(9, realizedPnl.doubleValue());
                        oStmt.executeUpdate();

                        try (ResultSet keys = oStmt.getGeneratedKeys()) {
                            if (keys.next()) {
                                orderId = keys.getInt(1);
                            } else {
                                throw new SQLException("Failed to get order ID.");
                            }
                        }
                    }

                    // 4. Record Transaction
                    String txSql = "INSERT INTO transactions (user_id, order_id, type, amount, balance_after, description) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement tStmt = conn.prepareStatement(txSql)) {
                        tStmt.setInt(1, userId);
                        tStmt.setInt(2, orderId);
                        tStmt.setString(3, "SELL");
                        tStmt.setDouble(4, netProceeds.doubleValue());
                        tStmt.setDouble(5, newBalance.doubleValue());
                        tStmt.setString(6, String.format("Sold %d shares of %s at ₹%s (Net: ₹%s, Realized P&L: ₹%s)",
                                quantity, ticker, bPrice.toPlainString(), netProceeds.toPlainString(), realizedPnl.toPlainString()));
                        tStmt.executeUpdate();
                    }

                    // 5. User Notification
                    String notifSql = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement nStmt = conn.prepareStatement(notifSql)) {
                        nStmt.setInt(1, userId);
                        nStmt.setString(2, "Order Executed: SELL " + ticker);
                        nStmt.setString(3, String.format("Sold %d shares of %s at ₹%s. Realized P&L: ₹%s", quantity, ticker, bPrice.toPlainString(), realizedPnl.toPlainString()));
                        nStmt.setString(4, "ORDER");
                        nStmt.executeUpdate();
                    }

                    conn.commit();
                    log.info("Executed SELL order #{} for user {}: {} x {}", orderId, userId, quantity, ticker);

                    return new Order(orderId, userId, ticker, OrderType.SELL, OrderStatus.COMPLETED, quantity, bPrice, fee, netProceeds, realizedPnl, null);
                } catch (Exception e) {
                    conn.rollback();
                    log.error("Failed to execute SELL order: {}", e.getMessage(), e);
                    if (e instanceof InsufficientHoldingsException) throw (InsufficientHoldingsException) e;
                    throw new RuntimeException("Trade execution failed: " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Database connection failure during sell trade", e);
                throw new RuntimeException("Database error during trade execution", e);
            }
        }
    }
}
