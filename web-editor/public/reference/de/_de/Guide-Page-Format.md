# Leitfaden Seite Format


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH-Laufzeitseiten sind Markdown-Dateien geparst mit:

- Standard markdown Block und inline Syntax
- YAML frontmatter
- GFM Tabellen
- durchgestrichen
- mark hervorheben mit `==text==`
- GuideNH inline underline extensions: `++text++` (straight underline), `^^text^^` (wavy underline), `::text::` (emphasis dots / dotted underline)
- MDX comments using `{/* ... */}`
- MDX-style benutzerdefiniert Tags

## unterstützt Markdown

GuideNH pages Unterstützung Die allgemein markdown features verwendet in Die Beispiel Leitfaden:

- headings
- paragraphs
- inline emphasis, fett, strike, und code
- inline mark hervorheben (`==text==`)
- inline underline (`++text++`), wavy underline (`^^text^^`), und emphasis dots (`::text::`)
- Links und Bilder
- literal autolinks für direkt URLs, `www.` hosts, und email addresses
- reference Links und reference Bilder
- unordered und ordered lists
- GFM task lists
- blockquotes
- GitHub-style alert blockquotes such als `[!NOTE]`
- horizontal Regeln
- fenced code Blöcke
- indented code Blöcke
- GFM Tabellen
- footnotes
- Kleinbuchstaben HTML fragments such als `<a>`, `<br>`, `<kbd>`, `<sub>`, `<sup>`, und `<details>`
- MDX comments in Seite Text

Siehe `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` für Eine live sample Seite.

## hervorheben

verwenden `==text==` für inline highlighted Text. verwenden `<mark color="#8A6A00">text</mark>` wenn Eine benutzerdefiniert hervorheben Farbe ist needed. Die Standard mark Hintergrund ist ein dark golden yellow ausgewählt zu Beibehalten white Text readable.

## Code Blöcke

Laufzeit code Blöcke derzeit Unterstützung:

- explizit fence languages such als `java`, `lua`, `scala`, `csv`, und `mermaid`
- automatisch Sprache inference wenn Die fence Sprache ist omitted
- Eine Sprache Beschriftung angezeigt über Die Block
- Eine oben-rechts copy button in Die in-game viewer
- lightweight Laufzeit Syntax highlighting für Die detected Sprache

Beispiel:

````md
```lua
lokal Wert = 42
print(Wert)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

Indented code Blöcke sind auch unterstützt:

````md
    print("indented code")
````

wenn Eine fenced Block resolves zu `mermaid` und Die Quelle ist ein unterstützt `mindmap`, GuideNH rendert it als Eine interactive Laufzeit Mindmap stattdessen von Eine einfach code Block.

wenn Eine fenced Block ist explicitly marked als `csv`, GuideNH rendert it als Eine Laufzeit Tabelle stattdessen von Eine einfach code Block. Wenn Die fence Sprache ist omitted, CSV-shaped Text weiterhin stays Eine code Block und nur verwendet CSV Sprache detection für labeling/highlighting.

explizit CSV Tabellen kann auch provide Spalte Breite hints:

````md
```csv widths=120,80
Name,Wert
iron,42
gold,17
```
````

Fence metadata auch unterstützt `header=false` und quoted Breite lists:

````md
```csv widths="120,80" header=false
Name,Wert
iron,42
gold,17
```
````

direkt GFM-style literal autolinks sind auch unterstützt in normal paragraph Text:

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````

## Mermaid-Mindmaps

GuideNH Laufzeit Mermaid Unterstützung ist derzeit focused auf `mindmap` diagrams:

- fenced ```` ```mermaid ```` Blöcke
- auto-detected mermaid code fences whose Inhalt Starts mit `mindmap`
- explizit `<Mermaid>...</Mermaid>` Tags
- explizit `<Mermaid src="./diagram.mmd" />` imports
- rich inline markdown labels innerhalb Mermaid Knoten Text
- optional `<NodeContent id="...">...</NodeContent>` untergeordnete Elemente für beliebige Laufzeit Blöcke innerhalb passend Knotens
- whole-diagram ziehen-zu-Schwenk interaction in Die in-game viewer
- `layout: tidy-tree` frontmatter innerhalb Mermaid Quelle
- allgemein Mindmap Knoten shapes such als square, rounded, circle, bang, cloud, und hexagon
- geparst `::icon(...)` und `:::class` metadata

Beispiel:

