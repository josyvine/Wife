package com.tradeanalyst.app;

/**
 * SYSTEM LIFECYCLE ENUM: PatternState
 * Defines the formal validation states and color-coding metadata of detected chart patterns.
 * Fulfills all technical requirements from Phase 3 and Phase 6.
 */
public enum PatternState {
    
    /**
     * Pattern is mathematically emerging but breakout confirmation has not occurred.
     * Visual Indicator: Yellow (#F59E0B)
     */
    FORMING("STATE_FORMING", "#F59E0B"),

    /**
     * Pattern is verified; breakout close has occurred beyond the neckline/boundary.
     * Visual Indicator: Green (#10B981)
     */
    CONFIRMED("STATE_CONFIRMED", "#10B981"),

    /**
     * Price retraces after breakout to test key structural levels without invalidating.
     * Visual Indicator: Blue (#3B82F6)
     */
    RETESTING("STATE_RETESTING", "#3B82F6"),

    /**
     * Price has breached the critical protective threshold, invalidating the pattern setup.
     * Visual Indicator: Red (#EF4444)
     */
    INVALIDATED("STATE_INVALIDATED", "#EF4444"),

    /**
     * Price action successfully achieved the projected mathematical target objective.
     * Visual Indicator: Purple (#8B5CF6)
     */
    COMPLETED("STATE_COMPLETED", "#8B5CF6");

    private final String label;
    private final String colorHex;

    /**
     * Constructs a PatternState enum item.
     *
     * @param label The formal state string used by the platform.
     * @param colorHex The exact hexadecimal color representation for drawing overlays.
     */
    PatternState(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getColorHex() {
        return colorHex;
    }

    /**
     * Safely parses a raw string label into its corresponding PatternState enum constant.
     * Falls back to FORMING if the string is unrecognized or null.
     *
     * @param label The raw state label string.
     * @return The mapped PatternState enum constant.
     */
    public static PatternState fromLabel(String label) {
        if (label != null) {
            String trimmed = label.trim();
            for (PatternState state : values()) {
                if (state.getLabel().equalsIgnoreCase(trimmed) || state.name().equalsIgnoreCase(trimmed)) {
                    return state;
                }
            }
        }
        return FORMING; // Default fallback to prevent crash scenarios
    }
}