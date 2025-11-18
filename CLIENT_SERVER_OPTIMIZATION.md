# Client-Server Optimization Architecture

AIBrigade uses a **server-authoritative** architecture where the client does minimal work.

## Architecture Principles

### ✅ SERVER HANDLES (Authoritative)
- **All AI logic** - pathfinding, goal selection, behavior
- **All game logic** - combat, item usage, interactions
- **All calculations** - distance checks, state updates
- **Performance optimization** - tick rate management, pathfinding control
- **Skin fetching** - Mojang API calls
- **Entity data synchronization** - automatic sync to clients

### ✅ CLIENT HANDLES (Minimal)
- **Rendering only** - display bots on screen
- **Simple distance checks** - for render culling
- **Skin display** - using textures synced from server
- **Animation playback** - using data from server

## Performance Benefits

### Client Load Reduction
- **No AI calculations** on client
- **No pathfinding** on client
- **No distance calculations** for game logic
- **No Mojang API calls** on client
- **Minimal LOD checks** for rendering only

### Result
With 200+ bots:
- **Server**: Handles all logic efficiently with distance-based tick rates
- **Client**: Only renders nearby bots, minimal CPU usage
- **Network**: Optimized sync - distant bots sync less frequently

## File Organization

### Server-Side Files
- `BotEntity.java` - All logic protected by `!level().isClientSide`
- `BotPerformanceOptimizer.java` - Server-side optimization (AI, pathfinding)
- `BotManager.java` - Server-side bot management
- `MojangSkinFetcher.java` - Server-side skin API calls
- All AI goals and behaviors

### Client-Side Files
- `BotPlayerSkinRenderer.java` - Minimal rendering with simple culling
- `BotClientOptimizer.java` - Simple distance checks for rendering
- No AI, no game logic

### Shared (Synced)
- `EntityDataAccessor` fields in BotEntity - auto-synced by Minecraft
- Skin texture data - fetched server-side, synced to clients

## Data Synchronization

### What Gets Synced
- Position, rotation (automatic by Minecraft)
- EntityDataAccessor fields:
  - Bot name
  - Skin textures (value + signature)
  - UUID
  - Equipment/armor
  - Static state
- Animation states (automatic)

### Sync Frequency (Distance-Based)
- **Close bots** (0-16m): Every tick
- **Medium bots** (16-32m): Every 2 ticks
- **Far bots** (32-64m): Every 10 ticks
- **Very far** (64+m): Every 40 ticks

### What Does NOT Get Synced
- AI state (server-only)
- Pathfinding data (server-only)
- Internal calculations (server-only)

## Code Examples

### ✅ CORRECT: Server-Side Logic
```java
@Override
public void tick() {
    super.tick();

    // SERVER ONLY - protected by isClientSide check
    if (!this.level().isClientSide) {
        updateAI();
        checkCombat();
        handlePathfinding();
    }
}
```

### ❌ WRONG: Client-Side Game Logic
```java
@Override
public void tick() {
    super.tick();

    // BAD - runs on both client and server
    updateAI(); // Client should NOT run AI!
}
```

### ✅ CORRECT: Client-Side Rendering
```java
@Override
public void render(...) {
    // Simple distance check for culling
    double dist = player.distanceTo(bot);
    if (dist > 128) return; // Don't render

    // Just render - no game logic
    super.render(...);
}
```

## Performance Comparison

### Before Optimization (Both client and server run logic)
- Client CPU: HIGH (AI + rendering for 200 bots)
- Server CPU: HIGH
- Network: HIGH (frequent syncs)
- **Result**: LAG with 50+ bots

### After Optimization (Server-authoritative)
- Client CPU: LOW (only rendering nearby bots)
- Server CPU: MEDIUM (optimized with distance-based ticks)
- Network: LOW (optimized sync frequency)
- **Result**: SMOOTH with 200+ bots

## Testing with 200+ Bots

Recommended test scenario:
1. Spawn 200 bots: `/aibrigade spawn group 200 player follow 10 false testGroup`
2. **Client metrics** (F3 debug):
   - FPS should remain 60+
   - Client tick time minimal
   - Memory stable
3. **Server metrics**:
   - TPS should remain 20
   - Bot AI updates staggered
   - Distant bots low CPU

## Key Takeaways

✅ **Server decides, client displays**
✅ **All logic server-side, protected by `!isClientSide`**
✅ **Client only does rendering with simple culling**
✅ **EntityDataAccessor auto-syncs important data**
✅ **Distance-based optimization reduces both client and server load**
