# Receptury

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH może renderować crafting i NEI-backed Receptury directly wewnątrz przewodnik strony.

## Obsługiwane tagi

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

wszystkie three share  ten sam kompilator i Atrybut Ustaw.

## tag Semantics

| tag              | zachowanie                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | renderować pojedynczy recipe dla  cel element             |
| `<Usage>`        | renderować pojedynczy recipe używając  cel element           |
| `<RecipeFor>`    | ten sam pojedynczy-recipe zachowanie, more autor-friendly nazwa |
| `<RecipeUsage>`  | ten sam pojedynczy-recipe zachowanie, more autor-friendly nazwa |
| `<RecipesFor>`   | renderować wiele pasujący Receptury                       |
| `<RecipesUsage>` | renderować wiele pasujący Receptury                       |

Jeśli wiele Receptury exist i you używać  pojedynczy-recipe forms, GuideNH renders tylko jeden wynik unless filters narrow it further.

## wspólny atrybuty

| Atrybut      | Wymagane | Znaczenie                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | yes      | cel element reference                             |
| `fallbackText` | no       | tekst wyświetlany gdy no usable recipe is found         |
| `handlerName`  | no       | case-insensitive substring filtr on handler nazwa |
| `handlerId`    | no       | exact overlay/handler id filtr, case-insensitive |
| `handlerBlacklist` | no   | comma-separated handlers do drop z  wyniki |
| `handlerWhitelist` | no   | comma-separated handlers do Zachowaj, even Jeśli blacklisted |
| `handlerOrder` | no       | 0-based index po handler filtering             |
| `input`        | no       | ingredient filtr wyrażenie                      |
| `output`       | no       | wynik filtr wyrażenie                          |
| `limit`        | no       | positive liczba całkowita max liczba of wyrenderowany Receptury   |

## element Id składnia

 `id`, `input`, i `output` atrybuty wszystkie używać  GuideNH extended element reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta wartości:

- `*`
- `32767`
- uppercase tokens takie jak `ANY`

## filtr wyrażenie składnia

 `input` i `output` filters obsługa:

- `,` dla lub
- `&` dla i
- `!` dla nie

Przykłady:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT może be combined z wszystkie three element-reference atrybuty. Commas i ampersands wewnątrz an
SNBT compound/lista są kept wewnątrz  reference; tylko góra-level separators split  filtr:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## renderowanie kolejność

GuideNH tries Receptury in Ten kolejność:

1. NEI native handler renderowanie
2. NEI slot-dane awaryjny
3. built-in vanilla crafting awaryjny

Jeśli nothing pasuje:

- `fallbackText` is używany gdy present
- otherwise inline authoring błąd is wyświetlany

## Przykłady

### pojedynczy recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### wiele Receptury

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler blacklist i whitelist

`handlerBlacklist` drops handlers z  wyniki, `handlerWhitelist` puts ich tył. Both take a
**comma-separated lista**, i wpis pasuje  handler id, jego overlay id lub jego class nazwa,
case-insensitively i as substring — so jeden wpis może nazwa pojedynczy handler lub whole package.

handler is dropped gdy it is named by blacklist **unless**  tag asks dla it: `handlerId`,
`handlerWhitelist` i lowercased class-nazwa pasować wszystkie Zachowaj it.

**lista, because jeden handler id rarely appears alone.** Planks są both crafting material i fuel, so
lista  handler ids you do nie want:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**package in jeden wpis.** wpis is substring of  class nazwa too, so mod id covers każdy handler
który mod registers — handy gdy jeden mod's handlers są wszystkie noise dla Ten element:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Whitelist, do undo blacklist dla jeden strona.**  configuration hides dwa multiblock handlers on każdy
strona. whitelist naming ich brings ich tył here, i lista works  ten sam way:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Whitelist nie filtr on jego własny.** It tylko rescues handlers blacklist would drop; używać
`handlerId` / `handlerName` / `handlerOrder` do narrow  wynik do jeden handler, i `limit` do cap how
many są drawn.

 configuration plik `config/guidenh/guidenh.cfg` holds `recipeHandlerBlacklist`, który stosuje do każdy
strona i by Domyślne zawiera:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

strona's własny lista is **added** do  configured jeden, so strona może ukrywać more but nie fewer. Editing 
configuration zmiany każdy strona; naming handler on tag zmiany tylko który strona.

### wejście/wyjście filtering

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### awaryjny tekst

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## Best Practices

- używać `fallbackText` dla opcjonalny-mod integrations
- używać `handlerId` gdy you know  exact NEI handler you want
- używać `handlerBlacklist` gdy jeden blok is produced by many handlers i tylko some są worth showing
-  dwa handlers below są ukryty by Domyślne in `config/guidenh/guidenh.cfg` because jeden renderować whole multiblock; nazwa jeden z `handlerId` lub `handlerWhitelist` do pokazywać it
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- używać `limit` gdy tag could expand do many Receptury
- Zachowaj complex filtr logic in comments near  tag dla maintainability

## Live czas wykonania Przykład

Zobacz `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` dla extensive recipe samples, including handler filters i wildcard/NBT element ids.
