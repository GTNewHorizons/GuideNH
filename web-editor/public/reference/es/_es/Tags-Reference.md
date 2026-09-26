# etiquetas Reference


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

esto página lists El built-en ejecución etiquetas registrado by `DefaultExtensions`.

## Usage Reglas

- etiquetas puede appear cualquiera en bloque context o inline context depending on El compilador.
- MDX comments usando `{/* ... */}` son Compatible en página contenido y son ignored by El ejecución analizador.
- no válido etiquetas o no válido atributos renderizan guíUn errors inline en su lugar of silently failing.
- Large feature etiquetas como recipes y 3D escenas son documented en sus propio páginas:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## Inline y Flow etiquetas

| etiqueta | Purpose | clave atributos |
| --- | --- | --- |
| `<a>` | interno/externo enlace y opcional ancla nombre | `href`, `title`, `name` |
| `<br>` | línea break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style texto | none |
| `<sup>` | smaller inline superscript-style texto | none |
| `<Color>` | colored inline texto | `id` o `color` |
| `<Spoiler>` | ocultas inline texto revealed on pasar el cursor | none |
| `<Tooltip>` | enriquecido pasar el cursor tooltip con markdown/etiqueta hijos | `label` |
| `<SoundLink>` | clickable enriquecido-texto sound trigger | `sound` o `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted texto; equivalent a `==text==` con opcional color control | `color` |
| `<PlayerName>` | inserts actual jugador username | none |
| `<KeyBind>` | inserts keybinding mostrar nombre | `id` o `action` |
| `<ItemImage>` | inline elemento icon | `id` o `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | elemento tooltip + opcional navigation enlace | `id` o `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command enlace | `command`, `title`, `close` |
| `<Latex>` | LaTeX math fórmula; inline en flow context, centered mostrar bloque en bloque context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest enlace con estado-aware styling (compat etiqueta, solo registrado cuando BetterQuesting es cargado) | `id`, `text`, `show_tooltip` |

Inline markdown también admite action enlaces para sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## bloque etiquetas

| etiqueta | Purpose | clave atributos |
| --- | --- | --- |
| `<div>` | pass-a través de bloque wrapper | none |
| `<ContentTabs>` | groups alternative enriquecido contenido under independent tabs | `title`, `color`, `icon`, `iconPng`, `icon_png`, `iconItem`, `icon_item`, `default`, `defaultIndex` |
| `<Tab>` | uno contenido panel dentro de `<ContentTabs>` | `title` |
| `<details>` | collapsible ejecución bloque | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | directorio-style outline con conector líneas | `indent`, `gap` |
| `<Row>` | horizontal flex diseño | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertical flex diseño | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | ancho-constrained footnote container usado by ejecución markdown footnotes | `width` |
| `<ItemGrid>` | compact grid of elemento icons | hijos debe ser `<ItemIcon id="..."/>` o `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-interactivo 3D único-bloque vista previa | `id` o `ore`, opcional `scale` (predeterminados a `4`), `float`, `perspective`, `nbt` |
| `<FloatingImage>` | cropped imagen bloque con decimal o true inline placement | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | navigation hijo listing | `id`, `alphabetical` |
| `<Category>` | lista páginas de Un category | `name`, `rows` |
| `<Special>` | lista built-en MediaWiki special páginas | `name`, `rows` |
| `<Structure>` | 2.5D isometric bloque diseño vista | `width`, `height` |
| `<Mermaid>` | ejecución Mermaid gráfico import/inline | `src`, `width`, `height` |
| `<CsvTable>` | ejecución CSV archivo import tabla | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered columna gráfico | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar gráfico | mismo as `<ColumnChart>` |
| `<LineChart>` | línea gráfico con categorical o numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie gráfico | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter gráfico | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-curva función gráfico | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | único-curva shorthand para `<FunctionGraph>` | `expr`, plus todos `<FunctionGraph>` panel atributos |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | Consulta [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D guíUn escena | Consulta [GameScene](GameScene) |
| `<QuestCard>` | bloque-level BetterQuesting quest summary card (compat etiqueta, solo registrado cuando BetterQuesting es cargado) | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` es un `<GameScene>` hijo etiqueta. It accepts `controller`, `piece`, `facing`,
`rotation`, `flip`, y `channel` atributos, y también admite StructureLib predeterminado hijo etiquetas:
`<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`,
`<GregTechActiveController>`, y `<GregTechPlaceHatches>`.

## etiqueta Details

### `<a>`

Acts like Un HTML-style ancla etiqueta:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` puede ser relativo, rooted, explícito `modid:path`, o HTTP/HTTPS
- `title` se convierte en El tooltip
- `name` inserts Un página ancla destino

### `<br>`

GuideNH también admite Un MDX break etiqueta con decimal clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` valores:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, y `<sup>`

GuideNH ejecución admite Un focused subset of lowercase documentation etiquetas para inline usar:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

crea Un collapsible ejecución bloque con Un summary fila. El `<summary>` línea admite normal
inline markdown/etiqueta contenido, y El cuerpo puede hold ordinary texto plus arbitrary bloque etiquetas como
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, tablas, charts, y diseño containers.
cuando `height` es Establezca, solo El cuerpo scrolls; El summary fila y outer frame permanecer fijo.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

Texto oculto de forma predeterminada con un [enlace de página normal](./index.md).

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

atributos:

- `open` — starts expanded cuando present
- `width` — preferido outer ancho en píxeles
- `height` — preferido cuerpo viewport alto en píxeles; overflow se convierte en scrollable en-game y en site export
- `wrap` — admite El usual bloque embedding modes como `square`, `tight`, y `through`
- `align` — `left`, `center`, o `right`; cuando combined con Un floating ajuste modo, El whole details bloque floats

### `<ContentTabs>`

Groups alternative enriquecido contenido under independent tabs. solo directo `<Tab>` hijos son válido. El container itself puede también renderizan Un quote-style heading fila encima de El tabs, coincidente El visual idioma of markdown callouts.

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

- `default` coincide El primero tab whose `title` coincide exactamente
- `defaultIndex` es zero-based y wins sobre `default` cuando ambos son present
- `color` optionally anula El izquierda accent línea y seleccionado-tab resaltar con `#RRGGBB` o `#AARRGGBB`
- `title` añade Un opcional simple-texto heading encima de El tab strip
- `icon`, `iconPng` / `icon_png`, y `iconItem` / `icon_item` usar El mismo heading icon semantics as markdown quote-style callouts
- no válido hijos o no válido predeterminados renderizan visible autor-facing errors

### `<FileTree>`

renderiza Un directorio-style outline con real conector líneas drawn de El prefix glyphs on cada fila. ambos Unicode box-drawing (`│ ├ └ ─`) y ASCII (`| +-- \-- ` / four spaces) forms son accepted y puede ser mixed. Payload texto admite El usual inline markdown (enlaces, **bold**, `code`, …), y those enlaces son clickable ambos en-game y en El built-en site export. El mismo contenido puede también ser escrito as Un fenced ` ```tree ` or ` ```filetree ` bloque.

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

opcional per-fila icons son introduced by Un leading directive on El payload:

- `{:icon=Text}` — short texto etiqueta (único o double quotes opcional)
- `{:iconPng=path/to/file.png}` — PNG recurso resuelto against El actual página
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft elemento icon. El opcional `meta` segment es un damage valor (o `*` para Un wildcard); Un opcional trailing `:{snbt}` bloque carries SNBT Un attach Un El pila.

````md
```filetree
mundo
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} sample recurso
```
````

atributos:

- `indent` — píxeles per depth level (predeterminado `14`)
- `gap` — extra píxeles entre filas (predeterminado `0`)

### ejecución Blockquotes

normal markdown blockquotes renderizan at ejecución con Un izquierda accent línea. GitHub alert sintaxis es Compatible:

````md
> [!NOTE]
> Alert body
````

GuideNH también admite Un ejecución-solo personalizado directive on El primero quoted línea:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

Compatible directive claves:

- `title`
- `color`
- `icon` para simple texto symbols
- `iconItem` para an `ItemStack` id
- `iconPng` para Un guíUn recurso png ruta

solo uno icon origen deberíUn ser proporcionado.

### `<Color>`

usar cualquiera Un symbolic color id o Un explícito hex valor:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

Use `<Spoiler>` for inline hidden text. The content stays rich-text aware, so nested markdown and runtime inline tags
such as `<Color>` still render correctly after reveal.

```mdx
<Spoiler>ocultas **bold** texto con <Color color="#55ccff">tint</Color>.</Spoiler>
<Spoiler>[Anchor Link](#headings) todavía behaves like Un normal hoverable enlace cuando revealed.</Spoiler>
```

````

Reglas:

- `id` y `color` son mutually exclusive en practice; provide uno
- `color` accepts `#RRGGBB`, `#AARRGGBB`, o `transparent`

### `<Tooltip>`

crea underlined texto que opens Un enriquecido contenido tooltip on pasar el cursor.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

Si `label` es omitido, El trigger texto predeterminados a `tooltip`.

### `<SoundLink>` y Sound Action enlaces

`<SoundLink>` renderiza enriquecido inline contenido que plays Un sound cuando clicked. It hace no navigate,
y su personalizado clic sound replaces El normal guíUn clic sound para que clic.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound atributos:

- `sound` es un sound event id como `modid:event.name`
- `src` puntos at an `.ogg` archivo; `modid:sounds/machine/start.ogg` se convierte en `modid:machine.start`
- `volume` predeterminados a `1.0`
- `pitch` predeterminados a `1.0`
- `cooldown` es milliseconds entre repeated plays, predeterminado `250`
- `radius` y `minVolume` control pantalla-espacio attenuation cuando usado en escenas

### `<PlayerName>`

Inserts El actual Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up Un keybinding by id o action y renderiza El jugador's actual bound clave nombre.

Accepted ids:

- El binding descripción id, como `key.jump` o `key.guidenh.open_guide`
- El legacy `category.description` form, como `key.categories.movement.key.jump`

Ejemplo:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX Comments

GuideNH ignores MDX comments en página contenido:

````md
Texto visible. {/* comentario en línea oculto */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH también ignores explícito `<Comment>` etiquetas:

````md
Texto visible. <Comment>Esto no se renderiza.</Comment> Sigue visible.
````

### `<ItemImage>`

muestra Un inline elemento icon.

| atributo | significado |
| --- | --- |
| `ore` | ore dictionary nombre; El primero coincidir wins |
| `id` | elemento reference usado cuando `ore` es absent |
| `nbt` | opcional SNBT elemento datos; merged onto cualquier inline SNBT en `id` |
| `scale` | decimal, predeterminado `1` |
| `noTooltip` | truthy cadena o vacío atributo suppresses tooltip (legacy; prefer `showTooltip`) |
| `showTooltip` | booleano, predeterminado `true`; `false` suppresses El pasar el cursor tooltip |
| `showIcon` | booleano, predeterminado `true`; `false` oculta El elemento icon graphic |
| `label` | `left` o `right` — muestra El elemento mostrar nombre as texto on El specified lado of El icon; omit para no etiqueta |
| `format` | format pattern para El etiqueta texto; admite Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) con opcional `%s` placeholder para El elemento nombre; predeterminado (no atributo) renderiza El nombre en italic |
| `yOffset` | entero píxel offset anular para El **icon** at scale `1`; hace no affect El etiqueta texto |
| `labelYOffset` | entero píxel offset anular para El **etiqueta texto** at scale `1`; hace no affect El icon |

Notas:

- `ore` takes precedence sobre `id` cuando ambos son proporcionado
- Si GregTech es installed, El seleccionado ore coincidir es passed a través de `GTOreDictUnificator.setStack(...)`
- `label` requires at least uno of `showIcon` o `label` Un produce visible salida; setting ambos `showIcon="false"` y omitting `label` renderiza nothing
- `format` solo aplica cuando `label` es Establezca; Si `format` tiene no `%s`, El literal format texto es usado as El etiqueta
- inline SNBT en `id` remains Compatible; cuando ambos forms son present, El standalone `nbt` atributo es merged último y anula conflicting claves

Ejemplo:

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

crea Un texto enlace usando El elemento's mostrar nombre y elemento tooltip. Si `item_ids` puntos Un Un guíUn página, al hacer clic navigates Un it. `ore` puede ser usado Un resolver El mostrar pila de El primero ore dictionary coincidir en su lugar of Un fijo registry id.

| atributo | predeterminado | significado |
| --- | --- | --- |
| `id` | — | elemento registry id, e.g. `minecraft:compass` o `minecraft:wool:1` |
| `ore` | — | ore-dictionary nombre; usa El primero coincidente elemento pila |
| `linksTo` | *(auto)* | anula El enlace destino; accepts Un página id con opcional `#anchor`, e.g. `./crafting.md#usage` o `#usage`; cuando omitido El destino es resuelto de `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | Establezca a `false` Un suppress El pasar el cursor tooltip; `noTooltip` es un legacy alias |
| `showIcon` | *(none)* | `left` o `right` (o cualquier truthy valor → derecha) — renderiza El elemento icon beside El enlace texto; omit Un mostrar texto solo |
| `scale` | `1.0` | mostrar scale para El opcional elemento icon; tiene no effect cuando `showIcon` es omitido |

Ejemplos:

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

Sends Un chat command cuando clicked.

| atributo | significado |
| --- | --- |
| `command` | obligatorio, debe inicio con `/` |
| `title` | opcional tooltip heading |
| `close` | analizado booleano atributo; actualmente analizado but no usado Un close El guía |

Ejemplo:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` y `<Column>`

Flex-style containers para bloque contenido.

| atributo | significado |
| --- | --- |
| `gap` | entero gap entre hijos, predeterminado `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | booleano expresión, predeterminado `false` |
| `width` | entero preferido ancho; useful para constraining lista línea ancho |

Ejemplo:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

Un constrain El ancho of normal markdown lists, ajuste ellos en Un container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH usa esto bloque etiqueta internally cuando ejecución markdown footnotes son expanded. It puede también ser escrito manualmente Si needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

renderiza Un compact elemento grid. hijos debe ser sin procesar `<ItemIcon>` elements, que son analizado directly by El grid compilador. cada hijo puede usar cualquiera `id` o `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

renderiza Un non-interactivo 3D único-bloque escena. El vista previa tiene no escena fondo, no escena
buttons, no layer controls, y no anotación features, but al pasar el cursor El bloque todavía muestra El
selección outline y tooltip. `ore` debe resolver Un Un bloque elemento pila.

| atributo | significado |
| --- | --- |
| `id` | bloque id; admite El normal `modid:block[:meta][:{snbt}]` forma |
| `ore` | ore dictionary lookup; El primero coincidente bloque elemento wins |
| `scale` | cámara zoom multiplier, predeterminado `4` |
| `float` | legacy flow decimal compatibilidad: `left` o `right` |
| `perspective` | `isometric-north-east` (predeterminado), `isometric-north-west`, o `up` |
| `nbt` | opcional SNBT bloque-entidad datos merged onto cualquier inline SNBT de `id` |

Notas:

- inline SNBT dentro de `id` es todavía accepted para compatibility, but `nbt="..."` es El preferido
  authoring form
- cuando ambos inline SNBT y `nbt` son present, El `nbt` atributo es merged último y por tanto
  anula conflicting claves
- GuideNH 1.7.10 hace no compatibilidad modern bloque-estado propiedad sintaxis here, so GuideME-style
  `p:<state>` atributos son intentionally no Compatible

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

Consulta [Images And Assets](Images-And-Assets) para El completo comportamiento.

Reglas rápidas:

- `src` admite relativo paths, rooted paths, y explícito `modid:path` texture ids
- `x`, `y`, `width` / `w`, y `height` / `h` define El crop rectangle on El original imagen y debe todos ser present
- `scaleX` y `scaleY` resize El cropped resultado y compatibilidad independent horizontal / vertical stretching
- `displayWidth` y `displayHeight` Establezca final dimensions en píxeles; uno valor preserves El crop aspect ratio, mientras dos valores permitir stretching
- `displayWidth` / `displayHeight` no puede ser combined con `scaleX` / `scaleY`
- `wrap="inline"` places El imagen truly inline dentro de texto flow; en que modo `align` es ignored
- old contenido que usado `width` / `height` as final mostrar tamaño debe ser migrated manualmente

### `<SubPages>`, `<Category>`, y `<Special>`

Consulta [Navigation](Navigation) para completo navigation comportamiento.

### `<Structure>`

Consulta [Examples](Examples) y [GameScene](GameScene) cuando deciding whether Un usar Un static estructura vista previa o Un completo 3D escena.

### `<Mermaid>`

usado para ejecución Mermaid contenido. actual ejecución compatibilidad es focused on `mindmap`, cualquiera inline o a través de Un página-relativo `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
Los nodos de ejecución pueden incrustar bloques normales.

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` y `height` constrain El ejecución viewport box
- dentro de El viewport, arrastrar pans y El mouse wheel zooms
- quoted Mermaid labels puede usar enriquecido inline markdown como `**bold**` y página enlaces
- `<NodeContent id="...">...</NodeContent>` puede ser añadido as hijos of `<Mermaid>` Un reemplazar Un node cuerpo con arbitrary ejecución bloques

### `<CsvTable>`

usado Un analizar Un CSV archivo en Un ejecución tabla:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves relativo Un El actual página, El mismo way escena importaciones y normal recurso enlaces do.

opcional atributos:

- `header`
  predeterminados a `true`; Establezca `header={false}` Un Conserve El primero fila unbolded
- `widths`
  Comma-separated entero ancho hints como `widths="120,80"`

Ejemplos:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

El related fenced ejecución CSV form también admite coincidente metadatos:

````md
```csv widths="120,80" header=false
nombre,valor
iron,42
gold,17
```
````

### `<Latex>`

renderiza Un LaTeX math fórmula usando jlatexmath. cuando usado inline (dentro de Un paragraph o texto flow), it renderiza as Un scaled glyph que expands El línea alto Un fit El fórmula. cuando escrito as su propio paragraph (bloque context), it renderiza centered as Un mostrar-modo fórmula.

| atributo | tipo | predeterminado | descripción |
| --- | --- | --- | --- |
| `formula` | cadena | *(obligatorio)* | LaTeX origen cadena |
| `color` | `#RRGGBB` o `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | decimal | `1.0` | mostrar tamaño multiplier applied on arriba of El automático línea-alto scaling |
| `sourceScale` | decimal | `100.0` | jlatexmath interno renderizan resolution; higher valores improve quality at large sizes |
| `tooltip` | cadena | *(none)* | simple tooltip texto mostrado on pasar el cursor |
| `showTooltip` | booleano | `false` | mostrar El sin procesar LaTeX origen as Un tooltip on pasar el cursor |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-solo. Vertical alignment dentro de El texto línea: `baseline` (predeterminado) aligns El fórmula's math baseline con El texto baseline; `top` aligns El fórmula arriba con El línea arriba; `center` centers it on El texto; `bottom` aligns El fórmula abajo con El texto abajo |
| `offsetX` | int | `0` | Horizontal píxel offset applied después de alignment (positive = derecha) |
| `offsetY` | int | `0` | Vertical píxel offset applied después de alignment (positive = down) |

Ejemplos:

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

As Un convenience you puede escribir `$$formula$$` directly en Markdown sin usando El `<Latex>` etiqueta.
todos renderizado parámetros usar sus predeterminados (white colour, scale 1.0, no tooltip, baseline-aligned).

- **Inline**: `$$formula$$` embedded dentro de Un paragraph renderiza as Un inline fórmula.
- **mostrar**: Un paragraph whose entire contenido es `$$formula$$` (con opcional surrounding whitespace) renderiza as Un centred mostrar-modo bloque.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

Notas:

- El fórmula alto es calibrated Un El actual línea texto alto. simple formulas renderizan at texto alto; taller formulas (fractions, summations, integrals, etc.) expand El enclosing línea alto automáticamente.
- `valign` solo aplica Un inline formulas. mostrar-modo (bloque-level) formulas son siempre centered horizontally; usar `offsetY` Un shift ellos vertically dentro de El bloque.
- `color` predeterminados Un white (`#FFFFFF`). usar `#AARRGGBB` format para Un semi-transparente fill.
- `sourceScale` solo affects renderizan sharpness, no El displayed tamaño. valores debajo `16` son clamped a `16`.
- Tooltip priority es: enriquecido hijo Markdown contenido, entonces `tooltip="..."`, entonces `showTooltip={true}` sin procesar origen reserva.
- hijo tooltip contenido es compilado as regular guíUn Markdown, so it puede incluir bold texto, lists, enlaces, elemento etiquetas, y nested `<Latex>` formulas.
- El `$$formula$$` shorthand siempre usa predeterminado parámetros. usar El `<Latex>` etiqueta para personalizado colour, scale, alignment o tooltip.

### escena ejecución etiquetas

These etiquetas solo funcionan dentro de `<GameScene>` / `<Scene>`:

| etiqueta | Purpose | clave atributos |
| --- | --- | --- |
| `<ImportStructure>` | import Un externo SNBT/NBT estructura recurso | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import Un StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | eliminar ya-colocado bloques que coincidir Un bloque matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp El mismo hijo anotaciones onto cada coincidente colocado bloque | `id` |

Consulta [GameScene](GameScene) para escena import/removal comportamiento y [Annotations](Annotations) para anotación template Reglas.


## Charts

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, y `<ScatterChart>` son interactivo gráfico bloques. todos charts share El following común atributos:

| atributo | descripción | predeterminado |
| --- | --- | --- |
| `title` | gráfico título | none |
| `width` / `height` | explícito tamaño | 320 / 200 |
| `background` / `border` | fondo y borde colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | título y valor-etiqueta colors | light grey |
| `legend` | leyenda posición: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | valor-etiqueta posición: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | interno plot leyenda posición: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum interno leyenda box tamaño | `120` / `64` |
| `cornerLegendBackground` | interno leyenda fondo color | `#AA111922` |

Cartesian charts (columna / Bar / línea / Scatter) additionally aceptan eje atributos `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` y El coincidente `yAxis*` Establezca, plus `showXGrid={true}` / `showYGrid={true}` Un toggle gridlines.

hijos:

* `<Series name="..." color="#..." data="10,20,30"/>` para category-based charts (columna / Bar / categorical línea).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` para numeric X (línea `numericX={true}`, Scatter).
* `<Slice name="..." value="..." color="#..."/>` para `<PieChart>` solo.

cuando `color` es omitido on a `<Series>` o `<Slice>`, GuideNH cycles a través de Un built-en 16-color palette.

`<Series>` y `<Slice>` también aceptan El following opcional icon / tooltip atributos:

* `icon="modid:item"` (mismo sintaxis as `<ItemImage>`'s `id`, puede incluir `@meta` y inline NBT JSON) — binds an `ItemStack` Un El entrada; El leyenda swatch se convierte en El elemento icon y al pasar el cursor El datos punto muestra El vanilla elemento tooltip con El gráfico descripción appended at El fin.
* `iconImage="images/foo.png"` — usar Un PNG recurso as El leyenda swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form texto appended Un El tooltip (usar `\n` para multi-línea).

Ejemplo:

```mdx
<PieChart title="Output share">
  <Slice name="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice name="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra atributos: `categories` (X-eje o Y-eje labels, comma separated), `barWidthRatio` (predeterminado 0.7). `<BarChart>` puts El categories on El Y-eje y valores on El X-eje.

#### Combo extensions

`<ColumnChart>` y `<BarChart>` aceptan dos extra hijo element types so varios gráfico styles puede share uno plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn as Un polyline overlay on arriba of El bars. cada línea punto sits at El cluster centro of El coincidente category index; El overlay shares El host gráfico's valor eje. You puede declare varios `<LineSeries>` Un overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — Un small pie gráfico drawn dentro de uno of El four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) of El plot area. su `<Slice>` hijos share El mismo sintaxis as en `<PieChart>`.

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

Extra atributos: `numericX={true}` Un enable Un numeric X-eje (hijos debe usar `points`); `showPoints={false}` oculta punto marcadores. El hovered punto es pushed outward by 2px a lo largo de El curva normal, enlarged, y outlined; El adjacent línea segments thicken by 1px.

`<LineChart>` y `<ScatterChart>` puede mostrar Un compact leyenda dentro de El plot area con `cornerLegend="topRight"` o another corner. entradas usar existing serie names y colors.

### `<PieChart>`

Extra atributos: `startAngle` (predeterminado `-90`, i.e. 12 o'clock); `clockwise={false}` Un reverse direction. El hovered sector pops outward 4px a lo largo de su bisector.

### `<ScatterChart>`

renderiza puntos solo; `<Series>` debe usar `points`. El X-eje es siempre numeric.

## función Graphs

`<FunctionGraph>` y El único-curva shorthand `<Function>` renderizan Un interactivo Desmos-style panel. El mismo panel es también disponible a través de a ` ```funcgraph ` fenced code bloque; Consulta El ejecución [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) para Un completo walkthrough.

Panel atributos (accepted by El container, El shorthand, y El fence header alike):

- `width` / `height` (predeterminados `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (predeterminado `true`)
- `xRange="a..b"` (o `xMin` / `xMax` separately), `xStep` para tick spacing; mismo para El Y eje
- `xLabel` / `yLabel` Añada Excel-style eje titles debajo y encima de El plot respectively. Estas compatibilidad inline `$$...$$` LaTeX; `domain="a..b"` es un legacy alias para `xRange` cuando no explícito X range es present
- `quadrants="1,2,3,4"` o `quadrants="all"` Un force El visible quadrants; omit Un inicio en quadrant 1 con auto-expansion cuando sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, y `cornerLegendBackground` mostrar Un compact leyenda dentro de El plot area usando non-vacío curva labels

curva hijos (`<Plot>` / `<Function>`):

- `expr="..."` &mdash; El expresión. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` absoluto valor, `√` / `sqrt` / `∛` / `cbrt`, implícito multiplication, y El constants `pi`, `tau`, `e`, `phi` son Compatible. Built-en calls cover El estándar trig/log/exp/rounding family plus dos-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates El expresión as `x = f(y)` y rotates El curva.
- `domain="a..b"` (x límites shorthand) o comma-separated clauses como `x>=0, x<5`.
- `color`, `label`. cualquier curva con Un non-vacío etiqueta es automáticamente listed en Un leyenda renderizado just debajo El panel: Un small color swatch followed by El etiqueta, con entradas flowing izquierda-a-derecha y wrapping onto Un nuevo fila cuando El siguiente entrada would no fit.
- `tooltip` añade simple texto debajo El computed tooltip. `showFunction` y `showValues` predeterminado a `true`; Establezca cualquiera a `false` Un ocultar El renderizado ecuación o live `(x, y)` valor respectively. A `<Plot>` / nested `<Function>` con `expr` puede contener Markdown y GuideNH etiquetas as Un enriquecido tooltip cuerpo. El orden es siempre etiqueta, renderizado ecuación, live valores, `tooltip` texto, entonces enriquecido hijo contenido; omitido o desactivado computed campos son skipped en que orden.
- `pointEveryX="step"` añade generado punto marcadores at regular x intervals on que curva.
- `pointEveryY="step"` añade generado punto marcadores where El curva intersects regular y intervals, usando Un bounded búsqueda.
- `autoPointLabel="none|x|y|xy"` controls generado punto labels; predeterminado es `none`.
- `autoPointColor="#..."` anula El generado punto color; omitido significa inherit El curva color.

Marked puntos (`<Point>`):

- explícito: `x="..."` y `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` o `atY="v"` (El ejecución bisects on El plot's x-domain Un find El coincidente `x`).
- opcional `color`, `label`.

Interaction: pasar el cursor Un curva Un resaltar it; press y hold Un scrub Un punto a lo largo de El curva. El tooltip starts con `label` cuando supplied, entonces El ecuación y live `(x, y)` valor salvo sus switches son desactivado; it stays anchored encima de El punto y flips debajo cuando there es no headroom.

## BetterQuesting Compatibility etiquetas

`<QuestLink>` y `<QuestCard>` son solo registrado cuando El BetterQuesting mod es cargado. Estas son documented en detail on El [Mod Compatibility](Mod-Compatibility) página; El summary debajo covers El most común usage.

### `<QuestLink>`

Inline enlace Un Un BetterQuesting quest. al hacer clic opens El quest dentro de El BetterQuesting GUI, salvo El quest id es también present en El actual guía's `quest_ids` frontmatter — en que case El enlace navigates Un que página en su lugar.

| atributo | significado |
| --- | --- |
| `id` | obligatorio BetterQuesting quest id; accepts canonical UUID strings y compact Base64 ids |
| `text` | opcional anular para El displayed texto |
| `show_tooltip` | opcional booleano (predeterminado `true`); Establezca a `false` Un suppress El quest-descripción tooltip. `showTooltip` es accepted as Un alias |

visibilidad comportamiento es decided per jugador at compilar tiempo:

- visible / completed quests renderizan as Un clickable enlace (completed quests son tinted green y append a `✓` mark)
- locked but non-ocultas quests todavía renderizan as clickable quest enlaces so Estas puede open El BetterQuesting quest pantalla o El indexed guíUn página
- ocultas / secret quests renderizan as Un darker italic placeholder usando `guidenh.compat.bq.hidden`
- desconocido quest ids renderizan as Un red placeholder usando `guidenh.compat.bq.missing`

Ejemplo:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

bloque-level summary card para Un BetterQuesting quest. renderiza El quest título con El mismo estado-aware styling as `<QuestLink>`, plus El quest descripción as Un cuerpo paragraph cuando El quest es visible Un El jugador.

| atributo | significado |
| --- | --- |
| `id` | obligatorio BetterQuesting quest id; accepts canonical UUID strings y compact Base64 ids |
| `show_desc` | opcional booleano (predeterminado `true`); Establezca a `false` Un suppress El descripción cuerpo |
| `show_tooltip` | opcional booleano (predeterminado `true`); Establezca a `false` Un suppress El quest-descripción tooltip on El clickable título. `showTooltip` es accepted as Un alias |

El accent color of El card borde follows El quest estado: green para completed, gray para locked / ocultas, red para faltante, y El estándar enlace color para visible quests. El título remains clickable para visible, completed, y locked-but-non-ocultas quests.

Ejemplo:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
