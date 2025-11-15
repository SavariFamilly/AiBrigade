# AIBrigade - Plan d'Action pour Mise en Cohérence Documentation/Code

**Date:** 2025-11-15
**Basé sur:** AUDIT_REPORT.md
**Objectif:** Synchroniser documentation et implémentation

---

## 🎯 Vue d'Ensemble

Ce document liste les actions concrètes à entreprendre pour corriger les incohérences identifiées dans l'audit.

**Résumé:**
- 📝 **17 fonctionnalités** à documenter
- 🔧 **4 commandes** manquantes à implémenter
- ✏️ **15 sections** de documentation à corriger
- ⚠️ **4 incohérences majeures** à résoudre

---

## 📋 Actions Prioritaires

### PRIORITÉ 1: Mettre à Jour README.md

#### Action 1.1: Ajouter Section "Nouvelles Commandes de Gestion Individuelle"

**Localisation:** README.md après ligne 219 (section "Show help")

**Contenu à ajouter:**

```markdown
#### Individual Bot Management

**Kill a specific bot:**
```
/aibrigade kill <botName>
```

Example:
```
/aibrigade kill BotSoldier_1
```

**Modify individual bot:**

Change bot name (fetches Mojang skin):
```
/aibrigade modify <botName> name <newName>
```

Example:
```
/aibrigade modify BotSoldier_1 name Technoblade
# Bot will receive Technoblade's skin from Mojang
```

Set item in main hand:
```
/aibrigade modify <botName> hand <item>
```

Example:
```
/aibrigade modify BotSoldier_1 hand diamond_sword
```

Set item in off-hand:
```
/aibrigade modify <botName> offhand <item>
```

Example:
```
/aibrigade modify BotSoldier_1 offhand oak_planks
```

Set armor piece:
```
/aibrigade modify <botName> armor <slot> <item>
```

Slots: `head`, `chest`, `legs`, `feet`

Example:
```
/aibrigade modify BotSoldier_1 armor head diamond_helmet
```

**Note:** Bot names are unique. If you spawn a bot with a name that already exists,
it will be automatically renamed with a suffix (e.g., BotSoldier_1, BotSoldier_2).
```

**Fichier:** README.md
**Ligne d'insertion:** Après ligne 219

---

#### Action 1.2: Ajouter Section "Follow Leader System"

**Localisation:** README.md après ligne 187 (section "Set follow radius")

**Contenu à ajouter:**

```markdown
**Enable/disable follow leader:**
```
/aibrigade followleader <groupName> <enabled> <radius>
```

Example:
```
/aibrigade followleader AlphaSquad true 15.0
```

**Follow Behavior:**
- **5/6 of bots** (radius-based): Follow leader until within configured radius, then stop
- **1/6 of bots** (active follow): Follow leader very closely (3 blocks minimum)
- **Static bots**: Only look at leader, do not move (except to attack hostile mobs)

Note: Use `/aibrigade assignleader` first to assign a leader to the group.
```

**Fichier:** README.md
**Ligne d'insertion:** Après ligne 187

---

#### Action 1.3: Ajouter Section "Jump System"

**Localisation:** README.md après "Toggle static state"

**Contenu à ajouter:**

```markdown
**Toggle jumping behavior:**
```
/aibrigade togglejump <target>
```

Modes:
- **Random jumps**: Bots jump every 2-30 minutes at random intervals (default)
- **Forced continuous jumps**: Bots jump continuously like bunny hopping (when toggled on)

Example:
```
/aibrigade togglejump AlphaSquad
# Toggle on: continuous jumping
# Toggle again: back to random jumps
```
```

**Fichier:** README.md
**Ligne d'insertion:** Après ligne 197

---

#### Action 1.4: Ajouter Section "Mojang Skins System"

**Localisation:** README.md section Features, après ligne 14

**Contenu à ajouter:**

```markdown
### Mojang Skin System
- **Real player skins**: Bots automatically receive skins from real Minecraft players via Mojang API
- **30+ famous players**: Random selection from professional players, streamers, and content creators
- **Unique names**: Each bot has a unique name to prevent conflicts
- **Dynamic skin changes**: Change bot name to get a different player's skin
- **Async loading**: Skins load asynchronously without impacting game performance
```

**Fichier:** README.md
**Ligne:** 14 (après "Performance optimized")

---

#### Action 1.5: Ajouter Section "Block Placement System"

**Localisation:** README.md section "Bot Capabilities", après ligne 24

**Contenu à ajouter:**

```markdown
- Place blocks to reach targets (bridges, towers, stairs)
- Build escape routes when trapped
- Intelligent pathfinding with block placement
- Toggle block placement per bot or group
```

**Fichier:** README.md
**Ligne:** 24 (après "Climb obstacles")

