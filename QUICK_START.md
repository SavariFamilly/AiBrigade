# Guide de Démarrage Rapide - AIBrigade 1.20.1

## 🚀 Installation (2 minutes)

1. **Prérequis**
   - Minecraft 1.20.1
   - Forge 47.3.0+
   - Java 21

2. **Installation**
   ```
   1. Téléchargez aibrigade-1.0.0.jar
   2. Placez dans /mods/
   3. Lancez Minecraft
   ```

3. **Vérification**
   ```
   /aibrigade help
   ```
   Si la commande fonctionne, l'installation est réussie!

---

## ⚡ Premiers Pas (5 minutes)

### Test 1: Spawn votre premier bot

```
/aibrigade spawn solo MyFirstBot
```

Le bot apparaît à votre position avec:
- Texture Steve
- Nom visible au-dessus de la tête
- Comportement idle (inactif)

### Test 2: Equiper le bot

```
/aibrigade givearmor MyFirstBot diamond
```

Le bot porte maintenant une armure diamant complète!

### Test 3: Créer un groupe

```
/aibrigade spawn group Squad1 5
```

5 bots apparaissent formant le groupe "Squad1".

### Test 4: Assigner un leader

```
/aibrigade assignleader Squad1 MyFirstBot
/aibrigade setbehavior Squad1 follow
```

Les 5 bots suivent maintenant MyFirstBot!

### Test 5: Visualisation debug

```
/aibrigade debug enable
/aibrigade debug paths true
/aibrigade debug ranges true
```

Vous voyez maintenant:
- Chemins de pathfinding (cyan)
- Rayons de suivi (jaune)

---

## 🎯 Cas d'Usage Courants

### Base Défensive

Créez une base gardée par des sentinelles statiques:

```bash
# Spawn 4 gardes aux coins
/aibrigade spawn solo Guard1 -10 70 -10
/aibrigade spawn solo Guard2 10 70 -10
/aibrigade spawn solo Guard3 10 70 10
/aibrigade spawn solo Guard4 -10 70 10

# Equipement netherite
/aibrigade givearmor Guard1 netherite
/aibrigade givearmor Guard2 netherite
/aibrigade givearmor Guard3 netherite
/aibrigade givearmor Guard4 netherite

# Mode statique (ne bougent pas)
/aibrigade togglestatic Guard1
/aibrigade togglestatic Guard2
/aibrigade togglestatic Guard3
/aibrigade togglestatic Guard4

# Comportement garde
/aibrigade setbehavior Guard1 guard
/aibrigade setbehavior Guard2 guard
/aibrigade setbehavior Guard3 guard
/aibrigade setbehavior Guard4 guard
```

### Escouade Mobile

Une équipe qui vous suit partout:

```bash
# Spawn le capitaine
/aibrigade spawn solo Captain

# Spawn l'escouade
/aibrigade spawn group Squad 10

# Equipement
/aibrigade givearmor Captain netherite
/aibrigade givearmor Squad iron

# Configuration suivi
/aibrigade assignleader Squad Captain
/aibrigade setbehavior Squad follow
/aibrigade setradius Squad 15

# Le capitaine vous suit (ou suivez-le!)
/aibrigade setbehavior Captain follow
```

### Armée d'Invasion

Groupe hostile pour tests PvE:

```bash
# Spawn armée
/aibrigade spawn group Army 50 -100 64 0

# Equipement varié
/aibrigade givearmor Army mixed

# Activer hostilité
/aibrigade hostile Army true

# Mode attack
/aibrigade setbehavior Army attack
```

### Patrouille Automatique

Bots qui patrouillent une zone:

```bash
# Spawn patrouille
/aibrigade spawn group Patrol 8

# Equipement
/aibrigade givearmor Patrol diamond

# Mode patrouille
/aibrigade setbehavior Patrol patrol
```

---

## 🎨 Générateur de Noms

Le mod génère automatiquement des noms, mais vous pouvez utiliser des presets:

### Preset Realistic
Noms réalistes (Jean Dupont, Mary Smith)

```bash
/aibrigade setpreset MyBot preset:realistic
```

### Preset Gamer
Style gamer (xXShadowXx, ProGamer360)

