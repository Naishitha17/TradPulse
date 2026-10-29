package com.tradepulse.model;

import java.math.BigDecimal;

public class UserProfile {
    private int userId;
    private String bio;
    private String avatarSeed;
    private BigDecimal virtualBalance;
    private BigDecimal initialBalance;
    private String currency;
    private String theme;
    private boolean isPublic;
    private boolean showTradesOnLeaderboard;
    private boolean notifyTrades;
    private boolean notifyPriceAlerts;
    private boolean notifyMessages;

    public UserProfile() {
        this.bio = "Active trader exploring markets on TradePulse Nexus.";
        this.avatarSeed = "avatar_1";
        this.virtualBalance = new BigDecimal("100000.00");
        this.initialBalance = new BigDecimal("100000.00");
        this.currency = "INR";
        this.theme = "DARK";
        this.isPublic = true;
        this.showTradesOnLeaderboard = true;
        this.notifyTrades = true;
        this.notifyPriceAlerts = true;
        this.notifyMessages = true;
    }

    public UserProfile(int userId, String bio, String avatarSeed, BigDecimal virtualBalance, BigDecimal initialBalance,
                       String currency, String theme, boolean isPublic, boolean showTradesOnLeaderboard,
                       boolean notifyTrades, boolean notifyPriceAlerts, boolean notifyMessages) {
        this.userId = userId;
        this.bio = bio;
        this.avatarSeed = avatarSeed;
        this.virtualBalance = virtualBalance;
        this.initialBalance = initialBalance;
        this.currency = currency;
        this.theme = theme;
        this.isPublic = isPublic;
        this.showTradesOnLeaderboard = showTradesOnLeaderboard;
        this.notifyTrades = notifyTrades;
        this.notifyPriceAlerts = notifyPriceAlerts;
        this.notifyMessages = notifyMessages;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarSeed() { return avatarSeed; }
    public void setAvatarSeed(String avatarSeed) { this.avatarSeed = avatarSeed; }

    public BigDecimal getVirtualBalance() { return virtualBalance; }
    public void setVirtualBalance(BigDecimal virtualBalance) { this.virtualBalance = virtualBalance; }

    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public boolean isShowTradesOnLeaderboard() { return showTradesOnLeaderboard; }
    public void setShowTradesOnLeaderboard(boolean showTradesOnLeaderboard) { this.showTradesOnLeaderboard = showTradesOnLeaderboard; }

    public boolean isNotifyTrades() { return notifyTrades; }
    public void setNotifyTrades(boolean notifyTrades) { this.notifyTrades = notifyTrades; }

    public boolean isNotifyPriceAlerts() { return notifyPriceAlerts; }
    public void setNotifyPriceAlerts(boolean notifyPriceAlerts) { this.notifyPriceAlerts = notifyPriceAlerts; }

    public boolean isNotifyMessages() { return notifyMessages; }
    public void setNotifyMessages(boolean notifyMessages) { this.notifyMessages = notifyMessages; }
}
