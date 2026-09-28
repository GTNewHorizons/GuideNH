# GameScene


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

`<GameScene>` est GuideNH's 3D aperçu balise. `<Scene>` est un alias avec Le même behavior.

## scène attributs

| attribut | type | par défaut | Meaning |
| --- | --- | --- | --- |
| `width` | entier | `256` | viewport largeur dans pixels |
| `height` | entier | `192` | viewport hauteur dans pixels |
| `zoom` | flottant | `1.0` | caméra zoom multiplier |
| `perspective` | chaîne | `isometric-north-east` | caméra preset |
| `rotateX` | flottant | auto | explicite X rotation remplacer |
| `rotateY` | flottant | auto | explicite Y rotation remplacer |
| `rotateZ` | flottant | auto | explicite Z rotation remplacer |
| `offsetX` | flottant | auto | écran-espace horizontal pan |
| `offsetY` | flottant | auto | écran-espace vertical pan |
| `centerX` | flottant | auto | explicite monde rotation centre X |
| `centerY` | flottant | auto | explicite monde rotation centre Y |
| `centerZ` | flottant | auto | explicite monde rotation centre Z |
| `interactive` | booléen expression | `true` | enables mouse interaction |
| `showBackground` | booléen expression | `true` | affiche Le scène arrière-plan fill et bordure |
| `allowLayerSlider` | booléen | `true` | affiche Le vertical layer slider |
| `gridButtonEnabled` | booléen | `true` | affiche Le floor grid toggle button |
| `showGrid` | booléen | `false` | initial visibilité of Le floor grid |

## bloc Statistics Overlay

scènes que contenir blocs enable Le bloc-stat toggle button by par défaut. Ajoutez a `<BlockStats>`
enfant lorsque you want vers remplacer son mode, placement, filters, visibilité, ou taille. Le liste est
cached et seulement rebuilt lorsque Le scène blocs, Ponder timeline état, StructureLib sélection, ou
bloc-stat settings modifier; normal rendu reuses Le prepared lignes. Long lists sont clipped vers
`maxWidth` et `maxHeight`; Si those sont omitted, chaque est Le larger of Le fixe `224` by `96` pixels et
40% of Le scène taille.
Overflow receives draggable scrollbars, et Le mouse wheel scrolls Le liste pendant Le cursor est
sur Le overlay. Hold Shift vers wheel-défiler horizontally.

dans automatique mode, GuideNH scans Le scène's filled blocs et resolves chaque bloc vers Le élément
stack users normally Voir. blocs que contenir plusieurs visible components peut contribute plusieurs
éléments depuis Le même coordonnée; ceci inclut AE2 cable bus parts et facades, ForgeMultipart part
drops, et Carpenters' blocs covers ou overlays lorsque those mods sont installed. Counts sont grouped
by `item:meta` et sorted by count.

automatique lists peut aussi être docked hors de Le scène avec `dock="left"`, `dock="top"`,
`dock="right"`, ou `dock="bottom"`. Docked lists wrap dans extra columns ou lignes based on Le
attached côté length, reserve mise en page espace, et avoid Le scène button colonne on Le droite. clic an
élément dans Un automatique liste vers surligner tous correspondant scène placements avec leurs résolu collision
boxes using Un toujours-on-haut face overlay; clic Le même élément again vers clear Le surligner. Counts
sont rendu à travers Le ItemStack stack-taille overlay. Définissez `showNames={true}` vers append Le count
après chaque nom as well, et survol an élément vers Voir Le exact bloc count dans Le tooltip.

Filters peut masquer courant blocs ou afficher seulement sélectionné blocs:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

utiliser manual mode lorsque Un guide wants vers afficher Un planned material liste à la placer of Le literal scène
contents:

````md
<GameScene>
  <Block id="minecraft:furnace" />
  <BlockStats mode="manual" corner="topRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:cobblestone" count="8" />
    <BlockStat item="minecraft:furnace" count="1" />
  </BlockStats>
</GameScene>
````

## Debug Mode Overlays

lorsque Le `enableDebugMode` option est activé dans Le GuideNH mod config, Le following extra
overlays become disponible dans Le 3D scène aperçu.

### Grid coordonnée Labels

lorsque debug mode est **on** et Le floor grid est **visible**, coordonnée labels sont rendu
en dessous chaque grid ligne:

