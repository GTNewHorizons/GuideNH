# Navigatie

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH builds its Navigatie tree van pagina frontmatter.

In De in-game sidebar, expanded ancestor pages stay pinned at De top terwijl their still-zichtbaar
descendants scrollen underneath. meerdere expanded ancestor levels kan stack at once, en elke sticky
rij is pushed away alleen wanneer its entire zichtbaar subtree scrolls out, similar naar De VSCode bestand
explorer.

## Navigatie-frontmatter

De `navigation` map controls whether Een pagina appears in De gids tree.

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### Veldreferentie

| Field | Description |
| --- | --- |
| `title` | Vereist weergeven title |
| `keyword` | optioneel enkele zoeken keyword of alias |
| `keywords` | optioneel lijst of Zoekwoorden of aliases |
| `parent` | optioneel bovenliggend pagina-ID, opgelost like Een gids­pagina link |
| `position` | optioneel sibling ordering hint |
| `recommend` | optioneel home-pagina recommendation priority; absent betekent De pagina is niet getoond in De Recommended panel |
| `priority` | optioneel Laadprioriteit voor zelfde-pad pagina overrides; Standaard `0` |
| `icon` | optioneel enkele item icon; ondersteunt inline `mod:item:meta:{snbt}` tails |
| `icons` | optioneel cycling item icons lijst; entries kan be plain item ids met optioneel inline `:{snbt}` of `{id, meta?, nbt?}` maps |
| `icon_texture` | optioneel texture icon opgelost van gids assets |
| `icon_textures` | optioneel cycling texture icon lijst |
| `required_mod` | optioneel enkele mod id; pagina is verborgen wanneer Deze mod is niet loaded |
| `required_mods` | optioneel lijst of mod ids; pagina is verborgen unless alle listed mods are loaded |
| `excluded_mod` | optioneel enkele mod id; pagina is verborgen wanneer Deze mod is loaded |
| `excluded_mods` | optioneel lijst of mod ids; pagina is verborgen wanneer any listed mod is loaded |

### `navigation.position`

`navigation.position` is Een optioneel geheel getal gebruikt naar order sibling pages in De Navigatie tree.

- Missing `position` defaults naar `0`.
- Larger waarden appear earlier.
- Als two pages have De zelfde waarde, they are sorted by title alphabetically.

### Zoekwoorden

`navigation.keyword` adds one zoeken alias. `navigation.keywords` adds Een lijst of aliases. Both fields
kan be gebruikt together; waarden are trimmed en duplicate waarden are ignored. Keyword matches gebruiken De zelfde
taal analyzer en prefix matching as title en pagina inhoud searches, terwijl results continue naar tonen
De normal Navigatie title.

```yaml
navigation:
  title: Molecular Assembler
  keyword: assembler
  keywords:
    - molecular assembler
    - autocrafting machine
```

## Aanbevelingen op de startpagina

### `navigation.recommend`

`navigation.recommend` is Een optioneel geheel getal gebruikt by De home pagina Recommended panel.

- Pages alleen appear in De Recommended panel wanneer Deze field is present.
- `0` is valid.
- Larger waarden appear earlier.
- Als two pages have De zelfde waarde, they are sorted by title alphabetically.
- De panel works at De `GuidePage` level, so elke recommended pagina entry jumps directly naar that pagina.

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Modvereisten

gebruiken `required_mod` of `required_mods` naar require one of more loaded mods. gebruiken `excluded_mod` of
`excluded_mods` naar verbergen Een pagina wanneer one of more incompatible mods are loaded. wanneer Een voorwaarde is niet
met, De pagina is excluded van De Navigatie tree en alle pagina indices (item, category, etc.) so it
kan niet be found through Navigatie of zoeken.

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

Both keys kan be combined; De pagina is alleen getoond wanneer iedere listed mod is present.

Vereist en excluded voorwaarden kan ook be combined. alle Vereist mods moet be loaded en none of De
excluded mods kan be loaded.

