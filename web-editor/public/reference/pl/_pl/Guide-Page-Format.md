# Format strony przewodnika

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH czas wykonania strony są Markdown files przeanalizowany z:

- standardowy Markdown blok i inline składnia
- YAML frontmatter
- GFM tabele
- strikethrough
- mark wyróżniać z `==text==`
- GuideNH inline underline extensions: `++text++` (straight underline), `^^text^^` (wavy underline), `::text::` (emphasis dots / dotted underline)
- MDX comments używając `{/* ... */}`
- MDX-style niestandardowy tagi

## Obsługiwany Markdown

GuideNH strony obsługa  wspólny Markdown features używany in  Przykład przewodnik:

- headings
- paragraphs
- inline emphasis, bold, strike, i code
- inline mark wyróżniać (`==text==`)
- inline underline (`++text++`), wavy underline (`^^text^^`), i emphasis dots (`::text::`)
- links i obrazy
- literal autolinks dla bezpośredni URLs, `www.` hosts, i email addresses
- reference links i reference obrazy
- unordered i ordered lists
- GFM task lists
- blockquotes
- GitHub-style alert blockquotes takie jak `[!NOTE]`
- horizontal Zasady
- fenced bloki kodu
- indented bloki kodu
- GFM tabele
- footnotes
- lowercase HTML fragments takie jak `<a>`, `<br>`, `<kbd>`, `<sub>`, `<sup>`, i `<details>`
- MDX comments in strona tekst

Zobacz `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` dla live sample strona.

## wyróżniać

używać `==text==` dla inline highlighted tekst. używać `<mark color="#8A6A00">text</mark>` gdy niestandardowy wyróżniać kolor is needed.  Domyślne mark tło is dark golden yellow wybrany do Zachowaj white tekst readable.

## bloki kodu

czas wykonania bloki kodu obecnie obsługa:

- jawny fence languages takie jak `java`, `lua`, `scala`, `csv`, i `mermaid`
- automatyczny język inference gdy  fence język is pominięty
- język etykieta wyświetlany nad  blok
- góra-prawo copy button in  in-game viewer
- lightweight czas wykonania składnia highlighting dla  detected język

Przykład:

````md
```lua
lokalny wartość = 42
print(wartość)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

Indented bloki kodu są także Obsługiwane:

````md
    print("indented code")
````

gdy fenced blok resolves do `mermaid` i  source is Obsługiwane `mindmap`, GuideNH renders it as interaktywny czas wykonania mindmap zamiast tego of plain blok kodu.

gdy fenced blok is explicitly marked as `csv`, GuideNH renders it as czas wykonania tabela zamiast tego of plain blok kodu. Jeśli  fence język is pominięty, CSV-shaped tekst nadal stays blok kodu i tylko używa CSV język detection dla labeling/highlighting.

jawny CSV tabele może także zapewniać kolumna szerokość hints:

````md
```csv widths=120,80
nazwa,wartość
iron,42
gold,17
```
````

Fence metadane także obsługuje `header=false` i quoted szerokość lists:

````md
```csv widths="120,80" header=false
nazwa,wartość
iron,42
gold,17
```
````

bezpośredni GFM-style literal autolinks są także Obsługiwane in normal paragraph tekst:

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````

## Mapy myśli Mermaid

GuideNH czas wykonania Mermaid obsługa is obecnie focused on `mindmap` diagrams:

- fenced ```` ```mermaid ```` bloki
- auto-detected mermaid code fences whose treść starts z `mindmap`
- jawny `<Mermaid>...</Mermaid>` tagi
- jawny `<Mermaid src="./diagram.mmd" />` importy
- bogata inline Markdown labels wewnątrz Mermaid node tekst
- opcjonalny `<NodeContent id="...">...</NodeContent>` elementy podrzędne dla arbitrary czas wykonania bloki wewnątrz pasujący nodes
- whole-diagram przeciąganie-do-pan interaction in  in-game viewer
- `layout: tidy-tree` frontmatter wewnątrz Mermaid source
- wspólny mindmap node shapes takie jak square, rounded, circle, bang, cloud, i hexagon
- przeanalizowany `::icon(...)` i `:::class` metadane

Przykład:

