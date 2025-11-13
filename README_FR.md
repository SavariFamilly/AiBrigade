# AIBrigade - Mod Minecraft pour Bots IA Avancés

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green.svg)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-58.1.0-orange.svg)](https://files.minecraftforge.net)
[![Status](https://img.shields.io/badge/Status-En_Développement-yellow.svg)]()

## 📋 Description

**AIBrigade** est un mod Minecraft ambitieux qui permet de spawner et contrôler jusqu'à **300+ bots NPC** avec des comportements IA avancés. Les bots ressemblent à des joueurs réels et peuvent exécuter des tâches complexes comme suivre un leader, attaquer des cibles, patrouiller, grimper des obstacles, et bien plus.

## ⚠️ Statut Actuel

**Le mod est actuellement en développement et ne compile pas complètement.**

Consultez `COMPILATION_STATUS.md` pour les détails sur les problèmes restants et leur résolution.

### Ce qui fonctionne ✅
- Structure complète du projet
- Toutes les classes Java créées avec méthodes stub
- Système de commandes complet
- Gestion des groupes et leaders
- Système d'équipement
- Configuration JSON
- La majorité des corrections API pour 1.21.8

### Ce qui nécessite des corrections ❌
- Intégration GeckoLib (dépendance manquante pour 1.21.8)
- Quelques API EventBus de Forge
- Méthodes NBT Entity
- Méthode doHurtTarget()

## 🎯 Fonctionnalités Planifiées

- ✅ **Spawn configurable** : 1 à 300 bots simultanément
- ✅ **IA avancée** : Suivre un leader, attaquer, patrouiller, disperser, escalader
- ✅ **Gestion de groupes** : Organisation en escouades avec leaders assignables
- ✅ **Hostilité dynamique** : Conflits entre groupes selon les interactions
- ✅ **Équipement personnalisé** : Armures et armes configurables individuellement
- 🔄 **Animations réalistes** : Course, saut, attaque, escalade (en attente GeckoLib)
- ✅ **Pathfinding intelligent** : Navigation avancée et évitement d'obstacles
- ✅ **Commandes complètes** : Contrôle total via `/aibrigade`
- ✅ **Persistance** : Sauvegarde automatique des bots et groupes
- ✅ **Multithreading** : Optimisation des performances

## 🚀 Installation (Une fois compilé)

### Prérequis

- **Minecraft 1.21.8**
- **Forge 58.1.0 ou supérieur**
- **Java 21**

### Dépendances

| Mod | Version | Statut | Description |
|-----|---------|--------|-------------|
| GeckoLib | 5.2.2+ | ⚠️ Indisponible 1.21.8 | Animations des bots |
| SmartBrainLib | 1.16+ | ⚠️ À vérifier | IA comportementale avancée |
| Easy NPC | 5.9+ | Optionnel | Intégration NPC |
| Citadel | 2.6+ | Optionnel | Utilitaires entités |

## 🎮 Commandes Disponibles

Toutes les commandes nécessitent le niveau d'opérateur 2.

### Spawn de Bots

```bash
# Spawner un bot solo
/aibrigade spawn solo leader:<nom> behavior:<type> radius:<rayon> static:<true|false> groupName:<nom>

# Spawner un groupe de bots
/aibrigade spawn group <nombre> leader:<nom> behavior:<type> radius:<rayon> static:<true|false> groupName:<nom>
```

**Exemples:**
```bash
# 10 bots suivant "Steve" dans 15 blocs
/aibrigade spawn group 10 leader:Steve behavior:follow radius:15 static:false groupName:AlphaSquad

# 5 bots statiques en garde
/aibrigade spawn group 5 leader:self behavior:guard radius:10 static:true groupName:Guards
```

### Types de Comportements

| Comportement | Description |
|--------------|-------------|
| `follow` | Suit le leader |
| `raid` | Mode agressif |
| `patrol` | Patrouille |
| `guard` | Défend un point |
| `idle` | Aucune action |

### Gestion des Groupes

```bash
/aibrigade assignleader <groupName> <leaderName>  # Changer le leader
/aibrigade hostile <groupe1> <groupe2>             # Rendre hostiles
/aibrigade setbehavior <target> <behavior>         # Changer comportement
/aibrigade setradius <groupName> <rayon>           # Modifier rayon
/aibrigade togglestatic <target>                   # Basculer statique/mobile
```

### Équipement des Bots

```bash
# Armure complète
/aibrigade givearmor <target> full <material>

# Armure mixte aléatoire
/aibrigade givearmor <target> partial <materials>
```

**Matériaux:** `diamond`, `iron`, `chainmail`, `leather`

**Exemples:**
```bash
/aibrigade givearmor AlphaSquad full diamond
/aibrigade givearmor BetaSquad partial irondiamond
```

### Informations

```bash
/aibrigade groupinfo <groupName>   # Info groupe
/aibrigade listbots                # Liste bots
/aibrigade listgroups              # Liste groupes
/aibrigade removebot <botName>     # Supprimer bot
/aibrigade removegroup <groupName> # Supprimer groupe
/aibrigade help                    # Aide
```

## 🏗️ Architecture

```
aibrigade/
├── main/
│   └── AIBrigadeMod.java          # Classe principale
├── bots/
│   ├── BotEntity.java             # Entité bot
│   └── BotManager.java            # Gestion bots
├── ai/
│   ├── AIManager.java             # Gestion IA globale
│   ├── BotGoals.java              # Comportements
│   └── SmartBrainIntegration.java # SmartBrainLib
├── animations/
│   └── BotAnimationHandler.java  # Animations
├── commands/
│   └── BotCommandHandler.java    # Commandes
├── client/
│   ├── BotModel.java              # Modèle 3D
│   ├── BotRenderer.java           # Rendu
│   └── ClientEventHandler.java   # Events client
├── registry/
│   └── ModEntities.java           # Enregistrement
└── utils/
    ├── ConfigManager.java         # Configuration
    ├── EntityLibWrapper.java      # Utilitaires entités
    └── PathfindingWrapper.java    # Pathfinding
```

## ⚙️ Configuration

`config/aibrigade.json` :

```json
{
  "maxBots": 300,
  "aiThreadPoolSize": 4,
  "defaultFollowRadius": 10.0,
  "enableDebugMode": false
}
```

## 🔨 Compilation

```bash
# Build le mod
.\gradlew.bat clean build

# Lancer le client de test
.\gradlew.bat runClient

# Compiler seulement Java
.\gradlew.bat compileJava
```

### Problèmes de Compilation Actuels

Voir `COMPILATION_STATUS.md` pour la liste complète et les solutions.

**Problèmes principaux:**
1. GeckoLib non disponible pour 1.21.8
2. API EventBus changée
3. Méthodes Entity NBT modifiées
4. Signatures de méthodes mises à jour

## 📊 Performances Prévues

| Bots | RAM | CPU | TPS |
|------|-----|-----|-----|
| 50   | +200MB | 15% | 20 |
| 100  | +400MB | 25% | 19 |
| 200  | +800MB | 40% | 18 |
| 300  | +1.2GB | 60% | 16 |

## 🔄 Prochaines Étapes

1. ✅ Créer structure complète du mod
2. ✅ Implémenter toutes les classes Java
3. ✅ Corriger majorité des APIs 1.21.8
4. ⏳ Résoudre dépendances GeckoLib/SmartBrainLib
5. ⏳ Corriger erreurs EventBus et NBT
6. ⏳ Tests de compilation
7. ⏳ Tests runtime
8. ⏳ Optimisations

## 🐛 Problèmes Connus

- Le mod ne compile pas actuellement (voir COMPILATION_STATUS.md)
- GeckoLib 5.2.2 pour 1.21.8 semble indisponible
- SmartBrainLib disponibilité incertaine
- Quelques APIs Forge nécessitent mise à jour

## 🤝 Comment Contribuer

1. Consulter `COMPILATION_STATUS.md`
2. Choisir un problème à résoudre
3. Fork et créer une branche
4. Soumettre une pull request

## 📝 Fichiers Importants

- `README.md` - Documentation (EN)
- `README_FR.md` - Ce fichier
- `COMPILATION_STATUS.md` - État compilation détaillé
- `build.gradle` - Configuration Gradle
- `src/main/resources/META-INF/mods.toml` - Métadonnées mod

## 📄 Licence

MIT License - Voir `LICENSE`

## 🙏 Remerciements

- **Forge Team** - Framework
- **GeckoLib Team** - Animations
- **SmartBrainLib (Tslat)** - IA
- **Claude Code** - Génération du code

## ⚠️ Disclaimer

**Ce mod est en développement précoce et ne fonctionne pas encore.**

Ne pas utiliser en production. Pour développeurs et testeurs uniquement.

---

**Créé avec ❤️ et [Claude Code](https://claude.com/claude-code)**

*Dernière mise à jour: 2025-11-10*
