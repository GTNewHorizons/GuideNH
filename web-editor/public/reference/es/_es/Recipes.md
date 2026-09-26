# Recetas


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH puede renderizan crafting y NEI-backed recipes directly dentro de guíUn páginas.

## Compatible etiquetas

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

todos tres share El mismo compilador y atributo Establezca.

## etiqueta Semantics

| etiqueta              | comportamiento                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | renderizan a único recipe para El destino elemento             |
| `<Usage>`        | renderizan a único recipe usando El destino elemento           |
| `<RecipeFor>`    | mismo único-recipe comportamiento, más autor-friendly nombre |
| `<RecipeUsage>`  | mismo único-recipe comportamiento, más autor-friendly nombre |
| `<RecipesFor>`   | renderizan varios coincidente recipes                       |
| `<RecipesUsage>` | renderizan varios coincidente recipes                       |

Si varios recipes exist y you usar El único-recipe forms, GuideNH renderiza solo uno resultado salvo filters narrow it further.

## común atributos

| atributo      | obligatorio | significado                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | sí      | destino elemento reference                             |
| `fallbackText` | no       | texto mostrado cuando no usable recipe es found         |
| `handlerName`  | no       | case-insensitive substring filtro on handler nombre |
| `handlerId`    | no       | exact overlay/handler id filtro, case-insensitive |
| `handlerBlacklist` | no   | comma-separated handlers Un drop de El resultados |
| `handlerWhitelist` | no   | comma-separated handlers Un Conserve, even Si blacklisted |
| `handlerOrder` | no       | 0-based index después de handler filtering             |
| `input`        | no       | ingredient filtro expresión                      |
| `output`       | no       | resultado filtro expresión                          |
| `limit`        | no       | positive entero max número of renderizado recipes   |

## elemento Id sintaxis

El `id`, `input`, y `output` atributos todos usar El GuideNH extended elemento reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta valores:

- `*`
- `32767`
- uppercase tokens como `ANY`

## filtro expresión sintaxis

El `input` y `output` filters compatibilidad:

- `,` para o
- `&` para y
- `!` para no

Ejemplos:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT puede ser combined con todos tres elemento-reference atributos. Commas y ampersands dentro de an
SNBT compound/lista son conservado dentro de El reference; solo arriba-level separators split El filtro:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## renderizado orden

GuideNH tries recipes en esto orden:

1. NEI native handler renderizado
2. NEI slot-datos reserva
3. built-en vanilla crafting reserva

Si nothing coincide:

- `fallbackText` es usado cuando present
- otherwise Un inline authoring error es mostrado

## Ejemplos

### único recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### varios recipes

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler blacklist y whitelist

`handlerBlacklist` drops handlers de El resultados, `handlerWhitelist` puts ellos atrás. ambos take a
**comma-separated lista**, y Un entrada coincide El handler id, su overlay id o su class nombre,
case-insensitively y as Un substring — so uno entrada puede nombre a único handler o Un whole package.

Un handler es dropped cuando it es named by Un blacklist **salvo** El etiqueta asks para it: `handlerId`,
`handlerWhitelist` y Un lowercased class-nombre coincidir todos Conserve it.

**Un lista, because uno handler id rarely appears alone.** Planks son ambos Un crafting material y Un fuel, so
lista El handler ids you do no want:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**Un package en uno entrada.** Un entrada es un substring of El class nombre too, so Un mod id covers cada handler
que mod registers — handy cuando uno mod's handlers son todos noise para esto elemento:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Whitelist, Un undo Un blacklist para uno página.** El configuration oculta dos multiblock handlers on cada
página. Un whitelist naming ellos brings ellos atrás here, y Un lista works El mismo way:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Whitelist hace no filtro on su propio.** It solo rescues handlers Un blacklist would drop; usar
`handlerId` / `handlerName` / `handlerOrder` Un narrow El resultado Un uno handler, y `limit` Un cap how
many son drawn.

El configuration archivo `config/guidenh/guidenh.cfg` holds `recipeHandlerBlacklist`, que aplica Un cada
página y by predeterminado contiene:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

Un página's propio lista es **añadido** Un El configured uno, so Un página puede ocultar más but no fewer. Editing El
configuration cambios cada página; naming Un handler on Un etiqueta cambios solo que página.

### entrada/salida filtering

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### reserva texto

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## Best Practices

- usar `fallbackText` para opcional-mod integrations
- usar `handlerId` cuando you know El exact NEI handler you want
- usar `handlerBlacklist` cuando uno bloque es produced by many handlers y solo some son worth showing
- El dos handlers debajo son ocultas by predeterminado en `config/guidenh/guidenh.cfg` because Estas renderizan Un whole multiblock; nombre uno con `handlerId` o `handlerWhitelist` Un mostrar it
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- usar `limit` cuando Un etiqueta could expand en many recipes
- Conserve complex filtro logic en comments cerca El etiqueta para maintainability

## Live ejecución Ejemplo

Consulta `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` para extensive recipe samples, including handler filters y wildcard/NBT elemento ids.
