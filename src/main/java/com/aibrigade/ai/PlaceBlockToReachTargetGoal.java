package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

import java.util.*;

/**
 * Goal that makes bots place blocks to reach their target when pathfinding fails.
 * This allows bots to build bridges or towers to reach enemies or follow their leader.
 * Improved version with continuous bridge building and intelligent pathfinding.
 */
public class PlaceBlockToReachTargetGoal extends Goal {
    private final BotEntity bot;
    private LivingEntity target;
    private int placeCooldown = 0;
    private static final int PLACE_COOLDOWN_TICKS = 3; // 0.15 seconds between placements (very fast)
    private static final double MIN_DISTANCE_TO_PLACE = 3.0; // Minimum distance to target to start placing
    private static final double MAX_DISTANCE_TO_PLACE = 50.0; // Maximum distance to target to place blocks (increased)
    private Queue<BlockPos> plannedPath = new LinkedList<>();
    private int pathRecalculationTimer = 0;
    private static final int PATH_RECALC_INTERVAL = 40; // Recalculate path every 2 seconds
    private static final int MAX_BLOCKS_TO_PLACE = 50; // Maximum blocks to plan (increased for tall structures)

    public PlaceBlockToReachTargetGoal(BotEntity bot) {
        this.bot = bot;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Vérifier si la construction est autorisée
        if (!bot.canPlaceBlocks()) {
            return false;
        }

        // Decrease cooldown
        if (placeCooldown > 0) {
            placeCooldown--;
            return false; // Don't interrupt other goals during cooldown
        }

        // Check if bot has a target (enemy or leader)
        target = bot.getTarget();
        if (target == null) {
            // Check if following leader
            if (bot.isFollowingLeader() && bot.getLeaderId() != null) {
                // Try to find leader entity
                target = bot.level().getPlayerByUUID(bot.getLeaderId());
                if (target == null) {
                    // Leader might be another bot
                    target = findBotById(bot.getLeaderId());
                }
            }
        }

        if (target == null || !target.isAlive()) {
            return false;
        }

        // Check distance to target
        double distance = bot.distanceTo(target);
        if (distance < MIN_DISTANCE_TO_PLACE || distance > MAX_DISTANCE_TO_PLACE) {
            return false;
        }

        // Check if bot has blocks in offhand
        ItemStack offhandItem = bot.getOffhandItem();
        if (offhandItem.isEmpty() || !(offhandItem.getItem() instanceof BlockItem)) {
            return false;
        }

        // Check if we need to place blocks (path is stuck or target is unreachable)
        Vec3 botPos = bot.position();
        Vec3 targetPos = target.position();

        // Check if target is significantly higher (need to build up)
        if (targetPos.y > botPos.y + 2.0) {
            return true;
        }

        // Check if bot is enclosed/trapped (can't reach target due to walls)
        if (isEnclosed()) {
            return true;
        }

        // Check if there's a gap between bot and target
        if (hasGapBetweenBotAndTarget()) {
            return true;
        }

        // Check if navigation has failed multiple times
        if (isNavigationFailingRepeatedly()) {
            return true;
        }

        return false;
    }

