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
     * Charge automatiquement les skins Mojang depuis les serveurs via le SkinManager
     */
    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        UUID playerUUID = bot.getPlayerUUID();
        String botName = bot.getBotName();

        System.out.println("[BotSkinRenderer] getTextureLocation called for bot: " + botName + ", UUID: " + playerUUID);

        if (playerUUID == null) {
            System.out.println("[BotSkinRenderer] UUID is NULL, returning default Steve skin");
            return DEFAULT_STEVE_SKIN;
        }

        try {
            if (botName == null || botName.isEmpty()) {
                botName = "Bot";
            }

            GameProfile profile = new GameProfile(playerUUID, botName);
            Minecraft minecraft = Minecraft.getInstance();

            System.out.println("[BotSkinRenderer] Created GameProfile: " + profile.getName() + " (" + profile.getId() + ")");

            // Utiliser registerSkins pour charger le profil complet depuis Mojang
            // C'est la méthode que Minecraft utilise pour les vrais joueurs
            if (!SKIN_LOAD_INITIATED.containsKey(playerUUID)) {
                SKIN_LOAD_INITIATED.put(playerUUID, true);

                System.out.println("[BotSkinRenderer] Initiating skin download for " + botName + " via registerSkins()");

                // registerSkins télécharge automatiquement les textures depuis Mojang
                minecraft.getSkinManager().registerSkins(profile, (type, location, profileTexture) -> {
                    System.out.println("[BotSkinRenderer] CALLBACK: Skin loaded for " + botName + " - Type: " + type + ", Location: " + location);
                }, true);
            } else {
                System.out.println("[BotSkinRenderer] Skin already initiated for " + botName);
            }

            // Retourner la texture (sera Steve jusqu'à ce que le téléchargement soit terminé)
            ResourceLocation skinLoc = minecraft.getSkinManager().getInsecureSkinLocation(profile);
            System.out.println("[BotSkinRenderer] Returning skin location: " + skinLoc + " for " + botName);
            return skinLoc;

        } catch (Exception e) {
            System.err.println("[BotSkinRenderer] ERROR for " + botName + ": " + e.getMessage());
            e.printStackTrace();
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
