# GameScene

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


`<GameScene>` is GuideNH's 3D перегляд тег. `<Scene>` is alias з  той самий поведінка.

## Атрибути сцени

| Атрибут | Тип | Типове | Значення |
| --- | --- | --- | --- |
| `width` | ціле число | `256` | viewport ширина in пікселі |
| `height` | ціле число | `192` | viewport висота in пікселі |
| `zoom` | число з плаваючою комою | `1.0` | камера масштаб multiplier |
| `perspective` | рядок | `isometric-north-east` | камера preset |
| `rotateX` | число з плаваючою комою | auto | явний X обертання перевизначати |
| `rotateY` | число з плаваючою комою | auto | явний Y обертання перевизначати |
| `rotateZ` | число з плаваючою комою | auto | явний Z обертання перевизначати |
| `offsetX` | число з плаваючою комою | auto | простір екрана horizontal pan |
| `offsetY` | число з плаваючою комою | auto | простір екрана vertical pan |
| `centerX` | число з плаваючою комою | auto | явний світ обертання центр X |
| `centerY` | число з плаваючою комою | auto | явний світ обертання центр Y |
| `centerZ` | число з плаваючою комою | auto | явний світ обертання центр Z |
| `interactive` | логічне значення вираз | `true` | enables mouse interaction |
| `showBackground` | логічне значення вираз | `true` | shows  сцена тло fill і межа |
| `allowLayerSlider` | логічне значення | `true` | shows  vertical layer slider |
| `gridButtonEnabled` | логічне значення | `true` | shows  floor grid toggle button |
| `showGrid` | логічне значення | `false` | початковий видимість of  floor grid |

## Накладка статистики блоків

сцени який містити блоки enable  блок-stat toggle button by Типове. Додайте a `<BlockStats>`
дочірній елемент коли you want до перевизначати його режим, placement, filters, видимість, або розмір.  список is
cached і лише rebuilt коли  сцена блоки, Анімація Ponder timeline стан, StructureLib вибір, або
блок-stat settings змінити; normal відтворення reuses  prepared рядки. Long lists є clipped до
`maxWidth` і `maxHeight`; Якщо those є пропущений, кожен is  larger of  фіксований `224` by `96` пікселі і
40% of  сцена розмір.
Overflow receives draggable scrollbars, і  mouse wheel scrolls  список поки  cursor is
over  overlay. Hold Shift до wheel-прокручування horizontally.

In автоматичний режим, GuideNH scans  сцена's filled блоки і resolves кожен блок до  предмет
стос users normally Дивіться. блоки який містити кілька видимий components може contribute кілька
items з  той самий координата; Цей includes AE2 cable bus parts і facades, ForgeMultipart part
drops, і Carpenters' блоки covers або overlays коли those mods є installed. Counts є grouped
by `item:meta` і sorted by count.

автоматичний lists може також be docked поза  сцена з `dock="left"`, `dock="top"`,
`dock="right"`, або `dock="bottom"`. Docked lists перенесення до extra стовпці або рядки based on 
attached бік length, reserve структура простір, і avoid  сцена button стовпець on  праворуч. натискання an
предмет in автоматичний список до виділяти усі відповідний сцена placements з їхні розв’язаний collision
boxes використовуючи завжди-on-угорі face overlay; натискання  той самий предмет again до clear  виділяти. Counts
є відтворений through  ItemStack стос-розмір overlay. Установіть `showNames={true}` до append  count
після кожен назва as well, і наведення an предмет до Дивіться  exact блок count in  підказка.

Filters може приховувати звичайний блоки або показувати лише вибраний блоки:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

використовувати manual режим коли a посібник wants до показувати planned material список замість цього of  literal сцена
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

## Накладки режиму налагодження

коли  `enableDebugMode` параметр is увімкнено in  GuideNH mod config,  following extra
overlays become доступний in  3D сцена перегляд.

### Grid координата Labels

коли debug режим is **on** і  floor grid is **видимий**, координата labels є відтворений
below кожен grid рядок:

