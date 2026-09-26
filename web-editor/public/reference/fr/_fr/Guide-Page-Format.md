# guide page Format


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

Les pages d’exécution de GuideNH sont des fichiers Markdown analysé avec:

- standard markdown bloc et inline syntaxe
- YAML frontmatter
- GFM tableaux
- strikethrough
- mark surligner avec `==text==`
- GuideNH inline underline extensions: `++text++` (straight underline), `^^text^^` (wavy underline), `::text::` (emphasis dots / dotted underline)
- MDX comments using `{/* ... */}`
- MDX-style personnalisé balises

## Pris en charge Markdown

GuideNH pages prise en charge Le courant markdown features utilisé dans Le Exemple guide:

- headings
- paragraphs
- inline emphasis, bold, strike, et code
- inline mark surligner (`==text==`)
- inline underline (`++text++`), wavy underline (`^^text^^`), et emphasis dots (`::text::`)
- liens et images
- literal autolinks pour direct URLs, `www.` hosts, et email addresses
- reference liens et reference images
- unordered et ordered lists
- GFM task lists
- blockquotes
- GitHub-style alert blockquotes comme `[!NOTE]`
- horizontal Règles
- fenced code blocs
- indented code blocs
- GFM tableaux
- footnotes
- lowercase HTML fragments comme `<a>`, `<br>`, `<kbd>`, `<sub>`, `<sup>`, et `<details>`
- MDX comments dans page texte

Voir `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` pour Un live sample page.

## surligner

utiliser `==text==` pour inline highlighted texte. utiliser `<mark color="#8A6A00">text</mark>` lorsque Un personnalisé surligner couleur est needed. Le par défaut mark arrière-plan est un dark golden yellow choisi vers Conservez white texte readable.

## Code blocs

exécution code blocs actuellement prise en charge:

- explicite fence languages comme `java`, `lua`, `scala`, `csv`, et `mermaid`
- automatique langue inference lorsque Le fence langue est omitted
- Un langue étiquette affiché au-dessus de Le bloc
- Un haut-droite copy button dans Le dans-game viewer
- lightweight exécution syntaxe highlighting pour Le detected langue

Exemple:

````md
```lua
local valeur = 42
print(valeur)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

Indented code blocs sont aussi Pris en charge:

````md
    print("indented code")
````

lorsque Un fenced bloc resolves vers `mermaid` et Le source est un Pris en charge `mindmap`, GuideNH renders it as Un interactive exécution mindmap à la placer of Un simple code bloc.

lorsque Un fenced bloc est explicitly marked as `csv`, GuideNH renders it as Un exécution tableau à la placer of Un simple code bloc. Si Le fence langue est omitted, CSV-shaped texte encore stays Un code bloc et seulement utilise CSV langue detection pour labeling/highlighting.

explicite CSV tableaux peut aussi provide colonne largeur hints:

````md
```csv widths=120,80
nom,valeur
iron,42
gold,17
```
````

Fence metadata aussi prend en charge `header=false` et quoted largeur lists:

````md
```csv widths="120,80" header=false
nom,valeur
iron,42
gold,17
```
````

direct GFM-style literal autolinks sont aussi Pris en charge dans normal paragraph texte:

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````

## Mermaid Mindmaps

GuideNH exécution Mermaid prise en charge est actuellement focused on `mindmap` diagrams:

- fenced ```` ```mermaid ```` blocs
- auto-detected mermaid code fences whose contenu starts avec `mindmap`
- explicite `<Mermaid>...</Mermaid>` balises
- explicite `<Mermaid src="./diagram.mmd" />` imports
- riche inline markdown labels dans Mermaid node texte
- facultatif `<NodeContent id="...">...</NodeContent>` enfants pour arbitrary exécution blocs dans correspondant nodes
- whole-diagram faire glisser-vers-pan interaction dans Le dans-game viewer
- `layout: tidy-tree` frontmatter dans Mermaid source
- courant mindmap node shapes comme square, rounded, circle, bang, cloud, et hexagon
- analysé `::icon(...)` et `:::class` metadata

Exemple:

````md
```mermaid
mindmap
  racine((GuideNH))
    exécution
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      faire glisser vers pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
Les nœuds d’exécution peuvent mélanger texte, liens et blocs.

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````

Mermaid diagrams que sont ne Pris en charge at exécution yet encore fall back vers regular Mermaid-labeled code blocs.

## CSV tableau Import

GuideNH aussi prend en charge exécution CSV fichier imports à travers Un explicite balise:

````md
<CsvTable src="./markdown-table.csv" />
````

Le `src` chemin resolves relatif vers Le actuel page, Le même way exécution ressource liens et scène `src` imports do.

Imported CSV tableaux peut aussi provide largeur hints:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

You peut aussi écrire Un CSV tableau inline avec Un explicite fence:

````md
```csv
nom,valeur
iron,42
gold,17
```
````

## Markdown tableau largeur Hints

Ordinary GFM markdown tableaux peut aussi provide exécution colonne largeur hints by adding Un trailing exécution attribut ligne immediately après Le tableau:

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

ceci keeps Le tableau itself standard markdown pendant letting GuideNH apply exécution-seulement préféré colonne widths.

## Task Lists, Alerts, et Footnotes

GuideNH exécution aussi prend en charge several useful GFM-style behaviors:

- task lists using `- [ ]` et `- [x]`
- GitHub alert blockquotes comme `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, et `[!CAUTION]`
- footnote references et definitions

Exemple:

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````

Footnote references rendent as tooltip-style inline markers, et GuideNH appends Un compact exécution footnote liste près Le bas of Le page.

## liste largeur Customization

standard markdown lists do ne define largeur controls, but GuideNH exécution containers peut constrain eux:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

ceci est actuellement Le recommended way vers customize liste ligne largeur at exécution.

## Reference liens et images

GuideNH prend en charge CommonMark reference definitions:

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## Lowercase HTML exécution balises

GuideNH exécution prend en charge Un focused subset of lowercase HTML-style balises directly:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````

autre brut HTML fragments encore fall back vers literal texte-style handling à la placer of browser-grade HTML rendu.

## MDX Comments

GuideNH prend en charge Le MDX comment form et ignores it avant markdown compilation:

````md
Texte visible. {/* commentaire intégré masqué */}

{/*
multiline comment
*/}

More visible text.
````

## Frontmatter

GuideNH reads Le premier YAML frontmatter bloc et parses these known clés:

| clé | type | Meaning |
| --- | --- | --- |
| `navigation` | map | Adds Le page vers Le navigation tree |
| `categories` | liste of strings | Adds Le page vers MediaWiki-style categories; chaque entrée peut optionally utiliser `category|sort key` |
| `item_id` | élément filtre expression | Un unique NEI-style élément expression que makes Le page discoverable by `<ItemLink>` |
| `item_ids` | liste of élément filtre expressions | liste form of `item_id`; quelconque correspondant expression makes Le page discoverable by `<ItemLink>` |
| `ore_ids` | liste of ore dictionary names | Makes Le page discoverable by ore-dictionary éléments (e.g. `ingotIron`, `oreCopper`) |
| `quest_ids` | liste of BetterQuesting quest ids | Makes Le page discoverable by `<QuestLink>` / `<QuestCard>` et by Le open-guide hotkey lorsque Un quest est hovered dans Le BQ GUI. Accepts canonical UUID strings et BetterQuesting's compact Base64 form. seulement consumed lorsque BetterQuesting est chargé. Voir [Mod Compatibility](Mod-Compatibility) |
| `author` | chaîne | unique author nom. Displayed dans Le bas bar. |
| `authors` | liste of strings ou `{name: ...}` maps | plusieurs author names. At most deux sont displayed; additional ones sont remplacé avec `...`. Takes precedence sur `author` Si les deux sont present. |
| `date` | chaîne ou YYYY-MM-DD date | contenu creation date. Displayed dans Le bas bar. |
| `updated` | chaîne ou YYYY-MM-DD date | dernier mis à jour date. Displayed dans Le bas bar. |
| `zoom` | positive flottant | Per-page contenu zoom multiplier (e.g. `1.5` = 150 %). Multiplied avec Le global `contentZoom` setting dans ModConfig. par défaut `1.0`. |
| quelconque autre clé | quelconque YAML valeur | Preserved dans `additionalProperties` pour extensions ou tooling |

### `navigation`

