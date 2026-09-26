# Nawigacja

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH builds jego Nawigacja tree z strona frontmatter.

In  in-game sidebar, expanded ancestor strony pozostają pinned at  góra podczas ich nadal-widoczny
descendants przewijanie underneath. wiele expanded ancestor levels może stos at once, i każdy sticky
wiersz is pushed away tylko gdy jego entire widoczny subtree scrolls out, similar do  VSCode plik
explorer.

## frontmatter nawigacji

 `navigation` map controls whether strona appears in  przewodnik tree.

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### Opis pól

| pole | opis |
| --- | --- |
| `title` | Wymagane wyświetlać tytuł |
| `keyword` | opcjonalny pojedynczy wyszukiwanie keyword lub alias |
| `keywords` | opcjonalny lista of Słowa kluczowe wyszukiwania lub aliases |
| `parent` | opcjonalny nadrzędny ID strony, rozwiązany like strona przewodnika odnośnik |
| `position` | opcjonalny sibling ordering hint |
| `recommend` | opcjonalny home-strona recommendation priority; absent oznacza  strona is nie wyświetlany in  Recommended panel |
| `priority` | opcjonalny Priorytet ładowania dla ten sam-ścieżka strona overrides; Domyślne `0` |
| `icon` | opcjonalny pojedynczy element icon; obsługuje inline `mod:item:meta:{snbt}` tails |
| `icons` | opcjonalny cycling element icons lista; wpisy może be plain element ids z opcjonalny inline `:{snbt}` lub `{id, meta?, nbt?}` maps |
| `icon_texture` | opcjonalny texture icon rozwiązany z przewodnik zasoby |
| `icon_textures` | opcjonalny cycling texture icon lista |
| `required_mod` | opcjonalny pojedynczy mod id; strona is ukryty gdy Ten mod is nie załadowany |
| `required_mods` | opcjonalny lista of mod ids; strona is ukryty unless wszystkie listed mods są załadowany |
| `excluded_mod` | opcjonalny pojedynczy mod id; strona is ukryty gdy Ten mod is załadowany |
| `excluded_mods` | opcjonalny lista of mod ids; strona is ukryty gdy any listed mod is załadowany |

### `navigation.position`

`navigation.position` is opcjonalny liczba całkowita używany do kolejność sibling strony in  Nawigacja tree.

- Missing `position` domyślne do `0`.
- Larger wartości appear earlier.
- Jeśli dwa strony have  ten sam wartość, jeden są sorted by tytuł alphabetically.

### Słowa kluczowe wyszukiwania

`navigation.keyword` dodaje jeden wyszukiwanie alias. `navigation.keywords` dodaje lista of aliases. Both pola
może be używany together; wartości są trimmed i duplicate wartości są ignored. Keyword pasuje używać  ten sam
język analyzer i prefix pasujący as tytuł i strona treść searches, podczas wyniki continue do pokazywać
 normal Nawigacja tytuł.

```yaml
navigation:
  title: Molecular Assembler
  keyword: assembler
  keywords:
    - molecular assembler
    - autocrafting machine
```

## Polecane na stronie głównej

### `navigation.recommend`

`navigation.recommend` is opcjonalny liczba całkowita używany by  home strona Recommended panel.

- strony tylko appear in  Recommended panel gdy Ten pole is present.
- `0` is prawidłowy.
- Larger wartości appear earlier.
- Jeśli dwa strony have  ten sam wartość, jeden są sorted by tytuł alphabetically.
-  panel works at  `GuidePage` level, so każdy recommended strona wpis jumps directly do który strona.

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Wymagania modów

używać `required_mod` lub `required_mods` do require jeden lub more załadowany mods. używać `excluded_mod` lub
`excluded_mods` do ukrywać strona gdy jeden lub more incompatible mods są załadowany. gdy warunek is nie
met,  strona is excluded z  Nawigacja tree i wszystkie strona indices (element, category, etc.) so it
nie może be found through Nawigacja lub wyszukiwanie.

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

Both klucze może be combined;  strona is tylko wyświetlany gdy każdy listed mod is present.

Wymagane i excluded warunki może także be combined. wszystkie Wymagane mods musi be załadowany i none of 
excluded mods może be załadowany.

