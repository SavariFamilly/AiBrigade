package com.aibrigade.debug;

import com.aibrigade.bots.BotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;

/**
 * DebugVisualizer - Visual debugging tools for bot AI
 *
 * Features:
 * - Pathfinding visualization (lines showing current path)
 * - Target highlighting (boxes around targets)
 * - Behavior state display (text above bots)
 * - Group connections (lines between group members and leader)
 * - Range circles (follow radius, attack range, etc.)
 * - Navigation goal visualization
 */
public class DebugVisualizer {

    private static boolean enabled = false;
    private static boolean showPaths = true;
    private static boolean showTargets = true;
    private static boolean showBehaviors = true;
    private static boolean showGroups = true;
    private static boolean showRanges = true;

    private static final Map<UUID, List<Vec3>> pathCache = new HashMap<>();
    private static final int PATH_CACHE_DURATION = 100; // ticks

    /**
     * Enable/disable debug visualization
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    /**
     * Check if debug visualization is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Toggle specific debug features
     */
    public static void setShowPaths(boolean show) {
        showPaths = show;
    }

    public static void setShowTargets(boolean show) {
        showTargets = show;
    }

    public static void setShowBehaviors(boolean show) {
        showBehaviors = show;
    }

    public static void setShowGroups(boolean show) {
        showGroups = show;
    }

    public static void setShowRanges(boolean show) {
        showRanges = show;
    }

    /**
     * Render debug visuals for a bot
     * Called from client-side rendering
     */
    public static void renderBotDebug(BotEntity bot, PoseStack poseStack, MultiBufferSource buffer, float partialTicks) {
        if (!enabled) {
            return;
        }

        // Render pathfinding
        // MAJOR FIX #31: Add null check for getNavigation()
        var navigation = bot.getNavigation();
        if (showPaths && navigation != null && navigation.getPath() != null) {
            renderPath(bot, navigation.getPath(), poseStack, buffer);
        }

        // Render target
        if (showTargets && bot.getTarget() != null) {
            renderTarget(bot.getTarget(), poseStack, buffer);
        }

        // Render behavior state (handled separately as text overlay)
        // This would require additional client-side integration

        // Render group connections
        // Note: Would need to look up leader entity by UUID
        // if (showGroups && bot.getLeaderId() != null) {
        //     renderGroupConnection(bot, leaderEntity, poseStack, buffer);
        // }

        // Render ranges
        if (showRanges) {
            renderRanges(bot, poseStack, buffer);
        }
    }

    /**
     * Render pathfinding path
     */
    private static void renderPath(BotEntity bot, Path path, PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        Vec3 botPos = bot.position();

        for (int i = 0; i < path.getNodeCount() - 1; i++) {
            BlockPos currentNode = path.getNode(i).asBlockPos();
            BlockPos nextNode = path.getNode(i + 1).asBlockPos();

            // Calculate positions relative to bot
            float x1 = (float) (currentNode.getX() + 0.5 - botPos.x);
            float y1 = (float) (currentNode.getY() + 0.5 - botPos.y);
            float z1 = (float) (currentNode.getZ() + 0.5 - botPos.z);

            float x2 = (float) (nextNode.getX() + 0.5 - botPos.x);
            float y2 = (float) (nextNode.getY() + 0.5 - botPos.y);
            float z2 = (float) (nextNode.getZ() + 0.5 - botPos.z);

            // Draw line (cyan color)
            vertexConsumer.vertex(matrix, x1, y1, z1).color(0, 255, 255, 255).normal(0, 1, 0).endVertex();
            vertexConsumer.vertex(matrix, x2, y2, z2).color(0, 255, 255, 255).normal(0, 1, 0).endVertex();
        }
    }

    /**
     * Render target highlight box
     */
    private static void renderTarget(Entity target, PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        AABB box = target.getBoundingBox();

        // Expand box slightly for visibility
        box = box.inflate(0.1);

        // Draw bounding box (red color)
        drawBox(matrix, vertexConsumer, box, 255, 0, 0, 255);
    }

