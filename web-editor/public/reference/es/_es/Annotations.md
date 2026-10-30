# Anotaciones


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH escena anotaciones son hijo etiquetas dentro de `<GameScene>` / `<Scene>`. Estas renderizan en mundo espacio y puede contener hijo markdown/etiqueta contenido que se convierte en Un enriquecido tooltip.

## Reglas generales

- anotaciones solo funcionan dentro de Un escena
- hijo contenido se convierte en El tooltip cuerpo
- anotaciones puede ser ocultas con El escena UI toggle
- `alwaysOnTop` dibuja encima de escena geometría cuando Compatible by El anotación tipo
- todos escena anotaciones también aceptan opcional `showWhenStructure`, `showWhenTier`, y `showWhenChannels` condiciones cuando El escena usa `<ImportStructureLib>`

## Compatible anotación etiquetas

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<OutlineAnnotation>`
- `<TextAnnotation>`

GuideNH también admite `<BlockAnnotationTemplate>`, que aplica su hijo anotaciones Un cada ya-colocado coincidente bloque en El actual escena.

## StructureLib condiciones

cuando Un escena contiene `<ImportStructureLib>`, cada anotación etiqueta puede restrict su visibilidad Un Un específico
StructureLib estado:

| atributo | significado |
| --- | --- |
| `showWhenStructure` | bind El anotación Un Un named `<ImportStructureLib name="...">`; omit it cuando El escena solo importaciones uno StructureLib estructura |
| `showWhenTier` | tier filtro como `2`, `1..3`, `!2`, o `1..5,!3` |
| `showWhenChannels` | per-channel filtro como `input:1..3, casing:!2, fluid:4` |

Reglas:

- `showWhenTier` y `showWhenChannels` son combined con logical y
- `showWhenChannels` puede mention varios canales en uno atributo
- negated-solo clauses like `!2` mean "cualquier valor except 2"
- El mismo atributos son también Compatible by `<PlaySound>` y `<BlockAnnotationTemplate>` hijo anotaciones

Ejemplo:

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    Solo visible para el estado de StructureLib seleccionado.
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

Resalta un volumen de bloque 1x1x1 único.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `pos` | sí | `x y z` vector |
| `color` | no | `#RRGGBB`, `#AARRGGBB`, o `transparent` |
| `thickness` | no | línea thickness decimal |
| `alwaysOnTop` | no | booleano expresión |

Ejemplo:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Resalta el bloque controlador.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Resalta una caja arbitraria alineada con los ejes.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `min` | sí | `x y z` minimum vector |
| `max` | sí | `x y z` maximum vector |
| `color` | no | anotación color |
| `thickness` | no | línea thickness decimal |
| `alwaysOnTop` | no | booleano expresión |

GuideNH automáticamente swaps min/max coordenadas per eje cuando Estas son proporcionado en reverse orden.

Ejemplo:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Resaltado de media altura.
</BoxAnnotation>
````

## `<LineAnnotation>`

dibuja Un línea segment o polyline en mundo espacio.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `from` | sí, salvo `points` es Establezca | `x y z` inicio vector |
| `to` | sí, salvo `points` es Establezca | `x y z` fin vector |
| `points` | no | Semicolon-separated `x y z` puntos para Un polyline; anula `from` / `to` |
| `color` | no | anotación color |
| `thickness` | no | línea thickness decimal |
| `alwaysOnTop` | no | booleano expresión |
| `arrow` | no | `start` o `end`; omitido significa no arrow |
| `showPoints` | no | booleano expresión; muestra cada punto as Un small cube |
| `pointColor` | no | predeterminado cube color; omitido usa El línea color |
| `pointSize` | no | predeterminado cube tamaño; omitido usa Un valor slightly larger que `thickness` |

`LineAnnotation` puede contener `<LinePoint>` hijos Un anular punto marker styling. `LinePoint`
usa `index`, opcional `show`, opcional `color`, y opcional `size`. puntos son zero-indexed.
Arrows puede solo ser colocado on El inicio o fin of El línea; intermediate polyline puntos no puede carry
arrows.

