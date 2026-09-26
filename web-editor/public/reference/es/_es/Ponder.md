# Ponder Animation Timeline


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH admite Ponder-style animated timelines dentro de `<GameScene>` bloques. You supply an
externo JSON archivo que defines keyframes, cámara movements y en-mundo anotaciones, y
GuideNH renderiza Un interactivo progress bar con play/pause controls debajo El 3D escena.

## rápido inicio

1. crear Un Ponder JSON archivo y colocar it en su recurso pack (Consulta [File Placement](#file-placement)).
2. Añada `<ImportPonder src="..."/>` dentro de a `<GameScene>` bloque alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **Nota:** `<ImportPonder>` debe appear dentro de a `<GameScene>` bloque. El `src` atributo es
> obligatorio. estructura datos es todavía proporcionado by `<ImportStructure>` o `<ImportStructureLib>`.

## archivo Placement

Ponder JSON files follow El mismo recurso-pack ruta Reglas as SNBT estructuras:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
```

El `src` atributo accepts ambos relativo y absoluto IDs:

| Ejemplo | resuelto as |
|---------|-------------|
| `src="my_machine.json"` | relativo Un El actual página's directorio |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | absoluto (`mymod` espacio de nombres) |

## JSON Format

```json
{
  "totalTime": 120,
  "keyframes": [
    {
      "time": 0,
      "label": "Start",
      "camera": {
        "zoom": 2.0,
        "rotX": 25.0,
        "rotY": -30.0,
        "rotZ": 0.0,
        "offX": 0.0,
        "offY": 0.0
      },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "Mid-point",
      "camera": {
        "rotY": -90.0
      },
      "annotations": [
        {
          "type": "diamond",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "color": "0xFFFF4444",
          "tooltip": "Input hatch",
          "alwaysOnTop": false
        }
      ]
    },
    {
      "time": 120,
      "camera": {
        "rotY": -150.0
      }
    }
  ]
}
```

### raíz campos

| campo | tipo | obligatorio | descripción |
|-------|------|----------|-------------|
| `totalTime` | entero | sí | Total duration en ticks (20 ticks = 1 second). Minimum clamped Un 1. |
| `keyframes` | array | sí | lista of keyframe objects. puede ser vacío. |

### Keyframe campos

| campo | tipo | obligatorio | descripción |
|-------|------|----------|-------------|
| `time` | entero | sí | Tick at que esto keyframe occurs (0 <= tiempo <= totalTime). |
| `hidden` | booleano | no | cuando `true`, El keyframe todavía aplica cámara/NBT/entidad/anotación estado at su `time`, but no visible node es drawn para it on El progress bar y visible keyframe navigation skips it. |
| `label` | cadena | no | opcional reserva etiqueta mostrado cuando al pasar el cursor El keyframe node on El progress bar. |
| `labelKey` | cadena | no | traducción clave para El keyframe etiqueta. cuando resuelto, it anula `label`. |
| `camera` | object | no | cámara estado at esto keyframe. Null campos inherit de El anterior keyframe. |
| `cameraEaseTicks` | entero o null | no | How many ticks El cámara takes Un ease de El **anterior** keyframe Un esto uno. `null` (predeterminado) = ease sobre El completo segment. `0` = instant snap. `N > 0` = ease sobre N ticks, entonces hold at El destino posición. |
| `layer` | entero o null | no | visible layer anular. `null` (o omitido) muestra todos layers. 1-based index. |
| `annotations` | array | no | lista of anotación objects mostrado mientras esto keyframe es active. |
| `sounds` | array | no | lista of sounds played once cuando esto keyframe se convierte en active durante forward playback. |
| `particles` | array | no | lista of ejecución particle bursts o presets fired cuando esto keyframe se convierte en active durante forward playback. |
| `blockChanges` | array | no | lista of bloque replacements applied cuando esto keyframe primero se convierte en active. |
| `mergeTileNBT` | array | no | Merge SNBT compounds en bloque entities at bloque positions. |
| `modifyTileNBT` | array | no | Establezca uno bloque-entidad NBT ruta Un Un SNBT valor. |
| `removeTileNBT` | array | no | eliminar uno bloque-entidad NBT ruta. |
| `createEntities` | array | no | crear Ponder-owned entities que puede ser referenced by later entidad NBT operations. |
| `setEntityNBT` | array | no | reemplazar Un referenced entidad's NBT con El supplied SNBT compound. |
| `mergeEntityNBT` | array | no | Merge Un SNBT compound en Un referenced entidad. |
| `modifyEntityNBT` | array | no | Establezca uno referenced entidad NBT ruta Un Un SNBT valor. |
| `removeEntityNBT` | array | no | eliminar uno referenced entidad NBT ruta. |
| `removeEntities` | array | no | eliminar uno o más Ponder-owned entities by `ref` usando El stable escena-entidad registry. |

ocultas keyframes son useful cuando you want additional intermediate estado cambios sin adding Un nuevo visible node
Un El timeline. para Ejemplo, you puede split several `modifyTileNBT` updates across varios ticks, mark El
intermediate keyframes as ocultas, y Conserve solo El major beats visible on El progress bar.

### cámara campos

todos cámara campos son opcional. cualquier `null` o omitido campo inherits su valor de El nearest
prior keyframe que defined it; Si no prior keyframe defined El campo, El escena's predeterminado
cámara valor es usado.

| campo | tipo | descripción |
|-------|------|-------------|
| `zoom` | decimal | cámara zoom level (0.1 - 10.0). |
| `rotX` | decimal | X-eje rotación en degrees. |
| `rotY` | decimal | Y-eje rotación en degrees. |
| `rotZ` | decimal | Z-eje rotación en degrees. |
| `offX` | decimal | Horizontal pan offset en pantalla píxeles. |
| `offY` | decimal | Vertical pan offset en pantalla píxeles. |

El cámara smoothly interpolates entre adjacent keyframes usando an **ease-en/ease-out** curva.
usar `cameraEaseTicks` on El **destination** keyframe Un control El easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
```

## bloque cambios

El `blockChanges` array en Un keyframe replaces bloques en El live estructura cuando que keyframe
se convierte en active. esto permite El animation Un mostrar antes de-y-después de states, colocar o eliminar
bloques, o animate Un machine powering on.

```json
{
  "time": 60,
  "blockChanges": [
    { "x": 1, "y": 1, "z": 1, "block": "minecraft:lit_furnace", "meta": 4, "particles": true },
    { "x": 1, "y": 2, "z": 1, "block": "minecraft:air", "particles": false },
    {
      "x": 2, "y": 1, "z": 2, "block": "minecraft:chest", "meta": 2,
      "nbt": "{Items:[{Slot:0b,id:\"minecraft:iron_ingot\",Count:8b,Damage:0s}]}"
    }
  ]
}
```

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `x`, `y`, `z` | entero | - | **obligatorio.** posición of El bloque Un cambiar (estructura coordenadas). |
| `block` | cadena | - | **obligatorio.** Registry nombre, e.g. `"minecraft:furnace"`. usar `"minecraft:air"` Un eliminar. |
| `meta` | entero | `0` | bloque metadatos / damage valor. |
| `particles` | booleano | `true` | Whether Un spawn bloque-texture particle effects cuando esto bloque cambiar fires durante forward playback. Particles son taken de El bloque's propio icon texture. Establezca a `false` Un suppress (e.g., para silent removal). |
| `nbt` | cadena | `null` | SNBT cadena para Un bloque entidad etiqueta, e.g. para chests, furnaces, etc. analizado con `JsonToNBT`. claves debe ser **unquoted** (estándar SNBT format). Ignored Si El bloque tiene no bloque entidad. |

**Seek-safe:** cuando seeking backwards El ejecución restores todos changed positions Un sus
original estructura estado, entonces re-aplica cambios de keyframes 0 a través de El actual uno.
El displayed estructura es siempre correct regardless of seek direction.

> **Nota on particles:** bloque-texture particles fire once, solo durante forward playback cuando El
> keyframe primero se convierte en active. Estas son cleared on seek, restart, o inicial cargar.

## Keyframe Sounds

Añada a `sounds` array Un Un keyframe Un play uno o más guíUn sounds cuando El keyframe se convierte en active
durante forward playback. Seeking y inicial cargar do no play keyframe sounds; restart clears El
play history so El sounds puede fire again.

```json
{
  "time": 130,
  "label": "Furnace lights up",
  "sounds": [
    { "sound": "guidenh:machine.start", "volume": 0.8 },
    { "src": "guidenh:sounds/machine/hum.ogg", "volume": 0.4, "x": 1.5, "y": 1.5, "z": 1.5 }
  ]
}
```

Sound campos:

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `sound` | cadena | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | cadena | - | Sound archivo id o ruta; `guidenh:sounds/machine/start.ogg` se convierte en `guidenh:machine.start`. |
| `volume` | decimal | `1.0` | Playback volume antes de attenuation. |
| `pitch` | decimal | `1.0` | Playback pitch. |
| `cooldown` | entero | `250` | Minimum milliseconds antes de El mismo sound puede play again. |
| `x`, `y`, `z` | decimal | none | opcional escena-espacio origen posición para pantalla-espacio attenuation. |
| `radius` | decimal | escena short lado * 0.75 | Attenuation radius en pantalla píxeles. |
| `minVolume` | decimal | `0.15` | Minimum attenuation factor. |

---

## Keyframe Particles

Añada a `particles` array Un Un keyframe cuando you want uno-shot particle bursts o timeline-local
weather overlays durante forward playback. These particles son no re-fired durante reverse
scrubbing, y seek/restart clears ellos antes de replaying El active estado.

Generic particles:

```json
{
  "time": 110,
  "particles": [
    {
      "name": "smoke",
      "x": 1.5,
      "y": 1.85,
      "z": 1.5,
      "vx": 0.0,
      "vy": 0.01,
      "vz": 0.0,
      "size": 0.18,
      "time": 16,
      "amount": 3
    }
  ]
}
```

Explosion preset:

```json
{
  "time": 160,
  "particles": [
    {
      "preset": "explosion",
      "x": 1.5,
      "y": 1.45,
      "z": 1.5,
      "time": 8,
      "power": 2.4
    }
  ]
}
```

Weather preset:

```json
{
  "time": 220,
  "particles": [
    {
      "preset": "rain",
      "weather": "snow",
      "x": [0, 2],
      "z": [0, 2],
      "time": 100,
      "amount": 8
    }
  ]
}
```

Particle campos:

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `preset` | cadena | none | Special preset. `explosion` spawns Un vanilla-style flash/smoke burst. `rain` enables El weather preset. |
| `weather` | cadena | `rain` | Weather tipo usado by `preset: "rain"`. Compatible valores: `rain`, `snow`. |
| `name` | cadena | none | Generic particle appearance. Compatible valores: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | cadena | none | Compatibility aliases para `name`. |
| `x`, `z` | decimal o array | escena límites | Particle origin o weather coverage. Generic particles usar scalar coordenadas. para `preset: "rain"`, scalar valores destino uno precipitation columna y arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | decimal | `0.0` | inicial motion vector. `motionX/Y/Z` son accepted aliases. |
| `time` / `lifetime` | entero | preset-específico | Particle lifetime en ticks. para `preset: "rain"` esto es El total weather duration, including fade-en y fade-out. |
| `size` | decimal | preset-específico | Generic particle half-tamaño en bloque units. |
| `amount` | entero | preset-específico | Generic particle count. para `explosion`, omitido amount scales de `power`. para `preset: "rain"`, esto es El average per-tick weather density. |
| `power` | decimal | `2.0` | Explosion strength para El `explosion` preset. |

Weather preset Notas:

- `preset: "rain"` es El shared weather preset entrada punto. usar `weather: "rain"` para rainfall o
  `weather: "snow"` para snowfall.
- esto preset es timeline-owned weather. It admite replay, pause, seek, y fast-forward together
  con El rest of El Ponder timeline.
- para siempre-on escena weather fuera de El Ponder timeline, usar El `<Weather>` etiqueta dentro de
  `<GameScene>` en su lugar.
- Weather presets ignore `y`; El vertical spawn range es derived de El actual escena límites.
- `x: 5, z: 8` targets uno precipitation columna. Arrays usar endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` crea dos covered rectangles.
- Si uno eje tiene extra unmatched array valores, El unmatched tail es ignored.
- El ejecución automáticamente shapes El effect con Un short inicio transition, Un steady middle
  section, y Un fin transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes sin splash particles.
- El weather area es derived de El actual GameScene límites so El effect scales con El
  importado estructura en su lugar of Un hard-coded box.
- El mismo `x/z` columna nunca stacks varios weather types at El mismo tiempo. Earlier overlapping
  weather declarations Conserve El shared columnas; later ones solo renderizan on El remaining area.

---

## bloque entidad NBT Operations

usar `mergeTileNBT`, `modifyTileNBT`, y `removeTileNBT` cuando El bloque stays en colocar but su
bloque entidad datos cambios. Operations son seek-safe: GuideNH restores El original bloque NBT y
entonces replays todos operations de keyframe 0 a través de El active keyframe.

```json
{
  "time": 80,
  "mergeTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "nbt": "{InputTanks:[{Level:{Speed:0.25,Target:0.25,Value:0.0},TankContent:{Amount:250,FluidName:\"minecraft:lava\"}}]}"
    }
  ],
  "modifyTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "path": "InputTanks[0].TankContent.Amount",
      "value": "500"
    }
  ],
  "removeTileNBT": [
    { "x": 2, "y": 1, "z": 2, "path": "InputTanks[0].Level.Target" }
  ]
}
```

| campo | usado by | descripción |
|-------|---------|-------------|
| `x`, `y`, `z` | todos | bloque-entidad bloque posición en estructura coordenadas. |
| `nbt` | `mergeTileNBT` | SNBT compound merged en El bloque entidad. Existing compound claves son merged recursively; otro valores reemplazar El old valor. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT ruta con lista indexes, e.g. `Items[0].Count` o `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT valor escrito at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow El mismo idea as Minecraft's `/data` paths: usar dots para compound claves y
`[index]` para lista entradas. lista traversal actualmente expects El traversed lista entradas Un ser
compounds, que coincide común bloque NBT como inventories, tanks, y recipe slots.

---

## entidad Actions

Regular `<Entity>` etiquetas son ya Compatible en `GameScene`. Ponder timelines puede también crear
sus propio entities con `createEntities`, entonces destino those entities by `ref` en later keyframes.

```json
{
  "time": 0,
  "createEntities": [
    {
      "ref": "marker",
      "sceneEntityId": "marker",
      "id": "minecraft:pig",
      "x": 1.5, "y": 1.0, "z": 2.5,
      "yaw": 180,
      "nbt": "{CustomName:\"Before\",CustomNameVisible:1b}"
    }
  ]
}
```

| campo | descripción |
|-------|-------------|
| `ref` | obligatorio local reference nombre para later operations. |
| `sceneEntityId` | opcional stable escena-local id usado para mount relations, replay-safe reemplazo, import/export restore, y cualquier later removal of esto logical entidad. predeterminados Un Un interno id derived de `ref`. |
| `id` | entidad ID, e.g. `minecraft:pig`, `Pig`, o Un mod entidad ID Compatible by El escena entidad loader. |
| `x`, `y`, `z` | opcional spawn posición. predeterminados a `0, 0, 0` salvo `nbt` supplies `Pos`. |
| `yaw`, `pitch` | opcional spawn rotación. predeterminados a `0, 0` salvo `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | opcional living-entidad cuerpo/head yaw anula. Si omitido mientras `yaw` es present, Estas follow `yaw`. |
| `nbt` | opcional SNBT compound applied cuando El entidad es creado. |
| `name`, `uuid` | opcional vista previa-jugador profile campos cuando creating Un vista previa jugador entidad. |
| `mount` | opcional stable `sceneEntityId` of El vehicle que esto entidad deberíUn ride después de creation o later replay. |
| `unmount` | opcional booleano que clears El entidad's actual stable mount relation antes de cualquier later `mount` es applied. |

después de creation, usar El entidad NBT operations:

```json
{
  "time": 60,
  "mergeEntityNBT": [
    { "ref": "marker", "nbt": "{Saddle:1b}" }
  ],
  "modifyEntityNBT": [
    { "ref": "marker", "path": "CustomName", "value": "\"After\"" }
  ],
  "removeEntityNBT": [
    { "ref": "marker", "path": "CustomNameVisible" }
  ]
}
```

`setEntityNBT` es también disponible cuando you want Un reemplazar El entidad's NBT en su lugar of merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

entidad actions puede también actualizar transform, vista previa-jugador pose, y stable mount estado sin
changing NBT:

```json
{
  "time": 80,
  "createEntities": [
    { "ref": "cart", "sceneEntityId": "cart", "id": "minecraft:minecart", "x": 1.5, "y": 1.0, "z": 1.5 },
    { "ref": "rider", "sceneEntityId": "rider", "id": "player", "name": "GuideNH", "x": 1.5, "y": 1.0, "z": 1.5, "mount": "cart" }
  ],
  "modifyEntityNBT": [
    { "ref": "rider", "headYaw": 270.0, "leftArmRotation": "-40 0 0" }
  ]
}
```

Un detach o eliminar Un timeline entidad, usar `unmount` o `removeEntities`:

```json
{
  "time": 120,
  "modifyEntityNBT": [
    { "ref": "rider", "unmount": true, "x": 2.5, "y": 1.0, "z": 1.5 }
  ],
  "removeEntities": [
    { "ref": "cart" }
  ]
}
```

Like bloque operations, entidad operations son replayed de El beginning whenever El active
keyframe cambios, so seeking backwards removes Ponder-creado entities y recreates El correct
estado para El destino tick.

Notas:

- `ref` identifies que Ponder-owned entidad El actual action deberíUn edit o eliminar.
- `sceneEntityId` y `mount` identify stable cross-entidad relations. `mount` siempre puntos Un a
  stable escena id, no Un another `ref`.
- Relying on sin procesar passenger NBT alone es no recommended para cross-entidad escena relationships. El
  stable registry es what mantiene mount y removal comportamiento deterministic across replay, rebuild,
  import/export, y editor vista previa refresh.

---

## anotación Fade

anotaciones smoothly fade en sobre **5 game ticks** (250 ms) whenever El active keyframe
cambios durante playback. Seeking o pausing siempre muestra anotaciones at completo opacity.

---

## anotación Types

cada entrada en El `annotations` array requires a `type` campo. Seven types son disponible.

---

### `diamond`

renderiza Un 3D diamond marker at Un mundo posición.

```json
{
  "type": "diamond",
  "x": 1.5,
  "y": 2.0,
  "z": 1.5,
  "color": "0xFFFF8800",
  "tooltip": "Click me",
  "alwaysOnTop": false
}
```

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `x`, `y`, `z` | decimal | `0.0` | mundo-espacio posición of El diamond tip. |
| `color` | cadena | `"0xFF00E000"` | ARGB color as `"0xAARRGGBB"`. |
| `tooltip` | cadena | `""` | reserva texto mostrado on pasar el cursor. |
| `tooltipKey` | cadena | `""` | traducción clave para El pasar el cursor texto. cuando resuelto, it anula `tooltip`. |
| `alwaysOnTop` | booleano | `false` | Si true, renderizado a través de solid bloques. |

---

### `box`

renderiza Un wireframe eje-aligned box de `min` a `max`.

```json
{
  "type": "box",
  "minX": 0.0, "minY": 0.0, "minZ": 0.0,
  "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
  "color": "0x8800FFFF",
  "lineWidth": 1.5,
  "alwaysOnTop": false
}
```

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `minX/Y/Z` | decimal | `0.0` | Minimum corner. |
| `maxX/Y/Z` | decimal | `1.0` | Maximum corner. |
| `color` | cadena | `"0xFFFFFFFF"` | ARGB línea color. |
| `lineWidth` | decimal | predeterminado | GL línea ancho. |
| `alwaysOnTop` | booleano | `false` | renderizan a través de bloques. |

---

### `block`

renderiza Un wireframe around uno whole bloque. esto es El Ponder JSON equivalent of
`<BlockAnnotation pos="x y z">` en Un regular `GameScene`.

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`"type": "blockBox"` y `"type": "block_box"` son accepted aliases.

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `pos` | número[3] o cadena | `[0, 0, 0]` | bloque coordenada as `[x, y, z]` o `"x y z"`. |
| `x`, `y`, `z` | número | `0` | Alternative bloque coordenada campos. valores son floored. |
| `blockX/Y/Z` | entero | `0` | Legacy-style bloque coordenada campos. |
| `color` | cadena | `"0xFFFFFFFF"` | ARGB línea color. |
| `lineWidth` | decimal | predeterminado | GL línea ancho. |
| `alwaysOnTop` | booleano | `false` | renderizan a través de bloques. |

---

### `line`

renderiza Un línea segment o polyline entre mundo positions. `points` takes priority sobre
`fromX/Y/Z` y `toX/Y/Z` cuando it contiene at least dos válido puntos.

```json
{
  "type": "line",
  "fromX": 0.5, "fromY": 0.5, "fromZ": 0.5,
  "toX": 2.5,   "toY": 0.5,   "toZ": 0.5,
  "color": "0xFFFFFF00",
  "arrow": "end",
  "lineWidth": 2.0,
  "alwaysOnTop": true
}
```

Polyline puntos puede ser escrito cualquiera as Un cadena o as Un array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `fromX/Y/Z` | decimal | `0.0` | inicio punto. |
| `toX/Y/Z` | decimal | `1.0` | fin punto. |
| `points` | cadena o array | `null` | Polyline puntos. usar `"x y z; x y z; ..."` o `[[x,y,z], ...]`. |
| `color` | cadena | `"0xFFFFFFFF"` | ARGB línea color. |
| `arrow` | cadena | `null` | `start` o `end`; omitido o no válido valores dibujar no arrow. |
| `lineWidth` | decimal | predeterminado | GL línea ancho. |
| `alwaysOnTop` | booleano | `false` | renderizan a través de bloques. |

---

### `blockface`

Resalta todas las caras de un bloque único con una superposición sólida translúcida.

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` y `"type": "block_face"` son accepted aliases.

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `pos` | número[3] o cadena | `[0, 0, 0]` | bloque coordenada as `[x, y, z]` o `"x y z"`. |
| `x`, `y`, `z` | número | `0` | Alternative bloque coordenada campos. valores son floored. |
| `blockX/Y/Z` | entero | `0` | Legacy-style bloque coordenada campos. |
| `color` | cadena | `"0x80FFFFFF"` | ARGB overlay color. |
| `alwaysOnTop` | booleano | `false` | renderizan a través de bloques. |

---

### `text`

renderiza Un speech-burbuja etiqueta anchored Un Un mundo posición. El box appears encima de El ancla by
predeterminado y es connected Un it con Un short vertical línea. texto contenido es keyframe-driven: usar
diferente `text` anotación entradas on diferente keyframes Un cambiar El displayed texto sobre tiempo.

```json
{
  "type": "text",
  "x": 1.5,
  "y": 2.5,
  "z": 1.5,
  "text": "Place items here",
  "color": "0xFF44AAFF",
  "connectorSide": "right",
  "connectorOffset": 8,
  "connectorLength": 12
}
```

para Un fijo pantalla-espacio posición que hace no project de mundo coordenadas, usar
**independent modo**. El burbuja es centered horizontally en El escena y colocado at
`yOffset` píxeles debajo El escena's vertical centre.

```json
{
  "type": "text",
  "text": "Independent label",
  "color": "0xFFFFCC00",
  "backgroundAlpha": 160,
  "independent": true,
  "yOffset": 40
}
```

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `x`, `y`, `z` | decimal | `0.0` | mundo-espacio ancla posición (ignored en independent modo). |
| `text` | cadena | - | **obligatorio.** texto Un mostrar dentro de El burbuja. |
| `color` | cadena | `"0xFFAAAAAA"` | ARGB color of El burbuja borde. |
| `backgroundAlpha` | entero | `204` | fondo opacity de `0` (transparente) a `255` (opaque). El RGB color remains El predeterminado dark navy. |
| `maxWidth` | entero | `0` | Si &gt; 0, wraps texto at esto ancho en píxeles. Omit o Establezca a `0` para a único-línea etiqueta. |
| `independent` | booleano | `false` | Si `true`, posición es relativo Un El escena centre rather que Un mundo punto. |
| `yOffset` | entero | `0` | píxel offset de El escena's vertical centre (positive = downward). usado con `independent: true`. |
| `connectorSide` | cadena | `"bottom"` | `bottom`, `top`, `left`, `right`, o `none`. Ignored en independent modo. |
| `connectorOffset` | entero | `0` | píxel offset a lo largo de El seleccionado burbuja borde; positive moves derecha para arriba/abajo y down para izquierda/derecha. |
| `connectorLength` | entero | `6` | píxel length of El conector línea. `0` oculta El línea mientras keeping lado-based placement. |
| `hlMinX/Y/Z` | decimal | `0.0` | Minimum corner of Un opcional resaltar box drawn alongside El texto burbuja. |
| `hlMaxX/Y/Z` | decimal | `1.0` | Maximum corner of El opcional resaltar box. |
| `highlightColor` | cadena | `"0x8000FFAA"` | ARGB color of El resaltar box. |

cuando `hlMinX` (o cualquier `hlMin/Max` coordenada) es present, an `InWorldBoxAnnotation` es también
creado at El specified límites con `highlightColor`. esto es useful para pointing at específico
bloque regions mientras explaining ellos.

El fondo es un dark navy burbuja by predeterminado (`#CC0E0E20`), y `backgroundAlpha` controls su
opacity. en mundo-anchored modo Un conector línea enlaces El box Un El ancla. texto admite El
completo GuideNH inline enriquecido-texto sintaxis: markdown formatting y MDX inline etiquetas. It es renderizado con
drop-shadow.

> **enriquecido texto:** El `text` campo admite El mismo inline markup usado en GuideNH guíUn páginas:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, y todos otro inline MDX etiquetas.
> simple Minecraft `§` format codes son **no** Compatible; usar MDX sintaxis en su lugar.

> A `text` anotación sin a `text` campo (o Un vacío cadena) es silently ignored.

---

### `input`

renderiza Un mouse-entrada icon (izquierda button, derecha button, o desplazar wheel) anchored Un Un mundo
posición. esto es usado Un hint que El jugador deberíUn perform Un específico interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

con Un opcional modifier clave prefix y Un elemento icon:

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb",
  "modifier": "sneak",
  "item": "minecraft:iron_ingot"
}
```

| campo | tipo | predeterminado | descripción |
|-------|------|---------|-------------|
| `x`, `y`, `z` | decimal | `0.0` | mundo-espacio ancla posición. |
| `inputType` | cadena | `"lmb"` | uno of `"lmb"`, `"rmb"`, o `"scroll"`. Case-insensitive. |
| `modifier` | cadena | `null` | opcional modifier clave: `"sneak"` o `"ctrl"`. muestra prefix texto encima de El icon. |
| `item` | cadena | `null` | opcional elemento registry ID (e.g. `"minecraft:iron_ingot"`). renderiza El elemento icon Un El izquierda of El mouse icon. admite `"modid:item:meta"` format para meta valores. |

El icon es un 16x16 sprite drawn de `ponder_widgets.png`. El box fondo es semi-transparente
dark (`#CC0E0E20`) con Un light-blue borde (`#80AAAADD`). cuando an `item` es specified El box
expands Un accommodate ambos El elemento icon y El mouse icon lado by lado.

---

## color Format

Colors son ARGB hexadecimal strings. ambos `"0xFFFFFF00"` (con `0x` prefix) y
`"FFFF00"` (sin prefix) son accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% transparente
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (predeterminado diamond color)
- `"0x8022CCFF"` - semi-transparente blue
- `"0xFFAAAAAA"` - light grey (predeterminado texto burbuja borde)

## Playback comportamiento

### Controls

| Control | Action |
|---------|--------|
| **Prev keyframe** | Jump Un El inicio of El anterior visible keyframe segment. ocultas keyframes son skipped. |
| **Play/Pause** | Toggle playback; restarts de El beginning Si ya finished. |
| **Restart** | devolver Un tick 0, reset estado, y begin playing. |
| Progress bar | clic o arrastrar Un seek Un cualquier posición. Seeking siempre pauses playback. |
| Keyframe nodes | Small tick marks on El bar para visible keyframes; pasar el cursor Un Consulta El etiqueta y direction arrow. |

### inicial estado

cuando Un página containing `<ImportPonder>` es primero opened, El escena starts **paused at tick 0**.
Press Play Un begin.

### cámara lock

mientras playback es **active** (no paused):
- El cámara follows El interpolated ruta defined by keyframes.
- Mouse arrastrar y desplazar zoom son **desactivado**.
- El layer slider y StructureLib sliders son **ocultas**.

mientras playback es **paused** o **finished**:
- completo interactivo cámara arrastrar, zoom, y layer/StructureLib control son restored.

### Keyframe node labels

cuando you pasar el cursor sobre Un keyframe node on El progress bar:
- El node grows slightly Un indicate it es hovered.
- Si El keyframe tiene a `label`, it es displayed beside El node.
- ocultas keyframes do no crear hoverable nodes, but Estas todavía apply sus timeline estado cuando playback o seeking reaches ellos.

### Layer control durante playback

El `layer` campo of El active keyframe anula El visible-layer filtro durante playback:
- `null` (o omitido) -> mostrar todos layers.
- `1`, `2`, `3`, ... -> restrict Un que 1-based layer index.

## completa Ejemplo

El following Ejemplo demonstrates cada anotación tipo across Un four-keyframe escena.

### directorio diseño

```
assets/mymod/guidebooks/
  pages/machines/grinder.mdx
  pages/machines/grinder.snbt
  pages/machines/grinder.json
```

### `grinder.json`

```json
{
  "totalTime": 240,
  "keyframes": [
    {
      "time": 0,
      "label": "Overview",
      "camera": { "zoom": 1.5, "rotX": 20, "rotY": 225 },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "Input hatch",
      "camera": { "rotY": 180 },
      "layer": null,
      "annotations": [
        {
          "type": "diamond",
          "x": 0.5, "y": 1.5, "z": 1.5,
          "color": "0xFF44FF44",
          "tooltip": "EV Input Bus",
          "alwaysOnTop": true
        },
        {
          "type": "text",
          "x": 0.5, "y": 3.0, "z": 1.5,
          "text": "Insert ore here",
          "color": "0xFF44FF44"
        },
        {
          "type": "input",
          "x": 0.5, "y": 2.0, "z": 1.5,
          "inputType": "rmb"
        }
      ]
    },
    {
      "time": 140,
      "label": "Output side",
      "camera": { "rotY": 90 },
      "layer": null,
      "annotations": [
        {
          "type": "box",
          "minX": 2.0, "minY": 0.0, "minZ": 0.0,
          "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
          "color": "0x8800AAFF",
          "lineWidth": 1.5
        },
        {
          "type": "line",
          "fromX": 2.0, "fromY": 1.0, "fromZ": 1.5,
          "toX": 2.5, "toY": 1.0, "toZ": 1.5,
          "color": "0xFFFFAA00",
          "lineWidth": 2.0,
          "alwaysOnTop": true
        },
        {
          "type": "blockface",
          "pos": [2, 1, 1],
          "color": "0x8833FF33"
        },
        {
          "type": "text",
          "x": 2.5, "y": 3.0, "z": 1.5,
          "text": "Collect dust here",
          "color": "0xFF00AAFF"
        },
        {
          "type": "input",
          "x": 2.5, "y": 2.0, "z": 1.5,
          "inputType": "lmb"
        }
      ]
    },
    {
      "time": 220,
      "label": "Scroll layer",
      "camera": { "rotY": 225 },
      "layer": null,
      "annotations": [
        {
          "type": "input",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "inputType": "scroll"
        },
        {
          "type": "text",
          "x": 1.5, "y": 3.5, "z": 1.5,
          "text": "Scroll to show layers",
          "color": "0xFFFFCC00"
        }
      ]
    },
    {
      "time": 240,
      "camera": { "rotY": 225 }
    }
  ]
}
```

### `grinder.snbt`

Un estándar NBT estructura archivo (creado con El `/structure save` command o Un tool como
Litematica). Consulta [Getting Started](Guide-Page-Format) para El completo SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

El triturador convierte los minerales en el doble de polvo. Pulsa **Reproducir** para ver la guía animada.
```

## Notas

- El Ponder progress bar es drawn encima de El layer/StructureLib sliders. durante playback El
  structural sliders son ocultas Un Conserve El UI clean; Estas reappear cuando paused.
- cámara interpolation es siempre smooth (ease-en/out) even Si some keyframes solo cambiar a
  subset of cámara ejes. usar `cameraEaseTicks` on Un keyframe Un snap El cámara instantly (`0`)
  o ease sobre Un fijo número of ticks antes de holding El destino posición.
- anotaciones belong Un a único keyframe - Estas appear solo mientras que keyframe es active
  (i.e., de su `time` tick until El siguiente keyframe's `time` tick). Overlay texto anotaciones
  fade out smoothly cuando El keyframe cambios durante playback.
- Ponder `line` anotaciones usar El mismo ejecución renderer as regular `LineAnnotation`, including
  polyline bends y inicio/fin arrows. punto marker cubes remain Un MDX-solo feature.
- Ponder `text` anotaciones usar El mismo ejecución renderer as regular `TextAnnotation`, including
  conector lado, offset, y length. Dynamic texto es keyframe-based rather que interpolated per tick.
- solo uno `<ImportPonder>` etiqueta es effective per `<GameScene>`. Un second etiqueta overwrites El primero.
- A `text` anotación con Un vacío o absent `text` campo es silently skipped.
- El `inputType` campo predeterminados a `"lmb"` Si omitido o unrecognised.
- `blockChanges` son applied en orden de El primero Un El actual keyframe cada tiempo El
  active keyframe cambios, so changing El mismo posición en varios keyframes works correctly.
- bloque/entidad NBT operations usar El mismo replay model as `blockChanges`; Estas son safe Un seek
  forwards o backwards.
- `text` anotaciones con a `maxWidth` &gt; 0 son word-wrapped usando El vanilla font renderer;
  El burbuja box alto adjusts automáticamente para multi-línea texto.
- `nbt` strings en `blockChanges` debe usar **unquoted** SNBT claves (estándar MC 1.7.10 format).
  Quoted claves será ser rejected by El analizador. cadena valores todavía require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` y `modifyEntityNBT` valores son SNBT valores, no JSON valores. para Un cadena
  valor, escape El SNBT quotes dentro de JSON: `"value": "\"hello\""`.
