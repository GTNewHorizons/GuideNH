# Ponder Animation Timeline

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH suporta Animação Ponder-style animated timelines dentro de `<GameScene>` blocos. You supply an
externo JSON arquivo que defines keyframes, câmera movements e em-mundo Anotações, e
GuideNH renders Um interativo progress bar com play/pause controls abaixo O 3D cena.

## emício rápido

1. criar Uma Animação Ponder JSON arquivo e colocar it em seu pacote de recursos (Consulte [File Placement](#file-placement)).
2. Adicione `<ImportPonder src="..."/>` dentro de a `<GameScene>` bloco alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **Observação:** `<ImportPonder>` deve appear dentro de a `<GameScene>` bloco. O `src` Atributo é
> Obrigatório. Structure dados é ainda fornecido by `<ImportStructure>` ou `<ImportStructureLib>`.

## Posicionamento de arquivos

Animação Ponder JSON files follow O mesmo recurso-pack caminho Regras as SNBT structures:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
```

O `src` Atributo accepts ambos relativo e absoluto IDs:

| Exemplo | resolvido as |
|---------|-------------|
| `src="my_machine.json"` | relativo para O atual página's diretório |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | absoluto (`mymod` namespace) |

## Formato JSON

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

### Campos raiz

| campo | Tipo | Obrigatório | descrição |
|-------|------|----------|-------------|
| `totalTime` | inteiro | sim | Total duration em ticks (20 ticks = 1 second). Minimum clamped para 1. |
| `keyframes` | array | sim | lista of keyframe objects. pode ser vazio. |

### Campos de keyframe

| campo | Tipo | Obrigatório | descrição |
|-------|------|----------|-------------|
| `time` | inteiro | sim | Tick at que Este keyframe occurs (0 <= time <= totalTime). |
| `hidden` | booleano | não | quando `true`, O keyframe ainda aplica câmera/NBT/entity/anotação estado at seu `time`, but não visível node é drawn para it on O progress bar e visível keyframe Navegação skips it. |
| `label` | texto | não | opcional reserva rótulo exibido quando ao passar o mouse O keyframe node on O progress bar. |
| `labelKey` | texto | não | chave de tradução para O keyframe rótulo. quando resolvido, it substitui `label`. |
| `camera` | object | não | câmera estado at Este keyframe. Null campos inherit de O anterior keyframe. |
| `cameraEaseTicks` | inteiro ou null | não | How many ticks O câmera takes para ease de O **anterior** keyframe para Este um. `null` (Padrão) = ease sobre O completo segment. `0` = instant snap. `N > 0` = ease sobre N ticks, então hold at O destino posição. |
| `layer` | inteiro ou null | não | visível layer substituir. `null` (ou omitted) mostra todos layers. 1-based index. |
| `annotations` | array | não | lista of anotação objects exibido enquanto Este keyframe é active. |
| `sounds` | array | não | lista of sounds played once quando Este keyframe torna-se active durante forward playback. |
| `particles` | array | não | lista of execução particle bursts ou presets fired quando Este keyframe torna-se active durante forward playback. |
| `blockChanges` | array | não | lista of bloco replacements applied quando Este keyframe primeiro torna-se active. |
| `mergeTileNBT` | array | não | Merge SNBT compounds em tile entities at bloco positions. |
| `modifyTileNBT` | array | não | Defina um tile-entity NBT caminho para Um SNBT valor. |
| `removeTileNBT` | array | não | remover um tile-entity NBT caminho. |
| `createEntities` | array | não | criar Animação Ponder-owned entities que pode ser referenced by later entity NBT operations. |
| `setEntityNBT` | array | não | substituir Uma referenced entity's NBT com O supplied SNBT compound. |
| `mergeEntityNBT` | array | não | Merge Um SNBT compound em Uma referenced entity. |
| `modifyEntityNBT` | array | não | Defina um referenced entity NBT caminho para Um SNBT valor. |
| `removeEntityNBT` | array | não | remover um referenced entity NBT caminho. |
| `removeEntities` | array | não | remover um ou mais Animação Ponder-owned entities by `ref` using O stable cena-entity registry. |

oculto keyframes são useful quando you want additional intermediate estado alterações sem adding Uma new visível node
para O timeline. para Exemplo, you pode split several `modifyTileNBT` updates across vários ticks, mark O
intermediate keyframes as oculto, e Mantenha somente O major beats visível on O progress bar.

### Campos da câmera

todos Campos da câmera são opcional. qualquer `null` ou omitted campo inherits seu valor de O nearest
prior keyframe que defined it; Se não prior keyframe defined O campo, O cena's Padrão
câmera valor é usado.

| campo | Tipo | descrição |
|-------|------|-------------|
| `zoom` | decimal | câmera zoom level (0.1 - 10.0). |
| `rotX` | decimal | X-axis rotação em degrees. |
| `rotY` | decimal | Y-axis rotação em degrees. |
| `rotZ` | decimal | Z-axis rotação em degrees. |
| `offX` | decimal | Horizontal pan offset em tela pixels. |
| `offY` | decimal | Vertical pan offset em tela pixels. |

O câmera smoothly interpolates entre adjacent keyframes using an **ease-em/ease-out** curva.
usar `cameraEaseTicks` on O **destination** keyframe para control O easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
```

## bloco alterações

O `blockChanges` array em Uma keyframe replaces blocos em O live structure quando que keyframe
torna-se active. Este permite O animation para mostrar antes de-e-depois de states, colocar ou remover
blocos, ou animate Uma machine powering on.

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

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `x`, `y`, `z` | inteiro | - | **Obrigatório.** posição of O bloco para alterar (structure coordenadas). |
| `block` | texto | - | **Obrigatório.** Registry nome, e.g. `"minecraft:furnace"`. usar `"minecraft:air"` para remover. |
| `meta` | inteiro | `0` | bloco metadata / damage valor. |
| `particles` | booleano | `true` | Whether para spawn bloco-texture particle effects quando Este bloco alterar fires durante forward playback. Particles são taken de O bloco's próprio icon texture. Defina para `false` para suppress (e.g., para silent removal). |
| `nbt` | texto | `null` | SNBT texto para Uma tile entity tag, e.g. para chests, furnaces, etc. analisado com `JsonToNBT`. chaves deve ser **unquoted** (padrão SNBT format). Ignored Se O bloco tem não tile entity. |

**Seek-safe:** quando seeking backwards O execução restores todos changed positions para seus
original structure estado, então re-aplica alterações de keyframes 0 por O atual um.
O displayed structure é sempre correct regardless of seek direction.

> **Observação on particles:** bloco-texture particles fire once, somente durante forward playback quando O
> keyframe primeiro torna-se active. Elas são cleared on seek, restart, ou inicial carregar.

## Keyframe Sounds

Adicione a `sounds` array para Uma keyframe para play um ou mais guia sounds quando O keyframe torna-se active
durante forward playback. Seeking e inicial carregar do não play keyframe sounds; restart clears O
play history so O sounds pode fire again.

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

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `sound` | texto | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | texto | - | Sound arquivo id ou caminho; `guidenh:sounds/machine/start.ogg` torna-se `guidenh:machine.start`. |
| `volume` | decimal | `1.0` | Playback volume antes de attenuation. |
| `pitch` | decimal | `1.0` | Playback pitch. |
| `cooldown` | inteiro | `250` | Minimum milliseconds antes de O mesmo sound pode play again. |
| `x`, `y`, `z` | decimal | none | opcional cena-espaço origem posição para espaço da tela attenuation. |
| `radius` | decimal | cena short lado * 0.75 | Attenuation radius em tela pixels. |
| `minVolume` | decimal | `0.15` | Minimum attenuation factor. |

---

## Keyframe Particles

Adicione a `particles` array para Uma keyframe quando you want um-shot particle bursts ou timeline-local
Clima overlays durante forward playback. Estes particles são não re-fired durante reverse
scrubbing, e seek/restart clears eles antes de replaying O active estado.

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

Clima preset:

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

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `preset` | texto | none | Special preset. `explosion` spawns Uma vanilla-style flash/smoke burst. `rain` enables O Clima preset. |
| `weather` | texto | `rain` | Clima Tipo usado by `preset: "rain"`. Compatível valores: `rain`, `snow`. |
| `name` | texto | none | Generic particle appearance. Compatível valores: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | texto | none | Compatibility aliases para `name`. |
| `x`, `z` | decimal ou array | cena bounds | Particle origin ou Clima coverage. Generic particles usar scalar coordenadas. para `preset: "rain"`, scalar valores destino um precipitation coluna e arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | decimal | `0.0` | inicial motion vetor. `motionX/Y/Z` são accepted aliases. |
| `time` / `lifetime` | inteiro | preset-específico | Particle lifetime em ticks. para `preset: "rain"` Este é O total Clima duration, including fade-em e fade-out. |
| `size` | decimal | preset-específico | Generic particle half-tamanho em bloco units. |
| `amount` | inteiro | preset-específico | Generic particle count. para `explosion`, omitted amount scales de `power`. para `preset: "rain"`, Este é O average per-tick Clima density. |
| `power` | decimal | `2.0` | Explosion strength para O `explosion` preset. |

Clima preset Observações:

- `preset: "rain"` é O shared Clima preset entrada ponto. usar `weather: "rain"` para rainfall ou
  `weather: "snow"` para snowfall.
- Este preset é timeline-owned Clima. It suporta replay, pause, seek, e fast-forward together
  com O rest of O Animação Ponder timeline.
- para sempre-on cena Clima fora de O Animação Ponder timeline, usar O `<Weather>` tag dentro de
  `<GameScene>` em vez disso.
- Clima presets ignore `y`; O vertical spawn range é derived de O atual cena bounds.
- `x: 5, z: 8` targets um precipitation coluna. Arrays usar endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` cria dois covered rectangles.
- Se um axis tem extra unmatched array valores, O unmatched tail é ignored.
- O execução automaticamente shapes O effect com Uma short emício transition, Uma steady middle
  section, e Um fim transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes sem splash particles.
- O Clima area é derived de O atual GameScene bounds so O effect scales com O
  imported structure em vez disso of Uma hard-coded box.
- O mesmo `x/z` coluna nunca stacks vários Clima types at O mesmo time. Earlier overlapping
  Clima declarations Mantenha O shared columns; later ones somente renderizar on O remaining area.

---

## Tile Entity NBT Operations

usar `mergeTileNBT`, `modifyTileNBT`, e `removeTileNBT` quando O bloco stays em colocar but seu
tile entity dados alterações. Operations são seek-safe: GuideNH restores O original tile NBT e
então replays todos operations de keyframe 0 por O active keyframe.

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

| campo | usado by | descrição |
|-------|---------|-------------|
| `x`, `y`, `z` | todos | Tile-entity bloco posição em structure coordenadas. |
| `nbt` | `mergeTileNBT` | SNBT compound merged em O tile entity. Existing compound chaves são merged recursively; outro valores substituir O old valor. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT caminho com lista indexes, e.g. `Items[0].Count` ou `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT valor written at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow O mesmo idea as Minecraft's `/data` paths: usar dots para compound chaves e
`[index]` para lista entradas. lista traversal atualmente expects O traversed lista entradas para ser
compounds, que corresponde comum tile NBT como inventories, tanks, e recipe slots.

---

## Entity Actions

Regular `<Entity>` tags são já Compatível em `GameScene`. Animação Ponder timelines pode também criar
seus próprio entities com `createEntities`, então destino those entities by `ref` em later keyframes.

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

| campo | descrição |
|-------|-------------|
| `ref` | Obrigatório local reference nome para later operations. |
| `sceneEntityId` | opcional stable cena-local id usado para mount relations, replay-safe replacement, import/export restore, e qualquer later removal of Este logical entity. Defaults para Um interno id derived de `ref`. |
| `id` | Entity ID, e.g. `minecraft:pig`, `Pig`, ou Uma mod entity ID Compatível by O cena entity loader. |
| `x`, `y`, `z` | opcional spawn posição. Defaults para `0, 0, 0` a menos que `nbt` supplies `Pos`. |
| `yaw`, `pitch` | opcional spawn rotação. Defaults para `0, 0` a menos que `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | opcional living-entity corpo/head yaw substitui. Se omitted enquanto `yaw` é present, Elas follow `yaw`. |
| `nbt` | opcional SNBT compound applied quando O entity é criado. |
| `name`, `uuid` | opcional visualização-player profile campos quando creating Uma visualização player entity. |
| `mount` | opcional stable `sceneEntityId` of O vehicle que Este entity deve ride depois de creation ou later replay. |
| `unmount` | opcional booleano que clears O entity's atual stable mount relation antes de qualquer later `mount` é applied. |

depois de creation, usar O entity NBT operations:

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

`setEntityNBT` é também disponível quando you want para substituir O entity's NBT em vez disso of merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

Entity actions pode também atualizar transform, visualização-player pose, e stable mount estado sem
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

para detach ou remover Uma timeline entity, usar `unmount` ou `removeEntities`:

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

Like tile operations, entity operations são replayed de O beginning whenever O active
keyframe alterações, so seeking backwards removes Animação Ponder-criado entities e recreates O correct
estado para O destino tick.

Observações:

- `ref` identifies que Animação Ponder-owned entity O atual action deve edit ou remover.
- `sceneEntityId` e `mount` identify stable cross-entity relations. `mount` sempre pontos para a
  stable cena id, não para another `ref`.
- Relying on bruto passenger NBT alone é não recommended para cross-entity cena relationships. O
  stable registry é what keeps mount e removal behavior deterministic across replay, rebuild,
  import/export, e editor visualização refresh.

---

## anotação Fade

Anotações smoothly fade em sobre **5 game ticks** (250 ms) whenever O active keyframe
alterações durante playback. Seeking ou pausing sempre mostra Anotações at completo opacity.

---

## anotação Types

cada entrada em O `annotations` array requires a `type` campo. Seven types são disponível.

---

### `diamond`

Renders Uma 3D diamond marker at Uma mundo posição.

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

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `x`, `y`, `z` | decimal | `0.0` | espaço do mundo posição of O diamond tip. |
| `color` | texto | `"0xFF00E000"` | ARGB cor as `"0xAARRGGBB"`. |
| `tooltip` | texto | `""` | reserva texto exibido on passar o mouse. |
| `tooltipKey` | texto | `""` | chave de tradução para O passar o mouse texto. quando resolvido, it substitui `tooltip`. |
| `alwaysOnTop` | booleano | `false` | Se true, renderizado por solid blocos. |

---

### `box`

Renders Uma wireframe alinhado aos eixos box de `min` para `max`.

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

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `minX/Y/Z` | decimal | `0.0` | Minimum corner. |
| `maxX/Y/Z` | decimal | `1.0` | Maximum corner. |
| `color` | texto | `"0xFFFFFFFF"` | ARGB linha cor. |
| `lineWidth` | decimal | Padrão | GL linha largura. |
| `alwaysOnTop` | booleano | `false` | renderizar por blocos. |

---

### `block`

Renders Uma wireframe around um whole bloco. Este é O Animação Ponder JSON equivalent of
`<BlockAnnotation pos="x y z">` em Uma regular `GameScene`.

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`"type": "blockBox"` e `"type": "block_box"` são accepted aliases.

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `pos` | número[3] ou texto | `[0, 0, 0]` | bloco coordenada as `[x, y, z]` ou `"x y z"`. |
| `x`, `y`, `z` | número | `0` | Alternative bloco coordenada campos. valores são floored. |
| `blockX/Y/Z` | inteiro | `0` | Legacy-style bloco coordenada campos. |
| `color` | texto | `"0xFFFFFFFF"` | ARGB linha cor. |
| `lineWidth` | decimal | Padrão | GL linha largura. |
| `alwaysOnTop` | booleano | `false` | renderizar por blocos. |

---

### `line`

Renders Uma segmento de linha ou polilinha entre mundo positions. `points` takes priority sobre
`fromX/Y/Z` e `toX/Y/Z` quando it contém at least dois válido pontos.

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

polilinha pontos pode ser written qualquer um as Uma texto ou as Um array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `fromX/Y/Z` | decimal | `0.0` | emício ponto. |
| `toX/Y/Z` | decimal | `1.0` | fim ponto. |
| `points` | texto ou array | `null` | polilinha pontos. usar `"x y z; x y z; ..."` ou `[[x,y,z], ...]`. |
| `color` | texto | `"0xFFFFFFFF"` | ARGB linha cor. |
| `arrow` | texto | `null` | `start` ou `end`; omitted ou inválido valores desenhar não seta. |
| `lineWidth` | decimal | Padrão | GL linha largura. |
| `alwaysOnTop` | booleano | `false` | renderizar por blocos. |

---

### `blockface`

Destaca todas as faces de um único bloco com uma sobreposição sólida translúcida.

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` e `"type": "block_face"` são accepted aliases.

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `pos` | número[3] ou texto | `[0, 0, 0]` | bloco coordenada as `[x, y, z]` ou `"x y z"`. |
| `x`, `y`, `z` | número | `0` | Alternative bloco coordenada campos. valores são floored. |
| `blockX/Y/Z` | inteiro | `0` | Legacy-style bloco coordenada campos. |
| `color` | texto | `"0x80FFFFFF"` | ARGB overlay cor. |
| `alwaysOnTop` | booleano | `false` | renderizar por blocos. |

---

### `text`

Renders Uma speech-bubble rótulo anchored para Uma mundo posição. O box appears acima de O anchor by
Padrão e é connected para it com Uma short vertical linha. texto conteúdo é keyframe-driven: usar
diferente `text` anotação entradas on diferente keyframes para alterar O displayed texto sobre time.

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

para Uma fixo espaço da tela posição que não faz project de mundo coordenadas, usar
**independent modo**. O bubble é centered horizontally em O cena e placed at
`yOffset` pixels abaixo O cena's vertical centre.

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

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `x`, `y`, `z` | decimal | `0.0` | espaço do mundo anchor posição (ignored em independent modo). |
| `text` | texto | - | **Obrigatório.** texto para exibir dentro de O bubble. |
| `color` | texto | `"0xFFAAAAAA"` | ARGB cor of O bubble borda. |
| `backgroundAlpha` | inteiro | `204` | fundo opacity de `0` (transparent) para `255` (opaque). O RGB cor remains O Padrão dark navy. |
| `maxWidth` | inteiro | `0` | Se &gt; 0, wraps texto at Este largura em pixels. Omit ou Defina para `0` para a único-linha rótulo. |
| `independent` | booleano | `false` | Se `true`, posição é relativo para O cena centre rather que Uma mundo ponto. |
| `yOffset` | inteiro | `0` | Pixel offset de O cena's vertical centre (positive = downward). usado com `independent: true`. |
| `connectorSide` | texto | `"bottom"` | `bottom`, `top`, `left`, `right`, ou `none`. Ignored em independent modo. |
| `connectorOffset` | inteiro | `0` | Pixel offset along O selecionado bubble edge; positive moves direita para topo/baixo e down para esquerda/direita. |
| `connectorLength` | inteiro | `6` | Pixel length of O connector linha. `0` oculta O linha enquanto keeping lado-based placement. |
| `hlMinX/Y/Z` | decimal | `0.0` | Minimum corner of Um opcional destacar box drawn alongside O texto bubble. |
| `hlMaxX/Y/Z` | decimal | `1.0` | Maximum corner of O opcional destacar box. |
| `highlightColor` | texto | `"0x8000FFAA"` | ARGB cor of O destacar box. |

quando `hlMinX` (ou qualquer `hlMin/Max` coordenada) é present, an `InWorldBoxAnnotation` é também
criado at O specified bounds com `highlightColor`. Este é useful para pointing at específico
bloco regions enquanto explaining eles.

O fundo é Uma dark navy bubble by Padrão (`#CC0E0E20`), e `backgroundAlpha` controls seu
opacity. em mundo-anchored modo Uma connector linha links O box para O anchor. texto suporta O
completo GuideNH inline rico-texto sintaxe: Markdown formatting e MDX inline tags. It é renderizado com
drop-shadow.

> **rico texto:** O `text` campo suporta O mesmo inline markup usado em GuideNH guia pages:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, e todos outro inline MDX tags.
> simples Minecraft `§` format codes são **não** Compatível; usar MDX sintaxe em vez disso.

> A `text` anotação sem a `text` campo (ou Um vazio texto) é silently ignored.

---

### `input`

Renders Uma mouse-entrada icon (esquerda button, direita button, ou rolar wheel) anchored para Uma mundo
posição. Este é usado para hint que O player deve perform Uma específico interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

com Um opcional modifier chave prefix e Um item icon:

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

| campo | Tipo | Padrão | descrição |
|-------|------|---------|-------------|
| `x`, `y`, `z` | decimal | `0.0` | espaço do mundo anchor posição. |
| `inputType` | texto | `"lmb"` | um of `"lmb"`, `"rmb"`, ou `"scroll"`. Case-insensitive. |
| `modifier` | texto | `null` | opcional modifier chave: `"sneak"` ou `"ctrl"`. mostra prefix texto acima de O icon. |
| `item` | texto | `null` | opcional item registry ID (e.g. `"minecraft:iron_ingot"`). Renders O item icon para O esquerda of O mouse icon. suporta `"modid:item:meta"` format para meta valores. |

O icon é Uma 16x16 sprite drawn de `ponder_widgets.png`. O box fundo é semi-transparent
dark (`#CC0E0E20`) com Uma light-blue borda (`#80AAAADD`). quando an `item` é specified O box
expands para accommodate ambos O item icon e O mouse icon lado by lado.

---

## Formato de cores

Colors são ARGB hexadecimal strings. ambos `"0xFFFFFF00"` (com `0x` prefix) e
`"FFFF00"` (sem prefix) são accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% transparent
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (Padrão diamond cor)
- `"0x8022CCFF"` - semi-transparent blue
- `"0xFFAAAAAA"` - light grey (Padrão texto bubble borda)

## Comportamento da reprodução

### Controls

| Control | Action |
|---------|--------|
| **Prev keyframe** | Jump para O emício of O anterior visível keyframe segment. oculto keyframes são skipped. |
| **Play/Pause** | Toggle playback; restarts de O beginning Se já finished. |
| **Restart** | retornar para tick 0, reset estado, e begin playing. |
| Progress bar | clicar ou arrastar para seek para qualquer posição. Seeking sempre pauses playback. |
| Keyframe nodes | Small tick marks on O bar para visível keyframes; passar o mouse para Consulte O rótulo e direction seta. |

### Estado inicial

quando Uma página containing `<ImportPonder>` é primeiro opened, O cena starts **paused at tick 0**.
Press Play para begin.

### câmera lock

enquanto playback é **active** (não paused):
- O câmera follows O interpolated caminho defined by keyframes.
- Mouse arrastar e rolar zoom são **desativado**.
- O layer slider e StructureLib sliders são **oculto**.

enquanto playback é **paused** ou **finished**:
- completo interativo câmera arrastar, zoom, e layer/StructureLib control são restored.

### Keyframe node labels

quando you passar o mouse sobre Uma keyframe node on O progress bar:
- O node grows slightly para indicate it é hovered.
- Se O keyframe tem a `label`, it é displayed beside O node.
- oculto keyframes do não criar hoverable nodes, but Elas ainda apply seus timeline estado quando playback ou seeking reaches eles.

### Layer control durante playback

O `layer` campo of O active keyframe substitui O visível-layer filtro durante playback:
- `null` (ou omitted) -> mostrar todos layers.
- `1`, `2`, `3`, ... -> restrict para que 1-based layer index.

## Exemplo completo

O following Exemplo demonstrates cada anotação Tipo across Uma four-keyframe cena.

### Estrutura de diretórios

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

Uma padrão NBT structure arquivo (criado com O `/structure save` command ou Uma tool como
Litematica). Consulte [Getting Started](Guide-Page-Format) para O completo SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

O triturador transforma minérios em pó duplicado. Pressione **Reproduzir** para ver o passo a passo animado.
```

## Observações

- O Animação Ponder progress bar é drawn acima de O layer/StructureLib sliders. durante playback O
  structural sliders são oculto para Mantenha O UI clean; Elas reappear quando paused.
- câmera interpolation é sempre smooth (ease-em/out) even Se some keyframes somente alterar a
  subset of câmera axes. usar `cameraEaseTicks` on Uma keyframe para snap O câmera instantly (`0`)
  ou ease sobre Uma fixo número of ticks antes de holding O destino posição.
- Anotações belong para a único keyframe - Elas appear somente enquanto que keyframe é active
  (i.e., de seu `time` tick until O próximo keyframe's `time` tick). Overlay texto Anotações
  fade out smoothly quando O keyframe alterações durante playback.
- Animação Ponder `line` Anotações usar O mesmo execução renderer as regular `LineAnnotation`, including
  polilinha bends e emício/fim arrows. ponto marker cubes remain Um MDX-somente feature.
- Animação Ponder `text` Anotações usar O mesmo execução renderer as regular `TextAnnotation`, including
  connector lado, offset, e length. Dynamic texto é keyframe-based rather que interpolated per tick.
- somente um `<ImportPonder>` tag é effective per `<GameScene>`. Uma second tag overwrites O primeiro.
- A `text` anotação com Um vazio ou absent `text` campo é silently skipped.
- O `inputType` campo defaults para `"lmb"` Se omitted ou unrecognised.
- `blockChanges` são applied em ordem de O primeiro para O atual keyframe cada time O
  active keyframe alterações, so changing O mesmo posição em vários keyframes works correctly.
- Tile/entity NBT operations usar O mesmo replay model as `blockChanges`; Elas são safe para seek
  forwards ou backwards.
- `text` Anotações com a `maxWidth` &gt; 0 são word-wrapped using O vanilla font renderer;
  O bubble box altura adjusts automaticamente para multi-linha texto.
- `nbt` strings em `blockChanges` deve usar **unquoted** SNBT chaves (padrão MC 1.7.10 format).
  Quoted chaves irá ser rejected by O analisador. texto valores ainda require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` e `modifyEntityNBT` valores são SNBT valores, não JSON valores. para Uma texto
  valor, escape O SNBT quotes dentro de JSON: `"value": "\"hello\""`.
