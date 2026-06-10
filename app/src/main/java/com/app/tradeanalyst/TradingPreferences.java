package com.tradeanalyst.app;

import android.content.Context;
import android.content.SharedPreferences;

public class TradingPreferences {
    private static final String PREF_NAME = "trade_analyst_prefs";
    private static final String KEY_API_KEY = "gemini_api_key";
    private static final String KEY_MODEL = "gemini_model_v1";
    private static final String KEY_GROUNDING = "search_grounding_enabled";
    private static final String KEY_THEME = "dark_theme_enabled";
    private static final String KEY_SYMBOL = "active_symbol";

    private final SharedPreferences mPrefs;

    public TradingPreferences(Context context) {
        mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveApiKey(String apiKey) {
        mPrefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getApiKey() {
        return mPrefs.getString(KEY_API_KEY, "");
    }

    public void saveModel(String model) {
        mPrefs.edit().putString(KEY_MODEL, model).apply();
    }

    public String getModel() {
        return mPrefs.getString(KEY_MODEL, "gemini-3.1-flash-live-preview");
    }

    public void saveGroundingEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_GROUNDING, enabled).apply();
    }

    public boolean isGroundingEnabled() {
        return mPrefs.getBoolean(KEY_GROUNDING, false);
    }

    public void saveDarkThemeEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_THEME, enabled).apply();
    }

    public boolean isDarkThemeEnabled() {
        return mPrefs.getBoolean(KEY_THEME, true); // default true (Velvet Emerald)
    }

    public void saveActiveSymbol(String symbol) {
        mPrefs.edit().putString(KEY_SYMBOL, symbol).apply();
    }

    public String getActiveSymbol() {
        return mPrefs.getString(KEY_SYMBOL, "BTC/USDT");
    }
}
