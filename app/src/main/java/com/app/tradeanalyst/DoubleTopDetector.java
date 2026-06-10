package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: DoubleTopDetector
 * Scans swing points to isolate Double Top patterns.
 * Enforces strict peak symmetry, valley depth requirements, and minimum index spacing (Phase 2).
 */
public class DoubleTopDetector extends BasePatternDetector {

    private static final double PEAK_PRICE_TOLERANCE = 0.025; // Peaks must be within 2.5% price symmetry
    private static final double MIN_VALLEY_DEPTH = 0.03; // Valley must separate peaks by at least 3% depth
    private static final int MIN_PEAK_SEPARATION = 6; // Minimum separation width in indices
    private static final int MAX_PEAK_SEPARATION = 100; // Maximum separation width in indices

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 3) {
            return detected;
        }

        int totalSwings = swings.size();

        // 1. Sliding window scanning across 3 consecutive swing points (HIGH - LOW - HIGH)
        for (int i = 0; i <= totalSwings - 3; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);

            if (p1.getType() == SwingPoint.Type.HIGH &&
                p2.getType() == SwingPoint.Type.LOW &&
                p3.getType() == SwingPoint.Type.HIGH) {

                ChartPattern dt = verifyDoubleTop(candles, p1, p2, p3);
                if (dt != null) {
                    detected.add(dt);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 3 candidate swing points against Double Top geometric rules.
     */
    private ChartPattern verifyDoubleTop(List<Candlestick> candles, SwingPoint peak1, SwingPoint valley, SwingPoint peak2) {
        int peakDistance = peak2.getIndex() - peak1.getIndex();

        // Rule 1: Validate peak index separation bounds
        if (peakDistance < MIN_PEAK_SEPARATION || peakDistance > MAX_PEAK_SEPARATION) {
            return null;
        }

        double p1Price = peak1.getPrice();
        double p2Price = peak2.getPrice();
        double valleyPrice = valley.getPrice();

        // Rule 2: Peak Price Symmetry (Peak 1 price must align closely with Peak 2)
        if (!isWithinTolerance(p1Price, p2Price, PEAK_PRICE_TOLERANCE)) {
            return null;
        }

        // Rule 3: Valley Separation Depth (Valley must drop sufficiently below peaks)
        double meanPeakPrice = (p1Price + p2Price) / 2.0;
        double depthRatio = (meanPeakPrice - valleyPrice) / meanPeakPrice;
        if (depthRatio < MIN_VALLEY_DEPTH) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double depthHeight = meanPeakPrice - valleyPrice;

        // Bearish breakout direction: target projects downward from the neckline valley price
        double targetPrice = valleyPrice - depthHeight;
        double stopLossPrice = meanPeakPrice * 1.015; // Stop Loss set 1.5% above peak levels
        String bias = "BEARISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(peak1.getIndex(), peak1.getPrice()));
        points.add(new ChartPattern.Point(valley.getIndex(), valley.getPrice()));
        points.add(new ChartPattern.Point(peak2.getIndex(), peak2.getPrice()));

        // Neckline represents the horizontal breakout line matching valley price
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(valley.getIndex(), valley.getPrice()));
        necklines.add(new ChartPattern.Point(peak2.getIndex(), valley.getPrice()));

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateDoubleTripleTopBottom(
            p1Price, p2Price, 0.0, 2, valleyPrice,
            candles.get(valley.getIndex()).volume, 100.0, false, 0.02, false
        );

        String typeLabel = "DOUBLE TOP";
        double priceSymmetryPercent = (1.0 - (Math.abs(p1Price - p2Price) / Math.max(p1Price, p2Price))) * 100.0;
        String explanation = String.format("Local math scan verified a symmetric Double Top pattern across lookback indices [%d to %d] with %.1f%% price symmetry.", 
                peak1.getIndex(), peak2.getIndex(), priceSymmetryPercent);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, peak1.getIndex(), peak2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching valley boundary coordinates
        double retestMargin = depthHeight * 0.15;
        pattern.setRetestZoneTop(valleyPrice + retestMargin);
        pattern.setRetestZoneBottom(valleyPrice - retestMargin);

        return pattern;
    }
}