package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: DoubleBottomDetector
 * Scans swing points to isolate Double Bottom patterns.
 * Enforces strict bottom symmetry, middle peak height requirements, and minimum index spacing (Phase 2).
 */
public class DoubleBottomDetector extends BasePatternDetector {

    private static final double BOTTOM_PRICE_TOLERANCE = 0.025; // Bottoms must be within 2.5% price symmetry
    private static final double MIN_PEAK_DEPTH = 0.03; // Peak must separate bottoms by at least 3% depth
    private static final int MIN_BOTTOM_SEPARATION = 6; // Minimum separation width in indices
    private static final int MAX_BOTTOM_SEPARATION = 100; // Maximum separation width in indices

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 3) {
            return detected;
        }

        int totalSwings = swings.size();

        // Sliding window scanning across 3 consecutive swing points (LOW - HIGH - LOW)
        for (int i = 0; i <= totalSwings - 3; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);

            if (p1.getType() == SwingPoint.Type.LOW &&
                p2.getType() == SwingPoint.Type.HIGH &&
                p3.getType() == SwingPoint.Type.LOW) {

                ChartPattern db = verifyDoubleBottom(candles, p1, p2, p3);
                if (db != null) {
                    detected.add(db);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 3 candidate swing points against Double Bottom geometric rules.
     */
    private ChartPattern verifyDoubleBottom(List<Candlestick> candles, SwingPoint bottom1, SwingPoint peak, SwingPoint bottom2) {
        int bottomDistance = bottom2.getIndex() - bottom1.getIndex();

        // Rule 1: Validate bottom index separation bounds
        if (bottomDistance < MIN_BOTTOM_SEPARATION || bottomDistance > MAX_BOTTOM_SEPARATION) {
            return null;
        }

        double b1Price = bottom1.getPrice();
        double b2Price = bottom2.getPrice();
        double peakPrice = peak.getPrice();

        // Rule 2: Bottom Price Symmetry (Bottom 1 price must align closely with Bottom 2)
        if (!isWithinTolerance(b1Price, b2Price, BOTTOM_PRICE_TOLERANCE)) {
            return null;
        }

        // Rule 3: Peak Separation Depth (Middle peak must rise sufficiently above bottoms)
        double meanBottomPrice = (b1Price + b2Price) / 2.0;
        double depthRatio = (peakPrice - meanBottomPrice) / meanBottomPrice;
        if (depthRatio < MIN_PEAK_DEPTH) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double depthHeight = peakPrice - meanBottomPrice;

        // Bullish breakout direction: target projects upward from the neckline peak price
        double targetPrice = peakPrice + depthHeight;
        double stopLossPrice = meanBottomPrice * 0.985; // Stop Loss set 1.5% below bottom levels
        String bias = "BULLISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(bottom1.getIndex(), bottom1.getPrice()));
        points.add(new ChartPattern.Point(peak.getIndex(), peak.getPrice()));
        points.add(new ChartPattern.Point(bottom2.getIndex(), bottom2.getPrice()));

        // Neckline represents the horizontal breakout line matching peak price
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(peak.getIndex(), peak.getPrice()));
        necklines.add(new ChartPattern.Point(bottom2.getIndex(), peak.getPrice()));

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateDoubleTripleTopBottom(
            b1Price, b2Price, 0.0, 2, peakPrice,
            candles.get(peak.getIndex()).volume, 100.0, false, -0.02, true
        );

        String typeLabel = "DOUBLE BOTTOM";
        double priceSymmetryPercent = (1.0 - (Math.abs(b1Price - b2Price) / Math.max(b1Price, b2Price))) * 100.0;
        String explanation = String.format("Local math scan verified a symmetric Double Bottom pattern across lookback indices [%d to %d] with %.1f%% price symmetry.", 
                bottom1.getIndex(), bottom2.getIndex(), priceSymmetryPercent);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, bottom1.getIndex(), bottom2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching peak boundary coordinates
        double retestMargin = depthHeight * 0.15;
        pattern.setRetestZoneTop(peakPrice + retestMargin);
        pattern.setRetestZoneBottom(peakPrice - retestMargin);

        return pattern;
    }
}