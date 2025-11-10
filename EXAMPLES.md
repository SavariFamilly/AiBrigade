# AIBrigade Usage Examples

This document provides practical examples for using AIBrigade in various scenarios.

## Quick Start Examples

### Example 1: Personal Bodyguard Squad

Create a small group of 5 bots to protect you:

```
/aibrigade spawn group 5 @s follow 8.0 false Bodyguards
/aibrigade givearmor Bodyguards full diamond
/aibrigade setbehavior Bodyguards guard
```

The bots will:
- Follow you within 8 blocks
- Wear full diamond armor
- Attack anything that threatens you
- Return to your position after combat

### Example 2: Base Defense Force

Set up 20 static guards around your base:

```
# Spawn guards at your current location
/aibrigade spawn group 20 @s guard 15.0 true BaseDefense

# Give them mixed iron/diamond armor
/aibrigade givearmor BaseDefense partial irondiamond

# They'll stay in place and attack intruders
```

### Example 3: Raid Party

Create an aggressive raid party to clear an area:

```
# Spawn 30 raiders
/aibrigade spawn group 30 @s raid 20.0 false RaidParty

# Equip with full iron armor
/aibrigade givearmor RaidParty full iron

# They'll follow you and attack everything hostile
```

### Example 4: Patrol Squad

Set up automated patrols around your village:

```
# Spawn patrol bots
/aibrigade spawn group 10 @s patrol 25.0 false VillagePatrol

# Give them chainmail armor
/aibrigade givearmor VillagePatrol full chainmail

# They'll patrol 25 blocks around spawn point
```

## Advanced Examples

### Example 5: Multi-Group Coordination

Create multiple specialized squads:

```
# Assault squad - aggressive fighters
/aibrigade spawn group 20 @s follow 10.0 false AssaultSquad
/aibrigade givearmor AssaultSquad full diamond
/aibrigade setbehavior AssaultSquad raid

# Support squad - guards and defenders
/aibrigade spawn group 15 @s follow 12.0 false SupportSquad
/aibrigade givearmor SupportSquad full iron
/aibrigade setbehavior SupportSquad guard

# Scout squad - fast and mobile
/aibrigade spawn group 10 @s follow 15.0 false ScoutSquad
/aibrigade givearmor ScoutSquad partial leather
/aibrigade setbehavior ScoutSquad patrol
```

### Example 6: Bot Warfare - Group vs Group

Set up two opposing forces:

```
# Team Alpha
/aibrigade spawn group 25 @s guard 15.0 false TeamAlpha
/aibrigade givearmor TeamAlpha full diamond

# Team Beta (move 50 blocks away first)
/aibrigade spawn group 25 @s guard 15.0 false TeamBeta
/aibrigade givearmor TeamBeta full iron

# Make them hostile to each other
/aibrigade hostile TeamAlpha TeamBeta
/aibrigade hostile TeamBeta TeamAlpha

# Watch the battle unfold!
```

### Example 7: Dynamic Leader Switching

Create a command chain for bot groups:

```
# Create primary leader group
/aibrigade spawn group 5 @s follow 8.0 false Leaders
/aibrigade givearmor Leaders full diamond

# Create follower groups assigned to one of the leader bots
/aibrigade spawn group 10 BotLeaderName follow 12.0 false Squad1
/aibrigade spawn group 10 BotLeaderName follow 12.0 false Squad2

# Now you command the leaders, and they command their squads
```

### Example 8: Mixed Armor Combinations

Create visually diverse squads:

```
# Mixed diamond and iron
/aibrigade spawn group 20 @s follow 10.0 false MixedSquad1
/aibrigade givearmor MixedSquad1 partial irondiamond

# Mixed chainmail and leather
/aibrigade spawn group 15 @s patrol 20.0 false MixedSquad2
/aibrigade givearmor MixedSquad2 partial chainmailleather

# Mixed gold and diamond (fancy!)
/aibrigade spawn group 10 @s guard 15.0 false EliteGuard
/aibrigade givearmor EliteGuard partial golddiamond
```

### Example 9: Fortress Defense System

Multi-layered defense with different behaviors:

```
# Inner guards (static, high armor)
/aibrigade spawn group 15 @s guard 10.0 true InnerGuard
/aibrigade givearmor InnerGuard full diamond

# Patrolling defenders (mobile, medium armor)
/aibrigade spawn group 20 @s patrol 30.0 false PatrolDefense
/aibrigade givearmor PatrolDefense full iron

# Outer sentries (wide patrol, light armor)
/aibrigade spawn group 10 @s patrol 50.0 false OuterSentries
/aibrigade givearmor OuterSentries full chainmail
```

### Example 10: Stress Test / Performance Benchmark

Test maximum bot capacity:

```
# Spawn maximum bots
/aibrigade spawn group 100 @s follow 15.0 false StressTest1
/aibrigade spawn group 100 @s patrol 20.0 false StressTest2
/aibrigade spawn group 100 @s guard 10.0 true StressTest3

# Monitor TPS and performance
# Press F3 to see performance metrics
```

## Scenario-Based Examples

### Scenario 1: Clearing a Nether Fortress

