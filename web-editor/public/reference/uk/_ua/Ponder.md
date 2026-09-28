# Ponder Animation Timeline

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH підтримує Анімація Ponder-style animated timelines усередині `<GameScene>` блоки. You supply an
зовнішній JSON файл який defines keyframes, камера movements і in-світ Анотації, і
GuideNH renders an інтерактивний progress bar з play/pause controls below  3D сцена.

## Швидкий старт

1. створити a Анімація Ponder JSON файл і place it in your пакет ресурсів (Дивіться [File Placement](#file-placement)).
2. Додайте `<ImportPonder src="..."/>` усередині a `<GameScene>` блок alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **Примітка:** `<ImportPonder>` має appear усередині a `<GameScene>` блок.  `src` Атрибут is
> Обов’язково. структура дані is досі provided by `<ImportStructure>` або `<ImportStructureLib>`.

## Розміщення файлів

Анімація Ponder JSON files follow  той самий ресурс-pack шлях Правила as SNBT структури:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
```

 `src` Атрибут accepts both відносний і абсолютний IDs:

| Приклад | розв’язаний as |
|---------|-------------|
| `src="my_machine.json"` | відносний до  поточний сторінка's каталог |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | абсолютний (`mymod` простір імен) |

## Формат JSON

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

### Кореневі поля

| поле | Тип | Обов’язково | опис |
|-------|------|----------|-------------|
| `totalTime` | ціле число | Yes | Total duration in ticks (20 ticks = 1 second). Minimum clamped до 1. |
| `keyframes` | array | Yes | список of keyframe objects. може be empty. |

### Поля ключових кадрів

| поле | Тип | Обов’язково | опис |
|-------|------|----------|-------------|
| `time` | ціле число | Yes | Tick at який Цей keyframe occurs (0 <= час <= totalTime). |
| `hidden` | логічне значення | No | коли `true`,  keyframe досі застосовує камера/NBT/сутність/анотація стан at його `time`, but no видимий node is drawn для it on  progress bar і видимий keyframe Навігація skips it. |
| `label` | рядок | No | необов’язковий резервний варіант мітка показано коли наведення  keyframe node on  progress bar. |
| `labelKey` | рядок | No | ключ перекладу для  keyframe мітка. коли розв’язаний, it overrides `label`. |
| `camera` | object | No | камера стан at Цей keyframe. Null поля inherit з  попередній keyframe. |
| `cameraEaseTicks` | ціле число або null | No | How many ticks  камера takes до ease з  **попередній** keyframe до Цей один. `null` (Типове) = ease over  full segment. `0` = instant snap. `N > 0` = ease over N ticks, потім hold at  ціль позиція. |
| `layer` | ціле число або null | No | видимий layer перевизначати. `null` (або пропущений) shows усі layers. 1-based index. |
| `annotations` | array | No | список of анотація objects показано поки Цей keyframe is active. |
| `sounds` | array | No | список of sounds played once коли Цей keyframe стає active during forward playback. |
| `particles` | array | No | список of виконання particle bursts або presets fired коли Цей keyframe стає active during forward playback. |
| `blockChanges` | array | No | список of блок replacements applied коли Цей keyframe перший стає active. |
| `mergeTileNBT` | array | No | Merge SNBT compounds до блок entities at блок positions. |
| `modifyTileNBT` | array | No | Установіть один блок-сутність NBT шлях до SNBT значення. |
| `removeTileNBT` | array | No | видалити один блок-сутність NBT шлях. |
| `createEntities` | array | No | створити Анімація Ponder-owned entities який може be referenced by later сутність NBT operations. |
| `setEntityNBT` | array | No | замінити referenced сутність's NBT з  supplied SNBT compound. |
| `mergeEntityNBT` | array | No | Merge SNBT compound до referenced сутність. |
| `modifyEntityNBT` | array | No | Установіть один referenced сутність NBT шлях до SNBT значення. |
| `removeEntityNBT` | array | No | видалити один referenced сутність NBT шлях. |
| `removeEntities` | array | No | видалити один або more Анімація Ponder-owned entities by `ref` використовуючи  stable сцена-сутність registry. |

прихований keyframes є useful коли you want additional intermediate стан зміни без adding новий видимий node
до  timeline. для Приклад, you може split several `modifyTileNBT` updates across кілька ticks, mark 
intermediate keyframes as прихований, і Збережіть лише  major beats видимий on  progress bar.

### Поля камери

усі Поля камери є необов’язковий. Any `null` або пропущений поле inherits його значення з  nearest
prior keyframe який defined it; Якщо no prior keyframe defined  поле,  сцена's Типове
камера значення is використовується.

| поле | Тип | опис |
|-------|------|-------------|
| `zoom` | число з плаваючою комою | камера масштаб level (0.1 - 10.0). |
| `rotX` | число з плаваючою комою | X-вісь обертання in degrees. |
| `rotY` | число з плаваючою комою | Y-вісь обертання in degrees. |
| `rotZ` | число з плаваючою комою | Z-вісь обертання in degrees. |
| `offX` | число з плаваючою комою | Horizontal pan offset in екран пікселі. |
| `offY` | число з плаваючою комою | Vertical pan offset in екран пікселі. |

 камера smoothly interpolates between adjacent keyframes використовуючи an **ease-in/ease-out** curve.
використовувати `cameraEaseTicks` on  **destination** keyframe до control  easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
```

## блок зміни

 `blockChanges` array in keyframe replaces блоки in  live структура коли який keyframe
стає active. Цей дозволяє  animation до показувати до-і-після states, place або видалити
блоки, або animate machine powering on.

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

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `x`, `y`, `z` | ціле число | - | **Обов’язково.** позиція of  блок до змінити (структура координати). |
| `block` | рядок | - | **Обов’язково.** Registry назва, e.g. `"minecraft:furnace"`. використовувати `"minecraft:air"` до видалити. |
| `meta` | ціле число | `0` | блок метадані / damage значення. |
| `particles` | логічне значення | `true` | Whether до spawn блок-texture particle effects коли Цей блок змінити fires during forward playback. Particles є taken з  блок's власний icon texture. Установіть до `false` до suppress (e.g., для silent removal). |
| `nbt` | рядок | `null` | SNBT рядок для блок сутність тег, e.g. для chests, furnaces, etc. проаналізований з `JsonToNBT`. ключі має be **unquoted** (стандартний SNBT format). Ignored Якщо  блок має no блок сутність. |

**Seek-safe:** коли seeking backwards  виконання restores усі changed positions до їхні
original структура стан, потім re-застосовує зміни з keyframes 0 through  поточний один.
 displayed структура is завжди correct regardless of seek direction.

> **Примітка on particles:** блок-texture particles fire once, лише during forward playback коли 
> keyframe перший стає active. Вони є cleared on seek, restart, або початковий завантажити.

## Keyframe Sounds

Додайте a `sounds` array до keyframe до play один або more посібник sounds коли  keyframe стає active
during forward playback. Seeking і початковий завантажити do не play keyframe sounds; restart clears 
play history so  sounds може fire again.

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

Sound поля:

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `sound` | рядок | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | рядок | - | Sound файл id або шлях; `guidenh:sounds/machine/start.ogg` стає `guidenh:machine.start`. |
| `volume` | число з плаваючою комою | `1.0` | Playback volume до attenuation. |
| `pitch` | число з плаваючою комою | `1.0` | Playback pitch. |
| `cooldown` | ціле число | `250` | Minimum milliseconds до  той самий sound може play again. |
| `x`, `y`, `z` | число з плаваючою комою | none | необов’язковий сцена-простір source позиція для простір екрана attenuation. |
| `radius` | число з плаваючою комою | сцена short бік * 0.75 | Attenuation radius in екран пікселі. |
| `minVolume` | число з плаваючою комою | `0.15` | Minimum attenuation factor. |

---

## Keyframe Particles

Додайте a `particles` array до keyframe коли you want один-shot particle bursts або timeline-локальний
Погода overlays during forward playback. Ці particles є не re-fired during reverse
scrubbing, і seek/restart clears їх до replaying  active стан.

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

Погода preset:

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

Particle поля:

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `preset` | рядок | none | Special preset. `explosion` spawns vanilla-style flash/smoke burst. `rain` enables  Погода preset. |
| `weather` | рядок | `rain` | Погода Тип використовується by `preset: "rain"`. Підтримувані значення: `rain`, `snow`. |
| `name` | рядок | none | Generic particle appearance. Підтримувані значення: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | рядок | none | Compatibility aliases для `name`. |
| `x`, `z` | число з плаваючою комою або array | сцена межі | Particle origin або Погода coverage. Generic particles використовувати scalar координати. для `preset: "rain"`, scalar значення ціль один precipitation стовпець і arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | число з плаваючою комою | `0.0` | початковий motion вектор. `motionX/Y/Z` є accepted aliases. |
| `time` / `lifetime` | ціле число | preset-певний | Particle lifetime in ticks. для `preset: "rain"` Цей is  total Погода duration, including fade-in і fade-out. |
| `size` | число з плаваючою комою | preset-певний | Generic particle half-розмір in блок units. |
| `amount` | ціле число | preset-певний | Generic particle count. для `explosion`, пропущений amount scales з `power`. для `preset: "rain"`, Цей is  average per-tick Погода density. |
| `power` | число з плаваючою комою | `2.0` | Explosion strength для  `explosion` preset. |

Погода preset Примітки:

- `preset: "rain"` is  shared Погода preset запис точка. використовувати `weather: "rain"` для rainfall або
  `weather: "snow"` для snowfall.
- Цей preset is timeline-owned Погода. It підтримує replay, pause, seek, і fast-forward together
  з  rest of  Анімація Ponder timeline.
- для завжди-on сцена Погода поза  Анімація Ponder timeline, використовувати  `<Weather>` тег усередині
  `<GameScene>` замість цього.
- Погода presets ignore `y`;  vertical spawn range is derived з  поточний сцена межі.
- `x: 5, z: 8` targets один precipitation стовпець. Arrays використовувати endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` creates два covered rectangles.
- Якщо один вісь має extra unmatched array значення,  unmatched tail is ignored.
-  виконання автоматично shapes  effect з short початок transition, steady middle
  section, і an кінець transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes без splash particles.
-  Погода area is derived з  поточний GameScene межі so  effect scales з 
  імпортований структура замість цього of hard-coded box.
-  той самий `x/z` стовпець ніколи stacks кілька Погода types at  той самий час. Earlier overlapping
  Погода declarations Збережіть  shared стовпці; later ones лише відтворювати on  remaining area.

---

## блок сутність NBT Operations

використовувати `mergeTileNBT`, `modifyTileNBT`, і `removeTileNBT` коли  блок stays in place but його
блок сутність дані зміни. Operations є seek-safe: GuideNH restores  original блок NBT і
потім replays усі operations з keyframe 0 through  active keyframe.

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

| поле | використовується by | опис |
|-------|---------|-------------|
| `x`, `y`, `z` | усі | блок-сутність блок позиція in структура координати. |
| `nbt` | `mergeTileNBT` | SNBT compound merged до  блок сутність. Existing compound ключі є merged recursively; other значення замінити  old значення. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT шлях з список indexes, e.g. `Items[0].Count` або `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT значення записаний at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow  той самий idea as Minecraft's `/data` paths: використовувати dots для compound ключі і
`[index]` для список записи. список traversal зараз expects  traversed список записи до be
compounds, який відповідає звичайний блок NBT наприклад inventories, tanks, і recipe slots.

---

## сутність Actions

Regular `<Entity>` теги є вже Підтримувані in `GameScene`. Анімація Ponder timelines може також створити
їхні власний entities з `createEntities`, потім ціль those entities by `ref` in later keyframes.

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

| поле | опис |
|-------|-------------|
| `ref` | Обов’язково локальний reference назва для later operations. |
| `sceneEntityId` | необов’язковий stable сцена-локальний id використовується для mount relations, replay-safe заміна, import/export restore, і any later removal of Цей logical сутність. типові до внутрішній id derived з `ref`. |
| `id` | сутність ID, e.g. `minecraft:pig`, `Pig`, або mod сутність ID Підтримувані by  сцена сутність loader. |
| `x`, `y`, `z` | необов’язковий spawn позиція. типові до `0, 0, 0` unless `nbt` supplies `Pos`. |
| `yaw`, `pitch` | необов’язковий spawn обертання. типові до `0, 0` unless `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | необов’язковий living-сутність body/head yaw overrides. Якщо пропущений поки `yaw` is present, Вони follow `yaw`. |
| `nbt` | необов’язковий SNBT compound applied коли  сутність is створений. |
| `name`, `uuid` | необов’язковий перегляд-гравець profile поля коли creating a перегляд гравець сутність. |
| `mount` | необов’язковий stable `sceneEntityId` of  vehicle який Цей сутність слід ride після creation або later replay. |
| `unmount` | необов’язковий логічне значення який clears  сутність's поточний stable mount relation до any later `mount` is applied. |

після creation, використовувати  сутність NBT operations:

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

`setEntityNBT` is також доступний коли you want до замінити  сутність's NBT замість цього of merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

сутність actions може також оновити transform, перегляд-гравець pose, і stable mount стан без
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

до detach або видалити timeline сутність, використовувати `unmount` або `removeEntities`:

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

Like блок operations, сутність operations є replayed з  beginning whenever  active
keyframe зміни, so seeking backwards removes Анімація Ponder-створений entities і recreates  correct
стан для  ціль tick.

Примітки:

- `ref` identifies який Анімація Ponder-owned сутність  поточний action слід edit або видалити.
- `sceneEntityId` і `mount` identify stable cross-сутність relations. `mount` завжди точки до a
  stable сцена id, не до another `ref`.
- Relying on raw passenger NBT alone is не recommended для cross-сутність сцена relationships. 
  stable registry is what зберігає mount і removal поведінка deterministic across replay, rebuild,
  import/export, і редактор перегляд refresh.

---

## анотація Fade

Анотації smoothly fade in over **5 game ticks** (250 ms) whenever  active keyframe
зміни during playback. Seeking або pausing завжди shows Анотації at full opacity.

---

## анотація Types

кожен запис in  `annotations` array requires a `type` поле. Seven types є доступний.

---

### `diamond`

Renders 3D diamond marker at a світ позиція.

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

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `x`, `y`, `z` | число з плаваючою комою | `0.0` | простір світу позиція of  diamond tip. |
| `color` | рядок | `"0xFF00E000"` | ARGB колір as `"0xAARRGGBB"`. |
| `tooltip` | рядок | `""` | резервний варіант текст показано on наведення. |
| `tooltipKey` | рядок | `""` | ключ перекладу для  наведення текст. коли розв’язаний, it overrides `tooltip`. |
| `alwaysOnTop` | логічне значення | `false` | Якщо true, відтворений through solid блоки. |

---

### `box`

Renders wireframe вирівняний за осями box з `min` до `max`.

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

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `minX/Y/Z` | число з плаваючою комою | `0.0` | Minimum corner. |
| `maxX/Y/Z` | число з плаваючою комою | `1.0` | Maximum corner. |
| `color` | рядок | `"0xFFFFFFFF"` | ARGB рядок колір. |
| `lineWidth` | число з плаваючою комою | Типове | GL рядок ширина. |
| `alwaysOnTop` | логічне значення | `false` | відтворювати through блоки. |

---

### `block`

Renders wireframe around один whole блок. Цей is  Анімація Ponder JSON equivalent of
`<BlockAnnotation pos="x y z">` in regular `GameScene`.

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`"type": "blockBox"` і `"type": "block_box"` є accepted aliases.

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `pos` | число[3] або рядок | `[0, 0, 0]` | блок координата as `[x, y, z]` або `"x y z"`. |
| `x`, `y`, `z` | число | `0` | Alternative блок координата поля. значення є floored. |
| `blockX/Y/Z` | ціле число | `0` | Legacy-style блок координата поля. |
| `color` | рядок | `"0xFFFFFFFF"` | ARGB рядок колір. |
| `lineWidth` | число з плаваючою комою | Типове | GL рядок ширина. |
| `alwaysOnTop` | логічне значення | `false` | відтворювати through блоки. |

---

### `line`

Renders a відрізок або ламана between світ positions. `points` takes priority over
`fromX/Y/Z` і `toX/Y/Z` коли it містить at least два дійсний точки.

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

ламана точки може be записаний either as a рядок або as array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `fromX/Y/Z` | число з плаваючою комою | `0.0` | початок точка. |
| `toX/Y/Z` | число з плаваючою комою | `1.0` | кінець точка. |
| `points` | рядок або array | `null` | ламана точки. використовувати `"x y z; x y z; ..."` або `[[x,y,z], ...]`. |
| `color` | рядок | `"0xFFFFFFFF"` | ARGB рядок колір. |
| `arrow` | рядок | `null` | `start` або `end`; пропущений або недійсний значення малювати no стрілка. |
| `lineWidth` | число з плаваючою комою | Типове | GL рядок ширина. |
| `alwaysOnTop` | логічне значення | `false` | відтворювати through блоки. |

---

### `blockface`

Підсвічує всі грані одного блока напівпрозорим суцільним накладанням.

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` і `"type": "block_face"` є accepted aliases.

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `pos` | число[3] або рядок | `[0, 0, 0]` | блок координата as `[x, y, z]` або `"x y z"`. |
| `x`, `y`, `z` | число | `0` | Alternative блок координата поля. значення є floored. |
| `blockX/Y/Z` | ціле число | `0` | Legacy-style блок координата поля. |
| `color` | рядок | `"0x80FFFFFF"` | ARGB overlay колір. |
| `alwaysOnTop` | логічне значення | `false` | відтворювати through блоки. |

---

### `text`

Renders speech-бульбашка мітка anchored до a світ позиція.  box appears над  якір by
Типове і is connected до it з short vertical рядок. текст вміст is keyframe-driven: використовувати
інший `text` анотація записи on інший keyframes до змінити  displayed текст over час.

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

для a фіксований простір екрана позиція який не project з світ координати, використовувати
**independent режим**.  бульбашка is centered horizontally in  сцена і розміщений at
`yOffset` пікселі below  сцена's vertical centre.

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

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `x`, `y`, `z` | число з плаваючою комою | `0.0` | простір світу якір позиція (ignored in independent режим). |
| `text` | рядок | - | **Обов’язково.** текст до відображати усередині  бульбашка. |
| `color` | рядок | `"0xFFAAAAAA"` | ARGB колір of  бульбашка межа. |
| `backgroundAlpha` | ціле число | `204` | тло opacity з `0` (прозорий) до `255` (opaque).  RGB колір remains  Типове dark navy. |
| `maxWidth` | ціле число | `0` | Якщо &gt; 0, wraps текст at Цей ширина in пікселі. Omit або Установіть до `0` для a один-рядок мітка. |
| `independent` | логічне значення | `false` | Якщо `true`, позиція is відносний до  сцена centre rather ніж a світ точка. |
| `yOffset` | ціле число | `0` | піксель offset з  сцена's vertical centre (positive = downward). використовується з `independent: true`. |
| `connectorSide` | рядок | `"bottom"` | `bottom`, `top`, `left`, `right`, або `none`. Ignored in independent режим. |
| `connectorOffset` | ціле число | `0` | піксель offset уздовж  вибраний бульбашка край; positive moves праворуч для угорі/унизу і down для ліворуч/праворуч. |
| `connectorLength` | ціле число | `6` | піксель length of  з’єднувач рядок. `0` hides  рядок поки keeping бік-based placement. |
| `hlMinX/Y/Z` | число з плаваючою комою | `0.0` | Minimum corner of an необов’язковий виділяти box drawn alongside  текст бульбашка. |
| `hlMaxX/Y/Z` | число з плаваючою комою | `1.0` | Maximum corner of  необов’язковий виділяти box. |
| `highlightColor` | рядок | `"0x8000FFAA"` | ARGB колір of  виділяти box. |

коли `hlMinX` (або any `hlMin/Max` координата) is present, an `InWorldBoxAnnotation` is також
створений at  specified межі з `highlightColor`. Цей is useful для pointing at певний
блок regions поки explaining їх.

 тло is dark navy бульбашка by Типове (`#CC0E0E20`), і `backgroundAlpha` controls його
opacity. In світ-anchored режим з’єднувач рядок links  box до  якір. текст підтримує 
full GuideNH inline розширений-текст синтаксис: Markdown formatting і MDX inline теги. It is відтворений з
drop-shadow.

> **розширений текст:**  `text` поле підтримує  той самий inline markup використовується in GuideNH посібник сторінки:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, і усі other inline MDX теги.
> Plain Minecraft `§` format codes є **не** Підтримувані; використовувати MDX синтаксис замість цього.

> A `text` анотація без a `text` поле (або empty рядок) is silently ignored.

---

### `input`

Renders mouse-ввід icon (ліворуч button, праворуч button, або прокручування wheel) anchored до a світ
позиція. Цей is використовується до hint який  гравець слід perform певний interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

з an необов’язковий modifier ключ prefix і an предмет icon:

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

| поле | Тип | Типове | опис |
|-------|------|---------|-------------|
| `x`, `y`, `z` | число з плаваючою комою | `0.0` | простір світу якір позиція. |
| `inputType` | рядок | `"lmb"` | один of `"lmb"`, `"rmb"`, або `"scroll"`. Case-insensitive. |
| `modifier` | рядок | `null` | необов’язковий modifier ключ: `"sneak"` або `"ctrl"`. Shows prefix текст над  icon. |
| `item` | рядок | `null` | необов’язковий предмет registry ID (e.g. `"minecraft:iron_ingot"`). Renders  предмет icon до  ліворуч of  mouse icon. підтримує `"modid:item:meta"` format для meta значення. |

 icon is 16x16 sprite drawn з `ponder_widgets.png`.  box тло is semi-прозорий
dark (`#CC0E0E20`) з light-blue межа (`#80AAAADD`). коли an `item` is specified  box
expands до accommodate both  предмет icon і  mouse icon бік by бік.

---

## Формат кольору

Colors є ARGB hexadecimal strings. Both `"0xFFFFFF00"` (з `0x` prefix) і
`"FFFF00"` (без prefix) є accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% прозорий
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (Типове diamond колір)
- `"0x8022CCFF"` - semi-прозорий blue
- `"0xFFAAAAAA"` - light grey (Типове текст бульбашка межа)

## Поведінка відтворення

### Controls

| Control | Action |
|---------|--------|
| **Prev keyframe** | Jump до  початок of  попередній видимий keyframe segment. прихований keyframes є skipped. |
| **Play/Pause** | Toggle playback; restarts з  beginning Якщо вже finished. |
| **Restart** | повертати до tick 0, reset стан, і begin playing. |
| Progress bar | натискання або перетягування до seek до any позиція. Seeking завжди pauses playback. |
| Keyframe nodes | Small tick marks on  bar для видимий keyframes; наведення до Дивіться  мітка і direction стрілка. |

### Початковий стан

коли a сторінка containing `<ImportPonder>` is перший opened,  сцена starts **paused at tick 0**.
Press Play до begin.

### камера lock

поки playback is **active** (не paused):
-  камера follows  interpolated шлях defined by keyframes.
- Mouse перетягування і прокручування масштаб є **вимкнено**.
-  layer slider і StructureLib sliders є **прихований**.

поки playback is **paused** або **finished**:
- Full інтерактивний камера перетягування, масштаб, і layer/StructureLib control є restored.

### Keyframe node labels

коли you наведення over keyframe node on  progress bar:
-  node grows slightly до indicate it is hovered.
- Якщо  keyframe має a `label`, it is displayed beside  node.
- прихований keyframes do не створити hoverable nodes, but Вони досі apply їхні timeline стан коли playback або seeking reaches їх.

### Layer control during playback

 `layer` поле of  active keyframe overrides  видимий-layer фільтр during playback:
- `null` (або пропущений) -> показувати усі layers.
- `1`, `2`, `3`, ... -> restrict до який 1-based layer index.

## Повний приклад

 following Приклад demonstrates кожен анотація Тип across four-keyframe сцена.

### Структура каталогів

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

стандартний NBT структура файл (створений з  `/structure save` command або tool наприклад
Litematica). Дивіться [Getting Started](Guide-Page-Format) для  full SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

Дробарка перетворює руди на подвійну кількість пилу. Натисніть **Відтворити**, щоб переглянути анімований посібник.
```

## Примітки

-  Анімація Ponder progress bar is drawn над  layer/StructureLib sliders. During playback 
  structural sliders є прихований до Збережіть  UI clean; Вони reappear коли paused.
- камера interpolation is завжди smooth (ease-in/out) even Якщо some keyframes лише змінити a
  subset of камера осі. використовувати `cameraEaseTicks` on keyframe до snap  камера instantly (`0`)
  або ease over a фіксований число of ticks до holding  ціль позиція.
- Анотації belong до a один keyframe - Вони appear лише поки який keyframe is active
  (i.e., з його `time` tick until  наступний keyframe's `time` tick). Overlay текст Анотації
  fade out smoothly коли  keyframe зміни during playback.
- Анімація Ponder `line` Анотації використовувати  той самий виконання renderer as regular `LineAnnotation`, including
  ламана bends і початок/кінець arrows. точка marker cubes remain MDX-лише feature.
- Анімація Ponder `text` Анотації використовувати  той самий виконання renderer as regular `TextAnnotation`, including
  з’єднувач бік, offset, і length. Dynamic текст is keyframe-based rather ніж interpolated per tick.
- лише один `<ImportPonder>` тег is effective per `<GameScene>`. second тег overwrites  перший.
- A `text` анотація з empty або absent `text` поле is silently skipped.
-  `inputType` поле типові до `"lmb"` Якщо пропущений або unrecognised.
- `blockChanges` є applied in порядок з  перший до  поточний keyframe кожен час 
  active keyframe зміни, so changing  той самий позиція in кілька keyframes works correctly.
- блок/сутність NBT operations використовувати  той самий replay model as `blockChanges`; Вони є safe до seek
  forwards або backwards.
- `text` Анотації з a `maxWidth` &gt; 0 є word-wrapped використовуючи  vanilla font renderer;
   бульбашка box висота adjusts автоматично для multi-рядок текст.
- `nbt` strings in `blockChanges` має використовувати **unquoted** SNBT ключі (стандартний MC 1.7.10 format).
  Quoted ключі буде be rejected by  аналізатор. рядок значення досі require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` і `modifyEntityNBT` значення є SNBT значення, не JSON значення. для a рядок
  значення, escape  SNBT quotes усередині JSON: `"value": "\"hello\""`.
