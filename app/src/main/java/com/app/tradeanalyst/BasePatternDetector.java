package com.tradeanalyst.app;

import java.util.List;

/**
 * MATHEMATICAL BASE DETECTOR: BasePatternDetector
 * Provides common geometric, vector, and tolerance calculations for the pattern scanning suite.
 * Enforces a strict contract for all concrete mathematical pattern detectors.
 */
public abstract class BasePatternDetector {

    /**
     * Executes the mathematical pattern scan on the provided swing points.
     * Must be implemented by all 19 concrete pattern detectors.
     *
     * @param candles The historical candlestick series.
     * @param swings The chronologically sorted list of ranked swing points.
     * @return List of identified mathematical chart pattern candidates.
     */
    public abstract List<ChartPattern> detect(List<Candlestick> candles, List<SwingPoint> swings);

    /**
     * Calculates the slope (m) of a straight line connecting two coordinates.
     */
    protected double calculateSlope(double x1, double y1, double x2, double y2) {
        if (x2 - x1 == 0) {
            return 0.0; // Prevent division-by-zero errors on adjacent or vertical points
        }
        return (y2 - y1) / (x2 - x1);
    }

    /**
     * Calculates the intercept (c) of a straight line passing through a point with a given slope.
     */
    protected double calculateIntercept(double x1, double y1, double slope) {
        return y1 - (slope * x1);
    }

    /**
     * Calculates the projected price (y) at a specific candle index along a defined line vector.
     */
    protected double getLinePriceAt(double index, double slope, double intercept) {
        return (slope * index) + intercept;
    }

    /**
     * Checks if a target price is within a specific percentage tolerance of a reference price.
     *
     * @param target The price value being evaluated.
     * @param reference The reference standard price.
     * @param tolerancePercentage The maximum allowed percentage variance (e.g. 0.02 represents 2%).
     * @return True if the target price falls within the acceptable margin.
     */
    protected boolean isWithinTolerance(double target, double reference, double tolerancePercentage) {
        if (reference == 0) {
            return false;
        }
        double difference = Math.abs(target - reference);
        double maxVal = Math.max(target, reference);
        return (difference / maxVal) <= tolerancePercentage;
    }

    /**
     * Checks if a swing point is resting on a specified trendline vector within tolerance limits.
     *
     * @param swing The target swing point.
     * @param slope The slope of the reference trendline.
     * @param intercept The vertical intercept of the reference trendline.
     * @param tolerancePercentage The margin of variance allowed.
     * @return True if the swing point is on the trendline within tolerance.
     */
    protected boolean isPointOnTrendline(SwingPoint swing, double slope, double intercept, double tolerancePercentage) {
        double expectedPrice = getLinePriceAt(swing.getIndex(), slope, intercept);
        return isWithinTolerance(swing.getPrice(), expectedPrice, tolerancePercentage);
    }

    /**
     * Mathematically measures the symmetry of two distinct intervals.
     * Used to evaluate shoulder proportions (left vs. right width) or double-top separation.
     *
     * @param len1 The length of the first segment.
     * @param len2 The length of the second segment.
     * @param tolerancePercentage The symmetry deviation threshold allowed.
     * @return True if the segments are symmetrical within the given bounds.
     */
    protected boolean isSymmetrical(double len1, double len2, double tolerancePercentage) {
        if (len1 == 0 || len2 == 0) {
            return false;
        }
        double ratioDiff = Math.abs(len1 - len2);
        double maxLen = Math.max(len1, len2);
        return (ratioDiff / maxLen) <= tolerancePercentage;
    }

    /**
     * Calculates whether two trendlines are converging over a specified distance.
     * Converging trendlines must have opposing slope values or different slopes approaching a point.
     *
     * @param slope1 Slope of the first trendline.
     * @param slope2 Slope of the second trendline.
     * @param intervalLength The distance over which convergence is being analyzed.
     * @return True if the distance between the lines is narrower at the end of the interval than at the start.
     */
    protected boolean areLinesConverging(double slope1, double slope2, int intervalLength) {
        // Calculate the initial height delta at arbitrary origin 0
        double initialDelta = Math.abs(getLinePriceAt(0, slope1, 0) - getLinePriceAt(0, slope2, 0));
        // Calculate the projected delta at the end of the interval
        double endingDelta = Math.abs(getLinePriceAt(intervalLength, slope1, 0) - getLinePriceAt(intervalLength, slope2, 0));
        return endingDelta < initialDelta;
    }

    /**
     * Calculates the vertical distance between a swing point and a trendline.
     */
    protected double calculateVerticalDistance(SwingPoint swing, double slope, double intercept) {
        double expectedPrice = getLinePriceAt(swing.getIndex(), slope, intercept);
        return Math.abs(swing.getPrice() - expectedPrice);
    }
}