- **X-вісь numbers** є показано уздовж  near край of  grid (north/−Z край in  Типове
  `isometric-north-east` камера).  кожен ціле число X світ-координата receives мітка.
- **Z-вісь numbers** є показано уздовж  near край of  grid (east/+X край).  кожен ціле число
  Z світ-координата receives мітка.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) є drawn at  midpoint of кожен
  respective grid край.

координати follow  actual світ X/Z значення stored in  сцена level, so Вони може be
negative коли  структура містить блоки з negative координати.

 grid toggle button is **завжди увімкнено** поки debug режим is active, regardless of 
`gridButtonEnabled` Атрибут, so you може показувати або приховувати  grid і його labels at any час.
 Типове grid видимість (`showGrid`) is не affected.

### блок координата підказка

коли debug режим is **on** і  cursor hovers over a блок усередині  сцена, second
підказка is відтворений над  primary блок підказка, showing  простір світу блок позиція
as `X, Y, Z` in gold текст.

Якщо  координата підказка would be clipped at  угорі of  екран it автоматично snaps
below  cursor area замість цього (magnetic snapping).

## Профілі перспективи

Accepted `perspective` значення:

- `isometric-north-east`
- `isometric-north-west`
- `up`

невідомий значення fall назад до `isometric-north-east`.

## Вбудовування вмісту та перенесення тексту

Any блок-level тег — including `<GameScene>` — підтримує два необов’язковий атрибути який control
how it is embedded in  сторінка, mirroring Microsoft Word's "текст Wrapping" параметри.

| Атрибут | значення | Типове | Значення |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | текст-wrapping режим |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Режими перенесення

| режим | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | In рядок з текст | Типове flow: сцена occupies його власний vertical slot (嵌入型) |
| `square` | Square | сцена floats ліворуч або праворуч; surrounding текст wraps in rectangle around it (方形环绕) |
| `tight` | Tight | Tighter перенесення; equivalent до `square` in Цей структура system (紧密型) |
| `through` | Through | Through-перенесення; equivalent до `square` in Цей структура system (穿越型) |
| `top-bottom` | угорі і унизу | текст лише над і below, не beside; respects `align` для horizontal placement (上下型) |
| `behind` | Behind текст | блок renders behind surrounding текст; respects `align` (衬于文字下方) |
| `front` | In спереду of текст | блок renders in спереду of surrounding текст; respects `align` (浮于文字上方) |

### Приклади

ліворуч-floating сцена — текст in  наступний paragraph wraps до  праворуч:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Текст, що обтікає праворуч від сцени…
````

праворуч-floating сцена:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

Текст, що обтікає ліворуч від сцени…
````

