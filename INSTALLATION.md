# Installation et Test d'AIBrigade

## Résumé du Build

✅ **Compilation réussie**
- JAR créé: `build/libs/aibrigade-1.0.0.jar`
- Taille: 77 KB
- Version: 1.0.0
- Minecraft: 1.20.1
- Forge: 47.3.0

## Dépendances Intégrées

Le mod utilise les dépendances suivantes:
- ✅ **GeckoLib 4.4.7** - Animations (intégré dans le code)
- ✅ **SmartBrainLib 1.15** - IA avancée
- ✅ **Citadel 2.6.2** - Framework d'entités (intégré dans le code)
- ✅ **Easy NPC 3.7.3** - Gestion des NPCs
- ✅ **LibX 1.20.1-5.0.13** - Bibliothèque utilitaire

## Installation

### 1. Préparer l'environnement Minecraft

1. Installer **Minecraft 1.20.1**
2. Installer **Forge 1.20.1-47.3.0** ou supérieur

### 2. Télécharger les dépendances

Télécharge et place les mods suivants dans le dossier `mods` de Minecraft:

1. **SmartBrainLib 1.15** (Minecraft 1.20.1)
   - CurseForge: https://www.curseforge.com/minecraft/mc-mods/smartbrainlib
   - File ID: 5654964

2. **GeckoLib 4.4.7** (Minecraft 1.20.1)
   - CurseForge: https://www.curseforge.com/minecraft/mc-mods/geckolib
   - Version: 4.4.7 pour Forge 1.20.1

3. **Citadel 2.6.2** (Minecraft 1.20.1)
   - CurseForge: https://www.curseforge.com/minecraft/mc-mods/citadel
   - File ID: 6702068

4. **Easy NPC 3.7.3** (Minecraft 1.20.1)
   - CurseForge: https://www.curseforge.com/minecraft/mc-mods/easy-npc
   - File ID: 5014652

5. **LibX 1.20.1-5.0.13**
   - CurseForge: https://www.curseforge.com/minecraft/mc-mods/libx
   - File ID: 5080274

### 3. Installer AIBrigade

1. Copie `build/libs/aibrigade-1.0.0.jar` dans le dossier `mods` de Minecraft
2. Lance Minecraft avec le profil Forge 1.20.1

## Tester le Mod

### Vérifier le chargement

1. Lance Minecraft
2. Dans le menu principal, clique sur "Mods"
3. Vérifie que **AIBrigade 1.0.0** apparaît dans la liste
4. Vérifie que toutes les dépendances sont chargées

### Créer un monde de test

1. Crée un nouveau monde en mode Créatif
2. Active les cheats (pour utiliser les commandes)
3. Entre dans le monde

### Tester les commandes

Une fois dans le monde, ouvre le chat (touche `T`) et teste les commandes:

#### 1. Commande de base
```
/aibrigade
```
Devrait afficher l'aide avec toutes les commandes disponibles.

#### 2. Spawn d'un bot unique
```
/aibrigade spawn <ton_pseudo> idle 10 false defaultgroup
```
Exemple:
```
/aibrigade spawn Steve idle 10 false test1
```

#### 3. Spawn d'un groupe de bots
```
/aibrigade spawngroup 5 <ton_pseudo> follow 15 false mygroup
```
Exemple:
```
/aibrigade spawngroup 5 Steve follow 15 false squad1
```

#### 4. Lister les bots
```
/aibrigade list
```

#### 5. Lister les groupes
```
/aibrigade groups
```

#### 6. Changer le comportement d'un groupe
```
/aibrigade behavior mygroup raid
```

#### 7. Téléporter un groupe
```
/aibrigade teleport mygroup ~ ~1 ~
```

#### 8. Équiper un groupe
```
/aibrigade equip mygroup
```

#### 9. Définir l'hostilité entre groupes
```
/aibrigade hostile mygroup enemygroup
```

#### 10. Supprimer tous les bots
```
/aibrigade killall
```

### Logs à vérifier

Pendant les tests, vérifie les logs Minecraft (fichier `latest.log`) pour:

1. **Initialisation du mod**:
   - `[AIBrigade/]: Initializing AIBrigade mod version 1.0.0`
   - `[AIBrigade/]: Entity types registered`
   - `[AIBrigade/]: Bot renderer registered successfully`

2. **Enregistrement des commandes**:
   - `[AIBrigade/]: Registering AIBrigade commands`
   - `[AIBrigade/]: Commands registered successfully`

3. **Exécution des commandes**:
   - Messages de spawn de bots
   - Messages de changement de comportement
   - Messages d'erreur si problèmes

## Problèmes Connus

### Environnement de Développement

⚠️ **Le mod ne peut PAS être testé avec `./gradlew runClient` en environnement de développement**

Raison: Citadel a des problèmes de mixin avec les mappings officiels en dev.

Erreur typique:
```
InvalidInjectionException: Critical injection failure: @Inject annotation on citadel_registerData
could not find any targets matching 'Lnet/minecraft/world/entity/LivingEntity;m_8097_()V'
```

**Solution**: Utiliser le JAR compilé dans un vrai client Minecraft Forge.

### Animations Manquantes

Les bots n'auront pas d'animations visuelles car les fichiers suivants n'ont pas été créés:
- `assets/aibrigade/geo/bot.geo.json` (modèle 3D)
- `assets/aibrigade/animations/bot.animation.json` (animations)
- `assets/aibrigade/textures/entity/bot_*.png` (textures)

Les bots apparaîtront mais sans rendu visuel. Pour ajouter les animations, utilise **Blockbench** pour créer:
1. Un modèle 3D de bot
2. Les 9 animations définies: idle, walk, run, attack, jump, climb, swim, damaged, sneak
3. Les textures pour chaque skin (default, soldier, scout, medic, engineer, heavy)

## Support

Si le mod ne se charge pas:
1. Vérifie que toutes les dépendances sont installées
2. Vérifie les versions (Minecraft 1.20.1, Forge 47.3.0+)
3. Consulte le fichier `latest.log` dans le dossier `logs` de Minecraft
4. Vérifie que le JAR n'est pas corrompu (77 KB minimum)

## Développement Futur

Pour continuer le développement:
1. Créer les modèles 3D et animations dans Blockbench
2. Ajouter les fichiers de ressources manquants
3. Tester le comportement AI avancé avec SmartBrainLib
4. Implémenter l'intégration complète avec Easy NPC pour les skins
5. Optimiser les performances pour 300+ bots simultanés
