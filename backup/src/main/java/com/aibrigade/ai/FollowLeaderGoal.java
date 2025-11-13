package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.BotManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.EnumSet;

/**
 * Custom AI Goal that makes bots follow their leader (player or bot) in a realistic way.
 * Bots will:
 * - Keep a comfortable distance (3-5 blocks) from the leader
 * - Spread out within the follow radius (not all converge to exact same spot)
 * - Get speed boost if too far away (instead of teleporting)
 * - Stop moving when close enough
 * - Path find naturally like real players
 */
public class FollowLeaderGoal extends Goal {
    private final BotEntity bot;
    private LivingEntity leader;
    private final double followSpeed;
    private final float minDistance;
    private final float maxDistance;
    private int timeToRecalcPath;
    private final float speedBoostDistance; // Distance at which bot gets speed boost
    private net.minecraft.world.phys.Vec3 targetPosition; // Specific position within radius

    /**
     * Create a new FollowLeaderGoal
     * @param bot The bot that will follow
     * @param followSpeed Speed multiplier when following (1.0 = normal speed)
     * @param minDistance Minimum distance to maintain from leader
     * @param maxDistance Distance at which to start following
     */
    public FollowLeaderGoal(BotEntity bot, double followSpeed, float minDistance, float maxDistance) {
        this.bot = bot;
        this.followSpeed = followSpeed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.speedBoostDistance = 100.0F; // Only remove speed boost when within 100 blocks
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Check if this goal can be used
     */
    @Override
    public boolean canUse() {
        // Only follow if followLeader is enabled
        if (!bot.isFollowingLeader()) {
            return false;
        }

        // Find the leader
        this.leader = findLeader();

        if (this.leader == null) {
            return false;
        }

        // Don't follow if leader is too close
        double distance = this.bot.distanceToSqr(this.leader);
        if (distance < (double)(this.minDistance * this.minDistance)) {
            return false;
        }

        return true;
    }

    /**
     * Check if this goal should continue executing
     */
    @Override
    public boolean canContinueToUse() {
        if (this.leader == null || !this.leader.isAlive()) {
            return false;
        }

        if (!bot.isFollowingLeader()) {
            return false;
        }

        double distance = this.bot.distanceToSqr(this.leader);

        // Stop if we're close enough
        if (distance <= (double)(this.minDistance * this.minDistance)) {
            return false;
        }

        // Continue if we're within max range
        return distance <= (double)(this.maxDistance * this.maxDistance);
    }

    /**
     * Start executing this goal
     */
    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.targetPosition = null; // Will be calculated on first tick
    }

    /**
     * Stop executing this goal
     */
    @Override
    public void stop() {
        this.leader = null;
        this.targetPosition = null;
        this.bot.getNavigation().stop();
    }

    /**
     * Update every tick
     */
    @Override
    public void tick() {
        if (this.leader == null) {
            return;
        }

        // Look at the leader
        this.bot.getLookControl().setLookAt(this.leader, 10.0F, (float)this.bot.getMaxHeadXRot());

        double distanceSqr = this.bot.distanceToSqr(this.leader);
        double distance = Math.sqrt(distanceSqr);

        // Apply speed boost if far away, remove it when within 100 blocks
        if (distance > this.speedBoostDistance) {
            // Calculate speed boost level based on distance
            // 100-150 blocks: Speed I
            // 150-200 blocks: Speed II
            // 200+ blocks: Speed III
            int speedLevel = 0;
            if (distance > 200) {
                speedLevel = 2; // Speed III
            } else if (distance > 150) {
                speedLevel = 1; // Speed II
            } else if (distance > 100) {
                speedLevel = 0; // Speed I
            }

            // Apply speed effect for 2 seconds
            this.bot.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, speedLevel, false, false));
        } else {
            // Remove speed effect when within 100 blocks
            if (this.bot.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                this.bot.removeEffect(MobEffects.MOVEMENT_SPEED);
            }
        }

        // Recalculate target position and path every 20 ticks (1 second)
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 20;

            // Don't path find if very close
            if (distanceSqr <= (double)(this.minDistance * this.minDistance)) {
                this.bot.getNavigation().stop();
                return;
            }

            // Calculate a spread position within the follow radius
            if (this.targetPosition == null || this.leader.position().distanceTo(this.targetPosition) > this.maxDistance) {
                this.targetPosition = calculateSpreadPosition();
            }

            // Check if we reached our target position
            if (this.bot.position().distanceTo(this.targetPosition) < 2.0) {
                // Reached target, calculate new position
                this.targetPosition = calculateSpreadPosition();
            }

            // Path find to our specific spread position, not the leader's exact position
            this.bot.getNavigation().moveTo(this.targetPosition.x, this.targetPosition.y, this.targetPosition.z, this.followSpeed);
        }
    }

    /**
     * Calculate a spread position within the follow radius around the leader
     * This prevents all bots from converging to the exact same spot
     */
    private net.minecraft.world.phys.Vec3 calculateSpreadPosition() {
        if (this.leader == null) {
            return this.bot.position();
        }

        // Use bot's UUID to generate a consistent but unique offset for this bot
        long seed = this.bot.getUUID().getMostSignificantBits();
        java.util.Random random = new java.util.Random(seed + this.bot.tickCount / 100); // Change slowly over time

        // Calculate a position within the follow radius
        // Use minDistance as the minimum radius, maxDistance as maximum
        double spreadRadius = this.minDistance + random.nextDouble() * (this.maxDistance - this.minDistance);
        double angle = random.nextDouble() * Math.PI * 2;

        double offsetX = Math.cos(angle) * spreadRadius;
        double offsetZ = Math.sin(angle) * spreadRadius;

        net.minecraft.core.BlockPos targetPos = new net.minecraft.core.BlockPos(
            (int)(this.leader.getX() + offsetX),
            (int)this.leader.getY(),
            (int)(this.leader.getZ() + offsetZ)
        );

        // Find ground level at target position
        net.minecraft.core.BlockPos groundPos = findGroundBelow(targetPos, 10);

        return new net.minecraft.world.phys.Vec3(
            targetPos.getX() + 0.5,
            groundPos.getY() + 1.0,
            targetPos.getZ() + 0.5
        );
    }

    /**
     * Find the ground level below a position
     */
    private net.minecraft.core.BlockPos findGroundBelow(net.minecraft.core.BlockPos pos, int maxDepth) {
        for (int i = 0; i < maxDepth; i++) {
            net.minecraft.core.BlockPos checkPos = pos.below(i);
            if (!this.bot.level().getBlockState(checkPos).isAir()) {
                return checkPos;
            }
        }
        return pos; // No ground found, return original
    }

    /**
     * Find the leader entity for this bot
     * @return The leader entity (Player or BotEntity) or null if not found
     */
    private LivingEntity findLeader() {
        java.util.UUID leaderId = bot.getLeaderId();

        if (leaderId == null) {
            return null;
        }

        // Try to find leader as a player first
        for (Player player : bot.level().players()) {
            if (player.getUUID().equals(leaderId)) {
                return player;
            }
        }

        // Try to find leader as a bot
        // Search nearby entities
        for (BotEntity nearbyBot : bot.level().getEntitiesOfClass(BotEntity.class,
                bot.getBoundingBox().inflate(64.0D))) {
            if (nearbyBot.getUUID().equals(leaderId)) {
                return nearbyBot;
            }
        }

        return null;
    }
}
