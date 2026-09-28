# Локалізація

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH підтримує локалізований посібник сторінки і локалізований посібник ресурси.

## Структура папок

локалізація під час виконання is папка-based:

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

 repository Приклад пакет ресурсів зберігає його documented outer `assets/` layer:

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

мова folders є recognized лише in  underscored form. Plain folders наприклад `en_us/` і `zh_cn/` є no longer treated as Локалізація roots.

## Порядок пошуку сторінок

Для кожного requested ідентифікатор сторінки, GuideNH tries:

1. `_<current language>/<page>`
2. `_<default language>/<page>` Якщо  поточна мова сторінка is missing
3. `<page>` без a папка мови

посібник сторінки лише fall назад до  посібник's `defaultLanguage`. Auto-discovered ресурс-pack посібники досі Типове який значення до `en_us`, so another мова is не promoted до a резервна мова just because it exists.

## сторінка Lang ключ Overrides

посібник сторінки може також замінити їхні full джерело Markdown з a `.lang` ключ, but лише коли  physical сторінка файл
вже exists.  файл remains  existence gate і  резервний варіант source.

- GuideNH перший resolves  сторінка файл з  normal мова резервний варіант порядок
- після a файл має been found, GuideNH looks для a сторінка-локалізований `.lang` значення для  requested мова
- Якщо який ключ exists і is non-empty, його full значення стає  сторінка джерело Markdown до parsing
- Якщо  ключ is missing або empty, GuideNH falls назад до  розв’язаний файл вміст

коли non-empty `.lang` сторінка значення is використовується, GuideNH merges missing frontmatter поля з  розв’язаний physical
сторінка до parsing. локалізований frontmatter завжди wins для поля it explicitly defines, so translated
`navigation.title` значення залишаються локалізований, поки newer structural поля наприклад `navigation.recommend`,
`navigation.priority`, categories, предмет links, authorship метадані, або сторінка масштаб може be inherited з 
резервний варіант `.md` файл. Цей зберігає older full-сторінка translations з accidentally dropping home-сторінка recommendations
коли  base сторінка метадані is оновлений.

 ключ format is:

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

Приклад:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

шлях separators become `.` in  ключ. Literal dots усередині a шлях segment є escaped so Вони do не collide з
segment separators:

```text
foo.bar.md -> foo_x2e_bar
```

Other non-alphanumeric characters є escaped з  той самий `_x<hex>_` pattern.

усередині  `.lang` значення, literal `\n` і `\r` sequences є converted до real рядок endings до Markdown parsing,
so  переклад значення може містити full сторінка including frontmatter, headings, lists, і MDX теги.

Authoring Правила:

- записувати  сторінка as один physical `.lang` рядок для який ключ
- використовувати literal `\n` усередині  значення коли you want Markdown рядок break
- do не insert real рядок breaks до  `.lang` значення itself, because Forge reads `.lang` files рядок by рядок
- do не записувати `\\n` unless you intentionally want  кінцевий джерело Markdown до містити  literal characters `\n`

GuideNH не synthesize сторінки з `.lang` alone. real сторінка файл має досі exist.

## ключ Length

GuideNH не impose extra character limit on Ці сторінка ключі. On Minecraft 1.7.10 / Forge,  backing мова
дані is effectively a рядок-властивість map, so  practical limits є normal memory usage і maintainability rather
ніж dedicated hard cap. Shorter сторінка paths досі make ключі easier до автор і review.

## Поради щодо створення

- Установіть `defaultLanguage` deliberately коли you want non-English резервна мова для a посібник
- Додайте shared мова-neutral сторінка лише коли cross-мова резервний варіант is actually intended
- translate сторінки перший, потім translate ресурси лише коли текст is embedded in  ресурс
- avoid мова-певний ресурс filenames коли rooted shared ресурс would do

## Порядок пошуку ресурсів

посібник ресурси використовувати slightly richer резервний варіант порядок:

1. `_<current language>/<path>`
2. Якщо  поточна мова is не  посібник типова мова, `_<default language>/<path>`
3. `<path>`

Цей makes it possible до localize зображення або texture-like ресурси коли needed.

## Пошук і мова

пошук documents store both  raw Minecraft мова і  analyzer мова використовується для Lucene. Якщо  поточний Minecraft мова is не mapped до known analyzer, пошук falls назад до English tokenization.

## Ignore переклад Config

GuideNH не expose a глобальний "ignore translations" switch. Якщо you want a посібник до fall назад до non-English мова, Установіть який посібник's `defaultLanguage` explicitly in code.

## Приклад

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

Repository Приклад pack equivalent:

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## Пов’язані сторінки

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