```bash
/aibrigade setpreset MySquad preset:gamer
```

### Preset Humor
Noms humoristiques (PotatoChip, TacoBell)

```bash
/aibrigade setpreset MyBot preset:humor
```

### Preset Randomize
Complètement aléatoire

```bash
/aibrigade setpreset MySquad preset:randomize
```

### Preset Mixed
Mélange avec préfixes/suffixes

```bash
/aibrigade setpreset MyBot preset:mixed
```

---

## 🐛 Debug et Troubleshooting

### Problème: Bots ne spawnen pas

**Vérifiez:**
```bash
/aibrigade listbots
```

Si vide, réessayez:
```bash
/aibrigade spawn solo TestBot
```

### Problème: Bots ne suivent pas le leader

**Vérifiez:**
1. Leader assigné:
   ```bash
   /aibrigade groupinfo YourGroup
   ```

2. Comportement correct:
   ```bash
   /aibrigade setbehavior YourGroup follow
   ```

3. Rayon suffisant:
   ```bash
   /aibrigade setradius YourGroup 15
   ```

### Problème: Lag avec beaucoup de bots

**Solutions:**
1. Réduire le nombre:
   ```bash
   /aibrigade removegroup LargeGroup
   ```

2. Désactiver debug:
   ```bash
   /aibrigade debug disable
   ```

3. Limiter à 100-150 bots maximum sur machines moyennes

### Voir les infos d'un bot

```bash
# Approchez-vous d'un bot puis:
/aibrigade debug info
```

Affiche:
- Nom, groupe
- Comportement actuel
- Santé
- Leader et distance
- État pathfinding

---

## 📊 Commandes Essentielles

### Gestion

| Commande | Description |
|----------|-------------|
| `/aibrigade spawn solo <name>` | Spawn 1 bot |
| `/aibrigade spawn group <name> <count>` | Spawn groupe |
| `/aibrigade removebot <name>` | Supprimer bot |
| `/aibrigade removegroup <name>` | Supprimer groupe |
| `/aibrigade listbots` | Liste tous les bots |
| `/aibrigade listgroups` | Liste tous les groupes |

### Configuration

| Commande | Description |
|----------|-------------|
| `/aibrigade assignleader <bot> <leader>` | Assigner leader |
| `/aibrigade setbehavior <bot> <behavior>` | Changer comportement |
| `/aibrigade setradius <bot> <radius>` | Rayon suivi (1-50) |
| `/aibrigade togglestatic <bot>` | Mode statique on/off |
| `/aibrigade hostile <group> <true/false>` | Hostilité on/off |

### Equipement

| Commande | Description |
|----------|-------------|
| `/aibrigade givearmor <bot> leather` | Armure cuir |
| `/aibrigade givearmor <bot> iron` | Armure fer |
| `/aibrigade givearmor <bot> gold` | Armure or |
| `/aibrigade givearmor <bot> diamond` | Armure diamant |
| `/aibrigade givearmor <bot> netherite` | Armure netherite |
| `/aibrigade givearmor <bot> random` | Armure aléatoire |
| `/aibrigade givearmor <bot> mixed` | Mix matériaux |

### Debug

| Commande | Description |
|----------|-------------|
| `/aibrigade debug enable` | Activer debug |
| `/aibrigade debug disable` | Désactiver debug |
| `/aibrigade debug paths true` | Voir chemins |
| `/aibrigade debug targets true` | Voir cibles |
| `/aibrigade debug ranges true` | Voir rayons |
| `/aibrigade debug info` | Info bot proche |

---

## 💡 Astuces

### 1. Nommer vos bots intelligemment

Utilisez des préfixes pour organiser:
```bash
/aibrigade spawn solo Guard_North
/aibrigade spawn solo Guard_South
/aibrigade spawn solo Patrol_Alpha1
/aibrigade spawn solo Patrol_Alpha2
```

### 2. Groupes thématiques

Organisez par rôle:
```bash
/aibrigade spawn group Guards 10
/aibrigade spawn group Scouts 5
/aibrigade spawn group Soldiers 20
```

### 3. Test avant déploiement

