package com.tradeanalyst.app;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * ROOM DATABASE ENTITY: BacktestStatsEntity
 * Represents the aggregate performance statistics of a specific mathematical pattern type.
 * Supports Phase 10 live statistics logging and historical probability adjustments.
 */
@Entity(tableName = "backtest_stats")
public class BacktestStatsEntity {

    @PrimaryKey
    @NonNull
    public String patternType;

    public int totalDetected;
    public int wins;
    public int losses;
    public double successRate;
    public double averageProfit;

    /**
     * Default constructor required by the Room persistence library [2].
     */
    public BacktestStatsEntity() {
        this.patternType = "";
    }

    /**
     * Parameterized constructor used to build structural performance records.
     *
     * @param patternType Mapped mathematical pattern class.
     * @param totalDetected Total tracked instances.
     * @param wins Count of target objectives achieved.
     * @param losses Count of protection limits hit.
     * @param successRate Mathematical win-rate percentage.
     * @param averageProfit Mean percentage profit achieved.
     */
    public BacktestStatsEntity(@NonNull String patternType, int totalDetected, int wins, int losses, 
                               double successRate, double averageProfit) {
        this.patternType = patternType;
        this.totalDetected = totalDetected;
        this.wins = wins;
        this.losses = losses;
        this.successRate = successRate;
        this.averageProfit = averageProfit;
    }

    @NonNull
    public String getPatternType() {
        return patternType;
    }

    public void setPatternType(@NonNull String patternType) {
        this.patternType = patternType;
    }

    public int getTotalDetected() {
        return totalDetected;
    }

    public void setTotalDetected(int totalDetected) {
        this.totalDetected = totalDetected;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getAverageProfit() {
        return averageProfit;
    }

    public void setAverageProfit(double averageProfit) {
        this.averageProfit = averageProfit;
    }
}