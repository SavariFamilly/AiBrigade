# AIBrigade

**AIBrigade** is a comprehensive Minecraft mod for Forge/NeoForge 1.20.1 that allows you to spawn and control up to 300+ bot NPCs with advanced AI behaviors. Bots can follow leaders, attack targets, patrol areas, climb obstacles, and execute complex group behaviors.

## Features

### Core Features
- **Spawn up to 300 bots simultaneously** with unique names, skins, and equipment
- **Advanced AI behaviors**: follow, patrol, raid, guard, and custom behaviors
- **Group management**: organize bots into groups with leaders and formations
- **Dynamic hostility system**: groups can become hostile to each other
- **Equipment system**: distribute armor and tools to bots with random variations
- **Realistic pathfinding**: bots navigate obstacles, climb blocks, and follow terrain
- **Smooth animations**: running, jumping, attacking, climbing (via GeckoLib/AnimationAPI)
- **Persistent data**: bot configurations saved between sessions
- **Performance optimized**: multithreaded AI processing

### Bot Capabilities
- Follow assigned leader (player or another bot)
- Maintain formation while following
- Attack hostile entities and other bot groups
- Patrol designated areas with waypoints
- Guard specific positions
- Climb obstacles and navigate complex terrain
- Disperse to avoid clustering
- Coordinate actions with group members

## Installation

### Requirements
- **Minecraft**: 1.20.1
- **Forge/NeoForge**: 47.3.0 or higher
- **Java**: 17 or higher

### Required Dependencies
None! AIBrigade works standalone.

### Optional Dependencies (Highly Recommended)
These mods enhance AIBrigade with animations, advanced AI, and pathfinding:

