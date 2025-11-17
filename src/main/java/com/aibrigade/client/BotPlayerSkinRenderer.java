package com.aibrigade.client;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.bots.MojangSkinFetcher;
import com.aibrigade.main.AIBrigadeMod;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
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
     * Utilise les textures synchronisées depuis le serveur
     */
    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        UUID playerUUID = bot.getPlayerUUID();
        String botName = bot.getBotName();
        String textureValue = bot.getSkinTextureValue();
        String textureSignature = bot.getSkinTextureSignature();

        AIBrigadeMod.LOGGER.info("[Renderer] Getting texture for {} (UUID: {})", botName, playerUUID);
        AIBrigadeMod.LOGGER.info("[Renderer] Texture value length: {}",
            textureValue != null ? textureValue.length() : 0);
        AIBrigadeMod.LOGGER.info("[Renderer] Texture signature length: {}",
            textureSignature != null ? textureSignature.length() : 0);

        // Si pas d'UUID, retourner le skin par défaut
        if (playerUUID == null) {
            AIBrigadeMod.LOGGER.warn("[Renderer] No UUID, returning default skin");
            return DEFAULT_STEVE_SKIN;
        }

        try {
            final String finalBotName = (botName == null || botName.isEmpty()) ? "Bot" : botName;
            Minecraft minecraft = Minecraft.getInstance();

            // Créer le GameProfile avec les textures
            GameProfile profile = new GameProfile(playerUUID, finalBotName);

            // Si on a les textures synchronisées du serveur, les ajouter au profil
            if (textureValue != null && !textureValue.isEmpty()) {
                AIBrigadeMod.LOGGER.info("[Renderer] Applying textures to profile for {}", finalBotName);

                // Ajouter la property "textures" avec value et signature
                Property textureProperty;
                if (textureSignature != null && !textureSignature.isEmpty()) {
                    textureProperty = new Property("textures", textureValue, textureSignature);
                } else {
                    textureProperty = new Property("textures", textureValue);
                }
                profile.getProperties().put("textures", textureProperty);

                // Enregistrer le profil avec le SkinManager pour charger la texture
                if (!SKIN_LOAD_INITIATED.containsKey(playerUUID)) {
                    SKIN_LOAD_INITIATED.put(playerUUID, true);
                    AIBrigadeMod.LOGGER.info("[Renderer] Registering skins with SkinManager for {}", finalBotName);
                    minecraft.getSkinManager().registerSkins(profile, (type, location, profileTexture) -> {
                        AIBrigadeMod.LOGGER.info("[Renderer] ✓ Skin loaded! Type: {}, Location: {}", type, location);
                        // Skin loaded callback - force re-render
                        if (minecraft.level != null && bot.isAlive()) {
                            bot.refreshDimensions();
                        }
                    }, true);
                } else {
                    AIBrigadeMod.LOGGER.info("[Renderer] Skins already registered for {}", finalBotName);
                }
            } else {
                AIBrigadeMod.LOGGER.warn("[Renderer] ⚠ No texture data for {}, will use default", finalBotName);
            }

            // Retourner la texture depuis le skin manager
            ResourceLocation skinLocation = minecraft.getSkinManager().getInsecureSkinLocation(profile);
            AIBrigadeMod.LOGGER.info("[Renderer] Returning skin location: {}", skinLocation);
            return skinLocation;

        } catch (Exception e) {
            AIBrigadeMod.LOGGER.error("[BotSkinRenderer] Error loading skin for {}: {}",
                (botName != null ? botName : "Unknown"), e.getMessage());
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
