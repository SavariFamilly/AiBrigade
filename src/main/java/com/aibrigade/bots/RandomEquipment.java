package com.aibrigade.bots;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RandomEquipment - Système d'équipement aléatoire pour les bots
 *
 * Selon le cahier des charges:
 * - Pioche en fer
 * - Rien
 * - Pioche en diamant
 * - Steak cuit
 * - Épée en fer
 * - Épée en diamant
 *
 * Distribution variée et naturelle (pas tous avec des épées)
 */
public class RandomEquipment {

    private static final Random RANDOM = new Random();

    /**
     * Types d'équipement possibles selon les spécifications
     */
    public enum EquipmentType {
        NOTHING,           // 20% - Mains vides
        IRON_PICKAXE,      // 15% - Pioche en fer
        DIAMOND_PICKAXE,   // 10% - Pioche en diamant (plus rare)
        COOKED_BEEF,       // 20% - Steak cuit
        IRON_SWORD,        // 20% - Épée en fer
        DIAMOND_SWORD      // 15% - Épée en diamant
    }

    /**
     * Distribution pondérée des équipements pour un effet naturel
     * Total = 100 (pourcentages)
     */
    private static final int[] EQUIPMENT_WEIGHTS = {
        20,  // NOTHING
        15,  // IRON_PICKAXE
        10,  // DIAMOND_PICKAXE
        20,  // COOKED_BEEF
        20,  // IRON_SWORD
        15   // DIAMOND_SWORD
    };

    /**
     * Génère un équipement aléatoire pour un bot avec distribution pondérée
     * @param bot Le bot à équiper
     */
    public static void equipRandomItem(BotEntity bot) {
        // Sélection pondérée basée sur les pourcentages
        EquipmentType type = selectWeightedEquipment();

        ItemStack mainHand = ItemStack.EMPTY;

        switch (type) {
            case NOTHING:
                // Pas d'équipement
                mainHand = ItemStack.EMPTY;
                break;

            case IRON_PICKAXE:
                mainHand = new ItemStack(Items.IRON_PICKAXE);
                break;

            case DIAMOND_PICKAXE:
                mainHand = new ItemStack(Items.DIAMOND_PICKAXE);
                break;

            case COOKED_BEEF:
                // Steak cuit
                mainHand = new ItemStack(Items.COOKED_BEEF, 1);
                break;

            case IRON_SWORD:
                mainHand = new ItemStack(Items.IRON_SWORD);
                break;

            case DIAMOND_SWORD:
                mainHand = new ItemStack(Items.DIAMOND_SWORD);
                break;
        }

        // Équiper le bot
        bot.setMainHandItem(mainHand);

        String itemName = mainHand.isEmpty() ? "Rien" : mainHand.getDisplayName().getString();
        System.out.println("[RandomEquipment] Bot équipé avec: " + itemName + " (type: " + type + ")");
    }

    /**
     * Sélectionne un équipement selon la distribution pondérée
     * @return Le type d'équipement sélectionné
     */
    private static EquipmentType selectWeightedEquipment() {
        int totalWeight = 0;
        for (int weight : EQUIPMENT_WEIGHTS) {
            totalWeight += weight;
        }

        int random = RANDOM.nextInt(totalWeight);
        int currentWeight = 0;

        EquipmentType[] types = EquipmentType.values();
        for (int i = 0; i < types.length; i++) {
            currentWeight += EQUIPMENT_WEIGHTS[i];
            if (random < currentWeight) {
                return types[i];
            }
        }

        // Fallback (ne devrait jamais arriver)
        return EquipmentType.NOTHING;
    }

    /**
     * Équipe un type spécifique d'item
     */
    public static void equipSpecificType(BotEntity bot, EquipmentType type) {
        ItemStack mainHand = ItemStack.EMPTY;

        switch (type) {
            case NOTHING:
                mainHand = ItemStack.EMPTY;
                break;

            case IRON_PICKAXE:
                mainHand = new ItemStack(Items.IRON_PICKAXE);
                break;

            case DIAMOND_PICKAXE:
                mainHand = new ItemStack(Items.DIAMOND_PICKAXE);
                break;

            case COOKED_BEEF:
                mainHand = new ItemStack(Items.COOKED_BEEF, 1);
                break;

            case IRON_SWORD:
                mainHand = new ItemStack(Items.IRON_SWORD);
                break;

            case DIAMOND_SWORD:
                mainHand = new ItemStack(Items.DIAMOND_SWORD);
                break;
        }

        bot.setMainHandItem(mainHand);
    }

    /**
     * Équipe un bot en fonction de son rôle
     */
    public static void equipByRole(BotEntity bot) {
        BotEntity.BotRole role = bot.getRole();

        switch (role) {
            case SOLDIER:
                // Les soldats ont des épées (fer ou diamant)
                if (RANDOM.nextBoolean()) {
                    bot.setMainHandItem(new ItemStack(Items.IRON_SWORD));
                } else {
                    bot.setMainHandItem(new ItemStack(Items.DIAMOND_SWORD));
                }
                break;

            case SCOUT:
                // Les scouts n'ont rien (mains libres pour courir)
                bot.setMainHandItem(ItemStack.EMPTY);
                break;

            case GUARD:
                // Les gardes ont des épées ou rien
                int guardChoice = RANDOM.nextInt(3);
                if (guardChoice == 0) {
                    bot.setMainHandItem(new ItemStack(Items.IRON_SWORD));
                } else if (guardChoice == 1) {
                    bot.setMainHandItem(new ItemStack(Items.DIAMOND_SWORD));
                } else {
                    bot.setMainHandItem(ItemStack.EMPTY);
                }
                break;

            case ENGINEER:
                // Les ingénieurs ont des pioches
                if (RANDOM.nextBoolean()) {
                    bot.setMainHandItem(new ItemStack(Items.IRON_PICKAXE));
                } else {
                    bot.setMainHandItem(new ItemStack(Items.DIAMOND_PICKAXE));
                }
                break;

            case MEDIC:
                // Les medics ont de la nourriture
                bot.setMainHandItem(new ItemStack(Items.COOKED_BEEF, 1));
                break;

            case LEADER:
                // Les leaders ont des épées en diamant
                bot.setMainHandItem(new ItemStack(Items.DIAMOND_SWORD));
                break;

            default:
                equipRandomItem(bot);
                break;
        }
    }

    /**
     * Liste tous les équipements possibles selon les spécifications
     */
    public static List<String> getAvailableEquipment() {
        List<String> equipment = new ArrayList<>();
        equipment.add("Rien (20%)");
        equipment.add("Pioche en fer (15%)");
        equipment.add("Pioche en diamant (10%)");
        equipment.add("Steak cuit (20%)");
        equipment.add("Épée en fer (20%)");
        equipment.add("Épée en diamant (15%)");
        return equipment;
    }
}
