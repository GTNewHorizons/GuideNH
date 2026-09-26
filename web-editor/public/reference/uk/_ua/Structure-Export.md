# Експорт структури

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


## English

`/exportStructure` exports PNG screenshots з either StructureLib previews або завантажений GuideNH GameScene блоки.  command is client-бік.  `structureLib` subcommand is доступний лише коли StructureLib is завантажений.

 exporter використовує orthographic відтворення. вивід розмір може змінити з  структура або сцена розмір, поки  configured блок scale stays stable.  Типове scale is 128 пікселі per блок.

### Command Forms

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

для `structureLib`, `controller` використовує `modid:name` або `modid:name:meta`. коли пропущений,  exporter attempts до discover усі StructureLib controllers.

для `gameScene`, no positional selector is використовується. It exports кожен GameScene скомпільований з  зараз завантажений посібники.

### Shared параметри

| параметр | опис |
| --- | --- |
| `--out <dir>` | вивід каталог. типові до `screenshots/structurelib/<timestamp>/` або `screenshots/gameScene/<timestamp>/`. |
| `--pixelsPerBlock <int>` | піксель density per світ блок. Типове: `128`. |
| `--scale <float>` | Multiplies `pixelsPerBlock`. |
| `--layers <expr\|each\|all>` | Layer видимість. Типове: `all`. |
| `--view <preset>` | камера preset. StructureLib Типове: `isometric-south-east`. GameScene Типове:  сцена's власний камера. |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | Fine камера перевизначати. In GameScene режим, any явний перегляд/обертання параметр switches з  сцена камера до fitted export камера. |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | Compatibility aliases для камера перевизначати. |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG тло. Типове: `transparent`. |
| `--maxPixels <long>` | Maximum піксель count для один зображення. Типове: `655360000`. використовувати `-1` для no limit. |
| `--batchSize <int>` | Flushes `manifest.json` після Цей many completed результати. Типове: `16`. |
| `--force` | дозволяє more ніж 256 згенерований screenshots. |
| `--dry-run` | Builds  plan і manifest без writing PNG files. |
| `--config <file>` / `@file.json` | завантажує JSON configuration. |

### StructureLib параметри

| параметр | опис |
| --- | --- |
| `--tier <expr>` | Master tier значення. |
| `--channel <name=expr>` | Named StructureLib channel значення. може be repeated. |
| `--facing <list>` | Facing значення для bulk орієнтація export. |
| `--rotation <list>` | обертання значення для bulk орієнтація export. |
| `--flip <list>` | Flip значення для bulk орієнтація export. |
| `--orientation <facing:rotation:flip,...>` | явний орієнтація combinations. |
| `--gt-active-controller` | GregTech лише. Renders  controller з його active machine texture коли possible. |
| `--gt-place-hatches` | GregTech лише. Enables normal GT Hatch channel placement для Hatch-лише перегляд positions; резервний варіант casing remains  Типове коли пропущений. |

### GameScene параметри

| параметр | опис |
| --- | --- |
| `--show-annotations` | Renders сцена Анотації, including in-світ і overlay Анотації. Типове: `false`. |
| `--show-grid` | Renders  сцена floor grid. Типове: `false`. |

GameScene режим respects кожен сцена's власний configured камера by Типове. використовувати `--view`, `--yaw`, `--pitch`, `--roll`, `--rotateX`, `--rotateY`, або `--rotateZ` коли you want fitted export перегляд замість цього.

### Numeric Filters

Numeric filters є використовується by `--tier`, `--channel`, і `--layers`.

```text
0
0-12
0-12,!5
!0,1
```

`0` відповідає один значення. `0-12` відповідає inclusive range. `!` excludes значення. Commas combine tokens.

### Layers

`--layers all` exports  full структура або сцена in один зображення.

`--layers 0-12,!5` exports один зображення where лише відповідний layers є видимий.

`--layers each` exports один зображення для кожен actual Y layer.

Layer-filtered відтворення forces exposed блок faces до відтворювати so прихований neighboring layers do не leave missing faces.

### StructureLib Tiers і канали

коли `--tier` і `--channel` є пропущений,  exporter inspects  controller і exports один screenshot Для кожного доступний unified tier.  той самий tier значення is applied до  master tier і кожен discovered StructureLib channel. автоматичний unified tier export is capped at 100 screenshots per controller/орієнтація.

коли `--tier` is provided і `--channel` is пропущений, кожен requested tier також drives кожен discovered channel з  той самий значення, clamped до кожен channel's Підтримувані range.

коли один або more `--channel` параметри є provided, those явний channel значення є використовується. кілька явний tier і channel значення є combined as Cartesian product.

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech параметри

GregTech integration зберігає необов’язковий Hatch-capable positions as їхні normal резервний варіант casing by Типове. Forced Hatch elements, наприклад Muffler позиція, досі відтворювати as  requested Hatch.

використовувати `--gt-place-hatches` коли you want GT's normal StructureLib Hatch channel logic до place Обов’язково Hatches для перегляд-лише screenshots. для Приклад, element declared з `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` може place  Обов’язково Hatch previews і оновити їхні textures замість цього of showing лише  резервний варіант casing.

використовувати `--gt-active-controller` коли controller слід відтворювати з його active texture.  exporter досі performs перегляд стан synchronization без treating failed machine check as export failure.

### орієнтація

Bulk синтаксис:

```text
--facing north,south --rotation normal,clockwise --flip none
```

явний синтаксис:

```text
--orientation north:normal:none,south:clockwise:none
```

Both forms може be використовується together. недійсний combinations є skipped коли StructureLib alignment limits reject їх. Якщо no орієнтація is specified,  controller Типове is використовується.

### Views

Підтримувані presets включати:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

You може refine any preset:

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON Config

StructureLib Приклад:

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

GameScene Приклад:

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

Run configs з:

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

відносний config paths є searched з  поточний working каталог і `config/guidenh/structure_exports/`.

### Вивід і маніфест

кожен export каталог містить PNG files і `manifest.json`.

StructureLib зображення names початок з  controller предмет відображати назва. Variant suffixes включати tier, канали, layers, орієнтація, і перегляд.

GameScene зображення names включати посібник ID, ідентифікатор сторінки, сцена index, layers, камера режим, і необов’язковий анотація/grid suffixes.

Names є sanitized для Windows filename Правила.  manifest records вивід шлях, зображення розмір, вибраний variants, warnings, і errors.

### Продуктивність

 command refuses plans over 256 screenshots unless `--force` is present.

By Типове, один зображення не може exceed `655360000` пікселі. Цей Типове is sized з 200x100x200 class machine використовуючи 128 пікселі-per-projected-блок budget. Якщо giant структура досі needs more room, lower `--pixelsPerBlock` або `--scale`, crop з `--layers`, raise `--maxPixels`, або використовувати `--maxPixels -1` до disable  піксель limit.

Large зображення використовувати tiled framebuffer відтворення коли Вони exceed  GPU texture розмір.
