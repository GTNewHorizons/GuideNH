# Strukturexport


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

## English

`/exportStructure` exports PNG screenshots von entweder StructureLib previews oder geladen GuideNH GameScene Blöcke. Die command ist client-Seite. Die `structureLib` subcommand ist verfügbar nur wenn StructureLib ist geladen.

Die exporter verwendet orthographic Darstellung. Ausgabe Größe kann ändern mit Die structure oder Szene Größe, während Die configured Block scale stays stable. Die Standard scale ist 128 Pixel per Block.

### Command Forms

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

für `structureLib`, `controller` verwendet `modid:name` oder `modid:name:meta`. wenn omitted, Die exporter attempts zu discover alle StructureLib controllers.

für `gameScene`, nein positional selector ist verwendet. It exports jede GameScene kompiliert von Die derzeit geladen Guides.

### Shared Parameter

| Parameter | Beschreibung |
| --- | --- |
| `--out <dir>` | Ausgabe Verzeichnis. Standards zu `screenshots/structurelib/<timestamp>/` oder `screenshots/gameScene/<timestamp>/`. |
| `--pixelsPerBlock <int>` | Pixel density per Welt Block. Standard: `128`. |
| `--scale <float>` | Multiplies `pixelsPerBlock`. |
| `--layers <expr\|each\|all>` | Layer Sichtbarkeit. Standard: `all`. |
| `--view <preset>` | Kamera Voreinstellung. StructureLib Standard: `isometric-south-east`. GameScene Standard: Die Szene's eigene Kamera. |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | Fine Kamera Überschreibung. in GameScene Modus, beliebig explizit view/Drehung Option switches von Die Szene Kamera zu fitted export Kamera. |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | Compatibility aliases für Kamera Überschreibung. |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG Hintergrund. Standard: `transparent`. |
| `--maxPixels <long>` | Maximum pixel count für eins Bild. Standard: `655360000`. verwenden `-1` für nein limit. |
| `--batchSize <int>` | Flushes `manifest.json` nach dies many completed results. Standard: `16`. |
| `--force` | erlaubt mehr als 256 generiert screenshots. |
| `--dry-run` | Builds Die plan und manifest ohne writing PNG files. |
| `--config <file>` / `@file.json` | lädt JSON configuration. |

### StructureLib Parameter

| Parameter | Beschreibung |
| --- | --- |
| `--tier <expr>` | Master tier Werte. |
| `--channel <name=expr>` | Named StructureLib channel Werte. kann sein repeated. |
| `--facing <list>` | Facing Werte für bulk orientation export. |
| `--rotation <list>` | Drehung Werte für bulk orientation export. |
| `--flip <list>` | Flip Werte für bulk orientation export. |
| `--orientation <facing:rotation:flip,...>` | explizit orientation combinations. |
| `--gt-active-controller` | GregTech nur. rendert Die controller mit seine active machine texture wenn possible. |
| `--gt-place-hatches` | GregTech nur. aktiviert normal GT Hatch channel Positionierung für Hatch-nur Vorschau positions; Fallback casing remains Die Standard wenn omitted. |

### GameScene Parameter

| Parameter | Beschreibung |
| --- | --- |
| `--show-annotations` | rendert Szene Annotationen, including in-Welt und overlay Annotationen. Standard: `false`. |
| `--show-grid` | rendert Die Szene Bodengitter. Standard: `false`. |

GameScene Modus respects jede Szene's eigene configured Kamera by Standard. verwenden `--view`, `--yaw`, `--pitch`, `--roll`, `--rotateX`, `--rotateY`, oder `--rotateZ` wenn you want Eine fitted export view stattdessen.

### Numeric Filter

Numeric Filter sind verwendet by `--tier`, `--channel`, und `--layers`.

```text
0
0-12
0-12,!5
!0,1
```

`0` stimmt überein eins Wert. `0-12` stimmt überein Eine inclusive range. `!` excludes Werte. Commas combine tokens.

### Layers

`--layers all` exports Die Vollständige structure oder Szene in eins Bild.

`--layers 0-12,!5` exports eins Bild where nur passend layers sind sichtbar.

`--layers each` exports eins Bild für jede aktuell Y layer.

