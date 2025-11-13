package com.aibrigade.utils;

import com.aibrigade.bots.BotEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.ArrayList;

/**
 * EntityLibWrapper - Utility methods for entity operations
 *
 * Provides helper methods for:
 * - Spawning entities with offsets
 * - Detecting obstacles and collision
 * - Calculating entity positions
 * - Managing entity spacing
 * - Terrain analysis
 *
 * This wrapper provides functionality similar to EntityLib mod
 * but implemented natively for AIBrigade.
 */
public class EntityLibWrapper {

    /**
     * Spawn entity with offset from origin
     *
     * @param level The world level
     * @param origin Origin position
     * @param offsetX X offset
     * @param offsetY Y offset
     * @param offsetZ Z offset
     * @param entity Entity to spawn
     * @return true if spawned successfully
     */
    public static boolean spawnEntityWithOffset(Level level, BlockPos origin,
                                                  double offsetX, double offsetY, double offsetZ,
                                                  Entity entity) {
        BlockPos spawnPos = origin.offset((int)offsetX, (int)offsetY, (int)offsetZ);

        // Check if spawn position is valid
        if (!isPositionValid(level, spawnPos)) {
            // Try to find nearby valid position
            spawnPos = findNearbyValidPosition(level, spawnPos, 5);
            if (spawnPos == null) {
                return false;
            }
        }

        entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        return level.addFreshEntity(entity);
    }

    /**
     * Check if position is valid for entity spawn
     *
     * @param level The world level
     * @param pos Position to check
     * @return true if valid
     */
    public static boolean isPositionValid(Level level, BlockPos pos) {
        // Check if position is loaded
        if (!level.isLoaded(pos)) {
            return false;
        }

        // Check if there's solid ground
        BlockState below = level.getBlockState(pos.below());
        if (!below.isSolidRender(level, pos.below())) {
            return false;
        }

        // Check if space is clear (2 blocks high for entity)
        BlockState at = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());

