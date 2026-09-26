# Referência de tags

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


Este página lists O built-em execução tags registrado by `DefaultExtensions`.

## Regras de uso

- tags pode appear qualquer um em bloco context ou inline context depending on O compilador.
- MDX comments using `{/* ... */}` são Compatível em página conteúdo e são ignored by O execução analisador.
- inválido tags ou inválido atributos renderizar guia errors inline em vez disso of silently failing.
- Large feature tags como Receitas e 3D cenas são documented em seus próprio pages:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## tags inline e de fluxo

| tag | Purpose | chave atributos |
| --- | --- | --- |
| `<a>` | interno/externo link e opcional anchor nome | `href`, `title`, `name` |
| `<br>` | linha break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style texto | none |
| `<sup>` | smaller inline superscript-style texto | none |
| `<Color>` | colored inline texto | `id` ou `color` |
| `<Spoiler>` | oculto inline texto revealed on passar o mouse | none |
| `<Tooltip>` | rico passar o mouse dica com Markdown/tag filhos | `label` |
| `<SoundLink>` | clickable rico-texto sound trigger | `sound` ou `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted texto; equivalent para `==text==` com opcional cor control | `color` |
| `<PlayerName>` | inserts atual player username | none |
| `<KeyBind>` | inserts keybinding exibir nome | `id` ou `action` |
| `<ItemImage>` | inline item icon | `id` ou `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | item dica + opcional Navegação link | `id` ou `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command link | `command`, `title`, `close` |
| `<Latex>` | LaTeX math fórmula; inline em flow context, centered exibir bloco em bloco context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest link com estado-aware styling (compat tag, somente registrado quando BetterQuesting é carregado) | `id`, `text`, `show_tooltip` |

Inline Markdown também suporta action links para sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## tags de bloco

| tag | Purpose | chave atributos |
| --- | --- | --- |
| `<div>` | pass-por bloco wrapper | none |
| `<ContentTabs>` | groups alternative rico conteúdo under independent tabs | `title`, `color`, `icon`, `iconPng`, `icon_png`, `iconItem`, `icon_item`, `default`, `defaultIndex` |
| `<Tab>` | um conteúdo panel dentro de `<ContentTabs>` | `title` |
| `<details>` | collapsible execução bloco | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | diretório-style outline com connector linhas | `indent`, `gap` |
| `<Row>` | horizontal flex layout | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertical flex layout | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | largura-constrained footnote container usado by execução Markdown footnotes | `width` |
| `<ItemGrid>` | compact grid of item icons | filhos deve ser `<ItemIcon id="..."/>` ou `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-interativo 3D único-bloco visualização | `id` ou `ore`, opcional `scale` (defaults para `4`), `float`, `perspective`, `nbt` |
| `<FloatingImage>` | cropped imagem bloco com decimal ou true inline placement | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | Navegação filho listing | `id`, `alphabetical` |
| `<Category>` | lista pages de Uma category | `name`, `rows` |
| `<Special>` | lista built-em MediaWiki special pages | `name`, `rows` |
| `<Structure>` | 2.5D isometric bloco layout view | `width`, `height` |
| `<Mermaid>` | execução Mermaid gráfico import/inline | `src`, `width`, `height` |
| `<CsvTable>` | execução CSV arquivo import tabela | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered coluna gráfico | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar gráfico | mesmo as `<ColumnChart>` |
| `<LineChart>` | linha gráfico com categorical ou numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie gráfico | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter gráfico | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-curva função gráfico | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | único-curva shorthand para `<FunctionGraph>` | `expr`, plus todos `<FunctionGraph>` panel atributos |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | Consulte [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D guia cena | Consulte [GameScene](GameScene) |
| `<QuestCard>` | bloco-level BetterQuesting quest summary card (compat tag, somente registrado quando BetterQuesting é carregado) | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` é a `<GameScene>` filho tag. It accepts `controller`, `piece`, `facing`,
`rotation`, `flip`, e `channel` atributos, e também suporta StructureLib Padrão filho tags:
`<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`,
`<GregTechActiveController>`, e `<GregTechPlaceHatches>`.

## Detalhes das tags

### `<a>`

Acts like Um HTML-style anchor tag:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` pode ser relativo, rooted, explícito `modid:path`, ou HTTP/HTTPS
- `title` torna-se O dica
- `name` inserts Uma página anchor destino

### `<br>`

GuideNH também suporta Um MDX break tag com decimal clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` valores:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, e `<sup>`

GuideNH execução suporta Uma focused subset of lowercase documentation tags para inline usar:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

cria Uma collapsible execução bloco com Uma summary linha. O `<summary>` linha suporta normal
inline Markdown/tag conteúdo, e O corpo pode hold ordinary texto plus arbitrary tags de bloco como
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, tabelas, Gráficos, e layout containers.
quando `height` é Defina, somente O corpo scrolls; O summary linha e outer frame stay fixo.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

Texto oculto por padrão com um [link normal de página](./index.md).

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

atributos:

- `open` — starts expanded quando present
- `width` — preferido outer largura em pixels
- `height` — preferido corpo viewport altura em pixels; overflow torna-se scrollable em-game e em site export
- `wrap` — suporta O usual bloco embedding modes como `square`, `tight`, e `through`
- `align` — `left`, `center`, ou `right`; quando combined com Uma floating wrap modo, O whole details bloco floats

### `<ContentTabs>`

Groups alternative rico conteúdo under independent tabs. somente direto `<Tab>` filhos são válido. O container itself pode também renderizar Uma quote-style heading linha acima de O tabs, correspondente O visual idioma of Markdown callouts.

````mdx
<ContentTabs
  title="Implementation Options"
  icon="</>"
  color="#4f8cff"
  default="Java"
>
  <Tab title="Java">
    ```java
    System.out.println("Hello GuideNH");
    ```
  </Tab>
  <Tab title="Scene">
    <BlockImage id="minecraft:crafting_table" />
  </Tab>
</ContentTabs>
````

- `default` corresponde O primeiro tab whose `title` corresponde exatamente
- `defaultIndex` é zero-based e wins sobre `default` quando ambos são present
- `color` optionally substitui O esquerda accent linha e selecionado-tab destacar com `#RRGGBB` ou `#AARRGGBB`
- `title` adds Um opcional simples-texto heading acima de O tab strip
- `icon`, `iconPng` / `icon_png`, e `iconItem` / `icon_item` usar O mesmo heading icon semantics as Markdown quote-style callouts
- inválido filhos ou inválido defaults renderizar visível author-facing errors

### `<FileTree>`

Renders Uma diretório-style outline com real connector linhas drawn de O prefix glyphs on cada linha. ambos Unicode box-drawing (`│ ├ └ ─`) e ASCII (`| +-- \-- ` / four spaces) forms são accepted e pode ser mixed. Payload texto suporta O usual inline Markdown (links, **bold**, `code`, …), e those links são clickable ambos em-game e em O built-em site export. O mesmo conteúdo pode também ser written as Uma fenced ` ```tree ` or ` ```filetree ` bloco.

````md
<FileTree indent="14" gap="0">
project
├── src
│   ├── **main**
│   │   └── [App.java](./index.md)
│   └── *test*
└── `README.md`
</FileTree>
````

opcional per-linha icons são introduced by Uma leading directive on O payload:

- `{:icon=Text}` — short texto rótulo (único ou double quotes opcional)
- `{:iconPng=path/to/file.png}` — PNG recurso resolvido against O atual página
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft item icon. O opcional `meta` segment é Uma damage valor (ou `*` para Uma wildcard); Um opcional trailing `:{snbt}` bloco carries SNBT para attach para O stack.

````md
```filetree
mundo
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} sample recurso
```
````

atributos:

- `indent` — pixels per depth level (Padrão `14`)
- `gap` — extra pixels entre linhas (Padrão `0`)

### Citações em tempo de execução

normal Markdown blockquotes renderizar at execução com Uma esquerda accent linha. GitHub alert sintaxe é Compatível:

````md
> [!NOTE]
> Alert body
````

GuideNH também suporta Uma execução-somente personalizado directive on O primeiro quoted linha:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

Compatível directive chaves:

- `title`
- `color`
- `icon` para simples texto symbols
- `iconItem` para an `ItemStack` id
- `iconPng` para Uma guia recurso png caminho

somente um icon origem deve ser fornecido.

### `<Color>`

usar qualquer um Uma symbolic cor id ou Um explícito hex valor:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

Use `<Spoiler>` for inline hidden text. The content stays rich-text aware, so nested markdown and runtime inline tags
such as `<Color>` still render correctly after reveal.

```mdx
<Spoiler>oculto **bold** texto com <Color color="#55ccff">tint</Color>.</Spoiler>
<Spoiler>[Anchor Link](#headings) ainda behaves like Uma normal hoverable link quando revealed.</Spoiler>
```

````

Regras:

- `id` e `color` são mutually exclusive em practice; fornecer um
- `color` accepts `#RRGGBB`, `#AARRGGBB`, ou `transparent`

### `<Tooltip>`

cria underlined texto que opens Uma rico conteúdo dica on passar o mouse.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

Se `label` é omitted, O trigger texto defaults para `tooltip`.

### `<SoundLink>` e Sound Action links

`<SoundLink>` renders rico inline conteúdo que plays Uma sound quando clicked. It não faz navigate,
e seu personalizado clicar sound replaces O normal guia clicar sound para que clicar.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound atributos:

- `sound` é Uma sound event id como `modid:event.name`
- `src` pontos at an `.ogg` arquivo; `modid:sounds/machine/start.ogg` torna-se `modid:machine.start`
- `volume` defaults para `1.0`
- `pitch` defaults para `1.0`
- `cooldown` é milliseconds entre repeated plays, Padrão `250`
- `radius` e `minVolume` control espaço da tela attenuation quando usado em cenas

### `<PlayerName>`

Inserts O atual Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up Uma keybinding by id ou action e renders O player's atual bound chave nome.

Accepted ids:

- O binding descrição id, como `key.jump` ou `key.guidenh.open_guide`
- O legacy `category.description` form, como `key.categories.movement.key.jump`

Exemplo:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX Comments

GuideNH ignores MDX comments em página conteúdo:

````md
Texto visível. {/* comentário embutido oculto */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH também ignores explícito `<Comment>` tags:

````md
Texto visível. <Comment>Isso não é renderizado.</Comment> Continua visível.
````

### `<ItemImage>`

mostra Um inline item icon.

| Atributo | Significado |
| --- | --- |
| `ore` | ore dictionary nome; O primeiro corresponder wins |
| `id` | item reference usado quando `ore` é absent |
| `nbt` | opcional SNBT item dados; merged onto qualquer inline SNBT em `id` |
| `scale` | decimal, Padrão `1` |
| `noTooltip` | truthy texto ou vazio Atributo suppresses dica (legacy; prefer `showTooltip`) |
| `showTooltip` | booleano, Padrão `true`; `false` suppresses O passar o mouse dica |
| `showIcon` | booleano, Padrão `true`; `false` oculta O item icon graphic |
| `label` | `left` ou `right` — mostra O item exibir nome as texto on O specified lado of O icon; omit para não rótulo |
| `format` | format pattern para O rótulo texto; suporta Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) com opcional `%s` placeholder para O item nome; Padrão (não Atributo) renders O nome em italic |
| `yOffset` | inteiro pixel offset substituir para O **icon** at scale `1`; não faz affect O rótulo texto |
| `labelYOffset` | inteiro pixel offset substituir para O **rótulo texto** at scale `1`; não faz affect O icon |

Observações:

- `ore` takes precedence sobre `id` quando ambos são fornecido
- Se GregTech é installed, O selecionado ore corresponder é passed por `GTOreDictUnificator.setStack(...)`
- `label` requires at least um of `showIcon` ou `label` para produce visível saída; setting ambos `showIcon="false"` e omitting `label` renders nothing
- `format` somente aplica quando `label` é Defina; Se `format` tem não `%s`, O literal format texto é usado as O rótulo
- inline SNBT em `id` remains Compatível; quando ambos forms são present, O standalone `nbt` Atributo é merged último e substitui conflicting chaves

Exemplo:

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
<ItemImage id="minecraft:diamond" label="right" />
<ItemImage id="minecraft:iron_ingot" label="left" format="**%s**" />
<ItemImage id="minecraft:book" showIcon="false" label="right" format="~~%s~~" />
<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<ItemLink>`

cria Uma texto link using O item's exibir nome e item dica. Se `item_ids` pontos para Uma página do guia, clique navigates para it. `ore` pode ser usado para resolver O exibir stack de O primeiro ore dictionary corresponder em vez disso of Uma fixo registry id.

| Atributo | Padrão | Significado |
| --- | --- | --- |
| `id` | — | item registry id, e.g. `minecraft:compass` ou `minecraft:wool:1` |
| `ore` | — | ore-dictionary nome; usa O primeiro correspondente item stack |
| `linksTo` | *(auto)* | substitui O link destino; accepts Uma ID da página com opcional `#anchor`, e.g. `./crafting.md#usage` ou `#usage`; quando omitted O destino é resolvido de `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | Defina para `false` para suppress O passar o mouse dica; `noTooltip` é Uma legacy alias |
| `showIcon` | *(none)* | `left` ou `right` (ou qualquer truthy valor → direita) — renders O item icon beside O link texto; omit para mostrar texto somente |
| `scale` | `1.0` | exibir scale para O opcional item icon; tem não effect quando `showIcon` é omitted |

Exemplos:

````md
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" />
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" showIcon="left" />
<ItemLink id="minecraft:diamond" showIcon="left" scale="2" />
<ItemLink id="minecraft:diamond" showIcon="right" showTooltip="false" />
<ItemLink ore="stickWood" />
<ItemLink id="minecraft:iron_ore" linksTo="./crafting.md#smelting" />
<ItemLink id="minecraft:compass" linksTo="#usage" />
````

### `<CommandLink>`

Sends Uma chat command quando clicked.

| Atributo | Significado |
| --- | --- |
| `command` | Obrigatório, deve emício com `/` |
| `title` | opcional dica heading |
| `close` | analisado booleano Atributo; atualmente analisado but não usado para close O guia |

Exemplo:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` e `<Column>`

Flex-style containers para bloco conteúdo.

| Atributo | Significado |
| --- | --- |
| `gap` | inteiro gap entre filhos, Padrão `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | booleano expressão, Padrão `false` |
| `width` | inteiro preferido largura; useful para constraining lista linha largura |

Exemplo:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

para constrain O largura of normal Markdown lists, wrap eles em Uma container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH usa Este bloco tag internally quando execução Markdown footnotes são expanded. It pode também ser written manualmente Se needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

Renders Uma compact item grid. filhos deve ser bruto `<ItemIcon>` elements, que são analisado directly by O grid compilador. cada filho pode usar qualquer um `id` ou `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

Renders Uma non-interativo 3D único-bloco cena. O visualização tem não cena fundo, não cena
buttons, não layer controls, e não anotação features, but ao passar o mouse O bloco ainda mostra O
seleção outline e dica. `ore` deve resolver para Uma bloco item stack.

| Atributo | Significado |
| --- | --- |
| `id` | bloco id; suporta O normal `modid:block[:meta][:{snbt}]` forma |
| `ore` | ore dictionary lookup; O primeiro correspondente bloco item wins |
| `scale` | câmera zoom multiplier, Padrão `4` |
| `float` | legacy flow decimal suporte: `left` ou `right` |
| `perspective` | `isometric-north-east` (Padrão), `isometric-north-west`, ou `up` |
| `nbt` | opcional SNBT tile-entity dados merged onto qualquer inline SNBT de `id` |

Observações:

- inline SNBT dentro de `id` é ainda accepted para compatibility, but `nbt="..."` é O preferido
  authoring form
- quando ambos inline SNBT e `nbt` são present, O `nbt` Atributo é merged último e portanto
  substitui conflicting chaves
- GuideNH 1.7.10 não faz suporte modern bloco-estado propriedade sintaxe here, so GuideME-style
  `p:<state>` atributos são intentionally não Compatível

````md
<BlockImage id="minecraft:crafting_table" scale="3" />
<BlockImage ore="logWood" scale="3" perspective="isometric-north-west" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<FloatingImage>`

Consulte [Images And Assets](Images-And-Assets) para O completo behavior.

rápido Regras:

- `src` suporta relativo paths, rooted paths, e explícito `modid:path` texture ids
- `x`, `y`, `width` / `w`, e `height` / `h` define O crop rectangle on O original imagem e deve todos ser present
- `scaleX` e `scaleY` resize O cropped resultado e suporte independent horizontal / vertical stretching
- `displayWidth` e `displayHeight` Defina final dimensions em pixels; um valor preserves O crop aspect ratio, enquanto dois valores permitir stretching
- `displayWidth` / `displayHeight` não pode ser combined com `scaleX` / `scaleY`
- `wrap="inline"` places O imagem truly inline dentro de texto flow; em que modo `align` é ignored
- old conteúdo que usado `width` / `height` as final exibir tamanho deve ser migrated manualmente

### `<SubPages>`, `<Category>`, e `<Special>`

Consulte [Navigation](Navigation) para completo Navegação behavior.

### `<Structure>`

Consulte [Examples](Examples) e [GameScene](GameScene) quando deciding whether para usar Uma static structure visualização ou Uma completo 3D cena.

### `<Mermaid>`

usado para execução Mermaid conteúdo. atual execução suporte é focused on `mindmap`, qualquer um inline ou por Uma página-relativo `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
Os nós de execução podem incorporar blocos normais.

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` e `height` constrain O execução viewport box
- dentro de O viewport, arrastar pans e O mouse wheel zooms
- quoted Mermaid labels pode usar rico inline Markdown como `**bold**` e página links
- `<NodeContent id="...">...</NodeContent>` pode ser adicionado as filhos of `<Mermaid>` para substituir Uma node corpo com arbitrary execução blocos

### `<CsvTable>`

usado para analisar Uma CSV arquivo em Uma execução tabela:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves relativo para O atual página, O mesmo way cena imports e normal recurso links do.

opcional atributos:

- `header`
  Defaults para `true`; Defina `header={false}` para Mantenha O primeiro linha unbolded
- `widths`
  Comma-separated inteiro largura hints como `widths="120,80"`

Exemplos:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

O related fenced execução CSV form também suporta correspondente metadata:

````md
```csv widths="120,80" header=false
nome,valor
iron,42
gold,17
```
````

### `<Latex>`

Renders Uma LaTeX math fórmula using jlatexmath. quando usado inline (dentro de Uma paragraph ou texto flow), it renders as Uma scaled glyph que expands O linha altura para fit O fórmula. quando written as seu próprio paragraph (bloco context), it renders centered as Uma exibir-modo fórmula.

| Atributo | Tipo | Padrão | descrição |
| --- | --- | --- | --- |
| `formula` | texto | *(Obrigatório)* | LaTeX origem texto |
| `color` | `#RRGGBB` ou `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | decimal | `1.0` | exibir tamanho multiplier applied on topo of O automático linha-altura scaling |
| `sourceScale` | decimal | `100.0` | jlatexmath interno renderizar resolution; higher valores improve quality at large sizes |
| `tooltip` | texto | *(none)* | simples dica texto exibido on passar o mouse |
| `showTooltip` | booleano | `false` | mostrar O bruto LaTeX origem as Uma dica on passar o mouse |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-somente. Vertical alignment dentro de O texto linha: `baseline` (Padrão) aligns O fórmula's math baseline com O texto baseline; `top` aligns O fórmula topo com O linha topo; `center` centers it on O texto; `bottom` aligns O fórmula baixo com O texto baixo |
| `offsetX` | int | `0` | Horizontal pixel offset applied depois de alignment (positive = direita) |
| `offsetY` | int | `0` | Vertical pixel offset applied depois de alignment (positive = down) |

Exemplos:

````md
Inline: <Latex formula="E=mc^2" />

Fraction that expands line height: <Latex formula="\frac{a+b}{c-d}" />

Gold colour: <Latex formula="\sqrt{x^2+y^2}" color="#FFD700" />

Scaled up: <Latex formula="\pi" scale="1.5" />

With hover tooltip: <Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2}" showTooltip={true} />

Plain custom tooltip: <Latex formula="E=mc^2" tooltip="Energy equals mass times the speed of light squared." />

Rich tooltip:
<Latex formula="\Delta G = \Delta H - T\Delta S">
  **Gibbs free energy**

  - <Latex formula="\Delta H" />: enthalpy change
  - <Latex formula="T\Delta S" />: entropy term
</Latex>

Bottom-aligned (formula bottom matches text bottom): <Latex formula="\frac{a}{b}" valign="bottom" />

Explicit baseline alignment (same as default): <Latex formula="E=mc^2" valign="baseline" />

Top-aligned with an upward nudge: <Latex formula="x^2" valign="top" offsetY="-1" />

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" />

<Latex formula="\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}" />
````

#### `$$formula$$` shorthand

As Uma convenience you pode escrever `$$formula$$` directly em Markdown sem using O `<Latex>` tag.
todos renderização parâmetros usar seus defaults (white colour, scale 1.0, não dica, baseline-aligned).

- **Inline**: `$$formula$$` embedded dentro de Uma paragraph renders as Um inline fórmula.
- **exibir**: Uma paragraph whose entire conteúdo é `$$formula$$` (com opcional surrounding whitespace) renders as Uma centred exibir-modo bloco.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

Observações:

- O fórmula altura é calibrated para O atual linha texto altura. Simple formulas renderizar at texto altura; taller formulas (fractions, summations, integrals, etc.) expand O enclosing linha altura automaticamente.
- `valign` somente aplica para inline formulas. exibir-modo (bloco-level) formulas são sempre centered horizontally; usar `offsetY` para shift eles vertically dentro de O bloco.
- `color` defaults para white (`#FFFFFF`). usar `#AARRGGBB` format para Uma semi-transparent fill.
- `sourceScale` somente affects renderizar sharpness, não O displayed tamanho. valores abaixo `16` são clamped para `16`.
- dica priority é: rico filho Markdown conteúdo, então `tooltip="..."`, então `showTooltip={true}` bruto origem reserva.
- filho dica conteúdo é compilado as regular guia Markdown, so it pode incluir bold texto, lists, links, item tags, e nested `<Latex>` formulas.
- O `$$formula$$` shorthand sempre usa Padrão parâmetros. usar O `<Latex>` tag para personalizado colour, scale, alignment ou dica.

### cena execução tags

Estes tags somente funcionam dentro de `<GameScene>` / `<Scene>`:

| tag | Purpose | chave atributos |
| --- | --- | --- |
| `<ImportStructure>` | import Um externo SNBT/NBT structure recurso | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import Uma StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | remover já-placed blocos que corresponder Uma bloco matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp O mesmo filho Anotações onto cada correspondente placed bloco | `id` |

Consulte [GameScene](GameScene) para cena import/removal behavior e [Annotations](Annotations) para anotação template Regras.


## Gráficos

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, e `<ScatterChart>` são interativo gráfico blocos. todos Gráficos share O following comum atributos:

| Atributo | descrição | Padrão |
| --- | --- | --- |
| `title` | gráfico título | none |
| `width` / `height` | explícito tamanho | 320 / 200 |
| `background` / `border` | fundo e borda colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | título e valor-rótulo colors | light grey |
| `legend` | legenda posição: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | valor-rótulo posição: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | interno plot legenda posição: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum interno legenda box tamanho | `120` / `64` |
| `cornerLegendBackground` | interno legenda fundo cor | `#AA111922` |

Cartesian Gráficos (coluna / Bar / linha / Scatter) additionally aceitam axis atributos `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` e O correspondente `yAxis*` Defina, plus `showXGrid={true}` / `showYGrid={true}` para toggle gridlines.

filhos:

* `<Series name="..." color="#..." data="10,20,30"/>` para category-based Gráficos (coluna / Bar / categorical linha).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` para numeric X (linha `numericX={true}`, Scatter).
* `<Slice name="..." value="..." color="#..."/>` para `<PieChart>` somente.

quando `color` é omitted on a `<Series>` ou `<Slice>`, GuideNH cycles por Uma built-em 16-cor palette.

`<Series>` e `<Slice>` também aceitam O following opcional icon / dica atributos:

* `icon="modid:item"` (mesmo sintaxe as `<ItemImage>`'s `id`, pode incluir `@meta` e inline NBT JSON) — binds an `ItemStack` para O entrada; O legenda swatch torna-se O item icon e ao passar o mouse O dados ponto mostra O vanilla item dica com O gráfico descrição appended at O fim.
* `iconImage="images/foo.png"` — usar Uma PNG recurso as O legenda swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form texto appended para O dica (usar `\n` para multi-linha).

Exemplo:

```mdx
<PieChart title="Output share">
  <Slice name="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice name="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra atributos: `categories` (X-axis ou Y-axis labels, comma separated), `barWidthRatio` (Padrão 0.7). `<BarChart>` puts O categories on O Y-axis e valores on O X-axis.

#### Combo extensions

`<ColumnChart>` e `<BarChart>` aceitam dois extra filho element types so vários gráfico styles pode share um plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn as Uma polilinha overlay on topo of O bars. cada linha ponto sits at O cluster centro of O correspondente category index; O overlay shares O host gráfico's valor axis. You pode declare vários `<LineSeries>` para overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — Uma small pie gráfico drawn dentro de um of O four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) of O plot area. seu `<Slice>` filhos share O mesmo sintaxe as em `<PieChart>`.

```mdx
<ColumnChart title="Quarterly output" categories="Q1,Q2,Q3,Q4">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="topRight" title="Total share">
    <Slice name="Iron" value="225" color="#a0a0a0"/>
    <Slice name="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

### `<LineChart>`

Extra atributos: `numericX={true}` para enable Uma numeric X-axis (filhos deve usar `points`); `showPoints={false}` oculta ponto markers. O hovered ponto é pushed outward by 2px along O curva normal, enlarged, e outlined; O adjacent linha segments thicken by 1px.

`<LineChart>` e `<ScatterChart>` pode mostrar Uma compact legenda dentro de O plot area com `cornerLegend="topRight"` ou another corner. entradas usar existing série names e colors.

### `<PieChart>`

Extra atributos: `startAngle` (Padrão `-90`, i.e. 12 o'clock); `clockwise={false}` para reverse direction. O hovered fatia pops outward 4px along seu bisector.

### `<ScatterChart>`

Renders pontos somente; `<Series>` deve usar `points`. O X-axis é sempre numeric.

## Gráficos de funções

`<FunctionGraph>` e O único-curva shorthand `<Function>` renderizar Um interativo Desmos-style panel. O mesmo panel é também disponível por a ` ```funcgraph ` fenced bloco de código; Consulte O execução [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) para Uma completo walkthrough.

Panel atributos (accepted by O container, O shorthand, e O fence header alike):

- `width` / `height` (defaults `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (Padrão `true`)
- `xRange="a..b"` (ou `xMin` / `xMax` separately), `xStep` para tick spacing; mesmo para O Y axis
- `xLabel` / `yLabel` Adicione Excel-style axis titles abaixo e acima de O plot respectively. Elas suporte inline `$$...$$` LaTeX; `domain="a..b"` é Uma legacy alias para `xRange` quando não explícito X range é present
- `quadrants="1,2,3,4"` ou `quadrants="all"` para force O visível quadrants; omit para emício em quadrant 1 com auto-expansion quando sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, e `cornerLegendBackground` mostrar Uma compact legenda dentro de O plot area using non-vazio curva labels

curva filhos (`<Plot>` / `<Function>`):

- `expr="..."` &mdash; O expressão. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` absoluto valor, `√` / `sqrt` / `∛` / `cbrt`, implícito multiplication, e O constants `pi`, `tau`, `e`, `phi` são Compatível. Built-em calls cover O padrão trig/log/exp/rounding family plus dois-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates O expressão as `x = f(y)` e rotates O curva.
- `domain="a..b"` (x bounds shorthand) ou comma-separated clauses como `x>=0, x<5`.
- `color`, `label`. qualquer curva com Uma non-vazio rótulo é automaticamente listed em Uma legenda renderizado just abaixo O panel: Uma small cor swatch followed by O rótulo, com entradas flowing esquerda-para-direita e wrapping onto Uma new linha quando O próximo entrada would não fit.
- `tooltip` adds simples texto abaixo O computed dica. `showFunction` e `showValues` Padrão para `true`; Defina qualquer um para `false` para ocultar O renderizado equação ou live `(x, y)` valor respectively. A `<Plot>` / nested `<Function>` com `expr` pode conter Markdown e GuideNH tags as Uma rico dica corpo. O ordem é sempre rótulo, renderizado equação, live valores, `tooltip` texto, então rico filho conteúdo; omitted ou desativado computed campos são skipped em que ordem.
- `pointEveryX="step"` adds gerado ponto markers at regular x intervals on que curva.
- `pointEveryY="step"` adds gerado ponto markers where O curva intersects regular y intervals, using Uma bounded pesquisa.
- `autoPointLabel="none|x|y|xy"` controls gerado ponto labels; Padrão é `none`.
- `autoPointColor="#..."` substitui O gerado ponto cor; omitted significa inherit O curva cor.

Marked pontos (`<Point>`):

- explícito: `x="..."` e `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` ou `atY="v"` (O execução bisects on O plot's x-domain para find O correspondente `x`).
- opcional `color`, `label`.

Interaction: passar o mouse Uma curva para destacar it; press e hold para scrub Uma ponto along O curva. O dica starts com `label` quando supplied, então O equação e live `(x, y)` valor a menos que seus switches são desativado; it stays anchored acima de O ponto e flips abaixo quando there é não headroom.

## BetterQuesting Compatibility tags

`<QuestLink>` e `<QuestCard>` são somente registrado quando O BetterQuesting mod é carregado. Elas são documented em detail on O [Mod Compatibility](Mod-Compatibility) página; O summary abaixo covers O most comum usage.

### `<QuestLink>`

Inline link para Uma BetterQuesting quest. clique opens O quest dentro de O BetterQuesting GUI, a menos que O quest id é também present em O atual guia's `quest_ids` frontmatter — em que case O link navigates para que página em vez disso.

| Atributo | Significado |
| --- | --- |
| `id` | Obrigatório BetterQuesting quest id; accepts canonical UUID strings e compact Base64 ids |
| `text` | opcional substituir para O displayed texto |
| `show_tooltip` | opcional booleano (Padrão `true`); Defina para `false` para suppress O quest-descrição dica. `showTooltip` é accepted as Um alias |

visibilidade behavior é decided per player at compilar time:

- visível / completed quests renderizar as Uma clickable link (completed quests são tinted green e append a `✓` mark)
- locked but non-oculto quests ainda renderizar as clickable quest links so Elas pode open O BetterQuesting quest tela ou O indexed página do guia
- oculto / secret quests renderizar as Uma darker italic placeholder using `guidenh.compat.bq.hidden`
- desconhecido quest ids renderizar as Uma red placeholder using `guidenh.compat.bq.missing`

Exemplo:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

bloco-level summary card para Uma BetterQuesting quest. Renders O quest título com O mesmo estado-aware styling as `<QuestLink>`, plus O quest descrição as Uma corpo paragraph quando O quest é visível para O player.

| Atributo | Significado |
| --- | --- |
| `id` | Obrigatório BetterQuesting quest id; accepts canonical UUID strings e compact Base64 ids |
| `show_desc` | opcional booleano (Padrão `true`); Defina para `false` para suppress O descrição corpo |
| `show_tooltip` | opcional booleano (Padrão `true`); Defina para `false` para suppress O quest-descrição dica on O clickable título. `showTooltip` é accepted as Um alias |

O accent cor of O card borda follows O quest estado: green para completed, gray para locked / oculto, red para ausente, e O padrão link cor para visível quests. O título remains clickable para visível, completed, e locked-but-non-oculto quests.

Exemplo:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