```yaml
navigation:
  title: Client-only Integration
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## Laadprioriteit

wanneer several loaded resource packs bieden De zelfde gids­pagina pad, GuideNH reads De pagina
frontmatter first en chooses De candidate met De highest `navigation.priority`.

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

Regels:

- missing `priority` is `0`
- waarden are signed Java integers up naar `2147483647`
- higher priority wins
- Als priorities are equal, De later processed resourcepack entry wins, matching Minecraft resource-pack override order
- priority alleen decides between candidates voor De zelfde pagina pad en taal/fallback layer

Deze is useful wanneer Een mod ships Een baseline gids­pagina en Een pack wants naar vervangen it zonder relying alleen on
resource-pack ordering.

## Pictogrambronnen

GuideNH chooses Navigatie/zoeken icons in Deze order:

1. `icon_textures` Als at least one texture entry is configured
2. `icon_texture` Als De texture bestand laadt successfully
3. `icons` Als at least one configured item resolves successfully
4. `icon` Als De item exists
5. no icon Als neither is usable

Texture icons are read van runtime assets, so relative pagina-lokale files such as `test1.png` work.

## Bovenliggende en hoofdknooppunten

- Omit `parent` naar maken Een root node.
- Stel in `parent: index.md` of any other pagina-ID naar maken Een kind node.
- De bovenliggend pagina moet exist in De zelfde gids Navigatie tree.

`navigation.parent` uses De zelfde namespace Regels as Markdown pagina links:

- `parent: index.md` en `parent: ./index.md` oplossen binnen De huidige pagina namespace.
- `parent: /index.md` resolves van De huidige pagina namespace root.
- `parent: gregtech:index.md` of `parent: gregtech:/index.md` explicitly targets another namespace.

Data-driven gidsen are isolated by namespace. Pages under `assets/guidenh/guidenh/_en_us/...` belong naar
`guidenh:guidenh`; pages under `assets/gregtech/guidenh/_en_us/...` belong naar `gregtech:guidenh`.
Relative parents en links never fall through naar another mod's zelfde-named pagina.

## Categoriepagina's

Pages kan join one of more named categories using frontmatter:

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

elke entry kan be either Een category naam of `category|sort key`.

Those categories are queryable through De built-in `<Category name="machines" rows="3" />` tag en ook auto-maken verborgen searchable pages such as `Category:machines`.
GuideNH ook auto-creates De verborgen searchable special pages `Special:AllPages` en `Special:Categories`.

## Item-geïndexeerde pagina's

Pages kan register item-naar-pagina mappings using `item_id` (one waarde) of `item_ids` (Een lijst):

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

Deze mappings are gebruikt by `<ItemLink>`. `item_id` is one NEI-style expressie en elke `item_ids`
entry is De zelfde kind of expressie. voor example, `minecraft:potion 16384-16462,!16386` matches a
metadata range except voor `16386`, terwijl `minecraft:potion 0-16,20-36,!28` combines two metadata ranges en
excludes `28`. `ae2:white_paint_ball:*` is Een compatible strict alle-meta mapping.

Een optioneel `#anchor` suffix scrolls naar Een specific heading wanneer De link is clicked.
De anchor is formed by lowercasing De heading tekst en replacing spaces met hyphens
(e.g. `## Crafting Recipe` → `#crafting-recipe`).

Lookup behavior:

1. exact item + exact meta
2. wildcard-meta fallback Als present
3. matching item expressie

## Ankerlinks voor koppen

GuideNH ondersteunt heading anchor Navigatie in Markdown links en `<a>` tags.
Anchors are derived van heading tekst by lowercasing en replacing spaces met hyphens.

**zelfde-pagina anchor:**

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

**Cross-pagina anchor:**

```md
[See Getting Started](./Guide-Page-Format#installation)
[Another guide](other-guide.md#usage)
```

**Absolute pad anchor** (using De gids namespace, avoids relative pad ambiguity in subdirectories):

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

De `namespace:path` format resolves naar De gids­pagina whose id matches `namespace:path`.
Deze is identical naar what relative paths oplossen naar, but avoids `../` Navigatie.
De pagina moet exist in De zelfde gids as De link source.

**Named inline anchors** kan ook be placed met `<a name="...">` in MDX:

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

Navigating naar Een link met Een anchor scrolls De gids naar De target heading of named anchor.

## `<SubPages>`

`<SubPages>` renders links naar Navigatie kinderen.

### attributen

| Attribuut | Type | Standaard | Betekenis |
| --- | --- | --- | --- |
| `id` | pagina-ID of empty tekenreeks | huidige pagina | pagina whose kinderen zou moeten be listed |
| `alphabetical` | boolean expressie | `false` | Sort kinderen by title instead of Navigatie order |

### Voorbeelden

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

Special case: `id=""` lists root Navigatie nodes.

## `<Category>`

`<Category>` renders links naar iedere pagina in Een named category.

### attributen

| Attribuut | Type | Standaard | Betekenis |
| --- | --- | --- | --- |
| `name` | tekenreeks | none | Category naam naar renderen |
| `rows` | positive geheel getal | `3` | getal of weergeven columns in De MediaWiki-style layout |

````md
<Category name="machines" rows="3" />
````

Als De category is missing, GuideNH renders Een inline error.

De zelfde category ook has Een auto-generated verborgen searchable pagina at `Category:machines`.

## `<Special>`

`<Special>` renders one of De built-in MediaWiki-style special pagina listings.

### attributen

| Attribuut | Type | Standaard | Betekenis |
| --- | --- | --- | --- |
| `name` | tekenreeks | none | Supported waarden: `AllPages`, `Categories` |
| `rows` | positive geheel getal | `3` | getal of weergeven columns in De MediaWiki-style layout |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

De zelfde inhoud is ook available through De verborgen searchable pages `Special:AllPages` en `Special:Categories`.

## Titels van zoekresultaten

zoeken titles are derived in Deze order:

1. `navigation.title`
2. first level-1 heading (`# Heading`)
3. raw pagina-ID

## Gerelateerde pagina's

- [Guide Page Format](Guide-Page-Format)
- [Search](Search)
- [Tags Reference](Tags-Reference)
