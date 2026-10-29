package com.tradepulse;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.Holding;
import com.tradepulse.model.Order;
import com.tradepulse.model.OrderStatus;
import com.tradepulse.model.OrderType;
import com.tradepulse.model.User;
import com.tradepulse.model.UserProfile;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.InsufficientFundsException;
import com.tradepulse.service.InsufficientHoldingsException;
import com.tradepulse.service.PortfolioService;
import com.tradepulse.service.TradingEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TradingEngineTest {
    private static DatabaseManager dbManager;
    private static AuthService authService;
    private static TradingEngine tradingEngine;
    private static PortfolioService portfolioService;

    @BeforeAll
    public static void setUp() {
        dbManager = TestDbHelper.createTestDatabase();
        authService = new AuthService(dbManager);
        tradingEngine = new TradingEngine(dbManager);
        portfolioService = new PortfolioService(dbManager);
    }

    @AfterAll
    public static void tearDown() {
        if (dbManager != null) {
            dbManager.shutdown();
        }
    }

    @Test
    public void testBuyStockSuccessAndAverageCostCalculation() {
        String username = "buyer_" + System.currentTimeMillis();
        User user = authService.register(username, username + "@trade.com", "Pass@123", "Buyer");

        // Buy 10 shares of AAPL at 200.00
        Order order1 = tradingEngine.executeBuy(user.getId(), "AAPL", 10, 200.00);
        assertNotNull(order1);
        assertEquals(OrderType.BUY, order1.getType());
        assertEquals(OrderStatus.COMPLETED, order1.getStatus());
        assertEquals(10, order1.getQuantity());

        // Check holding
        List<Holding> holdings1 = portfolioService.getHoldings(user.getId());
        assertEquals(1, holdings1.size());
        assertEquals("AAPL", holdings1.get(0).getTicker());
        assertEquals(10, holdings1.get(0).getShares());
        assertEquals(new BigDecimal("200.00"), holdings1.get(0).getAverageBuyPrice());

        // Buy 10 more shares of AAPL at 220.00 -> New average should be (10*200 + 10*220)/20 = 210.00
        Order order2 = tradingEngine.executeBuy(user.getId(), "AAPL", 10, 220.00);
        assertNotNull(order2);

        List<Holding> holdings2 = portfolioService.getHoldings(user.getId());
        assertEquals(1, holdings2.size());
        assertEquals(20, holdings2.get(0).getShares());
        assertEquals(new BigDecimal("210.00"), holdings2.get(0).getAverageBuyPrice());

        // Verify cash deduction
        UserProfile profile = authService.getUserProfile(user.getId());
        assertTrue(profile.getVirtualBalance().compareTo(new BigDecimal("100000.00")) < 0);
    }

    @Test
    public void testBuyStockInsufficientFundsThrowsException() {
        String username = "broke_buyer_" + System.currentTimeMillis();
        User user = authService.register(username, username + "@trade.com", "Pass@123", "Broke Buyer");

        // Attempt to buy 1,000 shares of a stock at 1,000.00 (Cost = 1,000,000 > 100,000 balance)
        assertThrows(InsufficientFundsException.class, () -> {
            tradingEngine.executeBuy(user.getId(), "MSFT", 1000, 1000.00);
        });

        // Verify balance was untouched
        UserProfile profile = authService.getUserProfile(user.getId());
        assertEquals(new BigDecimal("100000.00"), profile.getVirtualBalance());

        // Verify no holding was created
        List<Holding> holdings = portfolioService.getHoldings(user.getId());
        assertTrue(holdings.isEmpty());
    }

    @Test
    public void testSellStockSuccessAndRealizedPnlCalculation() {
        String username = "seller_" + System.currentTimeMillis();
        User user = authService.register(username, username + "@trade.com", "Pass@123", "Seller");

        // Buy 20 shares of NVDA at 100.00 (Total cost ~2001.00 with fee)
        tradingEngine.executeBuy(user.getId(), "NVDA", 20, 100.00);

        // Sell 10 shares of NVDA at 150.00
        // Expected gross proceeds = 1500.00, fee = 1.00, net proceeds = 1499.00
        // Cost basis = 10 * 100 = 1000.00
        // Realized P&L = 1500 - 1000 - 1.00 = 499.00
        Order sellOrder = tradingEngine.executeSell(user.getId(), "NVDA", 10, 150.00);
        assertNotNull(sellOrder);
        assertEquals(OrderType.SELL, sellOrder.getType());
        assertEquals(OrderStatus.COMPLETED, sellOrder.getStatus());
        assertEquals(10, sellOrder.getQuantity());
        assertEquals(new BigDecimal("499.00"), sellOrder.getRealizedPnl());

        // Verify remaining holdings: 10 shares remaining at 100.00 avg price
        List<Holding> holdings = portfolioService.getHoldings(user.getId());
        assertEquals(1, holdings.size());
        assertEquals(10, holdings.get(0).getShares());
        assertEquals(new BigDecimal("100.00"), holdings.get(0).getAverageBuyPrice());

        // Sell remaining 10 shares
        tradingEngine.executeSell(user.getId(), "NVDA", 10, 160.00);

        // Holding should be deleted cleanly
        List<Holding> emptyHoldings = portfolioService.getHoldings(user.getId());
        assertTrue(emptyHoldings.isEmpty());
    }

    @Test
    public void testSellStockInsufficientHoldingsThrowsException() {
        String username = "short_seller_" + System.currentTimeMillis();
        User user = authService.register(username, username + "@trade.com", "Pass@123", "Short Seller");

        // Attempt to sell stock not owned
        assertThrows(InsufficientHoldingsException.class, () -> {
            tradingEngine.executeSell(user.getId(), "GOOGL", 5, 150.00);
        });

        // Buy 5 shares
        tradingEngine.executeBuy(user.getId(), "GOOGL", 5, 150.00);

        // Attempt to sell 10 shares (> 5)
        assertThrows(InsufficientHoldingsException.class, () -> {
            tradingEngine.executeSell(user.getId(), "GOOGL", 10, 150.00);
        });
    }
}
