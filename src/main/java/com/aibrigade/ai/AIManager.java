package com.aibrigade.ai;

import com.aibrigade.main.AIBrigadeMod;
import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.BotManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.*;
import java.util.*;

/**
 * AIManager - Global AI management system
 *
 * Manages AI-related functionality for bots including:
 * - Group behavior coordination
 * - Cleanup of dead/invalid bots
 * - Thread pool for future async AI operations
 *
 * Note: Individual bot AI behaviors are handled by Minecraft's Goal system
 * through Goal classes registered in BotEntity.registerGoals():
 * - RealisticFollowLeaderGoal: Handles follow behavior with realistic movement
 * - ActiveGazeBehavior: Manages bot gaze and looking behavior
 * - PlaceBlockToReachTargetGoal: Enables bots to place blocks when climbing
 * - TeamAwareAttackGoal: Handles combat targeting without friendly fire
 * - Standard Minecraft Goals: FloatGoal, MeleeAttackGoal, WaterAvoidingRandomStrollGoal, etc.
 *
 * This manager focuses on high-level group coordination and resource management
 * rather than individual bot AI logic.
 */
public class AIManager {

    // Thread pool for AI processing (available for future async operations)
    private final ExecutorService aiThreadPool;
    private final int threadPoolSize;

    // Cleanup interval
    private static final int CLEANUP_INTERVAL = 100; // Cleanup dead bots every 100 ticks (5 seconds)
    private int tickCounter = 0;

    // Server reference
    private MinecraftServer server;

    // Flag to track if AI system is running
    private boolean isRunning = false;

    /**
     * Constructor
     *
     * @param threadPoolSize Number of threads for AI processing
     */
    public AIManager(int threadPoolSize) {
        this.threadPoolSize = Math.max(1, threadPoolSize);
        this.aiThreadPool = Executors.newFixedThreadPool(this.threadPoolSize);

        AIBrigadeMod.LOGGER.info("AIManager initialized with {} threads", this.threadPoolSize);
    }

    /**
     * Start AI system
     * Note: Individual bot AI is handled by their registered Goals.
     * This primarily manages cleanup and group coordination.
     *
     * @param server The minecraft server
     */
    public void startAITicking(MinecraftServer server) {
        this.server = server;
        this.isRunning = true;
        AIBrigadeMod.LOGGER.info("AI system started");
    }

    /**
     * Stop AI system and cleanup resources
     */
    public void stopAITicking() {
        this.isRunning = false;

        // Shutdown thread pool gracefully
        aiThreadPool.shutdown();
        try {
            if (!aiThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                aiThreadPool.shutdownNow();
                AIBrigadeMod.LOGGER.warn("AI thread pool forced shutdown");
            }
        } catch (InterruptedException e) {
            aiThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        AIBrigadeMod.LOGGER.info("AI system stopped and cleanup complete");
    }

    /**
     * Server tick event handler
     * Performs periodic cleanup of dead/invalid bots
     *
     * @param event The server tick event
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isRunning) {
            return;
        }

        tickCounter++;

        // Cleanup dead bots periodically (every 5 seconds)
        if (tickCounter % CLEANUP_INTERVAL == 0) {
            cleanupDeadBots();
        }
    }

    /**
     * Cleanup dead or invalid bots from the manager
     * Called every CLEANUP_INTERVAL ticks to ensure dead bots don't block spawns
     */
    private void cleanupDeadBots() {
        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager != null) {
            botManager.cleanupDeadBots();
        }
    }

    /**
     * Apply behavior to all bots in a group
     * This sets the behavior type which can be used by Goal classes to adjust their logic
     *
     * @param groupName The group name
     * @param behavior The behavior type (follow, raid, patrol, guard, etc.)
     */
    public void applyGroupBehavior(String groupName, String behavior) {
        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            return;
        }

        BotManager.BotGroup group = botManager.getBotGroups().get(groupName);
        if (group == null) {
            AIBrigadeMod.LOGGER.warn("Group {} not found", groupName);
            return;
        }

        for (UUID botId : group.getBotIds()) {
            BotEntity bot = botManager.getActiveBots().get(botId);
            if (bot != null) {
                bot.setBehaviorType(behavior);
                AIBrigadeMod.LOGGER.debug("Applied behavior {} to bot {}",
                    behavior, bot.getBotName());
            }
        }

        AIBrigadeMod.LOGGER.info("Applied behavior {} to group {}", behavior, groupName);
    }

    /**
     * Set follow radius for all bots in a group
     * This affects how far bots will stay from their leader
     *
     * @param groupName The group name
     * @param radius The new radius in blocks
     */
    public void setGroupRadius(String groupName, float radius) {
        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            return;
        }

        BotManager.BotGroup group = botManager.getBotGroups().get(groupName);
        if (group == null) {
            AIBrigadeMod.LOGGER.warn("Group {} not found", groupName);
            return;
        }

        group.setFollowRadius(radius);

        for (UUID botId : group.getBotIds()) {
            BotEntity bot = botManager.getActiveBots().get(botId);
            if (bot != null) {
                bot.setFollowRadius(radius);
            }
        }

        AIBrigadeMod.LOGGER.info("Set radius {} for group {}", radius, groupName);
    }

    /**
     * Toggle static state for group or individual bot
     * Static bots will not move (AI disabled)
     *
     * @param targetName Group or bot name
     */
    public void toggleStatic(String targetName) {
        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            return;
        }

        // Check if it's a group
        if (botManager.getBotGroups().containsKey(targetName)) {
            BotManager.BotGroup group = botManager.getBotGroups().get(targetName);
            for (UUID botId : group.getBotIds()) {
                BotEntity bot = botManager.getActiveBots().get(botId);
                if (bot != null) {
                    bot.setStatic(!bot.isStatic());
                }
            }
            AIBrigadeMod.LOGGER.info("Toggled static state for group {}", targetName);
        } else {
            // Try to find individual bot
            for (BotEntity bot : botManager.getActiveBots().values()) {
                if (bot.getBotName().equalsIgnoreCase(targetName)) {
                    bot.setStatic(!bot.isStatic());
                    AIBrigadeMod.LOGGER.info("Toggled static state for bot {}", targetName);
                    return;
                }
            }
        }
    }

    /**
     * Get AI thread pool size
     * @return Thread pool size
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    /**
     * Get the executor service for async operations
     * @return The thread pool executor
     */
    public ExecutorService getExecutor() {
        return aiThreadPool;
    }

    /**
     * Check if AI system is running
     * @return true if running
     */
    public boolean isRunning() {
        return isRunning;
    }
}
