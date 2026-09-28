# GameScene


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

`<GameScene>` es GuideNH's 3D vista previa etiqueta. `<Scene>` es un alias con El mismo comportamiento.

## escena atributos

| atributo | tipo | predeterminado | significado |
| --- | --- | --- | --- |
| `width` | entero | `256` | viewport ancho en píxeles |
| `height` | entero | `192` | viewport alto en píxeles |
| `zoom` | decimal | `1.0` | cámara zoom multiplier |
| `perspective` | cadena | `isometric-north-east` | cámara preset |
| `rotateX` | decimal | auto | explícito X rotación anular |
| `rotateY` | decimal | auto | explícito Y rotación anular |
| `rotateZ` | decimal | auto | explícito Z rotación anular |
| `offsetX` | decimal | auto | pantalla-espacio horizontal pan |
| `offsetY` | decimal | auto | pantalla-espacio vertical pan |
| `centerX` | decimal | auto | explícito mundo rotación centro X |
| `centerY` | decimal | auto | explícito mundo rotación centro Y |
| `centerZ` | decimal | auto | explícito mundo rotación centro Z |
| `interactive` | booleano expresión | `true` | enables mouse interaction |
| `showBackground` | booleano expresión | `true` | muestra El escena fondo fill y borde |
| `allowLayerSlider` | booleano | `true` | muestra El vertical layer slider |
| `gridButtonEnabled` | booleano | `true` | muestra El floor grid toggle button |
| `showGrid` | booleano | `false` | inicial visibilidad of El floor grid |

## bloque Statistics Overlay

escenas que contener bloques enable El bloque-stat toggle button by predeterminado. Añada a `<BlockStats>`
hijo cuando you want Un anular su modo, placement, filters, visibilidad, o tamaño. El lista es
cached y solo rebuilt cuando El escena bloques, Ponder timeline estado, StructureLib selección, o
bloque-stat settings cambiar; normal renderizado reuses El prepared filas. Long lists son clipped a
`maxWidth` y `maxHeight`; Si those son omitido, cada es El larger of El fijo `224` by `96` píxeles y
40% of El escena tamaño.
Overflow receives draggable scrollbars, y El mouse wheel scrolls El lista mientras El cursor es
sobre El overlay. Hold Shift Un wheel-desplazar horizontally.

en automático modo, GuideNH scans El escena's filled bloques y resolves cada bloque Un El elemento
pila users normally Consulta. bloques que contener varios visible components puede contribute varios
elementos de El mismo coordenada; esto incluye AE2 cable bus parts y facades, ForgeMultipart part
drops, y Carpenters' bloques covers o overlays cuando those mods son installed. Counts son grouped
by `item:meta` y sorted by count.

automático lists puede también ser docked fuera de El escena con `dock="left"`, `dock="top"`,
`dock="right"`, o `dock="bottom"`. Docked lists ajuste en extra columnas o filas based on El
attached lado length, reserve diseño espacio, y avoid El escena button columna on El derecha. clic an
elemento en Un automático lista Un resaltar todos coincidente escena placements con sus resuelto collision
boxes usando Un siempre-on-arriba face overlay; clic El mismo elemento again Un clear El resaltar. Counts
son renderizado a través de El ItemStack pila-tamaño overlay. Establezca `showNames={true}` Un append El count
después de cada nombre as well, y pasar el cursor Un elemento Un Consulta El exact bloque count en El tooltip.

Filters puede ocultar común bloques o mostrar solo seleccionado bloques:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

usar manual modo cuando Un guíUn wants Un mostrar Un planned material lista en su lugar of El literal escena
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

## Debug modo Overlays

cuando El `enableDebugMode` opción es activado en El GuideNH mod config, El following extra
overlays become disponible en El 3D escena vista previa.

### Grid coordenada Labels

cuando debug modo es **on** y El floor grid es **visible**, coordenada labels son renderizado
debajo cada grid línea:

- **X-eje numbers** son mostrado a lo largo de El cerca borde of El grid (north/−Z borde en El predeterminado
  `isometric-north-east` cámara).  cada entero X mundo-coordenada receives Un etiqueta.
