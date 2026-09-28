# Navigation


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH erstellt seine navigation tree von Seite frontmatter.

in Die in-game sidebar, expanded ancestor pages stay pinned at Die oben während ihre weiterhin-sichtbar
descendants scrollen underneath. mehrere expanded ancestor levels kann stack at once, und jede sticky
Zeile ist pushed away nur wenn seine entire sichtbar subtree scrolls out, similar zu Die VSCode Datei
explorer.

## Navigations-Frontmatter

Die `navigation` map controls whether Eine Seite appears in Die Leitfaden tree.

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### Feldreferenz

| Feld | Beschreibung |
| --- | --- |
| `title` | erforderlich anzeigen Titel |
| `keyword` | optional einzeln Suche keyword oder alias |
| `keywords` | optional Liste von Suche keywords oder aliases |
| `parent` | optional übergeordnet Seite id, aufgelöst like Eine Leitfaden Seite Link |
| `position` | optional sibling ordering hint |
| `recommend` | optional home-Seite recommendation priority; absent bedeutet Die Seite ist nicht angezeigt in Die Recommended panel |
| `priority` | optional Ladepriorität für gleich-Pfad Seite überschreibt; Standard `0` |
| `icon` | optional einzeln Element icon; unterstützt inline `mod:item:meta:{snbt}` tails |
| `icons` | optional cycling Element icons Liste; Einträge kann sein einfach Element ids mit optional inline `:{snbt}` oder `{id, meta?, nbt?}` maps |
| `icon_texture` | optional texture icon aufgelöst von Leitfaden assets |
| `icon_textures` | optional cycling texture icon Liste |
| `required_mod` | optional einzeln mod id; Seite ist verborgen wenn dies mod ist nicht geladen |
| `required_mods` | optional Liste von mod ids; Seite ist verborgen außer alle listed mods sind geladen |
| `excluded_mod` | optional einzeln mod id; Seite ist verborgen wenn dies mod ist geladen |
| `excluded_mods` | optional Liste von mod ids; Seite ist verborgen wenn beliebig listed mod ist geladen |

### `navigation.position`

`navigation.position` ist ein optional Ganzzahl verwendet zu Reihenfolge sibling pages in Die navigation tree.

- fehlend `position` Standards zu `0`.
- Larger Werte appear earlier.
- Wenn zwei pages have Die gleich Wert, Sie sind sorted by Titel alphabetically.

### Suche Keywords

`navigation.keyword` adds eins Suche alias. `navigation.keywords` adds Eine Liste von aliases. beide Felder
kann sein verwendet together; Werte sind trimmed und duplicate Werte sind ignored. Keyword stimmt überein verwenden Die gleich
Sprache analyzer und prefix passend als Titel und Seite Inhalt Suchees, während results continue zu anzeigen
Die normal navigation Titel.

```yaml
navigation:
  title: Molecular Assembler
  keyword: assembler
  keywords:
    - molecular assembler
    - autocrafting machine
```

## Home Seite Recommendations

### `navigation.recommend`

`navigation.recommend` ist ein optional Ganzzahl verwendet by Die home Seite Recommended panel.

- Pages nur appear in Die Recommended panel wenn dies Feld ist present.
- `0` ist gültig.
- Larger Werte appear earlier.
- Wenn zwei pages have Die gleich Wert, Sie sind sorted by Titel alphabetically.
- Die panel works at Die `GuidePage` level, so jede recommended Seite Eintrag jumps directly zu dass Seite.

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Mod-Anforderungen

verwenden `required_mod` oder `required_mods` zu require eins oder mehr geladen mods. verwenden `excluded_mod` oder
`excluded_mods` zu ausblenden Eine Seite wenn eins oder mehr incompatible mods sind geladen. wenn Eine Bedingung ist nicht
met, Die Seite ist excluded von Die navigation tree und alle Seite indices (Element, category, etc.) so it
kann nicht sein found durch navigation oder Suche.

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

beide Schlüssel kann sein kombiniert; Die Seite ist nur angezeigt wenn jede listed mod ist present.

erforderlich und excluded Bedingungen kann auch sein kombiniert. alle erforderlich mods muss sein geladen und none von Die
excluded mods kann sein geladen.

