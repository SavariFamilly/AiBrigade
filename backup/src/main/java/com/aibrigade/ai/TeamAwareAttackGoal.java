package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.TeamRelationship;
import com.aibrigade.main.AIBrigadeMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * Custom target goal that prevents bots from attacking members of their own team.
 * This extends NearestAttackableTargetGoal to add team-awareness.
 */
public class TeamAwareAttackGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    private final BotEntity bot;

    /**
     * Create a team-aware attack goal for targeting Players
     */
    public static TeamAwareAttackGoal<Player> forPlayer(BotEntity bot) {
        return new TeamAwareAttackGoal<>(bot, Player.class, 10, true, false,
            (target) -> {
                // Check if this specific player is hostile to the bot's group
                if (target instanceof Player) {
                    Player player = (Player) target;
                    String botGroup = bot.getGroupId();
                    if (botGroup != null && !botGroup.isEmpty()) {
                        TeamRelationship relationship = AIBrigadeMod.getBotManager()
                            .getPlayerGroupRelationship(player.getUUID(), botGroup);
                        // Attack if player is hostile to this bot's group
                        if (relationship == TeamRelationship.HOSTILE) {
                            return true;
                        }
                    }
                }
                // Otherwise use standard hostile check
                return bot.isHostile() && !isSameTeam(bot, target);
            });
    }

    /**
     * Create a team-aware attack goal for targeting other BotEntities
     */
    public static TeamAwareAttackGoal<BotEntity> forBot(BotEntity bot) {
        return new TeamAwareAttackGoal<>(bot, BotEntity.class, 10, true, false,
            (target) -> bot.isHostile() && !isSameTeam(bot, target));
    }

    /**
     * Private constructor - use factory methods forPlayer() or forBot()
     */
    private TeamAwareAttackGoal(BotEntity bot, Class<T> targetClass, int randomInterval,
                                boolean mustSee, boolean mustReach, Predicate<LivingEntity> targetPredicate) {
        super(bot, targetClass, randomInterval, mustSee, mustReach, targetPredicate);
        this.bot = bot;
    }

    /**
     * Check if two entities are on the same team or have friendly relationship
     */
    private static boolean isSameTeam(BotEntity bot, LivingEntity target) {
        // Get the bot's group ID
        String botGroup = bot.getGroupId();

        if (botGroup == null || botGroup.isEmpty()) {
            // Bot has no group, can attack anyone (if hostile)
            return false;
        }

        // If target is another bot, check group relationship
        if (target instanceof BotEntity) {
            BotEntity targetBot = (BotEntity) target;
            String targetGroup = targetBot.getGroupId();

            if (targetGroup != null && !targetGroup.isEmpty()) {
                // Check relationship between the two groups using BotManager
                TeamRelationship relationship = AIBrigadeMod.getBotManager().getTeamRelationship(botGroup, targetGroup);

                // Don't attack if ALLIED or NEUTRAL, only attack if HOSTILE
                if (relationship == TeamRelationship.ALLIED || relationship == TeamRelationship.NEUTRAL) {
                    return true; // Treat as same team (don't attack)
                }

                // HOSTILE relationship - can attack
                return false;
            }
        }

        // If target is a player, check relationships
        if (target instanceof Player) {
            Player player = (Player) target;
            java.util.UUID leaderId = bot.getLeaderId();

            // Don't attack your own leader
            if (leaderId != null && player.getUUID().equals(leaderId)) {
                return true;
            }

            // Check player-group relationship
            TeamRelationship playerRelationship = AIBrigadeMod.getBotManager().getPlayerGroupRelationship(player.getUUID(), botGroup);

            // Don't attack if ALLIED or NEUTRAL, only attack if HOSTILE
            if (playerRelationship == TeamRelationship.ALLIED || playerRelationship == TeamRelationship.NEUTRAL) {
                return true; // Treat as same team (don't attack)
            }

            // HOSTILE relationship - can attack
            return false;
        }

        // Not on same team
        return false;
    }

    /**
     * Only target if hostile mode is enabled and not same team
     */
    @Override
    public boolean canUse() {
        // Only attack if hostile mode is enabled
        if (!bot.isHostile()) {
            return false;
        }

        // Use parent logic with team filtering
        return super.canUse();
    }

    /**
     * Continue targeting only if still hostile and target is valid
     */
    @Override
    public boolean canContinueToUse() {
        // Stop attacking if hostile mode is disabled
        if (!bot.isHostile()) {
            return false;
        }

        // Check if target became a teammate (group changed)
        if (this.target != null && isSameTeam(bot, this.target)) {
            return false;
        }

        return super.canContinueToUse();
    }
}
