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

    // Team relationships (GroupName -> Map<OtherGroupName, Relationship>)
    private final Map<String, Map<String, TeamRelationship>> teamRelationships = new ConcurrentHashMap<>();

    // Player relationships with groups (PlayerUUID -> Map<GroupName, Relationship>)
    private final Map<UUID, Map<String, TeamRelationship>> playerRelationships = new ConcurrentHashMap<>();

    // Gson for JSON serialization
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
        // Le constructeur de BotEntity applique automatiquement:
        // - MojangSkinFetcher.applyRandomFamousSkin(this)
        // - RandomEquipment.equipRandomItem(this)
        BotEntity bot = new BotEntity(ModEntities.BOT.get(), level);

        AIBrigadeMod.LOGGER.info("Spawning bot at {} in group {} with behavior {}",
            pos, groupName, behavior);

        // Configure bot (NE PAS écraser le nom et skin déjà appliqués dans le constructeur)
        bot.setPos(pos.getX(), pos.getY(), pos.getZ());
        // bot.setBotName() et bot.setBotSkin() sont déjà définis par le constructeur via MojangSkinFetcher
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

        // L'équipement est déjà appliqué dans le constructeur via RandomEquipment.equipRandomItem()
        // Ne pas appeler giveStartingEquipment() qui écrase l'équipement

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
     * Remove a bot by UUID (command-triggered removal)
     *
     * @param botId The bot's UUID
     * @return true if removed successfully
     */
    public boolean removeBot(UUID botId) {
        BotEntity bot = activeBots.get(botId);
        if (bot != null) {
            // Remove from world (this will trigger onBotRemoved via BotEntity.remove())
            bot.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            return true;
        }
        return false;
    }

    /**
     * Internal cleanup method - called when a bot dies or is removed
     * Centralizes all cleanup logic to avoid duplication
     *
     * @param bot The bot to cleanup
     * @param reason Reason for cleanup (for logging)
     */
    private void cleanupBot(BotEntity bot, String reason) {
        if (bot == null) return;

        UUID botId = bot.getUUID();

        // Check if already cleaned (avoid double cleanup)
        if (!activeBots.containsKey(botId)) {
            return;
        }

        String groupName = bot.getBotGroup();
        String botName = bot.getBotName();

        // Remove from group
        removeBotFromGroup(groupName, botId);

        // Remove from active bots
        activeBots.remove(botId);

        AIBrigadeMod.LOGGER.info("Bot {} {} (remaining: {}/{})",
            botName, reason, activeBots.size(), MAX_BOTS);
    }

    /**
     * Called when a bot is removed from the world
     * This is called automatically by BotEntity.remove()
     *
     * @param bot The bot being removed
     */
    public void onBotRemoved(BotEntity bot) {
        cleanupBot(bot, "removed from world");
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
        teamRelationships.remove(groupName);

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

        // Find leader UUID (player or bot)
        UUID leaderId = null;

        // Update all bots in group - copy set to avoid concurrent modification
        for (UUID botId : new HashSet<>(group.getBotIds())) {
            BotEntity bot = activeBots.get(botId);
            if (bot != null) {
                // Find leader UUID on first iteration (using first bot's level)
                if (leaderId == null) {
                    leaderId = findLeaderUUID(bot.level(), leaderName);
                    if (leaderId == null) {
                        AIBrigadeMod.LOGGER.warn("Leader '{}' not found (not a player or bot)", leaderName);
                        return false;
                    }
                }

                // Assign leader to this bot
                bot.setLeaderId(leaderId);
            }
        }

        AIBrigadeMod.LOGGER.info("Assigned leader {} (UUID: {}) to group {}", leaderName, leaderId, groupName);
        return true;
    }

    /**
     * Set the relationship between two groups (bidirectional)
     *
     * @param group1 First group
     * @param group2 Second group
     * @param relationship The relationship type (ALLIED, NEUTRAL, HOSTILE)
     */
    public void setTeamRelationship(String group1, String group2, TeamRelationship relationship) {
        // Set bidirectional relationship
        teamRelationships.computeIfAbsent(group1, k -> new ConcurrentHashMap<>()).put(group2, relationship);
        teamRelationships.computeIfAbsent(group2, k -> new ConcurrentHashMap<>()).put(group1, relationship);

        AIBrigadeMod.LOGGER.info("Set relationship between {} and {} to {}", group1, group2, relationship);

        // Update bot hostile states if needed
        updateBotsHostileState(group1);
        updateBotsHostileState(group2);
    }

    /**
     * Get the relationship between two groups
     *
     * @param group1 First group
     * @param group2 Second group
     * @return The relationship, or NEUTRAL if not set
     */
    public TeamRelationship getTeamRelationship(String group1, String group2) {
        // Same group = always allied
        if (group1.equals(group2)) {
            return TeamRelationship.ALLIED;
        }

        Map<String, TeamRelationship> relationships = teamRelationships.get(group1);
        if (relationships != null && relationships.containsKey(group2)) {
            return relationships.get(group2);
        }

        // Default to neutral if no relationship set
        return TeamRelationship.NEUTRAL;
    }

    /**
     * Make one group hostile towards another (convenience method)
     *
     * @param sourceGroup The group becoming hostile
     * @param targetGroup The target group
     */
    public void setGroupHostile(String sourceGroup, String targetGroup) {
        setTeamRelationship(sourceGroup, targetGroup, TeamRelationship.HOSTILE);
    }

    /**
     * Make a group hostile towards a target (either another group or a player)
     * Automatically detects if the target is a player name or group name
     *
     * @param sourceGroup The group becoming hostile
     * @param targetName The target name (player or group)
     * @param server The server instance to lookup players
     * @return true if successful, false if sourceGroup not found or target invalid
     */
    public boolean setGroupHostileToTarget(String sourceGroup, String targetName, MinecraftServer server) {
        // Check if source group exists
        if (!botGroups.containsKey(sourceGroup)) {
            AIBrigadeMod.LOGGER.warn("Source group '{}' not found", sourceGroup);
            return false;
        }

        // Try to interpret target as a player first
        Player targetPlayer = server.getPlayerList().getPlayerByName(targetName);
        if (targetPlayer != null) {
            // Target is a player - set all bots in sourceGroup hostile to this player
            BotGroup group = botGroups.get(sourceGroup);
            for (UUID botId : new HashSet<>(group.getBotIds())) {
                BotEntity bot = activeBots.get(botId);
                if (bot != null) {
                    // Set hostile flag for bot
                    bot.setHostile(true);
                }
            }

            // Set player-group relationship
            setPlayerGroupRelationship(targetPlayer.getUUID(), sourceGroup, TeamRelationship.HOSTILE);
            AIBrigadeMod.LOGGER.info("Group '{}' is now hostile towards player '{}'", sourceGroup, targetName);
            return true;
        }

        // Try to interpret target as a group
        if (botGroups.containsKey(targetName)) {
            // Target is a group - set group-to-group relationship
            setGroupHostile(sourceGroup, targetName);
            AIBrigadeMod.LOGGER.info("Group '{}' is now hostile towards group '{}'", sourceGroup, targetName);
            return true;
        }

        // Target not found as either player or group
        AIBrigadeMod.LOGGER.warn("Target '{}' not found (not a player or group)", targetName);
        return false;
    }

    /**
     * Check if two groups are hostile
     *
     * @param group1 First group
     * @param group2 Second group
     * @return true if hostile
     */
    public boolean areGroupsHostile(String group1, String group2) {
        return getTeamRelationship(group1, group2) == TeamRelationship.HOSTILE;
    }

    /**
     * Update hostile state for all bots in a group based on relationships
     */
    private void updateBotsHostileState(String groupName) {
        BotGroup group = botGroups.get(groupName);
        if (group == null) return;

        // Check if this group has any hostile relationships (with groups or players)
        boolean hasHostileRelationships = false;

        // Check group-to-group relationships
        Map<String, TeamRelationship> relationships = teamRelationships.get(groupName);
        if (relationships != null) {
            for (TeamRelationship rel : relationships.values()) {
                if (rel == TeamRelationship.HOSTILE) {
                    hasHostileRelationships = true;
                    break;
                }
            }
        }

        // Check player-to-group relationships
        if (!hasHostileRelationships) {
            for (Map<String, TeamRelationship> playerRels : playerRelationships.values()) {
                if (playerRels.containsKey(groupName) && playerRels.get(groupName) == TeamRelationship.HOSTILE) {
                    hasHostileRelationships = true;
                    break;
                }
            }
        }

        // Set hostile state for all bots in the group - copy set to avoid concurrent modification
        for (UUID botId : new HashSet<>(group.getBotIds())) {
            BotEntity bot = activeBots.get(botId);
            if (bot != null) {
                bot.setHostile(hasHostileRelationships);
            }
        }
    }

    /**
     * Set the relationship between a player and a group
     *
     * @param playerId Player UUID
     * @param groupName Group name
     * @param relationship The relationship type
     */
    public void setPlayerGroupRelationship(UUID playerId, String groupName, TeamRelationship relationship) {
        playerRelationships.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(groupName, relationship);
        AIBrigadeMod.LOGGER.info("Set relationship between player {} and group {} to {}",
            playerId, groupName, relationship);

        // Update hostile state for the group
        updateBotsHostileState(groupName);
    }

    /**
     * Get the relationship between a player and a group
     *
     * @param playerId Player UUID
     * @param groupName Group name
     * @return The relationship, or NEUTRAL if not set
     */
    public TeamRelationship getPlayerGroupRelationship(UUID playerId, String groupName) {
        Map<String, TeamRelationship> relationships = playerRelationships.get(playerId);
        if (relationships != null && relationships.containsKey(groupName)) {
            return relationships.get(groupName);
        }
        return TeamRelationship.NEUTRAL;
    }

    /**
     * Enable/disable follow leader mode for a group with specified radius
     * Selon le cahier des charges:
     * - 5/6 des bots suivent dans le radius défini
     * - 1/6 des bots suivent activement le leader
     * Les probabilités sont assignées automatiquement lors de la création du Goal
     *
     * @param groupName Group name
     * @param enabled true to enable following, false to disable
     * @param radius Follow radius for the group
     * @return true if successful, false if group not found
     */
    public boolean setFollowLeader(String groupName, boolean enabled, float radius) {
        // Check if target is a group
        if (botGroups.containsKey(groupName)) {
            BotGroup group = botGroups.get(groupName);
            Set<UUID> groupBots = group.getBotIds();
            if (groupBots.isEmpty()) {
                return false;
            }

            // Update group radius
            group.setFollowRadius(radius);

            // Set follow leader for all bots in group - copy set to avoid concurrent modification
            int count = 0;
            int activeFollowers = 0;
            int radiusFollowers = 0;

            for (UUID botUUID : new HashSet<>(groupBots)) {
                BotEntity bot = activeBots.get(botUUID);
                if (bot != null) {
                    bot.setFollowingLeader(enabled);
                    bot.setFollowRadius(radius);
                    count++;

                    // Les probabilités sont déjà assignées dans RealisticFollowLeaderGoal
                    // On compte juste pour l'info
                    if (enabled) {
                        // Note: l'information exacte sur le type nécessiterait d'accéder au Goal
                        // Pour l'instant on estime: 1/6 active, 5/6 radius-based
                    }
                }
            }

            // Estimation des ratios pour le log
            activeFollowers = Math.round(count / 6.0f);
            radiusFollowers = count - activeFollowers;

            AIBrigadeMod.LOGGER.info("Set follow leader {} for {} bots in group '{}' with radius {} " +
                "(estimated: ~{} active followers, ~{} radius-based followers)",
                enabled ? "enabled" : "disabled", count, groupName, radius,
                activeFollowers, radiusFollowers);
            return count > 0;
        }

        AIBrigadeMod.LOGGER.warn("Group '{}' not found", groupName);
        return false;
    }

    /**
     * Legacy method for backward compatibility
     * Enable/disable follow leader mode for a bot or group without radius parameter
     *
     * @param targetName Bot name or group name
     * @param enabled true to enable following, false to disable
     * @return true if successful, false if bot/group not found
     */
    @Deprecated
    public boolean setFollowLeader(String targetName, boolean enabled) {
        // Use default radius of 10.0f
        return setFollowLeader(targetName, enabled, 10.0f);
    }

    /**
     * Toggle forced jumping for a bot or group
     * When enabled, bots jump continuously instead of at random intervals
     *
     * @param targetName Bot name or group name
     * @return Number of bots affected, 0 if target not found
     */
    public int toggleForcedJumping(String targetName) {
        int affected = 0;

        // Check if target is a group
        if (botGroups.containsKey(targetName)) {
            BotGroup group = botGroups.get(targetName);

            // Toggle for all bots in group - copy set to avoid concurrent modification
            for (UUID botId : new HashSet<>(group.getBotIds())) {
                BotEntity bot = activeBots.get(botId);
                if (bot != null) {
                    // Toggle the state
                    boolean currentState = bot.isForcedJumping();
                    bot.setForcedJumping(!currentState);
                    affected++;
                }
            }

            AIBrigadeMod.LOGGER.info("Toggled forced jumping for {} bots in group '{}'", affected, targetName);
            return affected;
        } else {
            // Try to find individual bot by name
            BotEntity bot = findBotByName(targetName);
            if (bot != null) {
                boolean currentState = bot.isForcedJumping();
                bot.setForcedJumping(!currentState);
                AIBrigadeMod.LOGGER.info("Toggled forced jumping for bot '{}' (now: {})",
                    targetName, !currentState);
                return 1;
            }
        }

        AIBrigadeMod.LOGGER.warn("Target '{}' not found for toggleForcedJumping", targetName);
        return 0;
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
        // Copy set to avoid concurrent modification
        for (UUID botId : new HashSet<>(group.getBotIds())) {
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
        // Slot: 0=helmet, 1=chestplate, 2=leggings, 3=boots
        switch (material) {
            case LEATHER:
                switch (slot) {
                    case 0: return new ItemStack(Items.LEATHER_HELMET);
                    case 1: return new ItemStack(Items.LEATHER_CHESTPLATE);
                    case 2: return new ItemStack(Items.LEATHER_LEGGINGS);
                    case 3: return new ItemStack(Items.LEATHER_BOOTS);
                }
                break;
            case CHAINMAIL:
                switch (slot) {
                    case 0: return new ItemStack(Items.CHAINMAIL_HELMET);
                    case 1: return new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                    case 2: return new ItemStack(Items.CHAINMAIL_LEGGINGS);
                    case 3: return new ItemStack(Items.CHAINMAIL_BOOTS);
                }
                break;
            case IRON:
                switch (slot) {
                    case 0: return new ItemStack(Items.IRON_HELMET);
                    case 1: return new ItemStack(Items.IRON_CHESTPLATE);
                    case 2: return new ItemStack(Items.IRON_LEGGINGS);
                    case 3: return new ItemStack(Items.IRON_BOOTS);
                }
                break;
            case GOLD:
                switch (slot) {
                    case 0: return new ItemStack(Items.GOLDEN_HELMET);
                    case 1: return new ItemStack(Items.GOLDEN_CHESTPLATE);
                    case 2: return new ItemStack(Items.GOLDEN_LEGGINGS);
                    case 3: return new ItemStack(Items.GOLDEN_BOOTS);
                }
                break;
            case DIAMOND:
                switch (slot) {
                    case 0: return new ItemStack(Items.DIAMOND_HELMET);
                    case 1: return new ItemStack(Items.DIAMOND_CHESTPLATE);
                    case 2: return new ItemStack(Items.DIAMOND_LEGGINGS);
                    case 3: return new ItemStack(Items.DIAMOND_BOOTS);
                }
                break;
            case NETHERITE:
                switch (slot) {
                    case 0: return new ItemStack(Items.NETHERITE_HELMET);
                    case 1: return new ItemStack(Items.NETHERITE_CHESTPLATE);
                    case 2: return new ItemStack(Items.NETHERITE_LEGGINGS);
                    case 3: return new ItemStack(Items.NETHERITE_BOOTS);
                }
                break;
        }
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
    public BotEntity findBotByName(String name) {
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
    /**
     * Give starting equipment to a new bot
     * - 128 oak planks in off-hand
     * - Random sword (1/3 stone, 1/3 iron, 1/3 diamond) in main hand
     */
    private void giveStartingEquipment(BotEntity bot) {
        Random random = new Random();

        // Give 128 oak planks in off-hand
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 128);
        bot.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, planks);

        // Give random sword (1/3 each type)
        ItemStack sword;
        int swordType = random.nextInt(3);
        if (swordType == 0) {
            sword = new ItemStack(Items.STONE_SWORD);
        } else if (swordType == 1) {
            sword = new ItemStack(Items.IRON_SWORD);
        } else {
            sword = new ItemStack(Items.DIAMOND_SWORD);
        }
        bot.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);

        AIBrigadeMod.LOGGER.info("Gave starting equipment to {}: {} planks and {} sword",
            bot.getBotName(), planks.getCount(), sword.getItem().toString());
    }

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

    /**
     * Clean up dead or invalid bots from the manager
     * Called periodically (every 5 seconds) to ensure dead bots don't block new spawns
     * This is a safety net in case onBotRemoved() wasn't called
     */
    public void cleanupDeadBots() {
        List<BotEntity> toRemove = new ArrayList<>();

        for (BotEntity bot : activeBots.values()) {
            // Remove if bot is dead, removed, or invalid
            if (bot == null || !bot.isAlive() || bot.isRemoved()) {
                toRemove.add(bot);
            }
        }

        if (!toRemove.isEmpty()) {
            for (BotEntity bot : toRemove) {
                // Use centralized cleanup method
                cleanupBot(bot, "found dead during periodic cleanup");
            }

            AIBrigadeMod.LOGGER.info("Periodic cleanup: removed {} dead/invalid bots (remaining: {}/{})",
                toRemove.size(), activeBots.size(), MAX_BOTS);
        }
    }

    // Getters

    public Map<UUID, BotEntity> getActiveBots() {
        return Collections.unmodifiableMap(activeBots);
    }

    public Map<String, BotGroup> getBotGroups() {
        return Collections.unmodifiableMap(botGroups);
    }

    /**
     * Get current bot count (for checking against limit)
     */
    public int getBotCount() {
        return activeBots.size();
    }

    /**
     * Get maximum allowed bots
     */
    public int getMaxBots() {
        return MAX_BOTS;
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
