# Ponder Animation Timeline


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH prend en charge Ponder-style animated timelines dans `<GameScene>` blocs. You supply an
externe JSON fichier que defines keyframes, caméra movements et dans-monde annotations, et
GuideNH renders Un interactive progress bar avec play/pause controls en dessous Le 3D scène.

## rapide début

1. créer Un Ponder JSON fichier et placer it dans votre ressource pack (Voir [File Placement](#file-placement)).
2. Ajoutez `<ImportPonder src="..."/>` dans a `<GameScene>` bloc alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **Remarque:** `<ImportPonder>` doit appear dans a `<GameScene>` bloc. Le `src` attribut est
> obligatoire. Structure données est encore fourni by `<ImportStructure>` ou `<ImportStructureLib>`.

## fichier Placement

Ponder JSON files follow Le même ressource-pack chemin Règles as SNBT structures:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
```

Le `src` attribut accepts les deux relatif et absolu IDs:

| Exemple | résolu as |
|---------|-------------|
| `src="my_machine.json"` | relatif vers Le actuel page's répertoire |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | absolu (`mymod` namespace) |

## JSON Format

```json
{
  "totalTime": 120,
  "keyframes": [
    {
      "time": 0,
      "label": "Start",
      "camera": {
        "zoom": 2.0,
        "rotX": 25.0,
        "rotY": -30.0,
        "rotZ": 0.0,
        "offX": 0.0,
        "offY": 0.0
      },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "Mid-point",
      "camera": {
        "rotY": -90.0
      },
      "annotations": [
        {
          "type": "diamond",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "color": "0xFFFF4444",
          "tooltip": "Input hatch",
          "alwaysOnTop": false
        }
      ]
    },
    {
      "time": 120,
      "camera": {
        "rotY": -150.0
      }
    }
  ]
}
```

### racine champs

| champ | type | obligatoire | description |
|-------|------|----------|-------------|
| `totalTime` | entier | oui | Total duration dans ticks (20 ticks = 1 second). Minimum clamped vers 1. |
| `keyframes` | array | oui | liste of keyframe objects. peut être vide. |

### Keyframe champs

| champ | type | obligatoire | description |
|-------|------|----------|-------------|
| `time` | entier | oui | Tick at qui ceci keyframe occurs (0 <= time <= totalTime). |
| `hidden` | booléen | non | lorsque `true`, Le keyframe encore s’applique caméra/NBT/entity/annotation état at son `time`, but non visible node est drawn pour it on Le progress bar et visible keyframe navigation skips it. |
| `label` | chaîne | non | facultatif repli étiquette affiché lorsque au survol Le keyframe node on Le progress bar. |
| `labelKey` | chaîne | non | traduction clé pour Le keyframe étiquette. lorsque résolu, it remplace `label`. |
| `camera` | object | non | caméra état at ceci keyframe. Null champs inherit depuis Le précédent keyframe. |
| `cameraEaseTicks` | entier ou null | non | How many ticks Le caméra takes vers ease depuis Le **précédent** keyframe vers ceci un. `null` (par défaut) = ease sur Le complet segment. `0` = instant snap. `N > 0` = ease sur N ticks, alors hold at Le cible position. |
| `layer` | entier ou null | non | visible layer remplacer. `null` (ou omitted) affiche tous layers. 1-based index. |
| `annotations` | array | non | liste of annotation objects affiché pendant ceci keyframe est active. |
| `sounds` | array | non | liste of sounds played once lorsque ceci keyframe devient active pendant forward playback. |
| `particles` | array | non | liste of exécution particle bursts ou presets fired lorsque ceci keyframe devient active pendant forward playback. |
| `blockChanges` | array | non | liste of bloc replacements applied lorsque ceci keyframe premier devient active. |
| `mergeTileNBT` | array | non | Merge SNBT compounds dans tile entities at bloc positions. |
| `modifyTileNBT` | array | non | Définissez un tile-entity NBT chemin vers Un SNBT valeur. |
| `removeTileNBT` | array | non | supprimer un tile-entity NBT chemin. |
| `createEntities` | array | non | créer Ponder-owned entities que peut être referenced by later entity NBT operations. |
| `setEntityNBT` | array | non | remplacer Un referenced entity's NBT avec Le supplied SNBT compound. |
| `mergeEntityNBT` | array | non | Merge Un SNBT compound dans Un referenced entity. |
| `modifyEntityNBT` | array | non | Définissez un referenced entity NBT chemin vers Un SNBT valeur. |
| `removeEntityNBT` | array | non | supprimer un referenced entity NBT chemin. |
| `removeEntities` | array | non | supprimer un ou plus Ponder-owned entities by `ref` using Le stable scène-entity registry. |

masquées keyframes sont useful lorsque you want additional intermediate état modifications sans adding Un new visible node
vers Le timeline. pour Exemple, you peut split several `modifyTileNBT` updates across plusieurs ticks, mark Le
intermediate keyframes as masquées, et Conservez seulement Le major beats visible on Le progress bar.

### caméra champs

tous caméra champs sont facultatif. quelconque `null` ou omitted champ inherits son valeur depuis Le nearest
prior keyframe que defined it; Si non prior keyframe defined Le champ, Le scène's par défaut
caméra valeur est utilisé.

| champ | type | description |
|-------|------|-------------|
| `zoom` | flottant | caméra zoom level (0.1 - 10.0). |
| `rotX` | flottant | X-axis rotation dans degrees. |
| `rotY` | flottant | Y-axis rotation dans degrees. |
| `rotZ` | flottant | Z-axis rotation dans degrees. |
| `offX` | flottant | Horizontal pan offset dans écran pixels. |
| `offY` | flottant | Vertical pan offset dans écran pixels. |

Le caméra smoothly interpolates entre adjacent keyframes using an **ease-dans/ease-out** courbe.
utiliser `cameraEaseTicks` on Le **destination** keyframe vers control Le easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
```

## bloc modifications

Le `blockChanges` array dans Un keyframe replaces blocs dans Le live structure lorsque que keyframe
devient active. ceci permet Le animation vers afficher avant-et-après states, placer ou supprimer
blocs, ou animate Un machine powering on.

```json
{
  "time": 60,
  "blockChanges": [
    { "x": 1, "y": 1, "z": 1, "block": "minecraft:lit_furnace", "meta": 4, "particles": true },
    { "x": 1, "y": 2, "z": 1, "block": "minecraft:air", "particles": false },
    {
      "x": 2, "y": 1, "z": 2, "block": "minecraft:chest", "meta": 2,
      "nbt": "{Items:[{Slot:0b,id:\"minecraft:iron_ingot\",Count:8b,Damage:0s}]}"
    }
  ]
}
```

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | entier | - | **obligatoire.** position of Le bloc vers modifier (structure coordonnées). |
| `block` | chaîne | - | **obligatoire.** Registry nom, e.g. `"minecraft:furnace"`. utiliser `"minecraft:air"` vers supprimer. |
| `meta` | entier | `0` | bloc metadata / damage valeur. |
| `particles` | booléen | `true` | Whether vers spawn bloc-texture particle effects lorsque ceci bloc modifier fires pendant forward playback. Particles sont taken depuis Le bloc's propre icon texture. Définissez vers `false` vers suppress (e.g., pour silent removal). |
| `nbt` | chaîne | `null` | SNBT chaîne pour Un tile entity balise, e.g. pour chests, furnaces, etc. analysé avec `JsonToNBT`. clés doit être **unquoted** (standard SNBT format). Ignored Si Le bloc a non tile entity. |

**Seek-safe:** lorsque seeking backwards Le exécution restores tous changed positions vers leurs
original structure état, alors re-s’applique modifications depuis keyframes 0 à travers Le actuel un.
Le displayed structure est toujours correct regardless of seek direction.

> **Remarque on particles:** bloc-texture particles fire once, seulement pendant forward playback lorsque Le
> keyframe premier devient active. Elles sont cleared on seek, restart, ou initial charger.

## Keyframe Sounds

Ajoutez a `sounds` array vers Un keyframe vers play un ou plus guide sounds lorsque Le keyframe devient active
pendant forward playback. Seeking et initial charger do ne play keyframe sounds; restart clears Le
play history so Le sounds peut fire again.

```json
{
  "time": 130,
  "label": "Furnace lights up",
  "sounds": [
    { "sound": "guidenh:machine.start", "volume": 0.8 },
    { "src": "guidenh:sounds/machine/hum.ogg", "volume": 0.4, "x": 1.5, "y": 1.5, "z": 1.5 }
  ]
}
```

Sound champs:

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `sound` | chaîne | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | chaîne | - | Sound fichier id ou chemin; `guidenh:sounds/machine/start.ogg` devient `guidenh:machine.start`. |
| `volume` | flottant | `1.0` | Playback volume avant attenuation. |
| `pitch` | flottant | `1.0` | Playback pitch. |
| `cooldown` | entier | `250` | Minimum milliseconds avant Le même sound peut play again. |
| `x`, `y`, `z` | flottant | none | facultatif scène-espace source position pour écran-espace attenuation. |
| `radius` | flottant | scène short côté * 0.75 | Attenuation radius dans écran pixels. |
| `minVolume` | flottant | `0.15` | Minimum attenuation factor. |

---

## Keyframe Particles

Ajoutez a `particles` array vers Un keyframe lorsque you want un-shot particle bursts ou timeline-local
weather overlays pendant forward playback. These particles sont ne re-fired pendant reverse
scrubbing, et seek/restart clears eux avant replaying Le active état.

Generic particles:

```json
{
  "time": 110,
  "particles": [
    {
      "name": "smoke",
      "x": 1.5,
      "y": 1.85,
      "z": 1.5,
      "vx": 0.0,
      "vy": 0.01,
      "vz": 0.0,
      "size": 0.18,
      "time": 16,
      "amount": 3
    }
  ]
}
```

Explosion preset:

```json
{
  "time": 160,
  "particles": [
    {
      "preset": "explosion",
      "x": 1.5,
      "y": 1.45,
      "z": 1.5,
      "time": 8,
      "power": 2.4
    }
  ]
}
```

Weather preset:

```json
{
  "time": 220,
  "particles": [
    {
      "preset": "rain",
      "weather": "snow",
      "x": [0, 2],
      "z": [0, 2],
      "time": 100,
      "amount": 8
    }
  ]
}
```

Particle champs:

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `preset` | chaîne | none | Special preset. `explosion` spawns Un vanilla-style flash/smoke burst. `rain` enables Le weather preset. |
| `weather` | chaîne | `rain` | Weather type utilisé by `preset: "rain"`. Pris en charge valeurs: `rain`, `snow`. |
| `name` | chaîne | none | Generic particle appearance. Pris en charge valeurs: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | chaîne | none | Compatibility aliases pour `name`. |
| `x`, `z` | flottant ou array | scène bounds | Particle origin ou weather coverage. Generic particles utiliser scalar coordonnées. pour `preset: "rain"`, scalar valeurs cible un precipitation colonne et arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | flottant | `0.0` | initial motion vector. `motionX/Y/Z` sont accepted aliases. |
| `time` / `lifetime` | entier | preset-spécifique | Particle lifetime dans ticks. pour `preset: "rain"` ceci est Le total weather duration, including fade-dans et fade-out. |
| `size` | flottant | preset-spécifique | Generic particle half-taille dans bloc units. |
| `amount` | entier | preset-spécifique | Generic particle count. pour `explosion`, omitted amount scales depuis `power`. pour `preset: "rain"`, ceci est Le average per-tick weather density. |
| `power` | flottant | `2.0` | Explosion strength pour Le `explosion` preset. |

Weather preset Remarques:

- `preset: "rain"` est Le shared weather preset entrée point. utiliser `weather: "rain"` pour rainfall ou
  `weather: "snow"` pour snowfall.
- ceci preset est timeline-owned weather. It prend en charge replay, pause, seek, et fast-forward together
  avec Le rest of Le Ponder timeline.
- pour toujours-on scène weather hors de Le Ponder timeline, utiliser Le `<Weather>` balise dans
  `<GameScene>` à la placer.
- Weather presets ignore `y`; Le vertical spawn range est derived depuis Le actuel scène bounds.
- `x: 5, z: 8` targets un precipitation colonne. Arrays utiliser endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` crée deux covered rectangles.
- Si un axis a extra unmatched array valeurs, Le unmatched tail est ignored.
- Le exécution automatiquement shapes Le effect avec Un short début transition, Un steady middle
  section, et Un fin transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes sans splash particles.
- Le weather area est derived depuis Le actuel GameScene bounds so Le effect scales avec Le
  imported structure à la placer of Un hard-coded box.
- Le même `x/z` colonne jamais stacks plusieurs weather types at Le même time. Earlier overlapping
  weather declarations Conservez Le shared columns; later ones seulement rendent on Le remaining area.

---

## Tile Entity NBT Operations

utiliser `mergeTileNBT`, `modifyTileNBT`, et `removeTileNBT` lorsque Le bloc stays dans placer but son
tile entity données modifications. Operations sont seek-safe: GuideNH restores Le original tile NBT et
alors replays tous operations depuis keyframe 0 à travers Le active keyframe.

```json
{
  "time": 80,
  "mergeTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "nbt": "{InputTanks:[{Level:{Speed:0.25,Target:0.25,Value:0.0},TankContent:{Amount:250,FluidName:\"minecraft:lava\"}}]}"
    }
  ],
  "modifyTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "path": "InputTanks[0].TankContent.Amount",
      "value": "500"
    }
  ],
  "removeTileNBT": [
    { "x": 2, "y": 1, "z": 2, "path": "InputTanks[0].Level.Target" }
  ]
}
```

| champ | utilisé by | description |
|-------|---------|-------------|
| `x`, `y`, `z` | tous | Tile-entity bloc position dans structure coordonnées. |
| `nbt` | `mergeTileNBT` | SNBT compound merged dans Le tile entity. Existing compound clés sont merged recursively; autre valeurs remplacer Le old valeur. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT chemin avec liste indexes, e.g. `Items[0].Count` ou `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT valeur written at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow Le même idea as Minecraft's `/data` paths: utiliser dots pour compound clés et
`[index]` pour liste entrées. liste traversal actuellement expects Le traversed liste entrées vers être
compounds, qui correspond courant tile NBT comme inventories, tanks, et recipe slots.

---

## Entity Actions

Regular `<Entity>` balises sont déjà Pris en charge dans `GameScene`. Ponder timelines peut aussi créer
leurs propre entities avec `createEntities`, alors cible those entities by `ref` dans later keyframes.

```json
{
  "time": 0,
  "createEntities": [
    {
      "ref": "marker",
      "sceneEntityId": "marker",
      "id": "minecraft:pig",
      "x": 1.5, "y": 1.0, "z": 2.5,
      "yaw": 180,
      "nbt": "{CustomName:\"Before\",CustomNameVisible:1b}"
    }
  ]
}
```

| champ | description |
|-------|-------------|
| `ref` | obligatoire local reference nom pour later operations. |
| `sceneEntityId` | facultatif stable scène-local id utilisé pour mount relations, replay-safe replacement, import/export restore, et quelconque later removal of ceci logical entity. Defaults vers Un interne id derived depuis `ref`. |
| `id` | Entity ID, e.g. `minecraft:pig`, `Pig`, ou Un mod entity ID Pris en charge by Le scène entity loader. |
| `x`, `y`, `z` | facultatif spawn position. Defaults vers `0, 0, 0` sauf `nbt` supplies `Pos`. |
| `yaw`, `pitch` | facultatif spawn rotation. Defaults vers `0, 0` sauf `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | facultatif living-entity corps/head yaw remplace. Si omitted pendant `yaw` est present, Elles follow `yaw`. |
| `nbt` | facultatif SNBT compound applied lorsque Le entity est créé. |
| `name`, `uuid` | facultatif aperçu-player profile champs lorsque creating Un aperçu player entity. |
| `mount` | facultatif stable `sceneEntityId` of Le vehicle que ceci entity devrait ride après creation ou later replay. |
| `unmount` | facultatif booléen que clears Le entity's actuel stable mount relation avant quelconque later `mount` est applied. |

après creation, utiliser Le entity NBT operations:

```json
{
  "time": 60,
  "mergeEntityNBT": [
    { "ref": "marker", "nbt": "{Saddle:1b}" }
  ],
  "modifyEntityNBT": [
    { "ref": "marker", "path": "CustomName", "value": "\"After\"" }
  ],
  "removeEntityNBT": [
    { "ref": "marker", "path": "CustomNameVisible" }
  ]
}
```

`setEntityNBT` est aussi disponible lorsque you want vers remplacer Le entity's NBT à la placer of merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

Entity actions peut aussi mettre à jour transform, aperçu-player pose, et stable mount état sans
changing NBT:

```json
{
  "time": 80,
  "createEntities": [
    { "ref": "cart", "sceneEntityId": "cart", "id": "minecraft:minecart", "x": 1.5, "y": 1.0, "z": 1.5 },
    { "ref": "rider", "sceneEntityId": "rider", "id": "player", "name": "GuideNH", "x": 1.5, "y": 1.0, "z": 1.5, "mount": "cart" }
  ],
  "modifyEntityNBT": [
    { "ref": "rider", "headYaw": 270.0, "leftArmRotation": "-40 0 0" }
  ]
}
```

vers detach ou supprimer Un timeline entity, utiliser `unmount` ou `removeEntities`:

```json
{
  "time": 120,
  "modifyEntityNBT": [
    { "ref": "rider", "unmount": true, "x": 2.5, "y": 1.0, "z": 1.5 }
  ],
  "removeEntities": [
    { "ref": "cart" }
  ]
}
```

Like tile operations, entity operations sont replayed depuis Le beginning whenever Le active
keyframe modifications, so seeking backwards removes Ponder-créé entities et recreates Le correct
état pour Le cible tick.

Remarques:

- `ref` identifies qui Ponder-owned entity Le actuel action devrait edit ou supprimer.
- `sceneEntityId` et `mount` identify stable cross-entity relations. `mount` toujours points vers a
  stable scène id, ne vers another `ref`.
- Relying on brut passenger NBT alone est ne recommended pour cross-entity scène relationships. Le
  stable registry est what keeps mount et removal behavior deterministic across replay, rebuild,
  import/export, et éditeur aperçu refresh.

---

## annotation Fade

annotations smoothly fade dans sur **5 game ticks** (250 ms) whenever Le active keyframe
modifications pendant playback. Seeking ou pausing toujours affiche annotations at complet opacity.

---

## annotation Types

chaque entrée dans Le `annotations` array requires a `type` champ. Seven types sont disponible.

---

### `diamond`

Renders Un 3D diamond marker at Un monde position.

```json
{
  "type": "diamond",
  "x": 1.5,
  "y": 2.0,
  "z": 1.5,
  "color": "0xFFFF8800",
  "tooltip": "Click me",
  "alwaysOnTop": false
}
```

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | flottant | `0.0` | monde-espace position of Le diamond tip. |
| `color` | chaîne | `"0xFF00E000"` | ARGB couleur as `"0xAARRGGBB"`. |
| `tooltip` | chaîne | `""` | repli texte affiché on survol. |
| `tooltipKey` | chaîne | `""` | traduction clé pour Le survol texte. lorsque résolu, it remplace `tooltip`. |
| `alwaysOnTop` | booléen | `false` | Si true, rendu à travers solid blocs. |

---

### `box`

Renders Un wireframe axis-aligned box depuis `min` vers `max`.

```json
{
  "type": "box",
  "minX": 0.0, "minY": 0.0, "minZ": 0.0,
  "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
  "color": "0x8800FFFF",
  "lineWidth": 1.5,
  "alwaysOnTop": false
}
```

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `minX/Y/Z` | flottant | `0.0` | Minimum corner. |
| `maxX/Y/Z` | flottant | `1.0` | Maximum corner. |
| `color` | chaîne | `"0xFFFFFFFF"` | ARGB ligne couleur. |
| `lineWidth` | flottant | par défaut | GL ligne largeur. |
| `alwaysOnTop` | booléen | `false` | rendent à travers blocs. |

---

### `block`

Renders Un wireframe around un whole bloc. ceci est Le Ponder JSON equivalent of
`<BlockAnnotation pos="x y z">` dans Un regular `GameScene`.

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`"type": "blockBox"` et `"type": "block_box"` sont accepted aliases.

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `pos` | nombre[3] ou chaîne | `[0, 0, 0]` | bloc coordonnée as `[x, y, z]` ou `"x y z"`. |
| `x`, `y`, `z` | nombre | `0` | Alternative bloc coordonnée champs. valeurs sont floored. |
| `blockX/Y/Z` | entier | `0` | Legacy-style bloc coordonnée champs. |
| `color` | chaîne | `"0xFFFFFFFF"` | ARGB ligne couleur. |
| `lineWidth` | flottant | par défaut | GL ligne largeur. |
| `alwaysOnTop` | booléen | `false` | rendent à travers blocs. |

---

### `line`

Renders Un ligne segment ou polyline entre monde positions. `points` takes priority sur
`fromX/Y/Z` et `toX/Y/Z` lorsque it contient at least deux valide points.

```json
{
  "type": "line",
  "fromX": 0.5, "fromY": 0.5, "fromZ": 0.5,
  "toX": 2.5,   "toY": 0.5,   "toZ": 0.5,
  "color": "0xFFFFFF00",
  "arrow": "end",
  "lineWidth": 2.0,
  "alwaysOnTop": true
}
```

Polyline points peut être written l’un ou l’autre as Un chaîne ou as Un array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `fromX/Y/Z` | flottant | `0.0` | début point. |
| `toX/Y/Z` | flottant | `1.0` | fin point. |
| `points` | chaîne ou array | `null` | Polyline points. utiliser `"x y z; x y z; ..."` ou `[[x,y,z], ...]`. |
| `color` | chaîne | `"0xFFFFFFFF"` | ARGB ligne couleur. |
| `arrow` | chaîne | `null` | `start` ou `end`; omitted ou invalide valeurs dessiner non arrow. |
| `lineWidth` | flottant | par défaut | GL ligne largeur. |
| `alwaysOnTop` | booléen | `false` | rendent à travers blocs. |

---

### `blockface`

Met en surbrillance toutes les faces d’un bloc unique avec un revêtement plein translucide.

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` et `"type": "block_face"` sont accepted aliases.

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `pos` | nombre[3] ou chaîne | `[0, 0, 0]` | bloc coordonnée as `[x, y, z]` ou `"x y z"`. |
| `x`, `y`, `z` | nombre | `0` | Alternative bloc coordonnée champs. valeurs sont floored. |
| `blockX/Y/Z` | entier | `0` | Legacy-style bloc coordonnée champs. |
| `color` | chaîne | `"0x80FFFFFF"` | ARGB overlay couleur. |
| `alwaysOnTop` | booléen | `false` | rendent à travers blocs. |

---

### `text`

Renders Un speech-bubble étiquette anchored vers Un monde position. Le box appears au-dessus de Le anchor by
par défaut et est connected vers it avec Un short vertical ligne. texte contenu est keyframe-driven: utiliser
différent `text` annotation entrées on différent keyframes vers modifier Le displayed texte sur time.

```json
{
  "type": "text",
  "x": 1.5,
  "y": 2.5,
  "z": 1.5,
  "text": "Place items here",
  "color": "0xFF44AAFF",
  "connectorSide": "right",
  "connectorOffset": 8,
  "connectorLength": 12
}
```

pour Un fixe écran-espace position que fait ne project depuis monde coordonnées, utiliser
**independent mode**. Le bubble est centered horizontally dans Le scène et placed at
`yOffset` pixels en dessous Le scène's vertical centre.

```json
{
  "type": "text",
  "text": "Independent label",
  "color": "0xFFFFCC00",
  "backgroundAlpha": 160,
  "independent": true,
  "yOffset": 40
}
```

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | flottant | `0.0` | monde-espace anchor position (ignored dans independent mode). |
| `text` | chaîne | - | **obligatoire.** texte vers afficher dans Le bubble. |
| `color` | chaîne | `"0xFFAAAAAA"` | ARGB couleur of Le bubble bordure. |
| `backgroundAlpha` | entier | `204` | arrière-plan opacity depuis `0` (transparent) vers `255` (opaque). Le RGB couleur remains Le par défaut dark navy. |
| `maxWidth` | entier | `0` | Si &gt; 0, wraps texte at ceci largeur dans pixels. Omit ou Définissez vers `0` pour Un unique-ligne étiquette. |
| `independent` | booléen | `false` | Si `true`, position est relatif vers Le scène centre rather que Un monde point. |
| `yOffset` | entier | `0` | Pixel offset depuis Le scène's vertical centre (positive = downward). utilisé avec `independent: true`. |
| `connectorSide` | chaîne | `"bottom"` | `bottom`, `top`, `left`, `right`, ou `none`. Ignored dans independent mode. |
| `connectorOffset` | entier | `0` | Pixel offset along Le sélectionné bubble edge; positive moves droite pour haut/bas et down pour gauche/droite. |
| `connectorLength` | entier | `6` | Pixel length of Le connector ligne. `0` masque Le ligne pendant keeping côté-based placement. |
| `hlMinX/Y/Z` | flottant | `0.0` | Minimum corner of Un facultatif surligner box drawn alongside Le texte bubble. |
| `hlMaxX/Y/Z` | flottant | `1.0` | Maximum corner of Le facultatif surligner box. |
| `highlightColor` | chaîne | `"0x8000FFAA"` | ARGB couleur of Le surligner box. |

lorsque `hlMinX` (ou quelconque `hlMin/Max` coordonnée) est present, an `InWorldBoxAnnotation` est aussi
créé at Le specified bounds avec `highlightColor`. ceci est useful pour pointing at spécifique
bloc regions pendant explaining eux.

Le arrière-plan est un dark navy bubble by par défaut (`#CC0E0E20`), et `backgroundAlpha` controls son
opacity. dans monde-anchored mode Un connector ligne liens Le box vers Le anchor. texte prend en charge Le
complet GuideNH inline riche-texte syntaxe: markdown formatting et MDX inline balises. It est rendu avec
drop-shadow.

> **riche texte:** Le `text` champ prend en charge Le même inline markup utilisé dans GuideNH guide pages:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, et tous autre inline MDX balises.
> simple Minecraft `§` format codes sont **ne** Pris en charge; utiliser MDX syntaxe à la placer.

> A `text` annotation sans a `text` champ (ou Un vide chaîne) est silently ignored.

---

### `input`

Renders Un mouse-entrée icon (gauche button, droite button, ou défiler wheel) anchored vers Un monde
position. ceci est utilisé vers hint que Le player devrait perform Un spécifique interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

avec Un facultatif modifier clé prefix et an élément icon:

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb",
  "modifier": "sneak",
  "item": "minecraft:iron_ingot"
}
```

| champ | type | par défaut | description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | flottant | `0.0` | monde-espace anchor position. |
| `inputType` | chaîne | `"lmb"` | un of `"lmb"`, `"rmb"`, ou `"scroll"`. Case-insensitive. |
| `modifier` | chaîne | `null` | facultatif modifier clé: `"sneak"` ou `"ctrl"`. affiche prefix texte au-dessus de Le icon. |
| `item` | chaîne | `null` | facultatif élément registry ID (e.g. `"minecraft:iron_ingot"`). Renders Le élément icon vers Le gauche of Le mouse icon. prend en charge `"modid:item:meta"` format pour meta valeurs. |

Le icon est un 16x16 sprite drawn depuis `ponder_widgets.png`. Le box arrière-plan est semi-transparent
dark (`#CC0E0E20`) avec Un light-blue bordure (`#80AAAADD`). lorsque an `item` est specified Le box
expands vers accommodate les deux Le élément icon et Le mouse icon côté by côté.

---

## couleur Format

Colors sont ARGB hexadecimal strings. les deux `"0xFFFFFF00"` (avec `0x` prefix) et
`"FFFF00"` (sans prefix) sont accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% transparent
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (par défaut diamond couleur)
- `"0x8022CCFF"` - semi-transparent blue
- `"0xFFAAAAAA"` - light grey (par défaut texte bubble bordure)

## Playback Behavior

### Controls

| Control | Action |
|---------|--------|
| **Prev keyframe** | Jump vers Le début of Le précédent visible keyframe segment. masquées keyframes sont skipped. |
| **Play/Pause** | Toggle playback; restarts depuis Le beginning Si déjà finished. |
| **Restart** | retourner vers tick 0, reset état, et begin playing. |
| Progress bar | clic ou faire glisser vers seek vers quelconque position. Seeking toujours pauses playback. |
| Keyframe nodes | Small tick marks on Le bar pour visible keyframes; survol vers Voir Le étiquette et direction arrow. |

### initial état

lorsque Un page containing `<ImportPonder>` est premier opened, Le scène starts **paused at tick 0**.
Press Play vers begin.

### caméra lock

pendant playback est **active** (ne paused):
- Le caméra follows Le interpolated chemin defined by keyframes.
- Mouse faire glisser et défiler zoom sont **désactivé**.
- Le layer slider et StructureLib sliders sont **masquées**.

pendant playback est **paused** ou **finished**:
- complet interactive caméra faire glisser, zoom, et layer/StructureLib control sont restored.

### Keyframe node labels

lorsque you survol sur Un keyframe node on Le progress bar:
- Le node grows slightly vers indicate it est hovered.
- Si Le keyframe a a `label`, it est displayed beside Le node.
- masquées keyframes do ne créer hoverable nodes, but Elles encore apply leurs timeline état lorsque playback ou seeking reaches eux.

### Layer control pendant playback

Le `layer` champ of Le active keyframe remplace Le visible-layer filtre pendant playback:
- `null` (ou omitted) -> afficher tous layers.
- `1`, `2`, `3`, ... -> restrict vers que 1-based layer index.

## complet Exemple

Le following Exemple demonstrates chaque annotation type across Un four-keyframe scène.

### répertoire mise en page

```
assets/mymod/guidebooks/
  pages/machines/grinder.mdx
  pages/machines/grinder.snbt
  pages/machines/grinder.json
```

### `grinder.json`

```json
{
  "totalTime": 240,
  "keyframes": [
    {
      "time": 0,
      "label": "Overview",
      "camera": { "zoom": 1.5, "rotX": 20, "rotY": 225 },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "Input hatch",
      "camera": { "rotY": 180 },
      "layer": null,
      "annotations": [
        {
          "type": "diamond",
          "x": 0.5, "y": 1.5, "z": 1.5,
          "color": "0xFF44FF44",
          "tooltip": "EV Input Bus",
          "alwaysOnTop": true
        },
        {
          "type": "text",
          "x": 0.5, "y": 3.0, "z": 1.5,
          "text": "Insert ore here",
          "color": "0xFF44FF44"
        },
        {
          "type": "input",
          "x": 0.5, "y": 2.0, "z": 1.5,
          "inputType": "rmb"
        }
      ]
    },
    {
      "time": 140,
      "label": "Output side",
      "camera": { "rotY": 90 },
      "layer": null,
      "annotations": [
        {
          "type": "box",
          "minX": 2.0, "minY": 0.0, "minZ": 0.0,
          "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
          "color": "0x8800AAFF",
          "lineWidth": 1.5
        },
        {
          "type": "line",
          "fromX": 2.0, "fromY": 1.0, "fromZ": 1.5,
          "toX": 2.5, "toY": 1.0, "toZ": 1.5,
          "color": "0xFFFFAA00",
          "lineWidth": 2.0,
          "alwaysOnTop": true
        },
        {
          "type": "blockface",
          "pos": [2, 1, 1],
          "color": "0x8833FF33"
        },
        {
          "type": "text",
          "x": 2.5, "y": 3.0, "z": 1.5,
          "text": "Collect dust here",
          "color": "0xFF00AAFF"
        },
        {
          "type": "input",
          "x": 2.5, "y": 2.0, "z": 1.5,
          "inputType": "lmb"
        }
      ]
    },
    {
      "time": 220,
      "label": "Scroll layer",
      "camera": { "rotY": 225 },
      "layer": null,
      "annotations": [
        {
          "type": "input",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "inputType": "scroll"
        },
        {
          "type": "text",
          "x": 1.5, "y": 3.5, "z": 1.5,
          "text": "Scroll to show layers",
          "color": "0xFFFFCC00"
        }
      ]
    },
    {
      "time": 240,
      "camera": { "rotY": 225 }
    }
  ]
}
```

### `grinder.snbt`

Un standard NBT structure fichier (créé avec Le `/structure save` command ou Un tool comme
Litematica). Voir [Getting Started](Guide-Page-Format) pour Le complet SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

Le broyeur transforme les minerais en double quantité de poussière. Appuyez sur **Lecture** pour voir la visite animée.
```

## Remarques

- Le Ponder progress bar est drawn au-dessus de Le layer/StructureLib sliders. pendant playback Le
  structural sliders sont masquées vers Conservez Le UI clean; Elles reappear lorsque paused.
- caméra interpolation est toujours smooth (ease-dans/out) even Si some keyframes seulement modifier a
  subset of caméra axes. utiliser `cameraEaseTicks` on Un keyframe vers snap Le caméra instantly (`0`)
  ou ease sur Un fixe nombre of ticks avant holding Le cible position.
- annotations belong vers Un unique keyframe - Elles appear seulement pendant que keyframe est active
  (i.e., depuis son `time` tick until Le suivant keyframe's `time` tick). Overlay texte annotations
  fade out smoothly lorsque Le keyframe modifications pendant playback.
- Ponder `line` annotations utiliser Le même exécution renderer as regular `LineAnnotation`, including
  polyline bends et début/fin arrows. point marker cubes remain Un MDX-seulement feature.
- Ponder `text` annotations utiliser Le même exécution renderer as regular `TextAnnotation`, including
  connector côté, offset, et length. Dynamic texte est keyframe-based rather que interpolated per tick.
- seulement un `<ImportPonder>` balise est effective per `<GameScene>`. Un second balise overwrites Le premier.
- A `text` annotation avec Un vide ou absent `text` champ est silently skipped.
- Le `inputType` champ defaults vers `"lmb"` Si omitted ou unrecognised.
- `blockChanges` sont applied dans ordre depuis Le premier vers Le actuel keyframe chaque time Le
  active keyframe modifications, so changing Le même position dans plusieurs keyframes works correctly.
- Tile/entity NBT operations utiliser Le même replay model as `blockChanges`; Elles sont safe vers seek
  forwards ou backwards.
- `text` annotations avec a `maxWidth` &gt; 0 sont word-wrapped using Le vanilla font renderer;
  Le bubble box hauteur adjusts automatiquement pour multi-ligne texte.
- `nbt` strings dans `blockChanges` doit utiliser **unquoted** SNBT clés (standard MC 1.7.10 format).
  Quoted clés sera être rejected by Le analyseur. chaîne valeurs encore require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` et `modifyEntityNBT` valeurs sont SNBT valeurs, ne JSON valeurs. pour Un chaîne
  valeur, escape Le SNBT quotes dans JSON: `"value": "\"hello\""`.
