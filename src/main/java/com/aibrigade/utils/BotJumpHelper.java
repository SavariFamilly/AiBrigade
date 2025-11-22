package com.aibrigade.utils;

import com.aibrigade.bots.BotBehaviorConfig;
import com.aibrigade.bots.BotEntity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility class for bot jumping logic
 * Consolidates jump behavior patterns
 */
public class BotJumpHelper {

    /**
     * Make bot jump if configured to do so
     */
    public static void tryJump(BotEntity bot, BotBehaviorConfig config) {
        if (bot == null || config == null) {
            return;
        }

        if (shouldJump(bot, config)) {
            bot.performJump(); // COMPILATION FIX: Use public wrapper instead of protected jumpFromGround()
        }
    }

    /**
     * Make bot jump unconditionally
     */
    public static void jump(BotEntity bot) {
        if (bot == null) {
            return;
        }
        bot.performJump(); // COMPILATION FIX: Use public wrapper instead of protected jumpFromGround()
    }

    /**
     * Check if bot should jump based on configuration
     */
    public static boolean shouldJump(BotEntity bot, BotBehaviorConfig config) {
        if (bot == null || config == null) {
            return false;
        }

        // Only jump if climbing is enabled in config
        if (!config.canClimbBlocks()) {
            return false;
        }

        // Don't jump if already in air
        if (!bot.onGround()) {
            return false;
        }

        return true;
    }

    /**
     * Check if bot should jump to reach a target entity
     * Based on height difference
     */
    public static boolean shouldJumpToReachTarget(BotEntity bot, LivingEntity target) {
        if (bot == null || target == null) {
            return false;
        }

        // Don't jump if not on ground
        if (!bot.onGround()) {
            return false;
        }

        double heightDiff = target.getY() - bot.getY();

        // Jump if target is higher but within jumpable range
        return heightDiff > BotAIConstants.JUMP_HEIGHT_MIN &&
               heightDiff < BotAIConstants.JUMP_HEIGHT_MAX;
    }

    /**
     * Check if height difference requires jumping
     */
    public static boolean isJumpableHeight(double heightDifference) {
        return heightDifference > BotAIConstants.JUMP_HEIGHT_MIN &&
               heightDifference < BotAIConstants.JUMP_HEIGHT_MAX;
    }

    /**
     * Try to jump if target is higher and within jumpable range
     */
    public static void tryJumpToTarget(BotEntity bot, LivingEntity target, BotBehaviorConfig config) {
        if (shouldJumpToReachTarget(bot, target) && config != null && config.canClimbBlocks()) {
            jump(bot);
        }
    }
}
