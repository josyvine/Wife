package com.tradeanalyst.app;

/**
 * DATA MODEL: ChartPatternSummary
 * Represents the high-level synthesis and trade recommendations compiled by the AI Pattern Analyst.
 */
public class ChartPatternSummary {

    private String bestPattern;
    private double confidence;
    private String recommendation; // BUY, SELL, or HOLD

    public ChartPatternSummary() {}

    public ChartPatternSummary(String bestPattern, double confidence, String recommendation) {
        this.bestPattern = bestPattern;
        this.confidence = confidence;
        this.recommendation = recommendation;
    }

    public String getBestPattern() {
        return bestPattern;
    }

    public void setBestPattern(String bestPattern) {
        this.bestPattern = bestPattern;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
