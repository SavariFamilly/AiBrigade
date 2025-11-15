package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;

import java.util.EnumSet;

/**
 * StaticBotDefenseGoal - Allows STATIC bots to attack hostile mobs
 *
 * Static bots normally don't move, but this goal allows them to:
 * - Detect nearby hostile mobs
 * - Move ONLY to attack those mobs
 * - Return to their position after combat
 *
 * This makes static bots act as defenders/guards that protect an area
 * without wandering away when there's no threat.
 */
public class StaticBotDefenseGoal extends Goal {

    private final BotEntity bot;
    private LivingEntity target;
    private final double detectionRange;
    private final double attackRange;

    // Movement control
    private int attackCooldown;
    private static final int ATTACK_COOLDOWN_TICKS = 20; // 1 second between attacks

    public StaticBotDefenseGoal(BotEntity bot, double detectionRange) {
        this.bot = bot;
        this.detectionRange = detectionRange;
        this.attackRange = 3.0; // Melee attack range
        this.attackCooldown = 0;

        // This goal controls MOVE and TARGET flags
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // Only works for STATIC bots
        if (!bot.isStatic()) {
            return false;
        }

        // Bot must be alive
        if (!bot.isAlive()) {
            return false;
        }

        // Find nearest hostile mob
        target = bot.level().getNearestEntity(
            Monster.class,
            net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat()
                .range(detectionRange)
                .selector(entity -> entity != null && entity.isAlive()),
            bot,
            bot.getX(),
            bot.getEyeY(),
            bot.getZ(),
            bot.getBoundingBox().inflate(detectionRange, 4.0, detectionRange)
        );

        // Log for debugging (every 5 seconds)
        if (target != null && bot.tickCount % 100 == 0) {
            System.out.println("[StaticDefense] Bot " + bot.getBotName() + " detected hostile: " + target.getName().getString() + " at " + String.format("%.1f", bot.distanceTo(target)) + " blocks");
        } else if (bot.tickCount % 200 == 0) {
            System.out.println("[StaticDefense] Static bot " + bot.getBotName() + " scanning for threats... (none found in " + detectionRange + " block range)");
        }

        // Can use if we found a target
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if target is dead or too far
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Stop if target is too far (outside detection range)
        double distance = bot.distanceTo(target);
        if (distance > detectionRange + 5.0) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        // Set the target for the bot's AI
        bot.setTarget(target);
        attackCooldown = 0;
        System.out.println("[StaticDefense] Bot " + bot.getBotName() + " STARTING attack on " + (target != null ? target.getName().getString() : "null"));
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            return;
        }

        // Look at target
        bot.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distance = bot.distanceTo(target);

        // Move towards target if not in attack range
        if (distance > attackRange) {
            // Move to target at normal speed
            bot.getNavigation().moveTo(target, 1.0);
            if (bot.tickCount % 40 == 0) {
                System.out.println("[StaticDefense] Bot " + bot.getBotName() + " moving to target at distance " + String.format("%.1f", distance));
            }
        } else {
            // Stop moving when in attack range
            bot.getNavigation().stop();

            // Attack if cooldown is ready
            if (attackCooldown <= 0) {
                bot.doHurtTarget(target);
                attackCooldown = ATTACK_COOLDOWN_TICKS;
                System.out.println("[StaticDefense] Bot " + bot.getBotName() + " ATTACKING " + target.getName().getString());
            }
        }

        // Decrease cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }

    @Override
    public void stop() {
        // Clear target
        bot.setTarget(null);
        target = null;

        // Stop navigation
        bot.getNavigation().stop();
    }

    /**
     * This goal has high priority for static bots
     */
    @Override
    public boolean isInterruptable() {
        return true;
    }
}