    /**
     * Render connection line between bot and leader
     */
    private static void renderGroupConnection(BotEntity bot, Entity leader, PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        Vec3 botPos = bot.position().add(0, bot.getBbHeight() / 2, 0);
        Vec3 leaderPos = leader.position().add(0, leader.getBbHeight() / 2, 0);

        float x1 = (float) (botPos.x - bot.getX());
        float y1 = (float) (botPos.y - bot.getY());
        float z1 = (float) (botPos.z - bot.getZ());

        float x2 = (float) (leaderPos.x - bot.getX());
        float y2 = (float) (leaderPos.y - bot.getY());
        float z2 = (float) (leaderPos.z - bot.getZ());

        // Draw line (green color)
        vertexConsumer.vertex(matrix, x1, y1, z1).color(0, 255, 0, 128).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, x2, y2, z2).color(0, 255, 0, 128).normal(0, 1, 0).endVertex();
    }

    /**
     * Render range circles
     */
    private static void renderRanges(BotEntity bot, PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float followRadius = bot.getFollowRadius();

        // Draw follow radius circle (yellow color)
        drawCircle(matrix, vertexConsumer, 0, 0, 0, followRadius, 255, 255, 0, 128, 32);
    }

    /**
     * Draw a circle in the XZ plane
     */
    private static void drawCircle(Matrix4f matrix, VertexConsumer vertexConsumer,
                                   float x, float y, float z, float radius,
                                   int r, int g, int b, int a, int segments) {
        float angleStep = (float) (Math.PI * 2 / segments);

        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;

            float x1 = x + radius * (float) Math.cos(angle1);
            float z1 = z + radius * (float) Math.sin(angle1);

            float x2 = x + radius * (float) Math.cos(angle2);
            float z2 = z + radius * (float) Math.sin(angle2);

            vertexConsumer.vertex(matrix, x1, y, z1).color(r, g, b, a).normal(0, 1, 0).endVertex();
            vertexConsumer.vertex(matrix, x2, y, z2).color(r, g, b, a).normal(0, 1, 0).endVertex();
        }
    }

    /**
     * Draw a bounding box
     */
    private static void drawBox(Matrix4f matrix, VertexConsumer vertexConsumer, AABB box,
                                int r, int g, int b, int a) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom face
        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        // Top face
        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        // Vertical edges
        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();

        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0).endVertex();
    }

    /**
     * Get debug info string for a bot
     */
    public static List<String> getDebugInfo(BotEntity bot) {
        List<String> info = new ArrayList<>();

        info.add("Bot: " + bot.getBotName());
        info.add("Group: " + bot.getBotGroup());
        info.add("Behavior: " + bot.getBehaviorType());
        info.add("Health: " + String.format("%.1f/%.1f", bot.getHealth(), bot.getMaxHealth()));

        if (bot.getTarget() != null) {
            info.add("Target: " + bot.getTarget().getName().getString());
            info.add("Distance: " + String.format("%.1f", bot.distanceTo(bot.getTarget())));
        }

        if (bot.getLeaderId() != null) {
            info.add("Leader ID: " + bot.getLeaderId().toString());
            // Note: Would need to look up leader entity to show name and distance
        }

        info.add("Follow Radius: " + bot.getFollowRadius());
        info.add("Static: " + bot.isStatic());

        // MAJOR FIX #31: Add null check for getNavigation()
        var navigation = bot.getNavigation();
        if (navigation != null && navigation.getPath() != null) {
            Path path = navigation.getPath();
            info.add("Path Nodes: " + path.getNodeCount());
            info.add("Path Progress: " + path.getNextNodeIndex() + "/" + path.getNodeCount());
        }

        return info;
    }

    /**
     * Clear cached data
     */
    public static void clearCache() {
        pathCache.clear();
    }
}
