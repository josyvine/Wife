package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: SymmetricalTriangleDetector
 * Scans swing points to isolate Symmetrical Triangle patterns.
 * Enforces falling resistance, rising support, and vector convergence parameters (Phase 2).
 */
public class SymmetricalTriangleDetector extends BasePatternDetector {

    private static final double MIN_PEAK_DESCENT = 0.015; // Peaks must descend by at least 1.5%
    private static final double MIN_VALLEY_ASCENT = 0.015; // Valleys must rise by at least 1.5%
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

                ChartPattern st = verifySymmetricalTriangle(candles, p1, p2, p3, p4);
                if (st != null) {
                    detected.add(st);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 4 candidate swing points against Symmetrical Triangle geometric rules.
     */
    private ChartPattern verifySymmetricalTriangle(
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

        // Rule 2: Falling Resistance Verification (Peak 2 must descend strictly below Peak 1)
        double requiredDescentPrice = p1Price * (1.0 - MIN_PEAK_DESCENT);
        if (p2Price > requiredDescentPrice) {
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

        // 2. DETERMINE TREND DIRECTION BIAS
        // Symmetrical triangles generally break out in direction of preceding trend
        double trendSlope = IndicatorsEngine.calculateSlope(candles, 40);
        String bias = trendSlope >= 0 ? "BULLISH" : "BEARISH";

        // 3. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double triangleHeight = p1Price - v1Price;
        double targetPrice;
        double stopLossPrice;

        double resistanceIntercept = calculateIntercept(peak1.getIndex(), p1Price, resistanceSlope);
        double supportIntercept = calculateIntercept(valley1.getIndex(), v1Price, supportSlope);

        if ("BULLISH".equals(bias)) {
            // Target projects upward from descending resistance boundary intersection
            double projectedBreakoutResistance = getLinePriceAt(valley2.getIndex(), resistanceSlope, resistanceIntercept);
            targetPrice = projectedBreakoutResistance + triangleHeight;
            // Stop loss protected just below the ascending support trendline
            stopLossPrice = getLinePriceAt(valley2.getIndex(), supportSlope, supportIntercept) * 0.985;
        } else {
            // Target projects downward from ascending support boundary intersection
            double projectedBreakoutSupport = getLinePriceAt(valley2.getIndex(), supportSlope, supportIntercept);
            targetPrice = projectedBreakoutSupport - triangleHeight;
            // Stop loss protected just above the descending resistance trendline
            stopLossPrice = getLinePriceAt(valley2.getIndex(), resistanceSlope, resistanceIntercept) * 1.015;
        }

        // 4. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        points.add(new ChartPattern.Point(valley1.getIndex(), v1Price));
        points.add(new ChartPattern.Point(peak2.getIndex(), p2Price));
        points.add(new ChartPattern.Point(valley2.getIndex(), v2Price));

        // Symmetric neckline is drawn connecting Peak 1 to Valley 2 reference lines
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        necklines.add(new ChartPattern.Point(valley2.getIndex(), getLinePriceAt(valley2.getIndex(), resistanceSlope, resistanceIntercept)));

        // Calculate mathematical convergence ratio
        double startSpread = Math.abs(p1Price - v1Price);
        double endSpread = Math.abs(getLinePriceAt(valley2.getIndex(), resistanceSlope, resistanceIntercept) - getLinePriceAt(valley2.getIndex(), supportSlope, supportIntercept));
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(valley2.getIndex()).volume, 100.0, false, trendSlope
        );

        String typeLabel = "SYMMETRICAL TRIANGLE";
        String explanation = String.format("Local math scan verified a symmetric %s pattern across lookback indices [%d to %d] with converging boundaries.", 
                typeLabel, peak1.getIndex(), valley2.getIndex());

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, peak1.getIndex(), valley2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching median triangle price coordinates
        double medianPrice = (p2Price + v2Price) / 2.0;
        double retestMargin = triangleHeight * 0.10;
        pattern.setRetestZoneTop(medianPrice + retestMargin);
        pattern.setRetestZoneBottom(medianPrice - retestMargin);

        return pattern;
    }
}