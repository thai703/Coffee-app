package com.example.myapplication.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageHelper {
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_LANG = "AppLang";

    public static void setLocale(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);

        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Use application context for global change
        Context appContext = context.getApplicationContext();
        if (appContext != context) {
            Resources appRes = appContext.getResources();
            Configuration appConfig = appRes.getConfiguration();
            appConfig.setLocale(locale);
            appRes.updateConfiguration(appConfig, appRes.getDisplayMetrics());
        }

        saveLanguage(context, langCode);
    }

    public static void loadLocale(Context context) {
        String lang = getSavedLanguage(context);
        if (!lang.isEmpty()) {
            setLocale(context, lang);
        }
    }

    private static void saveLanguage(Context context, String langCode) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_LANG, langCode);
        editor.apply();
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "vi"); // Default is Vietnamese
    }
}
