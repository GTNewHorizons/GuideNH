# Structuurexport

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


## English

`/exportStructure` exports PNG screenshots van either StructureLib previews of loaded GuideNH GameScene blokken. De command is client-side. De `structureLib` subcommand is available alleen wanneer StructureLib is loaded.

De exporter uses orthographic rendering. Output grootte kan wijzigen met De structure of scène grootte, terwijl De configured blok scale stays stable. De Standaard scale is 128 pixels per blok.

### Command Forms

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

voor `structureLib`, `controller` uses `modid:name` of `modid:name:meta`. wanneer omitted, De exporter attempts naar discover alle StructureLib controllers.

voor `gameScene`, no positional selector is gebruikt. It exports iedere GameScene compiled van De currently loaded gidsen.

### Shared parameters

| parameter | Description |
| --- | --- |
| `--out <dir>` | Output directory. Defaults naar `screenshots/structurelib/<timestamp>/` of `screenshots/gameScene/<timestamp>/`. |
| `--pixelsPerBlock <int>` | Pixel density per wereld blok. Standaard: `128`. |
| `--scale <float>` | Multiplies `pixelsPerBlock`. |
| `--layers <expr\|each\|all>` | Layer zichtbaarheid. Standaard: `all`. |
| `--view <preset>` | camera preset. StructureLib Standaard: `isometric-south-east`. GameScene Standaard: De scène's own camera. |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | Fine camera override. In GameScene modus, any explicit view/rotatie optie switches van De scène camera naar fitted export camera. |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | Compatibility aliases voor camera override. |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG achtergrond. Standaard: `transparent`. |
| `--maxPixels <long>` | Maximum pixel count voor one afbeelding. Standaard: `655360000`. gebruiken `-1` voor no limit. |
| `--batchSize <int>` | Flushes `manifest.json` na Deze many completed results. Standaard: `16`. |
| `--force` | staat toe more than 256 generated screenshots. |
| `--dry-run` | Builds De plan en manifest zonder writing PNG files. |
| `--config <file>` / `@file.json` | laadt JSON configuration. |

### StructureLib parameters

| parameter | Description |
| --- | --- |
| `--tier <expr>` | Master tier waarden. |
| `--channel <name=expr>` | Named StructureLib channel waarden. kan be repeated. |
| `--facing <list>` | Facing waarden voor bulk orientation export. |
| `--rotation <list>` | rotatie waarden voor bulk orientation export. |
| `--flip <list>` | Flip waarden voor bulk orientation export. |
| `--orientation <facing:rotation:flip,...>` | Explicit orientation combinations. |
| `--gt-active-controller` | GregTech alleen. Renders De controller met its active machine texture wanneer possible. |
| `--gt-place-hatches` | GregTech alleen. Enables normal GT Hatch channel placement voor Hatch-alleen voorbeeldweergave positions; fallback casing remains De Standaard wanneer omitted. |

### GameScene parameters

| parameter | Description |
| --- | --- |
| `--show-annotations` | Renders scène Annotaties, including in-wereld en overlay Annotaties. Standaard: `false`. |
| `--show-grid` | Renders De scène floor grid. Standaard: `false`. |

GameScene modus respects elke scène's own configured camera by Standaard. gebruiken `--view`, `--yaw`, `--pitch`, `--roll`, `--rotateX`, `--rotateY`, of `--rotateZ` wanneer you want Een fitted export view instead.

### Numeric Filters

Numeric filters are gebruikt by `--tier`, `--channel`, en `--layers`.

```text
0
0-12
0-12,!5
!0,1
```

`0` matches one waarde. `0-12` matches Een inclusive range. `!` excludes waarden. Commas combine tokens.

### Layers

`--layers all` exports De full structure of scène in one afbeelding.

`--layers 0-12,!5` exports one afbeelding where alleen matching layers are zichtbaar.

`--layers each` exports one afbeelding voor iedere actual Y layer.

Layer-filtered rendering forces exposed blok faces naar renderen so verborgen neighboring layers do niet leave missing faces.

### StructureLib Tiers en Channels

wanneer `--tier` en `--channel` are omitted, De exporter inspects De controller en exports one screenshot Voor elke available unified tier. De zelfde tier waarde is applied naar De master tier en iedere discovered StructureLib channel. Automatic unified tier export is capped at 100 screenshots per controller/orientation.

wanneer `--tier` is provided en `--channel` wordt weggelaten, elke requested tier ook drives iedere discovered channel met De zelfde waarde, clamped naar elke channel's supported range.

wanneer one of more `--channel` opties are provided, those explicit channel waarden are gebruikt. meerdere explicit tier en channel waarden are combined as Een Cartesian product.

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech opties

GregTech integration keeps optioneel Hatch-capable positions as their normal fallback casing by Standaard. Forced Hatch elements, such as Een Muffler positie, still renderen as De requested Hatch.

gebruiken `--gt-place-hatches` wanneer you want GT's normal StructureLib Hatch channel logic naar place Vereist Hatches voor voorbeeldweergave-alleen screenshots. voor example, Een element declared met `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` kan place De Vereist Hatch previews en bijwerken their textures instead of showing alleen De fallback casing.

gebruiken `--gt-active-controller` wanneer Een controller zou moeten renderen met its active texture. De exporter still performs voorbeeldweergave status synchronization zonder treating Een failed machine check as Een export failure.

### Orientation

Bulk syntaxis:

```text
--facing north,south --rotation normal,clockwise --flip none
```

Explicit syntaxis:

```text
--orientation north:normal:none,south:clockwise:none
```

Both forms kan be gebruikt together. Invalid combinations are skipped wanneer StructureLib alignment limits reject them. Als no orientation is specified, De controller Standaard is gebruikt.

### Views

Supported presets include:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

You kan refine any preset:

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON Config

StructureLib example:

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

GameScene example:

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

Run configs met:

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

Relative config paths are searched van De huidige working directory en `config/guidenh/structure_exports/`.

### Uitvoer en manifest

elke export directory bevat PNG files en `manifest.json`.

StructureLib afbeelding names begin met De controller item weergeven naam. Variant suffixes include tier, channels, layers, orientation, en view.

GameScene afbeelding names include gids ID, pagina-ID, scène index, layers, camera modus, en optioneel annotatie/grid suffixes.

Names are sanitized voor Windows filename Regels. De manifest records output pad, afbeelding grootte, geselecteerd variants, warnings, en errors.

### Prestaties

De command refuses plans over 256 screenshots unless `--force` is present.

By Standaard, one afbeelding kan niet exceed `655360000` pixels. Deze Standaard is sized van Een 200x100x200 class machine using Een 128 pixels-per-projected-blok budget. Als Een giant structure still needs more room, lower `--pixelsPerBlock` of `--scale`, crop met `--layers`, raise `--maxPixels`, of gebruiken `--maxPixels -1` naar disable De pixel limit.

Large afbeeldingen gebruiken tiled framebuffer rendering wanneer they exceed De GPU texture grootte.
