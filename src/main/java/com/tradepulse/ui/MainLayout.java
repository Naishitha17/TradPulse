package com.tradepulse.ui;

import com.tradepulse.model.Role;
import com.tradepulse.model.TraderRanking;
import com.tradepulse.model.User;
import com.tradepulse.model.UserProfile;
import com.tradepulse.service.ServiceRegistry;
import com.tradepulse.ui.components.HeaderBar;
import com.tradepulse.ui.components.MarketInsightsPanel;
import com.tradepulse.ui.components.NotificationModal;
import com.tradepulse.ui.components.Sidebar;
import com.tradepulse.ui.components.ToastNotification;
import com.tradepulse.ui.views.AdminView;
import com.tradepulse.ui.views.AlertsView;
import com.tradepulse.ui.views.AuthView;
import com.tradepulse.ui.views.CompanyDetailView;
import com.tradepulse.ui.views.DashboardView;
import com.tradepulse.ui.views.FeedbackView;
import com.tradepulse.ui.views.MarketWatchView;
import com.tradepulse.ui.views.NewsView;
import com.tradepulse.ui.views.OrdersView;
import com.tradepulse.ui.views.PortfolioView;
import com.tradepulse.ui.views.SettingsView;
import com.tradepulse.ui.views.SocialView;
import com.tradepulse.util.ThemeManager;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class MainLayout extends StackPane {
    private final ServiceRegistry services;
    private final BorderPane appShell = new BorderPane();
    private final StackPane workspacePane = new StackPane();

    private Sidebar sidebar;
    private HeaderBar headerBar;
    private MarketInsightsPanel insightsPanel;
    private AuthView authView;

    // Cached Views
    private DashboardView dashboardView;
    private MarketWatchView marketWatchView;
    private PortfolioView portfolioView;
    private OrdersView ordersView;
    private SocialView socialView;
    private NewsView newsView;
    private AlertsView alertsView;
    private FeedbackView feedbackView;
    private AdminView adminView;
    private SettingsView settingsView;
    private final Map<String, CompanyDetailView> companyViews = new HashMap<>();

    public MainLayout(ServiceRegistry services) {
        this.services = services;
        buildUI();
    }

    private void buildUI() {
        authView = new AuthView(services.getAuthService(), this::onLoginSuccess);

        // Check if user is currently logged in
        if (services.getAuthService().getCurrentUser() != null) {
            setupAuthenticatedLayout();
        } else {
            getChildren().setAll(authView);
        }
    }

    private void onLoginSuccess(User user) {
        setupAuthenticatedLayout();
        ToastNotification.show(this, "Welcome Back", "Signed in as " + user.getDisplayName(), ToastNotification.Type.SUCCESS);
    }

    private void setupAuthenticatedLayout() {
        getChildren().clear();

        sidebar = new Sidebar(services.getAuthService(), this::onSidebarNav, this::logout);
        headerBar = new HeaderBar(
                services.getMarketDataService(),
                services.getNotificationService(),
                this::navigateToCompany,
                this::openNotificationModal,
                () -> navigateTo(Sidebar.NavItem.SETTINGS)
        );

        insightsPanel = new MarketInsightsPanel(
                services.getMarketDataService(),
                services.getNewsService(),
                services.getLeaderboardService(),
                this::navigateToCompany,
                this::navigateToTraderChat
        );

        // Instantiate Views
        dashboardView = new DashboardView(services.getAuthService(), services.getMarketDataService(),
                services.getPortfolioService(), this::navigateToCompany, () -> navigateTo(Sidebar.NavItem.MARKET));

        marketWatchView = new MarketWatchView(services.getMarketDataService(), services.getPortfolioService(),
                services.getAuthService(), this::navigateToCompany);

        portfolioView = new PortfolioView(services.getAuthService(), services.getMarketDataService(),
                services.getPortfolioService(), this::navigateToCompany);

        ordersView = new OrdersView(services.getAuthService(), services.getPortfolioService());

        socialView = new SocialView(services.getAuthService(), services.getLeaderboardService(), services.getChatService());

        newsView = new NewsView(services.getNewsService(), this::navigateToCompany);

        alertsView = new AlertsView(services.getAuthService(), services.getAlertService(), services.getMarketDataService());

        feedbackView = new FeedbackView(services.getAuthService(), services.getAdminService());

        adminView = new AdminView(services.getAuthService(), services.getAdminService());

        settingsView = new SettingsView(services.getAuthService(), services.getPortfolioService());

        // Center Area: Header + Workspace
        VBox centerBox = new VBox();
        VBox.setVgrow(workspacePane, Priority.ALWAYS);
        centerBox.getChildren().addAll(headerBar, workspacePane);

        appShell.setLeft(sidebar);
        appShell.setCenter(centerBox);
        appShell.setRight(insightsPanel);

        getChildren().setAll(appShell);

        // Navigate to default Dashboard
        navigateTo(Sidebar.NavItem.DASHBOARD);
        refreshHeaderMetrics();

        // Listen for price alerts to show live toast
        services.getAlertService().addAlertListener(alert -> {
            Platform.runLater(() -> {
                ToastNotification.show(this, "Price Alert Triggered: " + alert.getTicker(),
                        String.format("%s hit target threshold of ₹%,.2f!", alert.getTicker(), alert.getTargetPrice()),
                        ToastNotification.Type.WARNING);
                refreshHeaderMetrics();
            });
        });
    }

    public void onSidebarNav(Sidebar.NavItem item) {
        navigateTo(item);
    }

    public void navigateTo(Sidebar.NavItem item) {
        if (sidebar != null) sidebar.updateActiveButton(item);

        switch (item) {
            case DASHBOARD -> {
                headerBar.setPageTitle("Trading Dashboard", "Personal market overview, metrics, and active watchlist");
                dashboardView.refresh();
                setWorkspaceContent(dashboardView);
            }
            case MARKET -> {
                headerBar.setPageTitle("Market Watch", "60+ Real-world equities across Tech, Finance, Healthcare, and Energy");
                marketWatchView.refresh();
                setWorkspaceContent(marketWatchView);
            }
            case PORTFOLIO -> {
                headerBar.setPageTitle("Portfolio & Holdings", "Asset valuation, realized/unrealized profit & loss, and sector allocation");
                portfolioView.refresh();
                setWorkspaceContent(portfolioView);
            }
            case ORDERS -> {
                headerBar.setPageTitle("Orders & Ledger", "Audit trail of executed virtual trades and cash balance transactions");
                ordersView.refresh();
                setWorkspaceContent(ordersView);
            }
            case SOCIAL -> {
                headerBar.setPageTitle("Social Trading Community", "Public trader leaderboard and private 1-to-1 encrypted messaging");
                socialView.refresh();
                setWorkspaceContent(socialView);
            }
            case NEWS -> {
                headerBar.setPageTitle("Live Financial News", "Global macroeconomic coverage and company earnings intelligence");
                newsView.refresh();
                setWorkspaceContent(newsView);
            }
            case ALERTS -> {
                headerBar.setPageTitle("Target Price Alerts", "Automated price threshold monitoring and real-time trigger center");
                alertsView.refresh();
                setWorkspaceContent(alertsView);
            }
            case FEEDBACK -> {
                headerBar.setPageTitle("User Feedback", "Submit ratings, bug reports, and suggestions for platform enhancement");
                setWorkspaceContent(feedbackView);
            }
            case SETTINGS -> {
                headerBar.setPageTitle("Terminal Settings", "Profile details, dark/light theme switcher, currency, and security");
                settingsView.refresh();
                setWorkspaceContent(settingsView);
            }
            case ADMIN -> {
                headerBar.setPageTitle("Admin Moderation Console", "System administration, feedback review, and user management");
                adminView.refresh();
                setWorkspaceContent(adminView);
            }
        }
        refreshHeaderMetrics();
    }

    public void navigateToCompany(String ticker) {
        if (ticker == null || ticker.isBlank()) return;
        ticker = ticker.toUpperCase().trim();

        headerBar.setPageTitle(ticker + " • Detail Terminal", "Interactive candlestick chart, order execution, and company overview");
        CompanyDetailView view = companyViews.computeIfAbsent(ticker, t ->
                new CompanyDetailView(t, services.getMarketDataService(), services.getTradingEngine(),
                        services.getPortfolioService(), services.getAuthService(),
                        services.getAlertService(), services.getNewsService(),
                        () -> navigateTo(Sidebar.NavItem.MARKET)));

        view.refresh();
        setWorkspaceContent(view);
        refreshHeaderMetrics();
    }

    public void navigateToTraderChat(TraderRanking trader) {
        navigateTo(Sidebar.NavItem.SOCIAL);
        if (socialView != null) {
            socialView.openChatWithTrader(trader);
        }
    }

    private void setWorkspaceContent(Node content) {
        workspacePane.getChildren().setAll(content);
    }

    private void refreshHeaderMetrics() {
        User u = services.getAuthService().getCurrentUser();
        if (u == null) return;

        UserProfile prof = services.getAuthService().getCurrentProfile();
        if (prof != null) {
            headerBar.updateCashBalance(String.format("₹%,.2f", prof.getVirtualBalance()));
        }

        int unread = services.getNotificationService().getUnreadCount(u.getId());
        headerBar.updateUnreadCount(unread);
    }

    private void openNotificationModal() {
        NotificationModal modal = new NotificationModal(
                services.getNotificationService(),
                services.getAuthService(),
                () -> {
                    getChildren().removeIf(node -> node instanceof NotificationModal);
                    refreshHeaderMetrics();
                }
        );
        getChildren().add(modal);
    }

    private void logout() {
        services.getAuthService().logout();
        getChildren().setAll(authView);
        ToastNotification.show(this, "Signed Out", "You have been logged out of TradePulse Nexus.", ToastNotification.Type.INFO);
    }
}
