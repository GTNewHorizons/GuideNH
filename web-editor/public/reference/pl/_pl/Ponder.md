# Ponder Animation Timeline

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH obsługuje Animacja Ponder-style animated timelines wewnątrz `<GameScene>` bloki. You supply an
zewnętrzny JSON plik który defines keyframes, kamera movements i in-świat Adnotacje, i
GuideNH renders interaktywny progress bar z play/pause controls below  3D scena.

## Szybki początek

1. utworzyć Animacja Ponder JSON plik i place it in your pakiet zasobów (Zobacz [File Placement](#file-placement)).
2. Dodaj `<ImportPonder src="..."/>` wewnątrz a `<GameScene>` blok alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **Uwaga:** `<ImportPonder>` musi appear wewnątrz a `<GameScene>` blok.  `src` Atrybut is
> Wymagane. struktura dane is nadal provided by `<ImportStructure>` lub `<ImportStructureLib>`.

## Rozmieszczenie plików

Animacja Ponder JSON files follow  ten sam zasób-pack ścieżka Zasady as SNBT struktury:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
```

 `src` Atrybut accepts both względny i bezwzględny IDs:

| Przykład | rozwiązany as |
|---------|-------------|
| `src="my_machine.json"` | względny do  bieżący strona's katalog |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | bezwzględny (`mymod` przestrzeń nazw) |

## Format JSON

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

### Pola główne

| pole | Typ | Wymagane | opis |
|-------|------|----------|-------------|
| `totalTime` | liczba całkowita | Yes | Total duration in ticks (20 ticks = 1 second). Minimum clamped do 1. |
| `keyframes` | array | Yes | lista of keyframe objects. może be empty. |

### Pola klatek kluczowych

| pole | Typ | Wymagane | opis |
|-------|------|----------|-------------|
| `time` | liczba całkowita | Yes | Tick at który Ten keyframe occurs (0 <= czas <= totalTime). |
| `hidden` | wartość logiczna | No | gdy `true`,  keyframe nadal stosuje kamera/NBT/byt/adnotacja stan at jego `time`, but no widoczny node is drawn dla it on  progress bar i widoczny keyframe Nawigacja skips it. |
| `label` | ciąg znaków | No | opcjonalny awaryjny etykieta wyświetlany gdy najechanie  keyframe node on  progress bar. |
| `labelKey` | ciąg znaków | No | klucz tłumaczenia dla  keyframe etykieta. gdy rozwiązany, it overrides `label`. |
| `camera` | object | No | kamera stan at Ten keyframe. Null pola inherit z  poprzedni keyframe. |
| `cameraEaseTicks` | liczba całkowita lub null | No | How many ticks  kamera takes do ease z  **poprzedni** keyframe do Ten jeden. `null` (Domyślne) = ease over  full segment. `0` = instant snap. `N > 0` = ease over N ticks, następnie hold at  cel pozycja. |
| `layer` | liczba całkowita lub null | No | widoczny layer nadpisywać. `null` (lub pominięty) shows wszystkie layers. 1-based index. |
| `annotations` | array | No | lista of adnotacja objects wyświetlany podczas Ten keyframe is active. |
| `sounds` | array | No | lista of sounds played once gdy Ten keyframe staje się active during forward playback. |
| `particles` | array | No | lista of czas wykonania particle bursts lub presets fired gdy Ten keyframe staje się active during forward playback. |
| `blockChanges` | array | No | lista of blok replacements applied gdy Ten keyframe pierwszy staje się active. |
| `mergeTileNBT` | array | No | Merge SNBT compounds do blok entities at blok positions. |
| `modifyTileNBT` | array | No | Ustaw jeden blok-byt NBT ścieżka do SNBT wartość. |
| `removeTileNBT` | array | No | usuwać jeden blok-byt NBT ścieżka. |
| `createEntities` | array | No | utworzyć Animacja Ponder-owned entities który może be referenced by later byt NBT operations. |
| `setEntityNBT` | array | No | zastąpić referenced byt's NBT z  supplied SNBT compound. |
| `mergeEntityNBT` | array | No | Merge SNBT compound do referenced byt. |
| `modifyEntityNBT` | array | No | Ustaw jeden referenced byt NBT ścieżka do SNBT wartość. |
| `removeEntityNBT` | array | No | usuwać jeden referenced byt NBT ścieżka. |
| `removeEntities` | array | No | usuwać jeden lub more Animacja Ponder-owned entities by `ref` używając  stable scena-byt registry. |

ukryty keyframes są useful gdy you want additional intermediate stan zmiany bez adding nowy widoczny node
do  timeline. dla Przykład, you może split several `modifyTileNBT` updates across wiele ticks, mark 
intermediate keyframes as ukryty, i Zachowaj tylko  major beats widoczny on  progress bar.

### Pola kamery

wszystkie Pola kamery są opcjonalny. Any `null` lub pominięty pole inherits jego wartość z  nearest
prior keyframe który defined it; Jeśli no prior keyframe defined  pole,  scena's Domyślne
kamera wartość is używany.

| pole | Typ | opis |
|-------|------|-------------|
| `zoom` | liczba zmiennoprzecinkowa | kamera powiększenie level (0.1 - 10.0). |
| `rotX` | liczba zmiennoprzecinkowa | X-oś obrót in degrees. |
| `rotY` | liczba zmiennoprzecinkowa | Y-oś obrót in degrees. |
| `rotZ` | liczba zmiennoprzecinkowa | Z-oś obrót in degrees. |
| `offX` | liczba zmiennoprzecinkowa | Horizontal pan offset in ekran piksele. |
| `offY` | liczba zmiennoprzecinkowa | Vertical pan offset in ekran piksele. |

 kamera smoothly interpolates between adjacent keyframes używając an **ease-in/ease-out** curve.
używać `cameraEaseTicks` on  **destination** keyframe do control  easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
```

## blok zmiany

 `blockChanges` array in keyframe replaces bloki in  live struktura gdy który keyframe
staje się active. Ten pozwala  animation do pokazywać przed-i-po states, place lub usuwać
bloki, lub animate machine powering on.

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

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `x`, `y`, `z` | liczba całkowita | - | **Wymagane.** pozycja of  blok do zmiana (struktura współrzędne). |
| `block` | ciąg znaków | - | **Wymagane.** Registry nazwa, e.g. `"minecraft:furnace"`. używać `"minecraft:air"` do usuwać. |
| `meta` | liczba całkowita | `0` | blok metadane / damage wartość. |
| `particles` | wartość logiczna | `true` | Whether do spawn blok-texture particle effects gdy Ten blok zmiana fires during forward playback. Particles są taken z  blok's własny icon texture. Ustaw do `false` do suppress (e.g., dla silent removal). |
| `nbt` | ciąg znaków | `null` | SNBT ciąg znaków dla blok byt tag, e.g. dla chests, furnaces, etc. przeanalizowany z `JsonToNBT`. klucze musi be **unquoted** (standardowy SNBT format). Ignored Jeśli  blok ma no blok byt. |

**Seek-safe:** gdy seeking backwards  czas wykonania restores wszystkie changed positions do ich
original struktura stan, następnie re-stosuje zmiany z keyframes 0 through  bieżący jeden.
 displayed struktura is zawsze correct regardless of seek direction.

> **Uwaga on particles:** blok-texture particles fire once, tylko during forward playback gdy 
> keyframe pierwszy staje się active. jeden są cleared on seek, restart, lub początkowy ładować.

## Keyframe Sounds

Dodaj a `sounds` array do keyframe do play jeden lub more przewodnik sounds gdy  keyframe staje się active
during forward playback. Seeking i początkowy ładować do nie play keyframe sounds; restart clears 
play history so  sounds może fire again.

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

Sound pola:

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `sound` | ciąg znaków | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | ciąg znaków | - | Sound plik id lub ścieżka; `guidenh:sounds/machine/start.ogg` staje się `guidenh:machine.start`. |
| `volume` | liczba zmiennoprzecinkowa | `1.0` | Playback volume przed attenuation. |
| `pitch` | liczba zmiennoprzecinkowa | `1.0` | Playback pitch. |
| `cooldown` | liczba całkowita | `250` | Minimum milliseconds przed  ten sam sound może play again. |
| `x`, `y`, `z` | liczba zmiennoprzecinkowa | none | opcjonalny scena-przestrzeń source pozycja dla przestrzeń ekranu attenuation. |
| `radius` | liczba zmiennoprzecinkowa | scena short strona * 0.75 | Attenuation radius in ekran piksele. |
| `minVolume` | liczba zmiennoprzecinkowa | `0.15` | Minimum attenuation factor. |

---

## Keyframe Particles

Dodaj a `particles` array do keyframe gdy you want jeden-shot particle bursts lub timeline-lokalny
Pogoda overlays during forward playback. Te particles są nie re-fired during reverse
scrubbing, i seek/restart clears ich przed replaying  active stan.

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

Pogoda preset:

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

Particle pola:

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `preset` | ciąg znaków | none | Special preset. `explosion` spawns vanilla-style flash/smoke burst. `rain` enables  Pogoda preset. |
| `weather` | ciąg znaków | `rain` | Pogoda Typ używany by `preset: "rain"`. Obsługiwane wartości: `rain`, `snow`. |
| `name` | ciąg znaków | none | Generic particle appearance. Obsługiwane wartości: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | ciąg znaków | none | Compatibility aliases dla `name`. |
| `x`, `z` | liczba zmiennoprzecinkowa lub array | scena granice | Particle origin lub Pogoda coverage. Generic particles używać scalar współrzędne. dla `preset: "rain"`, scalar wartości cel jeden precipitation kolumna i arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | liczba zmiennoprzecinkowa | `0.0` | początkowy motion wektor. `motionX/Y/Z` są accepted aliases. |
| `time` / `lifetime` | liczba całkowita | preset-określony | Particle lifetime in ticks. dla `preset: "rain"` Ten is  total Pogoda duration, including fade-in i fade-out. |
| `size` | liczba zmiennoprzecinkowa | preset-określony | Generic particle half-rozmiar in blok units. |
| `amount` | liczba całkowita | preset-określony | Generic particle count. dla `explosion`, pominięty amount scales z `power`. dla `preset: "rain"`, Ten is  average per-tick Pogoda density. |
| `power` | liczba zmiennoprzecinkowa | `2.0` | Explosion strength dla  `explosion` preset. |

Pogoda preset Uwagi:

- `preset: "rain"` is  shared Pogoda preset wpis punkt. używać `weather: "rain"` dla rainfall lub
  `weather: "snow"` dla snowfall.
- Ten preset is timeline-owned Pogoda. It obsługuje replay, pause, seek, i fast-forward together
  z  rest of  Animacja Ponder timeline.
- dla zawsze-on scena Pogoda poza  Animacja Ponder timeline, używać  `<Weather>` tag wewnątrz
  `<GameScene>` zamiast tego.
- Pogoda presets ignore `y`;  vertical spawn range is derived z  bieżący scena granice.
- `x: 5, z: 8` targets jeden precipitation kolumna. Arrays używać endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` creates dwa covered rectangles.
- Jeśli jeden oś ma extra unmatched array wartości,  unmatched tail is ignored.
-  czas wykonania automatycznie shapes  effect z short początek transition, steady middle
  section, i koniec transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes bez splash particles.
-  Pogoda area is derived z  bieżący GameScene granice so  effect scales z 
  zaimportowany struktura zamiast tego of hard-coded box.
-  ten sam `x/z` kolumna nigdy stacks wiele Pogoda types at  ten sam czas. Earlier overlapping
  Pogoda declarations Zachowaj  shared kolumny; later ones tylko renderować on  remaining area.

---

## blok byt NBT Operations

używać `mergeTileNBT`, `modifyTileNBT`, i `removeTileNBT` gdy  blok stays in place but jego
blok byt dane zmiany. Operations są seek-safe: GuideNH restores  original blok NBT i
następnie replays wszystkie operations z keyframe 0 through  active keyframe.

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

| pole | używany by | opis |
|-------|---------|-------------|
| `x`, `y`, `z` | wszystkie | blok-byt blok pozycja in struktura współrzędne. |
| `nbt` | `mergeTileNBT` | SNBT compound merged do  blok byt. Existing compound klucze są merged recursively; other wartości zastąpić  old wartość. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT ścieżka z lista indexes, e.g. `Items[0].Count` lub `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT wartość zapisany at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow  ten sam idea as Minecraft's `/data` paths: używać dots dla compound klucze i
`[index]` dla lista wpisy. lista traversal obecnie expects  traversed lista wpisy do be
compounds, który pasuje wspólny blok NBT takie jak inventories, tanks, i recipe slots.

---

## byt Actions

Regular `<Entity>` tagi są już Obsługiwane in `GameScene`. Animacja Ponder timelines może także utworzyć
ich własny entities z `createEntities`, następnie cel those entities by `ref` in later keyframes.

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

| pole | opis |
|-------|-------------|
| `ref` | Wymagane lokalny reference nazwa dla later operations. |
| `sceneEntityId` | opcjonalny stable scena-lokalny id używany dla mount relations, replay-safe zamiana, import/export restore, i any later removal of Ten logical byt. domyślne do wewnętrzny id derived z `ref`. |
| `id` | byt ID, e.g. `minecraft:pig`, `Pig`, lub mod byt ID Obsługiwane by  scena byt loader. |
| `x`, `y`, `z` | opcjonalny spawn pozycja. domyślne do `0, 0, 0` unless `nbt` supplies `Pos`. |
| `yaw`, `pitch` | opcjonalny spawn obrót. domyślne do `0, 0` unless `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | opcjonalny living-byt body/head yaw overrides. Jeśli pominięty podczas `yaw` is present, jeden follow `yaw`. |
| `nbt` | opcjonalny SNBT compound applied gdy  byt is utworzony. |
| `name`, `uuid` | opcjonalny podgląd-gracz profile pola gdy creating podgląd gracz byt. |
| `mount` | opcjonalny stable `sceneEntityId` of  vehicle który Ten byt powinien ride po creation lub later replay. |
| `unmount` | opcjonalny wartość logiczna który clears  byt's bieżący stable mount relation przed any later `mount` is applied. |

po creation, używać  byt NBT operations:

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

`setEntityNBT` is także dostępny gdy you want do zastąpić  byt's NBT zamiast tego of merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

byt actions może także aktualizować transform, podgląd-gracz pose, i stable mount stan bez
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

do detach lub usuwać timeline byt, używać `unmount` lub `removeEntities`:

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

Like blok operations, byt operations są replayed z  beginning whenever  active
keyframe zmiany, so seeking backwards removes Animacja Ponder-utworzony entities i recreates  correct
stan dla  cel tick.

Uwagi:

- `ref` identifies który Animacja Ponder-owned byt  bieżący action powinien edit lub usuwać.
- `sceneEntityId` i `mount` identify stable cross-byt relations. `mount` zawsze punkty do a
  stable scena id, nie do another `ref`.
- Relying on raw passenger NBT alone is nie recommended dla cross-byt scena relationships. 
  stable registry is what zachowuje mount i removal zachowanie deterministic across replay, rebuild,
  import/export, i edytor podgląd refresh.

---

## adnotacja Fade

Adnotacje smoothly fade in over **5 game ticks** (250 ms) whenever  active keyframe
zmiany during playback. Seeking lub pausing zawsze shows Adnotacje at full opacity.

---

## adnotacja Types

każdy wpis in  `annotations` array requires a `type` pole. Seven types są dostępny.

---

### `diamond`

Renders 3D diamond marker at a świat pozycja.

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

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `x`, `y`, `z` | liczba zmiennoprzecinkowa | `0.0` | przestrzeń świata pozycja of  diamond tip. |
| `color` | ciąg znaków | `"0xFF00E000"` | ARGB kolor as `"0xAARRGGBB"`. |
| `tooltip` | ciąg znaków | `""` | awaryjny tekst wyświetlany on najechanie. |
| `tooltipKey` | ciąg znaków | `""` | klucz tłumaczenia dla  najechanie tekst. gdy rozwiązany, it overrides `tooltip`. |
| `alwaysOnTop` | wartość logiczna | `false` | Jeśli true, wyrenderowany through solid bloki. |

---

### `box`

Renders wireframe wyrównany do osi box z `min` do `max`.

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

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `minX/Y/Z` | liczba zmiennoprzecinkowa | `0.0` | Minimum corner. |
| `maxX/Y/Z` | liczba zmiennoprzecinkowa | `1.0` | Maximum corner. |
| `color` | ciąg znaków | `"0xFFFFFFFF"` | ARGB wiersz kolor. |
| `lineWidth` | liczba zmiennoprzecinkowa | Domyślne | GL wiersz szerokość. |
| `alwaysOnTop` | wartość logiczna | `false` | renderować through bloki. |

---

### `block`

Renders wireframe around jeden whole blok. Ten is  Animacja Ponder JSON equivalent of
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

`"type": "blockBox"` i `"type": "block_box"` są accepted aliases.

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `pos` | liczba[3] lub ciąg znaków | `[0, 0, 0]` | blok współrzędna as `[x, y, z]` lub `"x y z"`. |
| `x`, `y`, `z` | liczba | `0` | Alternative blok współrzędna pola. wartości są floored. |
| `blockX/Y/Z` | liczba całkowita | `0` | Legacy-style blok współrzędna pola. |
| `color` | ciąg znaków | `"0xFFFFFFFF"` | ARGB wiersz kolor. |
| `lineWidth` | liczba zmiennoprzecinkowa | Domyślne | GL wiersz szerokość. |
| `alwaysOnTop` | wartość logiczna | `false` | renderować through bloki. |

---

### `line`

Renders odcinek lub polilinia between świat positions. `points` takes priority over
`fromX/Y/Z` i `toX/Y/Z` gdy it zawiera at least dwa prawidłowy punkty.

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

polilinia punkty może be zapisany either as ciąg znaków lub as array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `fromX/Y/Z` | liczba zmiennoprzecinkowa | `0.0` | początek punkt. |
| `toX/Y/Z` | liczba zmiennoprzecinkowa | `1.0` | koniec punkt. |
| `points` | ciąg znaków lub array | `null` | polilinia punkty. używać `"x y z; x y z; ..."` lub `[[x,y,z], ...]`. |
| `color` | ciąg znaków | `"0xFFFFFFFF"` | ARGB wiersz kolor. |
| `arrow` | ciąg znaków | `null` | `start` lub `end`; pominięty lub nieprawidłowy wartości rysować no strzałka. |
| `lineWidth` | liczba zmiennoprzecinkowa | Domyślne | GL wiersz szerokość. |
| `alwaysOnTop` | wartość logiczna | `false` | renderować through bloki. |

---

### `blockface`

Wyróżnia wszystkie ściany pojedynczego bloku półprzezroczystą pełną nakładką.

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` i `"type": "block_face"` są accepted aliases.

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `pos` | liczba[3] lub ciąg znaków | `[0, 0, 0]` | blok współrzędna as `[x, y, z]` lub `"x y z"`. |
| `x`, `y`, `z` | liczba | `0` | Alternative blok współrzędna pola. wartości są floored. |
| `blockX/Y/Z` | liczba całkowita | `0` | Legacy-style blok współrzędna pola. |
| `color` | ciąg znaków | `"0x80FFFFFF"` | ARGB overlay kolor. |
| `alwaysOnTop` | wartość logiczna | `false` | renderować through bloki. |

---

### `text`

Renders speech-dymek etykieta anchored do a świat pozycja.  box appears nad  kotwica by
Domyślne i is connected do it z short vertical wiersz. tekst treść is keyframe-driven: używać
inny `text` adnotacja wpisy on inny keyframes do zmiana  displayed tekst over czas.

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

dla stały przestrzeń ekranu pozycja który nie project z świat współrzędne, używać
**independent tryb**.  dymek is centered horizontally in  scena i umieszczony at
`yOffset` piksele below  scena's vertical centre.

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

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `x`, `y`, `z` | liczba zmiennoprzecinkowa | `0.0` | przestrzeń świata kotwica pozycja (ignored in independent tryb). |
| `text` | ciąg znaków | - | **Wymagane.** tekst do wyświetlać wewnątrz  dymek. |
| `color` | ciąg znaków | `"0xFFAAAAAA"` | ARGB kolor of  dymek obramowanie. |
| `backgroundAlpha` | liczba całkowita | `204` | tło opacity z `0` (przezroczysty) do `255` (opaque).  RGB kolor remains  Domyślne dark navy. |
| `maxWidth` | liczba całkowita | `0` | Jeśli &gt; 0, wraps tekst at Ten szerokość in piksele. Omit lub Ustaw do `0` dla pojedynczy-wiersz etykieta. |
| `independent` | wartość logiczna | `false` | Jeśli `true`, pozycja is względny do  scena centre rather niż a świat punkt. |
| `yOffset` | liczba całkowita | `0` | piksel offset z  scena's vertical centre (positive = downward). używany z `independent: true`. |
| `connectorSide` | ciąg znaków | `"bottom"` | `bottom`, `top`, `left`, `right`, lub `none`. Ignored in independent tryb. |
| `connectorOffset` | liczba całkowita | `0` | piksel offset wzdłuż  wybrany dymek krawędź; positive moves prawo dla góra/dół i down dla lewo/prawo. |
| `connectorLength` | liczba całkowita | `6` | piksel length of  łącznik wiersz. `0` hides  wiersz podczas keeping strona-based placement. |
| `hlMinX/Y/Z` | liczba zmiennoprzecinkowa | `0.0` | Minimum corner of opcjonalny wyróżniać box drawn alongside  tekst dymek. |
| `hlMaxX/Y/Z` | liczba zmiennoprzecinkowa | `1.0` | Maximum corner of  opcjonalny wyróżniać box. |
| `highlightColor` | ciąg znaków | `"0x8000FFAA"` | ARGB kolor of  wyróżniać box. |

gdy `hlMinX` (lub any `hlMin/Max` współrzędna) is present, an `InWorldBoxAnnotation` is także
utworzony at  specified granice z `highlightColor`. Ten is useful dla pointing at określony
blok regions podczas explaining ich.

 tło is dark navy dymek by Domyślne (`#CC0E0E20`), i `backgroundAlpha` controls jego
opacity. In świat-anchored tryb łącznik wiersz links  box do  kotwica. tekst obsługuje 
full GuideNH inline bogata-tekst składnia: Markdown formatting i MDX inline tagi. It is wyrenderowany z
drop-shadow.

> **bogata tekst:**  `text` pole obsługuje  ten sam inline markup używany in GuideNH przewodnik strony:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, i wszystkie other inline MDX tagi.
> Plain Minecraft `§` format codes są **nie** Obsługiwane; używać MDX składnia zamiast tego.

> A `text` adnotacja bez a `text` pole (lub empty ciąg znaków) is silently ignored.

---

### `input`

Renders mouse-wejście icon (lewo button, prawo button, lub przewijanie wheel) anchored do a świat
pozycja. Ten is używany do hint który  gracz powinien perform określony interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

z opcjonalny modifier klucz prefix i element icon:

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

| pole | Typ | Domyślne | opis |
|-------|------|---------|-------------|
| `x`, `y`, `z` | liczba zmiennoprzecinkowa | `0.0` | przestrzeń świata kotwica pozycja. |
| `inputType` | ciąg znaków | `"lmb"` | jeden of `"lmb"`, `"rmb"`, lub `"scroll"`. Case-insensitive. |
| `modifier` | ciąg znaków | `null` | opcjonalny modifier klucz: `"sneak"` lub `"ctrl"`. Shows prefix tekst nad  icon. |
| `item` | ciąg znaków | `null` | opcjonalny element registry ID (e.g. `"minecraft:iron_ingot"`). Renders  element icon do  lewo of  mouse icon. obsługuje `"modid:item:meta"` format dla meta wartości. |

 icon is 16x16 sprite drawn z `ponder_widgets.png`.  box tło is semi-przezroczysty
dark (`#CC0E0E20`) z light-blue obramowanie (`#80AAAADD`). gdy an `item` is specified  box
expands do accommodate both  element icon i  mouse icon strona by strona.

---

## Format kolorów

Colors są ARGB hexadecimal strings. Both `"0xFFFFFF00"` (z `0x` prefix) i
`"FFFF00"` (bez prefix) są accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% przezroczysty
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (Domyślne diamond kolor)
- `"0x8022CCFF"` - semi-przezroczysty blue
- `"0xFFAAAAAA"` - light grey (Domyślne tekst dymek obramowanie)

## Zachowanie odtwarzania

### Controls

| Control | Action |
|---------|--------|
| **Prev keyframe** | Jump do  początek of  poprzedni widoczny keyframe segment. ukryty keyframes są skipped. |
| **Play/Pause** | Toggle playback; restarts z  beginning Jeśli już finished. |
| **Restart** | zwracać do tick 0, reset stan, i begin playing. |
| Progress bar | kliknięcie lub przeciąganie do seek do any pozycja. Seeking zawsze pauses playback. |
| Keyframe nodes | Small tick marks on  bar dla widoczny keyframes; najechanie do Zobacz  etykieta i direction strzałka. |

### Stan początkowy

gdy strona containing `<ImportPonder>` is pierwszy opened,  scena starts **paused at tick 0**.
Press Play do begin.

### kamera lock

podczas playback is **active** (nie paused):
-  kamera follows  interpolated ścieżka defined by keyframes.
- Mouse przeciąganie i przewijanie powiększenie są **wyłączony**.
-  layer slider i StructureLib sliders są **ukryty**.

podczas playback is **paused** lub **finished**:
- Full interaktywny kamera przeciąganie, powiększenie, i layer/StructureLib control są restored.

### Keyframe node labels

gdy you najechanie over keyframe node on  progress bar:
-  node grows slightly do indicate it is hovered.
- Jeśli  keyframe ma a `label`, it is displayed beside  node.
- ukryty keyframes do nie utworzyć hoverable nodes, but jeden nadal apply ich timeline stan gdy playback lub seeking reaches ich.

### Layer control during playback

 `layer` pole of  active keyframe overrides  widoczny-layer filtr during playback:
- `null` (lub pominięty) -> pokazywać wszystkie layers.
- `1`, `2`, `3`, ... -> restrict do który 1-based layer index.

## Pełny przykład

 following Przykład demonstrates każdy adnotacja Typ across four-keyframe scena.

### Układ katalogów

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

standardowy NBT struktura plik (utworzony z  `/structure save` command lub tool takie jak
Litematica). Zobacz [Getting Started](Guide-Page-Format) dla  full SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

Młynek zamienia rudy w podwójną ilość pyłu. Naciśnij **Odtwórz**, aby zobaczyć animowany przewodnik.
```

## Uwagi

-  Animacja Ponder progress bar is drawn nad  layer/StructureLib sliders. During playback 
  structural sliders są ukryty do Zachowaj  UI clean; jeden reappear gdy paused.
- kamera interpolation is zawsze smooth (ease-in/out) even Jeśli some keyframes tylko zmiana a
  subset of kamera osie. używać `cameraEaseTicks` on keyframe do snap  kamera instantly (`0`)
  lub ease over stały liczba of ticks przed holding  cel pozycja.
- Adnotacje belong do pojedynczy keyframe - jeden appear tylko podczas który keyframe is active
  (i.e., z jego `time` tick until  następny keyframe's `time` tick). Overlay tekst Adnotacje
  fade out smoothly gdy  keyframe zmiany during playback.
- Animacja Ponder `line` Adnotacje używać  ten sam czas wykonania renderer as regular `LineAnnotation`, including
  polilinia bends i początek/koniec arrows. punkt marker cubes remain MDX-tylko feature.
- Animacja Ponder `text` Adnotacje używać  ten sam czas wykonania renderer as regular `TextAnnotation`, including
  łącznik strona, offset, i length. Dynamic tekst is keyframe-based rather niż interpolated per tick.
- tylko jeden `<ImportPonder>` tag is effective per `<GameScene>`. second tag overwrites  pierwszy.
- A `text` adnotacja z empty lub absent `text` pole is silently skipped.
-  `inputType` pole domyślne do `"lmb"` Jeśli pominięty lub unrecognised.
- `blockChanges` są applied in kolejność z  pierwszy do  bieżący keyframe każdy czas 
  active keyframe zmiany, so changing  ten sam pozycja in wiele keyframes works correctly.
- blok/byt NBT operations używać  ten sam replay model as `blockChanges`; jeden są safe do seek
  forwards lub backwards.
- `text` Adnotacje z a `maxWidth` &gt; 0 są word-wrapped używając  vanilla font renderer;
   dymek box wysokość adjusts automatycznie dla multi-wiersz tekst.
- `nbt` strings in `blockChanges` musi używać **unquoted** SNBT klucze (standardowy MC 1.7.10 format).
  Quoted klucze będzie be rejected by  parser. ciąg znaków wartości nadal require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` i `modifyEntityNBT` wartości są SNBT wartości, nie JSON wartości. dla ciąg znaków
  wartość, escape  SNBT quotes wewnątrz JSON: `"value": "\"hello\""`.
