package com.tradeanalyst.app;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * MULTI-TIMEFRAME ENGINE: MultiTimeframeValidator
 * Asynchronously fetches and analyzes alternate timeframes to validate structural setups.
 * Upgrades or penalizes the final scorecard confidence based on alignment (Phase 9).
 */
public class MultiTimeframeValidator {

    private static final String TAG = "MultiTimeframeValidator";
    private static final String BINANCE_BASE_URL = "https://api.binance.com/";
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    /**
     * Interface defining completion callbacks for background timeframe validations.
     */
    public interface ValidationCallback {
        /**
         * Invoked when background trend evaluations are completed.
         *
         * @param adjustedConfidence The updated scorecard confidence rating.
         * @param validationLog Summary logs details of multi-timeframe trends.
         */
        void onValidationCompleted(double adjustedConfidence, String validationLog);
    }

    /**
     * Natively resolves alternate intervals to scan based on the active primary chart.
     */
    private static String[] getAlternativeTimeframes(String primaryInterval) {
        switch (primaryInterval.toLowerCase()) {
            case "1m":
                return new String[]{"5m", "15m"};
            case "5m":
                return new String[]{"1m", "15m"};
            case "15m":
                return new String[]{"5m", "1h"};
            case "1h":
            default:
                return new String[]{"15m", "1d"}; // Pull daily/hourly trends for standard swings
            case "1d":
                return new String[]{"1h", "1w"}; // Fallback weekly tracking
        }
    }

    /**
     * Triggers asynchronous background trend calculations across alternative timeframes.
     *
     * @param symbol The asset symbol (e.g. BTCUSDT).
     * @param primaryInterval The active chart interval.
     * @param baseConfidence The mathematical confidence calculated on the active chart.
     * @param isBullish Pattern direction bias.
     * @param callback The completion callback interface.
     */
    public void validateTimeframeAlignment(
            final String symbol, final String primaryInterval, final double baseConfidence,
            final boolean isBullish, final ValidationCallback callback) {

        if (symbol == null || callback == null) {
            return;
        }

        mExecutor.execute(() -> {
            try {
                String[] alternates = getAlternativeTimeframes(primaryInterval);
                final String lowerTf = alternates[0];
                final String higherTf = alternates[1];

                Log.d(TAG, "Initiating background validations on: " + lowerTf + " and " + higherTf);

                // Fetch lower timeframe candles
                fetchTimeframeCandles(symbol, lowerTf, 50, new CandleFetchCallback() {
                    @Override
                    public void onCandlesFetched(List<Candlestick> lowerCandles) {
                        // Fetch higher timeframe candles
                        fetchTimeframeCandles(symbol, higherTf, 50, new CandleFetchCallback() {
                            @Override
                            public void onCandlesFetched(List<Candlestick> higherCandles) {
                                // Evaluate trend vectors mathematically
                                evaluateAndScore(baseConfidence, isBullish, lowerTf, lowerCandles, higherTf, higherCandles, callback);
                            }
                        });
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Multi-timeframe background check crashed", e);
                callback.onValidationCompleted(baseConfidence, "Validation error: " + e.getMessage());
            }
        });
    }

    private interface CandleFetchCallback {
        void onCandlesFetched(List<Candlestick> candles);
    }

    /**
     * Executes non-blocking REST calls to Binance APIs to retrieve background historical candles.
     */
    private void fetchTimeframeCandles(String symbol, String interval, int limit, final CandleFetchCallback fetchCallback) {
        String normalizedSymbol = symbol.replace("/", "").toUpperCase();
        if (normalizedSymbol.endsWith("USD")) {
            normalizedSymbol = normalizedSymbol.substring(0, normalizedSymbol.length() - 3) + "USDT";
        }

        String binanceInterval = interval.toLowerCase();
        if ("1d".equalsIgnoreCase(interval)) {
            binanceInterval = "1d";
        }

        OkHttpClient client = new OkHttpClient.Builder().build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BINANCE_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        BinanceApiService service = retrofit.create(BinanceApiService.class);
        service.getKlines(normalizedSymbol, binanceInterval, limit).enqueue(new Callback<List<List<Object>>>() {
            @Override
            public void onResponse(Call<List<List<Object>>> call, Response<List<List<Object>>> response) {
                List<Candlestick> candles = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (List<Object> kline : response.body()) {
                        try {
                            long openTime = ((Number) kline.get(0)).longValue();
                            double open = Double.parseDouble(kline.get(1).toString());
                            double high = Double.parseDouble(kline.get(2).toString());
                            double low = Double.parseDouble(kline.get(3).toString());
                            double close = Double.parseDouble(kline.get(4).toString());
                            double volume = Double.parseDouble(kline.get(5).toString());

                            candles.add(new Candlestick(open, high, low, close, volume, openTime));
                        } catch (Exception ignored) {}
                    }
                }
                fetchCallback.onCandlesFetched(candles);
            }

            @Override
            public void onFailure(Call<List<List<Object>>> call, Throwable t) {
                Log.e(TAG, "Failed to retrieve alternate timeframe candles for background validation", t);
                fetchCallback.onCandlesFetched(new ArrayList<>());
            }
        });
    }

    /**
     * Evaluates directional linear regressions across fetched datasets to finalize confidence.
     */
    private void evaluateAndScore(
            double baseConfidence, boolean isBullish, String lowerTf, List<Candlestick> lowerCandles,
            String higherTf, List<Candlestick> higherCandles, ValidationCallback callback) {

        double lowerSlope = IndicatorsEngine.calculateSlope(lowerCandles, 20);
        double higherSlope = IndicatorsEngine.calculateSlope(higherCandles, 20);

        boolean lowerAligns = isBullish ? (lowerSlope > 0) : (lowerSlope < 0);
        boolean higherAligns = isBullish ? (higherSlope > 0) : (higherSlope < 0);

        double scoreAdjustment = 0.0;
        StringBuilder logBuilder = new StringBuilder();

        logBuilder.append("MTF LOG: [").append(lowerTf).append(" Trend Slope: ").append(String.format("%.4f", lowerSlope)).append("] ")
                  .append("[").append(higherTf).append(" Trend Slope: ").append(String.format("%.4f", higherSlope)).append("]. ");

        if (lowerAligns && higherAligns) {
            scoreAdjustment = 10.0; // Both timeframes support: 10% Confidence Upgrade
            logBuilder.append("Result: Full Alignment. Confidence boosted by +10.0%.");
        } else if (!lowerAligns && !higherAligns) {
            scoreAdjustment = -15.0; // Both timeframes conflict: 15% Confidence Penalty
            logBuilder.append("Result: Severe Trend Conflict. Confidence penalized by -15.0%.");
        } else if (higherAligns) {
            scoreAdjustment = 3.0; // Higher timeframe validates: minor adjustment
            logBuilder.append("Result: Macro Trend Alignment. Confidence adjusted by +3.0%.");
        } else {
            scoreAdjustment = -5.0; // Higher timeframe conflicts: minor penalty
            logBuilder.append("Result: Macro Trend Conflict. Confidence adjusted by -5.0%.");
        }

        double finalConfidence = Math.max(0.0, Math.min(100.0, baseConfidence + scoreAdjustment));
        callback.onValidationCompleted(finalConfidence, logBuilder.toString());
    }

    /**
     * Safely shuts down background thread executors.
     */
    public void shutdown() {
        mExecutor.shutdown();
    }
}