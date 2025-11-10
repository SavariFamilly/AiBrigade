# AIBrigade Development Guide

This document provides detailed information for developers who want to contribute to or extend AIBrigade.

## Development Environment Setup

### Prerequisites
- **JDK 17** or higher ([Download](https://adoptium.net/))
- **Git** ([Download](https://git-scm.com/))
- **IDE**: IntelliJ IDEA (recommended) or Eclipse

### Initial Setup

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/aibrigade.git
cd aibrigade
```

2. **Generate IDE files**

For IntelliJ IDEA:
```bash
./gradlew genIntellijRuns
```

For Eclipse:
```bash
./gradlew eclipse
```

3. **Import project into IDE**
- IntelliJ: Open the project folder
- Eclipse: Import as Existing Gradle Project

4. **Build the project**
```bash
./gradlew build
```

5. **Run in development**
```bash
# Run Minecraft client
./gradlew runClient

# Run dedicated server
./gradlew runServer
```

## Project Structure Deep Dive

### Package Organization

```
com.aibrigade
├── main/                   # Core mod initialization
├── bots/                   # Bot entity and management
├── ai/                     # AI systems and behaviors
├── animations/             # Animation handlers
├── commands/               # Command implementations
└── utils/                  # Utility classes and helpers
```

### Key Classes

#### AIBrigadeMod.java
- Entry point for the mod
- Initializes all managers and systems
- Handles lifecycle events
- Registers commands and event handlers

#### BotEntity.java
- Extends `PathfinderMob`
- Represents a single bot
- Manages bot state, equipment, and properties
- Handles NBT serialization

#### BotManager.java
- Singleton managing all bots
- Spawning and despawning logic
- Group management
- Equipment distribution
- Persistence

#### AIManager.java
- Multithreaded AI processing
- Behavior execution
- State transitions
- Group coordination

#### BotGoals.java
- Custom AI goals extending Minecraft's `Goal` class
- Implementations for follow, attack, patrol, etc.
- Goal priority and interruption handling

## Implementing Custom Features

### Adding a New Behavior

1. **Create Goal Class** in `BotGoals.java`:

```java
public static class MyCustomGoal extends Goal {
    private final BotEntity bot;

    public MyCustomGoal(BotEntity bot) {
        this.bot = bot;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return bot.getBehaviorType().equals("mycustom");
    }

    @Override
    public void tick() {
        // Your custom behavior logic
    }
}
```

2. **Add to AIManager** in `executeBehavior()`:

```java
case CUSTOM:
    executeCustomBehavior(bot);
    break;
```

3. **Implement execution method**:

```java
private void executeCustomBehavior(BotEntity bot) {
    // Custom behavior logic
}
```

4. **Add configuration** in `behavior_config.json`:

```json
"mycustom": {
    "description": "My custom behavior",
    "defaultRadius": 15.0,
    "priority": 7,
    "allowedActions": ["move", "jump"],
    "updateInterval": 10
}
```

### Adding a New Command

1. **Add command registration** in `BotCommandHandler.java`:

```java
.then(Commands.literal("mycommand")
    .then(Commands.argument("param", StringArgumentType.string())
        .executes(BotCommandHandler::myCommand)))
```

2. **Implement command method**:

```java
private static int myCommand(CommandContext<CommandSourceStack> context) {
    String param = StringArgumentType.getString(context, "param");

    // Command logic here

    context.getSource().sendSuccess(() ->
        Component.literal("Command executed!"),
        true);

    return 1;
}
```

### Adding Bot Roles

1. **Add role to enum** in `BotEntity.java`:

```java
public enum BotRole {
    SOLDIER,
    SCOUT,
    GUARD,
    MEDIC,
    ENGINEER,
    SNIPER    // New role
}
```

2. **Implement role-specific behavior**:

```java
if (bot.getRole() == BotRole.SNIPER) {
    // Sniper-specific logic
}
```

### Creating Custom Animations

1. **Design model** in Blockbench with GeckoLib plugin

2. **Export files**:
   - `bot_model.geo.json` - Model geometry
   - `bot_animations.animation.json` - Animation definitions

3. **Place in resources**:
   - `src/main/resources/assets/aibrigade/animations/`

4. **Register animation** in `BotAnimationHandler.java`:

```java
public static final String ANIM_CUSTOM = "custom_animation";

// In updateAnimationForState()
case CUSTOM_STATE:
    return ANIM_CUSTOM;
```

## Testing

### Unit Testing

Create tests in `src/test/java/`:

```java
public class BotManagerTest {
    @Test
    public void testBotSpawning() {
        // Test bot spawning logic
    }
}
```

Run tests:
```bash
./gradlew test
```

### In-Game Testing

1. Start test client:
```bash
./gradlew runClient
```

2. Create test world (Creative mode recommended)

3. Get OP permissions:
```
/op YourUsername
```

4. Test commands:
```
/aibrigade spawn group 10 @s follow 10 false TestGroup
/aibrigade givearmor TestGroup full diamond
/aibrigade setbehavior TestGroup raid
```

### Performance Testing

1. **Enable debug mode** in `config/aibrigade_config.json`:
```json
"debugMode": true
```

2. **Monitor performance**:
   - Check TPS (ticks per second)
   - Monitor CPU usage
   - Check memory usage (F3 screen)

3. **Stress test**:
```
/aibrigade spawn group 300 @s follow 10 false StressTest
```

## Code Style Guidelines

### Java Conventions

- **Naming**:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Variables: `camelCase`

- **Formatting**:
  - Indent: 4 spaces
  - Line length: 100-120 characters
  - Braces: on same line for methods and classes

- **Comments**:
  - JavaDoc for all public methods and classes
  - Inline comments for complex logic
  - TODO comments for future improvements

### Example:

```java
/**
 * Calculate optimal spawn position for bot
 *
 * @param origin Base spawn position
 * @param index Bot index in group
 * @param total Total bots in group
 * @return Calculated spawn position
 */
private BlockPos calculateSpawnPosition(BlockPos origin, int index, int total) {
    // Use circular distribution for even spacing
    double angle = (2.0 * Math.PI * index) / total;
    // ...
}
```

## Debugging

### Logging

Use SLF4J logger:

```java
AIBrigadeMod.LOGGER.info("Normal information");
AIBrigadeMod.LOGGER.warn("Warning message");
AIBrigadeMod.LOGGER.error("Error message", exception);
AIBrigadeMod.LOGGER.debug("Debug information (only if debug enabled)");
```

### Breakpoints

1. Set breakpoints in your IDE
2. Run in debug mode: `./gradlew runClient --debug-jvm`
3. Attach debugger to port 5005

### Common Issues

**Issue**: Bots not spawning
- Check: Entity registration in `ModEntities` class
- Verify: Spawn conditions in `BotManager.spawnBot()`

**Issue**: AI not updating
- Check: AIManager initialization in `AIBrigadeMod.setup()`
- Verify: Thread pool is running

**Issue**: Animations not playing
- Check: GeckoLib/AnimationAPI is installed
- Verify: Animation files are present in resources
- Check: `isAnimationSupported()` returns true

## Performance Optimization

### AI Performance

1. **Batch updates**: Process multiple bots per tick
2. **Update intervals**: Not every bot needs to update every tick
3. **Spatial partitioning**: Group nearby bots for efficient queries
4. **Goal caching**: Cache pathfinding results

### Memory Management

1. **Weak references**: Use for large data structures
2. **Object pooling**: Reuse objects instead of creating new ones
3. **Lazy loading**: Load data only when needed
4. **Cleanup**: Remove references when bots are despawned

### Network Optimization

1. **Packet batching**: Send multiple updates in one packet
2. **Delta compression**: Only send changed data
3. **Update frequency**: Reduce network updates when possible

## Build and Release

### Building Release JAR

```bash
# Clean previous builds
./gradlew clean

# Build release JAR
./gradlew build

# Output: build/libs/aibrigade-1.0.0.jar
```

### Version Numbering

Follow Semantic Versioning: `MAJOR.MINOR.PATCH`

- **MAJOR**: Incompatible API changes
- **MINOR**: New features, backwards compatible
- **PATCH**: Bug fixes, backwards compatible

Update version in:
- `build.gradle` - `version = '1.0.0'`
- `mods.toml` - `version = "1.0.0"`

### Release Checklist

- [ ] Update version numbers
- [ ] Update changelog in README.md
- [ ] Test all features
- [ ] Run on clean Minecraft installation
- [ ] Test with optional dependencies
- [ ] Build release JAR
- [ ] Create GitHub release
- [ ] Upload to CurseForge/Modrinth
- [ ] Update documentation

## Contributing

### Pull Request Process

1. **Fork** the repository
2. **Create branch**: `git checkout -b feature/my-feature`
3. **Make changes** with clear, focused commits
4. **Test thoroughly**
5. **Update documentation**
6. **Push** to your fork
7. **Create Pull Request** with description

### Commit Messages

Format: `type: description`

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `perf`: Performance improvements

Examples:
```
feat: add sniper bot role
fix: resolve bot pathfinding crash
docs: update API usage examples
```

### Code Review

All PRs must:
- Pass CI checks
- Have no merge conflicts
- Include tests for new features
- Update relevant documentation
- Follow code style guidelines

## Resources

### Minecraft Modding
- [Forge Documentation](https://docs.minecraftforge.net/)
- [Minecraft Wiki](https://minecraft.fandom.com/wiki/Minecraft_Wiki)
- [Forge Forums](https://forums.minecraftforge.net/)

### Dependencies
- [GeckoLib Docs](https://geckolib.com/)
- [SmartBrainLib](https://github.com/tslat/SmartBrainLib)
- [Citadel](https://github.com/Alex-the-666/Citadel)

### Tools
- [Blockbench](https://www.blockbench.net/) - 3D modeling
- [MCreator](https://mcreator.net/) - Mod development tool
- [Minecraft Dev Plugin](https://plugins.jetbrains.com/plugin/8327-minecraft-development) - IntelliJ plugin

## Support

Need help? Contact us:
- GitHub Issues
- Discord (link TBD)
- Email: aibrigade@example.com

---

Happy coding! 🤖
