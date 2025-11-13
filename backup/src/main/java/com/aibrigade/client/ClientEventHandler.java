package com.aibrigade.client;

import com.aibrigade.main.AIBrigadeMod;
import com.aibrigade.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ClientEventHandler - Handles client-side events for AIBrigade
 *
 * This class is only loaded on the client side and handles registration
 * of entity renderers and other client-specific functionality.
 */
@Mod.EventBusSubscriber(modid = AIBrigadeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {

    /**
     * Register entity renderers
     * Called during client setup to bind renderers to entity types
     *
     * @param event The entity renderers registration event
     */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        AIBrigadeMod.LOGGER.info("Registering entity renderers");

        // Register bot entity renderer with GeckoLib support
        event.registerEntityRenderer(ModEntities.BOT.get(), BotRenderer::new);

        AIBrigadeMod.LOGGER.info("Bot renderer registered successfully");
    }
}
