package com.aibrigade.bots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

/**
 * BotPerformanceOptimizer - Optimizes bot performance for large numbers of bots
 *
 * Features:
 * - Variable tick rates based on distance from players
 * - Level of Detail (LOD) system
 * - Reduced AI updates for distant bots
 * - Optimized network synchronization
 *
 * Performance goals:
 * - Support 200+ bots without client lag
 * - Maintain smooth gameplay for nearby bots
 * - Reduce unnecessary computations
 */
public class BotPerformanceOptimizer {

    // Distance thresholds (in blocks)
    private static final double CLOSE_DISTANCE = 16.0; // Full updates
    private static final double MEDIUM_DISTANCE = 32.0; // Reduced updates
    private static final double FAR_DISTANCE = 64.0; // Minimal updates

    // Tick intervals for different distances
    private static final int CLOSE_TICK_INTERVAL = 1; // Every tick
    private static final int MEDIUM_TICK_INTERVAL = 4; // Every 4 ticks (0.2s)
    private static final int FAR_TICK_INTERVAL = 20; // Every second
    private static final int VERY_FAR_TICK_INTERVAL = 40; // Every 2 seconds

    /**
     * Determine if a bot should update AI this tick based on distance to nearest player
     *
     * @param bot The bot entity
     * @param tickCount Current server tick count
     * @return true if the bot should update AI this tick
     */
    public static boolean shouldUpdateAI(BotEntity bot, int tickCount) {
        // Static bots never need AI updates
        if (bot.isStatic()) {
            return false;
        }

        // Bots following a leader should update more frequently (every 2 ticks for smooth movement)
        if (bot.isFollowingLeader() && bot.getLeaderId() != null) {
            return tickCount % 2 == 0;
        }

        // Get nearest player distance
        double nearestPlayerDistance = getNearestPlayerDistance(bot);

        // Determine tick interval based on distance
        int tickInterval = getTickInterval(nearestPlayerDistance);

        // Use bot's UUID hashCode to stagger updates across different bots
        // This prevents all bots from updating on the same tick
        int offset = Math.abs(bot.getUUID().hashCode() % tickInterval);

        return (tickCount + offset) % tickInterval == 0;
    }

    /**
     * Determine if pathfinding should be enabled for this bot
     * Disabled for static bots and very distant bots (unless following a leader)
     */
    public static boolean shouldEnablePathfinding(BotEntity bot) {
        if (bot.isStatic()) {
            return false;
        }

        // ALWAYS enable pathfinding for bots that are following a leader
        if (bot.isFollowingLeader() && bot.getLeaderId() != null) {
            return true;
        }

        double nearestPlayerDistance = getNearestPlayerDistance(bot);

        // Disable pathfinding for very distant bots (only if not following)
        return nearestPlayerDistance < FAR_DISTANCE;
    }

    /**
     * Determine if bot should synchronize data to clients this tick
     * Reduces network traffic for distant bots
     */
    public static boolean shouldSyncToClient(BotEntity bot, int tickCount) {
        double nearestPlayerDistance = getNearestPlayerDistance(bot);

        if (nearestPlayerDistance < CLOSE_DISTANCE) {
            return true; // Always sync nearby bots
        } else if (nearestPlayerDistance < MEDIUM_DISTANCE) {
            return tickCount % 2 == 0; // Sync every other tick
        } else if (nearestPlayerDistance < FAR_DISTANCE) {
            return tickCount % 10 == 0; // Sync every 0.5s
        } else {
            return tickCount % 40 == 0; // Sync every 2s
        }
    }

    /**
     * Get the LOD (Level of Detail) level for rendering
     * 0 = Full detail, 1 = Medium, 2 = Low, 3 = Minimal
     */
    public static int getLODLevel(BotEntity bot) {
        double nearestPlayerDistance = getNearestPlayerDistance(bot);

        if (nearestPlayerDistance < CLOSE_DISTANCE) {
            return 0; // Full detail
        } else if (nearestPlayerDistance < MEDIUM_DISTANCE) {
            return 1; // Medium detail
        } else if (nearestPlayerDistance < FAR_DISTANCE) {
            return 2; // Low detail
        } else {
            return 3; // Minimal detail
        }
    }

    /**
     * Determine tick interval based on distance
     */
    private static int getTickInterval(double distance) {
        if (distance < CLOSE_DISTANCE) {
            return CLOSE_TICK_INTERVAL;
        } else if (distance < MEDIUM_DISTANCE) {
            return MEDIUM_TICK_INTERVAL;
        } else if (distance < FAR_DISTANCE) {
            return FAR_TICK_INTERVAL;
        } else {
            return VERY_FAR_TICK_INTERVAL;
        }
    }

    /**
     * Get distance to nearest player
     */
    private static double getNearestPlayerDistance(BotEntity bot) {
        if (!(bot.level() instanceof ServerLevel serverLevel)) {
            return Double.MAX_VALUE;
        }

        List<ServerPlayer> players = serverLevel.players();

        if (players.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double minDistance = Double.MAX_VALUE;

        for (ServerPlayer player : players) {
            double distance = bot.distanceToSqr(player);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        return Math.sqrt(minDistance);
    }

    /**
     * Check if bot animations should be updated
     * Reduces animation updates for distant bots
     */
    public static boolean shouldUpdateAnimations(BotEntity bot, int tickCount) {
        double nearestPlayerDistance = getNearestPlayerDistance(bot);

        if (nearestPlayerDistance < CLOSE_DISTANCE) {
            return true; // Always update nearby
        } else if (nearestPlayerDistance < MEDIUM_DISTANCE) {
            return tickCount % 2 == 0;
        } else {
            return tickCount % 4 == 0;
        }
    }

    /**
     * Get maximum goal execution frequency (in ticks)
     * Prevents goals from running every single tick
     */
    public static int getGoalUpdateInterval(BotEntity bot) {
        double nearestPlayerDistance = getNearestPlayerDistance(bot);

        if (nearestPlayerDistance < CLOSE_DISTANCE) {
            return 2; // Update every 2 ticks for nearby bots
        } else if (nearestPlayerDistance < MEDIUM_DISTANCE) {
            return 5; // Update every 5 ticks
        } else {
            return 10; // Update every 10 ticks for distant bots
        }
    }
}
