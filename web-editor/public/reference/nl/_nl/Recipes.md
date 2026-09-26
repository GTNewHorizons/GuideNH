# Recepten

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH kan renderen crafting en NEI-backed Recepten directly binnen gids pages.

## Supported tags

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

alle three share De zelfde compiler en Attribuut Stel in.

## tag Semantics

| tag              | Behavior                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | renderen Een enkele recipe voor De target item             |
| `<Usage>`        | renderen Een enkele recipe using De target item           |
| `<RecipeFor>`    | zelfde enkele-recipe behavior, more author-friendly naam |
| `<RecipeUsage>`  | zelfde enkele-recipe behavior, more author-friendly naam |
| `<RecipesFor>`   | renderen meerdere matching Recepten                       |
| `<RecipesUsage>` | renderen meerdere matching Recepten                       |

Als meerdere Recepten exist en you gebruiken De enkele-recipe forms, GuideNH renders alleen one result unless filters narrow it further.

## algemene attributen

| Attribuut      | Vereist | Betekenis                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | yes      | target item reference                             |
| `fallbackText` | no       | tekst getoond wanneer no usable recipe is found         |
| `handlerName`  | no       | case-insensitive substring filter on handler naam |
| `handlerId`    | no       | exact overlay/handler id filter, case-insensitive |
| `handlerBlacklist` | no   | comma-separated handlers naar drop van De results |
| `handlerWhitelist` | no   | comma-separated handlers naar Behoud, even Als blacklisted |
| `handlerOrder` | no       | 0-based index na handler filtering             |
| `input`        | no       | ingredient filter expressie                      |
| `output`       | no       | result filter expressie                          |
| `limit`        | no       | positive geheel getal max getal of gerenderd Recepten   |

## item Id syntaxis

De `id`, `input`, en `output` attributen alle gebruiken De GuideNH extended item reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta waarden:

- `*`
- `32767`
- uppercase tokens such as `ANY`

## filter expressie syntaxis

De `input` en `output` filters ondersteuning:

- `,` voor of
- `&` voor en
- `!` voor niet

Voorbeelden:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT kan be combined met alle three item-reference attributen. Commas en ampersands binnen an
SNBT compound/lijst are kept binnen De reference; alleen top-level separators split De filter:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## Rendering Order

GuideNH tries Recepten in Deze order:

1. NEI native handler rendering
2. NEI slot-data fallback
3. built-in vanilla crafting fallback

Als nothing matches:

- `fallbackText` is gebruikt wanneer present
- otherwise Een inline authoring error is getoond

## Voorbeelden

### enkele recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### meerdere Recepten

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler blacklist en whitelist

`handlerBlacklist` drops handlers van De results, `handlerWhitelist` puts them back. Both take a
**comma-separated lijst**, en Een entry matches De handler id, its overlay id of its class naam,
case-insensitively en as Een substring — so one entry kan naam Een enkele handler of Een whole package.

Een handler is dropped wanneer it is named by Een blacklist **unless** De tag asks voor it: `handlerId`,
`handlerWhitelist` en Een lowercased class-naam match alle Behoud it.

**Een lijst, because one handler id rarely appears alone.** Planks are both Een crafting material en Een fuel, so
lijst De handler ids you do niet want:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**Een package in one entry.** Een entry is Een substring of De class naam too, so Een mod id covers iedere handler
that mod registers — handy wanneer one mod's handlers are alle noise voor Deze item:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Whitelist, naar undo Een blacklist voor one pagina.** De configuration hides two multiblock handlers on iedere
pagina. Een whitelist naming them brings them back here, en Een lijst works De zelfde way:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Whitelist doet niet filter on its own.** It alleen rescues handlers Een blacklist would drop; gebruiken
`handlerId` / `handlerName` / `handlerOrder` naar narrow De result naar one handler, en `limit` naar cap how
many are drawn.

De configuration bestand `config/guidenh/guidenh.cfg` holds `recipeHandlerBlacklist`, which applies naar iedere
pagina en by Standaard bevat:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

Een pagina's own lijst is **added** naar De configured one, so Een pagina kan verbergen more but niet fewer. Editing De
configuration wijzigingen iedere pagina; naming Een handler on Een tag wijzigingen alleen that pagina.

### Input/output filtering

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### fallback tekst

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## Best Practices

- gebruiken `fallbackText` voor optioneel-mod integrations
- gebruiken `handlerId` wanneer you know De exact NEI handler you want
- gebruiken `handlerBlacklist` wanneer one blok is produced by many handlers en alleen some are worth showing
- De two handlers below are verborgen by Standaard in `config/guidenh/guidenh.cfg` because they renderen Een whole multiblock; naam one met `handlerId` of `handlerWhitelist` naar tonen it
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- gebruiken `limit` wanneer Een tag could expand into many Recepten
- Behoud complex filter logic in comments near De tag voor maintainability

## Live runtime Example

See `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` voor extensive recipe samples, including handler filters en wildcard/NBT item ids.
