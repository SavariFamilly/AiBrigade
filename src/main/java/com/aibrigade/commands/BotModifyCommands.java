package com.aibrigade.commands;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.BotManager;
import com.aibrigade.bots.MojangSkinFetcher;
import com.aibrigade.main.AIBrigadeMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/**
 * BotModifyCommands - Helper class for /aibrigade modify commands
 * Handles name changes, armor equipment, and hand/offhand item assignment
 */
public class BotModifyCommands {
    private static final Random RANDOM = new Random();

    /**
     * Change bot name and synchronize skin from the new username
     * @param botManager The bot manager
     * @param oldName Current bot name
     * @param newName New bot name to apply
     * @return true if successful
     */
    public static boolean modifyBotName(BotManager botManager, String oldName, String newName) {
        BotEntity bot = botManager.findBotByName(oldName);
        if (bot == null) {
            return false;
        }

        // Apply new skin based on new username
        MojangSkinFetcher.applyPlayerSkin(bot, newName);

        AIBrigadeMod.LOGGER.info("Changed bot name from {} to {}", oldName, newName);
        return true;
    }

    /**
     * Modify bot armor with random or preset materials
     * @param botManager The bot manager
     * @param botName Target bot name
     * @param slot Armor slot: full, head, chest, legs, boots
     * @param type random or preset
     * @param materialsStr Combined material string (e.g., "diamondironleather")
     * @return true if successful
     */
    public static boolean modifyBotArmor(BotManager botManager, String botName, String slot, String type, String materialsStr) {
        BotEntity bot = botManager.findBotByName(botName);
        if (bot == null) {
            return false;
        }

        // Parse materials from combined string
        String[] materials = parseMaterials(materialsStr);
        if (materials.length == 0) {
            return false;
        }

        boolean isRandom = type.equalsIgnoreCase("random");

        if (slot.equalsIgnoreCase("full")) {
            // Equip full armor set
            equipArmorSlot(bot, EquipmentSlot.HEAD, materials, isRandom);
            equipArmorSlot(bot, EquipmentSlot.CHEST, materials, isRandom);
            equipArmorSlot(bot, EquipmentSlot.LEGS, materials, isRandom);
            equipArmorSlot(bot, EquipmentSlot.FEET, materials, isRandom);
        } else {
            EquipmentSlot targetSlot = switch (slot.toLowerCase()) {
                case "head" -> EquipmentSlot.HEAD;
                case "chest" -> EquipmentSlot.CHEST;
                case "legs" -> EquipmentSlot.LEGS;
                case "boots", "feet" -> EquipmentSlot.FEET;
                default -> null;
            };

            if (targetSlot == null) {
                return false;
            }

            equipArmorSlot(bot, targetSlot, materials, isRandom);
        }

        return true;
    }

    /**
     * Set item in bot's main hand
     * @param botManager The bot manager
     * @param botName Target bot name
     * @param itemName Item to equip (e.g., "diamond_sword", "iron_pickaxe")
     * @return true if successful
     */
    public static boolean modifyBotHand(BotManager botManager, String botName, String itemName) {
        BotEntity bot = botManager.findBotByName(botName);
        if (bot == null) {
            return false;
        }

        ItemStack item = parseItemStack(itemName);
        if (item.isEmpty()) {
            return false;
        }

        bot.setItemSlot(EquipmentSlot.MAINHAND, item);
        AIBrigadeMod.LOGGER.info("Set {} in hand for bot {}", itemName, botName);
        return true;
    }

    /**
     * Set item in bot's offhand
     * @param botManager The bot manager
     * @param botName Target bot name
     * @param itemName Item to equip (e.g., "shield", "torch")
     * @return true if successful
     */
    public static boolean modifyBotOffhand(BotManager botManager, String botName, String itemName) {
        BotEntity bot = botManager.findBotByName(botName);
        if (bot == null) {
            return false;
        }

        ItemStack item = parseItemStack(itemName);
        if (item.isEmpty()) {
            return false;
        }

        bot.setItemSlot(EquipmentSlot.OFFHAND, item);
        AIBrigadeMod.LOGGER.info("Set {} in offhand for bot {}", itemName, botName);
        return true;
    }

    /**
     * Parse combined material string into array
     * Example: "diamondironleather" -> ["diamond", "iron", "leather"]
     */
    private static String[] parseMaterials(String materialsStr) {
        String lower = materialsStr.toLowerCase();

        // Known material names
        String[] knownMaterials = {"netherite", "diamond", "chainmail", "golden", "iron", "leather"};

        java.util.List<String> found = new java.util.ArrayList<>();
        for (String material : knownMaterials) {
            if (lower.contains(material)) {
                found.add(material);
                // Replace "golden" with "gold" for item naming
                if (material.equals("golden")) {
                    found.set(found.size() - 1, "gold");
                }
            }
        }

        return found.toArray(new String[0]);
    }

    /**
     * Equip armor piece on bot
     */
    private static void equipArmorSlot(BotEntity bot, EquipmentSlot slot, String[] materials, boolean random) {
        String material = random ? materials[RANDOM.nextInt(materials.length)] : materials[0];

        String slotName = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            default -> null;
        };

        if (slotName == null) {
            return;
        }

        // Handle special case for "gold" -> "golden"
        String itemMaterial = material.equals("gold") ? "golden" : material;
        String itemName = itemMaterial + "_" + slotName;

        ItemStack armorPiece = parseItemStack(itemName);
        if (!armorPiece.isEmpty()) {
            bot.setItemSlot(slot, armorPiece);
        }
    }

    /**
     * Parse item name to ItemStack
     * @param itemName Item identifier (e.g., "diamond_sword", "iron_pickaxe")
     * @return ItemStack or empty if not found
     */
    private static ItemStack parseItemStack(String itemName) {
        try {
            // Try with minecraft namespace
            ResourceLocation itemId = new ResourceLocation("minecraft", itemName.toLowerCase());
            var item = BuiltInRegistries.ITEM.get(itemId);

            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            AIBrigadeMod.LOGGER.warn("Failed to parse item: {}", itemName);
        }

        return ItemStack.EMPTY;
    }
}
