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
 * Manages all AI behaviors for bots including:
 * - Goal execution and updates
 * - Multithreaded AI processing
 * - Behavior state transitions
 * - Group coordination
 * - Hostility management
 *
 * Uses a thread pool for efficient parallel AI processing
 */
public class AIManager {

    // Thread pool for AI processing
    private final ExecutorService aiThreadPool;
    private final int threadPoolSize;

    // AI tick counter (for performance optimization)
    private int tickCounter = 0;
    private static final int AI_UPDATE_INTERVAL = 4; // Update AI every 4 ticks
    private static final int CLEANUP_INTERVAL = 100; // Cleanup dead bots every 100 ticks (5 seconds)

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
     * Start AI ticking system
     *
     * @param server The minecraft server
     */
    public void startAITicking(MinecraftServer server) {
        this.server = server;
        this.isRunning = true;
        AIBrigadeMod.LOGGER.info("AI ticking started");
    }

    /**
     * Stop AI ticking and cleanup
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

        AIBrigadeMod.LOGGER.info("AI ticking stopped and cleanup complete");
    }

    /**
     * Server tick event handler
     * Updates AI for all active bots
     *
     * @param event The server tick event
     * Note: This method is not currently registered to an event bus.
     * TODO: Register this class to the event bus in AIBrigadeMod or make this static
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isRunning) {
            return;
        }

        tickCounter++;

        // Only update AI every N ticks for performance
        if (tickCounter % AI_UPDATE_INTERVAL == 0) {
            updateAllBotAI();
        }

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
     * Update AI for all active bots
     * Distributes bot AI updates across thread pool
     */
    private void updateAllBotAI() {
        BotManager botManager = AIBrigadeMod.getBotManager();
        if (botManager == null) {
            return;
        }

        Map<UUID, BotEntity> bots = botManager.getActiveBots();
        if (bots.isEmpty()) {
            return;
        }

        // Create update tasks for each bot
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (BotEntity bot : bots.values()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> updateBotAI(bot),
                aiThreadPool
            );
            futures.add(future);
        }

