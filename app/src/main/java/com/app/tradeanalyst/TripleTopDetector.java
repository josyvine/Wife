package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: TripleTopDetector
 * Scans swing points to isolate Triple Top patterns.
 * Enforces strict peak price alignment, valley depth requirements, and temporal symmetry (Phase 2).
 */
public class TripleTopDetector extends BasePatternDetector {

    private static final double PEAK_PRICE_TOLERANCE = 0.025; // Peaks must be within 2.5% price symmetry
    private static final double MIN_VALLEY_DEPTH = 0.03; // Valleys must separate peaks by at least 3% depth
    private static final double WIDTH_SYMMETRY_TOLERANCE = 0.40; // Horizontal width symmetry of 40%
    private static final int MIN_PEAK_SEPARATION = 6; // Minimum separation width between adjacent peaks
    private static final int MAX_PEAK_SEPARATION = 100; // Maximum separation width between adjacent peaks

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 5) {
            return detected;
        }

        int totalSwings = swings.size();

        // Sliding window scanning across 5 consecutive swing points (HIGH - LOW - HIGH - LOW - HIGH)
        for (int i = 0; i <= totalSwings - 5; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);
            SwingPoint p4 = swings.get(i + 3);
            SwingPoint p5 = swings.get(i + 4);

            if (p1.getType() == SwingPoint.Type.HIGH &&
                p2.getType() == SwingPoint.Type.LOW &&
                p3.getType() == SwingPoint.Type.HIGH &&
                p4.getType() == SwingPoint.Type.LOW &&
                p5.getType() == SwingPoint.Type.HIGH) {

                ChartPattern tt = verifyTripleTop(candles, p1, p2, p3, p4, p5);
                if (tt != null) {
                    detected.add(tt);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 5 candidate swing points against Triple Top geometric rules.
     */
    private ChartPattern verifyTripleTop(
            List<Candlestick> candles, SwingPoint peak1, SwingPoint valley1,
            SwingPoint peak2, SwingPoint valley2, SwingPoint peak3) {

        int width1 = peak2.getIndex() - peak1.getIndex();
        int width2 = peak3.getIndex() - peak2.getIndex();

        // Rule 1: Validate adjacent peak separation bounds
        if (width1 < MIN_PEAK_SEPARATION || width1 > MAX_PEAK_SEPARATION ||
            width2 < MIN_PEAK_SEPARATION || width2 > MAX_PEAK_SEPARATION) {
            return null;
        }

        double p1 = peak1.getPrice();
        double p2 = peak2.getPrice();
        double p3 = peak3.getPrice();
        double v1 = valley1.getPrice();
        double v2 = valley2.getPrice();

        // Rule 2: Peak Price Symmetry (All 3 peaks must align closely)
        if (!isWithinTolerance(p1, p2, PEAK_PRICE_TOLERANCE) ||
            !isWithinTolerance(p1, p3, PEAK_PRICE_TOLERANCE) ||
            !isWithinTolerance(p2, p3, PEAK_PRICE_TOLERANCE)) {
            return null;
        }

        // Rule 3: Valley Separation Depth (Both valleys must drop sufficiently below peaks)
        double meanPeakPrice = (p1 + p2 + p3) / 3.0;
        double depthRatio1 = (meanPeakPrice - v1) / meanPeakPrice;
        double depthRatio2 = (meanPeakPrice - v2) / meanPeakPrice;
        if (depthRatio1 < MIN_VALLEY_DEPTH || depthRatio2 < MIN_VALLEY_DEPTH) {
            return null;
        }

        // Rule 4: Temporal Width Symmetry (Left Peak Separation vs. Right Peak Separation)
        if (!isSymmetrical(width1, width2, WIDTH_SYMMETRY_TOLERANCE)) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double lowestValleyPrice = Math.min(v1, v2);
        double depthHeight = meanPeakPrice - lowestValleyPrice;

        // Bearish breakout direction: target projects downward from the lowest valley price (neckline)
        double targetPrice = lowestValleyPrice - depthHeight;
        double highestPeakPrice = Math.max(p1, Math.max(p2, p3));
        double stopLossPrice = highestPeakPrice * 1.015; // Stop Loss set 1.5% above the highest peak
        String bias = "BEARISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(peak1.getIndex(), peak1.getPrice()));
        points.add(new ChartPattern.Point(valley1.getIndex(), valley1.getPrice()));
        points.add(new ChartPattern.Point(peak2.getIndex(), peak2.getPrice()));
        points.add(new ChartPattern.Point(valley2.getIndex(), valley2.getPrice()));
        points.add(new ChartPattern.Point(peak3.getIndex(), peak3.getPrice()));

        // Neckline represents the horizontal breakout line matching the lowest valley
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(valley1.getIndex(), lowestValleyPrice));
        necklines.add(new ChartPattern.Point(peak3.getIndex(), lowestValleyPrice));

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateDoubleTripleTopBottom(
            p1, p2, p3, 3, lowestValleyPrice,
            candles.get(valley2.getIndex()).volume, 100.0, false, 0.02, false
        );

        String typeLabel = "TRIPLE TOP";
        double priceDivergence1 = (Math.abs(p1 - p2) / Math.max(p1, p2)) * 100.0;
        double priceDivergence2 = (Math.abs(p2 - p3) / Math.max(p2, p3)) * 100.0;
        String explanation = String.format("Local math scan verified a symmetric Triple Top pattern across lookback indices [%d to %d] with low peak price divergence (%.1f%% and %.1f%%).", 
                peak1.getIndex(), peak3.getIndex(), priceDivergence1, priceDivergence2);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, peak1.getIndex(), peak3.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching lowest valley coordinates
        double retestMargin = depthHeight * 0.15;
        pattern.setRetestZoneTop(lowestValleyPrice + retestMargin);
        pattern.setRetestZoneBottom(lowestValleyPrice - retestMargin);

        return pattern;
    }
}