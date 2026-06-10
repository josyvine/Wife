package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: RoundingBottomDetector
 * Scans swing points to isolate Rounding Bottom (Saucer) continuation patterns.
 * Enforces strict rim price alignment and parabolic trough sequence matching (Phase 2).
 */
public class RoundingBottomDetector extends BasePatternDetector {

    private static final double RIM_PRICE_TOLERANCE = 0.05; // Rim peaks must align within 5% price symmetry
    private static final int MIN_ROUNDING_WIDTH = 20; // Minimum pattern width in indices
    private static final int MAX_ROUNDING_WIDTH = 150; // Maximum pattern width in indices
    private static final int MIN_REQUIRED_TROUGHS = 3; // Minimum swing lows to form a curve

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < (MIN_REQUIRED_TROUGHS + 2)) {
            return detected;
        }

        int totalSwings = swings.size();

        // 1. Scan for outer rim peaks (HIGH swing points)
        for (int i = 0; i < totalSwings - 4; i++) {
            SwingPoint leftRim = swings.get(i);
            if (leftRim.getType() != SwingPoint.Type.HIGH) {
                continue;
            }

            for (int j = i + MIN_REQUIRED_TROUGHS + 1; j < totalSwings; j++) {
                SwingPoint rightRim = swings.get(j);
                if (rightRim.getType() != SwingPoint.Type.HIGH) {
                    continue;
                }

                int totalWidth = rightRim.getIndex() - leftRim.getIndex();
                if (totalWidth >= MIN_ROUNDING_WIDTH && totalWidth <= MAX_ROUNDING_WIDTH) {
                    // Extract and evaluate intermediate swing lows
                    List<SwingPoint> intermediateLows = extractIntermediateLows(swings, i + 1, j - 1);
                    if (intermediateLows.size() >= MIN_REQUIRED_TROUGHS) {
                        ChartPattern rb = verifyRoundingBottom(candles, leftRim, rightRim, intermediateLows);
                        if (rb != null) {
                            detected.add(rb);
                            break; // Avoid overlapping duplicates starting from same left rim
                        }
                    }
                }
            }
        }

        return detected;
    }

    /**
     * Extracts all SwingPoint.Type.LOW items located between Left Rim and Right Rim index bounds.
     */
    private List<SwingPoint> extractIntermediateLows(List<SwingPoint> swings, int startIndex, int endIndex) {
        List<SwingPoint> lows = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            SwingPoint sp = swings.get(i);
            if (sp.getType() == SwingPoint.Type.LOW) {
                lows.add(sp);
            }
        }
        return lows;
    }

    /**
     * Mathematically evaluates the rim boundaries and intermediate troughs against Rounding Bottom rules.
     */
    private ChartPattern verifyRoundingBottom(
            List<Candlestick> candles, SwingPoint leftRim, SwingPoint rightRim, List<SwingPoint> troughs) {

        double lrPrice = leftRim.getPrice();
        double rrPrice = rightRim.getPrice();

        // Rule 1: Rim Price Symmetry (Left and Right Rim peaks must align closely)
        if (!isWithinTolerance(lrPrice, rrPrice, RIM_PRICE_TOLERANCE)) {
            return null;
        }

        // Rule 2: Identify absolute minimum trough (the bowl bottom)
        int minIndexInList = 0;
        double minPrice = troughs.get(0).getPrice();
        for (int i = 1; i < troughs.size(); i++) {
            double price = troughs.get(i).getPrice();
            if (price < minPrice) {
                minPrice = price;
                minIndexInList = i;
            }
        }

        SwingPoint bottomTrough = troughs.get(minIndexInList);

        // Rule 3: Math validation of descending sequence (before bottom)
        // Values must be progressively descending (allowing minor variance e.g. 1.0%)
        double prevPrice = lrPrice;
        for (int i = 0; i < minIndexInList; i++) {
            double price = troughs.get(i).getPrice();
            if (price > prevPrice * 1.01) { // Price rises prematurely before bottom
                return null;
            }
            prevPrice = price;
        }

        // Rule 4: Math validation of ascending sequence (after bottom)
        // Values must be progressively ascending
        prevPrice = minPrice;
        for (int i = minIndexInList + 1; i < troughs.size(); i++) {
            double price = troughs.get(i).getPrice();
            if (price < prevPrice * 0.99) { // Price drops prematurely after bottom
                return null;
            }
            prevPrice = price;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double meanRimPrice = (lrPrice + rrPrice) / 2.0;
        double bowlHeight = meanRimPrice - minPrice;

        // Bullish breakout direction: target projects full bowl height upwards from the rim resistance ceiling
        double targetPrice = meanRimPrice + bowlHeight;
        double stopLossPrice = minPrice * 0.985; // Stop Loss set 1.5% below the minimum trough
        String bias = "BULLISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(leftRim.getIndex(), lrPrice));
        for (SwingPoint trough : troughs) {
            points.add(new ChartPattern.Point(trough.getIndex(), trough.getPrice()));
        }
        points.add(new ChartPattern.Point(rightRim.getIndex(), rrPrice));

        // Upper neckline representing the horizontal rim resistance line
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(leftRim.getIndex(), meanRimPrice));
        necklines.add(new ChartPattern.Point(rightRim.getIndex(), meanRimPrice));

        // Calculate symmetry ratio of the rim peaks
        double convergenceRatio = Math.min(lrPrice, rrPrice) / Math.max(lrPrice, rrPrice);

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            troughs.size(), 2, convergenceRatio,
            candles.get(rightRim.getIndex()).volume, 100.0, false, 0.02
        );

        String typeLabel = "ROUNDING BOTTOM";
        double priceSymmetryPercent = (1.0 - (Math.abs(lrPrice - rrPrice) / Math.max(lrPrice, rrPrice))) * 100.0;
        String explanation = String.format("Local math scan verified a parabolic %s saucer spanning indices [%d to %d] with %.1f%% rim price symmetry and %d sequential troughs.", 
                typeLabel, leftRim.getIndex(), rightRim.getIndex(), priceSymmetryPercent, troughs.size());

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, leftRim.getIndex(), rightRim.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching Right Rim breakout boundary price level
        double retestMargin = bowlHeight * 0.15;
        pattern.setRetestZoneTop(meanRimPrice + retestMargin);
        pattern.setRetestZoneBottom(meanRimPrice - retestMargin);

        return pattern;
    }
}