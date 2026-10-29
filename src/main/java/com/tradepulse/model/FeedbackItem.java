package com.tradepulse.model;

import java.time.LocalDateTime;

public class FeedbackItem {
    private int id;
    private int userId;
    private String username;
    private int rating;
    private FeedbackCategory category;
    private String comment;
    private FeedbackStatus status;
    private LocalDateTime createdAt;

    public FeedbackItem() {
        this.status = FeedbackStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public FeedbackItem(int id, int userId, String username, int rating, FeedbackCategory category,
                        String comment, FeedbackStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.rating = rating;
        this.category = category;
        this.comment = comment;
        this.status = status != null ? status : FeedbackStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public FeedbackCategory getCategory() { return category; }
    public void setCategory(FeedbackCategory category) { this.category = category; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
