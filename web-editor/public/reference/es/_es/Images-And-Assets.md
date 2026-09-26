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

`<FloatingImage>` renderiza Un cropped bitmap region que puede decimal con texto o sit truly inline dentro de a
paragraph. It también accepts explícito `modid:path` texture ids, so it puede reference texture recursos de
otro mods directly.

### atributos

| atributo | obligatorio | significado |
| --- | --- | --- |
| `src` | sí | imagen ruta |
| `x` | sí | crop inicio X en origen-imagen píxeles |
| `y` | sí | crop inicio Y en origen-imagen píxeles |
| `width` / `w` | sí | crop ancho en origen-imagen píxeles; exactamente uno forma debe ser usado |
| `height` / `h` | sí | crop alto en origen-imagen píxeles; exactamente uno forma debe ser usado |
| `scaleX` | no | horizontal mostrar multiplier, predeterminado `1.0` |
| `scaleY` | no | vertical mostrar multiplier, predeterminado `1.0` |
| `displayWidth` | no | final mostrar ancho en píxeles; preserves El crop aspect ratio cuando usado alone |
| `displayHeight` | no | final mostrar alto en píxeles; preserves El crop aspect ratio cuando usado alone |
| `wrap` | no | `inline` para true inline placement, otherwise usar El normal wrapping modes |
| `align` | no | `left` o `right` para floating placement; ignored cuando `wrap="inline"` |
| `title` | no | tooltip/título texto |
| `sound` | no | sound event played by El whole imagen |
| `soundSrc` | no | sound archivo ruta para El whole imagen |
| `trigger` | no | `click` by predeterminado, o `hover` para pasar el cursor playback |

### Notas

- `x`, `y`, `width` / `w`, y `height` / `h` son todos obligatorio together cuando cropping
- cuando El crop atributos son todos omitido, `displayWidth` o `displayHeight` muestra El completo origen imagen
- `width` y `height` now describe El crop rectangle, no El final mostrar tamaño
- `scaleX` y `scaleY` calcular El final mostrar tamaño as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` o `displayHeight` sets El final mostrar tamaño en píxeles; cuando solo uno es present, El otro dimension es calculado de El crop aspect ratio
- providing ambos `displayWidth` y `displayHeight` permite intentional non-proportional stretching
- `displayWidth` / `displayHeight` no puede ser combined con `scaleX` / `scaleY`
- único-eje stretching es Compatible by setting solo uno scale differently
- `width` con `w`, o `height` con `h`, es no válido y renderiza Un visible error
- old `FloatingImage width/height as display size` contenido es intentionally breaking y debe ser migrated manualmente
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
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    Pasa el cursor sobre la imagen para ver este tooltip.
  </ImageAnnotation>
</FloatingImage>
````

Region anotación con Un visible borde:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    Este es el tooltip de la **región resaltada**.
  </ImageAnnotation>
</FloatingImage>
````

varios regions on uno imagen:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>
````

imagen regions puede también play sounds. usar `<SoundArea>` cuando you solo need sound, o put `sound`
directly on `<ImageAnnotation>` cuando El mismo region también tiene Un tooltip o borde.

````md
<FloatingImage
  src="test1.png"
  align="left"
  x="0"
  y="0"
  width="128"
  height="128"
  sound="guidenh:image.click"
>
  <SoundArea x="0" y="0" w="64" h="64" sound="guidenh:image.left" />
  <SoundArea x="64" y="0" w="64" h="64" sound="guidenh:image.right" trigger="hover" />
  <ImageAnnotation x="10" y="10" w="40" h="40" border sound="guidenh:image.note">
    Esta región contiene texto de tooltip y un sonido de clic.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers El whole imagen. Region sounds usar cropped-imagen coordenadas
y obey El mismo overlap priority as tooltips: later regions win.

## contenido Embedding y texto Wrapping

todos bloque-level etiquetas — `<FloatingImage>`, `<Recipe>`, `<GameScene>`, `<ItemImage>`, `<BlockImage>`,
y cualquier otro etiqueta backed by `BlockTagCompiler` — compatibilidad dos opcional diseño atributos que
provide Word-style contenido embedding.

| atributo | valores | predeterminado | significado |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | texto-wrapping modo |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### ajuste modes

| modo | Word equivalent | bloque-context behaviour | Flow-context behaviour |
| --- | --- | --- | --- |
| `inline` | en línea con texto | predeterminado pila (嵌入型) | Sits on El texto línea |
| `square` | Square | Document-level decimal; texto wraps around (方形环绕) | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | Tight | mismo as `square` (紧密型) | mismo as `square` |
| `through` | a través de | mismo as `square` (穿越型) | mismo as `square` |
| `top-bottom` | arriba y abajo | completo-ancho slot; `align` repositions horizontally (上下型) | línea-inline con breaks |
| `behind` | Behind texto | Aligned inline slot; renderiza behind texto (衬于文字下方) | Sits on El línea |
| `front` | en frente of texto | Aligned inline slot; renderiza en frente of texto (浮于文字上方) | Sits on El línea |

### Alignment con floating ajuste

para `wrap=square/tight/through`:
- `align=left` (predeterminado) — bloque floats Un El **izquierda**; texto fills El derecha lado.
- `align=right` — bloque floats Un El **derecha**; texto fills El izquierda lado.
- `align=center` — bloque es centred sin floating (no texto wrapping).

### Ejemplos

izquierda-floating imagen usando El nuevo `wrap` atributo:

````md
<FloatingImage
  src="test1.png"
  wrap="square"
  align="left"
  x="0"
  y="0"
  width="128"
  height="128"
  scaleX="0.5"
  scaleY="0.5"
/>

Texto de párrafo que fluye a la derecha de la imagen…
````

derecha-floating recipe:

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

Texto que fluye a la izquierda del cuadro de receta…
````

Centred elemento imagen (no texto wrapping):

````md
<ItemImage id="minecraft:diamond" align="center" />
````

derecha-aligned elemento imagen:

````md
<ItemImage id="minecraft:diamond" align="right" />
````

elemento NBT puede ser supplied separately de El elemento id. Inline SNBT en `id` es todavía Compatible;
cuando ambos forms son present, El standalone `nbt` atributo es merged último.

````md
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

> **Nota** — `wrap="inline"` now gives `<FloatingImage>` true inline placement dentro de flow texto.
> en inline modo, `align` es ignored en su lugar of producing Un error.

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

`<BlockImage>` usa El mismo bloque-level embedding Reglas as `<FloatingImage>`, but El visual
contenido es un transparente 3D único-bloque vista previa en su lugar of Un bitmap. It es best suited para
showing how Un colocado bloque looks en-mundo mientras todavía fitting inline con normal guíUn prose.

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
