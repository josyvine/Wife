package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: RisingWedgeDetector
 * Scans swing points to isolate Rising Wedge patterns.
 * Enforces rising resistance, rising support, and steeper support convergence (Phase 2).
 */
public class RisingWedgeDetector extends BasePatternDetector {

    private static final double MIN_ASCENT_THRESHOLD = 0.01; // Minimum rise of 1% to establish upward slopes
    private static final int MIN_WEDGE_WIDTH = 10; // Minimum index width in candlesticks
    private static final int MAX_WEDGE_WIDTH = 120; // Maximum index width in candlesticks

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

                ChartPattern rw = verifyRisingWedge(candles, p1, p2, p3, p4);
                if (rw != null) {
                    detected.add(rw);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 4 candidate swing points against Rising Wedge geometric rules.
     */
    private ChartPattern verifyRisingWedge(
            List<Candlestick> candles, SwingPoint peak1, SwingPoint valley1, SwingPoint peak2, SwingPoint valley2) {

        int totalWidth = valley2.getIndex() - peak1.getIndex();

        // Rule 1: Validate index width boundaries
        if (totalWidth < MIN_WEDGE_WIDTH || totalWidth > MAX_WEDGE_WIDTH) {
            return null;
        }

        double p1Price = peak1.getPrice();
        double p2Price = peak2.getPrice();
        double v1Price = valley1.getPrice();
        double v2Price = valley2.getPrice();

        // Rule 2: Ascending Resistance (Peak 2 must be higher than Peak 1)
        double requiredPeakAscent = p1Price * (1.0 + MIN_ASCENT_THRESHOLD);
        if (p2Price < requiredPeakAscent) {
            return null;
        }

        // Rule 3: Ascending Support (Valley 2 must be higher than Valley 1)
        double requiredValleyAscent = v1Price * (1.0 + MIN_ASCENT_THRESHOLD);
        if (v2Price < requiredValleyAscent) {
            return null;
        }

        // Rule 4: Slope Convergence Math (Support slope must be strictly steeper than resistance slope)
        double resistanceSlope = calculateSlope(peak1.getIndex(), p1Price, peak2.getIndex(), p2Price);
        double supportSlope = calculateSlope(valley1.getIndex(), v1Price, valley2.getIndex(), v2Price);

        // Rising condition checks
        if (resistanceSlope <= 0 || supportSlope <= 0) {
            return null;
        }

        // Steeper support check
        if (supportSlope <= resistanceSlope) {
            return null;
        }

        // Verify boundary convergence
        if (!areLinesConverging(resistanceSlope, supportSlope, totalWidth)) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double initialWedgeHeight = p1Price - v1Price;

        // Bearish breakout direction: target projects downward from support line at checkout index
        double supportIntercept = calculateIntercept(valley1.getIndex(), v1Price, supportSlope);
        double projectedSupportBreakoutPrice = getLinePriceAt(valley2.getIndex(), supportSlope, supportIntercept);
        
        double targetPrice = projectedSupportBreakoutPrice - initialWedgeHeight;
        
        // Stop loss: placed strictly above the ascending resistance line projected at the current index
        double resistanceIntercept = calculateIntercept(peak1.getIndex(), p1Price, resistanceSlope);
        double stopLossPrice = getLinePriceAt(valley2.getIndex(), resistanceSlope, resistanceIntercept) * 1.015; // 1.5% buffer
        String bias = "BEARISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        points.add(new ChartPattern.Point(valley1.getIndex(), v1Price));
        points.add(new ChartPattern.Point(peak2.getIndex(), p2Price));
        points.add(new ChartPattern.Point(valley2.getIndex(), v2Price));

        // Upper neckline vector connecting resistance peaks
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
            candles.get(valley2.getIndex()).volume, 100.0, false, -0.02
        );

        String typeLabel = "RISING WEDGE";
        String explanation = String.format("Local math scan verified a bearish-biased %s pattern with ascending boundaries converging upwards.", 
                typeLabel);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, peak1.getIndex(), valley2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching ascending support price levels
        double retestMargin = initialWedgeHeight * 0.15;
        pattern.setRetestZoneTop(projectedSupportBreakoutPrice + retestMargin);
        pattern.setRetestZoneBottom(projectedSupportBreakoutPrice - retestMargin);

        return pattern;
    }
}