# Système de nettoyage automatique des bots - Changelog

## 🎯 Problème résolu

**Avant** : Lorsque vous spawniez 300 bots et qu'ils mouraient tous, vous ne pouviez plus spawn de nouveaux bots car les bots morts restaient comptabilisés dans la limite.

**Après** : Les bots morts sont automatiquement retirés du système toutes les 5 secondes, libérant de la place pour de nouveaux spawns.

## ✅ Modifications apportées

### 1. BotEntity.java (lignes 762-795)
**Ajout de la méthode `die()`**
- Appelée automatiquement quand un bot meurt
- Notifie le BotManager pour nettoyer l'entrée
- Affiche un log : `[BotEntity] Bot X died and was cleaned up`

**Modification de la méthode `remove()`**
- Nettoie aussi le bot du BotManager
- Libère l'UUID Mojang pour réutilisation
- Évite les doublons de nettoyage

### 2. BotManager.java (lignes 190-232, 842-895)
**Nouvelles méthodes de nettoyage**

#### `onBotDeath(BotEntity bot)` (ligne 196)
- Appelée quand un bot meurt
- Retire le bot de `activeBots`
- Retire le bot de son groupe
- Log : "Bot X died and was cleaned up (remaining: Y/300)"

#### `onBotRemoved(BotEntity bot)` (ligne 218)
- Appelée quand un bot est retiré du monde
- Vérifie si déjà nettoyé (évite les doublons)
- Même nettoyage que `onBotDeath`

#### `cleanupDeadBots()` (ligne 847)
- Nettoyage périodique automatique
- Parcourt tous les bots actifs
- Retire ceux qui sont morts, invalides ou supprimés
- Log : "Cleaned up X dead/invalid bots (remaining: Y/300)"

#### Nouveaux getters
- `getBotCount()` : Retourne le nombre de bots actifs
- `getMaxBots()` : Retourne la limite (300)

### 3. AIManager.java (lignes 34, 107-122)
**Nettoyage périodique automatique**
- Nouvelle constante : `CLEANUP_INTERVAL = 100` (5 secondes)
- Appelle `cleanupDeadBots()` toutes les 5 secondes
- Intégré dans le tick du serveur

### 4. BotCommandHandler.java (lignes 131-132, 512-533, 589-590)
**Nouvelle commande : `/aibrigade cleanupbots`**
- Déclenche manuellement le nettoyage
- Affiche combien de bots ont été nettoyés
- Affiche le nouveau compte : "Cleaned up X dead bots. Active bots: Y/300"

**Mise à jour de `/aibrigade listbots`**
- Utilise maintenant `getBotCount()` et `getMaxBots()`
- Affichage cohérent : "Active bots: Y/300"

**Aide mise à jour**
- Ajout de la commande `cleanupbots` dans `/aibrigade help`
- Note : "Dead bots are automatically cleaned every 5 seconds"

## 🔄 Flux de nettoyage

```
Bot meurt
    ↓
BotEntity.die() est appelé
    ↓
BotManager.onBotDeath() est appelé
    ↓
Bot retiré de activeBots + groupe
    ↓
Place libérée pour nouveau spawn
```

### Nettoyage automatique périodique (backup)

```
Chaque 5 secondes (100 ticks)
    ↓
AIManager.onServerTick() détecte l'intervalle
    ↓
BotManager.cleanupDeadBots() vérifie tous les bots
    ↓
Retire les bots morts/invalides non détectés
```

## 📊 Avant vs Après

### Avant
```
Spawn 300 bots
Tous les bots meurent
activeBots.size() = 300 (bots morts comptent encore)
/aibrigade spawn group 10 → ERREUR: "Maximum bot limit (300) reached"
```

### Après
```
Spawn 300 bots
Tous les bots meurent
Attendre 5 secondes (nettoyage automatique)
activeBots.size() = 0
/aibrigade spawn group 300 → ✅ Succès!
```

## 🧪 Comment tester

### Test 1 : Nettoyage automatique
```bash
# Spawner des bots
/aibrigade spawn group 50 PlayerName follow 10.0 false testgroup

# Vérifier le compte
/aibrigade listbots
# Affiche : "Active bots: 50 / 300"

# Tuer tous les bots (commande ou combat)
/kill @e[type=aibrigade:bot]

# Attendre 5 secondes, puis vérifier
/aibrigade listbots
# Affiche : "Active bots: 0 / 300"

# Spawner à nouveau possible!
/aibrigade spawn group 50 PlayerName follow 10.0 false testgroup2
# ✅ Succès!
```

### Test 2 : Nettoyage manuel
```bash
# Si des bots morts restent bloqués
/aibrigade cleanupbots
# Affiche : "Cleaned up X dead bots. Active bots: Y/300"
```

### Test 3 : Vérifier les logs
Dans `logs/latest.log`, vous devriez voir :
```
[BotEntity] Bot Notch died and was cleaned up
[BotManager] Bot Notch died and was cleaned up (remaining: 49/300)
[AIManager] Cleaned up 0 dead/invalid bots (remaining: 49/300)
```

## 💡 Notes importantes

1. **Nettoyage triple sécurité** :
   - Immédiat quand le bot meurt (`die()`)
   - Quand le bot est retiré (`remove()`)
   - Périodique toutes les 5 secondes (backup)

2. **Libération des UUIDs Mojang** :
   - Les UUIDs sont libérés pour être réutilisés
   - Évite d'épuiser la liste des pseudos célèbres

3. **Thread-safe** :
   - `activeBots` est un `ConcurrentHashMap`
   - Pas de problèmes de concurrence

4. **Performance** :
   - Nettoyage toutes les 5 secondes (pas chaque tick)
   - Faible impact sur les performances

## 🚀 Prochaines étapes recommandées

1. **Tester intensivement** : Spawner 300 bots, les tuer, respawner
2. **Vérifier les logs** : S'assurer qu'il n'y a pas de fuites de mémoire
3. **Test de stress** : Spawner/tuer en boucle pendant plusieurs minutes
4. **Persistence** : Vérifier que les bots sauvegardés ne reviennent pas après redémarrage

## 🎉 Résultat final

Vous pouvez maintenant :
- ✅ Spawner 300 bots
- ✅ Les tuer tous
- ✅ Attendre 5 secondes
- ✅ Respawner 300 nouveaux bots
- ✅ Répéter à l'infini sans problème!
