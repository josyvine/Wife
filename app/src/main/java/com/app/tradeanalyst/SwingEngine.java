package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CORE ANALYTICAL ENGINE: SwingEngine
 * Parses raw candlestick listings to isolate, validate, and rank primary swing highs and lows.
 * Eliminates minor market noise to provide clear inputs to the mathematical detectors.
 */
public class SwingEngine {

    /**
     * Mathematically parses the provided candlestick list to detect validated swing peaks and troughs.
     *
     * @param candles The historical candlestick listing (OHLCV).
     * @param leftStrength The required lookback window length to verify a pivot.
     * @param rightStrength The required lookforward window length to verify a pivot.
     * @param proximityThreshold The minimum distance in candle indices allowed between similar pivot types to resolve duplicates.
     * @return Ranked and chronologically ordered list of verified swing points.
     */
    public static List<SwingPoint> detectSwingPoints(List<Candlestick> candles, int leftStrength, int rightStrength, int proximityThreshold) {
        List<SwingPoint> candidates = new ArrayList<>();
        if (candles == null || candles.size() < (leftStrength + rightStrength + 1)) {
            return candidates;
        }

        int totalCandles = candles.size();

        // 1. Core Mathematical Swing Scan
        for (int i = leftStrength; i < totalCandles - rightStrength; i++) {
            Candlestick current = candles.get(i);
            boolean isHigh = true;
            boolean isLow = true;

            // Verify left boundary conditions
            for (int j = i - leftStrength; j < i; j++) {
                if (candles.get(j).high > current.high) {
                    isHigh = false;
                }
                if (candles.get(j).low < current.low) {
                    isLow = false;
                }
            }

            // Verify right boundary conditions
            for (int j = i + 1; j <= i + rightStrength; j++) {
                if (candles.get(j).high > current.high) {
                    isHigh = false;
                }
                if (candles.get(j).low < current.low) {
                    isLow = false;
                }
            }

            // Map and store if the target criteria are met
            if (isHigh) {
                int strength = calculateSwingStrength(candles, i, true);
                candidates.add(new SwingPoint(i, current.timestamp, current.high, strength, SwingPoint.Type.HIGH));
            } else if (isLow) {
                int strength = calculateSwingStrength(candles, i, false);
                candidates.add(new SwingPoint(i, current.timestamp, current.low, strength, SwingPoint.Type.LOW));
            }
        }

        // 2. Resolve Nearby Duplicates and Filter Minor Market Noise
        return resolveDuplicates(candidates, proximityThreshold);
    }

    /**
     * Recursively measures the mathematical strength of a swing point.
     * Evaluates the maximum clean symmetric lookback range where the pivot holds extreme boundaries.
     */
    private static int calculateSwingStrength(List<Candlestick> candles, int index, boolean isHigh) {
        int strength = 0;
        int left = index - 1;
        int right = index + 1;
        int maxRange = candles.size();

        while (left >= 0 && right < maxRange) {
            if (isHigh) {
                if (candles.get(left).high <= candles.get(index).high && candles.get(right).high <= candles.get(index).high) {
                    strength++;
                } else {
                    break;
                }
            } else {
                if (candles.get(left).low >= candles.get(index).low && candles.get(right).low >= candles.get(index).low) {
                    strength++;
                } else {
                    break;
                }
            }
            left--;
            right++;
        }
        return strength;
    }

    /**
     * Eliminates duplicate nearby swing points within the proximity threshold window.
     * Ensures only the most significant (highest strength and extreme price coordinate) is preserved.
     */
    private static List<SwingPoint> resolveDuplicates(List<SwingPoint> points, int proximityThreshold) {
        if (points == null || points.size() < 2) {
            return points;
        }

        List<SwingPoint> filtered = new ArrayList<>(points);
        boolean duplicateFound = true;

        while (duplicateFound) {
            duplicateFound = false;
            int size = filtered.size();

            for (int i = 0; i < size - 1; i++) {
                SwingPoint p1 = filtered.get(i);
                SwingPoint p2 = filtered.get(i + 1);

                // Analyze proximity of matching structural pivot types
                if (p1.getType() == p2.getType() && Math.abs(p1.getIndex() - p2.getIndex()) <= proximityThreshold) {
                    duplicateFound = true;

                    // Rank by structural strength first, falling back to price extremeness
                    boolean keepP1 = false;
                    if (p1.getStrength() > p2.getStrength()) {
                        keepP1 = true;
                    } else if (p1.getStrength() == p2.getStrength()) {
                        if (p1.getType() == SwingPoint.Type.HIGH) {
                            keepP1 = p1.getPrice() >= p2.getPrice();
                        } else {
                            keepP1 = p1.getPrice() <= p2.getPrice();
                        }
                    }

                    if (keepP1) {
                        filtered.remove(i + 1);
                    } else {
                        filtered.remove(i);
                    }
                    break; // Restart evaluation loop to preserve index boundaries safely
                }
            }
        }

        // Return chronological sort order as expected by pattern logic
        Collections.sort(filtered, new Comparator<SwingPoint>() {
            @Override
            public int compare(SwingPoint s1, SwingPoint s2) {
                return Integer.compare(s1.getIndex(), s2.getIndex());
            }
        });

        return filtered;
    }

    /**
     * Helper utility to rank a collection of verified swings strictly by strength.
     * Supports prioritization filters.
     *
     * @param points The processed list of swing points.
     * @return Swings ordered by descending mathematical significance strength.
     */
    public static List<SwingPoint> rankSwingsBySignificance(List<SwingPoint> points) {
        List<SwingPoint> ranked = new ArrayList<>(points);
        Collections.sort(ranked, new Comparator<SwingPoint>() {
            @Override
            public int compare(SwingPoint s1, SwingPoint s2) {
                int comp = Integer.compare(s2.getStrength(), s1.getStrength());
                if (comp != 0) {
                    return comp;
                }
                // Fallback secondary sort prioritizing extreme absolute price bounds
                return Double.compare(Math.abs(s2.getPrice()), Math.abs(s1.getPrice()));
            }
        });
        return ranked;
    }
}