package com.aibrigade.bots;

import net.minecraft.world.entity.AnimationState;
import java.util.HashMap;
import java.util.Map;

/**
 * BotAnimationHandler - Internal animation system for bot entities
 *
 * This class provides animation management without requiring GeckoLib.
 * Uses Minecraft's built-in animation system for smooth transitions.
 *
 * Supported animations:
 * - idle: Standing still
 * - walk: Walking/running
 * - attack: Attacking animation
 * - jump: Jumping animation
 * - damage: Taking damage
 * - climb: Climbing blocks
 * - swim: Swimming animation
 * - crouch: Crouching/sneaking
 */
public class BotAnimationHandler {

    /**
     * Animation types
     */
    public enum Animation {
        IDLE("idle", 20),
        WALK("walk", 20),
        RUN("run", 20),
        ATTACK("attack", 10),
        JUMP("jump", 10),
        DAMAGE("damage", 10),
        CLIMB("climb", 20),
        SWIM("swim", 20),
        CROUCH("crouch", 20),
        DEATH("death", 20);

        private final String name;
        private final int duration; // Duration in ticks

        Animation(String name, int duration) {
            this.name = name;
            this.duration = duration;
        }

        public String getName() {
            return name;
        }

        public int getDuration() {
            return duration;
        }
    }

    /**
     * Animation state tracker
     */
    public static class AnimationStateTracker {
        private Animation currentAnimation = Animation.IDLE;
        private int animationTick = 0;
        private boolean isLooping = true;
        private final Map<Animation, AnimationState> animationStates = new HashMap<>();

        public AnimationStateTracker() {
            // Initialize animation states for each animation type
            for (Animation anim : Animation.values()) {
                animationStates.put(anim, new AnimationState());
            }
        }

        /**
         * Update animation state
         */
        public void tick(BotEntity bot) {
            // Increment animation tick
            animationTick++;

            // Check if animation should loop or stop
            if (!isLooping && animationTick >= currentAnimation.getDuration()) {
                // Animation finished, return to idle
                setAnimation(Animation.IDLE, true);
            }

            // Auto-detect animations based on bot state
            updateAnimationFromState(bot);
        }

        /**
         * Automatically update animation based on bot state
         */
        private void updateAnimationFromState(BotEntity bot) {
            // Death animation takes priority
            if (!bot.isAlive()) {
                if (currentAnimation != Animation.DEATH) {
                    setAnimation(Animation.DEATH, false);
                }
                return;
            }

            // Damage animation
            if (bot.hurtTime > 0 && currentAnimation != Animation.DAMAGE) {
                setAnimation(Animation.DAMAGE, false);
                return;
            }

            // Swimming animation
            if (bot.isInWater() && currentAnimation != Animation.SWIM) {
                setAnimation(Animation.SWIM, true);
                return;
            }

            // Climbing animation
            if (bot.onClimbable() && currentAnimation != Animation.CLIMB) {
                setAnimation(Animation.CLIMB, true);
                return;
            }

            // Crouching animation
            if (bot.isCrouching() && currentAnimation != Animation.CROUCH) {
                setAnimation(Animation.CROUCH, true);
                return;
            }

            // Attack animation
            if (bot.isAggressive() && bot.getTarget() != null) {
                double distanceToTarget = bot.distanceToSqr(bot.getTarget());
                if (distanceToTarget < 4.0 && currentAnimation != Animation.ATTACK) {
                    setAnimation(Animation.ATTACK, false);
                    return;
                }
            }

            // Jump animation
            if (!bot.onGround() && bot.getDeltaMovement().y > 0 && currentAnimation != Animation.JUMP) {
                setAnimation(Animation.JUMP, false);
                return;
            }

            // Movement animations
            if (bot.walkAnimation.isMoving()) {
                // Running if sprinting
                if (bot.isSprinting()) {
                    if (currentAnimation != Animation.RUN) {
                        setAnimation(Animation.RUN, true);
                    }
                }
                // Walking
                else if (currentAnimation != Animation.WALK) {
                    setAnimation(Animation.WALK, true);
                }
            }
            // Idle if not moving
            else if (currentAnimation != Animation.IDLE &&
                     currentAnimation != Animation.ATTACK &&
                     currentAnimation != Animation.DAMAGE) {
                setAnimation(Animation.IDLE, true);
            }
        }

        /**
         * Set the current animation
         *
         * @param animation The animation to play
         * @param loop Whether the animation should loop
         */
        public void setAnimation(Animation animation, boolean loop) {
            if (currentAnimation != animation) {
                // Stop previous animation
                animationStates.get(currentAnimation).stop();

                // Start new animation
                currentAnimation = animation;
                animationTick = 0;
                isLooping = loop;

                // Start the animation state
                animationStates.get(animation).start(0);
            }
        }

