package com.tradeanalyst.app;

import android.content.Context;
import android.util.Log;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CORE BACKTEST COORDINATOR: BacktestManager
 * Manages database logging, trade outcomes, and statistical recalculation loops.
 * Implements Phase 10 backtesting, success rate metrics, and historical feedback loops.
 */
public class BacktestManager {

    private static final String TAG = "BacktestManager";
    private static volatile BacktestManager sInstance;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private BacktestManager() {}

    /**
     * Singleton accessor for thread-safe execution of database write operations.
     */
    public static BacktestManager getInstance() {
        if (sInstance == null) {
            synchronized (BacktestManager.class) {
                if (sInstance == null) {
                    sInstance = new BacktestManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Logs a completed trade outcome into Room and triggers immediate statistical recalculations.
     *
     * @param context Application context to acquire database instance safely.
     * @param symbol Asset trade symbol (e.g. BTC/USDT).
     * @param patternType Mathematically identified pattern class name.
     * @param entryPrice Entrance price coordinate of the breakout.
     * @param stopLoss Safety protection margin price.
     * @param targetPrice Target profit boundary price.
     * @param result Match outcome rating ("WIN" or "LOSS").
     * @param profitPercentage Calculated rate of return percentage.
     */
    public void recordCompletedTrade(
            final Context context, final String symbol, final String patternType,
            final double entryPrice, final double stopLoss, final double targetPrice,
            final String result, final double profitPercentage) {

        if (context == null || patternType == null) {
            return;
        }

        mExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
                long timestamp = System.currentTimeMillis();

                // 1. Insert individual backtest record
                BacktestEntity entity = new BacktestEntity(
                    symbol,
                    patternType.toUpperCase(),
                    entryPrice,
                    stopLoss,
                    targetPrice,
                    result.toUpperCase(),
                    profitPercentage,
                    timestamp
                );
                db.tradeDao().insertBacktest(entity);
                Log.d(TAG, "Recorded historical backtest trade. Pattern: " + patternType + " | Result: " + result);

                // 2. Trigger automatic statistical updates for this pattern class
                recalculateStatsForPattern(db, patternType.toUpperCase());

            } catch (Exception e) {
                Log.e(TAG, "Failed to persist backtest trade record", e);
            }
        });
    }

    /**
     * Pulls the history array for a pattern class and updates the statistics summary table.
     */
    private void recalculateStatsForPattern(AppDatabase db, String patternType) {
        try {
            List<BacktestEntity> trades = db.tradeDao().getAllBacktests();
            int totalDetected = 0;
            int wins = 0;
            int losses = 0;
            double totalProfit = 0.0;

            // Filter records by pattern type to compute aggregates safely
            for (BacktestEntity t : trades) {
                if (patternType.equalsIgnoreCase(t.getPatternType())) {
                    totalDetected++;
                    if ("WIN".equalsIgnoreCase(t.getResult())) {
                        wins++;
                    } else if ("LOSS".equalsIgnoreCase(t.getResult())) {
                        losses++;
                    }
                    totalProfit += t.getProfitPercentage();
                }
            }

            if (totalDetected == 0) {
                return;
            }

            double successRate = ((double) wins / totalDetected) * 100.0;
            double averageProfit = totalProfit / totalDetected;

            // Build or replace Room statistical model
            BacktestStatsEntity stats = new BacktestStatsEntity(
                patternType,
                totalDetected,
                wins,
                losses,
                successRate,
                averageProfit
            );
            db.tradeDao().insertOrUpdateStats(stats);
            Log.d(TAG, "Recalculated stats for: " + patternType + " | Win Rate: " 
                + String.format("%.2f%%", successRate) + " | Matches: " + totalDetected);

        } catch (Exception e) {
            Log.e(TAG, "Failed to recalculate pattern statistics", e);
        }
    }

    /**
     * Interface callback to return success rates to the main scanning threads asynchronously.
     */
    public interface SuccessRateCallback {
        void onSuccessRateRetrieved(double successRate, int totalSampleCount);
    }

    /**
     * Queries the database asynchronously to pull historical success rates for dynamic confidence adjustment.
     *
     * @param context Application context.
     * @param patternType Mapped pattern class.
     * @param callback Asynchronous callback interface.
     */
    public void getHistoricalSuccessRate(final Context context, final String patternType, final SuccessRateCallback callback) {
        if (context == null || patternType == null || callback == null) {
            return;
        }

        mExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
                BacktestStatsEntity stats = db.tradeDao().getStatsForPattern(patternType.toUpperCase());

                if (stats != null) {
                    final double rate = stats.getSuccessRate();
                    final int count = stats.getTotalDetected();
                    // Return metrics safely on calling thread context
                    callback.onSuccessRateRetrieved(rate, count);
                } else {
                    // No data found; return neutral default baselines
                    callback.onSuccessRateRetrieved(50.0, 0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve historical statistics", e);
                callback.onSuccessRateRetrieved(50.0, 0);
            }
        });
    }

    /**
     * Safely closes background transaction execution channels.
     */
    public void shutdown() {
        mExecutor.shutdown();
    }
}