package com.aibrigade.utils;

/**
 * Centralized constants for Bot AI behaviors
 * Replaces scattered magic numbers and duplicated constants across the codebase
 */
public class BotAIConstants {

    // ==================== PROBABILITIES ====================

    /** Probability for active following behavior (16.67%) */
    public static final float ACTIVE_FOLLOW_PROBABILITY = 1.0f / 6.0f;

    /** Probability for look around behavior (33.33%) */
    public static final float LOOK_AROUND_PROBABILITY = 2.0f / 6.0f;

    // ==================== TIMING (in ticks, 20 ticks = 1 second) ====================

    /** General decision making interval (2 seconds) */
    public static final int DECISION_INTERVAL_TICKS = 40;

    /** Path recalculation interval (1 second) */
    public static final int PATH_RECALC_INTERVAL_TICKS = 20;

    /** Look around behavior interval (2 seconds) */
    public static final int LOOK_AROUND_INTERVAL_TICKS = 40;

    /** Speed change decision interval (1.5 seconds) */
    public static final int SPEED_CHANGE_INTERVAL_TICKS = 30;

    /** Combat cooldown interval (1 second) */
    public static final int COMBAT_COOLDOWN_TICKS = 20;

    // ==================== DISTANCES ====================

    /** Minimum distance to follow a leader before stopping */
    public static final double MIN_FOLLOW_DISTANCE = 2.0;

    /** Maximum distance before teleporting to leader */
    public static final double TELEPORT_DISTANCE = 50.0;

    /** Default patrol radius */
    public static final double DEFAULT_PATROL_RADIUS = 20.0;

    /** Default follow radius */
    public static final double DEFAULT_FOLLOW_RADIUS = 10.0;

    /** Default guard radius */
    public static final double DEFAULT_GUARD_RADIUS = 15.0;

    /** Distance for disperse behavior */
    public static final double DISPERSE_RADIUS = 3.0;

    // ==================== LOOK CONTROL ====================

    /** Standard yaw rotation speed for looking */
    public static final float LOOK_YAW_SPEED = 10.0f;

    /** Standard pitch rotation speed for looking */
    public static final float LOOK_PITCH_SPEED = 10.0f;

    /** Fast yaw rotation speed */
    public static final float LOOK_YAW_SPEED_FAST = 30.0f;

    /** Fast pitch rotation speed */
    public static final float LOOK_PITCH_SPEED_FAST = 30.0f;

    /** Maximum head X rotation */
    public static final float MAX_HEAD_X_ROT = 40.0f;

    // ==================== MOVEMENT SPEEDS ====================

    /** Normal walking speed multiplier */
    public static final double SPEED_WALK = 1.0;

    /** Running speed multiplier */
    public static final double SPEED_RUN = 1.2;

    /** Sprint speed multiplier */
    public static final double SPEED_SPRINT = 1.5;

    /** Slow movement speed multiplier */
    public static final double SPEED_SLOW = 0.8;

    // ==================== JUMPING & CLIMBING ====================

    /** Minimum height difference to consider jumping (blocks) */
    public static final double JUMP_HEIGHT_MIN = 0.5;

    /** Maximum height difference for jumping (blocks) */
    public static final double JUMP_HEIGHT_MAX = 2.0;

    // ==================== FORMATIONS ====================

    /** Default spacing between bots in formations (blocks) */
    public static final double FORMATION_SPACING = 2.5;

    /** Minimum spacing between bots to avoid clustering (blocks) */
    public static final double MIN_BOT_SPACING = 2.0;

    // ==================== RANDOM LOOK BEHAVIOR ====================

    /** Minimum distance for random look target */
    public static final double LOOK_DISTANCE_MIN = 5.0;

    /** Maximum additional distance for random look target */
    public static final double LOOK_DISTANCE_RANGE = 15.0;

    /** Maximum vertical offset for random look target (blocks) */
    public static final double LOOK_VERTICAL_OFFSET = 4.0;

    // ==================== AI UPDATE INTERVALS ====================

    /** Fast AI update interval for combat (ticks) */
    public static final int AI_UPDATE_FAST = 2;

    /** Normal AI update interval (ticks) */
    public static final int AI_UPDATE_NORMAL = 4;

    /** Slow AI update interval for idle behavior (ticks) */
    public static final int AI_UPDATE_SLOW = 40;

    // ==================== GOAL PRIORITIES ====================

    /** Priority for raid behavior */
    public static final int PRIORITY_RAID = 8;

    /** Priority for guard behavior */
    public static final int PRIORITY_GUARD = 6;

    /** Priority for follow behavior */
    public static final int PRIORITY_FOLLOW = 5;

    /** Priority for patrol behavior */
    public static final int PRIORITY_PATROL = 3;

    /** Priority for idle behavior */
    public static final int PRIORITY_IDLE = 1;
}
