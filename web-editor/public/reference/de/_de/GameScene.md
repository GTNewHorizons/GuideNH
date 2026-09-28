# GameScene


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

`<GameScene>` ist GuideNH's 3D Vorschau Tag. `<Scene>` ist ein alias mit Die gleich behavior.

## Szene Attribut

| Attribut | Typ | Standard | Bedeutung |
| --- | --- | --- | --- |
| `width` | Ganzzahl | `256` | Ansichtsbereich Breite in Pixel |
| `height` | Ganzzahl | `192` | Ansichtsbereich Höhe in Pixel |
| `zoom` | Gleitkommazahl | `1.0` | Kamera Zoom Multiplikator |
| `perspective` | Zeichenkette | `isometric-north-east` | Kamera Voreinstellung |
| `rotateX` | Gleitkommazahl | auto | explizit X Drehung Überschreibung |
| `rotateY` | Gleitkommazahl | auto | explizit Y Drehung Überschreibung |
| `rotateZ` | Gleitkommazahl | auto | explizit Z Drehung Überschreibung |
| `offsetX` | Gleitkommazahl | auto | Bildschirm-space horizontal Schwenk |
| `offsetY` | Gleitkommazahl | auto | Bildschirm-space vertikal Schwenk |
| `centerX` | Gleitkommazahl | auto | explizit Welt Drehung Mitte X |
| `centerY` | Gleitkommazahl | auto | explizit Welt Drehung Mitte Y |
| `centerZ` | Gleitkommazahl | auto | explizit Welt Drehung Mitte Z |
| `interactive` | Boolesch Ausdruck | `true` | aktiviert moVerwenden Sie interaction |
| `showBackground` | Boolesch Ausdruck | `true` | zeigt Die Szene Hintergrund fill und Rand |
| `allowLayerSlider` | Boolesch | `true` | zeigt Die vertikalen Ebenenregler |
| `gridButtonEnabled` | Boolesch | `true` | zeigt Die Bodengitter Umschalttaste |
| `showGrid` | Boolesch | `false` | anfänglich Sichtbarkeit von Die Bodengitter |

## Blockstatistik-Overlay

Szenen dass enthalten Blöcke enable Die Block-stat Umschalttaste by Standard. Fügen Sie hinzu ein `<BlockStats>`
untergeordnetes Element wenn you want zu Überschreibung seine Modus, Positionierung, Filter, Sichtbarkeit, oder Größe. Die Liste ist
zwischengespeichert und nur neu aufgebaut wenn Die Szene Blöcke, Ponder timeline Zustand, StructureLib Auswahl, oder
Block-stat Einstellungen ändern; normale Darstellung verwendet erneut Die vorbereiteten Zeilen. lange Listen sind gekürzt zu
`maxWidth` und `maxHeight`; Wenn those sind omitted, jede ist Die larger von Die festen `224` by `96` Pixel und
40% von Die Szene Größe.
Overflow erhält ziehbare scrollbars, und Die moVerwenden Sie wheel scrolls Die Liste während Die Mauszeiger ist
über Die overlay. Halten Sie die Umschalttaste zu wheel-scrollen horizontal.

in automatisch Modus, GuideNH scans Die Szene's filled Blöcke und resolves jede Block zu Die Element
stack users normally Siehe. Blöcke dass enthalten mehrere sichtbar components kann contribute mehrere
Elemente von Die gleich Koordinate; dies enthält AE2 cable bus parts und facades, ForgeMultipart part
drops, und Carpenters' Blöcke covers oder overlays wenn those mods sind installed. Counts sind grouped
by `item:meta` und sorted by count.

