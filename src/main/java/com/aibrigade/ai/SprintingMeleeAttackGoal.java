package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Random;

/**
 * SprintingMeleeAttackGoal - Melee attack avec sprint et sauts (comme un joueur en PvP)
 *
 * Comportement:
 * - Sprint pendant l'attaque pour rattraper la cible
 * - Saute pendant le sprint pour aller plus vite
 * - Désactive le sprint quand l'attaque s'arrête
 */
public class SprintingMeleeAttackGoal extends MeleeAttackGoal {

    private final BotEntity bot;
    private final Random random;
    private int jumpCooldown;

    public SprintingMeleeAttackGoal(BotEntity bot, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(bot, speedModifier, followingTargetEvenIfNotSeen);
        this.bot = bot;
        this.random = new Random(bot.getUUID().getMostSignificantBits());
        this.jumpCooldown = 0;
    }

    @Override
    public void start() {
        super.start();
        // Activer le sprint pour rattraper la cible (comme un joueur)
        bot.setSprinting(true);
    }

    @Override
    public void tick() {
        super.tick();

        // Sprint-jump pendant le combat (comme les joueurs en PvP)
        if (bot.isSprinting() && bot.onGround()) {
            jumpCooldown--;
            if (jumpCooldown <= 0) {
                // Vérifier que le bot se déplace
                if (bot.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                    bot.performJump(); // COMPILATION FIX: Use public wrapper instead of protected jumpFromGround()
                    jumpCooldown = 8 + random.nextInt(5); // Sauter toutes les 8-12 ticks (~0.4-0.6s)
                }
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        // Désactiver le sprint quand l'attaque s'arrête
        // SAUF si le bot est en train de suivre un leader (pour éviter les conflits)
        if (!bot.isFollowingLeader()) {
            bot.setSprinting(false);
        }
    }
}
