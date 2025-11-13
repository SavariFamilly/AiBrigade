package com.aibrigade.animations;

import com.aibrigade.main.AIBrigadeMod;
import com.aibrigade.bots.BotEntity;

/**
 * BotAnimationHandler - Manages bot animations
 *
 * This class handles animation playback for bot entities using GeckoLib
 * or AnimationAPI/LLibrary depending on what's available.
 *
 * Supported animations:
 * - Idle: Standing still, ambient movements
 * - Walk: Normal walking
 * - Run: Sprinting/running
 * - Jump: Jumping over obstacles
 * - Attack: Melee attack swing
 * - Damaged: Hit/damage reaction
 * - Climb: Climbing blocks/ladders
 * - Swim: Swimming animation
 * - Sneak: Crouching/sneaking
 *
 * Animations are synchronized with bot AI states for realistic behavior.
 */
public class BotAnimationHandler {

    // Animation system availability flags
    private static boolean geckoLibAvailable = false;
    private static boolean animationAPIAvailable = false;

    // Animation names/IDs
    public static final String ANIM_IDLE = "idle";
    public static final String ANIM_WALK = "walk";
    public static final String ANIM_RUN = "run";
    public static final String ANIM_JUMP = "jump";
    public static final String ANIM_ATTACK = "attack";
    public static final String ANIM_DAMAGED = "damaged";
    public static final String ANIM_CLIMB = "climb";
    public static final String ANIM_SWIM = "swim";
    public static final String ANIM_SNEAK = "sneak";

    /**
     * Initialize animation handler
     * Detects available animation libraries
     */
    public static void initialize() {
        // Check for GeckoLib
        try {
            Class.forName("software.bernie.geckolib.core.animatable.GeoAnimatable");
            geckoLibAvailable = true;
            AIBrigadeMod.LOGGER.info("GeckoLib detected - Animations enabled");
        } catch (ClassNotFoundException e) {
            geckoLibAvailable = false;
        }

        // Check for AnimationAPI/LLibrary
        if (!geckoLibAvailable) {
            try {
                Class.forName("net.ilexiconn.llibrary.server.animation.Animation");
                animationAPIAvailable = true;
                AIBrigadeMod.LOGGER.info("AnimationAPI detected - Animations enabled");
            } catch (ClassNotFoundException e) {
                animationAPIAvailable = false;
            }
        }

        if (!geckoLibAvailable && !animationAPIAvailable) {
            AIBrigadeMod.LOGGER.warn("No animation library found - Animations disabled");
        }
    }

    /**
     * Check if any animation library is available
     * @return true if animations are supported
     */
    public static boolean isAnimationSupported() {
        return geckoLibAvailable || animationAPIAvailable;
    }

    /**
     * Play animation on bot
     *
     * @param bot The bot entity
     * @param animationName Name of animation to play
     * @param loop Whether animation should loop
     */
    public static void playAnimation(BotEntity bot, String animationName, boolean loop) {
        if (!isAnimationSupported()) {
            return;
        }

        if (geckoLibAvailable) {
            playGeckoLibAnimation(bot, animationName, loop);
        } else if (animationAPIAvailable) {
            playAnimationAPIAnimation(bot, animationName, loop);
        }
    }

    /**
     * Play animation using GeckoLib
     */
    private static void playGeckoLibAnimation(BotEntity bot, String animationName, boolean loop) {
        // TODO: Implement GeckoLib animation playback
        // Example (when GeckoLib is properly integrated):
        /*
        AnimationController<?> controller = bot.getAnimationController("main");
        if (controller != null) {
            controller.setAnimation(new AnimationBuilder()
                .addAnimation(animationName, loop ? ILoopType.LOOP : ILoopType.PLAY_ONCE));
        }
        */

        AIBrigadeMod.LOGGER.debug("Playing GeckoLib animation '{}' on bot {}",
            animationName, bot.getBotName());
    }

    /**
     * Play animation using AnimationAPI/LLibrary
     */
    private static void playAnimationAPIAnimation(BotEntity bot, String animationName, boolean loop) {
        // TODO: Implement AnimationAPI animation playback
        // Example (when AnimationAPI is properly integrated):
        /*
        IAnimatedEntity animatedBot = (IAnimatedEntity) bot;
        Animation animation = getAnimationByName(animationName);
        if (animation != null) {
            AnimationHandler.INSTANCE.sendAnimationMessage(bot, animation);
        }
        */

        AIBrigadeMod.LOGGER.debug("Playing AnimationAPI animation '{}' on bot {}",
            animationName, bot.getBotName());
    }

    /**
     * Stop current animation
     *
     * @param bot The bot entity
     */
    public static void stopAnimation(BotEntity bot) {
        if (!isAnimationSupported()) {
            return;
        }

        if (geckoLibAvailable) {
            stopGeckoLibAnimation(bot);
        } else if (animationAPIAvailable) {
            stopAnimationAPIAnimation(bot);
        }
    }

    /**
     * Stop GeckoLib animation
     */
    private static void stopGeckoLibAnimation(BotEntity bot) {
        // TODO: Implement GeckoLib animation stop
        AIBrigadeMod.LOGGER.debug("Stopping GeckoLib animation on bot {}", bot.getBotName());
    }

