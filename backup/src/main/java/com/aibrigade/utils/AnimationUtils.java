package com.aibrigade.utils;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.animations.BotAnimationHandler;

/**
 * AnimationUtils - Utility methods for bot animations
 *
 * Provides helper methods for:
 * - Synchronizing animations with AI states
 * - Smooth animation transitions
 * - Animation timing calculations
 * - Group animation coordination
 */
public class AnimationUtils {

    /**
     * Synchronize bot animation with current action
     *
     * @param bot The bot entity
     */
    public static void syncAnimationWithAction(BotEntity bot) {
        if (!BotAnimationHandler.isAnimationSupported()) {
            return;
        }

        BotAnimationHandler.updateAnimationForState(bot);
    }

    /**
     * Calculate animation speed based on movement speed
     *
     * @param bot The bot entity
     * @return Animation speed multiplier
     */
    public static float calculateAnimationSpeed(BotEntity bot) {
        double movementSpeed = bot.getDeltaMovement().horizontalDistance();
        float baseSpeed = 1.0f;

        if (bot.isSprinting()) {
            baseSpeed = 1.5f;
        } else if (bot.isCrouching()) {
            baseSpeed = 0.7f;
        }

        // Scale with movement speed
        return baseSpeed * (float) (1.0 + movementSpeed);
    }

    /**
     * Apply animation to bot based on action
     *
     * @param bot The bot entity
     * @param action Action type
     */
    public static void applyActionAnimation(BotEntity bot, ActionType action) {
        if (!BotAnimationHandler.isAnimationSupported()) {
            return;
        }

        switch (action) {
            case ATTACK:
                BotAnimationHandler.playAttackAnimation(bot);
                break;

            case DAMAGED:
                BotAnimationHandler.playDamageAnimation(bot);
                break;

            case JUMP:
                BotAnimationHandler.playAnimation(bot, BotAnimationHandler.ANIM_JUMP, false);
                break;

            case CLIMB:
                BotAnimationHandler.playAnimation(bot, BotAnimationHandler.ANIM_CLIMB, true);
                break;

            case SWIM:
                BotAnimationHandler.playAnimation(bot, BotAnimationHandler.ANIM_SWIM, true);
                break;

            default:
                syncAnimationWithAction(bot);
                break;
        }
    }

    /**
     * Coordinate group animations
     * Synchronizes animations across multiple bots
     *
     * @param bots Array of bots
     * @param animation Animation to play
     * @param stagger Stagger delay in ticks
     */
    public static void coordinateGroupAnimation(BotEntity[] bots, String animation, int stagger) {
        if (!BotAnimationHandler.isAnimationSupported()) {
            return;
        }

        for (int i = 0; i < bots.length; i++) {
            final int index = i;
            final BotEntity bot = bots[i];

            // Schedule animation with stagger
            // TODO: Implement proper scheduling
            BotAnimationHandler.playAnimation(bot, animation, false);
        }
    }

    /**
     * Action types for animations
     */
    public enum ActionType {
        ATTACK,
        DAMAGED,
        JUMP,
        CLIMB,
        SWIM,
        IDLE
    }
}
