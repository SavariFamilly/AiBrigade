package com.aibrigade.main;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.BotManager;
import com.aibrigade.ai.AIManager;
import com.aibrigade.commands.BotCommandHandler;
import com.aibrigade.commands.BotBuildingCommands;
import com.aibrigade.commands.BotDebugCommands;
import com.aibrigade.persistence.BotDatabase;
import com.aibrigade.registry.ModEntities;
import com.aibrigade.utils.ConfigManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AIBrigade - Main Mod Class
 *
 * This mod allows spawning and controlling up to 300+ bot NPCs that behave like real players.
 * Bots can follow leaders, attack targets, patrol, disperse, climb obstacles, and execute
 * complex AI behaviors.
 *
 * Features:
 * - Advanced AI with SmartBrainLib integration
 * - Realistic pathfinding with Baritone API
 * - Animations via GeckoLib
 * - Dynamic group management and hostility system
 * - Individual bot customization (skins, equipment, inventory)
 * - Command-based control system
 * - Persistent bot/group data via JSON
 *
 * Compatible with Forge/NeoForge 1.21.1
 *
 * @author AIBrigade Team
 * @version 1.0.0
 */
@Mod(AIBrigadeMod.MOD_ID)
public class AIBrigadeMod {

    // Mod constants
    public static final String MOD_ID = "aibrigade";
    public static final String MODID = "aibrigade"; // Alias for compatibility
    public static final String MOD_NAME = "AIBrigade";
    public static final String VERSION = "1.0.0";

    // Logger for mod events and debugging
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    // Core managers
    private static BotManager botManager;
    private static AIManager aiManager;
    private static ConfigManager configManager;

    /**
     * Mod constructor - Called when mod is loaded by Forge
     * Registers event buses and initializes core systems
     */
    public AIBrigadeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOGGER.info("Initializing AIBrigade mod version {}", VERSION);

        // Register entity types
        ModEntities.register(modEventBus);

        // Register setup method
        modEventBus.addListener(this::setup);

        // Register entity attributes
        modEventBus.addListener(this::onEntityAttributeCreation);

        // Register this class for Forge events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("AIBrigade event buses registered successfully");
    }

    /**
     * Entity attribute creation event handler
     * Registers attributes for custom entities
     *
     * @param event The entity attribute creation event
     */
    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        LOGGER.info("Registering entity attributes for BotEntity");
        event.put(ModEntities.BOT.get(), BotEntity.createAttributes().build());
        LOGGER.info("BotEntity attributes registered successfully");
    }

    /**
     * Common setup phase - Initialize mod components
     * Called during FMLCommonSetupEvent
     *
     * @param event The setup event
     */
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Starting AIBrigade common setup");

        event.enqueueWork(() -> {
            // Initialize configuration manager
            configManager = new ConfigManager();
            configManager.loadConfiguration();
            LOGGER.info("Configuration loaded successfully");

            // Initialize bot manager
            botManager = new BotManager();
            LOGGER.info("Bot manager initialized");

            // Initialize AI manager with multithreading support
            aiManager = new AIManager(configManager.getAIThreadPoolSize());
            LOGGER.info("AI manager initialized with {} threads", configManager.getAIThreadPoolSize());

            // Register AI manager for events
            MinecraftForge.EVENT_BUS.register(aiManager);
            LOGGER.info("AI manager registered to event bus");

            // Verify dependencies
            verifyDependencies();

            LOGGER.info("AIBrigade setup completed successfully");
        });
    }

    /**
     * Server starting event handler
     * Loads persistent bot data and initializes server-side systems
     *
     * @param event The server starting event
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting - Loading AIBrigade data");

        // Initialize BotDatabase
        var worldPath = event.getServer().overworld().getLevel().getServer()
            .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        BotDatabase.initialize(worldPath);
        LOGGER.info("BotDatabase initialized at: {}", worldPath);

        // Load persistent bot and group data
        botManager.loadPersistentData(event.getServer());

        // Initialize AI tick handler
        aiManager.startAITicking(event.getServer());

        LOGGER.info("AIBrigade server initialization complete");
    }

    /**
     * Server stopping event handler
     * Saves persistent bot data and cleans up resources
     *
     * @param event The server stopping event
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - Saving AIBrigade data");

        // Save BotDatabase
        BotDatabase.saveDatabase();
        LOGGER.info("BotDatabase saved");

        // Save bot and group data
        botManager.savePersistentData(event.getServer());

        // Stop AI ticking and cleanup
        aiManager.stopAITicking();

        LOGGER.info("AIBrigade data saved and cleanup complete");
    }

    /**
     * Register commands event handler
     * Registers all AIBrigade commands
     *
     * @param event The register commands event
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering AIBrigade commands");
        BotCommandHandler.register(event.getDispatcher());
        BotBuildingCommands.register(event.getDispatcher());
        BotDebugCommands.register(event.getDispatcher());
        LOGGER.info("Commands registered successfully (including /bot building and /bot test)");
    }

    /**
     * Verify required mod dependencies are loaded
     * Logs warnings if optional dependencies are missing
     */
    private void verifyDependencies() {
        LOGGER.info("Verifying mod dependencies for AIBrigade");

        // Check for GeckoLib (required for animations)
        if (!checkModLoaded("geckolib")) {
            LOGGER.warn("GeckoLib not found - animations will be disabled");
        } else {
            LOGGER.info("GeckoLib found - animations enabled");
        }

        // Check for SmartBrainLib (required for advanced AI)
        if (!checkModLoaded("smartbrainlib")) {
            LOGGER.warn("SmartBrainLib not found - using basic AI behaviors");
        } else {
            LOGGER.info("SmartBrainLib found - advanced AI enabled");
        }

        // Check for Citadel (optional but recommended)
        if (!checkModLoaded("citadel")) {
            LOGGER.warn("Citadel not found - some AI features may be limited");
        } else {
            LOGGER.info("Citadel found - enhanced AI features enabled");
        }

        // Check for Easy NPC (optional integration)
        if (checkModLoaded("easynpc")) {
            LOGGER.info("Easy NPC found - NPC integration enabled");
        }

        LOGGER.info("Dependency verification complete");
    }

    /**
     * Check if a mod is loaded
     *
     * @param modId The mod ID to check
     * @return true if mod is loaded, false otherwise
     */
    private boolean checkModLoaded(String modId) {
        try {
            // Use ModList to check if mod is loaded
            return net.minecraftforge.fml.ModList.get().isLoaded(modId);
        } catch (Exception e) {
            LOGGER.error("Error checking for mod: {}", modId, e);
            return false;
        }
    }

    // Getters for managers

    /**
     * Get the bot manager instance
     * @return The bot manager
     */
    public static BotManager getBotManager() {
        return botManager;
    }

    /**
     * Get the AI manager instance
     * @return The AI manager
     */
    public static AIManager getAIManager() {
        return aiManager;
    }

    /**
     * Get the configuration manager instance
     * @return The configuration manager
     */
    public static ConfigManager getConfigManager() {
        return configManager;
    }
}
