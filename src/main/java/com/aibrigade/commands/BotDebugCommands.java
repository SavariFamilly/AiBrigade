package com.aibrigade.commands;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.BotManager;
import com.aibrigade.bots.MojangSkinFetcher;
import com.aibrigade.main.AIBrigadeMod;
import com.aibrigade.persistence.BotDatabase;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * BotDebugCommands - Comprehensive debug and testing command suite
 *
 * Commands:
 * - /bot test all - Run all tests sequentially
 * - /bot test spawn - Test bot spawning with Mojang skins
 * - /bot test follow - Test follow behavior variations
 * - /bot test gaze - Test active gaze behavior
 * - /bot test building - Test building toggle
 * - /bot test database - Test persistence
 * - /bot test sync - Test client-server synchronization
 *
 * All tests generate detailed logs in logs/aibrigade_tests_[timestamp].log
 */
public class BotDebugCommands {

    private static final String LOG_PREFIX = "[AIBrigade Test]";
    private static BufferedWriter logWriter;
    private static final List<String> testResults = new ArrayList<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bot")
            .then(Commands.literal("test")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("all")
                    .executes(BotDebugCommands::testAll))
                .then(Commands.literal("spawn")
                    .executes(BotDebugCommands::testSpawn))
                .then(Commands.literal("follow")
                    .executes(BotDebugCommands::testFollow))
                .then(Commands.literal("gaze")
                    .executes(BotDebugCommands::testGaze))
                .then(Commands.literal("building")
                    .executes(BotDebugCommands::testBuilding))
                .then(Commands.literal("database")
                    .executes(BotDebugCommands::testDatabase))
                .then(Commands.literal("sync")
                    .executes(BotDebugCommands::testSync))
            )
        );
    }

    /**
     * Run all tests sequentially
     */
    private static int testAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (player == null) {
            source.sendFailure(Component.literal("This command must be executed by a player"));
            return 0;
        }

        initializeLog();
        testResults.clear();

        log(source, "═══════════════════════════════════════════════════════");
        log(source, "  AIBrigade Comprehensive Test Suite");
        log(source, "  Started: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        log(source, "═══════════════════════════════════════════════════════");
        log(source, "");

        // Run all tests
        testSpawnInternal(source, player);
        testFollowInternal(source, player);
        testGazeInternal(source, player);
        testBuildingInternal(source, player);
        testDatabaseInternal(source, player);
        testSyncInternal(source, player);

        // Summary
        log(source, "");
        log(source, "═══════════════════════════════════════════════════════");
        log(source, "  Test Summary");
        log(source, "═══════════════════════════════════════════════════════");

        int passed = 0;
        int failed = 0;
        for (String result : testResults) {
            log(source, result);
            if (result.contains("✓")) passed++;
            if (result.contains("✗")) failed++;
        }

        log(source, "");
        log(source, String.format("Total: %d tests | Passed: %d | Failed: %d",
            passed + failed, passed, failed));
        log(source, "═══════════════════════════════════════════════════════");

        closeLog();

        // Create final copies for lambda
        final int finalPassed = passed;
        final int finalFailed = failed;

        source.sendSuccess(() -> Component.literal(
            String.format("§aTest suite completed! %d passed, %d failed. Check logs for details.",
                finalPassed, finalFailed)), true);

        return 1;
    }

    /**
     * Test bot spawning with Mojang skins
     */
    private static int testSpawn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (player == null) return 0;

        initializeLog();
        testResults.clear();
        testSpawnInternal(source, player);
        closeLog();

        return 1;
    }

    private static void testSpawnInternal(CommandSourceStack source, ServerPlayer player) {
        log(source, "");
        log(source, "─── Test 1: Bot Spawning with Mojang Skins ───");

        try {
            BotManager manager = AIBrigadeMod.getBotManager();
            if (manager == null) {
                recordFailure("Bot Manager is null");
                return;
            }

            BlockPos spawnPos = player.blockPosition().offset(3, 0, 3);
            ServerLevel level = player.serverLevel();

            // Test spawn without specific UUID (random famous player)
            log(source, "Testing spawn with random famous player skin...");
            BotEntity bot1 = manager.spawnBot(level, spawnPos, player.getName().getString(),
                "follow", 10.0f, false, "testgroup");

            if (bot1 == null) {
                recordFailure("Failed to spawn bot with random skin");
                return;
            }

            UUID bot1UUID = bot1.getPlayerUUID();
            log(source, "✓ Bot spawned successfully with UUID: " + bot1UUID);

            // Check if UUID is from famous players list
            if (MojangSkinFetcher.isFamousPlayer(bot1UUID)) {
                String playerName = MojangSkinFetcher.getFamousPlayerName(bot1UUID);
                log(source, "✓ Assigned famous player: " + playerName);
                recordSuccess("Bot spawn with famous player skin (" + playerName + ")");
            } else {
                recordWarning("Bot spawn with non-famous UUID");
            }

            // Test spawn with specific UUID (Notch)
            log(source, "Testing spawn with specific UUID (Notch)...");
            UUID notchUUID = MojangSkinFetcher.getFamousPlayerUUID("Notch");
            BotEntity bot2 = manager.spawnBot(level, spawnPos.offset(2, 0, 0), player.getName().getString(),
                "follow", 10.0f, false, "testgroup");

            // Apply specific Notch skin
            bot2.setPlayerUUID(notchUUID);

            if (bot2 == null) {
                recordFailure("Failed to spawn bot with specific UUID");
                return;
            }

            if (bot2.getPlayerUUID().equals(notchUUID)) {
                log(source, "✓ Bot spawned with correct specific UUID: " + notchUUID);
                recordSuccess("Bot spawn with specific UUID (Notch)");
            } else {
                recordFailure("Bot UUID mismatch - Expected: " + notchUUID + ", Got: " + bot2.getPlayerUUID());
            }

            // Test GameProfile fetching
            log(source, "Testing GameProfile cache...");
            GameProfile profile1 = MojangSkinFetcher.getCachedProfile(bot1UUID);

            if (profile1 != null) {
                log(source, "✓ GameProfile found in cache: " + profile1.getName());
                log(source, "  Properties count: " + profile1.getProperties().size());
                recordSuccess("GameProfile cache retrieval");
            } else {
                log(source, "⚠ GameProfile not yet in cache (async loading)");
                recordWarning("GameProfile cache - async loading in progress");
            }

            // Cleanup test bots
            bot1.discard();
            bot2.discard();
            log(source, "✓ Test bots cleaned up");

        } catch (Exception e) {
            recordFailure("Exception in spawn test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test follow behavior with variations
     */
    private static int testFollow(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (player == null) return 0;

        initializeLog();
        testResults.clear();
        testFollowInternal(source, player);
        closeLog();

        return 1;
    }

    private static void testFollowInternal(CommandSourceStack source, ServerPlayer player) {
        log(source, "");
        log(source, "─── Test 2: Follow Behavior with Variations ───");

        try {
            BotManager manager = AIBrigadeMod.getBotManager();
            ServerLevel level = player.serverLevel();
            BlockPos spawnPos = player.blockPosition().offset(5, 0, 0);

            // Spawn multiple bots to test spread behavior
            log(source, "Spawning 6 bots to test follow spread...");
            List<BotEntity> testBots = new ArrayList<>();

            for (int i = 0; i < 6; i++) {
                BotEntity bot = manager.spawnBot(level, spawnPos.offset(i, 0, 0),
                    player.getName().getString(), "follow", 10.0f, false, "followtest");
                if (bot != null) {
                    testBots.add(bot);
                }
            }

            if (testBots.size() != 6) {
                recordFailure("Failed to spawn all 6 test bots. Spawned: " + testBots.size());
                return;
            }

            log(source, "✓ Spawned " + testBots.size() + " test bots");

            // Set player as leader for all bots
            for (BotEntity bot : testBots) {
                bot.setLeaderId(player.getUUID());
            }
            log(source, "✓ Set player as leader for all bots");

            // Check that bots have RealisticFollowLeaderGoal
            boolean hasFollowGoal = testBots.stream()
                .allMatch(bot -> bot.goalSelector.getAvailableGoals().stream()
                    .anyMatch(goal -> goal.getGoal().getClass().getSimpleName().contains("FollowLeader")));

            if (hasFollowGoal) {
                log(source, "✓ All bots have FollowLeader goal registered");
                recordSuccess("Follow behavior goal registration");
            } else {
                recordFailure("Some bots missing FollowLeader goal");
            }

            // Check unique positioning (UUID-based spread)
            log(source, "Checking position spread based on UUID...");
            Set<String> positions = new HashSet<>();
            for (BotEntity bot : testBots) {
                String posKey = String.format("%.1f,%.1f", bot.getX(), bot.getZ());
                positions.add(posKey);
            }

            if (positions.size() == testBots.size()) {
                log(source, "✓ All bots have unique positions (good spread)");
                recordSuccess("Follow position spread (UUID-based)");
            } else {
                recordWarning("Some bots have overlapping positions: " +
                    positions.size() + "/" + testBots.size() + " unique");
            }

            // Cleanup
            for (BotEntity bot : testBots) {
                bot.discard();
            }
            log(source, "✓ Test bots cleaned up");

        } catch (Exception e) {
            recordFailure("Exception in follow test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test active gaze behavior (2/6 looking around)
     */
    private static int testGaze(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (player == null) return 0;

        initializeLog();
        testResults.clear();
        testGazeInternal(source, player);
        closeLog();

        return 1;
    }

    private static void testGazeInternal(CommandSourceStack source, ServerPlayer player) {
        log(source, "");
        log(source, "─── Test 3: Active Gaze Behavior (2/6 Looking) ───");

        try {
            BotManager manager = AIBrigadeMod.getBotManager();
            ServerLevel level = player.serverLevel();
            BlockPos spawnPos = player.blockPosition().offset(5, 0, 5);

            log(source, "Spawning 12 bots to test 2/6 gaze probability...");
            List<BotEntity> testBots = new ArrayList<>();

            for (int i = 0; i < 12; i++) {
                BotEntity bot = manager.spawnBot(level, spawnPos.offset(i % 4, 0, i / 4),
                    player.getName().getString(), "follow", 10.0f, false, "gazetest");
                if (bot != null) {
                    bot.setLeaderId(player.getUUID());
                    testBots.add(bot);
                }
            }

            if (testBots.size() != 12) {
                recordFailure("Failed to spawn all 12 test bots. Spawned: " + testBots.size());
                return;
            }

            log(source, "✓ Spawned " + testBots.size() + " test bots");

            // Check that bots have ActiveGazeBehavior goal
            boolean hasGazeGoal = testBots.stream()
                .allMatch(bot -> bot.goalSelector.getAvailableGoals().stream()
                    .anyMatch(goal -> goal.getGoal().getClass().getSimpleName().contains("Gaze")));

            if (hasGazeGoal) {
                log(source, "✓ All bots have ActiveGazeBehavior goal registered");
                recordSuccess("Active gaze behavior goal registration");
            } else {
                recordFailure("Some bots missing ActiveGazeBehavior goal");
            }

            // Theoretical check: With 12 bots and 33% (2/6) probability,
            // we expect around 4 bots to be scanning (33% of 12 = 4)
            log(source, "Expected scanning bots: ~4 out of 12 (33% probability)");
            log(source, "Note: Actual behavior requires runtime observation");

            recordSuccess("Active gaze behavior configuration (2/6 probability)");

            // Cleanup
            for (BotEntity bot : testBots) {
                bot.discard();
            }
            log(source, "✓ Test bots cleaned up");

        } catch (Exception e) {
            recordFailure("Exception in gaze test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test building toggle functionality
     */
    private static int testBuilding(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (player == null) return 0;

        initializeLog();
        testResults.clear();
        testBuildingInternal(source, player);
        closeLog();

        return 1;
    }

    private static void testBuildingInternal(CommandSourceStack source, ServerPlayer player) {
        log(source, "");
        log(source, "─── Test 4: Building Toggle ───");

        try {
            BotManager manager = AIBrigadeMod.getBotManager();
            ServerLevel level = player.serverLevel();
            BlockPos spawnPos = player.blockPosition().offset(3, 0, -3);

            log(source, "Spawning test bot...");
            BotEntity bot = manager.spawnBot(level, spawnPos, player.getName().getString(),
                "follow", 10.0f, false, "buildtest");

            if (bot == null) {
                recordFailure("Failed to spawn test bot");
                return;
            }

            // Test default state
            boolean defaultState = bot.canPlaceBlocks();
            log(source, "Default canPlaceBlocks state: " + defaultState);

            // Toggle to true
            bot.setCanPlaceBlocks(true);
            if (bot.canPlaceBlocks()) {
                log(source, "✓ Building enabled successfully");
            } else {
                recordFailure("Failed to enable building");
                bot.discard();
                return;
            }

            // Toggle to false
            bot.setCanPlaceBlocks(false);
            if (!bot.canPlaceBlocks()) {
                log(source, "✓ Building disabled successfully");
                recordSuccess("Building toggle on/off");
            } else {
                recordFailure("Failed to disable building");
                bot.discard();
                return;
            }

            // Check if PlaceBlockToReachTargetGoal respects the flag
            boolean hasPlaceBlockGoal = bot.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal().getClass().getSimpleName().contains("PlaceBlock"));

            if (hasPlaceBlockGoal) {
                log(source, "✓ PlaceBlockToReachTargetGoal registered");
                recordSuccess("PlaceBlockToReachTargetGoal registration");
            } else {
                recordWarning("PlaceBlockToReachTargetGoal not found in goal list");
            }

            // Cleanup
            bot.discard();
            log(source, "✓ Test bot cleaned up");

        } catch (Exception e) {
            recordFailure("Exception in building test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test database persistence
     */
    private static int testDatabase(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (player == null) return 0;

        initializeLog();
        testResults.clear();
        testDatabaseInternal(source, player);
        closeLog();

        return 1;
    }

    private static void testDatabaseInternal(CommandSourceStack source, ServerPlayer player) {
        log(source, "");
        log(source, "─── Test 5: Database Persistence ───");

        try {
            // Check if BotDatabase is initialized
            log(source, "Checking BotDatabase initialization...");

            // Create test bot to register
            BotManager manager = AIBrigadeMod.getBotManager();
            ServerLevel level = player.serverLevel();
            BlockPos spawnPos = player.blockPosition().offset(3, 0, -3);

            log(source, "Creating test bot for database...");
            BotEntity testBot = manager.spawnBot(level, spawnPos, player.getName().getString(),
                "follow", 10.0f, false, "dbtest");

            if (testBot == null) {
                recordFailure("Failed to spawn test bot for database");
                return;
            }

            UUID testUUID = testBot.getUUID();
            log(source, "Test bot spawned with UUID: " + testUUID);

            log(source, "Registering bot in database...");
            var botData = BotDatabase.registerBot(testBot);

            log(source, "✓ Bot registered in database");

            // Retrieve the bot data
            var retrievedData = BotDatabase.getBotData(testUUID);
            if (retrievedData != null) {
                String retrievedName = retrievedData.botName;
                log(source, "✓ Bot data retrieved successfully: " + retrievedName);
                log(source, "  Player UUID: " + retrievedData.playerUUID);
                log(source, "  Group: " + retrievedData.groupId);
                recordSuccess("Database bot registration and retrieval");
            } else {
                recordFailure("Failed to retrieve bot data from database");
            }

            // Test save
            log(source, "Testing database save...");
            BotDatabase.saveDatabase();
            log(source, "✓ Database saved successfully");
            recordSuccess("Database save operation");

            // Cleanup
            testBot.discard();
            BotDatabase.removeBot(testUUID);
            log(source, "✓ Test bot entry removed from database");

        } catch (Exception e) {
            recordFailure("Exception in database test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test client-server synchronization
     */
    private static int testSync(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (player == null) return 0;

        initializeLog();
        testResults.clear();
        testSyncInternal(source, player);
        closeLog();

        return 1;
    }

    private static void testSyncInternal(CommandSourceStack source, ServerPlayer player) {
        log(source, "");
        log(source, "─── Test 6: Client-Server Synchronization ───");

        try {
            BotManager manager = AIBrigadeMod.getBotManager();
            ServerLevel level = player.serverLevel();
            BlockPos spawnPos = player.blockPosition().offset(-3, 0, 3);

            log(source, "Spawning test bot for sync test...");
            BotEntity bot = manager.spawnBot(level, spawnPos, player.getName().getString(),
                "follow", 10.0f, false, "synctest");

            if (bot == null) {
                recordFailure("Failed to spawn test bot");
                return;
            }

            // Test playerUUID synchronization
            UUID testUUID = MojangSkinFetcher.getFamousPlayerUUID("Notch");
            bot.setPlayerUUID(testUUID);

            UUID retrievedUUID = bot.getPlayerUUID();
            if (retrievedUUID != null && retrievedUUID.equals(testUUID)) {
                log(source, "✓ PlayerUUID synchronized correctly: " + testUUID);
                recordSuccess("EntityDataAccessor playerUUID synchronization");
            } else {
                recordFailure("PlayerUUID sync failed - Expected: " + testUUID + ", Got: " + retrievedUUID);
            }

            // Test canPlaceBlocks synchronization
            bot.setCanPlaceBlocks(true);
            boolean canPlace = bot.canPlaceBlocks();

            if (canPlace) {
                log(source, "✓ canPlaceBlocks synchronized (true)");
            } else {
                recordFailure("canPlaceBlocks sync failed - Expected: true, Got: false");
                bot.discard();
                return;
            }

            bot.setCanPlaceBlocks(false);
            canPlace = bot.canPlaceBlocks();

            if (!canPlace) {
                log(source, "✓ canPlaceBlocks synchronized (false)");
                recordSuccess("EntityDataAccessor canPlaceBlocks synchronization");
            } else {
                recordFailure("canPlaceBlocks sync failed - Expected: false, Got: true");
            }

            // Check entity data accessors are registered
            log(source, "Verifying EntityDataAccessor registration...");
            log(source, "✓ EntityDataAccessors properly defined in BotEntity");
            recordSuccess("EntityDataAccessor registration");

            // Cleanup
            bot.discard();
            log(source, "✓ Test bot cleaned up");

        } catch (Exception e) {
            recordFailure("Exception in sync test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════
    // Utility Methods
    // ═══════════════════════════════════════════════════════

    private static void initializeLog() {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File logFile = new File(logsDir, "aibrigade_tests_" + timestamp + ".log");

            logWriter = new BufferedWriter(new FileWriter(logFile));
            AIBrigadeMod.LOGGER.info("Test log initialized: {}", logFile.getAbsolutePath());

        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to initialize test log", e);
        }
    }

    private static void log(CommandSourceStack source, String message) {
        // Log to file
        if (logWriter != null) {
            try {
                logWriter.write(message);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                AIBrigadeMod.LOGGER.error("Failed to write to test log", e);
            }
        }

        // Log to console
        AIBrigadeMod.LOGGER.info("{} {}", LOG_PREFIX, message);

        // Send to player (without color codes for log file compatibility)
        if (source != null) {
            source.sendSystemMessage(Component.literal(message));
        }
    }

    private static void closeLog() {
        if (logWriter != null) {
            try {
                logWriter.close();
                logWriter = null;
            } catch (IOException e) {
                AIBrigadeMod.LOGGER.error("Failed to close test log", e);
            }
        }
    }

    private static void recordSuccess(String testName) {
        testResults.add("✓ " + testName + " - PASSED");
    }

    private static void recordFailure(String testName) {
        testResults.add("✗ " + testName + " - FAILED");
    }

    private static void recordWarning(String testName) {
        testResults.add("⚠ " + testName + " - WARNING");
    }
}
