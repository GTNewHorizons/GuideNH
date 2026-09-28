# Навігація

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH builds його Навігація tree з сторінка frontmatter.

In  in-game sidebar, expanded ancestor сторінки залишаються pinned at  угорі поки їхні досі-видимий
descendants прокручування underneath. кілька expanded ancestor levels може стос at once, і кожен sticky
рядок is pushed away лише коли його entire видимий subtree scrolls out, similar до  VSCode файл
explorer.

## frontmatter навігації

 `navigation` map controls whether a сторінка appears in  посібник tree.

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### Довідка полів

| поле | опис |
| --- | --- |
| `title` | Обов’язково відображати заголовок |
| `keyword` | необов’язковий один пошук keyword або alias |
| `keywords` | необов’язковий список of Ключові слова пошуку або aliases |
| `parent` | необов’язковий батьківський ідентифікатор сторінки, розв’язаний like a сторінка посібника посилання |
| `position` | необов’язковий sibling ordering hint |
| `recommend` | необов’язковий home-сторінка recommendation priority; absent означає  сторінка is не показано in  Recommended panel |
| `priority` | необов’язковий Пріоритет завантаження для той самий-шлях сторінка overrides; Типове `0` |
| `icon` | необов’язковий один предмет icon; підтримує inline `mod:item:meta:{snbt}` tails |
| `icons` | необов’язковий cycling предмет icons список; записи може be plain предмет ids з необов’язковий inline `:{snbt}` або `{id, meta?, nbt?}` maps |
| `icon_texture` | необов’язковий texture icon розв’язаний з посібник ресурси |
| `icon_textures` | необов’язковий cycling texture icon список |
| `required_mod` | необов’язковий один mod id; сторінка is прихований коли Цей mod is не завантажений |
| `required_mods` | необов’язковий список of mod ids; сторінка is прихований unless усі listed mods є завантажений |
| `excluded_mod` | необов’язковий один mod id; сторінка is прихований коли Цей mod is завантажений |
| `excluded_mods` | необов’язковий список of mod ids; сторінка is прихований коли any listed mod is завантажений |

### `navigation.position`

`navigation.position` is an необов’язковий ціле число використовується до порядок sibling сторінки in  Навігація tree.

- Missing `position` типові до `0`.
- Larger значення appear earlier.
- Якщо два сторінки have  той самий значення, Вони є sorted by заголовок alphabetically.

### Ключові слова пошуку

`navigation.keyword` додає один пошук alias. `navigation.keywords` додає a список of aliases. Both поля
може be використовується together; значення є trimmed і duplicate значення є ignored. Keyword відповідає використовувати  той самий
мова analyzer і prefix відповідний as заголовок і сторінка вміст searches, поки результати continue до показувати
 normal Навігація заголовок.

```yaml
navigation:
  title: Molecular Assembler
  keyword: assembler
  keywords:
    - molecular assembler
    - autocrafting machine
```

## Рекомендації на головній сторінці

### `navigation.recommend`

`navigation.recommend` is an необов’язковий ціле число використовується by  home сторінка Recommended panel.

- сторінки лише appear in  Recommended panel коли Цей поле is present.
- `0` is дійсний.
- Larger значення appear earlier.
- Якщо два сторінки have  той самий значення, Вони є sorted by заголовок alphabetically.
-  panel works at  `GuidePage` level, so кожен recommended сторінка запис jumps directly до який сторінка.

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Вимоги модів

використовувати `required_mod` або `required_mods` до require один або more завантажений mods. використовувати `excluded_mod` або
`excluded_mods` до приховувати a сторінка коли один або more incompatible mods є завантажений. коли a умова is не
met,  сторінка is excluded з  Навігація tree і усі сторінка indices (предмет, category, etc.) so it
не може be found through Навігація або пошук.

```yaml
navigation:
  title: Applied Energistics Integration
  parent: index.md
  required_mod: appliedenergistics2

navigation:
  title: Multi-Mod Feature
  parent: index.md
  required_mods:
    - gregtech
    - appliedenergistics2
```

Both ключі може be combined;  сторінка is лише показано коли кожен listed mod is present.

Обов’язково і excluded умови може також be combined. усі Обов’язково mods має be завантажений і none of 
excluded mods може be завантажений.

