# guíUn página Format


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

Las páginas de ejecución de GuideNH son archivos Markdown analizado con:

- estándar markdown bloque y inline sintaxis
- YAML frontmatter
- GFM tablas
- strikethrough
- mark resaltar con `==text==`
- GuideNH inline underline extensions: `++text++` (straight underline), `^^text^^` (wavy underline), `::text::` (emphasis dots / dotted underline)
- MDX comments usando `{/* ... */}`
- MDX-style personalizado etiquetas

## Compatible Markdown

GuideNH páginas compatibilidad El común markdown features usado en El Ejemplo guía:

- headings
- paragraphs
- inline emphasis, bold, strike, y code
- inline mark resaltar (`==text==`)
- inline underline (`++text++`), wavy underline (`^^text^^`), y emphasis dots (`::text::`)
- enlaces y imágenes
- literal autolinks para directo URLs, `www.` hosts, y email addresses
- reference enlaces y reference imágenes
- unordered y ordered lists
- GFM task lists
- blockquotes
- GitHub-style alert blockquotes como `[!NOTE]`
- horizontal Reglas
- fenced code bloques
- indented code bloques
- GFM tablas
- footnotes
- lowercase HTML fragments como `<a>`, `<br>`, `<kbd>`, `<sub>`, `<sup>`, y `<details>`
- MDX comments en página texto

Consulta `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` para Un live sample página.

## resaltar

usar `==text==` para inline highlighted texto. usar `<mark color="#8A6A00">text</mark>` cuando Un personalizado resaltar color es needed. El predeterminado mark fondo es un dark golden yellow elegido Un Conserve white texto readable.

## Code bloques

ejecución code bloques actualmente compatibilidad:

- explícito fence languages como `java`, `lua`, `scala`, `csv`, y `mermaid`
- automático idioma inference cuando El fence idioma es omitido
- Un idioma etiqueta mostrado encima de El bloque
- Un arriba-derecha copy button en El en-game viewer
- lightweight ejecución sintaxis highlighting para El detected idioma

Ejemplo:

````md
```lua
local valor = 42
print(valor)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

Indented code bloques son también Compatible:

````md
    print("indented code")
````

cuando Un fenced bloque resolves a `mermaid` y El origen es un Compatible `mindmap`, GuideNH renderiza it as Un interactivo ejecución mindmap en su lugar of Un simple code bloque.

cuando Un fenced bloque es explicitly marked as `csv`, GuideNH renderiza it as Un ejecución tabla en su lugar of Un simple code bloque. Si El fence idioma es omitido, CSV-shaped texto todavía stays Un code bloque y solo usa CSV idioma detection para labeling/highlighting.

explícito CSV tablas puede también provide columna ancho hints:

````md
```csv widths=120,80
nombre,valor
iron,42
gold,17
```
````

Fence metadatos también admite `header=false` y quoted ancho lists:

````md
```csv widths="120,80" header=false
nombre,valor
iron,42
gold,17
```
````

directo GFM-style literal autolinks son también Compatible en normal paragraph texto:

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````

## Mermaid Mindmaps

GuideNH ejecución Mermaid compatibilidad es actualmente focused on `mindmap` diagrams:

- fenced ```` ```mermaid ```` bloques
- auto-detected mermaid code fences whose contenido starts con `mindmap`
- explícito `<Mermaid>...</Mermaid>` etiquetas
- explícito `<Mermaid src="./diagram.mmd" />` importaciones
- enriquecido inline markdown labels dentro de Mermaid node texto
- opcional `<NodeContent id="...">...</NodeContent>` hijos para arbitrary ejecución bloques dentro de coincidente nodes
- whole-diagram arrastrar-a-pan interaction en El en-game viewer
- `layout: tidy-tree` frontmatter dentro de Mermaid origen
- común mindmap node shapes como square, rounded, circle, bang, cloud, y hexagon
- analizado `::icon(...)` y `:::class` metadatos

Ejemplo:

````md
```mermaid
mindmap
  raíz((GuideNH))
    ejecución
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      arrastrar Un pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
Los nodos de ejecución pueden combinar texto, enlaces y bloques.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````

Mermaid diagrams que son no Compatible at ejecución yet todavía fall atrás Un regular Mermaid-labeled code bloques.

## CSV tabla Import

GuideNH también admite ejecución CSV archivo importaciones a través de Un explícito etiqueta:

````md
<CsvTable src="./markdown-table.csv" />
````

El `src` ruta resolves relativo Un El actual página, El mismo way ejecución recurso enlaces y escena `src` importaciones do.

importado CSV tablas puede también provide ancho hints:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

You puede también escribir Un CSV tabla inline con Un explícito fence:

````md
```csv
nombre,valor
iron,42
gold,17
```
````

## Markdown tabla ancho Hints

Ordinary GFM markdown tablas puede también provide ejecución columna ancho hints by adding Un trailing ejecución atributo línea immediately después de El tabla:

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

esto mantiene El tabla itself estándar markdown mientras letting GuideNH apply ejecución-solo preferido columna widths.

## Task Lists, Alerts, y Footnotes

GuideNH ejecución también admite several useful GFM-style behaviors:

- task lists usando `- [ ]` y `- [x]`
- GitHub alert blockquotes como `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, y `[!CAUTION]`
- footnote references y definitions

Ejemplo:

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````

Footnote references renderizan as tooltip-style inline marcadores, y GuideNH appends Un compact ejecución footnote lista cerca El abajo of El página.

## lista ancho Customization

estándar markdown lists do no define ancho controls, but GuideNH ejecución containers puede constrain ellos:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

esto es actualmente El recommended way Un customize lista línea ancho at ejecución.

## Reference enlaces y imágenes

GuideNH admite CommonMark reference definitions:

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## Lowercase HTML ejecución etiquetas

GuideNH ejecución admite Un focused subset of lowercase HTML-style etiquetas directly:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````

otro sin procesar HTML fragments todavía fall atrás Un literal texto-style handling en su lugar of browser-grade HTML renderizado.

## MDX Comments

GuideNH admite El MDX comentario form y ignores it antes de markdown compilation:

````md
Texto visible. {/* comentario en línea oculto */}

{/*
multiline comment
*/}

More visible text.
````

## frontmatter

GuideNH reads El primero YAML frontmatter bloque y parses these known claves:

| clave | tipo | significado |
| --- | --- | --- |
| `navigation` | map | añade El página Un El navigation tree |
| `categories` | lista of strings | añade El página Un MediaWiki-style categories; cada entrada puede optionally usar `category|sort key` |
| `item_id` | elemento filtro expresión | A único NEI-style elemento expresión que makes El página discoverable by `<ItemLink>` |
| `item_ids` | lista of elemento filtro expressions | lista form of `item_id`; cualquier coincidente expresión makes El página discoverable by `<ItemLink>` |
| `ore_ids` | lista of ore dictionary names | Makes El página discoverable by ore-dictionary elementos (e.g. `ingotIron`, `oreCopper`) |
| `quest_ids` | lista of BetterQuesting quest ids | Makes El página discoverable by `<QuestLink>` / `<QuestCard>` y by El open-guíUn hotkey cuando Un quest es hovered en El BQ GUI. Accepts canonical UUID strings y BetterQuesting's compact Base64 form. solo consumed cuando BetterQuesting es cargado. Consulta [Mod Compatibility](Mod-Compatibility) |
| `author` | cadena | único autor nombre. Displayed en El abajo bar. |
| `authors` | lista of strings o `{name: ...}` maps | varios autor names. At most dos son displayed; additional ones son reemplazado con `...`. Takes precedence sobre `author` Si ambos son present. |
| `date` | cadena o YYYY-MM-DD date | contenido creation date. Displayed en El abajo bar. |
| `updated` | cadena o YYYY-MM-DD date | último actualizado date. Displayed en El abajo bar. |
| `zoom` | positive decimal | Per-página contenido zoom multiplier (e.g. `1.5` = 150 %). Multiplied con El global `contentZoom` setting en ModConfig. predeterminado `1.0`. |
| cualquier otro clave | cualquier YAML valor | Preserved en `additionalProperties` para extensions o tooling |

### `navigation`

