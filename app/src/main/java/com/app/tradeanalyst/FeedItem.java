package com.tradeanalyst.app;

public class FeedItem {
    public String title;
    public String subtitle;
    public String badge;
    public int indicatorColor; // e.g. Color.GREEN, Color.RED
    public long timestamp;

    public FeedItem(String title, String subtitle, String badge, int indicatorColor, long timestamp) {
        this.title = title;
        this.subtitle = subtitle;
        this.badge = badge;
        this.indicatorColor = indicatorColor;
        this.timestamp = timestamp;
    }
}
