# Changelog

All notable changes to AIBrigade will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Individual bot management system**
  - `/aibrigade kill <botName>` - Kill specific bot by name
  - `/aibrigade modify <bot> name <name>` - Rename bot and fetch Mojang skin
  - `/aibrigade modify <bot> hand <item>` - Set main hand item
  - `/aibrigade modify <bot> offhand <item>` - Set off-hand item
  - `/aibrigade modify <bot> armor <slot> <item>` - Set armor piece (head/chest/legs/feet)
- **Unique bot names system**
  - Auto-rename on collision (appends _1, _2, etc.)
  - Prevents duplicate names across all bots
  - Name validation when spawning and renaming
- **Mojang skin system** ⭐ Major Feature
  - Bots automatically receive skins from real Minecraft players
  - 30+ famous players (Technoblade, Dream, Notch, Philza, etc.)
  - Async loading via Mojang API using CompletableFuture
  - Change skin by changing bot name with `/modify name`
  - Client-side rendering with BotPlayerSkinRenderer
  - UUID tracking for accurate skin rendering
- **Jump system**
  - Random jumps at 2-30 minute intervals (default)
  - Forced continuous jumping via `/aibrigade togglejump`
  - Per-bot or per-group control
  - Static bots excluded from jumping
- **Block placement system** ⭐ Advanced Feature
  - PlaceBlockToReachTargetGoal enables bridge/tower building
  - Automatic bridge construction over gaps
  - Vertical towers (pillar jumping)
  - Diagonal stairs for climbing
  - Escape route building when trapped
  - Toggle with canPlaceBlocks flag per bot
- **Info commands**
  - `/aibrigade listbots` - Show active bot count
  - `/aibrigade listgroups` - List all bot groups
  - `/aibrigade groupinfo <group>` - Detailed group information
  - `/aibrigade help` - Complete command reference
  - `/aibrigade cleanupbots` - Manual cleanup of dead bots
- **Advanced follow system**
  - `/aibrigade followleader <group> <enabled> <radius>` - Enable follow with radius
  - RADIUS_BASED mode (5/6 bots): Follow until within radius, then stop
  - ACTIVE_FOLLOW mode (1/6 bots): Follow very closely (3 blocks minimum)
  - Probabilistic behavior assignment for realistic group dynamics
- **Static bot defense**
  - Static bots can now attack hostile mobs within 16 block range
  - StaticBotDefenseGoal allows movement only for combat
  - Automatic targeting of monsters while staying in position
- **Hostility commands**
  - `/aibrigade sethostiletogroup <groupName>` - Player becomes hostile to group
  - Individual bot hostile response when attacked
- **Bot persistence system** ⭐ Major Feature
  - Bots and groups automatically saved on server shutdown
  - Bots automatically restored on server startup
  - Preserves all bot properties: position, behavior, equipment, skins, etc.
  - Preserves group configurations and relationships
  - Saves to `world/aibrigade/bots.json`
  - Multi-dimension support (Overworld, Nether, End)
  - Graceful error handling for corrupted data
  - Automatic cleanup of invalid entries

### Fixed
- **Critical:** Static bots now attack hostile mobs (commit 9ac729f)
  - Removed `setNoAi(true)` that was disabling all AI including combat
  - Static bots can now use StaticBotDefenseGoal properly
  - Target selector goals now work for static bots
- **Critical:** Static bots now spawn on ground (commit 9ac729f)
  - Added `findGroundBelow()` helper in BotManager
  - Searches down up to 256 blocks to find solid ground
  - Prevents static bots from floating in mid-air when spawned
  - Static bots spawn ON TOP of solid blocks, not inside them
- **Critical:** Follow system now works correctly (commit 9ac729f)
  - 5/6 bots (RADIUS_BASED) actively follow until within radius
  - 1/6 bots (ACTIVE_FOLLOW) follow very closely until 3 blocks
  - Simplified `canUse()` and `canContinueToUse()` logic
  - Removed complex `chaseChance` logic that was preventing movement
  - Static bots now correctly look at leader without moving
- **Critical:** Mojang skins now synchronize to clients (commit 9ac729f)
  - EntityData modifications now execute on server thread
  - Uses `bot.level().getServer().execute()` for thread-safe updates
  - Fixes skin not displaying issue on multiplayer servers
- **Movement goals respect static status**
  - Static bots no longer wander (WaterAvoidingRandomStrollGoal check)
  - Static bots no longer jump randomly (RandomJumpGoal check)
  - Static bots no longer place blocks (PlaceBlockToReachTargetGoal check)
  - Movement is prevented at goal level, not by disabling all AI
- **Missing imports fixed**
  - Added ItemStack, Items, BotEntity imports to BotCommandHandler
  - Fixed compilation errors in command handlers

