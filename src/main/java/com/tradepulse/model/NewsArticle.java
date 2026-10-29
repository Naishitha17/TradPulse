package com.tradepulse.model;

import java.time.LocalDateTime;

public class NewsArticle {
    private String id;
    private String title;
    private String description;
    private String url;
    private String source;
    private LocalDateTime publishedAt;
    private String relatedTicker;

    public NewsArticle() {
        this.publishedAt = LocalDateTime.now();
    }

    public NewsArticle(String id, String title, String description, String url, String source, LocalDateTime publishedAt, String relatedTicker) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt != null ? publishedAt : LocalDateTime.now();
        this.relatedTicker = relatedTicker;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public String getRelatedTicker() { return relatedTicker; }
    public void setRelatedTicker(String relatedTicker) { this.relatedTicker = relatedTicker; }
}
