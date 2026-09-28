# Receitas

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH pode renderizar crafting e NEI-backed Receitas directly dentro de guia pages.

## Compatível tags

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

todos três share O mesmo compilador e Atributo Defina.

## tag Semantics

| tag              | Behavior                                               |
|------------------|--------------------------------------------------------|
| `<Recipe>`       | renderizar a único recipe para O destino item             |
| `<Usage>`        | renderizar a único recipe using O destino item           |
| `<RecipeFor>`    | mesmo único-recipe behavior, mais author-friendly nome |
| `<RecipeUsage>`  | mesmo único-recipe behavior, mais author-friendly nome |
| `<RecipesFor>`   | renderizar vários correspondente Receitas                       |
| `<RecipesUsage>` | renderizar vários correspondente Receitas                       |

Se vários Receitas exist e you usar O único-recipe forms, GuideNH renders somente um resultado a menos que filters narrow it further.

## comum atributos

| Atributo      | Obrigatório | Significado                                           |
|----------------|----------|---------------------------------------------------|
| `id`           | sim      | destino item reference                             |
| `fallbackText` | não       | texto exibido quando não usable recipe é found         |
| `handlerName`  | não       | case-insensitive substring filtro on handler nome |
| `handlerId`    | não       | exact overlay/handler id filtro, case-insensitive |
| `handlerBlacklist` | não   | comma-separated handlers para drop de O results |
| `handlerWhitelist` | não   | comma-separated handlers para Mantenha, even Se blacklisted |
| `handlerOrder` | não       | 0-based index depois de handler filtering             |
| `input`        | não       | ingredient filtro expressão                      |
| `output`       | não       | resultado filtro expressão                          |
| `limit`        | não       | positive inteiro max número of renderizado Receitas   |

## item Id sintaxe

O `id`, `input`, e `output` atributos todos usar O GuideNH extended item reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta valores:

- `*`
- `32767`
- uppercase tokens como `ANY`

## filtro expressão sintaxe

O `input` e `output` filters suporte:

- `,` para ou
- `&` para e
- `!` para não

Exemplos:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT pode ser combined com todos três item-reference atributos. Commas e ampersands dentro de an
SNBT compound/lista são kept dentro de O reference; somente topo-level separators split O filtro:

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## renderização ordem

GuideNH tries Receitas em Este ordem:

1. NEI native handler renderização
2. NEI slot-dados reserva
3. built-em vanilla crafting reserva

Se nothing corresponde:

- `fallbackText` é usado quando present
- otherwise Um inline authoring erro é exibido

## Exemplos

### único recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### vários Receitas

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Handler blacklist e whitelist

`handlerBlacklist` drops handlers de O results, `handlerWhitelist` puts eles back. ambos take a
**comma-separated lista**, e Um entrada corresponde O handler id, seu overlay id ou seu class nome,
case-insensitively e as Uma substring — so um entrada pode nome a único handler ou Uma whole package.

Uma handler é dropped quando it é named by Uma blacklist **a menos que** O tag asks para it: `handlerId`,
`handlerWhitelist` e Uma lowercased class-nome corresponder todos Mantenha it.

**Uma lista, because um handler id rarely appears alone.** Planks são ambos Uma crafting material e Uma fuel, so
lista O handler ids you do não want:

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

**Uma package em um entrada.** Um entrada é Uma substring of O class nome too, so Uma mod id covers cada handler
que mod registers — handy quando um mod's handlers são todos noise para Este item:

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

**Whitelist, para undo Uma blacklist para um página.** O configuration oculta dois multiblock handlers on cada
página. Uma whitelist naming eles brings eles back here, e Uma lista works O mesmo way:

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

**Whitelist não faz filtro on seu próprio.** It somente rescues handlers Uma blacklist would drop; usar
`handlerId` / `handlerName` / `handlerOrder` para narrow O resultado para um handler, e `limit` para cap how
many são drawn.

O configuration arquivo `config/guidenh/guidenh.cfg` holds `recipeHandlerBlacklist`, que aplica para cada
página e by Padrão contém:

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

Uma página's próprio lista é **adicionado** para O configured um, so Uma página pode ocultar mais but não fewer. Editing O
configuration alterações cada página; naming Uma handler on Uma tag alterações somente que página.

### entrada/saída filtering

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
- usar `handlerId` quando you know O exact NEI handler you want
- usar `handlerBlacklist` quando um bloco é produced by many handlers e somente some são worth showing
- O dois handlers abaixo são oculto by Padrão em `config/guidenh/guidenh.cfg` because Elas renderizar Uma whole multiblock; nome um com `handlerId` ou `handlerWhitelist` para mostrar it
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- usar `limit` quando Uma tag could expand em many Receitas
- Mantenha complex filtro logic em comments perto O tag para maintainability

## Live execução Exemplo

Consulte `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` para extensive recipe samples, including handler filters e wildcard/NBT item ids.
