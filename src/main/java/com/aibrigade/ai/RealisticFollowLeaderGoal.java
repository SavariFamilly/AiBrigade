package com.aibrigade.ai;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.persistence.BotDatabase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;

/**
 * RealisticFollowLeaderGoal - Système de follow réaliste avec probabilités
 *
 * Selon le cahier des charges:
 * - 5/6 des bots suivent dans le radius défini (comportement radius-based)
 * - 1/6 des bots suivent activement le leader (comportement actif proche)
 * - Follow dans un rayon avec positions aléatoires (pas tous au même point)
 * - Variation de vitesse pour un effet organique
 * - Trajectoires légèrement courbes (pas de lignes droites)
 * - Petites pauses aléatoires
 * - Évitement des collisions
 * - Respect du mode statique
 */
public class RealisticFollowLeaderGoal extends Goal {

    private final BotEntity bot;
    private final double speedModifier;
    private final float minFollowDistance;
    private final float maxFollowDistance;
    private final Random random;

    // Comportement selon le cahier des charges
    private FollowBehaviorType behaviorType;  // Type de comportement (RADIUS_BASED ou ACTIVE_FOLLOW)
    private static final float ACTIVE_FOLLOW_PROBABILITY = 1.0f / 6.0f; // 1/6 = ~16.67%

    // Comportement aléatoire
    private float chaseChance;           // Probabilité de suivre activement (0.0 - 1.0)
    private boolean isActivelyChasing;   // Est-ce que ce bot est en train de poursuivre?
    private int chaseDecisionCooldown;   // Cooldown pour recalculer la décision
    private static final int DECISION_INTERVAL = 40; // 2 secondes

    /**
     * Types de comportement de follow
     */
    public enum FollowBehaviorType {
        RADIUS_BASED,    // 5/6 - Reste dans le radius, ne suit pas activement
        ACTIVE_FOLLOW    // 1/6 - Suit activement le leader de près
    }

    // Mouvement réaliste
    private Vec3 targetPosition;         // Position cible unique du bot
    private int recalculatePathTimer;
    private static final int PATH_RECALC_INTERVAL = 20; // 1 seconde

    // Variation de vitesse
    private double currentSpeedMultiplier;
    private int speedChangeTimer;
    private static final int SPEED_CHANGE_INTERVAL = 30; // 1.5 secondes

    // Pause aléatoire
    private int pauseTimer;
    private boolean isPaused;

    // Trajectoire courbe
    private double curveOffset;
    private int curveUpdateTimer;

