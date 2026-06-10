package com.tradeanalyst.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "price_alerts")
public class PriceAlertEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String symbol;
    public double targetPrice;
    public boolean isAbove; // true if alert triggers when price goes ABOVE, false for BELOW
    public boolean isActive;

    public PriceAlertEntity() {}

    public PriceAlertEntity(String symbol, double targetPrice, boolean isAbove, boolean isActive) {
        this.symbol = symbol;
        this.targetPrice = targetPrice;
        this.isAbove = isAbove;
        this.isActive = isActive;
    }
}
