# Tag-Referenz


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

dies Seite lists Die built-in Laufzeit Tags registriert by `DefaultExtensions`.

## Nutzungsregeln

- Tags kann appear entweder in Block context oder inline context depending auf Die Compiler.
- MDX comments using `{/* ... */}` sind unterstützt in Seite Inhalt und sind ignored by Die Laufzeit Parser.
- ungültig Tags oder ungültig Attribut rendern Leitfaden errors inline stattdessen von silently failing.
- Large feature Tags such als recipes und 3D Szenen sind documented in ihre eigene pages:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## Inline und Flow Tags

| Tag | Zweck | Schlüssel Attribut |
| --- | --- | --- |
| `<a>` | intern/extern Link und optional anchor Name | `href`, `title`, `name` |
| `<br>` | Linie break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style Text | none |
| `<sup>` | smaller inline superscript-style Text | none |
| `<Color>` | farbig inline Text | `id` oder `color` |
| `<Spoiler>` | verborgen inline Text revealed auf Hover | none |
| `<Tooltip>` | rich Hover Tooltip mit markdown/Tag untergeordnete Elemente | `label` |
| `<SoundLink>` | clickable rich-Text sound trigger | `sound` oder `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted Text; equivalent zu `==text==` mit optional Farbe control | `color` |
| `<PlayerName>` | inserts aktuell player username | none |
| `<KeyBind>` | inserts keybinding anzeigen Name | `id` oder `action` |
| `<ItemImage>` | inline Element icon | `id` oder `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | Element Tooltip + optional navigation Link | `id` oder `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command Link | `command`, `title`, `close` |
| `<Latex>` | LaTeX math Formel; inline in flow context, centered anzeigen Block in Block context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest Link mit Zustand-aware styling (compat Tag, nur registriert wenn BetterQuesting ist geladen) | `id`, `text`, `show_tooltip` |

Inline markdown auch unterstützt action Links für sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## Block-Tags

| Tag | Zweck | Schlüssel Attribut |
| --- | --- | --- |
| `<div>` | pass-durch Block wrapper | none |
| `<ContentTabs>` | groups alternative rich Inhalt under independent tabs | `title`, `color`, `icon`, `iconPng`, `icon_png`, `iconItem`, `icon_item`, `default`, `defaultIndex` |
| `<Tab>` | eins Inhalt panel innerhalb `<ContentTabs>` | `title` |
| `<details>` | collapsible Laufzeit Block | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | Verzeichnis-style outline mit connector Linien | `indent`, `gap` |
| `<Row>` | horizontal flex Layout | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertikal flex Layout | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | Breite-constrained footnote container verwendet by Laufzeit markdown footnotes | `width` |
| `<ItemGrid>` | compact grid von Element icons | untergeordnete Elemente muss sein `<ItemIcon id="..."/>` oder `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-interactive 3D einzeln-Block Vorschau | `id` oder `ore`, optional `scale` (Standards zu `4`), `float`, `perspective`, `nbt` |
| `<FloatingImage>` | cropped Bild Block mit Gleitkommazahl oder true inline Positionierung | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | navigation untergeordnetes Element listing | `id`, `alphabetical` |
| `<Category>` | Liste pages von Eine category | `name`, `rows` |
| `<Special>` | Liste built-in MediaWiki special pages | `name`, `rows` |
| `<Structure>` | 2.5D isometric Block Layout view | `width`, `height` |
| `<Mermaid>` | Laufzeit Mermaid Diagramm import/inline | `src`, `width`, `height` |
| `<CsvTable>` | Laufzeit CSV Datei import Tabelle | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered Spalte Diagramm | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar Diagramm | gleich als `<ColumnChart>` |
| `<LineChart>` | Linie Diagramm mit categorical oder numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie Diagramm | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter Diagramm | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-Kurve Funktion Diagramm | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | einzeln-Kurve shorthand für `<FunctionGraph>` | `expr`, plus alle `<FunctionGraph>` panel Attribut |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | Siehe [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D Leitfaden Szene | Siehe [GameScene](GameScene) |
| `<QuestCard>` | Block-level BetterQuesting quest summary card (compat Tag, nur registriert wenn BetterQuesting ist geladen) | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` ist ein `<GameScene>` untergeordnetes Element Tag. It accepts `controller`, `piece`, `facing`,
`rotation`, `flip`, und `channel` Attribut, und auch unterstützt StructureLib Standard untergeordnetes Element Tags:
`<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`,
`<GregTechActiveController>`, und `<GregTechPlaceHatches>`.

## Tag-Details

### `<a>`

Acts like Eine HTML-style anchor Tag:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` kann sein relativ, rooted, explizit `modid:path`, oder HTTP/HTTPS
- `title` wird zu Die tooltip
- `name` inserts Eine Seite anchor Ziel

### `<br>`

GuideNH auch unterstützt Eine MDX break Tag mit Gleitkommazahl clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` Werte:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, und `<sup>`

GuideNH Laufzeit unterstützt Eine focused subset von Kleinbuchstaben documentation Tags für inline verwenden:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

erstellt Eine collapsible Laufzeit Block mit Eine summary Zeile. Die `<summary>` Linie unterstützt normal
inline markdown/Tag Inhalt, und Die Inhalt kann hold ordinary Text plus beliebige Block-Tags such als
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, Tabellen, charts, und Layout containers.
wenn `height` ist Setzen Sie, nur Die Inhalt scrolls; Die summary Zeile und outer frame stay festen.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

Standardmäßig verborgener Text mit einem [normalen Seitenlink](./index.md).

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

Attribut:

- `open` — Starts expanded wenn present
- `width` — bevorzugt outer Breite in Pixel
- `height` — bevorzugt Inhalt Ansichtsbereich Höhe in Pixel; overflow wird zu scrollable in-game und in site export
- `wrap` — unterstützt Die usual Block embedding modes such als `square`, `tight`, und `through`
- `align` — `left`, `center`, oder `right`; wenn kombiniert mit Eine floating wrap Modus, Die whole details Block floats

### `<ContentTabs>`

Groups alternative rich Inhalt under independent tabs. nur direkt `<Tab>` untergeordnete Elemente sind gültig. Die container itself kann auch rendern Eine quote-style heading Zeile über Die tabs, passend Die visual Sprache von markdown callouts.

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

- `default` stimmt überein Die erste tab whose `title` stimmt überein genau
- `defaultIndex` ist zero-based und wins über `default` wenn beide sind present
- `color` optionally überschreibt Die Links accent Linie und ausgewählt-tab hervorheben mit `#RRGGBB` oder `#AARRGGBB`
- `title` adds Eine optional einfach-Text heading über Die tab strip
- `icon`, `iconPng` / `icon_png`, und `iconItem` / `icon_item` verwenden Die gleich heading icon semantics als markdown quote-style callouts
- ungültig untergeordnete Elemente oder ungültig Standards rendern sichtbar author-facing errors

### `<FileTree>`

rendert Eine Verzeichnis-style outline mit real connector Linien drawn von Die prefix glyphs auf jede Zeile. beide Unicode box-drawing (`│ ├ └ ─`) und ASCII (`| +-- \-- ` / four spaces) forms sind accepted und kann sein mixed. Payload Text unterstützt Die usual inline markdown (Links, **fett**, `code`, …), und those Links sind clickable beide in-game und in Die built-in site export. Die gleich Inhalt kann auch sein written als Eine fenced ` ```tree ` or ` ```filetree ` Block.

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

optional per-Zeile icons sind introduced by Eine leading directive auf Die payload:

- `{:icon=Text}` — short Text Beschriftung (einzeln oder double quotes optional)
- `{:iconPng=path/to/file.png}` — PNG Ressource aufgelöst against Die aktuell Seite
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft Element icon. Die optional `meta` segment ist ein damage Wert (oder `*` für Eine wildcard); Eine optional trailing `:{snbt}` Block carries SNBT zu attach zu Die stack.

````md
```filetree
Welt
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} Beispielressource
```
````

Attribut:

- `indent` — Pixel per depth level (Standard `14`)
- `gap` — extra Pixel zwischen Zeilen (Standard `0`)

### Laufzeit-Zitate

normal markdown blockquotes rendern at Laufzeit mit Eine Links accent Linie. GitHub alert Syntax ist unterstützt:

````md
> [!NOTE]
> Alert body
````

GuideNH auch unterstützt Eine Laufzeit-nur benutzerdefiniert directive auf Die erste quoted Linie:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

Unterstützte Direktiven-Schlüssel:

- `title`
- `color`
- `icon` für Klartext symbols
- `iconItem` für ein `ItemStack` id
- `iconPng` für Eine Leitfaden Ressource png Pfad

nur eins icon Quelle sollte sein bereitgestellt.

### `<Color>`

verwenden entweder Eine symbolic Farbe id oder Eine explizit hex Wert:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

Use `<Spoiler>` for inline hidden text. The content stays rich-text aware, so nested markdown and runtime inline tags
such as `<Color>` still render correctly after reveal.

```mdx
<Spoiler>ausgeblendet **fett** Text mit <Color color="#55ccff">tint</Color>.</Spoiler>
<Spoiler>[Anchor Link](#headings) weiterhin behaves like Eine normal hoverable Link wenn revealed.</Spoiler>
```

````

Regeln:

- `id` und `color` sind mutually exclusive in practice; provide eins
- `color` accepts `#RRGGBB`, `#AARRGGBB`, oder `transparent`

### `<Tooltip>`

erstellt underlined Text dass opens Eine rich Inhalt Tooltip auf Hover.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

Wenn `label` ist omitted, Die trigger Text Standards zu `tooltip`.

### `<SoundLink>` und Sound Action Links

`<SoundLink>` rendert rich inline Inhalt dass plays Eine sound wenn clicked. It tut nicht navigate,
und seine benutzerdefiniert Klick sound replaces Die normal Leitfaden Klick sound für dass Klick.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound Attribut:

- `sound` ist ein sound event id such als `modid:event.name`
- `src` Punkte at ein `.ogg` Datei; `modid:sounds/machine/start.ogg` wird zu `modid:machine.start`
- `volume` Standards zu `1.0`
- `pitch` Standards zu `1.0`
- `cooldown` ist milliseconds zwischen repeated plays, Standard `250`
- `radius` und `minVolume` control Bildschirm-space attenuation wenn verwendet in Szenen

### `<PlayerName>`

Inserts Die aktuell Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up Eine keybinding by id oder action und rendert Die player's aktuell bound Schlüssel Name.

Accepted ids:

- Die binding Beschreibung id, such als `key.jump` oder `key.guidenh.open_guide`
- Die legacy `category.description` form, such als `key.categories.movement.key.jump`

Beispiel:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX Comments

GuideNH ignores MDX comments in Seite Inhalt:

````md
Sichtbarer Text. {/* verborgener Inline-Kommentar */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH auch ignores explizit `<Comment>` Tags:

````md
Sichtbarer Text. <Comment>Dieser Inhalt wird nicht gerendert.</Comment> Bleibt sichtbar.
````

### `<ItemImage>`

zeigt Eine inline Element icon.

| Attribut | Bedeutung |
| --- | --- |
| `ore` | ore dictionary Name; Die erste übereinstimmen wins |
| `id` | Element reference verwendet wenn `ore` ist absent |
| `nbt` | optional SNBT Element Daten; merged onto beliebig inline SNBT in `id` |
| `scale` | Gleitkommazahl, Standard `1` |
| `noTooltip` | truthy Zeichenkette oder leer Attribut suppresses Tooltip (legacy; prefer `showTooltip`) |
| `showTooltip` | Boolesch, Standard `true`; `false` suppresses Die Hover Tooltip |
| `showIcon` | Boolesch, Standard `true`; `false` hides Die Element icon graphic |
| `label` | `left` oder `right` — zeigt Die Element anzeigen Name als Text auf Die specified Seite von Die icon; omit für nein Beschriftung |
| `format` | format pattern für Die Beschriftung Text; unterstützt Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) mit optional `%s` placeholder für Die Element Name; Standard (nein Attribut) rendert Die Name in kursiv |
| `yOffset` | Ganzzahl pixel offset Überschreibung für Die **icon** at scale `1`; tut nicht affect Die Beschriftung Text |
| `labelYOffset` | Ganzzahl pixel offset Überschreibung für Die **Beschriftung Text** at scale `1`; tut nicht affect Die icon |

Hinweise:

- `ore` takes precedence über `id` wenn beide sind bereitgestellt
- Wenn GregTech ist installed, Die ausgewählt ore übereinstimmen ist passed durch `GTOreDictUnificator.setStack(...)`
- `label` requires at least eins von `showIcon` oder `label` zu produce sichtbar Ausgabe; setting beide `showIcon="false"` und omitting `label` rendert nothing
- `format` nur wendet ein wenn `label` ist Setzen Sie; Wenn `format` hat nein `%s`, Die literal format Text ist verwendet als Die Beschriftung
- inline SNBT in `id` remains unterstützt; wenn beide forms sind present, Die standalone `nbt` Attribut ist merged letzte und überschreibt conflicting Schlüssel

Beispiel:

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

erstellt Eine Text Link using Die Element's anzeigen Name und Element tooltip. Wenn `item_ids` Punkte zu Eine Leitfaden Seite, Klicken navigates zu it. `ore` kann sein verwendet zu auflösen Die anzeigen stack von Die erste ore dictionary übereinstimmen stattdessen von Eine festen registry id.

| Attribut | Standard | Bedeutung |
| --- | --- | --- |
| `id` | — | Element registry id, e.g. `minecraft:compass` oder `minecraft:wool:1` |
| `ore` | — | ore-dictionary Name; verwendet Die erste passend Element stack |
| `linksTo` | *(auto)* | überschreibt Die Link Ziel; accepts Eine Seite id mit optional `#anchor`, e.g. `./crafting.md#usage` oder `#usage`; wenn omitted Die Ziel ist aufgelöst von `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | Setzen Sie zu `false` zu suppress Die Hover tooltip; `noTooltip` ist ein legacy alias |
| `showIcon` | *(none)* | `left` oder `right` (oder beliebig truthy Wert → rechts) — rendert Die Element icon beside Die Link Text; omit zu anzeigen Text nur |
| `scale` | `1.0` | anzeigen scale für Die optional Element icon; hat nein effect wenn `showIcon` ist omitted |

Beispiele:

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

Sends Eine chat command wenn clicked.

| Attribut | Bedeutung |
| --- | --- |
| `command` | erforderlich, muss Start mit `/` |
| `title` | optional Tooltip heading |
| `close` | geparst Boolesch Attribut; derzeit geparst but nicht verwendet zu close Die Leitfaden |

Beispiel:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` und `<Column>`

Flex-style containers für Block Inhalt.

| Attribut | Bedeutung |
| --- | --- |
| `gap` | Ganzzahl gap zwischen untergeordnete Elemente, Standard `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | Boolesch Ausdruck, Standard `false` |
| `width` | Ganzzahl bevorzugt Breite; useful für constraining Liste Linie Breite |

Beispiel:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

zu constrain Die Breite von normal markdown lists, wrap sie in Eine container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH verwendet dies Block Tag internally wenn Laufzeit markdown footnotes sind expanded. It kann auch sein written manuell Wenn needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

rendert Eine compact Element grid. untergeordnete Elemente muss sein roh `<ItemIcon>` elements, which sind geparst directly by Die grid Compiler. jede untergeordnetes Element kann verwenden entweder `id` oder `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

rendert Eine non-interactive 3D einzeln-Block Szene. Die Vorschau hat nein Szene Hintergrund, nein Szene
buttons, nein layer controls, und nein Annotation features, but Beim Überfahren Die Block weiterhin zeigt Die
Auswahl outline und tooltip. `ore` muss auflösen zu Eine Block Element stack.

| Attribut | Bedeutung |
| --- | --- |
| `id` | Block id; unterstützt Die normal `modid:block[:meta][:{snbt}]` Schreibweise |
| `ore` | ore dictionary lookup; Die erste passend Block Element wins |
| `scale` | Kamera Zoom Multiplikator, Standard `4` |
| `float` | legacy flow Gleitkommazahl Unterstützung: `left` oder `right` |
| `perspective` | `isometric-north-east` (Standard), `isometric-north-west`, oder `up` |
| `nbt` | optional SNBT tile-entity Daten merged onto beliebig inline SNBT von `id` |

Hinweise:

- inline SNBT innerhalb `id` ist weiterhin accepted für compatibility, but `nbt="..."` ist Die bevorzugt
  authoring form
- wenn beide inline SNBT und `nbt` sind present, Die `nbt` Attribut ist merged letzte und daher
  überschreibt conflicting Schlüssel
- GuideNH 1.7.10 tut nicht Unterstützung modern Block-Zustand Eigenschaft Syntax here, so GuideME-style
  `p:<state>` Attribut sind intentionally nicht unterstützt

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

Siehe [Images And Assets](Images-And-Assets) für Die vollständige Verhalten.

Kurzregeln:

- `src` unterstützt relativ paths, rooted paths, und explizit `modid:path` texture ids
- `x`, `y`, `width` / `w`, und `height` / `h` define Die crop rectangle auf Die original Bild und muss alle sein present
- `scaleX` und `scaleY` resize Die cropped Ergebnis und Unterstützung independent horizontal / vertikal stretching
- `displayWidth` und `displayHeight` Setzen Sie final dimensions in Pixel; eins Wert preserves Die crop aspect ratio, während zwei Werte erlauben stretching
- `displayWidth` / `displayHeight` kann nicht sein kombiniert mit `scaleX` / `scaleY`
- `wrap="inline"` places Die Bild truly inline innerhalb Text flow; in dass Modus `align` ist ignored
- old Inhalt dass verwendet `width` / `height` als final anzeigen Größe muss sein migrated manuell

### `<SubPages>`, `<Category>`, und `<Special>`

Siehe [Navigation](Navigation) für Vollständige navigation behavior.

### `<Structure>`

Siehe [Examples](Examples) und [GameScene](GameScene) wenn deciding whether zu verwenden Eine static structure Vorschau oder Eine Vollständige 3D Szene.

### `<Mermaid>`

verwendet für Laufzeit Mermaid Inhalt. aktuell Laufzeit Unterstützung ist focused auf `mindmap`, entweder inline oder durch Eine Seite-relativ `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
Laufzeitknoten können normale Blöcke einbetten.

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` und `height` constrain Die Laufzeit Ansichtsbereich box
- innerhalb Die Ansichtsbereich, ziehen pans und Die moVerwenden Sie wheel zooms
- quoted Mermaid labels kann verwenden rich inline markdown such als `**bold**` und Seite Links
- `<NodeContent id="...">...</NodeContent>` kann sein added als untergeordnete Elemente von `<Mermaid>` zu ersetzen Eine Knoten Inhalt mit beliebige Laufzeit Blöcke

### `<CsvTable>`

verwendet zu parsen Eine CSV Datei in Eine Laufzeit Tabelle:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves relativ zu Die aktuell Seite, Die gleich way Szene imports und normal Ressource Links do.

optional Attribut:

- `header`
  Standards zu `true`; Setzen Sie `header={false}` zu Beibehalten Die erste Zeile unfetted
- `widths`
  Comma-separated Ganzzahl Breite hints such als `widths="120,80"`

Beispiele:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

Die Verwandt fenced Laufzeit CSV form auch unterstützt passend metadata:

````md
```csv widths="120,80" header=false
Name,Wert
iron,42
gold,17
```
````

### `<Latex>`

rendert Eine LaTeX math Formel using jlatexmath. wenn verwendet inline (innerhalb Eine paragraph oder Text flow), it rendert als Eine scaled glyph dass expands Die Linie Höhe zu fit Die Formel. wenn written als seine eigene paragraph (Block context), it rendert centered als Eine anzeigen-Modus Formel.

| Attribut | Typ | Standard | Beschreibung |
| --- | --- | --- | --- |
| `formula` | Zeichenkette | *(erforderlich)* | LaTeX Quelle Zeichenkette |
| `color` | `#RRGGBB` oder `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | Gleitkommazahl | `1.0` | anzeigen Größe Multiplikator applied auf oben von Die automatisch Linie-Höhe scaling |
| `sourceScale` | Gleitkommazahl | `100.0` | jlatexmath intern rendern resolution; higher Werte improve quality at large sizes |
| `tooltip` | Zeichenkette | *(none)* | einfach Tooltip Text angezeigt auf Hover |
| `showTooltip` | Boolesch | `false` | anzeigen Die roh LaTeX Quelle als Eine Tooltip auf Hover |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-nur. vertikal alignment within Die Text Linie: `baseline` (Standard) aligns Die Formel's math baseline mit Die Text baseline; `top` aligns Die Formel oben mit Die Linie oben; `center` centers it auf Die Text; `bottom` aligns Die Formel unten mit Die Text unten |
| `offsetX` | int | `0` | horizontal pixel offset applied nach alignment (positive = rechts) |
| `offsetY` | int | `0` | vertikal pixel offset applied nach alignment (positive = down) |

Beispiele:

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

als Eine convenience you kann schreiben `$$formula$$` directly in Markdown ohne using Die `<Latex>` Tag.
alle Darstellung Parameter verwenden ihre Standards (white colour, scale 1.0, nein tooltip, baseline-aligned).

- **Inline**: `$$formula$$` embedded innerhalb Eine paragraph rendert als Eine inline Formel.
- **anzeigen**: Eine paragraph whose entire Inhalt ist `$$formula$$` (mit optional surrounding whitespace) rendert als Eine centred anzeigen-Modus Block.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

Hinweise:

- Die Formel Höhe ist calibrated zu Die aktuell Linie Text Höhe. Simple formulas rendern at Text Höhe; taller formulas (fractions, summations, integrals, etc.) expand Die enclosing Linie Höhe automatisch.
- `valign` nur wendet ein zu inline formulas. anzeigen-Modus (Block-level) formulas sind immer centered horizontal; verwenden `offsetY` zu shift sie vertically within Die Block.
- `color` Standards zu white (`#FFFFFF`). verwenden `#AARRGGBB` format für Eine semi-transparent fill.
- `sourceScale` nur affects rendern sharpness, nicht Die displayed Größe. Werte unter `16` sind clamped zu `16`.
- Tooltip priority ist: rich untergeordnetes Element Markdown Inhalt, dann `tooltip="..."`, dann `showTooltip={true}` roh Quelle Fallback.
- untergeordnetes Element Tooltip Inhalt ist kompiliert als regular Leitfaden Markdown, so it kann einschließen fett Text, lists, Links, Element Tags, und nested `<Latex>` formulas.
- Die `$$formula$$` shorthand immer verwendet Standard Parameter. verwenden Die `<Latex>` Tag für benutzerdefiniert colour, scale, alignment oder tooltip.

### Szene Laufzeit Tags

diese Tags nur funktionieren innerhalb `<GameScene>` / `<Scene>`:

| Tag | Zweck | Schlüssel Attribut |
| --- | --- | --- |
| `<ImportStructure>` | import Eine extern SNBT/NBT structure Ressource | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import Eine StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | entfernen bereits-placed Blöcke dass übereinstimmen Eine Block matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp Die gleich untergeordnetes Element Annotationen onto jede passend placed Block | `id` |

Siehe [GameScene](GameScene) für Szene import/removal behavior und [Annotations](Annotations) für Annotation template Regeln.


## Charts

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, und `<ScatterChart>` sind interactive Diagramm Blöcke. alle charts share Die following allgemein Attribut:

| Attribut | Beschreibung | Standard |
| --- | --- | --- |
| `title` | Diagramm Titel | none |
| `width` / `height` | explizit Größe | 320 / 200 |
| `background` / `border` | Hintergrund und Rand colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | Titel und Wert-Beschriftung colors | light grey |
| `legend` | Legende Position: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | Wert-Beschriftung Position: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | intern plot Legende Position: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum intern Legende box Größe | `120` / `64` |
| `cornerLegendBackground` | intern Legende Hintergrund Farbe | `#AA111922` |

Cartesian charts (Spalte / Bar / Linie / Scatter) additionally accept axis Attribut `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` und Die passend `yAxis*` Setzen Sie, plus `showXGrid={true}` / `showYGrid={true}` zu toggle gridlines.

untergeordnete Elemente:

* `<Series name="..." color="#..." data="10,20,30"/>` für category-based charts (Spalte / Bar / categorical Linie).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` für numeric X (Linie `numericX={true}`, Scatter).
* `<Slice name="..." value="..." color="#..."/>` für `<PieChart>` nur.

wenn `color` ist omitted auf ein `<Series>` oder `<Slice>`, GuideNH cycles durch Eine built-in 16-Farbe palette.

`<Series>` und `<Slice>` auch accept Die following optional icon / Tooltip Attribut:

* `icon="modid:item"` (gleich Syntax als `<ItemImage>`'s `id`, kann einschließen `@meta` und inline NBT JSON) — binds ein `ItemStack` zu Die Eintrag; Die Legende swatch wird zu Die Element icon und Beim Überfahren Die Daten Punkt zeigt Die vanilla Element Tooltip mit Die Diagramm Beschreibung appended at Die Ende.
* `iconImage="images/foo.png"` — verwenden Eine PNG Ressource als Die Legende swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form Text appended zu Die Tooltip (verwenden `\n` für multi-Linie).

Beispiel:

```mdx
<PieChart title="Output share">
  <Slice name="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice name="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra Attribut: `categories` (X-axis oder Y-axis labels, comma separated), `barWidthRatio` (Standard 0.7). `<BarChart>` puts Die categories auf Die Y-axis und Werte auf Die X-axis.

#### Combo extensions

`<ColumnChart>` und `<BarChart>` accept zwei extra untergeordnetes Element element Typs so mehrere Diagramm styles kann share eins plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn als Eine polyline overlay auf oben von Die bars. jede Linie Punkt sits at Die cluster Mitte von Die passend category index; Die overlay shares Die host Diagramm's Wert axis. You kann declare mehrere `<LineSeries>` zu overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — Eine small pie Diagramm drawn innerhalb eins von Die four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) von Die plot area. seine `<Slice>` untergeordnete Elemente share Die gleich Syntax als in `<PieChart>`.

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

Extra Attribut: `numericX={true}` zu enable Eine numeric X-axis (untergeordnete Elemente muss verwenden `points`); `showPoints={false}` hides Punkt markers. Die hovered Punkt ist pushed outward by 2px along Die Kurve normal, enlarged, und outlined; Die adjacent Linie segments thicken by 1px.

`<LineChart>` und `<ScatterChart>` kann anzeigen Eine compact Legende innerhalb Die plot area mit `cornerLegend="topRight"` oder another corner. Einträge verwenden existing Serie names und colors.

### `<PieChart>`

Extra Attribut: `startAngle` (Standard `-90`, i.e. 12 o'clock); `clockwise={false}` zu reverse direction. Die hovered Segment pops outward 4px along seine bisector.

### `<ScatterChart>`

rendert Punkte nur; `<Series>` muss verwenden `points`. Die X-axis ist immer numeric.

## Funktionsgraphen

`<FunctionGraph>` und Die einzeln-Kurve shorthand `<Function>` rendern Eine interactive Desmos-style panel. Die gleich panel ist auch verfügbar durch ein ` ```funcgraph ` fenced code Block; Siehe Die Laufzeit [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) für Eine Vollständige walkthrough.

Panel Attribut (accepted by Die container, Die shorthand, und Die fence header alike):

- `width` / `height` (Standards `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (Standard `true`)
- `xRange="a..b"` (oder `xMin` / `xMax` separately), `xStep` für tick spacing; gleich für Die Y axis
- `xLabel` / `yLabel` Fügen Sie hinzu Excel-style axis titles unter und über Die plot respectively. Sie Unterstützung inline `$$...$$` LaTeX; `domain="a..b"` ist ein legacy alias für `xRange` wenn nein explizit X range ist present
- `quadrants="1,2,3,4"` oder `quadrants="all"` zu force Die sichtbar quadrants; omit zu Start in quadrant 1 mit auto-expansion wenn sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, und `cornerLegendBackground` anzeigen Eine compact Legende innerhalb Die plot area using non-leer Kurve labels

Kurve untergeordnete Elemente (`<Plot>` / `<Function>`):

- `expr="..."` &mdash; Die Ausdruck. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` absolut Wert, `√` / `sqrt` / `∛` / `cbrt`, implizit multiplication, und Die constants `pi`, `tau`, `e`, `phi` sind unterstützt. Built-in calls cover Die Standard trig/log/exp/rounding family plus zwei-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates Die Ausdruck als `x = f(y)` und rotates Die Kurve.
- `domain="a..b"` (x bounds shorthand) oder comma-separated Klauseln such als `x>=0, x<5`.
- `color`, `label`. beliebig Kurve mit Eine non-leer Beschriftung ist automatisch listed in Eine Legende gerendert just unter Die panel: Eine small Farbe swatch followed by Die Beschriftung, mit Einträge flowing Links-zu-rechts und wrapping onto Eine new Zeile wenn Die nächste Eintrag would nicht fit.
- `tooltip` adds Klartext unter Die computed tooltip. `showFunction` und `showValues` Standard zu `true`; Setzen Sie entweder zu `false` zu ausblenden Die gerendert Gleichung oder live `(x, y)` Wert respectively. ein `<Plot>` / nested `<Function>` mit `expr` kann enthalten Markdown und GuideNH Tags als Eine Rich-Tooltip Inhalt. Die Reihenfolge ist immer Beschriftung, gerendert Gleichung, live Werte, `tooltip` Text, dann rich untergeordnetes Element Inhalt; omitted oder deaktiviert computed Felder sind skipped in dass Reihenfolge.
- `pointEveryX="step"` adds generiert Punkt markers at regular x intervals auf dass Kurve.
- `pointEveryY="step"` adds generiert Punkt markers where Die Kurve intersects regular y intervals, using Eine bounded Suche.
- `autoPointLabel="none|x|y|xy"` controls generiert Punkt labels; Standard ist `none`.
- `autoPointColor="#..."` überschreibt Die generiert Punkt Farbe; omitted bedeutet inherit Die Kurve Farbe.

Marked Punkte (`<Point>`):

- explizit: `x="..."` und `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` oder `atY="v"` (Die Laufzeit bisects auf Die plot's x-domain zu find Die passend `x`).
- optional `color`, `label`.

Interaction: Hover Eine Kurve zu hervorheben it; press und hold zu scrub Eine Punkt along Die Kurve. Die Tooltip Starts mit `label` wenn supplied, dann Die Gleichung und live `(x, y)` Wert außer ihre switches sind deaktiviert; it stays anchored über Die Punkt und flips unter wenn there ist nein headroom.

## BetterQuesting-Kompatibilitätstags

`<QuestLink>` und `<QuestCard>` sind nur registriert wenn Die BetterQuesting mod ist geladen. Sie sind documented in detail auf Die [Mod Compatibility](Mod-Compatibility) Seite; Die summary unter covers Die most allgemein usage.

### `<QuestLink>`

Inline Link zu Eine BetterQuesting quest. Klicken opens Die quest innerhalb Die BetterQuesting GUI, außer Die quest id ist auch present in Die aktuell Leitfaden's `quest_ids` frontmatter — in dass case Die Link navigates zu dass Seite stattdessen.

| Attribut | Bedeutung |
| --- | --- |
| `id` | erforderlich BetterQuesting quest id; accepts canonical UUID strings und compact Base64 ids |
| `text` | optional Überschreibung für Die displayed Text |
| `show_tooltip` | optional Boolesch (Standard `true`); Setzen Sie zu `false` zu suppress Die quest-Beschreibung tooltip. `showTooltip` ist accepted als Eine alias |

Sichtbarkeit behavior ist decided per player at kompilieren time:

- sichtbar / completed quests rendern als Eine clickable Link (completed quests sind tinted green und append ein `✓` mark)
- locked but non-ausgeblendet quests weiterhin rendern als clickable quest Links so Sie kann open Die BetterQuesting quest Bildschirm oder Die indexed Leitfaden Seite
- verborgen / secret quests rendern als Eine darker kursiv placeholder using `guidenh.compat.bq.hidden`
- unbekannt quest ids rendern als Eine red placeholder using `guidenh.compat.bq.missing`

Beispiel:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

Block-level summary card für Eine BetterQuesting quest. rendert Die quest Titel mit Die gleich Zustand-aware styling als `<QuestLink>`, plus Die quest Beschreibung als Eine Inhalt paragraph wenn Die quest ist sichtbar zu Die player.

| Attribut | Bedeutung |
| --- | --- |
| `id` | erforderlich BetterQuesting quest id; accepts canonical UUID strings und compact Base64 ids |
| `show_desc` | optional Boolesch (Standard `true`); Setzen Sie zu `false` zu suppress Die Beschreibung Inhalt |
| `show_tooltip` | optional Boolesch (Standard `true`); Setzen Sie zu `false` zu suppress Die quest-Beschreibung Tooltip auf Die clickable Titel. `showTooltip` ist accepted als Eine alias |

Die accent Farbe von Die card Rand follows Die quest Zustand: green für completed, gray für locked / ausgeblendet, red für fehlend, und Die Standard Link Farbe für sichtbar quests. Die Titel remains clickable für sichtbar, completed, und locked-but-non-ausgeblendet quests.

Beispiel:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
