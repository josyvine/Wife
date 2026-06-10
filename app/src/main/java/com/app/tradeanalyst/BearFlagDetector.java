package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: BearFlagDetector
 * Scans swing points to isolate Bear Flag patterns.
 * Enforces flagpole drop heights, parallel ascending flag channels, and golden ratio retracement limits (Phase 2).
 */
public class BearFlagDetector extends BasePatternDetector {

    private static final double MIN_POLE_DROP_PERCENT = 0.06; // Preceding flagpole must be at least a 6% sharp drop
    private static final int MAX_POLE_DURATION = 15; // Flagpole must form within a maximum of 15 candles
    private static final double MAX_RETRACEMENT_RATIO = 0.50; // Flag channel cannot retrace more than 50% of the pole
    private static final double PARALLELISM_TOLERANCE = 0.05; // Maximum allowed slope delta for boundary parallelism
    private static final int MIN_FLAG_WIDTH = 6; // Minimum width of flag consolidation in indices
    private static final int MAX_FLAG_WIDTH = 40; // Maximum width of flag consolidation in indices

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 5) {
            return detected;
        }

        int totalSwings = swings.size();

        // 1. Scan for a preceding flagpole drop and a 4-point alternating ascending channel
        // Sequence: Swing High (Pole peak) -> LOW (Flag Low 1) -> HIGH (Flag High 1) -> LOW (Flag Low 2) -> HIGH (Flag High 2)
        for (int i = 0; i <= totalSwings - 5; i++) {
            SwingPoint p1 = swings.get(i); // Flagpole Peak (HIGH)
            SwingPoint p2 = swings.get(i + 1); // Flag Low 1 (LOW)
            SwingPoint p3 = swings.get(i + 2); // Flag High 1 (HIGH)
            SwingPoint p4 = swings.get(i + 3); // Flag Low 2 (LOW)
            SwingPoint p5 = swings.get(i + 4); // Flag High 2 (HIGH)

            if (p1.getType() == SwingPoint.Type.HIGH &&
                p2.getType() == SwingPoint.Type.LOW &&
                p3.getType() == SwingPoint.Type.HIGH &&
                p4.getType() == SwingPoint.Type.LOW &&
                p5.getType() == SwingPoint.Type.HIGH) {

                ChartPattern bf = verifyBearFlag(candles, p1, p2, p3, p4, p5);
                if (bf != null) {
                    detected.add(bf);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the flagpole drop and flag channel properties against Bear Flag geometric rules.
     */
    private ChartPattern verifyBearFlag(
            List<Candlestick> candles, SwingPoint polePeak, SwingPoint flagLow1, 
            SwingPoint flagHigh1, SwingPoint flagLow2, SwingPoint flagHigh2) {

        int poleDuration = flagLow1.getIndex() - polePeak.getIndex();
        int flagWidth = flagHigh2.getIndex() - flagLow1.getIndex();

        // Rule 1: Validate flagpole duration limits
        if (poleDuration <= 0 || poleDuration > MAX_POLE_DURATION) {
            return null;
        }

        // Rule 2: Validate flag consolidation width limits
        if (flagWidth < MIN_FLAG_WIDTH || flagWidth > MAX_FLAG_WIDTH) {
            return null;
        }

        double peakPrice = polePeak.getPrice();
        double basePrice = flagLow1.getPrice();
        double poleHeight = peakPrice - basePrice;

        // Rule 3: Flagpole Drop Height Verification (Must meet minimum percentage drop)
        double minRequiredDrop = peakPrice * MIN_POLE_DROP_PERCENT;
        if (poleHeight < minRequiredDrop) {
            return null;
        }

        double fl1 = flagLow1.getPrice();
        double fl2 = flagLow2.getPrice();
        double fh1 = flagHigh1.getPrice();
        double fh2 = flagHigh2.getPrice();

        // Rule 4: Retracement Limit Check (Consolidation high must be below 50% retracement line)
        double highestFlagHigh = Math.max(fh1, fh2);
        double maximumRetracementLevel = basePrice + (poleHeight * MAX_RETRACEMENT_RATIO);
        if (highestFlagHigh > maximumRetracementLevel) {
            return null;
        }

        // Rule 5: Parallelism Slope Math (Slope of upper and lower flag bounds must be ascending and parallel)
        double supportSlope = calculateSlope(flagLow1.getIndex(), fl1, flagLow2.getIndex(), fl2);
        double resistanceSlope = calculateSlope(flagHigh1.getIndex(), fh1, flagHigh2.getIndex(), fh2);

        // Slopes must be positive (ascending channel)
        if (supportSlope < -0.001 || resistanceSlope < -0.001) {
            return null;
        }

        // Slopes must be approximately parallel
        if (Math.abs(resistanceSlope - supportSlope) > PARALLELISM_TOLERANCE) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double supportIntercept = calculateIntercept(flagLow1.getIndex(), fl1, supportSlope);
        double projectedBreakoutPrice = getLinePriceAt(flagHigh2.getIndex(), supportSlope, supportIntercept);
        
        // Measured Move Target: project full flagpole height downwards from flag support line at current index
        double targetPrice = projectedBreakoutPrice - poleHeight;
        
        // Stop loss: placed strictly above the highest high of the flag channel with 1% margin
        double stopLossPrice = highestFlagHigh * 1.01;
        String bias = "BEARISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(polePeak.getIndex(), peakPrice));
        points.add(new ChartPattern.Point(flagLow1.getIndex(), fl1));
        points.add(new ChartPattern.Point(flagHigh1.getIndex(), fh1));
        points.add(new ChartPattern.Point(flagLow2.getIndex(), fl2));
        points.add(new ChartPattern.Point(flagHigh2.getIndex(), fh2));

        // Lower neckline vector representing flag channel support line
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(flagLow1.getIndex(), fl1));
        necklines.add(new ChartPattern.Point(flagHigh2.getIndex(), projectedBreakoutPrice));

        // Calculate symmetry ratio of the channel width to height
        double startSpread = Math.abs(fh1 - fl1);
        double endSpread = Math.abs(fh2 - getLinePriceAt(flagHigh2.getIndex(), supportSlope, supportIntercept));
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(flagHigh2.getIndex()).volume, 100.0, false, -0.02
        );

        String typeLabel = "BEAR FLAG";
        String explanation = String.format("Local math scan verified a high-probability %s pattern with flagpole height $%,.2f and ascending parallel channel.", 
                typeLabel, poleHeight);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, polePeak.getIndex(), flagHigh2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching flag lower support price level
        double retestMargin = initialWedgeHeight(fh1, fl1) * 0.15;
        pattern.setRetestZoneTop(projectedBreakoutPrice + retestMargin);
        pattern.setRetestZoneBottom(projectedBreakoutPrice - retestMargin);

        return pattern;
    }

    private double initialWedgeHeight(double h, double l) {
        return Math.abs(h - l);
    }
}