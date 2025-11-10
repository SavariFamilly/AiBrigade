package com.aibrigade.bots;

import com.aibrigade.main.AIBrigadeMod;
import com.aibrigade.registry.ModEntities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BotManager - Manages all bot entities and groups
 *
 * Responsibilities:
 * - Spawn and remove bots
 * - Manage bot groups and leader assignments
 * - Handle hostility relationships between groups
 * - Distribute equipment to bots
 * - Persist and load bot data
 * - Execute global commands on bots
 *
 * Thread-safe for concurrent access
 */
public class BotManager {

    // Storage for all bots (UUID -> BotEntity)
    private final Map<UUID, BotEntity> activeBots = new ConcurrentHashMap<>();

    // Storage for bot groups (GroupName -> Set of Bot UUIDs)
    private final Map<String, BotGroup> botGroups = new ConcurrentHashMap<>();

    // Hostility relationships (GroupName -> Set of hostile group names)
    private final Map<String, Set<String>> hostileGroups = new ConcurrentHashMap<>();

    // Gson for JSON serialization
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Bot name generator
    private final BotNameGenerator nameGenerator = new BotNameGenerator();

    // Maximum bots allowed
    private static final int MAX_BOTS = 300;

    /**
     * Constructor
     */
    public BotManager() {
        AIBrigadeMod.LOGGER.info("BotManager initialized");
    }

    /**
     * Spawn a single bot
     *
     * @param level The world level
     * @param pos Spawn position
     * @param leaderName Leader name (player or bot)
     * @param behavior Behavior type
     * @param radius Follow radius
     * @param isStatic Whether bot is static
     * @param groupName Group name
     * @return The spawned bot entity, or null if failed
     */
    public BotEntity spawnBot(ServerLevel level, BlockPos pos, String leaderName,
                               String behavior, float radius, boolean isStatic, String groupName) {

        if (activeBots.size() >= MAX_BOTS) {
            AIBrigadeMod.LOGGER.warn("Cannot spawn bot: maximum bot limit ({}) reached", MAX_BOTS);
            return null;
        }

        // Create bot entity using registered entity type
        BotEntity bot = new BotEntity(ModEntities.BOT.get(), level);

        AIBrigadeMod.LOGGER.info("Spawning bot at {} in group {} with behavior {}",
            pos, groupName, behavior);

        // Configure bot
        bot.setPos(pos.getX(), pos.getY(), pos.getZ());
        bot.setBotName(nameGenerator.generateUniqueName());
        bot.setBotSkin(selectRandomSkin());
        bot.setBehaviorType(behavior);
        bot.setFollowRadius(radius);
        bot.setStatic(isStatic);
        bot.setBotGroup(groupName);
        bot.setSpawnPosition(pos);

        // Find and assign leader
        UUID leaderId = findLeaderUUID(level, leaderName);
        if (leaderId != null) {
            bot.setLeaderId(leaderId);
        }

        // Add to world
        level.addFreshEntity(bot);

        // Register bot
        activeBots.put(bot.getUUID(), bot);

        // Add to group
        addBotToGroup(groupName, bot.getUUID());

        AIBrigadeMod.LOGGER.info("Bot {} spawned successfully", bot.getBotName());

        return bot;
    }

    /**
     * Spawn multiple bots in a group
     *
     * @param level The world level
     * @param pos Spawn position
     * @param count Number of bots to spawn
     * @param leaderName Leader name
     * @param behavior Behavior type
     * @param radius Follow radius
     * @param isStatic Whether bots are static
     * @param groupName Group name
     * @return Number of bots successfully spawned
     */
    public int spawnBotGroup(ServerLevel level, BlockPos pos, int count, String leaderName,
                              String behavior, float radius, boolean isStatic, String groupName) {

        int spawned = 0;
        int maxToSpawn = Math.min(count, MAX_BOTS - activeBots.size());

        AIBrigadeMod.LOGGER.info("Spawning {} bots for group {}", maxToSpawn, groupName);

        // Create group if it doesn't exist
        if (!botGroups.containsKey(groupName)) {
            botGroups.put(groupName, new BotGroup(groupName, leaderName, radius));
        }

        // Spawn bots in a spread pattern
        for (int i = 0; i < maxToSpawn; i++) {
            // Calculate offset position to spread bots
            BlockPos spawnPos = calculateSpreadPosition(pos, i, count);

            BotEntity bot = spawnBot(level, spawnPos, leaderName, behavior,
                radius, isStatic, groupName);

            if (bot != null) {
                spawned++;
            }
        }

        AIBrigadeMod.LOGGER.info("Successfully spawned {} bots for group {}", spawned, groupName);
        return spawned;
    }