Testez avec 1-5 bots avant de spawn 50+:
```bash
/aibrigade spawn solo TestBot
# Testez comportements...
# Si OK:
/aibrigade spawn group MainForce 50
```

### 4. Utilisez le debug

Le mode debug vous aide à comprendre:
- Où vont les bots (paths)
- Qui ils ciblent (targets)
- Leur rayon d'action (ranges)

### 5. Sauvegarde automatique

Les bots sont automatiquement sauvegardés quand vous quittez le monde.
Ils reappara îtront au rechargement!

---

## 📈 Performance

### Recommandations

| Nombre de Bots | Performance | Usage |
|----------------|-------------|-------|
| 1-20 | Excellente | Tests, petits groupes |
| 20-50 | Bonne | Bases défensives |
| 50-100 | Correcte | Armées moyennes |
| 100-200 | Moyenne | Grandes batailles |
| 200-300 | Faible | Maximum absolu |

### Optimisations

1. **Désactiver debug** quand non utilisé
2. **Limiter animations** si lag
3. **Grouper les bots** plutôt que spread
4. **Utiliser mode statique** pour gardes
5. **Cleanup régulier** des bots inutilisés

---

## 🎓 Tutoriel Complet (10 minutes)

### Étape 1: Base Simple

```bash
# Vous êtes en x:0 y:70 z:0
/aibrigade spawn solo MainGuard 0 70 0
/aibrigade givearmor MainGuard netherite
/aibrigade togglestatic MainGuard
```

### Étape 2: Ajout Défenseurs

```bash
/aibrigade spawn group Defenders 4 0 70 0
/aibrigade givearmor Defenders diamond
/aibrigade assignleader Defenders MainGuard
/aibrigade setbehavior Defenders guard
/aibrigade setradius Defenders 10
```

### Étape 3: Patrouille Mobile

```bash
/aibrigade spawn solo PatrolLeader 20 70 20
/aibrigade spawn group PatrolTeam 3 20 70 20
/aibrigade givearmor PatrolLeader iron
/aibrigade givearmor PatrolTeam iron
/aibrigade assignleader PatrolTeam PatrolLeader
/aibrigade setbehavior PatrolTeam follow
/aibrigade setbehavior PatrolLeader patrol
```

### Étape 4: Test Ennemi

```bash
/aibrigade spawn group Enemies 10 -50 70 0
/aibrigade givearmor Enemies mixed
/aibrigade hostile Enemies true
/aibrigade setbehavior Enemies attack
```

### Étape 5: Observer

```bash
/aibrigade debug enable
/aibrigade debug paths true
/aibrigade debug targets true
```

Observez la bataille!

### Étape 6: Cleanup

```bash
/aibrigade removegroup Enemies
/aibrigade removegroup Defenders
/aibrigade removegroup PatrolTeam
/aibrigade removebot MainGuard
/aibrigade removebot PatrolLeader
/aibrigade debug disable
```

---

## 📚 Ressources

- **Audit Complet:** `AUDIT_REPORT.md`
- **Exemples JSON:** `examples/` folder
- **Commandes de Test:** `examples/test_commands.txt`
- **README Complet:** `README.md`

---

## ❓ FAQ

**Q: Combien de bots puis-je spawner?**
A: Jusqu'à 300, mais 100-150 recommandé pour bonnes performances.

**Q: Les bots persistent entre sessions?**
A: Oui! Sauvegarde automatique dans `world/aibrigade/`.

**Q: Puis-je modifier les bots en JSON?**
A: Oui! Editez `world/aibrigade/bots.json` (monde fermé).

**Q: Les bots peuvent-ils mourrir?**
A: Oui, ils ont 20 HP par défaut et peuvent être tués.

**Q: Compatibilité avec autres mods?**
A: La plupart oui, sauf ceux modifiant l'IA vanilla.

**Q: Support multiplayer?**
A: Conçu pour singleplayer, multiplayer non testé.

---

## 🆘 Besoin d'aide?

1. Consultez `/aibrigade help`
2. Lisez `AUDIT_REPORT.md`
3. Vérifiez `examples/test_commands.txt`
4. Ouvrez une issue GitHub

---

**Bon jeu avec AIBrigade! 🤖**
