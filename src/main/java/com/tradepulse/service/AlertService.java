package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.AlertCondition;
import com.tradepulse.model.PriceAlert;
import com.tradepulse.model.StockQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private final DatabaseManager dbManager;
    private final NotificationService notificationService;
    private final List<Consumer<PriceAlert>> alertListeners = new ArrayList<>();

    public AlertService(DatabaseManager dbManager, NotificationService notificationService) {
        this.dbManager = dbManager;
        this.notificationService = notificationService;
    }

    public void addAlertListener(Consumer<PriceAlert> listener) {
        this.alertListeners.add(listener);
    }

    public PriceAlert createAlert(int userId, String ticker, AlertCondition condition, BigDecimal targetPrice) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker symbol is required.");
        }
        if (targetPrice == null || targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target price must be greater than zero.");
        }

        ticker = ticker.toUpperCase().trim();
        targetPrice = targetPrice.setScale(2, RoundingMode.HALF_UP);

        String sql = "INSERT INTO price_alerts (user_id, ticker, condition, target_price) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, ticker);
            stmt.setString(3, condition.name());
            stmt.setDouble(4, targetPrice.doubleValue());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    log.info("Created price alert #{} for user {}: {} {} ₹{}", id, userId, ticker, condition, targetPrice);
                    return new PriceAlert(id, userId, ticker, condition, targetPrice, false, null);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to create price alert", e);
            throw new RuntimeException("Database error creating price alert", e);
        }
        return null;
    }

    public List<PriceAlert> getUserAlerts(int userId) {
        List<PriceAlert> list = new ArrayList<>();
        String sql = "SELECT id, user_id, ticker, condition, target_price, is_triggered, created_at " +
                     "FROM price_alerts WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    list.add(new PriceAlert(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("ticker"),
                            AlertCondition.valueOf(rs.getString("condition")),
                            BigDecimal.valueOf(rs.getDouble("target_price")),
                            rs.getInt("is_triggered") == 1,
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to fetch alerts for user {}", userId, e);
        }
        return list;
    }

    public boolean deleteAlert(int alertId, int userId) {
        String sql = "DELETE FROM price_alerts WHERE id = ? AND user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, alertId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Failed to delete alert #{}", alertId, e);
        }
        return false;
    }

    public void evaluateQuote(StockQuote quote) {
        if (quote == null) return;
        double currentPrice = quote.getCurrentPrice();

        String sql = "SELECT id, user_id, ticker, condition, target_price FROM price_alerts WHERE ticker = ? AND is_triggered = 0";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, quote.getTicker());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int userId = rs.getInt("user_id");
                    String ticker = rs.getString("ticker");
                    AlertCondition condition = AlertCondition.valueOf(rs.getString("condition"));
                    double targetPrice = rs.getDouble("target_price");

                    boolean triggered = false;
                    if (condition == AlertCondition.ABOVE && currentPrice >= targetPrice) {
                        triggered = true;
                    } else if (condition == AlertCondition.BELOW && currentPrice <= targetPrice) {
                        triggered = true;
                    }

                    if (triggered) {
                        triggerAlert(id, userId, ticker, condition, targetPrice, currentPrice);
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("Error during alert evaluation: {}", e.getMessage());
        }
    }

    private void triggerAlert(int alertId, int userId, String ticker, AlertCondition condition, double targetPrice, double currentPrice) {
        String updateSql = "UPDATE price_alerts SET is_triggered = 1 WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, alertId);
            stmt.executeUpdate();

            String condStr = condition == AlertCondition.ABOVE ? "surpassed" : "dropped below";
            String title = "Price Alert Triggered: " + ticker;
            String msg = String.format("%s has %s your target of ₹%.2f (Current: ₹%.2f).", ticker, condStr, targetPrice, currentPrice);

            notificationService.createNotification(userId, title, msg, "ALERT");
            log.info("Price alert #{} triggered for user {}: {}", alertId, userId, msg);

            PriceAlert alert = new PriceAlert(alertId, userId, ticker, condition, BigDecimal.valueOf(targetPrice), true, null);
            for (Consumer<PriceAlert> l : alertListeners) {
                try {
                    l.accept(alert);
                } catch (Exception ex) {
                    log.debug("Alert listener exception: {}", ex.getMessage());
                }
            }
        } catch (SQLException e) {
            log.error("Failed to update triggered alert #{}", alertId, e);
        }
    }
}