````md
```mermaid
mindmap
  katalog główny((GuideNH))
    czas wykonania
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      przeciąganie do pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
Węzły czasu wykonywania mogą łączyć tekst, odnośniki i bloki.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````

Mermaid diagrams który są nie Obsługiwane at czas wykonania yet nadal fall tył do regular Mermaid-labeled bloki kodu.

## Import tabeli CSV

GuideNH także obsługuje czas wykonania CSV plik importy through jawny tag:

````md
<CsvTable src="./markdown-table.csv" />
````

 `src` ścieżka resolves względny do  bieżący strona,  ten sam way czas wykonania zasób links i scena `src` importy do.

zaimportowany CSV tabele może także zapewniać szerokość hints:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

You może także zapisywać CSV tabela inline z jawny fence:

````md
```csv
nazwa,wartość
iron,42
gold,17
```
````

## Wskazówki szerokości tabel Markdown

Ordinary GFM Markdown tabele może także zapewniać czas wykonania kolumna szerokość hints by adding trailing czas wykonania Atrybut wiersz immediately po  tabela:

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

Ten zachowuje  tabela itself standardowy Markdown podczas letting GuideNH apply czas wykonania-tylko preferowany kolumna widths.

## Listy zadań, alerty i przypisy

GuideNH czas wykonania także obsługuje several useful GFM-style behaviors:

- task lists używając `- [ ]` i `- [x]`
- GitHub alert blockquotes takie jak `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, i `[!CAUTION]`
- footnote references i definitions

Przykład:

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````

Footnote references renderować as podpowiedź-style inline znaczniki, i GuideNH appends compact czas wykonania footnote lista near  dół of  strona.

## Dostosowanie szerokości listy

standardowy Markdown lists do nie define szerokość controls, but GuideNH czas wykonania containers może constrain ich:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

Ten is obecnie  recommended way do customize lista wiersz szerokość at czas wykonania.

## Łącza referencyjne i obrazy

GuideNH obsługuje CommonMark reference definitions:

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## Lowercase HTML czas wykonania tagi

GuideNH czas wykonania obsługuje focused subset of lowercase HTML-style tagi directly:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````

Other raw HTML fragments nadal fall tył do literal tekst-style handling zamiast tego of browser-grade HTML renderowanie.

## MDX Comments

GuideNH obsługuje  MDX komentarz form i ignores it przed Markdown compilation:

````md
Widoczny tekst. {/* ukryty komentarz wbudowany */}

{/*
multiline comment
*/}

More visible text.
````

## frontmatter

GuideNH reads  pierwszy YAML frontmatter blok i parses Te known klucze:

| klucz | Typ | Znaczenie |
| --- | --- | --- |
| `navigation` | map | dodaje  strona do  Nawigacja tree |
| `categories` | lista of strings | dodaje  strona do MediaWiki-style categories; każdy wpis może optionally używać `category|sort key` |
| `item_id` | element filtr wyrażenie | pojedynczy NEI-style element wyrażenie który makes  strona discoverable by `<ItemLink>` |
| `item_ids` | lista of element filtr expressions | lista form of `item_id`; any pasujący wyrażenie makes  strona discoverable by `<ItemLink>` |
| `ore_ids` | lista of ore dictionary names | Makes  strona discoverable by ore-dictionary items (e.g. `ingotIron`, `oreCopper`) |
| `quest_ids` | lista of BetterQuesting quest ids | Makes  strona discoverable by `<QuestLink>` / `<QuestCard>` i by  open-przewodnik hotkey gdy quest is hovered in  BQ GUI. Accepts canonical UUID strings i BetterQuesting's compact Base64 form. tylko consumed gdy BetterQuesting is załadowany. Zobacz [Mod Compatibility](Mod-Compatibility) |
| `author` | ciąg znaków | pojedynczy autor nazwa. Displayed in  dół bar. |
| `authors` | lista of strings lub `{name: ...}` maps | wiele autor names. At most dwa są displayed; additional ones są zastąpiony z `...`. Takes precedence over `author` Jeśli both są present. |
| `date` | ciąg znaków lub YYYY-MM-DD date | treść creation date. Displayed in  dół bar. |
| `updated` | ciąg znaków lub YYYY-MM-DD date | ostatni zaktualizowany date. Displayed in  dół bar. |
| `zoom` | positive liczba zmiennoprzecinkowa | Per-strona treść powiększenie multiplier (e.g. `1.5` = 150 %). Multiplied z  globalny `contentZoom` setting in ModConfig. Domyślne `1.0`. |
| any other klucz | any YAML wartość | Preserved in `additionalProperties` dla extensions lub tooling |

### `navigation`

