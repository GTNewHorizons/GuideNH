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

## Liens de recettes NEI dans le texte

Utilisez `&[texte](nei:...)` ou `<NeiRecipeLink>` pour ouvrir NEI depuis le texte. Les libellés des actions sont du texte brut ; la balise accepte du contenu mis en forme.

```md
&[Voir toutes les recettes du four à coke](nei:handler?handlerId=gt.recipe.cokeoven)
&[Voir les recettes](nei:recipes?id=minecraft:iron_ingot)
&[Voir les utilisations](nei:usage?id=gregtech:gt.blockmachines:236&handlerId=gt.recipe.cokeoven)

<NeiRecipeLink handlerId="gt.recipe.cokeoven">Voir toutes les recettes du four à coke</NeiRecipeLink>
<NeiRecipeLink id="minecraft:iron_ingot">Voir les **recettes**</NeiRecipeLink>
<NeiRecipeLink mode="usage" id="gregtech:gt.blockmachines:236" handlerId="gt.recipe.cokeoven" title="NEI">Voir les utilisations</NeiRecipeLink>

&[Voir les recettes de livres avec SNBT](nei:recipes?id=minecraft:written_book:0:%7Btitle:%22Guide%2BNEI%22%2Cauthor:GuideNH%7D)
```

`handler` exige `handlerId`, interdit `id` et ouvre toutes les recettes du gestionnaire choisi. `recipes` exige `id` et ouvre les recettes produisant cet objet. `usage` exige `id` et ouvre les utilisations, y compris les catalyseurs, quel que soit l’état de la touche Ctrl. Ces deux derniers modes acceptent un filtre facultatif avec `handlerId`.

`handlerId` correspond exactement à l’identifiant de superposition ou de gestionnaire NEI, sans tenir compte de la casse. Les noms de classes Java et les identifiants partiels ne sont pas acceptés.

Les identifiants d’objets sans espace de noms utilisent celui de la page actuelle. La casse du nom de registre, les métadonnées et SNBT sont conservés. Les jokers de métadonnées suivent les règles existantes des piles concrètes et ne sélectionnent pas toutes les variantes.

Les valeurs URI sont décodées une seule fois en UTF-8 strict ; un `+` littéral reste `+`. Encodez SNBT et les caractères réservés tels que `&`, `#`, `%`, `?` et `)` avec des échappements pourcentés.

Les paramètres inconnus ou répétés, les échappements invalides, les fragments, les modes non pris en charge, les attributs contradictoires et les cibles manquantes produisent des erreurs de rédaction. Seules les actions avec `&` et `<NeiRecipeLink>` ouvrent NEI ; les liens ordinaires, les liens de référence, les ancres HTML et `nei://` ne sont pas des variantes de cette syntaxe.

Si NEI est absent ou qu’aucune recette ne correspond, le guide reste ouvert et affiche un message. Les aperçus du navigateur et de l’IDE affichent du texte non cliquable. Ouvrez la page dans GuideNH pour utiliser le lien.

## Related references

- [Tags Reference](Tags-Reference)
- [Mod Compatibility](Mod-Compatibility)
- [Examples](Examples)