| champ | obligatoire | type | Remarques |
| --- | --- | --- | --- |
| `title` | oui | chaîne | afficher nom dans navigation et recherche titre repli |
| `keyword` | non | chaîne | un additional recherche keyword ou alias; prend en charge prefix correspondant |
| `keywords` | non | liste of strings | Additional recherche keywords ou aliases; valeurs sont combined et deduplicated |
| `parent` | non | page id | parent page id; omitted signifie haut-level node |
| `position` | non | entier | Sibling sort ordre; par défaut `0`, larger valeurs appear earlier |
| `priority` | non | entier | charger priority pour même-chemin page remplace; par défaut `0`, higher wins, equal priority lets Le later ressource pack entrée win |
| `icon` | non | élément id | élément icon affiché dans navigation/recherche. Accepts `modid:name`, `modid:name:meta`, ou `modid:name:meta:{snbt}`. Le inline SNBT tail est Le préféré way vers attach NBT comme Un personnalisé afficher nom. |
| `icons` | non | liste of élément ids | liste of élément icons pour animated cycling (un per second). chaque entrée utilise Le même syntaxe as `icon`, including inline `:{snbt}` tails. lorsque present takes priority sur `icon`. |
| `icon_texture` | non | ressource chemin | Texture icon chemin résolu like quelconque autre ressource lien |
| `icon_textures` | non | liste of ressource paths | liste of texture icons pour animated cycling (un per second). lorsque present takes priority sur `icon_texture`. |
| `required_mod` | non | mod id | masque Le page sauf ceci mod est chargé. |
| `required_mods` | non | liste of mod ids | masque Le page sauf chaque listed mod est chargé. |
| `excluded_mod` | non | mod id | masque Le page lorsque ceci mod est chargé. |
| `excluded_mods` | non | liste of mod ids | masque Le page lorsque quelconque listed mod est chargé. |

### Exemple Frontmatter

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

`categories` accepts l’un ou l’autre simple category names ou `category|sort key` entrées.
utiliser `<Category name="examples" rows="3" />` vers rendent Un category listing bloc, et
`<Special name="SpecialPages" rows="3" />` vers embed Le généré MediaWiki-style special-page index.

pour BetterQuesting integration, `quest_ids` accepts l’un ou l’autre of these formats:

- canonical UUID strings comme `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting compact quest ids comme `AAAAAAAAAAAAAAAAAAAMug==`

Do ne liste les deux forms pour Le même quest dans un page's `quest_ids`; Elles normalize vers Le même interne UUID et would être treated as duplicates.

lorsque quelconque of `author`, `authors`, `date`, ou `updated` est present, GuideNH affiche a
bas bar dans Le guide écran (correspondant Le haut toolbar style) avec droite-aligned
texte like: *contenu depuis MyMod, Author ExampleAuthor, Date 2024-01-15, mis à jour 2024-06-01*.

plusieurs authors Exemple:
```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```
ou avec structured entrées:
```yaml
authors:
  - name: Alice
  - name: Bob
```

### `zoom`

Le `zoom` clé lets you enlarge ou shrink Le contenu of Un unique page sans
affecting quelconque autre page. Le valeur est un positive flottant treated as Un multiplier:

| Exemple valeur | Effect |
| --- | --- |
| `1.0` (par défaut) | normal taille |
| `1.5` | 150 % — contenu 50 % larger |
| `0.75` | 75 % — contenu 25 % smaller |

Le per-page zoom est multiplied avec Le global **contentZoom** slider dans
ModConfig → GuideNH → UI. ceci lets server packs Définissez Un sensible baseline pendant
encore allowing individual pages vers fine-tune mise en page pour narrow ou wide contenu.

Exemple: Définissez ceci page vers afficher at 150 % of Le base zoom:

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```

page mise en page est recomputed at Le zoom-adjusted largeur, so texte wrapping et tous
bloc géométrie remain correct at quelconque zoom level.

## lien Resolution

GuideNH resolves ids et paths using these Règles:

### page liens

