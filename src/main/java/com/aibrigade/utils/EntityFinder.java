package com.aibrigade.utils;

import com.aibrigade.bots.BotEntity;
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
 */
public class EntityFinder {

    /**
     * Find any living entity (player or bot) by UUID within search radius
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
     * Find any living entity by UUID (searches entire level, no radius limit)
     * Use this when you don't have a search center or need to find distant entities
     */
    @Nullable
    public static LivingEntity findEntityByUUID(Level level, UUID entityUUID) {
        if (level == null || entityUUID == null) {
            return null;
        }

        // Try player first
        Player player = findPlayerByUUID(level, entityUUID);
        if (player != null) {
            return player;
        }

        // Try bot with large search area
        return findBotByUUID(level, entityUUID, null, Double.MAX_VALUE);
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
     */
    @Nullable
    public static BotEntity findBotByUUID(Level level, UUID botUUID, @Nullable Vec3 searchCenter, double searchRadius) {
        if (level == null || botUUID == null) {
            return null;
        }

        AABB searchBox = searchCenter != null
            ? new AABB(searchCenter.add(-searchRadius, -searchRadius, -searchRadius),
                       searchCenter.add(searchRadius, searchRadius, searchRadius))
            : new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000); // Full world bounds

        for (BotEntity bot : level.getEntitiesOfClass(BotEntity.class, searchBox)) {
            if (bot.getUUID().equals(botUUID)) {
                return bot;
            }
        }
        return null;
    }

    /**
     * Check if entity is within radius of center position
     */
    private static boolean isWithinRadius(LivingEntity entity, Vec3 center, double radius) {
        if (center == null || radius <= 0) {
            return true; // No radius limit
        }
        return entity.position().distanceToSqr(center) <= radius * radius;
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
