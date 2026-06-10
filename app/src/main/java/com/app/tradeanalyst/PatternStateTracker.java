package com.tradeanalyst.app;

import android.util.Log;
import java.util.List;

/**
 * LIFECYCLE MONITOR: PatternStateTracker
 * Tracks, evaluates, and transitions active chart patterns across state thresholds.
 * Enforces Phase 4 (Breakout Confirmations) and Phase 5 (Invalidation Logic) rules.
 */
public class PatternStateTracker {

    private static final String TAG = "PatternStateTracker";
    private static final double INVALIDATION_BUFFER = 0.01; // 1% Protection offset buffer

    /**
     * Evaluates and updates the state of a single chart pattern based on the latest live candle.
     *
     * @param pattern The chart pattern candidate being monitored.
     * @param currentCandle The latest incoming live market candle.
     * @param currentIndex The chronological array index of the current candle.
     * @param breakoutThresholdPercent Required minimum breakout close distance (e.g. 0.005 representing 0.5% clear separation).
     * @return True if a state transition occurred.
     */
    public static boolean updatePatternState(ChartPattern pattern, Candlestick currentCandle, int currentIndex, double breakoutThresholdPercent) {
        if (pattern == null || currentCandle == null) {
            return false;
        }

        String currentState = pattern.getState();
        PatternState stateEnum = PatternState.fromLabel(currentState);

        // Terminated states (COMPLETED, INVALIDATED) cannot transition further
        if (stateEnum == PatternState.COMPLETED || stateEnum == PatternState.INVALIDATED) {
            return false;
        }

        double close = currentCandle.close;
        double high = currentCandle.high;
        double low = currentCandle.low;

        // 1. EVALUATE CRITICAL INVALIDATION LOGIC (Phase 5)
        if (isPatternInvalidated(pattern, currentCandle)) {
            pattern.setState(PatternState.INVALIDATED.getLabel());
            Log.d(TAG, "Pattern Invalidation Triggered. Type: " + pattern.getType() + " | State -> INVALIDATED");
            return true;
        }

        // 2. STATE MACHINE ROUTING (Phases 3, 4, 5)
        switch (stateEnum) {
            case FORMING:
                // Check if a valid Breakout candle close occurs (Phase 4)
                if (evaluateBreakout(pattern, close, breakoutThresholdPercent)) {
                    pattern.setState(PatternState.CONFIRMED.getLabel());
                    pattern.setBreakoutIndex(currentIndex);
                    pattern.setBreakoutPrice(close);
                    pattern.setBreakoutTimestamp(currentCandle.timestamp);
                    Log.i(TAG, "Breakout Confirmed! Type: " + pattern.getType() + " | Price: " + close + " | State -> CONFIRMED");
                    return true;
                }
                break;

            case CONFIRMED:
                // Check if a Retest pullback occurs into the target retest zone
                if (pattern.getRetestZoneTop() > 0 && pattern.getRetestZoneBottom() > 0) {
                    if (low <= pattern.getRetestZoneTop() && high >= pattern.getRetestZoneBottom()) {
                        pattern.setState(PatternState.RETESTING.getLabel());
                        Log.d(TAG, "Retest Pullback Detected. State -> RETESTING");
                        return true;
                    }
                }
                // Fall-through checks to evaluate if the target has been completed
                if (evaluateTargetHit(pattern, currentCandle)) {
                    pattern.setState(PatternState.COMPLETED.getLabel());
                    Log.i(TAG, "Target Price Objective Achieved. State -> COMPLETED");
                    return true;
                }
                break;

            case RETESTING:
                // Check if target is achieved after retesting key levels
                if (evaluateTargetHit(pattern, currentCandle)) {
                    pattern.setState(PatternState.COMPLETED.getLabel());
                    Log.i(TAG, "Target Achieved Post-Retest. State -> COMPLETED");
                    return true;
                }
                break;

            default:
                break;
        }

        return false;
    }

