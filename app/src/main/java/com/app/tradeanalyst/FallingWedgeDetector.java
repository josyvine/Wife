package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: FallingWedgeDetector
 * Scans swing points to isolate Falling Wedge patterns.
 * Enforces falling resistance, falling support, and steeper resistance convergence (Phase 2).
 */
public class FallingWedgeDetector extends BasePatternDetector {

    private static final double MIN_DESCENT_THRESHOLD = 0.01; // Minimum drop of 1% to establish downward slopes
    private static final int MIN_WEDGE_WIDTH = 10; // Minimum index width in candlesticks
    private static final int MAX_WEDGE_WIDTH = 120; // Maximum index width in candlesticks

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

                ChartPattern fw = verifyFallingWedge(candles, p1, p2, p3, p4);
                if (fw != null) {
                    detected.add(fw);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 4 candidate swing points against Falling Wedge geometric rules.
     */
    private ChartPattern verifyFallingWedge(
            List<Candlestick> candles, SwingPoint valley1, SwingPoint peak1, SwingPoint valley2, SwingPoint peak2) {

        int totalWidth = peak2.getIndex() - valley1.getIndex();

        // Rule 1: Validate index width boundaries
        if (totalWidth < MIN_WEDGE_WIDTH || totalWidth > MAX_WEDGE_WIDTH) {
            return null;
        }

        double v1Price = valley1.getPrice();
        double v2Price = valley2.getPrice();
        double p1Price = peak1.getPrice();
        double p2Price = peak2.getPrice();

        // Rule 2: Descending Support (Valley 2 must be lower than Valley 1)
        double requiredValleyDescent = v1Price * (1.0 - MIN_DESCENT_THRESHOLD);
        if (v2Price > requiredValleyDescent) {
            return null;
        }

        // Rule 3: Descending Resistance (Peak 2 must be lower than Peak 1)
        double requiredPeakDescent = p1Price * (1.0 - MIN_DESCENT_THRESHOLD);
        if (p2Price > requiredPeakDescent) {
            return null;
        }

        // Rule 4: Slope Convergence Math (Resistance slope must be strictly steeper than support slope)
        double resistanceSlope = calculateSlope(peak1.getIndex(), p1Price, peak2.getIndex(), p2Price);
        double supportSlope = calculateSlope(valley1.getIndex(), v1Price, valley2.getIndex(), v2Price);

        // Falling condition checks
        if (resistanceSlope >= 0 || supportSlope >= 0) {
            return null;
        }

        // Steeper resistance check (e.g. -0.05 is less than -0.02, falling faster)
        if (resistanceSlope >= supportSlope) {
            return null;
        }

        // Verify boundary convergence
        if (!areLinesConverging(resistanceSlope, supportSlope, totalWidth)) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double initialWedgeHeight = p1Price - v1Price;

        // Bullish breakout direction: target projects upward from resistance line at breakout index
        double resistanceIntercept = calculateIntercept(peak1.getIndex(), p1Price, resistanceSlope);
        double projectedResistanceBreakoutPrice = getLinePriceAt(peak2.getIndex(), resistanceSlope, resistanceIntercept);
        
        double targetPrice = projectedResistanceBreakoutPrice + initialWedgeHeight;
        
        // Stop loss: placed strictly below the descending support line projected at the current index
        double supportIntercept = calculateIntercept(valley1.getIndex(), v1Price, supportSlope);
        double stopLossPrice = getLinePriceAt(peak2.getIndex(), supportSlope, supportIntercept) * 0.985; // 1.5% buffer
        String bias = "BULLISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(valley1.getIndex(), v1Price));
        points.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        points.add(new ChartPattern.Point(valley2.getIndex(), v2Price));
        points.add(new ChartPattern.Point(peak2.getIndex(), p2Price));

        // Lower neckline vector connecting support valleys
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(valley1.getIndex(), v1Price));
        necklines.add(new ChartPattern.Point(peak2.getIndex(), getLinePriceAt(peak2.getIndex(), supportSlope, supportIntercept)));

        // Calculate mathematical convergence ratio
        double startSpread = Math.abs(p1Price - v1Price);
        double endSpread = Math.abs(getLinePriceAt(peak2.getIndex(), resistanceSlope, resistanceIntercept) - getLinePriceAt(peak2.getIndex(), supportSlope, supportIntercept));
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(peak2.getIndex()).volume, 100.0, false, 0.02
        );

        String typeLabel = "FALLING WEDGE";
        String explanation = String.format("Local math scan verified a bullish-biased %s pattern with descending boundaries converging downwards.", 
                typeLabel);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, valley1.getIndex(), peak2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching descending resistance price levels
        double retestMargin = initialWedgeHeight * 0.15;
        pattern.setRetestZoneTop(projectedResistanceBreakoutPrice + retestMargin);
        pattern.setRetestZoneBottom(projectedResistanceBreakoutPrice - retestMargin);

        return pattern;
    }
}