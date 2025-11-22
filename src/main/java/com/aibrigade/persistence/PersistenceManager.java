package com.aibrigade.persistence;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.main.AIBrigadeMod;
import com.google.gson.*;
import net.minecraft.server.level.ServerLevel;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * PersistenceManager - Manages saving and loading bot data to/from disk
 *
 * Features:
 * - Auto-save on world save
 * - Load bots on world load
 * - Save groups and configurations
 * - Preset management
 * - Backup system
 */
public class PersistenceManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FOLDER = "aibrigade";
    private static final String BOTS_FILE = "bots.json";
    private static final String GROUPS_FILE = "groups.json";
    private static final String CONFIG_FILE = "config.json";
    private static final String PRESETS_FILE = "presets.json";

    private final Path dataPath;
    private boolean autoSaveEnabled = true;

    /**
     * Initialize persistence manager
     */
    public PersistenceManager(Path worldPath) {
        this.dataPath = worldPath.resolve(DATA_FOLDER);
        try {
            Files.createDirectories(dataPath);
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to create AIBrigade data directory", e);
        }
    }

    // ==================== BOT PERSISTENCE ====================

    /**
     * Save all bots to disk
     */
    public void saveBots(Collection<BotEntity> bots) {
        // MAJOR FIX #36: Add null check on bots parameter
        if (bots == null) {
            AIBrigadeMod.LOGGER.warn("Cannot save bots - collection is null");
            return;
        }

        JsonArray botsArray = new JsonArray();

        for (BotEntity bot : bots) {
            // Skip null bots in collection
            if (bot == null) {
                continue;
            }

            try {
                JsonObject botJson = BotDataSerializer.serializeBotToJson(bot);
                botsArray.add(botJson);
            } catch (Exception e) {
                AIBrigadeMod.LOGGER.error("Failed to serialize bot: " + bot.getBotName(), e);
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty("version", "1.0");
        root.addProperty("timestamp", System.currentTimeMillis());
        root.addProperty("count", botsArray.size());
        root.add("bots", botsArray);

        saveJsonFile(dataPath.resolve(BOTS_FILE), root);
        AIBrigadeMod.LOGGER.info("Saved {} bots to disk", botsArray.size());
    }

    /**
     * Load all bots from disk
     */
    public List<BotDataSerializer.BotData> loadBots() {
        List<BotDataSerializer.BotData> bots = new ArrayList<>();
        Path botsFile = dataPath.resolve(BOTS_FILE);

        if (!Files.exists(botsFile)) {
            AIBrigadeMod.LOGGER.info("No saved bots file found");
            return bots;
        }

        try {
            JsonObject root = loadJsonFile(botsFile);
            if (root == null || !root.has("bots")) {
                return bots;
            }

            JsonArray botsArray = root.getAsJsonArray("bots");
            for (JsonElement element : botsArray) {
                try {
                    BotDataSerializer.BotData botData = BotDataSerializer.deserializeBotFromJson(element.getAsJsonObject());
                    bots.add(botData);
                } catch (Exception e) {
                    AIBrigadeMod.LOGGER.error("Failed to deserialize bot", e);
                }
            }

            AIBrigadeMod.LOGGER.info("Loaded {} bots from disk", bots.size());
        } catch (Exception e) {
            AIBrigadeMod.LOGGER.error("Failed to load bots file", e);
        }

        return bots;
    }

    // ==================== GROUP PERSISTENCE ====================

    /**
     * Save group data
     */
    public void saveGroups(Map<String, List<UUID>> groups) {
        // MAJOR FIX #36: Add null check on groups parameter
        if (groups == null) {
            AIBrigadeMod.LOGGER.warn("Cannot save groups - map is null");
            return;
        }

        JsonObject root = new JsonObject();
        root.addProperty("version", "1.0");
        root.addProperty("timestamp", System.currentTimeMillis());

        JsonObject groupsObj = new JsonObject();
        for (Map.Entry<String, List<UUID>> entry : groups.entrySet()) {
            // Skip null entries
            if (entry == null || entry.getValue() == null) {
                continue;
            }

            JsonArray uuidsArray = new JsonArray();
            for (UUID uuid : entry.getValue()) {
                if (uuid != null) {
                    uuidsArray.add(uuid.toString());
                }
            }
            groupsObj.add(entry.getKey(), uuidsArray);
        }

        root.add("groups", groupsObj);
        saveJsonFile(dataPath.resolve(GROUPS_FILE), root);
        AIBrigadeMod.LOGGER.info("Saved {} groups to disk", groups.size());
    }

    /**
     * Load group data
     */
    public Map<String, List<UUID>> loadGroups() {
        Map<String, List<UUID>> groups = new HashMap<>();
        Path groupsFile = dataPath.resolve(GROUPS_FILE);

        if (!Files.exists(groupsFile)) {
            AIBrigadeMod.LOGGER.info("No saved groups file found");
            return groups;
        }

        try {
            JsonObject root = loadJsonFile(groupsFile);
            if (root == null || !root.has("groups")) {
                return groups;
            }

            JsonObject groupsObj = root.getAsJsonObject("groups");
            for (Map.Entry<String, JsonElement> entry : groupsObj.entrySet()) {
                List<UUID> uuids = new ArrayList<>();
                JsonArray uuidsArray = entry.getValue().getAsJsonArray();

                for (JsonElement element : uuidsArray) {
                    try {
                        uuids.add(UUID.fromString(element.getAsString()));
                    } catch (IllegalArgumentException e) {
                        AIBrigadeMod.LOGGER.error("Invalid UUID in group: " + entry.getKey(), e);
                    }
                }

                groups.put(entry.getKey(), uuids);
            }

            AIBrigadeMod.LOGGER.info("Loaded {} groups from disk", groups.size());
        } catch (Exception e) {
            AIBrigadeMod.LOGGER.error("Failed to load groups file", e);
        }

        return groups;
    }

    // ==================== CONFIG PERSISTENCE ====================

    /**
     * Save configuration
     */
    public void saveConfig(JsonObject config) {
        JsonObject root = new JsonObject();
        root.addProperty("version", "1.0");
        root.addProperty("timestamp", System.currentTimeMillis());
        root.add("config", config);

        saveJsonFile(dataPath.resolve(CONFIG_FILE), root);
        AIBrigadeMod.LOGGER.info("Saved configuration to disk");
    }

    /**
     * Load configuration
     */
    public JsonObject loadConfig() {
        Path configFile = dataPath.resolve(CONFIG_FILE);

        if (!Files.exists(configFile)) {
            AIBrigadeMod.LOGGER.info("No saved config file found, using defaults");
            return createDefaultConfig();
        }

        try {
            JsonObject root = loadJsonFile(configFile);
            if (root != null && root.has("config")) {
                AIBrigadeMod.LOGGER.info("Loaded configuration from disk");
                return root.getAsJsonObject("config");
            }
        } catch (Exception e) {
            AIBrigadeMod.LOGGER.error("Failed to load config file", e);
        }

        return createDefaultConfig();
    }

    /**
     * Create default configuration
     */
    private JsonObject createDefaultConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("maxBots", 300);
        config.addProperty("autoSave", true);
        config.addProperty("saveInterval", 300); // seconds
        config.addProperty("defaultBehavior", "idle");
        config.addProperty("defaultFollowRadius", 10.0);
        return config;
    }

    // ==================== PRESET PERSISTENCE ====================

    /**
     * Save presets
     */
    public void savePresets(Map<String, JsonObject> presets) {
        // MAJOR FIX #36: Add null check on presets parameter
        if (presets == null) {
            AIBrigadeMod.LOGGER.warn("Cannot save presets - map is null");
            return;
        }

        JsonObject root = new JsonObject();
        root.addProperty("version", "1.0");
        root.addProperty("timestamp", System.currentTimeMillis());

        JsonObject presetsObj = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : presets.entrySet()) {
            // Skip null entries
            if (entry == null || entry.getValue() == null) {
                continue;
            }
            presetsObj.add(entry.getKey(), entry.getValue());
        }

        root.add("presets", presetsObj);
        saveJsonFile(dataPath.resolve(PRESETS_FILE), root);
        AIBrigadeMod.LOGGER.info("Saved {} presets to disk", presets.size());
    }

    /**
     * Load presets
     */
    public Map<String, JsonObject> loadPresets() {
        Map<String, JsonObject> presets = new HashMap<>();
        Path presetsFile = dataPath.resolve(PRESETS_FILE);

        if (!Files.exists(presetsFile)) {
            AIBrigadeMod.LOGGER.info("No saved presets file found");
            return presets;
        }

        try {
            JsonObject root = loadJsonFile(presetsFile);
            if (root == null || !root.has("presets")) {
                return presets;
            }

            JsonObject presetsObj = root.getAsJsonObject("presets");
            for (Map.Entry<String, JsonElement> entry : presetsObj.entrySet()) {
                presets.put(entry.getKey(), entry.getValue().getAsJsonObject());
            }

            AIBrigadeMod.LOGGER.info("Loaded {} presets from disk", presets.size());
        } catch (Exception e) {
            AIBrigadeMod.LOGGER.error("Failed to load presets file", e);
        }

        return presets;
    }

    // ==================== BACKUP SYSTEM ====================

    /**
     * Create backup of current data
     */
    public void createBackup() {
        Path backupPath = dataPath.resolve("backups");
        try {
            Files.createDirectories(backupPath);

            String timestamp = String.valueOf(System.currentTimeMillis());
            Path backupFolder = backupPath.resolve("backup_" + timestamp);
            Files.createDirectories(backupFolder);

            // Copy all data files
            copyFileIfExists(dataPath.resolve(BOTS_FILE), backupFolder.resolve(BOTS_FILE));
            copyFileIfExists(dataPath.resolve(GROUPS_FILE), backupFolder.resolve(GROUPS_FILE));
            copyFileIfExists(dataPath.resolve(CONFIG_FILE), backupFolder.resolve(CONFIG_FILE));
            copyFileIfExists(dataPath.resolve(PRESETS_FILE), backupFolder.resolve(PRESETS_FILE));

            AIBrigadeMod.LOGGER.info("Created backup at: " + backupFolder);

            // Clean old backups (keep last 10)
            cleanOldBackups(backupPath, 10);
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to create backup", e);
        }
    }

    /**
     * Clean old backups, keeping only the most recent N
     */
    private void cleanOldBackups(Path backupPath, int keepCount) {
        try {
            List<Path> backups = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupPath, "backup_*")) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        backups.add(entry);
                    }
                }
            }

            // Sort by modification time, newest first
            backups.sort((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (IOException e) {
                    return 0;
                }
            });

            // Delete old backups
            for (int i = keepCount; i < backups.size(); i++) {
                deleteDirectory(backups.get(i));
            }
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to clean old backups", e);
        }
    }

    /**
     * Delete directory recursively
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        // MAJOR FIX #35: Wrap Files.walk() in try-with-resources to prevent resource leak
        // Files.walk() returns a Stream that holds open file descriptors
        // Without proper closure, this causes file handle leaks (especially on Windows)
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        AIBrigadeMod.LOGGER.error("Failed to delete: " + path, e);
                    }
                });
        }
    }

    // ==================== FILE I/O ====================

    /**
     * Save JSON to file
     */
    private void saveJsonFile(Path file, JsonObject json) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to save JSON file: " + file, e);
        }
    }

    /**
     * Load JSON from file
     */
    private JsonObject loadJsonFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to load JSON file: " + file, e);
            return null;
        }
    }

    /**
     * Copy file if it exists
     */
    private void copyFileIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ==================== UTILITY ====================

    /**
     * Check if auto-save is enabled
     */
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    /**
     * Set auto-save enabled
     */
    public void setAutoSaveEnabled(boolean enabled) {
        this.autoSaveEnabled = enabled;
    }

    /**
     * Get data path
     */
    public Path getDataPath() {
        return dataPath;
    }
}