- **X-axis numbers** sont affiché along Le près edge of Le grid (north/−Z edge dans Le par défaut
  `isometric-north-east` caméra).  chaque entier X monde-coordonnée receives Un étiquette.
- **Z-axis numbers** sont affiché along Le près edge of Le grid (east/+X edge).  chaque entier
  Z monde-coordonnée receives Un étiquette.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) sont drawn at Le midpoint of chaque
  respective grid edge.

coordonnées follow Le actuel monde X/Z valeurs stored dans Le scène level, so Elles peut être
negative lorsque Le structure contient blocs avec negative coordonnées.

Le grid toggle button est **toujours activé** pendant debug mode est active, regardless of Le
`gridButtonEnabled` attribut, so you peut afficher ou masquer Le grid et son labels at quelconque time.
Le par défaut grid visibilité (`showGrid`) est ne affected.

### bloc coordonnée Tooltip

lorsque debug mode est **on** et Le cursor hovers sur Un bloc dans Le scène, Un second
tooltip est rendu au-dessus de Le primary bloc tooltip, showing Le monde-espace bloc position
as `X, Y, Z` dans gold texte.

Si Le coordonnée tooltip would être clipped at Le haut of Le écran it automatiquement snaps
en dessous Le cursor area à la placer (magnetic snapping).

## Perspective Presets

Accepted `perspective` valeurs:

- `isometric-north-east`
- `isometric-north-west`
- `up`

inconnu valeurs fall back vers `isometric-north-east`.

## contenu Embedding et texte Wrapping

quelconque bloc-level balise — including `<GameScene>` — prend en charge deux facultatif attributs que control
how it est embedded dans Le page, mirroring Microsoft Word's "texte Wrapping" options.

| attribut | valeurs | par défaut | Meaning |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | texte-wrapping mode |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Wrap modes

| Mode | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | dans ligne avec texte | par défaut flow: scène occupies son propre vertical slot (嵌入型) |
| `square` | Square | scène floats gauche ou droite; surrounding texte wraps dans Un rectangle around it (方形环绕) |
| `tight` | Tight | Tighter wrap; equivalent vers `square` dans ceci mise en page system (紧密型) |
| `through` | à travers | à travers-wrap; equivalent vers `square` dans ceci mise en page system (穿越型) |
| `top-bottom` | haut et bas | texte seulement au-dessus de et en dessous, ne beside; respects `align` pour horizontal placement (上下型) |
| `behind` | Behind texte | bloc renders behind surrounding texte; respects `align` (衬于文字下方) |
| `front` | dans front of texte | bloc renders dans front of surrounding texte; respects `align` (浮于文字上方) |

### Exemples

gauche-floating scène — texte dans Le suivant paragraph wraps vers Le droite:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Texte qui s’écoule à droite de la scène…
````

droite-floating scène:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Texte qui s’écoule à gauche de la scène…
````

