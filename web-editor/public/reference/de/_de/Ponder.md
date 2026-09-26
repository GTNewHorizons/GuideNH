# Ponder Animation Timeline


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH unterstützt Ponder-style animated timelines innerhalb `<GameScene>` Blöcke. You supply ein
extern JSON Datei dass defines keyframes, Kamera movements und in-Welt Annotationen, und
GuideNH rendert Eine interactive progress bar mit play/paVerwenden Sie controls unter Die 3D Szene.

## SchnellStart

1. erstellen Eine Ponder JSON Datei und platzieren it in Ihre Ressource pack (Siehe [File Placement](#file-placement)).
2. Fügen Sie hinzu `<ImportPonder src="..."/>` innerhalb ein `<GameScene>` Block alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **Hinweis:** `<ImportPonder>` muss appear innerhalb ein `<GameScene>` Block. Die `src` Attribut ist
> erforderlich. Structure Daten ist weiterhin bereitgestellt by `<ImportStructure>` oder `<ImportStructureLib>`.

## Datei Positionierung

Ponder JSON files follow Die gleich Ressource-pack Pfad Regeln als SNBT structures:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
```

Die `src` Attribut accepts beide relativ und absolut IDs:

| Beispiel | aufgelöst als |
|---------|-------------|
| `src="my_machine.json"` | relativ zu Die aktuell Seite's Verzeichnis |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | absolut (`mymod` namespace) |

## JSON-Format

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

### Stammfelder

| Feld | Typ | erforderlich | Beschreibung |
|-------|------|----------|-------------|
| `totalTime` | Ganzzahl | ja | Total duration in ticks (20 ticks = 1 second). Minimum clamped zu 1. |
| `keyframes` | array | ja | Liste von keyframe objects. kann sein leer. |

### Keyframe-Felder

| Feld | Typ | erforderlich | Beschreibung |
|-------|------|----------|-------------|
| `time` | Ganzzahl | ja | Tick at which dies keyframe occurs (0 <= time <= totalTime). |
| `hidden` | Boolesch | nein | wenn `true`, Die keyframe weiterhin wendet ein Kamera/NBT/entity/Annotation Zustand at seine `time`, but nein sichtbar Knoten ist drawn für it auf Die progress bar und sichtbar keyframe navigation skips it. |
| `label` | Zeichenkette | nein | optional Fallback Beschriftung angezeigt wenn Beim Überfahren Die keyframe Knoten auf Die progress bar. |
| `labelKey` | Zeichenkette | nein | Übersetzung Schlüssel für Die keyframe Beschriftung. wenn aufgelöst, it überschreibt `label`. |
| `camera` | object | nein | Kamera Zustand at dies keyframe. Null Felder inherit von Die vorherig keyframe. |
| `cameraEaseTicks` | Ganzzahl oder null | nein | How many ticks Die Kamera takes zu ease von Die **vorherig** keyframe zu dies eins. `null` (Standard) = ease über Die Vollständige segment. `0` = instant snap. `N > 0` = ease über N ticks, dann hold at Die Ziel Position. |
| `layer` | Ganzzahl oder null | nein | sichtbar layer Überschreibung. `null` (oder omitted) zeigt alle layers. 1-based index. |
| `annotations` | array | nein | Liste von Annotation objects angezeigt während dies keyframe ist active. |
| `sounds` | array | nein | Liste von sounds played once wenn dies keyframe wird zu active during forward playback. |
| `particles` | array | nein | Liste von Laufzeit particle bursts oder presets fired wenn dies keyframe wird zu active during forward playback. |
| `blockChanges` | array | nein | Liste von Block replacements applied wenn dies keyframe erste wird zu active. |
| `mergeTileNBT` | array | nein | Merge SNBT compounds in tile entities at Block positions. |
| `modifyTileNBT` | array | nein | Setzen Sie eins tile-entity NBT Pfad zu Eine SNBT Wert. |
| `removeTileNBT` | array | nein | entfernen eins tile-entity NBT Pfad. |
| `createEntities` | array | nein | erstellen Ponder-owned entities dass kann sein referenced by later entity NBT operations. |
| `setEntityNBT` | array | nein | ersetzen Eine referenced entity's NBT mit Die supplied SNBT compound. |
| `mergeEntityNBT` | array | nein | Merge Eine SNBT compound in Eine referenced entity. |
| `modifyEntityNBT` | array | nein | Setzen Sie eins referenced entity NBT Pfad zu Eine SNBT Wert. |
| `removeEntityNBT` | array | nein | entfernen eins referenced entity NBT Pfad. |
| `removeEntities` | array | nein | entfernen eins oder mehr Ponder-owned entities by `ref` using Die stable Szene-entity registry. |

ausgeblendet keyframes sind useful wenn you want additional intermediate Zustand Änderungen ohne adding Eine new sichtbar Knoten
zu Die timeline. für Beispiel, you kann split several `modifyTileNBT` updates across mehrere ticks, mark Die
intermediate keyframes als ausgeblendet, und Beibehalten nur Die major beats sichtbar auf Die progress bar.

### Kamerafelder

alle Kamerafelder sind optional. beliebig `null` oder omitted Feld inherits seine Wert von Die nearest
prior keyframe dass defined it; Wenn nein prior keyframe defined Die Feld, Die Szene's Standard
Kamera Wert ist verwendet.

| Feld | Typ | Beschreibung |
|-------|------|-------------|
| `zoom` | Gleitkommazahl | Kamera Zoom level (0.1 - 10.0). |
| `rotX` | Gleitkommazahl | X-axis Drehung in degrees. |
| `rotY` | Gleitkommazahl | Y-axis Drehung in degrees. |
| `rotZ` | Gleitkommazahl | Z-axis Drehung in degrees. |
| `offX` | Gleitkommazahl | horizontal Schwenk offset in Bildschirm Pixel. |
| `offY` | Gleitkommazahl | vertikal Schwenk offset in Bildschirm Pixel. |

Die Kamera smoothly interpolates zwischen adjacent keyframes using ein **ease-in/ease-out** Kurve.
verwenden `cameraEaseTicks` auf Die **destination** keyframe zu control Die easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
```

## Block Änderungen

Die `blockChanges` array in Eine keyframe replaces Blöcke in Die live structure wenn dass keyframe
wird zu active. dies erlaubt Die animation zu anzeigen vor-und-nach states, platzieren oder entfernen
Blöcke, oder animate Eine machine powering auf.

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

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `x`, `y`, `z` | Ganzzahl | - | **erforderlich.** Position von Die Block zu ändern (structure Koordinaten). |
| `block` | Zeichenkette | - | **erforderlich.** Registrierungsname, e.g. `"minecraft:furnace"`. verwenden `"minecraft:air"` zu entfernen. |
| `meta` | Ganzzahl | `0` | Block metadata / damage Wert. |
| `particles` | Boolesch | `true` | Whether zu spawn Block-texture particle effects wenn dies Block ändern fires during forward playback. Particles sind taken von Die Block's eigene icon texture. Setzen Sie zu `false` zu suppress (e.g., für silent removal). |
| `nbt` | Zeichenkette | `null` | SNBT Zeichenkette für Eine tile entity Tag, e.g. für chests, furnaces, etc. geparst mit `JsonToNBT`. Schlüssel muss sein **unquoted** (Standard SNBT format). Ignored Wenn Die Block hat nein tile entity. |

**Seek-safe:** wenn seeking backwards Die Laufzeit restores alle changed positions zu ihre
original structure Zustand, dann re-wendet ein Änderungen von keyframes 0 durch Die aktuell eins.
Die displayed structure ist immer correct regardless von seek direction.

> **Hinweis auf particles:** Block-texture particles fire once, nur during forward playback wenn Die
> keyframe erste wird zu active. Sie sind cleared auf seek, reStart, oder anfänglich laden.

## Keyframe-Sounds

Fügen Sie hinzu ein `sounds` array zu Eine keyframe zu play eins oder mehr Leitfaden sounds wenn Die keyframe wird zu active
during forward playback. Seeking und anfänglich laden do nicht play Keyframe-Sounds; reStart clears Die
play history so Die sounds kann fire again.

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

Sound Felder:

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `sound` | Zeichenkette | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | Zeichenkette | - | Sound Datei id oder Pfad; `guidenh:sounds/machine/start.ogg` wird zu `guidenh:machine.start`. |
| `volume` | Gleitkommazahl | `1.0` | Playback volume vor attenuation. |
| `pitch` | Gleitkommazahl | `1.0` | Playback pitch. |
| `cooldown` | Ganzzahl | `250` | Minimum milliseconds vor Die gleich sound kann play again. |
| `x`, `y`, `z` | Gleitkommazahl | none | optional Szene-space Quelle Position für Bildschirm-space attenuation. |
| `radius` | Gleitkommazahl | Szene short Seite * 0.75 | Attenuation radius in Bildschirm Pixel. |
| `minVolume` | Gleitkommazahl | `0.15` | Minimum attenuation factor. |

---

## Keyframe-Partikel

Fügen Sie hinzu ein `particles` array zu Eine keyframe wenn you want eins-shot particle bursts oder timeline-lokal
weather overlays during forward playback. diese particles sind nicht re-fired during reverse
scrubbing, und seek/reStart clears sie vor replaying Die active Zustand.

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

Explosion Voreinstellung:

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

Weather Voreinstellung:

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

Particle Felder:

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `preset` | Zeichenkette | none | Special Voreinstellung. `explosion` spawns Eine vanilla-style flash/smoke burst. `rain` aktiviert Die weather Voreinstellung. |
| `weather` | Zeichenkette | `rain` | Weather Typ verwendet by `preset: "rain"`. unterstützt Werte: `rain`, `snow`. |
| `name` | Zeichenkette | none | Generic particle appearance. unterstützt Werte: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | Zeichenkette | none | Compatibility aliases für `name`. |
| `x`, `z` | Gleitkommazahl oder array | Szene bounds | Particle origin oder weather coverage. Generic particles verwenden scalar Koordinaten. für `preset: "rain"`, scalar Werte Ziel eins precipitation Spalte und arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | Gleitkommazahl | `0.0` | anfänglich motion Vektor. `motionX/Y/Z` sind accepted aliases. |
| `time` / `lifetime` | Ganzzahl | Voreinstellung-bestimmten | Particle lifetime in ticks. für `preset: "rain"` dies ist Die total weather duration, including fade-in und fade-out. |
| `size` | Gleitkommazahl | Voreinstellung-bestimmten | Generic particle half-Größe in Block units. |
| `amount` | Ganzzahl | Voreinstellung-bestimmten | Generic particle count. für `explosion`, omitted amount scales von `power`. für `preset: "rain"`, dies ist Die average per-tick weather density. |
| `power` | Gleitkommazahl | `2.0` | Explosion strength für Die `explosion` Voreinstellung. |

Weather Voreinstellung Hinweise:

- `preset: "rain"` ist Die shared weather Voreinstellung Eintrag Punkt. verwenden `weather: "rain"` für rainfall oder
  `weather: "snow"` für snowfall.
- dies Voreinstellung ist timeline-owned weather. It unterstützt replay, pause, seek, und fast-forward together
  mit Die rest von Die Ponder timeline.
- für immer-auf Szene weather außerhalb Die Ponder timeline, verwenden Die `<Weather>` Tag innerhalb
  `<GameScene>` stattdessen.
- Weather presets ignore `y`; Die vertikal spawn range ist derived von Die aktuell Szene bounds.
- `x: 5, z: 8` targets eins precipitation Spalte. Arrays verwenden endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` erstellt zwei covered rectangles.
- Wenn eins axis hat extra unmatched array Werte, Die unmatched tail ist ignored.
- Die Laufzeit automatisch shapes Die effect mit Eine short Start transition, Eine steady middle
  section, und Eine Ende transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes ohne splash particles.
- Die weather area ist derived von Die aktuell GameScene bounds so Die effect scales mit Die
  imported structure stattdessen von Eine hard-coded box.
- Die gleich `x/z` Spalte nie stacks mehrere weather Typs at Die gleich time. Earlier overlapping
  weather declarations Beibehalten Die shared columns; later ones nur rendern auf Die remaining area.

---

## Tile Entity NBT Operations

verwenden `mergeTileNBT`, `modifyTileNBT`, und `removeTileNBT` wenn Die Block stays in platzieren but seine
tile entity Daten Änderungen. Operations sind seek-safe: GuideNH restores Die original tile NBT und
dann replays alle operations von keyframe 0 durch Die active keyframe.

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

| Feld | verwendet by | Beschreibung |
|-------|---------|-------------|
| `x`, `y`, `z` | alle | Tile-entity Block Position in structure Koordinaten. |
| `nbt` | `mergeTileNBT` | SNBT compound merged in Die tile entity. Existing compound Schlüssel sind merged recursively; andere Werte ersetzen Die old Wert. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT Pfad mit Liste indexes, e.g. `Items[0].Count` oder `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT Wert written at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow Die gleich idea als Minecraft's `/data` paths: verwenden dots für compound Schlüssel und
`[index]` für Liste Einträge. Liste traversal derzeit expects Die traversed Liste Einträge zu sein
compounds, which stimmt überein allgemein tile NBT such als inventories, tanks, und recipe slots.

---

## Entitätsaktionen

Regular `<Entity>` Tags sind bereits unterstützt in `GameScene`. Ponder timelines kann auch erstellen
ihre eigene entities mit `createEntities`, dann Ziel those entities by `ref` in later keyframes.

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

| Feld | Beschreibung |
|-------|-------------|
| `ref` | erforderlich lokal reference Name für later operations. |
| `sceneEntityId` | optional stable Szene-lokal id verwendet für mount relations, replay-safe replacement, import/export restore, und beliebig later removal von dies logischen entity. Standards zu Eine intern id derived von `ref`. |
| `id` | Entity ID, e.g. `minecraft:pig`, `Pig`, oder Eine mod entity ID unterstützt by Die Szene entity loader. |
| `x`, `y`, `z` | optional spawn Position. Standards zu `0, 0, 0` außer `nbt` supplies `Pos`. |
| `yaw`, `pitch` | optional spawn Drehung. Standards zu `0, 0` außer `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | optional living-entity Inhalt/head yaw überschreibt. Wenn omitted während `yaw` ist present, Sie follow `yaw`. |
| `nbt` | optional SNBT compound applied wenn Die entity ist erstellt. |
| `name`, `uuid` | optional Vorschau-player profile Felder wenn creating Eine Vorschau player entity. |
| `mount` | optional stable `sceneEntityId` von Die vehicle dass dies entity sollte ride nach creation oder later replay. |
| `unmount` | optional Boolesch dass clears Die entity's aktuell stable mount relation vor beliebig later `mount` ist applied. |

nach creation, verwenden Die entity NBT operations:

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

`setEntityNBT` ist auch verfügbar wenn you want zu ersetzen Die entity's NBT stattdessen von merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

Entitätsaktionen kann auch aktualisieren transform, Vorschau-player pose, und stable mount Zustand ohne
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

zu detach oder entfernen Eine timeline entity, verwenden `unmount` oder `removeEntities`:

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

Like tile operations, entity operations sind replayed von Die beginning whenever Die active
keyframe Änderungen, so seeking backwards removes Ponder-erstellt entities und recreates Die correct
Zustand für Die Ziel tick.

Hinweise:

- `ref` identifies which Ponder-owned entity Die aktuell action sollte edit oder entfernen.
- `sceneEntityId` und `mount` identify stable cross-entity relations. `mount` immer Punkte zu ein
  stable Szene id, nicht zu another `ref`.
- Relying auf roh passenger NBT alone ist nicht recommended für cross-entity Szene relationships. Die
  stable registry ist what keeps mount und removal behavior deterministic across replay, rebuild,
  import/export, und Editor Vorschau refresh.

---

## Annotation Fade

Annotationen smoothly fade in über **5 game ticks** (250 ms) whenever Die active keyframe
Änderungen during playback. Seeking oder pausing immer zeigt Annotationen at Vollständige opacity.

---

## Annotation-Typn

jede Eintrag in Die `annotations` array requires ein `type` Feld. Seven Typs sind verfügbar.

---

### `diamond`

rendert Eine 3D diamond marker at Eine Welt Position.

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

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `x`, `y`, `z` | Gleitkommazahl | `0.0` | Welt-space Position von Die diamond tip. |
| `color` | Zeichenkette | `"0xFF00E000"` | ARGB Farbe als `"0xAARRGGBB"`. |
| `tooltip` | Zeichenkette | `""` | Fallback Text angezeigt auf Hover. |
| `tooltipKey` | Zeichenkette | `""` | Übersetzung Schlüssel für Die Hover Text. wenn aufgelöst, it überschreibt `tooltip`. |
| `alwaysOnTop` | Boolesch | `false` | Wenn true, gerendert durch solid Blöcke. |

---

### `box`

rendert Eine wireframe axis-aligned box von `min` zu `max`.

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

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `minX/Y/Z` | Gleitkommazahl | `0.0` | Minimum corner. |
| `maxX/Y/Z` | Gleitkommazahl | `1.0` | Maximum corner. |
| `color` | Zeichenkette | `"0xFFFFFFFF"` | ARGB Linie Farbe. |
| `lineWidth` | Gleitkommazahl | Standard | GL Linie Breite. |
| `alwaysOnTop` | Boolesch | `false` | rendern durch Blöcke. |

---

### `block`

rendert Eine wireframe around eins whole Block. dies ist Die Ponder JSON equivalent von
`<BlockAnnotation pos="x y z">` in Eine regular `GameScene`.

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`"type": "blockBox"` und `"type": "block_box"` sind accepted aliases.

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `pos` | Zahl[3] oder Zeichenkette | `[0, 0, 0]` | Block Koordinate als `[x, y, z]` oder `"x y z"`. |
| `x`, `y`, `z` | Zahl | `0` | Alternative Block Koordinate Felder. Werte sind floored. |
| `blockX/Y/Z` | Ganzzahl | `0` | Legacy-style Block Koordinate Felder. |
| `color` | Zeichenkette | `"0xFFFFFFFF"` | ARGB Linie Farbe. |
| `lineWidth` | Gleitkommazahl | Standard | GL Linie Breite. |
| `alwaysOnTop` | Boolesch | `false` | rendern durch Blöcke. |

---

### `line`

rendert Eine Linie segment oder polyline zwischen Welt positions. `points` takes priority über
`fromX/Y/Z` und `toX/Y/Z` wenn it enthält at least zwei gültig Punkte.

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

Polyline Punkte kann sein written entweder als Eine Zeichenkette oder als Eine array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `fromX/Y/Z` | Gleitkommazahl | `0.0` | Start Punkt. |
| `toX/Y/Z` | Gleitkommazahl | `1.0` | Ende Punkt. |
| `points` | Zeichenkette oder array | `null` | Polyline Punkte. verwenden `"x y z; x y z; ..."` oder `[[x,y,z], ...]`. |
| `color` | Zeichenkette | `"0xFFFFFFFF"` | ARGB Linie Farbe. |
| `arrow` | Zeichenkette | `null` | `start` oder `end`; omitted oder ungültig Werte zeichnen nein Pfeil. |
| `lineWidth` | Gleitkommazahl | Standard | GL Linie Breite. |
| `alwaysOnTop` | Boolesch | `false` | rendern durch Blöcke. |

---

### `blockface`

Hebt alle Seiten eines einzelnen Blocks mit einer durchscheinenden Vollflächenüberlagerung hervor.

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` und `"type": "block_face"` sind accepted aliases.

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `pos` | Zahl[3] oder Zeichenkette | `[0, 0, 0]` | Block Koordinate als `[x, y, z]` oder `"x y z"`. |
| `x`, `y`, `z` | Zahl | `0` | Alternative Block Koordinate Felder. Werte sind floored. |
| `blockX/Y/Z` | Ganzzahl | `0` | Legacy-style Block Koordinate Felder. |
| `color` | Zeichenkette | `"0x80FFFFFF"` | ARGB overlay Farbe. |
| `alwaysOnTop` | Boolesch | `false` | rendern durch Blöcke. |

---

### `text`

rendert Eine speech-bubble Beschriftung anchored zu Eine Welt Position. Die box appears über Die anchor by
Standard und ist connected zu it mit Eine short vertikal Linie. Text Inhalt ist keyframe-driven: verwenden
anders `text` Annotation Einträge auf anders keyframes zu ändern Die displayed Text über time.

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

für Eine festen Bildschirm-space Position dass tut nicht project von Welt Koordinaten, verwenden
**independent Modus**. Die bubble ist centered horizontal in Die Szene und placed at
`yOffset` Pixel unter Die Szene's vertikal centre.

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

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `x`, `y`, `z` | Gleitkommazahl | `0.0` | Welt-space anchor Position (ignored in independent Modus). |
| `text` | Zeichenkette | - | **erforderlich.** Text zu anzeigen innerhalb Die bubble. |
| `color` | Zeichenkette | `"0xFFAAAAAA"` | ARGB Farbe von Die bubble Rand. |
| `backgroundAlpha` | Ganzzahl | `204` | Hintergrund opacity von `0` (transparent) zu `255` (opaque). Die RGB Farbe remains Die Standard dark navy. |
| `maxWidth` | Ganzzahl | `0` | Wenn &gt; 0, wraps Text at dies Breite in Pixel. Omit oder Setzen Sie zu `0` für Eine einzeln-Linie Beschriftung. |
| `independent` | Boolesch | `false` | Wenn `true`, Position ist relativ zu Die Szene centre rather als Eine Welt Punkt. |
| `yOffset` | Ganzzahl | `0` | Pixel offset von Die Szene's vertikal centre (positive = downward). verwendet mit `independent: true`. |
| `connectorSide` | Zeichenkette | `"bottom"` | `bottom`, `top`, `left`, `right`, oder `none`. Ignored in independent Modus. |
| `connectorOffset` | Ganzzahl | `0` | Pixel offset along Die ausgewählt bubble edge; positive moves rechts für oben/unten und down für Links/rechts. |
| `connectorLength` | Ganzzahl | `6` | Pixel length von Die connector Linie. `0` hides Die Linie während keeping Seite-based Positionierung. |
| `hlMinX/Y/Z` | Gleitkommazahl | `0.0` | Minimum corner von Eine optional hervorheben box drawn alongside Die Text bubble. |
| `hlMaxX/Y/Z` | Gleitkommazahl | `1.0` | Maximum corner von Die optional hervorheben box. |
| `highlightColor` | Zeichenkette | `"0x8000FFAA"` | ARGB Farbe von Die hervorheben box. |

wenn `hlMinX` (oder beliebig `hlMin/Max` Koordinate) ist present, ein `InWorldBoxAnnotation` ist auch
erstellt at Die specified bounds mit `highlightColor`. dies ist useful für pointing at bestimmten
Block regions während explaining sie.

Die Hintergrund ist ein dark navy bubble by Standard (`#CC0E0E20`), und `backgroundAlpha` controls seine
opacity. in Welt-anchored Modus Eine connector Linie Links Die box zu Die anchor. Text unterstützt Die
Vollständige GuideNH inline rich-Text Syntax: markdown formatting und MDX inline Tags. It ist gerendert mit
drop-shadow.

> **Rich Text:** Die `text` Feld unterstützt Die gleich inline markup verwendet in GuideNH Leitfaden pages:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, und alle andere inline MDX Tags.
> einfach Minecraft `§` format codes sind **nicht** unterstützt; verwenden MDX Syntax stattdessen.

> ein `text` Annotation ohne ein `text` Feld (oder Eine leer Zeichenfolge) ist silently ignored.

---

### `input`

rendert Eine mouse-Eingabe icon (Links button, rechts button, oder scrollen wheel) anchored zu Eine Welt
Position. dies ist verwendet zu hint dass Die player sollte perform Eine bestimmten interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

mit Eine optional modifier Schlüssel prefix und Eine Element icon:

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

| Feld | Typ | Standard | Beschreibung |
|-------|------|---------|-------------|
| `x`, `y`, `z` | Gleitkommazahl | `0.0` | Welt-space anchor Position. |
| `inputType` | Zeichenkette | `"lmb"` | eins von `"lmb"`, `"rmb"`, oder `"scroll"`. Case-insensitive. |
| `modifier` | Zeichenkette | `null` | optional modifier Schlüssel: `"sneak"` oder `"ctrl"`. zeigt prefix Text über Die icon. |
| `item` | Zeichenkette | `null` | optional Element registry ID (e.g. `"minecraft:iron_ingot"`). rendert Die Element icon zu Die Links von Die moVerwenden Sie icon. unterstützt `"modid:item:meta"` format für meta Werte. |

Die icon ist ein 16x16 sprite drawn von `ponder_widgets.png`. Die box Hintergrund ist semi-transparent
dark (`#CC0E0E20`) mit Eine light-blue Rand (`#80AAAADD`). wenn ein `item` ist specified Die box
expands zu accommodate beide Die Element icon und Die moVerwenden Sie icon Seite by Seite.

---

## Farbformat

Colors sind ARGB hexadecimal strings. beide `"0xFFFFFF00"` (mit `0x` prefix) und
`"FFFF00"` (ohne prefix) sind accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% transparent
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (Standard diamond Farbe)
- `"0x8022CCFF"` - semi-transparent blue
- `"0xFFAAAAAA"` - light grey (Standard Text bubble Rand)

## Wiedergabeverhalten

### Controls

| Control | Action |
|---------|--------|
| **Prev keyframe** | Jump zu Die Start von Die vorherig sichtbar keyframe segment. verborgen keyframes sind skipped. |
| **Play/Pause** | Toggle playback; reStarts von Die beginning Wenn bereits finished. |
| **ReStart** | zurückgeben zu tick 0, reset Zustand, und begin playing. |
| Progress bar | Klick oder ziehen zu seek zu beliebig Position. Seeking immer pauses playback. |
| Keyframe Knotens | Small tick marks auf Die bar für sichtbar keyframes; Hover zu Siehe Die Beschriftung und direction Pfeil. |

### anfänglich Zustand

wenn Eine Seite containing `<ImportPonder>` ist erste opened, Die Szene Starts **paused at tick 0**.
Press Play zu begin.

### Kamerasperre

während playback ist **active** (nicht paused):
- Die Kamera follows Die interpolated Pfad defined by keyframes.
- MoVerwenden Sie ziehen und scrollen Zoom sind **deaktiviert**.
- Die layer slider und StructureLib sliders sind **ausgeblendet**.

während playback ist **paused** oder **finished**:
- Vollständige interactive Kamera ziehen, Zoom, und layer/StructureLib control sind restored.

### Keyframe Knoten labels

wenn you Hover über Eine keyframe Knoten auf Die progress bar:
- Die Knoten grows slightly zu indicate it ist hovered.
- Wenn Die keyframe hat ein `label`, it ist displayed beside Die Knoten.
- verborgen keyframes do nicht erstellen hoverable Knotens, but Sie weiterhin apply ihre timeline Zustand wenn playback oder seeking reaches sie.

### Layer control during playback

Die `layer` Feld von Die active keyframe überschreibt Die sichtbar-layer Filter during playback:
- `null` (oder omitted) -> anzeigen alle layers.
- `1`, `2`, `3`, ... -> einschränken zu dass 1-based layer index.

## vollständig Beispiel

Die following Beispiel demonstrates jede Annotation Typ across Eine four-keyframe Szene.

### Verzeichnisstruktur

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

Eine Standard NBT structure Datei (erstellt mit Die `/structure save` command oder Eine tool such als
Litematica). Siehe [Getting Started](Guide-Page-Format) für Die Vollständige SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

Der Zerkleinerer verwandelt Erze in doppelte Staubmenge. Drücke **Wiedergabe**, um die animierte Anleitung zu sehen.
```

## Hinweise

- Die Ponder progress bar ist drawn über Die layer/StructureLib sliders. During playback Die
  structural sliders sind verborgen zu Beibehalten Die UI clean; Sie reappear wenn paused.
- Kamera interpolation ist immer smooth (ease-in/out) even Wenn some keyframes nur ändern ein
  subset von Kamera axes. verwenden `cameraEaseTicks` auf Eine keyframe zu snap Die Kamera instantly (`0`)
  oder ease über Eine festen Zahl von ticks vor holding Die Ziel Position.
- Annotationen belong zu Eine einzeln keyframe - Sie appear nur während dass keyframe ist active
  (i.e., von seine `time` tick until Die nächste keyframe's `time` tick). Overlay Text Annotationen
  fade out smoothly wenn Die keyframe Änderungen during playback.
- Ponder `line` Annotationen verwenden Die gleich Laufzeit renderer als regular `LineAnnotation`, including
  polyline bends und Start/Ende arrows. Punkt marker cubes remain Eine MDX-nur feature.
- Ponder `text` Annotationen verwenden Die gleich Laufzeit renderer als regular `TextAnnotation`, including
  connector Seite, offset, und length. Dynamic Text ist keyframe-based rather als interpolated per tick.
- nur eins `<ImportPonder>` Tag ist effective per `<GameScene>`. Eine second Tag overwrites Die erste.
- ein `text` Annotation mit Eine leer oder absent `text` Feld ist silently skipped.
- Die `inputType` Feld Standards zu `"lmb"` Wenn omitted oder unrecognised.
- `blockChanges` sind applied in Reihenfolge von Die erste zu Die aktuell keyframe jede time Die
  active keyframe Änderungen, so changing Die gleich Position in mehrere keyframes works correctly.
- Tile/entity NBT operations verwenden Die gleich replay model als `blockChanges`; Sie sind safe zu seek
  forwards oder backwards.
- `text` Annotationen mit ein `maxWidth` &gt; 0 sind word-wrapped using Die vanilla font renderer;
  Die bubble box Höhe adjusts automatisch für multi-Linie Text.
- `nbt` strings in `blockChanges` muss verwenden **unquoted** SNBT Schlüssel (Standard MC 1.7.10 format).
  Quoted Schlüssel wird sein rejected by Die Parser. Zeichenkette Werte weiterhin require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` und `modifyEntityNBT` Werte sind SNBT Werte, nicht JSON Werte. für Eine Zeichenfolge
  Wert, escape Die SNBT quotes innerhalb JSON: `"value": "\"hello\""`.