| campo | obligatorio | tipo | Notas |
| --- | --- | --- | --- |
| `title` | sí | cadena | mostrar nombre en navigation y búsqueda título reserva |
| `keyword` | no | cadena | uno additional búsqueda keyword o alias; admite prefix coincidente |
| `keywords` | no | lista of strings | Additional búsqueda keywords o aliases; valores son combined y deduplicated |
| `parent` | no | página id | padre página id; omitido significa arriba-level node |
| `position` | no | entero | Sibling sort orden; predeterminado `0`, larger valores appear earlier |
| `priority` | no | entero | cargar priority para mismo-ruta página anula; predeterminado `0`, higher wins, equal priority lets El later recurso pack entrada win |
| `icon` | no | elemento id | elemento icon mostrado en navigation/búsqueda. Accepts `modid:name`, `modid:name:meta`, o `modid:name:meta:{snbt}`. El inline SNBT tail es El preferido way Un attach NBT como Un personalizado mostrar nombre. |
| `icons` | no | lista of elemento ids | lista of elemento icons para animated cycling (uno per second). cada entrada usa El mismo sintaxis as `icon`, including inline `:{snbt}` tails. cuando present takes priority sobre `icon`. |
| `icon_texture` | no | recurso ruta | Texture icon ruta resuelto like cualquier otro recurso enlace |
| `icon_textures` | no | lista of recurso paths | lista of texture icons para animated cycling (uno per second). cuando present takes priority sobre `icon_texture`. |
| `required_mod` | no | mod id | oculta El página salvo esto mod es cargado. |
| `required_mods` | no | lista of mod ids | oculta El página salvo cada listed mod es cargado. |
| `excluded_mod` | no | mod id | oculta El página cuando esto mod es cargado. |
| `excluded_mods` | no | lista of mod ids | oculta El página cuando cualquier listed mod es cargado. |

### Ejemplo frontmatter

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

`categories` accepts cualquiera simple category names o `category|sort key` entradas.
usar `<Category name="examples" rows="3" />` Un renderizan Un category listing bloque, y
`<Special name="SpecialPages" rows="3" />` Un embed El generado MediaWiki-style special-página index.

para BetterQuesting integration, `quest_ids` accepts cualquiera of these formats:

- canonical UUID strings como `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting compact quest ids como `AAAAAAAAAAAAAAAAAAAMug==`

Do no lista ambos forms para El mismo quest en uno página's `quest_ids`; Estas normalize Un El mismo interno UUID y would ser treated as duplicates.

cuando cualquier of `author`, `authors`, `date`, o `updated` es present, GuideNH muestra a
abajo bar en El guíUn pantalla (coincidente El arriba toolbar style) con derecha-aligned
texto like: *contenido de MyMod, autor ExampleAuthor, Date 2024-01-15, actualizado 2024-06-01*.

varios authors Ejemplo:
```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```
o con structured entradas:
```yaml
authors:
  - name: Alice
  - name: Bob
```

### `zoom`

El `zoom` clave lets you enlarge o shrink El contenido of a único página sin
affecting cualquier otro página. El valor es un positive decimal treated as Un multiplier:

| Ejemplo valor | Effect |
| --- | --- |
| `1.0` (predeterminado) | normal tamaño |
| `1.5` | 150 % — contenido 50 % larger |
| `0.75` | 75 % — contenido 25 % smaller |

El per-página zoom es multiplied con El global **contentZoom** slider en
ModConfig → GuideNH → UI. esto lets server packs Establezca Un sensible baseline mientras
todavía allowing individual páginas Un fine-tune diseño para narrow o wide contenido.

Ejemplo: Establezca esto página Un mostrar at 150 % of El base zoom:

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```

página diseño es recomputed at El zoom-adjusted ancho, so texto wrapping y todos
bloque geometría remain correct at cualquier zoom level.

## enlace Resolution

GuideNH resolves ids y paths usando these Reglas:

### página enlaces

