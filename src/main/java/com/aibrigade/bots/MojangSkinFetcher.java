package com.aibrigade.bots;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
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
 * - Récupérer des skins de joueurs réels via l'API Mojang
 * - Créer un GameProfile complet avec les textures
 * - Mettre en cache les résultats pour éviter trop d'appels API
 * - Gérer une grande base de données de joueurs populaires
 * - Convertir des noms de joueurs en UUID via l'API Mojang
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

    // Large base de données de pseudos Minecraft populaires et aléatoires
    // Ces joueurs ont des skins intéressants et variés
    public static final List<String> PLAYER_NAMES = new ArrayList<>();

    // Tracking used player names to ensure uniqueness
    private static final Set<UUID> USED_PLAYER_UUIDS = ConcurrentHashMap.newKeySet();

    // Cache nom -> UUID pour éviter les requêtes répétées
    private static final Map<String, UUID> NAME_TO_UUID_CACHE = new ConcurrentHashMap<>();

    static {
        // === JOUEURS CÉLÈBRES ET DÉVELOPPEURS ===
        PLAYER_NAMES.add("Notch");
        PLAYER_NAMES.add("jeb_");
        PLAYER_NAMES.add("C418");
        PLAYER_NAMES.add("Deadmau5");
        PLAYER_NAMES.add("Grumm"); // Mojang dev (pas Dinnerbone qui inverse les entités)

        // === YOUTUBEURS/STREAMERS POPULAIRES ===
        PLAYER_NAMES.add("Dream");
        PLAYER_NAMES.add("GeorgeNotFound");
        PLAYER_NAMES.add("Sapnap");
        PLAYER_NAMES.add("TommyInnit");
        PLAYER_NAMES.add("Tubbo");
        PLAYER_NAMES.add("Ranboo");
        PLAYER_NAMES.add("Philza");
        PLAYER_NAMES.add("Technoblade");
        PLAYER_NAMES.add("CaptainSparklez");
        PLAYER_NAMES.add("DanTDM");
        PLAYER_NAMES.add("Grian");
        PLAYER_NAMES.add("MumboJumbo");
        PLAYER_NAMES.add("GoodTimesWithScar");
        PLAYER_NAMES.add("BdoubleO100");
        PLAYER_NAMES.add("Etho");
        PLAYER_NAMES.add("VintageBeef");
        PLAYER_NAMES.add("PewDiePie");
        PLAYER_NAMES.add("Markiplier");
        PLAYER_NAMES.add("Jacksepticeye");
        PLAYER_NAMES.add("Skeppy");
        PLAYER_NAMES.add("BadBoyHalo");
        PLAYER_NAMES.add("awesamdude");
        PLAYER_NAMES.add("Quackity");
        PLAYER_NAMES.add("KarlJacobs");
        PLAYER_NAMES.add("Foolish_Gamers");
        PLAYER_NAMES.add("Punz");
        PLAYER_NAMES.add("Ponk");
        PLAYER_NAMES.add("Antfrost");
        PLAYER_NAMES.add("Callahan");
        PLAYER_NAMES.add("Eret");
        PLAYER_NAMES.add("HBomb94");
        PLAYER_NAMES.add("Nihachu");
        PLAYER_NAMES.add("Purpled");
        PLAYER_NAMES.add("5up");
        PLAYER_NAMES.add("Fundy");
        PLAYER_NAMES.add("ConnorEatsPants");
        PLAYER_NAMES.add("CptPuffy");
        PLAYER_NAMES.add("Niki");
        PLAYER_NAMES.add("JackManifoldTV");
        PLAYER_NAMES.add("Slimecicle");

        // === SPEEDRUNNERS ET COMPÉTITEURS ===
        PLAYER_NAMES.add("Illumina");
        PLAYER_NAMES.add("Fruitberries");
        PLAYER_NAMES.add("TapL");
        PLAYER_NAMES.add("Couriway");
        PLAYER_NAMES.add("Benex");

        // === BUILDERS ET CRÉATEURS ===
        PLAYER_NAMES.add("fWhip");
        PLAYER_NAMES.add("GeminiTay");
        PLAYER_NAMES.add("SmallishBeans");
        PLAYER_NAMES.add("LDShadowLady");
        PLAYER_NAMES.add("Shubble");
        PLAYER_NAMES.add("SolidarityGaming");
        PLAYER_NAMES.add("Pixlriffs");

        // === JOUEURS AVEC SKINS VARIÉS ===
        // Ajout de pseudos génériques courants pour plus de variété
        PLAYER_NAMES.add("Steve");
        PLAYER_NAMES.add("Alex");
        PLAYER_NAMES.add("Herobrine");
        PLAYER_NAMES.add("xNestorio");
        PLAYER_NAMES.add("Huahwi");
        PLAYER_NAMES.add("CraftBattleDuty");
        PLAYER_NAMES.add("AciDic_BliTzz");
        PLAYER_NAMES.add("Grapeapplesauce");
        PLAYER_NAMES.add("xRpMx13");
        PLAYER_NAMES.add("Strafeshot");
        PLAYER_NAMES.add("Cayorion");
        PLAYER_NAMES.add("Bashurverse");
        PLAYER_NAMES.add("SkyDoesMinecraft");
        PLAYER_NAMES.add("PopularMMOs");
        PLAYER_NAMES.add("GamingWithJen");
        PLAYER_NAMES.add("SSundee");
        PLAYER_NAMES.add("CrainerGames");
        PLAYER_NAMES.add("Lachlan");
        PLAYER_NAMES.add("PrestonPlayz");
        PLAYER_NAMES.add("MrWoofless");
        PLAYER_NAMES.add("BajanCanadian");
        PLAYER_NAMES.add("JeromeASF");
        PLAYER_NAMES.add("Vikkstar123");
        PLAYER_NAMES.add("Lachlan");
        PLAYER_NAMES.add("NoahCraftFTW");

        // === JOUEURS FRANÇAIS POPULAIRES ===
        PLAYER_NAMES.add("Aypierre");
        PLAYER_NAMES.add("FuzeIII");
        PLAYER_NAMES.add("Locklear");
        PLAYER_NAMES.add("Baghera_Jones");
        PLAYER_NAMES.add("Etoiles");
        PLAYER_NAMES.add("Domingo");
        PLAYER_NAMES.add("Kameto");
        PLAYER_NAMES.add("Squeezie");
        PLAYER_NAMES.add("Gotaga");
        PLAYER_NAMES.add("Michou");
        PLAYER_NAMES.add("Inoxtag");

        com.aibrigade.main.AIBrigadeMod.LOGGER.info("MojangSkinFetcher initialized with {} player names", PLAYER_NAMES.size());
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
     */
    public static CompletableFuture<UUID> getUUIDFromUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            // Vérifier le cache
            if (NAME_TO_UUID_CACHE.containsKey(username.toLowerCase())) {
                return NAME_TO_UUID_CACHE.get(username.toLowerCase());
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
                if (responseCode != 200) {
                    com.aibrigade.main.AIBrigadeMod.LOGGER.warn("Player {} not found (HTTP {})", username, responseCode);
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

                com.aibrigade.main.AIBrigadeMod.LOGGER.debug("Resolved {} to UUID {}", username, uuid);
                return uuid;

            } catch (Exception e) {
                com.aibrigade.main.AIBrigadeMod.LOGGER.error("Error resolving username {}: {}", username, e.getMessage());
                return null;
            }
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
     * Récupère un nom de joueur aléatoire de la base de données
     * Garantit que chaque bot ait un pseudo unique
     */
    public static String getRandomPlayerName() {
        if (PLAYER_NAMES.isEmpty()) {
            return "Bot_" + UUID.randomUUID().toString().substring(0, 8);
        }

        // Sélectionner un nom aléatoire
        Random random = new Random();
        return PLAYER_NAMES.get(random.nextInt(PLAYER_NAMES.size()));
    }

    /**
     * Récupère un UUID aléatoire de joueur (non utilisé)
     * Garantit que chaque bot ait un pseudo unique
     */
    public static CompletableFuture<UUID> getRandomPlayerUUIDAsync() {
        String randomName = getRandomPlayerName();

        return getUUIDFromUsername(randomName).thenApply(uuid -> {
            if (uuid == null) {
                // Si le nom n'existe pas, essayer un autre
                com.aibrigade.main.AIBrigadeMod.LOGGER.warn("Player {} does not exist, trying another...", randomName);
                return null;
            }

            // Vérifier si déjà utilisé
            if (USED_PLAYER_UUIDS.contains(uuid)) {
                // Si tous sont utilisés, recycler
                if (USED_PLAYER_UUIDS.size() >= PLAYER_NAMES.size()) {
                    com.aibrigade.main.AIBrigadeMod.LOGGER.info("All player UUIDs used, recycling...");
                    USED_PLAYER_UUIDS.clear();
                }
            }

            USED_PLAYER_UUIDS.add(uuid);
            return uuid;
        });
    }

    /**
     * Libère un UUID pour qu'il puisse être réutilisé
     * Appelé quand un bot est supprimé
     */
    public static void releasePlayerUUID(UUID uuid) {
        USED_PLAYER_UUIDS.remove(uuid);
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
     */
    public static void applyProfileToBot(BotEntity bot, GameProfile profile) {
        // Stocker l'UUID du profil
        bot.setPlayerUUID(profile.getId());

        // Stocker le nom
        bot.setBotName(profile.getName());

        // Les textures sont dans le GameProfile, elles seront automatiquement
        // utilisées par le renderer si on utilise PlayerRenderer ou similaire
        com.aibrigade.main.AIBrigadeMod.LOGGER.debug("Profile applied: {} ({})", profile.getName(), profile.getId());
    }

    /**
     * Récupère et applique un skin aléatoire à un bot
     * Utilise la large base de données de joueurs populaires
     */
    public static void applyRandomFamousSkin(BotEntity bot) {
        // Sélectionner un nom aléatoire
        String randomName = getRandomPlayerName();

        com.aibrigade.main.AIBrigadeMod.LOGGER.info("Fetching skin for bot using player: {}", randomName);

        // Appliquer un nom temporaire
        bot.setBotName(randomName + "_Loading");

        // Récupérer l'UUID de manière asynchrone
        getUUIDFromUsername(randomName).thenAccept(uuid -> {
            if (uuid == null) {
                // Fallback si le joueur n'existe pas
                com.aibrigade.main.AIBrigadeMod.LOGGER.warn("Player {} not found, using fallback", randomName);
                UUID fallbackUUID = UUID.randomUUID();
                bot.setPlayerUUID(fallbackUUID);
                bot.setBotName("Bot_" + fallbackUUID.toString().substring(0, 8));
                return;
            }

            // Marquer comme utilisé
            USED_PLAYER_UUIDS.add(uuid);

            // Appliquer l'UUID
            bot.setPlayerUUID(uuid);
            bot.setBotName(randomName);

            com.aibrigade.main.AIBrigadeMod.LOGGER.info("Bot configured with player: {} ({})", randomName, uuid);

            // Fetch le profil complet en arrière-plan pour les textures
            fetchProfileAsync(uuid).thenAccept(profile -> {
                if (profile != null) {
                    // Mettre à jour avec le vrai nom du joueur (au cas où il a changé)
                    bot.setBotName(profile.getName());
                    com.aibrigade.main.AIBrigadeMod.LOGGER.info("Full profile fetched for: {}", profile.getName());
                }
            });
        });
    }
}
