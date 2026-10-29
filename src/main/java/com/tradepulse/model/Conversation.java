package com.tradepulse.model;

import java.time.LocalDateTime;

public class Conversation {
    private int id;
    private int user1Id;
    private int user2Id;
    private String otherUsername;
    private String otherDisplayName;
    private String otherAvatarSeed;
    private String lastMessage;
    private int unreadCount;
    private LocalDateTime updatedAt;

    public Conversation() {
        this.updatedAt = LocalDateTime.now();
    }

    public Conversation(int id, int user1Id, int user2Id, LocalDateTime updatedAt) {
        this.id = id;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUser1Id() { return user1Id; }
    public void setUser1Id(int user1Id) { this.user1Id = user1Id; }

    public int getUser2Id() { return user2Id; }
    public void setUser2Id(int user2Id) { this.user2Id = user2Id; }

    public String getOtherUsername() { return otherUsername; }
    public void setOtherUsername(String otherUsername) { this.otherUsername = otherUsername; }

    public String getOtherDisplayName() { return otherDisplayName; }
    public void setOtherDisplayName(String otherDisplayName) { this.otherDisplayName = otherDisplayName; }

    public String getOtherAvatarSeed() { return otherAvatarSeed; }
    public void setOtherAvatarSeed(String otherAvatarSeed) { this.otherAvatarSeed = otherAvatarSeed; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