- **Z-eje numbers** son mostrado a lo largo de El cerca borde of El grid (east/+X borde).  cada entero
  Z mundo-coordenada receives Un etiqueta.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) son drawn at El midpoint of cada
  respective grid borde.

coordenadas follow El actual mundo X/Z valores stored en El escena level, so Estas puede ser
negative cuando El estructura contiene bloques con negative coordenadas.

El grid toggle button es **siempre activado** mientras debug modo es active, regardless of El
`gridButtonEnabled` atributo, so you puede mostrar o ocultar El grid y su labels at cualquier tiempo.
El predeterminado grid visibilidad (`showGrid`) es no affected.

### bloque coordenada Tooltip

cuando debug modo es **on** y El cursor hovers sobre Un bloque dentro de El escena, Un second
tooltip es renderizado encima de El primary bloque tooltip, showing El mundo-espacio bloque posición
as `X, Y, Z` en gold texto.

Si El coordenada tooltip would ser clipped at El arriba of El pantalla it automáticamente snaps
debajo El cursor area en su lugar (magnetic snapping).

## perspectiva Presets

Accepted `perspective` valores:

- `isometric-north-east`
- `isometric-north-west`
- `up`

desconocido valores fall atrás a `isometric-north-east`.

## contenido Embedding y texto Wrapping

cualquier bloque-level etiqueta — including `<GameScene>` — admite dos opcional atributos que control
how it es embedded en El página, mirroring Microsoft Word's "texto Wrapping" opciones.

| atributo | valores | predeterminado | significado |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | texto-wrapping modo |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### ajuste modes

| modo | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | en línea con texto | predeterminado flow: escena occupies su propio vertical slot (嵌入型) |
| `square` | Square | escena floats izquierda o derecha; surrounding texto wraps en Un rectangle around it (方形环绕) |
| `tight` | Tight | Tighter ajuste; equivalent a `square` en esto diseño system (紧密型) |
| `through` | a través de | a través de-ajuste; equivalent a `square` en esto diseño system (穿越型) |
| `top-bottom` | arriba y abajo | texto solo encima de y debajo, no beside; respects `align` para horizontal placement (上下型) |
| `behind` | Behind texto | bloque renderiza behind surrounding texto; respects `align` (衬于文字下方) |
| `front` | en frente of texto | bloque renderiza en frente of surrounding texto; respects `align` (浮于文字上方) |

### Ejemplos

izquierda-floating escena — texto en El siguiente paragraph wraps Un El derecha:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Texto que fluye a la derecha de la escena…
````

derecha-floating escena:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Texto que fluye a la izquierda de la escena…
````

