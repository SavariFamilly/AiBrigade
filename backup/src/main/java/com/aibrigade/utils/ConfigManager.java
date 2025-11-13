package com.aibrigade.utils;

import com.aibrigade.main.AIBrigadeMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ConfigManager - Manages mod configuration
 *
 * Handles loading and saving configuration from JSON file.
 * Configuration includes:
 * - AI thread pool size
 * - Maximum bot limit
 * - Default behaviors and radii
 * - Animation settings
 * - Pathfinding options
 * - Performance settings
 */
public class ConfigManager {

    private static final String CONFIG_FILE = "aibrigade_config.json";

    // Configuration values with defaults
    private int aiThreadPoolSize = 4;
    private int maxBots = 300;
    private float defaultFollowRadius = 10.0f;
    private String defaultBehavior = "follow";
    private boolean enableAnimations = true;
    private boolean enableAdvancedPathfinding = true;
    private int aiUpdateInterval = 4; // Ticks between AI updates
    private boolean debugMode = false;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Load configuration from file
     * Creates default config if file doesn't exist
     */
    public void loadConfiguration() {
        Path configPath = getConfigPath();

        if (!Files.exists(configPath)) {
            AIBrigadeMod.LOGGER.info("Configuration file not found, creating default");
            saveConfiguration();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject config = gson.fromJson(reader, JsonObject.class);

            if (config.has("aiThreadPoolSize")) {
                aiThreadPoolSize = config.get("aiThreadPoolSize").getAsInt();
            }

            if (config.has("maxBots")) {
                maxBots = config.get("maxBots").getAsInt();
            }

            if (config.has("defaultFollowRadius")) {
                defaultFollowRadius = config.get("defaultFollowRadius").getAsFloat();
            }

            if (config.has("defaultBehavior")) {
                defaultBehavior = config.get("defaultBehavior").getAsString();
            }

            if (config.has("enableAnimations")) {
                enableAnimations = config.get("enableAnimations").getAsBoolean();
            }

            if (config.has("enableAdvancedPathfinding")) {
                enableAdvancedPathfinding = config.get("enableAdvancedPathfinding").getAsBoolean();
            }

            if (config.has("aiUpdateInterval")) {
                aiUpdateInterval = config.get("aiUpdateInterval").getAsInt();
            }

            if (config.has("debugMode")) {
                debugMode = config.get("debugMode").getAsBoolean();
            }

            AIBrigadeMod.LOGGER.info("Configuration loaded successfully");
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to load configuration", e);
        }
    }

    /**
     * Save configuration to file
     */
    public void saveConfiguration() {
        JsonObject config = new JsonObject();

        config.addProperty("aiThreadPoolSize", aiThreadPoolSize);
        config.addProperty("maxBots", maxBots);
        config.addProperty("defaultFollowRadius", defaultFollowRadius);
        config.addProperty("defaultBehavior", defaultBehavior);
        config.addProperty("enableAnimations", enableAnimations);
        config.addProperty("enableAdvancedPathfinding", enableAdvancedPathfinding);
        config.addProperty("aiUpdateInterval", aiUpdateInterval);
        config.addProperty("debugMode", debugMode);

        Path configPath = getConfigPath();

        try {
            // Create config directory if it doesn't exist
            Files.createDirectories(configPath.getParent());

            try (Writer writer = Files.newBufferedWriter(configPath)) {
                gson.toJson(config, writer);
            }

            AIBrigadeMod.LOGGER.info("Configuration saved successfully");
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to save configuration", e);
        }
    }

    /**
     * Get configuration file path
     */
    private Path getConfigPath() {
        return Paths.get("config", CONFIG_FILE);
    }

    // Getters and setters

    public int getAIThreadPoolSize() {
        return aiThreadPoolSize;
    }

    public void setAIThreadPoolSize(int size) {
        this.aiThreadPoolSize = Math.max(1, size);
    }

    public int getMaxBots() {
        return maxBots;
    }

    public void setMaxBots(int maxBots) {
        this.maxBots = Math.max(1, Math.min(maxBots, 1000));
    }

    public float getDefaultFollowRadius() {
        return defaultFollowRadius;
    }

    public void setDefaultFollowRadius(float radius) {
        this.defaultFollowRadius = Math.max(1.0f, radius);
    }

    public String getDefaultBehavior() {
        return defaultBehavior;
    }

    public void setDefaultBehavior(String behavior) {
        this.defaultBehavior = behavior;
    }

    public boolean isAnimationsEnabled() {
        return enableAnimations;
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.enableAnimations = enabled;
    }

    public boolean isAdvancedPathfindingEnabled() {
        return enableAdvancedPathfinding;
    }

    public void setAdvancedPathfindingEnabled(boolean enabled) {
        this.enableAdvancedPathfinding = enabled;
    }

    public int getAIUpdateInterval() {
        return aiUpdateInterval;
    }

    public void setAIUpdateInterval(int interval) {
        this.aiUpdateInterval = Math.max(1, interval);
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}
