package com.tradeanalyst.app;

/**
 * CORE DATA MODEL: SwingPoint
 * Represents a mathematically confirmed pivot high or pivot low.
 * Used exclusively by the SwingEngine and pattern detection suite to filter market noise.
 */
public class SwingPoint {

    /**
     * Enum defining the pivot classification of the swing point.
     */
    public enum Type {
        HIGH, // Represents a market peak
        LOW   // Represents a market trough
    }

    private int index;
    private long timestamp;
    private double price;
    private int strength;
    private Type type;

    /**
     * Default constructor required for JSON serialization and database mapping operations.
     */
    public SwingPoint() {}

    /**
     * Construct a mathematically verified swing point.
     *
     * @param index The absolute candle array index location.
     * @param timestamp The exact millisecond timestamp of the source candle.
     * @param price The peak/trough price coordinate.
     * @param strength The neighborhood confirmation depth weight.
     * @param type The high/low structural classification enum.
     */
    public SwingPoint(int index, long timestamp, double price, int strength, Type type) {
        this.index = index;
        this.timestamp = timestamp;
        this.price = price;
        this.strength = strength;
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SwingPoint{" +
                "index=" + index +
                ", timestamp=" + timestamp +
                ", price=" + price +
                ", strength=" + strength +
                ", type=" + type +
                '}';
    }
}