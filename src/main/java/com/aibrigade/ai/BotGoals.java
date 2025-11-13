package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.utils.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;

/**
 * BotGoals - Custom AI goals for bot entities
 *
 * This class contains goal implementations for various bot behaviors:
 * - Following a leader (player or bot)
 * - Attacking hostile entities
 * - Patrolling an area
 * - Dispersing to avoid clustering
 * - Climbing obstacles
 * - Avoiding obstacles
 *
 * Each goal extends Minecraft's Goal class and can be added to a bot's
 * goal selector for autonomous behavior.
 */
public class BotGoals {

    /**
     * Goal: Follow Leader
     *
     * Bot follows assigned leader within a specified radius.
     * Will path around obstacles, climb when necessary, and maintain spacing.
     */
    public static class FollowLeaderGoal extends Goal {
        private final BotEntity bot;
        private LivingEntity leader;
        private final float followRadius;
        private final float minDistance;
        private int updatePathCooldown;

        public FollowLeaderGoal(BotEntity bot, float followRadius) {
            this.bot = bot;
            this.followRadius = followRadius;
            this.minDistance = (float) BotAIConstants.MIN_FOLLOW_DISTANCE;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // Check if bot has a leader assigned
            if (bot.getLeaderId() == null) {
                return false;
            }

            // Find leader entity
            leader = EntityFinder.findLeader(bot);
            if (!EntityValidator.isEntityValid(leader)) {
                return false;
            }

            // Check if bot is outside follow radius
            return DistanceHelper.isOutsideDistance(bot, leader, minDistance);
        }

        @Override
        public boolean canContinueToUse() {
            if (!EntityValidator.isEntityValid(leader)) {
                return false;
            }

            return DistanceHelper.isOutsideDistance(bot, leader, minDistance) &&
                   DistanceHelper.isWithinDistance(bot, leader, followRadius * 2);
        }

        @Override
        public void start() {
            updatePathCooldown = 0;
        }

        @Override
        public void tick() {
            if (leader == null) {
                return;
            }

            // Look at leader
            BotLookHelper.lookAtEntity(bot, leader);

            // Update path periodically
            if (--updatePathCooldown <= 0) {
                updatePathCooldown = 10; // Update every 10 ticks

                // If far from leader, move towards them
                if (DistanceHelper.isOutsideDistance(bot, leader, followRadius)) {
                    // Path to leader - move faster when catching up
                    BotMovementHelper.moveToEntity(bot, leader, BotAIConstants.SPEED_RUN);
                } else if (DistanceHelper.isWithinDistance(bot, leader, minDistance)) {
                    // Too close, back off slightly
                    BotMovementHelper.stopMovement(bot);
                } else {
                    // Normal follow speed
                    BotMovementHelper.moveToEntity(bot, leader);
                }

                // Check if stuck or need to climb
                if (BotMovementHelper.hasReachedDestination(bot) &&
                    DistanceHelper.isOutsideDistance(bot, leader, minDistance)) {
                    // Try to jump if blocked
                    BotJumpHelper.jump(bot);
                }
            }
        }

        @Override
        public void stop() {
            leader = null;
            BotMovementHelper.stopMovement(bot);
        }
    }

    /**
     * Goal: Attack Hostile Entities
     *
     * Bot attacks nearby hostile entities or entities from hostile groups.
     */
    public static class AttackHostileGoal extends Goal {
        private final BotEntity bot;
        private LivingEntity target;
        private final float attackRange;
        private int attackCooldown;

        public AttackHostileGoal(BotEntity bot, float attackRange) {
            this.bot = bot;
            this.attackRange = attackRange;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            // Find nearest hostile entity
            target = findNearestHostile();
            return EntityValidator.isEntityValid(target);
        }

        @Override
        public boolean canContinueToUse() {
            return EntityValidator.isEntityValid(target) &&
                   DistanceHelper.isWithinDistance(bot, target, attackRange * 2);
        }

        @Override
        public void start() {
            bot.setAIState(BotEntity.BotAIState.ATTACKING);
            attackCooldown = 0;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }

