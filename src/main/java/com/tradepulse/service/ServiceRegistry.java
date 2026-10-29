package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;

public class ServiceRegistry {
    private static ServiceRegistry instance;

    private final DatabaseManager dbManager;
    private final AuthService authService;
    private final MarketDataService marketDataService;
    private final TradingEngine tradingEngine;
    private final PortfolioService portfolioService;
    private final NotificationService notificationService;
    private final AlertService alertService;
    private final NewsService newsService;
    private final ChatService chatService;
    private final AdminService adminService;
    private final LeaderboardService leaderboardService;

    private ServiceRegistry() {
        this.dbManager = DatabaseManager.getInstance();
        this.authService = new AuthService(dbManager);
        this.marketDataService = new MarketDataService(dbManager);
        this.tradingEngine = new TradingEngine(dbManager);
        this.portfolioService = new PortfolioService(dbManager);
        this.notificationService = new NotificationService(dbManager);
        this.alertService = new AlertService(dbManager, notificationService);
        this.newsService = new NewsService();
        this.chatService = new ChatService(dbManager);
        this.adminService = new AdminService(dbManager);
        this.leaderboardService = new LeaderboardService(dbManager, marketDataService, portfolioService);

        // Connect market ticks to real-time price alerts evaluation
        this.marketDataService.addQuoteListener(alertService::evaluateQuote);
    }

    public static synchronized ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry();
        }
        return instance;
    }

    public static synchronized void reset() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    public DatabaseManager getDbManager() { return dbManager; }
    public AuthService getAuthService() { return authService; }
    public MarketDataService getMarketDataService() { return marketDataService; }
    public TradingEngine getTradingEngine() { return tradingEngine; }
    public PortfolioService getPortfolioService() { return portfolioService; }
    public NotificationService getNotificationService() { return notificationService; }
    public AlertService getAlertService() { return alertService; }
    public NewsService getNewsService() { return newsService; }
    public ChatService getChatService() { return chatService; }
    public AdminService getAdminService() { return adminService; }
    public LeaderboardService getLeaderboardService() { return leaderboardService; }

    public void shutdown() {
        if (marketDataService != null) marketDataService.shutdown();
        if (newsService != null) newsService.shutdown();
        if (dbManager != null) dbManager.shutdown();
    }
}
