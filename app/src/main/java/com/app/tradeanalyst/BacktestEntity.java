package com.tradeanalyst.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * ROOM DATABASE ENTITY: BacktestEntity
 * Represents a single backtested pattern outcome saved inside the local SQLite database.
 * Supports Phase 10 track-and-log specifications.
 */
@Entity(tableName = "backtests")
public class BacktestEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String symbol;
    public String patternType;
    public double entryPrice;
    public double stopLoss;
    public double targetPrice;
    public String result; // "WIN", "LOSS", or "ACTIVE"
    public double profitPercentage;
    public long timestamp;

    /**
     * Default constructor required by the Room persistence library [2].
     */
    public BacktestEntity() {}

    /**
     * Parameterized constructor used to build individual trade log files.
     *
     * @param symbol Asset identifier (e.g., BTC/USDT).
     * @param patternType Mapped mathematical pattern class.
     * @param entryPrice Exact breakthrough close entry value.
     * @param stopLoss Safety protection margin.
     * @param targetPrice Target profit boundary.
     * @param result Match outcome rating ("WIN", "LOSS", or "ACTIVE").
     * @param profitPercentage Calculated profit/loss rate of return percentage.
     * @param timestamp System millisecond epoch record.
     */
    public BacktestEntity(String symbol, String patternType, double entryPrice, double stopLoss, 
                          double targetPrice, String result, double profitPercentage, long timestamp) {
        this.symbol = symbol;
        this.patternType = patternType;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.targetPrice = targetPrice;
        this.result = result;
        this.profitPercentage = profitPercentage;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPatternType() {
        return patternType;
    }

    public void setPatternType(String patternType) {
        this.patternType = patternType;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public double getProfitPercentage() {
        return profitPercentage;
    }

    public void setProfitPercentage(double profitPercentage) {
        this.profitPercentage = profitPercentage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}