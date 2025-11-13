package com.aibrigade.ai;

import com.aibrigade.bots.BotBehaviorConfig;
import com.aibrigade.bots.BotEntity;
import com.aibrigade.utils.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Goal intelligent qui fait suivre le joueur au bot avec:
 * - Adaptation de vitesse (court si le joueur court)
 * - Franchissement d'obstacles (saute, monte des blocs)
 * - Téléportation si trop loin
 * - Respect de la configuration du bot
 */
public class SmartFollowPlayerGoal extends Goal {
    private final BotEntity bot;
    private final BotBehaviorConfig config;
    private Player targetPlayer;
    private int recheckTime;

    // Pour le calcul de vitesse du joueur
    private Vec3 lastPlayerPos;
    private int speedCheckTicks = 0;
    private double currentSpeedMultiplier = BotAIConstants.SPEED_WALK;

    public SmartFollowPlayerGoal(BotEntity bot, BotBehaviorConfig config) {
        this.bot = bot;
        this.config = config;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Vérifier si le mode de suivi est PLAYER
        if (config.getFollowMode() != BotBehaviorConfig.FollowMode.PLAYER) {
            return false;
        }

        // Trouver le joueur leader
        this.targetPlayer = findLeaderPlayer();

        if (this.targetPlayer == null) {
            return false;
        }

        // Ne pas suivre si trop proche
        float minDist = config.getFollowDistance();
        return DistanceHelper.isOutsideDistance(this.bot, this.targetPlayer, minDist);
    }

    @Override
    public boolean canContinueToUse() {
        if (!EntityValidator.isEntityValid(this.targetPlayer)) {
            return false;
        }

        if (config.getFollowMode() != BotBehaviorConfig.FollowMode.PLAYER) {
            return false;
        }

        float maxDist = config.getOperationRadius();

        // Continue tant qu'on est dans le rayon d'opération
        return DistanceHelper.isWithinDistance(this.bot, this.targetPlayer, maxDist);
    }

    @Override
    public void start() {
        this.recheckTime = 0;
        this.lastPlayerPos = this.targetPlayer.position();
        this.speedCheckTicks = 0;
        this.currentSpeedMultiplier = BotAIConstants.SPEED_WALK;
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        BotMovementHelper.stopMovement(this.bot);
        this.lastPlayerPos = null;
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) {
            return;
        }

        // Regarder le joueur
        BotLookHelper.lookAtEntityWithMaxRotation(this.bot, this.targetPlayer);

        // Téléportation si trop loin
        if (BotMovementHelper.teleportIfTooFar(this.bot, this.targetPlayer, BotAIConstants.TELEPORT_DISTANCE)) {
            return;
        }

        // Calculer la vitesse du joueur toutes les 5 ticks
        if (config.shouldMatchPlayerSpeed()) {
            speedCheckTicks++;
            if (speedCheckTicks >= 5) {
                updatePlayerSpeed();
                speedCheckTicks = 0;
            }
        }

        // Recalculer le chemin toutes les 10 ticks
        if (--this.recheckTime <= 0) {
            this.recheckTime = 10;

            // Ne pas pathfind si très proche
            float minDist = config.getFollowDistance();
            if (DistanceHelper.isWithinDistance(this.bot, this.targetPlayer, minDist)) {
                BotMovementHelper.stopMovement(this.bot);
                return;
            }

            // Vérifier les obstacles et ajuster le pathfinding
            checkAndHandleObstacles();

            // Pathfind vers le joueur avec la vitesse adaptée
            BotMovementHelper.moveToEntity(this.bot, this.targetPlayer, this.currentSpeedMultiplier);
        }