```yaml
navigation:
  title: Client-only Integration
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## Пріоритет завантаження

коли several завантажений ресурс packs надавати  той самий сторінка посібника шлях, GuideNH reads  сторінка
frontmatter перший і chooses  candidate з  highest `navigation.priority`.

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

Правила:

- missing `priority` is `0`
- значення є signed Java integers up до `2147483647`
- higher priority wins
- Якщо priorities є equal,  later processed пакет ресурсів запис wins, відповідний Minecraft ресурс-pack перевизначати порядок
- priority лише decides between candidates для  той самий сторінка шлях і мова/резервний варіант layer

Цей is useful коли mod ships baseline сторінка посібника і pack wants до замінити it без relying лише on
ресурс-pack ordering.

## Джерела піктограм

GuideNH chooses Навігація/пошук icons in Цей порядок:

1. `icon_textures` Якщо at least один texture запис is configured
2. `icon_texture` Якщо  texture файл завантажує successfully
3. `icons` Якщо at least один configured предмет resolves successfully
4. `icon` Якщо  предмет exists
5. no icon Якщо neither is usable

Texture icons є читати з виконання ресурси, so відносний сторінка-локальний files наприклад `test1.png` працюють.

## Батьківські та кореневі вузли

- Omit `parent` до створити корінь node.
- Установіть `parent: index.md` або any other ідентифікатор сторінки до створити a дочірній елемент node.
-  батьківський сторінка має exist in  той самий посібник Навігація tree.

`navigation.parent` використовує  той самий простір імен Правила as Markdown сторінка links:

- `parent: index.md` і `parent: ./index.md` розв’язати усередині  поточний сторінка простір імен.
- `parent: /index.md` resolves з  поточний сторінка простір імен корінь.
- `parent: gregtech:index.md` або `parent: gregtech:/index.md` explicitly targets another простір імен.

дані-driven посібники є isolated by простір імен. сторінки under `assets/guidenh/guidenh/_en_us/...` belong до
`guidenh:guidenh`; сторінки under `assets/gregtech/guidenh/_en_us/...` belong до `gregtech:guidenh`.
відносний parents і links ніколи fall through до another mod's той самий-named сторінка.

## Сторінки категорій

сторінки може join один або more named categories використовуючи frontmatter:

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

кожен запис може be either category назва або `category|sort key`.

Those categories є queryable through  built-in `<Category name="machines" rows="3" />` тег і також auto-створити прихований searchable сторінки наприклад `Category:machines`.
GuideNH також auto-creates  прихований searchable special сторінки `Special:AllPages` і `Special:Categories`.

## Сторінки з індексом предметів

сторінки може реєструвати предмет-до-сторінка mappings використовуючи `item_id` (один значення) або `item_ids` (a список):

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

Ці mappings є використовується by `<ItemLink>`. `item_id` is один NEI-style вираз і кожен `item_ids`
запис is  той самий тип of вираз. для Приклад, `minecraft:potion 16384-16462,!16386` відповідає a
метадані range except для `16386`, поки `minecraft:potion 0-16,20-36,!28` combines два метадані ranges і
excludes `28`. `ae2:white_paint_ball:*` is compatible strict усі-meta mapping.

An необов’язковий `#anchor` suffix scrolls до певний heading коли  посилання is clicked.
 якір is formed by lowercasing  heading текст і replacing spaces з hyphens
(e.g. `## Crafting Recipe` → `#crafting-recipe`).

Lookup поведінка:

1. exact предмет + exact meta
2. wildcard-meta резервний варіант Якщо present
3. відповідний предмет вираз

## Якорі заголовків

GuideNH підтримує heading якір Навігація in Markdown links і `<a>` теги.
Anchors є derived з heading текст by lowercasing і replacing spaces з hyphens.

**той самий-сторінка якір:**

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

**Cross-сторінка якір:**

```md
[See Getting Started](./Guide-Page-Format#installation)
[Another guide](other-guide.md#usage)
```

**абсолютний шлях якір** (використовуючи  посібник простір імен, avoids відносний шлях ambiguity in subdirectories):

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

 `namespace:path` format resolves до  сторінка посібника whose id відповідає `namespace:path`.
Цей is identical до what відносний paths розв’язати до, but avoids `../` Навігація.
 сторінка має exist in  той самий посібник as  посилання source.

**Named inline anchors** може також be розміщений з `<a name="...">` in MDX:

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

Navigating до посилання з якір scrolls  посібник до  ціль heading або named якір.

## `<SubPages>`

`<SubPages>` renders links до Навігація дочірні елементи.

### атрибути

| Атрибут | Тип | Типове | Значення |
| --- | --- | --- | --- |
| `id` | ідентифікатор сторінки або empty рядок | поточний сторінка | сторінка whose дочірні елементи слід be listed |
| `alphabetical` | логічне значення вираз | `false` | Sort дочірні елементи by заголовок замість цього of Навігація порядок |

### Приклади

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

Special case: `id=""` lists корінь Навігація nodes.

## `<Category>`

`<Category>` renders links до кожен сторінка in named category.

### атрибути

| Атрибут | Тип | Типове | Значення |
| --- | --- | --- | --- |
| `name` | рядок | none | Category назва до відтворювати |
| `rows` | positive ціле число | `3` | число of відображати стовпці in  MediaWiki-style структура |

````md
<Category name="machines" rows="3" />
````

Якщо  category is missing, GuideNH renders inline помилка.

 той самий category також має auto-згенерований прихований searchable сторінка at `Category:machines`.

## `<Special>`

`<Special>` renders один of  built-in MediaWiki-style special сторінка listings.

### атрибути

| Атрибут | Тип | Типове | Значення |
| --- | --- | --- | --- |
| `name` | рядок | none | Підтримувані значення: `AllPages`, `Categories` |
| `rows` | positive ціле число | `3` | число of відображати стовпці in  MediaWiki-style структура |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

 той самий вміст is також доступний through  прихований searchable сторінки `Special:AllPages` і `Special:Categories`.

## Заголовки результатів пошуку

пошук titles є derived in Цей порядок:

1. `navigation.title`
2. перший level-1 heading (`# Heading`)
3. raw ідентифікатор сторінки

## Пов’язані сторінки

- [Guide Page Format](Guide-Page-Format)
- [Search](Search)
- [Tags Reference](Tags-Reference)
