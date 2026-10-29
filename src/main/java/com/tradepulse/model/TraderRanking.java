package com.tradepulse.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TraderRanking {
    private int userId;
    private String username;
    private String displayName;
    private String avatarSeed;
    private String bio;
    private BigDecimal portfolioValue;
    private BigDecimal startingBalance;
    private double returnPercent;
    private double winRate;
    private int totalTrades;
    private LocalDateTime joinDate;
    private boolean isPublic;

    public TraderRanking() {}

    public TraderRanking(int userId, String username, String displayName, String avatarSeed, String bio,
                         BigDecimal portfolioValue, BigDecimal startingBalance, double returnPercent,
                         double winRate, int totalTrades, LocalDateTime joinDate, boolean isPublic) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.avatarSeed = avatarSeed;
        this.bio = bio;
        this.portfolioValue = portfolioValue;
        this.startingBalance = startingBalance;
        this.returnPercent = returnPercent;
        this.winRate = winRate;
        this.totalTrades = totalTrades;
        this.joinDate = joinDate;
        this.isPublic = isPublic;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarSeed() { return avatarSeed; }
    public void setAvatarSeed(String avatarSeed) { this.avatarSeed = avatarSeed; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public BigDecimal getPortfolioValue() { return portfolioValue; }
    public void setPortfolioValue(BigDecimal portfolioValue) { this.portfolioValue = portfolioValue; }

    public BigDecimal getStartingBalance() { return startingBalance; }
    public void setStartingBalance(BigDecimal startingBalance) { this.startingBalance = startingBalance; }

    public double getReturnPercent() { return returnPercent; }
    public void setReturnPercent(double returnPercent) { this.returnPercent = returnPercent; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }

    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }

    public LocalDateTime getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
}