    /**
     * Check if the bot is enclosed by walls (trapped in a room)
     */
    private boolean isEnclosed() {
        if (target == null) {
            return false;
        }

        Level level = bot.level();
        BlockPos botPos = bot.blockPosition();
        Vec3 directionToTarget = target.position().subtract(bot.position()).normalize();

        // Check if there are walls blocking in the direction of the target
        int wallsFound = 0;
        int airFound = 0;

        // Check 8 directions around the bot at eye level
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos checkPos = botPos.offset(dx, 0, dz);
                BlockPos aboveCheck = checkPos.above();

                // If solid block at walking level or head level
                if (!level.getBlockState(checkPos).isAir() || !level.getBlockState(aboveCheck).isAir()) {
                    wallsFound++;
                } else {
                    airFound++;
                }
            }
        }

        // If surrounded by walls on most sides (6+ out of 8)
        if (wallsFound >= 6) {
            // Check if there's an opening above (can escape by building up)
            BlockPos above = botPos.above(2);
            if (level.getBlockState(above).isAir()) {
                return true; // Enclosed but can build up to escape
            }
        }

        // Also check if blocked directly towards target
        for (int i = 1; i <= 2; i++) {
            BlockPos blockingPos = botPos.offset(
                (int)Math.round(directionToTarget.x * i),
                0,
                (int)Math.round(directionToTarget.z * i)
            );

            // Check if there's a wall in the way
            if (!level.getBlockState(blockingPos).isAir() ||
                !level.getBlockState(blockingPos.above()).isAir()) {

                // Check if we can build up to go over it
                BlockPos aboveWall = blockingPos.above(2);
                if (level.getBlockState(aboveWall).isAir()) {
                    return true; // Can build to go over the wall
                }
            }
        }

        return false;
    }

    /**
     * Check if there's a gap (pit, ravine, etc.) between bot and target
     */
    private boolean hasGapBetweenBotAndTarget() {
        if (target == null) {
            return false;
        }

        Vec3 direction = target.position().subtract(bot.position()).normalize();
        BlockPos checkPos = bot.blockPosition();
        Level level = bot.level();

        // Check 3 blocks ahead for gaps
        for (int i = 1; i <= 3; i++) {
            BlockPos nextPos = checkPos.offset(
                (int)(direction.x * i),
                0,
                (int)(direction.z * i)
            );

            // Check if there's air below (gap)
            BlockPos below = nextPos.below();
            BlockPos below2 = nextPos.below(2);

            if (level.getBlockState(below).isAir() && level.getBlockState(below2).isAir()) {
                return true; // Found a gap
            }
        }

        return false;
    }

    /**
     * Check if navigation is failing repeatedly (stuck)
     */
    private int navigationFailCount = 0;
    private int ticksSinceLastNavCheck = 0;

    private boolean isNavigationFailingRepeatedly() {
        ticksSinceLastNavCheck++;

        if (ticksSinceLastNavCheck >= 20) { // Check every second
            ticksSinceLastNavCheck = 0;

            if (bot.getNavigation().isDone() && target != null) {
                double distance = bot.distanceTo(target);
                if (distance > MIN_DISTANCE_TO_PLACE) {
                    navigationFailCount++;
                } else {
                    navigationFailCount = 0;
                }
            } else {
                navigationFailCount = 0;
            }
        }

        return navigationFailCount >= 3; // Failed 3 times in a row
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Continue if we have a planned path or still need to place blocks
        return !plannedPath.isEmpty() || canUse();
    }

    @Override
    public void start() {
        placeCooldown = 0;
        pathRecalculationTimer = 0;
        plannedPath.clear();
        calculateBridgePath();
    }

    @Override
    public void stop() {
        plannedPath.clear();
        pathRecalculationTimer = 0;
        navigationFailCount = 0;
        ticksSinceLastNavCheck = 0;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        // Look at target (or down if pillar jumping)
        if (!plannedPath.isEmpty()) {
            BlockPos nextPos = plannedPath.peek();
            if (nextPos != null && nextPos.equals(bot.blockPosition().above())) {
                // Looking down to place block under feet
                bot.getLookControl().setLookAt(bot.getX(), bot.getY() - 1, bot.getZ());
            } else {
                bot.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        } else {
            bot.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        // Recalculate path periodically
        pathRecalculationTimer++;
        if (pathRecalculationTimer >= PATH_RECALC_INTERVAL) {
            pathRecalculationTimer = 0;
            calculateBridgePath();
        }

        // Decrease cooldown
        if (placeCooldown > 0) {
            placeCooldown--;
            return;
        }

        // Place next block in the planned path
        if (!plannedPath.isEmpty()) {
            BlockPos nextPos = plannedPath.peek();
            if (nextPos != null) {
                // Special handling for pillar jumping (placing block right above current position)
                if (nextPos.equals(bot.blockPosition().above())) {
                    if (canPlaceBlockAt(nextPos)) {
                        placeBlock(nextPos);
                        plannedPath.poll();
                        placeCooldown = PLACE_COOLDOWN_TICKS;

                        // Jump up after placing
                        bot.getJumpControl().jump();
                        return;
                    } else {
                        plannedPath.poll(); // Can't place, skip
                    }
                }
                // Normal placement - check if close enough
                else if (bot.blockPosition().distSqr(nextPos) <= 25) { // Within 5 blocks
                    if (canPlaceBlockAt(nextPos)) {
                        placeBlock(nextPos);
                        plannedPath.poll();
                        placeCooldown = PLACE_COOLDOWN_TICKS;

                        // Move towards next block or target
                        if (!plannedPath.isEmpty()) {
                            BlockPos next = plannedPath.peek();
                            if (next != null) {
                                bot.getNavigation().moveTo(next.getX(), next.getY(), next.getZ(), 1.0);
                            }
                        } else {
                            bot.getNavigation().moveTo(target, 1.0);
                        }
                    } else {
                        plannedPath.poll(); // Can't place, skip
                    }
                } else {
                    // Move closer to placement position
                    bot.getNavigation().moveTo(nextPos.getX(), nextPos.getY(), nextPos.getZ(), 1.0);
                }
            }
        }
    }

    /**
     * Calculate a complete bridge/tower path from bot to target
     */
    private void calculateBridgePath() {
        plannedPath.clear();

        if (target == null) {
            return;
        }

        BlockPos botPos = bot.blockPosition();
        BlockPos targetPos = new BlockPos((int)target.getX(), (int)target.getY(), (int)target.getZ());
        Level level = bot.level();

        double heightDiff = target.getY() - bot.getY();
        double horizontalDistance = Math.sqrt(
            Math.pow(target.getX() - bot.getX(), 2) +
            Math.pow(target.getZ() - bot.getZ(), 2)
        );

        // PRIORITY 1: If bot is enclosed/trapped, build up to escape
        if (isEnclosed()) {
            buildEscapeRoute(botPos, targetPos, level);
        }
        // If target is very high up and not far horizontally, build a tower
        else if (heightDiff > 5.0 && horizontalDistance < 3.0) {
            buildVerticalTower(botPos, targetPos, level);
        }
        // If target is high and far, build diagonal stairs
        else if (heightDiff > 2.0) {
            buildDiagonalStairs(botPos, targetPos, level);
        }
        // Otherwise build a bridge
        else {
            buildHorizontalBridge(botPos, targetPos, level);
        }

        // Limit total blocks to place
        while (plannedPath.size() > MAX_BLOCKS_TO_PLACE) {
            plannedPath.poll();
        }
    }

    /**
     * Build an escape route when bot is enclosed/trapped
     */
    private void buildEscapeRoute(BlockPos botPos, BlockPos targetPos, Level level) {
        // First, build straight up to get out of the enclosed space
        int escapeHeight = 5; // Build 5 blocks up to escape most rooms

        // Build pillar up
        for (int y = 1; y <= escapeHeight; y++) {
            BlockPos pillarBlock = botPos.above(y);
            if (level.getBlockState(pillarBlock).isAir()) {
                plannedPath.offer(pillarBlock);
            }
        }

        // Once at top, check direction to target and build platform/bridge
        BlockPos topPos = botPos.above(escapeHeight);
        Vec3 direction = new Vec3(
            targetPos.getX() - botPos.getX(),
            0,
            targetPos.getZ() - botPos.getZ()
        ).normalize();

        // Build a small platform at top
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos platformBlock = topPos.offset(dx, 0, dz);
                if (level.getBlockState(platformBlock.below()).isAir()) {
                    plannedPath.offer(platformBlock.below());
                }
            }
        }

        // Build bridge/path towards target from elevated position
        int horizontalSteps = (int)Math.min(
            Math.sqrt(Math.pow(targetPos.getX() - botPos.getX(), 2) +
                     Math.pow(targetPos.getZ() - botPos.getZ(), 2)),
            15
        );

        for (int i = 1; i <= horizontalSteps; i++) {
            int x = botPos.getX() + (int)Math.round(direction.x * i);
            int z = botPos.getZ() + (int)Math.round(direction.z * i);
            int y = topPos.getY();

            BlockPos bridgeBlock = new BlockPos(x, y, z);
            BlockPos below = bridgeBlock.below();

            if (level.getBlockState(below).isAir()) {
                plannedPath.offer(below);
            }
        }
    }

    /**
     * Build a vertical tower (pillar jumping)
     */
    private void buildVerticalTower(BlockPos botPos, BlockPos targetPos, Level level) {
        int heightDiff = (int)(targetPos.getY() - botPos.getY());

        // Build a pillar straight up
        for (int y = 1; y <= heightDiff + 2; y++) {
            BlockPos pillarBlock = botPos.above(y);
            if (level.getBlockState(pillarBlock).isAir()) {
                plannedPath.offer(pillarBlock);
            }
        }

        // Once at target height, bridge horizontally if needed
        if (targetPos.getX() != botPos.getX() || targetPos.getZ() != botPos.getZ()) {
            Vec3 direction = new Vec3(
                targetPos.getX() - botPos.getX(),
                0,
                targetPos.getZ() - botPos.getZ()
            ).normalize();

            int horizontalSteps = (int)Math.ceil(Math.sqrt(
                Math.pow(targetPos.getX() - botPos.getX(), 2) +
                Math.pow(targetPos.getZ() - botPos.getZ(), 2)
            ));

            int topY = botPos.getY() + heightDiff;

            for (int i = 1; i <= horizontalSteps; i++) {
                int x = botPos.getX() + (int)Math.round(direction.x * i);
                int z = botPos.getZ() + (int)Math.round(direction.z * i);
                BlockPos bridgeBlock = new BlockPos(x, topY, z);
                BlockPos below = bridgeBlock.below();

                if (level.getBlockState(below).isAir()) {
                    plannedPath.offer(below);
                }
            }
        }
    }

    /**
     * Build diagonal stairs to reach target
     */
    private void buildDiagonalStairs(BlockPos botPos, BlockPos targetPos, Level level) {
        Vec3 direction = new Vec3(
            targetPos.getX() - botPos.getX(),
            0,
            targetPos.getZ() - botPos.getZ()
        ).normalize();

        double totalDistance = Math.sqrt(
            Math.pow(targetPos.getX() - botPos.getX(), 2) +
            Math.pow(targetPos.getZ() - botPos.getZ(), 2)
        );

        int steps = Math.min((int)totalDistance + 2, 40);
        double heightDiff = targetPos.getY() - botPos.getY();
        double heightPerStep = heightDiff / steps;

        BlockPos currentPos = botPos;

        for (int i = 1; i <= steps; i++) {
            int x = botPos.getX() + (int)Math.round(direction.x * i);
            int z = botPos.getZ() + (int)Math.round(direction.z * i);
            int y = botPos.getY() + (int)Math.ceil(heightPerStep * i);

            BlockPos stepPos = new BlockPos(x, y, z);

            // Build up from current position
            if (stepPos.getY() > currentPos.getY()) {
                for (int buildY = currentPos.getY() + 1; buildY <= stepPos.getY(); buildY++) {
                    BlockPos buildPos = new BlockPos(x, buildY, z);
                    if (level.getBlockState(buildPos).isAir()) {
                        plannedPath.offer(buildPos);
                    }
                }
            }

            // Place block below for walking surface
            BlockPos below = stepPos.below();
            if (level.getBlockState(below).isAir()) {
                plannedPath.offer(below);
            }

            currentPos = stepPos;

            // Stop if close enough
            if (stepPos.distSqr(targetPos) < 16) {
                break;
            }
        }
    }

    /**
     * Build a horizontal bridge
     */
    private void buildHorizontalBridge(BlockPos botPos, BlockPos targetPos, Level level) {
        Vec3 direction = new Vec3(
            targetPos.getX() - botPos.getX(),
            0,
            targetPos.getZ() - botPos.getZ()
        ).normalize();

        double distance = Math.sqrt(
            Math.pow(targetPos.getX() - botPos.getX(), 2) +
            Math.pow(targetPos.getZ() - botPos.getZ(), 2)
        );

        int steps = Math.min((int)distance + 2, 30);

        for (int i = 1; i <= steps; i++) {
            int x = botPos.getX() + (int)Math.round(direction.x * i);
            int z = botPos.getZ() + (int)Math.round(direction.z * i);
            int y = botPos.getY();

            BlockPos checkPos = new BlockPos(x, y, z);
            BlockPos below = checkPos.below();

            // Check for gaps and fill them
            if (level.getBlockState(below).isAir()) {
                int depth = 0;
                BlockPos checkBelow = below;
                while (level.getBlockState(checkBelow).isAir() && depth < 5) {
                    depth++;
                    checkBelow = checkBelow.below();
                }

                // Fill from bottom up
                for (int d = Math.min(depth, 4); d > 0; d--) {
                    BlockPos fillPos = checkPos.below(d);
                    if (level.getBlockState(fillPos).isAir()) {
                        plannedPath.offer(fillPos);
                    }
                }
            }

            if (checkPos.distSqr(targetPos) < 9) {
                break;
            }
        }
    }

    /**
     * Check if a block can be placed at the given position
     */
    private boolean canPlaceBlockAt(BlockPos pos) {
        Level level = bot.level();

        // Position must be air
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        // Must have a solid block nearby to place against
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            if (!level.getBlockState(adjacent).isAir()) {
                return true; // Found solid block to place against
            }
        }

        return false;
    }

    /**
     * Place a block at the specified position
     */
    private void placeBlock(BlockPos pos) {
        ItemStack offhandItem = bot.getOffhandItem();
        if (offhandItem.isEmpty() || !(offhandItem.getItem() instanceof BlockItem)) {
            return;
        }

        BlockItem blockItem = (BlockItem) offhandItem.getItem();
        Level level = bot.level();

        // Check if position is valid
        if (!level.getBlockState(pos).isAir()) {
            return;
        }

        // Place the block
        BlockState blockState = blockItem.getBlock().defaultBlockState();
        if (level.setBlock(pos, blockState, 3)) {
            // Consume one block from stack
            offhandItem.shrink(1);

            // Play placement sound
            level.playSound(null, pos, blockState.getSoundType().getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * Check if bot has line of sight to target
     */
    private boolean hasLineOfSight() {
        if (target == null) {
            return false;
        }
        return bot.hasLineOfSight(target);
    }

    /**
     * Check if the bot's navigation is stuck (not making progress)
     */
    private boolean isNavigationStuck() {
        // If navigation is trying but not in progress, likely stuck
        return !bot.getNavigation().isDone() && !bot.getNavigation().isInProgress();
    }

    /**
     * Find a bot entity by UUID
     */
    private BotEntity findBotById(java.util.UUID id) {
        if (bot.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (net.minecraft.world.entity.Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof BotEntity && entity.getUUID().equals(id)) {
                    return (BotEntity) entity;
                }
            }
        }
        return null;
    }
}
