package com.tradeanalyst.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PERSISTENT FOREGROUND SERVICE: VoiceAssistantService
 * Role: Keeps the raw audio capture and WebSockets loops alive persistently in the background.
 * Instantiates the headless WebView inside the service context so it survives Activity destruction.
 */
public class VoiceAssistantService extends Service {

    private static final String TAG = "VoiceAssistantService";
    private static final String CHANNEL_ID = "voice_assistant_service_channel";

    private final IBinder mBinder = new LocalBinder();
    private WebView mWebView;
    private TradingPreferences mPrefs;
    private AppDatabase mDb;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private ServiceListener mListener;
    
    // Persistent state tracker [1]
    private boolean mIsSessionActive = false;

    public interface ServiceListener {
        void onLiveWebSocketConnected();
        void onLiveWebSocketDisconnected(String reason);
        void onLiveTranscriptReceived(String sender, String message);
        void onLiveCommandReceived(String type, String payload);
        String getChartContext(int lookback); // Supports dynamic, model-requested candle ranges
        String getLatestPatternCandidate();   // Added for Phase 7 background validation parity
    }

    public class LocalBinder extends Binder {
        public VoiceAssistantService getService() {
            return VoiceAssistantService.this;
        }
    }

    // Public getter to expose session state to bound context [1]
    public boolean isSessionActive() {
        return mIsSessionActive;
    }

    // Immediate foreground transition to avoid background start restrictions [2]
    public void startForegroundSession() {
        startForegroundServiceNotification();
    }

    // Direct termination handler [2]
    public void stopForegroundSession() {
        mIsSessionActive = false;
        stopForeground(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = new TradingPreferences(this);
        mDb = AppDatabase.getDatabase(this);

        // Load the headless javascript WebSocket/WebRTC engine headlessly on application thread
        initHeadlessWebView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "VoiceAssistantService started with START_STICKY.");
        return START_STICKY; // Ensures Android recreates the service if memory becomes low
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setListener(ServiceListener listener) {
        this.mListener = listener;
    }

    public WebView getLiveAgentWebView() {
        return mWebView;
    }

    /**
     * Instantiates the headless WebView engine utilizing Application Context to bypass activity dependencies.
     */
    private void initHeadlessWebView() {
        mWebView = new WebView(getApplicationContext());
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Bypass SDK identification checks to keep WebRTC capture open
        String userAgent = settings.getUserAgentString().replace("; wv", "");
        settings.setUserAgentString(userAgent);

        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        }

        // Bind custom Javascript interfaces and clients natively
        mWebView.addJavascriptInterface(new ServiceBridge(), "AndroidInterface");
        mWebView.setWebViewClient(new PuterWebViewClient(this));
        mWebView.setWebChromeClient(new ServiceWebChromeClient());

        mWebView.loadUrl("https://appassets.androidplatform.net/assets/live_agent.html");
    }