Centred scène (non texte wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline dans texte (flow context) — texte wraps around Un small scène:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

## Exemple

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Import Exemples

These Exemples focus on Le scène-côté behavior que most souvent trips people up lorsque importing
structures.

StructureLib import avec explicite facing, rotation, flip, et offsets:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib
    name="main"
    controller="gregtech:gt.blockmachines:2741"
    facing="north"
    rotation="clockwise_180"
    flip="none"
    offsetX="2"
    offsetY="1"
    offsetZ="-3"
  />
</GameScene>
````

GregTech controllers stay unformed by par défaut, even lorsque Le imported multiblock est otherwise
valide:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

Définissez `formed={true}` seulement lorsque Le aperçu devrait intentionally afficher Le formed controller état:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

Le même par défaut aussi s’applique vers controllers placed directly avec `<Block>`, including GregTech
controllers que rely on surrounding multiblock casings:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

Simple bloc-seulement layouts peut encore être authored directly et remain compatible avec multiblock
inspection logic:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:water" />
  <Block id="minecraft:water" x="-1" />
  <Block id="minecraft:water" x="1" />
  <Block id="minecraft:grass" z="1" />
  <Block id="minecraft:grass" x="1" z="1" />
  <Block id="minecraft:glass" z="2" />
  <Block id="minecraft:glass" x="1" z="2" />
</GameScene>
````

## scène enfant Elements

GuideNH actuellement registers these scène enfant balises:

- `<Block>`
- `<ImportStructure>`
- `<ImportStructureLib>`
- `<IsometricCamera>`
- `<BlockStats>`
- `<PlaySound>`
- `<RemoveBlocks>`
- `<RemoveEntity>`
- `<ReplaceBlock>`
- `<PlaceBlock>`
- `<BlockAnnotationTemplate>`
- `<Entity>`
- annotation balises comme `<BoxAnnotation>` et `<LineAnnotation>`

## scène Sounds

`<PlaySound>` peut être placed dans `<GameScene>` vers play sounds depuis scène interaction ou timeline
entrée. Pris en charge triggers sont:

- `click`, Le par défaut
- `hover`, fired once lorsque Le cursor enters Le scène
- `enter`, fired once lorsque Le scène premier renders

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

lorsque `x`, `y`, et `z` sont fourni, Le sound volume est attenuated dans écran espace depuis Le
projected scène coordonnée vers Le clic point ou scène centre. `radius` defaults vers 75% of Le
shorter scène côté, et `minVolume` defaults vers `0.15`.

## `<BlockStats>` et `<BlockStat>`

Declares ou customizes Un bloc statistics overlay. scènes avec blocs enable Le automatique toggle
button even lorsque ceci enfant est omitted. Adding un ou plus `<BlockStat>` enfants switches Le
overlay vers manual statistics mode pour que scène.

`<BlockStats>` attributs:

| attribut | obligatoire | par défaut | Meaning |
| --- | --- | --- | --- |
| `visible` | non | config, par défaut `false` | initial overlay visibilité |
| `buttonEnabled` | non | config, par défaut `true` | affiche Le bloc statistics toggle button |
| `mode` | non | `auto` | `auto` ou `manual`; enfant `<BlockStat>` entrées force manual mode |
| `corner` | non | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, ou `bottomLeft` |
| `dock` | non | `inside` | automatique lists peut attach vers `inside`, `left`, `top`, `right`, ou `bottom`; manual mode toujours utilise Le dans overlay |
| `showNames` | non | `false` | whether vers afficher élément names beside icons; lorsque activé Le count est aussi appended après Le nom |
| `filterMode` | non | `blacklist` | `blacklist` ou `whitelist` |
| `filter` | non | vide | élément clés comme `minecraft:stone` ou `minecraft:stone:0`, separated by spaces, commas, ou semicolons |
| `maxWidth` | non | Le larger of `224` px et 40% of Le scène largeur | maximum overlay largeur dans pixels avant horizontal défilement |
| `maxHeight` | non | Le larger of `96` px et 40% of Le scène hauteur | maximum overlay hauteur dans pixels avant vertical défilement |

`<BlockStat>` attributs:

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `item` | oui, sauf `id` est utilisé | élément id affiché dans Le liste |
| `id` | oui, sauf `item` est utilisé | existing élément-stack attribut form |
| `count` | non | displayed count; omitting it affiche Le ligne once, et `count="0"` masque Le ligne |

Exemple:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

Places Un bloc dans Le aperçu monde.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `id` | oui, sauf `ore` est utilisé | bloc id |
| `ore` | non | ore dictionary nom; Le premier correspondant stack doit résoudre vers Un bloc élément |
| `x` | non | entier monde X, par défaut `0` |
| `y` | non | entier monde Y, par défaut `0` |
| `z` | non | entier monde Z, par défaut `0` |
| `meta` | non | entier bloc metadata |
| `facing` | non | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | non | SNBT TileEntity compound |
| `formed` | non | whether Le placed structure controller devrait être treated as formed pendant aperçu sync; par défaut `false` |

Remarques:

- `ore` takes precedence sur `id`; Si GregTech est installed, Le choisi stack est unified à travers `GTOreDictUnificator.setStack(...)`
- Si `meta` est omitted et an `ore` correspondre carries concrete non-wildcard élément damage, que damage est utilisé avant Le `facing` repli
- Si `meta` est omitted, some blocs derive Un sensible par défaut depuis `facing`
- Si `nbt` crée Un TileEntity successfully, Le aperçu utilise it
- Définissez `formed={false}` lorsque Un controller-based structure devrait stay unformed dans aperçu even though Le surrounding structure est otherwise valide

Exemple:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

charge Un externe structure fichier dans Le scène.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `src` | oui | structure ressource chemin |
| `x` | non | entier traduction X (alias pour `offsetX`) |
| `y` | non | entier traduction Y (alias pour `offsetY`) |
| `z` | non | entier traduction Z (alias pour `offsetZ`) |
| `offsetX` | non | entier traduction X (préféré sur `x`) |
| `offsetY` | non | entier traduction Y, clamped vers `[0, worldHeight-1]` (préféré sur `y`) |
| `offsetZ` | non | entier traduction Z (préféré sur `z`) |
| `formed` | non | whether imported structure controllers devrait être treated as formed pendant aperçu sync; par défaut `false` |

Pris en charge formats:

- SNBT texte
- gzipped binary NBT
- uncompressed binary NBT

obligatoire structure clés:

- `palette`
- `blocks`

Exemple:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

Imports Un StructureLib multiblock aperçu by controller id.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `controller` | oui | controller bloc id, using `modid:block[:meta]` |
| `name` | non | facultatif binding nom utilisé by `showWhenStructure` on annotations, templates, et sounds |
| `piece` | non | StructureLib piece nom remplacer |
| `facing` | non | facing remplacer passed vers Le importer |
| `rotation` | non | rotation remplacer passed vers Le importer |
| `flip` | non | flip/mirror remplacer passed vers Le importer |
| `channel` | non | entier channel remplacer pour channel-aware structures |
| `offsetX` | non | entier X offset applied vers tous placed blocs (par défaut `0`) |
| `offsetY` | non | entier Y offset applied vers tous placed blocs, clamped vers `[0, worldHeight-1]` (par défaut `0`) |
| `offsetZ` | non | entier Z offset applied vers tous placed blocs (par défaut `0`) |
| `formed` | non | whether imported StructureLib controllers devrait être treated as formed pendant aperçu sync; par défaut `false` |

Remarques:

- Le imported structure starts depuis scène `0 0 0`; Le controller est ne forced vers être placed at `0 0 0`
- ceci balise enables StructureLib-spécifique tooltip, hatch surligner, et channel slider UI lorsque metadata est disponible
- controller correspondant prend en charge Le GTNH-style `modid:block:meta` form
- utiliser `name` lorsque Le scène contient plusieurs StructureLib imports et another balise needs vers cible un spécifique structure état
- `facing`, `rotation`, et `flip` utiliser Le même orientation vocabulary as StructureLib export; lorsque Un requested combination est ne allowed by Le controller, GuideNH falls back vers Le premier valide alignment automatiquement
- GregTech controller previews now par défaut vers Le controller's opposite horizontal facing depuis Le older aperçu orientation, rotating Le aperçu front by 180 degrees around Le Y axis
- Définissez `formed={false}` lorsque Le imported controller devrait remain visibly unformed; ceci est Le Pris en charge alternative vers shipping intentionally broken NBT

Exemple:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

Structure-aware annotation et sound Exemple:

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
  <ImportStructureLib name="aux" controller="gregtech:gt.blockmachines:15412" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    Visible only for matching `main` states.
  </BlockAnnotation>

  <PlaySound
    sound="guidenh:machine.start"
    trigger="click"
    showWhenStructure="aux"
    showWhenTier="1..2"
  />
</GameScene>
````

StructureLib defaults peut aussi être supplied as enfant balises. These defaults sont part of Le scène's
initial interactive état, so Le reset-view button restores eux après Le user modifications tier ou
channel sliders.

| enfant balise | Meaning |
| --- | --- |
| `<Tier value="1" />` | Master tier valeur. |
| `<Channel name="channelName" value="1" />` | Named StructureLib channel remplacer. Repeat pour plusieurs channels. |
| `<Facing value="north" />` | par défaut facing. |
| `<Rotation value="normal" />` | par défaut rotation. |
| `<Flip value="none" />` | par défaut flip/mirror. |
| `<Orientation value="north:normal:none" />` | Facing, rotation, et flip dans un balise. |
| `<GregTechActiveController />` | GregTech seulement: rendent Le controller avec son active texture lorsque possible. |
| `<GregTechPlaceHatches />` | GregTech seulement: placer normal GT hatches pour hatch-seulement aperçu positions. sans ceci, GT previews encore utiliser survival construct pour hatch-aware machines, but vide hatch positions fall back vers casing blocs. |

pour GregTech controllers, GuideNH now utilise Le même StructureLib survival-aperçu chemin as Le
export command. ceci fixes hatch-seulement positions que normal `construct()` ne peut pas populate pendant
keeping repli casings by par défaut.

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:1000">
    <Tier value="4" />
    <Channel name="voltage" value="4" />
    <Facing value="north" />
    <Rotation value="normal" />
    <Flip value="none" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>
````

## `<IsometricCamera>`

s’applique explicite isometric caméra yaw/pitch/roll.

Si ceci balise est omitted, Le scène keeps using Le `<GameScene>` `perspective` preset. Le par défaut
`isometric-north-east` preset est equivalent vers:

````md
<IsometricCamera yaw="225" pitch="30" />
````

| attribut | Meaning |
| --- | --- |
| `yaw` | flottant |
| `pitch` | flottant |
| `roll` | flottant |

Exemple:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes chaque déjà-placed bloc correspondant Un cible bloc id.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `id` | oui | bloc id vers supprimer, using `modid:block[:meta]` |

ceci est useful après importing Un structure lorsque you want vers masquer spécifique blocs pour clarity.

Exemple:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

Replaces déjà-placed blocs que correspondre Un source bloc id (et optionally Un partial tile entity NBT
pattern) avec Un new bloc. Le recherche peut être global (tous filled blocs) ou restricted vers a
bounding box.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `from` | oui | source bloc vers correspondre, using `modid:block[:meta]` |
| `from_nbt` | non | partial SNBT compound; Un bloc correspond seulement lorsque son tile entity NBT contient tous listed clés |
| `to` | oui | replacement bloc, using `modid:block[:meta]` |
| `to_nbt` | non | SNBT TileEntity compound vers apply vers Le replacement |
| `x` | non | bounding box début X; Si quelconque of `x/y/z/dx/dy/dz` est present, Le box mode est activated |
| `y` | non | bounding box début Y |
| `z` | non | bounding box début Z |
| `dx` | non | bounding box length on Le X axis (par défaut `1`) |
| `dy` | non | bounding box hauteur on Le Y axis (par défaut `1`) |
| `dz` | non | bounding box largeur/depth on Le Z axis (par défaut `1`) |
| `formed` | non | whether replacement résultat controllers devrait être treated as formed pendant aperçu sync; par défaut `false` |

Remarques:

- lorsque none of `x/y/z/dx/dy/dz` sont fourni, tous filled blocs sont scanned globally
- `from_nbt` est un **partial** correspondre: seulement Le clés listed dans Le pattern doit correspondre; extra clés dans
  Le actuel tile entity sont ignored
- Le replacement est performed via Le même bloc placement pipeline as `<Block>`, so GregTech MetaTile
  et BartWorks tile entities sont handled correctly
- Si Le replacement places Un controller, `formed={false}` keeps que controller unformed pendant aperçu

Exemple:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills Un axis-aligned box avec Un unique bloc type, overwriting whatever was there avant.
Unlike `<Block>` (qui targets Un unique position), `<PlaceBlock>` prend en charge multi-bloc regions via
`dx`/`dy`/`dz`, ordered as length, hauteur, et largeur/depth on Le X/Y/Z axes.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `id` | oui | bloc id, using `modid:block[:meta]` |
| `nbt` | non | SNBT TileEntity compound applied vers chaque placed bloc |
| `x` | non | region début X, par défaut `0` |
| `y` | non | region début Y, par défaut `0` |
| `z` | non | region début Z, par défaut `0` |
| `dx` | non | region length on Le X axis, par défaut `1` |
| `dy` | non | region hauteur on Le Y axis, par défaut `1` |
| `dz` | non | region largeur/depth on Le Z axis, par défaut `1` |
| `formed` | non | whether placed controllers devrait être treated as formed pendant aperçu sync; par défaut `false` |

Remarques:

- tous blocs dans Le box sont unconditionally placed (non prior-bloc check)
- Le NBT compound est copied pour chaque individual placement
- Le même bloc placement pipeline as `<Block>` est utilisé, so GregTech MetaTile et BartWorks tile entities
  sont fully Pris en charge
- Si Le region places un ou plus controllers, `formed={false}` keeps chaque affected controller unformed

Exemple:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

Expands un ou plus enfant annotations onto chaque correspondant bloc que déjà exists dans Le actuel scène.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `id` | oui | bloc matcher dans `modid:block[:meta]` form |

Règles:

- placer it après Le blocs ou imported structures que it devrait correspondre
- correspondant happens against Le actuel scène état at analyser time
- enfant annotations utiliser local coordonnées relatif vers chaque correspondant bloc

Exemple:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

Adds Un entity vers Le aperçu scène.

Le attributs follow summon-style entity placement et SNBT données.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `id` | oui | entity type id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, et enregistré mod entity ids dans l’un ou l’autre `modid.entityName` ou `modid:entityName` form sont accepted |
| `x` | non | flottant X coordonnée Le entity est centered on, par défaut `0.5` |
| `y` | non | flottant Y coordonnée at Le bas of Le entity, par défaut `0` |
| `z` | non | flottant Z coordonnée Le entity est centered on, par défaut `0.5` |
| `rotationY` | non | yaw dans degrees, par défaut `-45` |
| `rotationX` | non | pitch dans degrees, par défaut `0` |
| `data` | non | summon-style SNBT merged dans Le entity NBT avant spawn |
| `sceneEntityId` | non | stable scène-local entity id utilisé by later `<Entity>` / `<RemoveEntity>` operations et by imported scène snapshots |
| `mount` | non | stable `sceneEntityId` of Le vehicle que ceci entity devrait ride après spawn |
| `unmount` | non | booléen expression que clears ceci entity's actuel stable mount relation après spawn ou état replay |
| `baby` | non | booléen expression forcing Pris en charge entities dans baby form; omitted leaves Le entity's normal age/état unchanged |
| `name` | non | aperçu player nom lorsque `id` est `player`, `fakeplayer`, `minecraft:player`, ou `minecraft:fakeplayer` |
| `uuid` | non | aperçu player UUID lorsque using un of Le player ids au-dessus de |
| `showName` | non | booléen expression controlling Le aperçu player nameplate, par défaut `true` pour player aperçu ids |
| `showCape` | non | booléen expression controlling Le aperçu player cape, par défaut `true` pour player aperçu ids |
| `headRotation` | non | aperçu player head rotation as `x y z` degrees |
| `leftArmRotation` | non | aperçu player gauche arm rotation as `x y z` degrees |
| `rightArmRotation` | non | aperçu player droite arm rotation as `x y z` degrees |
| `leftLegRotation` | non | aperçu player gauche leg rotation as `x y z` degrees |
| `rightLegRotation` | non | aperçu player droite leg rotation as `x y z` degrees |
| `capeRotation` | non | aperçu player cape rotation as `x y z` degrees; defaults vers Le standing-encore angle `6 0 0` |

Remarques:

- entity bounds participate dans scène auto-centering et visible-layer filtering
- entity creation falls back gracefully lorsque Le aperçu monde est ne ready yet, alors binds on premier rendent
- `sceneEntityId` est facultatif, but strongly recommended whenever later scène mutations need vers find, supprimer, remount, ou restore Le même logical entity sans scanning by brut exécution id
- un `sceneEntityId` peut propre plus que un exécution entity instance; `<RemoveEntity sceneEntityId="..."/>` removes chaque entity actuellement enregistré vers que stable id
- `mount` liens entities by stable scène id rather que by brut NBT passenger lists, so replay, import/export, aperçu rebuild, et Ponder seeking peut tous restore Le même rider/vehicle relation deterministically
- `unmount={true}` clears Le stable mount relation pour que entity avant quelconque later mount est applied
- `baby={true}` actuellement prend en charge aperçu players, ageable mobs, vanilla zombies, et modded entities que expose stable `setChild(boolean)` ou `setBaby(boolean)` style APIs
- enfant-état entities sont re-aligned vers leurs actuel position après resizing so survol et pick bounds stay centered on Le rendu model
- player aperçu ids créer Un client-côté fake remote player so Le normal player renderer et skin pipeline peut être utilisé
- lorsque les deux `name` et `uuid` sont omitted pour Un player aperçu, GuideNH falls back vers `Steve` et Le vanilla par défaut skin
- lorsque seulement `name` est given pour Un player aperçu, GuideNH premier tries vers résoudre Le real online profile so skins et capes peut charger; Si lookup fails, it falls back vers Un stable offline UUID
- lorsque seulement `uuid` est given pour Un player aperçu, GuideNH generates Un placeholder afficher nom et encore tries vers résoudre Le skin depuis Le profile
- `showName={false}` masque Le aperçu player's overhead nom sans bypassing Le normal player renderer
- `showCape={false}` masque Le aperçu player's cape pendant encore respecting Le normal player rendent chemin et Forge hooks
- player pose attributs utiliser trois espace-separated floats mapped vers model `X Y Z` rotation dans degrees
- omitted head et limb rotation attributs Conservez Le normal vanilla idle pose; omitted `capeRotation` falls back vers Le standing-encore cape angle `6 0 0`
- player previews require Un active client monde at analyser time because Minecraft's player entity constructor ne peut pas être créé worldless
- au survol Un entity affiche son localisé afficher nom, ou son personnalisé nom Si un was fourni

Exemple:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Stable-id mount Exemple:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

Removal et unmount Exemple:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
</GameScene>
````

Baby entity Exemple:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

aperçu player pose Exemple:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    name="ArtherSnow"
    headRotation="0 20 0"
    rightArmRotation="-35 0 0"
    leftArmRotation="10 0 -12"
    rightLegRotation="8 0 0"
    leftLegRotation="-8 0 0"
    capeRotation="12 0 0"
  />
</GameScene>
````

aperçu player nom et cape Exemple:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

Removes chaque exécution entity actuellement enregistré vers un stable `sceneEntityId`.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `sceneEntityId` | oui | stable scène-local entity id vers supprimer |
| `unmount` | non | booléen expression que clears Le stable mount relation avant removal |

Remarques:

- ceci est Le scène-côté counterpart vers Ponder's `removeEntities`
- removal works on Le indexed stable-id registry, so it fait ne need vers scan tous entities chaque frame
- Si plusieurs imported ou replayed entities share Le même `sceneEntityId`, Elles sont supprimé together

Exemple:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## Weather

`<Weather>` adds animated rain ou snow directly vers a `GameScene`. Unlike Ponder weather presets,
scène weather est ne timeline-owned: it keeps looping pendant normal scène rendu, it fait ne
fade dans ou fade out, et it ne peut pas être paused ou scrubbed independently. Le renderer encore utilise Le
même precipitation géométrie chemin as Ponder weather, so local aperçu et site export stay aligned.

| attribut | par défaut | description |
| --- | --- | --- |
| `weather` / `type` | `rain` | Weather kind. Pris en charge valeurs: `rain`, `snow`. |
| `x`, `z` | scène bounds | Covered precipitation columns. Un scalar targets un colonne. Arrays utiliser endpoint pairs vers define un ou plus rectangles. |
| `density` | type-spécifique | Coverage density. Higher valeurs Conservez plus precipitation columns active; lower valeurs sparsify Le effect. |

Remarques:

- `<Weather>` ignores `y`; Le vertical span est derived depuis Le actuel scène bounds et depuis Le
  highest precipitation-blocking bloc dans chaque covered colonne.
- Si un axis a unmatched extra array valeurs, Le unmatched tail est ignored.
- dans un weather declaration, rain et snow jamais stack on Le même `x/z` colonne. Si plusieurs
  weather balises overlap, earlier balises Conservez Le shared columns.
- différent non-overlapping columns dans Le même `GameScene` peut rendent rain et snow at Le même
  time.

Exemple:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## caméra centre Behavior

Si non explicite `centerX/Y/Z` est given, GuideNH auto-centers Le scène depuis Le placed bloc bounds. Si quelconque explicite centre coordonnée est Définissez, auto-centering est désactivé et manquant coordonnées par défaut vers `0`.

## Interaction Remarques

lorsque `interactive={true}` Le scène prend en charge rotation, pan, zoom, reset, annotation toggles, et autre UI controls exposed by Le guide écran.

- scènes spanning plusieurs Y levels afficher Un visible-layer slider au-dessus de Le bas edge
- StructureLib scènes peut Ajoutez Un hatch-surligner toggle button plus Un channel slider at Le very bas lorsque Le imported metadata fournit eux
- annotation survol takes priority sur bloc survol; bloc tooltips appear normally once non annotation hotspot est being hovered
- StructureLib survol keeps Le bloc nom on Le premier tooltip ligne, adds structure-spécifique texte starting on Le second ligne, et expands replacement candidates lorsque `Shift` est held

## Related Pages

- [Annotations](Annotations)
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [Examples](Examples)
