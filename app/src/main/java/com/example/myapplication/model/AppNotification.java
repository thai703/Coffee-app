package com.example.myapplication.model;

public class AppNotification {
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private int iconResId; // Resource ID for icon (e.g., promo, order)
    private boolean isRead;
    private String type; // "ORDER", "PROMO", "SYSTEM"

    public AppNotification() {
    }

    public AppNotification(String id, String title, String message, long timestamp, int iconResId, String type) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.iconResId = iconResId;
        this.type = type;
        this.isRead = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getType() {
        return type;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
