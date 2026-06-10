package com.tradeanalyst.app;

public class Candlestick {
    public double open;
    public double high;
    public double low;
    public double close;
    public long timestamp;
    public double volume; // Added to support Phase 8 Volume Confirmation mathematics

    // Preserved for backward compatibility to avoid breaking existing simulation/fallback logic
    public Candlestick(double open, double high, double low, double close, long timestamp) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.timestamp = timestamp;
        this.volume = 0.0; // Default fallback value
    }

    // Overloaded constructor to support native Binance volume ingestion
    public Candlestick(double open, double high, double low, double close, double volume, long timestamp) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
    }
}