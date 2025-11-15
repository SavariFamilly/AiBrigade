package com.aibrigade.client;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.MojangSkinFetcher;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * BotPlayerSkinRenderer - Renderer personnalisé qui affiche les vrais skins Mojang
 *
 * Ce renderer utilise le GameProfile récupéré via MojangSkinFetcher pour afficher
 * le skin du joueur associé à l'UUID du bot.
 *
 * Fonctionnalités:
 * - Affichage du skin Mojang depuis GameProfile
 * - Fallback vers skin par défaut si UUID invalide
 * - Support des couches de rendu (armor, items, etc.)
 * - Modèle de joueur avec animations
 */
public class BotPlayerSkinRenderer extends LivingEntityRenderer<BotEntity, PlayerModel<BotEntity>> {

    private static final ResourceLocation DEFAULT_STEVE_SKIN = DefaultPlayerSkin.getDefaultSkin();

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
     * Utilise les données de texture synchronisées via EntityData
     */
    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        UUID playerUUID = bot.getPlayerUUID();
        String textureValue = bot.getSkinTextureValue();
        String textureSignature = bot.getSkinTextureSignature();

        // Si pas d'UUID ou pas de données de texture, utiliser skin par défaut
        if (playerUUID == null || textureValue.isEmpty()) {
            return DEFAULT_STEVE_SKIN;
        }

        // Reconstruire un GameProfile avec les propriétés de texture
        GameProfile profile = new GameProfile(playerUUID, bot.getBotName());
        // La signature peut être null/vide pour certains serveurs non-authentifiés
        String signature = (textureSignature != null && !textureSignature.isEmpty()) ? textureSignature : null;
        profile.getProperties().put("textures",
            new com.mojang.authlib.properties.Property("textures", textureValue, signature));

        // Convertir le GameProfile en ResourceLocation pour le skin
        return getSkinLocation(profile);
    }

    /**
     * Convertit un GameProfile en ResourceLocation de skin
     * Utilise le système de cache de skins de Minecraft
     */
    private ResourceLocation getSkinLocation(GameProfile profile) {
        try {
            // Utiliser le SkinManager de Minecraft pour obtenir le skin
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var skinManager = minecraft.getSkinManager();

            // Récupérer les informations de skin depuis le profil
            var textureMap = skinManager.getInsecureSkinInformation(profile);

            if (textureMap != null && !textureMap.isEmpty()) {
                // Récupérer la texture de type SKIN
                var skinTexture = textureMap.get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
                if (skinTexture != null) {
                    return skinManager.registerTexture(skinTexture, com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
                }
            }
        } catch (Exception e) {
            System.err.println("[BotPlayerSkinRenderer] Error getting skin for profile: " + e.getMessage());
        }

        // Fallback vers skin par défaut
        return DEFAULT_STEVE_SKIN;
    }

    @Override
    public void render(BotEntity bot, float entityYaw, float partialTicks, PoseStack poseStack,
                      MultiBufferSource bufferSource, int packedLight) {

        // Appliquer les transformations de base
        poseStack.pushPose();

        // Scale si nécessaire (les bots ont la même taille qu'un joueur par défaut)
        // poseStack.scale(0.9375F, 0.9375F, 0.9375F);

        super.render(bot, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    protected void scale(BotEntity bot, PoseStack poseStack, float partialTicks) {
        // Taille normale (comme un joueur)
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
