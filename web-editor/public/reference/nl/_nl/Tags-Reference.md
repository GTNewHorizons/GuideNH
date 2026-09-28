# Tagreferentie

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


Deze pagina lists De built-in runtime tags registered by `DefaultExtensions`.

## Gebruiksregels

- tags kan appear either in blok context of inline context depending on De compiler.
- MDX comments using `{/* ... */}` are supported in pagina inhoud en are ignored by De runtime parser.
- Invalid tags of invalid attributen renderen gids errors inline instead of silently failing.
- Large feature tags such as Recepten en 3D scènes are documented in their own pages:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## Inline- en flow-tags

| tag | Purpose | Key attributen |
| --- | --- | --- |
| `<a>` | internal/external link en optioneel anchor naam | `href`, `title`, `name` |
| `<br>` | regel break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style tekst | none |
| `<sup>` | smaller inline superscript-style tekst | none |
| `<Color>` | colored inline tekst | `id` of `color` |
| `<Spoiler>` | verborgen inline tekst revealed on zweven | none |
| `<Tooltip>` | rich zweven tooltip met Markdown/tag kinderen | `label` |
| `<SoundLink>` | clickable rich-tekst sound trigger | `sound` of `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted tekst; equivalent naar `==text==` met optioneel kleur control | `color` |
| `<PlayerName>` | inserts huidige player username | none |
| `<KeyBind>` | inserts keybinding weergeven naam | `id` of `action` |
| `<ItemImage>` | inline item icon | `id` of `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | item tooltip + optioneel Navigatie link | `id` of `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command link | `command`, `title`, `close` |
| `<Latex>` | LaTeX math formula; inline in flow context, centered weergeven blok in blok context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest link met status-aware styling (compat tag, alleen registered wanneer BetterQuesting is loaded) | `id`, `text`, `show_tooltip` |

Inline Markdown ook ondersteunt action links voor sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## Bloktags

| tag | Purpose | Key attributen |
| --- | --- | --- |
| `<div>` | pass-through blok wrapper | none |
| `<ContentTabs>` | groups alternative rich inhoud under independent tabs | `title`, `color`, `icon`, `iconPng`, `icon_png`, `iconItem`, `icon_item`, `default`, `defaultIndex` |
| `<Tab>` | one inhoud panel binnen `<ContentTabs>` | `title` |
| `<details>` | collapsible runtime blok | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | directory-style outline met connector regels | `indent`, `gap` |
| `<Row>` | horizontal flex layout | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertical flex layout | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | breedte-constrained footnote container gebruikt by runtime Markdown footnotes | `width` |
| `<ItemGrid>` | compact grid of item icons | kinderen moet be `<ItemIcon id="..."/>` of `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-interactief 3D enkele-blok voorbeeldweergave | `id` of `ore`, optioneel `scale` (defaults naar `4`), `float`, `perspective`, `nbt` |
| `<FloatingImage>` | cropped afbeelding blok met kommagetal of true inline placement | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | Navigatie kind listing | `id`, `alphabetical` |
| `<Category>` | lijst pages van Een category | `name`, `rows` |
| `<Special>` | lijst built-in MediaWiki special pages | `name`, `rows` |
| `<Structure>` | 2.5D isometric blok layout view | `width`, `height` |
| `<Mermaid>` | runtime Mermaid graph import/inline | `src`, `width`, `height` |
| `<CsvTable>` | runtime CSV bestand import tabel | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered kolom chart | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar chart | zelfde as `<ColumnChart>` |
| `<LineChart>` | regel chart met categorical of numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie chart | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter chart | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-curve function graph | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | enkele-curve shorthand voor `<FunctionGraph>` | `expr`, plus alle `<FunctionGraph>` panel attributen |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | see [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D gids scène | see [GameScene](GameScene) |
| `<QuestCard>` | blok-level BetterQuesting quest summary card (compat tag, alleen registered wanneer BetterQuesting is loaded) | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` is a `<GameScene>` kind tag. It accepts `controller`, `piece`, `facing`,
`rotation`, `flip`, en `channel` attributen, en ook ondersteunt StructureLib Standaard kind tags:
`<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`,
`<GregTechActiveController>`, en `<GregTechPlaceHatches>`.

## Tagdetails

### `<a>`

Acts like Een HTML-style anchor tag:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` kan be relative, rooted, explicit `modid:path`, of HTTP/HTTPS
- `title` becomes De tooltip
- `name` inserts Een pagina anchor target

### `<br>`

GuideNH ook ondersteunt Een MDX break tag met kommagetal clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` waarden:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, en `<sup>`

GuideNH runtime ondersteunt Een focused subset of lowercase documentation tags voor inline gebruiken:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

Creates Een collapsible runtime blok met Een summary rij. De `<summary>` regel ondersteunt normal
inline Markdown/tag inhoud, en De body kan hold ordinary tekst plus arbitrary Bloktags such as
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, tabellen, Grafieken, en layout containers.
wanneer `height` is Stel in, alleen De body scrolls; De summary rij en outer frame stay vast.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

Standaard verborgen tekst met een [normale paginalink](./index.md).

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

attributen:

- `open` — starts expanded wanneer present
- `width` — preferred outer breedte in pixels
- `height` — preferred body viewport hoogte in pixels; overflow becomes scrollable in-game en in site export
- `wrap` — ondersteunt De usual blok embedding modes such as `square`, `tight`, en `through`
- `align` — `left`, `center`, of `right`; wanneer combined met Een floating wrap modus, De whole details blok floats

### `<ContentTabs>`

Groups alternative rich inhoud under independent tabs. alleen direct `<Tab>` kinderen are valid. De container itself kan ook renderen Een quote-style heading rij above De tabs, matching De visual taal of Markdown callouts.

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

- `default` matches De first tab whose `title` matches exactly
- `defaultIndex` is zero-based en wins over `default` wanneer both are present
- `color` optionally overrides De left accent regel en geselecteerd-tab markeren met `#RRGGBB` of `#AARRGGBB`
- `title` adds Een optioneel plain-tekst heading above De tab strip
- `icon`, `iconPng` / `icon_png`, en `iconItem` / `icon_item` gebruiken De zelfde heading icon semantics as Markdown quote-style callouts
- invalid kinderen of invalid defaults renderen zichtbaar author-facing errors

### `<FileTree>`

Renders Een directory-style outline met real connector regels drawn van De prefix glyphs on elke rij. Both Unicode box-drawing (`│ ├ └ ─`) en ASCII (`| +-- \-- ` / four spaces) forms are accepted en kan be mixed. Payload tekst ondersteunt De usual inline Markdown (links, **bold**, `code`, …), en those links are clickable both in-game en in De built-in site export. De zelfde inhoud kan ook be written as Een fenced ` ```tree ` or ` ```filetree ` blok.

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

optioneel per-rij icons are introduced by Een leading directive on De payload:

- `{:icon=Text}` — short tekst label (enkele of double quotes optioneel)
- `{:iconPng=path/to/file.png}` — PNG asset opgelost against De huidige pagina
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft item icon. De optioneel `meta` segment is Een damage waarde (of `*` voor Een wildcard); Een optioneel trailing `:{snbt}` blok carries SNBT naar attach naar De stack.

````md
```filetree
wereld
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} sample asset
```
````

attributen:

- `indent` — pixels per depth level (Standaard `14`)
- `gap` — extra pixels between rijen (Standaard `0`)

### runtime Blockquotes

Normal Markdown blockquotes renderen at runtime met Een left accent regel. GitHub alert syntaxis is supported:

````md
> [!NOTE]
> Alert body
````

GuideNH ook ondersteunt Een runtime-alleen aangepaste directive on De first quoted regel:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

Supported directive keys:

- `title`
- `color`
- `icon` voor plain tekst symbols
- `iconItem` voor an `ItemStack` id
- `iconPng` voor Een gids asset png pad

alleen one icon source zou moeten be provided.

### `<Color>`

gebruiken either Een symbolic kleur id of Een explicit hex waarde:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

Use `<Spoiler>` for inline hidden text. The content stays rich-text aware, so nested markdown and runtime inline tags
such as `<Color>` still render correctly after reveal.

```mdx
<Spoiler>verborgen **bold** tekst met <Color color="#55ccff">tint</Color>.</Spoiler>
<Spoiler>[Anchor Link](#headings) still behaves like Een normal hoverable link wanneer revealed.</Spoiler>
```

````

Regels:

- `id` en `color` are mutually exclusive in practice; bieden one
- `color` accepts `#RRGGBB`, `#AARRGGBB`, of `transparent`

### `<Tooltip>`

Creates underlined tekst that opens Een rich inhoud tooltip on zweven.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

Als `label` wordt weggelaten, De trigger tekst defaults naar `tooltip`.

### `<SoundLink>` en Sound Action Links

`<SoundLink>` renders rich inline inhoud that plays Een sound wanneer clicked. It doet niet navigate,
en its aangepaste klikken sound replaces De normal gids klikken sound voor that klikken.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound attributen:

- `sound` is Een sound event id such as `modid:event.name`
- `src` punten at an `.ogg` bestand; `modid:sounds/machine/start.ogg` becomes `modid:machine.start`
- `volume` defaults naar `1.0`
- `pitch` defaults naar `1.0`
- `cooldown` is milliseconds between repeated plays, Standaard `250`
- `radius` en `minVolume` control schermruimte attenuation wanneer gebruikt in scènes

### `<PlayerName>`

Inserts De huidige Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up Een keybinding by id of action en renders De player's huidige bound key naam.

Accepted ids:

- De binding description id, such as `key.jump` of `key.guidenh.open_guide`
- De legacy `category.description` form, such as `key.categories.movement.key.jump`

Example:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX Comments

GuideNH ignores MDX comments in pagina inhoud:

````md
Zichtbare tekst. {/* verborgen inlineopmerking */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH ook ignores explicit `<Comment>` tags:

````md
Zichtbare tekst. <Comment>Dit wordt niet gerenderd.</Comment> Blijft zichtbaar.
````

### `<ItemImage>`

Shows Een inline item icon.

| Attribuut | Betekenis |
| --- | --- |
| `ore` | ore dictionary naam; De first match wins |
| `id` | item reference gebruikt wanneer `ore` is absent |
| `nbt` | optioneel SNBT item data; merged onto any inline SNBT in `id` |
| `scale` | kommagetal, Standaard `1` |
| `noTooltip` | truthy tekenreeks of empty Attribuut suppresses tooltip (legacy; prefer `showTooltip`) |
| `showTooltip` | boolean, Standaard `true`; `false` suppresses De zweven tooltip |
| `showIcon` | boolean, Standaard `true`; `false` hides De item icon graphic |
| `label` | `left` of `right` — shows De item weergeven naam as tekst on De specified side of De icon; omit voor no label |
| `format` | format pattern voor De label tekst; ondersteunt Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) met optioneel `%s` placeholder voor De item naam; Standaard (no Attribuut) renders De naam in italic |
| `yOffset` | geheel getal pixel offset override voor De **icon** at scale `1`; doet niet affect De label tekst |
| `labelYOffset` | geheel getal pixel offset override voor De **label tekst** at scale `1`; doet niet affect De icon |

Notes:

- `ore` takes precedence over `id` wanneer both are provided
- Als GregTech is installed, De geselecteerd ore match is passed through `GTOreDictUnificator.setStack(...)`
- `label` requires at least one of `showIcon` of `label` naar produce zichtbaar output; setting both `showIcon="false"` en omitting `label` renders nothing
- `format` alleen applies wanneer `label` is Stel in; Als `format` has no `%s`, De literal format tekst is gebruikt as De label
- inline SNBT in `id` remains supported; wanneer both forms are present, De standalone `nbt` Attribuut is merged last en overrides conflicting keys

Example:

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

Creates Een tekst link using De item's weergeven naam en item tooltip. Als `item_ids` punten naar Een gids­pagina, clicking navigates naar it. `ore` kan be gebruikt naar oplossen De weergeven stack van De first ore dictionary match instead of Een vast registry id.

| Attribuut | Standaard | Betekenis |
| --- | --- | --- |
| `id` | — | item registry id, e.g. `minecraft:compass` of `minecraft:wool:1` |
| `ore` | — | ore-dictionary naam; uses De first matching item stack |
| `linksTo` | *(auto)* | overrides De link target; accepts Een pagina-ID met optioneel `#anchor`, e.g. `./crafting.md#usage` of `#usage`; wanneer omitted De target is opgelost van `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | Stel in naar `false` naar suppress De zweven tooltip; `noTooltip` is Een legacy alias |
| `showIcon` | *(none)* | `left` of `right` (of any truthy waarde → right) — renders De item icon beside De link tekst; omit naar tonen tekst alleen |
| `scale` | `1.0` | weergeven scale voor De optioneel item icon; has no effect wanneer `showIcon` wordt weggelaten |

Voorbeelden:

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

Sends Een chat command wanneer clicked.

| Attribuut | Betekenis |
| --- | --- |
| `command` | Vereist, moet begin met `/` |
| `title` | optioneel tooltip heading |
| `close` | parsed boolean Attribuut; currently parsed but niet gebruikt naar close De gids |

Example:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` en `<Column>`

Flex-style containers voor blok inhoud.

| Attribuut | Betekenis |
| --- | --- |
| `gap` | geheel getal gap between kinderen, Standaard `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | boolean expressie, Standaard `false` |
| `width` | geheel getal preferred breedte; useful voor constraining lijst regel breedte |

Example:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

naar constrain De breedte of normal Markdown lists, wrap them in Een container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH uses Deze blok tag internally wanneer runtime Markdown footnotes are expanded. It kan ook be written manually Als needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

Renders Een compact item grid. kinderen moet be raw `<ItemIcon>` elements, which are parsed directly by De grid compiler. elke kind kan gebruiken either `id` of `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

Renders Een non-interactief 3D enkele-blok scène. De voorbeeldweergave has no scène achtergrond, no scène
buttons, no layer controls, en no annotatie features, but hovering De blok still shows De
selectie outline en tooltip. `ore` moet oplossen naar Een blok item stack.

| Attribuut | Betekenis |
| --- | --- |
| `id` | blok id; ondersteunt De normal `modid:block[:meta][:{snbt}]` spelling |
| `ore` | ore dictionary lookup; De first matching blok item wins |
| `scale` | camera zoomen multiplier, Standaard `4` |
| `float` | legacy flow kommagetal ondersteuning: `left` of `right` |
| `perspective` | `isometric-north-east` (Standaard), `isometric-north-west`, of `up` |
| `nbt` | optioneel SNBT tile-entity data merged onto any inline SNBT van `id` |

Notes:

- inline SNBT binnen `id` is still accepted voor compatibility, but `nbt="..."` is De preferred
  authoring form
- wanneer both inline SNBT en `nbt` are present, De `nbt` Attribuut is merged last en therefore
  overrides conflicting keys
- GuideNH 1.7.10 doet niet ondersteuning modern blok-status eigenschap syntaxis here, so GuideME-style
  `p:<state>` attributen are intentionally niet supported

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

See [Images And Assets](Images-And-Assets) voor De full behavior.

Quick Regels:

- `src` ondersteunt relative paths, rooted paths, en explicit `modid:path` texture ids
- `x`, `y`, `width` / `w`, en `height` / `h` define De crop rectangle on De original afbeelding en moet alle be present
- `scaleX` en `scaleY` resize De cropped result en ondersteuning independent horizontal / vertical stretching
- `displayWidth` en `displayHeight` Stel in final dimensions in pixels; one waarde preserves De crop aspect ratio, terwijl two waarden toestaan stretching
- `displayWidth` / `displayHeight` kan niet be combined met `scaleX` / `scaleY`
- `wrap="inline"` places De afbeelding truly inline binnen tekst flow; in that modus `align` is ignored
- old inhoud that gebruikt `width` / `height` as final weergeven grootte moet be migrated manually

### `<SubPages>`, `<Category>`, en `<Special>`

See [Navigation](Navigation) voor full Navigatie behavior.

### `<Structure>`

See [Examples](Examples) en [GameScene](GameScene) wanneer deciding whether naar gebruiken Een static structure voorbeeldweergave of Een full 3D scène.

### `<Mermaid>`

gebruikt voor runtime Mermaid inhoud. huidige runtime ondersteuning is focused on `mindmap`, either inline of through Een pagina-relative `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
Runtimeknooppunten kunnen normale blokken insluiten.

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` en `height` constrain De runtime viewport box
- binnen De viewport, slepen pans en De mouse wheel zooms
- quoted Mermaid labels kan gebruiken rich inline Markdown such as `**bold**` en pagina links
- `<NodeContent id="...">...</NodeContent>` kan be added as kinderen of `<Mermaid>` naar vervangen Een node body met arbitrary runtime blokken

### `<CsvTable>`

gebruikt naar parse Een CSV bestand into Een runtime tabel:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves relative naar De huidige pagina, De zelfde way scène imports en normal asset links do.

optioneel attributen:

- `header`
  Defaults naar `true`; Stel in `header={false}` naar Behoud De first rij unbolded
- `widths`
  Comma-separated geheel getal breedte hints such as `widths="120,80"`

Voorbeelden:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

De related fenced runtime CSV form ook ondersteunt matching metadata:

````md
```csv widths="120,80" header=false
naam,waarde
iron,42
gold,17
```
````

### `<Latex>`

Renders Een LaTeX math formula using jlatexmath. wanneer gebruikt inline (binnen Een paragraph of tekst flow), it renders as Een scaled glyph that expands De regel hoogte naar fit De formula. wanneer written as its own paragraph (blok context), it renders centered as Een weergeven-modus formula.

| Attribuut | Type | Standaard | Description |
| --- | --- | --- | --- |
| `formula` | tekenreeks | *(Vereist)* | LaTeX source tekenreeks |
| `color` | `#RRGGBB` of `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | kommagetal | `1.0` | weergeven grootte multiplier applied on top of De automatic regel-hoogte scaling |
| `sourceScale` | kommagetal | `100.0` | jlatexmath internal renderen resolution; higher waarden improve quality at large sizes |
| `tooltip` | tekenreeks | *(none)* | Plain tooltip tekst getoond on zweven |
| `showTooltip` | boolean | `false` | tonen De raw LaTeX source as Een tooltip on zweven |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-alleen. Vertical alignment within De tekst regel: `baseline` (Standaard) aligns De formula's math baseline met De tekst baseline; `top` aligns De formula top met De regel top; `center` centers it on De tekst; `bottom` aligns De formula bottom met De tekst bottom |
| `offsetX` | int | `0` | Horizontal pixel offset applied na alignment (positive = right) |
| `offsetY` | int | `0` | Vertical pixel offset applied na alignment (positive = down) |

Voorbeelden:

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

As Een convenience you kan write `$$formula$$` directly in Markdown zonder using De `<Latex>` tag.
alle rendering parameters gebruiken their defaults (white colour, scale 1.0, no tooltip, baseline-aligned).

- **Inline**: `$$formula$$` embedded binnen Een paragraph renders as Een inline formula.
- **weergeven**: Een paragraph whose entire inhoud is `$$formula$$` (met optioneel surrounding whitespace) renders as Een centred weergeven-modus blok.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

Notes:

- De formula hoogte is calibrated naar De huidige regel tekst hoogte. Simple formulas renderen at tekst hoogte; taller formulas (fractions, summations, integrals, etc.) expand De enclosing regel hoogte automatisch.
- `valign` alleen applies naar inline formulas. weergeven-modus (blok-level) formulas are always centered horizontally; gebruiken `offsetY` naar shift them vertically within De blok.
- `color` defaults naar white (`#FFFFFF`). gebruiken `#AARRGGBB` format voor Een semi-transparent fill.
- `sourceScale` alleen affects renderen sharpness, niet De displayed grootte. waarden below `16` are clamped naar `16`.
- tooltip priority is: rich kind Markdown inhoud, then `tooltip="..."`, then `showTooltip={true}` raw source fallback.
- kind tooltip inhoud is compiled as regular gids Markdown, so it kan include bold tekst, lists, links, item tags, en nested `<Latex>` formulas.
- De `$$formula$$` shorthand always uses Standaard parameters. gebruiken De `<Latex>` tag voor aangepaste colour, scale, alignment of tooltip.

### scène runtime tags

Deze tags alleen work binnen `<GameScene>` / `<Scene>`:

| tag | Purpose | Key attributen |
| --- | --- | --- |
| `<ImportStructure>` | import Een external SNBT/NBT structure asset | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import Een StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | verwijderen al-placed blokken that match Een blok matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp De zelfde kind Annotaties onto iedere matching placed blok | `id` |

See [GameScene](GameScene) voor scène import/removal behavior en [Annotations](Annotations) voor annotatie template Regels.


## Grafieken

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, en `<ScatterChart>` are interactief chart blokken. alle Grafieken share De following algemene attributen:

| Attribuut | Description | Standaard |
| --- | --- | --- |
| `title` | Chart title | none |
| `width` / `height` | Explicit grootte | 320 / 200 |
| `background` / `border` | achtergrond en rand colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | Title en waarde-label colors | light grey |
| `legend` | Legend positie: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | waarde-label positie: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | Internal plot legend positie: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum internal legend box grootte | `120` / `64` |
| `cornerLegendBackground` | Internal legend achtergrond kleur | `#AA111922` |

Cartesian Grafieken (kolom / Bar / regel / Scatter) additionally accept axis attributen `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` en De matching `yAxis*` Stel in, plus `showXGrid={true}` / `showYGrid={true}` naar toggle gridlines.

kinderen:

* `<Series name="..." color="#..." data="10,20,30"/>` voor category-based Grafieken (kolom / Bar / categorical regel).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` voor numeric X (regel `numericX={true}`, Scatter).
* `<Slice name="..." value="..." color="#..."/>` voor `<PieChart>` alleen.

wanneer `color` wordt weggelaten on a `<Series>` of `<Slice>`, GuideNH cycles through Een built-in 16-kleur palette.

`<Series>` en `<Slice>` ook accept De following optioneel icon / tooltip attributen:

* `icon="modid:item"` (zelfde syntaxis as `<ItemImage>`'s `id`, kan include `@meta` en inline NBT JSON) — binds an `ItemStack` naar De entry; De legend swatch becomes De item icon en hovering De data punt shows De vanilla item tooltip met De chart description appended at De einde.
* `iconImage="images/foo.png"` — gebruiken Een PNG asset as De legend swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form tekst appended naar De tooltip (gebruiken `\n` voor multi-regel).

Example:

```mdx
<PieChart title="Output share">
  <Slice name="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice name="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra attributen: `categories` (X-axis of Y-axis labels, comma separated), `barWidthRatio` (Standaard 0.7). `<BarChart>` puts De categories on De Y-axis en waarden on De X-axis.

#### Combo extensions

`<ColumnChart>` en `<BarChart>` accept two extra kind element types so meerdere chart styles kan share one plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn as Een polylijn overlay on top of De bars. elke regel punt sits at De cluster center of De matching category index; De overlay shares De host chart's waarde axis. You kan declare meerdere `<LineSeries>` naar overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — Een small pie chart drawn binnen one of De four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) of De plot area. Its `<Slice>` kinderen share De zelfde syntaxis as in `<PieChart>`.

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

Extra attributen: `numericX={true}` naar enable Een numeric X-axis (kinderen moet gebruiken `points`); `showPoints={false}` hides punt markers. De hovered punt is pushed outward by 2px along De curve normal, enlarged, en outlined; De adjacent regel segments thicken by 1px.

`<LineChart>` en `<ScatterChart>` kan tonen Een compact legend binnen De plot area met `cornerLegend="topRight"` of another corner. Entries gebruiken existing series names en colors.

### `<PieChart>`

Extra attributen: `startAngle` (Standaard `-90`, i.e. 12 o'clock); `clockwise={false}` naar reverse direction. De hovered slice pops outward 4px along its bisector.

### `<ScatterChart>`

Renders punten alleen; `<Series>` moet gebruiken `points`. De X-axis is always numeric.

## Functiegrafieken

`<FunctionGraph>` en De enkele-curve shorthand `<Function>` renderen Een interactief Desmos-style panel. De zelfde panel is ook available through a ` ```funcgraph ` fenced codeblok; see De runtime [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) voor Een full walkthrough.

Panel attributen (accepted by De container, De shorthand, en De fence header alike):

- `width` / `height` (defaults `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (Standaard `true`)
- `xRange="a..b"` (of `xMin` / `xMax` separately), `xStep` voor tick spacing; zelfde voor De Y axis
- `xLabel` / `yLabel` Voeg toe Excel-style axis titles below en above De plot respectively. They ondersteuning inline `$$...$$` LaTeX; `domain="a..b"` is Een legacy alias voor `xRange` wanneer no explicit X range is present
- `quadrants="1,2,3,4"` of `quadrants="all"` naar force De zichtbaar quadrants; omit naar begin in quadrant 1 met auto-expansion wanneer sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, en `cornerLegendBackground` tonen Een compact legend binnen De plot area using non-empty curve labels

Curve kinderen (`<Plot>` / `<Function>`):

- `expr="..."` &mdash; De expressie. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` absolute waarde, `√` / `sqrt` / `∛` / `cbrt`, implicit multiplication, en De constants `pi`, `tau`, `e`, `phi` are supported. Built-in calls cover De standard trig/log/exp/rounding family plus two-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates De expressie as `x = f(y)` en rotates De curve.
- `domain="a..b"` (x bounds shorthand) of comma-separated clauses such as `x>=0, x<5`.
- `color`, `label`. Any curve met Een non-empty label is automatisch listed in Een legend gerenderd just below De panel: Een small kleur swatch followed by De label, met entries flowing left-naar-right en wrapping onto Een new rij wanneer De next entry would niet fit.
- `tooltip` adds plain tekst below De computed tooltip. `showFunction` en `showValues` Standaard naar `true`; Stel in either naar `false` naar verbergen De gerenderd equation of live `(x, y)` waarde respectively. A `<Plot>` / nested `<Function>` met `expr` kan bevatten Markdown en GuideNH tags as Een rich tooltip body. De order is always label, gerenderd equation, live waarden, `tooltip` tekst, then rich kind inhoud; omitted of uitgeschakeld computed fields are skipped in that order.
- `pointEveryX="step"` adds generated punt markers at regular x intervals on that curve.
- `pointEveryY="step"` adds generated punt markers where De curve intersects regular y intervals, using Een bounded zoeken.
- `autoPointLabel="none|x|y|xy"` controls generated punt labels; Standaard is `none`.
- `autoPointColor="#..."` overrides De generated punt kleur; omitted betekent inherit De curve kleur.

Marked punten (`<Point>`):

- Explicit: `x="..."` en `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` of `atY="v"` (De runtime bisects on De plot's x-domain naar find De matching `x`).
- optioneel `color`, `label`.

Interaction: zweven Een curve naar markeren it; press en hold naar scrub Een punt along De curve. De tooltip starts met `label` wanneer supplied, then De equation en live `(x, y)` waarde unless their switches are uitgeschakeld; it stays anchored above De punt en flips below wanneer there is no headroom.

## BetterQuesting Compatibility tags

`<QuestLink>` en `<QuestCard>` are alleen registered wanneer De BetterQuesting mod is loaded. They are documented in detail on De [Mod Compatibility](Mod-Compatibility) pagina; De summary below covers De most algemene usage.

### `<QuestLink>`

Inline link naar Een BetterQuesting quest. Clicking opens De quest binnen De BetterQuesting GUI, unless De quest id is ook present in De huidige gids's `quest_ids` frontmatter — in that case De link navigates naar that pagina instead.

| Attribuut | Betekenis |
| --- | --- |
| `id` | Vereist BetterQuesting quest id; accepts canonical UUID strings en compact Base64 ids |
| `text` | optioneel override voor De displayed tekst |
| `show_tooltip` | optioneel boolean (Standaard `true`); Stel in naar `false` naar suppress De quest-description tooltip. `showTooltip` is accepted as Een alias |

zichtbaarheid behavior is decided per player at compile time:

- zichtbaar / completed quests renderen as Een clickable link (completed quests are tinted green en append a `✓` mark)
- locked but non-verborgen quests still renderen as clickable quest links so they kan open De BetterQuesting quest scherm of De indexed gids­pagina
- verborgen / secret quests renderen as Een darker italic placeholder using `guidenh.compat.bq.hidden`
- onbekend quest ids renderen as Een red placeholder using `guidenh.compat.bq.missing`

Example:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

blok-level summary card voor Een BetterQuesting quest. Renders De quest title met De zelfde status-aware styling as `<QuestLink>`, plus De quest description as Een body paragraph wanneer De quest is zichtbaar naar De player.

| Attribuut | Betekenis |
| --- | --- |
| `id` | Vereist BetterQuesting quest id; accepts canonical UUID strings en compact Base64 ids |
| `show_desc` | optioneel boolean (Standaard `true`); Stel in naar `false` naar suppress De description body |
| `show_tooltip` | optioneel boolean (Standaard `true`); Stel in naar `false` naar suppress De quest-description tooltip on De clickable title. `showTooltip` is accepted as Een alias |

De accent kleur of De card rand follows De quest status: green voor completed, gray voor locked / verborgen, red voor missing, en De standard link kleur voor zichtbaar quests. De title remains clickable voor zichtbaar, completed, en locked-but-non-verborgen quests.

Example:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