Layer-filtered Darstellung forces exposed Block faces zu rendern so verborgen neighboring layers do nicht leave fehlend faces.

### StructureLib Tiers und Kanäle

wenn `--tier` und `--channel` sind omitted, Die exporter inspects Die controller und exports eins screenshot für jede verfügbar unified tier. Die gleich tier Wert ist applied zu Die master tier und jede discovered StructureLib channel. automatisch unified tier export ist capped at 100 screenshots per controller/orientation.

wenn `--tier` ist bereitgestellt und `--channel` ist omitted, jede requested tier auch drives jede discovered channel mit Die gleich Wert, clamped zu jede channel's unterstützt range.

wenn eins oder mehr `--channel` Optionen sind bereitgestellt, those explizit channel Werte sind verwendet. mehrere explizit tier und channel Werte sind kombiniert als Eine Cartesian product.

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech Optionen

GregTech integration keeps optional Hatch-capable positions als ihre normal Fallback casing by Standard. Forced Hatch elements, such als Eine Muffler Position, weiterhin rendern als Die requested Hatch.

verwenden `--gt-place-hatches` wenn you want GT's normal StructureLib Hatch channel logic zu platzieren erforderlich Hatches für Vorschau-nur screenshots. für Beispiel, Eine element declared mit `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` kann platzieren Die erforderlich Hatch previews und aktualisieren ihre textures stattdessen von showing nur Die Fallback casing.

verwenden `--gt-active-controller` wenn Eine controller sollte rendern mit seine active texture. Die exporter weiterhin performs Vorschau Zustand synchronization ohne treating Eine failed machine check als Eine export failure.

### Orientation

Bulk Syntax:

```text
--facing north,south --rotation normal,clockwise --flip none
```

explizit Syntax:

```text
--orientation north:normal:none,south:clockwise:none
```

beide forms kann sein verwendet together. ungültig combinations sind skipped wenn StructureLib alignment limits reject sie. Wenn nein orientation ist specified, Die controller Standard ist verwendet.

### Views

unterstützt presets einschließen:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

You kann refine beliebig Voreinstellung:

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON Config

StructureLib Beispiel:

```json
{
  "controller": "gregtech:gt.blockmachines:1234",
  "out": "screenshots/structurelib/demo",
  "pixelsPerBlock": 128,
  "scale": 1.0,
  "tier": "1-4",
  "channels": {
    "coil": "1-4",
    "casing": "1,2"
  },
  "layers": "0-12,!5",
  "orientation": "north:normal:none,south:clockwise:none",
  "view": "isometric-south-east",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "gtActiveController": false,
  "gtPlaceHatches": false,
  "force": false,
  "dryRun": false
}
```

GameScene Beispiel:

```json
{
  "out": "screenshots/gameScene/demo",
  "pixelsPerBlock": 128,
  "scale": 1.0,
  "layers": "all",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "showAnnotations": false,
  "showGrid": false,
  "force": false,
  "dryRun": false
}
```

Run configs mit:

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

relativ config paths sind Sucheed von Die aktuell working Verzeichnis und `config/guidenh/structure_exports/`.

### Ausgabe und Manifest

jede export Verzeichnis enthält PNG files und `manifest.json`.

StructureLib Bild names Start mit Die controller Element anzeigen Name. Variant suffixes einschließen tier, Kanäle, layers, orientation, und view.

GameScene Bild names einschließen Leitfaden ID, Seite ID, Szene index, layers, Kamera Modus, und optional Annotation/grid suffixes.

Names sind sanitized für Windows filename Regeln. Die manifest records Ausgabe Pfad, Bild Größe, ausgewählt variants, warnings, und errors.

### Performance

Die command refuses plans über 256 screenshots außer `--force` ist present.

By Standard, eins Bild kann nicht exceed `655360000` Pixel. dies Standard ist sized von Eine 200x100x200 class machine using Eine 128 Pixel-per-projected-Block budget. Wenn Eine giant structure weiterhin needs mehr room, lower `--pixelsPerBlock` oder `--scale`, crop mit `--layers`, raise `--maxPixels`, oder verwenden `--maxPixels -1` zu disable Die pixel limit.

Large Bilder verwenden tiled framebuffer Darstellung wenn Sie exceed Die GPU texture Größe.