    /**
     * Stop AnimationAPI animation
     */
    private static void stopAnimationAPIAnimation(BotEntity bot) {
        // TODO: Implement AnimationAPI animation stop
        AIBrigadeMod.LOGGER.debug("Stopping AnimationAPI animation on bot {}", bot.getBotName());
    }

    /**
     * Update bot animation based on AI state
     * Called every tick to keep animations synchronized with behavior
     *
     * @param bot The bot entity
     */
    public static void updateAnimationForState(BotEntity bot) {
        if (!isAnimationSupported()) {
            return;
        }

        BotEntity.BotAIState state = bot.getAIState();
        String currentAnimation = getCurrentAnimation(bot);

        // Determine appropriate animation for current state
        String targetAnimation = getAnimationForState(state, bot);

        // Play animation if different from current
        if (!targetAnimation.equals(currentAnimation)) {
            playAnimation(bot, targetAnimation, shouldLoop(targetAnimation));
        }
    }

    /**
     * Get appropriate animation for bot AI state
     *
     * @param state The bot's AI state
     * @param bot The bot entity
     * @return Animation name
     */
    private static String getAnimationForState(BotEntity.BotAIState state, BotEntity bot) {
        // Check for special conditions first
        if (bot.isInWater()) {
            return ANIM_SWIM;
        }

        if (bot.isSprinting()) {
            return ANIM_RUN;
        }

        if (bot.getDeltaMovement().y > 0.1) {
            return ANIM_JUMP;
        }

        // State-based animations
        switch (state) {
            case ATTACKING:
                return ANIM_ATTACK;

            case CLIMBING:
                return ANIM_CLIMB;

            case FOLLOWING:
            case PATROLLING:
                // Check movement speed
                double speed = bot.getDeltaMovement().horizontalDistance();
                if (speed > 0.1) {
                    return bot.isSprinting() ? ANIM_RUN : ANIM_WALK;
                }
                return ANIM_IDLE;

            case FLEEING:
            case DISPERSING:
                return ANIM_RUN;

            case GUARDING:
            case IDLE:
            default:
                return ANIM_IDLE;
        }
    }

    /**
     * Check if animation should loop
     *
     * @param animationName The animation name
     * @return true if should loop
     */
    private static boolean shouldLoop(String animationName) {
        // Attack and damaged animations should play once
        // Others should loop
        return !animationName.equals(ANIM_ATTACK) && !animationName.equals(ANIM_DAMAGED);
    }

    /**
     * Get current playing animation
     *
     * @param bot The bot entity
     * @return Current animation name, or empty string if none
     */
    private static String getCurrentAnimation(BotEntity bot) {
        // TODO: Implement current animation retrieval
        // This would query the animation controller for current animation
        return "";
    }

    /**
     * Play attack animation
     * Special method for attack with proper timing
     *
     * @param bot The bot entity
     */
    public static void playAttackAnimation(BotEntity bot) {
        playAnimation(bot, ANIM_ATTACK, false);

        // TODO: Implement attack timing
        // Schedule return to previous animation after attack completes
    }

    /**
     * Play damage animation
     * Special method for damage reaction
     *
     * @param bot The bot entity
     */
    public static void playDamageAnimation(BotEntity bot) {
        playAnimation(bot, ANIM_DAMAGED, false);

        // TODO: Implement damage reaction timing
        // Brief hit reaction, then return to previous animation
    }

    /**
     * Register bot animations with GeckoLib
     * Called during mod initialization
     */
    public static void registerGeckoLibAnimations() {
        if (!geckoLibAvailable) {
            return;
        }

        // TODO: Implement GeckoLib animation registration
        // Example:
        /*
        GeckoLibUtil.registerAnimatedEntity(BotEntity.class);
        */

        AIBrigadeMod.LOGGER.info("GeckoLib animations registered");
    }

    /**
     * Register bot animations with AnimationAPI
     * Called during mod initialization
     */
    public static void registerAnimationAPIAnimations() {
        if (!animationAPIAvailable) {
            return;
        }

        // TODO: Implement AnimationAPI animation registration
        AIBrigadeMod.LOGGER.info("AnimationAPI animations registered");
    }

    /**
     * Set animation speed multiplier
     * Useful for slow-motion or fast-forward effects
     *
     * @param bot The bot entity
     * @param speedMultiplier Speed multiplier (1.0 = normal)
     */
    public static void setAnimationSpeed(BotEntity bot, float speedMultiplier) {
        if (!isAnimationSupported()) {
            return;
        }

        // TODO: Implement animation speed control
        AIBrigadeMod.LOGGER.debug("Set animation speed for bot {} to {}x",
            bot.getBotName(), speedMultiplier);
    }

    /**
     * Blend between two animations
     * Creates smooth transition
     *
     * @param bot The bot entity
     * @param fromAnimation Starting animation
     * @param toAnimation Target animation
     * @param blendTime Time to blend in ticks
     */
    public static void blendAnimations(BotEntity bot, String fromAnimation,
                                        String toAnimation, int blendTime) {
        if (!isAnimationSupported()) {
            return;
        }

        // TODO: Implement animation blending
        AIBrigadeMod.LOGGER.debug("Blending bot {} animation from {} to {} over {} ticks",
            bot.getBotName(), fromAnimation, toAnimation, blendTime);
    }
}
