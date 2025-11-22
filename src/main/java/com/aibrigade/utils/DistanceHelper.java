package com.aibrigade.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for distance calculations
 * Eliminates inconsistent use of distance vs distanceSqr and repeated calculations
 *
 * OVERFLOW PROTECTION:
 * All distance calculations are protected against overflow by:
 * - Clamping distance parameters to safe ranges
 * - Validating intermediate calculations
 * - Using Double.MAX_VALUE as sentinel for invalid/infinite distances
 *
 * Maximum safe distance for squared operations: sqrt(Double.MAX_VALUE) ≈ 1.3e154
 * Minecraft coordinates: ±30,000,000 (safe for all operations)
 */
public class DistanceHelper {

    // Maximum safe distance before squaring would overflow
    // sqrt(Double.MAX_VALUE) ≈ 1.34e154, but we use a more conservative limit
    private static final double MAX_SAFE_DISTANCE = 1e100;

    /**
     * Clamp distance to safe range to prevent overflow in distance² calculations
     */
    private static double clampDistance(double distance) {
        if (Double.isNaN(distance) || Double.isInfinite(distance)) {
            return MAX_SAFE_DISTANCE;
        }
        return Math.min(Math.abs(distance), MAX_SAFE_DISTANCE);
    }

    /**
     * Check if entity is within distance of target entity
     * Uses squared distance for performance
     * OVERFLOW PROTECTION: Distance is clamped to prevent overflow in distance²
     */
    public static boolean isWithinDistance(Entity entity, Entity target, double distance) {
        if (entity == null || target == null) {
            return false;
        }
        // CRITICAL FIX: Clamp distance to prevent overflow
        double safeDist = clampDistance(distance);
        return entity.distanceToSqr(target) <= safeDist * safeDist;
    }

    /**
     * Check if entity is outside the specified distance from target
     * OVERFLOW PROTECTION: Distance is clamped to prevent overflow in distance²
     */
    public static boolean isOutsideDistance(Entity entity, Entity target, double distance) {
        if (entity == null || target == null) {
            return true;
        }
        // CRITICAL FIX: Clamp distance to prevent overflow
        double safeDist = clampDistance(distance);
        return entity.distanceToSqr(target) > safeDist * safeDist;
    }

    /**
     * Check if entity is within distance of a position
     * OVERFLOW PROTECTION: Distance is clamped to prevent overflow in distance²
     */
    public static boolean isWithinDistance(Entity entity, Vec3 position, double distance) {
        if (entity == null || position == null) {
            return false;
        }
        // CRITICAL FIX: Clamp distance to prevent overflow
        double safeDist = clampDistance(distance);
        return entity.position().distanceToSqr(position) <= safeDist * safeDist;
    }

    /**
     * Check if entity is within distance of a block position
     * OVERFLOW PROTECTION: Distance is clamped to prevent overflow in distance²
     */
    public static boolean isWithinDistance(Entity entity, BlockPos blockPos, double distance) {
        if (entity == null || blockPos == null) {
            return false;
        }
        // CRITICAL FIX: Clamp distance to prevent overflow
        double safeDist = clampDistance(distance);
        return entity.blockPosition().distSqr(blockPos) <= safeDist * safeDist;
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
     * OVERFLOW PROTECTION: Validates intermediate calculations
     */
    public static double getHorizontalDistance(Entity entity, Entity target) {
        if (entity == null || target == null) {
            return Double.MAX_VALUE;
        }

        Vec3 entityPos = entity.position();
        Vec3 targetPos = target.position();

        double dx = entityPos.x - targetPos.x;
        double dz = entityPos.z - targetPos.z;

        // CRITICAL FIX: Check for potential overflow before squaring
        // For Minecraft (±30M coords), max dx ≈ 60M → 60M² = 3.6e15 (SAFE)
        // But validate anyway for robustness
        double dxSqr = dx * dx;
        double dzSqr = dz * dz;

        // If overflow occurred, return max value
        if (Double.isInfinite(dxSqr) || Double.isInfinite(dzSqr)) {
            return Double.MAX_VALUE;
        }

        double sum = dxSqr + dzSqr;
        if (Double.isInfinite(sum)) {
            return Double.MAX_VALUE;
        }

        return Math.sqrt(sum);
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