        /**
         * Force set animation (even if already playing)
         */
        public void forceAnimation(Animation animation, boolean loop) {
            animationStates.get(currentAnimation).stop();
            currentAnimation = animation;
            animationTick = 0;
            isLooping = loop;
            animationStates.get(animation).start(0);
        }

        /**
         * Get current animation
         */
        public Animation getCurrentAnimation() {
            return currentAnimation;
        }

        /**
         * Get animation tick
         */
        public int getAnimationTick() {
            return animationTick;
        }

        /**
         * Check if animation is looping
         */
        public boolean isLooping() {
            return isLooping;
        }

        /**
         * Get animation progress (0.0 to 1.0)
         */
        public float getAnimationProgress() {
            if (currentAnimation.getDuration() == 0) {
                return 1.0f;
            }
            return Math.min(1.0f, (float) animationTick / currentAnimation.getDuration());
        }

        /**
         * Check if animation is finished
         */
        public boolean isAnimationFinished() {
            return !isLooping && animationTick >= currentAnimation.getDuration();
        }

        /**
         * Get AnimationState for a specific animation
         */
        public AnimationState getAnimationState(Animation animation) {
            return animationStates.get(animation);
        }

        /**
         * Reset all animations
         */
        public void reset() {
            for (AnimationState state : animationStates.values()) {
                state.stop();
            }
            currentAnimation = Animation.IDLE;
            animationTick = 0;
            isLooping = true;
        }
    }

    /**
     * Animation blend weights for smooth transitions
     */
    public static class AnimationBlender {
        private Animation previousAnimation = Animation.IDLE;
        private Animation targetAnimation = Animation.IDLE;
        private float blendProgress = 1.0f;
        private final float blendSpeed = 0.1f;

        /**
         * Update blend between animations
         */
        public void tick() {
            if (blendProgress < 1.0f) {
                blendProgress = Math.min(1.0f, blendProgress + blendSpeed);
            }
        }

        /**
         * Start blending to a new animation
         */
        public void blendTo(Animation newAnimation) {
            if (targetAnimation != newAnimation) {
                previousAnimation = targetAnimation;
                targetAnimation = newAnimation;
                blendProgress = 0.0f;
            }
        }

        /**
         * Get blend weight for previous animation
         */
        public float getPreviousWeight() {
            return 1.0f - blendProgress;
        }

        /**
         * Get blend weight for target animation
         */
        public float getTargetWeight() {
            return blendProgress;
        }

        /**
         * Check if blend is complete
         */
        public boolean isBlendComplete() {
            return blendProgress >= 1.0f;
        }

        /**
         * Get current target animation
         */
        public Animation getTargetAnimation() {
            return targetAnimation;
        }
    }

    /**
     * Animation controller that combines state tracking and blending
     */
    public static class AnimationController {
        private final AnimationStateTracker stateTracker;
        private final AnimationBlender blender;

        public AnimationController() {
            this.stateTracker = new AnimationStateTracker();
            this.blender = new AnimationBlender();
        }

        /**
         * Update animation controller
         */
        public void tick(BotEntity bot) {
            stateTracker.tick(bot);
            blender.tick();

            // Update blender target if animation changed
            if (stateTracker.getCurrentAnimation() != blender.getTargetAnimation()) {
                blender.blendTo(stateTracker.getCurrentAnimation());
            }
        }

        /**
         * Get state tracker
         */
        public AnimationStateTracker getStateTracker() {
            return stateTracker;
        }

        /**
         * Get blender
         */
        public AnimationBlender getBlender() {
            return blender;
        }

        /**
         * Manually trigger an animation
         */
        public void playAnimation(Animation animation, boolean loop) {
            stateTracker.setAnimation(animation, loop);
        }

        /**
         * Force play an animation
         */
        public void forcePlayAnimation(Animation animation, boolean loop) {
            stateTracker.forceAnimation(animation, loop);
        }

        /**
         * Reset controller
         */
        public void reset() {
            stateTracker.reset();
            blender.blendTo(Animation.IDLE);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get animation duration in ticks
     */
    public static int getAnimationDuration(Animation animation) {
        return animation.getDuration();
    }

    /**
     * Get animation duration in seconds
     */
    public static float getAnimationDurationSeconds(Animation animation) {
        return animation.getDuration() / 20.0f;
    }

    /**
     * Check if animation should loop by default
     */
    public static boolean shouldLoopByDefault(Animation animation) {
        switch (animation) {
            case IDLE:
            case WALK:
            case RUN:
            case CLIMB:
            case SWIM:
            case CROUCH:
                return true;
            case ATTACK:
            case JUMP:
            case DAMAGE:
            case DEATH:
                return false;
            default:
                return true;
        }
    }

    /**
     * Create an animation controller for a bot
     */
    public static AnimationController createController() {
        return new AnimationController();
    }
}
