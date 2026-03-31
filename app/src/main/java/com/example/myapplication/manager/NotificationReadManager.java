package com.example.myapplication.manager;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class NotificationReadManager {
    private static final String PREF_NAME = "notification_prefs";
    private static final String KEY_READ_IDS = "read_notification_ids";

    private SharedPreferences prefs;

    public NotificationReadManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isRead(String notificationId) {
        Set<String> readIds = prefs.getStringSet(KEY_READ_IDS, new HashSet<>());
        return readIds.contains(notificationId);
    }

    public void markAsRead(String notificationId) {
        Set<String> readIds = new HashSet<>(prefs.getStringSet(KEY_READ_IDS, new HashSet<>()));
        readIds.add(notificationId);
        prefs.edit().putStringSet(KEY_READ_IDS, readIds).apply();
    }

    private static final String KEY_DELETED_IDS = "deleted_notification_ids";

    public boolean isDeleted(String notificationId) {
        Set<String> deletedIds = prefs.getStringSet(KEY_DELETED_IDS, new HashSet<>());
        return deletedIds.contains(notificationId);
    }

    public void markAsDeleted(String notificationId) {
        Set<String> deletedIds = new HashSet<>(prefs.getStringSet(KEY_DELETED_IDS, new HashSet<>()));
        deletedIds.add(notificationId);
        prefs.edit().putStringSet(KEY_DELETED_IDS, deletedIds).apply();
    }

    public void markAsUnread(String notificationId) {
        Set<String> readIds = new HashSet<>(prefs.getStringSet(KEY_READ_IDS, new HashSet<>()));
        readIds.remove(notificationId);
        prefs.edit().putStringSet(KEY_READ_IDS, readIds).apply();
    }

    public void markAllAsRead(Set<String> allIds) {
        Set<String> readIds = new HashSet<>(prefs.getStringSet(KEY_READ_IDS, new HashSet<>()));
        readIds.addAll(allIds);
        prefs.edit().putStringSet(KEY_READ_IDS, readIds).apply();
    }
}
