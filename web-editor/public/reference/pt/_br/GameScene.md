# GameScene

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


`<GameScene>` é GuideNH's 3D visualização tag. `<Scene>` é Um alias com O mesmo behavior.

## Atributos da cena

| Atributo | Tipo | Padrão | Significado |
| --- | --- | --- | --- |
| `width` | inteiro | `256` | viewport largura em pixels |
| `height` | inteiro | `192` | viewport altura em pixels |
| `zoom` | decimal | `1.0` | câmera zoom multiplier |
| `perspective` | texto | `isometric-north-east` | câmera preset |
| `rotateX` | decimal | auto | explícito X rotação substituir |
| `rotateY` | decimal | auto | explícito Y rotação substituir |
| `rotateZ` | decimal | auto | explícito Z rotação substituir |
| `offsetX` | decimal | auto | espaço da tela horizontal pan |
| `offsetY` | decimal | auto | espaço da tela vertical pan |
| `centerX` | decimal | auto | explícito mundo rotação centro X |
| `centerY` | decimal | auto | explícito mundo rotação centro Y |
| `centerZ` | decimal | auto | explícito mundo rotação centro Z |
| `interactive` | booleano expressão | `true` | enables mouse interaction |
| `showBackground` | booleano expressão | `true` | mostra O cena fundo fill e borda |
| `allowLayerSlider` | booleano | `true` | mostra O vertical layer slider |
| `gridButtonEnabled` | booleano | `true` | mostra O floor grid toggle button |
| `showGrid` | booleano | `false` | inicial visibilidade of O floor grid |

## Sobreposição de estatísticas de blocos

cenas que conter blocos enable O bloco-stat toggle button by Padrão. Adicione a `<BlockStats>`
filho quando you want para substituir seu modo, placement, filters, visibilidade, ou tamanho. O lista é
cached e somente rebuilt quando O cena blocos, Animação Ponder timeline estado, StructureLib seleção, ou
bloco-stat settings alterar; normal renderização reuses O prepared linhas. Long lists são clipped para
`maxWidth` e `maxHeight`; Se those são omitted, cada é O larger of O fixo `224` by `96` pixels e
40% of O cena tamanho.
Overflow receives draggable scrollbars, e O mouse wheel scrolls O lista enquanto O cursor é
sobre O overlay. Hold Shift para wheel-rolar horizontally.

em automático modo, GuideNH scans O cena's filled blocos e resolves cada bloco para O item
stack users normally Consulte. blocos que conter vários visível components pode contribute vários
itens de O mesmo coordenada; Este inclui AE2 cable bus parts e facades, ForgeMultipart part
drops, e Carpenters' blocos covers ou overlays quando those mods são installed. Counts são grouped
by `item:meta` e sorted by count.

automático lists pode também ser docked fora de O cena com `dock="left"`, `dock="top"`,
`dock="right"`, ou `dock="bottom"`. Docked lists wrap em extra columns ou linhas based on O
attached lado length, reserve layout espaço, e avoid O cena button coluna on O direita. clicar an
item em Um automático lista para destacar todos correspondente cena placements com seus resolvido collision
boxes using Um sempre-on-topo face overlay; clicar O mesmo item again para clear O destacar. Counts
são renderizado por O ItemStack stack-tamanho overlay. Defina `showNames={true}` para append O count
depois de cada nome as well, e passar o mouse Um item para Consulte O exact bloco count em O dica.

Filters pode ocultar comum blocos ou mostrar somente selecionado blocos:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

usar manual modo quando Uma guia wants para mostrar Uma planned material lista em vez disso of O literal cena
contents:

````md
<GameScene>
  <Block id="minecraft:furnace" />
  <BlockStats mode="manual" corner="topRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:cobblestone" count="8" />
    <BlockStat item="minecraft:furnace" count="1" />
  </BlockStats>
</GameScene>
````

## Sobreposições do modo de depuração

quando O `enableDebugMode` opção é ativado em O GuideNH mod config, O following extra
overlays become disponível em O 3D cena visualização.

### Grid coordenada Labels

quando debug modo é **on** e O floor grid é **visível**, coordenada labels são renderizado
abaixo cada grid linha:

- **X-axis numbers** são exibido along O perto edge of O grid (north/−Z edge em O Padrão
  `isometric-north-east` câmera).  cada inteiro X mundo-coordenada receives Uma rótulo.
- **Z-axis numbers** são exibido along O perto edge of O grid (east/+X edge).  cada inteiro
  Z mundo-coordenada receives Uma rótulo.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) são drawn at O midpoint of cada
  respective grid edge.

coordenadas follow O atual mundo X/Z valores stored em O cena level, so Elas pode ser
negative quando O structure contém blocos com negative coordenadas.

O grid toggle button é **sempre ativado** enquanto debug modo é active, regardless of O
`gridButtonEnabled` Atributo, so you pode mostrar ou ocultar O grid e seu labels at qualquer time.
O Padrão grid visibilidade (`showGrid`) é não affected.

### bloco coordenada dica

quando debug modo é **on** e O cursor hovers sobre Uma bloco dentro de O cena, Uma second
dica é renderizado acima de O primary bloco dica, showing O espaço do mundo bloco posição
as `X, Y, Z` em gold texto.

Se O coordenada dica would ser clipped at O topo of O tela it automaticamente snaps
abaixo O cursor area em vez disso (magnetic snapping).

## Predefinições de perspectiva

Accepted `perspective` valores:

- `isometric-north-east`
- `isometric-north-west`
- `up`

desconhecido valores fall back para `isometric-north-east`.

## Incorporação de conteúdo e quebra de texto

qualquer bloco-level tag — including `<GameScene>` — suporta dois opcional atributos que control
how it é embedded em O página, mirroring Microsoft Word's "texto Wrapping" opções.

| Atributo | valores | Padrão | Significado |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | texto-wrapping modo |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Modos de quebra

| modo | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | em linha com texto | Padrão flow: cena occupies seu próprio vertical slot (嵌入型) |
| `square` | Square | cena floats esquerda ou direita; surrounding texto wraps em Uma rectangle around it (方形环绕) |
| `tight` | Tight | Tighter wrap; equivalent para `square` em Este layout system (紧密型) |
| `through` | por | por-wrap; equivalent para `square` em Este layout system (穿越型) |
| `top-bottom` | topo e baixo | texto somente acima de e abaixo, não beside; respects `align` para horizontal placement (上下型) |
| `behind` | Behind texto | bloco renders behind surrounding texto; respects `align` (衬于文字下方) |
| `front` | em front of texto | bloco renders em front of surrounding texto; respects `align` (浮于文字上方) |

### Exemplos

esquerda-floating cena — texto em O próximo paragraph wraps para O direita:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Texto que flui à direita da cena…
````

direita-floating cena:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Texto que flui à esquerda da cena…
````

