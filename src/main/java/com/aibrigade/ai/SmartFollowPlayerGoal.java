package com.aibrigade.ai;

import com.aibrigade.bots.BotBehaviorConfig;
import com.aibrigade.bots.BotEntity;
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
    private final float teleportDistance = 50.0F;

    // Pour le calcul de vitesse du joueur
    private Vec3 lastPlayerPos;
    private int speedCheckTicks = 0;
    private double currentSpeedMultiplier = 1.0;

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
        double distance = this.bot.distanceToSqr(this.targetPlayer);
        float minDist = config.getFollowDistance();

        if (distance < (double)(minDist * minDist)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
            return false;
        }

        if (config.getFollowMode() != BotBehaviorConfig.FollowMode.PLAYER) {
            return false;
        }

        double distance = this.bot.distanceToSqr(this.targetPlayer);
        float maxDist = config.getOperationRadius();

        // Continue tant qu'on est dans le rayon d'opération
        return distance <= (double)(maxDist * maxDist);
    }

    @Override
    public void start() {
        this.recheckTime = 0;
        this.lastPlayerPos = this.targetPlayer.position();
        this.speedCheckTicks = 0;
        this.currentSpeedMultiplier = 1.0;
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.bot.getNavigation().stop();
        this.lastPlayerPos = null;
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) {
            return;
        }

        // Regarder le joueur
        this.bot.getLookControl().setLookAt(
            this.targetPlayer,
            10.0F,
            (float)this.bot.getMaxHeadXRot()
        );

        double distance = this.bot.distanceToSqr(this.targetPlayer);

        // Téléportation si trop loin
        if (distance > (double)(this.teleportDistance * this.teleportDistance)) {
            if (this.targetPlayer.level() == this.bot.level()) {
                teleportNearPlayer();
                return;
            }
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
            if (distance <= (double)(minDist * minDist)) {
                this.bot.getNavigation().stop();
                return;
            }

            // Vérifier les obstacles et ajuster le pathfinding
            checkAndHandleObstacles();

            // Pathfind vers le joueur avec la vitesse adaptée
            this.bot.getNavigation().moveTo(
                this.targetPlayer,
                this.currentSpeedMultiplier
            );
        }

        // Gestion du saut pour obstacles
        if (config.canJumpObstacles() && shouldJump()) {
            this.bot.getJumpControl().jump();
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
            this.currentSpeedMultiplier = 1.5;
        } else if (distanceMoved > 0.15) {
            // Joueur marche vite
            this.currentSpeedMultiplier = 1.2;
        } else if (distanceMoved > 0.05) {
            // Joueur marche
            this.currentSpeedMultiplier = 1.0;
        } else {
            // Joueur arrêté ou marche lentement
            this.currentSpeedMultiplier = 0.8;
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

        BlockState frontBlock = this.bot.level().getBlockState(frontPos);

        // Sauter si bloc solide devant et air au-dessus
        if (!frontBlock.isAir() && frontBlock.isSolid()) {
            BlockPos abovePos = frontPos.above();
            BlockState aboveBlock = this.bot.level().getBlockState(abovePos);

            if (aboveBlock.isAir()) {
                return true;
            }
        }

        // Sauter si le joueur est plus haut
        if (this.targetPlayer != null) {
            double heightDiff = this.targetPlayer.getY() - this.bot.getY();
            if (heightDiff > 0.5 && heightDiff < 2.0) {
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
               this.bot.level().getBlockState(targetPos.below()).isAir()) {
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

        // Chercher le joueur avec cet UUID
        for (Player player : this.bot.level().players()) {
            if (player.getUUID().equals(leaderId)) {
                return player;
            }
        }

        return null;
    }
}
