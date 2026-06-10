package com.tradeanalyst.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "paper_trades")
public class PaperTradeTransaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String symbol;
    public String action; // BUY or SELL
    public double confidence; // e.g. 85.0
    public double entryPrice;
    public long timestamp;

    public PaperTradeTransaction() {}

    public PaperTradeTransaction(String symbol, String action, double confidence, double entryPrice, long timestamp) {
        this.symbol = symbol;
        this.action = action;
        this.confidence = confidence;
        this.entryPrice = entryPrice;
        this.timestamp = timestamp;
    }
}
