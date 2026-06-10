package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: BullFlagDetector
 * Scans swing points to isolate Bull Flag patterns.
 * Enforces flagpole surge heights, parallel descending flag channels, and golden ratio retracement limits (Phase 2).
 */
public class BullFlagDetector extends BasePatternDetector {

    private static final double MIN_POLE_SURGE_PERCENT = 0.06; // Preceding flagpole must be at least a 6% sharp rise
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

        // 1. Scan for a preceding flagpole surge and a 4-point alternating descending channel
        // Sequence: Swing Low (Pole base) -> HIGH (Flag High 1) -> LOW (Flag Low 1) -> HIGH (Flag High 2) -> LOW (Flag Low 2)
        for (int i = 0; i <= totalSwings - 5; i++) {
            SwingPoint p1 = swings.get(i); // Flagpole Base (LOW)
            SwingPoint p2 = swings.get(i + 1); // Flag High 1 (HIGH)
            SwingPoint p3 = swings.get(i + 2); // Flag Low 1 (LOW)
            SwingPoint p4 = swings.get(i + 3); // Flag High 2 (HIGH)
            SwingPoint p5 = swings.get(i + 4); // Flag Low 2 (LOW)

            if (p1.getType() == SwingPoint.Type.LOW &&
                p2.getType() == SwingPoint.Type.HIGH &&
                p3.getType() == SwingPoint.Type.LOW &&
                p4.getType() == SwingPoint.Type.HIGH &&
                p5.getType() == SwingPoint.Type.LOW) {

                ChartPattern bf = verifyBullFlag(candles, p1, p2, p3, p4, p5);
                if (bf != null) {
                    detected.add(bf);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the flagpole surge and flag channel properties against Bull Flag geometric rules.
     */
    private ChartPattern verifyBullFlag(
            List<Candlestick> candles, SwingPoint poleBase, SwingPoint flagHigh1, 
            SwingPoint flagLow1, SwingPoint flagHigh2, SwingPoint flagLow2) {

        int poleDuration = flagHigh1.getIndex() - poleBase.getIndex();
        int flagWidth = flagLow2.getIndex() - flagHigh1.getIndex();

        // Rule 1: Validate flagpole duration limits
        if (poleDuration <= 0 || poleDuration > MAX_POLE_DURATION) {
            return null;
        }

        // Rule 2: Validate flag consolidation width limits
        if (flagWidth < MIN_FLAG_WIDTH || flagWidth > MAX_FLAG_WIDTH) {
            return null;
        }

        double basePrice = poleBase.getPrice();
        double peakPrice = flagHigh1.getPrice();
        double poleHeight = peakPrice - basePrice;

        // Rule 3: Flagpole Surge Height Verification (Must meet minimum percentage height)
        double minRequiredSurge = basePrice * MIN_POLE_SURGE_PERCENT;
        if (poleHeight < minRequiredSurge) {
            return null;
        }

        double fh1 = flagHigh1.getPrice();
        double fh2 = flagHigh2.getPrice();
        double fl1 = flagLow1.getPrice();
        double fl2 = flagLow2.getPrice();

        // Rule 4: Retracement Limit Check (Consolidation low must be above 50% retracement line)
        double lowestFlagLow = Math.min(fl1, fl2);
        double maximumRetracementLevel = peakPrice - (poleHeight * MAX_RETRACEMENT_RATIO);
        if (lowestFlagLow < maximumRetracementLevel) {
            return null;
        }

        // Rule 5: Parallelism Slope Math (Slope of upper and lower flag bounds must be descending and parallel)
        double resistanceSlope = calculateSlope(flagHigh1.getIndex(), fh1, flagHigh2.getIndex(), fh2);
        double supportSlope = calculateSlope(flagLow1.getIndex(), fl1, flagLow2.getIndex(), fl2);

        // Slopes must be negative (descending channel)
        if (resistanceSlope > 0.001 || supportSlope > 0.001) {
            return null;
        }

        // Slopes must be approximately parallel
        if (Math.abs(resistanceSlope - supportSlope) > PARALLELISM_TOLERANCE) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double resistanceIntercept = calculateIntercept(flagHigh1.getIndex(), fh1, resistanceSlope);
        double projectedBreakoutPrice = getLinePriceAt(flagLow2.getIndex(), resistanceSlope, resistanceIntercept);
        
        // Measured Move Target: project full flagpole height upwards from flag resistance line at current index
        double targetPrice = projectedBreakoutPrice + poleHeight;
        
        // Stop loss: placed strictly below the lowest low of the flag channel with 1% margin
        double stopLossPrice = lowestFlagLow * 0.99;
        String bias = "BULLISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(poleBase.getIndex(), basePrice));
        points.add(new ChartPattern.Point(flagHigh1.getIndex(), fh1));
        points.add(new ChartPattern.Point(flagLow1.getIndex(), fl1));
        points.add(new ChartPattern.Point(flagHigh2.getIndex(), fh2));
        points.add(new ChartPattern.Point(flagLow2.getIndex(), fl2));

        // Upper neckline vector representing flag channel resistance line
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(flagHigh1.getIndex(), fh1));
        necklines.add(new ChartPattern.Point(flagLow2.getIndex(), projectedBreakoutPrice));

        // Calculate symmetry ratio of the channel width to height
        double startSpread = Math.abs(fh1 - fl1);
        double endSpread = Math.abs(getLinePriceAt(flagLow2.getIndex(), resistanceSlope, resistanceIntercept) - fl2);
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(flagLow2.getIndex()).volume, 100.0, false, 0.02
        );

        String typeLabel = "BULL FLAG";
        String explanation = String.format("Local math scan verified a high-probability %s pattern with flagpole height $%,.2f and descending parallel channel.", 
                typeLabel, poleHeight);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, poleBase.getIndex(), flagLow2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching flag upper resistance price level
        double retestMargin = initialWedgeHeight(fh1, fl1) * 0.15;
        pattern.setRetestZoneTop(projectedBreakoutPrice + retestMargin);
        pattern.setRetestZoneBottom(projectedBreakoutPrice - retestMargin);

        return pattern;
    }

    private double initialWedgeHeight(double h, double l) {
        return Math.abs(h - l);
    }
}