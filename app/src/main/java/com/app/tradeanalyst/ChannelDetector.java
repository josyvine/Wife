package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * MATHEMATICAL DETECTOR: ChannelDetector
 * Scans swing points to isolate parallel Ascending and Descending Channels.
 * Enforces strict directional parallelism and measured channel height projections (Phase 2).
 */
public class ChannelDetector extends BasePatternDetector {

    private static final double MIN_SLOPE_THRESHOLD = 0.005; // Slopes must be non-flat (at least 0.5% drift)
    private static final double PARALLELISM_TOLERANCE = 0.05; // Maximum allowed slope delta for boundaries
    private static final int MIN_CHANNEL_WIDTH = 10; // Minimum index width in candlesticks
    private static final int MAX_CHANNEL_WIDTH = 120; // Maximum index width in candlesticks

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

                ChartPattern ch = verifyChannel(candles, p1, p2, p3, p4);
                if (ch != null) {
                    detected.add(ch);
                }
            }
        }

        return detected;
    }

    /**
     * Mathematically evaluates the 4 candidate swing points against parallel Channel geometric rules.
     */
    private ChartPattern verifyChannel(
            List<Candlestick> candles, SwingPoint peak1, SwingPoint valley1, SwingPoint peak2, SwingPoint valley2) {

        int totalWidth = valley2.getIndex() - peak1.getIndex();

        // Rule 1: Validate index width boundaries
        if (totalWidth < MIN_CHANNEL_WIDTH || totalWidth > MAX_CHANNEL_WIDTH) {
            return null;
        }

        double p1Price = peak1.getPrice();
        double p2Price = peak2.getPrice();
        double v1Price = valley1.getPrice();
        double v2Price = valley2.getPrice();

        // Rule 2: Slope calculations
        double resistanceSlope = calculateSlope(peak1.getIndex(), p1Price, peak2.getIndex(), p2Price);
        double supportSlope = calculateSlope(valley1.getIndex(), v1Price, valley2.getIndex(), v2Price);

        // Rule 3: Directional Sign check (Slopes must be in the same direction, both ascending or both descending)
        if (Math.signum(resistanceSlope) != Math.signum(supportSlope)) {
            return null;
        }

        // Rule 4: Non-flat check (Reject standard flat Rectangles)
        if (Math.abs(resistanceSlope) < MIN_SLOPE_THRESHOLD || Math.abs(supportSlope) < MIN_SLOPE_THRESHOLD) {
            return null;
        }

        // Rule 5: Parallel Boundary Verification (Slopes must align within tolerance)
        if (Math.abs(resistanceSlope - supportSlope) > PARALLELISM_TOLERANCE) {
            return null;
        }

        // 2. CONSTRUCT MEASURED TARGET AND PROTECTION BOUNDS
        boolean isAscending = resistanceSlope > 0;
        String bias = isAscending ? "BEARISH" : "BULLISH"; // Ascending Channels break bearish, Descending break bullish

        double resistanceIntercept = calculateIntercept(peak1.getIndex(), p1Price, resistanceSlope);
        double supportIntercept = calculateIntercept(valley1.getIndex(), v1Price, supportSlope);

        double projectedResistance = getLinePriceAt(valley2.getIndex(), resistanceSlope, resistanceIntercept);
        double projectedSupport = getLinePriceAt(valley2.getIndex(), supportSlope, supportIntercept);
        double channelHeight = Math.abs(projectedResistance - projectedSupport);

        double targetPrice;
        double stopLossPrice;

        if ("BULLISH".equals(bias)) {
            // Descending channel bullish breakout above upper resistance
            targetPrice = projectedResistance + channelHeight;
            stopLossPrice = projectedResistance - (channelHeight * 0.50); // Midpoint stop
        } else {
            // Ascending channel bearish breakdown below lower support
            targetPrice = projectedSupport - channelHeight;
            stopLossPrice = projectedSupport + (channelHeight * 0.50); // Midpoint stop
        }

        // 3. COMPILE COORDINATE POINTS FOR SNR DRAWING AND SCORING (Phase 6)
        List<ChartPattern.Point> points = new ArrayList<>();
        points.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        points.add(new ChartPattern.Point(valley1.getIndex(), v1Price));
        points.add(new ChartPattern.Point(peak2.getIndex(), p2Price));
        points.add(new ChartPattern.Point(valley2.getIndex(), v2Price));

        // Upper neckline vector representing channel resistance boundary
        List<ChartPattern.Point> necklines = new ArrayList<>();
        necklines.add(new ChartPattern.Point(peak1.getIndex(), p1Price));
        necklines.add(new ChartPattern.Point(valley2.getIndex(), projectedResistance));

        // Calculate channel symmetry convergence ratio (perfect parallel channel approaches 1.0)
        double startSpread = Math.abs(p1Price - v1Price);
        double endSpread = Math.abs(projectedResistance - projectedSupport);
        double convergenceRatio = startSpread > 0 ? (endSpread / startSpread) : 1.0;

        // Calculate mathematical confidence score locally (Phase 8)
        double trendSlope = (resistanceSlope + supportSlope) / 2.0;
        double score = ConfidenceCalculator.calculateBoundaryPattern(
            2, 2, convergenceRatio,
            candles.get(valley2.getIndex()).volume, 100.0, false, trendSlope
        );

        String typeLabel = isAscending ? "ASCENDING CHANNEL" : "DESCENDING CHANNEL";
        String explanation = String.format("Local math scan verified a parallel %s corridor spanning index [%d to %d] with channel depth $%,.2f.", 
                typeLabel, peak1.getIndex(), valley2.getIndex(), channelHeight);

        ChartPattern pattern = new ChartPattern(
            typeLabel, score, bias, peak1.getIndex(), valley2.getIndex(),
            points, targetPrice, stopLossPrice, explanation
        );

        // Load metadata parameters to support exact snapping rendering
        pattern.setNecklinePoints(necklines);
        pattern.setState(PatternState.FORMING.getLabel());

        // Set retest zones matching breakout boundary price levels
        double retestMargin = channelHeight * 0.15;
        if ("BULLISH".equals(bias)) {
            pattern.setRetestZoneTop(projectedResistance + retestMargin);
            pattern.setRetestZoneBottom(projectedResistance - retestMargin);
        } else {
            pattern.setRetestZoneTop(projectedSupport + retestMargin);
            pattern.setRetestZoneBottom(projectedSupport - retestMargin);
        }

        return pattern;
    }
}