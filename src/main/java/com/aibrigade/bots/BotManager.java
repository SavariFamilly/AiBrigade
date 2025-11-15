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

        // Wait briefly for async name to be set, then ensure uniqueness
        // If name is already taken, append a number
        int attempts = 0;
        String originalName = bot.getBotName();
        while (isBotNameTaken(bot.getBotName()) && attempts < 100) {
            bot.setBotName(originalName + "_" + (attempts + 1));
            attempts++;
        }

        if (attempts > 0) {
            AIBrigadeMod.LOGGER.info("Bot name '{}' was taken, renamed to '{}'", originalName, bot.getBotName());
        }

        AIBrigadeMod.LOGGER.info("Spawning bot at {} in group {} with behavior {}",
            pos, groupName, behavior);

        // For STATIC bots, find the ground first so they don't float in mid-air
        BlockPos spawnPos = pos;
        if (isStatic) {
            spawnPos = findGroundBelow(level, pos);
            AIBrigadeMod.LOGGER.info("Static bot ground position found at Y={} (original Y={})", spawnPos.getY(), pos.getY());
        }

        // Configure bot (NE PAS écraser le nom et skin déjà appliqués dans le constructeur)
        bot.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        // bot.setBotName() et bot.setBotSkin() sont déjà définis par le constructeur via MojangSkinFetcher
        bot.setBehaviorType(behavior);
        bot.setFollowRadius(radius);
        bot.setStatic(isStatic);
        bot.setBotGroup(groupName);
        bot.setSpawnPosition(spawnPos);

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
                AIBrigadeMod.LOGGER.warn("Group '{}' has no bots", groupName);
                return false;
            }

            // Update group radius
            group.setFollowRadius(radius);

            // Set follow leader for all bots in group - copy set to avoid concurrent modification
            int count = 0;
            int withoutLeader = 0;
            int staticBots = 0;
            int activeFollowers = 0;
            int radiusFollowers = 0;

            for (UUID botUUID : new HashSet<>(groupBots)) {
                BotEntity bot = activeBots.get(botUUID);
                if (bot != null) {
                    // Check if bot has a leader assigned
                    if (bot.getLeaderId() == null) {
                        withoutLeader++;
                        AIBrigadeMod.LOGGER.warn("Bot {} in group '{}' has no leader assigned! Use /aibrigade assignleader first.",
                            bot.getBotName(), groupName);
                    }

                    // Check if bot is static
                    if (bot.isStatic()) {
                        staticBots++;
                        AIBrigadeMod.LOGGER.warn("Bot {} is in STATIC mode and cannot follow! Use /aibrigade togglestatic to enable movement.",
                            bot.getBotName());
                    }

                    bot.setFollowingLeader(enabled);
                    bot.setFollowRadius(radius);
                    count++;

                    System.out.println("[BotManager] Bot " + bot.getBotName() + " setFollowingLeader=" + enabled + " radius=" + radius + " static=" + bot.isStatic() + " hasLeader=" + (bot.getLeaderId() != null));

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

            // Log warnings summary
            if (withoutLeader > 0) {
                AIBrigadeMod.LOGGER.warn("WARNING: {} bot(s) have no leader assigned! They won't follow anyone.", withoutLeader);
                AIBrigadeMod.LOGGER.warn("Fix: Use /aibrigade assignleader {} <playerName>", groupName);
            }
            if (staticBots > 0) {
                AIBrigadeMod.LOGGER.warn("WARNING: {} bot(s) are in STATIC mode and cannot move!", staticBots);
                AIBrigadeMod.LOGGER.warn("Fix: Use /aibrigade togglestatic {} to allow movement", groupName);
            }

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
     * Find ground below a position for static bot spawning
     * Searches up to 256 blocks down, returns position ON TOP of solid ground
     *
     * @param level The world level
     * @param startPos Starting position to search from
     * @return BlockPos on top of ground, or original position if no ground found
     */
    private BlockPos findGroundBelow(ServerLevel level, BlockPos startPos) {
        // Check if already on solid ground
        BlockPos below = startPos.below();
        if (com.aibrigade.utils.BlockHelper.isSolidBlock(level, below)) {
            return startPos; // Already on ground
        }

        // Search down for solid block
        BlockPos groundBlock = com.aibrigade.utils.BlockHelper.findGroundBelow(level, startPos, 256);

        if (groundBlock != null) {
            // Return position ON TOP of the solid block (above it)
            return groundBlock.above();
        }

        // No ground found within 256 blocks, return original position
        AIBrigadeMod.LOGGER.warn("No ground found below {} within 256 blocks, spawning at original position", startPos);
        return startPos;
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

        File dataFile = getDataFile(server);
        if (!dataFile.exists()) {
            AIBrigadeMod.LOGGER.info("No persistent data found, starting fresh");
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            PersistentData data = gson.fromJson(reader, PersistentData.class);

            if (data == null) {
                AIBrigadeMod.LOGGER.warn("Persistent data file is empty or corrupted");
                return;
            }

            // Load groups first
            if (data.groups != null) {
                for (Map.Entry<String, GroupData> entry : data.groups.entrySet()) {
                    GroupData groupData = entry.getValue();
                    BotGroup group = new BotGroup(
                        entry.getKey(),
                        groupData.leaderName,
                        groupData.followRadius
                    );
                    botGroups.put(entry.getKey(), group);
                }
                AIBrigadeMod.LOGGER.info("Loaded {} bot groups", data.groups.size());
            }

            // Load bots
            if (data.bots != null) {
                int loadedCount = 0;
                int failedCount = 0;

                for (BotData botData : data.bots) {
                    try {
                        // Get the correct world level
                        ServerLevel level = server.getLevel(botData.getWorldKey());
                        if (level == null) {
                            AIBrigadeMod.LOGGER.warn("World {} not found for bot {}, skipping",
                                botData.world, botData.name);
                            failedCount++;
                            continue;
                        }

                        BlockPos pos = new BlockPos(botData.x, botData.y, botData.z);

                        // Spawn the bot
                        BotEntity bot = spawnBot(
                            level,
                            pos,
                            botData.leaderName,
                            botData.behavior,
                            botData.radius,
                            botData.isStatic,
                            botData.group
                        );

                        if (bot != null) {
                            // Restore additional properties
                            bot.setBotName(botData.name);
                            bot.setHostile(botData.isHostile);
                            bot.setFollowingLeader(botData.isFollowingLeader);
                            bot.setCanPlaceBlocks(botData.canPlaceBlocks);
                            bot.setForcedJumping(botData.forcedJumping);

                            // Restore player UUID for skin
                            if (botData.playerUUID != null) {
                                try {
                                    UUID playerUUID = UUID.fromString(botData.playerUUID);
                                    bot.setPlayerUUID(playerUUID);
                                } catch (IllegalArgumentException e) {
                                    AIBrigadeMod.LOGGER.warn("Invalid player UUID for bot {}", botData.name);
                                }
                            }

                            // Restore leader UUID
                            if (botData.leaderId != null) {
                                try {
                                    UUID leaderUUID = UUID.fromString(botData.leaderId);
                                    bot.setLeaderId(leaderUUID);
                                } catch (IllegalArgumentException e) {
                                    AIBrigadeMod.LOGGER.warn("Invalid leader UUID for bot {}", botData.name);
                                }
                            }

                            loadedCount++;
                        } else {
                            failedCount++;
                        }
                    } catch (Exception e) {
                        AIBrigadeMod.LOGGER.error("Failed to load bot {}: {}",
                            botData.name, e.getMessage());
                        failedCount++;
                    }
                }

                AIBrigadeMod.LOGGER.info("Persistent data loaded: {} bots restored, {} failed",
                    loadedCount, failedCount);
            }
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to load persistent data", e);
        } catch (Exception e) {
            AIBrigadeMod.LOGGER.error("Error parsing persistent data", e);
        }
    }

    /**
     * Save persistent data to disk
     */
    public void savePersistentData(MinecraftServer server) {
        AIBrigadeMod.LOGGER.info("Saving AIBrigade persistent data");

        File dataFile = getDataFile(server);

        try {
            // Ensure directory exists
            dataFile.getParentFile().mkdirs();

            // Create persistent data structure
            PersistentData data = new PersistentData();
            data.bots = new ArrayList<>();
            data.groups = new HashMap<>();

            // Save all active bots
            for (BotEntity bot : activeBots.values()) {
                if (bot != null && bot.isAlive() && !bot.isRemoved()) {
                    BotData botData = new BotData();
                    botData.uuid = bot.getUUID().toString();
                    botData.name = bot.getBotName();
                    botData.x = (int)bot.getX();
                    botData.y = (int)bot.getY();
                    botData.z = (int)bot.getZ();
                    botData.world = bot.level().dimension().location().toString();
                    botData.group = bot.getBotGroup();
                    botData.behavior = bot.getBehaviorType();
                    botData.radius = bot.getFollowRadius();
                    botData.isStatic = bot.isStatic();
                    botData.isHostile = bot.isHostile();
                    botData.isFollowingLeader = bot.isFollowingLeader();
                    botData.canPlaceBlocks = bot.canPlaceBlocks();
                    botData.forcedJumping = bot.isForcedJumping();

                    // Save player UUID for skin
                    UUID playerUUID = bot.getPlayerUUID();
                    if (playerUUID != null) {
                        botData.playerUUID = playerUUID.toString();
                    }

                    // Save leader UUID
                    UUID leaderUUID = bot.getLeaderId();
                    if (leaderUUID != null) {
                        botData.leaderId = leaderUUID.toString();
                        // Also save leader name for easier loading
                        botData.leaderName = findLeaderName(server, leaderUUID);
                    }

                    data.bots.add(botData);
                }
            }

            // Save all groups
            for (Map.Entry<String, BotGroup> entry : botGroups.entrySet()) {
                BotGroup group = entry.getValue();
                GroupData groupData = new GroupData();
                groupData.leaderName = group.getLeaderName();
                groupData.followRadius = group.getFollowRadius();
                data.groups.put(entry.getKey(), groupData);
            }

            // Write to file
            try (Writer writer = new FileWriter(dataFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(data, writer);
            }

            AIBrigadeMod.LOGGER.info("Persistent data saved: {} bots, {} groups",
                data.bots.size(), data.groups.size());
        } catch (IOException e) {
            AIBrigadeMod.LOGGER.error("Failed to save persistent data", e);
        }
    }

    /**
     * Find leader name by UUID
     */
    private String findLeaderName(MinecraftServer server, UUID leaderUUID) {
        // Check if leader is a player
        var player = server.getPlayerList().getPlayer(leaderUUID);
        if (player != null) {
            return player.getName().getString();
        }

        // Check if leader is a bot
        BotEntity leaderBot = activeBots.get(leaderUUID);
        if (leaderBot != null) {
            return leaderBot.getBotName();
        }

        return "unknown";
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
     * Get a bot by its name
     * @param botName The bot's name
     * @return The bot entity, or null if not found
     */
    public BotEntity getBotByName(String botName) {
        for (BotEntity bot : activeBots.values()) {
            if (bot.getBotName().equalsIgnoreCase(botName)) {
                return bot;
            }
        }
        return null;
    }

    /**
     * Check if a bot name is already in use
     * @param name The name to check
     * @return true if name is taken, false otherwise
     */
    public boolean isBotNameTaken(String name) {
        for (BotEntity bot : activeBots.values()) {
            if (bot.getBotName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kill a bot by its name
     * @param botName The bot's name
     * @return true if bot was found and killed, false otherwise
     */
    public boolean killBotByName(String botName) {
        BotEntity bot = getBotByName(botName);
        if (bot == null) {
            return false;
        }

        // Remove from group
        String groupId = bot.getGroupId();
        if (groupId != null && botGroups.containsKey(groupId)) {
            botGroups.get(groupId).removeBot(bot.getUUID());
        }

        // Remove from active bots
        activeBots.remove(bot.getUUID());

        // Kill the entity
        bot.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);

        AIBrigadeMod.LOGGER.info("Killed bot: {}", botName);
        return true;
    }

    /**
     * Change a bot's name and fetch new Mojang skin
     * @param bot The bot to rename
     * @param newName The new name
     * @return true if successful, false if name is taken
     */
    public boolean changeBotName(BotEntity bot, String newName) {
        // Check if new name is already taken
        if (isBotNameTaken(newName)) {
            return false;
        }

        // Change the name
        bot.setBotName(newName);

        // Fetch new Mojang skin for this name
        MojangSkinFetcher.getUUIDFromUsername(newName).thenAccept(uuid -> {
            if (uuid != null) {
                bot.setPlayerUUID(uuid);
                AIBrigadeMod.LOGGER.info("Bot renamed to '{}' with UUID {}", newName, uuid);

                // Fetch profile for skin
                MojangSkinFetcher.fetchProfileAsync(uuid).thenAccept(profile -> {
                    if (profile != null) {
                        AIBrigadeMod.LOGGER.info("Skin loaded for renamed bot '{}'", profile.getName());
                    }
                });
            } else {
                AIBrigadeMod.LOGGER.warn("Could not find Mojang UUID for name '{}', using random UUID", newName);
                bot.setPlayerUUID(UUID.randomUUID());
            }
        });

        return true;
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

    /**
     * Data class for persistent storage
     */
    private static class PersistentData {
        List<BotData> bots;
        Map<String, GroupData> groups;
    }

    /**
     * Data class for bot storage
     */
    private static class BotData {
        String uuid;
        String name;
        int x, y, z;
        String world;
        String group;
        String behavior;
        float radius;
        boolean isStatic;
        boolean isHostile;
        boolean isFollowingLeader;
        boolean canPlaceBlocks;
        boolean forcedJumping;
        String playerUUID;
        String leaderId;
        String leaderName;

        /**
         * Get world key from world string
         */
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> getWorldKey() {
            if (world == null || world.isEmpty()) {
                return net.minecraft.world.level.Level.OVERWORLD;
            }

            try {
                net.minecraft.resources.ResourceLocation location =
                    new net.minecraft.resources.ResourceLocation(world);
                return net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    location
                );
            } catch (Exception e) {
                return net.minecraft.world.level.Level.OVERWORLD;
            }
        }
    }

    /**
     * Data class for group storage
     */
    private static class GroupData {
        String leaderName;
        float followRadius;
    }
}
