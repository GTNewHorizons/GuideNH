# Structure Export


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

## English

`/exportStructure` exports PNG screenshots depuis l’un ou l’autre StructureLib previews ou chargé GuideNH GameScene blocs. Le command est client-côté. Le `structureLib` subcommand est disponible seulement lorsque StructureLib est chargé.

Le exporter utilise orthographic rendu. sortie taille peut modifier avec Le structure ou scène taille, pendant Le configured bloc scale stays stable. Le par défaut scale est 128 pixels per bloc.

### Command Forms

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

pour `structureLib`, `controller` utilise `modid:name` ou `modid:name:meta`. lorsque omitted, Le exporter attempts vers discover tous StructureLib controllers.

pour `gameScene`, non positional selector est utilisé. It exports chaque GameScene compilé depuis Le actuellement chargé guides.

### Shared paramètres

| paramètre | description |
| --- | --- |
| `--out <dir>` | sortie répertoire. Defaults vers `screenshots/structurelib/<timestamp>/` ou `screenshots/gameScene/<timestamp>/`. |
| `--pixelsPerBlock <int>` | Pixel density per monde bloc. par défaut: `128`. |
| `--scale <float>` | Multiplies `pixelsPerBlock`. |
| `--layers <expr\|each\|all>` | Layer visibilité. par défaut: `all`. |
| `--view <preset>` | caméra preset. StructureLib par défaut: `isometric-south-east`. GameScene par défaut: Le scène's propre caméra. |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | Fine caméra remplacer. dans GameScene mode, quelconque explicite view/rotation option switches depuis Le scène caméra vers fitted export caméra. |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | Compatibility aliases pour caméra remplacer. |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG arrière-plan. par défaut: `transparent`. |
| `--maxPixels <long>` | Maximum pixel count pour un image. par défaut: `655360000`. utiliser `-1` pour non limit. |
| `--batchSize <int>` | Flushes `manifest.json` après ceci many completed results. par défaut: `16`. |
| `--force` | permet plus que 256 généré screenshots. |
| `--dry-run` | Builds Le plan et manifest sans writing PNG files. |
| `--config <file>` / `@file.json` | charge JSON configuration. |

### StructureLib paramètres

| paramètre | description |
| --- | --- |
| `--tier <expr>` | Master tier valeurs. |
| `--channel <name=expr>` | Named StructureLib channel valeurs. peut être repeated. |
| `--facing <list>` | Facing valeurs pour bulk orientation export. |
| `--rotation <list>` | rotation valeurs pour bulk orientation export. |
| `--flip <list>` | Flip valeurs pour bulk orientation export. |
| `--orientation <facing:rotation:flip,...>` | explicite orientation combinations. |
| `--gt-active-controller` | GregTech seulement. Renders Le controller avec son active machine texture lorsque possible. |
| `--gt-place-hatches` | GregTech seulement. Enables normal GT Hatch channel placement pour Hatch-seulement aperçu positions; repli casing remains Le par défaut lorsque omitted. |

### GameScene paramètres

| paramètre | description |
| --- | --- |
| `--show-annotations` | Renders scène annotations, including dans-monde et overlay annotations. par défaut: `false`. |
| `--show-grid` | Renders Le scène floor grid. par défaut: `false`. |

GameScene mode respects chaque scène's propre configured caméra by par défaut. utiliser `--view`, `--yaw`, `--pitch`, `--roll`, `--rotateX`, `--rotateY`, ou `--rotateZ` lorsque you want Un fitted export view à la placer.

### Numeric Filters

Numeric filters sont utilisé by `--tier`, `--channel`, et `--layers`.

```text
0
0-12
0-12,!5
!0,1
```

`0` correspond un valeur. `0-12` correspond Un inclusive range. `!` excludes valeurs. Commas combine tokens.

### Layers

`--layers all` exports Le complet structure ou scène dans un image.

`--layers 0-12,!5` exports un image where seulement correspondant layers sont visible.

`--layers each` exports un image pour chaque actuel Y layer.

Layer-filtered rendu forces exposed bloc faces vers rendent so masquées neighboring layers do ne leave manquant faces.

### StructureLib Tiers et Channels

lorsque `--tier` et `--channel` sont omitted, Le exporter inspects Le controller et exports un screenshot pour chaque disponible unified tier. Le même tier valeur est applied vers Le master tier et chaque discovered StructureLib channel. automatique unified tier export est capped at 100 screenshots per controller/orientation.

lorsque `--tier` est fourni et `--channel` est omitted, chaque requested tier aussi drives chaque discovered channel avec Le même valeur, clamped vers chaque channel's Pris en charge range.

lorsque un ou plus `--channel` options sont fourni, those explicite channel valeurs sont utilisé. plusieurs explicite tier et channel valeurs sont combined as Un Cartesian product.

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech options

GregTech integration keeps facultatif Hatch-capable positions as leurs normal repli casing by par défaut. Forced Hatch elements, comme Un Muffler position, encore rendent as Le requested Hatch.

utiliser `--gt-place-hatches` lorsque you want GT's normal StructureLib Hatch channel logic vers placer obligatoire Hatches pour aperçu-seulement screenshots. pour Exemple, Un element declared avec `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` peut placer Le obligatoire Hatch previews et mettre à jour leurs textures à la placer of showing seulement Le repli casing.

utiliser `--gt-active-controller` lorsque Un controller devrait rendent avec son active texture. Le exporter encore performs aperçu état synchronization sans treating Un failed machine check as Un export failure.

### Orientation

Bulk syntaxe:

```text
--facing north,south --rotation normal,clockwise --flip none
```

explicite syntaxe:

```text
--orientation north:normal:none,south:clockwise:none
```

les deux forms peut être utilisé together. invalide combinations sont skipped lorsque StructureLib alignment limits reject eux. Si non orientation est specified, Le controller par défaut est utilisé.

### Views

Pris en charge presets inclure:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

You peut refine quelconque preset:

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON Config

StructureLib Exemple:

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

GameScene Exemple:

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

Run configs avec:

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

relatif config paths sont searched depuis Le actuel working répertoire et `config/guidenh/structure_exports/`.

### sortie et Manifest

chaque export répertoire contient PNG files et `manifest.json`.

StructureLib image names début avec Le controller élément afficher nom. Variant suffixes inclure tier, channels, layers, orientation, et view.

GameScene image names inclure guide ID, page ID, scène index, layers, caméra mode, et facultatif annotation/grid suffixes.

Names sont sanitized pour Windows filename Règles. Le manifest records sortie chemin, image taille, sélectionné variants, warnings, et errors.

### Performance

Le command refuses plans sur 256 screenshots sauf `--force` est present.

By par défaut, un image ne peut pas exceed `655360000` pixels. ceci par défaut est sized depuis Un 200x100x200 class machine using Un 128 pixels-per-projected-bloc budget. Si Un giant structure encore needs plus room, lower `--pixelsPerBlock` ou `--scale`, crop avec `--layers`, raise `--maxPixels`, ou utiliser `--maxPixels -1` vers disable Le pixel limit.

Large images utiliser tiled framebuffer rendu lorsque Elles exceed Le GPU texture taille.
