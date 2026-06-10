package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: PennantDetector
 * Scans swing points to isolate Bullish and Bearish Pennant continuation patterns.
 * Enforces preceding flagpole dynamics, converging bounds, and retracement limits (Phase 2).
 */
public class PennantDetector extends BasePatternDetector {

    private static final double MIN_POLE_SURGE_PERCENT = 0.06; // Preceding flagpole must be at least a 6% sharp move
    private static final int MAX_POLE_DURATION = 15; // Flagpole must form within a maximum of 15 candles
    private static final double MAX_RETRACEMENT_RATIO = 0.50; // Pennant retrace cannot exceed 50% of the pole height
    private static final int MIN_PENNANT_WIDTH = 6; // Minimum width of consolidation in indices
    private static final int MAX_PENNANT_WIDTH = 40; // Maximum width of consolidation in indices

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 5) {
            return detected;
        }

        int totalSwings = swings.size();

        // 1. Scan for outer flagpole move and 4-point alternating symmetrical pennant
        // Bullish Pennant Sequence: LOW (Pole base) -> HIGH (Pennant High 1) -> LOW (Pennant Low 1) -> HIGH (Pennant High 2) -> LOW (Pennant Low 2)
        // Bearish Pennant Sequence: HIGH (Pole peak) -> LOW (Pennant Low 1) -> HIGH (Pennant High 1) -> LOW (Pennant Low 2) -> HIGH (Pennant High 2)
        for (int i = 0; i <= totalSwings - 5; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);
            SwingPoint p4 = swings.get(i + 3);
            SwingPoint p5 = swings.get(i + 4);

            // Bullish Pennant checks
            if (p1.getType() == SwingPoint.Type.LOW &&
                p2.getType() == SwingPoint.Type.HIGH &&
                p3.getType() == SwingPoint.Type.LOW &&
                p4.getType() == SwingPoint.Type.HIGH &&
                p5.getType() == SwingPoint.Type.LOW) {

                ChartPattern bp = verifyPennant(candles, p1, p2, p3, p4, p5, false);
                if (bp != null) {
                    detected.add(bp);
                }
            }

            // Bearish Pennant checks
            if (p1.getType() == SwingPoint.Type.HIGH &&
                p2.getType() == SwingPoint.Type.LOW &&
                p3.getType() == SwingPoint.Type.HIGH &&
                p4.getType() == SwingPoint.Type.LOW &&
                p5.getType() == SwingPoint.Type.HIGH) {

                ChartPattern bp = verifyPennant(candles, p1, p2, p3, p4, p5, true);
                if (bp != null) {
                    detected.add(bp);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the flagpole and pennant properties against geometric rules.
     */
    private ChartPattern verifyPennant(
            List<Candlestick> candles, SwingPoint poleStart, SwingPoint pennant1, 
            SwingPoint pennant2, SwingPoint pennant3, SwingPoint pennant4, boolean isBearish) {

        int poleDuration = pennant1.getIndex() - poleStart.getIndex();
        int pennantWidth = pennant4.getIndex() - pennant1.getIndex();

        // Rule 1: Validate flagpole duration limits
        if (poleDuration <= 0 || poleDuration > MAX_POLE_DURATION) {
            return null;
        }

        // Rule 2: Validate pennant consolidation width limits
        if (pennantWidth < MIN_PENNANT_WIDTH || pennantWidth > MAX_PENNANT_WIDTH) {
            return null;
        }

        double startPrice = poleStart.getPrice();
        double peakPrice = pennant1.getPrice();
        double poleHeight = Math.abs(peakPrice - startPrice);

        // Rule 3: Flagpole Height Verification (Must meet minimum percentage height)
        double minRequiredMove = startPrice * MIN_POLE_SURGE_PERCENT;
        if (poleHeight < minRequiredMove) {
            return null;
        }

        double pe1 = pennant1.getPrice();
        double pe2 = pennant2.getPrice();
        double pe3 = pennant3.getPrice();
        double pe4 = pennant4.getPrice();

        // Rule 4: Retracement Limit Check (Consolidation low/high must be within 50% of the pole height)
        if (isBearish) {
            double highestConsolidationHigh = Math.max(pe2, pe4);
            double maximumRetracementLevel = peakPrice - (poleHeight * MAX_RETRACEMENT_RATIO);
            if (highestConsolidationHigh > maximumRetracementLevel) {
                return null;
            }
        } else {
            double lowestConsolidationLow = Math.min(pe2, pe4);
            double maximumRetracementLevel = peakPrice - (poleHeight * MAX_RETRACEMENT_RATIO);
            if (lowestConsolidationLow < maximumRetracementLevel) {
                return null;
            }
        }

        // Rule 5: Symmetrical Boundary Convergence Verification (m_res and m_sup must converge)
        double resistanceSlope;
        double supportSlope;

        if (isBearish) {
            // Support valleys are ascending, resistance peaks are descending (converging channels)
            supportSlope = calculateSlope(pennant1.getIndex(), pe1, pennant3.getIndex(), pe3);
            resistanceSlope = calculateSlope(pennant2.getIndex(), pe2, pennant4.getIndex(), pe4);
        } else {
            // Resistance peaks are descending, support valleys are ascending
            resistanceSlope = calculateSlope(pennant1.getIndex(), pe1, pennant3.getIndex(), pe3);
            supportSlope = calculateSlope(pennant2.getIndex(), pe2, pennant4.getIndex(), pe4);
        }

        if (!areLinesConverging(resistanceSlope, supportSlope, pennantWidth)) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double resistanceIntercept = calculateIntercept(pennant1.getIndex(), pe1, resistanceSlope);
        double supportIntercept = calculateIntercept(pennant2.getIndex(), pe2, supportSlope);

        double projectedResistance = getLinePriceAt(pennant4.getIndex(), resistanceSlope, resistanceIntercept);
        double projectedSupport = getLinePriceAt(pennant4.getIndex(), supportSlope, supportIntercept);

        double targetPrice;
        double stopLossPrice;
        String bias;

        if (isBearish) {
            // Bearish pennant continuation: target projects flagpole height downwards from support line
            targetPrice = projectedSupport - poleHeight;
            stopLossPrice = projectedResistance * 1.015; // 1.5% buffer above resistance
            bias = "BEARISH";
        } else {
            // Bullish pennant continuation: target projects flagpole height upwards from resistance line
            targetPrice = projectedResistance + poleHeight;
            stopLossPrice = projectedSupport * 0.985; // 1.5% buffer below support
            bias = "BULLISH";
        }

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(poleStart.getIndex(), startPrice));
        points.add(new ChartPattern.Point(pennant1.getIndex(), pe1));
        points.add(new ChartPattern.Point(pennant2.getIndex(), pe2));
        points.add(new ChartPattern.Point(pennant3.getIndex(), pe3));
        points.add(new ChartPattern.Point(pennant4.getIndex(), pe4));

        // Upper neckline vector representing pennant resistance boundary
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(pennant1.getIndex(), pe1));
        necklines.add(new ChartPattern.Point(pennant4.getIndex(), projectedResistance));

        // Calculate pennant boundary convergence ratio
        double startSpread = Math.abs(pe1 - pe2);
        double endSpread = Math.abs(projectedResistance - projectedSupport);
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(pennant4.getIndex()).volume, 100.0, false, isBearish ? -0.02 : 0.02
        );

        String typeLabel = isBearish ? "BEARISH PENNANT" : "BULLISH PENNANT";
        String explanation = String.format("Local math scan verified a high-probability %s continuation pattern with flagpole height $%,.2f and converging boundaries.", 
                typeLabel, poleHeight);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, poleStart.getIndex(), pennant4.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching median pennant price coordinates
        double medianPrice = (projectedResistance + projectedSupport) / 2.0;
        double retestMargin = initialWedgeHeight(pe1, pe2) * 0.10;
        pattern.setRetestZoneTop(medianPrice + retestMargin);
        pattern.setRetestZoneBottom(medianPrice - retestMargin);

        return pattern;
    }

    private double initialWedgeHeight(double h, double l) {
        return Math.abs(h - l);
    }
}