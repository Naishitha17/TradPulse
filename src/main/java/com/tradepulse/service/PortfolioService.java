package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.Holding;
import com.tradepulse.model.HoldingViewItem;
import com.tradepulse.model.Order;
import com.tradepulse.model.OrderStatus;
import com.tradepulse.model.OrderType;
import com.tradepulse.model.PortfolioSummary;
import com.tradepulse.model.StockQuote;
import com.tradepulse.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PortfolioService {
    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);
    private final DatabaseManager dbManager;

    public PortfolioService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Holding> getHoldings(int userId) {
        List<Holding> list = new ArrayList<>();
        String sql = "SELECT id, user_id, ticker, shares, average_buy_price, updated_at FROM holdings WHERE user_id = ? ORDER BY ticker ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("updated_at");
                    list.add(new Holding(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("ticker"),
                            rs.getInt("shares"),
                            BigDecimal.valueOf(rs.getDouble("average_buy_price")).setScale(2, RoundingMode.HALF_UP),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query holdings for user {}", userId, e);
        }
        return list;
    }

    public List<HoldingViewItem> getHoldingViewItems(int userId, MarketDataService marketDataService) {
        List<Holding> rawHoldings = getHoldings(userId);
        List<HoldingViewItem> result = new ArrayList<>();

        BigDecimal totalPortfolioMarketVal = BigDecimal.ZERO;
        for (Holding h : rawHoldings) {
            StockQuote quote = marketDataService.getStock(h.getTicker());
            double price = quote != null ? quote.getCurrentPrice() : h.getAverageBuyPrice().doubleValue();
            totalPortfolioMarketVal = totalPortfolioMarketVal.add(BigDecimal.valueOf(price * h.getShares()));
        }

        for (Holding h : rawHoldings) {
            StockQuote quote = marketDataService.getStock(h.getTicker());
            String name = quote != null ? quote.getName() : h.getTicker();
            String sector = quote != null ? quote.getSector() : "Equities";
            double currentP = quote != null ? quote.getCurrentPrice() : h.getAverageBuyPrice().doubleValue();
            BigDecimal bCurrentPrice = BigDecimal.valueOf(currentP).setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalCost = h.getAverageBuyPrice().multiply(BigDecimal.valueOf(h.getShares())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal marketVal = bCurrentPrice.multiply(BigDecimal.valueOf(h.getShares())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unrealizedPnl = marketVal.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);

            double pnlPct = totalCost.compareTo(BigDecimal.ZERO) > 0
                    ? unrealizedPnl.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            double allocPct = totalPortfolioMarketVal.compareTo(BigDecimal.ZERO) > 0
                    ? marketVal.divide(totalPortfolioMarketVal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            result.add(new HoldingViewItem(
                    h.getTicker(), name, sector, h.getShares(), h.getAverageBuyPrice(),
                    bCurrentPrice, totalCost, marketVal, unrealizedPnl, pnlPct, allocPct
            ));
        }

        return result;
    }

    public PortfolioSummary getPortfolioSummary(int userId, MarketDataService marketDataService) {
        BigDecimal cashBalance = BigDecimal.ZERO;
        BigDecimal initialBalance = BigDecimal.valueOf(100000.00);

        String profileSql = "SELECT virtual_balance, initial_balance FROM profiles WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(profileSql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    cashBalance = BigDecimal.valueOf(rs.getDouble("virtual_balance")).setScale(2, RoundingMode.HALF_UP);
                    initialBalance = BigDecimal.valueOf(rs.getDouble("initial_balance")).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query profile for user {}", userId, e);
        }

        List<Holding> holdings = getHoldings(userId);
        BigDecimal investedCost = BigDecimal.ZERO;
        BigDecimal currentMarketValue = BigDecimal.ZERO;
        BigDecimal todayPnl = BigDecimal.ZERO;

        for (Holding h : holdings) {
            investedCost = investedCost.add(h.getTotalInvestedCost());
            StockQuote quote = marketDataService.getStock(h.getTicker());
            if (quote != null) {
                BigDecimal curPrice = BigDecimal.valueOf(quote.getCurrentPrice());
                currentMarketValue = currentMarketValue.add(curPrice.multiply(BigDecimal.valueOf(h.getShares())));
                todayPnl = todayPnl.add(BigDecimal.valueOf(quote.getChange()).multiply(BigDecimal.valueOf(h.getShares())));
            } else {
                currentMarketValue = currentMarketValue.add(h.getTotalInvestedCost());
            }
        }

        investedCost = investedCost.setScale(2, RoundingMode.HALF_UP);
        currentMarketValue = currentMarketValue.setScale(2, RoundingMode.HALF_UP);
        todayPnl = todayPnl.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPortfolioValue = cashBalance.add(currentMarketValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalUnrealizedPnl = currentMarketValue.subtract(investedCost).setScale(2, RoundingMode.HALF_UP);

        // Sum realized PnL from orders
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;
        String pnlSql = "SELECT SUM(realized_pnl) AS total_realized FROM orders WHERE user_id = ? AND status = 'COMPLETED'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(pnlSql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalRealizedPnl = BigDecimal.valueOf(rs.getDouble("total_realized")).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to sum realized pnl for user {}", userId, e);
        }

        double totalReturnPercent = 0.0;
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal netProfit = totalPortfolioValue.subtract(initialBalance);
            totalReturnPercent = netProfit.divide(initialBalance, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        return new PortfolioSummary(
                cashBalance, investedCost, currentMarketValue, totalPortfolioValue,
                totalUnrealizedPnl, totalRealizedPnl, todayPnl, totalReturnPercent, holdings.size()
        );
    }

    public List<Order> getOrders(int userId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT id, user_id, ticker, type, status, quantity, price, fee, total_amount, realized_pnl, created_at " +
                     "FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT 100";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    list.add(new Order(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("ticker"),
                            OrderType.valueOf(rs.getString("type")),
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getInt("quantity"),
                            BigDecimal.valueOf(rs.getDouble("price")),
                            BigDecimal.valueOf(rs.getDouble("fee")),
                            BigDecimal.valueOf(rs.getDouble("total_amount")),
                            BigDecimal.valueOf(rs.getDouble("realized_pnl")),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query orders for user {}", userId, e);
        }
        return list;
    }

    public List<Transaction> getTransactions(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT id, user_id, order_id, type, amount, balance_after, description, created_at " +
                     "FROM transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT 100";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    int orderId = rs.getInt("order_id");
                    list.add(new Transaction(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.wasNull() ? null : orderId,
                            rs.getString("type"),
                            BigDecimal.valueOf(rs.getDouble("amount")),
                            BigDecimal.valueOf(rs.getDouble("balance_after")),
                            rs.getString("description"),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query transactions for user {}", userId, e);
        }
        return list;
    }

    public Set<String> getWatchlist(int userId) {
        Set<String> watchlist = new HashSet<>();
        String sql = "SELECT ticker FROM watchlists WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    watchlist.add(rs.getString("ticker"));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query watchlist for user {}", userId, e);
        }
        return watchlist;
    }

    public boolean isInWatchlist(int userId, String ticker) {
        String sql = "SELECT 1 FROM watchlists WHERE user_id = ? AND ticker = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, ticker.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Failed to check watchlist", e);
        }
        return false;
    }

    public void addToWatchlist(int userId, String ticker) {
        String sql = "INSERT OR IGNORE INTO watchlists (user_id, ticker) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, ticker.toUpperCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to add to watchlist", e);
        }
    }

    public void removeFromWatchlist(int userId, String ticker) {
        String sql = "DELETE FROM watchlists WHERE user_id = ? AND ticker = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, ticker.toUpperCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to remove from watchlist", e);
        }
    }

    public void resetAccount(int userId, BigDecimal startingBalance) {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete holdings
                try (PreparedStatement hStmt = conn.prepareStatement("DELETE FROM holdings WHERE user_id = ?")) {
                    hStmt.setInt(1, userId);
                    hStmt.executeUpdate();
                }

                // Reset balance
                try (PreparedStatement pStmt = conn.prepareStatement("UPDATE profiles SET virtual_balance = ?, initial_balance = ? WHERE user_id = ?")) {
                    pStmt.setDouble(1, startingBalance.doubleValue());
                    pStmt.setDouble(2, startingBalance.doubleValue());
                    pStmt.setInt(3, userId);
                    pStmt.executeUpdate();
                }

                // Record transaction
                try (PreparedStatement tStmt = conn.prepareStatement("INSERT INTO transactions (user_id, type, amount, balance_after, description) VALUES (?, ?, ?, ?, ?)")) {
                    tStmt.setInt(1, userId);
                    tStmt.setString(2, "RESET");
                    tStmt.setDouble(3, startingBalance.doubleValue());
                    tStmt.setDouble(4, startingBalance.doubleValue());
                    tStmt.setString(5, "Account balance reset to ₹" + startingBalance.toPlainString());
                    tStmt.executeUpdate();
                }

                conn.commit();
                log.info("Reset account for user {} to {}", userId, startingBalance);
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to reset account", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during account reset", e);
        }
    }
}
