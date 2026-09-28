# Анотації

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH сцена Анотації є дочірній елемент теги усередині `<GameScene>` / `<Scene>`. Вони відтворювати in світ простір і може містити дочірній елемент Markdown/тег вміст який стає розширений підказка.

## Загальні правила

- Анотації лише працюють усередині a сцена
- дочірній елемент вміст стає  підказка body
- Анотації може be прихований з  сцена UI toggle
- `alwaysOnTop` малює над сцена геометрією коли Підтримувані by  анотація Тип
- усі сцена Анотації також приймають необов’язковий `showWhenStructure`, `showWhenTier`, і `showWhenChannels` умови коли  сцена використовує `<ImportStructureLib>`

## Підтримувані теги анотацій

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

GuideNH також підтримує `<BlockAnnotationTemplate>`, який застосовує його дочірній елемент Анотації до кожен вже-розміщений відповідний блок in  поточний сцена.

## Умови StructureLib

коли a сцена містить `<ImportStructureLib>`, кожен анотація тег може restrict його видимість до певний
StructureLib стан:

| Атрибут | Значення |
| --- | --- |
| `showWhenStructure` | bind  анотація до named `<ImportStructureLib name="...">`; omit it коли  сцена лише імпорти один StructureLib структура |
| `showWhenTier` | tier фільтр наприклад `2`, `1..3`, `!2`, або `1..5,!3` |
| `showWhenChannels` | per-channel фільтр наприклад `input:1..3, casing:!2, fluid:4` |

Правила:

- `showWhenTier` і `showWhenChannels` є combined з logical і
- `showWhenChannels` може mention кілька канали in один Атрибут
- negated-лише clauses like `!2` mean "any значення except 2"
-  той самий атрибути є також Підтримувані by `<PlaySound>` і `<BlockAnnotationTemplate>` дочірній елемент Анотації

Приклад:

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
    Видно лише для вибраного стану StructureLib.
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

Підсвічує один об’єм блока 1x1x1.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `pos` | yes | `x y z` вектор |
| `color` | no | `#RRGGBB`, `#AARRGGBB`, або `transparent` |
| `thickness` | no | товщина лінії число з плаваючою комою |
| `alwaysOnTop` | no | логічне значення вираз |

Приклад:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Підсвічує блок контролера.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Підсвічує довільну коробку, вирівняну за осями.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `min` | yes | `x y z` minimum вектор |
| `max` | yes | `x y z` maximum вектор |
| `color` | no | анотація колір |
| `thickness` | no | товщина лінії число з плаваючою комою |
| `alwaysOnTop` | no | логічне значення вираз |

GuideNH автоматично swaps min/max координати per вісь коли Вони є provided in reverse порядок.

Приклад:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Підсвічування області на половину висоти.
</BoxAnnotation>
````

## `<LineAnnotation>`

малює a відрізок або ламана in світ простір.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `from` | yes, unless `points` is Установіть | `x y z` початок вектор |
| `to` | yes, unless `points` is Установіть | `x y z` кінець вектор |
| `points` | no | Semicolon-separated `x y z` точки для a ламана; overrides `from` / `to` |
| `color` | no | анотація колір |
| `thickness` | no | товщина лінії число з плаваючою комою |
| `alwaysOnTop` | no | логічне значення вираз |
| `arrow` | no | `start` або `end`; пропущений означає no стрілка |
| `showPoints` | no | логічне значення вираз; shows кожен точка as small cube |
| `pointColor` | no | Типове cube колір; пропущений використовує  рядок колір |
| `pointSize` | no | Типове cube розмір; пропущений використовує a значення slightly larger ніж `thickness` |

`LineAnnotation` може містити `<LinePoint>` дочірні елементи до перевизначати точка marker styling. `LinePoint`
використовує `index`, необов’язковий `show`, необов’язковий `color`, і необов’язковий `size`. точки є zero-indexed.
Arrows може лише be розміщений on  початок або кінець of  рядок; intermediate ламана точки не може carry
arrows.

Приклад:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Сигнальний шлях.
</LineAnnotation>
````

ламана з 3D endpoint стрілка і вибраний точка маркери:

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Сигнальний шлях через вигин.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places a екран-facing diamond marker at a світ позиція.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `pos` | yes | `x y z` marker позиція |
| `color` | no | tint колір; пропущений типові до bright green |

Приклад:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

малює speech-бульбашка текст мітка over  сцена. It може either follow a простір світу якір точка або
залишаються фіксований відносний до  сцена центр. Unlike  other анотація теги, його дочірній елемент вміст is 
бульбашка текст itself rather ніж a наведення підказка.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `pos` | no | `x y z` простір світу якір вектор |
| `x`, `y`, `z` | no | Alternative простір світу якір components коли `pos` is пропущений |
| `text` | no | бульбашка текст; дочірній елемент Markdown is використовується коли пропущений |
| `textKey` | no | ключ перекладу розв’язаний з ресурс-pack `lang` files до falling назад до `text` або дочірній елемент Markdown |
| `color` | no | бульбашка межа колір; типові до light grey |
| `backgroundAlpha` | no | тло opacity з `0` до `255`; типові до `204` |
| `maxWidth` | no | перенесення ширина in пікселі; `0` зберігає a один рядок |
| `independent` | no | `true` зберігає  бульбашка фіксований in екран простір |
| `yOffset` | no | піксель offset з  сцена центр коли `independent={true}` |
| `connectorSide` | no | `bottom`, `top`, `left`, `right`, або `none`; типові до `bottom` |
| `connectorOffset` | no | піксель offset уздовж  бульбашка край; positive moves праворуч для угорі/унизу і down для ліворуч/праворуч |
| `connectorLength` | no | піксель length of  з’єднувач рядок; типові до `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | no | необов’язковий companion виділяти box межі |
| `highlightColor` | no | необов’язковий виділяти box колір |

світ-anchored bubbles малювати з’єднувач рядок до їхні якір. використовувати `connectorSide` до вибрати який
край of  бульбашка точки at  якір, `connectorOffset` до move  attachment точка уздовж який
край, і `connectorLength` до control  gap between  бульбашка і якір. Independent bubbles
є centered horizontally in  сцена і використовувати `yOffset` для vertical placement. Вони do не малювати a
з’єднувач.  той самий виконання анотація is також використовується коли importing Анімація Ponder `text` Анотації.

Приклад:

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
  Вставте сюди предмети з **пріоритетом**.
</TextAnnotation>
````

фіксований простір екрана Приклад:

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## розширений підказка вміст

анотація дочірні елементи є скомпільований as normal GuideNH вміст, so tooltips може містити:

- Markdown paragraphs і headings
- предмет/блок зображення
- Рецепти
- nested non-інтерактивний сцени

Приклад:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

використовувати it коли you want до stamp  той самий анотація onto кожен відповідний блок.

| Атрибут | Обов’язково | Значення |
| --- | --- | --- |
| `id` | yes | блок matcher in `modid:block[:meta]` form |

Правила:

-  template лише sees блоки який вже exist коли it is проаналізований
- place it після `<Block>`, `<ImportStructure>`, або `<ImportStructureLib>` теги який слід feed it
- дочірній елемент Анотації використовувати локальний координати відносний до кожен відповідний блок

Приклад:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Підказка, створена шаблоном
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Пов’язані сторінки

- [GameScene](GameScene)
- [Examples](Examples)