automatisch lists kann auch sein docked außerhalb Die Szene mit `dock="left"`, `dock="top"`,
`dock="right"`, oder `dock="bottom"`. Docked lists wrap in extra columns oder Zeilen based auf Die
attached Seite length, reserve Layout space, und avoid Die Szene button Spalte auf Die rechts. Klick ein
Element in Eine automatisch Liste zu hervorheben alle passend Szene placements mit ihre aufgelöst collision
boxes using Eine immer-auf-oben face overlay; Klick Die gleich Element again zu clear Die hervorheben. Counts
sind gerendert durch Die ItemStack stack-Größe overlay. Setzen Sie `showNames={true}` zu append Die count
nach jede Name als well, und Hover Eine Element zu Siehe Die exact Block count in Die tooltip.

Filter kann ausblenden allgemein Blöcke oder anzeigen nur ausgewählt Blöcke:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

verwenden manual Modus wenn Eine Leitfaden wants zu anzeigen Eine planned material Liste stattdessen von Die literal Szene
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

## Overlays des Debug-Modus

wenn Die `enableDebugMode` Option ist aktiviert in Die GuideNH mod config, Die following extra
overlays werden zu verfügbar in Die 3D Szene Vorschau.

### Grid Koordinate Labels

wenn debug Modus ist **auf** und Die Bodengitter ist **sichtbar**, Koordinate labels sind gerendert
unter jede grid Linie:

- **X-axis numbers** sind angezeigt along Die nahe edge von Die grid (north/−Z edge in Die Standard
  `isometric-north-east` Kamera).  jede Ganzzahl X Welt-Koordinate erhält Eine Beschriftung.
- **Z-axis numbers** sind angezeigt along Die nahe edge von Die grid (east/+X edge).  jede Ganzzahl
  Z Welt-Koordinate erhält Eine Beschriftung.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) sind drawn at Die midpoint von jede
  respective grid edge.

Koordinaten follow Die aktuell Welt X/Z Werte stored in Die Szene level, so Sie kann sein
negative wenn Die structure enthält Blöcke mit negative Koordinaten.

Die grid Umschalttaste ist **immer aktiviert** während debug Modus ist active, regardless von Die
`gridButtonEnabled` Attribut, so you kann anzeigen oder ausblenden Die grid und seine labels at beliebig time.
Die Standard grid Sichtbarkeit (`showGrid`) ist nicht affected.

### Block Koordinate Tooltip

wenn debug Modus ist **auf** und Die Mauszeiger hovers über Eine Block innerhalb Die Szene, Eine second
tooltip ist gerendert über Die primary Block tooltip, showing Die Welt-space Block Position
als `X, Y, Z` in gold Text.

Wenn Die Koordinate Tooltip would sein gekürzt at Die oben von Die Bildschirm it automatisch snaps
unter Die Mauszeiger area stattdessen (magnetic snapping).

## Perspektivvorlagen

Accepted `perspective` Werte:

- `isometric-north-east`
- `isometric-north-west`
- `up`

unbekannt Werte fall back zu `isometric-north-east`.

## Inhalt Embedding und Text Wrapping

beliebig Block-level Tag — including `<GameScene>` — unterstützt zwei optional Attribut dass control
how it ist embedded in Die Seite, mirroring Microsoft Word's "Text Wrapping" Optionen.

| Attribut | Werte | Standard | Bedeutung |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | Text-wrapping Modus |
| `align` | `left` · `center` · `right` | `left` | horizontal alignment |

### Umbruchmodi

| Modus | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | in Linie mit Text | Standard flow: Szene occupies seine eigene vertikal slot (嵌入型) |
| `square` | Square | Szene floats Links oder rechts; surrounding Text wraps in Eine rectangle around it (方形环绕) |
| `tight` | Tight | Tighter wrap; equivalent zu `square` in dies Layout system (紧密型) |
| `through` | durch | durch-wrap; equivalent zu `square` in dies Layout system (穿越型) |
| `top-bottom` | oben und unten | Text nur über und unter, nicht beside; respects `align` für horizontal Positionierung (上下型) |
| `behind` | Behind Text | Block rendert behind surrounding Text; respects `align` (衬于文字下方) |
| `front` | in front von Text | Block rendert in front von surrounding Text; respects `align` (浮于文字上方) |

