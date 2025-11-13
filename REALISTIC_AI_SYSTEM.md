# 🤖 Système d'IA Réaliste et Semi-Autonome - Documentation Complète

## 📋 Vue d'ensemble

Ce système crée des bots/PNJ avec une IA réaliste, semi-autonome et personnalisée pour Minecraft 1.20.1 Forge.

### ✅ Fonctionnalités implémentées

1. **Système UUID avec skins Mojang réels**
2. **Base de données persistante JSON**
3. **Follow radius avec probabilités et randomisation**
4. **Regard actif (2/6 bots regardent ailleurs)**
5. **Commandes de construction (on/off)**
6. **Déplacements réalistes et organiques**
7. **Équipement aléatoire adapté au rôle**

---

## 🎯 1. Système UUID avec Skins Mojang

### Fichier: `MojangSkinFetcher.java`

**Fonction principale:** Récupère automatiquement les skins officiels depuis l'API Mojang

#### Caractéristiques:
- ✅ Cache intelligent (1 heure) pour éviter surcharge API
- ✅ 15 joueurs célèbres pré-configurés (Notch, Dream, Technoblade, etc.)
- ✅ Récupération asynchrone (pas de freeze)
- ✅ Fallback automatique en cas d'erreur
- ✅ GameProfile complet avec textures

#### Utilisation:

```java
// Appliquer un skin aléatoire de joueur célèbre
MojangSkinFetcher.applyRandomFamousSkin(bot);

// Récupérer un profil spécifique
UUID uuid = MojangSkinFetcher.getFamousPlayerUUID("Dream");
CompletableFuture<GameProfile> profile = MojangSkinFetcher.fetchProfileAsync(uuid);
```

#### Base de données de joueurs célèbres:
- **Notch** - `069a79f4-44e9-4726-a5be-fca90e38aaf5`
- **Dream** - `ec561538-f3fd-461d-aff5-086b22154bce`
- **Technoblade** - `e6b5c088-0680-44df-9e1b-9bf11792291b`
- **GeorgeNotFound** - `f7c77d99-9f15-4a66-a87d-c4a51ef30d19`
- ... et 11 autres

---

## 💾 2. Base de Données Persistante

### Fichier: `BotDatabase.java`

**Format:** JSON (lisible et extensible)

#### Données sauvegardées par bot:

**Identité:**
- UUID du bot
- UUID du joueur Minecraft (pour skin)
- Nom, skin
- Date de création, dernière activité

**Position & Groupe:**
- Position (X, Y, Z)
- Position home
- Dimension
- Groupe, UUID du leader

**État & Comportement:**
- État IA (IDLE, FOLLOWING, ATTACKING, etc.)
- Rôle (SOLDIER, SCOUT, GUARD, MEDIC, ENGINEER, LEADER)
- Type de comportement
- Rayon de follow
- Mode hostile/statique

**Configuration:**
- `canPlaceBlocks` - Toggle construction
- `movementSpeed` - Vitesse de déplacement
- `chaseChance` - Probabilité de chase (0.0 - 1.0)
- `lookAroundChance` - Probabilité de regarder ailleurs (0.33 par défaut)

**Statistiques:**
- Blocs placés
- Distance parcourue
- Ennemis tués

#### Utilisation:

```java
// Initialiser la base de données (au démarrage du serveur)
BotDatabase.initialize(worldPath);

// Enregistrer un nouveau bot
BotDatabase.registerBot(bot);

// Mettre à jour un bot existant
BotDatabase.updateBot(bot);

// Appliquer les données sauvegardées à un bot
BotDatabase.applyDataToBot(bot);

// Sauvegarder manuellement
BotDatabase.saveDatabase();

// Auto-save (appeler toutes les 5 minutes)
BotDatabase.autoSave();
```

#### Emplacement du fichier:
```
world/data/aibrigade/bot_database.json
```

---

## 🎯 3. Follow Réaliste avec Probabilités

