# Рецепты

GuideNH может отображать рецепты крафта и рецепты, поддерживаемые NEI, непосредственно на страницах руководства.

## Поддерживаемые теги

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

Все эти теги используют один и тот же компилятор и набор атрибутов.

## Native fluid recipe targets and filters

Exactly one of `id` and `fluid` selects the target of Recipe, Usage, RecipeFor, RecipeUsage, RecipesFor, or RecipesUsage. `fluid` is an exact Forge registry name. Existing handler, index, limit, fallback, and layout options apply.

`inputFluid` / `outputFluid` accept registry-name expressions with OR (`,`), AND (`&`), and NOT (`!`). Names remain case-sensitive. Every supplied item and fluid filter must pass; malformed fluid filters are errors and never weaken the query. Amount/NBT are not part of these name filters. Fluid output checks include auxiliary output slots as well as the primary result.

```mdx
<RecipeFor fluid="steam" fallbackText="No steam recipe provider is available." />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
<RecipesFor id="minecraft:iron_ingot" outputFluid="steam,water" />
```

Forge has no universal fluid machine-recipe registry. Optional providers supply those recipes; without a provider, fallbackText or a diagnostic is shown. NEI may create a temporary display item for its backend query, but numeric display IDs never enter guide sources. Real filled containers remain item ingredients. Only explicitly typed fluid slots or recognized fluid-display substitutes render as native fluid ingredients.

## Семантика тегов

| Тег              | Поведение                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | визуализировать один рецепт для целевого предмета.             |
| `<Usage>`        | визуализировать один рецепт с использованием целевого элемента.           |
| `<RecipeFor>`    | такое же поведение с одним рецептом, более удобное для автора имя. |
| `<RecipeUsage>`  | такое же поведение с одним рецептом, более удобное для автора имя. |
| `<RecipesFor>`   | отображать несколько совпадающих рецептов                       |
| `<RecipesUsage>` | отображать несколько совпадающих рецептов.                       |

Если существует несколько рецептов и вы используете формы с одним рецептом, GuideNH отображает только один результат, если фильтры не сузят его еще больше.

## Общие атрибуты

| Атрибут      | Обязательно. | Значение                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | да      | ссылка на целевой элемент                             |
| `fallbackText` | нет       | текст, отображаемый, когда не найден подходящий рецепт.         |
| `handlerName`  | нет       | фильтр подстроки без учета регистра в имени обработчика. |
| `handlerId`    | нет       | точный фильтр идентификатора наложения/обработчика, без учета регистра. |
| `handlerBlacklist` | нет   | разделенные запятыми обработчики для удаления из результатов. |
| `handlerWhitelist` | нет   | обработчики, разделенные запятыми, которые следует сохранить, даже если они внесены в черный список. |
| `handlerOrder` | нет       | индекс, отсчитываемый от 0, после фильтрации обработчика.             |
| `input`        | нет       | выражение фильтра ингредиентов                      |
| `output`       | нет       | выражение фильтра результата                          |
| `limit`        | нет       | максимальное положительное число отображаемых рецептов.   |

## Синтаксис идентификатора элемента

атрибуты `id`, `input` и `output` используют расширенный формат ссылки на элемент GuideNH:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Мета-значения подстановочных знаков:

- `*`
- `32767`
- токены в верхнем регистре, например `ANY`.

## Синтаксис выражения фильтра

Фильтры `input` и `output` поддерживают:

- `,` для ИЛИ
- `&` для И
- `!` для НЕ

Примеры:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT можно комбинировать со всеми тремя атрибутами ссылки на элемент. Запятые и амперсанды внутри
соединения/список SNBT хранятся внутри ссылки; только разделители верхнего уровня разделяют фильтр:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## Порядок рендеринга

GuideNH пробует рецепты в следующем порядке:

1. Отрисовка собственного обработчика NEI
2. Резервный вариант данных слота NEI
3. встроенный запасной вариант ванильного крафта.

Если ничего не совпадает:

- `fallbackText` используется, если он присутствует.
- в противном случае отображается встроенная ошибка авторинга.

## Примеры

### Один рецепт

````md
<RecipeFor id="minecraft:crafting_table" />
````

### Несколько рецептов

````md
<RecipesFor id="minecraft:torch" />
````

### Фильтрация обработчиков

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Чёрный и белый списки обработчиков

`handlerBlacklist` удаляет обработчики из результатов, `handlerWhitelist` возвращает их. Оба принимают
**список, разделенный запятыми**, и запись соответствует идентификатору обработчика, идентификатору его наложения или имени класса,
без учета регистра и в виде подстроки — поэтому одна запись может называть один обработчик или весь пакет.

Обработчик удаляется, когда он занесён в чёрный список, **если** тег явно не запрашивает его: `handlerId`,
`handlerWhitelist` и совпадение имени класса в нижнем регистре сохраняют его.

**Указывайте список, поскольку один идентификатор обработчика редко встречается один.** Доски — это одновременно материал для крафта и топливо, поэтому
перечислите идентификаторы обработчиков, которые вам не нужны:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**Пакет в одной записи.** Запись также является подстрокой имени класса, поэтому идентификатор мода охватывает каждый обработчик,
этот мод регистрируется — удобно, когда все обработчики одного мода являются шумом для этого элемента:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Белый список для отмены черного списка для одной страницы.** Конфигурация скрывает два обработчика мультиблока на каждой странице.
Белый список, присваивающий имена, возвращает их на этой странице:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Белый список не фильтрует сам по себе.** Он сохраняет только обработчики, которые мог бы удалить чёрный список; используйте
`handlerId` / `handlerName` / `handlerOrder`, чтобы сузить результат до одного обработчика, и `limit`, чтобы ограничить способ
и ограничить число отображаемых результатов.

Файл конфигурации `config/guidenh/guidenh.cfg` содержит `recipeHandlerBlacklist`, который применяется ко всем
страницам и по умолчанию содержит:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

Собственный список страницы **добавляется** к настроенному, поэтому страница может скрывать больше, но не меньше. Редактирование
изменение конфигурации влияет на каждую страницу, а имя обработчика в теге меняет только текущую страницу.

### Фильтрация ввода и вывода

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### Резервный текст

````md
<Recipe id="missingrecipe" fallbackText="Этот рецепт отключён." />
````

## Лучшие практики

- используйте `fallbackText` для интеграции дополнительных модов.
- используйте `handlerId`, если вы знаете, какой именно обработчик NEI вам нужен.
- используйте `handlerBlacklist`, когда один блок создается множеством обработчиков и только некоторые из них достойны отображения.
- два обработчика ниже по умолчанию скрыты в `config/guidenh/guidenh.cfg`, поскольку они отображают целый мультиблок; назовите его с помощью `handlerId` или `handlerWhitelist`, чтобы отобразить его.
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- используйте `limit`, если тег может расширяться на множество рецептов.
- сохраняйте сложную логику фильтра в комментариях рядом с тегом для удобства обслуживания.

## Пример живого выполнения

См. `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` для подробных примеров рецептов, включая фильтры обработчиков и идентификаторы элементов с подстановочными знаками/NBT.
