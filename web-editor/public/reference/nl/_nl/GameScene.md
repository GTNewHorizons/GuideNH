# GameScene

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


`<GameScene>` is GuideNH's 3D voorbeeldweergave tag. `<Scene>` is Een alias met De zelfde behavior.

## Scène-attributen

| Attribuut | Type | Standaard | Betekenis |
| --- | --- | --- | --- |
| `width` | geheel getal | `256` | viewport breedte in pixels |
| `height` | geheel getal | `192` | viewport hoogte in pixels |
| `zoom` | kommagetal | `1.0` | camera zoomen multiplier |
| `perspective` | tekenreeks | `isometric-north-east` | camera preset |
| `rotateX` | kommagetal | auto | explicit X rotatie override |
| `rotateY` | kommagetal | auto | explicit Y rotatie override |
| `rotateZ` | kommagetal | auto | explicit Z rotatie override |
| `offsetX` | kommagetal | auto | schermruimte horizontal pan |
| `offsetY` | kommagetal | auto | schermruimte vertical pan |
| `centerX` | kommagetal | auto | explicit wereld rotatie center X |
| `centerY` | kommagetal | auto | explicit wereld rotatie center Y |
| `centerZ` | kommagetal | auto | explicit wereld rotatie center Z |
| `interactive` | boolean expressie | `true` | enables mouse interaction |
| `showBackground` | boolean expressie | `true` | shows De scène achtergrond fill en rand |
| `allowLayerSlider` | boolean | `true` | shows De vertical layer slider |
| `gridButtonEnabled` | boolean | `true` | shows De floor grid toggle button |
| `showGrid` | boolean | `false` | initial zichtbaarheid of De floor grid |

## Overlay met blokstatistieken

scènes that bevatten blokken enable De blok-stat toggle button by Standaard. Voeg toe a `<BlockStats>`
kind wanneer you want naar override its modus, placement, filters, zichtbaarheid, of grootte. De lijst is
cached en alleen rebuilt wanneer De scène blokken, Ponder-animatie timeline status, StructureLib selectie, of
blok-stat settings wijzigen; normal rendering reuses De prepared rijen. Long lists are clipped naar
`maxWidth` en `maxHeight`; Als those are omitted, elke is De larger of De vast `224` by `96` pixels en
40% of De scène grootte.
Overflow receives draggable scrollbars, en De mouse wheel scrolls De lijst terwijl De cursor is
over De overlay. Hold Shift naar wheel-scrollen horizontally.

In automatic modus, GuideNH scans De scène's filled blokken en resolves elke blok naar De item
stack users normally see. blokken that bevatten meerdere zichtbaar components kan contribute meerdere
items van De zelfde coördinaat; Deze includes AE2 cable bus parts en facades, ForgeMultipart part
drops, en Carpenters' blokken covers of overlays wanneer those mods are installed. Counts are grouped
by `item:meta` en sorted by count.

Automatic lists kan ook be docked buiten De scène met `dock="left"`, `dock="top"`,
`dock="right"`, of `dock="bottom"`. Docked lists wrap into extra columns of rijen based on De
attached side length, reserve layout space, en avoid De scène button kolom on De right. klikken an
item in Een automatic lijst naar markeren alle matching scène placements met their opgelost collision
boxes using Een always-on-top face overlay; klikken De zelfde item again naar clear De markeren. Counts
are gerenderd through De ItemStack stack-grootte overlay. Stel in `showNames={true}` naar append De count
na elke naam as well, en zweven Een item naar see De exact blok count in De tooltip.

Filters kan verbergen algemene blokken of tonen alleen geselecteerd blokken:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

gebruiken manual modus wanneer Een gids wants naar tonen Een planned material lijst instead of De literal scène
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

## Overlays van de debugmodus

wanneer De `enableDebugMode` optie is ingeschakeld in De GuideNH mod config, De following extra
overlays become available in De 3D scène voorbeeldweergave.

### Grid coördinaat Labels