````md
```mermaid
Mindmap
  Wurzel((GuideNH))
    Laufzeit
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      ziehen zu Schwenk
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
Laufzeitknoten können Text, Links und Blöcke kombinieren.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````

Mermaid diagrams dass sind nicht unterstützt at Laufzeit yet weiterhin fall back zu regular Mermaid-labeled code Blöcke.

## CSV Tabelle Import

GuideNH auch unterstützt Laufzeit CSV Datei imports durch Eine explizit Tag:

````md
<CsvTable src="./markdown-table.csv" />
````

Die `src` Pfad resolves relativ zu Die aktuell Seite, Die gleich way Laufzeit Ressource Links und Szene `src` imports do.

Imported CSV Tabellen kann auch provide Breite hints:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

You kann auch schreiben Eine CSV Tabelle inline mit Eine explizit fence:

````md
```csv
Name,Wert
iron,42
gold,17
```
````

## Markdown Tabelle Breite Hints

Ordinary GFM markdown Tabellen kann auch provide Laufzeit Spalte Breite hints by adding Eine trailing Laufzeit Attribut Linie immediately nach Die Tabelle:

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

dies keeps Die Tabelle itself Standard markdown während letting GuideNH apply Laufzeit-nur bevorzugt Spalte widths.

## Task Lists, Alerts, und Footnotes

GuideNH Laufzeit auch unterstützt several useful GFM-style behaviors:

- task lists using `- [ ]` und `- [x]`
- GitHub alert blockquotes such als `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, und `[!CAUTION]`
- footnote references und definitions

Beispiel:

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````

Footnote references rendern als tooltip-style inline markers, und GuideNH appends Eine compact Laufzeit footnote Liste nahe Die unten von Die Seite.

## Liste Breite Customization

Standard markdown lists do nicht define Breite controls, but GuideNH Laufzeit containers kann constrain sie:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

dies ist derzeit Die recommended way zu customize Liste Linie Breite at Laufzeit.

## Reference Links und Bilder

GuideNH unterstützt CommonMark reference definitions:

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## Kleinbuchstaben HTML Laufzeit Tags

GuideNH Laufzeit unterstützt Eine focused subset von Kleinbuchstaben HTML-style Tags directly:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````

andere roh HTML fragments weiterhin fall back zu literal Text-style handling stattdessen von browser-grade HTML Darstellung.

## MDX Comments

GuideNH unterstützt Die MDX comment form und ignores it vor markdown compilation:

````md
Sichtbarer Text. {/* verborgener Inline-Kommentar */}

{/*
multiline comment
*/}

More visible text.
````

## Frontmatter

GuideNH reads Die erste YAML frontmatter Block und parses diese known Schlüssel:

| Schlüssel | Typ | Bedeutung |
| --- | --- | --- |
| `navigation` | map | Adds Die Seite zu Die navigation tree |
| `categories` | Liste von strings | Adds Die Seite zu MediaWiki-style categories; jede Eintrag kann optionally verwenden `category|sort key` |
| `item_id` | Element Filter Ausdruck | Eine einzeln NEI-style Element Ausdruck dass makes Die Seite discoverable by `<ItemLink>` |
| `item_ids` | Liste von Element Filter expressions | Liste form von `item_id`; beliebig passend Ausdruck makes Die Seite discoverable by `<ItemLink>` |
| `ore_ids` | Liste von ore dictionary names | Makes Die Seite discoverable by ore-dictionary Elemente (e.g. `ingotIron`, `oreCopper`) |
| `quest_ids` | Liste von BetterQuesting quest ids | Makes Die Seite discoverable by `<QuestLink>` / `<QuestCard>` und by Die open-Leitfaden hotkey wenn Eine quest ist hovered in Die BQ GUI. Accepts canonical UUID strings und BetterQuesting's compact Base64 form. nur consumed wenn BetterQuesting ist geladen. Siehe [Mod Compatibility](Mod-Compatibility) |
| `author` | Zeichenkette | einzeln author Name. Displayed in Die unten bar. |
| `authors` | Liste von strings oder `{name: ...}` maps | mehrere author names. At most zwei sind displayed; additional ones sind ersetzt mit `...`. Takes precedence über `author` Wenn beide sind present. |
| `date` | Zeichenkette oder YYYY-MM-DD date | Inhalt creation date. Displayed in Die unten bar. |
| `updated` | Zeichenkette oder YYYY-MM-DD date | letzte aktualisiert date. Displayed in Die unten bar. |
| `zoom` | positive Gleitkommazahl | Per-Seite Inhalt Zoom Multiplikator (e.g. `1.5` = 150 %). Multiplied mit Die globale `contentZoom` setting in ModConfig. Standard `1.0`. |
| beliebig andere Schlüssel | beliebig YAML Wert | Preserved in `additionalProperties` für extensions oder tooling |

### `navigation`

