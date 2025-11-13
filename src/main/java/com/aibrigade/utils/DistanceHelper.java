package com.aibrigade.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for distance calculations
 * Eliminates inconsistent use of distance vs distanceSqr and repeated calculations
 */
public class DistanceHelper {

    /**
     * Check if entity is within distance of target entity
     * Uses squared distance for performance
     */
    public static boolean isWithinDistance(Entity entity, Entity target, double distance) {
        if (entity == null || target == null) {
            return false;
        }
        return entity.distanceToSqr(target) <= distance * distance;
    }

    /**
     * Check if entity is outside the specified distance from target
     */
    public static boolean isOutsideDistance(Entity entity, Entity target, double distance) {
        if (entity == null || target == null) {
            return true;
        }
        return entity.distanceToSqr(target) > distance * distance;
    }

    /**
     * Check if entity is within distance of a position
     */
    public static boolean isWithinDistance(Entity entity, Vec3 position, double distance) {
        if (entity == null || position == null) {
            return false;
        }
        return entity.position().distanceToSqr(position) <= distance * distance;
    }

    /**
     * Check if entity is within distance of a block position
     */
    public static boolean isWithinDistance(Entity entity, BlockPos blockPos, double distance) {
        if (entity == null || blockPos == null) {
            return false;
        }
        return entity.blockPosition().distSqr(blockPos) <= distance * distance;
    }

    /**
     * Get actual (non-squared) distance between two entities
     */
    public static double getDistance(Entity entity, Entity target) {
        if (entity == null || target == null) {
            return Double.MAX_VALUE;
        }
        return entity.distanceTo(target);
    }

    /**
     * Get squared distance between two entities (more efficient)
     */
    public static double getDistanceSqr(Entity entity, Entity target) {
        if (entity == null || target == null) {
            return Double.MAX_VALUE;
        }
        return entity.distanceToSqr(target);
    }

    /**
     * Get horizontal distance only (ignoring Y axis)
     */
    public static double getHorizontalDistance(Entity entity, Entity target) {
        if (entity == null || target == null) {
            return Double.MAX_VALUE;
        }

        Vec3 entityPos = entity.position();
        Vec3 targetPos = target.position();

        double dx = entityPos.x - targetPos.x;
        double dz = entityPos.z - targetPos.z;

        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Get vertical distance (Y axis only)
     */
    public static double getVerticalDistance(Entity entity, Entity target) {
        if (entity == null || target == null) {
            return Double.MAX_VALUE;
        }
        return Math.abs(entity.getY() - target.getY());
    }

    /**
     * Check if entity should teleport to target (too far away)
     */
    public static boolean shouldTeleport(Entity entity, Entity target, double maxDistance) {
        return isOutsideDistance(entity, target, maxDistance);
    }
}
