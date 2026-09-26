# GameScene

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


`<GameScene>` is GuideNH's 3D podgląd tag. `<Scene>` is alias z  ten sam zachowanie.

## Atrybuty sceny

| Atrybut | Typ | Domyślne | Znaczenie |
| --- | --- | --- | --- |
| `width` | liczba całkowita | `256` | viewport szerokość in piksele |
| `height` | liczba całkowita | `192` | viewport wysokość in piksele |
| `zoom` | liczba zmiennoprzecinkowa | `1.0` | kamera powiększenie multiplier |
| `perspective` | ciąg znaków | `isometric-north-east` | kamera preset |
| `rotateX` | liczba zmiennoprzecinkowa | auto | jawny X obrót nadpisywać |
| `rotateY` | liczba zmiennoprzecinkowa | auto | jawny Y obrót nadpisywać |
| `rotateZ` | liczba zmiennoprzecinkowa | auto | jawny Z obrót nadpisywać |
| `offsetX` | liczba zmiennoprzecinkowa | auto | przestrzeń ekranu horizontal pan |
| `offsetY` | liczba zmiennoprzecinkowa | auto | przestrzeń ekranu vertical pan |
| `centerX` | liczba zmiennoprzecinkowa | auto | jawny świat obrót środek X |
| `centerY` | liczba zmiennoprzecinkowa | auto | jawny świat obrót środek Y |
| `centerZ` | liczba zmiennoprzecinkowa | auto | jawny świat obrót środek Z |
| `interactive` | wartość logiczna wyrażenie | `true` | enables mouse interaction |
| `showBackground` | wartość logiczna wyrażenie | `true` | shows  scena tło fill i obramowanie |
| `allowLayerSlider` | wartość logiczna | `true` | shows  vertical layer slider |
| `gridButtonEnabled` | wartość logiczna | `true` | shows  floor grid toggle button |
| `showGrid` | wartość logiczna | `false` | początkowy widoczność of  floor grid |

## Nakładka statystyk bloków

sceny który zawierać bloki enable  blok-stat toggle button by Domyślne. Dodaj a `<BlockStats>`
element podrzędny gdy you want do nadpisywać jego tryb, placement, filters, widoczność, lub rozmiar.  lista is
cached i tylko rebuilt gdy  scena bloki, Animacja Ponder timeline stan, StructureLib wybór, lub
blok-stat settings zmiana; normal renderowanie reuses  prepared wiersze. Long lists są clipped do
`maxWidth` i `maxHeight`; Jeśli those są pominięty, każdy is  larger of  stały `224` by `96` piksele i
40% of  scena rozmiar.
Overflow receives draggable scrollbars, i  mouse wheel scrolls  lista podczas  cursor is
over  overlay. Hold Shift do wheel-przewijanie horizontally.

In automatyczny tryb, GuideNH scans  scena's filled bloki i resolves każdy blok do  element
stos users normally Zobacz. bloki który zawierać wiele widoczny components może contribute wiele
items z  ten sam współrzędna; Ten includes AE2 cable bus parts i facades, ForgeMultipart part
drops, i Carpenters' bloki covers lub overlays gdy those mods są installed. Counts są grouped
by `item:meta` i sorted by count.

automatyczny lists może także be docked poza  scena z `dock="left"`, `dock="top"`,
`dock="right"`, lub `dock="bottom"`. Docked lists zawijanie do extra kolumny lub wiersze based on 
attached strona length, reserve układ przestrzeń, i avoid  scena button kolumna on  prawo. kliknięcie an
element in automatyczny lista do wyróżniać wszystkie pasujący scena placements z ich rozwiązany collision
boxes używając zawsze-on-góra face overlay; kliknięcie  ten sam element again do clear  wyróżniać. Counts
są wyrenderowany through  ItemStack stos-rozmiar overlay. Ustaw `showNames={true}` do append  count
po każdy nazwa as well, i najechanie element do Zobacz  exact blok count in  podpowiedź.

Filters może ukrywać wspólny bloki lub pokazywać tylko wybrany bloki:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

używać manual tryb gdy przewodnik wants do pokazywać planned material lista zamiast tego of  literal scena
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

## Nakładki trybu debugowania

gdy  `enableDebugMode` opcja is włączony in  GuideNH mod config,  following extra
overlays become dostępny in  3D scena podgląd.