```yaml
navigation:
  title: Client-only Integration
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## Priorytet ładowania

gdy several załadowany zasób packs zapewniać  ten sam strona przewodnika ścieżka, GuideNH reads  strona
frontmatter pierwszy i chooses  candidate z  highest `navigation.priority`.

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

Zasady:

- missing `priority` is `0`
- wartości są signed Java integers up do `2147483647`
- higher priority wins
- Jeśli priorities są equal,  later processed pakiet zasobów wpis wins, pasujący Minecraft zasób-pack nadpisywać kolejność
- priority tylko decides between candidates dla  ten sam strona ścieżka i język/awaryjny layer

Ten is useful gdy mod ships baseline strona przewodnika i pack wants do zastąpić it bez relying tylko on
zasób-pack ordering.

## Źródła ikon

GuideNH chooses Nawigacja/wyszukiwanie icons in Ten kolejność:

1. `icon_textures` Jeśli at least jeden texture wpis is configured
2. `icon_texture` Jeśli  texture plik ładuje successfully
3. `icons` Jeśli at least jeden configured element resolves successfully
4. `icon` Jeśli  element exists
5. no icon Jeśli neither is usable

Texture icons są czytać z czas wykonania zasoby, so względny strona-lokalny files takie jak `test1.png` działa.

## Węzły nadrzędne i główne

- Omit `parent` do utworzyć katalog główny node.
- Ustaw `parent: index.md` lub any other ID strony do utworzyć element podrzędny node.
-  nadrzędny strona musi exist in  ten sam przewodnik Nawigacja tree.

`navigation.parent` używa  ten sam przestrzeń nazw Zasady as Markdown strona links:

- `parent: index.md` i `parent: ./index.md` rozwiązać wewnątrz  bieżący strona przestrzeń nazw.
- `parent: /index.md` resolves z  bieżący strona przestrzeń nazw katalog główny.
- `parent: gregtech:index.md` lub `parent: gregtech:/index.md` explicitly targets another przestrzeń nazw.

dane-driven przewodniki są isolated by przestrzeń nazw. strony under `assets/guidenh/guidenh/_en_us/...` belong do
`guidenh:guidenh`; strony under `assets/gregtech/guidenh/_en_us/...` belong do `gregtech:guidenh`.
względny parents i links nigdy fall through do another mod's ten sam-named strona.

## Strony kategorii

strony może join jeden lub more named categories używając frontmatter:

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

każdy wpis może be either category nazwa lub `category|sort key`.

Those categories są queryable through  built-in `<Category name="machines" rows="3" />` tag i także auto-utworzyć ukryty searchable strony takie jak `Category:machines`.
GuideNH także auto-creates  ukryty searchable special strony `Special:AllPages` i `Special:Categories`.

## Strony indeksowane przedmiotami

strony może rejestrować element-do-strona mappings używając `item_id` (jeden wartość) lub `item_ids` (lista):

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

Te mappings są używany by `<ItemLink>`. `item_id` is jeden NEI-style wyrażenie i każdy `item_ids`
wpis is  ten sam rodzaj of wyrażenie. dla Przykład, `minecraft:potion 16384-16462,!16386` pasuje a
metadane range except dla `16386`, podczas `minecraft:potion 0-16,20-36,!28` combines dwa metadane ranges i
excludes `28`. `ae2:white_paint_ball:*` is compatible strict wszystkie-meta mapping.

opcjonalny `#anchor` suffix scrolls do określony heading gdy  odnośnik is clicked.
 kotwica is formed by lowercasing  heading tekst i replacing spaces z hyphens
(e.g. `## Crafting Recipe` → `#crafting-recipe`).

Lookup zachowanie:

1. exact element + exact meta
2. wildcard-meta awaryjny Jeśli present
3. pasujący element wyrażenie

## Łącza kotwic nagłówków

GuideNH obsługuje heading kotwica Nawigacja in Markdown links i `<a>` tagi.
Anchors są derived z heading tekst by lowercasing i replacing spaces z hyphens.

**ten sam-strona kotwica:**

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

**Cross-strona kotwica:**

```md
[See Getting Started](./Guide-Page-Format#installation)
[Another guide](other-guide.md#usage)
```

**bezwzględny ścieżka kotwica** (używając  przewodnik przestrzeń nazw, avoids względny ścieżka ambiguity in subdirectories):

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

 `namespace:path` format resolves do  strona przewodnika whose id pasuje `namespace:path`.
Ten is identical do what względny paths rozwiązać do, but avoids `../` Nawigacja.
 strona musi exist in  ten sam przewodnik as  odnośnik source.

**Named inline anchors** może także be umieszczony z `<a name="...">` in MDX:

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

Navigating do odnośnik z kotwica scrolls  przewodnik do  cel heading lub named kotwica.

## `<SubPages>`

`<SubPages>` renders links do Nawigacja elementy podrzędne.

### atrybuty

| Atrybut | Typ | Domyślne | Znaczenie |
| --- | --- | --- | --- |
| `id` | ID strony lub empty ciąg znaków | bieżący strona | strona whose elementy podrzędne powinien be listed |
| `alphabetical` | wartość logiczna wyrażenie | `false` | Sort elementy podrzędne by tytuł zamiast tego of Nawigacja kolejność |

### Przykłady

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

Special case: `id=""` lists katalog główny Nawigacja nodes.

## `<Category>`

`<Category>` renders links do każdy strona in named category.

### atrybuty

| Atrybut | Typ | Domyślne | Znaczenie |
| --- | --- | --- | --- |
| `name` | ciąg znaków | none | Category nazwa do renderować |
| `rows` | positive liczba całkowita | `3` | liczba of wyświetlać kolumny in  MediaWiki-style układ |

````md
<Category name="machines" rows="3" />
````

Jeśli  category is missing, GuideNH renders inline błąd.

 ten sam category także ma auto-wygenerowany ukryty searchable strona at `Category:machines`.

## `<Special>`

`<Special>` renders jeden of  built-in MediaWiki-style special strona listings.

### atrybuty

| Atrybut | Typ | Domyślne | Znaczenie |
| --- | --- | --- | --- |
| `name` | ciąg znaków | none | Obsługiwane wartości: `AllPages`, `Categories` |
| `rows` | positive liczba całkowita | `3` | liczba of wyświetlać kolumny in  MediaWiki-style układ |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

 ten sam treść is także dostępny through  ukryty searchable strony `Special:AllPages` i `Special:Categories`.

## Tytuły wyników wyszukiwania

wyszukiwanie titles są derived in Ten kolejność:

1. `navigation.title`
2. pierwszy level-1 heading (`# Heading`)
3. raw ID strony

## Powiązane strony

- [Guide Page Format](Guide-Page-Format)
- [Search](Search)
- [Tags Reference](Tags-Reference)
