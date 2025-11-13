package com.aibrigade.client;

import com.aibrigade.bots.BotEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * BotRenderer - Basic renderer for bot entities
 *
 * Uses standard humanoid rendering with Steve player texture.
 * Bots will appear as basic humanoid entities with armor support.
 */
public class BotRenderer extends HumanoidMobRenderer<BotEntity, HumanoidModel<BotEntity>> {

    // Use Steve's default player texture from Minecraft
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public BotRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(BotEntity entity) {
        // Use default Steve texture for all bots
        // Can be customized per-bot based on entity.getBotSkin() in the future
        return TEXTURE;
    }
}