---

#### Action 1.6: Corriger Section "Behaviors"

**Localisation:** README.md ligne 232-264

**Modification:**

```markdown
## Behaviors

### Follow
- Bots follow assigned leader within radius
- Two follow modes:
  - **Radius-based (5/6 bots)**: Follow until within configured radius
  - **Active follow (1/6 bots)**: Follow very closely (3 blocks)
- Maintain spacing to avoid clustering
- Static bots look at leader but don't move
- Defend leader if attacked

### Static Guard (Static Bots)
- **Stationary**: Bots stay at spawn position
- **Look at leader**: Turn to face leader when followleader enabled
- **Attack hostile mobs**: Automatically attack nearby monsters (16 block range)
- **No movement**: Don't wander, jump randomly, or place blocks
- **Ideal for base defense**

### Patrol (Coming Soon)
- Bots patrol around home position
- Move between random waypoints
- Attack threats that come near
- Return to patrol after combat

### Raid (Coming Soon)
- Aggressive behavior
- Attack nearby entities and structures
- Break blocks if configured
- Collect loot from kills

### Guard (Coming Soon)
- Defensive behavior
- Stay near guard position
- Scan surroundings for threats
- Alert group when enemies approach

### Idle
- Default passive behavior
- Minimal movement (random wandering)
- Look around occasionally
- Low resource usage

**Note:** Currently, patrol, raid, and guard behaviors use the basic follow/idle system.
Specialized AI goals for these behaviors are planned for future updates.
```

**Fichier:** README.md
**Lignes:** 232-264

---

### PRIORITÉ 2: Implémenter Commandes Manquantes

#### Action 2.1: Implémenter /aibrigade listbots

**Fichier:** src/main/java/com/aibrigade/commands/BotCommandHandler.java

**Ajouter après ligne 137:**

```java
.then(Commands.literal("listbots")
    .executes(BotCommandHandler::listBots))
```

**Méthode à ajouter:**

```java
private static int listBots(CommandContext<CommandSourceStack> context) {
    BotManager botManager = AIBrigadeMod.getBotManager();
    Map<UUID, BotEntity> activeBots = botManager.getActiveBots();

    if (activeBots.isEmpty()) {
        context.getSource().sendSuccess(() ->
            Component.literal("No bots are currently spawned."),
            false);
        return 0;
    }

    context.getSource().sendSuccess(() ->
        Component.literal("=== Active Bots (" + activeBots.size() + "/" + botManager.getMaxBots() + ") ==="),
        false);

    for (BotEntity bot : activeBots.values()) {
        String status = bot.isStatic() ? "[STATIC]" : "[MOBILE]";
        String behavior = bot.getBehaviorType();
        String group = bot.getBotGroup();

        context.getSource().sendSuccess(() ->
            Component.literal(String.format("  %s %s - Group: %s, Behavior: %s",
                status, bot.getBotName(), group, behavior)),
            false);
    }

    return 1;
}
```

**Localisation:** BotCommandHandler.java
**Action:** Ajouter commande et méthode

---

#### Action 2.2: Implémenter /aibrigade listgroups

**Fichier:** src/main/java/com/aibrigade/commands/BotCommandHandler.java

**Ajouter après listbots:**

```java
.then(Commands.literal("listgroups")
    .executes(BotCommandHandler::listGroups))
```

**Méthode à ajouter:**

```java
private static int listGroups(CommandContext<CommandSourceStack> context) {
    BotManager botManager = AIBrigadeMod.getBotManager();
    Map<String, BotManager.BotGroup> groups = botManager.getBotGroups();

    if (groups.isEmpty()) {
        context.getSource().sendSuccess(() ->
            Component.literal("No bot groups exist."),
            false);
        return 0;
    }

    context.getSource().sendSuccess(() ->
        Component.literal("=== Bot Groups (" + groups.size() + ") ==="),
        false);

    for (Map.Entry<String, BotManager.BotGroup> entry : groups.entrySet()) {
        BotManager.BotGroup group = entry.getValue();
        int botCount = group.getBotIds().size();
        String leader = group.getLeaderName();
        float radius = group.getFollowRadius();

        context.getSource().sendSuccess(() ->
            Component.literal(String.format("  %s: %d bots, Leader: %s, Radius: %.1f",
                entry.getKey(), botCount, leader, radius)),
            false);
    }

    return 1;
}
```

**Localisation:** BotCommandHandler.java
**Action:** Ajouter commande et méthode

---

#### Action 2.3: Implémenter /aibrigade groupinfo

**Fichier:** src/main/java/com/aibrigade/commands/BotCommandHandler.java

**Ajouter:**

```java
.then(Commands.literal("groupinfo")
    .then(Commands.argument("groupName", StringArgumentType.string())
        .executes(BotCommandHandler::groupInfo)))
```

