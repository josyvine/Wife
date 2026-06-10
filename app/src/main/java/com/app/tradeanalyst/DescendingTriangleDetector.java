package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: DescendingTriangleDetector
 * Scans swing points to isolate Descending Triangle patterns.
 * Enforces horizontal support, falling resistance, and vector convergence parameters (Phase 2).
 */
public class DescendingTriangleDetector extends BasePatternDetector {

    private static final double HORIZONTAL_VALLEY_TOLERANCE = 0.015; // Support valleys must align within 1.5%
    private static final double MIN_PEAK_DESCENT = 0.02; // Peaks must descend by at least 2% to establish slope
    private static final int MIN_TRIANGLE_WIDTH = 10; // Minimum index width in candlesticks
    private static final int MAX_TRIANGLE_WIDTH = 120; // Maximum index width in candlesticks

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 4) {
            return detected;
        }

        int totalSwings = swings.size();

        // Sliding window scanning across 4 alternating swing points (LOW - HIGH - LOW - HIGH)
        for (int i = 0; i <= totalSwings - 4; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);
            SwingPoint p4 = swings.get(i + 3);

            if (p1.getType() == SwingPoint.Type.LOW &&
                p2.getType() == SwingPoint.Type.HIGH &&
                p3.getType() == SwingPoint.Type.LOW &&
                p4.getType() == SwingPoint.Type.HIGH) {

                ChartPattern dt = verifyDescendingTriangle(candles, p1, p2, p3, p4);
                if (dt != null) {
                    detected.add(dt);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 4 candidate swing points against Descending Triangle geometric rules.
     */
    private ChartPattern verifyDescendingTriangle(
            List<Candlestick> candles, SwingPoint valley1, SwingPoint peak1, SwingPoint valley2, SwingPoint peak2) {

        int totalWidth = peak2.getIndex() - valley1.getIndex();

        // Rule 1: Validate index width boundaries
        if (totalWidth < MIN_TRIANGLE_WIDTH || totalWidth > MAX_TRIANGLE_WIDTH) {
            return null;
        }

        double v1Price = valley1.getPrice();
        double v2Price = valley2.getPrice();
        double p1Price = peak1.getPrice();
        double p2Price = peak2.getPrice();

        // Rule 2: Horizontal Support Verification (Support valleys must align within tolerance)
        if (!isWithinTolerance(v1Price, v2Price, HORIZONTAL_VALLEY_TOLERANCE)) {
            return null;
        }

        // Rule 3: Falling Resistance Verification (Peak 2 must descend strictly below Peak 1)
        double requiredDescentPrice = p1Price * (1.0 - MIN_PEAK_DESCENT);
        if (p2Price > requiredDescentPrice) {
            return null;
        }

        // Rule 4: Vector Convergence Mathematics (y = mx + c must converge over the index width)
        double supportSlope = calculateSlope(valley1.getIndex(), v1Price, valley2.getIndex(), v2Price);
        double resistanceSlope = calculateSlope(peak1.getIndex(), p1Price, peak2.getIndex(), p2Price);

        if (!areLinesConverging(resistanceSlope, supportSlope, totalWidth)) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double meanSupportPrice = (v1Price + v2Price) / 2.0;
        double triangleHeight = p1Price - meanSupportPrice;

        // Bearish breakout direction: target projects downward from flat support level
        double targetPrice = meanSupportPrice - triangleHeight;
        
        // Stop loss: placed strictly above the descending resistance line projected at the current index
        double resistanceIntercept = calculateIntercept(peak1.getIndex(), p1Price, resistanceSlope);
        double stopLossPrice = getLinePriceAt(peak2.getIndex(), resistanceSlope, resistanceIntercept) * 1.015; // 1.5% buffer above resistance
        String bias = "BEARISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(valley1.getIndex(), v1Price));
        points.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        points.add(new ChartPattern.Point(valley2.getIndex(), v2Price));
        points.add(new ChartPattern.Point(peak2.getIndex(), p2Price));

        // Neckline represents the horizontal support line connecting the valleys
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(valley1.getIndex(), meanSupportPrice));
        necklines.add(new ChartPattern.Point(peak2.getIndex(), meanSupportPrice));

        // Calculate mathematical convergence ratio
        double startSpread = Math.abs(p1Price - v1Price);
        double endSpread = Math.abs(getLinePriceAt(peak2.getIndex(), resistanceSlope, resistanceIntercept) - v2Price);
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(peak2.getIndex()).volume, 100.0, false, -0.02
        );

        String typeLabel = "DESCENDING TRIANGLE";
        String explanation = String.format("Local math scan verified a converging %s pattern with flat horizontal support around $%,.2f and falling resistance trendline.", 
                typeLabel, meanSupportPrice);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, valley1.getIndex(), peak2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching support price levels
        double retestMargin = triangleHeight * 0.15;
        pattern.setRetestZoneTop(meanSupportPrice + retestMargin);
        pattern.setRetestZoneBottom(meanSupportPrice - retestMargin);

        return pattern;
    }
}