| pole | Wymagane | Typ | Uwagi |
| --- | --- | --- | --- |
| `title` | yes | ciąg znaków | wyświetlać nazwa in Nawigacja i wyszukiwanie tytuł awaryjny |
| `keyword` | no | ciąg znaków | jeden additional wyszukiwanie keyword lub alias; obsługuje prefix pasujący |
| `keywords` | no | lista of strings | Additional Słowa kluczowe wyszukiwania lub aliases; wartości są combined i deduplicated |
| `parent` | no | ID strony | nadrzędny ID strony; pominięty oznacza góra-level node |
| `position` | no | liczba całkowita | Sibling sort kolejność; Domyślne `0`, larger wartości appear earlier |
| `priority` | no | liczba całkowita | Priorytet ładowania dla ten sam-ścieżka strona overrides; Domyślne `0`, higher wins, equal priority lets  later pakiet zasobów wpis win |
| `icon` | no | element id | element icon wyświetlany in Nawigacja/wyszukiwanie. Accepts `modid:name`, `modid:name:meta`, lub `modid:name:meta:{snbt}`.  inline SNBT tail is  preferowany way do attach NBT takie jak niestandardowy wyświetlać nazwa. |
| `icons` | no | lista of element ids | lista of element icons dla animated cycling (jeden per second). każdy wpis używa  ten sam składnia as `icon`, including inline `:{snbt}` tails. gdy present takes priority over `icon`. |
| `icon_texture` | no | zasób ścieżka | Texture icon ścieżka rozwiązany like any other zasób odnośnik |
| `icon_textures` | no | lista of zasób paths | lista of texture icons dla animated cycling (jeden per second). gdy present takes priority over `icon_texture`. |
| `required_mod` | no | mod id | Hides  strona unless Ten mod is załadowany. |
| `required_mods` | no | lista of mod ids | Hides  strona unless każdy listed mod is załadowany. |
| `excluded_mod` | no | mod id | Hides  strona gdy Ten mod is załadowany. |
| `excluded_mods` | no | lista of mod ids | Hides  strona gdy any listed mod is załadowany. |

### Przykład frontmatter

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

`categories` accepts either plain category names lub `category|sort key` wpisy.
używać `<Category name="examples" rows="3" />` do renderować category listing blok, i
`<Special name="SpecialPages" rows="3" />` do embed  wygenerowany MediaWiki-style special-strona index.

dla BetterQuesting integration, `quest_ids` accepts either of Te formats:

- canonical UUID strings takie jak `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting compact quest ids takie jak `AAAAAAAAAAAAAAAAAAAMug==`

Do nie lista both forms dla  ten sam quest in jeden strona's `quest_ids`; jeden normalize do  ten sam wewnętrzny UUID i would be treated as duplicates.

gdy any of `author`, `authors`, `date`, lub `updated` is present, GuideNH shows a
dół bar in  przewodnik ekran (pasujący  góra toolbar style) z prawo-aligned
tekst like: *treść z MyMod, autor ExampleAuthor, Date 2024-01-15, zaktualizowany 2024-06-01*.

wiele authors Przykład:
```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```
lub z structured wpisy:
```yaml
authors:
  - name: Alice
  - name: Bob
```

### `zoom`

 `zoom` klucz lets you enlarge lub shrink  treść of pojedynczy strona bez
affecting any other strona.  wartość is positive liczba zmiennoprzecinkowa treated as multiplier:

| Przykład wartość | Effect |
| --- | --- |
| `1.0` (Domyślne) | Normal rozmiar |
| `1.5` | 150 % — treść 50 % larger |
| `0.75` | 75 % — treść 25 % smaller |

 per-strona powiększenie is multiplied z  globalny **contentZoom** slider in
ModConfig → GuideNH → UI. Ten lets server packs Ustaw sensible baseline podczas
nadal allowing individual strony do fine-tune układ dla narrow lub wide treść.

Przykład: Ustaw Ten strona do wyświetlać at 150 % of  base powiększenie:

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```

strona układ is recomputed at  powiększenie-adjusted szerokość, so tekst wrapping i wszystkie
blok geometrią remain correct at any powiększenie level.

## Rozwiązywanie łączy

GuideNH resolves ids i paths używając Te Zasady:

### strona links

