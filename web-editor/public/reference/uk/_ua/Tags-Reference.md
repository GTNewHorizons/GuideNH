# Довідка тегів

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


Цей сторінка lists  built-in виконання теги зареєстрований by `DefaultExtensions`.

## Правила використання

- теги може appear either in блок context або inline context depending on  компілятор.
- MDX comments використовуючи `{/* ... */}` є Підтримувані in сторінка вміст і є ignored by  виконання аналізатор.
- недійсний теги або недійсний атрибути відтворювати посібник errors inline замість цього of silently failing.
- Large feature теги наприклад Рецепти і 3D сцени є documented in їхні власний сторінки:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## Вбудовані та потокові теги

| тег | Purpose | ключ атрибути |
| --- | --- | --- |
| `<a>` | внутрішній/зовнішній посилання і необов’язковий якір назва | `href`, `title`, `name` |
| `<br>` | рядок break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style текст | none |
| `<sup>` | smaller inline superscript-style текст | none |
| `<Color>` | colored inline текст | `id` або `color` |
| `<Spoiler>` | прихований inline текст revealed on наведення | none |
| `<Tooltip>` | розширений наведення підказка з Markdown/тег дочірні елементи | `label` |
| `<SoundLink>` | clickable розширений-текст sound trigger | `sound` або `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted текст; equivalent до `==text==` з необов’язковий колір control | `color` |
| `<PlayerName>` | inserts поточний гравець username | none |
| `<KeyBind>` | inserts keybinding відображати назва | `id` або `action` |
| `<ItemImage>` | inline предмет icon | `id` або `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | предмет підказка + необов’язковий Навігація посилання | `id` або `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command посилання | `command`, `title`, `close` |
| `<Latex>` | LaTeX math formula; inline in flow context, centered відображати блок in блок context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest посилання з стан-aware styling (compat тег, лише зареєстрований коли BetterQuesting is завантажений) | `id`, `text`, `show_tooltip` |

Inline Markdown також підтримує action links для sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## Блокові теги

| тег | Purpose | ключ атрибути |
| --- | --- | --- |
| `<div>` | pass-through блок wrapper | none |
| `<ContentTabs>` | groups alternative розширений вміст under independent tabs | `title`, `color`, `icon`, `iconPng`, `icon_png`, `iconItem`, `icon_item`, `default`, `defaultIndex` |
| `<Tab>` | один вміст panel усередині `<ContentTabs>` | `title` |
| `<details>` | collapsible виконання блок | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | каталог-style outline з з’єднувач рядки | `indent`, `gap` |
| `<Row>` | horizontal flex структура | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertical flex структура | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | ширина-constrained footnote container використовується by виконання Markdown footnotes | `width` |
| `<ItemGrid>` | compact grid of предмет icons | дочірні елементи має be `<ItemIcon id="..."/>` або `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-інтерактивний 3D один-блок перегляд | `id` або `ore`, необов’язковий `scale` (типові до `4`), `float`, `perspective`, `nbt` |
| `<FloatingImage>` | cropped зображення блок з число з плаваючою комою або true inline placement | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | Навігація дочірній елемент listing | `id`, `alphabetical` |
| `<Category>` | список сторінки з category | `name`, `rows` |
| `<Special>` | список built-in MediaWiki special сторінки | `name`, `rows` |
| `<Structure>` | 2.5D isometric блок структура перегляд | `width`, `height` |
| `<Mermaid>` | виконання Mermaid графік import/inline | `src`, `width`, `height` |
| `<CsvTable>` | виконання CSV файл import таблиця | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered стовпець діаграма | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar діаграма | той самий as `<ColumnChart>` |
| `<LineChart>` | рядок діаграма з categorical або numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie діаграма | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter діаграма | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-curve function графік | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | один-curve shorthand для `<FunctionGraph>` | `expr`, plus усі `<FunctionGraph>` panel атрибути |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | Дивіться [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D посібник сцена | Дивіться [GameScene](GameScene) |
| `<QuestCard>` | блок-level BetterQuesting quest summary card (compat тег, лише зареєстрований коли BetterQuesting is завантажений) | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` is a `<GameScene>` дочірній елемент тег. It accepts `controller`, `piece`, `facing`,
`rotation`, `flip`, і `channel` атрибути, і також підтримує StructureLib Типове дочірній елемент теги:
`<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`,
`<GregTechActiveController>`, і `<GregTechPlaceHatches>`.

## Відомості про теги

### `<a>`

Acts like HTML-style якір тег:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` може be відносний, rooted, явний `modid:path`, або HTTP/HTTPS
- `title` стає  підказка
- `name` inserts a сторінка якір ціль

### `<br>`

GuideNH також підтримує MDX break тег з число з плаваючою комою clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` значення:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, і `<sup>`

GuideNH виконання підтримує focused subset of lowercase documentation теги для inline використовувати:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

Creates collapsible виконання блок з summary рядок.  `<summary>` рядок підтримує normal
inline Markdown/тег вміст, і  body може hold ordinary текст plus arbitrary Блокові теги наприклад
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, таблиці, Діаграми, і структура containers.
коли `height` is Установіть, лише  body scrolls;  summary рядок і outer frame залишаються фіксований.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

Текст, прихований за замовчуванням, зі [звичайним посиланням на сторінку](./index.md).

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

атрибути:

- `open` — starts expanded коли present
- `width` — бажаний outer ширина in пікселі
- `height` — бажаний body viewport висота in пікселі; overflow стає scrollable in-game і in site export
- `wrap` — підтримує  usual блок embedding modes наприклад `square`, `tight`, і `through`
- `align` — `left`, `center`, або `right`; коли combined з floating перенесення режим,  whole details блок floats

### `<ContentTabs>`

Groups alternative розширений вміст under independent tabs. лише прямий `<Tab>` дочірні елементи є дійсний.  container itself може також відтворювати quote-style heading рядок над  tabs, відповідний  visual мова of Markdown callouts.

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

- `default` відповідає  перший tab whose `title` відповідає exactly
- `defaultIndex` is zero-based і wins over `default` коли both є present
- `color` optionally overrides  ліворуч accent рядок і вибраний-tab виділяти з `#RRGGBB` або `#AARRGGBB`
- `title` додає an необов’язковий plain-текст heading над  tab strip
- `icon`, `iconPng` / `icon_png`, і `iconItem` / `icon_item` використовувати  той самий heading icon semantics as Markdown quote-style callouts
- недійсний дочірні елементи або недійсний типові відтворювати видимий автор-facing errors

### `<FileTree>`

Renders каталог-style outline з real з’єднувач рядки drawn з  prefix glyphs on кожен рядок. Both Unicode box-drawing (`│ ├ └ ─`) і ASCII (`| +-- \-- ` / four spaces) forms є accepted і може be mixed. Payload текст підтримує  usual inline Markdown (links, **bold**, `code`, …), і those links є clickable both in-game і in  built-in site export.  той самий вміст може також be записаний as fenced ` ```tree ` or ` ```filetree ` блок.

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

необов’язковий per-рядок icons є introduced by leading directive on  payload:

- `{:icon=Text}` — short текст мітка (один або double quotes необов’язковий)
- `{:iconPng=path/to/file.png}` — PNG ресурс розв’язаний against  поточний сторінка
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft предмет icon.  необов’язковий `meta` segment is damage значення (або `*` для wildcard); an необов’язковий trailing `:{snbt}` блок carries SNBT до attach до  стос.

````md
```filetree
світ
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} sample ресурс
```
````

атрибути:

- `indent` — пікселі per depth level (Типове `14`)
- `gap` — extra пікселі between рядки (Типове `0`)

### виконання Blockquotes

Normal Markdown blockquotes відтворювати at виконання з ліворуч accent рядок. GitHub alert синтаксис is Підтримувані:

````md
> [!NOTE]
> Alert body
````

GuideNH також підтримує a виконання-лише власний directive on  перший quoted рядок:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

Підтримувані directive ключі:

- `title`
- `color`
- `icon` для plain текст symbols
- `iconItem` для an `ItemStack` id
- `iconPng` для a посібник ресурс png шлях

лише один icon source слід be provided.

### `<Color>`

використовувати either symbolic колір id або явний hex значення:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

Use `<Spoiler>` for inline hidden text. The content stays rich-text aware, so nested markdown and runtime inline tags
such as `<Color>` still render correctly after reveal.

```mdx
<Spoiler>прихований **bold** текст з <Color color="#55ccff">tint</Color>.</Spoiler>
<Spoiler>[Anchor Link](#headings) досі behaves like normal hoverable посилання коли revealed.</Spoiler>
```

````

Правила:

- `id` і `color` є mutually exclusive in practice; надавати один
- `color` accepts `#RRGGBB`, `#AARRGGBB`, або `transparent`

### `<Tooltip>`

Creates underlined текст який opens розширений вміст підказка on наведення.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

Якщо `label` is пропущений,  trigger текст типові до `tooltip`.

### `<SoundLink>` і Sound Action Links

`<SoundLink>` renders розширений inline вміст який plays sound коли clicked. It не navigate,
і його власний натискання sound replaces  normal посібник натискання sound для який натискання.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound атрибути:

- `sound` is sound event id наприклад `modid:event.name`
- `src` точки at an `.ogg` файл; `modid:sounds/machine/start.ogg` стає `modid:machine.start`
- `volume` типові до `1.0`
- `pitch` типові до `1.0`
- `cooldown` is milliseconds between repeated plays, Типове `250`
- `radius` і `minVolume` control простір екрана attenuation коли використовується in сцени

### `<PlayerName>`

Inserts  поточний Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up keybinding by id або action і renders  гравець's поточний bound ключ назва.

Accepted ids:

-  binding опис id, наприклад `key.jump` або `key.guidenh.open_guide`
-  legacy `category.description` form, наприклад `key.categories.movement.key.jump`

Приклад:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX Comments

GuideNH ignores MDX comments in сторінка вміст:

````md
Видимий текст. {/* прихований вбудований коментар */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH також ignores явний `<Comment>` теги:

````md
Видимий текст. <Comment>Це не відображається.</Comment> Текст залишається видимим.
````

### `<ItemImage>`

Shows inline предмет icon.

| Атрибут | Значення |
| --- | --- |
| `ore` | ore dictionary назва;  перший відповідати wins |
| `id` | предмет reference використовується коли `ore` is absent |
| `nbt` | необов’язковий SNBT предмет дані; merged onto any inline SNBT in `id` |
| `scale` | число з плаваючою комою, Типове `1` |
| `noTooltip` | truthy рядок або empty Атрибут suppresses підказка (legacy; prefer `showTooltip`) |
| `showTooltip` | логічне значення, Типове `true`; `false` suppresses  наведення підказка |
| `showIcon` | логічне значення, Типове `true`; `false` hides  предмет icon graphic |
| `label` | `left` або `right` — shows  предмет відображати назва as текст on  specified бік of  icon; omit для no мітка |
| `format` | format pattern для  мітка текст; підтримує Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) з необов’язковий `%s` placeholder для  предмет назва; Типове (no Атрибут) renders  назва in italic |
| `yOffset` | ціле число піксель offset перевизначати для  **icon** at scale `1`; не affect  мітка текст |
| `labelYOffset` | ціле число піксель offset перевизначати для  **мітка текст** at scale `1`; не affect  icon |

Примітки:

- `ore` takes precedence over `id` коли both є provided
- Якщо GregTech is installed,  вибраний ore відповідати is passed through `GTOreDictUnificator.setStack(...)`
- `label` requires at least один of `showIcon` або `label` до produce видимий вивід; setting both `showIcon="false"` і omitting `label` renders nothing
- `format` лише застосовує коли `label` is Установіть; Якщо `format` має no `%s`,  literal format текст is використовується as  мітка
- inline SNBT in `id` remains Підтримувані; коли both forms є present,  standalone `nbt` Атрибут is merged останній і overrides conflicting ключі

Приклад:

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

Creates a текст посилання використовуючи  предмет's відображати назва і предмет підказка. Якщо `item_ids` точки до a сторінка посібника, натискання navigates до it. `ore` може be використовується до розв’язати  відображати стос з  перший ore dictionary відповідати замість цього of a фіксований registry id.

| Атрибут | Типове | Значення |
| --- | --- | --- |
| `id` | — | предмет registry id, e.g. `minecraft:compass` або `minecraft:wool:1` |
| `ore` | — | ore-dictionary назва; використовує  перший відповідний предмет стос |
| `linksTo` | *(auto)* | overrides  посилання ціль; accepts a ідентифікатор сторінки з необов’язковий `#anchor`, e.g. `./crafting.md#usage` або `#usage`; коли пропущений  ціль is розв’язаний з `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | Установіть до `false` до suppress  наведення підказка; `noTooltip` is legacy alias |
| `showIcon` | *(none)* | `left` або `right` (або any truthy значення → праворуч) — renders  предмет icon beside  посилання текст; omit до показувати текст лише |
| `scale` | `1.0` | відображати scale для  необов’язковий предмет icon; має no effect коли `showIcon` is пропущений |

Приклади:

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

Sends chat command коли clicked.

| Атрибут | Значення |
| --- | --- |
| `command` | Обов’язково, має початок з `/` |
| `title` | необов’язковий підказка heading |
| `close` | проаналізований логічне значення Атрибут; зараз проаналізований but не використовується до close  посібник |

Приклад:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` і `<Column>`

Flex-style containers для блок вміст.

| Атрибут | Значення |
| --- | --- |
| `gap` | ціле число gap between дочірні елементи, Типове `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | логічне значення вираз, Типове `false` |
| `width` | ціле число бажаний ширина; useful для constraining список рядок ширина |

Приклад:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

до constrain  ширина of normal Markdown lists, перенесення їх in container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH використовує Цей блок тег internally коли виконання Markdown footnotes є expanded. It може також be записаний вручну Якщо needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

Renders compact предмет grid. дочірні елементи має be raw `<ItemIcon>` elements, який є проаналізований directly by  grid компілятор. кожен дочірній елемент може використовувати either `id` або `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

Renders non-інтерактивний 3D один-блок сцена.  перегляд має no сцена тло, no сцена
buttons, no layer controls, і no анотація features, but наведення  блок досі shows 
вибір outline і підказка. `ore` має розв’язати до a блок предмет стос.

| Атрибут | Значення |
| --- | --- |
| `id` | блок id; підтримує  normal `modid:block[:meta][:{snbt}]` spelling |
| `ore` | ore dictionary lookup;  перший відповідний блок предмет wins |
| `scale` | камера масштаб multiplier, Типове `4` |
| `float` | legacy flow число з плаваючою комою підтримка: `left` або `right` |
| `perspective` | `isometric-north-east` (Типове), `isometric-north-west`, або `up` |
| `nbt` | необов’язковий SNBT блок-сутність дані merged onto any inline SNBT з `id` |

Примітки:

- inline SNBT усередині `id` is досі accepted для compatibility, but `nbt="..."` is  бажаний
  authoring form
- коли both inline SNBT і `nbt` є present,  `nbt` Атрибут is merged останній і тому
  overrides conflicting ключі
- GuideNH 1.7.10 не підтримка modern блок-стан властивість синтаксис here, so GuideME-style
  `p:<state>` атрибути є intentionally не Підтримувані

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

Дивіться [Images And Assets](Images-And-Assets) для  full поведінка.

Quick Правила:

- `src` підтримує відносний paths, rooted paths, і явний `modid:path` texture ids
- `x`, `y`, `width` / `w`, і `height` / `h` define  crop rectangle on  original зображення і має усі be present
- `scaleX` і `scaleY` resize  cropped результат і підтримка independent horizontal / vertical stretching
- `displayWidth` і `displayHeight` Установіть кінцевий dimensions in пікселі; один значення preserves  crop aspect ratio, поки два значення дозволяти stretching
- `displayWidth` / `displayHeight` не може be combined з `scaleX` / `scaleY`
- `wrap="inline"` places  зображення truly inline усередині текст flow; in який режим `align` is ignored
- old вміст який використовується `width` / `height` as кінцевий відображати розмір має be migrated вручну

### `<SubPages>`, `<Category>`, і `<Special>`

Дивіться [Navigation](Navigation) для full Навігація поведінка.

### `<Structure>`

Дивіться [Examples](Examples) і [GameScene](GameScene) коли deciding whether до використовувати static структура перегляд або full 3D сцена.

### `<Mermaid>`

використовується для виконання Mermaid вміст. поточний виконання підтримка is focused on `mindmap`, either inline або through a сторінка-відносний `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
Вузли виконання можуть містити звичайні блоки.

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` і `height` constrain  виконання viewport box
- усередині  viewport, перетягування pans і  mouse wheel zooms
- quoted Mermaid labels може використовувати розширений inline Markdown наприклад `**bold**` і сторінка links
- `<NodeContent id="...">...</NodeContent>` може be added as дочірні елементи of `<Mermaid>` до замінити node body з arbitrary виконання блоки

### `<CsvTable>`

використовується до аналізувати CSV файл до a виконання таблиця:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves відносний до  поточний сторінка,  той самий way сцена імпорти і normal ресурс links do.

необов’язковий атрибути:

- `header`
  типові до `true`; Установіть `header={false}` до Збережіть  перший рядок unbolded
- `widths`
  Comma-separated ціле число ширина hints наприклад `widths="120,80"`

Приклади:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

 related fenced виконання CSV form також підтримує відповідний метадані:

````md
```csv widths="120,80" header=false
назва,значення
iron,42
gold,17
```
````

### `<Latex>`

Renders LaTeX math formula використовуючи jlatexmath. коли використовується inline (усередині paragraph або текст flow), it renders as scaled glyph який expands  рядок висота до fit  formula. коли записаний as його власний paragraph (блок context), it renders centered as a відображати-режим formula.

| Атрибут | Тип | Типове | опис |
| --- | --- | --- | --- |
| `formula` | рядок | *(Обов’язково)* | LaTeX source рядок |
| `color` | `#RRGGBB` або `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | число з плаваючою комою | `1.0` | відображати розмір multiplier applied on угорі of  автоматичний рядок-висота scaling |
| `sourceScale` | число з плаваючою комою | `100.0` | jlatexmath внутрішній відтворювати resolution; higher значення improve quality at large sizes |
| `tooltip` | рядок | *(none)* | Plain підказка текст показано on наведення |
| `showTooltip` | логічне значення | `false` | показувати  raw LaTeX source as a підказка on наведення |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-лише. Vertical alignment усередині  текст рядок: `baseline` (Типове) aligns  formula's math baseline з  текст baseline; `top` aligns  formula угорі з  рядок угорі; `center` centers it on  текст; `bottom` aligns  formula унизу з  текст унизу |
| `offsetX` | int | `0` | Horizontal піксель offset applied після alignment (positive = праворуч) |
| `offsetY` | int | `0` | Vertical піксель offset applied після alignment (positive = down) |

Приклади:

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

As convenience you може записувати `$$formula$$` directly in Markdown без використовуючи  `<Latex>` тег.
усі відтворення параметри використовувати їхні типові (white colour, scale 1.0, no підказка, baseline-aligned).

- **Inline**: `$$formula$$` embedded усередині paragraph renders as inline formula.
- **відображати**: paragraph whose entire вміст is `$$formula$$` (з необов’язковий surrounding whitespace) renders as centred відображати-режим блок.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

Примітки:

-  formula висота is calibrated до  поточний рядок текст висота. простий formulas відтворювати at текст висота; taller formulas (fractions, summations, integrals, etc.) expand  enclosing рядок висота автоматично.
- `valign` лише застосовує до inline formulas. відображати-режим (блок-level) formulas є завжди centered horizontally; використовувати `offsetY` до shift їх vertically усередині  блок.
- `color` типові до white (`#FFFFFF`). використовувати `#AARRGGBB` format для semi-прозорий fill.
- `sourceScale` лише affects відтворювати sharpness, не  displayed розмір. значення below `16` є clamped до `16`.
- підказка priority is: розширений дочірній елемент Markdown вміст, потім `tooltip="..."`, потім `showTooltip={true}` raw source резервний варіант.
- дочірній елемент підказка вміст is скомпільований as regular посібник Markdown, so it може включати bold текст, lists, links, предмет теги, і nested `<Latex>` formulas.
-  `$$formula$$` shorthand завжди використовує Типове параметри. використовувати  `<Latex>` тег для власний colour, scale, alignment або підказка.

### сцена виконання теги

Ці теги лише працюють усередині `<GameScene>` / `<Scene>`:

| тег | Purpose | ключ атрибути |
| --- | --- | --- |
| `<ImportStructure>` | import зовнішній SNBT/NBT структура ресурс | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | видалити вже-розміщений блоки який відповідати a блок matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp  той самий дочірній елемент Анотації onto кожен відповідний розміщений блок | `id` |

Дивіться [GameScene](GameScene) для сцена import/removal поведінка і [Annotations](Annotations) для анотація template Правила.


## Діаграми

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, і `<ScatterChart>` є інтерактивний діаграма блоки. усі Діаграми share  following звичайний атрибути:

| Атрибут | опис | Типове |
| --- | --- | --- |
| `title` | діаграма заголовок | none |
| `width` / `height` | явний розмір | 320 / 200 |
| `background` / `border` | тло і межа colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | заголовок і значення-мітка colors | light grey |
| `legend` | легенда позиція: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | значення-мітка позиція: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | внутрішній plot легенда позиція: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum внутрішній легенда box розмір | `120` / `64` |
| `cornerLegendBackground` | внутрішній легенда тло колір | `#AA111922` |

Cartesian Діаграми (стовпець / Bar / рядок / Scatter) additionally приймають вісь атрибути `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` і  відповідний `yAxis*` Установіть, plus `showXGrid={true}` / `showYGrid={true}` до toggle gridlines.

дочірні елементи:

* `<Series name="..." color="#..." data="10,20,30"/>` для category-based Діаграми (стовпець / Bar / categorical рядок).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` для numeric X (рядок `numericX={true}`, Scatter).
* `<Slice name="..." value="..." color="#..."/>` для `<PieChart>` лише.

коли `color` is пропущений on a `<Series>` або `<Slice>`, GuideNH cycles through built-in 16-колір palette.

`<Series>` і `<Slice>` також приймають  following необов’язковий icon / підказка атрибути:

* `icon="modid:item"` (той самий синтаксис as `<ItemImage>`'s `id`, може включати `@meta` і inline NBT JSON) — binds an `ItemStack` до  запис;  легенда swatch стає  предмет icon і наведення  дані точка shows  vanilla предмет підказка з  діаграма опис appended at  кінець.
* `iconImage="images/foo.png"` — використовувати PNG ресурс as  легенда swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form текст appended до  підказка (використовувати `\n` для multi-рядок).

Приклад:

```mdx
<PieChart title="Output share">
  <Slice name="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice name="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra атрибути: `categories` (X-вісь або Y-вісь labels, comma separated), `barWidthRatio` (Типове 0.7). `<BarChart>` puts  categories on  Y-вісь і значення on  X-вісь.

#### Combo extensions

`<ColumnChart>` і `<BarChart>` приймають два extra дочірній елемент element types so кілька діаграма styles може share один plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn as a ламана overlay on угорі of  bars. кожен рядок точка sits at  cluster центр of  відповідний category index;  overlay shares  host діаграма's значення вісь. You може declare кілька `<LineSeries>` до overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — small pie діаграма drawn усередині один of  four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) of  plot area. його `<Slice>` дочірні елементи share  той самий синтаксис as in `<PieChart>`.

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

Extra атрибути: `numericX={true}` до enable numeric X-вісь (дочірні елементи має використовувати `points`); `showPoints={false}` hides точка маркери.  hovered точка is pushed outward by 2px уздовж  curve normal, enlarged, і outlined;  adjacent рядок segments thicken by 1px.

`<LineChart>` і `<ScatterChart>` може показувати compact легенда усередині  plot area з `cornerLegend="topRight"` або another corner. записи використовувати existing серія names і colors.

### `<PieChart>`

Extra атрибути: `startAngle` (Типове `-90`, i.e. 12 o'clock); `clockwise={false}` до reverse direction.  hovered slice pops outward 4px уздовж його bisector.

### `<ScatterChart>`

Renders точки лише; `<Series>` має використовувати `points`.  X-вісь is завжди numeric.

## Графіки функцій

`<FunctionGraph>` і  один-curve shorthand `<Function>` відтворювати an інтерактивний Desmos-style panel.  той самий panel is також доступний through a ` ```funcgraph ` fenced блок коду; Дивіться  виконання [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) для full walkthrough.

Panel атрибути (accepted by  container,  shorthand, і  fence header alike):

- `width` / `height` (типові `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (Типове `true`)
- `xRange="a..b"` (або `xMin` / `xMax` separately), `xStep` для tick spacing; той самий для  Y вісь
- `xLabel` / `yLabel` Додайте Excel-style вісь titles below і над  plot respectively. Вони підтримка inline `$$...$$` LaTeX; `domain="a..b"` is legacy alias для `xRange` коли no явний X range is present
- `quadrants="1,2,3,4"` або `quadrants="all"` до force  видимий quadrants; omit до початок in quadrant 1 з auto-expansion коли sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, і `cornerLegendBackground` показувати compact легенда усередині  plot area використовуючи non-empty curve labels

Curve дочірні елементи (`<Plot>` / `<Function>`):

- `expr="..."` &mdash;  вираз. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` абсолютний значення, `√` / `sqrt` / `∛` / `cbrt`, неявний multiplication, і  constants `pi`, `tau`, `e`, `phi` є Підтримувані. Built-in calls cover  стандартний trig/log/exp/rounding family plus два-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates  вираз as `x = f(y)` і rotates  curve.
- `domain="a..b"` (x межі shorthand) або comma-separated clauses наприклад `x>=0, x<5`.
- `color`, `label`. Any curve з non-empty мітка is автоматично listed in легенда відтворений just below  panel: small колір swatch followed by  мітка, з записи flowing ліворуч-до-праворуч і wrapping onto новий рядок коли  наступний запис would не fit.
- `tooltip` додає plain текст below  computed підказка. `showFunction` і `showValues` Типове до `true`; Установіть either до `false` до приховувати  відтворений рівняння або live `(x, y)` значення respectively. A `<Plot>` / nested `<Function>` з `expr` може містити Markdown і GuideNH теги as розширений підказка body.  порядок is завжди мітка, відтворений рівняння, live значення, `tooltip` текст, потім розширений дочірній елемент вміст; пропущений або вимкнено computed поля є skipped in який порядок.
- `pointEveryX="step"` додає згенерований точка маркери at regular x intervals on який curve.
- `pointEveryY="step"` додає згенерований точка маркери where  curve intersects regular y intervals, використовуючи bounded пошук.
- `autoPointLabel="none|x|y|xy"` controls згенерований точка labels; Типове is `none`.
- `autoPointColor="#..."` overrides  згенерований точка колір; пропущений означає inherit  curve колір.

Marked точки (`<Point>`):

- явний: `x="..."` і `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` або `atY="v"` ( виконання bisects on  plot's x-domain до find  відповідний `x`).
- необов’язковий `color`, `label`.

Interaction: наведення curve до виділяти it; press і hold до scrub a точка уздовж  curve.  підказка starts з `label` коли supplied, потім  рівняння і live `(x, y)` значення unless їхні switches є вимкнено; it stays anchored над  точка і flips below коли there is no headroom.

## BetterQuesting Compatibility теги

`<QuestLink>` і `<QuestCard>` є лише зареєстрований коли  BetterQuesting mod is завантажений. Вони є documented in detail on  [Mod Compatibility](Mod-Compatibility) сторінка;  summary below covers  most звичайний usage.

### `<QuestLink>`

Inline посилання до BetterQuesting quest. натискання opens  quest усередині  BetterQuesting GUI, unless  quest id is також present in  поточний посібник's `quest_ids` frontmatter — in який case  посилання navigates до який сторінка замість цього.

| Атрибут | Значення |
| --- | --- |
| `id` | Обов’язково BetterQuesting quest id; accepts canonical UUID strings і compact Base64 ids |
| `text` | необов’язковий перевизначати для  displayed текст |
| `show_tooltip` | необов’язковий логічне значення (Типове `true`); Установіть до `false` до suppress  quest-опис підказка. `showTooltip` is accepted as alias |

видимість поведінка is decided per гравець at компілювати час:

- видимий / completed quests відтворювати as clickable посилання (completed quests є tinted green і append a `✓` mark)
- locked but non-прихований quests досі відтворювати as clickable quest links so Вони може open  BetterQuesting quest екран або  indexed сторінка посібника
- прихований / secret quests відтворювати as darker italic placeholder використовуючи `guidenh.compat.bq.hidden`
- невідомий quest ids відтворювати as red placeholder використовуючи `guidenh.compat.bq.missing`

Приклад:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

блок-level summary card для BetterQuesting quest. Renders  quest заголовок з  той самий стан-aware styling as `<QuestLink>`, plus  quest опис as body paragraph коли  quest is видимий до  гравець.

| Атрибут | Значення |
| --- | --- |
| `id` | Обов’язково BetterQuesting quest id; accepts canonical UUID strings і compact Base64 ids |
| `show_desc` | необов’язковий логічне значення (Типове `true`); Установіть до `false` до suppress  опис body |
| `show_tooltip` | необов’язковий логічне значення (Типове `true`); Установіть до `false` до suppress  quest-опис підказка on  clickable заголовок. `showTooltip` is accepted as alias |

 accent колір of  card межа follows  quest стан: green для completed, gray для locked / прихований, red для missing, і  стандартний посилання колір для видимий quests.  заголовок remains clickable для видимий, completed, і locked-but-non-прихований quests.

Приклад:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
