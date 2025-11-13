package com.aibrigade.bots;

import net.minecraft.nbt.CompoundTag;

/**
 * Configuration flexible pour le comportement de chaque bot
 * Permet de définir:
 * - Le rayon d'opération
 * - Si le bot est chef de groupe
 * - Le mode de suivi (joueur, leader, patrol, etc.)
 * - Les comportements spéciaux
 */
public class BotBehaviorConfig {

    // Configuration du rayon d'opération
    private float operationRadius = 32.0F;

    // Chef de groupe
    private boolean isGroupLeader = false;

    // Modes de suivi
    private FollowMode followMode = FollowMode.NONE;
    private float followDistance = 5.0F;
    private boolean matchPlayerSpeed = true;
    private boolean canClimbBlocks = true;
    private boolean canJumpObstacles = true;

    // Comportements spéciaux
    private boolean canSwim = true;
    private boolean canOpenDoors = false;
    private boolean avoidFire = true;
    private boolean avoidCliffs = true;

    // Combat
    private float aggroRange = 16.0F;
    private boolean attackPlayers = false;
    private boolean attackBots = false;

    public enum FollowMode {
        NONE("none"),           // Ne suit personne
        PLAYER("player"),       // Suit le joueur leader
        GROUP_LEADER("leader"), // Suit le chef de groupe
        FORMATION("formation"), // Suit en formation
        PATROL("patrol");       // Patrouille autour d'un point

        private final String name;

        FollowMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static FollowMode fromString(String name) {
            for (FollowMode mode : values()) {
                if (mode.name.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return NONE;
        }
    }

    /**
     * Constructeur par défaut
     */
    public BotBehaviorConfig() {
    }

    /**
     * Constructeur avec configuration rapide
     */
    public BotBehaviorConfig(float operationRadius, boolean isGroupLeader, FollowMode followMode) {
        this.operationRadius = operationRadius;
        this.isGroupLeader = isGroupLeader;
        this.followMode = followMode;
    }

    // === GETTERS ===

    public float getOperationRadius() {
        return operationRadius;
    }

    public boolean isGroupLeader() {
        return isGroupLeader;
    }

    public FollowMode getFollowMode() {
        return followMode;
    }

    public float getFollowDistance() {
        return followDistance;
    }

    public boolean shouldMatchPlayerSpeed() {
        return matchPlayerSpeed;
    }

    public boolean canClimbBlocks() {
        return canClimbBlocks;
    }

    public boolean canJumpObstacles() {
        return canJumpObstacles;
    }

    public boolean canSwim() {
        return canSwim;
    }

    public boolean canOpenDoors() {
        return canOpenDoors;
    }

    public boolean shouldAvoidFire() {
        return avoidFire;
    }

    public boolean shouldAvoidCliffs() {
        return avoidCliffs;
    }

    public float getAggroRange() {
        return aggroRange;
    }

    public boolean canAttackPlayers() {
        return attackPlayers;
    }

    public boolean canAttackBots() {
        return attackBots;
    }

    // === SETTERS ===

    public BotBehaviorConfig setOperationRadius(float radius) {
        this.operationRadius = radius;
        return this;
    }

    public BotBehaviorConfig setGroupLeader(boolean isLeader) {
        this.isGroupLeader = isLeader;
        return this;
    }

    public BotBehaviorConfig setFollowMode(FollowMode mode) {
        this.followMode = mode;
        return this;
    }

    public BotBehaviorConfig setFollowDistance(float distance) {
        this.followDistance = distance;
        return this;
    }

    public BotBehaviorConfig setMatchPlayerSpeed(boolean match) {
        this.matchPlayerSpeed = match;
        return this;
    }

    public BotBehaviorConfig setCanClimbBlocks(boolean canClimb) {
        this.canClimbBlocks = canClimb;
        return this;
    }

    public BotBehaviorConfig setCanJumpObstacles(boolean canJump) {
        this.canJumpObstacles = canJump;
        return this;
    }

    public BotBehaviorConfig setCanSwim(boolean canSwim) {
        this.canSwim = canSwim;
        return this;
    }

    public BotBehaviorConfig setCanOpenDoors(boolean canOpen) {
        this.canOpenDoors = canOpen;
        return this;
    }

    public BotBehaviorConfig setAvoidFire(boolean avoid) {
        this.avoidFire = avoid;
        return this;
    }

    public BotBehaviorConfig setAvoidCliffs(boolean avoid) {
        this.avoidCliffs = avoid;
        return this;
    }

    public BotBehaviorConfig setAggroRange(float range) {
        this.aggroRange = range;
        return this;
    }

    public BotBehaviorConfig setAttackPlayers(boolean attack) {
        this.attackPlayers = attack;
        return this;
    }

    public BotBehaviorConfig setAttackBots(boolean attack) {
        this.attackBots = attack;
        return this;
    }

    // === SERIALIZATION ===

    /**
     * Sauvegarde la configuration dans NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putFloat("operationRadius", operationRadius);
        tag.putBoolean("isGroupLeader", isGroupLeader);
        tag.putString("followMode", followMode.getName());
        tag.putFloat("followDistance", followDistance);
        tag.putBoolean("matchPlayerSpeed", matchPlayerSpeed);
        tag.putBoolean("canClimbBlocks", canClimbBlocks);
        tag.putBoolean("canJumpObstacles", canJumpObstacles);
        tag.putBoolean("canSwim", canSwim);
        tag.putBoolean("canOpenDoors", canOpenDoors);
        tag.putBoolean("avoidFire", avoidFire);
        tag.putBoolean("avoidCliffs", avoidCliffs);
        tag.putFloat("aggroRange", aggroRange);
        tag.putBoolean("attackPlayers", attackPlayers);
        tag.putBoolean("attackBots", attackBots);

        return tag;
    }

    /**
     * Charge la configuration depuis NBT
     */
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("operationRadius")) {
            operationRadius = tag.getFloat("operationRadius");
        }
        if (tag.contains("isGroupLeader")) {
            isGroupLeader = tag.getBoolean("isGroupLeader");
        }
        if (tag.contains("followMode")) {
            followMode = FollowMode.fromString(tag.getString("followMode"));
        }
        if (tag.contains("followDistance")) {
            followDistance = tag.getFloat("followDistance");
        }
        if (tag.contains("matchPlayerSpeed")) {
            matchPlayerSpeed = tag.getBoolean("matchPlayerSpeed");
        }
        if (tag.contains("canClimbBlocks")) {
            canClimbBlocks = tag.getBoolean("canClimbBlocks");
        }
        if (tag.contains("canJumpObstacles")) {
            canJumpObstacles = tag.getBoolean("canJumpObstacles");
        }
        if (tag.contains("canSwim")) {
            canSwim = tag.getBoolean("canSwim");
        }
        if (tag.contains("canOpenDoors")) {
            canOpenDoors = tag.getBoolean("canOpenDoors");
        }
        if (tag.contains("avoidFire")) {
            avoidFire = tag.getBoolean("avoidFire");
        }
        if (tag.contains("avoidCliffs")) {
            avoidCliffs = tag.getBoolean("avoidCliffs");
        }
        if (tag.contains("aggroRange")) {
            aggroRange = tag.getFloat("aggroRange");
        }
        if (tag.contains("attackPlayers")) {
            attackPlayers = tag.getBoolean("attackPlayers");
        }
        if (tag.contains("attackBots")) {
            attackBots = tag.getBoolean("attackBots");
        }
    }

