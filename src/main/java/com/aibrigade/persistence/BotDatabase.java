package com.aibrigade.persistence;

import com.aibrigade.bots.BotEntity;
import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BotDatabase - Système de persistance complet pour les bots
 *
 * Sauvegarde et charge automatiquement :
 * - UUID du joueur (pour le skin Mojang)
 * - Nom, équipement, inventaire
 * - État comportemental, groupe, leader
 * - Position de spawn et home
 * - Statistiques et configuration
 *
 * Format: JSON pour lisibilité et extensibilité
 */
public class BotDatabase {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // Chemin de sauvegarde
    private static Path DATABASE_PATH = Paths.get("world", "data", "aibrigade", "bot_database.json");

    // Base de données en mémoire (UUID du bot -> Données)
    private static final Map<UUID, BotData> BOT_DATABASE = new ConcurrentHashMap<>();

    // Indicateur de modification (pour auto-save)
    private static boolean isDirty = false;

    /**
     * Classe représentant toutes les données d'un bot
     */
    public static class BotData {
        // Identité
        public UUID botUUID;              // UUID unique du bot
        public UUID playerUUID;           // UUID du joueur Minecraft (pour skin)
        public String botName;
        public String botSkin;
        public long creationTime;
        public long lastActive;

        // Groupe et leader
        public String groupId;
        public UUID leaderUUID;
        public boolean isFollowingLeader;

        // Position
        public double posX, posY, posZ;
        public double homeX, homeY, homeZ;
        public String dimension;

        // État et comportement
        public String aiState;            // IDLE, FOLLOWING, ATTACKING, etc.
        public String role;               // SOLDIER, SCOUT, GUARD, etc.
        public String behaviorType;
        public boolean isStatic;
        public float followRadius;
        public boolean isHostile;

        // Équipement
        public String mainHandItem;
        public String offHandItem;
        public String[] armorSlots = new String[4];

        // Configuration comportementale
        public boolean canPlaceBlocks;    // Toggle building
        public float movementSpeed;
        public float chaseChance;         // Probabilité de chase (0.0 - 1.0)
        public float lookAroundChance;    // Probabilité de regarder ailleurs (0.0 - 1.0)
        public int lookAroundInterval;    // Intervalle en ticks

        // Statistiques
        public int blocksPlaced;
        public int distanceTraveled;
        public int enemiesKilled;

        public BotData() {
            this.botUUID = UUID.randomUUID();
            this.creationTime = System.currentTimeMillis();
            this.lastActive = System.currentTimeMillis();
            this.armorSlots = new String[]{"", "", "", ""};
            this.canPlaceBlocks = true;
            this.movementSpeed = 1.0f;
            this.chaseChance = 0.7f;
            this.lookAroundChance = 0.33f; // 2/6 = 1/3
            this.lookAroundInterval = 40; // 2 secondes
        }
    }

    /**
     * Initialise la base de données
     */
    public static void initialize(Path worldPath) {
        DATABASE_PATH = worldPath.resolve("data").resolve("aibrigade").resolve("bot_database.json");

        try {
            Files.createDirectories(DATABASE_PATH.getParent());
            System.out.println("[BotDatabase] Chemin de base de données: " + DATABASE_PATH);
        } catch (IOException e) {
            System.err.println("[BotDatabase] Erreur lors de la création du dossier: " + e.getMessage());
        }

        loadDatabase();
    }