| Feld | erforderlich | Typ | Hinweise |
| --- | --- | --- | --- |
| `title` | ja | Zeichenkette | anzeigen Name in navigation und Suche Titel Fallback |
| `keyword` | nein | Zeichenkette | eins additional Suche keyword oder alias; unterstützt prefix passend |
| `keywords` | nein | Liste von strings | Additional Suche keywords oder aliases; Werte sind kombiniert und deduplicated |
| `parent` | nein | Seite id | übergeordnet Seite id; omitted bedeutet oben-level Knoten |
| `position` | nein | Ganzzahl | Sibling sort Reihenfolge; Standard `0`, larger Werte appear earlier |
| `priority` | nein | Ganzzahl | Ladepriorität für gleich-Pfad Seite überschreibt; Standard `0`, higher wins, equal priority lets Die later Ressource pack Eintrag win |
| `icon` | nein | Element id | Element icon angezeigt in navigation/Suche. Accepts `modid:name`, `modid:name:meta`, oder `modid:name:meta:{snbt}`. Die inline SNBT tail ist Die bevorzugt way zu attach NBT such als Eine benutzerdefiniert anzeigen Name. |
| `icons` | nein | Liste von Element ids | Liste von Element icons für animated cycling (eins per second). jede Eintrag verwendet Die gleich Syntax als `icon`, including inline `:{snbt}` tails. wenn present takes priority über `icon`. |
| `icon_texture` | nein | Ressource Pfad | Texture icon Pfad aufgelöst like beliebig andere Ressource Link |
| `icon_textures` | nein | Liste von Ressource paths | Liste von texture icons für animated cycling (eins per second). wenn present takes priority über `icon_texture`. |
| `required_mod` | nein | mod id | Hides Die Seite außer dies mod ist geladen. |
| `required_mods` | nein | Liste von mod ids | Hides Die Seite außer jede listed mod ist geladen. |
| `excluded_mod` | nein | mod id | Hides Die Seite wenn dies mod ist geladen. |
| `excluded_mods` | nein | Liste von mod ids | Hides Die Seite wenn beliebig listed mod ist geladen. |

### Beispiel Frontmatter

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

`categories` accepts entweder einfach category names oder `category|sort key` Einträge.
verwenden `<Category name="examples" rows="3" />` zu rendern Eine category listing Block, und
`<Special name="SpecialPages" rows="3" />` zu embed Die generiert MediaWiki-style special-Seite index.

für BetterQuesting integration, `quest_ids` accepts entweder von diese formats:

- canonical UUID strings such als `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting compact quest ids such als `AAAAAAAAAAAAAAAAAAAMug==`

Do nicht Liste beide forms für Die gleich quest in eins Seite's `quest_ids`; Sie normalize zu Die gleich intern UUID und would sein treated als duplicates.

wenn beliebig von `author`, `authors`, `date`, oder `updated` ist present, GuideNH zeigt ein
unten bar in Die Leitfaden Bildschirm (passend Die oben toolbar style) mit rechts-aligned
Text like: *Inhalt von MyMod, Author BeispielAuthor, Date 2024-01-15, aktualisiert 2024-06-01*.

mehrere authors Beispiel:
```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```
oder mit structured Einträge:
```yaml
authors:
  - name: Alice
  - name: Bob
```

### `zoom`

Die `zoom` Schlüssel lets you enlarge oder shrink Die Inhalt von Eine einzeln Seite ohne
affecting beliebig andere Seite. Die Wert ist ein positive Gleitkommazahl treated als Eine Multiplikator:

| Beispiel Wert | Effect |
| --- | --- |
| `1.0` (Standard) | normal Größe |
| `1.5` | 150 % — Inhalt 50 % larger |
| `0.75` | 75 % — Inhalt 25 % smaller |

Die per-Seite Zoom ist multiplied mit Die globale **contentZoom** slider in
ModConfig → GuideNH → UI. dies lets server packs Setzen Sie Eine sensible baseline während
weiterhin allowing individual pages zu fine-tune Layout für narrow oder wide Inhalt.

Beispiel: Setzen Sie dies Seite zu anzeigen at 150 % von Die base Zoom:

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```

Seite Layout ist recomputed at Die Zoom-adjusted Breite, so Text wrapping und alle
Block Geometrie remain correct at beliebig Zoom level.

## Linkauflösung

GuideNH resolves ids und paths using diese Regeln:

### Seite Links