### Changed
- **Follow behavior is now probabilistic**
  - RealisticFollowLeaderGoal assigns random behavior type on creation
  - 1/6 probability for ACTIVE_FOLLOW (close following)
  - 5/6 probability for RADIUS_BASED (radius-based following)
  - Makes group movement more realistic and varied
- **Static bots can now use AI goals**
  - Static status no longer calls `setNoAi(true)`
  - Individual goals check `isStatic()` and behave accordingly
  - Allows static bots to attack while staying stationary
- **Random equipment system applies on spawn**
  - Bots receive random tools, food, blocks, or empty hands (40% probability)
  - Uses RandomEquipment class for varied starting gear
  - More realistic and diverse bot appearances

### Documentation
- Updated README.md with all new commands and features
- Added Mojang Skin System section
- Added Block Placement System section
- Clarified Follow Behavior (1/6 vs 5/6 modes)
- Added Individual Bot Management section
- Added Jump System section
- Corrected Behaviors section (noted patrol/raid/guard coming soon)
- Added comprehensive examples in EXAMPLES.md
- Created AUDIT_REPORT.md with code-documentation analysis
- Created ACTION_PLAN.md with implementation roadmap

## [1.0.0] - 2024-11-13 - Initial Release

### Added
- **Bot spawning system**
  - Solo and group spawning
  - Configurable spawn positions
  - Leader assignment (player or bot)
  - Group organization
- **Group management**
  - Multiple concurrent groups
  - Leader tracking by UUID
  - Follow radius configuration
  - Static/mobile bot modes
- **Basic AI behaviors**
  - Follow leader within radius
  - Idle wandering
  - Basic combat (MeleeAttackGoal)
  - Float in water
  - Look at players
- **Equipment distribution system**
  - Armor management with randomization
  - Full or partial armor sets
  - Mixed materials support (diamond/iron/chainmail/leather/gold/netherite)
  - Diversity rule for varied appearance
- **Hostility system**
  - Group vs group hostility
  - Team-aware combat (no friendly fire)
  - Individual hostile response when attacked
  - Relationship tracking
- **Commands system** (15+ commands)
  - Spawn commands (solo/group)
  - Leader assignment
  - Behavior management
  - Equipment commands
  - Group operations
  - Hostility management
- **AI Goal system**
  - RealisticFollowLeaderGoal - Advanced following with variations
  - ActiveGazeBehavior - Realistic gaze behavior (2/6 bots)
  - TeamAwareAttackGoal - Combat without friendly fire
  - StaticBotDefenseGoal - Static bot defense against mobs
  - PlaceBlockToReachTargetGoal - Block placement for navigation
  - RandomJumpGoal - Random and forced jumping
- **Bot persistence structure** (framework in place, not yet functional)
  - PersistenceManager class
  - Save/load methods defined (stubs)
  - JSON serialization planned
- **Thread pool for AI processing** (available for future use)
  - Configurable thread pool size
  - Cleanup system every 5 seconds
  - Dead bot removal
- **Configuration system**
  - ConfigManager class
  - JSON configuration support
  - Runtime configuration loading
- **Client rendering**
  - BotPlayerSkinRenderer for Mojang skins
  - Skin texture caching
  - Proper player model rendering

### Known Issues
- **Animations not implemented** - All GeckoLib code is commented out waiting for dependencies
- **Patrol/Raid/Guard behaviors** - Not yet implemented as specific goals, use basic follow/idle
- **Multithreading AI** - Thread pool exists but not actively used for AI processing
- **Formation pathfinding** - Planned but not yet implemented

### Technical Details
- **Minecraft Version:** 1.20.1
- **Forge Version:** 47.3.0+
- **Java Version:** 17+
- **Max Bots:** 300 concurrent
- **Goal Priorities:** 0-9 (0=highest)
- **Cleanup Interval:** 100 ticks (5 seconds)
- **Thread Pool:** 4 threads (configurable)

---

## Future Plans

### Version 1.1.0 (Planned)
- [ ] Implement Patrol AI goal
- [ ] Implement Raid AI goal
- [ ] Implement Guard AI goal
- [x] Functional persistence (save/load bots) - ✅ COMPLETED
- [ ] GeckoLib animations integration
- [ ] Formation movement system
- [ ] Waypoint editor

### Version 1.2.0 (Planned)
- [ ] GUI for bot management
- [ ] Bot inventory system
- [ ] Ranged combat support
- [ ] Bot leveling/XP system
- [ ] Custom AI scripting
- [ ] Voice commands integration

### Version 2.0.0 (Future)
- [ ] Multiplayer synchronization improvements
- [ ] Advanced AI behaviors
- [ ] Structure building commands
- [ ] Bot roles and classes
- [ ] Team colors and uniforms
- [ ] Economy integration

---

**Note:** This changelog is maintained manually. Please report any missing or incorrect information.
