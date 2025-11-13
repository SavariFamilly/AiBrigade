# 🤖 AIBrigade - Guide d'intégration des mods

Ce guide explique comment utiliser AIBrigade avec l'écosystème de mods recommandé pour créer des bots ultra-intelligents.

## 📦 Architecture du système

```
┌─────────────────────────────────────────────────────────┐
│                    🎮 MINECRAFT 1.20.1                  │
└─────────────────────────────────────────────────────────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
┌───▼────┐          ┌─────▼─────┐          ┌────▼────┐
│ Forge  │          │ AIBrigade │          │  LibX   │
│47.3.0  │          │   1.0.0   │          │ 5.0.12  │
└────────┘          └─────┬─────┘          └─────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    ┌────▼─────┐    ┌────▼────┐    ┌─────▼──────┐
    │SmartBrain│    │Easy NPC │    │ Your Mods  │
    │Lib 1.15  │    │  5.9.2  │    │ (Optional) │
    └──────────┘    └─────────┘    └────────────┘
```

## 🧩 Composants installés

### ✅ Dépendances intégrées (automatiques)

| Mod | Version | Rôle | Status |
|-----|---------|------|--------|
| **SmartBrainLib** | 1.15 | 🧠 Intelligence comportementale avancée | ✅ Installé |
| **Easy NPC** | 5.9.2 | 👤 Personnalisation des bots (skins, dialogues) | ✅ Installé |
| **LibX** | 5.0.12 | ⚙️ Bibliothèque utilitaire | ✅ Installé |

### 📥 Mods optionnels (à installer manuellement)

Ces mods doivent être **téléchargés et placés** dans votre dossier `mods/` :

| Mod | Version | Rôle | Lien |
|-----|---------|------|------|
| **AIOT Bot Mod** | 1.20.1 | 🦾 Actions physiques avancées (mining, farming) | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/aiot-botania) |
| **CC: Tweaked** | 1.20.1 | 💻 Programmation Lua pour automatisation | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/cc-tweaked) |

---

## 🎯 Cas d'usage par composant

### 1️⃣ SmartBrainLib - Intelligence comportementale

**Ce qu'il apporte :**
- Système de "sensors" (capteurs) pour détecter l'environnement
- Système de "behaviors" (comportements) modulaires
- Système de "memory" pour que les bots se souviennent
- Priorisation automatique des tâches

**Comment l'utiliser :**

```java
// Utiliser SmartBrainBotEntity au lieu de BotEntity
SmartBrainBotEntity bot = new SmartBrainBotEntity(entityType, level);

// Le bot aura automatiquement :
// - Détection des joueurs à proximité
// - Détection des ennemis
// - Réaction aux attaques
// - Comportements de combat/idle intelligents
```

**Exemple de logique SmartBrain :**
```
Bot détecte un joueur (NearbyPlayersSensor)
  ↓
Si joueur = leader → Activer FollowLeaderGoal (vanilla)
Si joueur = ennemi → Activer FightTasks (SmartBrain)
Sinon → Activer IdleTasks (SmartBrain)
```

---

### 2️⃣ Easy NPC - Personnalisation visuelle

**Ce qu'il apporte :**
- Skins personnalisés pour chaque bot
- Système de dialogues
- Système de trading/commerce
- Poses et animations custom

**Comment l'utiliser :**

```java
// Dans BotEntity
public void syncWithEasyNPC() {
    // Easy NPC fournit l'apparence
    // AIBrigade fournit les comportements

    // Exemples de customisation :
    // - Définir un skin unique par bot
    // - Créer des dialogues basés sur le rôle du bot
    // - Ajouter un shop pour acheter/vendre items
}
```

**Workflow :**
1. Utilisez Easy NPC GUI pour customiser l'apparence
2. AIBrigade gère automatiquement les comportements
3. Le bot a une apparence unique + IA avancée

---

### 3️⃣ AIOT Bot Mod - Actions physiques (optionnel)

**Ce qu'il apporte :**
- Mining automatique avec pioche
- Farming automatique (récolte/replante)
- Construction de structures complexes
- Gestion d'inventaire avancée

**Quand l'utiliser :**
- ❌ **PAS nécessaire** si vous voulez juste que les bots combattent/suivent
- ✅ **Utile** si vous voulez que les bots minent/farm pour vous
- ✅ **Utile** si vous voulez automatiser la collecte de ressources