wanneer debug modus is **on** en De floor grid is **zichtbaar**, coördinaat labels are gerenderd
below elke grid regel:

- **X-axis numbers** are getoond along De near edge of De grid (north/−Z edge in De Standaard
  `isometric-north-east` camera).  elke geheel getal X wereld-coördinaat receives Een label.
- **Z-axis numbers** are getoond along De near edge of De grid (east/+X edge).  elke geheel getal
  Z wereld-coördinaat receives Een label.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) are drawn at De midpoint of elke
  respective grid edge.

coördinaten follow De actual wereld X/Z waarden stored in De scène level, so they kan be
negative wanneer De structure bevat blokken met negative coördinaten.

De grid toggle button is **always ingeschakeld** terwijl debug modus is active, regardless of De
`gridButtonEnabled` Attribuut, so you kan tonen of verbergen De grid en its labels at any time.
De Standaard grid zichtbaarheid (`showGrid`) is niet affected.

### blok coördinaat tooltip

wanneer debug modus is **on** en De cursor hovers over Een blok binnen De scène, Een second
tooltip is gerenderd above De primary blok tooltip, showing De wereldruimte blok positie
as `X, Y, Z` in gold tekst.

Als De coördinaat tooltip would be clipped at De top of De scherm it automatisch snaps
below De cursor area instead (magnetic snapping).

## Perspectiefpresets

Accepted `perspective` waarden:

- `isometric-north-east`
- `isometric-north-west`
- `up`

onbekend waarden fall back naar `isometric-north-east`.

## Inhoud insluiten en tekstterugloop

Any blok-level tag — including `<GameScene>` — ondersteunt two optioneel attributen that control
how it is embedded in De pagina, mirroring Microsoft Word's "tekst Wrapping" opties.

| Attribuut | waarden | Standaard | Betekenis |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | tekst-wrapping modus |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Terugloopmodi

| modus | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | In regel met tekst | Standaard flow: scène occupies its own vertical slot (嵌入型) |
| `square` | Square | scène floats left of right; surrounding tekst wraps in Een rectangle around it (方形环绕) |
| `tight` | Tight | Tighter wrap; equivalent naar `square` in Deze layout system (紧密型) |
| `through` | Through | Through-wrap; equivalent naar `square` in Deze layout system (穿越型) |
| `top-bottom` | Top en Bottom | tekst alleen above en below, niet beside; respects `align` voor horizontal placement (上下型) |
| `behind` | Behind tekst | blok renders behind surrounding tekst; respects `align` (衬于文字下方) |
| `front` | In front of tekst | blok renders in front of surrounding tekst; respects `align` (浮于文字上方) |

### Voorbeelden

Left-floating scène — tekst in De next paragraph wraps naar De right:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Tekst die rechts van de scène loopt…
````

Right-floating scène:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Tekst die links van de scène loopt…
````