| wejście | Znaczenie |
| --- | --- |
| `subpage.md` | względny do  bieżący strona, in  bieżący strona przestrzeń nazw |
| `./subpage.md` | względny do  bieżący strona, in  bieżący strona przestrzeń nazw |
| `/guide.md` | rooted do  bieżący strona przestrzeń nazw, equivalent do `currentmod:guide.md` |
| `gregtech:guide.md` | jawny przestrzeń nazw; opens `gregtech:guidenh` gdy  bieżący przewodnik ścieżka is `guidenh` |
| `gregtech:/guide.md` | jawny przestrzeń nazw plus rooted ścieżka, normalized do `gregtech:guide.md` |
| `subpage.md#anchor` | strona plus kotwica fragment |
| `guidenh:other.md#anchor` | jawny `modid:path#anchor` |
| `https://example.com` | zewnętrzny HTTP/HTTPS odnośnik |

strona links są isolated by przestrzeń nazw. odnośnik zapisany z `assets/guidenh/guidenh/_en_us/index.md` as
`[Guide](guide.md)` resolves do `guidenh:guide.md`;  ten sam tekst in
`assets/gregtech/guidenh/_en_us/index.md` resolves do `gregtech:guide.md`. Jeśli który strona is missing in 
bieżący przestrzeń nazw, GuideNH reports it as broken odnośnik zamiast tego of falling tył do another mod's strona.

jawny `modid:path` links może cross z jeden mod's dane-driven przewodnik do another.  przewodnik id is derived z
 cel strona przestrzeń nazw i  bieżący przewodnik ścieżka, so odnośnik z `guidenh:guidenh` do `gregtech:guide.md`
opens strona `gregtech:guide.md` in przewodnik `gregtech:guidenh`.

kotwica fragments przewijanie  przewodnik do heading whose tekst lowercased i spaces zastąpiony z hyphens
pasuje  fragment (e.g. `#crafting-recipe` scrolls do `## Crafting Recipe`), lub do a `<a name="...">` kotwica.

### zasób links

zasoby używać  ten sam resolution Zasady as links. dla Przykład:

- `test1.png` resolves względny do  bieżący strona plik.
- `/assets/example_structure.snbt` resolves do  przewodnik's zasób katalog główny.
- `guidenh:textures/gui/example.png` resolves as jawny zasób location.

## Składnia odwołań do przedmiotów

Nawigacja `icon` i `icons`, wzdłuż z tagi który akceptują element id, używać ordinary element references:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

pominięty `meta` domyślne do `0`. SNBT tail starts at  pierwszy `{` i is przeanalizowany as element NBT. Where wildcard
metadane is Obsługiwane, `*` może be combined z  SNBT tail.

Przykłady:

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### Wyrażenia indeksu przedmiotów

`item_id` accepts jeden NEI-style wyrażenie i `item_ids` accepts YAML lista of  ten sam expressions. każdy
`item_ids` wpis is independent;  strona is linked gdy any wpis pasuje. Whitespace combines terms, `|`
combines alternatives, i `,` combines Zasady wewnątrz jeden term.

- `minecraft:lava` performs case-insensitive partial registry-id pasować, so it także pasuje `minecraft:lava_bucket`.
- `<minecraft:wool:14>` strictly pasuje jeden element i meta wartość.
- `ae2:white_paint_ball:*`, `:32767`, i uppercase meta tokens takie jak `:ANY` są compatible strict wszystkie-meta forms.
- `16384-16462,!16386` pasuje metadane range podczas excluding `16386`.
- `!minecraft:portal` excludes pasujący registry id.
- `r/^m\\w{6}ft$/` używa Java regular wyrażenie against  registry id.

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

In Ten Przykład, whitespace combines warunki, `,` combines Zasady wewnątrz jeden term, `!` excludes pasować, i
`|` separates alternatives.  pierwszy wyrażenie dlatego accepts potion metadane z `16384` through `16462`
except `16386`, lub any metadane of `ae2:white_paint_ball`.  second wyrażenie demonstrates wildcard element
reference in comma-separated wyrażenie z reverse (`!`) rule.  ostatni dwa wpisy pokazywać metadane union
z `28` excluded i element mapping który opens  `Usage` heading kotwica.

opcjonalny `#anchor` suffix opens pasujący strona at heading kotwica. Exact element i jawny-meta mappings używać
 bezpośredni index pierwszy; expressions są evaluated tylko gdy needed.

## Obsługa błędów

Jeśli strona fails do analizować, GuideNH creates błąd strona zamiast tego of crashing  przewodnik. nieprawidłowy tagi, ids, i atrybuty są reported inline as przewodnik-wyrenderowany błąd tekst.

## Powiązane strony

- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
