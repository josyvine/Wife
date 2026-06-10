package com.tradeanalyst.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * ROOM DATABASE ENTITY: PatternAnalysisRequest
 * Stores and tracks the parameters of a request sent to the AI Pattern Analyst.
 */
@Entity(tableName = "pattern_analysis_requests")
public class PatternAnalysisRequest {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String timeframe;
    public int lookbackCandleCount;
    public String symbol;
    public long timestamp;

    public PatternAnalysisRequest() {}

    public PatternAnalysisRequest(String timeframe, int lookbackCandleCount, String symbol, long timestamp) {
        this.timeframe = timeframe;
        this.lookbackCandleCount = lookbackCandleCount;
        this.symbol = symbol;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public int getLookbackCandleCount() {
        return lookbackCandleCount;
    }

    public void setLookbackCandleCount(int lookbackCandleCount) {
        this.lookbackCandleCount = lookbackCandleCount;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