Centred scène (no tekst wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline in tekst (flow context) — tekst wraps around Een small scène:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

## Example

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Import Voorbeelden

Deze Voorbeelden focus on De scène-side behavior that most often trips people up wanneer importing
structures.

StructureLib import met explicit facing, rotatie, flip, en offsets:

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

GregTech controllers stay unformed by Standaard, even wanneer De imported multiblock is otherwise
valid:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

Stel in `formed={true}` alleen wanneer De voorbeeldweergave zou moeten intentionally tonen De formed controller status:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

De zelfde Standaard ook applies naar controllers placed directly met `<Block>`, including GregTech
controllers that rely on surrounding multiblock casings:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

Simple blok-alleen layouts kan still be authored directly en remain compatible met multiblock
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

## Kindelementen van de scène

GuideNH currently registers Deze scène kind tags:

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
- annotatie tags such as `<BoxAnnotation>` en `<LineAnnotation>`

## Scèngeluiden

`<PlaySound>` kan be placed binnen `<GameScene>` naar play sounds van scène interaction of timeline
entry. Supported triggers are:

- `click`, De Standaard
- `hover`, fired once wanneer De cursor enters De scène
- `enter`, fired once wanneer De scène first renders

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

wanneer `x`, `y`, en `z` are provided, De sound volume is attenuated in scherm space van De
projected scène coördinaat naar De klikken punt of scène center. `radius` defaults naar 75% of De
shorter scène side, en `minVolume` defaults naar `0.15`.

## `<BlockStats>` en `<BlockStat>`

Declares of customizes Een Overlay met blokstatistieken. scènes met blokken enable De automatic toggle
button even wanneer Deze kind wordt weggelaten. Adding one of more `<BlockStat>` kinderen switches De
overlay naar manual statistics modus voor that scène.

`<BlockStats>` attributen:

| Attribuut | Vereist | Standaard | Betekenis |
| --- | --- | --- | --- |
| `visible` | no | config, Standaard `false` | initial overlay zichtbaarheid |
| `buttonEnabled` | no | config, Standaard `true` | shows De blok statistics toggle button |
| `mode` | no | `auto` | `auto` of `manual`; kind `<BlockStat>` entries force manual modus |
| `corner` | no | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, of `bottomLeft` |
| `dock` | no | `inside` | automatic lists kan attach naar `inside`, `left`, `top`, `right`, of `bottom`; manual modus always uses De binnen overlay |
| `showNames` | no | `false` | whether naar tonen item names beside icons; wanneer ingeschakeld De count is ook appended na De naam |
| `filterMode` | no | `blacklist` | `blacklist` of `whitelist` |
| `filter` | no | empty | item keys such as `minecraft:stone` of `minecraft:stone:0`, separated by spaces, commas, of semicolons |
| `maxWidth` | no | De larger of `224` px en 40% of De scène breedte | maximum overlay breedte in pixels voor horizontal scrolling |
| `maxHeight` | no | De larger of `96` px en 40% of De scène hoogte | maximum overlay hoogte in pixels voor vertical scrolling |

`<BlockStat>` attributen:

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `item` | yes, unless `id` is gebruikt | item id getoond in De lijst |
| `id` | yes, unless `item` is gebruikt | existing item-stack Attribuut form |
| `count` | no | displayed count; omitting it shows De rij once, en `count="0"` hides De rij |

Example:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

Places Een blok into De voorbeeldweergave wereld.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `id` | yes, unless `ore` is gebruikt | blok id |
| `ore` | no | ore dictionary naam; De first matching stack moet oplossen naar Een blok item |
| `x` | no | geheel getal wereld X, Standaard `0` |
| `y` | no | geheel getal wereld Y, Standaard `0` |
| `z` | no | geheel getal wereld Z, Standaard `0` |
| `meta` | no | geheel getal blok metadata |
| `facing` | no | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | no | SNBT TileEntity compound |
| `formed` | no | whether De placed structure controller zou moeten be treated as formed during voorbeeldweergave sync; Standaard `false` |

Notes:

- `ore` takes precedence over `id`; Als GregTech is installed, De chosen stack is unified through `GTOreDictUnificator.setStack(...)`
- Als `meta` wordt weggelaten en an `ore` match carries concrete non-wildcard item damage, that damage is gebruikt voor De `facing` fallback
- Als `meta` wordt weggelaten, some blokken derive Een sensible Standaard van `facing`
- Als `nbt` creates Een TileEntity successfully, De voorbeeldweergave uses it
- Stel in `formed={false}` wanneer Een controller-based structure zou moeten stay unformed in voorbeeldweergave even though De surrounding structure is otherwise valid

Example:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

laadt Een external structure bestand into De scène.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `src` | yes | structure asset pad |
| `x` | no | geheel getal vertaling X (alias voor `offsetX`) |
| `y` | no | geheel getal vertaling Y (alias voor `offsetY`) |
| `z` | no | geheel getal vertaling Z (alias voor `offsetZ`) |
| `offsetX` | no | geheel getal vertaling X (preferred over `x`) |
| `offsetY` | no | geheel getal vertaling Y, clamped naar `[0, worldHeight-1]` (preferred over `y`) |
| `offsetZ` | no | geheel getal vertaling Z (preferred over `z`) |
| `formed` | no | whether imported structure controllers zou moeten be treated as formed during voorbeeldweergave sync; Standaard `false` |

Supported formats:

- SNBT tekst
- gzipped binary NBT
- uncompressed binary NBT

Vereist structure keys:

- `palette`
- `blocks`

Example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

Imports Een StructureLib multiblock voorbeeldweergave by controller id.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `controller` | yes | controller blok id, using `modid:block[:meta]` |
| `name` | no | optioneel binding naam gebruikt by `showWhenStructure` on Annotaties, templates, en sounds |
| `piece` | no | StructureLib piece naam override |
| `facing` | no | facing override passed naar De importer |
| `rotation` | no | rotatie override passed naar De importer |
| `flip` | no | flip/mirror override passed naar De importer |
| `channel` | no | geheel getal channel override voor channel-aware structures |
| `offsetX` | no | geheel getal X offset applied naar alle placed blokken (Standaard `0`) |
| `offsetY` | no | geheel getal Y offset applied naar alle placed blokken, clamped naar `[0, worldHeight-1]` (Standaard `0`) |
| `offsetZ` | no | geheel getal Z offset applied naar alle placed blokken (Standaard `0`) |
| `formed` | no | whether imported StructureLib controllers zou moeten be treated as formed during voorbeeldweergave sync; Standaard `false` |

Notes:

- De imported structure starts van scène `0 0 0`; De controller is niet forced naar be placed at `0 0 0`
- Deze tag enables StructureLib-specific tooltip, hatch markeren, en channel slider UI wanneer metadata is available
- controller matching ondersteunt De GTNH-style `modid:block:meta` form
- gebruiken `name` wanneer De scène bevat meerdere StructureLib imports en another tag needs naar target one specific structure status
- `facing`, `rotation`, en `flip` gebruiken De zelfde orientation vocabulary as StructureLib export; wanneer Een requested combination is niet allowed by De controller, GuideNH falls back naar De first valid alignment automatisch
- GregTech controller previews now Standaard naar De controller's opposite horizontal facing van De older voorbeeldweergave orientation, rotating De voorbeeldweergave front by 180 degrees around De Y axis
- Stel in `formed={false}` wanneer De imported controller zou moeten remain visibly unformed; Deze is De supported alternative naar shipping intentionally broken NBT

Example:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

Structure-aware annotatie en sound example:

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

StructureLib defaults kan ook be supplied as kind tags. Deze defaults are part of De scène's
initial interactief status, so De reset-view button restores them na De user wijzigingen tier of
channel sliders.

| kind tag | Betekenis |
| --- | --- |
| `<Tier value="1" />` | Master tier waarde. |
| `<Channel name="channelName" value="1" />` | Named StructureLib channel override. Repeat voor meerdere channels. |
| `<Facing value="north" />` | Standaard facing. |
| `<Rotation value="normal" />` | Standaard rotatie. |
| `<Flip value="none" />` | Standaard flip/mirror. |
| `<Orientation value="north:normal:none" />` | Facing, rotatie, en flip in one tag. |
| `<GregTechActiveController />` | GregTech alleen: renderen De controller met its active texture wanneer possible. |
| `<GregTechPlaceHatches />` | GregTech alleen: place normal GT hatches voor hatch-alleen voorbeeldweergave positions. zonder Deze, GT previews still gebruiken survival construct voor hatch-aware machines, but empty hatch positions fall back naar casing blokken. |

voor GregTech controllers, GuideNH now uses De zelfde StructureLib survival-voorbeeldweergave pad as De
export command. Deze fixes hatch-alleen positions that normal `construct()` kan niet populate terwijl
keeping fallback casings by Standaard.

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

Applies explicit isometric camera yaw/pitch/roll.

Als Deze tag wordt weggelaten, De scène keeps using De `<GameScene>` `perspective` preset. De Standaard
`isometric-north-east` preset is equivalent naar:

````md
<IsometricCamera yaw="225" pitch="30" />
````

| Attribuut | Betekenis |
| --- | --- |
| `yaw` | kommagetal |
| `pitch` | kommagetal |
| `roll` | kommagetal |

Example:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes iedere al-placed blok matching Een target blok id.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `id` | yes | blok id naar verwijderen, using `modid:block[:meta]` |

Deze is useful na importing Een structure wanneer you want naar verbergen specific blokken voor clarity.

Example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

Replaces al-placed blokken that match Een source blok id (en optionally Een partial tile entity NBT
pattern) met Een new blok. De zoeken kan be globale (alle filled blokken) of restricted naar a
bounding box.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `from` | yes | source blok naar match, using `modid:block[:meta]` |
| `from_nbt` | no | partial SNBT compound; Een blok matches alleen wanneer its tile entity NBT bevat alle listed keys |
| `to` | yes | replacement blok, using `modid:block[:meta]` |
| `to_nbt` | no | SNBT TileEntity compound naar apply naar De replacement |
| `x` | no | bounding box begin X; Als any of `x/y/z/dx/dy/dz` is present, De box modus is activated |
| `y` | no | bounding box begin Y |
| `z` | no | bounding box begin Z |
| `dx` | no | bounding box length on De X axis (Standaard `1`) |
| `dy` | no | bounding box hoogte on De Y axis (Standaard `1`) |
| `dz` | no | bounding box breedte/depth on De Z axis (Standaard `1`) |
| `formed` | no | whether replacement result controllers zou moeten be treated as formed during voorbeeldweergave sync; Standaard `false` |

Notes:

- wanneer none of `x/y/z/dx/dy/dz` are provided, alle filled blokken are scanned globally
- `from_nbt` is a **partial** match: alleen De keys listed in De pattern moet match; extra keys in
  De actual tile entity are ignored
- De replacement is performed via De zelfde blok placement pipeline as `<Block>`, so GregTech MetaTile
  en BartWorks tile entities are handled correctly
- Als De replacement places Een controller, `formed={false}` keeps that controller unformed during voorbeeldweergave

Example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills Een asuitgelijnd box met Een enkele blok Type, overwriting whatever was there voor.
Unlike `<Block>` (which targets Een enkele positie), `<PlaceBlock>` ondersteunt multi-blok regions via
`dx`/`dy`/`dz`, ordered as length, hoogte, en breedte/depth on De X/Y/Z axes.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `id` | yes | blok id, using `modid:block[:meta]` |
| `nbt` | no | SNBT TileEntity compound applied naar iedere placed blok |
| `x` | no | region begin X, Standaard `0` |
| `y` | no | region begin Y, Standaard `0` |
| `z` | no | region begin Z, Standaard `0` |
| `dx` | no | region length on De X axis, Standaard `1` |
| `dy` | no | region hoogte on De Y axis, Standaard `1` |
| `dz` | no | region breedte/depth on De Z axis, Standaard `1` |
| `formed` | no | whether placed controllers zou moeten be treated as formed during voorbeeldweergave sync; Standaard `false` |

Notes:

- alle blokken in De box are unconditionally placed (no prior-blok check)
- De NBT compound is copied Voor elke individual placement
- De zelfde blok placement pipeline as `<Block>` is gebruikt, so GregTech MetaTile en BartWorks tile entities
  are fully supported
- Als De region places one of more controllers, `formed={false}` keeps iedere affected controller unformed

Example:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

Expands one of more kind Annotaties onto iedere matching blok that al exists in De huidige scène.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `id` | yes | blok matcher in `modid:block[:meta]` form |

Regels:

- place it na De blokken of imported structures that it zou moeten match
- matching happens against De huidige scène status at parse time
- kind Annotaties gebruiken lokale coördinaten relative naar elke matched blok

Example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

Adds Een entity naar De voorbeeldweergave scène.

De attributen follow summon-style entity placement en SNBT data.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `id` | yes | entity Type id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, en registered mod entity ids in either `modid.entityName` of `modid:entityName` form are accepted |
| `x` | no | kommagetal X coördinaat De entity is centered on, Standaard `0.5` |
| `y` | no | kommagetal Y coördinaat at De bottom of De entity, Standaard `0` |
| `z` | no | kommagetal Z coördinaat De entity is centered on, Standaard `0.5` |
| `rotationY` | no | yaw in degrees, Standaard `-45` |
| `rotationX` | no | pitch in degrees, Standaard `0` |
| `data` | no | summon-style SNBT merged into De entity NBT voor spawn |
| `sceneEntityId` | no | stable scène-lokale entity id gebruikt by later `<Entity>` / `<RemoveEntity>` operations en by imported scène snapshots |
| `mount` | no | stable `sceneEntityId` of De vehicle that Deze entity zou moeten ride na spawn |
| `unmount` | no | boolean expressie that clears Deze entity's huidige stable mount relation na spawn of status replay |
| `baby` | no | boolean expressie forcing supported entities into baby form; omitted leaves De entity's normal age/status unchanged |
| `name` | no | voorbeeldweergave player naam wanneer `id` is `player`, `fakeplayer`, `minecraft:player`, of `minecraft:fakeplayer` |
| `uuid` | no | voorbeeldweergave player UUID wanneer using one of De player ids above |
| `showName` | no | boolean expressie controlling De voorbeeldweergave player nameplate, Standaard `true` voor player voorbeeldweergave ids |
| `showCape` | no | boolean expressie controlling De voorbeeldweergave player cape, Standaard `true` voor player voorbeeldweergave ids |
| `headRotation` | no | voorbeeldweergave player head rotatie as `x y z` degrees |
| `leftArmRotation` | no | voorbeeldweergave player left arm rotatie as `x y z` degrees |
| `rightArmRotation` | no | voorbeeldweergave player right arm rotatie as `x y z` degrees |
| `leftLegRotation` | no | voorbeeldweergave player left leg rotatie as `x y z` degrees |
| `rightLegRotation` | no | voorbeeldweergave player right leg rotatie as `x y z` degrees |
| `capeRotation` | no | voorbeeldweergave player cape rotatie as `x y z` degrees; defaults naar De standing-still angle `6 0 0` |

Notes:

- entity bounds participate in scène auto-centering en zichtbaar-layer filtering
- entity creation falls back gracefully wanneer De voorbeeldweergave wereld is niet ready yet, then binds on first renderen
- `sceneEntityId` is optioneel, but strongly recommended whenever later scène mutations need naar find, verwijderen, remount, of restore De zelfde logical entity zonder scanning by raw runtime id
- one `sceneEntityId` kan own more than one runtime entity instance; `<RemoveEntity sceneEntityId="..."/>` removes iedere entity currently registered naar that stable id
- `mount` links entities by stable scène id rather than by raw NBT passenger lists, so replay, import/export, voorbeeldweergave rebuild, en Ponder-animatie seeking kan alle restore De zelfde rider/vehicle relation deterministically
- `unmount={true}` clears De stable mount relation voor that entity voor any later mount is applied
- `baby={true}` currently ondersteunt voorbeeldweergave players, ageable mobs, vanilla zombies, en modded entities that expose stable `setChild(boolean)` of `setBaby(boolean)` style APIs
- kind-status entities are re-aligned naar their huidige positie na resizing so zweven en pick bounds stay centered on De gerenderd model
- player voorbeeldweergave ids maken Een client-side fake remote player so De normal player renderer en skin pipeline kan be gebruikt
- wanneer both `name` en `uuid` are omitted voor Een player voorbeeldweergave, GuideNH falls back naar `Steve` en De vanilla Standaard skin
- wanneer alleen `name` is given voor Een player voorbeeldweergave, GuideNH first tries naar oplossen De real online profile so skins en capes kan laden; Als lookup fails, it falls back naar Een stable offline UUID
- wanneer alleen `uuid` is given voor Een player voorbeeldweergave, GuideNH generates Een placeholder weergeven naam en still tries naar oplossen De skin van De profile
- `showName={false}` hides De voorbeeldweergave player's overhead naam zonder bypassing De normal player renderer
- `showCape={false}` hides De voorbeeldweergave player's cape terwijl still respecting De normal player renderen pad en Forge hooks
- player pose attributen gebruiken three space-separated floats mapped naar model `X Y Z` rotatie in degrees
- omitted head en limb rotatie attributen Behoud De normal vanilla idle pose; omitted `capeRotation` falls back naar De standing-still cape angle `6 0 0`
- player previews require Een active client wereld at parse time because Minecraft's player entity constructor kan niet be gemaakt worldless
- hovering Een entity shows its gelokaliseerd weergeven naam, of its aangepaste naam Als one was provided

Example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Stable-id mount example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

Removal en unmount example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
</GameScene>
````

Baby entity example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

voorbeeldweergave player pose example:

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

voorbeeldweergave player naam en cape example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

Removes iedere runtime entity currently registered naar one stable `sceneEntityId`.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `sceneEntityId` | yes | stable scène-lokale entity id naar verwijderen |
| `unmount` | no | boolean expressie that clears De stable mount relation voor removal |

Notes:

- Deze is De scène-side counterpart naar Ponder-animatie's `removeEntities`
- removal works on De indexed stable-id registry, so it doet niet need naar scan alle entities iedere frame
- Als meerdere imported of replayed entities share De zelfde `sceneEntityId`, they are verwijderd together

Example:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## Weer

`<Weather>` adds animated rain of snow directly naar a `GameScene`. Unlike Ponder-animatie Weer presets,
scène Weer is niet timeline-owned: it keeps looping during normal scène rendering, it doet niet
fade in of fade out, en it kan niet be paused of scrubbed independently. De renderer still uses De
zelfde precipitation geometry pad as Ponder-animatie Weer, so lokale voorbeeldweergave en site export stay aligned.

| Attribuut | Standaard | Description |
| --- | --- | --- |
| `weather` / `type` | `rain` | Weer kind. Supported waarden: `rain`, `snow`. |
| `x`, `z` | scène bounds | Covered precipitation columns. Een scalar targets one kolom. Arrays gebruiken endpoint pairs naar define one of more rectangles. |
| `density` | Type-specific | Coverage density. Higher waarden Behoud more precipitation columns active; lower waarden sparsify De effect. |

Notes:

- `<Weather>` ignores `y`; De vertical span is derived van De huidige scène bounds en van De
  highest precipitation-blocking blok in elke covered kolom.
- Als one axis has unmatched extra array waarden, De unmatched tail is ignored.
- Within one Weer declaration, rain en snow never stack on De zelfde `x/z` kolom. Als meerdere
  Weer tags overlap, earlier tags Behoud De shared columns.
- anders non-overlapping columns in De zelfde `GameScene` kan renderen rain en snow at De zelfde
  time.

Example:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## camera Center Behavior

Als no explicit `centerX/Y/Z` is given, GuideNH auto-centers De scène van De placed blok bounds. Als any explicit center coördinaat is Stel in, auto-centering is uitgeschakeld en missing coördinaten Standaard naar `0`.

## Opmerkingen over interactie

wanneer `interactive={true}` De scène ondersteunt rotatie, pan, zoomen, reset, annotatie toggles, en other UI controls exposed by De gids scherm.

- scènes spanning meerdere Y levels tonen Een zichtbaar-layer slider above De bottom edge
- StructureLib scènes kan Voeg toe Een hatch-markeren toggle button plus Een channel slider at De very bottom wanneer De imported metadata biedt them
- annotatie zweven takes priority over blok zweven; blok tooltips appear normally once no annotatie hotspot is being hovered
- StructureLib zweven keeps De blok naam on De first tooltip regel, adds structure-specific tekst starting on De second regel, en expands replacement candidates wanneer `Shift` is held

## Gerelateerde pagina's

- [Annotations](Annotations)
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [Examples](Examples)