    /**
     * Checks if a breakout close price exceeds key boundary levels with the required clearance.
     */
    private static boolean evaluateBreakout(ChartPattern pattern, double close, double thresholdPercent) {
        String bias = pattern.getBias() != null ? pattern.getBias().toUpperCase() : "NEUTRAL";
        double referenceBoundary = pattern.getTarget() > pattern.getStopLoss() ? pattern.getStopLoss() : pattern.getTarget(); // neck reference estimate

        if ("BULLISH".equals(bias)) {
            // Price close must break above the neckline resistance
            double requiredClosePrice = referenceBoundary * (1.0 + thresholdPercent);
            return close >= requiredClosePrice;
        } else if ("BEARISH".equals(bias)) {
            // Price close must break below the neckline support
            double requiredClosePrice = referenceBoundary * (1.0 - thresholdPercent);
            return close <= requiredClosePrice;
        }
        return false;
    }

    /**
     * Evaluates if the price has hit the target projection boundary.
     */
    private static boolean evaluateTargetHit(ChartPattern pattern, Candlestick candle) {
        String bias = pattern.getBias() != null ? pattern.getBias().toUpperCase() : "NEUTRAL";
        double target = pattern.getTarget();

        if (target <= 0) {
            return false;
        }

        if ("BULLISH".equals(bias)) {
            return candle.high >= target; // Bullish target achieved
        } else if ("BEARISH".equals(bias)) {
            return candle.low <= target; // Bearish target achieved
        }
        return false;
    }

    /**
     * Encapsulates the specific mathematical invalidation criteria of Phase 5.
     */
    private static boolean isPatternInvalidated(ChartPattern pattern, Candlestick candle) {
        String type = pattern.getType() != null ? pattern.getType().toUpperCase() : "";
        String bias = pattern.getBias() != null ? pattern.getBias().toUpperCase() : "NEUTRAL";
        double close = candle.close;

        // 1. Head and Shoulders Invalidation (Price exceeds extreme Head levels)
        if (type.contains("HEAD AND SHOULDERS")) {
            double headLevel = getExtremumPrice(pattern, true); // Head is the highest high
            if ("BULLISH".equals(bias)) { // Inverse Head & Shoulders
                double inverseHeadLevel = getExtremumPrice(pattern, false); // Bottom trough
                if (close < inverseHeadLevel * (1.0 - INVALIDATION_BUFFER)) {
                    return true; // Breached below inverse head
                }
            } else {
                if (close > headLevel * (1.0 + INVALIDATION_BUFFER)) {
                    return true; // Breached above head
                }
            }
        }

        // 2. Double/Triple Top Invalidation (Price breaks significantly above peaks)
        if (type.contains("TOP")) {
            double peakLevel = getExtremumPrice(pattern, true);
            if (close > peakLevel * (1.0 + INVALIDATION_BUFFER)) {
                return true; // Breached above peaks
            }
        }

        // 3. Double/Triple Bottom Invalidation (Price breaks significantly below bottoms)
        if (type.contains("BOTTOM")) {
            double bottomLevel = getExtremumPrice(pattern, false);
            if (close < bottomLevel * (1.0 - INVALIDATION_BUFFER)) {
                return true; // Breached below bottoms
            }
        }

        // 4. Triangle, Wedge, or Channel Invalidation (Opposite-direction breakout occurs)
        if (type.contains("TRIANGLE") || type.contains("WEDGE") || type.contains("CHANNEL") || type.contains("FLAG")) {
            double stopLoss = pattern.getStopLoss();
            if (stopLoss > 0) {
                if ("BULLISH".equals(bias) && close < stopLoss) {
                    return true; // Opposite bearish breakout occurred
                } else if ("BEARISH".equals(bias) && close > stopLoss) {
                    return true; // Opposite bullish breakout occurred
                }
            }
        }

        return false;
    }

    /**
     * Helper to find the absolute extreme high or low price among a pattern's key pivot points.
     */
    private static double getExtremumPrice(ChartPattern pattern, boolean findMax) {
        List<ChartPattern.Point> points = pattern.getPoints();
        if (points == null || points.isEmpty()) {
            return findMax ? Double.MIN_VALUE : Double.MAX_VALUE;
        }

        double extremum = points.get(0).getPrice();
        for (ChartPattern.Point pt : points) {
            if (findMax) {
                if (pt.getPrice() > extremum) {
                    extremum = pt.getPrice();
                }
            } else {
                if (pt.getPrice() < extremum) {
                    extremum = pt.getPrice();
                }
            }
        }
        return extremum;
    }
}