    /**
     * Clone la configuration
     */
    public BotBehaviorConfig copy() {
        BotBehaviorConfig copy = new BotBehaviorConfig();
        copy.loadFromNBT(this.saveToNBT());
        return copy;
    }

    // === PRESETS ===

    /**
     * Preset: Garde du corps (suit le joueur de près)
     */
    public static BotBehaviorConfig createBodyguard() {
        return new BotBehaviorConfig()
            .setFollowMode(FollowMode.PLAYER)
            .setFollowDistance(3.0F)
            .setMatchPlayerSpeed(true)
            .setCanClimbBlocks(true)
            .setCanJumpObstacles(true)
            .setOperationRadius(50.0F)
            .setAggroRange(20.0F)
            .setAttackPlayers(true);
    }

    /**
     * Preset: Soldat (suit le chef de groupe)
     */
    public static BotBehaviorConfig createSoldier() {
        return new BotBehaviorConfig()
            .setFollowMode(FollowMode.GROUP_LEADER)
            .setFollowDistance(5.0F)
            .setMatchPlayerSpeed(true)
            .setCanClimbBlocks(true)
            .setOperationRadius(40.0F)
            .setAggroRange(16.0F);
    }

    /**
     * Preset: Chef de groupe
     */
    public static BotBehaviorConfig createLeader() {
        return new BotBehaviorConfig()
            .setGroupLeader(true)
            .setFollowMode(FollowMode.PLAYER)
            .setFollowDistance(4.0F)
            .setMatchPlayerSpeed(true)
            .setCanClimbBlocks(true)
            .setOperationRadius(60.0F)
            .setAggroRange(18.0F);
    }

    /**
     * Preset: Patrouille (reste dans un rayon)
     */
    public static BotBehaviorConfig createPatrol() {
        return new BotBehaviorConfig()
            .setFollowMode(FollowMode.PATROL)
            .setOperationRadius(25.0F)
            .setAggroRange(20.0F)
            .setAvoidCliffs(true);
    }

    @Override
    public String toString() {
        return "BotBehaviorConfig{" +
            "radius=" + operationRadius +
            ", leader=" + isGroupLeader +
            ", follow=" + followMode +
            ", dist=" + followDistance +
            ", speed=" + matchPlayerSpeed +
            ", climb=" + canClimbBlocks +
            '}';
    }
}