**Méthode à ajouter:**

```java
private static int groupInfo(CommandContext<CommandSourceStack> context) {
    String groupName = StringArgumentType.getString(context, "groupName");
    BotManager botManager = AIBrigadeMod.getBotManager();

    Map<String, BotManager.BotGroup> groups = botManager.getBotGroups();
    if (!groups.containsKey(groupName)) {
        context.getSource().sendFailure(
            Component.literal("Group '" + groupName + "' not found."));
        return 0;
    }

    BotManager.BotGroup group = groups.get(groupName);

    context.getSource().sendSuccess(() ->
        Component.literal("=== Group Info: " + groupName + " ==="),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("  Leader: " + group.getLeaderName()),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("  Follow Radius: " + group.getFollowRadius()),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("  Bot Count: " + group.getBotIds().size()),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("  Bots:"),
        false);

    Map<UUID, BotEntity> activeBots = botManager.getActiveBots();
    for (UUID botId : group.getBotIds()) {
        BotEntity bot = activeBots.get(botId);
        if (bot != null) {
            String status = bot.isStatic() ? "[STATIC]" : "[MOBILE]";
            String behavior = bot.getBehaviorType();
            boolean following = bot.isFollowingLeader();

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("    %s %s - %s, Following: %s",
                    status, bot.getBotName(), behavior, following)),
                false);
        }
    }

    return 1;
}
```

**Localisation:** BotCommandHandler.java
**Action:** Ajouter commande et méthode

---

#### Action 2.4: Implémenter /aibrigade help

**Fichier:** src/main/java/com/aibrigade/commands/BotCommandHandler.java

**Ajouter:**

```java
.then(Commands.literal("help")
    .executes(BotCommandHandler::showHelp))
```

**Méthode à ajouter:**

```java
private static int showHelp(CommandContext<CommandSourceStack> context) {
    context.getSource().sendSuccess(() ->
        Component.literal("=== AIBrigade Commands ==="),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("Spawn:"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade spawn solo <leader> <behavior> <radius> <static> <group>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade spawn group <count> <leader> <behavior> <radius> <static> <group>"),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("Management:"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade kill <botName>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade removegroup <groupName>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade assignleader <group> <leader>"),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("Behavior:"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade followleader <group> <enabled> <radius>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade setbehavior <target> <behavior>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade togglestatic <target>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade togglejump <target>"),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("Equipment:"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade givearmor <target> <full|partial> <materials>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade modify <bot> hand <item>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade modify <bot> offhand <item>"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade modify <bot> armor <slot> <item>"),
        false);

    context.getSource().sendSuccess(() ->
        Component.literal("Info:"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade listbots"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade listgroups"),
        false);
    context.getSource().sendSuccess(() ->
        Component.literal("  /aibrigade groupinfo <groupName>"),
        false);

    return 1;
}
```

**Localisation:** BotCommandHandler.java
**Action:** Ajouter commande et méthode

---

### PRIORITÉ 3: Mettre à Jour EXAMPLES.md

#### Action 3.1: Corriger Exemples avec Nouvelles Commandes

**Fichier:** EXAMPLES.md
**Modifications:**

1. **Ligne 224-237:** Remplacer `removebot` par `kill`
2. **Ajouter nouvel exemple:** "Modifier un bot individuel"

```markdown
### Example: Individual Bot Customization

Customize a specific bot:

```
# List all bots to find the name
/aibrigade listbots

# Change bot name (gets new Mojang skin)
/aibrigade modify BotSoldier_1 name Dream

# Give diamond sword in main hand
/aibrigade modify Dream hand diamond_sword

# Give shield in off-hand
/aibrigade modify Dream offhand shield

# Give diamond helmet
/aibrigade modify Dream armor head diamond_helmet

# Enable continuous jumping
/aibrigade togglejump Dream
```
```

**Localisation:** EXAMPLES.md après ligne 237
**Action:** Ajouter nouvel exemple

---

### PRIORITÉ 4: Créer CHANGELOG.md

#### Action 4.1: Créer Fichier CHANGELOG.md

**Fichier:** CHANGELOG.md (nouveau)

**Contenu:**

