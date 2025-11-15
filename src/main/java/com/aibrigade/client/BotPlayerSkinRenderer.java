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
     * Utilise le GameProfile si disponible, sinon fallback
     */
    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        UUID playerUUID = bot.getPlayerUUID();

        if (playerUUID == null) {
            return DEFAULT_STEVE_SKIN;
        }

        try {
            // Créer un GameProfile avec l'UUID et le nom du bot
            String botName = bot.getBotName();
            if (botName == null || botName.isEmpty()) {
                botName = "Bot";
            }

            GameProfile profile = new GameProfile(playerUUID, botName);

            // Utiliser le SkinManager de Minecraft pour charger le skin
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var skinManager = minecraft.getSkinManager();

            // Enregistrer le profil pour charger les textures de manière asynchrone
            // Cela déclenche le téléchargement des textures si elles ne sont pas déjà en cache
            skinManager.registerSkins(profile, (type, location, texture) -> {
                // Callback appelé quand la texture est chargée (ne fait rien ici, juste pour déclencher le chargement)
            }, true);

            // Récupérer le skin (sera le skin par défaut jusqu'à ce que le téléchargement soit terminé)
            return skinManager.getInsecureSkinLocation(profile);
        } catch (Exception e) {
            System.err.println("[BotPlayerSkinRenderer] Error loading skin for UUID " + playerUUID + ": " + e.getMessage());
            return DEFAULT_STEVE_SKIN;
        }
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
