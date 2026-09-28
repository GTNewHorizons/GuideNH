# Indeling van gids­pagina's

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH runtime pages are Markdown files parsed met:

- standard Markdown blok en inline syntaxis
- YAML frontmatter
- GFM tabellen
- strikethrough
- mark markeren met `==text==`
- GuideNH inline underline extensions: `++text++` (straight underline), `^^text^^` (wavy underline), `::text::` (emphasis dots / dotted underline)
- MDX comments using `{/* ... */}`
- MDX-style aangepaste tags

## Ondersteunde Markdown

GuideNH pages ondersteuning De algemene Markdown features gebruikt in De example gids:

- headings
- paragraphs
- inline emphasis, bold, strike, en code
- inline mark markeren (`==text==`)
- inline underline (`++text++`), wavy underline (`^^text^^`), en emphasis dots (`::text::`)
- links en afbeeldingen
- literal autolinks voor direct URLs, `www.` hosts, en email addresses
- reference links en reference afbeeldingen
- unordered en ordered lists
- GFM task lists
- blockquotes
- GitHub-style alert blockquotes such as `[!NOTE]`
- horizontal Regels
- fenced codeblokken
- indented codeblokken
- GFM tabellen
- footnotes
- lowercase HTML fragments such as `<a>`, `<br>`, `<kbd>`, `<sub>`, `<sup>`, en `<details>`
- MDX comments in pagina tekst

See `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` voor Een live sample pagina.

## markeren

gebruiken `==text==` voor inline highlighted tekst. gebruiken `<mark color="#8A6A00">text</mark>` wanneer Een aangepaste markeren kleur is needed. De Standaard mark achtergrond is Een dark golden yellow chosen naar Behoud white tekst readable.

## codeblokken

runtime codeblokken currently ondersteuning:

- explicit fence languages such as `java`, `lua`, `scala`, `csv`, en `mermaid`
- automatic taal inference wanneer De fence taal wordt weggelaten
- Een taal label getoond above De blok
- Een top-right copy button in De in-game viewer
- lightweight runtime syntaxis highlighting voor De detected taal

Example:

````md
```lua
lokale waarde = 42
print(waarde)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

Indented codeblokken are ook supported:

````md
    print("indented code")
````

wanneer Een fenced blok resolves naar `mermaid` en De source is Een supported `mindmap`, GuideNH renders it as Een interactief runtime mindmap instead of Een plain codeblok.

wanneer Een fenced blok is explicitly marked as `csv`, GuideNH renders it as Een runtime tabel instead of Een plain codeblok. Als De fence taal wordt weggelaten, CSV-shaped tekst still stays Een codeblok en alleen uses CSV taal detection voor labeling/highlighting.

Explicit CSV tabellen kan ook bieden kolom breedte hints:

````md
```csv widths=120,80
naam,waarde
iron,42
gold,17
```
````

Fence metadata ook ondersteunt `header=false` en quoted breedte lists:

````md
```csv widths="120,80" header=false
naam,waarde
iron,42
gold,17
```
````

Direct GFM-style literal autolinks are ook supported in normal paragraph tekst:

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````

## Mermaid-mindmaps

GuideNH runtime Mermaid ondersteuning is currently focused on `mindmap` diagrams:

- fenced ```` ```mermaid ```` blokken
- auto-detected mermaid code fences whose inhoud starts met `mindmap`
- explicit `<Mermaid>...</Mermaid>` tags
- explicit `<Mermaid src="./diagram.mmd" />` imports
- rich inline Markdown labels binnen Mermaid node tekst
- optioneel `<NodeContent id="...">...</NodeContent>` kinderen voor arbitrary runtime blokken binnen matching nodes
- whole-diagram slepen-naar-pan interaction in De in-game viewer
- `layout: tidy-tree` frontmatter binnen Mermaid source
- algemene mindmap node shapes such as square, rounded, circle, bang, cloud, en hexagon
- parsed `::icon(...)` en `:::class` metadata

Example:

````md
```mermaid
mindmap
  root((GuideNH))
    runtime
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      slepen naar pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
Runtimeknooppunten kunnen tekst, links en blokken combineren.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````

Mermaid diagrams that are niet supported at runtime yet still fall back naar regular Mermaid-labeled codeblokken.

## CSV-tabel importeren

GuideNH ook ondersteunt runtime CSV bestand imports through Een explicit tag:

````md
<CsvTable src="./markdown-table.csv" />
````

De `src` pad resolves relative naar De huidige pagina, De zelfde way runtime asset links en scène `src` imports do.

Imported CSV tabellen kan ook bieden breedte hints:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

You kan ook write Een CSV tabel inline met Een explicit fence:

````md
```csv
naam,waarde
iron,42
gold,17
```
````

## Breedte-aanwijzingen voor Markdown-tabellen

Ordinary GFM Markdown tabellen kan ook bieden runtime kolom breedte hints by adding Een trailing runtime Attribuut regel immediately na De tabel:

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

Deze keeps De tabel itself standard Markdown terwijl letting GuideNH apply runtime-alleen preferred kolom widths.

## Takenlijsten, meldingen en voetnoten

GuideNH runtime ook ondersteunt several useful GFM-style behaviors:

- task lists using `- [ ]` en `- [x]`
- GitHub alert blockquotes such as `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, en `[!CAUTION]`
- footnote references en definitions

Example:

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````

Footnote references renderen as tooltip-style inline markers, en GuideNH appends Een compact runtime footnote lijst near De bottom of De pagina.

## Lijstbreedte aanpassen

Standard Markdown lists do niet define breedte controls, but GuideNH runtime containers kan constrain them:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

Deze is currently De recommended way naar customize lijst regel breedte at runtime.

## Referentielinks en afbeeldingen

GuideNH ondersteunt CommonMark reference definitions:

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## Runtime-tags voor kleine HTML-letters

GuideNH runtime ondersteunt Een focused subset of lowercase HTML-style tags directly:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````

Other raw HTML fragments still fall back naar literal tekst-style handling instead of browser-grade HTML rendering.

## MDX Comments

GuideNH ondersteunt De MDX comment form en ignores it voor Markdown compilation:

````md
Zichtbare tekst. {/* verborgen inlineopmerking */}

{/*
multiline comment
*/}

More visible text.
````

## frontmatter

GuideNH reads De first YAML frontmatter blok en parses Deze known keys:

| Key | Type | Betekenis |
| --- | --- | --- |
| `navigation` | map | Adds De pagina naar De Navigatie tree |
| `categories` | lijst of strings | Adds De pagina naar MediaWiki-style categories; elke entry kan optionally gebruiken `category|sort key` |
| `item_id` | item filter expressie | Een enkele NEI-style item expressie that makes De pagina discoverable by `<ItemLink>` |
| `item_ids` | lijst of item filter expressions | lijst form of `item_id`; any matching expressie makes De pagina discoverable by `<ItemLink>` |
| `ore_ids` | lijst of ore dictionary names | Makes De pagina discoverable by ore-dictionary items (e.g. `ingotIron`, `oreCopper`) |
| `quest_ids` | lijst of BetterQuesting quest ids | Makes De pagina discoverable by `<QuestLink>` / `<QuestCard>` en by De open-gids hotkey wanneer Een quest is hovered in De BQ GUI. Accepts canonical UUID strings en BetterQuesting's compact Base64 form. alleen consumed wanneer BetterQuesting is loaded. See [Mod Compatibility](Mod-Compatibility) |
| `author` | tekenreeks | enkele author naam. Displayed in De bottom bar. |
| `authors` | lijst of strings of `{name: ...}` maps | meerdere author names. At most two are displayed; additional ones are vervangen met `...`. Takes precedence over `author` Als both are present. |
| `date` | tekenreeks of YYYY-MM-DD date | inhoud creation date. Displayed in De bottom bar. |
| `updated` | tekenreeks of YYYY-MM-DD date | Last bijgewerkt date. Displayed in De bottom bar. |
| `zoom` | positive kommagetal | Per-pagina inhoud zoomen multiplier (e.g. `1.5` = 150 %). Multiplied met De globale `contentZoom` setting in ModConfig. Standaard `1.0`. |
| any other key | any YAML waarde | Preserved in `additionalProperties` voor extensions of tooling |

