# Eksport struktury

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


## English

`/exportStructure` exports PNG screenshots z either StructureLib previews lub załadowany GuideNH GameScene bloki.  command is client-strona.  `structureLib` subcommand is dostępny tylko gdy StructureLib is załadowany.

 exporter używa orthographic renderowanie. wyjście rozmiar może zmiana z  struktura lub scena rozmiar, podczas  configured blok scale stays stable.  Domyślne scale is 128 piksele per blok.

### Command Forms

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

dla `structureLib`, `controller` używa `modid:name` lub `modid:name:meta`. gdy pominięty,  exporter attempts do discover wszystkie StructureLib controllers.

dla `gameScene`, no positional selector is używany. It exports każdy GameScene skompilowany z  obecnie załadowany przewodniki.

### Shared parametry

| parametr | opis |
| --- | --- |
| `--out <dir>` | wyjście katalog. domyślne do `screenshots/structurelib/<timestamp>/` lub `screenshots/gameScene/<timestamp>/`. |
| `--pixelsPerBlock <int>` | piksel density per świat blok. Domyślne: `128`. |
| `--scale <float>` | Multiplies `pixelsPerBlock`. |
| `--layers <expr\|each\|all>` | Layer widoczność. Domyślne: `all`. |
| `--view <preset>` | kamera preset. StructureLib Domyślne: `isometric-south-east`. GameScene Domyślne:  scena's własny kamera. |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | Fine kamera nadpisywać. In GameScene tryb, any jawny widok/obrót opcja switches z  scena kamera do fitted export kamera. |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | Compatibility aliases dla kamera nadpisywać. |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG tło. Domyślne: `transparent`. |
| `--maxPixels <long>` | Maximum piksel count dla jeden obraz. Domyślne: `655360000`. używać `-1` dla no limit. |
| `--batchSize <int>` | Flushes `manifest.json` po Ten many completed wyniki. Domyślne: `16`. |
| `--force` | pozwala more niż 256 wygenerowany screenshots. |
| `--dry-run` | Builds  plan i manifest bez writing PNG files. |
| `--config <file>` / `@file.json` | ładuje JSON configuration. |

### StructureLib parametry

| parametr | opis |
| --- | --- |
| `--tier <expr>` | Master tier wartości. |
| `--channel <name=expr>` | Named StructureLib channel wartości. może be repeated. |
| `--facing <list>` | Facing wartości dla bulk orientacja export. |
| `--rotation <list>` | obrót wartości dla bulk orientacja export. |
| `--flip <list>` | Flip wartości dla bulk orientacja export. |
| `--orientation <facing:rotation:flip,...>` | jawny orientacja combinations. |
| `--gt-active-controller` | GregTech tylko. Renders  controller z jego active machine texture gdy possible. |
| `--gt-place-hatches` | GregTech tylko. Enables normal GT Hatch channel placement dla Hatch-tylko podgląd positions; awaryjny casing remains  Domyślne gdy pominięty. |

### GameScene parametry

| parametr | opis |
| --- | --- |
| `--show-annotations` | Renders scena Adnotacje, including in-świat i overlay Adnotacje. Domyślne: `false`. |
| `--show-grid` | Renders  scena floor grid. Domyślne: `false`. |

GameScene tryb respects każdy scena's własny configured kamera by Domyślne. używać `--view`, `--yaw`, `--pitch`, `--roll`, `--rotateX`, `--rotateY`, lub `--rotateZ` gdy you want fitted export widok zamiast tego.

### Numeric Filters

Numeric filters są używany by `--tier`, `--channel`, i `--layers`.

```text
0
0-12
0-12,!5
!0,1
```

`0` pasuje jeden wartość. `0-12` pasuje inclusive range. `!` excludes wartości. Commas combine tokens.

### Layers

`--layers all` exports  full struktura lub scena in jeden obraz.

`--layers 0-12,!5` exports jeden obraz where tylko pasujący layers są widoczny.

`--layers each` exports jeden obraz dla każdy actual Y layer.

Layer-filtered renderowanie forces exposed blok faces do renderować so ukryty neighboring layers do nie leave missing faces.

### StructureLib Tiers i kanały

gdy `--tier` i `--channel` są pominięty,  exporter inspects  controller i exports jeden screenshot Dla każdego dostępny unified tier.  ten sam tier wartość is applied do  master tier i każdy discovered StructureLib channel. automatyczny unified tier export is capped at 100 screenshots per controller/orientacja.

gdy `--tier` is provided i `--channel` is pominięty, każdy requested tier także drives każdy discovered channel z  ten sam wartość, clamped do każdy channel's Obsługiwane range.

gdy jeden lub more `--channel` opcje są provided, those jawny channel wartości są używany. wiele jawny tier i channel wartości są combined as Cartesian product.

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech opcje

GregTech integration zachowuje opcjonalny Hatch-capable positions as ich normal awaryjny casing by Domyślne. Forced Hatch elements, takie jak Muffler pozycja, nadal renderować as  requested Hatch.

używać `--gt-place-hatches` gdy you want GT's normal StructureLib Hatch channel logic do place Wymagane Hatches dla podgląd-tylko screenshots. dla Przykład, element declared z `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` może place  Wymagane Hatch previews i aktualizować ich textures zamiast tego of showing tylko  awaryjny casing.

używać `--gt-active-controller` gdy controller powinien renderować z jego active texture.  exporter nadal performs podgląd stan synchronization bez treating failed machine check as export failure.

### orientacja

Bulk składnia:

```text
--facing north,south --rotation normal,clockwise --flip none
```

jawny składnia:

```text
--orientation north:normal:none,south:clockwise:none
```

Both forms może be używany together. nieprawidłowy combinations są skipped gdy StructureLib alignment limits reject ich. Jeśli no orientacja is specified,  controller Domyślne is używany.

### Views

Obsługiwane presets uwzględniać:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

You może refine any preset:

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON Config

StructureLib Przykład:

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

GameScene Przykład:

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

Run configs z:

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

względny config paths są searched z  bieżący working katalog i `config/guidenh/structure_exports/`.

### Wyjście i manifest

każdy export katalog zawiera PNG files i `manifest.json`.

StructureLib obraz names początek z  controller element wyświetlać nazwa. Variant suffixes uwzględniać tier, kanały, layers, orientacja, i widok.

GameScene obraz names uwzględniać przewodnik ID, ID strony, scena index, layers, kamera tryb, i opcjonalny adnotacja/grid suffixes.

Names są sanitized dla Windows filename Zasady.  manifest records wyjście ścieżka, obraz rozmiar, wybrany variants, warnings, i errors.

### Wydajność

 command refuses plans over 256 screenshots unless `--force` is present.

By Domyślne, jeden obraz nie może exceed `655360000` piksele. Ten Domyślne is sized z 200x100x200 class machine używając 128 piksele-per-projected-blok budget. Jeśli giant struktura nadal needs more room, lower `--pixelsPerBlock` lub `--scale`, crop z `--layers`, raise `--maxPixels`, lub używać `--maxPixels -1` do disable  piksel limit.

Large obrazy używać tiled framebuffer renderowanie gdy jeden exceed  GPU texture rozmiar.
