package com.aibrigade.client;

import com.aibrigade.main.AIBrigadeMod;
import com.aibrigade.bots.BotEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * BotModel - GeckoLib model for bot entities
 *
 * This class defines the 3D model, textures, and animations for bot entities.
 * GeckoLib uses JSON model files created with Blockbench.
 */
public class BotModel extends GeoModel<BotEntity> {

    /**
     * Get the model resource location
     * Points to the .geo.json file created in Blockbench
     *
     * @param object The bot entity
     * @return The model resource location
     */
    @Override
    public ResourceLocation getModelResource(BotEntity object) {
        return new ResourceLocation(AIBrigadeMod.MODID, "geo/bot.geo.json");
    }

    /**
     * Get the texture resource location
     * Points to the texture PNG file
     *
     * @param object The bot entity
     * @return The texture resource location
     */
    @Override
    public ResourceLocation getTextureResource(BotEntity object) {
        String skinName = object.getBotSkin();

        // If skin is "default" or null, use the default bot texture
        if (skinName == null || skinName.isEmpty() || skinName.equals("default")) {
            return new ResourceLocation(AIBrigadeMod.MODID, "textures/entity/bot_default.png");
        }

        // Otherwise, use the specified skin
        return new ResourceLocation(AIBrigadeMod.MODID, "textures/entity/bot_" + skinName + ".png");
    }

    /**
     * Get the animation resource location
     * Points to the .animation.json file created in Blockbench
     *
     * @param animatable The bot entity
     * @return The animation resource location
     */
    @Override
    public ResourceLocation getAnimationResource(BotEntity animatable) {
        return new ResourceLocation(AIBrigadeMod.MODID, "animations/bot.animation.json");
    }
}
