# Рецепти

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH може відтворювати crafting і NEI-backed Рецепти directly усередині посібник сторінки.

## Підтримувані теги

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

усі three share  той самий компілятор і Атрибут Установіть.

## тег Semantics

| тег              | поведінка                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | відтворювати a один recipe для  ціль предмет             |
| `<Usage>`        | відтворювати a один recipe використовуючи  ціль предмет           |
| `<RecipeFor>`    | той самий один-recipe поведінка, more автор-friendly назва |
| `<RecipeUsage>`  | той самий один-recipe поведінка, more автор-friendly назва |
| `<RecipesFor>`   | відтворювати кілька відповідний Рецепти                       |
| `<RecipesUsage>` | відтворювати кілька відповідний Рецепти                       |

Якщо кілька Рецепти exist і you використовувати  один-recipe forms, GuideNH renders лише один результат unless filters narrow it further.

## звичайний атрибути

| Атрибут      | Обов’язково | Значення                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | yes      | ціль предмет reference                             |
| `fallbackText` | no       | текст показано коли no usable recipe is found         |
| `handlerName`  | no       | case-insensitive substring фільтр on handler назва |
| `handlerId`    | no       | exact overlay/handler id фільтр, case-insensitive |
| `handlerBlacklist` | no   | comma-separated handlers до drop з  результати |
| `handlerWhitelist` | no   | comma-separated handlers до Збережіть, even Якщо blacklisted |
| `handlerOrder` | no       | 0-based index після handler filtering             |
| `input`        | no       | ingredient фільтр вираз                      |
| `output`       | no       | результат фільтр вираз                          |
| `limit`        | no       | positive ціле число max число of відтворений Рецепти   |

## предмет Id синтаксис

 `id`, `input`, і `output` атрибути усі використовувати  GuideNH extended предмет reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta значення:

- `*`
- `32767`
- uppercase tokens наприклад `ANY`

## фільтр вираз синтаксис

 `input` і `output` filters підтримка:

- `,` для або
- `&` для і
- `!` для не

Приклади:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT може be combined з усі three предмет-reference атрибути. Commas і ampersands усередині an
SNBT compound/список є kept усередині  reference; лише угорі-level separators split  фільтр:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## відтворення порядок

GuideNH tries Рецепти in Цей порядок:

1. NEI native handler відтворення
2. NEI slot-дані резервний варіант
3. built-in vanilla crafting резервний варіант

Якщо nothing відповідає:

- `fallbackText` is використовується коли present
- otherwise inline authoring помилка is показано

## Приклади

### один recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### кілька Рецепти

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler blacklist і whitelist

`handlerBlacklist` drops handlers з  результати, `handlerWhitelist` puts їх назад. Both take a
**comma-separated список**, і запис відповідає  handler id, його overlay id або його class назва,
case-insensitively і as substring — so один запис може назва a один handler або whole package.

handler is dropped коли it is named by blacklist **unless**  тег asks для it: `handlerId`,
`handlerWhitelist` і lowercased class-назва відповідати усі Збережіть it.

**A список, because один handler id rarely appears alone.** Planks є both crafting material і fuel, so
список  handler ids you do не want:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**package in один запис.** запис is substring of  class назва too, so mod id covers кожен handler
який mod registers — handy коли один mod's handlers є усі noise для Цей предмет:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Whitelist, до undo blacklist для один сторінка.**  configuration hides два multiblock handlers on кожен
сторінка. whitelist naming їх brings їх назад here, і a список works  той самий way:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Whitelist не фільтр on його власний.** It лише rescues handlers blacklist would drop; використовувати
`handlerId` / `handlerName` / `handlerOrder` до narrow  результат до один handler, і `limit` до cap how
many є drawn.

 configuration файл `config/guidenh/guidenh.cfg` holds `recipeHandlerBlacklist`, який застосовує до кожен
сторінка і by Типове містить:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

A сторінка's власний список is **added** до  configured один, so a сторінка може приховувати more but не fewer. Editing 
configuration зміни кожен сторінка; naming handler on a тег зміни лише який сторінка.

### ввід/вивід filtering

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### резервний варіант текст

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## Best Practices

- використовувати `fallbackText` для необов’язковий-mod integrations
- використовувати `handlerId` коли you know  exact NEI handler you want
- використовувати `handlerBlacklist` коли один блок is produced by many handlers і лише some є worth showing
-  два handlers below є прихований by Типове in `config/guidenh/guidenh.cfg` because Вони відтворювати whole multiblock; назва один з `handlerId` або `handlerWhitelist` до показувати it
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- використовувати `limit` коли a тег could expand до many Рецепти
- Збережіть complex фільтр logic in comments near  тег для maintainability

## Live виконання Приклад

Дивіться `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` для extensive recipe samples, including handler filters і wildcard/NBT предмет ids.