```yaml
navigation:
  title: Client-only Integration
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## Ladepriorität

wenn several geladen Ressource packs provide Die gleich Leitfaden Seite Pfad, GuideNH reads Die Seite
frontmatter erste und chooses Die candidate mit Die highest `navigation.priority`.

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

Regeln:

- fehlend `priority` ist `0`
- Werte sind signed Java integers up zu `2147483647`
- higher priority wins
- Wenn priorities sind equal, Die later processed Ressource pack Eintrag wins, passend Minecraft Ressource-pack Überschreibung Reihenfolge
- priority nur decides zwischen candidates für Die gleich Seite Pfad und Sprache/Fallback layer

dies ist useful wenn Eine mod ships Eine baseline Leitfaden Seite und Eine pack wants zu ersetzen it ohne relying nur auf
Ressource-pack ordering.

## Symbolquellen

GuideNH chooses navigation/Suche icons in dies Reihenfolge:

1. `icon_textures` Wenn at least eins texture Eintrag ist configured
2. `icon_texture` Wenn Die texture Datei lädt successfully
3. `icons` Wenn at least eins configured Element resolves successfully
4. `icon` Wenn Die Element exists
5. nein icon Wenn neither ist usable

Texture icons sind lesen von Laufzeit assets, so relativ Seite-lokal files such als `test1.png` funktionieren.

## übergeordnet und Wurzel Knotens

- Omit `parent` zu erstellen Eine Wurzel Knoten.
- Setzen Sie `parent: index.md` oder beliebig andere Seite id zu erstellen Eine untergeordnetes Element Knoten.
- Die übergeordnet Seite muss exist in Die gleich Leitfaden navigation tree.

`navigation.parent` verwendet Die gleich namespace Regeln als Markdown Seite Links:

- `parent: index.md` und `parent: ./index.md` auflösen innerhalb Die aktuell Seite namespace.
- `parent: /index.md` resolves von Die aktuell Seite namespace Wurzel.
- `parent: gregtech:index.md` oder `parent: gregtech:/index.md` explicitly targets ananderer Namespace.

Daten-driven Guides sind isolated by namespace. Pages under `assets/guidenh/guidenh/_en_us/...` belong zu
`guidenh:guidenh`; pages under `assets/gregtech/guidenh/_en_us/...` belong zu `gregtech:guidenh`.
relativ parents und Links nie fall durch zu another mod's gleich-named Seite.

## Kategorieseiten

Pages kann join eins oder mehr named categories using frontmatter:

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

jede Eintrag kann sein entweder Eine category Name oder `category|sort key`.

Those categories sind queryable durch Die built-in `<Category name="machines" rows="3" />` Tag und auch auto-erstellen verborgen Sucheable pages such als `Category:machines`.
GuideNH auch auto-erstellt Die verborgen Sucheable special pages `Special:AllPages` und `Special:Categories`.

## Element-Indexed Pages

Pages kann registrieren Element-zu-Seite mappings using `item_id` (eins Wert) oder `item_ids` (Eine Liste):

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

diese mappings sind verwendet by `<ItemLink>`. `item_id` ist eins NEI-style Ausdruck und jede `item_ids`
Eintrag ist Die gleich kind von Ausdruck. für Beispiel, `minecraft:potion 16384-16462,!16386` stimmt überein ein
metadata range außer für `16386`, während `minecraft:potion 0-16,20-36,!28` combines zwei metadata ranges und
excludes `28`. `ae2:white_paint_ball:*` ist ein compatible strict alle-meta mapping.

Eine optional `#anchor` suffix scrolls zu Eine bestimmten heading wenn Die Link ist clicked.
Die anchor ist formed by lowercasing Die heading Text und replacing spaces mit hyphens
(e.g. `## Crafting Recipe` → `#crafting-recipe`).

Lookup behavior:

1. exact Element + exact meta
2. wildcard-meta Fallback Wenn present
3. passend Element Ausdruck

## Ankerlinks für Überschriften

GuideNH unterstützt heading anchor navigation in Markdown Links und `<a>` Tags.
Anchors sind derived von heading Text by lowercasing und replacing spaces mit hyphens.

**gleich-Seite anchor:**

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

**Cross-Seite anchor:**

```md
[See Getting Started](./Guide-Page-Format#installation)
[Another guide](other-guide.md#usage)
```

**absolut Pfad anchor** (using Die Leitfaden namespace, avoids relativ Pfad ambiguity in subdirectories):

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

Die `namespace:path` format resolves zu Die Leitfaden Seite whose id stimmt überein `namespace:path`.
dies ist identical zu what relativ paths auflösen zu, but avoids `../` navigation.
Die Seite muss exist in Die gleich Leitfaden als Die Link Quelle.

**Named inline anchors** kann auch sein placed mit `<a name="...">` in MDX:

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

Navigating zu Eine Link mit Eine anchor scrolls Die Leitfaden zu Die Ziel heading oder named anchor.

## `<SubPages>`

`<SubPages>` rendert Links zu navigation untergeordnete Elemente.

### Attribut

| Attribut | Typ | Standard | Bedeutung |
| --- | --- | --- | --- |
| `id` | Seite id oder leer Zeichenkette | aktuell Seite | Seite whose untergeordnete Elemente sollte sein listed |
| `alphabetical` | Boolesch Ausdruck | `false` | Sort untergeordnete Elemente by Titel stattdessen von navigation Reihenfolge |

### Beispiele

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

Special case: `id=""` lists Wurzel navigation Knotens.

## `<Category>`

`<Category>` rendert Links zu jede Seite in Eine named category.

### Attribut

| Attribut | Typ | Standard | Bedeutung |
| --- | --- | --- | --- |
| `name` | Zeichenkette | none | Category Name zu rendern |
| `rows` | positive Ganzzahl | `3` | Zahl von anzeigen columns in Die MediaWiki-style Layout |

````md
<Category name="machines" rows="3" />
````

Wenn Die category ist fehlend, GuideNH rendert Eine inline Fehler.

Die gleich category auch hat Eine auto-generiert verborgen Sucheable Seite at `Category:machines`.

## `<Special>`

`<Special>` rendert eins von Die built-in MediaWiki-style special Seite listings.

### Attribut

| Attribut | Typ | Standard | Bedeutung |
| --- | --- | --- | --- |
| `name` | Zeichenkette | none | unterstützt Werte: `AllPages`, `Categories` |
| `rows` | positive Ganzzahl | `3` | Zahl von anzeigen columns in Die MediaWiki-style Layout |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

Die gleich Inhalt ist auch verfügbar durch Die verborgen Sucheable pages `Special:AllPages` und `Special:Categories`.

## Suche Ergebnis Titles

Suche titles sind derived in dies Reihenfolge:

1. `navigation.title`
2. erste level-1 heading (`# Heading`)
3. roh Seite id

## Verwandte Seiten

- [Guide Page Format](Guide-Page-Format)
- [Search](Search)
- [Tags Reference](Tags-Reference)