**Comment l'intégrer :**

```java
// Vérifier si AIOT Bot est installé
if (ModList.get().isLoaded("aiotbotania")) {
    // Utiliser l'API AIOT Bot pour miner
    AIOTBotAPI.setTask(bot, AIOTTask.MINE_AREA, position);
}

// Notre PlaceBlockToReachTargetGoal gère déjà :
// - Placement de blocs pour navigation
// - Construction d'escaliers/ponts
// - Évasion de salles fermées
```

**Différence avec notre système :**
- **AIBrigade** : Placement de blocs pour **navigation/combat**
- **AIOT Bot** : Placement de blocs pour **construction/farming**

---

### 4️⃣ CC: Tweaked - Programmation avancée (optionnel)

**Ce qu'il apporte :**
- Turtles programmables en Lua
- Scripts complexes pour automation
- Contrôle précis des mouvements
- Détection de blocs avancée

**Quand l'utiliser :**
- ✅ **Utile** pour des patterns de construction complexes
- ✅ **Utile** pour de l'automation répétitive
- ❌ **PAS nécessaire** pour le combat/suivi basique

**Exemple d'intégration :**

```lua
-- Script Lua CC:Tweaked
-- Appeler ce script depuis AIBrigade quand un bot est bloqué

function buildBridge(length)
    for i = 1, length do
        turtle.placeDown()  -- Place block
        turtle.forward()    -- Move forward
    end
end

-- Recevoir des commandes d'AIBrigade via rednet
rednet.open("back")
while true do
    local senderId, message = rednet.receive()
    if message == "BUILD_BRIDGE" then
        buildBridge(10)
        rednet.send(senderId, "DONE")
    end
end
```

**Comment AIBrigade communique avec CC:Tweaked :**

```java
// Envoyer une commande à un turtle CC:Tweaked
public void requestTurtleHelp(BlockPos position) {
    if (ModList.get().isLoaded("computercraft")) {
        // Trouver un turtle proche
        // Envoyer commande "BUILD_BRIDGE"
        // Attendre réponse "DONE"
        // Continuer le pathfinding
    }
}
```

---

## 🔧 Configuration recommandée par scénario

### Scénario 1 : Bot de combat simple
```
✅ AIBrigade (core)
✅ SmartBrainLib (IA avancée)
✅ Easy NPC (skins personnalisés)
❌ AIOT Bot (pas nécessaire)
❌ CC:Tweaked (pas nécessaire)
```

### Scénario 2 : Bot mineur/farmer
```
✅ AIBrigade (core)
✅ SmartBrainLib (IA avancée)
✅ Easy NPC (skins personnalisés)
✅ AIOT Bot (mining/farming)
❌ CC:Tweaked (optionnel pour patterns complexes)
```

### Scénario 3 : Bot architecte/constructeur
```
✅ AIBrigade (core)
✅ SmartBrainLib (IA avancée)
✅ Easy NPC (skins personnalisés)
✅ AIOT Bot (construction)
✅ CC:Tweaked (patterns complexes)
```

### Scénario 4 : Armée de bots (300+)
```
✅ AIBrigade (core)
✅ SmartBrainLib (IA avancée)
⚠️ Easy NPC (peut ralentir avec 300+ bots)
❌ AIOT Bot (ralentit les performances)
❌ CC:Tweaked (ralentit les performances)
```

---

## 🚀 Démarrage rapide

### Installation minimale (recommandée)
1. AIBrigade s'installe automatiquement avec :
   - SmartBrainLib 1.15
   - Easy NPC 5.9.2
   - LibX 5.0.12

2. Lancez Minecraft
3. Créez vos bots avec `/botspawn`
4. Les bots utilisent automatiquement SmartBrainLib

### Installation complète (avancée)
1. Téléchargez et installez manuellement :
   - AIOT Bot Mod (si vous voulez mining/farming)
   - CC: Tweaked (si vous voulez automation Lua)

2. Placez les JARs dans `/mods/`

3. AIBrigade détectera automatiquement ces mods

---

## 🧠 Logique de décision du bot

Voici comment un bot décide quoi faire :