            // Look at target
            BotLookHelper.lookAtEntity(bot, target, BotAIConstants.LOOK_YAW_SPEED_FAST, BotAIConstants.LOOK_PITCH_SPEED_FAST);

            // Move towards target if too far
            if (DistanceHelper.isOutsideDistance(bot, target, attackRange)) {
                BotMovementHelper.moveToEntity(bot, target, BotAIConstants.SPEED_RUN);
            } else {
                BotMovementHelper.stopMovement(bot);

                // Attack if cooldown expired
                if (--attackCooldown <= 0) {
                    attackCooldown = BotAIConstants.COMBAT_COOLDOWN_TICKS;
                    bot.doHurtTarget(target);
                }
            }
        }

        @Override
        public void stop() {
            target = null;
            BotMovementHelper.stopMovement(bot);
        }

        /**
         * Find nearest hostile entity
         */
        private LivingEntity findNearestHostile() {
            // TODO: Implement hostile entity detection
            // - Check for hostile mobs
            // - Check for bots from hostile groups
            // - Check for players if applicable
            return null;
        }
    }

    /**
     * Goal: Patrol Area
     *
     * Bot patrols around home position, moving between waypoints.
     */
    public static class PatrolGoal extends Goal {
        private final BotEntity bot;
        private final float patrolRadius;
        private BlockPos currentWaypoint;
        private int waypointCooldown;

        public PatrolGoal(BotEntity bot, float patrolRadius) {
            this.bot = bot;
            this.patrolRadius = patrolRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bot.getHomePosition() != null && !bot.isStatic();
        }

        @Override
        public void start() {
            bot.setAIState(BotEntity.BotAIState.PATROLLING);
            waypointCooldown = 0;
            selectNewWaypoint();
        }

        @Override
        public void tick() {
            if (currentWaypoint == null) {
                selectNewWaypoint();
                return;
            }

            // Check if reached waypoint
            if (DistanceHelper.isWithinDistance(bot, currentWaypoint, 2.0)) {
                // Wait at waypoint
                if (--waypointCooldown <= 0) {
                    selectNewWaypoint();
                }
            } else {
                // Move to waypoint
                BotMovementHelper.moveToBlockPos(bot, currentWaypoint, BotAIConstants.SPEED_SLOW);
            }
        }

        /**
         * Select a new random waypoint within patrol radius
         */
        private void selectNewWaypoint() {
            BlockPos home = bot.getHomePosition();
            if (home == null) {
                return;
            }

            // Generate random point within patrol radius using PositionCalculator
            currentWaypoint = BlockPos.containing(
                PositionCalculator.getRandomOffsetPosition(
                    home.getCenter(),
                    patrolRadius,
                    bot.getRandom()
                )
            );
            waypointCooldown = 100; // Wait 5 seconds at waypoint
        }

        @Override
        public void stop() {
            currentWaypoint = null;
            BotMovementHelper.stopMovement(bot);
        }
    }

    /**
     * Goal: Disperse
     *
     * Bot spreads out to avoid clustering with other bots.
     */
    public static class DisperseGoal extends Goal {
        private final BotEntity bot;
        private final float minSpacing;
        private BlockPos disperseTarget;

        public DisperseGoal(BotEntity bot, float minSpacing) {
            this.bot = bot;
            this.minSpacing = minSpacing;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Check if too close to other bots
            return hasNearbyBots();
        }

        @Override
        public void start() {
            bot.setAIState(BotEntity.BotAIState.DISPERSING);
            calculateDisperseDirection();
        }

        @Override
        public void tick() {
            if (disperseTarget != null) {
                BotMovementHelper.moveToBlockPos(bot, disperseTarget, BotAIConstants.SPEED_WALK);
            }
        }

        @Override
        public void stop() {
            disperseTarget = null;
            BotMovementHelper.stopMovement(bot);
        }

        /**
         * Check if there are nearby bots within min spacing
         */
        private boolean hasNearbyBots() {
            // TODO: Implement nearby bot detection
            // Count bots within minSpacing radius
            return false;
        }

        /**
         * Calculate direction to disperse away from cluster
         */
        private void calculateDisperseDirection() {
            // TODO: Implement disperse calculation
            // - Find all nearby bots
            // - Calculate average position
            // - Move away from center
        }
    }

    /**
     * Goal: Climb Obstacles
     *
     * Bot climbs obstacles to reach leader or destination.
     */
    public static class ClimbObstacleGoal extends Goal {
        private final BotEntity bot;
        private BlockPos obstaclePos;

        public ClimbObstacleGoal(BotEntity bot) {
            this.bot = bot;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            // Check if bot's path is blocked by obstacle
            return isPathBlocked();
        }

        @Override
        public void start() {
            bot.setAIState(BotEntity.BotAIState.CLIMBING);
        }

        @Override
        public void tick() {
            // Jump to climb
            BotJumpHelper.jump(bot);

            // TODO: Implement advanced climbing
            // - Place blocks if bot has them
            // - Break blocks if allowed
            // - Use ladders/vines if available
        }

        @Override
        public void stop() {
            obstaclePos = null;
        }

        /**
         * Check if path is blocked by obstacle
         */
        private boolean isPathBlocked() {
            Path path = bot.getNavigation().getPath();
            if (path == null || path.isDone()) {
                return false;
            }

            // Check if stuck (hasn't moved recently)
            // TODO: Implement stuck detection
            return false;
        }
    }

    /**
     * Goal: Guard Position
     *
     * Bot stays at a position and defends it.
     */
    public static class GuardPositionGoal extends Goal {
        private final BotEntity bot;
        private final float guardRadius;
        private int lookAroundCooldown;

        public GuardPositionGoal(BotEntity bot, float guardRadius) {
            this.bot = bot;
            this.guardRadius = guardRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return bot.getHomePosition() != null;
        }

        @Override
        public void start() {
            bot.setAIState(BotEntity.BotAIState.GUARDING);
            lookAroundCooldown = 0;
        }

        @Override
        public void tick() {
            BlockPos home = bot.getHomePosition();
            if (home == null) {
                return;
            }

            // Return to home if too far
            double distanceToHome = bot.distanceToSqr(home.getX(), home.getY(), home.getZ());
            if (distanceToHome > guardRadius * guardRadius) {
                BotMovementHelper.moveToBlockPos(bot, home, BotAIConstants.SPEED_WALK);
            } else {
                BotMovementHelper.stopMovement(bot);

                // Look around randomly
                if (--lookAroundCooldown <= 0) {
                    lookAroundCooldown = 60; // Look around every 3 seconds

                    // Use BotLookHelper to look at a random position
                    BotLookHelper.lookAtRandomTarget(bot);
                }
            }
        }

        @Override
        public void stop() {
            BotMovementHelper.stopMovement(bot);
        }
    }

    /**
     * Goal: Flee from Danger
     *
     * Bot retreats when health is low or overwhelmed.
     */
    public static class FleeGoal extends Goal {
        private final BotEntity bot;
        private BlockPos fleeTarget;

        public FleeGoal(BotEntity bot) {
            this.bot = bot;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Flee if health is low
            return bot.getHealth() < bot.getMaxHealth() * 0.3f;
        }

        @Override
        public void start() {
            bot.setAIState(BotEntity.BotAIState.FLEEING);
            calculateFleeDirection();
        }

        @Override
        public void tick() {
            if (fleeTarget != null) {
                BotMovementHelper.moveToBlockPos(bot, fleeTarget, BotAIConstants.SPEED_SPRINT);
            }
        }

        @Override
        public void stop() {
            fleeTarget = null;
            BotMovementHelper.stopMovement(bot);
        }

        /**
         * Calculate safe direction to flee
         */
        private void calculateFleeDirection() {
            // TODO: Implement flee direction calculation
            // - Find threats
            // - Move away from threats
            // - Seek cover if possible
        }
    }
}
