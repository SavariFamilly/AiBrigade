package com.aibrigade.bots;

/**
 * Defines the relationship types between bot groups.
 * This determines how bots from different groups interact with each other.
 */
public enum TeamRelationship {
    /**
     * Allied groups - will not attack each other, may help each other
     */
    ALLIED,

    /**
     * Neutral groups - will not attack each other unless provoked
     */
    NEUTRAL,

    /**
     * Hostile groups - will actively attack each other on sight
     */
    HOSTILE
}