### `navigation`

| Field | Vereist | Type | Notes |
| --- | --- | --- | --- |
| `title` | yes | tekenreeks | weergeven naam in Navigatie en zoeken title fallback |
| `keyword` | no | tekenreeks | One additional zoeken keyword of alias; ondersteunt prefix matching |
| `keywords` | no | lijst of strings | Additional Zoekwoorden of aliases; waarden are combined en deduplicated |
| `parent` | no | pagina-ID | bovenliggend pagina-ID; omitted betekent top-level node |
| `position` | no | geheel getal | Sibling sort order; Standaard `0`, larger waarden appear earlier |
| `priority` | no | geheel getal | Laadprioriteit voor zelfde-pad pagina overrides; Standaard `0`, higher wins, equal priority lets De later resourcepack entry win |
| `icon` | no | item id | item icon getoond in Navigatie/zoeken. Accepts `modid:name`, `modid:name:meta`, of `modid:name:meta:{snbt}`. De inline SNBT tail is De preferred way naar attach NBT such as Een aangepaste weergeven naam. |
| `icons` | no | lijst of item ids | lijst of item icons voor animated cycling (one per second). elke entry uses De zelfde syntaxis as `icon`, including inline `:{snbt}` tails. wanneer present takes priority over `icon`. |
| `icon_texture` | no | asset pad | Texture icon pad opgelost like any other asset link |
| `icon_textures` | no | lijst of asset paths | lijst of texture icons voor animated cycling (one per second). wanneer present takes priority over `icon_texture`. |
| `required_mod` | no | mod id | Hides De pagina unless Deze mod is loaded. |
| `required_mods` | no | lijst of mod ids | Hides De pagina unless iedere listed mod is loaded. |
| `excluded_mod` | no | mod id | Hides De pagina wanneer Deze mod is loaded. |
| `excluded_mods` | no | lijst of mod ids | Hides De pagina wanneer any listed mod is loaded. |

### Example frontmatter

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

`categories` accepts either plain category names of `category|sort key` entries.
gebruiken `<Category name="examples" rows="3" />` naar renderen Een category listing blok, en
`<Special name="SpecialPages" rows="3" />` naar embed De generated MediaWiki-style special-pagina index.

voor BetterQuesting integration, `quest_ids` accepts either of Deze formats:

- canonical UUID strings such as `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting compact quest ids such as `AAAAAAAAAAAAAAAAAAAMug==`

Do niet lijst both forms voor De zelfde quest in one pagina's `quest_ids`; they normalize naar De zelfde internal UUID en would be treated as duplicates.

wanneer any of `author`, `authors`, `date`, of `updated` is present, GuideNH shows a
bottom bar in De gids scherm (matching De top toolbar style) met right-aligned
tekst like: *inhoud van MyMod, Author ExampleAuthor, Date 2024-01-15, bijgewerkt 2024-06-01*.

meerdere authors example:
```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```
of met structured entries:
```yaml
authors:
  - name: Alice
  - name: Bob
```

### `zoom`

De `zoom` key lets you enlarge of shrink De inhoud of Een enkele pagina zonder
affecting any other pagina. De waarde is Een positive kommagetal treated as Een multiplier:

| Example waarde | Effect |
| --- | --- |
| `1.0` (Standaard) | Normal grootte |
| `1.5` | 150 % — inhoud 50 % larger |
| `0.75` | 75 % — inhoud 25 % smaller |

De per-pagina zoomen is multiplied met De globale **contentZoom** slider in
ModConfig → GuideNH → UI. Deze lets server packs Stel in Een sensible baseline terwijl
still allowing individual pages naar fine-tune layout voor narrow of wide inhoud.

Example: Stel in Deze pagina naar weergeven at 150 % of De base zoomen:

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```

pagina layout is recomputed at De zoomen-adjusted breedte, so tekst wrapping en alle
blok geometry remain correct at any zoomen level.

## Linkresolutie

GuideNH resolves ids en paths using Deze Regels:

### pagina links