        // Gestion du saut pour obstacles
        if (config.canJumpObstacles() && shouldJump()) {
            BotJumpHelper.jump(this.bot);
        }
    }

    /**
     * Calcule la vitesse actuelle du joueur et ajuste le multiplicateur
     */
    private void updatePlayerSpeed() {
        if (this.lastPlayerPos == null) {
            this.lastPlayerPos = this.targetPlayer.position();
            return;
        }

        Vec3 currentPos = this.targetPlayer.position();
        double distanceMoved = this.lastPlayerPos.distanceTo(currentPos);
        this.lastPlayerPos = currentPos;

        // Convertir en multiplicateur de vitesse
        // Joueur qui marche: ~0.1 blocks/tick
        // Joueur qui court: ~0.28 blocks/tick
        // Joueur qui sprint-saute: ~0.4+ blocks/tick

        if (distanceMoved > 0.3) {
            // Joueur court/sprint
            this.currentSpeedMultiplier = BotAIConstants.SPEED_SPRINT;
        } else if (distanceMoved > 0.15) {
            // Joueur marche vite
            this.currentSpeedMultiplier = BotAIConstants.SPEED_RUN;
        } else if (distanceMoved > 0.05) {
            // Joueur marche
            this.currentSpeedMultiplier = BotAIConstants.SPEED_WALK;
        } else {
            // Joueur arrêté ou marche lentement
            this.currentSpeedMultiplier = BotAIConstants.SPEED_SLOW;
        }
    }

    /**
     * Vérifie et gère les obstacles entre le bot et le joueur
     */
    private void checkAndHandleObstacles() {
        if (!config.canClimbBlocks()) {
            return;
        }

        BlockPos botPos = this.bot.blockPosition();
        BlockPos playerPos = this.targetPlayer.blockPosition();

        // Si le joueur est au-dessus
        if (playerPos.getY() > botPos.getY()) {
            int heightDiff = playerPos.getY() - botPos.getY();

            // Si la différence est de 1-2 blocs, essayer de placer des blocs ou sauter
            if (heightDiff <= 2) {
                // Le bot va naturellement sauter via shouldJump()
                // Ici on pourrait ajouter la logique de placement de blocs
                // si le bot a des blocs dans son inventaire
            }

            // Si plus haut, le bot va chercher un chemin naturel
        }
    }

    /**
     * Détermine si le bot doit sauter
     */
    private boolean shouldJump() {
        if (!config.canJumpObstacles()) {
            return false;
        }

        // Vérifier s'il y a un bloc devant
        Vec3 lookVec = this.bot.getLookAngle();
        BlockPos frontPos = this.bot.blockPosition().offset(
            (int)Math.round(lookVec.x),
            0,
            (int)Math.round(lookVec.z)
        );

        // Sauter si bloc solide devant et air au-dessus
        if (BlockHelper.isSolidBlock(this.bot.level(), frontPos)) {
            BlockPos abovePos = frontPos.above();

            if (BlockHelper.isAirBlock(this.bot.level(), abovePos)) {
                return true;
            }
        }

        // Sauter si le joueur est plus haut
        if (this.targetPlayer != null) {
            double heightDiff = this.targetPlayer.getY() - this.bot.getY();
            if (BotJumpHelper.isJumpableHeight(heightDiff)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Téléporte le bot près du joueur
     */
    private void teleportNearPlayer() {
        // Trouver une position sûre près du joueur
        double offsetX = (this.bot.getRandom().nextDouble() - 0.5) * 4.0;
        double offsetZ = (this.bot.getRandom().nextDouble() - 0.5) * 4.0;

        double targetX = this.targetPlayer.getX() + offsetX;
        double targetY = this.targetPlayer.getY();
        double targetZ = this.targetPlayer.getZ() + offsetZ;

        // Vérifier que la position est sûre
        BlockPos targetPos = new BlockPos((int)targetX, (int)targetY, (int)targetZ);

        // S'assurer qu'il y a un sol
        while (targetPos.getY() > this.targetPlayer.getY() - 3 &&
               BlockHelper.isAirBlock(this.bot.level(), targetPos.below())) {
            targetPos = targetPos.below();
        }

        // Téléporter
        this.bot.teleportTo(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        this.recheckTime = 0;
    }

    /**
     * Trouve le joueur leader
     */
    private Player findLeaderPlayer() {
        java.util.UUID leaderId = this.bot.getLeaderId();

        if (leaderId == null) {
            return null;
        }

        // Utiliser EntityFinder pour trouver le joueur
        return EntityFinder.findPlayerByUUID(this.bot.level(), leaderId);
    }
}
