package com.aibrigade.client;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.MojangSkinFetcher;
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
     * Utilise le système de Minecraft pour télécharger automatiquement les skins Mojang
     */
    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        UUID playerUUID = bot.getPlayerUUID();
        String botName = bot.getBotName();

        if (playerUUID == null) {
            return DEFAULT_STEVE_SKIN;
        }

        try {
            // Créer un GameProfile avec l'UUID et le nom
            if (botName == null || botName.isEmpty()) {
                botName = "Bot";
            }

            // Variable finale pour utilisation dans les lambdas
            final String finalBotName = botName;

            GameProfile profile = new GameProfile(playerUUID, botName);

            // Obtenir le Minecraft client et les services
            Minecraft minecraft = Minecraft.getInstance();

            // Charger le profil complet depuis les serveurs Mojang (une seule fois)
            if (!SKIN_LOAD_INITIATED.containsKey(playerUUID)) {
                SKIN_LOAD_INITIATED.put(playerUUID, true);

                System.out.println("[BotSkinRenderer] Initiating skin load for " + finalBotName + " (UUID: " + playerUUID + ")");

                // Utiliser le SessionService pour remplir le profil avec les propriétés de texture
                MinecraftSessionService sessionService = minecraft.getMinecraftSessionService();

                // Lancer le téléchargement du profil de manière asynchrone
                Minecraft.getInstance().execute(() -> {
                    try {
                        GameProfile completeProfile = sessionService.fillProfileProperties(profile, false);
                        System.out.println("[BotSkinRenderer] Profile loaded for " + finalBotName +
                            ", has textures: " + !completeProfile.getProperties().isEmpty());
                    } catch (Exception e) {
                        System.err.println("[BotSkinRenderer] Failed to load profile for " + finalBotName + ": " + e.getMessage());
                    }
                });
            }

            // Utiliser le SkinManager pour obtenir le skin
            // Il utilisera le profil complet une fois téléchargé
            return minecraft.getSkinManager().getInsecureSkinLocation(profile);

        } catch (Exception e) {
            System.err.println("[BotSkinRenderer] Error getting skin for " + botName + ": " + e.getMessage());
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