### Beispiele

Links-floating Szene — Text in Die nächste paragraph wraps zu Die rechts:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Text, der rechts neben der Szene fließt …
````

rechts-floating Szene:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Text, der links neben der Szene fließt …
````

Centred Szene (nein Text wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline in Text (flow context) — Text wraps around Eine small Szene:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

## Beispiel

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Import Beispiele

diese Beispiele focus auf Die Szene-Seite behavior dass most oft trips people up wenn importing
structures.

StructureLib import mit explizit facing, Drehung, flip, und offsets:

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

GregTech controllers stay unformed by Standard, even wenn Die imported multiblock ist otherwise
gültig:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

Setzen Sie `formed={true}` nur wenn Die Vorschau sollte intentionally anzeigen Die formed controller Zustand:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

Die gleich Standard auch wendet ein zu controllers placed directly mit `<Block>`, including GregTech
controllers dass rely auf surrounding multiblock casings:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

Simple Block-nur layouts kann weiterhin sein authored directly und remain compatible mit multiblock
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

## Szene untergeordnetes Element Elements

GuideNH derzeit registers diese Szene untergeordnetes Element Tags:

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
- Annotation Tags such als `<BoxAnnotation>` und `<LineAnnotation>`

## Szene Sounds

`<PlaySound>` kann sein placed innerhalb `<GameScene>` zu play sounds von Szene interaction oder timeline
Eintrag. unterstützt triggers sind:

- `click`, Die Standard
- `hover`, fired once wenn Die Mauszeiger enters Die Szene
- `enter`, fired once wenn Die Szene erste rendert

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

wenn `x`, `y`, und `z` sind bereitgestellt, Die sound volume ist attenuated in Bildschirm space von Die
projected Szene Koordinate zu Die Klick Punkt oder Szene Mitte. `radius` Standards zu 75% von Die
shorter Szene Seite, und `minVolume` Standards zu `0.15`.

## `<BlockStats>` und `<BlockStat>`

Declares oder customizes Eine Blockstatistik-Overlay. Szenen mit Blöcke enable Die automatisch toggle
button even wenn dies untergeordnetes Element ist omitted. Adding eins oder mehr `<BlockStat>` untergeordnete Elemente switches Die
overlay zu manual statistics Modus für dass Szene.

`<BlockStats>` Attribut:

| Attribut | erforderlich | Standard | Bedeutung |
| --- | --- | --- | --- |
| `visible` | nein | config, Standard `false` | anfänglich overlay Sichtbarkeit |
| `buttonEnabled` | nein | config, Standard `true` | zeigt Die Block statistics Umschalttaste |
| `mode` | nein | `auto` | `auto` oder `manual`; untergeordnetes Element `<BlockStat>` Einträge force manual Modus |
| `corner` | nein | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, oder `bottomLeft` |
| `dock` | nein | `inside` | automatisch lists kann attach zu `inside`, `left`, `top`, `right`, oder `bottom`; manual Modus immer verwendet Die innerhalb overlay |
| `showNames` | nein | `false` | whether zu anzeigen Element names beside icons; wenn aktiviert Die count ist auch appended nach Die Name |
| `filterMode` | nein | `blacklist` | `blacklist` oder `whitelist` |
| `filter` | nein | leer | Element Schlüssel such als `minecraft:stone` oder `minecraft:stone:0`, separated by spaces, commas, oder semicolons |
| `maxWidth` | nein | Die larger von `224` px und 40% von Die Szene Breite | maximum overlay Breite in Pixel vor horizontal Scrollen |
| `maxHeight` | nein | Die larger von `96` px und 40% von Die Szene Höhe | maximum overlay Höhe in Pixel vor vertikal Scrollen |

`<BlockStat>` Attribut:

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `item` | ja, außer `id` ist verwendet | Element id angezeigt in Die Liste |
| `id` | ja, außer `item` ist verwendet | existing Element-stack Attribut form |
| `count` | nein | displayed count; omitting it zeigt Die Zeile once, und `count="0"` hides Die Zeile |

Beispiel:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

Places Eine Block in Die Vorschau Welt.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `id` | ja, außer `ore` ist verwendet | Block id |
| `ore` | nein | ore dictionary Name; Die erste passend stack muss auflösen zu Eine Block Element |
| `x` | nein | Ganzzahl Welt X, Standard `0` |
| `y` | nein | Ganzzahl Welt Y, Standard `0` |
| `z` | nein | Ganzzahl Welt Z, Standard `0` |
| `meta` | nein | Ganzzahl Block metadata |
| `facing` | nein | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | nein | SNBT TileEntity compound |
| `formed` | nein | whether Die placed structure controller sollte sein treated als formed during Vorschau sync; Standard `false` |

Hinweise:

- `ore` takes precedence über `id`; Wenn GregTech ist installed, Die ausgewählt stack ist unified durch `GTOreDictUnificator.setStack(...)`
- Wenn `meta` ist omitted und ein `ore` übereinstimmen carries concrete non-wildcard Element damage, dass damage ist verwendet vor Die `facing` Fallback
- Wenn `meta` ist omitted, some Blöcke derive Eine sensible Standard von `facing`
- Wenn `nbt` erstellt Eine TileEntity successfully, Die Vorschau verwendet it
- Setzen Sie `formed={false}` wenn Eine controller-based structure sollte stay unformed in Vorschau even though Die surrounding structure ist otherwise gültig

Beispiel:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

lädt Eine extern structure Datei in Die Szene.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `src` | ja | structure Ressource Pfad |
| `x` | nein | Ganzzahl Übersetzung X (alias für `offsetX`) |
| `y` | nein | Ganzzahl Übersetzung Y (alias für `offsetY`) |
| `z` | nein | Ganzzahl Übersetzung Z (alias für `offsetZ`) |
| `offsetX` | nein | Ganzzahl Übersetzung X (bevorzugt über `x`) |
| `offsetY` | nein | Ganzzahl Übersetzung Y, clamped zu `[0, worldHeight-1]` (bevorzugt über `y`) |
| `offsetZ` | nein | Ganzzahl Übersetzung Z (bevorzugt über `z`) |
| `formed` | nein | whether imported structure controllers sollte sein treated als formed during Vorschau sync; Standard `false` |

unterstützt formats:

- SNBT Text
- gzipped binary NBT
- uncompressed binary NBT

erforderlich structure Schlüssel:

- `palette`
- `blocks`

Beispiel:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

Imports Eine StructureLib multiblock Vorschau by controller id.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `controller` | ja | controller Block id, using `modid:block[:meta]` |
| `name` | nein | optional binding Name verwendet by `showWhenStructure` auf Annotationen, templates, und sounds |
| `piece` | nein | StructureLib piece Name Überschreibung |
| `facing` | nein | facing Überschreibung passed zu Die importer |
| `rotation` | nein | Drehung Überschreibung passed zu Die importer |
| `flip` | nein | flip/mirror Überschreibung passed zu Die importer |
| `channel` | nein | Ganzzahl channel Überschreibung für channel-aware structures |
| `offsetX` | nein | Ganzzahl X offset applied zu alle placed Blöcke (Standard `0`) |
| `offsetY` | nein | Ganzzahl Y offset applied zu alle placed Blöcke, clamped zu `[0, worldHeight-1]` (Standard `0`) |
| `offsetZ` | nein | Ganzzahl Z offset applied zu alle placed Blöcke (Standard `0`) |
| `formed` | nein | whether imported StructureLib controllers sollte sein treated als formed during Vorschau sync; Standard `false` |

Hinweise:

- Die imported structure Starts von Szene `0 0 0`; Die controller ist nicht forced zu sein placed at `0 0 0`
- dies Tag aktiviert StructureLib-bestimmten tooltip, hatch hervorheben, und channel slider UI wenn metadata ist verfügbar
- controller passend unterstützt Die GTNH-style `modid:block:meta` form
- verwenden `name` wenn Die Szene enthält mehrere StructureLib imports und another Tag needs zu Ziel eins bestimmten structure Zustand
- `facing`, `rotation`, und `flip` verwenden Die gleich orientation vocabulary als StructureLib export; wenn Eine requested combination ist nicht allowed by Die controller, GuideNH falls back zu Die erste gültig alignment automatisch
- GregTech controller previews now Standard zu Die controller's opposite horizontal facing von Die older Vorschau orientation, rotating Die Vorschau front by 180 degrees around Die Y axis
- Setzen Sie `formed={false}` wenn Die imported controller sollte remain visibly unformed; dies ist Die unterstützt alternative zu shipping intentionally broken NBT

Beispiel:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

Structure-aware Annotation und sound Beispiel:

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

StructureLib Standards kann auch sein supplied als untergeordnetes Element Tags. diese Standards sind part von Die Szene's
anfänglich interactive Zustand, so Die reset-view button restores sie nach Die user Änderungen tier oder
channel sliders.

| untergeordnetes Element Tag | Bedeutung |
| --- | --- |
| `<Tier value="1" />` | Master tier Wert. |
| `<Channel name="channelName" value="1" />` | Named StructureLib channel Überschreibung. Repeat für mehrere Kanäle. |
| `<Facing value="north" />` | Standard facing. |
| `<Rotation value="normal" />` | Standard Drehung. |
| `<Flip value="none" />` | Standard flip/mirror. |
| `<Orientation value="north:normal:none" />` | Facing, Drehung, und flip in eins Tag. |
| `<GregTechActiveController />` | GregTech nur: rendern Die controller mit seine active texture wenn possible. |
| `<GregTechPlaceHatches />` | GregTech nur: platzieren normal GT hatches für hatch-nur Vorschau positions. ohne dies, GT previews weiterhin verwenden survival construct für hatch-aware machines, but leer hatch positions fall back zu casing Blöcke. |

für GregTech controllers, GuideNH now verwendet Die gleich StructureLib survival-Vorschau Pfad als Die
export command. dies fixes hatch-nur positions dass normal `construct()` kann nicht populate während
keeping Fallback casings by Standard.

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

wendet ein explizit isometric Kamera yaw/pitch/roll.

Wenn dies Tag ist omitted, Die Szene keeps using Die `<GameScene>` `perspective` Voreinstellung. Die Standard
`isometric-north-east` Voreinstellung ist equivalent zu:

````md
<IsometricCamera yaw="225" pitch="30" />
````

| Attribut | Bedeutung |
| --- | --- |
| `yaw` | Gleitkommazahl |
| `pitch` | Gleitkommazahl |
| `roll` | Gleitkommazahl |

Beispiel:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes jede bereits-placed Block passend Eine Ziel Block id.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `id` | ja | Block id zu entfernen, using `modid:block[:meta]` |

dies ist useful nach importing Eine structure wenn you want zu ausblenden bestimmten Blöcke für clarity.

Beispiel:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

Replaces bereits-placed Blöcke dass übereinstimmen Eine Quelle Block id (und optionally Eine partial tile entity NBT
pattern) mit Eine new Block. Die Suche kann sein globale (alle filled Blöcke) oder restricted zu ein
bounding box.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `from` | ja | Quelle Block zu übereinstimmen, using `modid:block[:meta]` |
| `from_nbt` | nein | partial SNBT compound; Eine Block stimmt überein nur wenn seine tile entity NBT enthält alle listed Schlüssel |
| `to` | ja | replacement Block, using `modid:block[:meta]` |
| `to_nbt` | nein | SNBT TileEntity compound zu apply zu Die replacement |
| `x` | nein | bounding box Start X; Wenn beliebig von `x/y/z/dx/dy/dz` ist present, Die box Modus ist activated |
| `y` | nein | bounding box Start Y |
| `z` | nein | bounding box Start Z |
| `dx` | nein | bounding box length auf Die X axis (Standard `1`) |
| `dy` | nein | bounding box Höhe auf Die Y axis (Standard `1`) |
| `dz` | nein | bounding box Breite/depth auf Die Z axis (Standard `1`) |
| `formed` | nein | whether replacement Ergebnis controllers sollte sein treated als formed during Vorschau sync; Standard `false` |

Hinweise:

- wenn none von `x/y/z/dx/dy/dz` sind bereitgestellt, alle filled Blöcke sind scanned globally
- `from_nbt` ist ein **partial** übereinstimmen: nur Die Schlüssel listed in Die pattern muss übereinstimmen; extra Schlüssel in
  Die aktuell tile entity sind ignored
- Die replacement ist performed via Die gleich Block Positionierung pipeline als `<Block>`, so GregTech MetaTile
  und BartWorks tile entities sind handled correctly
- Wenn Die replacement places Eine controller, `formed={false}` keeps dass controller unformed during Vorschau

Beispiel:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills Eine axis-aligned box mit Eine einzeln Block Typ, overwriting whatever was there vor.
Unlike `<Block>` (which targets Eine einzeln Position), `<PlaceBlock>` unterstützt multi-Block regions via
`dx`/`dy`/`dz`, ordered als length, Höhe, und Breite/depth auf Die X/Y/Z axes.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `id` | ja | Block id, using `modid:block[:meta]` |
| `nbt` | nein | SNBT TileEntity compound applied zu jede placed Block |
| `x` | nein | region Start X, Standard `0` |
| `y` | nein | region Start Y, Standard `0` |
| `z` | nein | region Start Z, Standard `0` |
| `dx` | nein | region length auf Die X axis, Standard `1` |
| `dy` | nein | region Höhe auf Die Y axis, Standard `1` |
| `dz` | nein | region Breite/depth auf Die Z axis, Standard `1` |
| `formed` | nein | whether placed controllers sollte sein treated als formed during Vorschau sync; Standard `false` |

Hinweise:

- alle Blöcke in Die box sind unconditionally placed (nein prior-Block check)
- Die NBT compound ist copied für jede individual Positionierung
- Die gleich Block Positionierung pipeline als `<Block>` ist verwendet, so GregTech MetaTile und BartWorks tile entities
  sind fully unterstützt
- Wenn Die region places eins oder mehr controllers, `formed={false}` keeps jede affected controller unformed

Beispiel:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

Expands eins oder mehr untergeordnetes Element Annotationen onto jede passend Block dass bereits exists in Die aktuell Szene.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `id` | ja | Block matcher in `modid:block[:meta]` form |

Regeln:

- platzieren it nach Die Blöcke oder imported structures dass it sollte übereinstimmen
- passend happens against Die aktuell Szene Zustand at parsen time
- untergeordnetes Element Annotationen verwenden lokal Koordinaten relativ zu jede passend Block

Beispiel:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

Adds Eine entity zu Die Vorschau Szene.

Die Attribut follow summon-style entity Positionierung und SNBT Daten.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `id` | ja | entity Typ id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, und registriert mod entity ids in entweder `modid.entityName` oder `modid:entityName` form sind accepted |
| `x` | nein | Gleitkommazahl X Koordinate Die entity ist centered auf, Standard `0.5` |
| `y` | nein | Gleitkommazahl Y Koordinate at Die unten von Die entity, Standard `0` |
| `z` | nein | Gleitkommazahl Z Koordinate Die entity ist centered auf, Standard `0.5` |
| `rotationY` | nein | yaw in degrees, Standard `-45` |
| `rotationX` | nein | pitch in degrees, Standard `0` |
| `data` | nein | summon-style SNBT merged in Die entity NBT vor spawn |
| `sceneEntityId` | nein | stable Szene-lokal entity id verwendet by later `<Entity>` / `<RemoveEntity>` operations und by imported Szene snapshots |
| `mount` | nein | stable `sceneEntityId` von Die vehicle dass dies entity sollte ride nach spawn |
| `unmount` | nein | Boolesch Ausdruck dass clears dies entity's aktuell stable mount relation nach spawn oder Zustand replay |
| `baby` | nein | Boolesch Ausdruck forcing unterstützt entities in baby form; omitted leaves Die entity's normal age/Zustand unchanged |
| `name` | nein | Vorschau player Name wenn `id` ist `player`, `fakeplayer`, `minecraft:player`, oder `minecraft:fakeplayer` |
| `uuid` | nein | Vorschau player UUID wenn using eins von Die player ids über |
| `showName` | nein | Boolesch Ausdruck controlling Die Vorschau player nameplate, Standard `true` für player Vorschau ids |
| `showCape` | nein | Boolesch Ausdruck controlling Die Vorschau player cape, Standard `true` für player Vorschau ids |
| `headRotation` | nein | Vorschau player head Drehung als `x y z` degrees |
| `leftArmRotation` | nein | Vorschau player Links arm Drehung als `x y z` degrees |
| `rightArmRotation` | nein | Vorschau player rechts arm Drehung als `x y z` degrees |
| `leftLegRotation` | nein | Vorschau player Links leg Drehung als `x y z` degrees |
| `rightLegRotation` | nein | Vorschau player rechts leg Drehung als `x y z` degrees |
| `capeRotation` | nein | Vorschau player cape Drehung als `x y z` degrees; Standards zu Die standing-weiterhin angle `6 0 0` |

Hinweise:

- entity bounds participate in Szene auto-centering und sichtbar-layer filtering
- entity creation falls back gracefully wenn Die Vorschau Welt ist nicht ready yet, dann binds auf erste rendern
- `sceneEntityId` ist optional, but strongly recommended whenever later Szene mutations need zu find, entfernen, remount, oder restore Die gleich logischen entity ohne scanning by roh Laufzeit id
- eins `sceneEntityId` kann eigene mehr als eins Laufzeit entity instance; `<RemoveEntity sceneEntityId="..."/>` removes jede entity derzeit registriert zu dass stable id
- `mount` Links entities by stable Szene id rather als by roh NBT passenger lists, so replay, import/export, Vorschau rebuild, und Ponder seeking kann alle restore Die gleich rider/vehicle relation deterministically
- `unmount={true}` clears Die stable mount relation für dass entity vor beliebig later mount ist applied
- `baby={true}` derzeit unterstützt Vorschau players, ageable mobs, vanilla zombies, und modded entities dass expose stable `setChild(boolean)` oder `setBaby(boolean)` style APIs
- untergeordnetes Element-Zustand entities sind re-aligned zu ihre aktuell Position nach resizing so Hover und pick bounds stay centered auf Die gerendert model
- player Vorschau ids erstellen Eine client-Seite fake remote player so Die normal player renderer und skin pipeline kann sein verwendet
- wenn beide `name` und `uuid` sind omitted für Eine player Vorschau, GuideNH falls back zu `Steve` und Die vanilla Standard skin
- wenn nur `name` ist given für Eine player Vorschau, GuideNH erste tries zu auflösen Die real online profile so skins und capes kann laden; Wenn lookup fails, it falls back zu Eine stable offline UUID
- wenn nur `uuid` ist given für Eine player Vorschau, GuideNH generates Eine placeholder anzeigen Name und weiterhin tries zu auflösen Die skin von Die profile
- `showName={false}` hides Die Vorschau player's overhead Name ohne bypassing Die normal player renderer
- `showCape={false}` hides Die Vorschau player's cape während weiterhin respecting Die normal player rendern Pfad und Forge hooks
- player pose Attribut verwenden drei space-separated floats mapped zu model `X Y Z` Drehung in degrees
- omitted head und limb Drehung Attribut Beibehalten Die normal vanilla idle pose; omitted `capeRotation` falls back zu Die standing-weiterhin cape angle `6 0 0`
- player previews require Eine active client Welt at parsen time becaVerwenden Sie Minecraft's player entity constructor kann nicht sein erstellt Weltless
- Beim Überfahren Eine entity zeigt seine lokalisiert anzeigen Name, oder seine benutzerdefiniert Name Wenn eins was bereitgestellt

Beispiel:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Stable-id mount Beispiel:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

Removal und unmount Beispiel:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
</GameScene>
````

Baby entity Beispiel:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

Vorschau player pose Beispiel:

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

Vorschau player Name und cape Beispiel:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

Removes jede Laufzeit entity derzeit registriert zu eins stable `sceneEntityId`.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `sceneEntityId` | ja | stable Szene-lokal entity id zu entfernen |
| `unmount` | nein | Boolesch Ausdruck dass clears Die stable mount relation vor removal |

Hinweise:

- dies ist Die Szene-Seite counterpart zu Ponder's `removeEntities`
- removal works auf Die indexed stable-id registry, so it tut nicht need zu scan alle entities jede frame
- Wenn mehrere imported oder replayed entities share Die gleich `sceneEntityId`, Sie sind entfernt together

Beispiel:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## Weather

`<Weather>` adds animated rain oder snow directly zu ein `GameScene`. Unlike Ponder weather presets,
Szene weather ist nicht timeline-owned: it keeps looping during normal Szene Darstellung, it tut nicht
fade in oder fade out, und it kann nicht sein paused oder scrubbed independently. Die renderer weiterhin verwendet Die
gleich precipitation Geometrie Pfad als Ponder weather, so lokal Vorschau und site export stay aligned.

| Attribut | Standard | Beschreibung |
| --- | --- | --- |
| `weather` / `type` | `rain` | Weather kind. unterstützt Werte: `rain`, `snow`. |
| `x`, `z` | Szene bounds | Covered precipitation columns. Eine scalar targets eins Spalte. Arrays verwenden endpoint pairs zu define eins oder mehr rectangles. |
| `density` | Typ-bestimmten | Coverage density. Higher Werte Beibehalten mehr precipitation columns active; lower Werte sparsify Die effect. |

Hinweise:

- `<Weather>` ignores `y`; Die vertikal span ist derived von Die aktuell Szene bounds und von Die
  highest precipitation-blocking Block in jede covered Spalte.
- Wenn eins axis hat unmatched extra array Werte, Die unmatched tail ist ignored.
- Within eins weather declaration, rain und snow nie stack auf Die gleich `x/z` Spalte. Wenn mehrere
  weather Tags overlap, earlier Tags Beibehalten Die shared columns.
- anders non-overlapping columns in Die gleich `GameScene` kann rendern rain und snow at Die gleich
  time.

Beispiel:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## Kamera Mitte Behavior

Wenn nein explizit `centerX/Y/Z` ist given, GuideNH auto-centers Die Szene von Die placed Block bounds. Wenn beliebig explizit Mitte Koordinate ist Setzen Sie, auto-centering ist deaktiviert und fehlend Koordinaten Standard zu `0`.

## Interaction Hinweise

wenn `interactive={true}` Die Szene unterstützt Drehung, Schwenk, Zoom, reset, Annotation toggles, und andere UI controls exposed by Die Leitfaden Bildschirm.

- Szenen spanning mehrere Y levels anzeigen Eine sichtbar-layer slider über Die unten edge
- StructureLib Szenen kann Fügen Sie hinzu Eine hatch-hervorheben Umschalttaste plus Eine channel slider at Die very unten wenn Die imported metadata bietet sie
- Annotation Hover takes priority über Block Hover; Block tooltips appear normally once nein Annotation hotspot ist being hovered
- StructureLib Hover keeps Die Block Name auf Die erste Tooltip Linie, adds structure-bestimmten Text Starting auf Die second Linie, und expands replacement candidates wenn `Shift` ist held

## Verwandte Seiten

- [Annotations](Annotations)
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [Examples](Examples)
