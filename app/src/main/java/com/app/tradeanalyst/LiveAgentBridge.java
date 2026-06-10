package com.tradeanalyst.app;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * NATIVE JAVASCRIPT INTERFACE BRIDGE: LiveAgentBridge
 * Role: Acts as the secure, real-time gateway between headless live_agent.html and Java.
 * Location: App flat package (side-by-side with MainActivity).
 */
public class LiveAgentBridge {

    private static final String TAG = "LiveAgentBridge";
    private final MainActivity mActivity;
    private final TradingPreferences mPrefs;

    /**
     * Constructor for LiveAgentBridge.
     * 
     * @param activity Reference to MainActivity for running callbacks on the UI thread.
     */
    public LiveAgentBridge(MainActivity activity) {
        this.mActivity = activity;
        this.mPrefs = activity.getPrefs();
    }

    /**
     * Exposes the securely saved Gemini API Key from native SharedPreferences to JavaScript.
     */
    @JavascriptInterface
    public String getGeminiApiKey() {
        if (mPrefs != null) {
            String apiKey = mPrefs.getApiKey();
            Log.d(TAG, "getGeminiApiKey: Bridge fetched API Key from SharedPreferences. Length: " + (apiKey != null ? apiKey.length() : 0));
            return apiKey;
        }
        Log.w(TAG, "getGeminiApiKey: Preferences reference is null.");
        return "";
    }

    /**
     * Exposes the active Model ID from SharedPreferences to JavaScript.
     */
    @JavascriptInterface
    public String getModelId() {
        if (mPrefs != null) {
            String model = mPrefs.getModel();
            Log.d(TAG, "getModelId: Selected Model fetched: " + model);
            return model;
        }
        return "gemini-3.1-flash-live-preview"; // Default fallback
    }

    /**
     * Exposes the active Search Grounding switch state from SharedPreferences to JavaScript.
     */
    @JavascriptInterface
    public boolean isSearchGroundingEnabled() {
        if (mPrefs != null) {
            boolean enabled = mPrefs.isGroundingEnabled();
            Log.d(TAG, "isSearchGroundingEnabled: Grounding state fetched: " + enabled);
            return enabled;
        }
        return false;
    }

    /**
     * Exposes the active chart parameter snapshot dynamically to JavaScript.
     * Defaults to 10 lookback candles if count parameter is absent.
     */
    @JavascriptInterface
    public String getChartContext() {
        return getChartContext(10);
    }

    /**
     * Exposes lookback-aware dynamic chart context snapshots to JavaScript.
     */
    @JavascriptInterface
    public String getChartContext(int lookback) {
        if (mActivity != null) {
            return mActivity.getChartContext(lookback);
        }
        return "Chart context currently offline: Activity is in background.";
    }

    /**
     * Exposes the latest mathematically confirmed pattern candidate in serialized JSON format.
     * JavaScript calls this to package the geometry payload for Gemini's validation turn (Phase 7).
     *
     * @return Serialized JSON string of the active mathematical pattern, or empty object.
     */
    @JavascriptInterface
    public String getLatestPatternCandidate() {
        if (mActivity != null) {
            String candidateJson = mActivity.getLatestPatternCandidateJson();
            if (candidateJson != null) {
                return candidateJson;
            }
        }
        return "{}";
    }

    /**
     * Helper to expose the full TradingPreferences class wrapper securely.
     */
    public TradingPreferences getPrefs() {
        return mPrefs;
    }

    /**
     * Callback invoked by JavaScript when the WebSocket Live connection is established.
     */
    @JavascriptInterface
    public void onLiveWebSocketConnected() {
        Log.d(TAG, "onLiveWebSocketConnected: Connection successful handshake completed.");
        if (mActivity != null) {
            mActivity.onLiveWebSocketConnected();
        }
    }

    /**
     * Callback invoked by JavaScript when the WebSocket Live connection is closed or fails.
     */
    @JavascriptInterface
    public void onLiveWebSocketDisconnected(String reason) {
        Log.w(TAG, "onLiveWebSocketDisconnected: Connection closed. Reason: " + reason);
        if (mActivity != null) {
            mActivity.onLiveWebSocketDisconnected(reason);
        }
    }

    /**
     * Callback invoked by JavaScript when text transcription (User or Bot) is received.
     */
    @JavascriptInterface
    public void onLiveTranscriptReceived(String sender, String message) {
        Log.d(TAG, "onLiveTranscriptReceived: [" + sender + "] -> " + message);
        if (mActivity != null) {
            mActivity.onLiveTranscriptReceived(sender, message);
        }
    }

    /**
     * Callback invoked by JavaScript when a custom command tag (SIGNAL or INDICATOR) is parsed.
     */
    @JavascriptInterface
    public void onLiveCommandReceived(String type, String payload) {
        Log.d(TAG, "onLiveCommandReceived: Captured Action Tag -> Type: " + type + " | Payload: " + payload);
        if (mActivity != null) {
            mActivity.onLiveCommandReceived(type, payload);
        }
    }

    /**
     * Interface logger for writing background WebView Javascript events directly to Android Logcat.
     */
    @JavascriptInterface
    public void logDiagnostic(String message, String category) {
        String logLine = String.format("[WEBVIEW_JS] [%s] %s", category.toUpperCase(), message);
        if ("ERROR".equalsIgnoreCase(category)) {
            Log.e(TAG, logLine);
        } else if ("WARN".equalsIgnoreCase(category)) {
            Log.w(TAG, logLine);
        } else {
            Log.i(TAG, logLine);
        }
    }
}