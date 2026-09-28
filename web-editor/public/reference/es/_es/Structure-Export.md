# estructura Export


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

## English

`/exportStructure` exports PNG screenshots de cualquiera StructureLib previews o cargado GuideNH GameScene bloques. El command es client-lado. El `structureLib` subcommand es disponible solo cuando StructureLib es cargado.

El exporter usa orthographic renderizado. salida tamaño puede cambiar con El estructura o escena tamaño, mientras El configured bloque scale stays stable. El predeterminado scale es 128 píxeles per bloque.

### Command Forms

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

para `structureLib`, `controller` usa `modid:name` o `modid:name:meta`. cuando omitido, El exporter attempts Un discover todos StructureLib controllers.

para `gameScene`, no positional selector es usado. It exports cada GameScene compilado de El actualmente cargado guías.

### Shared parámetros

| parámetro | descripción |
| --- | --- |
| `--out <dir>` | salida directorio. predeterminados a `screenshots/structurelib/<timestamp>/` o `screenshots/gameScene/<timestamp>/`. |
| `--pixelsPerBlock <int>` | píxel density per mundo bloque. predeterminado: `128`. |
| `--scale <float>` | Multiplies `pixelsPerBlock`. |
| `--layers <expr\|each\|all>` | Layer visibilidad. predeterminado: `all`. |
| `--view <preset>` | cámara preset. StructureLib predeterminado: `isometric-south-east`. GameScene predeterminado: El escena's propio cámara. |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | Fine cámara anular. en GameScene modo, cualquier explícito vista/rotación opción switches de El escena cámara Un fitted export cámara. |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | Compatibility aliases para cámara anular. |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG fondo. predeterminado: `transparent`. |
| `--maxPixels <long>` | Maximum píxel count para uno imagen. predeterminado: `655360000`. usar `-1` para no limit. |
| `--batchSize <int>` | Flushes `manifest.json` después de esto many completed resultados. predeterminado: `16`. |
| `--force` | permite más que 256 generado screenshots. |
| `--dry-run` | Builds El plan y manifest sin writing PNG files. |
| `--config <file>` / `@file.json` | carga JSON configuration. |

### StructureLib parámetros

| parámetro | descripción |
| --- | --- |
| `--tier <expr>` | Master tier valores. |
| `--channel <name=expr>` | Named StructureLib channel valores. puede ser repeated. |
| `--facing <list>` | Facing valores para bulk orientación export. |
| `--rotation <list>` | rotación valores para bulk orientación export. |
| `--flip <list>` | Flip valores para bulk orientación export. |
| `--orientation <facing:rotation:flip,...>` | explícito orientación combinations. |
| `--gt-active-controller` | GregTech solo. renderiza El controller con su active machine texture cuando possible. |
| `--gt-place-hatches` | GregTech solo. Enables normal GT Hatch channel placement para Hatch-solo vista previa positions; reserva casing remains El predeterminado cuando omitido. |

### GameScene parámetros

| parámetro | descripción |
| --- | --- |
| `--show-annotations` | renderiza escena anotaciones, including en-mundo y overlay anotaciones. predeterminado: `false`. |
| `--show-grid` | renderiza El escena floor grid. predeterminado: `false`. |

GameScene modo respects cada escena's propio configured cámara by predeterminado. usar `--view`, `--yaw`, `--pitch`, `--roll`, `--rotateX`, `--rotateY`, o `--rotateZ` cuando you want Un fitted export vista en su lugar.

### Numeric Filters

Numeric filters son usado by `--tier`, `--channel`, y `--layers`.

```text
0
0-12
0-12,!5
!0,1
```

`0` coincide uno valor. `0-12` coincide Un inclusive range. `!` excludes valores. Commas combine tokens.

### Layers

`--layers all` exports El completo estructura o escena en uno imagen.

`--layers 0-12,!5` exports uno imagen where solo coincidente layers son visible.

`--layers each` exports uno imagen para cada actual Y layer.

Layer-filtered renderizado forces exposed bloque faces Un renderizan so ocultas neighboring layers do no leave faltante faces.

### StructureLib Tiers y canales

cuando `--tier` y `--channel` son omitido, El exporter inspects El controller y exports uno screenshot para cada disponible unified tier. El mismo tier valor es applied Un El master tier y cada discovered StructureLib channel. automático unified tier export es capped at 100 screenshots per controller/orientación.

cuando `--tier` es proporcionado y `--channel` es omitido, cada requested tier también drives cada discovered channel con El mismo valor, clamped Un cada channel's Compatible range.

cuando uno o más `--channel` opciones son proporcionado, those explícito channel valores son usado. varios explícito tier y channel valores son combined as Un Cartesian product.

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech opciones

GregTech integration mantiene opcional Hatch-capable positions as sus normal reserva casing by predeterminado. Forced Hatch elements, como Un Muffler posición, todavía renderizan as El requested Hatch.

usar `--gt-place-hatches` cuando you want GT's normal StructureLib Hatch channel logic Un colocar obligatorio Hatches para vista previa-solo screenshots. para Ejemplo, Un element declared con `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` puede colocar El obligatorio Hatch previews y actualizar sus textures en su lugar of showing solo El reserva casing.

usar `--gt-active-controller` cuando Un controller deberíUn renderizan con su active texture. El exporter todavía performs vista previa estado synchronization sin treating Un failed machine check as Un export failure.

### orientación

Bulk sintaxis:

```text
--facing north,south --rotation normal,clockwise --flip none
```

explícito sintaxis:

```text
--orientation north:normal:none,south:clockwise:none
```

ambos forms puede ser usado together. no válido combinations son skipped cuando StructureLib alignment limits reject ellos. Si no orientación es specified, El controller predeterminado es usado.

### Views

Compatible presets incluir:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

You puede refine cualquier preset:

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON Config

StructureLib Ejemplo:

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

GameScene Ejemplo:

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

Run configs con:

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

relativo config paths son searched de El actual working directorio y `config/guidenh/structure_exports/`.

### salida y Manifest

cada export directorio contiene PNG files y `manifest.json`.

StructureLib imagen names inicio con El controller elemento mostrar nombre. Variant suffixes incluir tier, canales, layers, orientación, y vista.

GameScene imagen names incluir guíUn ID, página ID, escena index, layers, cámara modo, y opcional anotación/grid suffixes.

Names son sanitized para Windows filename Reglas. El manifest records salida ruta, imagen tamaño, seleccionado variants, warnings, y errors.

### Performance

El command refuses plans sobre 256 screenshots salvo `--force` es present.

By predeterminado, uno imagen no puede exceed `655360000` píxeles. esto predeterminado es sized de Un 200x100x200 class machine usando Un 128 píxeles-per-projected-bloque budget. Si Un giant estructura todavía needs más room, lower `--pixelsPerBlock` o `--scale`, crop con `--layers`, raise `--maxPixels`, o usar `--maxPixels -1` Un disable El píxel limit.

Large imágenes usar tiled framebuffer renderizado cuando Estas exceed El GPU texture tamaño.