Ejemplo:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Ruta de señal.
</LineAnnotation>
````

Polyline con Un 3D endpoint arrow y seleccionado punto marcadores:

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Ruta de señal a través de una curva.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places Un pantalla-facing diamond marker at Un mundo posición.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `pos` | sí | `x y z` marker posición |
| `color` | no | tint color; omitido predeterminados Un bright green |

Ejemplo:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<OutlineAnnotation>`

Dibuja un contorno de silueta brillante alrededor de bloques y entidades, como el efecto Brillo. El contorno sigue la forma renderizada de cada objetivo, sigue siendo visible a través de los bloques y entidades que tiene delante y se fusiona con los contornos que lo tocan en una sola forma.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `pos` | no | bloque `x y z` a contornear; por defecto `0 0 0`, el bloque coincidente dentro de `<BlockAnnotationTemplate>` |
| `min` / `max` | no | esquinas de bloque inclusivas de una región; se contornea cada bloque dentro de ella |
| `entity` | no | `sceneEntityId` de una `<Entity>` a contornear |
| `target` | no | `blocks` (por defecto), `entities` o `all`: qué se contornea dentro de la región; `entities` sin región contornea todas las entidades |
| `mode` | no | `auto` (por defecto), `geometry`, `cutout` o `bounds` |
| `color` | no | color del contorno; si se omite, blanco, o el color del equipo del marcador para entidades |
| `thickness` | no | ancho del contorno en píxeles de GUI, de `0.25` a `8`, por defecto `1` |
| `alwaysOnTop` | no | expresión booleana; las partes ocultas conservan el brillo completo en lugar de oscurecerse |
| `pulse` | no | expresión booleana; hace que el contorno respire |
| `pulsePeriod` | no | periodo de respiración en segundos, por defecto `2` |

Modos de silueta:

- `auto` decide por objetivo. Los modelos cerrados (cubos, vidrio, hielo, fluidos, paneles, bloques multiparte y de cables, modelos OBJ y TESR) usan su geometría, así las texturas transparentes como el vidrio contornean el bloque entero y no el marco del vidrio. Los renderizados tipo sprite (flores, brotes, cultivos, antorchas, raíles, escaleras de mano, enredaderas, redstone) y las entidades usan el alfa de la textura, así el contorno sigue la forma dibujada y no los quads. Los bloques que no renderizan nada usan su caja de selección.
- `geometry` usa siempre la geometría renderizada.
- `cutout` usa siempre el alfa de la textura.
- `bounds` contornea las cajas de selección de los bloques y las cajas de colisión de las entidades.

Reglas:

- el contorno solo se dibuja fuera de la silueta; el objetivo no se tiñe
- las partes ocultas tras otros bloques o entidades siguen visibles y se oscurecen como otras anotaciones ocultas; `alwaysOnTop` las mantiene con brillo completo
- las entidades invisibles también reciben contorno
- los contornos que se tocan o se solapan se fusionan en uno, también entre anotaciones distintas
- los colores translúcidos y el borde suave se mezclan con la escena, así el borde nunca se vuelve negro
- el contenido hijo se convierte en el tooltip; aparece al pasar sobre cualquier bloque o entidad contorneado
- dentro de `<BlockAnnotationTemplate>`, `pos` es relativo a cada bloque coincidente y los contornos de todas las coincidencias se fusionan en una forma
- los sitios exportados dibujan el mismo contorno con WebGL

Ejemplo:

````md
<GameScene zoom={3} interactive={true}>
  <Block id="minecraft:glass" />
  <Block id="minecraft:red_flower" x="2" />
  <Entity id="minecraft:sheep" sceneEntityId="sheep" x="1.5" z="2.5" />
  <OutlineAnnotation pos="0 0 0" color="#55FFFF">
    El vidrio se contornea como un cubo completo.
  </OutlineAnnotation>
  <OutlineAnnotation pos="2 0 0" color="#FFFF55" thickness="1.5" />
  <OutlineAnnotation entity="sheep" color="#FFAA00" alwaysOnTop={true} pulse={true} />
</GameScene>
````

Contornear todos los bloques coincidentes de una estructura:

````md
<BlockAnnotationTemplate id="minecraft:glass">
  <OutlineAnnotation color="#AAFFFF" />
</BlockAnnotationTemplate>
````

## `<TextAnnotation>`

dibuja Un speech-burbuja texto etiqueta sobre El escena. It puede cualquiera follow Un mundo-espacio ancla punto o
permanecer fijo relativo Un El escena centro. Unlike El otro anotación etiquetas, su hijo contenido es El
burbuja texto itself rather que Un pasar el cursor tooltip.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `pos` | no | `x y z` mundo-espacio ancla vector |
| `x`, `y`, `z` | no | Alternative mundo-espacio ancla components cuando `pos` es omitido |
| `text` | no | burbuja texto; hijo markdown es usado cuando omitido |
| `textKey` | no | traducción clave resuelto de recurso-pack `lang` files antes de falling atrás a `text` o hijo markdown |
| `color` | no | burbuja borde color; predeterminados Un light grey |
| `backgroundAlpha` | no | fondo opacity de `0` a `255`; predeterminados a `204` |
| `maxWidth` | no | ajuste ancho en píxeles; `0` mantiene a único línea |
| `independent` | no | `true` mantiene El burbuja fijo en pantalla espacio |
| `yOffset` | no | píxel offset de El escena centro cuando `independent={true}` |
| `connectorSide` | no | `bottom`, `top`, `left`, `right`, o `none`; predeterminados a `bottom` |
| `connectorOffset` | no | píxel offset a lo largo de El burbuja borde; positive moves derecha para arriba/abajo y down para izquierda/derecha |
| `connectorLength` | no | píxel length of El conector línea; predeterminados a `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | no | opcional companion resaltar box límites |
| `highlightColor` | no | opcional resaltar box color |