Centred сцена (no текст wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline in текст (flow context) — текст wraps around small сцена:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

## Приклад

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Import Приклади

Ці Приклади focus on  сцена-бік поведінка який most часто trips people up коли importing
структури.

StructureLib import з явний facing, обертання, flip, і offsets:

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

GregTech controllers залишаються unformed by Типове, even коли  імпортований multiblock is otherwise
дійсний:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

Установіть `formed={true}` лише коли  перегляд слід intentionally показувати  formed controller стан:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

 той самий Типове також застосовує до controllers розміщений directly з `<Block>`, including GregTech
controllers який rely on surrounding multiblock casings:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

простий блок-лише layouts може досі be authored directly і remain compatible з multiblock
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

## Дочірні елементи сцени

GuideNH зараз registers Ці сцена дочірній елемент теги:

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
- анотація теги наприклад `<BoxAnnotation>` і `<LineAnnotation>`

## Звуки сцени

`<PlaySound>` може be розміщений усередині `<GameScene>` до play sounds з сцена interaction або timeline
запис. Підтримувані triggers є:

- `click`,  Типове
- `hover`, fired once коли  cursor enters  сцена
- `enter`, fired once коли  сцена перший renders

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

коли `x`, `y`, і `z` є provided,  sound volume is attenuated in екран простір з 
projected сцена координата до  натискання точка або сцена центр. `radius` типові до 75% of 
shorter сцена бік, і `minVolume` типові до `0.15`.

## `<BlockStats>` і `<BlockStat>`

Declares або customizes a Накладка статистики блоків. сцени з блоки enable  автоматичний toggle
button even коли Цей дочірній елемент is пропущений. Adding один або more `<BlockStat>` дочірні елементи switches 
overlay до manual statistics режим для який сцена.

`<BlockStats>` атрибути:

| Атрибут | Обов’язково | Типове | Значення |
| --- | --- | --- | --- |
| `visible` | no | config, Типове `false` | початковий overlay видимість |
| `buttonEnabled` | no | config, Типове `true` | shows  блок statistics toggle button |
| `mode` | no | `auto` | `auto` або `manual`; дочірній елемент `<BlockStat>` записи force manual режим |
| `corner` | no | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, або `bottomLeft` |
| `dock` | no | `inside` | автоматичний lists може attach до `inside`, `left`, `top`, `right`, або `bottom`; manual режим завжди використовує  усередині overlay |
| `showNames` | no | `false` | whether до показувати предмет names beside icons; коли увімкнено  count is також appended після  назва |
| `filterMode` | no | `blacklist` | `blacklist` або `whitelist` |
| `filter` | no | empty | предмет ключі наприклад `minecraft:stone` або `minecraft:stone:0`, separated by spaces, commas, або semicolons |
| `maxWidth` | no |  larger of `224` px і 40% of  сцена ширина | maximum overlay ширина in пікселі до horizontal прокручування |
| `maxHeight` | no |  larger of `96` px і 40% of  сцена висота | maximum overlay висота in пікселі до vertical прокручування |

`<BlockStat>` атрибути:

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `item` | yes, unless `id` is використовується | предмет id показано in  список |
| `id` | yes, unless `item` is використовується | existing предмет-стос Атрибут form |
| `count` | no | displayed count; omitting it shows  рядок once, і `count="0"` hides  рядок |

Приклад:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

Places a блок до  перегляд світ.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `id` | yes, unless `ore` is використовується | блок id |
| `ore` | no | ore dictionary назва;  перший відповідний стос має розв’язати до a блок предмет |
| `x` | no | ціле число світ X, Типове `0` |
| `y` | no | ціле число світ Y, Типове `0` |
| `z` | no | ціле число світ Z, Типове `0` |
| `meta` | no | ціле число блок метадані |
| `facing` | no | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | no | SNBT TileEntity compound |
| `formed` | no | whether  розміщений структура controller слід be treated as formed during перегляд sync; Типове `false` |

Примітки:

- `ore` takes precedence over `id`; Якщо GregTech is installed,  вибраний стос is unified through `GTOreDictUnificator.setStack(...)`
- Якщо `meta` is пропущений і an `ore` відповідати carries concrete non-wildcard предмет damage, який damage is використовується до  `facing` резервний варіант
- Якщо `meta` is пропущений, some блоки derive sensible Типове з `facing`
- Якщо `nbt` creates TileEntity successfully,  перегляд використовує it
- Установіть `formed={false}` коли controller-based структура слід залишаються unformed in перегляд even though  surrounding структура is otherwise дійсний

Приклад:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

завантажує зовнішній структура файл до  сцена.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `src` | yes | структура ресурс шлях |
| `x` | no | ціле число переклад X (alias для `offsetX`) |
| `y` | no | ціле число переклад Y (alias для `offsetY`) |
| `z` | no | ціле число переклад Z (alias для `offsetZ`) |
| `offsetX` | no | ціле число переклад X (бажаний over `x`) |
| `offsetY` | no | ціле число переклад Y, clamped до `[0, worldHeight-1]` (бажаний over `y`) |
| `offsetZ` | no | ціле число переклад Z (бажаний over `z`) |
| `formed` | no | whether імпортований структура controllers слід be treated as formed during перегляд sync; Типове `false` |

Підтримувані formats:

- SNBT текст
- gzipped binary NBT
- uncompressed binary NBT

Обов’язково структура ключі:

- `palette`
- `blocks`

Приклад:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

імпорти StructureLib multiblock перегляд by controller id.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `controller` | yes | controller блок id, використовуючи `modid:block[:meta]` |
| `name` | no | необов’язковий binding назва використовується by `showWhenStructure` on Анотації, templates, і sounds |
| `piece` | no | StructureLib piece назва перевизначати |
| `facing` | no | facing перевизначати passed до  importer |
| `rotation` | no | обертання перевизначати passed до  importer |
| `flip` | no | flip/mirror перевизначати passed до  importer |
| `channel` | no | ціле число channel перевизначати для channel-aware структури |
| `offsetX` | no | ціле число X offset applied до усі розміщений блоки (Типове `0`) |
| `offsetY` | no | ціле число Y offset applied до усі розміщений блоки, clamped до `[0, worldHeight-1]` (Типове `0`) |
| `offsetZ` | no | ціле число Z offset applied до усі розміщений блоки (Типове `0`) |
| `formed` | no | whether імпортований StructureLib controllers слід be treated as formed during перегляд sync; Типове `false` |

Примітки:

-  імпортований структура starts з сцена `0 0 0`;  controller is не forced до be розміщений at `0 0 0`
- Цей тег enables StructureLib-певний підказка, hatch виділяти, і channel slider UI коли метадані is доступний
- controller відповідний підтримує  GTNH-style `modid:block:meta` form
- використовувати `name` коли  сцена містить кілька StructureLib імпорти і another тег needs до ціль один певний структура стан
- `facing`, `rotation`, і `flip` використовувати  той самий орієнтація vocabulary as StructureLib export; коли requested combination is не allowed by  controller, GuideNH falls назад до  перший дійсний alignment автоматично
- GregTech controller previews now Типове до  controller's opposite horizontal facing з  older перегляд орієнтація, rotating  перегляд спереду by 180 degrees around  Y вісь
- Установіть `formed={false}` коли  імпортований controller слід remain visibly unformed; Цей is  Підтримувані alternative до shipping intentionally broken NBT

Приклад:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

структура-aware анотація і sound Приклад:

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

StructureLib типові може також be supplied as дочірній елемент теги. Ці типові є part of  сцена's
початковий інтерактивний стан, so  reset-перегляд button restores їх після  user зміни tier або
channel sliders.

| дочірній елемент тег | Значення |
| --- | --- |
| `<Tier value="1" />` | Master tier значення. |
| `<Channel name="channelName" value="1" />` | Named StructureLib channel перевизначати. Repeat для кілька канали. |
| `<Facing value="north" />` | Типове facing. |
| `<Rotation value="normal" />` | Типове обертання. |
| `<Flip value="none" />` | Типове flip/mirror. |
| `<Orientation value="north:normal:none" />` | Facing, обертання, і flip in один тег. |
| `<GregTechActiveController />` | GregTech лише: відтворювати  controller з його active texture коли possible. |
| `<GregTechPlaceHatches />` | GregTech лише: place normal GT hatches для hatch-лише перегляд positions. без Цей, GT previews досі використовувати survival construct для hatch-aware machines, but empty hatch positions fall назад до casing блоки. |

для GregTech controllers, GuideNH now використовує  той самий StructureLib survival-перегляд шлях as 
export command. Цей fixes hatch-лише positions який normal `construct()` не може populate поки
keeping резервний варіант casings by Типове.

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

застосовує явний isometric камера yaw/pitch/roll.

Якщо Цей тег is пропущений,  сцена зберігає використовуючи  `<GameScene>` `perspective` preset.  Типове
`isometric-north-east` preset is equivalent до:

````md
<IsometricCamera yaw="225" pitch="30" />
````

| Атрибут | Значення |
| --- | --- |
| `yaw` | число з плаваючою комою |
| `pitch` | число з плаваючою комою |
| `roll` | число з плаваючою комою |

Приклад:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes кожен вже-розміщений блок відповідний ціль блок id.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `id` | yes | блок id до видалити, використовуючи `modid:block[:meta]` |

Цей is useful після importing структура коли you want до приховувати певний блоки для clarity.

Приклад:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

Replaces вже-розміщений блоки який відповідати source блок id (і optionally partial блок сутність NBT
pattern) з новий блок.  пошук може be глобальний (усі filled блоки) або restricted до a
bounding box.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `from` | yes | source блок до відповідати, використовуючи `modid:block[:meta]` |
| `from_nbt` | no | partial SNBT compound; a блок відповідає лише коли його блок сутність NBT містить усі listed ключі |
| `to` | yes | заміна блок, використовуючи `modid:block[:meta]` |
| `to_nbt` | no | SNBT TileEntity compound до apply до  заміна |
| `x` | no | bounding box початок X; Якщо any of `x/y/z/dx/dy/dz` is present,  box режим is activated |
| `y` | no | bounding box початок Y |
| `z` | no | bounding box початок Z |
| `dx` | no | bounding box length on  X вісь (Типове `1`) |
| `dy` | no | bounding box висота on  Y вісь (Типове `1`) |
| `dz` | no | bounding box ширина/depth on  Z вісь (Типове `1`) |
| `formed` | no | whether заміна результат controllers слід be treated as formed during перегляд sync; Типове `false` |

Примітки:

- коли none of `x/y/z/dx/dy/dz` є provided, усі filled блоки є scanned globally
- `from_nbt` is a **partial** відповідати: лише  ключі listed in  pattern має відповідати; extra ключі in
   actual блок сутність є ignored
-  заміна is performed via  той самий блок placement pipeline as `<Block>`, so GregTech MetaTile
  і BartWorks блок entities є handled correctly
- Якщо  заміна places controller, `formed={false}` зберігає який controller unformed during перегляд

Приклад:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills an вирівняний за осями box з a один блок Тип, overwriting whatever was there до.
Unlike `<Block>` (який targets a один позиція), `<PlaceBlock>` підтримує multi-блок regions via
`dx`/`dy`/`dz`, ordered as length, висота, і ширина/depth on  X/Y/Z осі.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `id` | yes | блок id, використовуючи `modid:block[:meta]` |
| `nbt` | no | SNBT TileEntity compound applied до кожен розміщений блок |
| `x` | no | region початок X, Типове `0` |
| `y` | no | region початок Y, Типове `0` |
| `z` | no | region початок Z, Типове `0` |
| `dx` | no | region length on  X вісь, Типове `1` |
| `dy` | no | region висота on  Y вісь, Типове `1` |
| `dz` | no | region ширина/depth on  Z вісь, Типове `1` |
| `formed` | no | whether розміщений controllers слід be treated as formed during перегляд sync; Типове `false` |

Примітки:

- усі блоки in  box є unconditionally розміщений (no prior-блок check)
-  NBT compound is copied Для кожного individual placement
-  той самий блок placement pipeline as `<Block>` is використовується, so GregTech MetaTile і BartWorks блок entities
  є fully Підтримувані
- Якщо  region places один або more controllers, `formed={false}` зберігає кожен affected controller unformed

Приклад:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

Expands один або more дочірній елемент Анотації onto кожен відповідний блок який вже exists in  поточний сцена.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `id` | yes | блок matcher in `modid:block[:meta]` form |

Правила:

- place it після  блоки або імпортований структури який it слід відповідати
- відповідний happens against  поточний сцена стан at аналізувати час
- дочірній елемент Анотації використовувати локальний координати відносний до кожен відповідний блок

Приклад:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

додає сутність до  перегляд сцена.

 атрибути follow summon-style сутність placement і SNBT дані.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `id` | yes | сутність Тип id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, і зареєстрований mod сутність ids in either `modid.entityName` або `modid:entityName` form є accepted |
| `x` | no | число з плаваючою комою X координата  сутність is centered on, Типове `0.5` |
| `y` | no | число з плаваючою комою Y координата at  унизу of  сутність, Типове `0` |
| `z` | no | число з плаваючою комою Z координата  сутність is centered on, Типове `0.5` |
| `rotationY` | no | yaw in degrees, Типове `-45` |
| `rotationX` | no | pitch in degrees, Типове `0` |
| `data` | no | summon-style SNBT merged до  сутність NBT до spawn |
| `sceneEntityId` | no | stable сцена-локальний сутність id використовується by later `<Entity>` / `<RemoveEntity>` operations і by імпортований сцена snapshots |
| `mount` | no | stable `sceneEntityId` of  vehicle який Цей сутність слід ride після spawn |
| `unmount` | no | логічне значення вираз який clears Цей сутність's поточний stable mount relation після spawn або стан replay |
| `baby` | no | логічне значення вираз forcing Підтримувані entities до baby form; пропущений leaves  сутність's normal age/стан unchanged |
| `name` | no | перегляд гравець назва коли `id` is `player`, `fakeplayer`, `minecraft:player`, або `minecraft:fakeplayer` |
| `uuid` | no | перегляд гравець UUID коли використовуючи один of  гравець ids над |
| `showName` | no | логічне значення вираз controlling  перегляд гравець nameplate, Типове `true` для гравець перегляд ids |
| `showCape` | no | логічне значення вираз controlling  перегляд гравець cape, Типове `true` для гравець перегляд ids |
| `headRotation` | no | перегляд гравець head обертання as `x y z` degrees |
| `leftArmRotation` | no | перегляд гравець ліворуч arm обертання as `x y z` degrees |
| `rightArmRotation` | no | перегляд гравець праворуч arm обертання as `x y z` degrees |
| `leftLegRotation` | no | перегляд гравець ліворуч leg обертання as `x y z` degrees |
| `rightLegRotation` | no | перегляд гравець праворуч leg обертання as `x y z` degrees |
| `capeRotation` | no | перегляд гравець cape обертання as `x y z` degrees; типові до  standing-досі angle `6 0 0` |

Примітки:

- сутність межі participate in сцена auto-centering і видимий-layer filtering
- сутність creation falls назад gracefully коли  перегляд світ is не ready yet, потім binds on перший відтворювати
- `sceneEntityId` is необов’язковий, but strongly recommended whenever later сцена mutations need до find, видалити, remount, або restore  той самий logical сутність без scanning by raw виконання id
- один `sceneEntityId` може власний more ніж один виконання сутність instance; `<RemoveEntity sceneEntityId="..."/>` removes кожен сутність зараз зареєстрований до який stable id
- `mount` links entities by stable сцена id rather ніж by raw NBT passenger lists, so replay, import/export, перегляд rebuild, і Анімація Ponder seeking може усі restore  той самий rider/vehicle relation deterministically
- `unmount={true}` clears  stable mount relation для який сутність до any later mount is applied
- `baby={true}` зараз підтримує перегляд players, ageable mobs, vanilla zombies, і modded entities який expose stable `setChild(boolean)` або `setBaby(boolean)` style APIs
- дочірній елемент-стан entities є re-aligned до їхні поточний позиція після resizing so наведення і pick межі залишаються centered on  відтворений model
- гравець перегляд ids створити client-бік fake remote гравець so  normal гравець renderer і skin pipeline може be використовується
- коли both `name` і `uuid` є пропущений для гравець перегляд, GuideNH falls назад до `Steve` і  vanilla Типове skin
- коли лише `name` is given для гравець перегляд, GuideNH перший tries до розв’язати  real online profile so skins і capes може завантажити; Якщо lookup fails, it falls назад до stable offline UUID
- коли лише `uuid` is given для гравець перегляд, GuideNH generates placeholder відображати назва і досі tries до розв’язати  skin з  profile
- `showName={false}` hides  перегляд гравець's overhead назва без bypassing  normal гравець renderer
- `showCape={false}` hides  перегляд гравець's cape поки досі respecting  normal гравець відтворювати шлях і Forge hooks
- гравець pose атрибути використовувати three простір-separated floats mapped до model `X Y Z` обертання in degrees
- пропущений head і limb обертання атрибути Збережіть  normal vanilla idle pose; пропущений `capeRotation` falls назад до  standing-досі cape angle `6 0 0`
- гравець previews require active client світ at аналізувати час because Minecraft's гравець сутність constructor не може be створений worldless
- наведення сутність shows його локалізований відображати назва, або його власний назва Якщо один was provided

Приклад:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Stable-id mount Приклад:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

Removal і unmount Приклад:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
</GameScene>
````

Baby сутність Приклад:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

перегляд гравець pose Приклад:

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

перегляд гравець назва і cape Приклад:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

Removes кожен виконання сутність зараз зареєстрований до один stable `sceneEntityId`.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `sceneEntityId` | yes | stable сцена-локальний сутність id до видалити |
| `unmount` | no | логічне значення вираз який clears  stable mount relation до removal |

Примітки:

- Цей is  сцена-бік counterpart до Анімація Ponder's `removeEntities`
- removal works on  indexed stable-id registry, so it не need до scan усі entities кожен frame
- Якщо кілька імпортований або replayed entities share  той самий `sceneEntityId`, Вони є видалений together

Приклад:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## Погода

`<Weather>` додає animated rain або snow directly до a `GameScene`. Unlike Анімація Ponder Погода presets,
сцена Погода is не timeline-owned: it зберігає looping during normal сцена відтворення, it не
fade in або fade out, і it не може be paused або scrubbed independently.  renderer досі використовує 
той самий precipitation геометрією шлях as Анімація Ponder Погода, so локальний перегляд і site export залишаються aligned.

| Атрибут | Типове | опис |
| --- | --- | --- |
| `weather` / `type` | `rain` | Погода тип. Підтримувані значення: `rain`, `snow`. |
| `x`, `z` | сцена межі | Covered precipitation стовпці. scalar targets один стовпець. Arrays використовувати endpoint pairs до define один або more rectangles. |
| `density` | Тип-певний | Coverage density. Higher значення Збережіть more precipitation стовпці active; lower значення sparsify  effect. |

Примітки:

- `<Weather>` ignores `y`;  vertical span is derived з  поточний сцена межі і з 
  highest precipitation-blocking блок in кожен covered стовпець.
- Якщо один вісь має unmatched extra array значення,  unmatched tail is ignored.
- усередині один Погода declaration, rain і snow ніколи стос on  той самий `x/z` стовпець. Якщо кілька
  Погода теги overlap, earlier теги Збережіть  shared стовпці.
- інший non-overlapping стовпці in  той самий `GameScene` може відтворювати rain і snow at  той самий
  час.

Приклад:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## камера центр поведінка

Якщо no явний `centerX/Y/Z` is given, GuideNH auto-centers  сцена з  розміщений блок межі. Якщо any явний центр координата is Установіть, auto-centering is вимкнено і missing координати Типове до `0`.

## Примітки щодо взаємодії

коли `interactive={true}`  сцена підтримує обертання, pan, масштаб, reset, анотація toggles, і other UI controls exposed by  посібник екран.

- сцени spanning кілька Y levels показувати a видимий-layer slider над  унизу край
- StructureLib сцени може Додайте hatch-виділяти toggle button plus channel slider at  very унизу коли  імпортований метадані надає їх
- анотація наведення takes priority over блок наведення; блок tooltips appear normally once no анотація hotspot is being hovered
- StructureLib наведення зберігає  блок назва on  перший підказка рядок, додає структура-певний текст starting on  second рядок, і expands заміна candidates коли `Shift` is held

## Пов’язані сторінки

- [Annotations](Annotations)
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [Examples](Examples)