    /**
     * Builds and registers Oreo (API 26+) compatible Persistent Notification banners.
     */
    private void startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Persistent Voice Assistant Call Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Trade Analyst")
            .setContentText("Persistent Voice Session Active (Background Capture on)")
            .setSmallIcon(android.R.drawable.presence_online) // standard Android built-in icon
            .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1005, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(1005, builder.build());
        }
    }

    /**
     * ServiceBridge: Mirrors standard Javascript-to-Java bridges and isolates execution loops.
     */
    private class ServiceBridge {
        @JavascriptInterface
        public String getGeminiApiKey() {
            return mPrefs.getApiKey();
        }

        @JavascriptInterface
        public String getModelId() {
            return mPrefs.getModel();
        }

        @JavascriptInterface
        public boolean isSearchGroundingEnabled() {
            return mPrefs.isGroundingEnabled();
        }

        @JavascriptInterface
        public String getChartContext() {
            // Falls back to standard lookback of 10 candles if no size argument is provided
            return getChartContext(10);
        }

        @JavascriptInterface
        public String getChartContext(int lookback) {
            if (mListener != null) {
                return mListener.getChartContext(lookback);
            }
            return "Chart context currently offline: Activity is in background.";
        }

        /**
         * Exposes the latest mathematically confirmed pattern candidate in serialized JSON format.
         * Enforces background validation parity in Phase 7.
         *
         * @return Serialized JSON string of the active mathematical pattern, or empty object.
         */
        @JavascriptInterface
        public String getLatestPatternCandidate() {
            if (mListener != null) {
                String candidateJson = mListener.getLatestPatternCandidate();
                if (candidateJson != null) {
                    return candidateJson;
                }
            }
            return "{}";
        }

        @JavascriptInterface
        public void onLiveWebSocketConnected() {
            mIsSessionActive = true; // Set active [1]
            startForegroundServiceNotification();
            if (mListener != null) {
                mListener.onLiveWebSocketConnected();
            }
        }

        @JavascriptInterface
        public void onLiveWebSocketDisconnected(String reason) {
            mIsSessionActive = false; // Set inactive [1]
            stopForeground(true);
            if (mListener != null) {
                mListener.onLiveWebSocketDisconnected(reason);
            }
        }

        @JavascriptInterface
        public void onLiveTranscriptReceived(String sender, String message) {
            if (mListener != null) {
                mListener.onLiveTranscriptReceived(sender, message);
            } else {
                // Background fallback: Log transcripts directly to database if Activity is killed
                saveConversationToDb(sender, message);
            }
        }

        @JavascriptInterface
        public void onLiveCommandReceived(String type, String payload) {
            if (mListener != null) {
                mListener.onLiveCommandReceived(type, payload);
            } else {
                // Background fallback: Process automatic trading breakout signals even when closed
                handleBackgroundCommand(type, payload);
            }
        }

        @JavascriptInterface
        public void logDiagnostic(String message, String category) {
            Log.d("ServiceBridgeJS", "[" + category.toUpperCase() + "] " + message);
        }
    }

    /**
     * ServiceWebChromeClient: Handles hardware permissions automatically inside the Background Service.
     */
    private class ServiceWebChromeClient extends WebChromeClient {
        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            if (request != null) {
                request.grant(request.getResources());
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage != null) {
                Log.d("ServiceWebView", "Console [" + consoleMessage.messageLevel() + "]: " + consoleMessage.message());
            }
            return true;
        }
    }

    private void saveConversationToDb(String sender, String message) {
        mExecutor.execute(() -> {
            try {
                ConversationEntity entity = new ConversationEntity(sender, message, System.currentTimeMillis());
                mDb.tradeDao().insertConversation(entity);
            } catch (Exception e) {
                Log.e(TAG, "Background failed to save transcript: " + e.getMessage());
            }
        });
    }

    private void handleBackgroundCommand(String type, String payload) {
        mExecutor.execute(() -> {
            if ("SIGNAL".equalsIgnoreCase(type)) {
                try {
                    String[] parts = payload.split("\\|");
                    if (parts.length >= 3) {
                        String action = parts[0].trim();
                        double confidence = Double.parseDouble(parts[1].trim());
                        double targetPrice = Double.parseDouble(parts[2].trim());
                        String symbol = mPrefs.getActiveSymbol(); // retrieve saved active symbol

                        PaperTradeTransaction trade = new PaperTradeTransaction(
                            symbol,
                            action.toUpperCase(),
                            confidence,
                            targetPrice,
                            System.currentTimeMillis()
                        );
                        mDb.tradeDao().insertPaperTrade(trade);
                        Log.d(TAG, "Background automated paper trade successfully executed: " + action + " for " + symbol);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Background trade execution failure", e);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.destroy();
        }
        mExecutor.shutdown();
        Log.i(TAG, "VoiceAssistantService successfully destroyed and resources cleaned up.");
    }
}