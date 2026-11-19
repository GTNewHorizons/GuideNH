# imágenes y recursos


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH admite ambos normal markdown imágenes y several ejecución-específico visual elements.

## recurso Resolution Reglas

guíUn recursos resolver con El mismo Reglas usado by página enlaces.

| ruta form | Ejemplo | significado |
| --- | --- | --- |
| relativo | `test1.png` | relativo Un El actual página archivo |
| rooted | `/assets/example_structure.snbt` | relativo Un El actual guíUn raíz |
| explícito recurso id | `guidenh:textures/gui/example.png` | absoluto `modid:path` lookup |

## Markdown imágenes

normal markdown imágenes son Compatible:

````md
![Example](test1.png)
````

GuideNH resolves El ruta y carga El binary recurso de El guíUn contenido raíz.

## `FloatingImage`

Con `wrap="inline"`, la imagen se centra verticalmente respecto al texto que la rodea y la línea aumenta su altura para contenerla por completo. Las líneas de texto adyacentes conservan su altura habitual; no hay un límite máximo fijo para la altura de línea. Si una imagen muy alta debe ocupar su propia línea, coloca la etiqueta en un párrafo independiente, con una línea en blanco antes y después.

`<FloatingImage>` renderiza Un cropped bitmap region que puede decimal con texto o sit truly inline dentro de a
paragraph. It también accepts explícito `modid:path` texture ids, so it puede reference texture recursos de
otro mods directly.

### atributos

| atributo | obligatorio | significado |
| --- | --- | --- |
| `src` | sí | imagen ruta |
| `x` | al recortar | crop inicio X en origen-imagen píxeles |
| `y` | al recortar | crop inicio Y en origen-imagen píxeles |
| `width` / `w` | al recortar | crop ancho en origen-imagen píxeles; exactamente uno forma debe ser usado |
| `height` / `h` | al recortar | crop alto en origen-imagen píxeles; exactamente uno forma debe ser usado |
| `scaleX` | no | horizontal mostrar multiplier, predeterminado `1.0` |
| `scaleY` | no | vertical mostrar multiplier, predeterminado `1.0` |
| `displayWidth` | no | final mostrar ancho en píxeles; preserves El crop aspect ratio cuando usado alone |
| `displayHeight` | no | final mostrar alto en píxeles; preserves El crop aspect ratio cuando usado alone |
| `wrap` | no | `inline` coloca la imagen en la línea; en otro caso, `align` elige el lado flotante |
| `align` | no | `left` o `right` para floating placement; ignored cuando `wrap="inline"` |
| `title` | no | tooltip/título texto |
| `alt` | no | Texto alternativo |
| `sound` | no | sound event played by El whole imagen |
| `soundSrc` | no | sound archivo ruta para El whole imagen |
| `trigger` | no | `click` by predeterminado, o `hover` para pasar el cursor playback |

### Notas

- `x`, `y`, `width` / `w`, y `height` / `h` son todos obligatorio together cuando cropping
- cuando El crop atributos son todos omitido, `displayWidth` o `displayHeight` muestra El completo origen imagen
- `width` y `height` indican el recorte original. Usa `displayWidth` / `displayHeight` o factores de escala para el tamaño mostrado.
- `scaleX` y `scaleY` calcular El final mostrar tamaño as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` o `displayHeight` sets El final mostrar tamaño en píxeles; cuando solo uno es present, El otro dimension es calculado de El crop aspect ratio
- providing ambos `displayWidth` y `displayHeight` permite intentional non-proportional stretching
- `displayWidth` / `displayHeight` no puede ser combined con `scaleX` / `scaleY`
- único-eje stretching es Compatible by setting solo uno scale differently
- `width` con `w`, o `height` con `h`, es no válido y renderiza Un visible error
- `src` puede ser relativo, rooted, o Un explícito `modid:path` texture id como `minecraft:textures/gui/options_background.png`

### Ejemplo

````md
<FloatingImage
  src="minecraft:textures/gui/options_background.png"
  x="0"
  y="0"
  width="32"
  height="32"
  displayWidth="64"
  displayHeight="64"
  wrap="inline"
  title="Example"
/>
````

## `ImageAnnotation`

`<ImageAnnotation>` es un hijo element of `<FloatingImage>` que attaches Un enriquecido-texto tooltip (y
Un opcional colored borde) Un Un rectangular region of El imagen. coordenadas son specified en
**cropped-imagen píxeles** y son automáticamente proportionally scaled cuando El cropped imagen es resized
o stretched.

### atributos

| atributo | obligatorio | predeterminado | significado |
| --- | --- | --- | --- |
| `x` | no | — | izquierda borde of El region en imagen píxeles |
| `y` | no | — | arriba borde of El region en imagen píxeles |
| `w` | no | — | ancho of El region en imagen píxeles |
| `h` | no | — | alto of El region en imagen píxeles |
| `border` | no | `false` | mostrar Un colored borde around El region |
| `borderColor` | no | random | borde color (`#RRGGBB` o `#AARRGGBB`) |
| `borderThickness` | no | `1` | borde thickness en mostrar píxeles |
| `sound` | no | none | opcional sound event played para esto region |
| `src` | no | none | opcional sound archivo ruta; converted Un Un sound event id |
| `trigger` | no | `click` | `click` o `hover` |

