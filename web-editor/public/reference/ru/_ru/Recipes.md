# Рецепты

Теги рецептов показывают производство и использование предметов или жидкостей. Машинные рецепты зависят от установленных интеграций. Обычный крафт доступен без NEI.

## Выбор тега

| Тег | Направление поиска | Количество по умолчанию |
| --- | --- | --- |
| `<Recipe>` / `<RecipeFor>` | Производство цели | Один |
| `<Usage>` / `<RecipeUsage>` | Потребление цели | Один |
| `<RecipesFor>` | Производство цели | Все совпадения |
| `<RecipesUsage>` | Потребление цели | Все совпадения |

Все шесть тегов принимают одинаковые атрибуты. `limit` ограничивает количество результатов, а `recipeIndex` выбирает конкретный рецепт.

## Атрибуты

Укажите ровно одну цель: `id` для предмета или `fluid` для жидкости. Используйте полное имя жидкости в реестре Forge, например `water` или `steam`, с учётом регистра. Временные числовые ID предметов отображения не подходят.

| Атрибут | Значение |
| --- | --- |
| `id` | Целевой предмет; несовместим с `fluid` |
| `fluid` | Имя целевой жидкости в реестре; несовместим с `id` |
| `fallbackText` | Текст, если подходящий рецепт недоступен |
| `handlerName` | Подстрока отображаемого имени обработчика без учёта регистра |
| `handlerId` | Точное совпадение overlay/handler ID или подстрока простого имени класса обработчика, без учёта регистра |
| `handlerBlacklist` | Исключаемые обработчики через запятую |
| `handlerWhitelist` | Обработчики, сохраняемые вопреки чёрному списку; сам по себе не сужает поиск |
| `handlerOrder` | Неотрицательный индекс в отфильтрованном списке обработчиков, начиная с нуля |
| `recipeIndex` | Неотрицательный индекс рецепта, начиная с нуля |
| `input` / `output` | Фильтры входных/выходных предметов |
| `inputFluid` / `outputFluid` | Фильтры входных/выходных жидкостей |
| `limit` | Положительное целое число: максимум результатов |

Без нужной интеграции выводится `fallbackText`, а если он не задан — ошибка оформления. Заполненные ёмкости остаются предметными ингредиентами.

## Ссылки и фильтры

Предметы задаются как `modid:name`, `modid:name:meta` или `modid:name:meta:{snbt}`. Для любых метаданных используйте `*`, `32767` или токен в верхнем регистре, например `ANY`.

Предметные и жидкостные фильтры поддерживают `,` (ИЛИ), `&` (И) и `!` (НЕ). Все заданные входные и выходные фильтры должны выполняться. Разделители внутри составного SNBT-тега или списка не разбивают ссылку на предмет. Жидкостные фильтры сопоставляют имена реестра, включая дополнительные выходные слоты, без количества и NBT. Неверное выражение вызывает ошибку оформления.

## Выбор обработчика

Используйте `handlerId` для известного обработчика, `handlerName` для отображаемого имени и `handlerOrder` для позиции после фильтрации. `recipeIndex` выбирает рецепт в запросе.

Чёрный и белый списки сравнивают handler ID, overlay ID и имена классов как подстроки без учёта регистра. Список страницы добавляется к `recipeHandlerBlacklist` в `config/guidenh/guidenh.cfg`. Явные `handlerId` и `handlerWhitelist` сохраняют обработчики, попавшие в чёрный список.

Эти обработчики исключены по умолчанию, поскольку показывают целую структуру. Добавьте их в белый список на нужной странице:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

## Примеры

```md
<RecipeFor id="minecraft:crafting_table" />
<RecipesFor id="minecraft:torch" recipeIndex="0" />
<RecipeFor fluid="steam" fallbackText="Рецепт пара недоступен." />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
<RecipesFor id="minecraft:iron_ingot" outputFluid="steam,water" limit="3" />
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" limit="3" />
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" limit="6" />
<RecipeFor id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Предпросмотр мультиблока недоступен." />
```

Для широких запросов задавайте `limit`. См. также [Справочник тегов](Tags-Reference), [Совместимость с модами](Mod-Compatibility) и [Примеры](Examples).

## Inline NEI recipe links

Use `&[label](nei:...)` or `<NeiRecipeLink>` to open NEI from inline text.

```md
&[Check all Coke Oven recipes](nei:handler?handlerId=gt.recipe.cokeoven)
&[Check recipes](nei:recipes?id=minecraft:iron_ingot)
&[Check usages](nei:usage?id=gregtech:gt.blockmachines:236&handlerId=gt.recipe.cokeoven)

<NeiRecipeLink handlerId="gt.recipe.cokeoven">Check NEI</NeiRecipeLink>
<NeiRecipeLink id="minecraft:iron_ingot">Check **recipes**</NeiRecipeLink>
<NeiRecipeLink mode="usage" id="gregtech:gt.blockmachines:236" handlerId="gt.recipe.cokeoven" title="NEI">Check usages</NeiRecipeLink>

&[Book recipes with SNBT](nei:recipes?id=minecraft:written_book:0:%7Btitle:%22Guide%2BNEI%22%2Cauthor:GuideNH%7D)
```

`handler` requires `handlerId` and forbids `id`; it opens every recipe in the selected handler. `recipes` requires `id` and opens recipes producing that item. `usage` requires `id` and opens usages including catalyst behavior, regardless of Ctrl. The latter two modes accept an optional `handlerId` filter.

`<NeiRecipeLink>` accepts only `id`, `handlerId`, `mode`, and optional tooltip `title`. Without `mode`, an `id` selects `recipes`; otherwise it selects `handler`. Visible inline children are required. Use the tag for rich formatting; custom action labels are plain text.

`handlerId` matches the exact NEI overlay identifier or handler ID, ignoring case. It does not accept a Java class name or partial identifier.

Item IDs use the current page namespace when omitted and retain registry-name casing, metadata and SNBT. Wildcard metadata uses the existing concrete-stack rules; it does not request every metadata variant. URI values are decoded once as strict UTF-8; literal `+` stays `+`. Percent-encode SNBT and reserved characters such as `&`, `#`, `%`, `?`, and `)`.

Duplicate or unknown query parameters, invalid escapes, fragments, unsupported modes, conflicting attributes, and missing targets produce authoring errors. Only ampersand actions and `<NeiRecipeLink>` are NEI entry points; ordinary, reference, and HTML-anchor links are not aliases, and `nei://` is unsupported.

If NEI is unavailable or no recipes match, the guide remains open and displays a message. Browser and IDE previews display the link as inactive text; open the page in GuideNH to use it.
