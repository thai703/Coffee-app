package com.example.myapplication.manager;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/**
 * Singleton manager to handle persistent storage of user favorites
 */
public class FavoritesManager {

    private static FavoritesManager instance;
    private static final String PREF_NAME = "FavoritesPref";
    private static final String KEY_PREFIX = "favorites_";

    private SharedPreferences prefs;

    private FavoritesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context);
        }
        return instance;
    }

    /**
     * Add a product to user's favorites
     */
    public void addFavorite(String userId, String productId) {
        if (userId == null || productId == null)
            return;

        Set<String> favorites = getFavorites(userId);
        favorites.add(productId);
        saveFavorites(userId, favorites);
    }

    /**
     * Remove a product from user's favorites
     */
    public void removeFavorite(String userId, String productId) {
        if (userId == null || productId == null)
            return;

        Set<String> favorites = getFavorites(userId);
        favorites.remove(productId);
        saveFavorites(userId, favorites);
    }

    /**
     * Check if a product is in user's favorites
     */
    public boolean isFavorite(String userId, String productId) {
        if (userId == null || productId == null)
            return false;

        Set<String> favorites = getFavorites(userId);
        return favorites.contains(productId);
    }

    /**
     * Get all favorite product IDs for a user
     */
    public Set<String> getAllFavorites(String userId) {
        if (userId == null)
            return new HashSet<>();
        return getFavorites(userId);
    }

    /**
     * Clear all favorites for a user (useful for logout)
     */
    public void clearFavorites(String userId) {
        if (userId == null)
            return;

        String key = KEY_PREFIX + userId;
        prefs.edit().remove(key).apply();
    }

    // Private helper methods

    private Set<String> getFavorites(String userId) {
        String key = KEY_PREFIX + userId;
        return new HashSet<>(prefs.getStringSet(key, new HashSet<>()));
    }

    private void saveFavorites(String userId, Set<String> favorites) {
        String key = KEY_PREFIX + userId;
        prefs.edit().putStringSet(key, favorites).apply();
    }
}