```markdown
# Changelog

All notable changes to AIBrigade will be documented in this file.

## [Unreleased]

### Added
- Individual bot management system
  - `/aibrigade kill <botName>` - Kill specific bot by name
  - `/aibrigade modify <bot> name <name>` - Rename bot and fetch Mojang skin
  - `/aibrigade modify <bot> hand <item>` - Set main hand item
  - `/aibrigade modify <bot> offhand <item>` - Set off-hand item
  - `/aibrigade modify <bot> armor <slot> <item>` - Set armor piece
- Unique bot names system
  - Auto-rename on collision (appends _1, _2, etc.)
  - Prevents duplicate names
- Mojang skin system
  - Bots automatically receive skins from real Minecraft players
  - 30+ famous players (Technoblade, Dream, Notch, etc.)
  - Async loading via Mojang API
  - Change skin by changing bot name
- Jump system
  - Random jumps (2-30 minute intervals)
  - Forced continuous jumping via `/aibrigade togglejump`
- Block placement system
  - PlaceBlockToReachTargetGoal enables bridge/tower building
  - Builds bridges, towers, diagonal stairs, escape routes
  - Toggle with canPlaceBlocks flag
- `/aibrigade followleader <group> <enabled> <radius>` - Enable follow with radius
- `/aibrigade sethostiletogroup <groupName>` - Player becomes hostile to group

### Fixed
- **Critical:** Static bots now attack hostile mobs (commit 9ac729f)
  - Removed setNoAi(true) that was disabling all AI
  - Static bots can now use StaticBotDefenseGoal properly
- **Critical:** Static bots now spawn on ground (commit 9ac729f)
  - Added findGroundBelow() to search down up to 256 blocks
  - Prevents floating static bots in mid-air
- **Critical:** Follow system now works correctly (commit 9ac729f)
  - 5/6 bots actively follow until within radius (RADIUS_BASED)
  - 1/6 bots follow very closely (ACTIVE_FOLLOW)
  - Simplified canUse() and canContinueToUse() logic
- **Critical:** Mojang skins now synchronize to clients (commit 9ac729f)
  - EntityData modifications now execute on server thread
  - Fixes skin not displaying issue
- Static bots no longer wander, jump randomly, or place blocks
- Missing imports fixed (ItemStack, Items, BotEntity)

### Changed
- Follow behavior now has two distinct modes
  - RADIUS_BASED (5/6): Follow until within maxFollowDistance
  - ACTIVE_FOLLOW (1/6): Follow closely until minFollowDistance (3 blocks)
- Static bots can now attack while staying stationary
- Random equipment system applies on spawn (tools, food, blocks, or empty)

## [1.0.0] - Initial Release

### Added
- Bot spawning system (solo and group)
- Group management
- Basic AI behaviors (follow, idle)
- Equipment distribution system
- Armor management with randomization
- Static bot mode
- Hostility system between groups
- Commands system with 15+ commands
- Bot persistence (structure in place)
- Thread pool for future AI processing
- Configuration system

### Known Issues
- Patrol, Raid, and Guard behaviors not yet implemented as specific goals
- Persistence save/load is stubbed (not functional)
- Animations commented out (waiting for GeckoLib dependency)
- `/aibrigade listbots`, `/aibrigade listgroups`, `/aibrigade groupinfo`, `/aibrigade help` not yet implemented
```

**Localisation:** Racine du projet
**Action:** Créer nouveau fichier

---

## 📊 Résumé des Fichiers à Modifier

| Fichier | Actions | Priorité |
|---------|---------|----------|
| **README.md** | 6 ajouts/modifications | P1 |
| **BotCommandHandler.java** | 4 nouvelles commandes | P2 |
| **EXAMPLES.md** | 1 correction, 1 nouvel exemple | P3 |
| **CHANGELOG.md** | Création nouveau fichier | P4 |

---

## ✅ Checklist de Validation

Après avoir effectué les modifications:

- [ ] README.md mis à jour avec toutes les nouvelles commandes
- [ ] Section Mojang Skins ajoutée à README.md
- [ ] Section Behaviors corrigée dans README.md
- [ ] Commande `/aibrigade listbots` implémentée et testée
- [ ] Commande `/aibrigade listgroups` implémentée et testée
- [ ] Commande `/aibrigade groupinfo` implémentée et testée
- [ ] Commande `/aibrigade help` implémentée et testée
- [ ] EXAMPLES.md mis à jour
- [ ] CHANGELOG.md créé
- [ ] Build teste sans erreurs
- [ ] Test en jeu des nouvelles commandes
- [ ] Audit final pour vérifier cohérence

---

## 🎯 Ordre d'Exécution Recommandé

1. **Jour 1:** Actions 1.1 à 1.6 (Mise à jour README.md)
2. **Jour 2:** Actions 2.1 à 2.4 (Implémenter commandes manquantes)
3. **Jour 3:** Action 3.1 (Mise à jour EXAMPLES.md)
4. **Jour 4:** Action 4.1 (Créer CHANGELOG.md) + Tests finaux

**Temps estimé total:** 4 jours

---

**Document créé par:** Claude Code
**Date:** 2025-11-15
**Basé sur:** AUDIT_REPORT.md