```
┌─────────────────────────────────────────┐
│   Bot reçoit objectif (ex: suivre joueur) │
└──────────────┬──────────────────────────┘
               │
        ┌──────▼──────┐
        │ SmartBrainLib│
        │ évalue       │
        └──────┬───────┘
               │
    ┌──────────┴───────────┐
    │                      │
┌───▼────┐          ┌─────▼──────┐
│Capteurs│          │  Mémoire   │
│actifs? │          │ du bot     │
└───┬────┘          └─────┬──────┘
    │                     │
    └──────────┬──────────┘
               │
        ┌──────▼────────┐
        │ Comportement  │
        │ sélectionné   │
        └──────┬────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
┌───▼───┐  ┌──▼───┐  ┌──▼────┐
│Vanilla│  │Smart │  │Custom │
│Goals  │  │Brain │  │Action │
└───┬───┘  └──┬───┘  └──┬────┘
    │         │         │
    └─────────┴─────────┘
              │
       ┌──────▼──────┐
       │ Bot exécute │
       │   l'action  │
       └─────────────┘
```

**Exemple concret :**

```
Situation : Joueur à 50 blocs, mur de 5 blocs de haut entre le bot et le joueur

1. SmartBrainLib détecte :
   - NearbyPlayersSensor : Joueur = leader à 50 blocs
   - PathBlockedSensor : Chemin bloqué par mur

2. Décision :
   - Priorité 1 : FollowLeaderGoal (vanilla) - ACTIF
   - Détection : Chemin bloqué
   - Trigger : PlaceBlockToReachTargetGoal (vanilla)

3. Exécution :
   - Bot calcule : Mur = 5 blocs haut
   - Bot choisit : Mode "Escaliers diagonaux"
   - Bot construit : 5 blocs en escalier
   - Bot monte : Franchit le mur
   - Bot continue : Rejoint le joueur
```

---

## ⚡ Performance et optimisation

### Consommation de ressources

| Composant | RAM | CPU | Impact réseau |
|-----------|-----|-----|---------------|
| AIBrigade core | Faible | Moyen | Faible |
| SmartBrainLib | Moyen | Faible | Aucun |
| Easy NPC | Moyen | Faible | Moyen |
| AIOT Bot | Élevé | Élevé | Moyen |
| CC: Tweaked | Faible | Moyen | Faible |

### Recommandations

**Pour 10-50 bots :**
- Tous les mods peuvent être utilisés sans problème

**Pour 50-150 bots :**
- Désactiver AIOT Bot si pas utilisé
- Limiter les scripts CC:Tweaked

**Pour 150-300+ bots :**
- Utiliser uniquement AIBrigade + SmartBrainLib
- Désactiver Easy NPC skins si lag
- Optimiser les AI tasks

---

## 🐛 Dépannage

### SmartBrainLib ne charge pas
```
Erreur : "Missing SmartBrainLib dependency"
Solution : Réinstaller AIBrigade, SmartBrainLib s'installe automatiquement
```

### Bots ne placent pas de blocs
```
Problème : PlaceBlockToReachTargetGoal ne s'active pas
Solutions :
1. Vérifier que le bot a des blocs dans l'offhand
2. Vérifier que la distance au leader est entre 3-50 blocs
3. Vérifier les logs pour erreurs
```

### CC:Tweaked turtles ne répondent pas
```
Problème : Commandes rednet non reçues
Solutions :
1. Vérifier que le turtle a un modem (crafté avec)
2. Vérifier que rednet.open() est appelé
3. Vérifier les IDs des ordinateurs
```

---

## 📚 Ressources supplémentaires

- **AIBrigade Wiki** : [Documentation complète]
- **SmartBrainLib Wiki** : https://wiki.tslat.com/SmartBrainLib
- **Easy NPC Wiki** : https://github.com/MarkusBordihn/BOs-Easy-NPC
- **CC: Tweaked Docs** : https://tweaked.cc/
- **AIOT Bot Docs** : [CurseForge page]

---

## 🎓 Tutoriel complet

Consultez `TUTORIAL.md` pour un tutoriel pas-à-pas de création d'un bot intelligent qui :
1. Suit le joueur
2. Construit des ponts automatiquement
3. Combat les ennemis
4. Mine des ressources (avec AIOT Bot)
5. Exécute des scripts custom (avec CC:Tweaked)

---

**Backup créé dans** : `C:\Users\magnu\Documents\AIBrigade\backup\`
**Version actuelle** : AIBrigade 1.0.0 avec SmartBrainLib 1.15 + Easy NPC 5.9.2
