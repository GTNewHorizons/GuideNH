# Зображення та ресурси

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH підтримує both normal Markdown зображення і several виконання-певний visual elements.

## ресурс Resolution Правила

посібник ресурси розв’язати з  той самий Правила використовується by сторінка links.

| шлях form | Приклад | Значення |
| --- | --- | --- |
| відносний | `test1.png` | відносний до  поточний сторінка файл |
| rooted | `/assets/example_structure.snbt` | відносний до  поточний посібник корінь |
| явний ресурс id | `guidenh:textures/gui/example.png` | абсолютний `modid:path` lookup |

## Markdown зображення

Normal Markdown зображення є Підтримувані:

````md
![Example](test1.png)
````

GuideNH resolves  шлях і завантажує  binary ресурс з  посібник вміст корінь.

## `FloatingImage`

За `wrap="inline"` зображення вирівнюється по вертикальному центру навколишнього тексту, а висота рядка збільшується, щоб умістити зображення повністю. Сусідні рядки тексту зберігають звичайну висоту; фіксованого обмеження висоти рядка немає. Якщо дуже високе зображення має займати окремий рядок, розмістіть тег в окремому абзаці з порожнім рядком перед ним і після нього.

`<FloatingImage>` renders cropped bitmap region який може число з плаваючою комою з текст або sit truly inline усередині a
paragraph. It також accepts явний `modid:path` texture ids, so it може reference texture ресурси з
other mods directly.

### атрибути

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `src` | yes | зображення шлях |
| `x` | під час обрізання | crop початок X in source-зображення пікселі |
| `y` | під час обрізання | crop початок Y in source-зображення пікселі |
| `width` / `w` | під час обрізання | crop ширина in source-зображення пікселі; exactly один spelling має be використовується |
| `height` / `h` | під час обрізання | crop висота in source-зображення пікселі; exactly один spelling має be використовується |
| `scaleX` | no | horizontal відображати multiplier, Типове `1.0` |
| `scaleY` | no | vertical відображати multiplier, Типове `1.0` |
| `displayWidth` | no | кінцевий відображати ширина in пікселі; preserves  crop aspect ratio коли використовується alone |
| `displayHeight` | no | кінцевий відображати висота in пікселі; preserves  crop aspect ratio коли використовується alone |
| `wrap` | no | `inline` розміщує зображення в рядку; інакше `align` вибирає бік обтікання |
| `align` | no | `left` або `right` для floating placement; ignored коли `wrap="inline"` |
| `title` | no | підказка/заголовок текст |
| `alt` | no | Альтернативний текст |
| `sound` | no | sound event played by  whole зображення |
| `soundSrc` | no | sound шлях до файлу для  whole зображення |
| `trigger` | no | `click` by Типове, або `hover` для наведення playback |

### Примітки

- `x`, `y`, `width` / `w`, і `height` / `h` є усі Обов’язково together коли cropping
- коли  crop атрибути є усі пропущений, `displayWidth` або `displayHeight` displays  full source зображення
- `width` і `height` задають розмір обрізання джерела. Для розміру показу використовуйте `displayWidth` / `displayHeight` або масштаб.
- `scaleX` і `scaleY` обчислювати  кінцевий відображати розмір as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` або `displayHeight` sets  кінцевий відображати розмір in пікселі; коли лише один is present,  other dimension is обчислений з  crop aspect ratio
- providing both `displayWidth` і `displayHeight` дозволяє intentional non-proportional stretching
- `displayWidth` / `displayHeight` не може be combined з `scaleX` / `scaleY`
- один-вісь stretching is Підтримувані by setting лише один scale differently
- `width` з `w`, або `height` з `h`, is недійсний і renders a видимий помилка
- `src` може be відносний, rooted, або явний `modid:path` texture id наприклад `minecraft:textures/gui/options_background.png`

### Приклад

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

`<ImageAnnotation>` is a дочірній елемент element of `<FloatingImage>` який attaches розширений-текст підказка (і
an необов’язковий colored межа) до rectangular region of  зображення. координати є specified in
**cropped-зображення пікселі** і є автоматично proportionally scaled коли  cropped зображення is resized
або stretched.

### атрибути

| Атрибут | Обов’язково | Типове | Значення |
| --- | --- | --- | --- |
| `x` | no | — | ліворуч край of  region in зображення пікселі |
| `y` | no | — | угорі край of  region in зображення пікселі |
| `w` | no | — | ширина of  region in зображення пікселі |
| `h` | no | — | висота of  region in зображення пікселі |
| `border` | no | `false` | показувати colored межа around  region |
| `borderColor` | no | random | межа колір (`#RRGGBB` або `#AARRGGBB`) |
| `borderThickness` | no | `1` | межа thickness in відображати пікселі |
| `sound` | no | none | необов’язковий sound event played для Цей region |
| `src` | no | none | необов’язковий sound шлях до файлу; converted до sound event id |
| `trigger` | no | `click` | `click` або `hover` |

