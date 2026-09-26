# Exportação de estrutura

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


## English

`/exportStructure` exports PNG screenshots de qualquer um StructureLib previews ou carregado GuideNH GameScene blocos. O command é client-lado. O `structureLib` subcommand é disponível somente quando StructureLib é carregado.

O exporter usa orthographic renderização. saída tamanho pode alterar com O structure ou cena tamanho, enquanto O configured bloco scale stays stable. O Padrão scale é 128 pixels per bloco.

### Command Forms

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

para `structureLib`, `controller` usa `modid:name` ou `modid:name:meta`. quando omitted, O exporter attempts para discover todos StructureLib controllers.

para `gameScene`, não positional selector é usado. It exports cada GameScene compilado de O atualmente carregado guias.

### Shared parâmetros

| parâmetro | descrição |
| --- | --- |
| `--out <dir>` | saída diretório. Defaults para `screenshots/structurelib/<timestamp>/` ou `screenshots/gameScene/<timestamp>/`. |
| `--pixelsPerBlock <int>` | Pixel density per mundo bloco. Padrão: `128`. |
| `--scale <float>` | Multiplies `pixelsPerBlock`. |
| `--layers <expr\|each\|all>` | Layer visibilidade. Padrão: `all`. |
| `--view <preset>` | câmera preset. StructureLib Padrão: `isometric-south-east`. GameScene Padrão: O cena's próprio câmera. |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | Fine câmera substituir. em GameScene modo, qualquer explícito view/rotação opção switches de O cena câmera para fitted export câmera. |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | Compatibility aliases para câmera substituir. |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG fundo. Padrão: `transparent`. |
| `--maxPixels <long>` | Maximum pixel count para um imagem. Padrão: `655360000`. usar `-1` para não limit. |
| `--batchSize <int>` | Flushes `manifest.json` depois de Este many completed results. Padrão: `16`. |
| `--force` | permite mais que 256 gerado screenshots. |
| `--dry-run` | Builds O plan e manifest sem writing PNG files. |
| `--config <file>` / `@file.json` | carrega JSON configuration. |

### StructureLib parâmetros

| parâmetro | descrição |
| --- | --- |
| `--tier <expr>` | Master tier valores. |
| `--channel <name=expr>` | Named StructureLib channel valores. pode ser repeated. |
| `--facing <list>` | Facing valores para bulk orientation export. |
| `--rotation <list>` | rotação valores para bulk orientation export. |
| `--flip <list>` | Flip valores para bulk orientation export. |
| `--orientation <facing:rotation:flip,...>` | explícito orientation combinations. |
| `--gt-active-controller` | GregTech somente. Renders O controller com seu active machine texture quando possible. |
| `--gt-place-hatches` | GregTech somente. Enables normal GT Hatch channel placement para Hatch-somente visualização positions; reserva casing remains O Padrão quando omitted. |

### GameScene parâmetros

| parâmetro | descrição |
| --- | --- |
| `--show-annotations` | Renders cena Anotações, including em-mundo e overlay Anotações. Padrão: `false`. |
| `--show-grid` | Renders O cena floor grid. Padrão: `false`. |

GameScene modo respects cada cena's próprio configured câmera by Padrão. usar `--view`, `--yaw`, `--pitch`, `--roll`, `--rotateX`, `--rotateY`, ou `--rotateZ` quando you want Uma fitted export view em vez disso.

### Numeric Filters

Numeric filters são usado by `--tier`, `--channel`, e `--layers`.

```text
0
0-12
0-12,!5
!0,1
```

`0` corresponde um valor. `0-12` corresponde Um inclusive range. `!` excludes valores. Commas combine tokens.

### Layers

`--layers all` exports O completo structure ou cena em um imagem.

`--layers 0-12,!5` exports um imagem where somente correspondente layers são visível.

`--layers each` exports um imagem para cada atual Y layer.

Layer-filtered renderização forces exposed bloco faces para renderizar so oculto neighboring layers do não leave ausente faces.

### StructureLib Tiers e Channels

quando `--tier` e `--channel` são omitted, O exporter inspects O controller e exports um screenshot Para cada disponível unified tier. O mesmo tier valor é applied para O master tier e cada discovered StructureLib channel. automático unified tier export é capped at 100 screenshots per controller/orientation.

quando `--tier` é fornecido e `--channel` é omitted, cada requested tier também drives cada discovered channel com O mesmo valor, clamped para cada channel's Compatível range.

quando um ou mais `--channel` opções são fornecido, those explícito channel valores são usado. vários explícito tier e channel valores são combined as Uma Cartesian product.

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech opções

GregTech integration keeps opcional Hatch-capable positions as seus normal reserva casing by Padrão. Forced Hatch elements, como Uma Muffler posição, ainda renderizar as O requested Hatch.

usar `--gt-place-hatches` quando you want GT's normal StructureLib Hatch channel logic para colocar Obrigatório Hatches para visualização-somente screenshots. para Exemplo, Um element declared com `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` pode colocar O Obrigatório Hatch previews e atualizar seus textures em vez disso of showing somente O reserva casing.

usar `--gt-active-controller` quando Uma controller deve renderizar com seu active texture. O exporter ainda performs visualização estado synchronization sem treating Uma failed machine check as Um export failure.

### Orientation

Bulk sintaxe:

```text
--facing north,south --rotation normal,clockwise --flip none
```

explícito sintaxe:

```text
--orientation north:normal:none,south:clockwise:none
```

ambos forms pode ser usado together. inválido combinations são skipped quando StructureLib alignment limits reject eles. Se não orientation é specified, O controller Padrão é usado.

### Views

Compatível presets incluir:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

You pode refine qualquer preset:

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON Config

StructureLib Exemplo:

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

GameScene Exemplo:

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

Run configs com:

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

relativo config paths são searched de O atual working diretório e `config/guidenh/structure_exports/`.

### Saída e manifesto

cada export diretório contém PNG files e `manifest.json`.

StructureLib imagem names emício com O controller item exibir nome. Variant suffixes incluir tier, channels, layers, orientation, e view.

GameScene imagem names incluir guia ID, ID da página, cena index, layers, câmera modo, e opcional anotação/grid suffixes.

Names são sanitized para Windows filename Regras. O manifest records saída caminho, imagem tamanho, selecionado variants, warnings, e errors.

### Desempenho

O command refuses plans sobre 256 screenshots a menos que `--force` é present.

By Padrão, um imagem não pode exceed `655360000` pixels. Este Padrão é sized de Uma 200x100x200 class machine using Uma 128 pixels-per-projected-bloco budget. Se Uma giant structure ainda needs mais room, lower `--pixelsPerBlock` ou `--scale`, crop com `--layers`, raise `--maxPixels`, ou usar `--maxPixels -1` para disable O pixel limit.

Large imagens usar tiled framebuffer renderização quando Elas exceed O GPU texture tamanho.
