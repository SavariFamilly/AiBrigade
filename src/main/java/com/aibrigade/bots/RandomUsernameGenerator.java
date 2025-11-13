package com.aibrigade.bots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * RandomUsernameGenerator - Génère des pseudos aléatoires de style Minecraft
 *
 * Génère des pseudos réalistes qui ressemblent à de vrais joueurs Minecraft
 * en combinant préfixes, suffixes et patterns courants.
 */
public class RandomUsernameGenerator {

    private static final Random RANDOM = new Random();

    // Préfixes courants dans les pseudos Minecraft
    private static final String[] PREFIXES = {
        "Dark", "Shadow", "Light", "Fire", "Ice", "Storm", "Night", "Day",
        "Sky", "Star", "Moon", "Sun", "Red", "Blue", "Green", "Gold",
        "Silver", "Black", "White", "Dragon", "Wolf", "Tiger", "Lion",
        "Fox", "Bear", "Eagle", "Hawk", "Raven", "Ninja", "Samurai",
        "Knight", "Warrior", "Mage", "Wizard", "Archer", "Hunter", "Ranger",
        "King", "Queen", "Prince", "Lord", "Master", "Cyber", "Neon",
        "Pixel", "Retro", "Epic", "Mega", "Super", "Ultra", "Hyper",
        "Pro", "Elite", "Legend", "Mythic", "Divine", "Sacred", "Ancient",
        "Frozen", "Flame", "Thunder", "Lightning", "Ocean", "Mountain",
        "Forest", "Desert", "Crystal", "Diamond", "Emerald", "Ruby",
        "Sapphire", "Iron", "Steel", "Bronze", "Copper", "Ghost",
        "Phantom", "Spirit", "Soul", "Demon", "Angel", "Devil", "Hero",
        "Villain", "Rogue", "Bandit", "Pirate", "Viking", "Spartan"
    };

    // Suffixes courants
    private static final String[] SUFFIXES = {
        "Slayer", "Killer", "Destroyer", "Builder", "Crafter", "Miner",
        "Fighter", "Striker", "Blaster", "Smasher", "Crusher", "Breaker",
        "Master", "Lord", "King", "Boss", "Chief", "Captain", "Leader",
        "Warrior", "Knight", "Ninja", "Samurai", "Hunter", "Ranger",
        "Wizard", "Mage", "Sorcerer", "Enchanter", "Alchemist",
        "Gamer", "Player", "Pro", "God", "Legend", "Hero", "Champion",
        "Noob", "Newbie", "Rookie", "Veteran", "Elder", "Ancient",
        "YT", "TV", "Plays", "Gaming", "Stream", "Live", "Official",
        "HD", "4K", "Prime", "Plus", "Max", "Ultra", "Mega",
        "Craft", "Build", "Mine", "PvP", "Survival", "Creative",
        "Redstone", "Command", "Mod", "Pack"
    };

    // Mots du milieu optionnels
    private static final String[] MIDDLE_WORDS = {
        "The", "Of", "And", "In", "On", "At", "By", "For", "With",
        "X", "Vs", "Pro", "MC", "Craft"
    };

    // Noms génériques style Minecraft
    private static final String[] GENERIC_NAMES = {
        "Steve", "Alex", "Creeper", "Enderman", "Zombie", "Skeleton",
        "Spider", "Slime", "Ghast", "Blaze", "Pigman", "Villager",
        "Wither", "Dragon", "Golem", "Squid", "Cow", "Pig", "Sheep",
        "Diamond", "Emerald", "Gold", "Iron", "Stone", "Wood",
        "Grass", "Dirt", "Sand", "Gravel", "Obsidian", "Bedrock",
        "Redstone", "Lapis", "Coal", "Quartz", "Netherite"
    };

    // Suffixes numériques courants
    private static final String[] NUMBER_PATTERNS = {
        "123", "456", "789", "99", "88", "77", "1234", "420", "69",
        "2000", "2001", "2010", "13", "21", "42", "100", "1000",
        "YT", "HD", "4K", "XD", "OP", "GG"
    };

    /**
     * Génère un pseudo aléatoire de style Minecraft
     *
     * @return Un pseudo aléatoire
     */
    public static String generateRandomUsername() {
        int pattern = RANDOM.nextInt(10);

        return switch (pattern) {
            case 0 -> generatePrefixSuffix(); // DarkSlayer
            case 1 -> generatePrefixSuffixNumber(); // DarkSlayer123
            case 2 -> generatePrefixGenericName(); // DarkSteve
            case 3 -> generateGenericNameSuffix(); // SteveKiller
            case 4 -> generateGenericNameNumber(); // Steve123
            case 5 -> generateSingleWithNumber(); // Shadow99
            case 6 -> generateTripleWord(); // DarkTheSlayer
            case 7 -> generateWithUnderscore(); // Dark_Slayer
            case 8 -> generateXStyleName(); // xDarkSlayerx
            default -> generateSimpleName(); // DarkWarrior
        };
    }

    /**
     * Pattern: Prefix + Suffix
     * Exemples: DarkSlayer, FireMage, IceKing
     */
    private static String generatePrefixSuffix() {
        String prefix = PREFIXES[RANDOM.nextInt(PREFIXES.length)];
        String suffix = SUFFIXES[RANDOM.nextInt(SUFFIXES.length)];
        return prefix + suffix;
    }

