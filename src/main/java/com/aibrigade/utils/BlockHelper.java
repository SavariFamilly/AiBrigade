package com.aibrigade.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility class for block state checking
 * Consolidates repeated block validation patterns
 *
 * THREAD SAFETY:
 * This class is THREAD-SAFE because:
 * - All methods are stateless (no mutable shared state)
 * - No caching or instance variables
 * - All operations are read-only queries via level.getBlockState()
 * - Minecraft's Level handles concurrent access internally
 *
 * NOTE: Results from multi-block checks (hasLineOfSight, hasVerticalClearance)
 * represent a snapshot in time and may become stale if blocks are modified
 * concurrently. This is expected behavior for read-only queries.
 *
 * @ThreadSafe All methods can be safely called from multiple threads
 */
public class BlockHelper {

    /**
     * Check if a block is air
     */
    public static boolean isAirBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        return level.getBlockState(pos).isAir();
    }

    /**
     * Check if a block is solid (not air and has collision)
     */
    public static boolean isSolidBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.isSolid();
    }

    /**
     * Check if a position is walkable (solid block below, air above)
     */
    public static boolean isWalkableSurface(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        BlockPos below = pos.below();
        return isSolidBlock(level, below) && isAirBlock(level, pos);
    }

    /**
     * Check if there's enough vertical clearance for an entity
     * @param height Height in blocks needed (typically 2 for player-sized entities)
     */
    public static boolean hasVerticalClearance(Level level, BlockPos pos, int height) {
        if (level == null || pos == null) {
            return false;
        }

        for (int i = 0; i < height; i++) {
            if (!isAirBlock(level, pos.above(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if position is safe for entity spawning/teleporting
     * Requires solid ground and 2 blocks of air space
     */
    public static boolean isSafeStandingPosition(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        // Need solid ground below
        if (!isSolidBlock(level, pos.below())) {
            return false;
        }

        // Need air at foot level and head level
        return hasVerticalClearance(level, pos, 2);
    }

    /**
     * Check if a block can be broken (not air, not bedrock, etc.)
     */
    public static boolean isBreakableBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        // Can't break air
        if (state.isAir()) {
            return false;
        }

        // Can't break bedrock or other indestructible blocks
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }

        return true;
    }

    /**
     * Check if a block can be placed at position
     */
    public static boolean canPlaceBlockAt(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        // Position must be air or replaceable
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    /**
     * Find the ground level below a position
     * Returns the BlockPos of the first solid block, or null if none found within range
     */
    public static BlockPos findGroundBelow(Level level, BlockPos startPos, int maxDepth) {
        if (level == null || startPos == null) {
            return null;
        }

        BlockPos checkPos = startPos;
        for (int i = 0; i < maxDepth; i++) {
            if (isSolidBlock(level, checkPos)) {
                return checkPos;
            }
            checkPos = checkPos.below();
        }

        return null;
    }

    /**
     * Check if there's a path between two positions (no solid blocks in between)
     */
    public static boolean hasLineOfSight(Level level, BlockPos from, BlockPos to) {
        if (level == null || from == null || to == null) {
            return false;
        }

        // Simple raycast check
        int steps = Math.max(Math.abs(to.getX() - from.getX()),
                           Math.max(Math.abs(to.getY() - from.getY()),
                                  Math.abs(to.getZ() - from.getZ())));

        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int x = (int) (from.getX() + t * (to.getX() - from.getX()));
            int y = (int) (from.getY() + t * (to.getY() - from.getY()));
            int z = (int) (from.getZ() + t * (to.getZ() - from.getZ()));

            BlockPos checkPos = new BlockPos(x, y, z);
            if (isSolidBlock(level, checkPos)) {
                return false;
            }
        }

        return true;
    }
}