    public RealisticFollowLeaderGoal(BotEntity bot, double speed, float minDist, float maxDist) {
        this.bot = bot;
        this.speedModifier = speed;
        this.minFollowDistance = minDist;
        this.maxFollowDistance = maxDist;
        this.random = new Random(bot.getUUID().getMostSignificantBits());

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));

        // Déterminer le type de comportement selon les probabilités (1/6 vs 5/6)
        if (random.nextFloat() < ACTIVE_FOLLOW_PROBABILITY) {
            this.behaviorType = FollowBehaviorType.ACTIVE_FOLLOW;
            this.chaseChance = 0.95f; // Suit activement presque toujours
        } else {
            this.behaviorType = FollowBehaviorType.RADIUS_BASED;
            this.chaseChance = 0.3f; // Suit peu souvent, reste dans le radius
        }

        // Initialiser les comportements aléatoires
        this.currentSpeedMultiplier = 0.9 + random.nextDouble() * 0.2; // 0.9-1.1x
        this.isActivelyChasing = random.nextFloat() < chaseChance;

        System.out.println("[RealisticFollowLeaderGoal] Bot " + bot.getBotName() +
            " configured with behavior: " + behaviorType + " (chase chance: " + chaseChance + ")");
    }

    @Override
    public boolean canUse() {
        // Vérifier le mode statique
        if (bot.isStatic()) {
            return false;
        }

        // Le bot doit suivre un leader
        if (!bot.isFollowingLeader()) {
            return false;
        }

        UUID leaderId = bot.getLeaderId();
        if (leaderId == null) {
            return false;
        }

        // Trouver le leader
        LivingEntity leader = findLeader(leaderId);
        if (leader == null) {
            return false;
        }

        // Distance au leader
        double distance = bot.distanceTo(leader);

        // Comportement selon le type
        if (behaviorType == FollowBehaviorType.ACTIVE_FOLLOW) {
            // Active follow: suit toujours le leader de près
            return distance > minFollowDistance;
        } else {
            // Radius-based: reste dans le radius
            // Si trop proche, ne pas suivre
            if (distance < minFollowDistance) {
                return false;
            }

            // Si dans le rayon et pas en train de chase, ne pas bouger
            if (distance < maxFollowDistance && !isActivelyChasing) {
                return false;
            }

            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        // Vérifier le mode statique
        if (bot.isStatic()) {
            return false;
        }

        UUID leaderId = bot.getLeaderId();
        if (leaderId == null || !bot.isFollowingLeader()) {
            return false;
        }

        LivingEntity leader = findLeader(leaderId);
        if (leader == null) {
            return false;
        }

        double distance = bot.distanceTo(leader);

        // Si trop proche, arrêter
        if (distance < minFollowDistance) {
            return false;
        }

        // Si en pause, continuer l'objectif mais ne pas bouger
        if (isPaused) {
            return true;
        }

        return distance > minFollowDistance;
    }

    @Override
    public void start() {
        // Initialiser le mouvement
        recalculatePathTimer = 0;
        speedChangeTimer = 0;
        curveUpdateTimer = 0;
        pauseTimer = 0;
        isPaused = false;

        // Décider si ce bot va activement chase
        updateChaseDecision();
    }

    @Override
    public void tick() {
        UUID leaderId = bot.getLeaderId();
        if (leaderId == null) return;

        LivingEntity leader = findLeader(leaderId);
        if (leader == null) return;

        // === 1. Décision de chase (probabilité) ===
        chaseDecisionCooldown--;
        if (chaseDecisionCooldown <= 0) {
            updateChaseDecision();
        }

        // Si pas en train de chase et dans le rayon, ne rien faire
        double distance = bot.distanceTo(leader);
        if (!isActivelyChasing && distance < maxFollowDistance) {
            bot.getNavigation().stop();
            return;
        }

        // === 2. Pause aléatoire ===
        if (isPaused) {
            pauseTimer--;
            if (pauseTimer <= 0) {
                isPaused = false;
            }
            bot.getNavigation().stop();
            return;
        }

        // Chance de pause (5%)
        if (random.nextFloat() < 0.05) {
            isPaused = true;
            pauseTimer = 10 + random.nextInt(20); // 0.5 - 1.5 secondes
            return;
        }

        // === 3. Variation de vitesse ===
        speedChangeTimer--;
        if (speedChangeTimer <= 0) {
            // Changer légèrement la vitesse (0.85x - 1.15x)
            currentSpeedMultiplier = 0.85 + random.nextDouble() * 0.3;
            speedChangeTimer = SPEED_CHANGE_INTERVAL;
        }

        // === 4. Trajectoire courbe ===
        curveUpdateTimer--;
        if (curveUpdateTimer <= 0) {
            curveOffset = (random.nextDouble() - 0.5) * 2.0; // -1.0 à +1.0
            curveUpdateTimer = 15; // 0.75 secondes
        }

        // === 5. Recalcul de la position cible ===
        recalculatePathTimer--;
        if (recalculatePathTimer <= 0 || targetPosition == null) {
            // Position différente selon le type de comportement
            if (behaviorType == FollowBehaviorType.ACTIVE_FOLLOW) {
                // Active follow: vise une position très proche du leader
                targetPosition = calculateClosePosition(leader);
            } else {
                // Radius-based: vise une position dans le radius
                targetPosition = calculateSpreadPosition(leader);
            }
            recalculatePathTimer = PATH_RECALC_INTERVAL;
        }

        // === 6. Appliquer la trajectoire courbe ===
        Vec3 curvedTarget = applyCurveToPath(targetPosition);

        // === 7. Déplacement ===
        double finalSpeed = speedModifier * currentSpeedMultiplier;

        // Boost de vitesse selon le type et la distance
        if (behaviorType == FollowBehaviorType.ACTIVE_FOLLOW) {
            // Active followers sont plus rapides pour rester près
            if (distance > minFollowDistance * 3) {
                finalSpeed *= 1.4;
            } else if (distance > minFollowDistance * 2) {
                finalSpeed *= 1.2;
            }
        } else {
            // Radius-based boost si trop loin du radius
            if (distance > maxFollowDistance * 2) {
                finalSpeed *= 1.3;
            } else if (distance > maxFollowDistance * 1.5) {
                finalSpeed *= 1.15;
            }
        }

        // Naviguer vers la position
        bot.getNavigation().moveTo(curvedTarget.x, curvedTarget.y, curvedTarget.z, finalSpeed);

        // === 8. Regarder le leader ===
        bot.getLookControl().setLookAt(leader, 30.0F, 30.0F);
    }

    @Override
    public void stop() {
        bot.getNavigation().stop();
        targetPosition = null;
    }

    /**
     * Met à jour la décision de chase basée sur la probabilité
     */
    private void updateChaseDecision() {
        // Récupérer la chance depuis la base de données si disponible
        BotDatabase.BotData data = BotDatabase.getBotData(bot.getUUID());
        if (data != null) {
            chaseChance = data.chaseChance;
        }

        // Décider si le bot va activement chase
        isActivelyChasing = random.nextFloat() < chaseChance;

        // Reset le cooldown
        chaseDecisionCooldown = DECISION_INTERVAL;
    }

    /**
     * Calcule une position très proche du leader (pour active followers)
     */
    private Vec3 calculateClosePosition(LivingEntity leader) {
        Vec3 leaderPos = leader.position();

        // Utiliser l'UUID pour avoir une position cohérente
        long seed = bot.getUUID().getMostSignificantBits() ^ leader.getUUID().getMostSignificantBits();
        Random posRandom = new Random(seed + (System.currentTimeMillis() / 1000));

        // Angle unique basé sur l'UUID
        double baseAngle = (bot.getUUID().getMostSignificantBits() % 360) * Math.PI / 180.0;
        double angleVariation = (posRandom.nextDouble() - 0.5) * 0.3;
        double angle = baseAngle + angleVariation;

        // Distance très proche (2-4 blocs du leader)
        double distance = minFollowDistance + posRandom.nextDouble() * 2.0;

        // Calculer la position
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        Vec3 targetPos = leaderPos.add(offsetX, 0, offsetZ);

        // Trouver le sol (Y)
        Level level = bot.level();
        BlockPos groundPos = new BlockPos((int)targetPos.x, (int)leaderPos.y, (int)targetPos.z);

        for (int dy = -3; dy <= 3; dy++) {
            BlockPos checkPos = groundPos.offset(0, dy, 0);
            if (!level.getBlockState(checkPos).isAir() &&
                level.getBlockState(checkPos.above()).isAir()) {
                return new Vec3(targetPos.x, checkPos.getY() + 1, targetPos.z);
            }
        }

        return targetPos;
    }

    /**
     * Calcule une position éparpillée unique pour ce bot dans le rayon
     */
    private Vec3 calculateSpreadPosition(LivingEntity leader) {
        Vec3 leaderPos = leader.position();

        // Utiliser l'UUID pour avoir une position cohérente mais unique
        long seed = bot.getUUID().getMostSignificantBits() ^ leader.getUUID().getMostSignificantBits();
        Random posRandom = new Random(seed + (System.currentTimeMillis() / 1000)); // Change chaque seconde

        // Angle basé sur l'UUID (chaque bot a son propre angle)
        double baseAngle = (bot.getUUID().getMostSignificantBits() % 360) * Math.PI / 180.0;

        // Ajouter une légère variation
        double angleVariation = (posRandom.nextDouble() - 0.5) * 0.5; // ±0.25 radians
        double angle = baseAngle + angleVariation;

        // Distance dans le rayon (70% - 90% du rayon max pour éviter les bords)
        float radius = bot.getFollowRadius();
        double distance = radius * (0.7 + posRandom.nextDouble() * 0.2);

        // Calculer la position
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        Vec3 targetPos = leaderPos.add(offsetX, 0, offsetZ);

        // Trouver le sol (Y)
        Level level = bot.level();
        BlockPos groundPos = new BlockPos((int)targetPos.x, (int)leaderPos.y, (int)targetPos.z);

        // Chercher le bloc solide le plus proche
        for (int dy = -3; dy <= 3; dy++) {
            BlockPos checkPos = groundPos.offset(0, dy, 0);
            if (!level.getBlockState(checkPos).isAir() &&
                level.getBlockState(checkPos.above()).isAir()) {
                return new Vec3(targetPos.x, checkPos.getY() + 1, targetPos.z);
            }
        }

        return targetPos;
    }

    /**
     * Applique une courbe à la trajectoire pour éviter les lignes droites
     */
    private Vec3 applyCurveToPath(Vec3 target) {
        Vec3 botPos = bot.position();
        Vec3 direction = target.subtract(botPos).normalize();

        // Vecteur perpendiculaire pour la courbe
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x);

        // Appliquer l'offset de courbe
        return target.add(perpendicular.scale(curveOffset));
    }

    /**
     * Trouve le leader par son UUID
     */
    private LivingEntity findLeader(UUID leaderId) {
        Level level = bot.level();

        // Chercher dans les joueurs
        for (Player player : level.players()) {
            if (player.getUUID().equals(leaderId)) {
                return player;
            }
        }

        // Chercher dans les autres bots (leader bot)
        for (BotEntity otherBot : level.getEntitiesOfClass(BotEntity.class,
                bot.getBoundingBox().inflate(100.0))) {
            if (otherBot.getUUID().equals(leaderId)) {
                return otherBot;
            }
        }

        return null;
    }

    /**
     * Change la probabilité de chase pour ce bot
     */
    public void setChaseChance(float chance) {
        this.chaseChance = Math.max(0.0f, Math.min(1.0f, chance));
    }

    /**
     * Obtient la probabilité de chase actuelle
     */
    public float getChaseChance() {
        return chaseChance;
    }

    /**
     * Vérifie si le bot est en train de chase activement
     */
    public boolean isActivelyChasing() {
        return isActivelyChasing;
    }
}