### Grid współrzędna Labels

gdy debug tryb is **on** i  floor grid is **widoczny**, współrzędna labels są wyrenderowany
below każdy grid wiersz:

- **X-oś numbers** są wyświetlany wzdłuż  near krawędź of  grid (north/−Z krawędź in  Domyślne
  `isometric-north-east` kamera).  każdy liczba całkowita X świat-współrzędna receives etykieta.
- **Z-oś numbers** są wyświetlany wzdłuż  near krawędź of  grid (east/+X krawędź).  każdy liczba całkowita
  Z świat-współrzędna receives etykieta.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) są drawn at  midpoint of każdy
  respective grid krawędź.

współrzędne follow  actual świat X/Z wartości stored in  scena level, so jeden może be
negative gdy  struktura zawiera bloki z negative współrzędne.

 grid toggle button is **zawsze włączony** podczas debug tryb is active, regardless of 
`gridButtonEnabled` Atrybut, so you może pokazywać lub ukrywać  grid i jego labels at any czas.
 Domyślne grid widoczność (`showGrid`) is nie affected.

### blok współrzędna podpowiedź

gdy debug tryb is **on** i  cursor hovers over blok wewnątrz  scena, second
podpowiedź is wyrenderowany nad  primary blok podpowiedź, showing  przestrzeń świata blok pozycja
as `X, Y, Z` in gold tekst.

Jeśli  współrzędna podpowiedź would be clipped at  góra of  ekran it automatycznie snaps
below  cursor area zamiast tego (magnetic snapping).

## Presety perspektywy

Accepted `perspective` wartości:

- `isometric-north-east`
- `isometric-north-west`
- `up`

nieznany wartości fall tył do `isometric-north-east`.

## Osadzanie treści i zawijanie tekstu

Any blok-level tag — including `<GameScene>` — obsługuje dwa opcjonalny atrybuty który control
how it is embedded in  strona, mirroring Microsoft Word's "tekst Wrapping" opcje.

| Atrybut | wartości | Domyślne | Znaczenie |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | tekst-wrapping tryb |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Tryby zawijania

| tryb | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | In wiersz z tekst | Domyślne flow: scena occupies jego własny vertical slot (嵌入型) |
| `square` | Square | scena floats lewo lub prawo; surrounding tekst wraps in rectangle around it (方形环绕) |
| `tight` | Tight | Tighter zawijanie; equivalent do `square` in Ten układ system (紧密型) |
| `through` | Through | Through-zawijanie; equivalent do `square` in Ten układ system (穿越型) |
| `top-bottom` | góra i dół | tekst tylko nad i below, nie beside; respects `align` dla horizontal placement (上下型) |
| `behind` | Behind tekst | blok renders behind surrounding tekst; respects `align` (衬于文字下方) |
| `front` | In przód of tekst | blok renders in przód of surrounding tekst; respects `align` (浮于文字上方) |

### Przykłady

lewo-floating scena — tekst in  następny paragraph wraps do  prawo:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Tekst płynący po prawej stronie sceny…
````

prawo-floating scena:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Tekst płynący po lewej stronie sceny…
````

