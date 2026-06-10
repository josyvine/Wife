package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: InverseHeadAndShouldersDetector
 * Scans swing points to isolate Inverse Head and Shoulders patterns.
 * Enforces strict mathematical rules, symmetry tolerances, and measured targets (Phase 2).
 */
public class InverseHeadAndShouldersDetector extends BasePatternDetector {

    private static final double MIN_HEAD_CLEARANCE = 0.015; // Head must be at least 1.5% lower than shoulders
    private static final double SHOULDER_PRICE_TOLERANCE = 0.04; // Shoulder prices must be within 4% symmetry
    private static final double SHOULDER_WIDTH_TOLERANCE = 0.40; // Temporal width symmetry tolerance of 40%
    private static final int MIN_PATTERN_WIDTH = 12; // Minimum index width in candlesticks
    private static final int MAX_PATTERN_WIDTH = 150; // Maximum index width in candlesticks

    @Override
    public List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings) {
        List<ChartPattern> detected = new ArrayList<>();
        if (candles == null || swings == null || swings.size() < 5) {
            return detected;
        }

        int totalSwings = swings.size();

        // Sliding window scanning across 5 consecutive swing points
        for (int i = 0; i <= totalSwings - 5; i++) {
            SwingPoint p1 = swings.get(i);
            SwingPoint p2 = swings.get(i + 1);
            SwingPoint p3 = swings.get(i + 2);
            SwingPoint p4 = swings.get(i + 3);
            SwingPoint p5 = swings.get(i + 4);

            // Check Inverse Head and Shoulders configuration (LOW-HIGH-LOW-HIGH-LOW)
            if (p1.getType() == SwingPoint.Type.LOW &&
                p2.getType() == SwingPoint.Type.HIGH &&
                p3.getType() == SwingPoint.Type.LOW &&
                p4.getType() == SwingPoint.Type.HIGH &&
                p5.getType() == SwingPoint.Type.LOW) {

                ChartPattern ihs = verifyInverseHeadAndShoulders(candles, p1, p2, p3, p4, p5);
                if (ihs != null) {
                    detected.add(ihs);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 5 candidate swing points against Inverse H&S geometric rules.
     */
    private ChartPattern verifyInverseHeadAndShoulders(
            List<Candlestick> candles, SwingPoint leftShoulder, SwingPoint leftValley,
            SwingPoint head, SwingPoint rightValley, SwingPoint rightShoulder) {

        int totalWidth = rightShoulder.getIndex() - leftShoulder.getIndex();

        // Rule 1: Pattern width boundaries (too small or too large are rejected)
        if (totalWidth < MIN_PATTERN_WIDTH || totalWidth > MAX_PATTERN_WIDTH) {
            return null;
        }

        double lsPrice = leftShoulder.getPrice();
        double rsPrice = rightShoulder.getPrice();
        double headPrice = head.getPrice();

        // Rule 2: Inverse Head Clearance (Head must be sufficiently lower than shoulders)
        double requiredCeiling = Math.min(lsPrice, rsPrice) * (1.0 - MIN_HEAD_CLEARANCE);
        if (headPrice > requiredCeiling) {
            return null;
        }

        // Rule 3: Inverse Shoulder Price Symmetry
        if (!isWithinTolerance(lsPrice, rsPrice, SHOULDER_PRICE_TOLERANCE)) {
            return null;
        }

        // Rule 4: Temporal Width Symmetry (Width of Left Cup vs. Right Cup)
        double leftWidth = head.getIndex() - leftShoulder.getIndex();
        double rightWidth = rightShoulder.getIndex() - head.getIndex();
        if (!isSymmetrical(leftWidth, rightWidth, SHOULDER_WIDTH_TOLERANCE)) {
            return null;
        }

        // Rule 5: Neckline slope validation (Reject extremely steep lines)
        double necklineSlope = calculateSlope(leftValley.getIndex(), leftValley.getPrice(), rightValley.getIndex(), rightValley.getPrice());
        double necklineRad = Math.atan(Math.abs(necklineSlope));
        if (necklineRad > (Math.PI / 6.0)) { // Reject if slope exceeds 30 degrees (extremely asymmetric)
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        double necklineIntercept = calculateIntercept(leftValley.getIndex(), leftValley.getPrice(), necklineSlope);
        double headNecklineProjectionPrice = getLinePriceAt(head.getIndex(), necklineSlope, necklineIntercept);
        double depthHeight = Math.abs(headPrice - headNecklineProjectionPrice);

        // Bullish breakout direction: target projects upward from neckline
        double targetPrice = rightValley.getPrice() + depthHeight;
        double stopLossPrice = headPrice; // Stop Loss set below the lower Head trough
        String bias = "BULLISH";

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(leftShoulder.getIndex(), leftShoulder.getPrice()));
        points.add(new ChartPattern.Point(leftValley.getIndex(), leftValley.getPrice()));
        points.add(new ChartPattern.Point(head.getIndex(), head.getPrice()));
        points.add(new ChartPattern.Point(rightValley.getIndex(), rightValley.getPrice()));
        points.add(new ChartPattern.Point(rightShoulder.getIndex(), rightShoulder.getPrice()));

        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(leftValley.getIndex(), leftValley.getPrice()));
        necklines.add(new ChartPattern.Point(rightValley.getIndex(), rightValley.getPrice()));

        // Calculate mathematical confidence score locally (Phase 8)
        double score = ConfidenceCalculator.calculateHeadAndShoulders(
            lsPrice, rsPrice, leftWidth, rightWidth, necklineSlope,
            candles.get(rightValley.getIndex()).volume, 100.0, false, -0.02, true
        );

        String typeLabel = "INVERSE HEAD AND SHOULDERS";
        double priceSymmetryPercent = (1.0 - (Math.abs(lsPrice - rsPrice) / Math.max(lsPrice, rsPrice))) * 100.0;
        String explanation = String.format("Local math scan verified a symmetric Inverse Head and Shoulders pattern across lookback indices [%d to %d] with %.1f%% price symmetry confirmation.", 
                leftShoulder.getIndex(), rightShoulder.getIndex(), priceSymmetryPercent);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, leftShoulder.getIndex(), rightShoulder.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching right valley boundary coordinates
        double retestMargin = depthHeight * 0.15;
        pattern.setRetestZoneTop(rightValley.getPrice() + retestMargin);
        pattern.setRetestZoneBottom(rightValley.getPrice() - retestMargin);

        return pattern;
    }
}