```
# Create a raid party
/aibrigade spawn group 40 @s raid 15.0 false NetherRaiders

# Equip with fire-resistant armor (diamond for best protection)
/aibrigade givearmor NetherRaiders full diamond

# Bots will attack blazes, wither skeletons, and other mobs
# Follow them through the fortress
```

### Scenario 2: Ocean Monument Assault

```
# Create underwater assault team
/aibrigade spawn group 30 @s raid 20.0 false OceanAssault

# Diamond armor for protection
/aibrigade givearmor OceanAssault full diamond

# They'll attack guardians while following you
# Note: Bots can swim and fight underwater!
```

### Scenario 3: Protecting Villagers

```
# Spawn guards around village
/aibrigade spawn group 20 @s guard 25.0 true VillageProtectors

# Mixed armor for cost efficiency
/aibrigade givearmor VillageProtectors partial irondiamond

# Guards will attack zombies, pillagers, and other threats
```

### Scenario 4: Mining Expedition Escort

```
# Personal escort while mining
/aibrigade spawn group 8 @s follow 10.0 false MiningEscort

# Iron armor sufficient for caves
/aibrigade givearmor MiningEscort full iron

# They'll protect you from cave mobs while you mine
```

### Scenario 5: PvP Practice

```
# Create practice opponents
/aibrigade spawn group 5 @s guard 15.0 true PvPPractice

# Vary armor levels for difficulty
/aibrigade givearmor PvPPractice partial ironleather

# Make them hostile to you (as a group)
# Be careful - they will attack!
```

## Command Combinations

### Managing Large Operations

```
# Check current bots
/aibrigade listbots
/aibrigade listgroups

# Get specific group info
/aibrigade groupinfo AssaultSquad

# Adjust radius mid-operation
/aibrigade setradius AssaultSquad 25.0

# Change behavior dynamically
/aibrigade setbehavior AssaultSquad patrol

# Freeze bots in place
/aibrigade togglestatic AssaultSquad

# Unfreeze
/aibrigade togglestatic AssaultSquad
```

### Cleanup Operations

```
# Remove specific group
/aibrigade removegroup OldSquad

# Check remaining
/aibrigade listgroups

# Remove all by group
/aibrigade removegroup Group1
/aibrigade removegroup Group2
/aibrigade removegroup Group3
```

## Best Practices

### Performance Tips

1. **Start small**: Test with 10-20 bots before scaling up
2. **Use static bots**: For stationary guards to reduce AI load
3. **Adjust radius**: Smaller radius = less pathfinding = better performance
4. **Increase update interval**: In config for less frequent AI updates
5. **Disable animations**: If you don't need them, disable for better FPS

### Strategy Tips

1. **Mixed armor**: Saves resources while maintaining visual diversity
2. **Layered defense**: Combine static guards with mobile patrols
3. **Leader hierarchy**: Use bots as leaders for command chains
4. **Behavior switching**: Change behaviors based on situation
5. **Hostility management**: Carefully manage which groups are hostile

### Organization Tips

1. **Naming convention**: Use descriptive group names (e.g., "InnerGuard", "OuterPatrol")
2. **Document groups**: Keep track of group purposes and locations
3. **Color coding**: Use different armor combinations to identify groups visually
4. **Spawn locations**: Mark spawn locations with blocks for reference
5. **Save configurations**: Keep command sequences in text file for reuse

## Troubleshooting Examples

### Example: Bots Won't Follow

```
# Check group info
/aibrigade groupinfo MyGroup

# Verify leader is correct
/aibrigade assignleader MyGroup @s

# Ensure not static
/aibrigade togglestatic MyGroup

# Set appropriate radius
/aibrigade setradius MyGroup 15.0

# Check behavior
/aibrigade setbehavior MyGroup follow
```

### Example: Performance Issues

```
# Reduce group sizes
/aibrigade removegroup LargeGroup1
/aibrigade spawn group 25 @s follow 10.0 false SmallerGroup1

# Make distant guards static
/aibrigade togglestatic DistantGuards

# Adjust config:
# Edit config/aibrigade_config.json
# Increase aiUpdateInterval from 4 to 10
```

## Creative Applications

### Cinematic Battles

```
# Create opposing armies
/aibrigade spawn group 50 @s guard 20.0 false ArmyRed
/aibrigade spawn group 50 @s guard 20.0 false ArmyBlue
/aibrigade hostile ArmyRed ArmyBlue
/aibrigade hostile ArmyBlue ArmyRed

# Different armor for visual distinction
/aibrigade givearmor ArmyRed full iron
/aibrigade givearmor ArmyBlue full diamond

# Spectate and record!
```

### Defense Demonstrations

```
# Set up test base
/aibrigade spawn group 30 @s guard 15.0 true BaseDefenders
/aibrigade givearmor BaseDefenders full diamond

# Spawn attackers
/aibrigade spawn group 40 @s raid 20.0 false Attackers
/aibrigade givearmor Attackers full iron

# Make attackers hostile
/aibrigade hostile Attackers BaseDefenders

# Watch the defense in action
```

### Training Scenarios

```
# Create training dummies (low armor)
/aibrigade spawn group 10 @s guard 10.0 true TrainingDummies
/aibrigade givearmor TrainingDummies full leather

# Practice combat without much risk
```

---

**More examples coming soon! Share your creations with the community!**
