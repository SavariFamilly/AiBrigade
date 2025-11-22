package com.aibrigade.persistence;

import com.aibrigade.bots.BotEntity;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * BotDataSerializer - Serializes and deserializes bot data to/from JSON
 *
 * Handles saving and loading individual bot configurations including:
 * - UUID and entity ID
 * - Name and skin
 * - Position and rotation
 * - Group membership
 * - Behavior type
 * - Stats and equipment
 */
public class BotDataSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Bot data container for JSON serialization
     */
    public static class BotData {
        public String uuid;
        public String name;
        public String skin;
        public String group;
        public String behaviorType;
        public boolean isStatic;
        public float followRadius;

        // Position
        public double posX;
        public double posY;
        public double posZ;

        // Rotation
        public float yaw;
        public float pitch;

        // Stats
        public double health;
        public double maxHealth;

        // Leader
        public String leaderUUID;

        // Equipment (NBT serialized as SNBT string for full data preservation)
        // Uses Minecraft's official SNBT format to preserve enchantments, NBT, durability, etc.
        public String helmet;
        public String chestplate;
        public String leggings;
        public String boots;
        public String mainHand;
        public String offHand;

        public BotData() {
        }

        /**
         * Create BotData from BotEntity
         */
        public static BotData fromBot(BotEntity bot) {
            BotData data = new BotData();

            data.uuid = bot.getUUID().toString();
            data.name = bot.getBotName();
            data.skin = bot.getBotSkin();
            data.group = bot.getBotGroup();
            data.behaviorType = bot.getBehaviorType();
            data.isStatic = bot.isStatic();
            data.followRadius = bot.getFollowRadius();

            // Position
            Vec3 pos = bot.position();
            data.posX = pos.x;
            data.posY = pos.y;
            data.posZ = pos.z;

            // Rotation
            data.yaw = bot.getYRot();
            data.pitch = bot.getXRot();

            // Stats
            data.health = bot.getHealth();
            data.maxHealth = bot.getMaxHealth();

            // Leader
            UUID leaderId = bot.getLeaderId();
            data.leaderUUID = leaderId != null ? leaderId.toString() : null;

            // Equipment (serialize using NBT to preserve all data)
            data.helmet = serializeItemStack(bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
            data.chestplate = serializeItemStack(bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
            data.leggings = serializeItemStack(bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
            data.boots = serializeItemStack(bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
            data.mainHand = serializeItemStack(bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
            data.offHand = serializeItemStack(bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND));

            return data;
        }
    }

    /**
     * Serialize ItemStack to SNBT (String NBT) format
     * Preserves ALL data (enchantments, durability, custom names, NBT tags, etc.)
     * Uses Minecraft's official SNBT format which is the standard for ItemStack serialization
     */
    private static String serializeItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        try {
            CompoundTag nbt = new CompoundTag();
            stack.save(nbt);
            // Convert to SNBT (String NBT) - Official Minecraft format
            return nbt.toString();
        } catch (Exception e) {
            System.err.println("[BotDataSerializer] Error serializing ItemStack: " + e.getMessage());
            return "";
        }
    }

    /**
     * Deserialize SNBT string to ItemStack
     * Restores ALL data preserved during serialization
     * Uses Minecraft's official TagParser for robust deserialization
     */
    public static ItemStack deserializeItemStack(String snbt) {
        if (snbt == null || snbt.isEmpty()) {
            return ItemStack.EMPTY;
        }

        try {
            // Parse SNBT string to CompoundTag using Minecraft's official parser
            CompoundTag nbt = TagParser.parseTag(snbt);
            return ItemStack.of(nbt);
        } catch (Exception e) {
            System.err.println("[BotDataSerializer] Error deserializing ItemStack from SNBT: " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    /**
     * Serialize bot to JSON string
     */
    public static String serializeBot(BotEntity bot) {
        BotData data = BotData.fromBot(bot);
        return GSON.toJson(data);
    }

    /**
     * Serialize bot to JsonObject
     */
    public static JsonObject serializeBotToJson(BotEntity bot) {
        BotData data = BotData.fromBot(bot);
        return GSON.toJsonTree(data).getAsJsonObject();
    }

    /**
     * Deserialize bot from JSON string
     */
    public static BotData deserializeBot(String json) {
        return GSON.fromJson(json, BotData.class);
    }

    /**
     * Deserialize bot from JsonObject
     */
    public static BotData deserializeBotFromJson(JsonObject json) {
        return GSON.fromJson(json, BotData.class);
    }
}