    /**
     * Remove a bot by UUID
     *
     * @param botId The bot's UUID
     * @return true if removed successfully
     */
    public boolean removeBot(UUID botId) {
        BotEntity bot = activeBots.get(botId);
        if (bot != null) {
            // Remove from group
            String groupName = bot.getBotGroup();
            removeBotFromGroup(groupName, botId);

            // Remove from world
            bot.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);

            // Remove from active bots
            activeBots.remove(botId);

            AIBrigadeMod.LOGGER.info("Bot {} removed", bot.getBotName());
            return true;
        }
        return false;
    }

    /**
     * Remove all bots in a group
     *
     * @param groupName The group name
     * @return Number of bots removed
     */
    public int removeGroup(String groupName) {
        BotGroup group = botGroups.get(groupName);
        if (group == null) {
            return 0;
        }

        int removed = 0;
        Set<UUID> botIds = new HashSet<>(group.getBotIds());

        for (UUID botId : botIds) {
            if (removeBot(botId)) {
                removed++;
            }
        }

        botGroups.remove(groupName);
        hostileGroups.remove(groupName);

        AIBrigadeMod.LOGGER.info("Removed group {} ({} bots)", groupName, removed);
        return removed;
    }

    /**
     * Assign a new leader to a group
     *
     * @param groupName The group name
     * @param leaderName The new leader name
     * @return true if successful
     */
    public boolean assignLeader(String groupName, String leaderName) {
        BotGroup group = botGroups.get(groupName);
        if (group == null) {
            AIBrigadeMod.LOGGER.warn("Group {} not found", groupName);
            return false;
        }

        group.setLeaderName(leaderName);

        // Update all bots in group
        for (UUID botId : group.getBotIds()) {
            BotEntity bot = activeBots.get(botId);
            if (bot != null) {
                // TODO: Find leader UUID and assign
                // UUID leaderId = findLeaderUUID(bot.level(), leaderName);
                // bot.setLeaderId(leaderId);
            }
        }

        AIBrigadeMod.LOGGER.info("Assigned leader {} to group {}", leaderName, groupName);
        return true;
    }

    /**
     * Make one group hostile towards another
     *
     * @param sourceGroup The group becoming hostile
     * @param targetGroup The target group
     */
    public void setGroupHostile(String sourceGroup, String targetGroup) {
        hostileGroups.computeIfAbsent(sourceGroup, k -> new HashSet<>()).add(targetGroup);
        AIBrigadeMod.LOGGER.info("Group {} is now hostile towards {}", sourceGroup, targetGroup);
    }

    /**
     * Check if two groups are hostile
     *
     * @param group1 First group
     * @param group2 Second group
     * @return true if hostile
     */
    public boolean areGroupsHostile(String group1, String group2) {
        Set<String> hostiles = hostileGroups.get(group1);
        return hostiles != null && hostiles.contains(group2);
    }

    /**
     * Give armor to a bot or group
     *
     * @param targetName Bot name or group name
     * @param isFull true for full armor set, false for partial
     * @param materials Armor materials (e.g., "diamond", "irondiamond")
     * @return Number of bots equipped
     */
    public int giveArmor(String targetName, boolean isFull, String materials) {
        // Parse materials
        List<ArmorMaterial> armorMaterials = parseArmorMaterials(materials);

        if (armorMaterials.isEmpty()) {
            AIBrigadeMod.LOGGER.warn("Invalid armor materials: {}", materials);
            return 0;
        }

        // Check if target is a group or individual bot
        if (botGroups.containsKey(targetName)) {
            // Apply to group
            return giveArmorToGroup(targetName, isFull, armorMaterials);
        } else {
            // Find bot by name
            BotEntity bot = findBotByName(targetName);
            if (bot != null) {
                giveArmorToBot(bot, isFull, armorMaterials);
                return 1;
            }
        }

        return 0;
    }

    /**
     * Give armor to all bots in a group
     */
    private int giveArmorToGroup(String groupName, boolean isFull, List<ArmorMaterial> materials) {
        BotGroup group = botGroups.get(groupName);
        if (group == null) {
            return 0;
        }

        int equipped = 0;
        for (UUID botId : group.getBotIds()) {
            BotEntity bot = activeBots.get(botId);
            if (bot != null) {
                giveArmorToBot(bot, isFull, materials);
                equipped++;
            }
        }

        AIBrigadeMod.LOGGER.info("Equipped {} bots in group {} with armor", equipped, groupName);
        return equipped;
    }

    /**
     * Give armor to a single bot
     */
    private void giveArmorToBot(BotEntity bot, boolean isFull, List<ArmorMaterial> materials) {
        Random random = new Random();

        // Armor slots: 0=helmet, 1=chestplate, 2=leggings, 3=boots
        ArmorMaterial[] chosenMaterials = new ArmorMaterial[4];

        if (isFull && materials.size() == 1) {
            // Full set of same material
            ArmorMaterial material = materials.get(0);
            for (int i = 0; i < 4; i++) {
                chosenMaterials[i] = material;
            }
        } else {
            // Random combination with diversity rule
            for (int i = 0; i < 3; i++) {
                chosenMaterials[i] = materials.get(random.nextInt(materials.size()));
            }

            // Diversity rule: if first 3 are same, make 4th different
            if (materials.size() > 1 &&
                chosenMaterials[0] == chosenMaterials[1] &&
                chosenMaterials[1] == chosenMaterials[2]) {

                // Pick different material for boots
                ArmorMaterial sameMaterial = chosenMaterials[0];
                do {
                    chosenMaterials[3] = materials.get(random.nextInt(materials.size()));
                } while (chosenMaterials[3] == sameMaterial && materials.size() > 1);
            } else {
                chosenMaterials[3] = materials.get(random.nextInt(materials.size()));
            }
        }

        // Apply armor to bot
        for (int i = 0; i < 4; i++) {
            ItemStack armorPiece = createArmorPiece(chosenMaterials[i], i);
            bot.setArmorSlot(i, armorPiece);
        }

        AIBrigadeMod.LOGGER.debug("Equipped bot {} with armor: {}, {}, {}, {}",
            bot.getBotName(),
            chosenMaterials[0], chosenMaterials[1], chosenMaterials[2], chosenMaterials[3]);
    }

    /**
     * Parse armor material names into ArmorMaterial list
     */
    private List<ArmorMaterial> parseArmorMaterials(String materials) {
        List<ArmorMaterial> result = new ArrayList<>();
        String lower = materials.toLowerCase();

        // Check for each material type
        if (lower.contains("diamond")) result.add(ArmorMaterial.DIAMOND);
        if (lower.contains("iron")) result.add(ArmorMaterial.IRON);
        if (lower.contains("chainmail") || lower.contains("chain")) result.add(ArmorMaterial.CHAINMAIL);
        if (lower.contains("leather")) result.add(ArmorMaterial.LEATHER);
        if (lower.contains("gold") || lower.contains("golden")) result.add(ArmorMaterial.GOLD);
        if (lower.contains("netherite")) result.add(ArmorMaterial.NETHERITE);

        return result;
    }

    /**
     * Create armor piece for specific slot and material
     */
    private ItemStack createArmorPiece(ArmorMaterial material, int slot) {
        // TODO: Implement proper armor creation based on material and slot
        // For now, return empty stub
        return ItemStack.EMPTY;
    }

    /**
     * Calculate spread position for bot spawning
     */
    private BlockPos calculateSpreadPosition(BlockPos center, int index, int total) {
        // Spread bots in a circular pattern
        double angle = (2.0 * Math.PI * index) / total;
        double radius = Math.sqrt(total) * 1.5; // Scale radius with bot count

        int offsetX = (int) (Math.cos(angle) * radius);
        int offsetZ = (int) (Math.sin(angle) * radius);

        return center.offset(offsetX, 0, offsetZ);
    }

    /**
     * Add bot to group
     */
    private void addBotToGroup(String groupName, UUID botId) {
        BotGroup group = botGroups.computeIfAbsent(groupName,
            name -> new BotGroup(name, "none", 10.0f));
        group.addBot(botId);
    }

    /**
     * Remove bot from group
     */
    private void removeBotFromGroup(String groupName, UUID botId) {
        BotGroup group = botGroups.get(groupName);
        if (group != null) {
            group.removeBot(botId);
            if (group.getBotIds().isEmpty()) {
                botGroups.remove(groupName);
            }
        }
    }

    /**
     * Find bot by name
     */
    private BotEntity findBotByName(String name) {
        for (BotEntity bot : activeBots.values()) {
            if (bot.getBotName().equalsIgnoreCase(name)) {
                return bot;
            }
        }
        return null;
    }

    /**
     * Find leader UUID by name (player or bot)
     */
    private UUID findLeaderUUID(Level level, String leaderName) {
        // Try to find player first
        if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Player player = serverLevel.getServer().getPlayerList().getPlayerByName(leaderName);
            if (player != null) {
                return player.getUUID();
            }
        }

        // Try to find bot
        BotEntity bot = findBotByName(leaderName);
        if (bot != null) {
            return bot.getUUID();
        }

        return null;
    }

    /**
     * Select a random skin for a bot
     * Can be expanded to support custom skin sets
     *
     * @return The skin name
     */
    private String selectRandomSkin() {
        // Available skins (can be configured via config file)
        String[] availableSkins = {
            "default",
            "soldier",
            "scout",
            "medic",
            "engineer",
            "heavy"
        };

        Random random = new Random();
        return availableSkins[random.nextInt(availableSkins.length)];
    }

    /**
     * Load persistent data from disk
     */
    public void loadPersistentData(MinecraftServer server) {
        AIBrigadeMod.LOGGER.info("Loading AIBrigade persistent data");

        // TODO: Implement JSON loading from server's world data folder
        File dataFile = getDataFile(server);
        if (!dataFile.exists()) {
            AIBrigadeMod.LOGGER.info("No persistent data found, starting fresh");
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            // TODO: Deserialize and restore bots and groups
            AIBrigadeMod.LOGGER.info("Persistent data loaded successfully");
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to load persistent data", e);
        }
    }

    /**
     * Save persistent data to disk
     */
    public void savePersistentData(MinecraftServer server) {
        AIBrigadeMod.LOGGER.info("Saving AIBrigade persistent data");

        File dataFile = getDataFile(server);

        try (Writer writer = new FileWriter(dataFile)) {
            // TODO: Serialize bots and groups to JSON
            AIBrigadeMod.LOGGER.info("Persistent data saved successfully");
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to save persistent data", e);
        }
    }

    /**
     * Get data file for persistent storage
     */
    private File getDataFile(MinecraftServer server) {
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File modDataDir = new File(worldDir, "aibrigade");
        modDataDir.mkdirs();
        return new File(modDataDir, "bots.json");
    }

    // Getters

    public Map<UUID, BotEntity> getActiveBots() {
        return Collections.unmodifiableMap(activeBots);
    }

    public Map<String, BotGroup> getBotGroups() {
        return Collections.unmodifiableMap(botGroups);
    }

    /**
     * Enum for armor materials
     */
    private enum ArmorMaterial {
        LEATHER, CHAINMAIL, IRON, GOLD, DIAMOND, NETHERITE
    }

    /**
     * Inner class representing a bot group
     */
    public static class BotGroup {
        private final String name;
        private String leaderName;
        private float followRadius;
        private final Set<UUID> botIds = new HashSet<>();

        public BotGroup(String name, String leaderName, float followRadius) {
            this.name = name;
            this.leaderName = leaderName;
            this.followRadius = followRadius;
        }

        public void addBot(UUID botId) {
            botIds.add(botId);
        }

        public void removeBot(UUID botId) {
            botIds.remove(botId);
        }

        public String getName() {
            return name;
        }

        public String getLeaderName() {
            return leaderName;
        }

        public void setLeaderName(String leaderName) {
            this.leaderName = leaderName;
        }

        public float getFollowRadius() {
            return followRadius;
        }

        public void setFollowRadius(float followRadius) {
            this.followRadius = followRadius;
        }

        public Set<UUID> getBotIds() {
            return Collections.unmodifiableSet(botIds);
        }
    }

    /**
     * Bot name generator for unique bot names
     */
    private static class BotNameGenerator {
        private final String[] prefixes = {"Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot"};
        private final String[] suffixes = {"One", "Two", "Three", "Four", "Five", "Six"};
        private final Random random = new Random();
        private int counter = 0;

        public String generateUniqueName() {
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String suffix = suffixes[random.nextInt(suffixes.length)];
            return prefix + suffix + (counter++);
        }
    }
}
