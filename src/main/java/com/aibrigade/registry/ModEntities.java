package com.aibrigade.registry;

import com.aibrigade.bots.BotEntity;
import com.aibrigade.main.AIBrigadeMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModEntities - Registry for all entities in AIBrigade
 *
 * This class handles registration of custom entities using Forge's deferred registration system.
 */
public class ModEntities {

    // Deferred register for entity types
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AIBrigadeMod.MOD_ID);

    /**
     * Bot entity type registration
     * Defines the entity type for bot NPCs with GeckoLib animation support
     */
    public static final RegistryObject<EntityType<BotEntity>> BOT =
            ENTITY_TYPES.register("bot",
                    () -> EntityType.Builder.of(BotEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(64)  // Same as players for better visibility
                            .updateInterval(3)        // PERFORMANCE FIX: Use 3 (vanilla default) instead of 1
                                                      // updateInterval(1) = 6000 packets/sec with 300 bots = CATASTROPHIC
                                                      // updateInterval(3) = 2000 packets/sec with 300 bots = acceptable
                                                      // Skin/data sync works perfectly fine with interval 3 via EntityDataAccessor
                            .build(AIBrigadeMod.MOD_ID + ":bot"));

    /**
     * Register all entities to the mod event bus
     *
     * @param eventBus The mod event bus
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        AIBrigadeMod.LOGGER.info("Entity types registered");
    }
}