Centred cena (não texto wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline em texto (flow context) — texto wraps around Uma small cena:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

## Exemplo

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Import Exemplos

Estes Exemplos focus on O cena-lado behavior que most frequentemente trips people up quando importing
structures.

StructureLib import com explícito facing, rotação, flip, e offsets:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib
    name="main"
    controller="gregtech:gt.blockmachines:2741"
    facing="north"
    rotation="clockwise_180"
    flip="none"
    offsetX="2"
    offsetY="1"
    offsetZ="-3"
  />
</GameScene>
````

GregTech controllers stay unformed by Padrão, even quando O imported multiblock é otherwise
válido:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

Defina `formed={true}` somente quando O visualização deve intentionally mostrar O formed controller estado:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

O mesmo Padrão também aplica para controllers placed directly com `<Block>`, including GregTech
controllers que rely on surrounding multiblock casings:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

Simple bloco-somente layouts pode ainda ser authored directly e remain compatible com multiblock
inspection logic:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:water" />
  <Block id="minecraft:water" x="-1" />
  <Block id="minecraft:water" x="1" />
  <Block id="minecraft:grass" z="1" />
  <Block id="minecraft:grass" x="1" z="1" />
  <Block id="minecraft:glass" z="2" />
  <Block id="minecraft:glass" x="1" z="2" />
</GameScene>
````

## Elementos filhos da cena

GuideNH atualmente registers Estes cena filho tags:

- `<Block>`
- `<ImportStructure>`
- `<ImportStructureLib>`
- `<IsometricCamera>`
- `<BlockStats>`
- `<PlaySound>`
- `<RemoveBlocks>`
- `<RemoveEntity>`
- `<ReplaceBlock>`
- `<PlaceBlock>`
- `<BlockAnnotationTemplate>`
- `<Entity>`
- anotação tags como `<BoxAnnotation>` e `<LineAnnotation>`

## Sons da cena

`<PlaySound>` pode ser placed dentro de `<GameScene>` para play sounds de cena interaction ou timeline
entrada. Compatível triggers são:

- `click`, O Padrão
- `hover`, fired once quando O cursor enters O cena
- `enter`, fired once quando O cena primeiro renders

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

quando `x`, `y`, e `z` são fornecido, O sound volume é attenuated em tela espaço de O
projected cena coordenada para O clicar ponto ou cena centro. `radius` defaults para 75% of O
shorter cena lado, e `minVolume` defaults para `0.15`.

## `<BlockStats>` e `<BlockStat>`

Declares ou customizes Uma Sobreposição de estatísticas de blocos. cenas com blocos enable O automático toggle
button even quando Este filho é omitted. Adding um ou mais `<BlockStat>` filhos switches O
overlay para manual statistics modo para que cena.

`<BlockStats>` atributos:

| Atributo | Obrigatório | Padrão | Significado |
| --- | --- | --- | --- |
| `visible` | não | config, Padrão `false` | inicial overlay visibilidade |
| `buttonEnabled` | não | config, Padrão `true` | mostra O bloco statistics toggle button |
| `mode` | não | `auto` | `auto` ou `manual`; filho `<BlockStat>` entradas force manual modo |
| `corner` | não | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, ou `bottomLeft` |
| `dock` | não | `inside` | automático lists pode attach para `inside`, `left`, `top`, `right`, ou `bottom`; manual modo sempre usa O dentro de overlay |
| `showNames` | não | `false` | whether para mostrar item names beside icons; quando ativado O count é também appended depois de O nome |
| `filterMode` | não | `blacklist` | `blacklist` ou `whitelist` |
| `filter` | não | vazio | item chaves como `minecraft:stone` ou `minecraft:stone:0`, separated by spaces, commas, ou semicolons |
| `maxWidth` | não | O larger of `224` px e 40% of O cena largura | maximum overlay largura em pixels antes de horizontal rolagem |
| `maxHeight` | não | O larger of `96` px e 40% of O cena altura | maximum overlay altura em pixels antes de vertical rolagem |

`<BlockStat>` atributos:

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `item` | sim, a menos que `id` é usado | item id exibido em O lista |
| `id` | sim, a menos que `item` é usado | existing item-stack Atributo form |
| `count` | não | displayed count; omitting it mostra O linha once, e `count="0"` oculta O linha |

Exemplo:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

Places Uma bloco em O visualização mundo.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `id` | sim, a menos que `ore` é usado | bloco id |
| `ore` | não | ore dictionary nome; O primeiro correspondente stack deve resolver para Uma bloco item |
| `x` | não | inteiro mundo X, Padrão `0` |
| `y` | não | inteiro mundo Y, Padrão `0` |
| `z` | não | inteiro mundo Z, Padrão `0` |
| `meta` | não | inteiro bloco metadata |
| `facing` | não | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | não | SNBT TileEntity compound |
| `formed` | não | whether O placed structure controller deve ser treated as formed durante visualização sync; Padrão `false` |

Observações:

- `ore` takes precedence sobre `id`; Se GregTech é installed, O escolhido stack é unified por `GTOreDictUnificator.setStack(...)`
- Se `meta` é omitted e an `ore` corresponder carries concrete non-wildcard item damage, que damage é usado antes de O `facing` reserva
- Se `meta` é omitted, some blocos derive Uma sensible Padrão de `facing`
- Se `nbt` cria Uma TileEntity successfully, O visualização usa it
- Defina `formed={false}` quando Uma controller-based structure deve stay unformed em visualização even though O surrounding structure é otherwise válido

Exemplo:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

carrega Um externo structure arquivo em O cena.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `src` | sim | structure recurso caminho |
| `x` | não | inteiro tradução X (alias para `offsetX`) |
| `y` | não | inteiro tradução Y (alias para `offsetY`) |
| `z` | não | inteiro tradução Z (alias para `offsetZ`) |
| `offsetX` | não | inteiro tradução X (preferido sobre `x`) |
| `offsetY` | não | inteiro tradução Y, clamped para `[0, worldHeight-1]` (preferido sobre `y`) |
| `offsetZ` | não | inteiro tradução Z (preferido sobre `z`) |
| `formed` | não | whether imported structure controllers deve ser treated as formed durante visualização sync; Padrão `false` |

Compatível formats:

- SNBT texto
- gzipped binary NBT
- uncompressed binary NBT

Obrigatório structure chaves:

- `palette`
- `blocks`

Exemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

Imports Uma StructureLib multiblock visualização by controller id.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `controller` | sim | controller bloco id, using `modid:block[:meta]` |
| `name` | não | opcional binding nome usado by `showWhenStructure` on Anotações, templates, e sounds |
| `piece` | não | StructureLib piece nome substituir |
| `facing` | não | facing substituir passed para O importer |
| `rotation` | não | rotação substituir passed para O importer |
| `flip` | não | flip/mirror substituir passed para O importer |
| `channel` | não | inteiro channel substituir para channel-aware structures |
| `offsetX` | não | inteiro X offset applied para todos placed blocos (Padrão `0`) |
| `offsetY` | não | inteiro Y offset applied para todos placed blocos, clamped para `[0, worldHeight-1]` (Padrão `0`) |
| `offsetZ` | não | inteiro Z offset applied para todos placed blocos (Padrão `0`) |
| `formed` | não | whether imported StructureLib controllers deve ser treated as formed durante visualização sync; Padrão `false` |

Observações:

- O imported structure starts de cena `0 0 0`; O controller é não forced para ser placed at `0 0 0`
- Este tag enables StructureLib-específico dica, hatch destacar, e channel slider UI quando metadata é disponível
- controller correspondente suporta O GTNH-style `modid:block:meta` form
- usar `name` quando O cena contém vários StructureLib imports e another tag needs para destino um específico structure estado
- `facing`, `rotation`, e `flip` usar O mesmo orientation vocabulary as StructureLib export; quando Uma requested combination é não allowed by O controller, GuideNH falls back para O primeiro válido alignment automaticamente
- GregTech controller previews now Padrão para O controller's opposite horizontal facing de O older visualização orientation, rotating O visualização front by 180 degrees around O Y axis
- Defina `formed={false}` quando O imported controller deve remain visibly unformed; Este é O Compatível alternative para shipping intentionally broken NBT

Exemplo:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

Structure-aware anotação e sound Exemplo:

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
  <ImportStructureLib name="aux" controller="gregtech:gt.blockmachines:15412" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    Visible only for matching `main` states.
  </BlockAnnotation>

  <PlaySound
    sound="guidenh:machine.start"
    trigger="click"
    showWhenStructure="aux"
    showWhenTier="1..2"
  />
</GameScene>
````

StructureLib defaults pode também ser supplied as filho tags. Estes defaults são part of O cena's
inicial interativo estado, so O reset-view button restores eles depois de O user alterações tier ou
channel sliders.

| filho tag | Significado |
| --- | --- |
| `<Tier value="1" />` | Master tier valor. |
| `<Channel name="channelName" value="1" />` | Named StructureLib channel substituir. Repeat para vários channels. |
| `<Facing value="north" />` | Padrão facing. |
| `<Rotation value="normal" />` | Padrão rotação. |
| `<Flip value="none" />` | Padrão flip/mirror. |
| `<Orientation value="north:normal:none" />` | Facing, rotação, e flip em um tag. |
| `<GregTechActiveController />` | GregTech somente: renderizar O controller com seu active texture quando possible. |
| `<GregTechPlaceHatches />` | GregTech somente: colocar normal GT hatches para hatch-somente visualização positions. sem Este, GT previews ainda usar survival construct para hatch-aware machines, but vazio hatch positions fall back para casing blocos. |

para GregTech controllers, GuideNH now usa O mesmo StructureLib survival-visualização caminho as O
export command. Este fixes hatch-somente positions que normal `construct()` não pode populate enquanto
keeping reserva casings by Padrão.

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:1000">
    <Tier value="4" />
    <Channel name="voltage" value="4" />
    <Facing value="north" />
    <Rotation value="normal" />
    <Flip value="none" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>
````

## `<IsometricCamera>`

aplica explícito isometric câmera yaw/pitch/roll.

Se Este tag é omitted, O cena keeps using O `<GameScene>` `perspective` preset. O Padrão
`isometric-north-east` preset é equivalent para:

````md
<IsometricCamera yaw="225" pitch="30" />
````

| Atributo | Significado |
| --- | --- |
| `yaw` | decimal |
| `pitch` | decimal |
| `roll` | decimal |

Exemplo:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes cada já-placed bloco correspondente Uma destino bloco id.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `id` | sim | bloco id para remover, using `modid:block[:meta]` |

Este é useful depois de importing Uma structure quando you want para ocultar específico blocos para clarity.

Exemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

Replaces já-placed blocos que corresponder Uma origem bloco id (e optionally Uma partial tile entity NBT
pattern) com Uma new bloco. O pesquisa pode ser global (todos filled blocos) ou restricted para a
bounding box.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `from` | sim | origem bloco para corresponder, using `modid:block[:meta]` |
| `from_nbt` | não | partial SNBT compound; Uma bloco corresponde somente quando seu tile entity NBT contém todos listed chaves |
| `to` | sim | replacement bloco, using `modid:block[:meta]` |
| `to_nbt` | não | SNBT TileEntity compound para apply para O replacement |
| `x` | não | bounding box emício X; Se qualquer of `x/y/z/dx/dy/dz` é present, O box modo é activated |
| `y` | não | bounding box emício Y |
| `z` | não | bounding box emício Z |
| `dx` | não | bounding box length on O X axis (Padrão `1`) |
| `dy` | não | bounding box altura on O Y axis (Padrão `1`) |
| `dz` | não | bounding box largura/depth on O Z axis (Padrão `1`) |
| `formed` | não | whether replacement resultado controllers deve ser treated as formed durante visualização sync; Padrão `false` |

Observações:

- quando none of `x/y/z/dx/dy/dz` são fornecido, todos filled blocos são scanned globally
- `from_nbt` é a **partial** corresponder: somente O chaves listed em O pattern deve corresponder; extra chaves em
  O atual tile entity são ignored
- O replacement é performed via O mesmo bloco placement pipeline as `<Block>`, so GregTech MetaTile
  e BartWorks tile entities são handled correctly
- Se O replacement places Uma controller, `formed={false}` keeps que controller unformed durante visualização

Exemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills Um alinhado aos eixos box com a único bloco Tipo, overwriting whatever was there antes de.
Unlike `<Block>` (que targets a único posição), `<PlaceBlock>` suporta multi-bloco regions via
`dx`/`dy`/`dz`, ordered as length, altura, e largura/depth on O X/Y/Z axes.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `id` | sim | bloco id, using `modid:block[:meta]` |
| `nbt` | não | SNBT TileEntity compound applied para cada placed bloco |
| `x` | não | region emício X, Padrão `0` |
| `y` | não | region emício Y, Padrão `0` |
| `z` | não | region emício Z, Padrão `0` |
| `dx` | não | region length on O X axis, Padrão `1` |
| `dy` | não | region altura on O Y axis, Padrão `1` |
| `dz` | não | region largura/depth on O Z axis, Padrão `1` |
| `formed` | não | whether placed controllers deve ser treated as formed durante visualização sync; Padrão `false` |

Observações:

- todos blocos em O box são unconditionally placed (não prior-bloco check)
- O NBT compound é copied Para cada individual placement
- O mesmo bloco placement pipeline as `<Block>` é usado, so GregTech MetaTile e BartWorks tile entities
  são fully Compatível
- Se O region places um ou mais controllers, `formed={false}` keeps cada affected controller unformed

Exemplo:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

Expands um ou mais filho Anotações onto cada correspondente bloco que já exists em O atual cena.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `id` | sim | bloco matcher em `modid:block[:meta]` form |

Regras:

- colocar it depois de O blocos ou imported structures que it deve corresponder
- correspondente happens against O atual cena estado at analisar time
- filho Anotações usar local coordenadas relativo para cada correspondente bloco

Exemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

Adds Um entity para O visualização cena.

O atributos follow summon-style entity placement e SNBT dados.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `id` | sim | entity Tipo id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, e registrado mod entity ids em qualquer um `modid.entityName` ou `modid:entityName` form são accepted |
| `x` | não | decimal X coordenada O entity é centered on, Padrão `0.5` |
| `y` | não | decimal Y coordenada at O baixo of O entity, Padrão `0` |
| `z` | não | decimal Z coordenada O entity é centered on, Padrão `0.5` |
| `rotationY` | não | yaw em degrees, Padrão `-45` |
| `rotationX` | não | pitch em degrees, Padrão `0` |
| `data` | não | summon-style SNBT merged em O entity NBT antes de spawn |
| `sceneEntityId` | não | stable cena-local entity id usado by later `<Entity>` / `<RemoveEntity>` operations e by imported cena snapshots |
| `mount` | não | stable `sceneEntityId` of O vehicle que Este entity deve ride depois de spawn |
| `unmount` | não | booleano expressão que clears Este entity's atual stable mount relation depois de spawn ou estado replay |
| `baby` | não | booleano expressão forcing Compatível entities em baby form; omitted leaves O entity's normal age/estado unchanged |
| `name` | não | visualização player nome quando `id` é `player`, `fakeplayer`, `minecraft:player`, ou `minecraft:fakeplayer` |
| `uuid` | não | visualização player UUID quando using um of O player ids acima de |
| `showName` | não | booleano expressão controlling O visualização player nameplate, Padrão `true` para player visualização ids |
| `showCape` | não | booleano expressão controlling O visualização player cape, Padrão `true` para player visualização ids |
| `headRotation` | não | visualização player head rotação as `x y z` degrees |
| `leftArmRotation` | não | visualização player esquerda arm rotação as `x y z` degrees |
| `rightArmRotation` | não | visualização player direita arm rotação as `x y z` degrees |
| `leftLegRotation` | não | visualização player esquerda leg rotação as `x y z` degrees |
| `rightLegRotation` | não | visualização player direita leg rotação as `x y z` degrees |
| `capeRotation` | não | visualização player cape rotação as `x y z` degrees; defaults para O standing-ainda angle `6 0 0` |

Observações:

- entity bounds participate em cena auto-centering e visível-layer filtering
- entity creation falls back gracefully quando O visualização mundo é não ready yet, então binds on primeiro renderizar
- `sceneEntityId` é opcional, but strongly recommended whenever later cena mutations need para find, remover, remount, ou restore O mesmo logical entity sem scanning by bruto execução id
- um `sceneEntityId` pode próprio mais que um execução entity instance; `<RemoveEntity sceneEntityId="..."/>` removes cada entity atualmente registrado para que stable id
- `mount` links entities by stable cena id rather que by bruto NBT passenger lists, so replay, import/export, visualização rebuild, e Animação Ponder seeking pode todos restore O mesmo rider/vehicle relation deterministically
- `unmount={true}` clears O stable mount relation para que entity antes de qualquer later mount é applied
- `baby={true}` atualmente suporta visualização players, ageable mobs, vanilla zombies, e modded entities que expose stable `setChild(boolean)` ou `setBaby(boolean)` style APIs
- filho-estado entities são re-aligned para seus atual posição depois de resizing so passar o mouse e pick bounds stay centered on O renderizado model
- player visualização ids criar Uma client-lado fake remote player so O normal player renderer e skin pipeline pode ser usado
- quando ambos `name` e `uuid` são omitted para Uma player visualização, GuideNH falls back para `Steve` e O vanilla Padrão skin
- quando somente `name` é given para Uma player visualização, GuideNH primeiro tries para resolver O real online profile so skins e capes pode carregar; Se lookup fails, it falls back para Uma stable offline UUID
- quando somente `uuid` é given para Uma player visualização, GuideNH generates Uma placeholder exibir nome e ainda tries para resolver O skin de O profile
- `showName={false}` oculta O visualização player's overhead nome sem bypassing O normal player renderer
- `showCape={false}` oculta O visualização player's cape enquanto ainda respecting O normal player renderizar caminho e Forge hooks
- player pose atributos usar três espaço-separated floats mapped para model `X Y Z` rotação em degrees
- omitted head e limb rotação atributos Mantenha O normal vanilla idle pose; omitted `capeRotation` falls back para O standing-ainda cape angle `6 0 0`
- player previews require Um active client mundo at analisar time because Minecraft's player entity constructor não pode ser criado worldless
- ao passar o mouse Um entity mostra seu localizado exibir nome, ou seu personalizado nome Se um was fornecido

Exemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Stable-id mount Exemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

Removal e unmount Exemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
</GameScene>
````

Baby entity Exemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

visualização player pose Exemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    name="ArtherSnow"
    headRotation="0 20 0"
    rightArmRotation="-35 0 0"
    leftArmRotation="10 0 -12"
    rightLegRotation="8 0 0"
    leftLegRotation="-8 0 0"
    capeRotation="12 0 0"
  />
</GameScene>
````

visualização player nome e cape Exemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

Removes cada execução entity atualmente registrado para um stable `sceneEntityId`.

| Atributo | Obrigatório | Significado |
| --- | --- | --- |
| `sceneEntityId` | sim | stable cena-local entity id para remover |
| `unmount` | não | booleano expressão que clears O stable mount relation antes de removal |

Observações:

- Este é O cena-lado counterpart para Animação Ponder's `removeEntities`
- removal works on O indexed stable-id registry, so it não faz need para scan todos entities cada frame
- Se vários imported ou replayed entities share O mesmo `sceneEntityId`, Elas são removido together

Exemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## Clima

`<Weather>` adds animated rain ou snow directly para a `GameScene`. Unlike Animação Ponder Clima presets,
cena Clima é não timeline-owned: it keeps looping durante normal cena renderização, it não faz
fade em ou fade out, e it não pode ser paused ou scrubbed independently. O renderer ainda usa O
mesmo precipitation geometria caminho as Animação Ponder Clima, so local visualização e site export stay aligned.

| Atributo | Padrão | descrição |
| --- | --- | --- |
| `weather` / `type` | `rain` | Clima kind. Compatível valores: `rain`, `snow`. |
| `x`, `z` | cena bounds | Covered precipitation columns. Uma scalar targets um coluna. Arrays usar endpoint pairs para define um ou mais rectangles. |
| `density` | Tipo-específico | Coverage density. Higher valores Mantenha mais precipitation columns active; lower valores sparsify O effect. |

Observações:

- `<Weather>` ignores `y`; O vertical span é derived de O atual cena bounds e de O
  highest precipitation-blocking bloco em cada covered coluna.
- Se um axis tem unmatched extra array valores, O unmatched tail é ignored.
- dentro de um Clima declaration, rain e snow nunca stack on O mesmo `x/z` coluna. Se vários
  Clima tags overlap, earlier tags Mantenha O shared columns.
- diferente non-overlapping columns em O mesmo `GameScene` pode renderizar rain e snow at O mesmo
  time.

Exemplo:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## câmera centro Behavior

Se não explícito `centerX/Y/Z` é given, GuideNH auto-centers O cena de O placed bloco bounds. Se qualquer explícito centro coordenada é Defina, auto-centering é desativado e ausente coordenadas Padrão para `0`.

## Notas de interação

quando `interactive={true}` O cena suporta rotação, pan, zoom, reset, anotação toggles, e outro UI controls exposed by O guia tela.

- cenas spanning vários Y levels mostrar Uma visível-layer slider acima de O baixo edge
- StructureLib cenas pode Adicione Uma hatch-destacar toggle button plus Uma channel slider at O very baixo quando O imported metadata fornece eles
- anotação passar o mouse takes priority sobre bloco passar o mouse; bloco tooltips appear normally once não anotação hotspot é being hovered
- StructureLib passar o mouse keeps O bloco nome on O primeiro dica linha, adds structure-específico texto starting on O second linha, e expands replacement candidates quando `Shift` é held

## Páginas relacionadas

- [Annotations](Annotations)
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [Examples](Examples)
