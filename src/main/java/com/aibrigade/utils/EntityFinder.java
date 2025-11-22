package com.aibrigade.utils;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.main.AIBrigadeMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Utility class for finding entities in the world
 * Eliminates duplicate entity searching logic across AI classes
 *
 * PERFORMANCE OPTIMIZATIONS:
 * - Uses ServerLevel.getEntity() for O(1) UUID lookup when available
 * - Limits maximum search radius to prevent performance issues
 * - Uses efficient AABB construction
 * - Warns when expensive full-world scans occur
 */
public class EntityFinder {

    /**
     * Maximum search radius before warning (blocks)
     * Prevents accidental full-world scans that cause lag
     */
    private static final double MAX_SAFE_RADIUS = 512.0;

    /**
     * Default search radius when none specified (blocks)
     * Reasonable for most leader-following scenarios
     */
    private static final double DEFAULT_SEARCH_RADIUS = 128.0;

    /**
     * Find any living entity (player or bot) by UUID within search radius
     *
     * CRITICAL PERFORMANCE FIX:
     * - Uses ServerLevel.getEntity() for O(1) lookup instead of iteration
     * - Only searches AABB if entity type unknown
     * - Clamps radius to prevent full-world scans
     *
     * @param level The world level to search in
     * @param entityUUID The UUID to search for
     * @param searchCenter Center position for search
     * @param searchRadius Maximum search radius
     * @return The found entity, or null if not found
     */
    @Nullable
    public static LivingEntity findEntityByUUID(Level level, UUID entityUUID, Vec3 searchCenter, double searchRadius) {
        if (level == null || entityUUID == null) {
            return null;
        }

        // CRITICAL PERFORMANCE FIX: Use O(1) UUID lookup if available
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(entityUUID);
            if (entity instanceof LivingEntity livingEntity) {
                // Check if within radius
                if (isWithinRadius(livingEntity, searchCenter, searchRadius)) {
                    return livingEntity;
                }
            }
            return null;
        }

        // Fallback for client-side (rare): search manually
        // First try to find as player (more common for leaders)
        Player player = findPlayerByUUID(level, entityUUID);
        if (player != null && isWithinRadius(player, searchCenter, searchRadius)) {
            return player;
        }

        // Then try to find as bot
        BotEntity bot = findBotByUUID(level, entityUUID, searchCenter, searchRadius);
        if (bot != null) {
            return bot;
        }

        return null;
    }

    /**
     * Find any living entity by UUID (no radius limit, uses O(1) lookup)
     * Use this when you don't have a search center or need to find distant entities
     *
     * CRITICAL PERFORMANCE FIX:
     * - Uses ServerLevel.getEntity() for O(1) lookup (instant)
     * - Old code used Double.MAX_VALUE radius → full-world scan → EXTREMELY SLOW
     * - New code: O(1) instead of O(n) where n = number of bots
     */
    @Nullable
    public static LivingEntity findEntityByUUID(Level level, UUID entityUUID) {
        if (level == null || entityUUID == null) {
            return null;
        }

        // CRITICAL PERFORMANCE FIX: Use O(1) UUID lookup if available
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(entityUUID);
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
            return null;
        }

        // Fallback for client-side: try player first (fast)
        Player player = findPlayerByUUID(level, entityUUID);
        if (player != null) {
            return player;
        }

        // CRITICAL FIX: Use reasonable default radius instead of Double.MAX_VALUE
        // Old: Double.MAX_VALUE caused full-world scan (VERY SLOW)
        // New: DEFAULT_SEARCH_RADIUS with null center (still searches efficiently)
        AIBrigadeMod.LOGGER.warn("EntityFinder.findEntityByUUID() called without search center - using default radius");
        return findBotByUUID(level, entityUUID, null, DEFAULT_SEARCH_RADIUS);
    }

    /**
     * Find a player by UUID
     */
    @Nullable
    public static Player findPlayerByUUID(Level level, UUID playerUUID) {
        if (level == null || playerUUID == null) {
            return null;
        }

        for (Player player : level.players()) {
            if (player.getUUID().equals(playerUUID)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Find a bot by UUID within a search area
     *
     * CRITICAL PERFORMANCE FIX:
     * - Clamps search radius to prevent full-world scans
     * - Uses efficient AABB construction
     * - Warns when expensive searches occur
     */
    @Nullable
    public static BotEntity findBotByUUID(Level level, UUID botUUID, @Nullable Vec3 searchCenter, double searchRadius) {
        if (level == null || botUUID == null) {
            return null;
        }

        // CRITICAL PERFORMANCE FIX: Try O(1) lookup first if ServerLevel
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(botUUID);
            if (entity instanceof BotEntity bot) {
                // Verify it's within radius if center specified
                if (searchCenter == null || isWithinRadius(bot, searchCenter, searchRadius)) {
                    return bot;
                }
            }
            return null;
        }

        // Fallback for client-side: AABB search
        // CRITICAL FIX: Clamp radius to prevent performance issues
        double clampedRadius = Math.min(searchRadius, MAX_SAFE_RADIUS);
        if (searchRadius > MAX_SAFE_RADIUS) {
            AIBrigadeMod.LOGGER.warn("EntityFinder.findBotByUUID() called with excessive radius {} - clamped to {}",
                searchRadius, MAX_SAFE_RADIUS);
        }

        AABB searchBox;
        if (searchCenter != null) {
            // CRITICAL FIX: Use efficient AABB construction
            searchBox = new AABB(
                searchCenter.x - clampedRadius, searchCenter.y - clampedRadius, searchCenter.z - clampedRadius,
                searchCenter.x + clampedRadius, searchCenter.y + clampedRadius, searchCenter.z + clampedRadius
            );
        } else {
            // CRITICAL FIX: If no center, use DEFAULT_SEARCH_RADIUS around origin
            // Old code used full world bounds → VERY SLOW
            AIBrigadeMod.LOGGER.warn("EntityFinder.findBotByUUID() called with null search center - using origin with default radius");
            searchBox = new AABB(
                -DEFAULT_SEARCH_RADIUS, -DEFAULT_SEARCH_RADIUS, -DEFAULT_SEARCH_RADIUS,
                DEFAULT_SEARCH_RADIUS, DEFAULT_SEARCH_RADIUS, DEFAULT_SEARCH_RADIUS
            );
        }

        for (BotEntity bot : level.getEntitiesOfClass(BotEntity.class, searchBox)) {
            if (bot.getUUID().equals(botUUID)) {
                return bot;
            }
        }
        return null;
    }

    /**
     * Check if entity is within radius of center position
     *
     * CRITICAL FIX: Uses DistanceHelper for overflow-safe distance calculation
     */
    private static boolean isWithinRadius(LivingEntity entity, Vec3 center, double radius) {
        if (center == null || radius <= 0) {
            return true; // No radius limit
        }

        // CRITICAL FIX: Use DistanceHelper for overflow protection
        return DistanceHelper.isWithinDistance(entity, center, radius);
    }

    /**
     * Find the leader entity for a bot
     * This is the most common use case in the codebase
     */
    @Nullable
    public static LivingEntity findLeader(BotEntity bot) {
        if (bot == null || !bot.isFollowingLeader()) {
            return null;
        }

        UUID leaderId = bot.getLeaderId();
        if (leaderId == null) {
            return null;
        }

        // Search within a reasonable radius around the bot
        return findEntityByUUID(bot.level(), leaderId, bot.position(), 100.0);
    }
}
