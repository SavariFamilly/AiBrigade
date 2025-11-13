package com.aibrigade.persistence;

import com.aibrigade.bots.BotEntity;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
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

        // Equipment (item IDs)
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

            // Equipment
            data.helmet = bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).toString();
            data.chestplate = bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).toString();
            data.leggings = bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).toString();
            data.boots = bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).toString();
            data.mainHand = bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).toString();
            data.offHand = bot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).toString();

            return data;
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
