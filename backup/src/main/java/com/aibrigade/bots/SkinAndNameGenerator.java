package com.aibrigade.bots;

import java.util.*;

/**
 * SkinAndNameGenerator - Generates realistic/humorous names and assigns varied skins
 *
 * Features:
 * - Multiple name presets (realistic, gamer, humor, randomize)
 * - Probability-based name selection
 * - Skin URL management for player textures
 * - Integration with BotEntity for custom names and skins
 */
public class SkinAndNameGenerator {

    private static final Random RANDOM = new Random();

    // ==================== NAME PRESETS ====================

    /**
     * Realistic first names
     */
    private static final String[] REALISTIC_FIRST_NAMES = {
        "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph",
        "Thomas", "Christopher", "Daniel", "Matthew", "Anthony", "Mark", "Donald", "Steven",
        "Andrew", "Paul", "Joshua", "Kenneth", "Kevin", "Brian", "George", "Timothy",
        "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica",
        "Sarah", "Karen", "Lisa", "Nancy", "Betty", "Margaret", "Sandra", "Ashley",
        "Emily", "Donna", "Michelle", "Carol", "Amanda", "Melissa", "Deborah", "Stephanie"
    };

    /**
     * Realistic last names
     */
    private static final String[] REALISTIC_LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris",
        "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young", "Allen"
    };

    /**
     * Gamer-style names
     */
    private static final String[] GAMER_NAMES = {
        "xXShadowXx", "DarkKnight", "SilentAssassin", "PhantomStriker", "BlazeMaster",
        "FrostByte", "NoobSlayer", "ProGamer360", "EpicNinja", "LegendKiller",
        "ViperStrike", "ThunderBolt", "DragonSlayer", "StealthSniper", "CyberPunk",
        "ToxicAvenger", "MidnightRider", "IronFist", "SavageHunter", "ApexPredator",
        "ShadowReaper", "BloodHound", "GhostRider", "NightCrawler", "DeathDealer",
        "WarLord", "BeastMode", "RogueAgent", "EliteForce", "VenomStrike",
        "CrimsonBlade", "SteelTitan", "FuryWarrior", "VoidWalker", "NeonPhantom"
    };

    /**
     * Humorous/Silly names
     */
    private static final String[] HUMOR_NAMES = {
        "PotatoChip", "TacoBell", "BurgerKing", "PizzaTime", "ChickenNugget",
        "ToasterStrudel", "WaffleHouse", "PancakePete", "CerealKiller", "MuffinMan",
        "CookieMonster", "DoughnutDan", "BaconBits", "CheeseWhiz", "PickleRick",
        "SirLoinSteak", "CaptainCrunch", "MrPotatoHead", "FruitLoop", "JellyBean",
        "SugarRush", "CandyCane", "GummyBear", "LollipopLarry", "ChocolateThunder",
        "VanillaIce", "SpaghettiYeti", "NachoLibre", "SushiSamurai", "RamenNoodle",
        "BubbleTea", "CoffeeCup", "TeaBag", "SodaPop", "MilkShake",
        "HotDogHero", "BreadStick", "ButterFingers", "PeanutButter", "JellyTime"
    };

    /**
     * Name prefixes for randomization
     */
    private static final String[] NAME_PREFIXES = {
        "Sir", "Lord", "King", "Duke", "Baron", "Count", "Captain", "Major",
        "General", "Admiral", "Doctor", "Professor", "Agent", "Sergeant", "Master"
    };

    /**
     * Name suffixes for randomization
    */
    private static final String[] NAME_SUFFIXES = {
        "Jr", "Sr", "III", "IV", "the Great", "the Wise", "the Bold", "the Brave",
        "the Swift", "the Strong", "the Mighty", "the Fearless", "the Valiant"
    };

    // ==================== SKIN PRESETS ====================

    /**
     * Default Minecraft player skins
     */
    private static final String[] DEFAULT_SKINS = {
        "minecraft:textures/entity/player/wide/steve.png",
        "minecraft:textures/entity/player/wide/alex.png"
    };

    /**
     * Preset categories
     */
    public enum NamePreset {
        REALISTIC,
        GAMER,
        HUMOR,
        RANDOMIZE,
        MIXED
    }

    // ==================== NAME GENERATION ====================

    /**
     * Generate a name based on the specified preset
     *
     * @param preset The preset category to use
     * @return Generated name string
     */
    public static String generateName(NamePreset preset) {
        switch (preset) {
            case REALISTIC:
                return generateRealisticName();
            case GAMER:
                return generateGamerName();
            case HUMOR:
                return generateHumorName();
            case RANDOMIZE:
                return generateRandomName();
            case MIXED:
                return generateMixedName();
            default:
                return generateRealisticName();
        }
    }

    /**
     * Generate a realistic name (First + Last)
     */
    private static String generateRealisticName() {
        String first = REALISTIC_FIRST_NAMES[RANDOM.nextInt(REALISTIC_FIRST_NAMES.length)];
        String last = REALISTIC_LAST_NAMES[RANDOM.nextInt(REALISTIC_LAST_NAMES.length)];

        // 20% chance to add suffix
        if (RANDOM.nextDouble() < 0.20) {
            String suffix = NAME_SUFFIXES[RANDOM.nextInt(NAME_SUFFIXES.length)];
            return first + " " + last + " " + suffix;
        }

        return first + " " + last;
    }

    /**
     * Generate a gamer-style name
     */
    private static String generateGamerName() {
        String baseName = GAMER_NAMES[RANDOM.nextInt(GAMER_NAMES.length)];

        // 30% chance to add numbers
        if (RANDOM.nextDouble() < 0.30) {
            int number = RANDOM.nextInt(1000);
            return baseName + number;
        }

        return baseName;
    }

    /**
     * Generate a humorous name
     */
    private static String generateHumorName() {
        String baseName = HUMOR_NAMES[RANDOM.nextInt(HUMOR_NAMES.length)];

        // 25% chance to add prefix
        if (RANDOM.nextDouble() < 0.25) {
            String prefix = NAME_PREFIXES[RANDOM.nextInt(NAME_PREFIXES.length)];
            return prefix + " " + baseName;
        }

        return baseName;
    }

    /**
     * Generate a completely random name from all categories
     */
    private static String generateRandomName() {
        int category = RANDOM.nextInt(3);
        switch (category) {
            case 0:
                return generateRealisticName();
            case 1:
                return generateGamerName();
            case 2:
                return generateHumorName();
            default:
                return "Bot" + RANDOM.nextInt(9999);
        }
    }

    /**
     * Generate a mixed name combining elements from multiple categories
     */
    private static String generateMixedName() {
        StringBuilder name = new StringBuilder();

        // 40% chance for prefix
        if (RANDOM.nextDouble() < 0.40) {
            name.append(NAME_PREFIXES[RANDOM.nextInt(NAME_PREFIXES.length)]).append(" ");
        }

        // Pick a base name from any category
        int category = RANDOM.nextInt(4);
        switch (category) {
            case 0:
                name.append(REALISTIC_FIRST_NAMES[RANDOM.nextInt(REALISTIC_FIRST_NAMES.length)]);
                break;
            case 1:
                name.append(GAMER_NAMES[RANDOM.nextInt(GAMER_NAMES.length)]);
                break;
            case 2:
                name.append(HUMOR_NAMES[RANDOM.nextInt(HUMOR_NAMES.length)]);
                break;
            case 3:
                name.append(REALISTIC_LAST_NAMES[RANDOM.nextInt(REALISTIC_LAST_NAMES.length)]);
                break;
        }

        // 30% chance for number suffix
        if (RANDOM.nextDouble() < 0.30) {
            name.append(RANDOM.nextInt(1000));
        }

        return name.toString();
    }

    /**
     * Generate a name from a specific preset string
     *
     * @param presetName Name of the preset (realistic, gamer, humor, randomize, mixed)
     * @return Generated name
     */
    public static String generateNameFromPreset(String presetName) {
        try {
            NamePreset preset = NamePreset.valueOf(presetName.toUpperCase());
            return generateName(preset);
        } catch (IllegalArgumentException e) {
            // Default to randomize if preset not found
            return generateName(NamePreset.RANDOMIZE);
        }
    }

    // ==================== SKIN GENERATION ====================

    /**
     * Get a random skin from default skins
     *
     * @return ResourceLocation path to skin texture
     */
    public static String getRandomSkin() {
        return DEFAULT_SKINS[RANDOM.nextInt(DEFAULT_SKINS.length)];
    }

    /**
     * Get a skin based on preset
     * Currently returns random default skin, can be extended for preset-specific skins
     *
     * @param preset The preset category
     * @return ResourceLocation path to skin texture
     */
    public static String getSkinForPreset(NamePreset preset) {
        // For now, return random default skin
        // Can be extended to have preset-specific skin pools
        return getRandomSkin();
    }

    /**
     * Get a skin from a preset name string
     *
     * @param presetName Name of the preset
     * @return ResourceLocation path to skin texture
     */
    public static String getSkinFromPreset(String presetName) {
        try {
            NamePreset preset = NamePreset.valueOf(presetName.toUpperCase());
            return getSkinForPreset(preset);
        } catch (IllegalArgumentException e) {
            return getRandomSkin();
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get all available preset names
     *
     * @return List of preset name strings
     */
    public static List<String> getAvailablePresets() {
        List<String> presets = new ArrayList<>();
        for (NamePreset preset : NamePreset.values()) {
            presets.add(preset.name().toLowerCase());
        }
        return presets;
    }

    /**
     * Check if a preset name is valid
     *
     * @param presetName Name to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidPreset(String presetName) {
        try {
            NamePreset.valueOf(presetName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Generate a batch of names for a group
     *
     * @param count Number of names to generate
     * @param preset Preset to use
     * @return List of generated names
     */
    public static List<String> generateNameBatch(int count, NamePreset preset) {
        List<String> names = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String name = generateName(preset);

            // Ensure unique names by adding number if duplicate
            int attempt = 0;
            String uniqueName = name;
            while (usedNames.contains(uniqueName) && attempt < 10) {
                uniqueName = name + "_" + (++attempt);
            }

            names.add(uniqueName);
            usedNames.add(uniqueName);
        }

        return names;
    }

    /**
     * Get preset description for help text
     *
     * @param preset The preset to describe
     * @return Description string
     */
    public static String getPresetDescription(NamePreset preset) {
        switch (preset) {
            case REALISTIC:
                return "Realistic first + last names (e.g., John Smith, Mary Johnson)";
            case GAMER:
                return "Gamer-style names (e.g., xXShadowXx, ProGamer360)";
            case HUMOR:
                return "Humorous/silly names (e.g., PotatoChip, TacoBell)";
            case RANDOMIZE:
                return "Completely random selection from all categories";
            case MIXED:
                return "Mixed elements from multiple categories with prefixes/suffixes";
            default:
                return "Unknown preset";
        }
    }
}
