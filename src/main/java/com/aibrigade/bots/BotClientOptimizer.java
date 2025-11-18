package com.aibrigade.bots;

/**
 * BotClientOptimizer - Minimal client-side optimization
 *
 * Client only handles rendering - all logic is server-side.
 * This reduces client load significantly for 200+ bots.
 */
public class BotClientOptimizer {

    // Distance thresholds for rendering (client-side only)
    private static final double RENDER_DISTANCE = 128.0;
    private static final double LOD_DISTANCE = 64.0;

    /**
     * Check if bot should be rendered at all
     * Simple distance check - no complex calculations
     */
    public static boolean shouldRender(BotEntity bot, double distanceSqr) {
        return distanceSqr <= (RENDER_DISTANCE * RENDER_DISTANCE);
    }

    /**
     * Get simple LOD level for rendering
     * 0 = full detail, 1 = reduced detail
     */
    public static int getSimpleLOD(double distanceSqr) {
        if (distanceSqr > LOD_DISTANCE * LOD_DISTANCE) {
            return 1; // Distant - minimal rendering
        }
        return 0; // Close - full rendering
    }
}