mundo-anchored bubbles dibujar Un conector línea Un sus ancla. usar `connectorSide` Un elegir que
borde of El burbuja puntos at El ancla, `connectorOffset` Un move El attachment punto a lo largo de que
borde, y `connectorLength` Un control El gap entre El burbuja y ancla. Independent bubbles
son centered horizontally en El escena y usar `yOffset` para vertical placement. Estas do no dibujar a
conector. El mismo ejecución anotación es también usado cuando importing Ponder `text` anotaciones.

Ejemplo:

````md
<TextAnnotation
  pos="1.5 2 1.5"
  textKey="guidenh.sample.scene.insert_items"
  color="#FF44AAFF"
  maxWidth={120}
  backgroundAlpha={180}
  connectorSide="right"
  connectorOffset={8}
  connectorLength={12}
>
  Inserta aquí los objetos con **prioridad**.
</TextAnnotation>
````

fijo pantalla-espacio Ejemplo:

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## enriquecido Tooltip contenido

anotación hijos son compilado as normal GuideNH contenido, so tooltips puede contener:

- markdown paragraphs y headings
- elemento/bloque imágenes
- recipes
- nested non-interactivo escenas

Ejemplo:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

usar it cuando you want Un stamp El mismo anotación onto cada coincidente bloque.

| atributo | obligatorio | significado |
| --- | --- | --- |
| `id` | sí | bloque matcher en `modid:block[:meta]` form |

Reglas:

- El template solo sees bloques que ya exist cuando it es analizado
- colocar it después de `<Block>`, `<ImportStructure>`, o `<ImportStructureLib>` etiquetas que deberíUn feed it
- hijo anotaciones usar local coordenadas relativo Un cada coincidente bloque

Ejemplo:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Tooltip generado por la plantilla
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Related páginas

- [GameScene](GameScene)
- [Examples](Examples)
