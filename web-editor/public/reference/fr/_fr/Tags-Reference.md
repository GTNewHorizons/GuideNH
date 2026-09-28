# balises Reference


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

ceci page lists Le built-dans exécution balises enregistré by `DefaultExtensions`.

## Usage Règles

- balises peut appear l’un ou l’autre dans bloc context ou inline context depending on Le compilateur.
- MDX comments using `{/* ... */}` sont Pris en charge dans page contenu et sont ignored by Le exécution analyseur.
- invalide balises ou invalide attributs rendent guide errors inline à la placer of silently failing.
- Large feature balises comme recipes et 3D scènes sont documented dans leurs propre pages:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## Inline et Flow balises

| balise | Purpose | clé attributs |
| --- | --- | --- |
| `<a>` | interne/externe lien et facultatif anchor nom | `href`, `title`, `name` |
| `<br>` | ligne break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style texte | none |
| `<sup>` | smaller inline superscript-style texte | none |
| `<Color>` | colored inline texte | `id` ou `color` |
| `<Spoiler>` | masquées inline texte revealed on survol | none |
| `<Tooltip>` | riche survol tooltip avec markdown/balise enfants | `label` |
| `<SoundLink>` | clickable riche-texte sound trigger | `sound` ou `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted texte; equivalent vers `==text==` avec facultatif couleur control | `color` |
| `<PlayerName>` | inserts actuel player username | none |
| `<KeyBind>` | inserts keybinding afficher nom | `id` ou `action` |
| `<ItemImage>` | inline élément icon | `id` ou `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | élément tooltip + facultatif navigation lien | `id` ou `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command lien | `command`, `title`, `close` |
| `<Latex>` | LaTeX math formule; inline dans flow context, centered afficher bloc dans bloc context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest lien avec état-aware styling (compat balise, seulement enregistré lorsque BetterQuesting est chargé) | `id`, `text`, `show_tooltip` |

Inline markdown aussi prend en charge action liens pour sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## bloc balises

| balise | Purpose | clé attributs |
| --- | --- | --- |
| `<div>` | pass-à travers bloc wrapper | none |
| `<ContentTabs>` | groups alternative riche contenu under independent tabs | `title`, `color`, `icon`, `iconPng`, `icon_png`, `iconItem`, `icon_item`, `default`, `defaultIndex` |
| `<Tab>` | un contenu panel dans `<ContentTabs>` | `title` |
| `<details>` | collapsible exécution bloc | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | répertoire-style outline avec connector lignes | `indent`, `gap` |
| `<Row>` | horizontal flex mise en page | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertical flex mise en page | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | largeur-constrained footnote container utilisé by exécution markdown footnotes | `width` |
| `<ItemGrid>` | compact grid of élément icons | enfants doit être `<ItemIcon id="..."/>` ou `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-interactive 3D unique-bloc aperçu | `id` ou `ore`, facultatif `scale` (defaults vers `4`), `float`, `perspective`, `nbt` |
| `<FloatingImage>` | cropped image bloc avec flottant ou true inline placement | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | navigation enfant listing | `id`, `alphabetical` |
| `<Category>` | liste pages depuis Un category | `name`, `rows` |
| `<Special>` | liste built-dans MediaWiki special pages | `name`, `rows` |
| `<Structure>` | 2.5D isometric bloc mise en page view | `width`, `height` |
| `<Mermaid>` | exécution Mermaid graphique import/inline | `src`, `width`, `height` |
| `<CsvTable>` | exécution CSV fichier import tableau | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered colonne graphique | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar graphique | même as `<ColumnChart>` |
| `<LineChart>` | ligne graphique avec categorical ou numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie graphique | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter graphique | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-courbe fonction graphique | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | unique-courbe shorthand pour `<FunctionGraph>` | `expr`, plus tous `<FunctionGraph>` panel attributs |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | Voir [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D guide scène | Voir [GameScene](GameScene) |
| `<QuestCard>` | bloc-level BetterQuesting quest summary card (compat balise, seulement enregistré lorsque BetterQuesting est chargé) | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` est un `<GameScene>` enfant balise. It accepts `controller`, `piece`, `facing`,
`rotation`, `flip`, et `channel` attributs, et aussi prend en charge StructureLib par défaut enfant balises:
`<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`,
`<GregTechActiveController>`, et `<GregTechPlaceHatches>`.

## balise Details

### `<a>`

Acts like Un HTML-style anchor balise:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` peut être relatif, rooted, explicite `modid:path`, ou HTTP/HTTPS
- `title` devient Le tooltip
- `name` inserts Un page anchor cible

### `<br>`

GuideNH aussi prend en charge Un MDX break balise avec flottant clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` valeurs:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, et `<sup>`

GuideNH exécution prend en charge Un focused subset of lowercase documentation balises pour inline utiliser:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

crée Un collapsible exécution bloc avec Un summary ligne. Le `<summary>` ligne prend en charge normal
inline markdown/balise contenu, et Le corps peut hold ordinary texte plus arbitrary bloc balises comme
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, tableaux, charts, et mise en page containers.
lorsque `height` est Définissez, seulement Le corps scrolls; Le summary ligne et outer frame stay fixe.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

Texte masqué par défaut avec un [lien de page normal](./index.md).

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

attributs:

- `open` — starts expanded lorsque present
- `width` — préféré outer largeur dans pixels
- `height` — préféré corps viewport hauteur dans pixels; overflow devient scrollable dans-game et dans site export
- `wrap` — prend en charge Le usual bloc embedding modes comme `square`, `tight`, et `through`
- `align` — `left`, `center`, ou `right`; lorsque combined avec Un floating wrap mode, Le whole details bloc floats

### `<ContentTabs>`

Groups alternative riche contenu under independent tabs. seulement direct `<Tab>` enfants sont valide. Le container itself peut aussi rendent Un quote-style heading ligne au-dessus de Le tabs, correspondant Le visual langue of markdown callouts.

````mdx
<ContentTabs
  title="Implementation Options"
  icon="</>"
  color="#4f8cff"
  default="Java"
>
  <Tab title="Java">
    ```java
    System.out.println("Hello GuideNH");
    ```
  </Tab>
  <Tab title="Scene">
    <BlockImage id="minecraft:crafting_table" />
  </Tab>
</ContentTabs>
````

- `default` correspond Le premier tab whose `title` correspond exactement
- `defaultIndex` est zero-based et wins sur `default` lorsque les deux sont present
- `color` optionally remplace Le gauche accent ligne et sélectionné-tab surligner avec `#RRGGBB` ou `#AARRGGBB`
- `title` adds Un facultatif simple-texte heading au-dessus de Le tab strip
- `icon`, `iconPng` / `icon_png`, et `iconItem` / `icon_item` utiliser Le même heading icon semantics as markdown quote-style callouts
- invalide enfants ou invalide defaults rendent visible author-facing errors

### `<FileTree>`

Renders Un répertoire-style outline avec real connector lignes drawn depuis Le prefix glyphs on chaque ligne. les deux Unicode box-drawing (`│ ├ └ ─`) et ASCII (`| +-- \-- ` / four spaces) forms sont accepted et peut être mixed. Payload texte prend en charge Le usual inline markdown (liens, **bold**, `code`, …), et those liens sont clickable les deux dans-game et dans Le built-dans site export. Le même contenu peut aussi être written as Un fenced ` ```tree ` or ` ```filetree ` bloc.

````md
<FileTree indent="14" gap="0">
project
├── src
│   ├── **main**
│   │   └── [App.java](./index.md)
│   └── *test*
└── `README.md`
</FileTree>
````

facultatif per-ligne icons sont introduced by Un leading directive on Le payload:

- `{:icon=Text}` — short texte étiquette (unique ou double quotes facultatif)
- `{:iconPng=path/to/file.png}` — PNG ressource résolu against Le actuel page
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft élément icon. Le facultatif `meta` segment est un damage valeur (ou `*` pour Un wildcard); Un facultatif trailing `:{snbt}` bloc carries SNBT vers attach vers Le stack.

````md
```filetree
monde
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} sample ressource
```
````

attributs:

- `indent` — pixels per depth level (par défaut `14`)
- `gap` — extra pixels entre lignes (par défaut `0`)

### Citations à l’exécution

normal markdown blockquotes rendent at exécution avec Un gauche accent ligne. GitHub alert syntaxe est Pris en charge:

````md
> [!NOTE]
> Alert body
````

GuideNH aussi prend en charge Un exécution-seulement personnalisé directive on Le premier quoted ligne:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

Pris en charge directive clés:

- `title`
- `color`
- `icon` pour simple texte symbols
- `iconItem` pour an `ItemStack` id
- `iconPng` pour Un guide ressource png chemin

seulement un icon source devrait être fourni.

### `<Color>`

utiliser l’un ou l’autre Un symbolic couleur id ou Un explicite hex valeur:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

Use `<Spoiler>` for inline hidden text. The content stays rich-text aware, so nested markdown and runtime inline tags
such as `<Color>` still render correctly after reveal.

```mdx
<Spoiler>masquées **bold** texte avec <Color color="#55ccff">tint</Color>.</Spoiler>
<Spoiler>[Anchor Link](#headings) encore behaves like Un normal hoverable lien lorsque revealed.</Spoiler>
```

````

Règles:

- `id` et `color` sont mutually exclusive dans practice; provide un
- `color` accepts `#RRGGBB`, `#AARRGGBB`, ou `transparent`

### `<Tooltip>`

crée underlined texte que opens Un riche contenu tooltip on survol.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

Si `label` est omitted, Le trigger texte defaults vers `tooltip`.

### `<SoundLink>` et Sound Action liens

`<SoundLink>` renders riche inline contenu que plays Un sound lorsque clicked. It fait ne navigate,
et son personnalisé clic sound replaces Le normal guide clic sound pour que clic.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound attributs:

- `sound` est un sound event id comme `modid:event.name`
- `src` points at an `.ogg` fichier; `modid:sounds/machine/start.ogg` devient `modid:machine.start`
- `volume` defaults vers `1.0`
- `pitch` defaults vers `1.0`
- `cooldown` est milliseconds entre repeated plays, par défaut `250`
- `radius` et `minVolume` control écran-espace attenuation lorsque utilisé dans scènes

### `<PlayerName>`

Inserts Le actuel Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up Un keybinding by id ou action et renders Le player's actuel bound clé nom.

Accepted ids:

- Le binding description id, comme `key.jump` ou `key.guidenh.open_guide`
- Le legacy `category.description` form, comme `key.categories.movement.key.jump`

Exemple:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX Comments

GuideNH ignores MDX comments dans page contenu:

````md
Texte visible. {/* commentaire intégré masqué */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH aussi ignores explicite `<Comment>` balises:

````md
Texte visible. <Comment>Ce contenu ne sera pas rendu.</Comment> Reste visible.
````

### `<ItemImage>`

affiche Un inline élément icon.

| attribut | Meaning |
| --- | --- |
| `ore` | ore dictionary nom; Le premier correspondre wins |
| `id` | élément reference utilisé lorsque `ore` est absent |
| `nbt` | facultatif SNBT élément données; merged onto quelconque inline SNBT dans `id` |
| `scale` | flottant, par défaut `1` |
| `noTooltip` | truthy chaîne ou vide attribut suppresses tooltip (legacy; prefer `showTooltip`) |
| `showTooltip` | booléen, par défaut `true`; `false` suppresses Le survol tooltip |
| `showIcon` | booléen, par défaut `true`; `false` masque Le élément icon graphic |
| `label` | `left` ou `right` — affiche Le élément afficher nom as texte on Le specified côté of Le icon; omit pour non étiquette |
| `format` | format pattern pour Le étiquette texte; prend en charge Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) avec facultatif `%s` placeholder pour Le élément nom; par défaut (non attribut) renders Le nom dans italic |
| `yOffset` | entier pixel offset remplacer pour Le **icon** at scale `1`; fait ne affect Le étiquette texte |
| `labelYOffset` | entier pixel offset remplacer pour Le **étiquette texte** at scale `1`; fait ne affect Le icon |

Remarques:

- `ore` takes precedence sur `id` lorsque les deux sont fourni
- Si GregTech est installed, Le sélectionné ore correspondre est passed à travers `GTOreDictUnificator.setStack(...)`
- `label` requires at least un of `showIcon` ou `label` vers produce visible sortie; setting les deux `showIcon="false"` et omitting `label` renders nothing
- `format` seulement s’applique lorsque `label` est Définissez; Si `format` a non `%s`, Le literal format texte est utilisé as Le étiquette
- inline SNBT dans `id` remains Pris en charge; lorsque les deux forms sont present, Le standalone `nbt` attribut est merged dernier et remplace conflicting clés

Exemple:

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
<ItemImage id="minecraft:diamond" label="right" />
<ItemImage id="minecraft:iron_ingot" label="left" format="**%s**" />
<ItemImage id="minecraft:book" showIcon="false" label="right" format="~~%s~~" />
<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<ItemLink>`

crée Un texte lien using Le élément's afficher nom et élément tooltip. Si `item_ids` points vers Un guide page, clic navigates vers it. `ore` peut être utilisé vers résoudre Le afficher stack depuis Le premier ore dictionary correspondre à la placer of Un fixe registry id.

| attribut | par défaut | Meaning |
| --- | --- | --- |
| `id` | — | élément registry id, e.g. `minecraft:compass` ou `minecraft:wool:1` |
| `ore` | — | ore-dictionary nom; utilise Le premier correspondant élément stack |
| `linksTo` | *(auto)* | remplace Le lien cible; accepts Un page id avec facultatif `#anchor`, e.g. `./crafting.md#usage` ou `#usage`; lorsque omitted Le cible est résolu depuis `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | Définissez vers `false` vers suppress Le survol tooltip; `noTooltip` est un legacy alias |
| `showIcon` | *(none)* | `left` ou `right` (ou quelconque truthy valeur → droite) — renders Le élément icon beside Le lien texte; omit vers afficher texte seulement |
| `scale` | `1.0` | afficher scale pour Le facultatif élément icon; a non effect lorsque `showIcon` est omitted |

Exemples:

````md
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" />
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" showIcon="left" />
<ItemLink id="minecraft:diamond" showIcon="left" scale="2" />
<ItemLink id="minecraft:diamond" showIcon="right" showTooltip="false" />
<ItemLink ore="stickWood" />
<ItemLink id="minecraft:iron_ore" linksTo="./crafting.md#smelting" />
<ItemLink id="minecraft:compass" linksTo="#usage" />
````

### `<CommandLink>`

Sends Un chat command lorsque clicked.

| attribut | Meaning |
| --- | --- |
| `command` | obligatoire, doit début avec `/` |
| `title` | facultatif tooltip heading |
| `close` | analysé booléen attribut; actuellement analysé but ne utilisé vers close Le guide |

Exemple:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` et `<Column>`

Flex-style containers pour bloc contenu.

| attribut | Meaning |
| --- | --- |
| `gap` | entier gap entre enfants, par défaut `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | booléen expression, par défaut `false` |
| `width` | entier préféré largeur; useful pour constraining liste ligne largeur |

Exemple:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

vers constrain Le largeur of normal markdown lists, wrap eux dans Un container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH utilise ceci bloc balise internally lorsque exécution markdown footnotes sont expanded. It peut aussi être written manuellement Si needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

Renders Un compact élément grid. enfants doit être brut `<ItemIcon>` elements, qui sont analysé directly by Le grid compilateur. chaque enfant peut utiliser l’un ou l’autre `id` ou `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

Renders Un non-interactive 3D unique-bloc scène. Le aperçu a non scène arrière-plan, non scène
buttons, non layer controls, et non annotation features, but au survol Le bloc encore affiche Le
sélection outline et tooltip. `ore` doit résoudre vers Un bloc élément stack.

| attribut | Meaning |
| --- | --- |
| `id` | bloc id; prend en charge Le normal `modid:block[:meta][:{snbt}]` forme |
| `ore` | ore dictionary lookup; Le premier correspondant bloc élément wins |
| `scale` | caméra zoom multiplier, par défaut `4` |
| `float` | legacy flow flottant prise en charge: `left` ou `right` |
| `perspective` | `isometric-north-east` (par défaut), `isometric-north-west`, ou `up` |
| `nbt` | facultatif SNBT tile-entity données merged onto quelconque inline SNBT depuis `id` |

Remarques:

- inline SNBT dans `id` est encore accepted pour compatibility, but `nbt="..."` est Le préféré
  authoring form
- lorsque les deux inline SNBT et `nbt` sont present, Le `nbt` attribut est merged dernier et donc
  remplace conflicting clés
- GuideNH 1.7.10 fait ne prise en charge modern bloc-état propriété syntaxe here, so GuideME-style
  `p:<state>` attributs sont intentionally ne Pris en charge

````md
<BlockImage id="minecraft:crafting_table" scale="3" />
<BlockImage ore="logWood" scale="3" perspective="isometric-north-west" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<FloatingImage>`

Voir [Images And Assets](Images-And-Assets) pour Le complet behavior.

Règles rapides:

- `src` prend en charge relatif paths, rooted paths, et explicite `modid:path` texture ids
- `x`, `y`, `width` / `w`, et `height` / `h` define Le crop rectangle on Le original image et doit tous être present
- `scaleX` et `scaleY` resize Le cropped résultat et prise en charge independent horizontal / vertical stretching
- `displayWidth` et `displayHeight` Définissez final dimensions dans pixels; un valeur preserves Le crop aspect ratio, pendant deux valeurs autoriser stretching
- `displayWidth` / `displayHeight` ne peut pas être combined avec `scaleX` / `scaleY`
- `wrap="inline"` places Le image truly inline dans texte flow; dans que mode `align` est ignored
- old contenu que utilisé `width` / `height` as final afficher taille doit être migrated manuellement

### `<SubPages>`, `<Category>`, et `<Special>`

Voir [Navigation](Navigation) pour complet navigation behavior.

### `<Structure>`

Voir [Examples](Examples) et [GameScene](GameScene) lorsque deciding whether vers utiliser Un static structure aperçu ou Un complet 3D scène.

### `<Mermaid>`

utilisé pour exécution Mermaid contenu. actuel exécution prise en charge est focused on `mindmap`, l’un ou l’autre inline ou à travers Un page-relatif `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
Les nœuds d’exécution peuvent intégrer des blocs normaux.

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` et `height` constrain Le exécution viewport box
- dans Le viewport, faire glisser pans et Le mouse wheel zooms
- quoted Mermaid labels peut utiliser riche inline markdown comme `**bold**` et page liens
- `<NodeContent id="...">...</NodeContent>` peut être ajouté as enfants of `<Mermaid>` vers remplacer Un node corps avec arbitrary exécution blocs

### `<CsvTable>`

utilisé vers analyser Un CSV fichier dans Un exécution tableau:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves relatif vers Le actuel page, Le même way scène imports et normal ressource liens do.

facultatif attributs:

- `header`
  Defaults vers `true`; Définissez `header={false}` vers Conservez Le premier ligne unbolded
- `widths`
  Comma-separated entier largeur hints comme `widths="120,80"`

Exemples:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

Le related fenced exécution CSV form aussi prend en charge correspondant metadata:

````md
```csv widths="120,80" header=false
nom,valeur
iron,42
gold,17
```
````

### `<Latex>`

Renders Un LaTeX math formule using jlatexmath. lorsque utilisé inline (dans Un paragraph ou texte flow), it renders as Un scaled glyph que expands Le ligne hauteur vers fit Le formule. lorsque written as son propre paragraph (bloc context), it renders centered as Un afficher-mode formule.

| attribut | type | par défaut | description |
| --- | --- | --- | --- |
| `formula` | chaîne | *(obligatoire)* | LaTeX source chaîne |
| `color` | `#RRGGBB` ou `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | flottant | `1.0` | afficher taille multiplier applied on haut of Le automatique ligne-hauteur scaling |
| `sourceScale` | flottant | `100.0` | jlatexmath interne rendent resolution; higher valeurs improve quality at large sizes |
| `tooltip` | chaîne | *(none)* | simple tooltip texte affiché on survol |
| `showTooltip` | booléen | `false` | afficher Le brut LaTeX source as Un tooltip on survol |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-seulement. Vertical alignment dans Le texte ligne: `baseline` (par défaut) aligns Le formule's math baseline avec Le texte baseline; `top` aligns Le formule haut avec Le ligne haut; `center` centers it on Le texte; `bottom` aligns Le formule bas avec Le texte bas |
| `offsetX` | int | `0` | Horizontal pixel offset applied après alignment (positive = droite) |
| `offsetY` | int | `0` | Vertical pixel offset applied après alignment (positive = down) |

Exemples:

````md
Inline: <Latex formula="E=mc^2" />

Fraction that expands line height: <Latex formula="\frac{a+b}{c-d}" />

Gold colour: <Latex formula="\sqrt{x^2+y^2}" color="#FFD700" />

Scaled up: <Latex formula="\pi" scale="1.5" />

With hover tooltip: <Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2}" showTooltip={true} />

Plain custom tooltip: <Latex formula="E=mc^2" tooltip="Energy equals mass times the speed of light squared." />

Rich tooltip:
<Latex formula="\Delta G = \Delta H - T\Delta S">
  **Gibbs free energy**

  - <Latex formula="\Delta H" />: enthalpy change
  - <Latex formula="T\Delta S" />: entropy term
</Latex>

Bottom-aligned (formula bottom matches text bottom): <Latex formula="\frac{a}{b}" valign="bottom" />

Explicit baseline alignment (same as default): <Latex formula="E=mc^2" valign="baseline" />

Top-aligned with an upward nudge: <Latex formula="x^2" valign="top" offsetY="-1" />

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" />

<Latex formula="\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}" />
````

#### `$$formula$$` shorthand

As Un convenience you peut écrire `$$formula$$` directly dans Markdown sans using Le `<Latex>` balise.
tous rendu paramètres utiliser leurs defaults (white colour, scale 1.0, non tooltip, baseline-aligned).

- **Inline**: `$$formula$$` embedded dans Un paragraph renders as Un inline formule.
- **afficher**: Un paragraph whose entire contenu est `$$formula$$` (avec facultatif surrounding whitespace) renders as Un centred afficher-mode bloc.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

Remarques:

- Le formule hauteur est calibrated vers Le actuel ligne texte hauteur. Simple formulas rendent at texte hauteur; taller formulas (fractions, summations, integrals, etc.) expand Le enclosing ligne hauteur automatiquement.
- `valign` seulement s’applique vers inline formulas. afficher-mode (bloc-level) formulas sont toujours centered horizontally; utiliser `offsetY` vers shift eux vertically dans Le bloc.
- `color` defaults vers white (`#FFFFFF`). utiliser `#AARRGGBB` format pour Un semi-transparent fill.
- `sourceScale` seulement affects rendent sharpness, ne Le displayed taille. valeurs en dessous `16` sont clamped vers `16`.
- Tooltip priority est: riche enfant Markdown contenu, alors `tooltip="..."`, alors `showTooltip={true}` brut source repli.
- enfant tooltip contenu est compilé as regular guide Markdown, so it peut inclure bold texte, lists, liens, élément balises, et nested `<Latex>` formulas.
- Le `$$formula$$` shorthand toujours utilise par défaut paramètres. utiliser Le `<Latex>` balise pour personnalisé colour, scale, alignment ou tooltip.

### scène exécution balises

These balises seulement fonctionnent dans `<GameScene>` / `<Scene>`:

| balise | Purpose | clé attributs |
| --- | --- | --- |
| `<ImportStructure>` | import Un externe SNBT/NBT structure ressource | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import Un StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | supprimer déjà-placed blocs que correspondre Un bloc matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp Le même enfant annotations onto chaque correspondant placed bloc | `id` |

Voir [GameScene](GameScene) pour scène import/removal behavior et [Annotations](Annotations) pour annotation template Règles.


## Charts

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, et `<ScatterChart>` sont interactive graphique blocs. tous charts share Le following courant attributs:

| attribut | description | par défaut |
| --- | --- | --- |
| `title` | graphique titre | none |
| `width` / `height` | explicite taille | 320 / 200 |
| `background` / `border` | arrière-plan et bordure colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | titre et valeur-étiquette colors | light grey |
| `legend` | légende position: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | valeur-étiquette position: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | interne plot légende position: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum interne légende box taille | `120` / `64` |
| `cornerLegendBackground` | interne légende arrière-plan couleur | `#AA111922` |

Cartesian charts (colonne / Bar / ligne / Scatter) additionally acceptent axis attributs `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` et Le correspondant `yAxis*` Définissez, plus `showXGrid={true}` / `showYGrid={true}` vers toggle gridlines.

enfants:

* `<Series name="..." color="#..." data="10,20,30"/>` pour category-based charts (colonne / Bar / categorical ligne).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` pour numeric X (ligne `numericX={true}`, Scatter).
* `<Slice name="..." value="..." color="#..."/>` pour `<PieChart>` seulement.

lorsque `color` est omitted on a `<Series>` ou `<Slice>`, GuideNH cycles à travers Un built-dans 16-couleur palette.

`<Series>` et `<Slice>` aussi acceptent Le following facultatif icon / tooltip attributs:

* `icon="modid:item"` (même syntaxe as `<ItemImage>`'s `id`, peut inclure `@meta` et inline NBT JSON) — binds an `ItemStack` vers Le entrée; Le légende swatch devient Le élément icon et au survol Le données point affiche Le vanilla élément tooltip avec Le graphique description appended at Le fin.
* `iconImage="images/foo.png"` — utiliser Un PNG ressource as Le légende swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form texte appended vers Le tooltip (utiliser `\n` pour multi-ligne).

Exemple:

```mdx
<PieChart title="Output share">
  <Slice name="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice name="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra attributs: `categories` (X-axis ou Y-axis labels, comma separated), `barWidthRatio` (par défaut 0.7). `<BarChart>` puts Le categories on Le Y-axis et valeurs on Le X-axis.

#### Combo extensions

`<ColumnChart>` et `<BarChart>` acceptent deux extra enfant element types so plusieurs graphique styles peut share un plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn as Un polyline overlay on haut of Le bars. chaque ligne point sits at Le cluster centre of Le correspondant category index; Le overlay shares Le host graphique's valeur axis. You peut declare plusieurs `<LineSeries>` vers overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — Un small pie graphique drawn dans un of Le four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) of Le plot area. son `<Slice>` enfants share Le même syntaxe as dans `<PieChart>`.

```mdx
<ColumnChart title="Quarterly output" categories="Q1,Q2,Q3,Q4">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="topRight" title="Total share">
    <Slice name="Iron" value="225" color="#a0a0a0"/>
    <Slice name="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

### `<LineChart>`

Extra attributs: `numericX={true}` vers enable Un numeric X-axis (enfants doit utiliser `points`); `showPoints={false}` masque point markers. Le hovered point est pushed outward by 2px along Le courbe normal, enlarged, et outlined; Le adjacent ligne segments thicken by 1px.

`<LineChart>` et `<ScatterChart>` peut afficher Un compact légende dans Le plot area avec `cornerLegend="topRight"` ou another corner. entrées utiliser existing série names et colors.

### `<PieChart>`

Extra attributs: `startAngle` (par défaut `-90`, i.e. 12 o'clock); `clockwise={false}` vers reverse direction. Le hovered secteur pops outward 4px along son bisector.

### `<ScatterChart>`

Renders points seulement; `<Series>` doit utiliser `points`. Le X-axis est toujours numeric.

## fonction Graphs

`<FunctionGraph>` et Le unique-courbe shorthand `<Function>` rendent Un interactive Desmos-style panel. Le même panel est aussi disponible à travers a ` ```funcgraph ` fenced code bloc; Voir Le exécution [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) pour Un complet walkthrough.

Panel attributs (accepted by Le container, Le shorthand, et Le fence header alike):

- `width` / `height` (defaults `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (par défaut `true`)
- `xRange="a..b"` (ou `xMin` / `xMax` separately), `xStep` pour tick spacing; même pour Le Y axis
- `xLabel` / `yLabel` Ajoutez Excel-style axis titles en dessous et au-dessus de Le plot respectively. Elles prise en charge inline `$$...$$` LaTeX; `domain="a..b"` est un legacy alias pour `xRange` lorsque non explicite X range est present
- `quadrants="1,2,3,4"` ou `quadrants="all"` vers force Le visible quadrants; omit vers début dans quadrant 1 avec auto-expansion lorsque sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, et `cornerLegendBackground` afficher Un compact légende dans Le plot area using non-vide courbe labels

courbe enfants (`<Plot>` / `<Function>`):

- `expr="..."` &mdash; Le expression. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` absolu valeur, `√` / `sqrt` / `∛` / `cbrt`, implicite multiplication, et Le constants `pi`, `tau`, `e`, `phi` sont Pris en charge. Built-dans calls cover Le standard trig/log/exp/rounding family plus deux-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates Le expression as `x = f(y)` et rotates Le courbe.
- `domain="a..b"` (x bounds shorthand) ou comma-separated clauses comme `x>=0, x<5`.
- `color`, `label`. quelconque courbe avec Un non-vide étiquette est automatiquement listed dans Un légende rendu just en dessous Le panel: Un small couleur swatch followed by Le étiquette, avec entrées flowing gauche-vers-droite et wrapping onto Un new ligne lorsque Le suivant entrée would ne fit.
- `tooltip` adds simple texte en dessous Le computed tooltip. `showFunction` et `showValues` par défaut vers `true`; Définissez l’un ou l’autre vers `false` vers masquer Le rendu équation ou live `(x, y)` valeur respectively. A `<Plot>` / nested `<Function>` avec `expr` peut contenir Markdown et GuideNH balises as Un riche tooltip corps. Le ordre est toujours étiquette, rendu équation, live valeurs, `tooltip` texte, alors riche enfant contenu; omitted ou désactivé computed champs sont skipped dans que ordre.
- `pointEveryX="step"` adds généré point markers at regular x intervals on que courbe.
- `pointEveryY="step"` adds généré point markers where Le courbe intersects regular y intervals, using Un bounded recherche.
- `autoPointLabel="none|x|y|xy"` controls généré point labels; par défaut est `none`.
- `autoPointColor="#..."` remplace Le généré point couleur; omitted signifie inherit Le courbe couleur.

Marked points (`<Point>`):

- explicite: `x="..."` et `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` ou `atY="v"` (Le exécution bisects on Le plot's x-domain vers find Le correspondant `x`).
- facultatif `color`, `label`.

Interaction: survol Un courbe vers surligner it; press et hold vers scrub Un point along Le courbe. Le tooltip starts avec `label` lorsque supplied, alors Le équation et live `(x, y)` valeur sauf leurs switches sont désactivé; it stays anchored au-dessus de Le point et flips en dessous lorsque there est non headroom.

## BetterQuesting Compatibility balises

`<QuestLink>` et `<QuestCard>` sont seulement enregistré lorsque Le BetterQuesting mod est chargé. Elles sont documented dans detail on Le [Mod Compatibility](Mod-Compatibility) page; Le summary en dessous covers Le most courant usage.

### `<QuestLink>`

Inline lien vers Un BetterQuesting quest. clic opens Le quest dans Le BetterQuesting GUI, sauf Le quest id est aussi present dans Le actuel guide's `quest_ids` frontmatter — dans que case Le lien navigates vers que page à la placer.

| attribut | Meaning |
| --- | --- |
| `id` | obligatoire BetterQuesting quest id; accepts canonical UUID strings et compact Base64 ids |
| `text` | facultatif remplacer pour Le displayed texte |
| `show_tooltip` | facultatif booléen (par défaut `true`); Définissez vers `false` vers suppress Le quest-description tooltip. `showTooltip` est accepted as Un alias |

visibilité behavior est decided per player at compilateur time:

- visible / completed quests rendent as Un clickable lien (completed quests sont tinted green et append a `✓` mark)
- locked but non-masquées quests encore rendent as clickable quest liens so Elles peut open Le BetterQuesting quest écran ou Le indexed guide page
- masquées / secret quests rendent as Un darker italic placeholder using `guidenh.compat.bq.hidden`
- inconnu quest ids rendent as Un red placeholder using `guidenh.compat.bq.missing`

Exemple:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

bloc-level summary card pour Un BetterQuesting quest. Renders Le quest titre avec Le même état-aware styling as `<QuestLink>`, plus Le quest description as Un corps paragraph lorsque Le quest est visible vers Le player.

| attribut | Meaning |
| --- | --- |
| `id` | obligatoire BetterQuesting quest id; accepts canonical UUID strings et compact Base64 ids |
| `show_desc` | facultatif booléen (par défaut `true`); Définissez vers `false` vers suppress Le description corps |
| `show_tooltip` | facultatif booléen (par défaut `true`); Définissez vers `false` vers suppress Le quest-description tooltip on Le clickable titre. `showTooltip` est accepted as Un alias |

Le accent couleur of Le card bordure follows Le quest état: green pour completed, gray pour locked / masquées, red pour manquant, et Le standard lien couleur pour visible quests. Le titre remains clickable pour visible, completed, et locked-but-non-masquées quests.

Exemple:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
