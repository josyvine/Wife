package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * DATA MODEL: ChartPattern
 * Represents an individual chart pattern identified by the AI Pattern Analyst.
 */
public class ChartPattern {

    private String type;
    private double confidence;
    private String bias; // BULLISH, BEARISH, or NEUTRAL
    private int startIndex;
    private int endIndex;
    private List<Point> points = new ArrayList<>();
    private double target;
    private double stopLoss;
    private String explanation;

    // =========================================================================
    // ADVANCED MATHEMATICAL GEOMETRY & lifecycle VARIABLES (Option B)
    // =========================================================================
    private String state = "STATE_FORMING"; // Phase 3 Lifecycle State
    private List<Point> necklinePoints = new ArrayList<>(); // Phase 6 Snapped Neckline
    
    // Phase 4 Breakout Confirmation Metadata
    private int breakoutIndex = -1;
    private double breakoutPrice = -1.0;
    private long breakoutTimestamp = 0;

    // Phase 6 Retest Zone Bounding Coordinates
    private double retestZoneTop = -1.0;
    private double retestZoneBottom = -1.0;

    // Phase 6 Projected Trajectory Area Indicators
    private int projectionStartIndex = -1;
    private int projectionEndIndex = -1;
    private double projectionTargetPrice = -1.0;

    /**
     * Nested class representing a key mathematical pivot point in the pattern.
     */
    public static class Point {
        private int index;
        private double price;

        public Point() {}

        public Point(int index, double price) {
            this.index = index;
            this.price = price;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    public ChartPattern() {}

    public ChartPattern(String type, double confidence, String bias, int startIndex, int endIndex, 
                        List<Point> points, double target, double stopLoss, String explanation) {
        this.type = type;
        this.confidence = confidence;
        this.bias = bias;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.points = points;
        this.target = target;
        this.stopLoss = stopLoss;
        this.explanation = explanation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getBias() {
        return bias;
    }

    public void setBias(String bias) {
        this.bias = bias;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public double getTarget() {
        return target;
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    // =========================================================================
    // GETTERS AND SETTERS FOR EXPANDED LIFECYCLE AND snapped OVERLAYS
    // =========================================================================

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<Point> getNecklinePoints() {
        return necklinePoints;
    }

    public void setNecklinePoints(List<Point> necklinePoints) {
        this.necklinePoints = necklinePoints;
    }

    public int getBreakoutIndex() {
        return breakoutIndex;
    }

    public void setBreakoutIndex(int breakoutIndex) {
        this.breakoutIndex = breakoutIndex;
    }

    public double getBreakoutPrice() {
        return breakoutPrice;
    }

    public void setBreakoutPrice(double breakoutPrice) {
        this.breakoutPrice = breakoutPrice;
    }

    public long getBreakoutTimestamp() {
        return breakoutTimestamp;
    }

    public void setBreakoutTimestamp(long breakoutTimestamp) {
        this.breakoutTimestamp = breakoutTimestamp;
    }

    public double getRetestZoneTop() {
        return retestZoneTop;
    }

    public void setRetestZoneTop(double retestZoneTop) {
        this.retestZoneTop = retestZoneTop;
    }

    public double getRetestZoneBottom() {
        return retestZoneBottom;
    }

    public void setRetestZoneBottom(double retestZoneBottom) {
        this.retestZoneBottom = retestZoneBottom;
    }

    public int getProjectionStartIndex() {
        return projectionStartIndex;
    }

    public void setProjectionStartIndex(int projectionStartIndex) {
        this.projectionStartIndex = projectionStartIndex;
    }

    public int getProjectionEndIndex() {
        return projectionEndIndex;
    }

    public void setProjectionEndIndex(int projectionEndIndex) {
        this.projectionEndIndex = projectionEndIndex;
    }

    public double getProjectionTargetPrice() {
        return projectionTargetPrice;
    }

    public void setProjectionTargetPrice(double projectionTargetPrice) {
        this.projectionTargetPrice = projectionTargetPrice;
    }
}