### Fichier: `RealisticFollowLeaderGoal.java`

**Remplace:** `FollowLeaderGoal.java`

#### Comportements implémentés:

**1. Follow dans un rayon avec positions aléatoires**
- Chaque bot a une position unique basée sur son UUID
- Les bots ne convergent PAS tous au même point
- Calcul de position: angle basé sur UUID + variation aléatoire
- Distance: 70%-90% du rayon max

**2. Probabilité de chase (configurable)**
- Par défaut: 70% de chance de chase actif
- Certains bots "hésitent" ou "traînent"
- Décision recalculée toutes les 2 secondes

**3. Variation de vitesse**
- Multiplica

teur: 0.85x - 1.15x (change toutes les 1.5 secondes)
- Boost si très loin: +30% ou +15%
- Effet organique et naturel

**4. Trajectoires courbes**
- Pas de lignes droites robotiques
- Vecteur perpendiculaire avec offset aléatoire
- Mise à jour tous les 0.75 secondes

**5. Pauses aléatoires**
- 5% de chance de pause à chaque tick
- Durée: 0.5 - 1.5 secondes
- Simule l'hésitation naturelle

#### Configuration:

```java
// Créer le goal avec paramètres
RealisticFollowLeaderGoal goal = new RealisticFollowLeaderGoal(
    bot,
    1.1D,   // Vitesse de base
    3.0F,   // Distance min
    10.0F   // Distance max
);

// Changer la probabilité de chase
goal.setChaseChance(0.8f); // 80%

// Vérifier si le bot est en train de chase
boolean isChasing = goal.isActivelyChasing();
```

---

## 👀 4. Regard Actif (2/6 Bots)

### Fichier: `ActiveGazeBehavior.java`

**Probabilité par défaut:** 33% (2/6 bots)

#### Machine à états:

**État 1: LOOKING_AT_LEADER**
- Regarde le leader par défaut
- Timer pour décider de regarder ailleurs

**État 2: SCANNING_AROUND**
- Scanne les alentours (2-5 secondes)
- Change de cible toutes les 0.75 - 2 secondes
- Regarde dans un angle de ±90° (pas derrière)
- Hauteur variable (±2 blocs)

**État 3: RETURNING_TO_LEADER**
- Retourne regarder le leader
- Rotation fluide et progressive

#### Configuration:

```java
// Créer le behavior
ActiveGazeBehavior gaze = new ActiveGazeBehavior(bot);

// Changer la probabilité
gaze.setLookAroundChance(0.5f); // 50% = 3/6 bots

// Changer l'intervalle (en ticks)
gaze.setLookAroundInterval(60); // 3 secondes

// Vérifier l'état
boolean isLooking = gaze.isLookingAround();
GazeState state = gaze.getGazeState();
```

---

## 🛠️ 5. Système d'Équipement Aléatoire

### Fichier: `RandomEquipment.java`

#### Types d'équipement:

**PICKAXE** (Pioche)
- Pierre, Fer, Or, Diamant

**SWORD** (Épée)
- Pierre, Fer, Or, Diamant

**FOOD** (Nourriture)
- Steak cuit

**BLOCKS** (Blocs x64)
- Terre, Cobblestone, Bois, Planches, Pierre, Cobbled Deepslate

**NOTHING** (Rien)
- Mains vides

#### Utilisation:

```java
// Équiper aléatoirement
RandomEquipment.equipRandomItem(bot);

// Équiper selon le rôle
RandomEquipment.equipByRole(bot);

// Équiper un type spécifique
RandomEquipment.equipSpecificType(bot, EquipmentType.SWORD);

// Donner des blocs supplémentaires
RandomEquipment.giveExtraBlocks(bot, 256);
```

#### Distribution par rôle:

| Rôle      | Équipement                           |
|-----------|--------------------------------------|
| SOLDIER   | Épée (aléatoire)                    |
| SCOUT     | Rien (mains libres)                 |
| GUARD     | 50% Épée / 50% Rien                 |
| ENGINEER  | 50% Pioche / 50% Blocs              |
| MEDIC     | Nourriture                          |
| LEADER    | Épée de diamant                     |

---

## 🎮 6. Commandes

### `/bot building on [botName]`

Active la construction de blocs.

**Exemples:**
```
/bot building on             → Active pour TOUS les bots
/bot building on ShadowBlade → Active pour ShadowBlade uniquement
```

### `/bot building off [botName]`

Désactive la construction de blocs.

**Exemples:**
```
/bot building off            → Désactive pour TOUS les bots
/bot building off DarkHunter → Désactive pour DarkHunter uniquement
```

---

## 🔧 7. Intégration dans BotEntity

### Modifications apportées:

**Nouvelles propriétés:**
```java
private UUID playerUUID;           // UUID pour skin Mojang
private boolean canPlaceBlocks;    // Toggle construction
```

**Constructeur mis à jour:**
```java
public BotEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
    // ... code existant ...

    // Appliquer skin Mojang aléatoire
    MojangSkinFetcher.applyRandomFamousSkin(this);

    // Équiper selon le rôle
    RandomEquipment.equipByRole(this);
}
```

**Sauvegarde NBT:**
```java
// Save
tag.putUUID("PlayerUUID", playerUUID);
tag.putBoolean("CanPlaceBlocks", canPlaceBlocks);

// Load
playerUUID = tag.getUUID("PlayerUUID");
canPlaceBlocks = tag.getBoolean("CanPlaceBlocks");
```

---

## 📊 8. Architecture du Système

```
┌─────────────────────────────────────────────────────┐
│                    BotEntity                         │
│  - UUID bot                                          │
│  - UUID joueur (skin Mojang)                        │
│  - Équipement                                        │
│  - Configuration comportementale                     │
└──────────────┬──────────────────────────────────────┘
               │
               ├─► MojangSkinFetcher
               │   └─► API Mojang Session Server
               │       └─► GameProfile + Textures
               │
               ├─► BotDatabase (JSON)
               │   └─► Persistance complète
               │       └─► Auto-save toutes les 5 min
               │
               ├─► RealisticFollowLeaderGoal
               │   ├─► Probabilité de chase
               │   ├─► Variation de vitesse
               │   ├─► Trajectoires courbes
               │   └─► Pauses aléatoires
               │
               ├─► ActiveGazeBehavior
               │   ├─► 33% regardent ailleurs
               │   ├─► Scanner fluide
               │   └─► Retour au leader
               │
               ├─► RandomEquipment
               │   └─► Équipement par rôle
               │
               └─► PlaceBlockToReachTargetGoal
                   └─► Vérifie canPlaceBlocks()
```

---

## 🚀 9. Utilisation Complète

### Créer un bot avec le système complet:

```java
// 1. Créer l'entité bot
BotEntity bot = new BotEntity(ModEntities.BOT_ENTITY.get(), level);

// 2. Le système s'initialise automatiquement:
//    - UUID Mojang aléatoire appliqué
//    - Skin récupéré de l'API
//    - Équipement selon le rôle
//    - Enregistrement dans la base de données

// 3. Configurer le comportement
bot.setRole(BotEntity.BotRole.SCOUT);
bot.setFollowingLeader(true);
bot.setLeaderId(player.getUUID());
bot.setFollowRadius(15.0f);

// 4. Configurer les probabilités (optionnel)
BotDatabase.BotData data = BotDatabase.getBotData(bot.getUUID());
data.chaseChance = 0.8f;         // 80% de chance de chase
data.lookAroundChance = 0.5f;    // 50% de chance de regarder ailleurs

// 5. Spawn dans le monde
level.addFreshEntity(bot);

// 6. Les données sont automatiquement sauvegardées
```

### Gestion de la construction:

```java
// Désactiver la construction pour tous les bots d'un groupe
for (BotEntity bot : botManager.getBotsByGroup("builders")) {
    bot.setCanPlaceBlocks(false);
}

// Réactiver pour les engineers uniquement
for (BotEntity bot : botManager.getAllBots()) {
    if (bot.getRole() == BotEntity.BotRole.ENGINEER) {
        bot.setCanPlaceBlocks(true);
    }
}
```

---

## 🔍 10. Dépendances Requises

### build.gradle

```gradle
dependencies {
    // SmartBrainLib - IA avancée
    implementation fg.deobf("curse.maven:smartbrainlib-661293:5654964")

    // Easy NPC - Gestion NPCs
    implementation fg.deobf("curse.maven:easy-npc-559312:5689125")

    // LibX - Utilitaires
    implementation fg.deobf("curse.maven:libx-412525:4947474")
}
```

---

## ⚙️ 11. Configuration Recommandée

### Paramètres par défaut optimaux:

```java
// Follow system
followRadius = 10.0F;
chaseChance = 0.7F;              // 70%
speedModifier = 1.1D;

// Gaze system
lookAroundChance = 0.33F;        // 33% = 2/6
lookAroundInterval = 40;         // 2 secondes

// Building system
canPlaceBlocks = true;

// Movement
movementSpeed = 0.35D;           // Vitesse normale
currentSpeedMultiplier = 0.9-1.1 // Variation
```

---

## 📝 12. Registre des Goals

### Ordre recommandé (priorité):

```java
@Override
protected void registerGoals() {
    // 0. Float in water
    this.goalSelector.addGoal(0, new FloatGoal(this));

    // 1. Active gaze behavior (regard actif)
    this.goalSelector.addGoal(1, new ActiveGazeBehavior(this));

    // 2. Realistic follow leader
    this.goalSelector.addGoal(2, new RealisticFollowLeaderGoal(this, 1.1D, 3.0F, 10.0F));

    // 3. Place blocks to reach target
    this.goalSelector.addGoal(3, new PlaceBlockToReachTargetGoal(this));

    // 4. Melee attack
    this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2D, false));

    // 5. Random stroll
    this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));

    // 6. Look at player
    this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
}
```

---

## ✅ 13. Checklist d'Intégration

- [x] MojangSkinFetcher.java créé
- [x] BotDatabase.java créé
- [x] RealisticFollowLeaderGoal.java créé
- [x] ActiveGazeBehavior.java créé
- [x] RandomEquipment.java créé
- [x] BotBuildingCommands.java créé
- [x] BotEntity modifié (UUID, canPlaceBlocks)
- [x] PlaceBlockToReachTargetGoal modifié (vérification toggle)
- [ ] Compiler le projet
- [ ] Tester en jeu
- [ ] Enregistrer les commandes dans le mod principal

---

## 🎯 14. Prochaines Étapes

1. **Compiler** le projet pour vérifier qu'il n'y a pas d'erreurs
2. **Enregistrer** BotBuildingCommands dans AIBrigadeMod
3. **Remplacer** FollowLeaderGoal par RealisticFollowLeaderGoal dans BotEntity
4. **Ajouter** ActiveGazeBehavior dans registerGoals()
5. **Initialiser** BotDatabase au démarrage du serveur
6. **Tester** en créant des bots et en vérifiant:
   - Les skins Mojang s'appliquent correctement
   - Les bots suivent avec variation
   - 2/6 regardent ailleurs
   - /bot building on/off fonctionne
   - L'équipement est adapté au rôle
   - Les données persistent après redémarrage

---

## 📞 Support

Si tu as des questions ou besoin d'aide pour intégrer ce système:
1. Vérifie que toutes les dépendances sont installées
2. Assure-toi que les imports sont corrects
3. Teste chaque système individuellement
4. Consulte les logs pour les erreurs d'API Mojang

**Bon codage! 🚀**
