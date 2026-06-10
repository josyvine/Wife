package com.tradeanalyst.app;

import java.util.List;

/**
 * CORE CONFIDENCE ENGINE: ConfidenceCalculator
 * Computes deterministic, mathematical scorecards to establish explainable confidence ratings.
 * Replaces generative AI estimations in accordance with Phase 8 specifications.
 */
public class ConfidenceCalculator {

    /**
     * Standardizes the final scoring bounds safely between 0 and 100.
     */
    private static double clamp(double score) {
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Calculates the explainable scorecard for Head and Shoulders (and Inverse H&S) patterns.
     */
    public static double calculateHeadAndShoulders(
            double leftShoulderPrice, double rightShoulderPrice,
            double leftShoulderWidth, double rightShoulderWidth,
            double necklineSlope, double breakoutVolume, double averageVolume,
            boolean isRetested, double trendSlope, boolean isInverse) {

        double shoulderSymmetryScore = 0.0;
        double necklineQualityScore = 0.0;
        double breakoutQualityScore = 0.0;
        double volumeConfirmationScore = 0.0;
        double retestQualityScore = 0.0;
        double trendContextScore = 0.0;

        // 1. Shoulder Symmetry (25% Max)
        double priceDiff = Math.abs(leftShoulderPrice - rightShoulderPrice);
        double maxPrice = Math.max(leftShoulderPrice, rightShoulderPrice);
        double priceSymmetry = maxPrice > 0 ? (1.0 - (priceDiff / maxPrice)) : 0.0;

        double widthDiff = Math.abs(leftShoulderWidth - rightShoulderWidth);
        double maxWidth = Math.max(leftShoulderWidth, rightShoulderWidth);
        double widthSymmetry = maxWidth > 0 ? (1.0 - (widthDiff / maxWidth)) : 0.0;

        // Combine price and temporal width symmetry
        shoulderSymmetryScore = ((priceSymmetry * 0.6) + (widthSymmetry * 0.4)) * 25.0;

        // 2. Neckline Quality (20% Max)
        // High quality necklines approach flat parameters (0.0 slope)
        double slopeRad = Math.atan(Math.abs(necklineSlope));
        double flatness = Math.max(0.0, 1.0 - (slopeRad / (Math.PI / 4.0))); // Max 45 degrees deviation
        necklineQualityScore = flatness * 20.0;

        // 3. Breakout Quality (20% Max)
        // Set standard confirmation baselines
        breakoutQualityScore = 20.0; 

        // 4. Volume Confirmation (15% Max)
        if (averageVolume > 0) {
            double volumeRatio = breakoutVolume / averageVolume;
            if (volumeRatio >= 1.5) {
                volumeConfirmationScore = 15.0; // High volume breakout
            } else if (volumeRatio >= 1.0) {
                volumeConfirmationScore = 10.0; // Standard volume
            } else {
                volumeConfirmationScore = 5.0;  // Sub-average volume
            }
        }

        // 5. Retest Quality (10% Max)
        if (isRetested) {
            retestQualityScore = 10.0; // Full confirmation retest complete
        }

        // 6. Trend Context (10% Max)
        // Direct H&S requires preceding bullish trend slope, Inverse requires bearish
        if (isInverse) {
            trendContextScore = trendSlope < 0 ? 10.0 : 2.0;
        } else {
            trendContextScore = trendSlope > 0 ? 10.0 : 2.0;
        }

        return clamp(shoulderSymmetryScore + necklineQualityScore + breakoutQualityScore +
                     volumeConfirmationScore + retestQualityScore + trendContextScore);
    }

    /**
     * Calculates the explainable scorecard for Double/Triple Tops and Bottoms.
     */
    public static double calculateDoubleTripleTopBottom(
            double peak1, double peak2, double peak3, int peakCount,
            double valleyPrice, double breakoutVolume, double averageVolume,
            boolean isRetested, double trendSlope, boolean isBottom) {

        double peakSymmetryScore = 0.0;
        double separatorQualityScore = 0.0;
        double breakoutQualityScore = 0.0;
        double volumeConfirmationScore = 0.0;
        double retestQualityScore = 0.0;
        double trendContextScore = 0.0;

        // 1. Peak Symmetry (25% Max)
        double priceDiff = Math.abs(peak1 - peak2);
        double maxPrice = Math.max(peak1, peak2);
        double baseSymmetry = maxPrice > 0 ? (1.0 - (priceDiff / maxPrice)) : 0.0;

        if (peakCount == 3 && peak3 > 0) {
            double diff3_1 = Math.abs(peak1 - peak3);
            double maxPrice3 = Math.max(peak1, peak3);
            double sym3 = maxPrice3 > 0 ? (1.0 - (diff3_1 / maxPrice3)) : 0.0;
            peakSymmetryScore = ((baseSymmetry + sym3) / 2.0) * 25.0;
        } else {
            peakSymmetryScore = baseSymmetry * 25.0;
        }

        // 2. Separator Quality (20% Max)
        // Evaluates if valley price provides sufficient depth separation from the peak boundaries
        double depth = Math.abs(peak1 - valleyPrice);
        double depthRatio = depth / Math.max(peak1, valleyPrice);
        if (depthRatio >= 0.03 && depthRatio <= 0.25) {
            separatorQualityScore = 20.0; // Optimal 3% - 25% depth separation range
        } else if (depthRatio > 0) {
            separatorQualityScore = 10.0; // Marginal depth
        }

        // 3. Breakout Quality (20% Max)
        breakoutQualityScore = 20.0;

        // 4. Volume Confirmation (15% Max)
        if (averageVolume > 0) {
            double ratio = breakoutVolume / averageVolume;
            volumeConfirmationScore = ratio >= 1.5 ? 15.0 : (ratio >= 1.0 ? 10.0 : 5.0);
        }

        // 5. Retest Quality (10% Max)
        if (isRetested) {
            retestQualityScore = 10.0;
        }

        // 6. Trend Context (10% Max)
        if (isBottom) {
            trendContextScore = trendSlope < 0 ? 10.0 : 3.0; // Expects preceding downward trend
        } else {
            trendContextScore = trendSlope > 0 ? 10.0 : 3.0; // Expects preceding upward trend
        }

        return clamp(peakSymmetryScore + separatorQualityScore + breakoutQualityScore +
                     volumeConfirmationScore + retestQualityScore + trendContextScore);
    }

    /**
     * Calculates the explainable scorecard for Triangle, Wedge, and Channel configurations.
     */
    public static double calculateBoundaryPattern(
            int supportTouches, int resistanceTouches, double convergenceRatio,
            double breakoutVolume, double averageVolume, boolean isRetested, double trendSlope) {

        double touchPointsScore = 0.0;
        double boundarySymmetryScore = 0.0;
        double breakoutQualityScore = 0.0;
        double volumeConfirmationScore = 0.0;
        double retestQualityScore = 0.0;
        double trendContextScore = 0.0;

        // 1. Touch Points (25% Max)
        // Confirmed mathematical trendlines require multiple verification touches
        int totalTouches = supportTouches + resistanceTouches;
        if (totalTouches >= 6) {
            touchPointsScore = 25.0;
        } else if (totalTouches >= 4) {
            touchPointsScore = 18.0;
        } else {
            touchPointsScore = 10.0;
        }

        // 2. Boundary Convergence Symmetry (20% Max)
        if (convergenceRatio > 0 && convergenceRatio <= 1.0) {
            boundarySymmetryScore = (1.0 - convergenceRatio) * 20.0;
        } else {
            boundarySymmetryScore = 10.0;
        }

        // 3. Breakout Quality (20% Max)
        breakoutQualityScore = 20.0;

        // 4. Volume Confirmation (15% Max)
        if (averageVolume > 0) {
            double ratio = breakoutVolume / averageVolume;
            volumeConfirmationScore = ratio >= 1.5 ? 15.0 : (ratio >= 1.0 ? 10.0 : 5.0);
        }

        // 5. Retest Quality (10% Max)
        if (isRetested) {
            retestQualityScore = 10.0;
        }

        // 6. Trend Context (10% Max)
        trendContextScore = Math.abs(trendSlope) > 0.001 ? 10.0 : 5.0;

        return clamp(touchPointsScore + boundarySymmetryScore + breakoutQualityScore +
                     volumeConfirmationScore + retestQualityScore + trendContextScore);
    }

    /**
     * Fallback generic scorecard calculation to protect arbitrary patterns from scoring exceptions.
     */
    public static double calculateGeneric(double baseSymmetry, double volumeRatio, boolean isRetested, double trendSlope) {
        double symmetry = baseSymmetry * 30.0; // 30% Max
        double volume = volumeRatio >= 1.4 ? 20.0 : (volumeRatio >= 1.0 ? 15.0 : 5.0); // 20% Max
        double retest = isRetested ? 20.0 : 0.0; // 20% Max
        double trend = Math.abs(trendSlope) > 0.001 ? 10.0 : 5.0; // 10% Max
        double breakout = 20.0; // 20% Max

        return clamp(symmetry + volume + retest + trend + breakout);
    }
}