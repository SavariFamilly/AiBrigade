package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.control.JumpControl;

import java.util.EnumSet;
import java.util.Random;

/**
 * RandomJumpGoal - Makes bots jump at random intervals or continuously
 *
 * Behavior:
 * - Random jumps: Every 2-30 minutes (randomly chosen each time)
 * - Forced jumps: Continuous jumping when enabled via command
 * - Respects bot's movement state (only jumps if on ground)
 * - Can be toggled on/off per bot or group
 */
public class RandomJumpGoal extends Goal {

    private final BotEntity bot;
    private final Random random;

    // Random jump configuration
    private static final int MIN_JUMP_INTERVAL = 20 * 60 * 2;    // 2 minutes in ticks
    private static final int MAX_JUMP_INTERVAL = 20 * 60 * 30;   // 30 minutes in ticks

    // Timers
    private int nextJumpTimer;
    private int forcedJumpCooldown;  // Cooldown between forced jumps (to avoid spam)

    public RandomJumpGoal(BotEntity bot) {
        this.bot = bot;
        this.random = new Random(bot.getUUID().getMostSignificantBits());
        this.setFlags(EnumSet.of(Goal.Flag.JUMP));

        // Initialize with random interval
        this.nextJumpTimer = calculateNextJumpInterval();
        this.forcedJumpCooldown = 0;
    }

    @Override
    public boolean canUse() {
        // Always active to handle both random and forced jumps
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public void tick() {
        // Decrease cooldown
        if (forcedJumpCooldown > 0) {
            forcedJumpCooldown--;
        }

        // Check if forced jumping is enabled
        if (bot.isForcedJumping()) {
            // Forced jumping: jump every 10 ticks (0.5 seconds) when on ground
            if (forcedJumpCooldown <= 0 && bot.onGround()) {
                performJump();
                forcedJumpCooldown = 10; // 0.5 second cooldown
            }
        } else {
            // Random jumping
            nextJumpTimer--;

            if (nextJumpTimer <= 0 && bot.onGround()) {
                performJump();
                // Set next random interval
                nextJumpTimer = calculateNextJumpInterval();
            }
        }
    }

    /**
     * Make the bot jump
     */
    private void performJump() {
        // Use the jump control to make the bot jump
        bot.getJumpControl().jump();
    }

    /**
     * Calculate a random interval between MIN and MAX
     */
    private int calculateNextJumpInterval() {
        return MIN_JUMP_INTERVAL + random.nextInt(MAX_JUMP_INTERVAL - MIN_JUMP_INTERVAL);
    }

    /**
     * Reset the jump timer (useful when toggling forced jumping)
     */
    public void resetTimer() {
        this.nextJumpTimer = calculateNextJumpInterval();
    }
}
