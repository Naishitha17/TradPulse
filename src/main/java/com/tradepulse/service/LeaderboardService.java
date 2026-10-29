package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.Holding;
import com.tradepulse.model.StockQuote;
import com.tradepulse.model.TraderRanking;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    private final DatabaseManager dbManager;
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;

    public LeaderboardService(DatabaseManager dbManager, MarketDataService marketDataService, PortfolioService portfolioService) {
        this.dbManager = dbManager;
        this.marketDataService = marketDataService;
        this.portfolioService = portfolioService;
    }

    public List<TraderRanking> getTopTraders(int limit) {
        List<TraderRanking> allTraders = getAllPublicTraders();
        return allTraders.stream()
                .sorted(Comparator.comparingDouble(TraderRanking::getReturnPercent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<TraderRanking> getAllPublicTraders() {
        List<TraderRanking> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.display_name, u.created_at, " +
                     "p.bio, p.avatar_seed, p.virtual_balance, p.initial_balance, p.is_public " +
                     "FROM users u " +
                     "JOIN profiles p ON p.user_id = u.id " +
                     "WHERE u.is_active = 1 AND p.is_public = 1 AND p.show_trades_on_leaderboard = 1 AND u.role = 'USER'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");
                String displayName = rs.getString("display_name");
                Timestamp ts = rs.getTimestamp("created_at");
                String bio = rs.getString("bio");
                String avatarSeed = rs.getString("avatar_seed");
                BigDecimal cash = BigDecimal.valueOf(rs.getDouble("virtual_balance"));
                BigDecimal initial = BigDecimal.valueOf(rs.getDouble("initial_balance"));

                // Calculate current market value of holdings
                List<Holding> holdings = portfolioService.getHoldings(userId);
                BigDecimal holdingsValue = BigDecimal.ZERO;
                for (Holding h : holdings) {
                    StockQuote quote = marketDataService.getStock(h.getTicker());
                    double currentPrice = quote != null ? quote.getCurrentPrice() : h.getAverageBuyPrice().doubleValue();
                    holdingsValue = holdingsValue.add(BigDecimal.valueOf(currentPrice * h.getShares()));
                }

                BigDecimal totalPortfolioValue = cash.add(holdingsValue).setScale(2, RoundingMode.HALF_UP);

                // Calculate return %
                double returnPct = 0.0;
                if (initial.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal netProfit = totalPortfolioValue.subtract(initial);
                    returnPct = netProfit.divide(initial, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                }

                // Win rate and trade count
                int totalTrades = 0;
                int winningTrades = 0;
                String tradeSql = "SELECT COUNT(*) AS total, SUM(CASE WHEN realized_pnl > 0 THEN 1 ELSE 0 END) AS wins " +
                                  "FROM orders WHERE user_id = ? AND status = 'COMPLETED'";
                try (PreparedStatement tStmt = conn.prepareStatement(tradeSql)) {
                    tStmt.setInt(1, userId);
                    try (ResultSet tRs = tStmt.executeQuery()) {
                        if (tRs.next()) {
                            totalTrades = tRs.getInt("total");
                            winningTrades = tRs.getInt("wins");
                        }
                    }
                }

                double winRate = totalTrades > 0 ? ((double) winningTrades / totalTrades) * 100.0 : 0.0;
                winRate = Math.round(winRate * 10.0) / 10.0;

                list.add(new TraderRanking(
                        userId, username, displayName, avatarSeed, bio,
                        totalPortfolioValue, initial, Math.round(returnPct * 100.0) / 100.0,
                        winRate, totalTrades, ts != null ? ts.toLocalDateTime() : null, true
                ));
            }
        } catch (SQLException e) {
            log.error("Failed to calculate trader leaderboard", e);
        }

        return list;
    }
}
