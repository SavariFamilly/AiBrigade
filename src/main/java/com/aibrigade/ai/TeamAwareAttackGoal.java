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
     * Attacks based on group relationship, NOT bot.isHostile() flag
     */
    public static TeamAwareAttackGoal<Player> forPlayer(BotEntity bot) {
        return new TeamAwareAttackGoal<>(bot, Player.class, 10, true, false,
            (target) -> {
                // Only attack based on group relationships
                if (target instanceof Player) {
                    Player player = (Player) target;
                    String botGroup = bot.getGroupId();
                    if (botGroup != null && !botGroup.isEmpty()) {
                        TeamRelationship relationship = AIBrigadeMod.getBotManager()
                            .getPlayerGroupRelationship(player.getUUID(), botGroup);
                        // Attack ONLY if player is hostile to this bot's group
                        return relationship == TeamRelationship.HOSTILE;
                    }
                }
                // No group or no relationship = don't attack
                return false;
            });
    }

    /**
     * Create a team-aware attack goal for targeting other BotEntities
     * Attacks based on group relationship, NOT bot.isHostile() flag
     */
    public static TeamAwareAttackGoal<BotEntity> forBot(BotEntity bot) {
        return new TeamAwareAttackGoal<>(bot, BotEntity.class, 10, true, false,
            (target) -> {
                // Only attack based on group relationships
                if (target instanceof BotEntity) {
                    BotEntity targetBot = (BotEntity) target;
                    String botGroup = bot.getGroupId();
                    String targetGroup = targetBot.getGroupId();

                    if (botGroup != null && !botGroup.isEmpty() &&
                        targetGroup != null && !targetGroup.isEmpty()) {
                        TeamRelationship relationship = AIBrigadeMod.getBotManager()
                            .getTeamRelationship(botGroup, targetGroup);
                        // Attack ONLY if groups are hostile to each other
                        return relationship == TeamRelationship.HOSTILE;
                    }
                }
                // No group or no relationship = don't attack
                return false;
            });
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
     * Only target based on group relationships (not bot.isHostile flag)
     */
    @Override
    public boolean canUse() {
        // Don't attack if bot is static
        if (bot.isStatic()) {
            return false;
        }

        // Use parent logic with team filtering via predicate
        return super.canUse();
    }

    /**
     * Continue targeting based on group relationships
     */
    @Override
    public boolean canContinueToUse() {
        // Stop attacking if bot became static
        if (bot.isStatic()) {
            return false;
        }

        // Stop if relationship changed (no longer hostile)
        if (this.target != null) {
            // Re-check relationship for players
            if (this.target instanceof Player) {
                Player player = (Player) this.target;
                String botGroup = bot.getGroupId();
                if (botGroup != null && !botGroup.isEmpty()) {
                    TeamRelationship relationship = AIBrigadeMod.getBotManager()
                        .getPlayerGroupRelationship(player.getUUID(), botGroup);
                    if (relationship != TeamRelationship.HOSTILE) {
                        return false;
                    }
                }
            }
            // Re-check relationship for bots
            else if (this.target instanceof BotEntity) {
                BotEntity targetBot = (BotEntity) this.target;
                String botGroup = bot.getGroupId();
                String targetGroup = targetBot.getGroupId();
                if (botGroup != null && !botGroup.isEmpty() &&
                    targetGroup != null && !targetGroup.isEmpty()) {
                    TeamRelationship relationship = AIBrigadeMod.getBotManager()
                        .getTeamRelationship(botGroup, targetGroup);
                    if (relationship != TeamRelationship.HOSTILE) {
                        return false;
                    }
                }
            }
        }

        return super.canContinueToUse();
    }
}
