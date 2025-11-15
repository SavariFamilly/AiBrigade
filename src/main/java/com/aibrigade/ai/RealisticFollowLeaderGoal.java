package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.utils.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;

/**
 * RealisticFollowLeaderGoal - Système de follow simple et fonctionnel
 *
 * Deux modes selon le ratio configuré:
 * - RADIUS_FOLLOWER (5/6): Suit pour rester dans le radius défini autour du leader
 * - CLOSE_FOLLOWER (1/6): Suit très proche du leader (2-4 blocs)
 *
 * Chaque bot a une position unique calculée à partir de son UUID.
 * Pas de système de "chase probability" - c'est simple et direct.
 */
public class RealisticFollowLeaderGoal extends Goal {

    private final BotEntity bot;
    private final double speedModifier;
    private final Random random;

    // Type de suiveur (défini au spawn, ne change jamais)
    private final FollowType followType;

    /**
     * Types de suiveur
     */
    public enum FollowType {
        RADIUS_FOLLOWER,    // 5/6 - Suit dans le radius
        CLOSE_FOLLOWER      // 1/6 - Suit très proche
    }

    // Position cible
    private Vec3 targetPosition;
    private int recalculateTimer;

    // Variation de vitesse
    private double currentSpeedMultiplier;
    private int speedChangeTimer;