Centred escena (no texto wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline en texto (flow context) — texto wraps around Un small escena:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

## Ejemplo

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Import Ejemplos

These Ejemplos focus on El escena-lado comportamiento que most a menudo trips people up cuando importing
estructuras.

StructureLib import con explícito facing, rotación, flip, y offsets:

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

GregTech controllers permanecer unformed by predeterminado, even cuando El importado multiblock es otherwise
válido:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

Establezca `formed={true}` solo cuando El vista previa deberíUn intentionally mostrar El formed controller estado:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

El mismo predeterminado también aplica Un controllers colocado directly con `<Block>`, including GregTech
controllers que rely on surrounding multiblock casings:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

simple bloque-solo layouts puede todavía ser authored directly y remain compatible con multiblock
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

## escena hijo Elements

GuideNH actualmente registers these escena hijo etiquetas:

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
- anotación etiquetas como `<BoxAnnotation>` y `<LineAnnotation>`

## escena Sounds

`<PlaySound>` puede ser colocado dentro de `<GameScene>` Un play sounds de escena interaction o timeline
entrada. Compatible triggers son:

- `click`, El predeterminado
- `hover`, fired once cuando El cursor enters El escena
- `enter`, fired once cuando El escena primero renderiza

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

cuando `x`, `y`, y `z` son proporcionado, El sound volume es attenuated en pantalla espacio de El
projected escena coordenada Un El clic punto o escena centro. `radius` predeterminados Un 75% of El
shorter escena lado, y `minVolume` predeterminados a `0.15`.

## `<BlockStats>` y `<BlockStat>`

Declares o customizes Un bloque statistics overlay. escenas con bloques enable El automático toggle
button even cuando esto hijo es omitido. Adding uno o más `<BlockStat>` hijos switches El
overlay Un manual statistics modo para que escena.

`<BlockStats>` atributos:

| atributo | obligatorio | predeterminado | significado |
| --- | --- | --- | --- |
| `visible` | no | config, predeterminado `false` | inicial overlay visibilidad |
| `buttonEnabled` | no | config, predeterminado `true` | muestra El bloque statistics toggle button |
| `mode` | no | `auto` | `auto` o `manual`; hijo `<BlockStat>` entradas force manual modo |
| `corner` | no | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, o `bottomLeft` |
| `dock` | no | `inside` | automático lists puede attach a `inside`, `left`, `top`, `right`, o `bottom`; manual modo siempre usa El dentro de overlay |
| `showNames` | no | `false` | whether Un mostrar elemento names beside icons; cuando activado El count es también appended después de El nombre |
| `filterMode` | no | `blacklist` | `blacklist` o `whitelist` |
| `filter` | no | vacío | elemento claves como `minecraft:stone` o `minecraft:stone:0`, separated by spaces, commas, o semicolons |
| `maxWidth` | no | El larger of `224` px y 40% of El escena ancho | maximum overlay ancho en píxeles antes de horizontal al desplazar |
| `maxHeight` | no | El larger of `96` px y 40% of El escena alto | maximum overlay alto en píxeles antes de vertical al desplazar |

`<BlockStat>` atributos:

| atributo | obligatorio | significado |
| --- | --- | --- |
| `item` | sí, salvo `id` es usado | elemento id mostrado en El lista |
| `id` | sí, salvo `item` es usado | existing elemento-pila atributo form |
| `count` | no | displayed count; omitting it muestra El fila once, y `count="0"` oculta El fila |

Ejemplo:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

Places Un bloque en El vista previa mundo.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `id` | sí, salvo `ore` es usado | bloque id |
| `ore` | no | ore dictionary nombre; El primero coincidente pila debe resolver Un Un bloque elemento |
| `x` | no | entero mundo X, predeterminado `0` |
| `y` | no | entero mundo Y, predeterminado `0` |
| `z` | no | entero mundo Z, predeterminado `0` |
| `meta` | no | entero bloque metadatos |
| `facing` | no | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | no | SNBT TileEntity compound |
| `formed` | no | whether El colocado estructura controller deberíUn ser treated as formed durante vista previa sync; predeterminado `false` |

Notas:

- `ore` takes precedence sobre `id`; Si GregTech es installed, El elegido pila es unified a través de `GTOreDictUnificator.setStack(...)`
- Si `meta` es omitido y an `ore` coincidir carries concrete non-wildcard elemento damage, que damage es usado antes de El `facing` reserva
- Si `meta` es omitido, some bloques derive Un sensible predeterminado de `facing`
- Si `nbt` crea Un TileEntity successfully, El vista previa usa it
- Establezca `formed={false}` cuando Un controller-based estructura deberíUn permanecer unformed en vista previa even though El surrounding estructura es otherwise válido

Ejemplo:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

carga Un externo estructura archivo en El escena.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `src` | sí | estructura recurso ruta |
| `x` | no | entero traducción X (alias para `offsetX`) |
| `y` | no | entero traducción Y (alias para `offsetY`) |
| `z` | no | entero traducción Z (alias para `offsetZ`) |
| `offsetX` | no | entero traducción X (preferido sobre `x`) |
| `offsetY` | no | entero traducción Y, clamped a `[0, worldHeight-1]` (preferido sobre `y`) |
| `offsetZ` | no | entero traducción Z (preferido sobre `z`) |
| `formed` | no | whether importado estructura controllers deberíUn ser treated as formed durante vista previa sync; predeterminado `false` |

Compatible formats:

- SNBT texto
- gzipped binary NBT
- uncompressed binary NBT

obligatorio estructura claves:

- `palette`
- `blocks`

Ejemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

importaciones Un StructureLib multiblock vista previa by controller id.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `controller` | sí | controller bloque id, usando `modid:block[:meta]` |
| `name` | no | opcional binding nombre usado by `showWhenStructure` on anotaciones, templates, y sounds |
| `piece` | no | StructureLib piece nombre anular |
| `facing` | no | facing anular passed Un El importer |
| `rotation` | no | rotación anular passed Un El importer |
| `flip` | no | flip/mirror anular passed Un El importer |
| `channel` | no | entero channel anular para channel-aware estructuras |
| `offsetX` | no | entero X offset applied Un todos colocado bloques (predeterminado `0`) |
| `offsetY` | no | entero Y offset applied Un todos colocado bloques, clamped a `[0, worldHeight-1]` (predeterminado `0`) |
| `offsetZ` | no | entero Z offset applied Un todos colocado bloques (predeterminado `0`) |
| `formed` | no | whether importado StructureLib controllers deberíUn ser treated as formed durante vista previa sync; predeterminado `false` |

Notas:

- El importado estructura starts de escena `0 0 0`; El controller es no forced Un ser colocado at `0 0 0`
- esto etiqueta enables StructureLib-específico tooltip, hatch resaltar, y channel slider UI cuando metadatos es disponible
- controller coincidente admite El GTNH-style `modid:block:meta` form
- usar `name` cuando El escena contiene varios StructureLib importaciones y another etiqueta needs Un destino uno específico estructura estado
- `facing`, `rotation`, y `flip` usar El mismo orientación vocabulary as StructureLib export; cuando Un requested combination es no allowed by El controller, GuideNH falls atrás Un El primero válido alignment automáticamente
- GregTech controller previews now predeterminado Un El controller's opposite horizontal facing de El older vista previa orientación, rotating El vista previa frente by 180 degrees around El Y eje
- Establezca `formed={false}` cuando El importado controller deberíUn remain visibly unformed; esto es El Compatible alternative Un shipping intentionally broken NBT

Ejemplo:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

estructura-aware anotación y sound Ejemplo:

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

StructureLib predeterminados puede también ser supplied as hijo etiquetas. These predeterminados son part of El escena's
inicial interactivo estado, so El reset-vista button restores ellos después de El user cambios tier o
channel sliders.

| hijo etiqueta | significado |
| --- | --- |
| `<Tier value="1" />` | Master tier valor. |
| `<Channel name="channelName" value="1" />` | Named StructureLib channel anular. Repeat para varios canales. |
| `<Facing value="north" />` | predeterminado facing. |
| `<Rotation value="normal" />` | predeterminado rotación. |
| `<Flip value="none" />` | predeterminado flip/mirror. |
| `<Orientation value="north:normal:none" />` | Facing, rotación, y flip en uno etiqueta. |
| `<GregTechActiveController />` | GregTech solo: renderizan El controller con su active texture cuando possible. |
| `<GregTechPlaceHatches />` | GregTech solo: colocar normal GT hatches para hatch-solo vista previa positions. sin esto, GT previews todavía usar survival construct para hatch-aware machines, but vacío hatch positions fall atrás Un casing bloques. |

para GregTech controllers, GuideNH now usa El mismo StructureLib survival-vista previa ruta as El
export command. esto fixes hatch-solo positions que normal `construct()` no puede populate mientras
keeping reserva casings by predeterminado.

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

aplica explícito isometric cámara yaw/pitch/roll.

Si esto etiqueta es omitido, El escena mantiene usando El `<GameScene>` `perspective` preset. El predeterminado
`isometric-north-east` preset es equivalent a:

````md
<IsometricCamera yaw="225" pitch="30" />
````

| atributo | significado |
| --- | --- |
| `yaw` | decimal |
| `pitch` | decimal |
| `roll` | decimal |

Ejemplo:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes cada ya-colocado bloque coincidente Un destino bloque id.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `id` | sí | bloque id Un eliminar, usando `modid:block[:meta]` |

esto es useful después de importing Un estructura cuando you want Un ocultar específico bloques para clarity.

Ejemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

Replaces ya-colocado bloques que coincidir Un origen bloque id (y optionally Un partial bloque entidad NBT
pattern) con Un nuevo bloque. El búsqueda puede ser global (todos filled bloques) o restricted Un a
bounding box.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `from` | sí | origen bloque Un coincidir, usando `modid:block[:meta]` |
| `from_nbt` | no | partial SNBT compound; Un bloque coincide solo cuando su bloque entidad NBT contiene todos listed claves |
| `to` | sí | reemplazo bloque, usando `modid:block[:meta]` |
| `to_nbt` | no | SNBT TileEntity compound Un apply Un El reemplazo |
| `x` | no | bounding box inicio X; Si cualquier of `x/y/z/dx/dy/dz` es present, El box modo es activated |
| `y` | no | bounding box inicio Y |
| `z` | no | bounding box inicio Z |
| `dx` | no | bounding box length on El X eje (predeterminado `1`) |
| `dy` | no | bounding box alto on El Y eje (predeterminado `1`) |
| `dz` | no | bounding box ancho/depth on El Z eje (predeterminado `1`) |
| `formed` | no | whether reemplazo resultado controllers deberíUn ser treated as formed durante vista previa sync; predeterminado `false` |

Notas:

- cuando none of `x/y/z/dx/dy/dz` son proporcionado, todos filled bloques son scanned globally
- `from_nbt` es un **partial** coincidir: solo El claves listed en El pattern debe coincidir; extra claves en
  El actual bloque entidad son ignored
- El reemplazo es performed via El mismo bloque placement pipeline as `<Block>`, so GregTech MetaTile
  y BartWorks bloque entities son handled correctly
- Si El reemplazo places Un controller, `formed={false}` mantiene que controller unformed durante vista previa

Ejemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills Un eje-aligned box con a único bloque tipo, overwriting whatever was there antes de.
Unlike `<Block>` (que targets a único posición), `<PlaceBlock>` admite multi-bloque regions via
`dx`/`dy`/`dz`, ordered as length, alto, y ancho/depth on El X/Y/Z ejes.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `id` | sí | bloque id, usando `modid:block[:meta]` |
| `nbt` | no | SNBT TileEntity compound applied Un cada colocado bloque |
| `x` | no | region inicio X, predeterminado `0` |
| `y` | no | region inicio Y, predeterminado `0` |
| `z` | no | region inicio Z, predeterminado `0` |
| `dx` | no | region length on El X eje, predeterminado `1` |
| `dy` | no | region alto on El Y eje, predeterminado `1` |
| `dz` | no | region ancho/depth on El Z eje, predeterminado `1` |
| `formed` | no | whether colocado controllers deberíUn ser treated as formed durante vista previa sync; predeterminado `false` |

Notas:

- todos bloques en El box son unconditionally colocado (no prior-bloque check)
- El NBT compound es copied para cada individual placement
- El mismo bloque placement pipeline as `<Block>` es usado, so GregTech MetaTile y BartWorks bloque entities
  son fully Compatible
- Si El region places uno o más controllers, `formed={false}` mantiene cada affected controller unformed

Ejemplo:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

Expands uno o más hijo anotaciones onto cada coincidente bloque que ya exists en El actual escena.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `id` | sí | bloque matcher en `modid:block[:meta]` form |

Reglas:

- colocar it después de El bloques o importado estructuras que it deberíUn coincidir
- coincidente happens against El actual escena estado at analizar tiempo
- hijo anotaciones usar local coordenadas relativo Un cada coincidente bloque

Ejemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

añade Un entidad Un El vista previa escena.

El atributos follow summon-style entidad placement y SNBT datos.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `id` | sí | entidad tipo id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, y registrado mod entidad ids en cualquiera `modid.entityName` o `modid:entityName` form son accepted |
| `x` | no | decimal X coordenada El entidad es centered on, predeterminado `0.5` |
| `y` | no | decimal Y coordenada at El abajo of El entidad, predeterminado `0` |
| `z` | no | decimal Z coordenada El entidad es centered on, predeterminado `0.5` |
| `rotationY` | no | yaw en degrees, predeterminado `-45` |
| `rotationX` | no | pitch en degrees, predeterminado `0` |
| `data` | no | summon-style SNBT merged en El entidad NBT antes de spawn |
| `sceneEntityId` | no | stable escena-local entidad id usado by later `<Entity>` / `<RemoveEntity>` operations y by importado escena snapshots |
| `mount` | no | stable `sceneEntityId` of El vehicle que esto entidad deberíUn ride después de spawn |
| `unmount` | no | booleano expresión que clears esto entidad's actual stable mount relation después de spawn o estado replay |
| `baby` | no | booleano expresión forcing Compatible entities en baby form; omitido leaves El entidad's normal age/estado unchanged |
| `name` | no | vista previa jugador nombre cuando `id` es `player`, `fakeplayer`, `minecraft:player`, o `minecraft:fakeplayer` |
| `uuid` | no | vista previa jugador UUID cuando usando uno of El jugador ids encima de |
| `showName` | no | booleano expresión controlling El vista previa jugador nameplate, predeterminado `true` para jugador vista previa ids |
| `showCape` | no | booleano expresión controlling El vista previa jugador cape, predeterminado `true` para jugador vista previa ids |
| `headRotation` | no | vista previa jugador head rotación as `x y z` degrees |
| `leftArmRotation` | no | vista previa jugador izquierda arm rotación as `x y z` degrees |
| `rightArmRotation` | no | vista previa jugador derecha arm rotación as `x y z` degrees |
| `leftLegRotation` | no | vista previa jugador izquierda leg rotación as `x y z` degrees |
| `rightLegRotation` | no | vista previa jugador derecha leg rotación as `x y z` degrees |
| `capeRotation` | no | vista previa jugador cape rotación as `x y z` degrees; predeterminados Un El standing-todavía angle `6 0 0` |

Notas:

- entidad límites participate en escena auto-centering y visible-layer filtering
- entidad creation falls atrás gracefully cuando El vista previa mundo es no ready yet, entonces binds on primero renderizan
- `sceneEntityId` es opcional, but strongly recommended whenever later escena mutations need Un find, eliminar, remount, o restore El mismo logical entidad sin scanning by sin procesar ejecución id
- uno `sceneEntityId` puede propio más que uno ejecución entidad instance; `<RemoveEntity sceneEntityId="..."/>` removes cada entidad actualmente registrado Un que stable id
- `mount` enlaces entities by stable escena id rather que by sin procesar NBT passenger lists, so replay, import/export, vista previa rebuild, y Ponder seeking puede todos restore El mismo rider/vehicle relation deterministically
- `unmount={true}` clears El stable mount relation para que entidad antes de cualquier later mount es applied
- `baby={true}` actualmente admite vista previa players, ageable mobs, vanilla zombies, y modded entities que expose stable `setChild(boolean)` o `setBaby(boolean)` style APIs
- hijo-estado entities son re-aligned Un sus actual posición después de resizing so pasar el cursor y pick límites permanecer centered on El renderizado model
- jugador vista previa ids crear Un client-lado fake remote jugador so El normal jugador renderer y skin pipeline puede ser usado
- cuando ambos `name` y `uuid` son omitido para Un jugador vista previa, GuideNH falls atrás a `Steve` y El vanilla predeterminado skin
- cuando solo `name` es given para Un jugador vista previa, GuideNH primero tries Un resolver El real online profile so skins y capes puede cargar; Si lookup fails, it falls atrás Un Un stable offline UUID
- cuando solo `uuid` es given para Un jugador vista previa, GuideNH generates Un placeholder mostrar nombre y todavía tries Un resolver El skin de El profile
- `showName={false}` oculta El vista previa jugador's overhead nombre sin bypassing El normal jugador renderer
- `showCape={false}` oculta El vista previa jugador's cape mientras todavía respecting El normal jugador renderizan ruta y Forge hooks
- jugador pose atributos usar tres espacio-separated floats mapped Un model `X Y Z` rotación en degrees
- omitido head y limb rotación atributos Conserve El normal vanilla idle pose; omitido `capeRotation` falls atrás Un El standing-todavía cape angle `6 0 0`
- jugador previews require Un active client mundo at analizar tiempo because Minecraft's jugador entidad constructor no puede ser creado worldless
- al pasar el cursor Un entidad muestra su localizado mostrar nombre, o su personalizado nombre Si uno was proporcionado

Ejemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Stable-id mount Ejemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

Removal y unmount Ejemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
</GameScene>
````

Baby entidad Ejemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

vista previa jugador pose Ejemplo:

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

vista previa jugador nombre y cape Ejemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

Removes cada ejecución entidad actualmente registrado Un uno stable `sceneEntityId`.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `sceneEntityId` | sí | stable escena-local entidad id Un eliminar |
| `unmount` | no | booleano expresión que clears El stable mount relation antes de removal |

Notas:

- esto es El escena-lado counterpart Un Ponder's `removeEntities`
- removal works on El indexed stable-id registry, so it hace no need Un scan todos entities cada frame
- Si varios importado o replayed entities share El mismo `sceneEntityId`, Estas son eliminado together

Ejemplo:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## Weather

`<Weather>` añade animated rain o snow directly Un a `GameScene`. Unlike Ponder weather presets,
escena weather es no timeline-owned: it mantiene looping durante normal escena renderizado, it hace no
fade en o fade out, y it no puede ser paused o scrubbed independientemente. El renderer todavía usa El
mismo precipitation geometría ruta as Ponder weather, so local vista previa y site export permanecer aligned.

| atributo | predeterminado | descripción |
| --- | --- | --- |
| `weather` / `type` | `rain` | Weather tipo. Compatible valores: `rain`, `snow`. |
| `x`, `z` | escena límites | Covered precipitation columnas. Un scalar targets uno columna. Arrays usar endpoint pairs Un define uno o más rectangles. |
| `density` | tipo-específico | Coverage density. Higher valores Conserve más precipitation columnas active; lower valores sparsify El effect. |

Notas:

- `<Weather>` ignores `y`; El vertical span es derived de El actual escena límites y de El
  highest precipitation-blocking bloque en cada covered columna.
- Si uno eje tiene unmatched extra array valores, El unmatched tail es ignored.
- dentro de uno weather declaration, rain y snow nunca pila on El mismo `x/z` columna. Si varios
  weather etiquetas overlap, earlier etiquetas Conserve El shared columnas.
- diferente non-overlapping columnas en El mismo `GameScene` puede renderizan rain y snow at El mismo
  tiempo.

Ejemplo:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## cámara centro comportamiento

Si no explícito `centerX/Y/Z` es given, GuideNH auto-centers El escena de El colocado bloque límites. Si cualquier explícito centro coordenada es Establezca, auto-centering es desactivado y faltante coordenadas predeterminado a `0`.

## Interaction Notas

cuando `interactive={true}` El escena admite rotación, pan, zoom, reset, anotación toggles, y otro UI controls exposed by El guíUn pantalla.

- escenas spanning varios Y levels mostrar Un visible-layer slider encima de El abajo borde
- StructureLib escenas puede Añada Un hatch-resaltar toggle button plus Un channel slider at El very abajo cuando El importado metadatos proporciona ellos
- anotación pasar el cursor takes priority sobre bloque pasar el cursor; bloque tooltips appear normally once no anotación hotspot es being hovered
- StructureLib pasar el cursor mantiene El bloque nombre on El primero tooltip línea, añade estructura-específico texto starting on El second línea, y expands reemplazo candidates cuando `Shift` es held

## Related páginas

- [Annotations](Annotations)
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [Examples](Examples)
