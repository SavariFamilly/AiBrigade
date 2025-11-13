package com.aibrigade.bots;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MojangSkinFetcher - Récupère automatiquement les skins officiels Mojang via UUID
 *
 * Ce système permet de :
 * - Récupérer le skin d'un joueur réel via son UUID Minecraft
 * - Créer un GameProfile complet avec les textures
 * - Mettre en cache les résultats pour éviter trop d'appels API
 * - Gérer les erreurs et fournir des fallbacks
 */
public class MojangSkinFetcher {

    // Cache des GameProfiles pour éviter de surcharger l'API Mojang
    private static final Map<UUID, GameProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CACHE_TIMESTAMP = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 3600000; // 1 heure

    // Rate limiter: 10 requêtes par seconde max (600/minute pour être sûr)
    private static final RateLimiter API_RATE_LIMITER = RateLimiter.create(10.0);

    // URL de l'API Mojang
    private static final String SESSION_SERVER_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    // UUIDs de joueurs célèbres pour la base de données initiale
    public static final Map<String, UUID> FAMOUS_PLAYERS = new HashMap<>();

    // Tracking used player names to ensure uniqueness
    private static final Set<UUID> USED_PLAYER_UUIDS = ConcurrentHashMap.newKeySet();

    static {
        // Base de données de joueurs célèbres avec leurs vrais UUIDs
        // Note: Dinnerbone retiré car il fait spawn les entités à l'envers
        FAMOUS_PLAYERS.put("Notch", UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
        FAMOUS_PLAYERS.put("jeb_", UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6"));
        FAMOUS_PLAYERS.put("C418", UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"));
        FAMOUS_PLAYERS.put("Deadmau5", UUID.fromString("0d252b7f-7a3f-4b3d-9baa-5fcf3a38d7ea"));
        FAMOUS_PLAYERS.put("CaptainSparklez", UUID.fromString("f3c8d69b-0776-4620-b4c0-ccddc0b35076"));
        FAMOUS_PLAYERS.put("DanTDM", UUID.fromString("5c115ca7-0c24-4686-bbd1-c10be43f5fd1"));
        FAMOUS_PLAYERS.put("Technoblade", UUID.fromString("e6b5c088-0680-44df-9e1b-9bf11792291b"));
        FAMOUS_PLAYERS.put("Dream", UUID.fromString("ec561538-f3fd-461d-aff5-086b22154bce"));
        FAMOUS_PLAYERS.put("GeorgeNotFound", UUID.fromString("f7c77d99-9f15-4a66-a87d-c4a51ef30d19"));
        FAMOUS_PLAYERS.put("Sapnap", UUID.fromString("8b8d8e8f-2759-4106-8f06-7c9cc1be8b1f"));
        FAMOUS_PLAYERS.put("TommyInnit", UUID.fromString("1e18d5ff-643d-45c8-b509-43b8461d8614"));
        FAMOUS_PLAYERS.put("Tubbo", UUID.fromString("6a00c897-a3f1-4c7b-9c0e-64b2d1f4c9e5"));
        FAMOUS_PLAYERS.put("Ranboo", UUID.fromString("b7c7c5db-d0f7-4a6e-8c42-6b5c8f4e7f9a"));
        FAMOUS_PLAYERS.put("Philza", UUID.fromString("e8438c85-72d5-4203-abd5-83a424e09c82"));
    }

    /**
     * Récupère un GameProfile complet avec skin depuis l'UUID
     * Utilise le cache si disponible et valide
     *
     * @param uuid L'UUID du joueur
     * @return CompletableFuture contenant le GameProfile avec textures
     */
    public static CompletableFuture<GameProfile> fetchProfileAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Vérifier le cache
            if (isCacheValid(uuid)) {
                return PROFILE_CACHE.get(uuid);
            }

            try {
                // Récupérer le profil depuis l'API Mojang
                GameProfile profile = fetchProfileFromMojang(uuid);

                // Mettre en cache
                if (profile != null) {
                    PROFILE_CACHE.put(uuid, profile);
                    CACHE_TIMESTAMP.put(uuid, System.currentTimeMillis());
                }

                return profile;
            } catch (Exception e) {
                System.err.println("[MojangSkinFetcher] Erreur lors de la récupération du profil: " + e.getMessage());
                return createFallbackProfile(uuid);
            }
        }, Util.backgroundExecutor());
    }

    /**
     * Récupère le profil depuis l'API Mojang (bloquant)
     * Rate limited à 10 requêtes/seconde
     */
    private static GameProfile fetchProfileFromMojang(UUID uuid) throws Exception {
        // Appliquer le rate limiting (bloque si trop rapide)
        API_RATE_LIMITER.acquire();

        String uuidString = uuid.toString().replace("-", "");
        URL url = new URL(SESSION_SERVER_URL + uuidString + "?unsigned=false");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("HTTP " + responseCode);
        }

        // Lire la réponse JSON
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Parser le JSON
        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
        String name = json.get("name").getAsString();
        String id = json.get("id").getAsString();

        // Créer le GameProfile
        GameProfile profile = new GameProfile(uuid, name);

        // Ajouter les propriétés de texture (skin + cape)
        if (json.has("properties")) {
            json.getAsJsonArray("properties").forEach(element -> {
                JsonObject property = element.getAsJsonObject();
                String propName = property.get("name").getAsString();
                String value = property.get("value").getAsString();
                String signature = property.has("signature") ? property.get("signature").getAsString() : null;

                profile.getProperties().put(propName, new Property(propName, value, signature));
            });
        }

