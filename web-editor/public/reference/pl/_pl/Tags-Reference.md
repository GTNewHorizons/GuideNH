# Opis tagów

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


Ten strona lists  built-in czas wykonania tagi zarejestrowany by `DefaultExtensions`.

## Zasady użycia

- tagi może appear either in blok context lub inline context depending on  kompilator.
- MDX comments używając `{/* ... */}` są Obsługiwane in strona treść i są ignored by  czas wykonania parser.
- nieprawidłowy tagi lub nieprawidłowy atrybuty renderować przewodnik errors inline zamiast tego of silently failing.
- Large feature tagi takie jak Receptury i 3D sceny są documented in ich własny strony:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## Tagi inline i przepływu

| tag | Purpose | klucz atrybuty |
| --- | --- | --- |
| `<a>` | wewnętrzny/zewnętrzny odnośnik i opcjonalny kotwica nazwa | `href`, `title`, `name` |
| `<br>` | wiersz break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style tekst | none |
| `<sup>` | smaller inline superscript-style tekst | none |
| `<Color>` | colored inline tekst | `id` lub `color` |
| `<Spoiler>` | ukryty inline tekst revealed on najechanie | none |
| `<Tooltip>` | bogata najechanie podpowiedź z Markdown/tag elementy podrzędne | `label` |
| `<SoundLink>` | clickable bogata-tekst sound trigger | `sound` lub `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted tekst; equivalent do `==text==` z opcjonalny kolor control | `color` |
| `<PlayerName>` | inserts bieżący gracz username | none |
| `<KeyBind>` | inserts keybinding wyświetlać nazwa | `id` lub `action` |
| `<ItemImage>` | inline element icon | `id` lub `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | element podpowiedź + opcjonalny Nawigacja odnośnik | `id` lub `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command odnośnik | `command`, `title`, `close` |
| `<Latex>` | LaTeX math formula; inline in flow context, centered wyświetlać blok in blok context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest odnośnik z stan-aware styling (compat tag, tylko zarejestrowany gdy BetterQuesting is załadowany) | `id`, `text`, `show_tooltip` |

Inline Markdown także obsługuje action links dla sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## Tagi blokowe

| tag | Purpose | klucz atrybuty |
| --- | --- | --- |
| `<div>` | pass-through blok wrapper | none |
| `<ContentTabs>` | groups alternative bogata treść under independent tabs | `title`, `color`, `icon`, `iconPng`, `icon_png`, `iconItem`, `icon_item`, `default`, `defaultIndex` |
| `<Tab>` | jeden treść panel wewnątrz `<ContentTabs>` | `title` |
| `<details>` | collapsible czas wykonania blok | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | katalog-style outline z łącznik wiersze | `indent`, `gap` |
| `<Row>` | horizontal flex układ | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertical flex układ | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | szerokość-constrained footnote container używany by czas wykonania Markdown footnotes | `width` |
| `<ItemGrid>` | compact grid of element icons | elementy podrzędne musi be `<ItemIcon id="..."/>` lub `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-interaktywny 3D pojedynczy-blok podgląd | `id` lub `ore`, opcjonalny `scale` (domyślne do `4`), `float`, `perspective`, `nbt` |
| `<FloatingImage>` | cropped obraz blok z liczba zmiennoprzecinkowa lub true inline placement | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | Nawigacja element podrzędny listing | `id`, `alphabetical` |
| `<Category>` | lista strony z category | `name`, `rows` |
| `<Special>` | lista built-in MediaWiki special strony | `name`, `rows` |
| `<Structure>` | 2.5D isometric blok układ widok | `width`, `height` |
| `<Mermaid>` | czas wykonania Mermaid wykres import/inline | `src`, `width`, `height` |
| `<CsvTable>` | czas wykonania CSV plik import tabela | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered kolumna wykres | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar wykres | ten sam as `<ColumnChart>` |
| `<LineChart>` | wiersz wykres z categorical lub numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie wykres | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter wykres | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-curve function wykres | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | pojedynczy-curve shorthand dla `<FunctionGraph>` | `expr`, plus wszystkie `<FunctionGraph>` panel atrybuty |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | Zobacz [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D przewodnik scena | Zobacz [GameScene](GameScene) |
| `<QuestCard>` | blok-level BetterQuesting quest summary card (compat tag, tylko zarejestrowany gdy BetterQuesting is załadowany) | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` is a `<GameScene>` element podrzędny tag. It accepts `controller`, `piece`, `facing`,
`rotation`, `flip`, i `channel` atrybuty, i także obsługuje StructureLib Domyślne element podrzędny tagi:
`<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`,
`<GregTechActiveController>`, i `<GregTechPlaceHatches>`.

## Szczegóły tagów

### `<a>`

Acts like HTML-style kotwica tag:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` może be względny, rooted, jawny `modid:path`, lub HTTP/HTTPS
- `title` staje się  podpowiedź
- `name` inserts strona kotwica cel

### `<br>`

GuideNH także obsługuje MDX break tag z liczba zmiennoprzecinkowa clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` wartości:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, i `<sup>`

GuideNH czas wykonania obsługuje focused subset of lowercase documentation tagi dla inline używać:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

Creates collapsible czas wykonania blok z summary wiersz.  `<summary>` wiersz obsługuje normal
inline Markdown/tag treść, i  body może hold ordinary tekst plus arbitrary Tagi blokowe takie jak
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, tabele, Wykresy, i układ containers.
gdy `height` is Ustaw, tylko  body scrolls;  summary wiersz i outer frame pozostają stały.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

Domyślnie ukryty tekst z [normalnym odnośnikiem do strony](./index.md).

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

atrybuty:

- `open` — starts expanded gdy present
- `width` — preferowany outer szerokość in piksele
- `height` — preferowany body viewport wysokość in piksele; overflow staje się scrollable in-game i in site export
- `wrap` — obsługuje  usual blok embedding modes takie jak `square`, `tight`, i `through`
- `align` — `left`, `center`, lub `right`; gdy combined z floating zawijanie tryb,  whole details blok floats

### `<ContentTabs>`

Groups alternative bogata treść under independent tabs. tylko bezpośredni `<Tab>` elementy podrzędne są prawidłowy.  container itself może także renderować quote-style heading wiersz nad  tabs, pasujący  visual język of Markdown callouts.

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

- `default` pasuje  pierwszy tab whose `title` pasuje exactly
- `defaultIndex` is zero-based i wins over `default` gdy both są present
- `color` optionally overrides  lewo accent wiersz i wybrany-tab wyróżniać z `#RRGGBB` lub `#AARRGGBB`
- `title` dodaje opcjonalny plain-tekst heading nad  tab strip
- `icon`, `iconPng` / `icon_png`, i `iconItem` / `icon_item` używać  ten sam heading icon semantics as Markdown quote-style callouts
- nieprawidłowy elementy podrzędne lub nieprawidłowy domyślne renderować widoczny autor-facing errors

### `<FileTree>`

Renders katalog-style outline z real łącznik wiersze drawn z  prefix glyphs on każdy wiersz. Both Unicode box-drawing (`│ ├ └ ─`) i ASCII (`| +-- \-- ` / four spaces) forms są accepted i może be mixed. Payload tekst obsługuje  usual inline Markdown (links, **bold**, `code`, …), i those links są clickable both in-game i in  built-in site export.  ten sam treść może także be zapisany as fenced ` ```tree ` or ` ```filetree ` blok.

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

opcjonalny per-wiersz icons są introduced by leading directive on  payload:

- `{:icon=Text}` — short tekst etykieta (pojedynczy lub double quotes opcjonalny)
- `{:iconPng=path/to/file.png}` — PNG zasób rozwiązany against  bieżący strona
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft element icon.  opcjonalny `meta` segment is damage wartość (lub `*` dla wildcard); opcjonalny trailing `:{snbt}` blok carries SNBT do attach do  stos.

````md
```filetree
świat
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} sample zasób
```
````

atrybuty:

- `indent` — piksele per depth level (Domyślne `14`)
- `gap` — extra piksele between wiersze (Domyślne `0`)

### Cytaty czasu wykonania

Normal Markdown blockquotes renderować at czas wykonania z lewo accent wiersz. GitHub alert składnia is Obsługiwane:

````md
> [!NOTE]
> Alert body
````

GuideNH także obsługuje czas wykonania-tylko niestandardowy directive on  pierwszy quoted wiersz:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

Obsługiwane directive klucze:

- `title`
- `color`
- `icon` dla plain tekst symbols
- `iconItem` dla an `ItemStack` id
- `iconPng` dla przewodnik zasób png ścieżka

tylko jeden icon source powinien be provided.

### `<Color>`

używać either symbolic kolor id lub jawny hex wartość:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

Use `<Spoiler>` for inline hidden text. The content stays rich-text aware, so nested markdown and runtime inline tags
such as `<Color>` still render correctly after reveal.

```mdx
<Spoiler>ukryty **bold** tekst z <Color color="#55ccff">tint</Color>.</Spoiler>
<Spoiler>[Anchor Link](#headings) nadal behaves like normal hoverable odnośnik gdy revealed.</Spoiler>
```

````

Zasady:

- `id` i `color` są mutually exclusive in practice; zapewniać jeden
- `color` accepts `#RRGGBB`, `#AARRGGBB`, lub `transparent`

### `<Tooltip>`

Creates underlined tekst który opens bogata treść podpowiedź on najechanie.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

Jeśli `label` is pominięty,  trigger tekst domyślne do `tooltip`.

### `<SoundLink>` i Sound Action Links

`<SoundLink>` renders bogata inline treść który plays sound gdy clicked. It nie navigate,
i jego niestandardowy kliknięcie sound replaces  normal przewodnik kliknięcie sound dla który kliknięcie.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound atrybuty:

- `sound` is sound event id takie jak `modid:event.name`
- `src` punkty at an `.ogg` plik; `modid:sounds/machine/start.ogg` staje się `modid:machine.start`
- `volume` domyślne do `1.0`
- `pitch` domyślne do `1.0`
- `cooldown` is milliseconds between repeated plays, Domyślne `250`
- `radius` i `minVolume` control przestrzeń ekranu attenuation gdy używany in sceny

### `<PlayerName>`

Inserts  bieżący Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up keybinding by id lub action i renders  gracz's bieżący bound klucz nazwa.

Accepted ids:

-  binding opis id, takie jak `key.jump` lub `key.guidenh.open_guide`
-  legacy `category.description` form, takie jak `key.categories.movement.key.jump`

Przykład:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX Comments

GuideNH ignores MDX comments in strona treść:

````md
Widoczny tekst. {/* ukryty komentarz wbudowany */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH także ignores jawny `<Comment>` tagi:

````md
Widoczny tekst. <Comment>To nie jest renderowane.</Comment> Nadal widoczny.
````

### `<ItemImage>`

Shows inline element icon.

| Atrybut | Znaczenie |
| --- | --- |
| `ore` | ore dictionary nazwa;  pierwszy pasować wins |
| `id` | element reference używany gdy `ore` is absent |
| `nbt` | opcjonalny SNBT element dane; merged onto any inline SNBT in `id` |
| `scale` | liczba zmiennoprzecinkowa, Domyślne `1` |
| `noTooltip` | truthy ciąg znaków lub empty Atrybut suppresses podpowiedź (legacy; prefer `showTooltip`) |
| `showTooltip` | wartość logiczna, Domyślne `true`; `false` suppresses  najechanie podpowiedź |
| `showIcon` | wartość logiczna, Domyślne `true`; `false` hides  element icon graphic |
| `label` | `left` lub `right` — shows  element wyświetlać nazwa as tekst on  specified strona of  icon; omit dla no etykieta |
| `format` | format pattern dla  etykieta tekst; obsługuje Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) z opcjonalny `%s` placeholder dla  element nazwa; Domyślne (no Atrybut) renders  nazwa in italic |
| `yOffset` | liczba całkowita piksel offset nadpisywać dla  **icon** at scale `1`; nie affect  etykieta tekst |
| `labelYOffset` | liczba całkowita piksel offset nadpisywać dla  **etykieta tekst** at scale `1`; nie affect  icon |

Uwagi:

- `ore` takes precedence over `id` gdy both są provided
- Jeśli GregTech is installed,  wybrany ore pasować is passed through `GTOreDictUnificator.setStack(...)`
- `label` requires at least jeden of `showIcon` lub `label` do produce widoczny wyjście; setting both `showIcon="false"` i omitting `label` renders nothing
- `format` tylko stosuje gdy `label` is Ustaw; Jeśli `format` ma no `%s`,  literal format tekst is używany as  etykieta
- inline SNBT in `id` remains Obsługiwane; gdy both forms są present,  standalone `nbt` Atrybut is merged ostatni i overrides conflicting klucze

Przykład:

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

Creates tekst odnośnik używając  element's wyświetlać nazwa i element podpowiedź. Jeśli `item_ids` punkty do strona przewodnika, kliknięcie navigates do it. `ore` może be używany do rozwiązać  wyświetlać stos z  pierwszy ore dictionary pasować zamiast tego of stały registry id.

| Atrybut | Domyślne | Znaczenie |
| --- | --- | --- |
| `id` | — | element registry id, e.g. `minecraft:compass` lub `minecraft:wool:1` |
| `ore` | — | ore-dictionary nazwa; używa  pierwszy pasujący element stos |
| `linksTo` | *(auto)* | overrides  odnośnik cel; accepts ID strony z opcjonalny `#anchor`, e.g. `./crafting.md#usage` lub `#usage`; gdy pominięty  cel is rozwiązany z `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | Ustaw do `false` do suppress  najechanie podpowiedź; `noTooltip` is legacy alias |
| `showIcon` | *(none)* | `left` lub `right` (lub any truthy wartość → prawo) — renders  element icon beside  odnośnik tekst; omit do pokazywać tekst tylko |
| `scale` | `1.0` | wyświetlać scale dla  opcjonalny element icon; ma no effect gdy `showIcon` is pominięty |

Przykłady:

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

Sends chat command gdy clicked.

| Atrybut | Znaczenie |
| --- | --- |
| `command` | Wymagane, musi początek z `/` |
| `title` | opcjonalny podpowiedź heading |
| `close` | przeanalizowany wartość logiczna Atrybut; obecnie przeanalizowany but nie używany do close  przewodnik |

Przykład:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` i `<Column>`

Flex-style containers dla blok treść.

| Atrybut | Znaczenie |
| --- | --- |
| `gap` | liczba całkowita gap between elementy podrzędne, Domyślne `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | wartość logiczna wyrażenie, Domyślne `false` |
| `width` | liczba całkowita preferowany szerokość; useful dla constraining lista wiersz szerokość |

Przykład:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

do constrain  szerokość of normal Markdown lists, zawijanie ich in container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH używa Ten blok tag internally gdy czas wykonania Markdown footnotes są expanded. It może także be zapisany ręcznie Jeśli needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

Renders compact element grid. elementy podrzędne musi be raw `<ItemIcon>` elements, który są przeanalizowany directly by  grid kompilator. każdy element podrzędny może używać either `id` lub `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

Renders non-interaktywny 3D pojedynczy-blok scena.  podgląd ma no scena tło, no scena
buttons, no layer controls, i no adnotacja features, but najechanie  blok nadal shows 
wybór outline i podpowiedź. `ore` musi rozwiązać do blok element stos.

| Atrybut | Znaczenie |
| --- | --- |
| `id` | blok id; obsługuje  normal `modid:block[:meta][:{snbt}]` spelling |
| `ore` | ore dictionary lookup;  pierwszy pasujący blok element wins |
| `scale` | kamera powiększenie multiplier, Domyślne `4` |
| `float` | legacy flow liczba zmiennoprzecinkowa obsługa: `left` lub `right` |
| `perspective` | `isometric-north-east` (Domyślne), `isometric-north-west`, lub `up` |
| `nbt` | opcjonalny SNBT blok-byt dane merged onto any inline SNBT z `id` |

Uwagi:

- inline SNBT wewnątrz `id` is nadal accepted dla compatibility, but `nbt="..."` is  preferowany
  authoring form
- gdy both inline SNBT i `nbt` są present,  `nbt` Atrybut is merged ostatni i dlatego
  overrides conflicting klucze
- GuideNH 1.7.10 nie obsługa modern blok-stan właściwość składnia here, so GuideME-style
  `p:<state>` atrybuty są intentionally nie Obsługiwane

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

Zobacz [Images And Assets](Images-And-Assets) dla  full zachowanie.

Quick Zasady:

- `src` obsługuje względny paths, rooted paths, i jawny `modid:path` texture ids
- `x`, `y`, `width` / `w`, i `height` / `h` define  crop rectangle on  original obraz i musi wszystkie be present
- `scaleX` i `scaleY` resize  cropped wynik i obsługa independent horizontal / vertical stretching
- `displayWidth` i `displayHeight` Ustaw końcowy dimensions in piksele; jeden wartość preserves  crop aspect ratio, podczas dwa wartości zezwalać stretching
- `displayWidth` / `displayHeight` nie może be combined z `scaleX` / `scaleY`
- `wrap="inline"` places  obraz truly inline wewnątrz tekst flow; in który tryb `align` is ignored
- old treść który używany `width` / `height` as końcowy wyświetlać rozmiar musi be migrated ręcznie

### `<SubPages>`, `<Category>`, i `<Special>`

Zobacz [Navigation](Navigation) dla full Nawigacja zachowanie.

### `<Structure>`

Zobacz [Examples](Examples) i [GameScene](GameScene) gdy deciding whether do używać static struktura podgląd lub full 3D scena.

### `<Mermaid>`

używany dla czas wykonania Mermaid treść. bieżący czas wykonania obsługa is focused on `mindmap`, either inline lub through strona-względny `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
Węzły czasu wykonywania mogą osadzać zwykłe bloki.

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` i `height` constrain  czas wykonania viewport box
- wewnątrz  viewport, przeciąganie pans i  mouse wheel zooms
- quoted Mermaid labels może używać bogata inline Markdown takie jak `**bold**` i strona links
- `<NodeContent id="...">...</NodeContent>` może be added as elementy podrzędne of `<Mermaid>` do zastąpić node body z arbitrary czas wykonania bloki

### `<CsvTable>`

używany do analizować CSV plik do czas wykonania tabela:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves względny do  bieżący strona,  ten sam way scena importy i normal zasób links do.

opcjonalny atrybuty:

- `header`
  domyślne do `true`; Ustaw `header={false}` do Zachowaj  pierwszy wiersz unbolded
- `widths`
  Comma-separated liczba całkowita szerokość hints takie jak `widths="120,80"`

Przykłady:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

 related fenced czas wykonania CSV form także obsługuje pasujący metadane:

````md
```csv widths="120,80" header=false
nazwa,wartość
iron,42
gold,17
```
````

### `<Latex>`

Renders LaTeX math formula używając jlatexmath. gdy używany inline (wewnątrz paragraph lub tekst flow), it renders as scaled glyph który expands  wiersz wysokość do fit  formula. gdy zapisany as jego własny paragraph (blok context), it renders centered as wyświetlać-tryb formula.

| Atrybut | Typ | Domyślne | opis |
| --- | --- | --- | --- |
| `formula` | ciąg znaków | *(Wymagane)* | LaTeX source ciąg znaków |
| `color` | `#RRGGBB` lub `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | liczba zmiennoprzecinkowa | `1.0` | wyświetlać rozmiar multiplier applied on góra of  automatyczny wiersz-wysokość scaling |
| `sourceScale` | liczba zmiennoprzecinkowa | `100.0` | jlatexmath wewnętrzny renderować resolution; higher wartości improve quality at large sizes |
| `tooltip` | ciąg znaków | *(none)* | Plain podpowiedź tekst wyświetlany on najechanie |
| `showTooltip` | wartość logiczna | `false` | pokazywać  raw LaTeX source as podpowiedź on najechanie |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-tylko. Vertical alignment wewnątrz  tekst wiersz: `baseline` (Domyślne) aligns  formula's math baseline z  tekst baseline; `top` aligns  formula góra z  wiersz góra; `center` centers it on  tekst; `bottom` aligns  formula dół z  tekst dół |
| `offsetX` | int | `0` | Horizontal piksel offset applied po alignment (positive = prawo) |
| `offsetY` | int | `0` | Vertical piksel offset applied po alignment (positive = down) |

Przykłady:

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

As convenience you może zapisywać `$$formula$$` directly in Markdown bez używając  `<Latex>` tag.
wszystkie renderowanie parametry używać ich domyślne (white colour, scale 1.0, no podpowiedź, baseline-aligned).

- **Inline**: `$$formula$$` embedded wewnątrz paragraph renders as inline formula.
- **wyświetlać**: paragraph whose entire treść is `$$formula$$` (z opcjonalny surrounding whitespace) renders as centred wyświetlać-tryb blok.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

Uwagi:

-  formula wysokość is calibrated do  bieżący wiersz tekst wysokość. prosty formulas renderować at tekst wysokość; taller formulas (fractions, summations, integrals, etc.) expand  enclosing wiersz wysokość automatycznie.
- `valign` tylko stosuje do inline formulas. wyświetlać-tryb (blok-level) formulas są zawsze centered horizontally; używać `offsetY` do shift ich vertically wewnątrz  blok.
- `color` domyślne do white (`#FFFFFF`). używać `#AARRGGBB` format dla semi-przezroczysty fill.
- `sourceScale` tylko affects renderować sharpness, nie  displayed rozmiar. wartości below `16` są clamped do `16`.
- podpowiedź priority is: bogata element podrzędny Markdown treść, następnie `tooltip="..."`, następnie `showTooltip={true}` raw source awaryjny.
- element podrzędny podpowiedź treść is skompilowany as regular przewodnik Markdown, so it może uwzględniać bold tekst, lists, links, element tagi, i nested `<Latex>` formulas.
-  `$$formula$$` shorthand zawsze używa Domyślne parametry. używać  `<Latex>` tag dla niestandardowy colour, scale, alignment lub podpowiedź.

### scena czas wykonania tagi

Te tagi tylko działa wewnątrz `<GameScene>` / `<Scene>`:

| tag | Purpose | klucz atrybuty |
| --- | --- | --- |
| `<ImportStructure>` | import zewnętrzny SNBT/NBT struktura zasób | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | usuwać już-umieszczony bloki który pasować blok matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp  ten sam element podrzędny Adnotacje onto każdy pasujący umieszczony blok | `id` |

Zobacz [GameScene](GameScene) dla scena import/removal zachowanie i [Annotations](Annotations) dla adnotacja template Zasady.


## Wykresy

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, i `<ScatterChart>` są interaktywny wykres bloki. wszystkie Wykresy share  following wspólny atrybuty:

| Atrybut | opis | Domyślne |
| --- | --- | --- |
| `title` | wykres tytuł | none |
| `width` / `height` | jawny rozmiar | 320 / 200 |
| `background` / `border` | tło i obramowanie colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | tytuł i wartość-etykieta colors | light grey |
| `legend` | legenda pozycja: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | wartość-etykieta pozycja: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | wewnętrzny plot legenda pozycja: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum wewnętrzny legenda box rozmiar | `120` / `64` |
| `cornerLegendBackground` | wewnętrzny legenda tło kolor | `#AA111922` |

Cartesian Wykresy (kolumna / Bar / wiersz / Scatter) additionally akceptują oś atrybuty `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` i  pasujący `yAxis*` Ustaw, plus `showXGrid={true}` / `showYGrid={true}` do toggle gridlines.

elementy podrzędne:

* `<Series name="..." color="#..." data="10,20,30"/>` dla category-based Wykresy (kolumna / Bar / categorical wiersz).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` dla numeric X (wiersz `numericX={true}`, Scatter).
* `<Slice name="..." value="..." color="#..."/>` dla `<PieChart>` tylko.

gdy `color` is pominięty on a `<Series>` lub `<Slice>`, GuideNH cycles through built-in 16-kolor palette.

`<Series>` i `<Slice>` także akceptują  following opcjonalny icon / podpowiedź atrybuty:

* `icon="modid:item"` (ten sam składnia as `<ItemImage>`'s `id`, może uwzględniać `@meta` i inline NBT JSON) — binds an `ItemStack` do  wpis;  legenda swatch staje się  element icon i najechanie  dane punkt shows  vanilla element podpowiedź z  wykres opis appended at  koniec.
* `iconImage="images/foo.png"` — używać PNG zasób as  legenda swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form tekst appended do  podpowiedź (używać `\n` dla multi-wiersz).

Przykład:

```mdx
<PieChart title="Output share">
  <Slice name="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice name="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra atrybuty: `categories` (X-oś lub Y-oś labels, comma separated), `barWidthRatio` (Domyślne 0.7). `<BarChart>` puts  categories on  Y-oś i wartości on  X-oś.

#### Combo extensions

`<ColumnChart>` i `<BarChart>` akceptują dwa extra element podrzędny element types so wiele wykres styles może share jeden plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn as polilinia overlay on góra of  bars. każdy wiersz punkt sits at  cluster środek of  pasujący category index;  overlay shares  host wykres's wartość oś. You może declare wiele `<LineSeries>` do overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — small pie wykres drawn wewnątrz jeden of  four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) of  plot area. jego `<Slice>` elementy podrzędne share  ten sam składnia as in `<PieChart>`.

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

Extra atrybuty: `numericX={true}` do enable numeric X-oś (elementy podrzędne musi używać `points`); `showPoints={false}` hides punkt znaczniki.  hovered punkt is pushed outward by 2px wzdłuż  curve normal, enlarged, i outlined;  adjacent wiersz segments thicken by 1px.

`<LineChart>` i `<ScatterChart>` może pokazywać compact legenda wewnątrz  plot area z `cornerLegend="topRight"` lub another corner. wpisy używać existing seria names i colors.

### `<PieChart>`

Extra atrybuty: `startAngle` (Domyślne `-90`, i.e. 12 o'clock); `clockwise={false}` do reverse direction.  hovered slice pops outward 4px wzdłuż jego bisector.

### `<ScatterChart>`

Renders punkty tylko; `<Series>` musi używać `points`.  X-oś is zawsze numeric.

## Wykresy funkcji

`<FunctionGraph>` i  pojedynczy-curve shorthand `<Function>` renderować interaktywny Desmos-style panel.  ten sam panel is także dostępny through a ` ```funcgraph ` fenced blok kodu; Zobacz  czas wykonania [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) dla full walkthrough.

Panel atrybuty (accepted by  container,  shorthand, i  fence header alike):

- `width` / `height` (domyślne `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (Domyślne `true`)
- `xRange="a..b"` (lub `xMin` / `xMax` separately), `xStep` dla tick spacing; ten sam dla  Y oś
- `xLabel` / `yLabel` Dodaj Excel-style oś titles below i nad  plot respectively. jeden obsługa inline `$$...$$` LaTeX; `domain="a..b"` is legacy alias dla `xRange` gdy no jawny X range is present
- `quadrants="1,2,3,4"` lub `quadrants="all"` do force  widoczny quadrants; omit do początek in quadrant 1 z auto-expansion gdy sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, i `cornerLegendBackground` pokazywać compact legenda wewnątrz  plot area używając non-empty curve labels

Curve elementy podrzędne (`<Plot>` / `<Function>`):

- `expr="..."` &mdash;  wyrażenie. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` bezwzględny wartość, `√` / `sqrt` / `∛` / `cbrt`, niejawny multiplication, i  constants `pi`, `tau`, `e`, `phi` są Obsługiwane. Built-in calls cover  standardowy trig/log/exp/rounding family plus dwa-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates  wyrażenie as `x = f(y)` i rotates  curve.
- `domain="a..b"` (x granice shorthand) lub comma-separated clauses takie jak `x>=0, x<5`.
- `color`, `label`. Any curve z non-empty etykieta is automatycznie listed in legenda wyrenderowany just below  panel: small kolor swatch followed by  etykieta, z wpisy flowing lewo-do-prawo i wrapping onto nowy wiersz gdy  następny wpis would nie fit.
- `tooltip` dodaje plain tekst below  computed podpowiedź. `showFunction` i `showValues` Domyślne do `true`; Ustaw either do `false` do ukrywać  wyrenderowany równanie lub live `(x, y)` wartość respectively. A `<Plot>` / nested `<Function>` z `expr` może zawierać Markdown i GuideNH tagi as bogata podpowiedź body.  kolejność is zawsze etykieta, wyrenderowany równanie, live wartości, `tooltip` tekst, następnie bogata element podrzędny treść; pominięty lub wyłączony computed pola są skipped in który kolejność.
- `pointEveryX="step"` dodaje wygenerowany punkt znaczniki at regular x intervals on który curve.
- `pointEveryY="step"` dodaje wygenerowany punkt znaczniki where  curve intersects regular y intervals, używając bounded wyszukiwanie.
- `autoPointLabel="none|x|y|xy"` controls wygenerowany punkt labels; Domyślne is `none`.
- `autoPointColor="#..."` overrides  wygenerowany punkt kolor; pominięty oznacza inherit  curve kolor.

Marked punkty (`<Point>`):

- jawny: `x="..."` i `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` lub `atY="v"` ( czas wykonania bisects on  plot's x-domain do find  pasujący `x`).
- opcjonalny `color`, `label`.

Interaction: najechanie curve do wyróżniać it; press i hold do scrub punkt wzdłuż  curve.  podpowiedź starts z `label` gdy supplied, następnie  równanie i live `(x, y)` wartość unless ich switches są wyłączony; it stays anchored nad  punkt i flips below gdy there is no headroom.

## BetterQuesting Compatibility tagi

`<QuestLink>` i `<QuestCard>` są tylko zarejestrowany gdy  BetterQuesting mod is załadowany. jeden są documented in detail on  [Mod Compatibility](Mod-Compatibility) strona;  summary below covers  most wspólny usage.

### `<QuestLink>`

Inline odnośnik do BetterQuesting quest. kliknięcie opens  quest wewnątrz  BetterQuesting GUI, unless  quest id is także present in  bieżący przewodnik's `quest_ids` frontmatter — in który case  odnośnik navigates do który strona zamiast tego.

| Atrybut | Znaczenie |
| --- | --- |
| `id` | Wymagane BetterQuesting quest id; accepts canonical UUID strings i compact Base64 ids |
| `text` | opcjonalny nadpisywać dla  displayed tekst |
| `show_tooltip` | opcjonalny wartość logiczna (Domyślne `true`); Ustaw do `false` do suppress  quest-opis podpowiedź. `showTooltip` is accepted as alias |

widoczność zachowanie is decided per gracz at kompilować czas:

- widoczny / completed quests renderować as clickable odnośnik (completed quests są tinted green i append a `✓` mark)
- locked but non-ukryty quests nadal renderować as clickable quest links so jeden może open  BetterQuesting quest ekran lub  indexed strona przewodnika
- ukryty / secret quests renderować as darker italic placeholder używając `guidenh.compat.bq.hidden`
- nieznany quest ids renderować as red placeholder używając `guidenh.compat.bq.missing`

Przykład:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

blok-level summary card dla BetterQuesting quest. Renders  quest tytuł z  ten sam stan-aware styling as `<QuestLink>`, plus  quest opis as body paragraph gdy  quest is widoczny do  gracz.

| Atrybut | Znaczenie |
| --- | --- |
| `id` | Wymagane BetterQuesting quest id; accepts canonical UUID strings i compact Base64 ids |
| `show_desc` | opcjonalny wartość logiczna (Domyślne `true`); Ustaw do `false` do suppress  opis body |
| `show_tooltip` | opcjonalny wartość logiczna (Domyślne `true`); Ustaw do `false` do suppress  quest-opis podpowiedź on  clickable tytuł. `showTooltip` is accepted as alias |

 accent kolor of  card obramowanie follows  quest stan: green dla completed, gray dla locked / ukryty, red dla missing, i  standardowy odnośnik kolor dla widoczny quests.  tytuł remains clickable dla widoczny, completed, i locked-but-non-ukryty quests.

Przykład:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
