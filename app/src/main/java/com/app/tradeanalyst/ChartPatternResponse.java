package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * DATA MODEL: ChartPatternResponse
 * Root container class that maps the JSON output structure returned by the AI Pattern Analyst.
 */
public class ChartPatternResponse {

    private List<ChartPattern> patterns = new ArrayList<>();
    private ChartPatternSummary summary;
    private int lookback = 10;

    // =========================================================================
    // PHASE 7 VALIDATION METADATA FIELDS (Option B)
    // =========================================================================
    private boolean isAiValidated = false; // Flag representing Gemini validation
    private String validationExplanation;  // Validator explanation text
    private double aiConfidenceAdjustment = 0.0; // AI-driven score adjustment (Max +-5%)

    public ChartPatternResponse() {}

    public ChartPatternResponse(List<ChartPattern> patterns, ChartPatternSummary summary) {
        this.patterns = patterns;
        this.summary = summary;
    }

    public int getLookback() {
        return lookback;
    }

    public void setLookback(int lookback) {
        this.lookback = lookback;
    }

    public List<ChartPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<ChartPattern> patterns) {
        this.patterns = patterns;
    }

    public ChartPatternSummary getSummary() {
        return summary;
    }

    public void setSummary(ChartPatternSummary summary) {
        this.summary = summary;
    }

    // =========================================================================
    // GETTERS AND SETTERS FOR NATIVE AI VALIDATION METADATA
    // =========================================================================

    public boolean isAiValidated() {
        return isAiValidated;
    }

    public void setAiValidated(boolean aiValidated) {
        this.isAiValidated = aiValidated;
    }

    public String getValidationExplanation() {
        return validationExplanation;
    }

    public void setValidationExplanation(String validationExplanation) {
        this.validationExplanation = validationExplanation;
    }

    public double getAiConfidenceAdjustment() {
        return aiConfidenceAdjustment;
    }

    public void setAiConfidenceAdjustment(double aiConfidenceAdjustment) {
        this.aiConfidenceAdjustment = aiConfidenceAdjustment;
    }
}