    /**
     * Pattern: Prefix + Suffix + Number
     * Exemples: DarkSlayer123, FireMage99
     */
    private static String generatePrefixSuffixNumber() {
        String base = generatePrefixSuffix();
        String number = NUMBER_PATTERNS[RANDOM.nextInt(NUMBER_PATTERNS.length)];
        return base + number;
    }

    /**
     * Pattern: Prefix + Generic Name
     * Exemples: DarkSteve, FireCreeper
     */
    private static String generatePrefixGenericName() {
        String prefix = PREFIXES[RANDOM.nextInt(PREFIXES.length)];
        String name = GENERIC_NAMES[RANDOM.nextInt(GENERIC_NAMES.length)];
        return prefix + name;
    }

    /**
     * Pattern: Generic Name + Suffix
     * Exemples: SteveKiller, CreeperSlayer
     */
    private static String generateGenericNameSuffix() {
        String name = GENERIC_NAMES[RANDOM.nextInt(GENERIC_NAMES.length)];
        String suffix = SUFFIXES[RANDOM.nextInt(SUFFIXES.length)];
        return name + suffix;
    }

    /**
     * Pattern: Generic Name + Number
     * Exemples: Steve123, Alex456
     */
    private static String generateGenericNameNumber() {
        String name = GENERIC_NAMES[RANDOM.nextInt(GENERIC_NAMES.length)];
        String number = NUMBER_PATTERNS[RANDOM.nextInt(NUMBER_PATTERNS.length)];
        return name + number;
    }

    /**
     * Pattern: Single Word + Number
     * Exemples: Shadow99, Dragon420
     */
    private static String generateSingleWithNumber() {
        String word = RANDOM.nextBoolean()
            ? PREFIXES[RANDOM.nextInt(PREFIXES.length)]
            : GENERIC_NAMES[RANDOM.nextInt(GENERIC_NAMES.length)];
        String number = NUMBER_PATTERNS[RANDOM.nextInt(NUMBER_PATTERNS.length)];
        return word + number;
    }

    /**
     * Pattern: Prefix + Middle + Suffix
     * Exemples: DarkTheSlayer, FireOfDoom
     */
    private static String generateTripleWord() {
        String prefix = PREFIXES[RANDOM.nextInt(PREFIXES.length)];
        String middle = MIDDLE_WORDS[RANDOM.nextInt(MIDDLE_WORDS.length)];
        String suffix = SUFFIXES[RANDOM.nextInt(SUFFIXES.length)];
        return prefix + middle + suffix;
    }

    /**
     * Pattern: Word_Word
     * Exemples: Dark_Slayer, Fire_Mage
     */
    private static String generateWithUnderscore() {
        String word1 = RANDOM.nextBoolean()
            ? PREFIXES[RANDOM.nextInt(PREFIXES.length)]
            : GENERIC_NAMES[RANDOM.nextInt(GENERIC_NAMES.length)];
        String word2 = RANDOM.nextBoolean()
            ? SUFFIXES[RANDOM.nextInt(SUFFIXES.length)]
            : GENERIC_NAMES[RANDOM.nextInt(GENERIC_NAMES.length)];
        return word1 + "_" + word2;
    }

    /**
     * Pattern: xNamex
     * Exemples: xDarkSlayerx, xProGamerx
     */
    private static String generateXStyleName() {
        String base = generatePrefixSuffix();
        return "x" + base + "x";
    }

    /**
     * Pattern: Simple word combination
     * Exemples: DarkWarrior, FireKnight
     */
    private static String generateSimpleName() {
        return generatePrefixSuffix();
    }

    /**
     * Génère plusieurs pseudos aléatoires
     *
     * @param count Nombre de pseudos à générer
     * @return Liste de pseudos
     */
    public static List<String> generateMultipleUsernames(int count) {
        List<String> usernames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            usernames.add(generateRandomUsername());
        }
        return usernames;
    }

    /**
     * Génère un pseudo aléatoire qui respecte les contraintes Minecraft
     * (3-16 caractères, alphanumérique + underscore)
     *
     * @return Un pseudo valide pour Minecraft
     */
    public static String generateValidMinecraftUsername() {
        String username;
        int attempts = 0;
        int maxAttempts = 50;

        do {
            username = generateRandomUsername();
            attempts++;
        } while (!isValidMinecraftUsername(username) && attempts < maxAttempts);

        // Si après 50 tentatives on n'a pas de nom valide, créer un simple
        if (!isValidMinecraftUsername(username)) {
            String prefix = PREFIXES[RANDOM.nextInt(PREFIXES.length)];
            String number = String.valueOf(RANDOM.nextInt(100));
            username = prefix + number;
        }

        return username;
    }

    /**
     * Vérifie si un pseudo respecte les règles Minecraft
     * - 3 à 16 caractères
     * - Seulement lettres, chiffres et underscores
     * - Pas d'underscore au début ou à la fin
     */
    private static boolean isValidMinecraftUsername(String username) {
        if (username == null) return false;
        if (username.length() < 3 || username.length() > 16) return false;
        if (username.startsWith("_") || username.endsWith("_")) return false;
        return username.matches("^[a-zA-Z0-9_]+$");
    }
}
