package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: AscendingTriangleDetector
 * Scans swing points to isolate Ascending Triangle patterns.
 * Enforces horizontal resistance, rising support, and vector convergence parameters (Phase 2).
 */
public class AscendingTriangleDetector extends BasePatternDetector {

    private static final double HORIZONTAL_PEAK_TOLERANCE = 0.015; // Resistance peaks must align within 1.5%
    private static final double MIN_VALLEY_ASCENT = 0.02; // Valleys must rise by at least 2% to establish slope
    private static final int MIN_TRIANGLE_WIDTH = 10; // Minimum index width in candlesticks
    private static final int MAX_TRIANGLE_WIDTH = 120; // Maximum index width in candlesticks

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 4) {
            return detected;
        }

        int totalSwings = swings.size();

        // Sliding window scanning across 4 alternating swing points (HIGH - LOW - HIGH - LOW)
        for (int i = 0; i <= totalSwings - 4; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);
            SwingPoint p4 = swings.get(i + 3);

            if (p1.getType() == SwingPoint.Type.HIGH &&
                p2.getType() == SwingPoint.Type.LOW &&
                p3.getType() == SwingPoint.Type.HIGH &&
                p4.getType() == SwingPoint.Type.LOW) {

                ChartPattern at = verifyAscendingTriangle(candles, p1, p2, p3, p4);
                if (at != null) {
                    detected.add(at);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 4 candidate swing points against Ascending Triangle geometric rules.
     */
    private ChartPattern verifyAscendingTriangle(
            List<Candlestick> candles, SwingPoint peak1, SwingPoint valley1, SwingPoint peak2, SwingPoint valley2) {

        int totalWidth = valley2.getIndex() - peak1.getIndex();

        // Rule 1: Validate index width boundaries
        if (totalWidth < MIN_TRIANGLE_WIDTH || totalWidth > MAX_TRIANGLE_WIDTH) {
            return null;
        }

        double p1Price = peak1.getPrice();
        double p2Price = peak2.getPrice();
        double v1Price = valley1.getPrice();
        double v2Price = valley2.getPrice();

        // Rule 2: Horizontal Resistance Verification (Resistance peaks must align within tolerance)
        if (!isWithinTolerance(p1Price, p2Price, HORIZONTAL_PEAK_TOLERANCE)) {
            return null;
        }

        // Rule 3: Rising Support Verification (Valley 2 must rise strictly above Valley 1)
        double requiredAscentPrice = v1Price * (1.0 + MIN_VALLEY_ASCENT);
        if (v2Price < requiredAscentPrice) {
            return null;
        }

        // Rule 4: Vector Convergence Mathematics (y = mx + c must converge over the index width)
        double resistanceSlope = calculateSlope(peak1.getIndex(), p1Price, peak2.getIndex(), p2Price);
        double supportSlope = calculateSlope(valley1.getIndex(), v1Price, valley2.getIndex(), v2Price);

        if (!areLinesConverging(resistanceSlope, supportSlope, totalWidth)) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double meanResistancePrice = (p1Price + p2Price) / 2.0;
        double triangleHeight = meanResistancePrice - v1Price;

        // Bullish breakout direction: target projects upward from flat resistance level
        double targetPrice = meanResistancePrice + triangleHeight;
        
        // Stop loss: placed strictly below the ascending support line projected at the current index
        double supportIntercept = calculateIntercept(valley1.getIndex(), v1Price, supportSlope);
        double stopLossPrice = getLinePriceAt(valley2.getIndex(), supportSlope, supportIntercept) * 0.985; // 1.5% buffer below support
        String bias = "BULLISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        points.add(new ChartPattern.Point(valley1.getIndex(), v1Price));
        points.add(new ChartPattern.Point(peak2.getIndex(), p2Price));
        points.add(new ChartPattern.Point(valley2.getIndex(), v2Price));

        // Neckline represents the horizontal resistance line connecting the peaks
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(peak1.getIndex(), meanResistancePrice));
        necklines.add(new ChartPattern.Point(valley2.getIndex(), meanResistancePrice));

        // Calculate mathematical convergence ratio
        double startSpread = Math.abs(p1Price - v1Price);
        double endSpread = Math.abs(getLinePriceAt(valley2.getIndex(), resistanceSlope, calculateIntercept(peak1.getIndex(), p1Price, resistanceSlope)) - v2Price);
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(valley2.getIndex()).volume, 100.0, false, 0.02
        );

        String typeLabel = "ASCENDING TRIANGLE";
        String explanation = String.format("Local math scan verified a converging %s pattern with flat horizontal resistance around $%,.2f and rising support trendline.", 
                typeLabel, meanResistancePrice);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, peak1.getIndex(), valley2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching resistance price levels
        double retestMargin = triangleHeight * 0.15;
        pattern.setRetestZoneTop(meanResistancePrice + retestMargin);
        pattern.setRetestZoneBottom(meanResistancePrice - retestMargin);

        return pattern;
    }
}