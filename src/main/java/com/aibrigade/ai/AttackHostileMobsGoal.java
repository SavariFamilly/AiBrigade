package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.utils.EntityValidator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;

/**
 * Goal for bots to attack hostile mobs (zombies, skeletons, etc.)
 * - Only non-static bots can attack
 * - Always attacks hostile mobs (doesn't depend on bot.isHostile())
 * - Ignores team relationships (mobs are always enemies)
 */
public class AttackHostileMobsGoal extends NearestAttackableTargetGoal<Monster> {
    private final BotEntity bot;

    public AttackHostileMobsGoal(BotEntity bot) {
        // targetClass, randomInterval, mustSee, mustReach, targetPredicate
        super(bot, Monster.class, 10, true, false,
            (target) -> target != null && target.isAlive());
        this.bot = bot;
    }

    /**
     * Can use this goal if:
     * - Bot is not static (non-static bots can attack)
     * - Bot is alive
     * - There's a valid monster target nearby
     */
    @Override
    public boolean canUse() {
        // Only non-static bots can attack mobs
        if (!EntityValidator.isBotAIReady(bot)) {
            return false;
        }

        // Use parent logic to find nearest monster
        return super.canUse();
    }

    /**
     * Continue using this goal if bot is still non-static
     */
    @Override
    public boolean canContinueToUse() {
        // Stop if bot became static
        if (!EntityValidator.isBotAIReady(bot)) {
            return false;
        }

        return super.canContinueToUse();
    }
}
