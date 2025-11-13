package com.aibrigade.utils;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for calculating positions
 * Eliminates duplicate random position generation code
 */
public class PositionCalculator {

    /**
     * Get a random offset position within a radius from center
     * Uses circular distribution (polar coordinates)
     */
    public static Vec3 getRandomOffsetPosition(Vec3 center, double radius, RandomSource random) {
        if (center == null || random == null) {
            return center;
        }

        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * radius;
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        return center.add(offsetX, 0, offsetZ);
    }

    /**
     * Get a random position within a box area (rectangular distribution)
     */
    public static Vec3 getRandomPositionInBox(Vec3 center, double radiusX, double radiusZ, RandomSource random) {
        if (center == null || random == null) {
            return center;
        }

        double offsetX = (random.nextDouble() - 0.5) * 2 * radiusX;
        double offsetZ = (random.nextDouble() - 0.5) * 2 * radiusZ;

        return center.add(offsetX, 0, offsetZ);
    }

    /**
     * Get a position in a ring around center (between minRadius and maxRadius)
     * Useful for patrol points
     */
    public static Vec3 getRandomRingPosition(Vec3 center, double minRadius, double maxRadius, RandomSource random) {
        if (center == null || random == null) {
            return center;
        }

        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        return center.add(offsetX, 0, offsetZ);
    }

    /**
     * Get a position at specific angle and distance from center
     * Useful for formations
     */
    public static Vec3 getPositionAtAngle(Vec3 center, double angle, double distance) {
        if (center == null) {
            return Vec3.ZERO;
        }

        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        return center.add(offsetX, 0, offsetZ);
    }

    /**
     * Get formation position for bot in line formation
     * @param leaderPos Leader position
     * @param index Bot index in formation (0, 1, 2, ...)
     * @param spacing Distance between bots
     * @param angle Formation angle (direction)
     */
    public static Vec3 getLineFormationPosition(Vec3 leaderPos, int index, double spacing, double angle) {
        // Offset perpendicular to movement direction
        double perpAngle = angle + Math.PI / 2;
        int side = (index % 2 == 0) ? 1 : -1; // Alternate sides
        int row = (index + 1) / 2; // Distance from leader

        double offsetX = Math.cos(perpAngle) * spacing * row * side;
        double offsetZ = Math.sin(perpAngle) * spacing * row * side;

        return leaderPos.add(offsetX, 0, offsetZ);
    }

    /**
     * Get formation position for bot in circle formation
     */
    public static Vec3 getCircleFormationPosition(Vec3 center, int index, int totalBots, double radius) {
        if (totalBots <= 0) {
            return center;
        }

        double angleStep = 2 * Math.PI / totalBots;
        double angle = angleStep * index;

        return getPositionAtAngle(center, angle, radius);
    }

    /**
     * Get formation position for bot in square formation
     */
    public static Vec3 getSquareFormationPosition(Vec3 center, int index, double spacing) {
        int sideLength = (int) Math.ceil(Math.sqrt(index + 1));
        int posInSquare = index % (sideLength * sideLength);

        int row = posInSquare / sideLength;
        int col = posInSquare % sideLength;

        double offsetX = (col - sideLength / 2.0) * spacing;
        double offsetZ = (row - sideLength / 2.0) * spacing;

        return center.add(offsetX, 0, offsetZ);
    }

    /**
     * Get formation position for bot in wedge formation (V-shape)
     */
    public static Vec3 getWedgeFormationPosition(Vec3 leaderPos, int index, double spacing, double angle) {
        int side = (index % 2 == 0) ? 1 : -1; // Alternate sides
        int row = (index + 1) / 2; // Distance from leader

        // Angle offset increases with distance from leader (creates V shape)
        double wedgeAngle = angle + (Math.PI / 6 * side); // 30 degree spread

        double offsetX = Math.cos(wedgeAngle) * spacing * row;
        double offsetZ = Math.sin(wedgeAngle) * spacing * row;

        return leaderPos.add(offsetX, 0, offsetZ);
    }
}
