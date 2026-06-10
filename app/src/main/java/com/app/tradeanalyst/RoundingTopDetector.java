package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: RoundingTopDetector
 * Scans swing points to isolate Rounding Top (Dome) continuation patterns.
 * Enforces strict rim price alignment and parabolic peak sequence matching (Phase 2).
 */
public class RoundingTopDetector extends BasePatternDetector {

    private static final double RIM_PRICE_TOLERANCE = 0.05; // Rim troughs must align within 5% price symmetry
    private static final int MIN_ROUNDING_WIDTH = 20; // Minimum pattern width in indices
    private static final int MAX_ROUNDING_WIDTH = 150; // Maximum pattern width in indices
    private static final int MIN_REQUIRED_PEAKS = 3; // Minimum swing highs to form a curve

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < (MIN_REQUIRED_PEAKS + 2)) {
            return detected;
        }

        int totalSwings = swings.size();

        // 1. Scan for outer rim troughs (LOW swing points)
        for (int i = 0; i < totalSwings - 4; i++) {
            SwingPoint leftRim = swings.get(i);
            if (leftRim.getType() != SwingPoint.Type.LOW) {
                continue;
            }

            for (int j = i + MIN_REQUIRED_PEAKS + 1; j < totalSwings; j++) {
                SwingPoint rightRim = swings.get(j);
                if (rightRim.getType() != SwingPoint.Type.LOW) {
                    continue;
                }

                int totalWidth = rightRim.getIndex() - leftRim.getIndex();
                if (totalWidth >= MIN_ROUNDING_WIDTH && totalWidth <= MAX_ROUNDING_WIDTH) {
                    // Extract and evaluate intermediate swing highs
                    List<SwingPoint> intermediateHighs = extractIntermediateHighs(swings, i + 1, j - 1);
                    if (intermediateHighs.size() >= MIN_REQUIRED_PEAKS) {
                        ChartPattern rt = verifyRoundingTop(candles, leftRim, rightRim, intermediateHighs);
                        if (rt != null) {
                            detected.add(rt);
                            break; // Avoid overlapping duplicates starting from same left rim
                        }
                    }
                }
            }
        }

        return detected;
    }

    /**
     * Extracts all SwingPoint.Type.HIGH items located between Left Rim and Right Rim index bounds.
     */
    private List<SwingPoint> extractIntermediateHighs(List<SwingPoint> swings, int startIndex, int endIndex) {
        List<SwingPoint> highs = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            SwingPoint sp = swings.get(i);
            if (sp.getType() == SwingPoint.Type.HIGH) {
                highs.add(sp);
            }
        }
        return highs;
    }

    /**
     * Mathematically evaluates the rim boundaries and intermediate peaks against Rounding Top rules.
     */
    private ChartPattern verifyRoundingTop(
            List<Candlestick> candles, SwingPoint leftRim, SwingPoint rightRim, List<SwingPoint> highs) {

        double lrPrice = leftRim.getPrice();
        double rrPrice = rightRim.getPrice();

        // Rule 1: Rim Price Symmetry (Left and Right Rim troughs must align closely)
        if (!isWithinTolerance(lrPrice, rrPrice, RIM_PRICE_TOLERANCE)) {
            return null;
        }

        // Rule 2: Identify absolute maximum peak (the dome top)
        int maxIndexInList = 0;
        double maxPrice = highs.get(0).getPrice();
        for (int i = 1; i < highs.size(); i++) {
            double price = highs.get(i).getPrice();
            if (price > maxPrice) {
                maxPrice = price;
                maxIndexInList = i;
            }
        }

        SwingPoint topPeak = highs.get(maxIndexInList);

        // Rule 3: Math validation of ascending sequence (before top)
        // Values must be progressively ascending (allowing minor variance e.g. 1.0%)
        double prevPrice = lrPrice;
        for (int i = 0; i < maxIndexInList; i++) {
            double price = highs.get(i).getPrice();
            if (price < prevPrice * 0.99) { // Price drops prematurely before top
                return null;
            }
            prevPrice = price;
        }

        // Rule 4: Math validation of descending sequence (after top)
        // Values must be progressively descending
        prevPrice = maxPrice;
        for (int i = maxIndexInList + 1; i < highs.size(); i++) {
            double price = highs.get(i).getPrice();
            if (price > prevPrice * 1.01) { // Price rises prematurely after top
                return null;
            }
            prevPrice = price;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double meanRimPrice = (lrPrice + rrPrice) / 2.0;
        double domeHeight = maxPrice - meanRimPrice;

        // Bearish breakout direction: target projects full dome height downwards from the rim support floor
        double targetPrice = meanRimPrice - domeHeight;
        double stopLossPrice = maxPrice * 1.015; // Stop Loss set 1.5% above the maximum peak
        String bias = "BEARISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(leftRim.getIndex(), lrPrice));
        for (SwingPoint high : highs) {
            points.add(new ChartPattern.Point(high.getIndex(), high.getPrice()));
        }
        points.add(new ChartPattern.Point(rightRim.getIndex(), rrPrice));

        // Lower neckline representing the horizontal rim support line
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(leftRim.getIndex(), meanRimPrice));
        necklines.add(new ChartPattern.Point(rightRim.getIndex(), meanRimPrice));

        // Calculate symmetry ratio of the rim troughs
        double convergenceRatio = Math.min(lrPrice, rrPrice) / Math.max(lrPrice, rrPrice);

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            highs.size(), 2, convergenceRatio,
            candles.get(rightRim.getIndex()).volume, 100.0, false, -0.02
        );

        String typeLabel = "ROUNDING TOP";
        double priceSymmetryPercent = (1.0 - (Math.abs(lrPrice - rrPrice) / Math.max(lrPrice, rrPrice))) * 100.0;
        String explanation = String.format("Local math scan verified a parabolic %s dome spanning indices [%d to %d] with %.1f%% rim price symmetry and %d sequential peaks.", 
                typeLabel, leftRim.getIndex(), rightRim.getIndex(), priceSymmetryPercent, highs.size());

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, leftRim.getIndex(), rightRim.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching Right Rim breakout boundary price level
        double retestMargin = domeHeight * 0.15;
        pattern.setRetestZoneTop(meanRimPrice + retestMargin);
        pattern.setRetestZoneBottom(meanRimPrice - retestMargin);

        return pattern;
    }
}