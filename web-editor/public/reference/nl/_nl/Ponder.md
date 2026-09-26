# Ponder Animation Timeline

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH ondersteunt Ponder-animatie-style animated timelines binnen `<GameScene>` blokken. You supply an
external JSON bestand that defines keyframes, camera movements en in-wereld Annotaties, en
GuideNH renders Een interactief progress bar met play/pause controls below De 3D scène.

## Snel starten

1. maken Een Ponder-animatie JSON bestand en place it in your resourcepack (see [File Placement](#file-placement)).
2. Voeg toe `<ImportPonder src="..."/>` binnen a `<GameScene>` blok alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **Opmerking:** `<ImportPonder>` moet appear binnen a `<GameScene>` blok. De `src` Attribuut is
> Vereist. Structure data is still provided by `<ImportStructure>` of `<ImportStructureLib>`.

## Bestandsplaatsing

Ponder-animatie JSON files follow De zelfde resource-pack pad Regels as SNBT structures:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
```

De `src` Attribuut accepts both relative en absolute IDs:

| Example | opgelost as |
|---------|-------------|
| `src="my_machine.json"` | Relative naar De huidige pagina's directory |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | Absolute (`mymod` namespace) |

## JSON-indeling

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

### Hoofdvelden

| Field | Type | Vereist | Description |
|-------|------|----------|-------------|
| `totalTime` | geheel getal | Yes | Total duration in ticks (20 ticks = 1 second). Minimum clamped naar 1. |
| `keyframes` | array | Yes | lijst of keyframe objects. kan be empty. |

### Keyframevelden

| Field | Type | Vereist | Description |
|-------|------|----------|-------------|
| `time` | geheel getal | Yes | Tick at which Deze keyframe occurs (0 <= time <= totalTime). |
| `hidden` | boolean | No | wanneer `true`, De keyframe still applies camera/NBT/entity/annotatie status at its `time`, but no zichtbaar node is drawn voor it on De progress bar en zichtbaar keyframe Navigatie skips it. |
| `label` | tekenreeks | No | optioneel fallback label getoond wanneer hovering De keyframe node on De progress bar. |
| `labelKey` | tekenreeks | No | vertaalsleutel voor De keyframe label. wanneer opgelost, it overrides `label`. |
| `camera` | object | No | camera status at Deze keyframe. Null fields inherit van De previous keyframe. |
| `cameraEaseTicks` | geheel getal of null | No | How many ticks De camera takes naar ease van De **previous** keyframe naar Deze one. `null` (Standaard) = ease over De full segment. `0` = instant snap. `N > 0` = ease over N ticks, then hold at De target positie. |
| `layer` | geheel getal of null | No | zichtbaar layer override. `null` (of omitted) shows alle layers. 1-based index. |
| `annotations` | array | No | lijst of annotatie objects getoond terwijl Deze keyframe is active. |
| `sounds` | array | No | lijst of sounds played once wanneer Deze keyframe becomes active during forward playback. |
| `particles` | array | No | lijst of runtime particle bursts of presets fired wanneer Deze keyframe becomes active during forward playback. |
| `blockChanges` | array | No | lijst of blok replacements applied wanneer Deze keyframe first becomes active. |
| `mergeTileNBT` | array | No | Merge SNBT compounds into tile entities at blok positions. |
| `modifyTileNBT` | array | No | Stel in one tile-entity NBT pad naar Een SNBT waarde. |
| `removeTileNBT` | array | No | verwijderen one tile-entity NBT pad. |
| `createEntities` | array | No | maken Ponder-animatie-owned entities that kan be referenced by later entity NBT operations. |
| `setEntityNBT` | array | No | vervangen Een referenced entity's NBT met De supplied SNBT compound. |
| `mergeEntityNBT` | array | No | Merge Een SNBT compound into Een referenced entity. |
| `modifyEntityNBT` | array | No | Stel in one referenced entity NBT pad naar Een SNBT waarde. |
| `removeEntityNBT` | array | No | verwijderen one referenced entity NBT pad. |
| `removeEntities` | array | No | verwijderen one of more Ponder-animatie-owned entities by `ref` using De stable scène-entity registry. |

verborgen keyframes are useful wanneer you want additional intermediate status wijzigingen zonder adding Een new zichtbaar node
naar De timeline. voor example, you kan split several `modifyTileNBT` updates across meerdere ticks, mark De
intermediate keyframes as verborgen, en Behoud alleen De major beats zichtbaar on De progress bar.

### Cameravelden

alle Cameravelden are optioneel. Any `null` of omitted field inherits its waarde van De nearest
prior keyframe that defined it; Als no prior keyframe defined De field, De scène's Standaard
camera waarde is gebruikt.

| Field | Type | Description |
|-------|------|-------------|
| `zoom` | kommagetal | camera zoomen level (0.1 - 10.0). |
| `rotX` | kommagetal | X-axis rotatie in degrees. |
| `rotY` | kommagetal | Y-axis rotatie in degrees. |
| `rotZ` | kommagetal | Z-axis rotatie in degrees. |
| `offX` | kommagetal | Horizontal pan offset in scherm pixels. |
| `offY` | kommagetal | Vertical pan offset in scherm pixels. |

De camera smoothly interpolates between adjacent keyframes using an **ease-in/ease-out** curve.
gebruiken `cameraEaseTicks` on De **destination** keyframe naar control De easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
```

## blok wijzigingen

De `blockChanges` array in Een keyframe replaces blokken in De live structure wanneer that keyframe
becomes active. Deze staat toe De animation naar tonen voor-en-na states, place of verwijderen
blokken, of animate Een machine powering on.

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

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | geheel getal | - | **Vereist.** positie of De blok naar wijzigen (structure coördinaten). |
| `block` | tekenreeks | - | **Vereist.** Registry naam, e.g. `"minecraft:furnace"`. gebruiken `"minecraft:air"` naar verwijderen. |
| `meta` | geheel getal | `0` | blok metadata / damage waarde. |
| `particles` | boolean | `true` | Whether naar spawn blok-texture particle effects wanneer Deze blok wijzigen fires during forward playback. Particles are taken van De blok's own icon texture. Stel in naar `false` naar suppress (e.g., voor silent removal). |
| `nbt` | tekenreeks | `null` | SNBT tekenreeks voor Een tile entity tag, e.g. voor chests, furnaces, etc. Parsed met `JsonToNBT`. Keys moet be **unquoted** (standard SNBT format). Ignored Als De blok has no tile entity. |

**Seek-safe:** wanneer seeking backwards De runtime restores alle changed positions naar their
original structure status, then re-applies wijzigingen van keyframes 0 through De huidige one.
De displayed structure is always correct regardless of seek direction.

> **Opmerking on particles:** blok-texture particles fire once, alleen during forward playback wanneer De
> keyframe first becomes active. They are cleared on seek, restart, of initial laden.

## Keyframe Sounds

Voeg toe a `sounds` array naar Een keyframe naar play one of more gids sounds wanneer De keyframe becomes active
during forward playback. Seeking en initial laden do niet play keyframe sounds; restart clears De
play history so De sounds kan fire again.

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

Sound fields:

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `sound` | tekenreeks | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | tekenreeks | - | Sound bestand id of pad; `guidenh:sounds/machine/start.ogg` becomes `guidenh:machine.start`. |
| `volume` | kommagetal | `1.0` | Playback volume voor attenuation. |
| `pitch` | kommagetal | `1.0` | Playback pitch. |
| `cooldown` | geheel getal | `250` | Minimum milliseconds voor De zelfde sound kan play again. |
| `x`, `y`, `z` | kommagetal | none | optioneel scène-space source positie voor schermruimte attenuation. |
| `radius` | kommagetal | scène short side * 0.75 | Attenuation radius in scherm pixels. |
| `minVolume` | kommagetal | `0.15` | Minimum attenuation factor. |

---

## Keyframe Particles

Voeg toe a `particles` array naar Een keyframe wanneer you want one-shot particle bursts of timeline-lokale
Weer overlays during forward playback. Deze particles are niet re-fired during reverse
scrubbing, en seek/restart clears them voor replaying De active status.

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

Weer preset:

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

Particle fields:

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `preset` | tekenreeks | none | Special preset. `explosion` spawns Een vanilla-style flash/smoke burst. `rain` enables De Weer preset. |
| `weather` | tekenreeks | `rain` | Weer Type gebruikt by `preset: "rain"`. Supported waarden: `rain`, `snow`. |
| `name` | tekenreeks | none | Generic particle appearance. Supported waarden: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | tekenreeks | none | Compatibility aliases voor `name`. |
| `x`, `z` | kommagetal of array | scène bounds | Particle origin of Weer coverage. Generic particles gebruiken scalar coördinaten. voor `preset: "rain"`, scalar waarden target one precipitation kolom en arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | kommagetal | `0.0` | Initial motion vector. `motionX/Y/Z` are accepted aliases. |
| `time` / `lifetime` | geheel getal | preset-specific | Particle lifetime in ticks. voor `preset: "rain"` Deze is De total Weer duration, including fade-in en fade-out. |
| `size` | kommagetal | preset-specific | Generic particle half-grootte in blok units. |
| `amount` | geheel getal | preset-specific | Generic particle count. voor `explosion`, omitted amount scales van `power`. voor `preset: "rain"`, Deze is De average per-tick Weer density. |
| `power` | kommagetal | `2.0` | Explosion strength voor De `explosion` preset. |

Weer preset notes:

- `preset: "rain"` is De shared Weer preset entry punt. gebruiken `weather: "rain"` voor rainfall of
  `weather: "snow"` voor snowfall.
- Deze preset is timeline-owned Weer. It ondersteunt replay, pause, seek, en fast-forward together
  met De rest of De Ponder-animatie timeline.
- voor always-on scène Weer buiten De Ponder-animatie timeline, gebruiken De `<Weather>` tag binnen
  `<GameScene>` instead.
- Weer presets ignore `y`; De vertical spawn range is derived van De huidige scène bounds.
- `x: 5, z: 8` targets one precipitation kolom. Arrays gebruiken endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` creates two covered rectangles.
- Als one axis has extra unmatched array waarden, De unmatched tail is ignored.
- De runtime automatisch shapes De effect met Een short begin transition, Een steady middle
  section, en Een einde transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes zonder splash particles.
- De Weer area is derived van De huidige GameScene bounds so De effect scales met De
  imported structure instead of Een hard-coded box.
- De zelfde `x/z` kolom never stacks meerdere Weer types at De zelfde time. Earlier overlapping
  Weer declarations Behoud De shared columns; later ones alleen renderen on De remaining area.

---

## Tile Entity NBT Operations

gebruiken `mergeTileNBT`, `modifyTileNBT`, en `removeTileNBT` wanneer De blok stays in place but its
tile entity data wijzigingen. Operations are seek-safe: GuideNH restores De original tile NBT en
then replays alle operations van keyframe 0 through De active keyframe.

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

| Field | gebruikt by | Description |
|-------|---------|-------------|
| `x`, `y`, `z` | alle | Tile-entity blok positie in structure coördinaten. |
| `nbt` | `mergeTileNBT` | SNBT compound merged into De tile entity. Existing compound keys are merged recursively; other waarden vervangen De old waarde. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT pad met lijst indexes, e.g. `Items[0].Count` of `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT waarde written at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow De zelfde idea as Minecraft's `/data` paths: gebruiken dots voor compound keys en
`[index]` voor lijst entries. lijst traversal currently expects De traversed lijst entries naar be
compounds, which matches algemene tile NBT such as inventories, tanks, en recipe slots.

---

## Entity Actions

Regular `<Entity>` tags are al supported in `GameScene`. Ponder-animatie timelines kan ook maken
their own entities met `createEntities`, then target those entities by `ref` in later keyframes.

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

| Field | Description |
|-------|-------------|
| `ref` | Vereist lokale reference naam voor later operations. |
| `sceneEntityId` | optioneel stable scène-lokale id gebruikt voor mount relations, replay-safe replacement, import/export restore, en any later removal of Deze logical entity. Defaults naar Een internal id derived van `ref`. |
| `id` | Entity ID, e.g. `minecraft:pig`, `Pig`, of Een mod entity ID supported by De scène entity loader. |
| `x`, `y`, `z` | optioneel spawn positie. Defaults naar `0, 0, 0` unless `nbt` supplies `Pos`. |
| `yaw`, `pitch` | optioneel spawn rotatie. Defaults naar `0, 0` unless `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | optioneel living-entity body/head yaw overrides. Als omitted terwijl `yaw` is present, they follow `yaw`. |
| `nbt` | optioneel SNBT compound applied wanneer De entity is gemaakt. |
| `name`, `uuid` | optioneel voorbeeldweergave-player profile fields wanneer creating Een voorbeeldweergave player entity. |
| `mount` | optioneel stable `sceneEntityId` of De vehicle that Deze entity zou moeten ride na creation of later replay. |
| `unmount` | optioneel boolean that clears De entity's huidige stable mount relation voor any later `mount` is applied. |

na creation, gebruiken De entity NBT operations:

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

`setEntityNBT` is ook available wanneer you want naar vervangen De entity's NBT instead of merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

Entity actions kan ook bijwerken transform, voorbeeldweergave-player pose, en stable mount status zonder
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

naar detach of verwijderen Een timeline entity, gebruiken `unmount` of `removeEntities`:

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

Like tile operations, entity operations are replayed van De beginning whenever De active
keyframe wijzigingen, so seeking backwards removes Ponder-animatie-gemaakt entities en recreates De correct
status voor De target tick.

Notes:

- `ref` identifies which Ponder-animatie-owned entity De huidige action zou moeten edit of verwijderen.
- `sceneEntityId` en `mount` identify stable cross-entity relations. `mount` always punten naar a
  stable scène id, niet naar another `ref`.
- Relying on raw passenger NBT alone is niet recommended voor cross-entity scène relationships. De
  stable registry is what keeps mount en removal behavior deterministic across replay, rebuild,
  import/export, en editor voorbeeldweergave refresh.

---

## annotatie Fade

Annotaties smoothly fade in over **5 game ticks** (250 ms) whenever De active keyframe
wijzigingen during playback. Seeking of pausing always shows Annotaties at full opacity.

---

## annotatie Types

elke entry in De `annotations` array requires a `type` field. Seven types are available.

---

### `diamond`

Renders Een 3D diamond marker at Een wereld positie.

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

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | kommagetal | `0.0` | wereldruimte positie of De diamond tip. |
| `color` | tekenreeks | `"0xFF00E000"` | ARGB kleur as `"0xAARRGGBB"`. |
| `tooltip` | tekenreeks | `""` | fallback tekst getoond on zweven. |
| `tooltipKey` | tekenreeks | `""` | vertaalsleutel voor De zweven tekst. wanneer opgelost, it overrides `tooltip`. |
| `alwaysOnTop` | boolean | `false` | Als true, gerenderd through solid blokken. |

---

### `box`

Renders Een wireframe asuitgelijnd box van `min` naar `max`.

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

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `minX/Y/Z` | kommagetal | `0.0` | Minimum corner. |
| `maxX/Y/Z` | kommagetal | `1.0` | Maximum corner. |
| `color` | tekenreeks | `"0xFFFFFFFF"` | ARGB regel kleur. |
| `lineWidth` | kommagetal | Standaard | GL regel breedte. |
| `alwaysOnTop` | boolean | `false` | renderen through blokken. |

---

### `block`

Renders Een wireframe around one whole blok. Deze is De Ponder-animatie JSON equivalent of
`<BlockAnnotation pos="x y z">` in Een regular `GameScene`.

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`"type": "blockBox"` en `"type": "block_box"` are accepted aliases.

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `pos` | getal[3] of tekenreeks | `[0, 0, 0]` | blok coördinaat as `[x, y, z]` of `"x y z"`. |
| `x`, `y`, `z` | getal | `0` | Alternative blok coördinaat fields. waarden are floored. |
| `blockX/Y/Z` | geheel getal | `0` | Legacy-style blok coördinaat fields. |
| `color` | tekenreeks | `"0xFFFFFFFF"` | ARGB regel kleur. |
| `lineWidth` | kommagetal | Standaard | GL regel breedte. |
| `alwaysOnTop` | boolean | `false` | renderen through blokken. |

---

### `line`

Renders Een lijnsegment of polylijn between wereld positions. `points` takes priority over
`fromX/Y/Z` en `toX/Y/Z` wanneer it bevat at least two valid punten.

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

polylijn punten kan be written either as Een tekenreeks of as Een array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `fromX/Y/Z` | kommagetal | `0.0` | begin punt. |
| `toX/Y/Z` | kommagetal | `1.0` | einde punt. |
| `points` | tekenreeks of array | `null` | polylijn punten. gebruiken `"x y z; x y z; ..."` of `[[x,y,z], ...]`. |
| `color` | tekenreeks | `"0xFFFFFFFF"` | ARGB regel kleur. |
| `arrow` | tekenreeks | `null` | `start` of `end`; omitted of invalid waarden tekenen no pijl. |
| `lineWidth` | kommagetal | Standaard | GL regel breedte. |
| `alwaysOnTop` | boolean | `false` | renderen through blokken. |

---

### `blockface`

Markeert alle zijden van één blok met een doorschijnende effen overlay.

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` en `"type": "block_face"` are accepted aliases.

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `pos` | getal[3] of tekenreeks | `[0, 0, 0]` | blok coördinaat as `[x, y, z]` of `"x y z"`. |
| `x`, `y`, `z` | getal | `0` | Alternative blok coördinaat fields. waarden are floored. |
| `blockX/Y/Z` | geheel getal | `0` | Legacy-style blok coördinaat fields. |
| `color` | tekenreeks | `"0x80FFFFFF"` | ARGB overlay kleur. |
| `alwaysOnTop` | boolean | `false` | renderen through blokken. |

---

### `text`

Renders Een speech-bubble label anchored naar Een wereld positie. De box appears above De anchor by
Standaard en is connected naar it met Een short vertical regel. tekst inhoud is keyframe-driven: gebruiken
anders `text` annotatie entries on anders keyframes naar wijzigen De displayed tekst over time.

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

voor Een vast schermruimte positie that doet niet project van wereld coördinaten, gebruiken
**independent modus**. De bubble is centered horizontally in De scène en placed at
`yOffset` pixels below De scène's vertical centre.

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

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | kommagetal | `0.0` | wereldruimte anchor positie (ignored in independent modus). |
| `text` | tekenreeks | - | **Vereist.** tekst naar weergeven binnen De bubble. |
| `color` | tekenreeks | `"0xFFAAAAAA"` | ARGB kleur of De bubble rand. |
| `backgroundAlpha` | geheel getal | `204` | achtergrond opacity van `0` (transparent) naar `255` (opaque). De RGB kleur remains De Standaard dark navy. |
| `maxWidth` | geheel getal | `0` | Als &gt; 0, wraps tekst at Deze breedte in pixels. Omit of Stel in naar `0` voor Een enkele-regel label. |
| `independent` | boolean | `false` | Als `true`, positie is relative naar De scène centre rather than Een wereld punt. |
| `yOffset` | geheel getal | `0` | Pixel offset van De scène's vertical centre (positive = downward). gebruikt met `independent: true`. |
| `connectorSide` | tekenreeks | `"bottom"` | `bottom`, `top`, `left`, `right`, of `none`. Ignored in independent modus. |
| `connectorOffset` | geheel getal | `0` | Pixel offset along De geselecteerd bubble edge; positive moves right voor top/bottom en down voor left/right. |
| `connectorLength` | geheel getal | `6` | Pixel length of De connector regel. `0` hides De regel terwijl keeping side-based placement. |
| `hlMinX/Y/Z` | kommagetal | `0.0` | Minimum corner of Een optioneel markeren box drawn alongside De tekst bubble. |
| `hlMaxX/Y/Z` | kommagetal | `1.0` | Maximum corner of De optioneel markeren box. |
| `highlightColor` | tekenreeks | `"0x8000FFAA"` | ARGB kleur of De markeren box. |

wanneer `hlMinX` (of any `hlMin/Max` coördinaat) is present, an `InWorldBoxAnnotation` is ook
gemaakt at De specified bounds met `highlightColor`. Deze is useful voor pointing at specific
blok regions terwijl explaining them.

De achtergrond is Een dark navy bubble by Standaard (`#CC0E0E20`), en `backgroundAlpha` controls its
opacity. In wereld-anchored modus Een connector regel links De box naar De anchor. tekst ondersteunt De
full GuideNH inline rich-tekst syntaxis: Markdown formatting en MDX inline tags. It is gerenderd met
drop-shadow.

> **Rich tekst:** De `text` field ondersteunt De zelfde inline markup gebruikt in GuideNH gids pages:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, en alle other inline MDX tags.
> Plain Minecraft `§` format codes are **niet** supported; gebruiken MDX syntaxis instead.

> A `text` annotatie zonder a `text` field (of Een empty tekenreeks) is silently ignored.

---

### `input`

Renders Een mouse-input icon (left button, right button, of scrollen wheel) anchored naar Een wereld
positie. Deze is gebruikt naar hint that De player zou moeten perform Een specific interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

met Een optioneel modifier key prefix en Een item icon:

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

| Field | Type | Standaard | Description |
|-------|------|---------|-------------|
| `x`, `y`, `z` | kommagetal | `0.0` | wereldruimte anchor positie. |
| `inputType` | tekenreeks | `"lmb"` | One of `"lmb"`, `"rmb"`, of `"scroll"`. Case-insensitive. |
| `modifier` | tekenreeks | `null` | optioneel modifier key: `"sneak"` of `"ctrl"`. Shows prefix tekst above De icon. |
| `item` | tekenreeks | `null` | optioneel item registry ID (e.g. `"minecraft:iron_ingot"`). Renders De item icon naar De left of De mouse icon. ondersteunt `"modid:item:meta"` format voor meta waarden. |

De icon is Een 16x16 sprite drawn van `ponder_widgets.png`. De box achtergrond is semi-transparent
dark (`#CC0E0E20`) met Een light-blue rand (`#80AAAADD`). wanneer an `item` is specified De box
expands naar accommodate both De item icon en De mouse icon side by side.

---

## Kleurindeling

Colors are ARGB hexadecimal strings. Both `"0xFFFFFF00"` (met `0x` prefix) en
`"FFFF00"` (zonder prefix) are accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% transparent
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (Standaard diamond kleur)
- `"0x8022CCFF"` - semi-transparent blue
- `"0xFFAAAAAA"` - light grey (Standaard tekst bubble rand)

## Afspeelgedrag

### Controls

| Control | Action |
|---------|--------|
| **Prev keyframe** | Jump naar De begin of De previous zichtbaar keyframe segment. verborgen keyframes are skipped. |
| **Play/Pause** | Toggle playback; restarts van De beginning Als al finished. |
| **Restart** | Return naar tick 0, reset status, en begin playing. |
| Progress bar | klikken of slepen naar seek naar any positie. Seeking always pauses playback. |
| Keyframe nodes | Small tick marks on De bar voor zichtbaar keyframes; zweven naar see De label en direction pijl. |

### Beginstatus

wanneer Een pagina containing `<ImportPonder>` is first opened, De scène starts **paused at tick 0**.
Press Play naar begin.

### camera lock

terwijl playback is **active** (niet paused):
- De camera follows De interpolated pad defined by keyframes.
- Mouse slepen en scrollen zoomen are **uitgeschakeld**.
- De layer slider en StructureLib sliders are **verborgen**.

terwijl playback is **paused** of **finished**:
- Full interactief camera slepen, zoomen, en layer/StructureLib control are restored.

### Keyframe node labels

wanneer you zweven over Een keyframe node on De progress bar:
- De node grows slightly naar indicate it is hovered.
- Als De keyframe has a `label`, it is displayed beside De node.
- verborgen keyframes do niet maken hoverable nodes, but they still apply their timeline status wanneer playback of seeking reaches them.

### Layer control during playback

De `layer` field of De active keyframe overrides De zichtbaar-layer filter during playback:
- `null` (of omitted) -> tonen alle layers.
- `1`, `2`, `3`, ... -> restrict naar that 1-based layer index.

## Volledig voorbeeld

De following example demonstrates iedere annotatie Type across Een four-keyframe scène.

### Mapindeling

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

Een standard NBT structure bestand (gemaakt met De `/structure save` command of Een tool such as
Litematica). See [Getting Started](Guide-Page-Format) voor De full SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

De vergruizer verandert ertsen in dubbele hoeveelheden stof. Druk op **Afspelen** om de animatie te bekijken.
```

## Notes

- De Ponder-animatie progress bar is drawn above De layer/StructureLib sliders. During playback De
  structural sliders are verborgen naar Behoud De UI clean; they reappear wanneer paused.
- camera interpolation is always smooth (ease-in/out) even Als some keyframes alleen wijzigen a
  subset of camera axes. gebruiken `cameraEaseTicks` on Een keyframe naar snap De camera instantly (`0`)
  of ease over Een vast getal of ticks voor holding De target positie.
- Annotaties belong naar Een enkele keyframe - they appear alleen terwijl that keyframe is active
  (i.e., van its `time` tick until De next keyframe's `time` tick). Overlay tekst Annotaties
  fade out smoothly wanneer De keyframe wijzigingen during playback.
- Ponder-animatie `line` Annotaties gebruiken De zelfde runtime renderer as regular `LineAnnotation`, including
  polylijn bends en begin/einde arrows. punt marker cubes remain Een MDX-alleen feature.
- Ponder-animatie `text` Annotaties gebruiken De zelfde runtime renderer as regular `TextAnnotation`, including
  connector side, offset, en length. Dynamic tekst is keyframe-based rather than interpolated per tick.
- alleen one `<ImportPonder>` tag is effective per `<GameScene>`. Een second tag overwrites De first.
- A `text` annotatie met Een empty of absent `text` field is silently skipped.
- De `inputType` field defaults naar `"lmb"` Als omitted of unrecognised.
- `blockChanges` are applied in order van De first naar De huidige keyframe iedere time De
  active keyframe wijzigingen, so changing De zelfde positie in meerdere keyframes works correctly.
- Tile/entity NBT operations gebruiken De zelfde replay model as `blockChanges`; they are safe naar seek
  forwards of backwards.
- `text` Annotaties met a `maxWidth` &gt; 0 are word-wrapped using De vanilla font renderer;
  De bubble box hoogte adjusts automatisch voor multi-regel tekst.
- `nbt` strings in `blockChanges` moet gebruiken **unquoted** SNBT keys (standard MC 1.7.10 format).
  Quoted keys zal be rejected by De parser. tekenreeks waarden still require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` en `modifyEntityNBT` waarden are SNBT waarden, niet JSON waarden. voor Een tekenreeks
  waarde, escape De SNBT quotes binnen JSON: `"value": "\"hello\""`.
