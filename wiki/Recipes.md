# Recipes

GuideNH can render crafting and NEI-backed recipes directly inside guide pages.

## Supported Tags

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

All three share the same compiler and attribute set.

## Tag Semantics

| Tag              | Behavior                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | render a single recipe for the target item             |
| `<Usage>`        | render a single recipe using the target item           |
| `<RecipeFor>`    | same single-recipe behavior, more author-friendly name |
| `<RecipeUsage>`  | same single-recipe behavior, more author-friendly name |
| `<RecipesFor>`   | render multiple matching recipes                       |
| `<RecipesUsage>` | render multiple matching recipes                       |

If multiple recipes exist and you use the single-recipe forms, GuideNH renders only one result unless filters narrow it further.

## Common Attributes

| Attribute      | Required | Meaning                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | yes      | target item reference                             |
| `fallbackText` | no       | text shown when no usable recipe is found         |
| `handlerName`  | no       | case-insensitive substring filter on handler name |
| `handlerId`    | no       | exact overlay/handler id filter, case-insensitive |
| `handlerBlacklist` | no   | comma-separated handlers to drop from the results |
| `handlerWhitelist` | no   | comma-separated handlers to keep, even if blacklisted |
| `handlerOrder` | no       | 0-based index after handler filtering             |
| `input`        | no       | ingredient filter expression                      |
| `output`       | no       | result filter expression                          |
| `limit`        | no       | positive integer max number of rendered recipes   |

## Item Id Syntax

The `id`, `input`, and `output` attributes all use the GuideNH extended item reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta values:

- `*`
- `32767`
- uppercase tokens such as `ANY`

## Filter Expression Syntax

The `input` and `output` filters support:

- `,` for OR
- `&` for AND
- `!` for NOT

Examples:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT can be combined with all three item-reference attributes. Commas and ampersands inside an
SNBT compound/list are kept inside the reference; only top-level separators split the filter:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## Rendering Order

GuideNH tries recipes in this order:

1. NEI native handler rendering
2. NEI slot-data fallback
3. built-in vanilla crafting fallback

If nothing matches:

- `fallbackText` is used when present
- otherwise an inline authoring error is shown

## Examples

### Single recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### Multiple recipes

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler blacklist and whitelist

`handlerBlacklist` drops handlers from the results, `handlerWhitelist` puts them back. Both take a
**comma-separated list**, and an entry matches the handler id, its overlay id or its class name,
case-insensitively and as a substring — so one entry can name a single handler or a whole package.

A handler is dropped when it is named by a blacklist **unless** the tag asks for it: `handlerId`,
`handlerWhitelist` and a lowercased class-name match all keep it.

**A list, because one handler id rarely appears alone.** Planks are both a crafting material and a fuel, so
list the handler ids you do not want:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**A package in one entry.** An entry is a substring of the class name too, so a mod id covers every handler
that mod registers — handy when one mod's handlers are all noise for this item:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Whitelist, to undo a blacklist for one page.** The configuration hides two multiblock handlers on every
page. A whitelist naming them brings them back here, and a list works the same way:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Whitelist does not filter on its own.** It only rescues handlers a blacklist would drop; use
`handlerId` / `handlerName` / `handlerOrder` to narrow the result to one handler, and `limit` to cap how
many are drawn.

The configuration file `config/guidenh/guidenh.cfg` holds `recipeHandlerBlacklist`, which applies to every
page and by default contains:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

A page's own list is **added** to the configured one, so a page can hide more but not fewer. Editing the
configuration changes every page; naming a handler on a tag changes only that page.

### Input/output filtering

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### Fallback text

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## Best Practices

- use `fallbackText` for optional-mod integrations
- use `handlerId` when you know the exact NEI handler you want
- use `handlerBlacklist` when one block is produced by many handlers and only some are worth showing
- the two handlers below are hidden by default in `config/guidenh/guidenh.cfg` because they render a whole multiblock; name one with `handlerId` or `handlerWhitelist` to show it
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- use `limit` when a tag could expand into many recipes
- keep complex filter logic in comments near the tag for maintainability

## Live Runtime Example

See `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` for extensive recipe samples, including handler filters and wildcard/NBT item ids.
