package com.aibrigade.utils;

import com.aibrigade.bots.BotEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for bot look control
 * Centralizes look/gaze behavior patterns used across AI goals
 */
public class BotLookHelper {

    /**
     * Make bot look at an entity with standard speed
     */
    public static void lookAtEntity(BotEntity bot, LivingEntity target) {
        if (bot == null || target == null) {
            return;
        }
        bot.getLookControl().setLookAt(
            target,
            BotAIConstants.LOOK_YAW_SPEED,
            BotAIConstants.LOOK_PITCH_SPEED
        );
    }

    /**
     * Make bot look at an entity with custom speed
     */
    public static void lookAtEntity(BotEntity bot, LivingEntity target, float yawSpeed, float pitchSpeed) {
        if (bot == null || target == null) {
            return;
        }
        bot.getLookControl().setLookAt(target, yawSpeed, pitchSpeed);
    }

    /**
     * Make bot look at an entity quickly (fast rotation)
     */
    public static void lookAtEntityFast(BotEntity bot, LivingEntity target) {
        if (bot == null || target == null) {
            return;
        }
        bot.getLookControl().setLookAt(
            target,
            BotAIConstants.LOOK_YAW_SPEED_FAST,
            BotAIConstants.LOOK_PITCH_SPEED_FAST
        );
    }

    /**
     * Make bot look at a position with standard speed
     */
    public static void lookAtPosition(BotEntity bot, Vec3 position) {
        if (bot == null || position == null) {
            return;
        }
        bot.getLookControl().setLookAt(
            position.x,
            position.y,
            position.z,
            BotAIConstants.LOOK_YAW_SPEED,
            BotAIConstants.LOOK_PITCH_SPEED
        );
    }

    /**
     * Make bot look at a position with custom speed
     */
    public static void lookAtPosition(BotEntity bot, Vec3 position, float yawSpeed, float pitchSpeed) {
        if (bot == null || position == null) {
            return;
        }
        bot.getLookControl().setLookAt(position.x, position.y, position.z, yawSpeed, pitchSpeed);
    }

    /**
     * Make bot look at entity using max head rotation
     */
    public static void lookAtEntityWithMaxRotation(BotEntity bot, LivingEntity target) {
        if (bot == null || target == null) {
            return;
        }
        bot.getLookControl().setLookAt(
            target,
            BotAIConstants.LOOK_YAW_SPEED,
            bot.getMaxHeadXRot()
        );
    }

    /**
     * Generate a random look target in front of the bot
     * Used for idle look-around behavior
     */
    public static Vec3 getRandomLookTarget(BotEntity bot) {
        if (bot == null) {
            return Vec3.ZERO;
        }

        // Get bot's current yaw and add random offset
        double angle = bot.getYRot() * Math.PI / 180.0;
        double angleOffset = (bot.getRandom().nextDouble() - 0.5) * Math.PI;
        double finalAngle = angle + angleOffset;

        // Random distance
        double distance = BotAIConstants.LOOK_DISTANCE_MIN +
                         bot.getRandom().nextDouble() * BotAIConstants.LOOK_DISTANCE_RANGE;

        // Calculate position
        double x = bot.getX() + Math.cos(finalAngle) * distance;
        double y = bot.getEyePosition().y +
                  (bot.getRandom().nextDouble() - 0.5) * BotAIConstants.LOOK_VERTICAL_OFFSET;
        double z = bot.getZ() + Math.sin(finalAngle) * distance;

        return new Vec3(x, y, z);
    }

    /**
     * Make bot look at a random target (idle behavior)
     */
    public static void lookAtRandomTarget(BotEntity bot) {
        if (bot == null) {
            return;
        }
        Vec3 randomTarget = getRandomLookTarget(bot);
        lookAtPosition(bot, randomTarget);
    }
}
