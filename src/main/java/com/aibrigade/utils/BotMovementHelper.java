package com.aibrigade.utils;

import com.aibrigade.bots.BotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for bot movement and navigation
 * Centralizes navigation patterns used across AI goals
 */
public class BotMovementHelper {

    /**
     * Move bot to entity with specified speed
     * CRITICAL FIX: Added null check on getNavigation()
     */
    public static void moveToEntity(BotEntity bot, LivingEntity target, double speed) {
        if (bot == null || target == null) {
            return;
        }

        // CRITICAL FIX: getNavigation() can return null if bot is in invalid state
        var navigation = bot.getNavigation();
        if (navigation == null) {
            return;
        }

        navigation.moveTo(target, speed);
    }

    /**
     * Move bot to entity with normal walking speed
     */
    public static void moveToEntity(BotEntity bot, LivingEntity target) {
        moveToEntity(bot, target, BotAIConstants.SPEED_WALK);
    }

    /**
     * Move bot to entity quickly (running)
     */
    public static void runToEntity(BotEntity bot, LivingEntity target) {
        moveToEntity(bot, target, BotAIConstants.SPEED_RUN);
    }

    /**
     * Move bot to entity at sprint speed
     */
    public static void sprintToEntity(BotEntity bot, LivingEntity target) {
        moveToEntity(bot, target, BotAIConstants.SPEED_SPRINT);
    }

    /**
     * Move bot to position with specified speed
     * CRITICAL FIX: Added null check on getNavigation()
     */
    public static void moveToPosition(BotEntity bot, Vec3 position, double speed) {
        if (bot == null || position == null) {
            return;
        }

        // CRITICAL FIX: Null check on navigation
        var navigation = bot.getNavigation();
        if (navigation == null) {
            return;
        }

        navigation.moveTo(position.x, position.y, position.z, speed);
    }

    /**
     * Move bot to block position with specified speed
     * CRITICAL FIX: Added null check on getNavigation()
     */
    public static void moveToBlockPos(BotEntity bot, BlockPos pos, double speed) {
        if (bot == null || pos == null) {
            return;
        }

        // CRITICAL FIX: Null check on navigation
        var navigation = bot.getNavigation();
        if (navigation == null) {
            return;
        }

        navigation.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, speed);
    }

    /**
     * Move bot to position with normal walking speed
     */
    public static void moveToPosition(BotEntity bot, Vec3 position) {
        moveToPosition(bot, position, BotAIConstants.SPEED_WALK);
    }

    /**
     * Stop bot movement
     * CRITICAL FIX: Added null check on getNavigation()
     */
    public static void stopMovement(BotEntity bot) {
        if (bot == null) {
            return;
        }

        // CRITICAL FIX: Null check on navigation
        var navigation = bot.getNavigation();
        if (navigation == null) {
            return;
        }

        navigation.stop();
    }

    /**
     * Check if bot has reached its destination
     * CRITICAL FIX: Added null check on getNavigation()
     */
    public static boolean hasReachedDestination(BotEntity bot) {
        if (bot == null) {
            return true;
        }

        // CRITICAL FIX: Null check on navigation
        var navigation = bot.getNavigation();
        if (navigation == null) {
            return true; // If no navigation, consider it "done"
        }

        return navigation.isDone();
    }

    /**
     * Check if bot is currently moving
     * CRITICAL FIX: Added null check on getNavigation()
     */
    public static boolean isMoving(BotEntity bot) {
        if (bot == null) {
            return false;
        }

        // CRITICAL FIX: Null check on navigation
        var navigation = bot.getNavigation();
        if (navigation == null) {
            return false; // If no navigation, not moving
        }

        return !navigation.isDone();
    }

    /**
     * Teleport bot to entity if too far away
     * Returns true if teleported
     *
     * NO TELEPORT FIX: Téléportation complètement désactivée
     * Cette méthode retourne toujours false et ne téléporte JAMAIS
     */
    public static boolean teleportIfTooFar(BotEntity bot, LivingEntity target, double maxDistance) {
        if (bot == null || target == null) {
            return false;
        }

        // NO TELEPORT FIX: Téléportation désactivée - les bots marchent toujours
        // if (DistanceHelper.shouldTeleport(bot, target, maxDistance)) {
        //     bot.teleportTo(target.getX(), target.getY(), target.getZ());
        //     stopMovement(bot);
        //     return true;
        // }

        return false; // Jamais de téléportation
    }

    /**
     * Get current movement speed multiplier based on distance to target
     * Closer = slower, farther = faster
     */
    public static double getAdaptiveSpeed(BotEntity bot, LivingEntity target, double minDistance, double maxDistance) {
        if (bot == null || target == null) {
            return BotAIConstants.SPEED_WALK;
        }

        double distance = DistanceHelper.getDistance(bot, target);

        if (distance < minDistance) {
            return BotAIConstants.SPEED_SLOW;
        } else if (distance > maxDistance) {
            return BotAIConstants.SPEED_SPRINT;
        } else if (distance > minDistance * 2) {
            return BotAIConstants.SPEED_RUN;
        }

        return BotAIConstants.SPEED_WALK;
    }
}
