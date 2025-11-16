package com.aibrigade.client;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.MojangSkinFetcher;
import com.aibrigade.main.AIBrigadeMod;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
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
import net.minecraft.world.entity.player.Player;

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
     * Charge automatiquement les skins Mojang depuis les serveurs via le SkinManager
     */
    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        UUID playerUUID = bot.getPlayerUUID();
        String botName = bot.getBotName();

        if (playerUUID == null) {
            return DEFAULT_STEVE_SKIN;
        }

        try {
            // Make botName final for lambda usage
            final String finalBotName = (botName == null || botName.isEmpty()) ? "Bot" : botName;

            GameProfile profile = new GameProfile(playerUUID, finalBotName);
            Minecraft minecraft = Minecraft.getInstance();

            // Utiliser registerSkins pour charger le profil complet depuis Mojang
            // C'est la méthode que Minecraft utilise pour les vrais joueurs
            if (!SKIN_LOAD_INITIATED.containsKey(playerUUID)) {
                SKIN_LOAD_INITIATED.put(playerUUID, true);

                // registerSkins télécharge automatiquement les textures depuis Mojang
                minecraft.getSkinManager().registerSkins(profile, (type, location, profileTexture) -> {
                    // Force entity to refresh visually after skin loads
                    // This ensures the renderer is called again with the new skin
                    if (minecraft.level != null && bot.isAlive()) {
                        bot.refreshDimensions();
                    }
                }, true);
            }

            // Retourner la texture (sera Steve jusqu'à ce que le téléchargement soit terminé)
            return minecraft.getSkinManager().getInsecureSkinLocation(profile);

        } catch (Exception e) {
            AIBrigadeMod.LOGGER.error("[BotSkinRenderer] ERROR for {}: {}", (botName != null ? botName : "Unknown"), e.getMessage(), e);
            return DEFAULT_STEVE_SKIN;
        }
    }

    @Override
    public void render(BotEntity bot, float entityYaw, float partialTicks, PoseStack poseStack,
                      MultiBufferSource bufferSource, int packedLight) {

        // Appliquer les transformations de base
        poseStack.pushPose();

        super.render(bot, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    protected void scale(BotEntity bot, PoseStack poseStack, float partialTicks) {
        // Taille normale (comme un joueur)
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
