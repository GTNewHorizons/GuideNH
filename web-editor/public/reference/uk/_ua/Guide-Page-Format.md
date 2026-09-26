# Формат сторінки посібника

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH виконання сторінки є Markdown files проаналізований з:

- стандартний Markdown блок і inline синтаксис
- YAML frontmatter
- GFM таблиці
- strikethrough
- mark виділяти з `==text==`
- GuideNH inline underline extensions: `++text++` (straight underline), `^^text^^` (wavy underline), `::text::` (emphasis dots / dotted underline)
- MDX comments використовуючи `{/* ... */}`
- MDX-style власний теги

## Підтримуваний Markdown

GuideNH сторінки підтримка  звичайний Markdown features використовується in  Приклад посібник:

- headings
- paragraphs
- inline emphasis, bold, strike, і code
- inline mark виділяти (`==text==`)
- inline underline (`++text++`), wavy underline (`^^text^^`), і emphasis dots (`::text::`)
- links і зображення
- literal autolinks для прямий URLs, `www.` hosts, і email addresses
- reference links і reference зображення
- unordered і ordered lists
- GFM task lists
- blockquotes
- GitHub-style alert blockquotes наприклад `[!NOTE]`
- horizontal Правила
- fenced блоки коду
- indented блоки коду
- GFM таблиці
- footnotes
- lowercase HTML fragments наприклад `<a>`, `<br>`, `<kbd>`, `<sub>`, `<sup>`, і `<details>`
- MDX comments in сторінка текст

Дивіться `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` для live sample сторінка.

## виділяти

використовувати `==text==` для inline highlighted текст. використовувати `<mark color="#8A6A00">text</mark>` коли a власний виділяти колір is needed.  Типове mark тло is dark golden yellow вибраний до Збережіть white текст readable.

## блоки коду

виконання блоки коду зараз підтримка:

- явний fence languages наприклад `java`, `lua`, `scala`, `csv`, і `mermaid`
- автоматичний мова inference коли  fence мова is пропущений
- a мова мітка показано над  блок
- угорі-праворуч copy button in  in-game viewer
- lightweight виконання синтаксис highlighting для  detected мова

Приклад:

````md
```lua
локальний значення = 42
print(значення)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

Indented блоки коду є також Підтримувані:

````md
    print("indented code")
````

коли fenced блок resolves до `mermaid` і  source is Підтримувані `mindmap`, GuideNH renders it as an інтерактивний виконання mindmap замість цього of plain блок коду.

коли fenced блок is explicitly marked as `csv`, GuideNH renders it as a виконання таблиця замість цього of plain блок коду. Якщо  fence мова is пропущений, CSV-shaped текст досі stays a блок коду і лише використовує CSV мова detection для labeling/highlighting.

явний CSV таблиці може також надавати стовпець ширина hints:

````md
```csv widths=120,80
назва,значення
iron,42
gold,17
```
````

Fence метадані також підтримує `header=false` і quoted ширина lists:

````md
```csv widths="120,80" header=false
назва,значення
iron,42
gold,17
```
````

прямий GFM-style literal autolinks є також Підтримувані in normal paragraph текст:

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````

## Ментальні карти Mermaid

GuideNH виконання Mermaid підтримка is зараз focused on `mindmap` diagrams:

- fenced ```` ```mermaid ```` блоки
- auto-detected mermaid code fences whose вміст starts з `mindmap`
- явний `<Mermaid>...</Mermaid>` теги
- явний `<Mermaid src="./diagram.mmd" />` імпорти
- розширений inline Markdown labels усередині Mermaid node текст
- необов’язковий `<NodeContent id="...">...</NodeContent>` дочірні елементи для arbitrary виконання блоки усередині відповідний nodes
- whole-diagram перетягування-до-pan interaction in  in-game viewer
- `layout: tidy-tree` frontmatter усередині Mermaid source
- звичайний mindmap node shapes наприклад square, rounded, circle, bang, cloud, і hexagon
- проаналізований `::icon(...)` і `:::class` метадані

Приклад:

````md
```mermaid
mindmap
  корінь((GuideNH))
    виконання
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      перетягування до pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
Вузли виконання можуть поєднувати текст, посилання та блоки.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````

Mermaid diagrams який є не Підтримувані at виконання yet досі fall назад до regular Mermaid-labeled блоки коду.

## Імпорт таблиці CSV

GuideNH також підтримує виконання CSV файл імпорти through явний тег:

````md
<CsvTable src="./markdown-table.csv" />
````

 `src` шлях resolves відносний до  поточний сторінка,  той самий way виконання ресурс links і сцена `src` імпорти do.

імпортований CSV таблиці може також надавати ширина hints:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

You може також записувати CSV таблиця inline з явний fence:

````md
```csv
назва,значення
iron,42
gold,17
```
````

## Підказки ширини таблиць Markdown

Ordinary GFM Markdown таблиці може також надавати виконання стовпець ширина hints by adding trailing виконання Атрибут рядок immediately після  таблиця:

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

Цей зберігає  таблиця itself стандартний Markdown поки letting GuideNH apply виконання-лише бажаний стовпець widths.

## Списки завдань, сповіщення та виноски

GuideNH виконання також підтримує several useful GFM-style behaviors:

- task lists використовуючи `- [ ]` і `- [x]`
- GitHub alert blockquotes наприклад `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, і `[!CAUTION]`
- footnote references і definitions

Приклад:

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````

Footnote references відтворювати as підказка-style inline маркери, і GuideNH appends compact виконання footnote список near  унизу of  сторінка.

## Налаштування ширини списку

стандартний Markdown lists do не define ширина controls, but GuideNH виконання containers може constrain їх:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

Цей is зараз  recommended way до customize список рядок ширина at виконання.

## Довідкові посилання та зображення

GuideNH підтримує CommonMark reference definitions:

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## Lowercase HTML виконання теги

GuideNH виконання підтримує focused subset of lowercase HTML-style теги directly:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````

Other raw HTML fragments досі fall назад до literal текст-style handling замість цього of browser-grade HTML відтворення.

## MDX Comments

GuideNH підтримує  MDX коментар form і ignores it до Markdown compilation:

````md
Видимий текст. {/* прихований вбудований коментар */}

{/*
multiline comment
*/}

More visible text.
````

## frontmatter

GuideNH reads  перший YAML frontmatter блок і parses Ці known ключі:

| ключ | Тип | Значення |
| --- | --- | --- |
| `navigation` | map | додає  сторінка до  Навігація tree |
| `categories` | список of strings | додає  сторінка до MediaWiki-style categories; кожен запис може optionally використовувати `category|sort key` |
| `item_id` | предмет фільтр вираз | A один NEI-style предмет вираз який makes  сторінка discoverable by `<ItemLink>` |
| `item_ids` | список of предмет фільтр expressions | список form of `item_id`; any відповідний вираз makes  сторінка discoverable by `<ItemLink>` |
| `ore_ids` | список of ore dictionary names | Makes  сторінка discoverable by ore-dictionary items (e.g. `ingotIron`, `oreCopper`) |
| `quest_ids` | список of BetterQuesting quest ids | Makes  сторінка discoverable by `<QuestLink>` / `<QuestCard>` і by  open-посібник hotkey коли quest is hovered in  BQ GUI. Accepts canonical UUID strings і BetterQuesting's compact Base64 form. лише consumed коли BetterQuesting is завантажений. Дивіться [Mod Compatibility](Mod-Compatibility) |
| `author` | рядок | один автор назва. Displayed in  унизу bar. |
| `authors` | список of strings або `{name: ...}` maps | кілька автор names. At most два є displayed; additional ones є замінений з `...`. Takes precedence over `author` Якщо both є present. |
| `date` | рядок або YYYY-MM-DD date | вміст creation date. Displayed in  унизу bar. |
| `updated` | рядок або YYYY-MM-DD date | останній оновлений date. Displayed in  унизу bar. |
| `zoom` | positive число з плаваючою комою | Per-сторінка вміст масштаб multiplier (e.g. `1.5` = 150 %). Multiplied з  глобальний `contentZoom` setting in ModConfig. Типове `1.0`. |
| any other ключ | any YAML значення | Preserved in `additionalProperties` для extensions або tooling |

### `navigation`