Centred scena (no tekst wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline in tekst (flow context) — tekst wraps around small scena:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

## Przykład

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Import Przykłady

Te Przykłady focus on  scena-strona zachowanie który most często trips people up gdy importing
struktury.

StructureLib import z jawny facing, obrót, flip, i offsets:

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

GregTech controllers pozostają unformed by Domyślne, even gdy  zaimportowany multiblock is otherwise
prawidłowy:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

Ustaw `formed={true}` tylko gdy  podgląd powinien intentionally pokazywać  formed controller stan:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

 ten sam Domyślne także stosuje do controllers umieszczony directly z `<Block>`, including GregTech
controllers który rely on surrounding multiblock casings:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

prosty blok-tylko layouts może nadal be authored directly i remain compatible z multiblock
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

## Elementy podrzędne sceny

GuideNH obecnie registers Te scena element podrzędny tagi:

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
- adnotacja tagi takie jak `<BoxAnnotation>` i `<LineAnnotation>`

## Dźwięki sceny

`<PlaySound>` może be umieszczony wewnątrz `<GameScene>` do play sounds z scena interaction lub timeline
wpis. Obsługiwane triggers są:

- `click`,  Domyślne
- `hover`, fired once gdy  cursor enters  scena
- `enter`, fired once gdy  scena pierwszy renders

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

gdy `x`, `y`, i `z` są provided,  sound volume is attenuated in ekran przestrzeń z 
projected scena współrzędna do  kliknięcie punkt lub scena środek. `radius` domyślne do 75% of 
shorter scena strona, i `minVolume` domyślne do `0.15`.

## `<BlockStats>` i `<BlockStat>`

Declares lub customizes Nakładka statystyk bloków. sceny z bloki enable  automatyczny toggle
button even gdy Ten element podrzędny is pominięty. Adding jeden lub more `<BlockStat>` elementy podrzędne switches 
overlay do manual statistics tryb dla który scena.

`<BlockStats>` atrybuty:

| Atrybut | Wymagane | Domyślne | Znaczenie |
| --- | --- | --- | --- |
| `visible` | no | config, Domyślne `false` | początkowy overlay widoczność |
| `buttonEnabled` | no | config, Domyślne `true` | shows  blok statistics toggle button |
| `mode` | no | `auto` | `auto` lub `manual`; element podrzędny `<BlockStat>` wpisy force manual tryb |
| `corner` | no | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, lub `bottomLeft` |
| `dock` | no | `inside` | automatyczny lists może attach do `inside`, `left`, `top`, `right`, lub `bottom`; manual tryb zawsze używa  wewnątrz overlay |
| `showNames` | no | `false` | whether do pokazywać element names beside icons; gdy włączony  count is także appended po  nazwa |
| `filterMode` | no | `blacklist` | `blacklist` lub `whitelist` |
| `filter` | no | empty | element klucze takie jak `minecraft:stone` lub `minecraft:stone:0`, separated by spaces, commas, lub semicolons |
| `maxWidth` | no |  larger of `224` px i 40% of  scena szerokość | maximum overlay szerokość in piksele przed horizontal przewijanie |
| `maxHeight` | no |  larger of `96` px i 40% of  scena wysokość | maximum overlay wysokość in piksele przed vertical przewijanie |

`<BlockStat>` atrybuty:

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `item` | yes, unless `id` is używany | element id wyświetlany in  lista |
| `id` | yes, unless `item` is używany | existing element-stos Atrybut form |
| `count` | no | displayed count; omitting it shows  wiersz once, i `count="0"` hides  wiersz |

Przykład:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

Places blok do  podgląd świat.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `id` | yes, unless `ore` is używany | blok id |
| `ore` | no | ore dictionary nazwa;  pierwszy pasujący stos musi rozwiązać do blok element |
| `x` | no | liczba całkowita świat X, Domyślne `0` |
| `y` | no | liczba całkowita świat Y, Domyślne `0` |
| `z` | no | liczba całkowita świat Z, Domyślne `0` |
| `meta` | no | liczba całkowita blok metadane |
| `facing` | no | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | no | SNBT TileEntity compound |
| `formed` | no | whether  umieszczony struktura controller powinien be treated as formed during podgląd sync; Domyślne `false` |

Uwagi:

- `ore` takes precedence over `id`; Jeśli GregTech is installed,  wybrany stos is unified through `GTOreDictUnificator.setStack(...)`
- Jeśli `meta` is pominięty i an `ore` pasować carries concrete non-wildcard element damage, który damage is używany przed  `facing` awaryjny
- Jeśli `meta` is pominięty, some bloki derive sensible Domyślne z `facing`
- Jeśli `nbt` creates TileEntity successfully,  podgląd używa it
- Ustaw `formed={false}` gdy controller-based struktura powinien pozostają unformed in podgląd even though  surrounding struktura is otherwise prawidłowy

Przykład:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

ładuje zewnętrzny struktura plik do  scena.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `src` | yes | struktura zasób ścieżka |
| `x` | no | liczba całkowita tłumaczenie X (alias dla `offsetX`) |
| `y` | no | liczba całkowita tłumaczenie Y (alias dla `offsetY`) |
| `z` | no | liczba całkowita tłumaczenie Z (alias dla `offsetZ`) |
| `offsetX` | no | liczba całkowita tłumaczenie X (preferowany over `x`) |
| `offsetY` | no | liczba całkowita tłumaczenie Y, clamped do `[0, worldHeight-1]` (preferowany over `y`) |
| `offsetZ` | no | liczba całkowita tłumaczenie Z (preferowany over `z`) |
| `formed` | no | whether zaimportowany struktura controllers powinien be treated as formed during podgląd sync; Domyślne `false` |

Obsługiwane formats:

- SNBT tekst
- gzipped binary NBT
- uncompressed binary NBT

Wymagane struktura klucze:

- `palette`
- `blocks`

Przykład:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

importy StructureLib multiblock podgląd by controller id.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `controller` | yes | controller blok id, używając `modid:block[:meta]` |
| `name` | no | opcjonalny binding nazwa używany by `showWhenStructure` on Adnotacje, templates, i sounds |
| `piece` | no | StructureLib piece nazwa nadpisywać |
| `facing` | no | facing nadpisywać passed do  importer |
| `rotation` | no | obrót nadpisywać passed do  importer |
| `flip` | no | flip/mirror nadpisywać passed do  importer |
| `channel` | no | liczba całkowita channel nadpisywać dla channel-aware struktury |
| `offsetX` | no | liczba całkowita X offset applied do wszystkie umieszczony bloki (Domyślne `0`) |
| `offsetY` | no | liczba całkowita Y offset applied do wszystkie umieszczony bloki, clamped do `[0, worldHeight-1]` (Domyślne `0`) |
| `offsetZ` | no | liczba całkowita Z offset applied do wszystkie umieszczony bloki (Domyślne `0`) |
| `formed` | no | whether zaimportowany StructureLib controllers powinien be treated as formed during podgląd sync; Domyślne `false` |

Uwagi:

-  zaimportowany struktura starts z scena `0 0 0`;  controller is nie forced do be umieszczony at `0 0 0`
- Ten tag enables StructureLib-określony podpowiedź, hatch wyróżniać, i channel slider UI gdy metadane is dostępny
- controller pasujący obsługuje  GTNH-style `modid:block:meta` form
- używać `name` gdy  scena zawiera wiele StructureLib importy i another tag needs do cel jeden określony struktura stan
- `facing`, `rotation`, i `flip` używać  ten sam orientacja vocabulary as StructureLib export; gdy requested combination is nie allowed by  controller, GuideNH falls tył do  pierwszy prawidłowy alignment automatycznie
- GregTech controller previews now Domyślne do  controller's opposite horizontal facing z  older podgląd orientacja, rotating  podgląd przód by 180 degrees around  Y oś
- Ustaw `formed={false}` gdy  zaimportowany controller powinien remain visibly unformed; Ten is  Obsługiwane alternative do shipping intentionally broken NBT

Przykład:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

struktura-aware adnotacja i sound Przykład:

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

StructureLib domyślne może także be supplied as element podrzędny tagi. Te domyślne są part of  scena's
początkowy interaktywny stan, so  reset-widok button restores ich po  user zmiany tier lub
channel sliders.

| element podrzędny tag | Znaczenie |
| --- | --- |
| `<Tier value="1" />` | Master tier wartość. |
| `<Channel name="channelName" value="1" />` | Named StructureLib channel nadpisywać. Repeat dla wiele kanały. |
| `<Facing value="north" />` | Domyślne facing. |
| `<Rotation value="normal" />` | Domyślne obrót. |
| `<Flip value="none" />` | Domyślne flip/mirror. |
| `<Orientation value="north:normal:none" />` | Facing, obrót, i flip in jeden tag. |
| `<GregTechActiveController />` | GregTech tylko: renderować  controller z jego active texture gdy possible. |
| `<GregTechPlaceHatches />` | GregTech tylko: place normal GT hatches dla hatch-tylko podgląd positions. bez Ten, GT previews nadal używać survival construct dla hatch-aware machines, but empty hatch positions fall tył do casing bloki. |

dla GregTech controllers, GuideNH now używa  ten sam StructureLib survival-podgląd ścieżka as 
export command. Ten fixes hatch-tylko positions który normal `construct()` nie może populate podczas
keeping awaryjny casings by Domyślne.

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

stosuje jawny isometric kamera yaw/pitch/roll.

Jeśli Ten tag is pominięty,  scena zachowuje używając  `<GameScene>` `perspective` preset.  Domyślne
`isometric-north-east` preset is equivalent do:

````md
<IsometricCamera yaw="225" pitch="30" />
````

| Atrybut | Znaczenie |
| --- | --- |
| `yaw` | liczba zmiennoprzecinkowa |
| `pitch` | liczba zmiennoprzecinkowa |
| `roll` | liczba zmiennoprzecinkowa |

Przykład:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes każdy już-umieszczony blok pasujący cel blok id.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `id` | yes | blok id do usuwać, używając `modid:block[:meta]` |

Ten is useful po importing struktura gdy you want do ukrywać określony bloki dla clarity.

Przykład:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

Replaces już-umieszczony bloki który pasować source blok id (i optionally partial blok byt NBT
pattern) z nowy blok.  wyszukiwanie może be globalny (wszystkie filled bloki) lub restricted do a
bounding box.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `from` | yes | source blok do pasować, używając `modid:block[:meta]` |
| `from_nbt` | no | partial SNBT compound; blok pasuje tylko gdy jego blok byt NBT zawiera wszystkie listed klucze |
| `to` | yes | zamiana blok, używając `modid:block[:meta]` |
| `to_nbt` | no | SNBT TileEntity compound do apply do  zamiana |
| `x` | no | bounding box początek X; Jeśli any of `x/y/z/dx/dy/dz` is present,  box tryb is activated |
| `y` | no | bounding box początek Y |
| `z` | no | bounding box początek Z |
| `dx` | no | bounding box length on  X oś (Domyślne `1`) |
| `dy` | no | bounding box wysokość on  Y oś (Domyślne `1`) |
| `dz` | no | bounding box szerokość/depth on  Z oś (Domyślne `1`) |
| `formed` | no | whether zamiana wynik controllers powinien be treated as formed during podgląd sync; Domyślne `false` |

Uwagi:

- gdy none of `x/y/z/dx/dy/dz` są provided, wszystkie filled bloki są scanned globally
- `from_nbt` is a **partial** pasować: tylko  klucze listed in  pattern musi pasować; extra klucze in
   actual blok byt są ignored
-  zamiana is performed via  ten sam blok placement pipeline as `<Block>`, so GregTech MetaTile
  i BartWorks blok entities są handled correctly
- Jeśli  zamiana places controller, `formed={false}` zachowuje który controller unformed during podgląd

Przykład:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills wyrównany do osi box z pojedynczy blok Typ, overwriting whatever was there przed.
Unlike `<Block>` (który targets pojedynczy pozycja), `<PlaceBlock>` obsługuje multi-blok regions via
`dx`/`dy`/`dz`, ordered as length, wysokość, i szerokość/depth on  X/Y/Z osie.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `id` | yes | blok id, używając `modid:block[:meta]` |
| `nbt` | no | SNBT TileEntity compound applied do każdy umieszczony blok |
| `x` | no | region początek X, Domyślne `0` |
| `y` | no | region początek Y, Domyślne `0` |
| `z` | no | region początek Z, Domyślne `0` |
| `dx` | no | region length on  X oś, Domyślne `1` |
| `dy` | no | region wysokość on  Y oś, Domyślne `1` |
| `dz` | no | region szerokość/depth on  Z oś, Domyślne `1` |
| `formed` | no | whether umieszczony controllers powinien be treated as formed during podgląd sync; Domyślne `false` |

Uwagi:

- wszystkie bloki in  box są unconditionally umieszczony (no prior-blok check)
-  NBT compound is copied Dla każdego individual placement
-  ten sam blok placement pipeline as `<Block>` is używany, so GregTech MetaTile i BartWorks blok entities
  są fully Obsługiwane
- Jeśli  region places jeden lub more controllers, `formed={false}` zachowuje każdy affected controller unformed

Przykład:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

Expands jeden lub more element podrzędny Adnotacje onto każdy pasujący blok który już exists in  bieżący scena.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `id` | yes | blok matcher in `modid:block[:meta]` form |

Zasady:

- place it po  bloki lub zaimportowany struktury który it powinien pasować
- pasujący happens against  bieżący scena stan at analizować czas
- element podrzędny Adnotacje używać lokalny współrzędne względny do każdy pasujący blok

Przykład:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

dodaje byt do  podgląd scena.

 atrybuty follow summon-style byt placement i SNBT dane.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `id` | yes | byt Typ id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, i zarejestrowany mod byt ids in either `modid.entityName` lub `modid:entityName` form są accepted |
| `x` | no | liczba zmiennoprzecinkowa X współrzędna  byt is centered on, Domyślne `0.5` |
| `y` | no | liczba zmiennoprzecinkowa Y współrzędna at  dół of  byt, Domyślne `0` |
| `z` | no | liczba zmiennoprzecinkowa Z współrzędna  byt is centered on, Domyślne `0.5` |
| `rotationY` | no | yaw in degrees, Domyślne `-45` |
| `rotationX` | no | pitch in degrees, Domyślne `0` |
| `data` | no | summon-style SNBT merged do  byt NBT przed spawn |
| `sceneEntityId` | no | stable scena-lokalny byt id używany by later `<Entity>` / `<RemoveEntity>` operations i by zaimportowany scena snapshots |
| `mount` | no | stable `sceneEntityId` of  vehicle który Ten byt powinien ride po spawn |
| `unmount` | no | wartość logiczna wyrażenie który clears Ten byt's bieżący stable mount relation po spawn lub stan replay |
| `baby` | no | wartość logiczna wyrażenie forcing Obsługiwane entities do baby form; pominięty leaves  byt's normal age/stan unchanged |
| `name` | no | podgląd gracz nazwa gdy `id` is `player`, `fakeplayer`, `minecraft:player`, lub `minecraft:fakeplayer` |
| `uuid` | no | podgląd gracz UUID gdy używając jeden of  gracz ids nad |
| `showName` | no | wartość logiczna wyrażenie controlling  podgląd gracz nameplate, Domyślne `true` dla gracz podgląd ids |
| `showCape` | no | wartość logiczna wyrażenie controlling  podgląd gracz cape, Domyślne `true` dla gracz podgląd ids |
| `headRotation` | no | podgląd gracz head obrót as `x y z` degrees |
| `leftArmRotation` | no | podgląd gracz lewo arm obrót as `x y z` degrees |
| `rightArmRotation` | no | podgląd gracz prawo arm obrót as `x y z` degrees |
| `leftLegRotation` | no | podgląd gracz lewo leg obrót as `x y z` degrees |
| `rightLegRotation` | no | podgląd gracz prawo leg obrót as `x y z` degrees |
| `capeRotation` | no | podgląd gracz cape obrót as `x y z` degrees; domyślne do  standing-nadal angle `6 0 0` |

Uwagi:

- byt granice participate in scena auto-centering i widoczny-layer filtering
- byt creation falls tył gracefully gdy  podgląd świat is nie ready yet, następnie binds on pierwszy renderować
- `sceneEntityId` is opcjonalny, but strongly recommended whenever later scena mutations need do find, usuwać, remount, lub restore  ten sam logical byt bez scanning by raw czas wykonania id
- jeden `sceneEntityId` może własny more niż jeden czas wykonania byt instance; `<RemoveEntity sceneEntityId="..."/>` removes każdy byt obecnie zarejestrowany do który stable id
- `mount` links entities by stable scena id rather niż by raw NBT passenger lists, so replay, import/export, podgląd rebuild, i Animacja Ponder seeking może wszystkie restore  ten sam rider/vehicle relation deterministically
- `unmount={true}` clears  stable mount relation dla który byt przed any later mount is applied
- `baby={true}` obecnie obsługuje podgląd players, ageable mobs, vanilla zombies, i modded entities który expose stable `setChild(boolean)` lub `setBaby(boolean)` style APIs
- element podrzędny-stan entities są re-aligned do ich bieżący pozycja po resizing so najechanie i pick granice pozostają centered on  wyrenderowany model
- gracz podgląd ids utworzyć client-strona fake remote gracz so  normal gracz renderer i skin pipeline może be używany
- gdy both `name` i `uuid` są pominięty dla gracz podgląd, GuideNH falls tył do `Steve` i  vanilla Domyślne skin
- gdy tylko `name` is given dla gracz podgląd, GuideNH pierwszy tries do rozwiązać  real online profile so skins i capes może ładować; Jeśli lookup fails, it falls tył do stable offline UUID
- gdy tylko `uuid` is given dla gracz podgląd, GuideNH generates placeholder wyświetlać nazwa i nadal tries do rozwiązać  skin z  profile
- `showName={false}` hides  podgląd gracz's overhead nazwa bez bypassing  normal gracz renderer
- `showCape={false}` hides  podgląd gracz's cape podczas nadal respecting  normal gracz renderować ścieżka i Forge hooks
- gracz pose atrybuty używać three przestrzeń-separated floats mapped do model `X Y Z` obrót in degrees
- pominięty head i limb obrót atrybuty Zachowaj  normal vanilla idle pose; pominięty `capeRotation` falls tył do  standing-nadal cape angle `6 0 0`
- gracz previews require active client świat at analizować czas because Minecraft's gracz byt constructor nie może be utworzony worldless
- najechanie byt shows jego zlokalizowany wyświetlać nazwa, lub jego niestandardowy nazwa Jeśli jeden was provided

Przykład:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Stable-id mount Przykład:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

Removal i unmount Przykład:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
</GameScene>
````

Baby byt Przykład:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

podgląd gracz pose Przykład:

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

podgląd gracz nazwa i cape Przykład:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

Removes każdy czas wykonania byt obecnie zarejestrowany do jeden stable `sceneEntityId`.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `sceneEntityId` | yes | stable scena-lokalny byt id do usuwać |
| `unmount` | no | wartość logiczna wyrażenie który clears  stable mount relation przed removal |

Uwagi:

- Ten is  scena-strona counterpart do Animacja Ponder's `removeEntities`
- removal works on  indexed stable-id registry, so it nie need do scan wszystkie entities każdy frame
- Jeśli wiele zaimportowany lub replayed entities share  ten sam `sceneEntityId`, jeden są usunięty together

Przykład:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## Pogoda

`<Weather>` dodaje animated rain lub snow directly do a `GameScene`. Unlike Animacja Ponder Pogoda presets,
scena Pogoda is nie timeline-owned: it zachowuje looping during normal scena renderowanie, it nie
fade in lub fade out, i it nie może be paused lub scrubbed independently.  renderer nadal używa 
ten sam precipitation geometrią ścieżka as Animacja Ponder Pogoda, so lokalny podgląd i site export pozostają aligned.

| Atrybut | Domyślne | opis |
| --- | --- | --- |
| `weather` / `type` | `rain` | Pogoda rodzaj. Obsługiwane wartości: `rain`, `snow`. |
| `x`, `z` | scena granice | Covered precipitation kolumny. scalar targets jeden kolumna. Arrays używać endpoint pairs do define jeden lub more rectangles. |
| `density` | Typ-określony | Coverage density. Higher wartości Zachowaj more precipitation kolumny active; lower wartości sparsify  effect. |

Uwagi:

- `<Weather>` ignores `y`;  vertical span is derived z  bieżący scena granice i z 
  highest precipitation-blocking blok in każdy covered kolumna.
- Jeśli jeden oś ma unmatched extra array wartości,  unmatched tail is ignored.
- wewnątrz jeden Pogoda declaration, rain i snow nigdy stos on  ten sam `x/z` kolumna. Jeśli wiele
  Pogoda tagi overlap, earlier tagi Zachowaj  shared kolumny.
- inny non-overlapping kolumny in  ten sam `GameScene` może renderować rain i snow at  ten sam
  czas.

Przykład:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## kamera środek zachowanie

Jeśli no jawny `centerX/Y/Z` is given, GuideNH auto-centers  scena z  umieszczony blok granice. Jeśli any jawny środek współrzędna is Ustaw, auto-centering is wyłączony i missing współrzędne Domyślne do `0`.

## Uwagi dotyczące interakcji

gdy `interactive={true}`  scena obsługuje obrót, pan, powiększenie, reset, adnotacja toggles, i other UI controls exposed by  przewodnik ekran.

- sceny spanning wiele Y levels pokazywać widoczny-layer slider nad  dół krawędź
- StructureLib sceny może Dodaj hatch-wyróżniać toggle button plus channel slider at  very dół gdy  zaimportowany metadane zapewnia ich
- adnotacja najechanie takes priority over blok najechanie; blok tooltips appear normally once no adnotacja hotspot is being hovered
- StructureLib najechanie zachowuje  blok nazwa on  pierwszy podpowiedź wiersz, dodaje struktura-określony tekst starting on  second wiersz, i expands zamiana candidates gdy `Shift` is held

## Powiązane strony

- [Annotations](Annotations)
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [Examples](Examples)
