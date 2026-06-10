package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: RectangleDetector
 * Scans swing points to isolate Rectangle consolidations.
 * Enforces flat horizontal resistance ceilings, flat support floors, and parallel bounds (Phase 2).
 */
public class RectangleDetector extends BasePatternDetector {

    private static final double HORIZONTAL_CEILING_TOLERANCE = 0.015; // Resistance peaks must align within 1.5%
    private static final double HORIZONTAL_FLOOR_TOLERANCE = 0.015; // Support valleys must align within 1.5%
    private static final double PARALLELISM_TOLERANCE = 0.02; // Slope divergence tolerance of 2%
    private static final int MIN_RECTANGLE_WIDTH = 12; // Minimum index width in candlesticks
    private static final int MAX_RECTANGLE_WIDTH = 150; // Maximum index width in candlesticks

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

                ChartPattern rc = verifyRectangle(candles, p1, p2, p3, p4);
                if (rc != null) {
                    detected.add(rc);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 4 candidate swing points against Rectangle geometric rules.
     */
    private ChartPattern verifyRectangle(
            List<Candlestick> candles, SwingPoint peak1, SwingPoint valley1, SwingPoint peak2, SwingPoint valley2) {

        int totalWidth = valley2.getIndex() - peak1.getIndex();

        // Rule 1: Validate index width boundaries
        if (totalWidth < MIN_RECTANGLE_WIDTH || totalWidth > MAX_RECTANGLE_WIDTH) {
            return null;
        }

        double p1Price = peak1.getPrice();
        double p2Price = peak2.getPrice();
        double v1Price = valley1.getPrice();
        double v2Price = valley2.getPrice();

        // Rule 2: Horizontal Resistance Ceiling (Resistance peaks must align within tolerance)
        if (!isWithinTolerance(p1Price, p2Price, HORIZONTAL_CEILING_TOLERANCE)) {
            return null;
        }

        // Rule 3: Horizontal Support Floor (Support valleys must align within tolerance)
        if (!isWithinTolerance(v1Price, v2Price, HORIZONTAL_FLOOR_TOLERANCE)) {
            return null;
        }

        // Rule 4: Parallel Boundary Verification (Both slopes must approach zero and be parallel)
        double resistanceSlope = calculateSlope(peak1.getIndex(), p1Price, peak2.getIndex(), p2Price);
        double supportSlope = calculateSlope(valley1.getIndex(), v1Price, valley2.getIndex(), v2Price);

        if (Math.abs(resistanceSlope - supportSlope) > PARALLELISM_TOLERANCE) {
            return null;
        }

        // 2. DETERMINE DIRECTION BIAS AND CONSTRUCT TARGETS
        double trendSlope = IndicatorsEngine.calculateSlope(candles, 40);
        String bias = trendSlope >= 0 ? "BULLISH" : "BEARISH";

        double meanCeiling = (p1Price + p2Price) / 2.0;
        double meanFloor = (v1Price + v2Price) / 2.0;
        double channelHeight = meanCeiling - meanFloor;

        double targetPrice;
        double stopLossPrice;

        // Rectangle breakout projection: project channel height from the respective breakout boundary
        if ("BULLISH".equals(bias)) {
            targetPrice = meanCeiling + channelHeight;
            stopLossPrice = meanCeiling - (channelHeight * 0.50); // Protected at the channel midpoint
        } else {
            targetPrice = meanFloor - channelHeight;
            stopLossPrice = meanFloor + (channelHeight * 0.50); // Protected at the channel midpoint
        }

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        points.add(new ChartPattern.Point(valley1.getIndex(), v1Price));
        points.add(new ChartPattern.Point(peak2.getIndex(), p2Price));
        points.add(new ChartPattern.Point(valley2.getIndex(), v2Price));

        // Upper neckline vector representing the flat resistance ceiling
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(peak1.getIndex(), meanCeiling));
        necklines.add(new ChartPattern.Point(valley2.getIndex(), meanCeiling));

        // Calculate channel symmetry convergence ratio (perfect parallel channel approaches 1.0)
        double startSpread = Math.abs(p1Price - v1Price);
        double endSpread = Math.abs(p2Price - v2Price);
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(valley2.getIndex()).volume, 100.0, false, trendSlope
        );

        String typeLabel = "RECTANGLE";
        String explanation = String.format("Local math scan verified a parallel %s consolidation channel between flat resistance at $%,.2f and support floor at $%,.2f.", 
                typeLabel, meanCeiling, meanFloor);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, peak1.getIndex(), valley2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching flat breakout boundary coordinates
        double retestMargin = channelHeight * 0.15;
        if ("BULLISH".equals(bias)) {
            pattern.setRetestZoneTop(meanCeiling + retestMargin);
            pattern.setRetestZoneBottom(meanCeiling - retestMargin);
        } else {
            pattern.setRetestZoneTop(meanFloor + retestMargin);
            pattern.setRetestZoneBottom(meanFloor - retestMargin);
        }

        return pattern;
    }
}