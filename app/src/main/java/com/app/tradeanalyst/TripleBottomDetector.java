package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: TripleBottomDetector
 * Scans swing points to isolate Triple Bottom patterns.
 * Enforces strict bottom price alignment, peak height requirements, and temporal symmetry (Phase 2).
 */
public class TripleBottomDetector extends BasePatternDetector {

    private static final double BOTTOM_PRICE_TOLERANCE = 0.025; // Bottoms must be within 2.5% price symmetry
    private static final double MIN_PEAK_DEPTH = 0.03; // Peaks must separate bottoms by at least 3% depth
    private static final double WIDTH_SYMMETRY_TOLERANCE = 0.40; // Horizontal width symmetry of 40%
    private static final int MIN_BOTTOM_SEPARATION = 6; // Minimum separation width between adjacent bottoms
    private static final int MAX_BOTTOM_SEPARATION = 100; // Maximum separation width between adjacent bottoms

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 5) {
            return detected;
        }

        int totalSwings = swings.size();

        // Sliding window scanning across 5 consecutive swing points (LOW - HIGH - LOW - HIGH - LOW)
        for (int i = 0; i <= totalSwings - 5; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);
            SwingPoint p4 = swings.get(i + 3);
            SwingPoint p5 = swings.get(i + 4);

            if (p1.getType() == SwingPoint.Type.LOW &&
                p2.getType() == SwingPoint.Type.HIGH &&
                p3.getType() == SwingPoint.Type.LOW &&
                p4.getType() == SwingPoint.Type.HIGH &&
                p5.getType() == SwingPoint.Type.LOW) {

                ChartPattern tb = verifyTripleBottom(candles, p1, p2, p3, p4, p5);
                if (tb != null) {
                    detected.add(tb);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 5 candidate swing points against Triple Bottom geometric rules.
     */
    private ChartPattern verifyTripleBottom(
            List<Candlestick> candles, SwingPoint bottom1, SwingPoint peak1,
            SwingPoint bottom2, SwingPoint peak2, SwingPoint bottom3) {

        int width1 = bottom2.getIndex() - bottom1.getIndex();
        int width2 = bottom3.getIndex() - bottom2.getIndex();

        // Rule 1: Validate adjacent bottom separation bounds
        if (width1 < MIN_BOTTOM_SEPARATION || width1 > MAX_BOTTOM_SEPARATION ||
            width2 < MIN_BOTTOM_SEPARATION || width2 > MAX_BOTTOM_SEPARATION) {
            return null;
        }

        double b1 = bottom1.getPrice();
        double b2 = bottom2.getPrice();
        double b3 = bottom3.getPrice();
        double p1 = peak1.getPrice();
        double p2 = peak2.getPrice();

        // Rule 2: Bottom Price Symmetry (All 3 bottoms must align closely)
        if (!isWithinTolerance(b1, b2, BOTTOM_PRICE_TOLERANCE) ||
            !isWithinTolerance(b1, b3, BOTTOM_PRICE_TOLERANCE) ||
            !isWithinTolerance(b2, b3, BOTTOM_PRICE_TOLERANCE)) {
            return null;
        }

        // Rule 3: Peak Separation Depth (Both peaks must rise sufficiently above bottoms)
        double meanBottomPrice = (b1 + b2 + b3) / 3.0;
        double depthRatio1 = (p1 - meanBottomPrice) / meanBottomPrice;
        double depthRatio2 = (p2 - meanBottomPrice) / meanBottomPrice;
        if (depthRatio1 < MIN_PEAK_DEPTH || depthRatio2 < MIN_PEAK_DEPTH) {
            return null;
        }

        // Rule 4: Temporal Width Symmetry (Left Bottom Separation vs. Right Bottom Separation)
        if (!isSymmetrical(width1, width2, WIDTH_SYMMETRY_TOLERANCE)) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double highestPeakPrice = Math.max(p1, p2);
        double depthHeight = highestPeakPrice - meanBottomPrice;

        // Bullish breakout direction: target projects upward from the highest peak price (neckline)
        double targetPrice = highestPeakPrice + depthHeight;
        double lowestBottomPrice = Math.min(b1, Math.min(b2, b3));
        double stopLossPrice = lowestBottomPrice * 0.985; // Stop Loss set 1.5% below the lowest bottom
        String bias = "BULLISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(bottom1.getIndex(), bottom1.getPrice()));
        points.add(new ChartPattern.Point(peak1.getIndex(), peak1.getPrice()));
        points.add(new ChartPattern.Point(bottom2.getIndex(), bottom2.getPrice()));
        points.add(new ChartPattern.Point(peak2.getIndex(), peak2.getPrice()));
        points.add(new ChartPattern.Point(bottom3.getIndex(), bottom3.getPrice()));

        // Neckline represents the horizontal breakout line matching the highest peak
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(peak1.getIndex(), highestPeakPrice));
        necklines.add(new ChartPattern.Point(bottom3.getIndex(), highestPeakPrice));

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateDoubleTripleTopBottom(
            b1, b2, b3, 3, highestPeakPrice,
            candles.get(peak2.getIndex()).volume, 100.0, false, -0.02, true
        );

        String typeLabel = "TRIPLE BOTTOM";
        double priceDivergence1 = (Math.abs(b1 - b2) / Math.max(b1, b2)) * 100.0;
        double priceDivergence2 = (Math.abs(b2 - b3) / Math.max(b2, b3)) * 100.0;
        String explanation = String.format("Local math scan verified a symmetric Triple Bottom pattern across lookback indices [%d to %d] with low bottom price divergence (%.1f%% and %.1f%%).", 
                bottom1.getIndex(), bottom3.getIndex(), priceDivergence1, priceDivergence2);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, bottom1.getIndex(), bottom3.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching highest peak coordinates
        double retestMargin = depthHeight * 0.15;
        pattern.setRetestZoneTop(highestPeakPrice + retestMargin);
        pattern.setRetestZoneBottom(highestPeakPrice - retestMargin);

        return pattern;
    }
}