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
 * MojangSkinFetcher - Récupère automatiquement les skins de joueurs Minecraft aléatoires
 *
 * Ce système permet de :
 * - Générer des pseudos aléatoires de style Minecraft
 * - Vérifier leur existence via l'API Mojang officielle
 * - Récupérer les skins de joueurs réels existants
 * - Mettre en cache les résultats pour éviter trop d'appels API
 * - Créer un GameProfile complet avec les textures
 */
public class MojangSkinFetcher {

    // Cache des GameProfiles pour éviter de surcharger l'API Mojang
    private static final Map<UUID, GameProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CACHE_TIMESTAMP = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 3600000; // 1 heure

    // Rate limiter: 10 requêtes par seconde max (600/minute pour être sûr)
    private static final RateLimiter API_RATE_LIMITER = RateLimiter.create(10.0);

    // URLs de l'API Mojang
    private static final String SESSION_SERVER_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String USERNAME_TO_UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";

    // Tracking used player UUIDs to ensure uniqueness
    private static final Set<UUID> USED_PLAYER_UUIDS = ConcurrentHashMap.newKeySet();

    // Cache nom -> UUID pour éviter les requêtes répétées
    private static final Map<String, UUID> NAME_TO_UUID_CACHE = new ConcurrentHashMap<>();

    // Cache des pseudos vérifiés qui existent
    private static final Set<String> VERIFIED_USERNAMES = ConcurrentHashMap.newKeySet();

    // Cache des pseudos qui n'existent PAS (pour éviter de les retester)
    private static final Set<String> NON_EXISTENT_USERNAMES = ConcurrentHashMap.newKeySet();

    // Statistiques
    private static int totalAttempts = 0;
    private static int successfulFinds = 0;
    private static int failedAttempts = 0;

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
                com.aibrigade.main.AIBrigadeMod.LOGGER.error("Error fetching profile for UUID {}: {}", uuid, e.getMessage());
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
     * Convertit un nom de joueur en UUID via l'API Mojang
     * Utilise le cache si disponible
     * Vérifie que le joueur existe
     *
     * @param username Le pseudo du joueur
     * @return CompletableFuture<UUID> ou null si le joueur n'existe pas
     */
    public static CompletableFuture<UUID> getUUIDFromUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            totalAttempts++;

            // Vérifier le cache positif
            if (NAME_TO_UUID_CACHE.containsKey(username.toLowerCase())) {
                successfulFinds++;
                return NAME_TO_UUID_CACHE.get(username.toLowerCase());
            }

            // Vérifier le cache négatif
            if (NON_EXISTENT_USERNAMES.contains(username.toLowerCase())) {
                failedAttempts++;
                return null;
            }

            try {
                // Appliquer le rate limiting
                API_RATE_LIMITER.acquire();

                URL url = new URL(USERNAME_TO_UUID_URL + username);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 204 || responseCode == 404) {
                    // Le joueur n'existe pas
                    NON_EXISTENT_USERNAMES.add(username.toLowerCase());
                    failedAttempts++;
                    return null;
                }

                if (responseCode != 200) {
                    failedAttempts++;
                    return null;
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
                String uuidString = json.get("id").getAsString();

                // Convertir UUID sans tirets en UUID avec tirets
                String formattedUUID = uuidString.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"
                );

                UUID uuid = UUID.fromString(formattedUUID);