| entrée | Meaning |
| --- | --- |
| `subpage.md` | relatif vers Le actuel page, dans Le actuel page namespace |
| `./subpage.md` | relatif vers Le actuel page, dans Le actuel page namespace |
| `/guide.md` | rooted vers Le actuel page namespace, equivalent vers `currentmod:guide.md` |
| `gregtech:guide.md` | explicite namespace; opens `gregtech:guidenh` lorsque Le actuel guide chemin est `guidenh` |
| `gregtech:/guide.md` | explicite namespace plus rooted chemin, normalized vers `gregtech:guide.md` |
| `subpage.md#anchor` | page plus anchor fragment |
| `guidenh:other.md#anchor` | explicite `modid:path#anchor` |
| `https://example.com` | externe HTTP/HTTPS lien |

page liens sont isolated by namespace. Un lien written depuis `assets/guidenh/guidenh/_en_us/index.md` as
`[Guide](guide.md)` resolves vers `guidenh:guide.md`; Le même texte dans
`assets/gregtech/guidenh/_en_us/index.md` resolves vers `gregtech:guide.md`. Si que page est manquant dans Le
actuel namespace, GuideNH reports it as Un broken lien à la placer of falling back vers another mod's page.

explicite `modid:path` liens peut cross depuis un mod's données-driven guide vers another. Le guide id est derived depuis
Le cible page namespace et Le actuel guide chemin, so Un lien depuis `guidenh:guidenh` vers `gregtech:guide.md`
opens page `gregtech:guide.md` dans guide `gregtech:guidenh`.

Anchor fragments défiler Le guide vers Un heading whose texte lowercased et spaces remplacé avec hyphens
correspond Le fragment (e.g. `#crafting-recipe` scrolls vers `## Crafting Recipe`), ou vers a `<a name="...">` anchor.

### ressource liens

Assets utiliser Le même resolution Règles as liens. pour Exemple:

- `test1.png` resolves relatif vers Le actuel page fichier.
- `/assets/example_structure.snbt` resolves vers Le guide's ressource racine.
- `guidenh:textures/gui/example.png` resolves as Un explicite ressource location.

## élément Reference syntaxe

Navigation `icon` et `icons`, along avec balises que acceptent an élément id, utiliser ordinary élément references:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Un omitted `meta` defaults vers `0`. Un SNBT tail starts at Le premier `{` et est analysé as élément NBT. Where wildcard
metadata est Pris en charge, `*` peut être combined avec Le SNBT tail.

Exemples:

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### élément Index Expressions

`item_id` accepts un NEI-style expression et `item_ids` accepts Un YAML liste of Le même expressions. chaque
`item_ids` entrée est independent; Le page est linked lorsque quelconque entrée correspond. Whitespace combines terms, `|`
combines alternatives, et `,` combines Règles dans un term.

- `minecraft:lava` performs Un case-insensitive partial registry-id correspondre, so it aussi correspond `minecraft:lava_bucket`.
- `<minecraft:wool:14>` strictly correspond un élément et meta valeur.
- `ae2:white_paint_ball:*`, `:32767`, et uppercase meta tokens comme `:ANY` sont compatible strict tous-meta forms.
- `16384-16462,!16386` correspond Un metadata range pendant excluding `16386`.
- `!minecraft:portal` excludes Un correspondant registry id.
- `r/^m\\w{6}ft$/` utilise Un Java regular expression against Le registry id.

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

dans ceci Exemple, whitespace combines conditions, `,` combines Règles dans un term, `!` excludes Un correspondre, et
`|` separates alternatives. Le premier expression donc accepts potion metadata depuis `16384` à travers `16462`
except `16386`, ou quelconque metadata of `ae2:white_paint_ball`. Le second expression demonstrates Un wildcard élément
reference dans Un comma-separated expression avec Un reverse (`!`) règle. Le dernier deux entrées afficher Un metadata union
avec `28` excluded et an élément mapping que opens Le `Usage` heading anchor.

Un facultatif `#anchor` suffix opens Un correspondant page at Un heading anchor. Exact élément et explicite-meta mappings utiliser
Le direct index premier; expressions sont evaluated seulement lorsque needed.

## erreur Handling

Si Un page fails vers analyser, GuideNH crée Un erreur page à la placer of crashing Le guide. invalide balises, ids, et attributs sont reported inline as guide-rendu erreur texte.

## Related Pages

- [Navigation](Navigation)
- [Images And Assets](Images-And-Assets)
- [Tags Reference](Tags-Reference)
