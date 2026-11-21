# Recetas

La referencia detallada de esta página está disponible en inglés. Los nombres de etiquetas, atributos, identificadores y rutas se utilizan sin traducir.

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

## Enlaces de recetas de NEI en línea

Usa `&[texto](nei:...)` o `<NeiRecipeLink>` para abrir NEI desde el texto. Las etiquetas de acción usan texto sin formato; la etiqueta admite contenido con formato.

```md
&[Ver todas las recetas del horno de coque](nei:handler?handlerId=gt.recipe.cokeoven)
&[Ver recetas](nei:recipes?id=minecraft:iron_ingot)
&[Ver usos](nei:usage?id=gregtech:gt.blockmachines:236&handlerId=gt.recipe.cokeoven)

<NeiRecipeLink handlerId="gt.recipe.cokeoven">Ver todas las recetas del horno de coque</NeiRecipeLink>
<NeiRecipeLink id="minecraft:iron_ingot">Ver **recetas**</NeiRecipeLink>
<NeiRecipeLink mode="usage" id="gregtech:gt.blockmachines:236" handlerId="gt.recipe.cokeoven" title="NEI">Ver usos</NeiRecipeLink>

&[Ver recetas de libros con SNBT](nei:recipes?id=minecraft:written_book:0:%7Btitle:%22Guide%2BNEI%22%2Cauthor:GuideNH%7D)
```

`handler` requiere `handlerId`, prohíbe `id` y abre todas las recetas del manejador elegido. `recipes` requiere `id` y abre recetas que producen ese objeto. `usage` requiere `id` y abre sus usos, incluidos los catalizadores, independientemente de la tecla Ctrl. Los dos últimos modos admiten un filtro opcional mediante `handlerId`.

`handlerId` coincide exactamente con el identificador de superposición o de manejador de NEI, sin distinguir mayúsculas. No admite nombres de clases Java ni identificadores parciales.

Los ID de objetos sin espacio de nombres usan el de la página actual. Se conservan las mayúsculas del nombre de registro, los metadatos y SNBT. Los comodines de metadatos siguen las reglas existentes para pilas concretas y no seleccionan todas las variantes.

Los valores URI se decodifican una sola vez como UTF-8 estricto; un `+` literal sigue siendo `+`. Codifica SNBT y los caracteres reservados como `&`, `#`, `%`, `?` y `)` con escapes porcentuales.

Los parámetros duplicados o desconocidos, escapes inválidos, fragmentos, modos no admitidos, atributos contradictorios y destinos ausentes generan errores de escritura. Solo las acciones con `&` y `<NeiRecipeLink>` abren NEI; los enlaces normales, de referencia, anclas HTML y `nei://` no son formas alternativas.

Si NEI no está disponible o no hay recetas coincidentes, la guía permanece abierta y muestra un mensaje. Las vistas previas del navegador y del IDE muestran texto sin enlace activo. Abre la página en GuideNH para usarlo.

## Related references

- [Tags Reference](Tags-Reference)
- [Mod Compatibility](Mod-Compatibility)
- [Examples](Examples)
