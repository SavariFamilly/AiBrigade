# Instructions de test pour AIBrigade

## ⚠️ IMPORTANT : Relancez le jeu !
Le mod a été recompilé avec les corrections suivantes :
- ✅ Équipements variés (pioches fer/diamant, épées fer/diamant, steak, rien)
- ✅ Pseudos Mojang réels (Notch, jeb_, Dream, etc.)
- ✅ Système de follow 5/6 vs 1/6
- ✅ **NOUVEAU** : Nettoyage automatique des bots morts

Vous devez **arrêter et relancer Minecraft** pour que les changements prennent effet.

## 🧪 Tests à effectuer

### 1. Test des équipements aléatoires

Supprimez tous les bots existants :
```
/aibrigade removegroup testgroup
```

Créez un nouveau groupe de 20 bots :
```
/aibrigade spawn group 20 <VotreNom> follow 10.0 false testgroup
```

**Vérification attendue :**
- Les bots doivent avoir des objets variés (pas tous des épées)
- Distribution : 20% rien, 15% pioche fer, 10% pioche diamant, 20% steak, 20% épée fer, 15% épée diamant
- Regardez dans les logs du serveur pour voir `[RandomEquipment] Bot équipé avec: ...`

### 2. Test des pseudos Mojang

**Vérification attendue :**
- Les bots doivent avoir des pseudos de joueurs célèbres : Notch, jeb_, Dream, TommyInnit, Philza, etc.
- Pas de noms comme "Sarah Ramirez" ou autres noms génériques
- Regardez dans les logs pour voir `[MojangSkinFetcher] Bot configuré avec pseudo: ...`

### 3. Test du système de follow (5/6 vs 1/6)

Activez le follow avec un radius de 15 blocs :
```
/aibrigade followleader testgroup true 15.0
```

**Vérification attendue :**
- Le message doit indiquer : "5/6 bots follow in radius, 1/6 follow actively"
- Regardez dans les logs pour voir les messages `[RealisticFollowLeaderGoal] Bot X configured with behavior: ...`
- Environ 3-4 bots (1/6 de 20) doivent vous suivre de très près (2-4 blocs)
- Les 16-17 autres doivent rester éparpillés dans le radius de 15 blocs

### 4. Test du mode statique

```
/aibrigade togglestatic testgroup
```

**Vérification attendue :**
- Les bots ne doivent plus bouger du tout
- Réexécutez la commande pour désactiver et ils doivent recommencer à bouger

## 🔍 Où trouver le mod compilé

Le fichier JAR est dans :
```
C:\Users\magnu\Documents\AIBrigade\build\libs\
```

Le fichier s'appelle : `aibrigade-<version>.jar`

### 5. Test du nettoyage automatique des bots morts (NOUVEAU!)

Vérifiez le compteur de bots :
```
/aibrigade listbots
```

Tuez quelques bots (par exemple avec /kill ou en combat)

Attendez 5 secondes, puis vérifiez à nouveau :
```
/aibrigade listbots
```

**Vérification attendue :**
- Le compteur doit diminuer automatiquement après quelques secondes
- Les bots morts ne doivent plus compter dans la limite
- Vous pouvez maintenant spawn de nouveaux bots même après avoir tué les 300

Commande manuelle de nettoyage (si besoin) :
```
/aibrigade cleanupbots
```

## 📋 Checklist de vérification

- [ ] Jeu relancé après recompilation
- [ ] Anciens bots supprimés
- [ ] Nouveaux bots créés
- [ ] Équipements variés observés
- [ ] Pseudos Mojang observés (Notch, jeb_, etc.)
- [ ] Commande followleader avec radius fonctionne
- [ ] 1/6 des bots suivent activement
- [ ] 5/6 des bots restent dans le radius
- [ ] Mode statique fonctionne
- [ ] **Bots morts sont automatiquement nettoyés**
- [ ] **Peut spawn après avoir tué des bots**

## 🐛 En cas de problème

Vérifiez les logs du jeu :
1. Dans le dossier `.minecraft/logs/latest.log`
2. Cherchez les messages avec `[RandomEquipment]`, `[MojangSkinFetcher]`, `[RealisticFollowLeaderGoal]`
3. Envoyez-moi les messages de log si quelque chose ne fonctionne pas