        return at.isAir() && above.isAir();
    }

    /**
     * Find nearby valid spawn position
     *
     * @param level The world level
     * @param origin Origin position
     * @param searchRadius Search radius
     * @return Valid position, or null if none found
     */
    public static BlockPos findNearbyValidPosition(Level level, BlockPos origin, int searchRadius) {
        // Search in expanding spiral
        for (int radius = 1; radius <= searchRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Only check perimeter
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    BlockPos checkPos = origin.offset(x, 0, z);
                    if (isPositionValid(level, checkPos)) {
                        return checkPos;
                    }

                    // Also check one block up/down
                    for (int y = -1; y <= 1; y++) {
                        checkPos = origin.offset(x, y, z);
                        if (isPositionValid(level, checkPos)) {
                            return checkPos;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Detect obstacles between two positions
     *
     * @param level The world level
     * @param from Start position
     * @param to End position
     * @return List of obstacle positions
     */
    public static List<BlockPos> detectObstacles(Level level, Vec3 from, Vec3 to) {
        List<BlockPos> obstacles = new ArrayList<>();

        // Raycast from 'from' to 'to'
        Vec3 direction = to.subtract(from).normalize();
        double distance = from.distanceTo(to);

        for (double d = 0; d < distance; d += 0.5) {
            Vec3 point = from.add(direction.scale(d));
            BlockPos pos = new BlockPos((int)point.x, (int)point.y, (int)point.z);

            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isSolidRender(level, pos)) {
                obstacles.add(pos);
            }
        }

        return obstacles;
    }

    /**
     * Check if path is clear between two entities
     *
     * @param level The world level
     * @param from Start entity
     * @param to End entity
     * @return true if path is clear
     */
    public static boolean isPathClear(Level level, Entity from, Entity to) {
        List<BlockPos> obstacles = detectObstacles(level, from.position(), to.position());
        return obstacles.isEmpty();
    }

    /**
     * Calculate safe spacing position
     * Finds position that maintains minimum spacing from other entities
     *
     * @param level The world level
     * @param origin Origin position
     * @param nearbyEntities Nearby entities to avoid
     * @param minSpacing Minimum spacing distance
     * @return Safe position
     */
    public static Vec3 calculateSpacingPosition(Level level, Vec3 origin,
                                                  List<Entity> nearbyEntities, double minSpacing) {
        // Calculate repulsion vector from nearby entities
        Vec3 repulsion = Vec3.ZERO;

        for (Entity entity : nearbyEntities) {
            Vec3 toEntity = entity.position().subtract(origin);
            double distance = toEntity.length();

            if (distance < minSpacing && distance > 0) {
                // Push away from entity
                Vec3 pushDirection = toEntity.normalize().scale(-1);
                double pushStrength = (minSpacing - distance) / minSpacing;
                repulsion = repulsion.add(pushDirection.scale(pushStrength));
            }
        }

        // Calculate target position
        if (repulsion.lengthSqr() > 0) {
            return origin.add(repulsion.normalize().scale(minSpacing / 2));
        }

        return origin;
    }

    /**
     * Get entities within radius
     *
     * @param level The world level
     * @param center Center position
     * @param radius Search radius
     * @param entityClass Entity class filter
     * @return List of entities
     */
    public static <T extends Entity> List<T> getEntitiesInRadius(Level level, Vec3 center,
                                                                   double radius, Class<T> entityClass) {
        return level.getEntitiesOfClass(entityClass, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        ));
    }

    /**
     * Get bots within radius
     *
     * @param level The world level
     * @param center Center position
     * @param radius Search radius
     * @return List of bots
     */
    public static List<BotEntity> getBotsInRadius(Level level, Vec3 center, double radius) {
        return getEntitiesInRadius(level, center, radius, BotEntity.class);
    }

    /**
     * Calculate average position of entities
     *
     * @param entities List of entities
     * @return Average position
     */
    public static Vec3 calculateAveragePosition(List<? extends Entity> entities) {
        if (entities.isEmpty()) {
            return Vec3.ZERO;
        }

        Vec3 sum = Vec3.ZERO;
        for (Entity entity : entities) {
            sum = sum.add(entity.position());
        }

        return sum.scale(1.0 / entities.size());
    }

    /**
     * Check if position has line of sight to target
     *
     * @param level The world level
     * @param from Start position
     * @param to End position
     * @return true if line of sight exists
     */
    public static boolean hasLineOfSight(Level level, Vec3 from, Vec3 to) {
        // Simple raycast check
        return detectObstacles(level, from, to).isEmpty();
    }

    /**
     * Get terrain height at position
     *
     * @param level The world level
     * @param x X coordinate
     * @param z Z coordinate
     * @return Ground Y level
     */
    public static int getTerrainHeight(Level level, int x, int z) {
        BlockPos pos = new BlockPos(x, level.getMaxBuildHeight(), z);

        // Scan downwards for first solid block
        for (int y = pos.getY(); y >= level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(checkPos);

            if (state.isSolidRender(level, checkPos)) {
                return y + 1; // Return position above solid block
            }
        }

        return level.getMinBuildHeight();
    }

    /**
     * Check if position is in water
     *
     * @param level The world level
     * @param pos Position to check
     * @return true if in water
     */
    public static boolean isInWater(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isSource();
    }

    /**
     * Check if position is dangerous (lava, void, etc.)
     *
     * @param level The world level
     * @param pos Position to check
     * @return true if dangerous
     */
    public static boolean isDangerousPosition(Level level, BlockPos pos) {
        // Check for lava
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() == net.minecraft.world.level.block.Blocks.LAVA) {
            return true;
        }

        // Check for void (too low)
        if (pos.getY() < level.getMinBuildHeight() + 5) {
            return true;
        }

        // Check for fire
        if (state.getBlock() == net.minecraft.world.level.block.Blocks.FIRE) {
            return true;
        }

        return false;
    }

    /**
     * Snap position to ground
     *
     * @param level The world level
     * @param pos Position to snap
     * @param maxDistance Maximum distance to search
     * @return Ground position, or original if not found
     */
    public static BlockPos snapToGround(Level level, BlockPos pos, int maxDistance) {
        // Search down for ground
        for (int i = 0; i < maxDistance; i++) {
            BlockPos checkPos = pos.below(i);
            if (isPositionValid(level, checkPos)) {
                return checkPos;
            }
        }

        // Search up for ground
        for (int i = 1; i < maxDistance; i++) {
            BlockPos checkPos = pos.above(i);
            if (isPositionValid(level, checkPos)) {
                return checkPos;
            }
        }

        return pos;
    }
}