| поле | Обов’язково | Тип | Примітки |
| --- | --- | --- | --- |
| `title` | yes | рядок | відображати назва in Навігація і пошук заголовок резервний варіант |
| `keyword` | no | рядок | один additional пошук keyword або alias; підтримує prefix відповідний |
| `keywords` | no | список of strings | Additional Ключові слова пошуку або aliases; значення є combined і deduplicated |
| `parent` | no | ідентифікатор сторінки | батьківський ідентифікатор сторінки; пропущений означає угорі-level node |
| `position` | no | ціле число | Sibling sort порядок; Типове `0`, larger значення appear earlier |
| `priority` | no | ціле число | Пріоритет завантаження для той самий-шлях сторінка overrides; Типове `0`, higher wins, equal priority lets  later пакет ресурсів запис win |
| `icon` | no | предмет id | предмет icon показано in Навігація/пошук. Accepts `modid:name`, `modid:name:meta`, або `modid:name:meta:{snbt}`.  inline SNBT tail is  бажаний way до attach NBT наприклад a власний відображати назва. |
| `icons` | no | список of предмет ids | список of предмет icons для animated cycling (один per second). кожен запис використовує  той самий синтаксис as `icon`, including inline `:{snbt}` tails. коли present takes priority over `icon`. |
| `icon_texture` | no | ресурс шлях | Texture icon шлях розв’язаний like any other ресурс посилання |
| `icon_textures` | no | список of ресурс paths | список of texture icons для animated cycling (один per second). коли present takes priority over `icon_texture`. |
| `required_mod` | no | mod id | Hides  сторінка unless Цей mod is завантажений. |
| `required_mods` | no | список of mod ids | Hides  сторінка unless кожен listed mod is завантажений. |
| `excluded_mod` | no | mod id | Hides  сторінка коли Цей mod is завантажений. |
| `excluded_mods` | no | список of mod ids | Hides  сторінка коли any listed mod is завантажений. |

### Приклад frontmatter

```yaml
item_id: minecraft:potion 16384-16462,!16386
item_ids:
  - ae2:white_paint_ball:*
  - "<minecraft:wool:14>"
navigation:
  title: Root
  parent: index.md
  position: 10
  priority: 0
  icon: minecraft:book:0:{display:{Name:"My Custom Book"}}
  # Use meta/damage to select a specific subtype:
  # icon: minecraft:wool:1       (orange wool, colon form)
  # Cycling icons list — cycles one per second:
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4:{display:{Name:"Custom Green Wool"}}
  #   - minecraft:wool:14

  icon_texture: test1.png
  # Cycling textures:
  # icon_textures:
  #   - test1.png
  #   - test2.png
categories:
  - basics
  - examples|Examples Overview
ore_ids:
  - ingotIron
  - oreCopper
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
author: ExampleAuthor
date: 2024-01-15
updated: 2024-06-01
```

`categories` accepts either plain category names або `category|sort key` записи.
використовувати `<Category name="examples" rows="3" />` до відтворювати category listing блок, і
`<Special name="SpecialPages" rows="3" />` до embed  згенерований MediaWiki-style special-сторінка index.

для BetterQuesting integration, `quest_ids` accepts either of Ці formats:

- canonical UUID strings наприклад `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting compact quest ids наприклад `AAAAAAAAAAAAAAAAAAAMug==`

Do не список both forms для  той самий quest in один сторінка's `quest_ids`; Вони normalize до  той самий внутрішній UUID і would be treated as duplicates.

коли any of `author`, `authors`, `date`, або `updated` is present, GuideNH shows a
унизу bar in  посібник екран (відповідний  угорі toolbar style) з праворуч-aligned
текст like: *вміст з MyMod, автор ExampleAuthor, Date 2024-01-15, оновлений 2024-06-01*.

кілька authors Приклад:
```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```
або з structured записи:
```yaml
authors:
  - name: Alice
  - name: Bob
```

### `zoom`

 `zoom` ключ lets you enlarge або shrink  вміст of a один сторінка без
affecting any other сторінка.  значення is positive число з плаваючою комою treated as multiplier:

| Приклад значення | Effect |
| --- | --- |
| `1.0` (Типове) | Normal розмір |
| `1.5` | 150 % — вміст 50 % larger |
| `0.75` | 75 % — вміст 25 % smaller |

 per-сторінка масштаб is multiplied з  глобальний **contentZoom** slider in
ModConfig → GuideNH → UI. Цей lets server packs Установіть sensible baseline поки
досі allowing individual сторінки до fine-tune структура для narrow або wide вміст.

Приклад: Установіть Цей сторінка до відображати at 150 % of  base масштаб:

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```

сторінка структура is recomputed at  масштаб-adjusted ширина, so текст wrapping і усі
блок геометрією remain correct at any масштаб level.

## Розв’язання посилань

GuideNH resolves ids і paths використовуючи Ці Правила:

### сторінка links