| entrada | significado |
| --- | --- |
| `subpage.md` | relativo Un El actual página, en El actual página espacio de nombres |
| `./subpage.md` | relativo Un El actual página, en El actual página espacio de nombres |
| `/guide.md` | rooted Un El actual página espacio de nombres, equivalent a `currentmod:guide.md` |
| `gregtech:guide.md` | explícito espacio de nombres; opens `gregtech:guidenh` cuando El actual guíUn ruta es `guidenh` |
| `gregtech:/guide.md` | explícito espacio de nombres plus rooted ruta, normalized a `gregtech:guide.md` |
| `subpage.md#anchor` | página plus ancla fragment |
| `guidenh:other.md#anchor` | explícito `modid:path#anchor` |
| `https://example.com` | externo HTTP/HTTPS enlace |

página enlaces son isolated by espacio de nombres. Un enlace escrito de `assets/guidenh/guidenh/_en_us/index.md` as
`[Guide](guide.md)` resolves a `guidenh:guide.md`; El mismo texto en
`assets/gregtech/guidenh/_en_us/index.md` resolves a `gregtech:guide.md`. Si que página es faltante en El
actual espacio de nombres, GuideNH reports it as Un broken enlace en su lugar of falling atrás Un another mod's página.

explícito `modid:path` enlaces puede cross de uno mod's datos-driven guíUn Un another. El guíUn id es derived de
El destino página espacio de nombres y El actual guíUn ruta, so Un enlace de `guidenh:guidenh` a `gregtech:guide.md`
opens página `gregtech:guide.md` en guía `gregtech:guidenh`.

ancla fragments desplazar El guíUn Un Un heading whose texto lowercased y spaces reemplazado con hyphens
coincide El fragment (e.g. `#crafting-recipe` scrolls a `## Crafting Recipe`), o Un a `<a name="...">` ancla.

### recurso enlaces

recursos usar El mismo resolution Reglas as enlaces. para Ejemplo:

- `test1.png` resolves relativo Un El actual página archivo.
- `/assets/example_structure.snbt` resolves Un El guía's recurso raíz.
- `guidenh:textures/gui/example.png` resolves as Un explícito recurso location.

## elemento Reference sintaxis

Navigation `icon` y `icons`, a lo largo de con etiquetas que aceptan Un elemento id, usar ordinary elemento references:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Un omitido `meta` predeterminados a `0`. Un SNBT tail starts at El primero `{` y es analizado as elemento NBT. Where wildcard
metadatos es Compatible, `*` puede ser combined con El SNBT tail.

Ejemplos:

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### elemento Index Expressions

`item_id` accepts uno NEI-style expresión y `item_ids` accepts Un YAML lista of El mismo expressions. cada
`item_ids` entrada es independent; El página es linked cuando cualquier entrada coincide. Whitespace combines terms, `|`
combines alternatives, y `,` combines Reglas dentro de uno term.

- `minecraft:lava` performs Un case-insensitive partial registry-id coincidir, so it también coincide `minecraft:lava_bucket`.
- `<minecraft:wool:14>` strictly coincide uno elemento y meta valor.
- `ae2:white_paint_ball:*`, `:32767`, y uppercase meta tokens como `:ANY` son compatible strict todos-meta forms.
- `16384-16462,!16386` coincide Un metadatos range mientras excluding `16386`.
- `!minecraft:portal` excludes Un coincidente registry id.
- `r/^m\\w{6}ft$/` usa Un Java regular expresión against El registry id.

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

en esto Ejemplo, whitespace combines condiciones, `,` combines Reglas dentro de uno term, `!` excludes Un coincidir, y
`|` separates alternatives. El primero expresión por tanto accepts potion metadatos de `16384` a través de `16462`
except `16386`, o cualquier metadatos of `ae2:white_paint_ball`. El second expresión demonstrates Un wildcard elemento
reference en Un comma-separated expresión con Un reverse (`!`) regla. El último dos entradas mostrar Un metadatos union
con `28` excluded y Un elemento mapping que opens El `Usage` heading ancla.

Un opcional `#anchor` suffix opens Un coincidente página at Un heading ancla. Exact elemento y explícito-meta mappings usar
El directo index primero; expressions son evaluated solo cuando needed.

## error Handling

Si Un página fails Un analizar, GuideNH crea Un error página en su lugar of crashing El guía. no válido etiquetas, ids, y atributos son reported inline as guía-renderizado error texto.

## Related páginas

- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
