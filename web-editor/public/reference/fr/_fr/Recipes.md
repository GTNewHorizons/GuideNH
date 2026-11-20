# Recettes

La référence détaillée de cette page est disponible en anglais. Les noms des balises, attributs, identifiants et chemins s’utilisent sans traduction.

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

## Related references

- [Tags Reference](Tags-Reference)
- [Mod Compatibility](Mod-Compatibility)
- [Examples](Examples)