### Примітки

- omitting усі four of `x`, `y`, `w`, `h` makes  анотація cover  **whole зображення**
- Якщо any of  four is present,  remaining пропущений ones Типове до `0` (origin) або `1` (розмір)
- межа is **не показано by Типове**; Додайте `border` або `border={true}` до enable it
- коли `borderColor` is пропущений і `border` is увімкнено, random fully-opaque колір is використовується
- дочірній елемент MDX вміст is відтворений as  підказка body і може включати any inline/блок elements
- later Анотації (lower in  список) take наведення priority over earlier ones коли regions overlap

### Приклад

Whole-зображення анотація:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation>
    Наведіть курсор на зображення, щоб побачити цю підказку.
  </ImageAnnotation>
</FloatingImage>
````

Region анотація з a видимий межа:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="2" y="2" w="15" h="10" border borderColor="#FFFF4444" borderThickness="2">
    Це підказка для **підсвіченої області**.
  </ImageAnnotation>
</FloatingImage>
````

кілька regions on один зображення:

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

зображення regions може також play sounds. використовувати `<SoundArea>` коли you лише need sound, або put `sound`
directly on `<ImageAnnotation>` коли  той самий region також має a підказка або межа.

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
    Ця область має текст підказки та звук натискання.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers  whole зображення. Region sounds використовувати cropped-зображення координати
і obey  той самий overlap priority as tooltips: later regions win.

## Вбудовування вмісту та перенесення тексту

Блокові теги на основі `BlockTagCompiler`, зокрема `<Recipe>`, `<GameScene>` та `<BlockImage>`, приймають `wrap` і `align`.

`<FloatingImage>` має власні правила: `wrap="inline"` розміщує зображення в рядку; інакше `align="left"` або `"right"` вибирає бік обтікання. `<ItemImage>` — вбудована піктограма; для вирівнювання використовуйте `<Column>` або `<Row>`.

| Атрибут | Типово | Значення |
| --- | --- | --- |
| `wrap` | `inline` | `inline`: звичайне розміщення. `square`, `tight`, `through`: обтікання ліворуч або праворуч. `top-bottom`, `behind`, `front`: наразі звичайне вирівняне розміщення без окремих шарів тексту. |
| `align` | `left` | `left`, `center` або `right` у блоковому контексті. При обтіканні `right` вибирає правий бік, інші значення — лівий. Центрування: `wrap="inline" align="center"`. |
| `float` | — | Застарілі `left` / `right` у тексті за відсутності `wrap` та `align`. Рекомендовано явні атрибути. |

```mdx
<BlockImage id="minecraft:stone" wrap="square" align="right" />

<BlockImage id="minecraft:diamond_block" wrap="inline" align="center" />

<Column align="center">
  <ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
</Column>
```

## Навігація Texture Icons

frontmatter може використовувати `icon_texture` до показувати texture замість цього of an предмет in Навігація/пошук:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

 файл має decode as an зображення.  шлях is розв’язаний like any other посібник ресурс шлях.

## Non-зображення ресурси

GuideNH сторінки може також reference non-зображення виконання ресурси, especially структура files, для Приклад:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

Ці ресурси є завантажений through  той самий посібник ресурс pipeline but є consumed by власний теги rather ніж відтворений directly as зображення.

## Best Practices

- Збережіть сторінка-локальний зображення near  сторінка який використовує їх
- Збережіть reusable files under  посібник корінь `assets/` папка
- prefer rooted `/assets/...` paths для shared files referenced by кілька сторінки
- використовувати texture icons лише для real зображення ресурси

## `BlockImage`

`<BlockImage>` показує розміщений блок як прозорий 3D-перегляд і підтримує наведені вище атрибути вирівнювання та обтікання.

ключ поведінка:

- прозорий тло і межа
- no сцена buttons, no layer slider, no анотація authoring surface
- наведення досі shows  вибраний блок outline і підказка
- `scale` зміни камера масштаб і типові до `4`
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, і `up`
- `nbt` supplies блок-сутність SNBT; inline `id="mod:block:meta:{...}"` SNBT досі works, but 
  standalone `nbt` Атрибут is easier до читати і is бажаний

Приклад:

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## виконання Приклад Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Пов’язані сторінки

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
