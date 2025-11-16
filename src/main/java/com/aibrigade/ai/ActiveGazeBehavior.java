package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.persistence.BotDatabase;
import com.aibrigade.utils.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;

/**
 * ActiveGazeBehavior - Système de regard actif pour les bots statiques
 *
 * Comportement:
 * - Par défaut: regarde vers le leader
 * - 2/6 des bots (33%) regardent ailleurs de temps en temps
 * - Mouvement fluide et non mécanique
 * - Retour au leader après avoir scanné les alentours
 *
 * Ce comportement rend les bots plus vivants et attentifs à leur environnement
 */
public class ActiveGazeBehavior extends Goal {

    private final BotEntity bot;
    private final Random random;

    // Configuration
    private float lookAroundChance;      // Probabilité de regarder ailleurs (0.33 = 2/6)
    private boolean isLookingAround;     // En train de regarder ailleurs?

    // Timing
    private int lookAroundInterval;      // Intervalle entre les checks (en ticks)
    private int lookAroundTimer;         // Timer actuel
    private int lookAroundDuration;      // Durée du look around actuel
    private int lookAroundDurationTimer;

    // Direction de regard
    private Vec3 lookTarget;             // Cible de regard actuelle
    private float targetYaw;
    private float targetPitch;
    private int lookTargetChangeTimer;

    // États
    private enum GazeState {
        LOOKING_AT_LEADER,   // Regarde le leader
        SCANNING_AROUND,     // Scanne les alentours
        RETURNING_TO_LEADER  // Retourne regarder le leader
    }
    private GazeState gazeState;

    public ActiveGazeBehavior(BotEntity bot) {
        this.bot = bot;
        this.random = new Random(bot.getUUID().getLeastSignificantBits());

        this.setFlags(EnumSet.of(Goal.Flag.LOOK));

        // Configuration par défaut
        this.lookAroundChance = BotAIConstants.LOOK_AROUND_PROBABILITY;
        this.lookAroundInterval = BotAIConstants.LOOK_AROUND_INTERVAL_TICKS;
        this.gazeState = GazeState.LOOKING_AT_LEADER;
        this.lookAroundTimer = lookAroundInterval;
    }

    @Override
    public boolean canUse() {
        // Actif uniquement pour les bots statiques (pas en follow)
        return bot.isStatic() && !bot.isFollowingLeader();
    }

    @Override
    public boolean canContinueToUse() {
        // Arrêter si le bot n'est plus statique ou commence à suivre
        return bot.isStatic() && !bot.isFollowingLeader();
    }

    @Override
    public void start() {
        lookAroundTimer = lookAroundInterval;
        gazeState = GazeState.LOOKING_AT_LEADER;
    }

    @Override
    public void tick() {
        // Récupérer la configuration depuis la base de données
        BotDatabase.BotData data = BotDatabase.getBotData(bot.getUUID());
        if (data != null) {
            lookAroundChance = data.lookAroundChance;
            lookAroundInterval = data.lookAroundInterval;
        }

        // Machine à états pour le comportement de regard
        switch (gazeState) {
            case LOOKING_AT_LEADER:
                handleLookingAtLeader();
                break;

            case SCANNING_AROUND:
                handleScanningAround();
                break;

            case RETURNING_TO_LEADER:
                handleReturningToLeader();
                break;
        }
    }

    /**
     * État: Regarde le leader
     */
    private void handleLookingAtLeader() {
        UUID leaderId = bot.getLeaderId();

        // Si pas de leader, regarder droit devant
        if (leaderId == null || !bot.isFollowingLeader()) {
            return;
        }

        LivingEntity leader = EntityFinder.findEntityByUUID(bot.level(), leaderId, bot.position(), 100.0);
        if (leader != null) {
            // Regarder le leader avec un peu d'inertie
            BotLookHelper.lookAtEntity(bot, leader);
        }

        // Timer pour décider de regarder ailleurs
        lookAroundTimer--;
        if (lookAroundTimer <= 0) {
            // Décider si ce bot va regarder ailleurs
            if (random.nextFloat() < lookAroundChance) {
                // Commencer à scanner les alentours
                gazeState = GazeState.SCANNING_AROUND;
                lookAroundDuration = 40 + random.nextInt(60); // 2-5 secondes
                lookAroundDurationTimer = lookAroundDuration;
                lookTargetChangeTimer = 0;

                // Générer une première cible de regard
                lookTarget = BotLookHelper.getRandomLookTarget(bot);
            }

            // Reset le timer
            lookAroundTimer = lookAroundInterval + random.nextInt(20);
        }
    }

    /**
     * État: Scanne les alentours
     */
    private void handleScanningAround() {
        // Durée du scan
        lookAroundDurationTimer--;
        if (lookAroundDurationTimer <= 0) {
            // Fini de scanner, retourner au leader
            gazeState = GazeState.RETURNING_TO_LEADER;
            return;
        }

        // Changer de cible de regard de temps en temps
        lookTargetChangeTimer--;
        if (lookTargetChangeTimer <= 0) {
            lookTarget = BotLookHelper.getRandomLookTarget(bot);
            lookTargetChangeTimer = 15 + random.nextInt(25); // 0.75 - 2 secondes
        }

        // Regarder vers la cible avec fluidité
        if (lookTarget != null) {
            BotLookHelper.lookAtPosition(bot, lookTarget, 5.0F, 5.0F);
        }
    }

    /**
     * État: Retourne regarder le leader
     */
    private void handleReturningToLeader() {
        UUID leaderId = bot.getLeaderId();

        if (leaderId == null || !bot.isFollowingLeader()) {
            gazeState = GazeState.LOOKING_AT_LEADER;
            return;
        }

        LivingEntity leader = EntityFinder.findEntityByUUID(bot.level(), leaderId, bot.position(), 100.0);
        if (leader != null) {
            // Regarder le leader avec une rotation fluide
            BotLookHelper.lookAtEntity(bot, leader, 8.0F, 8.0F);

            // Vérifier si on regarde bien le leader maintenant
            Vec3 lookVec = bot.getViewVector(1.0F);
            Vec3 toLeader = leader.position().subtract(bot.position()).normalize();
            double dot = lookVec.dot(toLeader);

            // Si on regarde suffisamment vers le leader (dot > 0.9)
            if (dot > 0.9) {
                gazeState = GazeState.LOOKING_AT_LEADER;
            }
        } else {
            gazeState = GazeState.LOOKING_AT_LEADER;
        }
    }

    /**
     * Change la probabilité de regarder ailleurs
     */
    public void setLookAroundChance(float chance) {
        this.lookAroundChance = Math.max(0.0f, Math.min(1.0f, chance));
    }

    /**
     * Obtient la probabilité actuelle
     */
    public float getLookAroundChance() {
        return lookAroundChance;
    }

    /**
     * Change l'intervalle de regard
     */
    public void setLookAroundInterval(int interval) {
        this.lookAroundInterval = Math.max(10, interval);
    }

    /**
     * Vérifie si le bot est en train de regarder ailleurs
     */
    public boolean isLookingAround() {
        return gazeState == GazeState.SCANNING_AROUND;
    }

    /**
     * Obtient l'état de regard actuel
     */
    public GazeState getGazeState() {
        return gazeState;
    }
}
