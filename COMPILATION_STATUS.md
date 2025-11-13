# AIBrigade Mod - État de Compilation

## Statut Actuel: ⚠️ EN COURS DE CORRECTION

Le mod AIBrigade a été créé pour Minecraft 1.21.8 avec Forge, mais rencontre actuellement des problèmes de compilation dus aux changements significatifs dans les APIs entre les versions.

## Corrections Déjà Effectuées ✅

### 1. ModEntities.java
- **Problème**: `EntityType.Builder.build()` ne prend plus de String
- **Solution**: Changé `build("bot")` en `build(null)`

### 2. AIBrigadeMod.java
- **Problème**: `IEventBus` et les APIs d'enregistrement ont changé
- **Solution**: Mis à jour pour utiliser le nouveau constructeur avec paramètres `IEventBus` et `ModContainer`

### 3. BotCommandHandler.java
- **Problème**: `ServerPlayer.serverLevel()` n'existe plus
- **Solution**: Remplacé par `(ServerLevel) player.level()`

### 4. EntityLibWrapper.java
- **Problème**: Plusieurs méthodes ont changé leurs signatures:
  - `BlockState.isSolidRender()` ne prend plus de paramètres
  - `Level.getMaxBuildHeight()` → `Level.getMaxY()`
  - `Level.getMinBuildHeight()` → `Level.getMinY()`
- **Solution**: Mis à jour tous les appels de méthodes

### 5. ResourceLocation
- **Problème**: Le constructeur `new ResourceLocation(namespace, path)` est privé
- **Solution**: Remplacé par `ResourceLocation.fromNamespaceAndPath(namespace, path)`

## Problèmes Restants À Corriger ❌

### 1. Dépendance GeckoLib Non Trouvée
**Fichiers affectés**: BotEntity.java, BotModel.java, BotRenderer.java, ClientEventHandler.java

**Erreur**: `package software.bernie.geckolib.* does not exist`

**Cause**: La version 1.21.8 est très récente et GeckoLib pourrait ne pas encore avoir de version stable pour cette version de Minecraft.

**Solutions possibles**:
1. Vérifier si une version de GeckoLib pour 1.21.8 existe sur CurseForge/Modrinth
2. Modifier le `build.gradle` pour utiliser une version compatible
3. Implémenter temporairement un système d'animation basique sans GeckoLib
4. Revenir à Minecraft 1.21.1 où toutes les dépendances sont disponibles

**Fichiers temporairement désactivés**:
- `BotModel.java` - remplacé par une classe placeholder
- `BotRenderer.java` - remplacé par une classe placeholder
- `ClientEventHandler.java` - méthode registerRenderers commentée
- `BotEntity.java` - tout le code GeckoLib commenté

### 2. EventBus API Changes
**Fichiers affectés**: AIManager.java, Plusieurs autres

**Erreur**: `package net.minecraftforge.eventbus.api does not exist`

**Problème**: Le package EventBus a peut-être changé dans Forge 1.21.8

**À corriger**:
```java
// Ancien import (ne fonctionne pas)
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Nouveau import à tester
import net.minecraftforge.event.EventBusSubscriber;
import net.minecraftforge.common.MinecraftForge;
```

### 3. BotEntity - API Entity Changes
**Fichiers affectés**: BotEntity.java

**Problèmes multiples**:

#### A. defineSynchedData() signature changée
```java
// Ancien (ne fonctionne pas)
@Override
protected void defineSynchedData() {
    super.defineSynchedData();
    this.entityData.define(BOT_NAME, "Bot");
}

// Nouveau (à implémenter)
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(BOT_NAME, "Bot");
}
```

#### B. NBT Methods Changed
```java
// Ancien
@Override
public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putString("BotName", getBotName());
}

// Nouveau (à vérifier l'API exacte)
@Override
public void addAdditionalSaveData(CompoundTag tag, HolderLookup.Provider provider) {
    super.addAdditionalSaveData(tag, provider);
    tag.putString("BotName", getBotName());
}
```

#### C. Lecture NBT avec Optional
```java
// Ancien
setBotName(tag.getString("BotName"));

// Nouveau
tag.getString("BotName").ifPresent(this::setBotName);
// OU
setBotName(tag.getString("BotName").orElse("Bot"));
```

### 4. BotGoals.java
**Fichier affecté**: BotGoals.java

**Erreur**: `doHurtTarget()` signature changée

```java
// Ancien
bot.doHurtTarget(target);

// Nouveau
bot.doHurtTarget((ServerLevel) bot.level(), target);
```

### 5. SmartBrainLib Non Trouvée
**Fichier affecté**: SmartBrainIntegration.java

**Cause**: Similaire à GeckoLib, la bibliothèque pourrait ne pas être disponible pour 1.21.8

## Ordre de Corrections Recommandé

1. **PRIORITÉ 1**: Résoudre les problèmes d'EventBus
   - Trouver les bons imports pour Forge 1.21.8
   - Mettre à jour tous les fichiers utilisant @SubscribeEvent

2. **PRIORITÉ 2**: Corriger BotEntity.java
   - Mettre à jour defineSynchedData() avec Builder
   - Corriger toutes les méthodes NBT (add/readAdditionalSaveData)
   - Utiliser Optional pour la lecture NBT

3. **PRIORITÉ 3**: Corriger BotGoals.java
   - Mettre à jour tous les appels à doHurtTarget()
   - Vérifier les autres méthodes d'attaque

4. **PRIORITÉ 4**: Décider pour GeckoLib
   - Option A: Trouver version compatible pour 1.21.8
   - Option B: Downgrade vers 1.21.1
   - Option C: Implémenter animations simples sans GeckoLib

5. **PRIORITÉ 5**: Tester compilation complète
   - Corriger les erreurs restantes au fur et à mesure
   - Tester le build final

6. **PRIORITÉ 6**: Tests runtime
   - Lancer le client
   - Tester spawn des bots
   - Corriger les erreurs d'exécution

## Commandes Utiles

### Compiler seulement le Java
```bash
.\gradlew.bat compileJava --no-daemon
```

### Build complet
```bash
.\gradlew.bat clean build --no-daemon
```

### Lancer le client Minecraft
```bash
.\gradlew.bat runClient --no-daemon
```

### Voir les erreurs détaillées
```bash
.\gradlew.bat compileJava --no-daemon --stacktrace
```

## Ressources Utiles

- [Forge 1.21 Breaking Changes](https://docs.minecraftforge.net/en/1.21.x/gettingstarted/)
- [GeckoLib Documentation](https://github.com/bernie-g/geckolib)
- [Minecraft Forge Forums](https://forums.minecraftforge.net/)

## Notes

- Le mod est structuré correctement avec tous les packages et classes
- La logique métier est en place (AI, commandes, gestion des bots)
- Seules les incompatibilités API bloquent la compilation
- Une fois ces problèmes résolus, le mod devrait fonctionner

## Prochaines Étapes

1. Rechercher documentation Forge 1.21.8 pour EventBus
2. Mettre à jour BotEntity selon nouvelle API
3. Vérifier disponibilité GeckoLib/SmartBrainLib pour 1.21.8
4. Si dépendances indisponibles, considérer downgrade vers 1.21.1
