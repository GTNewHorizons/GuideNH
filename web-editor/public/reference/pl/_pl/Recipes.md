# Receptury

Szczegółowa dokumentacja na tej stronie jest dostępna po angielsku. Nazwy tagów, atrybuty, identyfikatory i ścieżki należy stosować bez tłumaczenia.

Use recipe tags to show how an item or fluid is produced or consumed. Machine recipes depend on the installed recipe integrations; vanilla crafting is available without NEI.

## Choosing a tag

| Tag | Query | Default result count |
| --- | --- | --- |
| `<Recipe>` / `<RecipeFor>` | Produces the target | One |
| `<Usage>` / `<RecipeUsage>` | Consumes the target | One |
| `<RecipesFor>` | Produces the target | All matches |
| `<RecipesUsage>` | Consumes the target | All matches |

All six tags accept the same attributes. Use `limit` to cap the result count and `recipeIndex` to select a specific recipe.

## Attributes

Provide exactly one target: `id` for an item or `fluid` for a fluid. Fluid names are exact, case-sensitive Forge registry names such as `water` or `steam`; use registered names rather than temporary numeric display IDs.

| Attribute | Meaning |
| --- | --- |
| `id` | Target item reference; mutually exclusive with `fluid` |
| `fluid` | Target fluid registry name; mutually exclusive with `id` |
| `fallbackText` | Text displayed when no usable recipe is found |
| `handlerName` | Case-insensitive substring of the recipe handler's display name |
| `handlerId` | Exact overlay/handler ID, case-insensitive; also accepts a substring of the handler's simple class name |
| `handlerBlacklist` | Comma-separated handlers to exclude |
| `handlerWhitelist` | Comma-separated handlers to retain despite a blacklist; does not narrow results by itself |
| `handlerOrder` | Nonnegative, zero-based index into the filtered handler list |
| `recipeIndex` | Nonnegative, zero-based recipe index |
| `input` / `output` | Item ingredient/result filter expressions |
| `inputFluid` / `outputFluid` | Fluid ingredient/result filter expressions |
| `limit` | Positive integer maximum result count |

```md
<RecipeFor id="minecraft:crafting_table" />
<RecipesFor id="minecraft:torch" limit="3" />
<RecipeFor fluid="steam" fallbackText="No steam recipe is available." />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
```

If an optional recipe integration is unavailable, `fallbackText` is displayed; without it, the page shows an authoring error. Filled containers remain item ingredients.

## Item references

`id`, `input` and `output` accept these forms:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard metadata may be written as `*`, `32767` or an uppercase token such as `ANY`.

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
```

## Ingredient filters

Both item and fluid filters support `,` for OR, `&` for AND and `!` for NOT. Every supplied input/output filter must match. Commas and ampersands inside an item's SNBT compound or list stay within that reference.

```md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
<RecipesFor id="minecraft:iron_ingot" outputFluid="steam,water" />
<RecipesUsage fluid="water" inputFluid="water&!lava" limit="3" />
```

Fluid filters match registry names, including fluids in auxiliary output slots. They do not match amounts or NBT. Invalid filter syntax produces an authoring error.

## Handler selection

Use `handlerId` for a known recipe handler, `handlerName` for a display-name match or `handlerOrder` to choose from the filtered list. `recipeIndex` selects a recipe within the query.

```md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" limit="3" />
<RecipeFor id="minecraft:torch" recipeIndex="0" />
```

Blacklist and whitelist entries match handler IDs, overlay IDs and class names as case-insensitive substrings. Page blacklists add to `recipeHandlerBlacklist` in `config/guidenh/guidenh.cfg`. Explicit `handlerId` or `handlerWhitelist` selection can retain a blacklisted handler.

```md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" limit="6" />
<RecipeFor id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
```

The following multiblock handlers are excluded by default because they render a whole structure:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

Use a whitelist to include them on a page. Use `limit` for broad queries to keep pages readable.

## Odnośniki do receptur NEI w tekście

Użyj `&[tekst](nei:...)` lub `<NeiRecipeLink>`, aby otworzyć NEI z tekstu. Etykiety akcji są zwykłym tekstem; tag obsługuje formatowanie zawartości.

```md
&[Pokaż wszystkie receptury pieca koksowniczego](nei:handler?handlerId=gt.recipe.cokeoven)
&[Pokaż receptury](nei:recipes?id=minecraft:iron_ingot)
&[Pokaż zastosowania](nei:usage?id=gregtech:gt.blockmachines:236&handlerId=gt.recipe.cokeoven)

<NeiRecipeLink handlerId="gt.recipe.cokeoven">Pokaż wszystkie receptury pieca koksowniczego</NeiRecipeLink>
<NeiRecipeLink id="minecraft:iron_ingot">Pokaż **receptury**</NeiRecipeLink>
<NeiRecipeLink mode="usage" id="gregtech:gt.blockmachines:236" handlerId="gt.recipe.cokeoven" title="NEI">Pokaż zastosowania</NeiRecipeLink>

&[Pokaż receptury książek z SNBT](nei:recipes?id=minecraft:written_book:0:%7Btitle:%22Guide%2BNEI%22%2Cauthor:GuideNH%7D)
```

`handler` wymaga `handlerId`, zabrania `id` i otwiera wszystkie receptury wybranego modułu obsługi. `recipes` wymaga `id` i otwiera receptury wytwarzające ten przedmiot. `usage` wymaga `id` i otwiera zastosowania, w tym użycie jako katalizatora, niezależnie od klawisza Ctrl. Dwa ostatnie tryby pozwalają na opcjonalny filtr `handlerId`.

`handlerId` musi dokładnie odpowiadać identyfikatorowi nakładki lub modułu obsługi NEI, bez rozróżniania wielkości liter. Nazwy klas Java i częściowe identyfikatory nie są akceptowane.

Identyfikatory przedmiotów bez przestrzeni nazw używają przestrzeni nazw bieżącej strony. Wielkość liter nazwy rejestru, metadane i SNBT są zachowywane. Symbole wieloznaczne metadanych stosują istniejące reguły konkretnych stosów przedmiotów i nie wybierają wszystkich wariantów.

Wartości URI są dekodowane dokładnie raz jako ścisłe UTF-8; dosłowny znak `+` pozostaje `+`. SNBT i znaki zastrzeżone, takie jak `&`, `#`, `%`, `?` i `)`, należy kodować procentowo.

Powtórzone lub nieznane parametry, błędne sekwencje ucieczki, fragmenty, nieobsługiwane tryby, sprzeczne atrybuty i brakujące cele powodują błędy tworzenia strony. NEI otwierają tylko akcje z `&` i `<NeiRecipeLink>`; zwykłe odnośniki, odnośniki referencyjne, kotwice HTML i `nei://` nie są alternatywną składnią.

Jeśli NEI jest niedostępne lub nie ma pasujących receptur, poradnik pozostaje otwarty i wyświetla komunikat. Podglądy w przeglądarce i IDE pokazują tekst, którego nie można kliknąć. Otwórz stronę w GuideNH, aby użyć odnośnika.

## Related references

- [Tags Reference](Tags-Reference)
- [Mod Compatibility](Mod-Compatibility)
- [Examples](Examples)
