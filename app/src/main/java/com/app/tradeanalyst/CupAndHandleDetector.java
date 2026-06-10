package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: CupAndHandleDetector
 * Scans swing points to isolate Cup and Handle continuation patterns.
 * Enforces rim price symmetry, base rounding ratios, and handle retracement limits (Phase 2).
 */
public class CupAndHandleDetector extends BasePatternDetector {

    private static final double RIM_PRICE_TOLERANCE = 0.04; // Rim peaks must align within 4% price symmetry
    private static final double MAX_HANDLE_RETRACE_RATIO = 0.50; // Handle retrace cannot exceed 50% of the cup height
    private static final double MIN_HANDLE_WIDTH_RATIO = 0.10; // Handle width must be at least 10% of the cup width
    private static final double MAX_HANDLE_WIDTH_RATIO = 0.35; // Handle width must be under 35% of the cup width
    private static final int MIN_CUP_WIDTH = 15; // Minimum cup width in indices
    private static final int MAX_CUP_WIDTH = 150; // Maximum cup width in indices

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 4) {
            return detected;
        }

        int totalSwings = swings.size();

        // 1. Sliding window scanning across 4 consecutive swing points
        // Sequence: HIGH (Left Rim) -> LOW (Cup Bottom) -> HIGH (Right Rim) -> LOW (Handle Low)
        for (int i = 0; i <= totalSwings - 4; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);
            SwingPoint p4 = swings.get(i + 3);

            if (p1.getType() == SwingPoint.Type.HIGH &&
                p2.getType() == SwingPoint.Type.LOW &&
                p3.getType() == SwingPoint.Type.HIGH &&
                p4.getType() == SwingPoint.Type.LOW) {

                ChartPattern ch = verifyCupAndHandle(candles, p1, p2, p3, p4);
                if (ch != null) {
                    detected.add(ch);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 4 candidate swing points against Cup and Handle geometric rules.
     */
    private ChartPattern verifyCupAndHandle(
            List<Candlestick> candles, SwingPoint leftRim, SwingPoint cupBottom, SwingPoint rightRim, SwingPoint handleLow) {

        int cupWidth = rightRim.getIndex() - leftRim.getIndex();
        int handleWidth = handleLow.getIndex() - rightRim.getIndex();

        // Rule 1: Validate cup width boundaries
        if (cupWidth < MIN_CUP_WIDTH || cupWidth > MAX_CUP_WIDTH) {
            return null;
        }

        double lrPrice = leftRim.getPrice();
        double rrPrice = rightRim.getPrice();
        double cbPrice = cupBottom.getPrice();
        double hlPrice = handleLow.getPrice();

        // Rule 2: Rim Price Symmetry (Left and Right Rim peaks must align closely)
        if (!isWithinTolerance(lrPrice, rrPrice, RIM_PRICE_TOLERANCE)) {
            return null;
        }

        // Rule 3: Reject sharp V-shape bottoms (spend at least 3 candles near the absolute low price)
        double meanRimPrice = (lrPrice + rrPrice) / 2.0;
        double cupHeight = meanRimPrice - cbPrice;
        
        int lowRangeCount = 0;
        double lowBoundaryPrice = cbPrice + (cupHeight * 0.15); // Bottom 15% range of the cup height
        for (int i = leftRim.getIndex() + 1; i < rightRim.getIndex(); i++) {
            if (candles.get(i).low <= lowBoundaryPrice) {
                lowRangeCount++;
            }
        }
        
        // Require at least 3 candles within the bottom region to verify rounding U-shape
        if (lowRangeCount < 3) {
            return null;
        }

        // Rule 4: Handle Retracement Limit Check (Consolidation low must be above 50% of the cup)
        double maximumRetracementLevel = rrPrice - (cupHeight * MAX_HANDLE_RETRACE_RATIO);
        if (hlPrice < maximumRetracementLevel) {
            return null;
        }

        // Rule 5: Temporal Handle Width Proportion Check (Handle is 10% - 35% of the cup width)
        double widthRatio = (double) handleWidth / cupWidth;
        if (widthRatio < MIN_HANDLE_WIDTH_RATIO || widthRatio > MAX_HANDLE_WIDTH_RATIO) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        // Bullish breakout direction: target projects full cup height upwards from the Right Rim ceiling
        double targetPrice = rrPrice + cupHeight;
        double stopLossPrice = hlPrice * 0.985; // Stop Loss set 1.5% below the lowest point of the handle
        String bias = "BULLISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(leftRim.getIndex(), lrPrice));
        points.add(new ChartPattern.Point(cupBottom.getIndex(), cbPrice));
        points.add(new ChartPattern.Point(rightRim.getIndex(), rrPrice));
        points.add(new ChartPattern.Point(handleLow.getIndex(), hlPrice));

        // Upper neckline representing the horizontal rim resistance line
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(leftRim.getIndex(), meanRimPrice));
        necklines.add(new ChartPattern.Point(rightRim.getIndex(), meanRimPrice));

        // Calculate symmetry ratio of the rim peaks
        double convergenceRatio = Math.min(lrPrice, rrPrice) / Math.max(lrPrice, rrPrice);

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(handleLow.getIndex()).volume, 100.0, false, 0.02
        );

        String typeLabel = "CUP AND HANDLE";
        String explanation = String.format("Local math scan verified a high-probability U-shaped %s pattern with cup height $%,.2f and shallow handle consolidation.", 
                typeLabel, cupHeight);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, leftRim.getIndex(), handleLow.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching Right Rim breakout boundary price level
        double retestMargin = cupHeight * 0.15;
        pattern.setRetestZoneTop(rrPrice + retestMargin);
        pattern.setRetestZoneBottom(rrPrice - retestMargin);

        return pattern;
    }
}