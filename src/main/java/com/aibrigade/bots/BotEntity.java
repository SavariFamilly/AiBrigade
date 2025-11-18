package com.aibrigade.bots;

import com.aibrigade.ai.RealisticFollowLeaderGoal;
import com.aibrigade.ai.ActiveGazeBehavior;
import com.aibrigade.ai.TeamAwareAttackGoal;
import com.aibrigade.ai.PlaceBlockToReachTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
// GeckoLib animations will be added when dependency is resolved
// import software.bernie.geckolib.animatable.GeoEntity;
// import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
// import software.bernie.geckolib.core.animation.AnimatableManager;
// import software.bernie.geckolib.core.animation.AnimationController;
// import software.bernie.geckolib.core.animation.AnimationState;
// import software.bernie.geckolib.core.animation.RawAnimation;
// import software.bernie.geckolib.core.object.PlayState;
// import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BotEntity - Represents an AI-controlled bot NPC with GeckoLib animations
 *
 * This entity class represents a single bot that can be controlled via commands
 * and AI behaviors. Each bot has:
 * - Unique name and skin
 * - Individual inventory and equipment
 * - AI state and behavior type
 * - Group assignment and leader tracking
 * - Statistics (health, attack damage, movement speed)
 * - Spawn position and home location
 * - GeckoLib animations (walk, run, attack, idle, etc.)
 *
 * Bots extend PathfinderMob to inherit advanced pathfinding capabilities.
 * GeckoLib animation support will be added when dependency is resolved.
 */
public class BotEntity extends PathfinderMob {

    // GeckoLib animation cache - will be restored when GeckoLib is available
    // private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Animation definitions - will be restored when GeckoLib is available
    // private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.bot.idle");
    // private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.bot.walk");
    // private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("animation.bot.run");
    // private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.bot.attack");
    // private static final RawAnimation JUMP_ANIM = RawAnimation.begin().thenPlay("animation.bot.jump");
    // private static final RawAnimation CLIMB_ANIM = RawAnimation.begin().thenLoop("animation.bot.climb");
    // private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("animation.bot.swim");
    // private static final RawAnimation DAMAGED_ANIM = RawAnimation.begin().thenPlay("animation.bot.damaged");
    // private static final RawAnimation SNEAK_ANIM = RawAnimation.begin().thenLoop("animation.bot.sneak");

    // Data accessors for synced entity data
    private static final EntityDataAccessor<String> BOT_NAME =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> BOT_SKIN =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> BOT_GROUP =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> BEHAVIOR_TYPE =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_STATIC =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> FOLLOW_RADIUS =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_HOSTILE =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FOLLOWING_LEADER =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<java.util.Optional<UUID>> PLAYER_UUID =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> CAN_PLACE_BLOCKS =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> SKIN_TEXTURE_VALUE =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_TEXTURE_SIGNATURE =
        SynchedEntityData.defineId(BotEntity.class, EntityDataSerializers.STRING);

    // Bot properties
    private UUID leaderId; // UUID of the leader (player or bot)
    private BlockPos spawnPosition;
    private BlockPos homePosition;
    private BotAIState aiState;
    private BotRole role;
    private long spawnTime;

    // Behavior configuration
    private BotBehaviorConfig behaviorConfig;

    // Equipment and inventory
    private ItemStack[] armorSlots = new ItemStack[4]; // Head, chest, legs, boots
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;

    /**
     * Constructor for BotEntity
     *
     * @param entityType The entity type
     * @param level The world/level
     */
    public BotEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.aiState = BotAIState.IDLE;
        this.role = BotRole.SOLDIER;
        this.spawnTime = System.currentTimeMillis();

        // Initialize behavior config with default soldier preset
        this.behaviorConfig = BotBehaviorConfig.createSoldier();

        // Initialize armor slots
        for (int i = 0; i < armorSlots.length; i++) {
            armorSlots[i] = ItemStack.EMPTY;
        }