| Input | Betekenis |
| --- | --- |
| `subpage.md` | relative naar De huidige pagina, in De huidige pagina namespace |
| `./subpage.md` | relative naar De huidige pagina, in De huidige pagina namespace |
| `/guide.md` | rooted naar De huidige pagina namespace, equivalent naar `currentmod:guide.md` |
| `gregtech:guide.md` | explicit namespace; opens `gregtech:guidenh` wanneer De huidige gids pad is `guidenh` |
| `gregtech:/guide.md` | explicit namespace plus rooted pad, normalized naar `gregtech:guide.md` |
| `subpage.md#anchor` | pagina plus anchor fragment |
| `guidenh:other.md#anchor` | explicit `modid:path#anchor` |
| `https://example.com` | external HTTP/HTTPS link |

pagina links are isolated by namespace. Een link written van `assets/guidenh/guidenh/_en_us/index.md` as
`[Guide](guide.md)` resolves naar `guidenh:guide.md`; De zelfde tekst in
`assets/gregtech/guidenh/_en_us/index.md` resolves naar `gregtech:guide.md`. Als that pagina is missing in De
huidige namespace, GuideNH reports it as Een broken link instead of falling back naar another mod's pagina.

Explicit `modid:path` links kan cross van one mod's data-driven gids naar another. De gids id is derived van
De target pagina namespace en De huidige gids pad, so Een link van `guidenh:guidenh` naar `gregtech:guide.md`
opens pagina `gregtech:guide.md` in gids `gregtech:guidenh`.

Anchor fragments scrollen De gids naar Een heading whose tekst lowercased en spaces vervangen met hyphens
matches De fragment (e.g. `#crafting-recipe` scrolls naar `## Crafting Recipe`), of naar a `<a name="...">` anchor.

### asset links

Assets gebruiken De zelfde resolution Regels as links. voor example:

- `test1.png` resolves relative naar De huidige pagina bestand.
- `/assets/example_structure.snbt` resolves naar De gids's asset root.
- `guidenh:textures/gui/example.png` resolves as Een explicit resource location.

## Syntaxis voor itemverwijzingen

Navigatie `icon` en `icons`, along met tags that accept Een item id, gebruiken ordinary item references:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Een omitted `meta` defaults naar `0`. Een SNBT tail starts at De first `{` en is parsed as item NBT. Where wildcard
metadata is supported, `*` kan be combined met De SNBT tail.

Voorbeelden:

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### Item-indexexpressies

`item_id` accepts one NEI-style expressie en `item_ids` accepts Een YAML lijst of De zelfde expressions. elke
`item_ids` entry is independent; De pagina is linked wanneer any entry matches. Whitespace combines terms, `|`
combines alternatives, en `,` combines Regels within one term.

- `minecraft:lava` performs Een case-insensitive partial registry-id match, so it ook matches `minecraft:lava_bucket`.
- `<minecraft:wool:14>` strictly matches one item en meta waarde.
- `ae2:white_paint_ball:*`, `:32767`, en uppercase meta tokens such as `:ANY` are compatible strict alle-meta forms.
- `16384-16462,!16386` matches Een metadata range terwijl excluding `16386`.
- `!minecraft:portal` excludes Een matching registry id.
- `r/^m\\w{6}ft$/` uses Een Java regular expressie against De registry id.

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

In Deze example, whitespace combines voorwaarden, `,` combines Regels within one term, `!` excludes Een match, en
`|` separates alternatives. De first expressie therefore accepts potion metadata van `16384` through `16462`
except `16386`, of any metadata of `ae2:white_paint_ball`. De second expressie demonstrates Een wildcard item
reference in Een comma-separated expressie met Een reverse (`!`) rule. De last two entries tonen Een metadata union
met `28` excluded en Een item mapping that opens De `Usage` heading anchor.

Een optioneel `#anchor` suffix opens Een matching pagina at Een heading anchor. Exact item en explicit-meta mappings gebruiken
De direct index first; expressions are evaluated alleen wanneer needed.

## Foutafhandeling

Als Een pagina fails naar parse, GuideNH creates Een error pagina instead of crashing De gids. Invalid tags, ids, en attributen are reported inline as gids-gerenderd error tekst.

## Gerelateerde pagina's

- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