| Mod | Version | Purpose |
|-----|---------|---------|
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 4.4.0+ | Bot animations |
| [SmartBrainLib](https://www.curseforge.com/minecraft/mc-mods/smartbrainlib) | 1.15+ | Advanced AI behaviors |
| [Citadel](https://www.curseforge.com/minecraft/mc-mods/citadel) | 2.5.4+ | Entity framework enhancements |
| [Easy NPC](https://www.curseforge.com/minecraft/mc-mods/easy-npc) | 5.0+ | NPC customization integration |
| [Baritone](https://github.com/cabaletta/baritone) | Latest | Advanced pathfinding (experimental) |

### Installation Steps
1. Download and install Forge/NeoForge 1.20.1 (version 47.3.0 or higher)
2. Download AIBrigade mod JAR file
3. Place the JAR in your `mods/` folder
4. (Optional) Install recommended dependencies
5. Launch Minecraft
6. Check logs to confirm AIBrigade loaded successfully

## Building from Source

### Prerequisites
- JDK 17 or higher
- Git (optional)

### Build Steps
```bash
# Clone or download the repository
git clone https://github.com/yourusername/aibrigade.git
cd aibrigade

# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

The compiled JAR will be in `build/libs/aibrigade-1.0.0.jar`

### Development Environment Setup
```bash
# Generate IDE project files

# For Eclipse
gradlew.bat eclipse

# For IntelliJ IDEA
gradlew.bat genIntellijRuns

# Import project in your IDE
```

## Usage

### Basic Commands

All commands require operator permission (level 2).

#### Spawn Bots

**Spawn a single bot:**
```
/aibrigade spawn solo <leaderName> <behavior> <radius> <static> <groupName>
```

Example:
```
/aibrigade spawn solo Steve follow 10.0 false AlphaSquad
```

**Spawn a group of bots:**
```
/aibrigade spawn group <count> <leaderName> <behavior> <radius> <static> <groupName>
```

Example:
```
/aibrigade spawn group 50 Steve raid 15.0 false AlphaSquad
```

#### Manage Groups

**Assign a new leader:**
```
/aibrigade assignleader <groupName> <leaderName>
```

Example:
```
/aibrigade assignleader AlphaSquad Bob
```

**Make groups hostile:**
```
/aibrigade hostile <sourceGroup> <targetGroup>
```

Example:
```
/aibrigade hostile AlphaSquad BetaSquad
```

#### Equipment Management

**Give armor to bots:**
```
/aibrigade givearmor <target> <full|partial> <materials>
```

Examples:
```
# Full diamond armor for entire group
/aibrigade givearmor AlphaSquad full diamond

# Mixed iron/diamond armor (random per bot)
/aibrigade givearmor AlphaSquad partial irondiamond

# Partial chainmail/leather armor
/aibrigade givearmor BotName partial chainmailleather
```

**Armor Distribution Rules:**
- `full` with single material: all bots get complete matching set
- `partial` or multiple materials: each armor piece randomly chosen from materials
- Diversity rule: if 3 pieces match, 4th piece will be different (when possible)

#### Behavior Management

**Change bot behavior:**
```
/aibrigade setbehavior <target> <behavior>
```

Behaviors: `follow`, `patrol`, `raid`, `guard`, `idle`

Example:
```
/aibrigade setbehavior AlphaSquad patrol
```

**Set follow radius:**
```
/aibrigade setradius <groupName> <radius>
```

Example:
```
/aibrigade setradius AlphaSquad 20.0
```

**Toggle static state:**
```
/aibrigade togglestatic <target>
```

Example:
```
/aibrigade togglestatic AlphaSquad
```

#### Information Commands

**View group info:**
```
/aibrigade groupinfo <groupName>
```

**List all bots:**
```
/aibrigade listbots
```

**List all groups:**
```
/aibrigade listgroups
```

**Show help:**
```
/aibrigade help
```

#### Cleanup Commands

**Remove a bot:**
```
/aibrigade removebot <botName>
```

**Remove entire group:**
```
/aibrigade removegroup <groupName>
```

## Behaviors

### Follow
- Bots follow assigned leader within radius
- Maintain spacing to avoid clustering
- Mimic leader actions (jumping, climbing)
- Defend leader if attacked

### Patrol
- Bots patrol around home position
- Move between random waypoints
- Attack threats that come near
- Return to patrol after combat

### Raid
- Aggressive behavior
- Attack nearby entities and structures
- Break blocks if configured
- Collect loot from kills

### Guard
- Defensive behavior
- Stay near guard position
- Scan surroundings for threats
- Alert group when enemies approach

### Idle
- Default passive behavior
- Minimal movement
- Look around occasionally
- Low resource usage

## Configuration

### Config File Location
`config/aibrigade_config.json`

### Key Settings
```json
{
  "aiThreadPoolSize": 4,           // CPU threads for AI processing
  "maxBots": 300,                  // Maximum bot limit
  "defaultFollowRadius": 10.0,     // Default follow distance
  "defaultBehavior": "follow",     // Default spawn behavior
  "enableAnimations": true,        // Enable/disable animations
  "enableAdvancedPathfinding": true, // Use Baritone if available
  "aiUpdateInterval": 4,           // Ticks between AI updates
  "debugMode": false               // Enable debug logging
}
```

### Behavior Configuration
`src/main/resources/data/aibrigade/config/behavior_config.json`

Customize behavior parameters, combat settings, formations, and more.

## Architecture

### Project Structure
```
aibrigade/
├── src/main/java/com/aibrigade/
│   ├── main/
│   │   └── AIBrigadeMod.java          # Main mod class
│   ├── bots/
│   │   ├── BotEntity.java             # Bot entity definition
│   │   └── BotManager.java            # Bot lifecycle management
│   ├── ai/
│   │   ├── AIManager.java             # Global AI coordinator
│   │   ├── BotGoals.java              # AI goal implementations
│   │   └── SmartBrainIntegration.java # SmartBrainLib wrapper
│   ├── animations/
│   │   └── BotAnimationHandler.java   # Animation system
│   ├── commands/
│   │   └── BotCommandHandler.java     # Command implementations
│   └── utils/
│       ├── ConfigManager.java         # Configuration handling
│       ├── EntityLibWrapper.java      # Entity utilities
│       ├── PathfindingWrapper.java    # Pathfinding integration
│       └── AnimationUtils.java        # Animation helpers
└── src/main/resources/
    ├── META-INF/mods.toml             # Mod metadata
    ├── pack.mcmeta                    # Resource pack info
    ├── data/aibrigade/config/         # Behavior configs
    └── assets/aibrigade/
        ├── textures/bots/             # Bot skins
        └── animations/                # Animation files
```

### Key Systems

#### AI System
- **Multithreaded processing**: AI updates distributed across thread pool
- **Goal-based behaviors**: Minecraft goal system + SmartBrainLib integration
- **State machine**: Bot AI states (idle, following, attacking, etc.)
- **Group coordination**: Bots communicate and coordinate actions

#### Animation System
- **Library abstraction**: Supports GeckoLib and AnimationAPI
- **State synchronization**: Animations sync with AI states
- **Smooth transitions**: Blending between animations
- **Performance optimized**: Animations only update when needed

#### Pathfinding System
- **Multiple backends**: Vanilla, Baritone, or custom pathfinding
- **Obstacle detection**: Climb, jump, or navigate around obstacles
- **Formation pathfinding**: Groups maintain formations while moving
- **Dynamic recalculation**: Paths update when blocked

## Performance Considerations

### Bot Limits
- **Recommended**: 50-100 bots for smooth gameplay
- **Maximum**: 300 bots (may impact performance)
- **Optimization**: Adjust `aiUpdateInterval` in config (higher = less frequent updates)

### CPU Usage
- AI processing uses thread pool (default 4 threads)
- Increase threads on powerful CPUs: `aiThreadPoolSize` in config
- Reduce threads on weaker CPUs to avoid lag

### Memory Usage
- Each bot uses ~1-2 MB RAM
- 300 bots ≈ 300-600 MB additional RAM
- Ensure adequate JVM heap size (`-Xmx` argument)

## Troubleshooting

### Bots won't spawn
- Check you have permission (OP level 2)
- Verify you haven't reached bot limit (300)
- Ensure spawn position is valid (solid ground, clear space)
- Check server logs for errors

### Bots don't move/follow
- Verify behavior is not `idle`
- Check `static` is set to `false`
- Ensure leader exists and is alive
- Verify follow radius is appropriate

### Animations not working
- Install GeckoLib or AnimationAPI
- Check `enableAnimations` in config
- Verify animation files are present
- Check logs for animation library detection

### Performance issues
- Reduce bot count
- Increase `aiUpdateInterval` in config
- Reduce `aiThreadPoolSize` if CPU usage is high
- Disable animations: `enableAnimations: false`

### Bots attacking each other
- Check hostility settings between groups
- Use `/aibrigade groupinfo` to verify group membership
- Ensure bots aren't assigned to hostile groups

## Development

### Adding Custom Behaviors

1. Create behavior class in `com.aibrigade.ai.BotGoals`
2. Extend `Goal` class and implement logic
3. Register in `AIManager.executeBehavior()`
4. Add configuration in `behavior_config.json`

Example:
```java
public static class CustomBehaviorGoal extends Goal {
    private final BotEntity bot;

    public CustomBehaviorGoal(BotEntity bot) {
        this.bot = bot;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return bot.getBehaviorType().equals("custom");
    }

    @Override
    public void tick() {
        // Your behavior logic here
    }
}
```

### Adding Bot Skins

1. Create 64x64 PNG skin file (Minecraft player skin format)
2. Place in `src/main/resources/assets/aibrigade/textures/bots/`
3. Name format: `bot_<type>_<number>.png`
4. Mod will automatically detect and use

### Creating Animations

**For GeckoLib:**
1. Create model in Blockbench with GeckoLib plugin
2. Export `.geo.json` and `.animation.json`
3. Place in `assets/aibrigade/animations/`
4. Reference in `BotAnimationHandler`

**For AnimationAPI:**
1. Create animation JSON following AnimationAPI format
2. Place in `assets/aibrigade/animations/`
3. Register in `BotAnimationHandler.registerAnimationAPIAnimations()`

## API Usage

AIBrigade can be used as a library in other mods:

```java
// Get bot manager
BotManager botManager = AIBrigadeMod.getBotManager();

// Spawn bot programmatically
BotEntity bot = botManager.spawnBot(
    level,
    pos,
    "leaderName",
    "follow",
    10.0f,
    false,
    "groupName"
);

// Change bot behavior
AIManager aiManager = AIBrigadeMod.getAIManager();
aiManager.applyGroupBehavior("groupName", "raid");
```

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes with clear commits
4. Add/update documentation
5. Test thoroughly
6. Submit a pull request

### Code Style
- Follow existing code style
- Add JavaDoc comments to public methods
- Keep methods focused and readable
- Use meaningful variable names

## License

MIT License - See LICENSE file for details

## Credits

- **Created with**: Claude Code
- **Dependencies**: GeckoLib, SmartBrainLib, Citadel, Forge/NeoForge
- **Inspired by**: NPC mods, bot frameworks, AI testing tools

## Support

- **Issues**: Report bugs on [GitHub Issues](https://github.com/yourusername/aibrigade/issues)
- **Wiki**: Documentation at [GitHub Wiki](https://github.com/yourusername/aibrigade/wiki)
- **Discord**: Join our community (link TBD)

## Changelog

### Version 1.0.0 (Initial Release)
- Initial mod structure and framework
- Bot spawning and management system
- Basic AI behaviors (follow, patrol, raid, guard)
- Group management and hostility system
- Equipment distribution system
- Command system
- Configuration system
- Animation framework (GeckoLib/AnimationAPI)
- Pathfinding integration
- Performance optimizations
- Full documentation

## Roadmap

### Planned Features
- [ ] GUI for bot management
- [ ] Custom AI behavior scripting
- [ ] More formation types
- [ ] Ranged combat support
- [ ] Bot inventory management
- [ ] Bot leveling/XP system
- [ ] Waypoint editor
- [ ] Bot roles and classes
- [ ] Team colors and uniforms
- [ ] Voice commands integration
- [ ] Multiplayer synchronization improvements

## FAQ

**Q: Can I use this on a server?**
A: Yes! AIBrigade is fully compatible with dedicated servers.

**Q: Do bots persist through server restarts?**
A: Yes, bot data is saved and restored automatically.

**Q: Can bots use items and tools?**
A: Partially implemented. Full inventory management coming in future update.

**Q: How do I make bots build structures?**
A: Not yet implemented. Planned for future version with block placing.

**Q: Can bots fight each other?**
A: Yes! Use `/aibrigade hostile` to make groups fight.

**Q: Do bots respawn when killed?**
A: Not by default. Set `respawnOnDeath: true` in behavior config.

**Q: Can I control individual bot actions?**
A: Group-level commands are supported. Individual control coming in future update.

---

**Enjoy AIBrigade! Create your bot army today!**
#   A i B r i g a d e  
 