        // Wait for all updates to complete (non-blocking for game thread)
        // In production, you might want to handle this differently
        // to avoid any potential blocking
    }

    /**
     * Update AI for a single bot
     *
     * @param bot The bot entity
     */
    private void updateBotAI(BotEntity bot) {
        if (bot == null || !bot.isAlive()) {
            return;
        }

        try {
            // Update bot's AI state based on current conditions
            updateAIState(bot);

            // Execute current goals/behaviors
            executeBehavior(bot);

            // Check for hostility triggers
            checkHostilityTriggers(bot);

        } catch (Exception e) {
            AIBrigadeMod.LOGGER.error("Error updating AI for bot {}: {}",
                bot.getBotName(), e.getMessage());
        }
    }

    /**
     * Update bot's AI state based on current conditions
     *
     * @param bot The bot entity
     */
    private void updateAIState(BotEntity bot) {
        // Determine appropriate AI state based on:
        // - Current behavior type
        // - Leader position and distance
        // - Nearby threats
        // - Static/mobile setting
        // - Group coordination needs

        if (bot.isStatic()) {
            // Static bots only guard or idle
            if (hasNearbyThreats(bot)) {
                bot.setAIState(BotEntity.BotAIState.ATTACKING);
            } else {
                bot.setAIState(BotEntity.BotAIState.GUARDING);
            }
            return;
        }

        // Mobile bot state logic
        String behavior = bot.getBehaviorType();

        switch (behavior.toLowerCase()) {
            case "raid":
                if (hasNearbyThreats(bot)) {
                    bot.setAIState(BotEntity.BotAIState.ATTACKING);
                } else if (shouldFollowLeader(bot)) {
                    bot.setAIState(BotEntity.BotAIState.FOLLOWING);
                } else {
                    bot.setAIState(BotEntity.BotAIState.PATROLLING);
                }
                break;

            case "patrol":
                if (hasNearbyThreats(bot)) {
                    bot.setAIState(BotEntity.BotAIState.ATTACKING);
                } else {
                    bot.setAIState(BotEntity.BotAIState.PATROLLING);
                }
                break;

            case "guard":
                if (hasNearbyThreats(bot)) {
                    bot.setAIState(BotEntity.BotAIState.ATTACKING);
                } else if (isAwayFromHome(bot)) {
                    bot.setAIState(BotEntity.BotAIState.FOLLOWING); // Return to home
                } else {
                    bot.setAIState(BotEntity.BotAIState.GUARDING);
                }
                break;

            case "follow":
            default:
                if (hasNearbyThreats(bot)) {
                    bot.setAIState(BotEntity.BotAIState.ATTACKING);
                } else if (shouldFollowLeader(bot)) {
                    bot.setAIState(BotEntity.BotAIState.FOLLOWING);
                } else {
                    bot.setAIState(BotEntity.BotAIState.IDLE);
                }
                break;
        }
    }

    /**
     * Execute behavior based on bot's current AI state
     *
     * @param bot The bot entity
     */
    private void executeBehavior(BotEntity bot) {
        BotEntity.BotAIState state = bot.getAIState();

        switch (state) {
            case FOLLOWING:
                executeFollowBehavior(bot);
                break;

            case ATTACKING:
                executeAttackBehavior(bot);
                break;

            case PATROLLING:
                executePatrolBehavior(bot);
                break;

            case GUARDING:
                executeGuardBehavior(bot);
                break;

            case DISPERSING:
                executeDisperseBehavior(bot);
                break;

            case CLIMBING:
                executeClimbBehavior(bot);
                break;

            case FLEEING:
                executeFleeBehavior(bot);
                break;

            case IDLE:
            default:
                executeIdleBehavior(bot);
                break;
        }
    }

    /**
     * Execute follow leader behavior
     * Bot stays within follow radius of leader and mimics leader actions
     */
    private void executeFollowBehavior(BotEntity bot) {
        // TODO: Implement follow logic
        // - Get leader position
        // - Calculate distance to leader
        // - If too far, move towards leader
        // - If leader is climbing/jumping, mimic action
        // - Maintain slight dispersion to avoid clustering
        // - Use pathfinding for obstacle avoidance
    }

    /**
     * Execute attack behavior
     * Bot engages nearby hostile entities
     */
    private void executeAttackBehavior(BotEntity bot) {
        // TODO: Implement attack logic
        // - Find nearest hostile entity
        // - Move towards target
        // - Execute attack when in range
        // - Coordinate with group if applicable
        // - Switch to flee if health too low
    }

    /**
     * Execute patrol behavior
     * Bot patrols around home position or designated area
     */
    private void executePatrolBehavior(BotEntity bot) {
        // TODO: Implement patrol logic
        // - Define patrol waypoints
        // - Move between waypoints
        // - Scan for threats
        // - Return to patrol if distracted
    }

    /**
     * Execute guard behavior
     * Bot defends a specific position
     */
    private void executeGuardBehavior(BotEntity bot) {
        // TODO: Implement guard logic
        // - Stay near guard position
        // - Face random directions (scanning)
        // - Attack anything hostile that comes near
        // - Alert other bots in group
    }

    /**
     * Execute disperse behavior
     * Bot spreads out to avoid clustering
     */
    private void executeDisperseBehavior(BotEntity bot) {
        // TODO: Implement disperse logic
        // - Find nearby bots
        // - Calculate average position
        // - Move away from center
        // - Maintain minimum spacing
    }

    /**
     * Execute climb behavior
     * Bot climbs obstacles to follow leader or reach destination
     */
    private void executeClimbBehavior(BotEntity bot) {
        // TODO: Implement climb logic
        // - Detect obstacle height
        // - Jump and place blocks if needed
        // - Break blocks if necessary
        // - Use ladder/vine climbing
    }

    /**
     * Execute flee behavior
     * Bot retreats from danger
     */
    private void executeFleeBehavior(BotEntity bot) {
        // TODO: Implement flee logic
        // - Identify threat direction
        // - Move away from threat
        // - Seek cover
        // - Rejoin group when safe
    }

    /**
     * Execute idle behavior
     * Bot performs ambient actions
     */
    private void executeIdleBehavior(BotEntity bot) {
        // TODO: Implement idle logic
        // - Occasional look around
        // - Small random movements
        // - Play idle animations
    }

    /**
     * Check for hostility triggers
     * Determines if bot's group should become hostile to another
     */
    private void checkHostilityTriggers(BotEntity bot) {
        // TODO: Implement hostility checking
        // - Check if bot is being attacked
        // - Check if leader is being attacked
        // - Check if group member is being attacked
        // - If trigger detected, make entire group hostile
    }

    /**
     * Check if bot has nearby threats
     *
     * @param bot The bot entity
     * @return true if threats nearby
     */
    private boolean hasNearbyThreats(BotEntity bot) {
        // TODO: Implement threat detection
        // - Scan for hostile mobs
        // - Check for hostile bots from other groups
        // - Consider player attacks
        return false;
    }

    /**
     * Check if bot should follow leader
     *
     * @param bot The bot entity
     * @return true if should follow
     */
    private boolean shouldFollowLeader(BotEntity bot) {
        // TODO: Implement leader distance check
        // - Get leader position
        // - Calculate distance
        // - Return true if outside follow radius
        return false;
    }

    /**
     * Check if bot is away from home position
     *
     * @param bot The bot entity
     * @return true if away from home
     */
    private boolean isAwayFromHome(BotEntity bot) {
        // TODO: Implement home distance check
        if (bot.getHomePosition() == null) {
            return false;
        }

        double distance = bot.blockPosition().distSqr(bot.getHomePosition());
        return distance > (bot.getFollowRadius() * bot.getFollowRadius());
    }

    /**
     * Apply behavior to all bots in a group
     *
     * @param groupName The group name
     * @param behavior The behavior type
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
     * Set radius for all bots in a group
     *
     * @param groupName The group name
     * @param radius The new radius
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
     * Toggle static state for group or bot
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
}