    public RealisticFollowLeaderGoal(BotEntity bot, double speed) {
        this.bot = bot;
        this.speedModifier = speed;
        this.random = new Random(bot.getUUID().getMostSignificantBits());

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));

        // Déterminer le type DÉFINITIVEMENT (1/6 vs 5/6)
        // Utilisez l'UUID pour que ce soit consistant
        if (random.nextFloat() < BotAIConstants.ACTIVE_FOLLOW_PROBABILITY) {
            this.followType = FollowType.CLOSE_FOLLOWER;
        } else {
            this.followType = FollowType.RADIUS_FOLLOWER;
        }

        this.currentSpeedMultiplier = 1.0;

        com.aibrigade.main.AIBrigadeMod.LOGGER.info("Bot {} configured as {}",
            bot.getBotName(), followType);
    }

    @Override
    public boolean canUse() {
        // Ne suit pas si statique ou follow désactivé
        if (!EntityValidator.isBotAIReady(bot) || !bot.isFollowingLeader()) {
            return false;
        }

        UUID leaderId = bot.getLeaderId();
        if (leaderId == null) {
            return false;
        }

        // Trouver le leader
        LivingEntity leader = EntityFinder.findEntityByUUID(bot.level(), leaderId, bot.position(), 100.0);
        if (leader == null) {
            return false;
        }

        double distance = DistanceHelper.getDistance(bot, leader);

        // Logic simple selon le type
        if (followType == FollowType.CLOSE_FOLLOWER) {
            // Suit si plus loin que 2 blocs
            return distance > 2.0;
        } else {
            // RADIUS_FOLLOWER: suit si hors du radius
            float radius = bot.getFollowRadius();
            return distance > radius || distance < (radius * 0.5); // Trop loin OU trop proche
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!EntityValidator.isBotAIReady(bot) || !bot.isFollowingLeader()) {
            return false;
        }

        UUID leaderId = bot.getLeaderId();
        if (leaderId == null) {
            return false;
        }

        LivingEntity leader = EntityFinder.findEntityByUUID(bot.level(), leaderId, bot.position(), 100.0);
        if (leader == null) {
            return false;
        }

        double distance = DistanceHelper.getDistance(bot, leader);

        if (followType == FollowType.CLOSE_FOLLOWER) {
            // Continue si pas trop proche
            return distance > 1.5;
        } else {
            // RADIUS_FOLLOWER: continue si pas parfaitement positionné
            float radius = bot.getFollowRadius();
            return distance > radius * 0.9 || distance < radius * 0.6;
        }
    }

    @Override
    public void start() {
        recalculateTimer = 0;
        speedChangeTimer = 0;
        targetPosition = null;
    }

    @Override
    public void tick() {
        UUID leaderId = bot.getLeaderId();
        if (leaderId == null) return;

        LivingEntity leader = EntityFinder.findEntityByUUID(bot.level(), leaderId, bot.position(), 100.0);
        if (leader == null) return;

        double distance = DistanceHelper.getDistance(bot, leader);

        // Variation de vitesse toutes les 3 secondes
        speedChangeTimer--;
        if (speedChangeTimer <= 0) {
            currentSpeedMultiplier = 0.9 + random.nextDouble() * 0.2; // 0.9-1.1x
            speedChangeTimer = 60;
        }

        // Recalculer la position cible toutes les secondes
        recalculateTimer--;
        if (recalculateTimer <= 0 || targetPosition == null) {
            if (followType == FollowType.CLOSE_FOLLOWER) {
                targetPosition = calculateClosePosition(leader);
            } else {
                targetPosition = calculateRadiusPosition(leader);
            }
            recalculateTimer = 20; // 1 seconde
        }

        // Calculer la vitesse
        double finalSpeed = speedModifier * currentSpeedMultiplier;

        // Boost si trop loin
        if (distance > bot.getFollowRadius() * 1.5) {
            finalSpeed *= 1.4;
        } else if (distance > bot.getFollowRadius() * 1.2) {
            finalSpeed *= 1.2;
        }

        // Se déplacer
        BotMovementHelper.moveToPosition(bot, targetPosition, finalSpeed);

        // Regarder le leader
        BotLookHelper.lookAtEntity(bot, leader,
            BotAIConstants.LOOK_YAW_SPEED_FAST,
            BotAIConstants.LOOK_PITCH_SPEED_FAST);
    }

    @Override
    public void stop() {
        BotMovementHelper.stopMovement(bot);
        targetPosition = null;
    }

    /**
     * Calcule une position proche du leader (2-4 blocs)
     */
    private Vec3 calculateClosePosition(LivingEntity leader) {
        Vec3 leaderPos = leader.position();

        // Angle unique basé sur l'UUID (constant pour ce bot)
        double angle = (bot.getUUID().getMostSignificantBits() % 360) * Math.PI / 180.0;

        // Distance 2-3.5 blocs
        double distance = 2.0 + random.nextDouble() * 1.5;

        // Position autour du leader
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        return findGroundPosition(leaderPos.add(offsetX, 0, offsetZ), leaderPos.y);
    }

    /**
     * Calcule une position dans le radius défini
     */
    private Vec3 calculateRadiusPosition(LivingEntity leader) {
        Vec3 leaderPos = leader.position();
        float radius = bot.getFollowRadius();

        // Angle unique basé sur l'UUID (constant pour ce bot)
        double angle = (bot.getUUID().getMostSignificantBits() % 360) * Math.PI / 180.0;

        // Distance dans le radius (70-90% du rayon pour éviter les bords)
        double distance = radius * (0.7 + random.nextDouble() * 0.2);

        // Position dans le radius
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        return findGroundPosition(leaderPos.add(offsetX, 0, offsetZ), leaderPos.y);
    }

    /**
     * Trouve le sol à partir d'une position cible
     */
    private Vec3 findGroundPosition(Vec3 targetPos, double leaderY) {
        Level level = bot.level();
        BlockPos checkPos = new BlockPos((int)targetPos.x, (int)leaderY, (int)targetPos.z);

        // Chercher le sol ±3 blocs
        for (int dy = -3; dy <= 3; dy++) {
            BlockPos pos = checkPos.offset(0, dy, 0);
            if (BlockHelper.isSolidBlock(level, pos) && BlockHelper.isAirBlock(level, pos.above())) {
                return new Vec3(targetPos.x, pos.getY() + 1, targetPos.z);
            }
        }

        // Fallback
        return targetPos;
    }

    /**
     * Obtient le type de suiveur
     */
    public FollowType getFollowType() {
        return followType;
    }
}