| ввід | Значення |
| --- | --- |
| `subpage.md` | відносний до  поточний сторінка, in  поточний сторінка простір імен |
| `./subpage.md` | відносний до  поточний сторінка, in  поточний сторінка простір імен |
| `/guide.md` | rooted до  поточний сторінка простір імен, equivalent до `currentmod:guide.md` |
| `gregtech:guide.md` | явний простір імен; opens `gregtech:guidenh` коли  поточний посібник шлях is `guidenh` |
| `gregtech:/guide.md` | явний простір імен plus rooted шлях, normalized до `gregtech:guide.md` |
| `subpage.md#anchor` | сторінка plus якір fragment |
| `guidenh:other.md#anchor` | явний `modid:path#anchor` |
| `https://example.com` | зовнішній HTTP/HTTPS посилання |

сторінка links є isolated by простір імен. посилання записаний з `assets/guidenh/guidenh/_en_us/index.md` as
`[Guide](guide.md)` resolves до `guidenh:guide.md`;  той самий текст in
`assets/gregtech/guidenh/_en_us/index.md` resolves до `gregtech:guide.md`. Якщо який сторінка is missing in 
поточний простір імен, GuideNH reports it as broken посилання замість цього of falling назад до another mod's сторінка.

явний `modid:path` links може cross з один mod's дані-driven посібник до another.  посібник id is derived з
 ціль сторінка простір імен і  поточний посібник шлях, so посилання з `guidenh:guidenh` до `gregtech:guide.md`
opens сторінка `gregtech:guide.md` in посібник `gregtech:guidenh`.

якір fragments прокручування  посібник до heading whose текст lowercased і spaces замінений з hyphens
відповідає  fragment (e.g. `#crafting-recipe` scrolls до `## Crafting Recipe`), або до a `<a name="...">` якір.

### ресурс links

ресурси використовувати  той самий resolution Правила as links. для Приклад:

- `test1.png` resolves відносний до  поточний сторінка файл.
- `/assets/example_structure.snbt` resolves до  посібник's ресурс корінь.
- `guidenh:textures/gui/example.png` resolves as явний ресурс location.

## Синтаксис посилань на предмети

Навігація `icon` і `icons`, уздовж з теги який приймають an предмет id, використовувати ordinary предмет references:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

пропущений `meta` типові до `0`. SNBT tail starts at  перший `{` і is проаналізований as предмет NBT. Where wildcard
метадані is Підтримувані, `*` може be combined з  SNBT tail.

Приклади:

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### Вирази індексу предметів

`item_id` accepts один NEI-style вираз і `item_ids` accepts YAML список of  той самий expressions. кожен
`item_ids` запис is independent;  сторінка is linked коли any запис відповідає. Whitespace combines terms, `|`
combines alternatives, і `,` combines Правила усередині один term.

- `minecraft:lava` performs case-insensitive partial registry-id відповідати, so it також відповідає `minecraft:lava_bucket`.
- `<minecraft:wool:14>` strictly відповідає один предмет і meta значення.
- `ae2:white_paint_ball:*`, `:32767`, і uppercase meta tokens наприклад `:ANY` є compatible strict усі-meta forms.
- `16384-16462,!16386` відповідає метадані range поки excluding `16386`.
- `!minecraft:portal` excludes відповідний registry id.
- `r/^m\\w{6}ft$/` використовує Java regular вираз against  registry id.

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:crafting_table
  - appliedenergistics2:item.ItemMultiMaterial:1
  - "minecraft:written_book:*:{title:TestBook,author:GuideNH},!minecraft:written_book:0"
  - "<minecraft:wool:14>"
  - wrench|hammer
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:diamond#Usage
```

In Цей Приклад, whitespace combines умови, `,` combines Правила усередині один term, `!` excludes відповідати, і
`|` separates alternatives.  перший вираз тому accepts potion метадані з `16384` through `16462`
except `16386`, або any метадані of `ae2:white_paint_ball`.  second вираз demonstrates wildcard предмет
reference in comma-separated вираз з reverse (`!`) rule.  останній два записи показувати метадані union
з `28` excluded і an предмет mapping який opens  `Usage` heading якір.

An необов’язковий `#anchor` suffix opens відповідний сторінка at heading якір. Exact предмет і явний-meta mappings використовувати
 прямий index перший; expressions є evaluated лише коли needed.

## Обробка помилок

Якщо a сторінка fails до аналізувати, GuideNH creates помилка сторінка замість цього of crashing  посібник. недійсний теги, ids, і атрибути є reported inline as посібник-відтворений помилка текст.

## Пов’язані сторінки

- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