        return profile;
    }

    /**
     * Vérifie si le cache est valide pour cet UUID
     */
    private static boolean isCacheValid(UUID uuid) {
        if (!PROFILE_CACHE.containsKey(uuid)) {
            return false;
        }

        Long timestamp = CACHE_TIMESTAMP.get(uuid);
        if (timestamp == null) {
            return false;
        }

        return (System.currentTimeMillis() - timestamp) < CACHE_DURATION_MS;
    }

    /**
     * Crée un profil de fallback en cas d'erreur
     */
    private static GameProfile createFallbackProfile(UUID uuid) {
        // Utiliser le skin de Steve par défaut
        GameProfile profile = new GameProfile(uuid, "Bot_" + uuid.toString().substring(0, 8));
        return profile;
    }

    /**
     * Récupère un UUID aléatoire de joueur célèbre (non utilisé)
     * Garantit que chaque bot ait un pseudo unique
     */
    public static UUID getRandomFamousPlayerUUID() {
        List<UUID> availableUUIDs = new ArrayList<>(FAMOUS_PLAYERS.values());
        availableUUIDs.removeAll(USED_PLAYER_UUIDS);

        // Si tous les UUIDs sont utilisés, recycler en commençant par le plus ancien
        if (availableUUIDs.isEmpty()) {
            System.out.println("[MojangSkinFetcher] Tous les pseudos sont utilisés, recyclage activé");
            availableUUIDs = new ArrayList<>(FAMOUS_PLAYERS.values());
            USED_PLAYER_UUIDS.clear();
        }

        UUID selectedUUID = availableUUIDs.get(new Random().nextInt(availableUUIDs.size()));
        USED_PLAYER_UUIDS.add(selectedUUID);
        return selectedUUID;
    }

    /**
     * Libère un UUID pour qu'il puisse être réutilisé
     * Appelé quand un bot est supprimé
     */
    public static void releasePlayerUUID(UUID uuid) {
        USED_PLAYER_UUIDS.remove(uuid);
    }

    /**
     * Récupère un UUID par nom de joueur célèbre
     */
    public static UUID getFamousPlayerUUID(String name) {
        return FAMOUS_PLAYERS.get(name);
    }

    /**
     * Vérifie si un UUID est dans la base de joueurs célèbres
     */
    public static boolean isFamousPlayer(UUID uuid) {
        return FAMOUS_PLAYERS.containsValue(uuid);
    }

    /**
     * Obtient le nom associé à un UUID de joueur célèbre
     */
    public static String getFamousPlayerName(UUID uuid) {
        for (Map.Entry<String, UUID> entry : FAMOUS_PLAYERS.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Obtient un profil depuis le cache (sans requête API)
     * Utilisé par le renderer côté client
     *
     * @param uuid L'UUID du joueur
     * @return Le GameProfile en cache, ou null si pas trouvé
     */
    public static GameProfile getCachedProfile(UUID uuid) {
        return PROFILE_CACHE.get(uuid);
    }

    /**
     * Nettoie le cache (appeler périodiquement)
     */
    public static void cleanCache() {
        long currentTime = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();

        CACHE_TIMESTAMP.forEach((uuid, timestamp) -> {
            if (currentTime - timestamp > CACHE_DURATION_MS) {
                toRemove.add(uuid);
            }
        });

        toRemove.forEach(uuid -> {
            PROFILE_CACHE.remove(uuid);
            CACHE_TIMESTAMP.remove(uuid);
        });

        System.out.println("[MojangSkinFetcher] Cache nettoyé: " + toRemove.size() + " entrées supprimées");
    }

    /**
     * Applique un GameProfile à un BotEntity
     */
    public static void applyProfileToBot(BotEntity bot, GameProfile profile) {
        // Stocker l'UUID du profil
        bot.setPlayerUUID(profile.getId());

        // Stocker le nom
        bot.setBotName(profile.getName());

        // Les textures sont dans le GameProfile, elles seront automatiquement
        // utilisées par le renderer si on utilise PlayerRenderer ou similaire
        System.out.println("[MojangSkinFetcher] Profil appliqué: " + profile.getName() + " (" + profile.getId() + ")");
    }

    /**
     * Récupère et applique un skin aléatoire à un bot
     */
    public static void applyRandomFamousSkin(BotEntity bot) {
        UUID randomUUID = getRandomFamousPlayerUUID();

        // Trouver le nom associé à cet UUID
        String playerName = getFamousPlayerName(randomUUID);

        // Appliquer immédiatement l'UUID et le nom (pas besoin d'attendre le fetch)
        bot.setPlayerUUID(randomUUID);

        if (playerName != null) {
            bot.setBotName(playerName);
            System.out.println("[MojangSkinFetcher] Bot configuré avec pseudo: " + playerName + " (" + randomUUID + ")");
        } else {
            bot.setBotName("Bot_" + randomUUID.toString().substring(0, 8));
            System.out.println("[MojangSkinFetcher] Bot configuré avec UUID: " + randomUUID);
        }

        // Fetch le profil complet en arrière-plan (pour les textures)
        // Cela n'affecte pas le nom qui est déjà défini
        fetchProfileAsync(randomUUID).thenAccept(profile -> {
            if (profile != null) {
                // Les textures sont maintenant disponibles dans le cache
                System.out.println("[MojangSkinFetcher] Profil complet récupéré pour: " + playerName);
            }
        });
    }
}