### Notas

- omitting todos four of `x`, `y`, `w`, `h` makes El anotación cover El **whole imagen**
- Si cualquier of El four es present, El remaining omitido ones predeterminado a `0` (origin) o `1` (tamaño)
- borde es **no mostrado by predeterminado**; Añada `border` o `border={true}` Un enable it
- cuando `borderColor` es omitido y `border` es activado, Un random fully-opaque color es usado
- hijo MDX contenido es renderizado as El tooltip cuerpo y puede incluir cualquier inline/bloque elements
- later anotaciones (lower en El lista) take pasar el cursor priority sobre earlier ones cuando regions overlap

### Ejemplo

Whole-imagen anotación:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation>
    Pasa el cursor sobre la imagen para ver este tooltip.
  </ImageAnnotation>
</FloatingImage>
````

Region anotación con Un visible borde:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="2" y="2" w="15" h="10" border borderColor="#FFFF4444" borderThickness="2">
    Este es el tooltip de la **región resaltada**.
  </ImageAnnotation>
</FloatingImage>
````

varios regions on uno imagen:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="0" y="0" w="16" h="32" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="16" y="0" w="16" h="32" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>
````

imagen regions puede también play sounds. usar `<SoundArea>` cuando you solo need sound, o put `sound`
directly on `<ImageAnnotation>` cuando El mismo region también tiene Un tooltip o borde.

````md
<FloatingImage
  displayWidth="128" displayHeight="128" src="test1.png"
  align="left"
  x="0"
  y="0"
  width="32"
  height="32"
  sound="guidenh:guide.sample_click"
>
  <SoundArea x="0" y="0" w="16" h="32" sound="guidenh:guide.sample_left" />
  <SoundArea x="16" y="0" w="16" h="32" sound="guidenh:guide.sample_hover" trigger="hover" />
  <ImageAnnotation x="2" y="2" w="10" h="10" border sound="guidenh:guide.sample_click">
    Esta región contiene texto de tooltip y un sonido de clic.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers El whole imagen. Region sounds usar cropped-imagen coordenadas
y obey El mismo overlap priority as tooltips: later regions win.

## contenido Embedding y texto Wrapping

Las etiquetas basadas en `BlockTagCompiler`, como `<Recipe>`, `<GameScene>` y `<BlockImage>`, aceptan `wrap` y `align`.

`<FloatingImage>` usa reglas propias: `wrap="inline"` la sitúa en la línea; en otro caso, `align="left"` o `"right"` elige el lado flotante. `<ItemImage>` es un icono en línea; usa `<Column>` o `<Row>` para alinearlo.

| Atributo | Predeterminado | Significado |
| --- | --- | --- |
| `wrap` | `inline` | `inline`: posición normal. `square`, `tight`, `through`: flotar a izquierda o derecha. `top-bottom`, `behind`, `front`: por ahora, posición normal alineada, sin capas de texto separadas. |
| `align` | `left` | `left`, `center` o `right` en contexto de bloque. Al flotar, `right` elige la derecha y los demás valores la izquierda. Para centrar: `wrap="inline" align="center"`. |
| `float` | — | `left` / `right` heredado en texto, cuando faltan `wrap` y `align`. Se recomiendan estos atributos explícitos. |

```mdx
<BlockImage id="minecraft:stone" wrap="square" align="right" />

<BlockImage id="minecraft:diamond_block" wrap="inline" align="center" />

<Column align="center">
  <ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
</Column>
```

## Navigation Texture Icons

frontmatter puede usar `icon_texture` Un mostrar Un texture en su lugar of Un elemento en navigation/búsqueda:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

El archivo debe decode as Un imagen. El ruta es resuelto like cualquier otro guíUn recurso ruta.

## Non-imagen recursos

GuideNH páginas puede también reference non-imagen ejecución recursos, especially estructura files, para Ejemplo:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

These recursos son cargado a través de El mismo guíUn recurso pipeline but son consumed by personalizado etiquetas rather que renderizado directly as imágenes.

## Best Practices

- Conserve página-local imágenes cerca El página que usa ellos
- Conserve reusable files under El guíUn raíz `assets/` carpeta
- prefer rooted `/assets/...` paths para shared files referenced by varios páginas
- usar texture icons solo para real imagen recursos

## `BlockImage`

`<BlockImage>` muestra un bloque colocado como vista previa 3D transparente y admite los atributos anteriores de alineación y ajuste de texto.

clave comportamiento:

- transparente fondo y borde
- no escena buttons, no layer slider, no anotación authoring surface
- pasar el cursor todavía muestra El seleccionado bloque outline y tooltip
- `scale` cambios cámara zoom y predeterminados a `4`
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, y `up`
- `nbt` supplies bloque-entidad SNBT; inline `id="mod:block:meta:{...}"` SNBT todavía works, but El
  standalone `nbt` atributo es easier Un leer y es preferido

Ejemplo:

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## ejecución Ejemplo Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Related páginas

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
