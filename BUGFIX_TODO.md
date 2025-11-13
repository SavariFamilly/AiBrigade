# BUGS À CORRIGER - AIBrigade

## Status Actuel
- ✅ Bots se déplacent (goals AI ajoutés)
- ✅ Bots ne despawnent plus
- ✅ Noms générés correctement
- ❌ Armure ne s'applique PAS
- ❌ Bots ne s'attaquent PAS (hostile ne fonctionne pas)
- ❌ Pas d'équipement de départ

---

## 1. GIVEARMOR NE FONCTIONNE PAS

### Problème
La commande `/aibrigade givearmor TestBot diamond` ne fait rien.

### Cause Probable
La méthode `botManager.giveArmor()` dans BotManager.java ne met pas réellement l'armure sur le bot.

### Solution Requise
Modifier `BotManager.giveArmor()` pour:
1. Créer les ItemStack d'armure (casque, plastron, jambières, bottes)
2. Appeler `bot.setItemSlot(EquipmentSlot.HEAD/CHEST/LEGS/FEET, armorItem)`
3. S'assurer que les slots d'équipement sont synchronisés client-serveur

### Fichier à Modifier
`src/main/java/com/aibrigade/bots/BotManager.java` - méthode `giveArmor()`

---

## 2. HOSTILE NE FAIT PAS ATTAQUER

### Problème
`/aibrigade hostile Group1 true` ne fait pas attaquer les bots.

### Cause
Les bots ont `MeleeAttackGoal` mais pas de target selector pour trouver des ennemis.

### Solution Requise
Dans `BotEntity.registerGoals()`:
```java
// Ajouter ces target selectors:
this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, BotEntity.class, true));
```

Modifier aussi la commande hostile pour activer/désactiver ces goals.

### Fichiers à Modifier
- `src/main/java/com/aibrigade/bots/BotEntity.java` - méthode `registerGoals()`
- `src/main/java/com/aibrigade/bots/BotManager.java` - méthode pour gérer l'hostilité

---

## 3. ÉQUIPEMENT DE DÉPART MANQUANT

### Requis
Chaque bot spawn doit avoir:
- **128 planches de bois** (oak planks) dans l'inventaire
- **1 épée aléatoire**: 1/3 pierre, 1/3 fer, 1/3 diamant

### Solution
Dans `BotManager.spawnBot()`, après la ligne `bot.setPos(...)`:

```java
// Équipement de départ
Random rand = new Random();

// 128 planches de bois
ItemStack planks = new ItemStack(Items.OAK_PLANKS, 128);
bot.getInventory().addItem(planks); // Nécessite d'ajouter un système d'inventaire

// Épée aléatoire (1/3 chaque)
int swordType = rand.nextInt(3);
ItemStack sword;
if (swordType == 0) {
    sword = new ItemStack(Items.STONE_SWORD);
} else if (swordType == 1) {
    sword = new ItemStack(Items.IRON_SWORD);
} else {
    sword = new ItemStack(Items.DIAMOND_SWORD);
}
bot.setItemSlot(EquipmentSlot.MAINHAND, sword);
```

### Problème
BotEntity n'a PAS de méthode `getInventory()` - il faut l'ajouter!

### Fichiers à Modifier
- `src/main/java/com/aibrigade/bots/BotEntity.java` - ajouter système d'inventaire
- `src/main/java/com/aibrigade/bots/BotManager.java` - méthode `spawnBot()`

---

## 4. BOTS NE FONT QUE REGARDER AUTOUR

### Problème
Bots ont des goals mais ne font que `RandomLookAroundGoal`.

### Cause
Les autres goals (WaterAvoidingRandomStrollGoal) ont peut-être une priorité trop basse ou des conditions qui ne sont jamais satisfaites.

### Solution
Réorganiser les priorités des goals:
```java
this.goalSelector.addGoal(0, new FloatGoal(this));
this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D)); // Vitesse augmentée
this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
```

Augmenter la vitesse de déplacement dans les attributs:
```java
.add(Attributes.MOVEMENT_SPEED, 0.35D) // Au lieu de 0.3D
```

### Fichier à Modifier
`src/main/java/com/aibrigade/bots/BotEntity.java`

---

## ORDRE DE PRIORITÉ DES FIXES

1. **CRITIQUE**: Équipement de départ (épée + planches)
2. **HAUTE**: GiveArmor fonctionne
3. **HAUTE**: Hostile fait attaquer
4. **MOYENNE**: Bots se déplacent plus

---

## COMMANDE MANQUANTE À AJOUTER

### /aibrigade giveitem

```java
.then(Commands.literal("giveitem")
    .then(Commands.argument("target", StringArgumentType.string())
        .then(Commands.argument("item", ItemArgument.item(context))
            .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                .executes(BotCommandHandler::giveItem)))))
```

Cette commande permettra de donner n'importe quel item aux bots.

### Fichier à Modifier
`src/main/java/com/aibrigade/commands/BotCommandHandler.java`

---

## NOTES TECHNIQUES

### Inventaire des Bots
PathfinderMob n'a PAS d'inventaire par défaut. Il faut soit:
- Option A: Créer un `SimpleContainer` dans BotEntity
- Option B: Utiliser les slots d'équipement (limité à 6 items: 4 armures + 2 mains)

Pour 128 planches, il FAUT un inventaire complet (Option A).

### Synchronisation Client-Serveur
Quand on change l'équipement d'un bot:
```java
bot.setItemSlot(EquipmentSlot.HEAD, helmet);
// Minecraft synchronise automatiquement via packets
```

Mais pour l'inventaire personnalisé, il faudra peut-être synchroniser manuellement.

---

## ESTIMATION DES MODIFICATIONS

- Équipement de départ: **30 minutes**
- Fix GiveArmor: **20 minutes**
- Fix Hostile/Attack: **15 minutes**
- Améliorer mouvement: **5 minutes**
- Commande GiveItem: **25 minutes**

**TOTAL: ~1h30**
