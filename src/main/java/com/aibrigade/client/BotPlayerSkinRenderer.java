package com.aibrigade.client;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.BotClientOptimizer;
import com.aibrigade.main.AIBrigadeMod;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BotPlayerSkinRenderer - Renderer personnalisé qui affiche les vrais skins Mojang
 *
 * Ce renderer utilise le système de skins de Minecraft pour télécharger et afficher
 * automatiquement les skins associés aux UUIDs des joueurs.
 */
public class BotPlayerSkinRenderer extends LivingEntityRenderer<BotEntity, PlayerModel<BotEntity>> {

    private static final ResourceLocation DEFAULT_STEVE_SKIN = DefaultPlayerSkin.getDefaultSkin();

    // Cache pour éviter de télécharger les skins constamment
    private static final Map<UUID, Boolean> SKIN_LOAD_INITIATED = new ConcurrentHashMap<>();

    public BotPlayerSkinRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        // Ajouter les couches de rendu comme pour un joueur
        this.addLayer(new HumanoidArmorLayer<>(this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new ArrowLayer<>(context, this));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
    }

    /**
     * Récupère la texture (skin) pour ce bot
     * Utilise les textures synchronisées depuis le serveur
     * OPTIMIZED: Removed excessive logging for better performance with 200+ bots
     */
    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        UUID playerUUID = bot.getPlayerUUID();
        String botName = bot.getBotName();
        String textureValue = bot.getSkinTextureValue();
        String textureSignature = bot.getSkinTextureSignature();

        // Si pas d'UUID, retourner le skin par défaut
        if (playerUUID == null) {
            return DEFAULT_STEVE_SKIN;
        }

        try {
            final String finalBotName = (botName == null || botName.isEmpty()) ? "Bot" : botName;
            Minecraft minecraft = Minecraft.getInstance();

            // Créer le GameProfile avec les textures
            GameProfile profile = new GameProfile(playerUUID, finalBotName);

            // Si on a les textures synchronisées du serveur, les ajouter au profil
            if (textureValue != null && !textureValue.isEmpty()) {
                // Ajouter la property "textures" avec value et signature
                Property textureProperty;
                if (textureSignature != null && !textureSignature.isEmpty()) {
                    textureProperty = new Property("textures", textureValue, textureSignature);
                } else {
                    textureProperty = new Property("textures", textureValue);
                }
                profile.getProperties().put("textures", textureProperty);

                // Enregistrer le profil avec le SkinManager pour charger la texture (une seule fois)
                if (!SKIN_LOAD_INITIATED.containsKey(playerUUID)) {
                    SKIN_LOAD_INITIATED.put(playerUUID, true);
                    minecraft.getSkinManager().registerSkins(profile, (type, location, profileTexture) -> {
                        // Skin loaded callback - force re-render
                        if (minecraft.level != null && bot.isAlive()) {
                            bot.refreshDimensions();
                        }
                    }, true);
                }
            }

            // Retourner la texture depuis le skin manager
            return minecraft.getSkinManager().getInsecureSkinLocation(profile);

        } catch (Exception e) {
            // Log errors only (not every texture lookup)
            AIBrigadeMod.LOGGER.error("[BotSkinRenderer] Error loading skin for {}: {}",
                (botName != null ? botName : "Unknown"), e.getMessage());
            return DEFAULT_STEVE_SKIN;
        }
    }

    @Override
    public void render(BotEntity bot, float entityYaw, float partialTicks, PoseStack poseStack,
                      MultiBufferSource bufferSource, int packedLight) {

        // CLIENT OPTIMIZATION: Minimal distance check only
        // All game logic is handled server-side
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            double distanceSqr = minecraft.player.distanceToSqr(bot);

            // Simple render culling - don't render far bots
            if (!BotClientOptimizer.shouldRender(bot, distanceSqr)) {
                return;
            }

            // Simple LOD - skip very distant bots
            int lod = BotClientOptimizer.getSimpleLOD(distanceSqr);
            if (lod >= 1) {
                // Distant bots: skip rendering to save client FPS
                return;
            }
        }

        // Render the bot - all AI/pathfinding is server-side
        poseStack.pushPose();
        super.render(bot, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    protected void scale(BotEntity bot, PoseStack poseStack, float partialTicks) {
        // Taille normale (comme un joueur)
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public boolean shouldRender(BotEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        // PERFORMANCE: Frustum culling - don't render bots outside camera view
        return super.shouldRender(entity, frustum, x, y, z);
    }
}
