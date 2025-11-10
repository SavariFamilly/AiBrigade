package com.aibrigade.client;

import com.aibrigade.main.AIBrigadeMod;
import com.aibrigade.bots.BotEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * BotRenderer - GeckoLib renderer for bot entities
 *
 * This renderer handles the visual representation of bot entities using GeckoLib.
 * It uses the BotModel for the 3D model and applies textures based on the bot's skin.
 */
public class BotRenderer extends GeoEntityRenderer<BotEntity> {

    /**
     * Constructor for BotRenderer
     *
     * @param renderManager The entity renderer provider
     */
    public BotRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BotModel());
        this.shadowRadius = 0.5F; // Shadow size under the bot
    }

    /**
     * Get the texture location for this bot
     * Allows different bots to have different skins
     *
     * @param entity The bot entity
     * @return The texture resource location
     */
    @Override
    public ResourceLocation getTextureLocation(BotEntity entity) {
        String skinName = entity.getBotSkin();

        // If skin is "default" or null, use the default bot texture
        if (skinName == null || skinName.isEmpty() || skinName.equals("default")) {
            return new ResourceLocation(AIBrigadeMod.MODID, "textures/entity/bot_default.png");
        }

        // Otherwise, use the specified skin
        return new ResourceLocation(AIBrigadeMod.MODID, "textures/entity/bot_" + skinName + ".png");
    }
}
