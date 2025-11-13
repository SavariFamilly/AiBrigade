package com.aibrigade.utils;

import com.aibrigade.bots.BotEntity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Utility class for validating entities and their states
 * Consolidates repeated null checks and validity verification
 */
public class EntityValidator {

    /**
     * Check if an entity is valid and alive
     */
    public static boolean isEntityValid(@Nullable LivingEntity entity) {
        return entity != null && entity.isAlive();
    }

    /**
     * Check if a bot can follow a leader
     * Validates both bot and leader states
     */
    public static boolean canBotFollowLeader(@Nullable BotEntity bot, @Nullable LivingEntity leader) {
        if (bot == null || leader == null) {
            return false;
        }

        // Bot must not be static
        if (bot.isStatic()) {
            return false;
        }

        // Bot must be set to follow
        if (!bot.isFollowingLeader()) {
            return false;
        }

        // Leader must be alive
        if (!leader.isAlive()) {
            return false;
        }

        return true;
    }

    /**
     * Check if a bot is in a valid state to execute AI
     */
    public static boolean isBotAIReady(@Nullable BotEntity bot) {
        if (bot == null || !bot.isAlive()) {
            return false;
        }

        // Static bots don't execute AI
        if (bot.isStatic()) {
            return false;
        }

        return true;
    }

    /**
     * Check if an entity is a valid attack target
     */
    public static boolean isValidAttackTarget(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        if (!entity.isAlive()) {
            return false;
        }

        // Can't attack invulnerable entities
        if (entity.isInvulnerable()) {
            return false;
        }

        return true;
    }

    /**
     * Check if a bot has a valid leader assigned
     */
    public static boolean hasValidLeader(@Nullable BotEntity bot) {
        if (bot == null) {
            return false;
        }

        if (!bot.isFollowingLeader()) {
            return false;
        }

        if (bot.getLeaderId() == null) {
            return false;
        }

        return true;
    }

    /**
     * Validate that leader exists and matches the bot's leader ID
     */
    public static boolean isCorrectLeader(@Nullable BotEntity bot, @Nullable LivingEntity leader) {
        if (bot == null || leader == null) {
            return false;
        }

        if (!bot.isFollowingLeader()) {
            return false;
        }

        if (bot.getLeaderId() == null) {
            return false;
        }

        return bot.getLeaderId().equals(leader.getUUID());
    }
}