        // Apply random Mojang skin and equipment
        if (!level.isClientSide) {
            // Appliquer un skin Mojang aléatoire avec vrai UUID
            MojangSkinFetcher.applyRandomFamousSkin(this);

            // Équiper un item complètement aléatoire (pioche, épée, blocs, nourriture, rien)
            RandomEquipment.equipRandomItem(this);
        }
    }

    /**
     * Define synced entity data
     * Minecraft 1.20.1 version
     */
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BOT_NAME, "Bot");
        this.entityData.define(BOT_SKIN, "default");
        this.entityData.define(BOT_GROUP, "none");
        this.entityData.define(BEHAVIOR_TYPE, "idle");
        this.entityData.define(IS_STATIC, false);
        this.entityData.define(FOLLOW_RADIUS, 10.0f);
        this.entityData.define(IS_HOSTILE, false);
        this.entityData.define(IS_FOLLOWING_LEADER, false);
        this.entityData.define(PLAYER_UUID, java.util.Optional.empty());
        this.entityData.define(CAN_PLACE_BLOCKS, true);
        this.entityData.define(SKIN_TEXTURE_VALUE, "");
        this.entityData.define(SKIN_TEXTURE_SIGNATURE, "");
    }

    /**
     * Create attribute supplier for bot entities
     * Defines base attributes like health, movement speed, attack damage, etc.
     *
     * @return AttributeSupplier with bot attributes
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.1D) // Vitesse identique au joueur
            .add(Attributes.ATTACK_DAMAGE, 3.0D)
            .add(Attributes.ARMOR, 2.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    /**
     * Register goals for this bot
     * Called after entity construction to setup AI behaviors
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Priorité 0: Float in water
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));

        // Priorité 1: Active gaze behavior (regard actif 2/6 bots)
        this.goalSelector.addGoal(1, new ActiveGazeBehavior(this));

        // Priorité 2: Realistic follow leader (avec probabilités et variations)
        RealisticFollowLeaderGoal followGoal = new RealisticFollowLeaderGoal(this, 1.1D, 3.0F, 10.0F);
        this.goalSelector.addGoal(2, followGoal);

        // Priorité 3: Place blocks to reach target (avec toggle canPlaceBlocks)
        this.goalSelector.addGoal(3, new PlaceBlockToReachTargetGoal(this));

        // Priorité 4: Melee attack
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2D, false));

        // Priorité 5: Wander when idle
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8D));

        // Priorité 6: Look at player (secondaire car ActiveGazeBehavior gère déjà)
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 8.0F));

        // Priorité 7: Random look around
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

        // Add TEAM-AWARE attack target selectors (won't attack teammates)
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, TeamAwareAttackGoal.forPlayer(this)); // Only attack players if hostile & not leader
        this.targetSelector.addGoal(3, TeamAwareAttackGoal.forBot(this)); // Only attack bots if hostile & not same team
        this.targetSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
            this, net.minecraft.world.entity.monster.Monster.class, true)); // Attack hostile mobs
    }

    // Getters and setters for bot properties

    /**
     * Get the bot's display name
     * @return The bot name
     */
    public String getBotName() {
        return this.entityData.get(BOT_NAME);
    }

    /**
     * Set the bot's display name
     * @param name The new name
     */
    public void setBotName(String name) {
        this.entityData.set(BOT_NAME, name);
    }

    /**
     * Get the bot's skin identifier
     * @return The skin name/path
     */
    public String getBotSkin() {
        return this.entityData.get(BOT_SKIN);
    }

    /**
     * Set the bot's skin
     * @param skin The skin name/path
     */
    public void setBotSkin(String skin) {
        this.entityData.set(BOT_SKIN, skin);
    }

    /**
     * Get the bot's group name
     * @return The group name
     */
    public String getBotGroup() {
        return this.entityData.get(BOT_GROUP);
    }

    /**
     * Set the bot's group
     * @param group The group name
     */
    public void setBotGroup(String group) {
        this.entityData.set(BOT_GROUP, group);
    }

    /**
     * Get the bot's behavior type
     * @return The behavior type (raid, patrol, guard, etc.)
     */
    public String getBehaviorType() {
        return this.entityData.get(BEHAVIOR_TYPE);
    }

    /**
     * Set the bot's behavior type
     * @param behavior The new behavior type
     */
    public void setBehaviorType(String behavior) {
        this.entityData.set(BEHAVIOR_TYPE, behavior);
    }

    /**
     * Check if bot is static (doesn't move)
     * @return true if static, false if mobile
     */
    public boolean isStatic() {
        return this.entityData.get(IS_STATIC);
    }

    /**
     * Set bot static state
     * @param isStatic true for static, false for mobile
     */
    public void setStatic(boolean isStatic) {
        this.entityData.set(IS_STATIC, isStatic);
    }

    /**
     * Get the follow radius around leader
     * @return The radius in blocks
     */
    public float getFollowRadius() {
        return this.entityData.get(FOLLOW_RADIUS);
    }

    /**
     * Set the follow radius
     * @param radius The radius in blocks
     */
    public void setFollowRadius(float radius) {
        this.entityData.set(FOLLOW_RADIUS, radius);
    }

    /**
     * Check if the bot is in hostile mode
     * @return true if hostile
     */
    public boolean isHostile() {
        return this.entityData.get(IS_HOSTILE);
    }

    /**
     * Set hostile mode
     * @param hostile true to enable hostile mode
     */
    public void setHostile(boolean hostile) {
        this.entityData.set(IS_HOSTILE, hostile);
    }

    /**
     * Check if the bot is following its leader
     * @return true if following leader
     */
    public boolean isFollowingLeader() {
        return this.entityData.get(IS_FOLLOWING_LEADER);
    }

    /**
     * Set follow leader mode
     * @param following true to enable following
     */
    public void setFollowingLeader(boolean following) {
        this.entityData.set(IS_FOLLOWING_LEADER, following);
    }

    /**
     * Get the group ID (same as bot group)
     * @return The group ID
     */
    public String getGroupId() {
        return getBotGroup();
    }

    /**
     * Get the leader's UUID
     * @return Leader UUID or null
     */
    @Nullable
    public UUID getLeaderId() {
        return leaderId;
    }

    /**
     * Set the leader
     * @param leaderId The leader's UUID
     */
    public void setLeaderId(@Nullable UUID leaderId) {
        this.leaderId = leaderId;
    }

    /**
     * Get the spawn position
     * @return The spawn position
     */
    public BlockPos getSpawnPosition() {
        return spawnPosition;
    }

    /**
     * Set the spawn position
     * @param pos The spawn position
     */
    public void setSpawnPosition(BlockPos pos) {
        this.spawnPosition = pos;
        this.homePosition = pos; // Home is initially spawn position
    }

    /**
     * Get the home position (for guard/patrol behaviors)
     * @return The home position
     */
    public BlockPos getHomePosition() {
        return homePosition;
    }

    /**
     * Set the home position
     * @param pos The home position
     */
    public void setHomePosition(BlockPos pos) {
        this.homePosition = pos;
    }

    /**
     * Get the AI state
     * @return The current AI state
     */
    public BotAIState getAIState() {
        return aiState;
    }

    /**
     * Set the AI state
     * @param state The new AI state
     */
    public void setAIState(BotAIState state) {
        this.aiState = state;
    }

    /**
     * Get the bot's role
     * @return The bot role
     */
    public BotRole getRole() {
        return role;
    }

    /**
     * Set the bot's role
     * @param role The new role
     */
    public void setRole(BotRole role) {
        this.role = role;
    }

    /**
     * Get spawn time (milliseconds since epoch)
     * @return The spawn time
     */
    public long getSpawnTime() {
        return spawnTime;
    }

    /**
     * Get the player UUID (for Mojang skin)
     * @return The player UUID, or null if not set
     */
    public UUID getPlayerUUID() {
        return this.entityData.get(PLAYER_UUID).orElse(null);
    }

    /**
     * Set the player UUID (for Mojang skin)
     * Synchronized across client and server
     * @param uuid The player UUID
     */
    public void setPlayerUUID(UUID uuid) {
        this.entityData.set(PLAYER_UUID, java.util.Optional.ofNullable(uuid));
    }

    /**
     * Get the skin texture value (Base64 encoded texture data)
     * @return The texture value, or empty string if not set
     */
    public String getSkinTextureValue() {
        return this.entityData.get(SKIN_TEXTURE_VALUE);
    }

    /**
     * Set the skin texture value (Base64 encoded texture data)
     * Synchronized across client and server
     * @param value The texture value
     */
    public void setSkinTextureValue(String value) {
        this.entityData.set(SKIN_TEXTURE_VALUE, value != null ? value : "");
    }

    /**
     * Get the skin texture signature
     * @return The texture signature, or empty string if not set
     */
    public String getSkinTextureSignature() {
        return this.entityData.get(SKIN_TEXTURE_SIGNATURE);
    }

    /**
     * Set the skin texture signature
     * Synchronized across client and server
     * @param signature The texture signature
     */
    public void setSkinTextureSignature(String signature) {
        this.entityData.set(SKIN_TEXTURE_SIGNATURE, signature != null ? signature : "");
    }

    /**
     * Check if the bot can place blocks
     * @return true if can place blocks
     */
    public boolean canPlaceBlocks() {
        return this.entityData.get(CAN_PLACE_BLOCKS);
    }

    /**
     * Set whether the bot can place blocks
     * Synchronized across client and server
     * @param canPlace true to allow block placement
     */
    public void setCanPlaceBlocks(boolean canPlace) {
        this.entityData.set(CAN_PLACE_BLOCKS, canPlace);
    }

    /**
     * Get the bot's behavior configuration
     * @return The behavior config
     */
    public BotBehaviorConfig getBehaviorConfig() {
        if (behaviorConfig == null) {
            behaviorConfig = BotBehaviorConfig.createSoldier();
        }
        return behaviorConfig;
    }

    /**
     * Set the bot's behavior configuration
     * @param config The new configuration
     */
    public void setBehaviorConfig(BotBehaviorConfig config) {
        this.behaviorConfig = config;
    }

    /**
     * Equip armor piece in specific slot
     *
     * @param slot The armor slot (0=helmet, 1=chestplate, 2=leggings, 3=boots)
     * @param item The armor item
     */
    public void setArmorSlot(int slot, ItemStack item) {
        if (slot >= 0 && slot < 4) {
            armorSlots[slot] = item;
            // Update visual equipment
            // Minecraft armor slots: FEET=0, LEGS=1, CHEST=2, HEAD=3
            // Our slots: 0=helmet, 1=chestplate, 2=leggings, 3=boots
            EquipmentSlot equipmentSlot;
            switch (slot) {
                case 0: equipmentSlot = EquipmentSlot.HEAD; break;
                case 1: equipmentSlot = EquipmentSlot.CHEST; break;
                case 2: equipmentSlot = EquipmentSlot.LEGS; break;
                case 3: equipmentSlot = EquipmentSlot.FEET; break;
                default: return;
            }
            this.setItemSlot(equipmentSlot, item);
        }
    }

    /**
     * Get armor in specific slot
     *
     * @param slot The armor slot
     * @return The armor item
     */
    public ItemStack getArmorSlot(int slot) {
        if (slot >= 0 && slot < 4) {
            return armorSlots[slot];
        }
        return ItemStack.EMPTY;
    }

    /**
     * Set main hand item
     * @param item The item to hold
     */
    public void setMainHandItem(ItemStack item) {
        this.mainHandItem = item;
        this.setItemSlot(EquipmentSlot.MAINHAND, item);
    }

    /**
     * Set off hand item
     * @param item The item to hold
     */
    public void setOffHandItem(ItemStack item) {
        this.offHandItem = item;
        this.setItemSlot(EquipmentSlot.OFFHAND, item);
    }

    /**
     * Save bot data to NBT
     */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putString("BotName", getBotName());
        tag.putString("BotSkin", getBotSkin());
        tag.putString("BotGroup", getBotGroup());
        tag.putString("BehaviorType", getBehaviorType());
        tag.putBoolean("IsStatic", isStatic());
        tag.putFloat("FollowRadius", getFollowRadius());

        if (leaderId != null) {
            tag.putUUID("LeaderId", leaderId);
        }

        if (spawnPosition != null) {
            tag.putLong("SpawnPos", spawnPosition.asLong());
        }

        if (homePosition != null) {
            tag.putLong("HomePos", homePosition.asLong());
        }

        tag.putString("AIState", aiState.name());
        tag.putString("Role", role.name());
        tag.putLong("SpawnTime", spawnTime);

        // Save player UUID for Mojang skin (from synced data)
        UUID playerUUID = getPlayerUUID();
        if (playerUUID != null) {
            tag.putUUID("PlayerUUID", playerUUID);
        }

        // Save skin textures (from synced data)
        tag.putString("SkinTextureValue", getSkinTextureValue());
        tag.putString("SkinTextureSignature", getSkinTextureSignature());

        // Save building toggle (from synced data)
        tag.putBoolean("CanPlaceBlocks", canPlaceBlocks());

        // Save behavior config
        if (behaviorConfig != null) {
            tag.put("BehaviorConfig", behaviorConfig.saveToNBT());
        }
    }

    /**
     * Load bot data from NBT
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        setBotName(tag.getString("BotName"));
        setBotSkin(tag.getString("BotSkin"));
        setBotGroup(tag.getString("BotGroup"));
        setBehaviorType(tag.getString("BehaviorType"));
        setStatic(tag.getBoolean("IsStatic"));
        setFollowRadius(tag.getFloat("FollowRadius"));

        if (tag.hasUUID("LeaderId")) {
            leaderId = tag.getUUID("LeaderId");
        }

        if (tag.contains("SpawnPos")) {
            spawnPosition = BlockPos.of(tag.getLong("SpawnPos"));
        }

        if (tag.contains("HomePos")) {
            homePosition = BlockPos.of(tag.getLong("HomePos"));
        }

        if (tag.contains("AIState")) {
            try {
                aiState = BotAIState.valueOf(tag.getString("AIState"));
            } catch (IllegalArgumentException e) {
                aiState = BotAIState.IDLE; // Default fallback for corrupted data
                com.aibrigade.main.AIBrigadeMod.LOGGER.warn("Invalid AIState in saved data, using IDLE: {}", e.getMessage());
            }
        }

        if (tag.contains("Role")) {
            try {
                role = BotRole.valueOf(tag.getString("Role"));
            } catch (IllegalArgumentException e) {
                role = BotRole.SOLDIER; // Default fallback for corrupted data
                com.aibrigade.main.AIBrigadeMod.LOGGER.warn("Invalid Role in saved data, using SOLDIER: {}", e.getMessage());
            }
        }

        spawnTime = tag.getLong("SpawnTime");

        // Load player UUID for Mojang skin (into synced data)
        if (tag.hasUUID("PlayerUUID")) {
            setPlayerUUID(tag.getUUID("PlayerUUID"));
        }

        // Load skin textures (into synced data)
        if (tag.contains("SkinTextureValue")) {
            setSkinTextureValue(tag.getString("SkinTextureValue"));
        }
        if (tag.contains("SkinTextureSignature")) {
            setSkinTextureSignature(tag.getString("SkinTextureSignature"));
        }

        // Load building toggle (into synced data)
        if (tag.contains("CanPlaceBlocks")) {
            setCanPlaceBlocks(tag.getBoolean("CanPlaceBlocks"));
        }

        // Load behavior config
        if (tag.contains("BehaviorConfig")) {
            if (behaviorConfig == null) {
                behaviorConfig = new BotBehaviorConfig();
            }
            behaviorConfig.loadFromNBT(tag.getCompound("BehaviorConfig"));
        }
    }

    /**
     * Check if the bot's name should always be visible
     * @return true to always show name tag
     */
    @Override
    public boolean hasCustomName() {
        return true;
    }

    /**
     * Get the custom name for the bot
     * @return The bot's display name
     */
    @Override
    public net.minecraft.network.chat.Component getCustomName() {
        return net.minecraft.network.chat.Component.literal(getBotName());
    }

    /**
     * Always show the name tag above the bot
     * @return true to always render name
     */
    @Override
    public boolean isCustomNameVisible() {
        return true;
    }

    /**
     * Prevent bots from despawning when player is far away
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // NEVER despawn bots
    }

    /**
     * Override to prevent bots from being affected by difficulty
     */
    @Override
    public boolean isPersistenceRequired() {
        return true; // Bots ALWAYS persist
    }

    /**
     * Custom tick method for bot-specific logic
     */
    @Override
    public void tick() {
        super.tick();

        // Bot-specific tick logic here
        // This is called every game tick (20 times per second)

        if (!this.level().isClientSide) {
            // Server-side only logic
            updateAIState();
        }
    }

    /**
     * Update AI state based on current conditions
     * Called every tick to determine bot behavior
     */
    private void updateAIState() {
        // Stub: AI state updates will be handled by AIManager
        // This method can be used for quick state checks and transitions

        // NOTE: We don't use setNoAi(true) for static bots because it disables gravity
        // Instead, the static check is handled in EntityValidator.isBotAIReady()
        // which prevents AI goals from running while keeping physics active

        // Static bots have AI disabled via EntityValidator but gravity still works
        // Non-static bots have normal AI and physics
    }

    // GeckoLib animation methods - will be restored when GeckoLib is available
    /*
    private PlayState predicate(AnimationState<BotEntity> state) {
        if (this.isInWater()) {
            state.getController().setAnimation(SWIM_ANIM);
            return PlayState.CONTINUE;
        }
        if (this.aiState == BotAIState.CLIMBING || this.onClimbable()) {
            state.getController().setAnimation(CLIMB_ANIM);
            return PlayState.CONTINUE;
        }
        if (this.aiState == BotAIState.ATTACKING || this.isAggressive()) {
            state.getController().setAnimation(ATTACK_ANIM);
            return PlayState.CONTINUE;
        }
        if (this.isShiftKeyDown()) {
            state.getController().setAnimation(SNEAK_ANIM);
            return PlayState.CONTINUE;
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D) {
            if (this.isSprinting()) {
                state.getController().setAnimation(RUN_ANIM);
            } else {
                state.getController().setAnimation(WALK_ANIM);
            }
            return PlayState.CONTINUE;
        }
        state.getController().setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    */

    /**
     * Called when the bot is removed from the world
     * Handles all cleanup: UUID release and BotManager cleanup
     * Note: die() is called BEFORE remove(), so we only need cleanup here
     */
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            // Release the player UUID for reuse
            UUID playerUUID = getPlayerUUID();
            if (playerUUID != null) {
                MojangSkinFetcher.releasePlayerUUID(playerUUID);
            }

            // Cleanup from BotManager (works for both death and manual removal)
            com.aibrigade.main.AIBrigadeMod.getBotManager().onBotRemoved(this);

            com.aibrigade.main.AIBrigadeMod.LOGGER.info("Bot {} removed and cleaned up", this.getBotName());
        }

        super.remove(reason);
    }

    /**
     * AI state enumeration
     * Represents different states the bot can be in
     */
    public enum BotAIState {
        IDLE,           // Standing still, no current task
        FOLLOWING,      // Following assigned leader
        ATTACKING,      // Engaging in combat
        PATROLLING,     // Patrolling assigned area
        GUARDING,       // Guarding a position
        FLEEING,        // Retreating from danger
        DISPERSING,     // Spreading out to avoid clustering
        CLIMBING        // Climbing obstacles
    }

    /**
     * Bot role enumeration
     * Determines bot's primary function and behavior tendencies
     */
    public enum BotRole {
        SOLDIER,        // Combat-focused, aggressive
        SCOUT,          // Fast, explores ahead
        GUARD,          // Defensive, protects area
        MEDIC,          // Support, helps other bots
        ENGINEER,       // Builds/breaks blocks
        LEADER          // Commands other bots
    }
}