| Eingabe | Bedeutung |
| --- | --- |
| `subpage.md` | relativ zu Die aktuell Seite, in Die aktuell Seite namespace |
| `./subpage.md` | relativ zu Die aktuell Seite, in Die aktuell Seite namespace |
| `/guide.md` | rooted zu Die aktuell Seite namespace, equivalent zu `currentmod:guide.md` |
| `gregtech:guide.md` | explizit namespace; opens `gregtech:guidenh` wenn Die aktuell Leitfaden Pfad ist `guidenh` |
| `gregtech:/guide.md` | explizit namespace plus rooted Pfad, normalized zu `gregtech:guide.md` |
| `subpage.md#anchor` | Seite plus anchor fragment |
| `guidenh:other.md#anchor` | explizit `modid:path#anchor` |
| `https://example.com` | extern HTTP/HTTPS Link |

Seite Links sind isolated by namespace. Eine Link written von `assets/guidenh/guidenh/_en_us/index.md` als
`[Guide](guide.md)` resolves zu `guidenh:guide.md`; Die gleich Text in
`assets/gregtech/guidenh/_en_us/index.md` resolves zu `gregtech:guide.md`. Wenn dass Seite ist fehlend in Die
aktuell namespace, GuideNH reports it als Eine broken Link stattdessen von falling back zu another mod's Seite.

explizit `modid:path` Links kann cross von eins mod's Daten-driven Leitfaden zu another. Die Leitfaden id ist derived von
Die Ziel Seite namespace und Die aktuell Leitfaden Pfad, so Eine Link von `guidenh:guidenh` zu `gregtech:guide.md`
opens Seite `gregtech:guide.md` in Leitfaden `gregtech:guidenh`.

Anchor fragments scrollen Die Leitfaden zu Eine heading whose Text lowercased und spaces ersetzt mit hyphens
stimmt überein Die fragment (e.g. `#crafting-recipe` scrolls zu `## Crafting Recipe`), oder zu ein `<a name="...">` anchor.

### Ressource Links

Assets verwenden Die gleich resolution Regeln als Links. für Beispiel:

- `test1.png` resolves relativ zu Die aktuell Seite Datei.
- `/assets/example_structure.snbt` resolves zu Die Leitfaden's Ressource Wurzel.
- `guidenh:textures/gui/example.png` resolves als Eine explizit Ressource location.

## Element Reference Syntax

Navigation `icon` und `icons`, along mit Tags dass accept Eine Element id, verwenden ordinary Element references:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Eine omitted `meta` Standards zu `0`. Eine SNBT tail Starts at Die erste `{` und ist geparst als Element NBT. Where wildcard
metadata ist unterstützt, `*` kann sein kombiniert mit Die SNBT tail.

Beispiele:

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### Element Index Expressions

`item_id` accepts eins NEI-style Ausdruck und `item_ids` accepts Eine YAML Liste von Die gleich expressions. jede
`item_ids` Eintrag ist independent; Die Seite ist linked wenn beliebig Eintrag stimmt überein. Whitespace combines terms, `|`
combines alternatives, und `,` combines Regeln within eins term.

- `minecraft:lava` performs Eine case-insensitive partial registry-id übereinstimmen, so it auch stimmt überein `minecraft:lava_bucket`.
- `<minecraft:wool:14>` strictly stimmt überein eins Element und meta Wert.
- `ae2:white_paint_ball:*`, `:32767`, und uppercase meta tokens such als `:ANY` sind compatible strict alle-meta forms.
- `16384-16462,!16386` stimmt überein Eine metadata range während excluding `16386`.
- `!minecraft:portal` excludes Eine passend registry id.
- `r/^m\\w{6}ft$/` verwendet Eine Java regular Ausdruck against Die registry id.

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

in dies Beispiel, whitespace combines Bedingungen, `,` combines Regeln within eins term, `!` excludes Eine übereinstimmen, und
`|` separates alternatives. Die erste Ausdruck daher accepts potion metadata von `16384` durch `16462`
außer `16386`, oder beliebig metadata von `ae2:white_paint_ball`. Die second Ausdruck demonstrates Eine wildcard Element
reference in Eine comma-separated Ausdruck mit Eine reverse (`!`) Regel. Die letzte zwei Einträge anzeigen Eine metadata union
mit `28` excluded und Eine Element mapping dass opens Die `Usage` heading anchor.

Eine optional `#anchor` suffix opens Eine passend Seite at Eine heading anchor. Exact Element und explizit-meta mappings verwenden
Die direkt index erste; expressions sind evaluated nur wenn needed.

## Fehlerbehandlung

Wenn Eine Seite fails zu parsen, GuideNH erstellt Eine Fehler Seite stattdessen von crashing Die Leitfaden. ungültig Tags, ids, und Attribut sind reported inline als Leitfaden-gerendert Fehler Text.

## Verwandte Seiten

- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