                // Mettre en cache
                NAME_TO_UUID_CACHE.put(username.toLowerCase(), uuid);
                VERIFIED_USERNAMES.add(username);
                successfulFinds++;
                return uuid;

            } catch (Exception e) {
                com.aibrigade.main.AIBrigadeMod.LOGGER.error("Error checking username {}: {}", username, e.getMessage());
                failedAttempts++;
                return null;
            }
        }, Util.backgroundExecutor());
    }

    /**
     * Trouve un joueur aléatoire qui existe vraiment
     * Génère des pseudos aléatoires et vérifie leur existence
     *
     * @param maxAttempts Nombre maximum de tentatives
     * @return CompletableFuture<String> contenant le pseudo trouvé ou null
     */
    public static CompletableFuture<String> findRandomExistingPlayer(int maxAttempts) {
        return CompletableFuture.supplyAsync(() -> {
            int attempts = 0;

            while (attempts < maxAttempts) {
                // Générer un pseudo aléatoire
                String randomUsername = RandomUsernameGenerator.generateValidMinecraftUsername();

                // Vérifier s'il existe (de manière synchrone pour cette opération)
                try {
                    UUID uuid = getUUIDFromUsername(randomUsername).get();
                    if (uuid != null) {
                        return randomUsername;
                    }
                } catch (Exception e) {
                    // Player doesn't exist, continue searching
                }

                attempts++;
            }

            com.aibrigade.main.AIBrigadeMod.LOGGER.warn("Could not find existing player after {} attempts", maxAttempts);
            return null;
        }, Util.backgroundExecutor());
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
     * Libère un UUID pour qu'il puisse être réutilisé
     * Appelé quand un bot est supprimé
     */
    public static void releasePlayerUUID(UUID uuid) {
        USED_PLAYER_UUIDS.remove(uuid);
    }

    /**
     * Récupère un UUID par nom de joueur (liste des joueurs connus)
     */
    public static UUID getFamousPlayerUUID(String name) {
        return NAME_TO_UUID_CACHE.get(name.toLowerCase());
    }

    /**
     * Vérifie si un UUID est dans la liste des joueurs connus
     */
    public static boolean isFamousPlayer(UUID uuid) {
        return NAME_TO_UUID_CACHE.containsValue(uuid);
    }

    /**
     * Obtient le nom associé à un UUID de joueur
     */
    public static String getFamousPlayerName(UUID uuid) {
        for (Map.Entry<String, UUID> entry : NAME_TO_UUID_CACHE.entrySet()) {
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

        if (!toRemove.isEmpty()) {
            com.aibrigade.main.AIBrigadeMod.LOGGER.info("Cache cleaned: {} entries removed", toRemove.size());
        }
    }

    /**
     * Applique un GameProfile à un BotEntity
     * Extrait les textures et les synchronise au client
     */
    public static void applyProfileToBot(BotEntity bot, GameProfile profile) {
        // Stocker l'UUID du profil
        bot.setPlayerUUID(profile.getId());

        // Stocker le nom
        bot.setBotName(profile.getName());

        // Extraire les textures du profil Mojang
        if (profile.getProperties().containsKey("textures")) {
            Property textureProperty = profile.getProperties().get("textures").iterator().next();
            String value = textureProperty.getValue();
            String signature = textureProperty.getSignature();

            // Appliquer les textures au bot (synchronisé au client)
            bot.setSkinTextureValue(value);
            bot.setSkinTextureSignature(signature);

            com.aibrigade.main.AIBrigadeMod.LOGGER.info("✓ Skin textures applied to bot {} (UUID: {})",
                profile.getName(), profile.getId());
        } else {
            com.aibrigade.main.AIBrigadeMod.LOGGER.warn("Profile {} has no textures!", profile.getName());
        }
    }

    /**
     * Récupère et applique un skin aléatoire à un bot
     * Génère des pseudos aléatoires et trouve un qui existe vraiment
     */
    public static void applyRandomFamousSkin(BotEntity bot) {
        // Appliquer un nom temporaire
        bot.setBotName("Searching_Player");

        // Chercher un joueur existant (max 20 tentatives)
        findRandomExistingPlayer(20).thenAccept(username -> {
            if (username == null) {
                // Fallback : utiliser un nom généré
                UUID fallbackUUID = UUID.randomUUID();
                bot.setPlayerUUID(fallbackUUID);
                bot.setBotName("Bot_" + fallbackUUID.toString().substring(0, 8));

                // Force client sync for fallback UUID
                if (!bot.level().isClientSide && bot.isAlive()) {
                    bot.refreshDimensions();
                }
                return;
            }

            // Récupérer l'UUID du joueur trouvé
            getUUIDFromUsername(username).thenAccept(uuid -> {
                if (uuid == null) {
                    // Ne devrait pas arriver car on a déjà vérifié
                    UUID fallbackUUID = UUID.randomUUID();
                    bot.setPlayerUUID(fallbackUUID);
                    bot.setBotName("Bot_" + fallbackUUID.toString().substring(0, 8));

                    // Force client sync for fallback UUID
                    if (!bot.level().isClientSide && bot.isAlive()) {
                        bot.refreshDimensions();
                    }
                    return;
                }

                // Marquer comme utilisé
                USED_PLAYER_UUIDS.add(uuid);

                // Appliquer l'UUID et le nom
                bot.setPlayerUUID(uuid);
                bot.setBotName(username);

                // Fetch le profil complet en arrière-plan pour les textures
                fetchProfileAsync(uuid).thenAccept(profile -> {
                    if (profile != null) {
                        // Appliquer le profil complet avec textures
                        applyProfileToBot(bot, profile);

                        // Force entity data sync to all tracking players
                        if (!bot.level().isClientSide && bot.isAlive()) {
                            // Refresh entity dimensions to trigger a full sync packet
                            bot.refreshDimensions();
                        }
                    }
                });
            });
        });
    }

    /**
     * Affiche les statistiques de recherche de joueurs
     */
    public static void printStatistics() {
        double successRate = totalAttempts > 0 ? (successfulFinds * 100.0 / totalAttempts) : 0;

        com.aibrigade.main.AIBrigadeMod.LOGGER.info("=== MojangSkinFetcher Statistics ===");
        com.aibrigade.main.AIBrigadeMod.LOGGER.info("Total attempts: {}", totalAttempts);
        com.aibrigade.main.AIBrigadeMod.LOGGER.info("Successful finds: {}", successfulFinds);
        com.aibrigade.main.AIBrigadeMod.LOGGER.info("Failed attempts: {}", failedAttempts);
        com.aibrigade.main.AIBrigadeMod.LOGGER.info("Success rate: {:.2f}%", successRate);
        com.aibrigade.main.AIBrigadeMod.LOGGER.info("Verified usernames cached: {}", VERIFIED_USERNAMES.size());
        com.aibrigade.main.AIBrigadeMod.LOGGER.info("Non-existent usernames cached: {}", NON_EXISTENT_USERNAMES.size());
    }
}