    /**
     * Charge la base de données depuis le fichier JSON
     */
    public static void loadDatabase() {
        if (!Files.exists(DATABASE_PATH)) {
            System.out.println("[BotDatabase] Aucune base de données existante, création d'une nouvelle");
            return;
        }

        try {
            String jsonContent = Files.readString(DATABASE_PATH);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonArray bots = root.getAsJsonArray("bots");

            int loadedCount = 0;
            for (JsonElement element : bots) {
                JsonObject botJson = element.getAsJsonObject();
                BotData data = GSON.fromJson(botJson, BotData.class);

                if (data != null && data.botUUID != null) {
                    BOT_DATABASE.put(data.botUUID, data);
                    loadedCount++;
                }
            }

            System.out.println("[BotDatabase] Chargé " + loadedCount + " bots depuis la base de données");
            isDirty = false;

        } catch (Exception e) {
            System.err.println("[BotDatabase] Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sauvegarde la base de données dans le fichier JSON
     */
    public static void saveDatabase() {
        try {
            JsonObject root = new JsonObject();
            JsonArray botsArray = new JsonArray();

            BOT_DATABASE.values().forEach(data -> {
                data.lastActive = System.currentTimeMillis();
                JsonElement botJson = GSON.toJsonTree(data);
                botsArray.add(botJson);
            });

            root.add("bots", botsArray);
            root.addProperty("version", "1.0");
            root.addProperty("lastSaved", System.currentTimeMillis());

            String json = GSON.toJson(root);
            Files.writeString(DATABASE_PATH, json);

            System.out.println("[BotDatabase] Sauvegardé " + BOT_DATABASE.size() + " bots");
            isDirty = false;

        } catch (Exception e) {
            System.err.println("[BotDatabase] Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Enregistre un nouveau bot dans la base de données
     */
    public static BotData registerBot(BotEntity bot) {
        BotData data = new BotData();
        data.botUUID = bot.getUUID();

        // Récupérer toutes les données du bot
        updateBotData(data, bot);

        BOT_DATABASE.put(data.botUUID, data);
        isDirty = true;

        System.out.println("[BotDatabase] Bot enregistré: " + data.botName + " (" + data.botUUID + ")");
        return data;
    }

    /**
     * Met à jour les données d'un bot existant
     */
    public static void updateBot(BotEntity bot) {
        UUID uuid = bot.getUUID();
        BotData data = BOT_DATABASE.get(uuid);

        if (data == null) {
            // Si le bot n'existe pas, l'enregistrer
            data = registerBot(bot);
        } else {
            // Sinon, mettre à jour ses données
            updateBotData(data, bot);
            isDirty = true;
        }
    }

    /**
     * Copie les données du BotEntity vers BotData
     */
    private static void updateBotData(BotData data, BotEntity bot) {
        // Identité
        data.playerUUID = bot.getPlayerUUID();
        data.botName = bot.getBotName();
        data.botSkin = bot.getBotSkin();

        // Groupe et leader
        data.groupId = bot.getBotGroup();
        data.leaderUUID = bot.getLeaderId();
        data.isFollowingLeader = bot.isFollowingLeader();

        // Position
        data.posX = bot.getX();
        data.posY = bot.getY();
        data.posZ = bot.getZ();
        data.dimension = bot.level().dimension().location().toString();

        if (bot.getHomePosition() != null) {
            data.homeX = bot.getHomePosition().getX();
            data.homeY = bot.getHomePosition().getY();
            data.homeZ = bot.getHomePosition().getZ();
        }

        // État
        data.aiState = bot.getAIState().name();
        data.role = bot.getRole().name();
        data.behaviorType = bot.getBehaviorType();
        data.isStatic = bot.isStatic();
        data.followRadius = bot.getFollowRadius();
        data.isHostile = bot.isHostile();

        // Configuration
        data.canPlaceBlocks = bot.canPlaceBlocks();
        data.movementSpeed = (float) bot.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
    }

    /**
     * Applique les données sauvegardées à un BotEntity
     */
    public static void applyDataToBot(BotEntity bot) {
        UUID uuid = bot.getUUID();
        BotData data = BOT_DATABASE.get(uuid);

        if (data == null) {
            System.out.println("[BotDatabase] Aucune donnée trouvée pour le bot " + uuid);
            return;
        }

        // Appliquer l'identité
        bot.setPlayerUUID(data.playerUUID);
        bot.setBotName(data.botName);
        bot.setBotSkin(data.botSkin);

        // Appliquer le groupe et leader
        bot.setBotGroup(data.groupId);
        bot.setLeaderId(data.leaderUUID);
        bot.setFollowingLeader(data.isFollowingLeader);

        // Appliquer l'état
        bot.setAIState(BotEntity.BotAIState.valueOf(data.aiState));
        bot.setRole(BotEntity.BotRole.valueOf(data.role));
        bot.setBehaviorType(data.behaviorType);
        bot.setStatic(data.isStatic);
        bot.setFollowRadius(data.followRadius);
        bot.setHostile(data.isHostile);

        // Configuration
        bot.setCanPlaceBlocks(data.canPlaceBlocks);

        System.out.println("[BotDatabase] Données appliquées au bot " + data.botName);
    }

    /**
     * Récupère les données d'un bot par son UUID
     */
    public static BotData getBotData(UUID botUUID) {
        return BOT_DATABASE.get(botUUID);
    }

    /**
     * Supprime un bot de la base de données
     */
    public static void removeBot(UUID botUUID) {
        if (BOT_DATABASE.remove(botUUID) != null) {
            isDirty = true;
            System.out.println("[BotDatabase] Bot supprimé: " + botUUID);
        }
    }

    /**
     * Obtient tous les bots enregistrés
     */
    public static Collection<BotData> getAllBots() {
        return BOT_DATABASE.values();
    }

    /**
     * Vérifie si la base de données a été modifiée
     */
    public static boolean isDirty() {
        return isDirty;
    }

    /**
     * Auto-save toutes les 5 minutes si modifié
     */
    public static void autoSave() {
        if (isDirty) {
            saveDatabase();
        }
    }

    /**
     * Nettoie les bots inactifs depuis plus de X jours
     */
    public static void cleanupInactiveBots(long maxInactivityMs) {
        long currentTime = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();

        BOT_DATABASE.forEach((uuid, data) -> {
            if (currentTime - data.lastActive > maxInactivityMs) {
                toRemove.add(uuid);
            }
        });

        toRemove.forEach(BOT_DATABASE::remove);

        if (!toRemove.isEmpty()) {
            isDirty = true;
            System.out.println("[BotDatabase] Nettoyé " + toRemove.size() + " bots inactifs");
        }
    }
}
